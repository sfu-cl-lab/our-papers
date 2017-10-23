/**
 * $Id: ColumnValueLikeFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnValueLikeFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class ColumnValueLikeFilterTest extends TestCase {

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


    public void testApplyFindsStrings() throws Exception {
        Filter filter = new ColumnValueLikeFilter("name", "joh%");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 2) {
            fail("Expected 2 rows for name=john, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }


    public void testFail() throws Exception {
        Filter filter = new ColumnValueLikeFilter("name", "joh"); // no %
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
