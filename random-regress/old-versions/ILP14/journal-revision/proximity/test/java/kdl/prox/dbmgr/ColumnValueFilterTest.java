/**
 * $Id: ColumnValueFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnValueFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class ColumnValueFilterTest extends TestCase {

    /**
     * a db manager and an NST. Setup by setUp()
     */
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
            new ColumnValueFilter("", "a");
            fail("Created a filter with an empty column name --invalid");
        } catch (Exception e) {
        }
        try {
            new ColumnValueFilter("s", null);
            fail("Created a filter with a null expected value --invalid");
        } catch (Exception e) {
        }
    }


    public void testApplyNullArguments() throws Exception {
        Filter filter = new ColumnValueFilter("a", "a");
        try {
            filter.getApplyCmd(null);
            fail("Applied filter to a null nst --invalid");
        } catch (Exception e) {
        }
    }


    public void testApplyFindsStrings() throws Exception {
        Filter filter = new ColumnValueFilter("name", "john");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 2) {
            fail("Expected 2 rows for name=john, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }


    public void testApplyFindsints() throws Exception {
        Filter filter = new ColumnValueFilter("phone", "253");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 1) {
            fail("Expected 1 rows for phone=253, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }

    public void testLE() throws Exception {
        ColumnValueFilter filter = new ColumnValueFilter("phone", ComparisonOperatorEnum.LE, "400");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(2, foundRows);
        Connection.releaseSavedVar(resultBat);
    }

    public void testGE() throws Exception {
        ColumnValueFilter filter = new ColumnValueFilter("phone", ComparisonOperatorEnum.GE, "471");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(1, foundRows);
        Connection.releaseSavedVar(resultBat);
    }

    public void testLT() throws Exception {
        ColumnValueFilter filter = new ColumnValueFilter("phone", ComparisonOperatorEnum.LT, "471");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(2, foundRows);
        Connection.releaseSavedVar(resultBat);
    }

    public void testGT() throws Exception {
        ColumnValueFilter filter = new ColumnValueFilter("phone", ComparisonOperatorEnum.GT, "324");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(1, foundRows);
        Connection.releaseSavedVar(resultBat);
    }


    public void testNE() throws Exception {
        ColumnValueFilter filter = new ColumnValueFilter("name", ComparisonOperatorEnum.NE, "john");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        assertEquals(1, foundRows);
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
