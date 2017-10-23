/**
 * $Id: QueryEqualsTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qgraph2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qged.QueryCanvasTest;
import org.apache.log4j.Logger;


public class QueryEqualsTest extends TestCase {

    private static final Logger log = Logger.getLogger(QueryEqualsTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests that the two query instances loaded from the same file are equals().
     *
     * @throws Exception
     */
    public void testQueryEquals() throws Exception {
        verifyQueryEquals("test-query.xml");
        verifyQueryEquals("hot100-neighborhood.qg2.xml");
    }

    /**
     * Loads the Query in fileName twice and ensures that the two copies are
     * equal().
     *
     * @param fileName input qgraph query file name (relative to this class) to load
     */
    private void verifyQueryEquals(String fileName) throws Exception {
        Query query1 = QueryCanvasTest.loadQueryFromFile(getClass(), fileName);
        Query query2 = QueryCanvasTest.loadQueryFromFile(getClass(), fileName);
        assertTrue(query1.equals(query2));
    }

}
