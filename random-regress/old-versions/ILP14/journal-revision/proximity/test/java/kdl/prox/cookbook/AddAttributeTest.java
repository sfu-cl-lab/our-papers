/**
 * $Id: AddAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AddAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;

public class AddAttributeTest extends TestCase {

    public void setUp() {
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // initialize the database
        DB.getRootContainer().deleteAllChildren();
        DB.getRootContainer().createChild("studio-clusters");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("1, 1, 'Movie'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("2, 1, 'Movie'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("3, 1, 'Studio'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("4, 1, 'Actor'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("5, 1, 'Actor'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("6, 2, 'Movie'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("7, 2, 'Studio'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("8, 2, 'Director'");
        DB.getContainer("studio-clusters").getObjectsNST().insertRow("9, 2, 'Actor'");

    }


    public void tearDown() {
        TestUtil.closeTestConnection();
    }


    public void testIt() {
        Container c = DB.getContainer("studio-clusters");
        NST objects = c.getObjectsNST();
        objects.addDistinctCountColumn("subg_id", "name", "cnt");
        objects.print();
        NST attrNST = objects.projectDistinct("subg_id, cnt");
        attrNST.renameColumns("id, value");
        objects.removeColumn("cnt");
        attrNST.print();
        c.getSubgraphAttrs().defineAttributeWithData("distinct_types", "int", attrNST);

        assertEquals(2, c.getSubgraphAttrs().getAttrDataNST("distinct_types").count());

    }

}
