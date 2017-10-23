/**
 * $Id: DegreeAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DegreeAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;

public class DegreeAttributeTest extends TestCase {

    public void setUp() {
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.getObjectNST().insertRow("1");
        DB.getObjectNST().insertRow("2");
        DB.getObjectNST().insertRow("3");
        DB.getObjectNST().insertRow("4");
        DB.getObjectNST().insertRow("5");
        DB.getObjectNST().insertRow("6");

        DB.getLinkNST().deleteRows();
        DB.getLinkNST().insertRow("1, 1, 3");
        DB.getLinkNST().insertRow("2, 2, 3");
        DB.getLinkNST().insertRow("3, 2, 4");
        DB.getLinkNST().insertRow("4, 1, 2");
        DB.getLinkNST().insertRow("5, 2, 1");

        DB.getObjectAttrs().deleteAllAttributes();
    }

    public void tearDown() {
        TestUtil.closeTestConnection();
    }


    public void testDegree() {
        NST inverseLinks = DB.getLinkNST().project("o2_id, o1_id");
        NST biDirLinks = DB.getLinkNST().project("o1_id, o2_id");
        biDirLinks.insertRowsFromNST(inverseLinks);

        NST fullDegreeNST = biDirLinks.aggregate("count", "o1_id", "o2_id");

        // create attribute
        fullDegreeNST.renameColumns("id, value");
        DB.getObjectAttrs().defineAttributeWithData("degree", "int", fullDegreeNST);

        // test values
        NST attrNST = DB.getObjectAttrs().getAttrDataNST("degree");
        TestUtil.verifyCollections(new String[]{"1@0.3", "2@0.4", "3@0.2", "4@0.1"}, attrNST);
    }

    public void testDegreeToUniqueObjects() {
        NST inverseLinks = DB.getLinkNST().project("o2_id, o1_id");
        NST biDirLinks = DB.getLinkNST().project("o1_id, o2_id");
        biDirLinks.insertRowsFromNST(inverseLinks);

        NST degreeNST = biDirLinks.aggregate("card", "o1_id", "o2_id");
        DB.getObjectAttrs().defineAttributeWithData("degree", "int", degreeNST);

        // test values
        NST attrNST = DB.getObjectAttrs().getAttrDataNST("degree");
        TestUtil.verifyCollections(new String[]{"1@0.2", "2@0.3", "3@0.2", "4@0.1"}, attrNST);
    }

    public void testDegreeWithAllObjects() {
        NST inverseLinks = DB.getLinkNST().project("o2_id, o1_id");
        NST biDirLinks = DB.getLinkNST().project("o1_id, o2_id");
        biDirLinks.insertRowsFromNST(inverseLinks);

        NST zeroes = DB.getObjectNST().copy();
        zeroes.renameColumn("id", "o1_id");
        NST fullDegreeNST = biDirLinks.aggregate("count", "o1_id", "o2_id", zeroes);

        // create attribute
        fullDegreeNST.renameColumns("id, value");
        DB.getObjectAttrs().defineAttributeWithData("degree", "int", fullDegreeNST);

        // test values
        NST attrNST = DB.getObjectAttrs().getAttrDataNST("degree");
        attrNST.print();
        TestUtil.verifyCollections(new String[]{"1@0.3", "2@0.4", "3@0.2", "4@0.1", "5@0.0", "6@0.0"},
                attrNST);
    }


}
