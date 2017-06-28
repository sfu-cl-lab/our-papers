/**
 * $Id: SubgraphTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: SubgraphTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.List;


/**
 * Tests the Container class.
 *
 * @see Container
 */
public class SubgraphTest extends TestCase {

    private Container rootContainer;

    private static Logger log = Logger.getLogger(SubgraphTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        // delete all children containers
        rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testInsert() {
        // create some links
        DB.insertLink(1, 3, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 4);
        DB.insertLink(4, 4, 4);
        // create a child container
        String realChildName = "root-child-0";
        Container childContainer = rootContainer.createChild(realChildName);
        int subgAOID = 0;
        int subgBOID = 1;
        Subgraph subgraphA = childContainer.getSubgraph(subgAOID);
        Subgraph subgraphB = childContainer.getSubgraph(subgBOID);
        subgraphA.insertObject(1, "a");
        subgraphB.insertObject(2, "b");
        subgraphA.insertLink(1, "a");
        subgraphA.insertLink(2, "b");
        subgraphB.insertLink(3, "c");
        subgraphB.insertLink(4, "d");
        List objsA = subgraphA.getObjects();
        List objsB = subgraphB.getObjects();
        List linksA = subgraphA.getLinks();
        List linksB = subgraphB.getLinks();
        NST aSubgObjectNST = subgraphA.getSubgObjectNST();
        NST aSubgLinkNST = subgraphA.getSubgLinkNST();
        NST bSubgObjectNST = subgraphB.getSubgObjectNST();
        NST bSubgLinkNST = subgraphB.getSubgLinkNST();
        assertEquals(1, aSubgObjectNST.getRowCount());
        assertEquals(1, bSubgObjectNST.getRowCount());
        assertEquals(2, aSubgLinkNST.getRowCount());
        assertEquals(2, bSubgLinkNST.getRowCount());
        assertEquals(1, objsA.size());
        assertEquals(1, objsB.size());
        assertEquals(2, linksA.size());
        assertEquals(2, linksB.size());
        assertTrue(objsA.contains(new ProxObj(1, "a")));
        assertTrue(objsB.contains(new ProxObj(2, "b")));
        assertTrue(linksA.contains(new ProxLink(1, "a", 3, 2)));
        assertTrue(linksA.contains(new ProxLink(2, "b", 1, 3)));
        assertTrue(linksB.contains(new ProxLink(3, "c", 2, 4)));
        assertTrue(linksB.contains(new ProxLink(4, "d", 4, 4)));
        aSubgObjectNST.release();
        bSubgObjectNST.release();
        aSubgLinkNST.release();
        bSubgLinkNST.release();
    }


    public void testRemoveObject() {
        DB.insertLink(1, 3, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 4);
        DB.insertLink(4, 4, 4);
        // first insert something into the container
        String realChildName = "root-child-0";
        Container childContainer = rootContainer.createChild(realChildName);
        int subgAOID = 0;
        int subgBOID = 1;
        Subgraph subgraphA = childContainer.getSubgraph(subgAOID);
        Subgraph subgraphB = childContainer.getSubgraph(subgBOID);
        subgraphA.insertObject(1, "a");
        subgraphA.insertObject(2, "b");
        subgraphB.insertObject(2, "b");
        subgraphA.insertLink(1, "a");
        subgraphA.insertLink(2, "b");
        subgraphB.insertLink(3, "c");
        subgraphB.insertLink(4, "d");

        // now remove some of the objects
        subgraphA.removeObject(2);

        List objsA = subgraphA.getObjects();
        List objsB = subgraphB.getObjects();
        List linksA = subgraphA.getLinks();
        List linksB = subgraphB.getLinks();

        assertEquals(1, objsA.size());
        assertEquals(1, objsB.size());
        assertEquals(2, linksA.size());
        assertEquals(2, linksB.size());
        assertTrue(objsA.contains(new ProxObj(1, "a")));
        assertTrue(objsB.contains(new ProxObj(2, "b")));
        assertTrue(linksA.contains(new ProxLink(1, "a", 3, 2)));
        assertTrue(linksA.contains(new ProxLink(2, "b", 1, 3)));
        assertTrue(linksB.contains(new ProxLink(3, "c", 2, 4)));
        assertTrue(linksB.contains(new ProxLink(4, "d", 4, 4)));
    }

    public void testRemoveObjectWithName() {
        DB.insertLink(1, 3, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 4);
        DB.insertLink(4, 4, 4);
        // first insert something into the container
        String realChildName = "root-child-0";
        Container childContainer = rootContainer.createChild(realChildName);
        int subgAOID = 0;
        int subgBOID = 1;
        Subgraph subgraphA = childContainer.getSubgraph(subgAOID);
        subgraphA.insertObject(1, "a");
        subgraphA.insertObject(2, "b");
        subgraphA.insertObject(2, "c");

        // now remove some of the objects
        subgraphA.removeObject(2, "c");

        List objsA = subgraphA.getObjects();

        NST subgObjectNST = subgraphA.getSubgObjectNST();
        assertEquals(2, subgObjectNST.getRowCount());
        subgObjectNST.release();
        assertEquals(2, objsA.size());
        assertTrue(objsA.contains(new ProxObj(1, "a")));
        assertTrue(objsA.contains(new ProxObj(2, "b")));

    }

    public void testRemoveLink() {
        DB.insertLink(1, 3, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 4);
        DB.insertLink(4, 4, 4);
        // first insert something into the container
        String realChildName = "root-child-0";
        Container childContainer = rootContainer.createChild(realChildName);
        int subgAOID = 0;
        int subgBOID = 1;
        Subgraph subgraphA = childContainer.getSubgraph(subgAOID);
        Subgraph subgraphB = childContainer.getSubgraph(subgBOID);
        subgraphA.insertObject(1, "a");
        subgraphA.insertObject(2, "b");
        subgraphB.insertObject(2, "b");
        subgraphA.insertLink(1, "a");
        subgraphA.insertLink(2, "b");
        subgraphB.insertLink(2, "b");
        subgraphB.insertLink(3, "c");
        subgraphB.insertLink(4, "d");

        // now remove one of the links
        subgraphA.removeLink(2);

        List objsA = subgraphA.getObjects();
        List objsB = subgraphB.getObjects();
        List linksA = subgraphA.getLinks();
        List linksB = subgraphB.getLinks();

        NST aSubgObjectNST = subgraphA.getSubgObjectNST();
        NST aSubgLinkNST = subgraphA.getSubgLinkNST();
        NST bSubgObjectNST = subgraphB.getSubgObjectNST();
        NST bSubgLinkNST = subgraphB.getSubgLinkNST();
        assertEquals(2, aSubgObjectNST.getRowCount());
        assertEquals(1, bSubgObjectNST.getRowCount());
        assertEquals(1, aSubgLinkNST.getRowCount());
        assertEquals(3, bSubgLinkNST.getRowCount());
        assertEquals(2, objsA.size());
        assertEquals(1, objsB.size());
        assertEquals(1, linksA.size());
        assertEquals(3, linksB.size());
        assertTrue(objsA.contains(new ProxObj(1, "a")));
        assertTrue(objsB.contains(new ProxObj(2, "b")));
        assertTrue(linksA.contains(new ProxLink(1, "a", 3, 2)));
        assertTrue(linksB.contains(new ProxLink(2, "b", 1, 3)));
        assertTrue(linksB.contains(new ProxLink(3, "c", 2, 4)));
        assertTrue(linksB.contains(new ProxLink(4, "d", 4, 4)));
        aSubgObjectNST.release();
        bSubgObjectNST.release();
        aSubgLinkNST.release();
        bSubgLinkNST.release();
    }


}
