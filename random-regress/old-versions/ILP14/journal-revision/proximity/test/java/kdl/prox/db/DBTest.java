/**
 * $Id: DBTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DBTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;

import java.util.List;


/**
 * Tests the DB class.
 *
 * @see DB
 */
public class DBTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        Connection.beginScope();
        // some objects with attributes
        DB.getObjectNST().deleteRows();
        DB.getObjectNST().insertRow(new String[]{"1"});
        DB.getObjectNST().insertRow(new String[]{"3"});
        DB.getObjectNST().insertRow(new String[]{"2"});

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("test-1", "str");
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"1", "a"});
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"2", "b"});
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"3", "c"});

        DB.getObjectAttrs().defineAttribute("test-2", "str");
        DB.getObjectAttrs().getAttrDataNST("test-2").insertRow(new String[]{"1", "x"});
        DB.getObjectAttrs().getAttrDataNST("test-2").insertRow(new String[]{"2", "y"});

        DB.getObjectAttrs().defineAttribute("test-3", "str");
        DB.getObjectAttrs().getAttrDataNST("test-3").insertRow(new String[]{"1", "m"});
        DB.getObjectAttrs().getAttrDataNST("test-3").insertRow(new String[]{"2", "n"});

        // some links with attributes
        DB.getLinkNST().deleteRows();
        DB.getLinkNST().insertRow(new String[]{"1", "0", "1"});
        DB.getLinkNST().insertRow(new String[]{"2", "1", "0"});
        DB.getLinkNST().insertRow(new String[]{"3", "0", "2"});

        DB.getLinkAttrs().deleteAllAttributes();
        DB.getLinkAttrs().defineAttribute("test-1", "str");
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"1", "a"});
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"2", "b"});
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"3", "c"});

        DB.getLinkAttrs().defineAttribute("test-2", "str");
        DB.getLinkAttrs().getAttrDataNST("test-2").insertRow(new String[]{"1", "x"});
        DB.getLinkAttrs().getAttrDataNST("test-2").insertRow(new String[]{"2", "y"});

        DB.getLinkAttrs().defineAttribute("test-n", "str");
        DB.getLinkAttrs().getAttrDataNST("test-n").insertRow(new String[]{"1", "x"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }

    public void testcreateLinks() {
        DB.getLinkAttrs().deleteAllAttributes();
        DB.getLinkNST().deleteRows();

        DB.getLinkNST().insertRow(new String[]{"1", "0", "1"});
        DB.getLinkNST().insertRow(new String[]{"2", "1", "0"});
        DB.getLinkNST().insertRow(new String[]{"3", "0", "2"});
        NST toConnect = new NST("from, to, attr_test-3, not_an_attr", "oid, oid, str, int"); //not an attr ignored
        toConnect.insertRow("1, 3, x, 5").insertRow("2,3,x, 5");

        // Create new links, and new attribute
        DB.createLinks(toConnect);
        List addedLinks = toConnect.selectRows("link_id").toOIDList(1);
        assertEquals(2, addedLinks.size());
        assertTrue(addedLinks.contains(new Integer(4)));
        assertTrue(addedLinks.contains(new Integer(5)));
        assertTrue(DB.getLinkAttrs().isAttributeDefined("test-3"));
        assertFalse(DB.getLinkAttrs().isAttributeDefined("not_an_attr"));
        assertFalse(DB.getLinkAttrs().isAttributeDefined("attr_test_3"));
        NST attrsForLinks = DB.getAttrsForItems(toConnect, DB.getLinkAttrs(), "*", "test-3");
        List addedAttrs = attrsForLinks.selectRows("test-3").toStringList(1);
        assertEquals(2, addedAttrs.size());
        assertTrue(addedAttrs.contains("x"));
        attrsForLinks.release();
        toConnect.release();

        // Create new links on existing attribute
        toConnect = new NST("from, to, attr_test-3, not_an_attr", "oid, oid, str, int"); //not an attr ignored
        toConnect.insertRow("1, 3, x, 5").insertRow("2,3,x, 5");
        DB.createLinks(toConnect);
        addedLinks = toConnect.selectRows("link_id").toOIDList(1);
        assertEquals(2, addedLinks.size());
        assertTrue(addedLinks.contains(new Integer(6)));
        assertTrue(addedLinks.contains(new Integer(7)));
        attrsForLinks = DB.getAttrsForItems(toConnect, DB.getLinkAttrs(), "*", "test-3");
        addedAttrs = attrsForLinks.selectRows("test-3").toStringList(1);
        assertEquals(2, addedAttrs.size());
        assertTrue(addedAttrs.contains("x"));
        attrsForLinks.release();

        toConnect.release();
    }


    public void testCreateNewTempContainer() {
        DB.deleteTempContainers();
        DB.createNewTempContainer();
        DB.createNewTempContainer();
        Container tempParentContainer = DB.getTempParentContainer();
        assertEquals(2, tempParentContainer.getChildrenNames().size());
    }

    public void testDeleteLinks() {
        DB.deleteLinks("test-1 = test-2");
        assertEquals(3, DB.getLinkNST().getRowCount()); //nothing deleted
        DB.deleteLinks("o1_id = 3243 OR o2_id = 345345");
        assertEquals(3, DB.getLinkNST().getRowCount()); //nothing deleted

        DB.deleteLinks("test-1 = 'a'");
        assertEquals(2, DB.getLinkNST().getRowCount());
        assertEquals(2, DB.getLinkAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(1, DB.getLinkAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-n").getRowCount());

        // test-1 = 'c' OR test-1 = 'b' OR test-2 = 'y' removes all, as you'd expect
        DB.deleteLinks("test-1 = 'c' OR test-1 = 'b' OR test-2 = 'y'");
        assertEquals(0, DB.getLinkNST().getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-n").getRowCount());

    }

    public void testDeleteAllLinks() {
        DB.deleteLinks("*");
        assertEquals(0, DB.getLinkNST().getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-n").getRowCount());
    }

    public void testDeleteObjects() {
        DB.deleteObjects("test-1 = test-2"); // objects where test-1 = test-2
        assertEquals(3, DB.getObjectNST().getRowCount()); //nothing deleted

        DB.deleteObjects("test-1 = 'a'"); // objects where test-1 = 'a'
        assertEquals(2, DB.getObjectNST().getRowCount());
        assertEquals(2, DB.getObjectAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(1, DB.getObjectAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(1, DB.getLinkNST().getRowCount()); // links should be removed too
        assertEquals(1, DB.getLinkAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-n").getRowCount());

        // test-1 = 'c' OR test-1 = 'b' OR test-2 = 'y' removes all, as you'd expect
        DB.deleteObjects("test-1 = 'c' OR test-1 = 'b' OR test-2 = 'y'");
        assertEquals(0, DB.getObjectNST().getRowCount());
        assertEquals(0, DB.getObjectAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getObjectAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkNST().getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-2").getRowCount());
        assertEquals(0, DB.getLinkAttrs().getAttrDataNST("test-n").getRowCount());
    }

    public void testDeleteAllObjects() {
        DB.deleteObjects("*");
        assertEquals(0, DB.getObjectNST().getRowCount());
        assertEquals(0, DB.getObjectAttrs().getAttrDataNST("test-1").getRowCount());
        assertEquals(0, DB.getObjectAttrs().getAttrDataNST("test-2").getRowCount());
    }

    public void testDeleteTempContainers() {
        DB.deleteTempContainers();
        Container tempParentContainer = DB.getTempParentContainer();
        assertEquals(0, tempParentContainer.getChildrenNames().size());
    }

    // Not testing that the rows returns ARE THE RIGHT ones, but we trust that. The count is enough
    public void testGetItems() {
        assertEquals(3, DB.getObjects("*").getRowCount());
        assertEquals(1, DB.getObjects("*").getNSTColumns().size());
        assertEquals(1, DB.getObjects("test-1 = 'c'").getRowCount());
        assertEquals(0, DB.getObjects("test-1 = 'c' and test-3 = 'm'").getRowCount());
        assertEquals(1, DB.getObjects("test-1 = 'c' or test-3 = 'x'").getRowCount());
        assertEquals(3, DB.getObjects("test-1 = 'c' or test-3 = 'x'").getNSTColumns().size());

        assertEquals(3, DB.getLinks("*").getRowCount());
        assertEquals(3, DB.getLinks("*").getNSTColumns().size());
        assertEquals(1, DB.getLinks("test-n = 'x'").getRowCount());
        assertEquals(4, DB.getLinks("test-n = 'x'").getNSTColumns().size());

        assertEquals(1, DB.getLinks("link_id = 1").getRowCount());
        assertEquals(3, DB.getLinks("link_id < 10").getRowCount());
        assertEquals(1, DB.getObjects("id > 2").getRowCount());
        assertEquals(1, DB.getLinks("o1_id >= 1").getRowCount());
    }


    public void testGetLinksAndAllAttrs() {
        NST linksWithAttrs = DB.getLinks("*", "*");
        assertEquals(3, linksWithAttrs.getRowCount());
        assertEquals(6, linksWithAttrs.getNSTColumnNames().size());
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("link_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o1_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o2_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-2"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-n"));
    }

    public void testGetLinksAndAttrs() {
        NST linksWithAttrs = DB.getLinks("*", "test-1, test-2");
        assertEquals(3, linksWithAttrs.getRowCount());
        assertEquals(5, linksWithAttrs.getNSTColumnNames().size());
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("link_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o1_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o2_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-2"));

        linksWithAttrs = DB.getLinks("test-1 = nil OR test-2 = nil", "test-1, test-2");
        assertEquals(1, linksWithAttrs.getRowCount());
    }

    public void testGetLinksAndAttrsAndFilter() {
        NST linksWithAttrs = DB.getLinks("test-1 = 'a'", "test-1, test-2");
        assertEquals(1, linksWithAttrs.getRowCount());
        assertEquals(5, linksWithAttrs.getNSTColumnNames().size());
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("link_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o1_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("o2_id"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(linksWithAttrs.getNSTColumnNames().contains("test-2"));
    }

    public void testGetObjectsConnectedTo() {
        NST connectedTo = DB.getObjectsConnectedTo(0);
        assertEquals(1, connectedTo.getNSTColumnNames().size());
        assertTrue(connectedTo.getNSTColumnNames().contains("id"));
        assertEquals(2, connectedTo.getRowCount());
        List list = connectedTo.selectRows().toOIDList(1);
        assertTrue(list.contains(new Integer(1)));
        assertTrue(list.contains(new Integer(2)));
    }

    public void testGetObjectsAndAllAttrs() {
        NST objectsWithAttrs = DB.getObjects("*", "*");
        assertEquals(3, objectsWithAttrs.getRowCount());
        assertEquals(4, objectsWithAttrs.getNSTColumnNames().size());
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("id"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-2"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-3"));
    }


    public void testGetObjectsAndAttrs() {
        NST objectsWithAttrs = DB.getObjects("*", "test-1, test-2");
        assertEquals(3, objectsWithAttrs.getRowCount());
        assertEquals(3, objectsWithAttrs.getNSTColumnNames().size());
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("id"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-2"));

        objectsWithAttrs = DB.getObjects("test-1 = nil OR test-2 = nil");
        assertEquals(1, objectsWithAttrs.getRowCount());
    }

    public void testGetObjectsAndAttrsAndFilter() {
        NST objectsWithAttrs = DB.getObjects("test-1 = 'a'", "test-1, test-2");
        assertEquals(1, objectsWithAttrs.getRowCount());
        assertEquals(3, objectsWithAttrs.getNSTColumnNames().size());
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("id"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(objectsWithAttrs.getNSTColumnNames().contains("test-2"));
    }


    /**
     * Test that insertLink correctly adds the next link ID
     */
    public void testInsertLink() {
        DB.getLinkNST().deleteRows();
        DB.insertLink(3, 1, 1);
        int newLinkID = DB.insertLink(2, 2);
        assertEquals(4, newLinkID);
    }


    /**
     * Test that insertObject correctly adds the next object ID
     */
    public void testInsertObject() {
        DB.getObjectNST().deleteRows();
        DB.insertObject(3);
        int newObjectID = DB.insertObject();
        assertEquals(4, newObjectID);
    }

    public void testIsLinkExists() {
        assertTrue(DB.isLinkExists(1));
        assertTrue(DB.isLinkExists(3));
        assertTrue(DB.isLinkExists(2));
        assertFalse(DB.isLinkExists(4));
    }

    public void testIsObjectExists() {
        assertTrue(DB.isLinkExists(1));
        assertTrue(DB.isLinkExists(3));
        assertTrue(DB.isLinkExists(2));
        assertFalse(DB.isLinkExists(4));
    }

    public void testLs() {
        assertTrue(DB.ls().size() > 0);
        DB.clearDB();
        assertEquals(0, DB.ls().size());
        NST nst = new NST("a,b", "int,int");
        assertEquals(0, DB.ls().size());
        nst.save("test_nst");
        assertEquals(1, DB.ls().size());
        nst.delete();
        assertEquals(0, DB.ls().size());
        DB.initEmptyDB(); // other tests assume that the main tables are there!
    }


}
