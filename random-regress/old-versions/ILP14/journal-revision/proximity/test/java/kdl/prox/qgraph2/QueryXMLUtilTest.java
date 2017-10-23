/**
 * $Id: QueryXMLUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qgraph2;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Element;


public class QueryXMLUtilTest extends TestCase {

    private static final Logger log = Logger.getLogger(QueryXMLUtilTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private File getTempXMLFile() {
        return new File(System.getProperty("java.io.tmpdir", "tmp"), "query-test.xml");
    }

    public void testQueryXMLUtil() throws Exception {
        verifyQueryFile("test-query-util.xml");
        verifyQueryFile("test-query-cond.xml");
        verifyQueryFile("frequent_coauthor.xml");
        verifyQueryFile("hot100-neighborhood-util.qg2.xml");
    }

    /**
     * Loads the specified query, creates XML for it, and compares the two.
     *
     * @param fileName input qgraph query file name (relative to this class) to load
     */
    private void verifyQueryFile(String fileName) throws Exception {
        URL expXMLFileURL = getClass().getResource(fileName);
        File expQueryFile = new File(expXMLFileURL.getFile());
        Element expGraphQueryEle = QueryXMLUtil.graphQueryEleFromFile(expQueryFile);
        Query expQuery = QueryXMLUtil.graphQueryEleToQuery(expGraphQueryEle);

        File actualXMLFile = getTempXMLFile();
        Element actGraphQueryEle = QueryXMLUtil.queryToGraphQueryEle(expQuery);
        QueryXMLUtil.graphQueryEleToFile(actGraphQueryEle, actualXMLFile);

        String expectedFileCont = Util.readStringFromFile(expQueryFile);
        String actualFileCont = Util.readStringFromFile(actualXMLFile);
        assertEquals(expectedFileCont, actualFileCont);
    }

}
