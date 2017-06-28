/**
 * $Id: ColumnValueRandomFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnValueRandomFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;


public class ColumnValueRandomFilterTest extends TestCase {

    static Logger log = Logger.getLogger(ColumnValueRandomFilterTest.class);

    NST nst;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        createTestNST();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        nst.release();
        TestUtil.closeTestConnection();
    }


    /**
     * Test that constructor doesn't take null arguments
     *
     * @throws Exception
     */
    public void testNullArguments() throws Exception {
        try {
            new ColumnValueRandomFilter("", 3);
            fail("Created a filter with an empty column name --invalid");
        } catch (Exception e) {
            //ignore
        }
    }


    public void testIt() throws Exception {
        Filter filter = new ColumnValueRandomFilter("name", 2);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(3, foundRows);
        Connection.releaseSavedVar(resultBat);
    }


    public void testOverSize() throws Exception {
        Filter filter = new ColumnValueRandomFilter("name", 100);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(3, foundRows);
        Connection.releaseSavedVar(resultBat);
    }


    public void testRandomSize() throws Exception {
        Filter filter = new ColumnValueRandomFilter("name", 1);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows == 1 || foundRows == 2) {
            // this is correct
        } else {
            fail("Wrong number of rows; should have returned 1 or 2: " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }

    public void testZeroSize() throws Exception {
        Filter filter = new ColumnValueRandomFilter("name", 0);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(0, foundRows);
        Connection.releaseSavedVar(resultBat);
    }

    private void createTestNST() {
        String[] columnNames = new String[]{"name", "phone"};
        String[] columnTypes = new String[]{"str", "int"};
        nst = new NST(columnNames, columnTypes);
        nst.insertRow(new String[]{"john", "253"});
        nst.insertRow(new String[]{"paul", "471"});
        nst.insertRow(new String[]{"john", "324"});
    }


}
