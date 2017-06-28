/**
 * $Id: QueryGraph2CompOpTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryGraph2CompOpTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.*;

public class QueryGraph2CompOpTest extends TestCase {

    private static final Logger log = Logger.getLogger(QueryGraph2CompOpTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getRootContainer().deleteAllChildren();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateLinks() {
        DB.getLinkNST().deleteRows();
        DB.insertLink(0, 1, 2);
        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 2);
        DB.insertLink(3, 1, 2);

        Attributes attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("link_type", "str");

        DB.getRootContainer().deleteAllChildren();
        Container sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow("4, 1, A");
        objectNST.insertRow("5, 1, B");
        objectNST.insertRow("6, 1, C");
        objectNST.insertRow("7, 2, A");
        objectNST.insertRow("8, 2, B");
        objectNST.insertRow("9, 2, C");

        // Create links between 4,5 and 7,8
        QGAddLink addLink = new QGAddLink("A", "B", "link_type", "x");
        List addLinkList = new ArrayList();
        addLinkList.add(addLink);
        new QueryGraph2CompOp().createLinks(sourceContainer, addLinkList);

        List oidList = DB.getLinkNST().selectRows("link_id").toOIDList(1);
        assertEquals(6, oidList.size());
        assertTrue(oidList.contains(new Integer(0)));
        assertTrue(oidList.contains(new Integer(1)));
        assertTrue(oidList.contains(new Integer(2)));
        assertTrue(oidList.contains(new Integer(3)));
        assertTrue(oidList.contains(new Integer(4)));
        assertTrue(oidList.contains(new Integer(5)));

        NST linksAndAttrs = DB.getLinks("link_type != nil");
        List attrList = linksAndAttrs.selectRows("link_type").toStringList(1);
        assertEquals(2, attrList.size());
        assertTrue(attrList.contains("x"));
    }

    public void testCreateLinksNoDups() {
        DB.getLinkNST().deleteRows();
        DB.insertLink(0, 1, 2);
        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 2);
        DB.insertLink(3, 1, 2);

        Attributes attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("link_type", "str");

        DB.getRootContainer().deleteAllChildren();
        Container sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow("4, 1, A");
        objectNST.insertRow("5, 1, B");
        objectNST.insertRow("6, 1, C");
        objectNST.insertRow("4, 2, A");   // ---> They are the same; should create a single link
        objectNST.insertRow("5, 2, B");
        objectNST.insertRow("9, 2, C");

        // Create links between 4,5 and 7,8
        QGAddLink addLink = new QGAddLink("A", "B", "link_type", "x");
        List addLinkList = new ArrayList();
        addLinkList.add(addLink);
        new QueryGraph2CompOp().createLinks(sourceContainer, addLinkList);

        List oidList = DB.getLinkNST().selectRows("link_id").toOIDList(1);
        assertEquals(5, oidList.size());
        assertTrue(oidList.contains(new Integer(0)));
        assertTrue(oidList.contains(new Integer(1)));
        assertTrue(oidList.contains(new Integer(2)));
        assertTrue(oidList.contains(new Integer(3)));
        assertTrue(oidList.contains(new Integer(4)));

        NST linksAndAttrs = DB.getLinks("link_type != nil");
        List attrList = linksAndAttrs.selectRows("link_type").toStringList(1);
        assertEquals(1, attrList.size());
        assertTrue(attrList.contains("x"));
    }

    public void testFindCachedEdges() {
        createTestContainer("testcont");
        ArrayList pathElements = new ArrayList();
        pathElements.add("A");
        pathElements.add("B");
        pathElements.add("D");
        pathElements.add("A.X.B");
        pathElements.add("A.X.B.Y.D");

        Map cachedSources = new HashMap();
        cachedSources.put("A", "testcont");
        cachedSources.put("B", "testcont");
        cachedSources.put("X", "testcont");

        TempTableMgr tempTableMgr = new TempTableMgr();
        new QueryGraph2CompOp().fetchCachedEdges(pathElements, cachedSources, tempTableMgr);

        Set vertexNames = tempTableMgr.getVertexNames();
        assertEquals(3, vertexNames.size());
        assertTrue(vertexNames.contains("A"));
        assertTrue(vertexNames.contains("B"));
        assertTrue(vertexNames.contains("A.X.B"));
        assertEquals(3, tempTableMgr.getNSTForVertex("A", true).getRowCount());
        assertEquals(0, tempTableMgr.getNSTForVertex("A", false).getRowCount());
        assertEquals(6, tempTableMgr.getNSTForVertex("A.X.B", true).getRowCount());
        assertEquals(3, tempTableMgr.getNSTForVertex("A.X.B", false).getRowCount());
    }

    public void testFindCachedEdgesDiffContainers() {
        createTestContainer("testcont");
        createTestContainer("testcont2");
        ArrayList pathElements = new ArrayList();
        pathElements.add("A");
        pathElements.add("B");
        pathElements.add("D");
        pathElements.add("A.X.B");
        pathElements.add("A.X.B.Y.D");

        Map cachedSources = new HashMap();
        cachedSources.put("A", "testcont");
        cachedSources.put("B", "testcont2");
        cachedSources.put("X", "testcont");

        TempTableMgr tempTableMgr = new TempTableMgr();
        new QueryGraph2CompOp().fetchCachedEdges(pathElements, cachedSources, tempTableMgr);

        // A.X.B should not be cached, since they are in different containers
        Set vertexNames = tempTableMgr.getVertexNames();
        assertEquals(2, vertexNames.size());
        assertTrue(vertexNames.contains("A"));
        assertTrue(vertexNames.contains("B"));
    }

    private void createTestContainer(String contName) {
        Container child = DB.getRootContainer().createChild(contName);
        child.getObjectsNST().insertRow("1, 1, A");
        child.getObjectsNST().insertRow("1, 2, A");
        child.getObjectsNST().insertRow("1, 3, A");
        child.getObjectsNST().insertRow("2, 1, B");
        child.getObjectsNST().insertRow("2, 2, B");
        child.getObjectsNST().insertRow("2, 3, B");
        child.getObjectsNST().insertRow("3, 1, M");
        child.getObjectsNST().insertRow("3, 2, M");
        child.getObjectsNST().insertRow("3, 3, M");

        child.getLinksNST().insertRow("1, 1, X");
        child.getLinksNST().insertRow("1, 2, X");
        child.getLinksNST().insertRow("1, 3, X");
        child.getLinksNST().insertRow("2, 1, W");
        child.getLinksNST().insertRow("2, 2, W");
        child.getObjectsNST().insertRow("2, 3, W");
    }

}
