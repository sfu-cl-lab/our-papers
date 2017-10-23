/**
 * $Id: KeyNotInFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: KeyNotInFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;

/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class KeyNotInFilterTest extends TestCase {

    NST nst;
    NST filterNST;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        createTestNST();
        createFilterNST();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        filterNST.release();
        nst.release();
        TestUtil.closeTestConnection();
    }


    public void testApplyFindsExistingKey() throws Exception {
        filterNST.insertRow(new String[]{"1"});
        Filter filter = new KeyNotInFilter(filterNST, "oid");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 2) {
            fail("Expected 2 rows for key != 1, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }


    public void testApplyFindsWrongKey() throws Exception {
        filterNST.insertRow(new String[]{"4"});
        Filter filter = new KeyNotInFilter(filterNST, "oid");
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 3) {
            fail("Expected 3 rows for key != 4, found " + foundRows);
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


    private void createFilterNST() {
        String[] columnNames = new String[]{"oid"};
        String[] columnTypes = new String[]{"oid"};
        filterNST = new NST(columnNames, columnTypes);
    }


    /**
     * Testing only constructor with a single string (BAT)
     *
     * @throws Exception
     */
    public void testApplyFindsExistingKeyFromBAT() throws Exception {
        filterNST.insertRow(new String[]{"1"});
        Filter filter = new KeyNotInFilter(filterNST.getNSTColumn("oid").getBATName());
        String resultBat = Connection.executeAndSave(filter.getApplyCmd(nst));
        int foundRows = MonetUtil.getRowCount(resultBat);
        if (foundRows != 2) {
            fail("Expected 2 rows for key != 1, found " + foundRows);
        }
        Connection.releaseSavedVar(resultBat);
    }


}
