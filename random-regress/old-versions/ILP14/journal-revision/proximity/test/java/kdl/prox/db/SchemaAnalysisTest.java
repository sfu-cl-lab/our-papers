/**
 * $Id: SchemaAnalysisTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: SchemaAnalysisTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.MonetException;
import org.apache.log4j.Logger;


/**
 * MIL Lab: Defines unit tests for SchemaAnalysisLabEx.
 */
public class SchemaAnalysisTest extends TestCase {

    private static final Logger log = Logger.getLogger(SchemaAnalysisTest.class);
    private SchemaAnalysis schemaAnalLE;

    private String dirByType = "IsDirectedBy", shareType = "SharesStudio";
    private String movieType = "Movie", dirType = "Director";
    private String objTypeStr = "objecttype";
    private String linkTypeStr = "linktype";
    int numMovies = 4;


    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getRootContainer().deleteAllChildren();
        DB.deleteTempContainers();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        // define an attribute called objecttype on objects
        Attributes objAttrs = DB.getObjectAttrs();
        objAttrs.deleteAllAttributes();
        objAttrs.defineAttribute(objTypeStr, "str");
        NST objTypeNST = objAttrs.getAttrDataNST(objTypeStr);

        // define a link type attribute on links
        Attributes linkAttrs = DB.getLinkAttrs();
        linkAttrs.deleteAllAttributes();
        linkAttrs.defineAttribute(linkTypeStr, "str");
        NST linkTypeNST = linkAttrs.getAttrDataNST(linkTypeStr);

        // objects:
        // name    id     objType
        // ----    --     -------
        // M1       1     movie
        // M2       2     movie
        // M3       3     movie
        // M4       4     movie
        // D1       5     director
        // D2       6     director
        //
        // links:
        // from    to    id     linkType
        // ----    --    --     --------
        // M2      D2    1      isDirectedBy
        // M4      D2    2      isDirectedBy
        // M1      D1    3      isDirectedBy
        // M3      D1    4      isDirectedBy

        // M1      D1    5      sharesStudio
        // D1      M1    6      sharesStudio
        // M2      M4    7      sharesStudio
        // M2      D2    8      sharesStudio
        // D2      M2    9      sharesStudio
        // M3      M1    10      sharesStudio
        // M3      D1    11     sharesStudio
        // D1      M3    12     sharesStudio
        // M4      M2    13     sharesStudio
        // M4      D2    14     sharesStudio
        // D2      M4    15     sharesStudio


        // add objects
        int objId = 0;


        // these arrays hold the objIds of the directors and movies, as
        // they have been inserted into the DB
        int[] movies = new int[numMovies];
        int[] directors = new int[numMovies / 2];

        for (int movieCount = 0; movieCount < numMovies; movieCount++) {
            DB.insertObject(objId);
            movies[movieCount] = objId;
            objTypeNST.insertRow(new String[]{"" + objId, movieType});
            objId++;
        }

        for (int dirCount = 0; dirCount < numMovies / 2; dirCount++) {
            DB.insertObject(objId);
            directors[dirCount] = objId;
            objTypeNST.insertRow(new String[]{"" + objId, dirType});
            objId++;
        }

        // add directed by links
        int linkId = 0;
        for (int i = 0; i < numMovies; i++) {
            DB.insertLink(linkId, movies[i], directors[i % 2]);
            linkTypeNST.insertRow(new String[]{"" + linkId, dirByType});
            linkId++;
        }

        // add sharesStudio links
        // M1 is special, so there is a special section ahead of the for loop to deal with it
        DB.insertLink(linkId, movies[0], directors[0 % 2]);
        linkTypeNST.insertRow(new String[]{"" + linkId, shareType});
        linkId++;
        DB.insertLink(linkId, directors[0 % 2], movies[0]);
        linkTypeNST.insertRow(new String[]{"" + linkId, shareType});
        linkId++;
        // add in the rest of the "sharesStudio" links
        for (int i = 1; i < numMovies; i++) {
            DB.insertLink(linkId, movies[i], directors[i % 2]);
            linkTypeNST.insertRow(new String[]{"" + linkId, shareType});
            linkId++;
            DB.insertLink(linkId, directors[i % 2], movies[i]);
            linkTypeNST.insertRow(new String[]{"" + linkId, shareType});
            linkId++;
            DB.insertLink(linkId, movies[i], movies[i % 2]);
            linkTypeNST.insertRow(new String[]{"" + linkId, shareType});
            linkId++;
        }
        objTypeNST.release();
        linkTypeNST.release();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testGetLinkConnectedTypes() {
        try {
            schemaAnalLE = new SchemaAnalysis(objTypeStr, linkTypeStr);

            Set linkTypes = schemaAnalLE.getItemTypes(false);
            String[] testLinks = {dirByType, shareType};
            TestUtil.verifyCollections(testLinks, linkTypes);

            // directed by should be 100% movies to directors, with 4 total links
            Set directedByLinkStats = schemaAnalLE.getLinkConnectedObjStats(dirByType);
            SchemaAnalysis.LinkStats[] directedByStatsArray =
                    {new SchemaAnalysis.LinkStats(movieType, dirType, dirByType, 1.0, 4)};
            TestUtil.verifyCollections(directedByStatsArray, directedByLinkStats);

            Set sharedStudioLinkStats = schemaAnalLE.getLinkConnectedObjStats(shareType);
            int tShare = 11;
            SchemaAnalysis.LinkStats[] sharedStudioStatsArray =
                    {
                        new SchemaAnalysis.LinkStats(movieType, dirType, shareType, ((double) 4) / tShare, 4),
                        new SchemaAnalysis.LinkStats(dirType, movieType, shareType, ((double) 4) / tShare, 4),
                        new SchemaAnalysis.LinkStats(movieType, movieType, shareType, ((double) 3) / tShare, 3)

                    };
            TestUtil.verifyCollections(sharedStudioStatsArray, sharedStudioLinkStats);
        } catch (MonetException mexp) {
            log.error("Got exception");
            log.error(mexp);
        }
    }

}
