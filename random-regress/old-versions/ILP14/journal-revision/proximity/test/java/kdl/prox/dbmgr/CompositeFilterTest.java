/**
 * $Id: CompositeFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CompositeFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class CompositeFilterTest extends TestCase {

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

    public void testApplyAND() throws Exception {
        Filter filter1 = new ColumnValueLikeFilter("name", "j%");
        Filter filter2 = new ColumnValueFilter("phone", "253");
        CompositeFilter filter = new CompositeFilter(filter1, LogicalConnectorEnum.AND, filter2);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 1) {
            fail("Expected 1 rows for phone=253 && name=john, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }

    public void testApplyOR() throws Exception {
        Filter filter1 = new ColumnValueLikeFilter("name", "j%");
        Filter filter2 = new ColumnValueFilter("phone", "471");
        CompositeFilter filter = new CompositeFilter(filter1, LogicalConnectorEnum.OR, filter2);
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 3) {
            fail("Expected 3 rows for phone=471 || name=john, found " + foundRows);
        }
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
