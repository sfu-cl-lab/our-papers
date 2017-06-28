/**
 * $Id: AggregateTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AggregateTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;

public class AggregateTest extends TestCase {

    public void setUp() {
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // initialize the database
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getLinkNST().deleteRows();
    }


    public void tearDown() {
        TestUtil.closeTestConnection();
    }


    // general tests
    public void testIt() {
        NST s = new NST("id, value", "oid, int");
        s.insertRow("1,10");
        s.insertRow("1,20");
        s.insertRow("2,5");
        s.insertRow("2,7");
        s.insertRow("3,10");
        s.insertRow("3,10");

        NST ms = s.aggregate("max", "id", "value");
        TestUtil.verifyCollections(new String[]{"1@0.20", "2@0.7", "3@0.10"}, ms);

        NST as = s.aggregate("avg", "id", "value");
        TestUtil.verifyCollections(new String[]{"1@0.15", "2@0.6", "3@0.10"}, as);

        NST ds = s.aggregate("card", "id", "value");
        TestUtil.verifyCollections(new String[]{"1@0.2", "2@0.2", "3@0.1"}, ds);

        NST b = s.copy();
        b.addConditionColumn("value >= 10", "gte10");
        NST gte = b.aggregate("size", "id", "gte10");
        TestUtil.verifyCollections(new String[]{"1@0.2", "2@0.0", "3@0.2"}, gte);
    }


    // Test use of aggregator to find out-degree of objects
    public void testLinks() {
        DB.getLinkNST().insertRow("1, 0, 1");
        DB.getLinkNST().insertRow("2, 0, 2");
        DB.getLinkNST().insertRow("3, 0, 3");
        DB.getLinkNST().insertRow("4, 1, 2");
        DB.getLinkNST().insertRow("5, 1, 4");
        DB.getLinkNST().insertRow("6, 2, 1");

        NST od = DB.getLinkNST().aggregate("count", "o1_id", "o2_id");
        TestUtil.verifyCollections(new String[]{"0@0.3", "1@0.2", "2@0.1"}, od);
    }

    // test types of created columns
    public void testTypes() {
        NST s = new NST("id, value", "oid, int");
        s.insertRow("1,10");
        s.insertRow("1,11");
        s.insertRow("2,5");
        s.insertRow("2,6");

        NST as = s.aggregate("avg", "id", "value");
        TestUtil.verifyCollections(new String[]{"1@0.10.5", "2@0.5.5"}, as);
        assertEquals("oid", as.getNSTColumn("id").getType().toString());
        assertEquals("dbl", as.getNSTColumn("value").getType().toString());
    }
}
