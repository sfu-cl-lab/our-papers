/**
 * $Id: ColumnDistinctFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnDistinctFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.MonetUtil;

import java.util.List;


public class ColumnDistinctFilterTest extends TestCase {

    NST phoneNST;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        createTestNST();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        phoneNST.release();
        TestUtil.closeTestConnection();
    }


    public void testIt() throws Exception {
        phoneNST.addKeyColumn("key");
        int minKey = phoneNST.min("key");
        phoneNST.removeColumn("key");

        Filter filter = new ColumnDistinctFilter("phone");
        // make sure that list contains "0", "1", and either "2" or "3"
        List oids = MonetUtil.read(filter.getApplyCmd(phoneNST)).toOIDList(0);
        assertEquals(3, oids.size());
        assertTrue(oids.contains(new Integer(minKey + 0)));
        assertTrue(oids.contains(new Integer(minKey + 1)));
        assertTrue(oids.contains(new Integer(minKey + 2)) || oids.contains(new Integer(minKey + 3)));
    }


    private void createTestNST() {
        String[] columnNames = new String[]{"name", "phone"};
        String[] columnTypes = new String[]{"str", "int"};
        phoneNST = new NST(columnNames, columnTypes);
        phoneNST.insertRow(new String[]{"john", "253"});
        phoneNST.insertRow(new String[]{"paul", "471"});
        phoneNST.insertRow(new String[]{"andrew", "324"});
        phoneNST.insertRow(new String[]{"amy", "324"});
    }


}
