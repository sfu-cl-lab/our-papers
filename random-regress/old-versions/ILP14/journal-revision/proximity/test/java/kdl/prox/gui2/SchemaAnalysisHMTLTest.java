/**
 * SchemaAnalysisHMTLTest.java,v 1.1 2004/04/09 14:16:09 schapira Exp
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.gui2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.db.SchemaAnalysis;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;


public class SchemaAnalysisHMTLTest extends TestCase {

    private static final Logger log = Logger.getLogger(SchemaAnalysisHMTLTest.class);

    private String dirByType = "isDirectedBy", shareType = "sharesStudio";
    private String movieType = "movie", dirType = "director";
    private String objTypeStr = "objecttype";
    private String objNameStr = "name";
    private String objLabelStr = "label";
    private String objTitleStr = "title";
    private String linkTypeStr = "linktype";
    private String linkSalaryStr = "salary";

    int numMovies = 4;


    /**
     * objects:
     * name    id     objType    name   title   label
     * ----    --     -------    ----   -----   -----
     * M1       0     movie             foo     M1
     * M2       1     movie             bar     M2
     * M3       2     movie             foo     M3
     * M4       3     movie             bar     M4
     * D1       4     director   Paul           D5
     * D2       5     director   John           D6
     * <p/>
     * links:
     * from    to    id     linkType       salary
     * ----    --    --     --------       ------
     * M2      D2    0      isDirectedBy   10
     * M4      D2    1      isDirectedBy   20
     * M1      D1    2      isDirectedBy   30
     * M3      D1    3      isDirectedBy   40
     * <p/>
     * M1      D1    4      sharesStudio
     * D1      M1    5      sharesStudio
     * M2      M4    6      sharesStudio
     * M2      D2    7      sharesStudio
     * D2      M2    8      sharesStudio
     * M3      M1    9      sharesStudio
     * M3      D1    10     sharesStudio
     * D1      M3    11     sharesStudio
     * M4      M2    12     sharesStudio
     * M4      D2    13     sharesStudio
     * D2      M4    14     sharesStudio
     */
    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getRootContainer().deleteAllChildren();
        DB.deleteTempContainers();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        // define attributes on objects
        Attributes objAttrs = DB.getObjectAttrs();
        objAttrs.deleteAllAttributes();
        objAttrs.defineAttributeOrClearValuesIfExists(objTypeStr, "str");
        objAttrs.defineAttributeOrClearValuesIfExists(objNameStr, "str");
        objAttrs.defineAttributeOrClearValuesIfExists(objTitleStr, "str");
        objAttrs.defineAttributeOrClearValuesIfExists(objLabelStr, "str");

        // define attributes on links
        Attributes linkAttrs = DB.getLinkAttrs();
        linkAttrs.deleteAllAttributes();
        linkAttrs.defineAttributeOrClearValuesIfExists(linkTypeStr, "str");
        linkAttrs.defineAttributeOrClearValuesIfExists(linkSalaryStr, "str");
        NST linkTypeNST = linkAttrs.getAttrDataNST(linkTypeStr);

        // add objects
        // these arrays hold the objIds of the directors and movies, as
        // they have been inserted into the DB
        int objId = 0;
        int[] movies = new int[numMovies];
        int[] directors = new int[numMovies / 2];
        NST objTypeNST = objAttrs.getAttrDataNST(objTypeStr);

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

        // insert attributes
        NST objNameNST = objAttrs.getAttrDataNST(objNameStr);
        objNameNST.insertRow(new String[]{"4", "Paul"});
        objNameNST.insertRow(new String[]{"5", "John"});
        NST objTitleNST = objAttrs.getAttrDataNST(objTitleStr);
        objTitleNST.insertRow(new String[]{"0", "foo"});
        objTitleNST.insertRow(new String[]{"1", "bar"});
        objTitleNST.insertRow(new String[]{"2", "foo"});
        objTitleNST.insertRow(new String[]{"3", "bar"});
        NST objLabelNST = objAttrs.getAttrDataNST(objLabelStr);
        objLabelNST.insertRow(new String[]{"0", "M1"});
        objLabelNST.insertRow(new String[]{"1", "M2"});
        objLabelNST.insertRow(new String[]{"2", "M3"});
        objLabelNST.insertRow(new String[]{"3", "M4"});
        objLabelNST.insertRow(new String[]{"4", "D1"});
        objLabelNST.insertRow(new String[]{"5", "D1"});


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

        // link attributes
        NST linkSalaryNST = linkAttrs.getAttrDataNST(linkSalaryStr);
        linkSalaryNST.insertRow(new String[]{"0", "10"});
        linkSalaryNST.insertRow(new String[]{"1", "20"});
        linkSalaryNST.insertRow(new String[]{"2", "30"});
        linkSalaryNST.insertRow(new String[]{"3", "40"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testGetSchemaPage() {
        String expectedBody = "<html><body>\n" +
                "<h2>Schema Analysis: Report</h2>\n" +
                "<h2>Object Types</h2>\n" +
                "<table border=1 valign=top>\n" +
                "  <tr>\n" +
                "    <td><strong>Type</strong></td>\n" +
                "    <td><strong>Associated Attributes</strong></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong>movie</strong></td>\n" +
                "    <td>title, label</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong>director</strong></td>\n" +
                "    <td>label, name</td>\n" +
                "  </tr>\n" +
                "</table>\n" +
                "<p>\n" +
                "<h2>Link Types</h2>\n" +
                "<table border=1 valign=top>\n" +
                "  <tr>\n" +
                "    <td><strong>Type</strong></td>\n" +
                "    <td><strong>Connects Types...</strong></td>\n" +
                "    <td><strong>Associated Attributes</strong></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong>isDirectedBy</strong></td>\n" +
                "    <td>movie -> director (100%)</td>\n" +
                "    <td>salary</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong>sharesStudio</strong></td>\n" +
                "    <td>director -> movie (36%), movie -> director (36%), movie -> movie (27%)</td>\n" +
                "    <td></td>\n" +
                "  </tr>\n" +
                "</table>\n" +
                "<p>\n" +
                "</body></html>\n";

        String actualBody = RunSchemaAnalysisImpl.getDBSchemaHTML(new SchemaAnalysis(objTypeStr, linkTypeStr));
        assertEquals(expectedBody, actualBody);
    }

}
