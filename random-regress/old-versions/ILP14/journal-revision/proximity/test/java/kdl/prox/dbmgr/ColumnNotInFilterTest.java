/**
 * $Id: ColumnNotInFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnNotInFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.MonetUtil;

import java.util.List;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class ColumnNotInFilterTest extends TestCase {

    NST phoneNST;
    NST ageNST;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        createTestNSTs();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        phoneNST.release();
        ageNST.release();
        TestUtil.closeTestConnection();
    }


    public void testIt() throws Exception {
        phoneNST.addKeyColumn("key");
        int minKey = phoneNST.min("key");
        phoneNST.removeColumn("key");

        Filter filter = new ColumnNotInFilter("name", ageNST, "name");
        List oids = MonetUtil.read(filter.getApplyCmd(phoneNST)).toOIDList(0);
        assertTrue(oids.contains(new Integer(minKey + 2)));
        assertTrue(oids.contains(new Integer(minKey + 3)));
        assertEquals(2, oids.size());
    }


    private void createTestNSTs() {
        String[] columnNames = new String[]{"name", "phone"};
        String[] columnTypes = new String[]{"str", "int"};
        phoneNST = new NST(columnNames, columnTypes);
        phoneNST.insertRow(new String[]{"john", "253"});
        phoneNST.insertRow(new String[]{"paul", "471"});
        phoneNST.insertRow(new String[]{"andrew", "324"});
        phoneNST.insertRow(new String[]{"amy", "324"});
        String[] columnNames1 = new String[]{"name", "age"};
        String[] columnTypes1 = new String[]{"str", "int"};
        ageNST = new NST(columnNames1, columnTypes1);
        ageNST.insertRow(new String[]{"john", "20"});
        ageNST.insertRow(new String[]{"paul", "25"});
    }


}