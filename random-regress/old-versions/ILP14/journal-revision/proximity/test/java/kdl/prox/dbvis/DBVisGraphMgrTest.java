/**
 * $Id: DBVisGraphMgrTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserDataContainer;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Util;
import kdl.prox.db.DB;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.*;


public class DBVisGraphMgrTest extends TestCase {

    private static final Logger log = Logger.getLogger(DBVisGraphMgrTest.class);

    // smart ascii file info: file name, dict name, and required attributes
    private static final String SMART_ASCII_FILE = "test-db.txt";
    private static final String SMART_ASCII_DICT_NAME = "names";    // for smart ASCII name dictionary
    private static final String OBJ_LABEL_ATTR_NAME = "obj-lab";
    private static final String OBJ_OS_ATTR_NAME = "os";
    private static final String LINK_LABEL_ATTR_NAME = "link-lab";
    private static final String[][] SMART_ASCII_ATTR_DEFS =
            new String[][]{{"O", "str", SMART_ASCII_DICT_NAME},
                    {"O", "str", "os"},
                    {"O", "str", OBJ_LABEL_ATTR_NAME},
                    {"L", "str", "ls"},
                    {"L", "str", LINK_LABEL_ATTR_NAME},
            };
    private static final Integer OBJ_A_OID = new Integer(0);
    private static final Integer OBJ_B_OID = new Integer(1);
    private static final Integer OBJ_C_OID = new Integer(2);
    private static final Integer OBJ_H_OID = new Integer(7);
    private static final Integer EDGE_A_TO_B_OID = new Integer(0);
    private static final Integer EDGE_C_TO_A_OID = new Integer(1);
    private static final Integer EDGE_A_TO_D_OID = new Integer(2);

    protected void setUp() throws Exception {
        super.setUp();

        // loadDB takes care of cleaning up the DB, so don't call it twice here
        Util.initProxApp();

        URL smartAsciiFileURL = getClass().getResource(SMART_ASCII_FILE);
        SmartAsciiTestHelper.loadDB(smartAsciiFileURL,
                SMART_ASCII_ATTR_DEFS, SMART_ASCII_DICT_NAME);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    /**
     * @param dbVisGraphMgr
     * @param vertsOrEdges
     * @return Set of Integers, one for each Vertex or Edge in vertsOrEdges
     */
    private Set getOIDInts(DBVisGraphMgr dbVisGraphMgr, Set vertsOrEdges) {
        Set vertsOrEdgesInts = new HashSet();
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            Integer oidInt = vertOrEdge.getOID();
            vertsOrEdgesInts.add(oidInt);
        }
        return vertsOrEdgesInts;
    }

    /**
     * @param vertsOrEdges
     * @return Vertices or Edges in vertsOrEdges that are special pseudo (and
     *         possibly pager) ones
     */
    private Set getPseudoVertsOrEdges(Set vertsOrEdges) {
        Set pseudoVertsOrEdges = new HashSet();
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            UserDataContainer vertOrEdge = (UserDataContainer) vertOrEdgeIter.next();
            ProxItemData proxItemData = ((ProxItemData) vertOrEdge);
            if (proxItemData.isPseudo()) {
                pseudoVertsOrEdges.add(vertOrEdge);
            }
        }
        return pseudoVertsOrEdges;
    }

    public void testAddBadOID() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        try {
            dbVisGraphMgr.addVertexForObjID(new Integer(999999));
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException iaExc) {
            // expected - ignore
        }
    }

    public void testBadLabelAttrs() {
        String multiColAttrName = "multicol";
        DB.getObjectAttrs().defineAttributeOrClearValuesIfExists(multiColAttrName,
                "val1: str, val2: int");

        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        try {
            dbVisGraphMgr.setItemLabelAttribute("bad-attr", true);
            fail("didn't catch undefined attribute");
        } catch (IllegalArgumentException iaExc) {
            // expected - ignore
        }

        try {
            dbVisGraphMgr.setItemLabelAttribute(multiColAttrName, true);
            fail("didn't catch multi-column attribute");
        } catch (IllegalArgumentException iaExc) {
            // expected - ignore
        }
    }

    public void testClear() {
        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();    // empty graph & history
        Graph graph = dbVisGraphMgr.getGraph();
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertex, 99);
        assertEquals(6, graph.getVertices().size());
        assertEquals(7, graph.getEdges().size());

        dbVisGraphMgr.clear();

        // check graph and history
        assertEquals(0, graph.getVertices().size());
        assertEquals(0, graph.getEdges().size());

        List history = dbVisGraphMgr.getHistory();
        assertEquals(0, history.size());
    }

    public void testExpandNoVertPageNoEdgePageMultiStart() {
        // expand a to get into initial start state
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();      // empty graph & history
        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);

        // expand b
        ProxItemData vertexB = dbVisGraphMgr.getVertOrEdgeForOID(OBJ_B_OID, true);
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex((ProxSparseVertex) vertexB, 99);

        // check graph's vertices and edges
        Graph graph = dbVisGraphMgr.getGraph();
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3, 4, 5, 6},
                graph.getVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8},
                graph.getEdges());

        // check graphEdit
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{6},
                graphEdit.getAddedVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{7, 8},
                graphEdit.getAddedEdges());

        // check history
        List history = dbVisGraphMgr.getHistory();
        assertEquals(2, history.size());
        assertTrue(history.get(0) == vertexA);
        assertTrue(history.get(1) == vertexB);
    }

    public void testExpandNoVertPageNoEdgePageSingleStart() {
        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();      // empty graph & history
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex(vertex, 99);

        // check graph's vertices and edges
        Graph graph = dbVisGraphMgr.getGraph();
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3, 4, 5},
                graph.getVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3, 4, 5, 6},
                graph.getEdges());

        // check graphEdit
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{1, 2, 3, 4, 5},
                graphEdit.getAddedVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3, 4, 5, 6},
                graphEdit.getAddedEdges());

        // check history
        List history = dbVisGraphMgr.getHistory();
        assertEquals(1, history.size());
        assertTrue(history.get(0) == vertex);
    }

    public void testExpandOrphanVertex() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();    // empty graph & history
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_H_OID);
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex(vertex, 3);    // NB: maxDegreePerVert
        assertEquals(0, graphEdit.getAddedVertices().size());
        assertEquals(0, graphEdit.getAddedEdges().size());
    }

    /**
     * Expand a vertex pager such that some unshown vertices are added, but not
     * all. Verify pager page size and % are correct after.
     */
    public void testExpandPagerVertPartial() {
        int maxDegreePerVert = 2;

        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertex, maxDegreePerVert);

        // should have added b & c, and their links (2 total), plus the pager.
        // here we just check the pager's page size and %, then move on
        Graph graph = dbVisGraphMgr.getGraph();
        Set vertices = graph.getVertices();
        Set pseudoVerts = getPseudoVertsOrEdges(vertices);
        assertEquals(1, pseudoVerts.size());

        ProxItemData pseudoVert1 = (ProxItemData) pseudoVerts.iterator().next();
        assertEquals(2, pseudoVert1.getPagerNumShown());
        assertEquals(5, pseudoVert1.getPagerNumTotal());
        assertEquals(maxDegreePerVert, pseudoVert1.getPageSize());

        // expand the pager and check results
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex((ProxSparseVertex) pseudoVert1, -1); // 2nd arg ignored for pagers

        // check: still have same pager
        vertices = graph.getVertices();
        pseudoVerts = getPseudoVertsOrEdges(vertices);
        ProxItemData pseudoVert1b = (ProxItemData) pseudoVerts.iterator().next();
        assertEquals(1, pseudoVerts.size());
        assertEquals(pseudoVert1, pseudoVert1b);

        // check: pager's % updated - 1/5
        assertEquals(4, pseudoVert1.getPagerNumShown());
        assertEquals(5, pseudoVert1.getPagerNumTotal());
        assertEquals(maxDegreePerVert, pseudoVert1.getPageSize());

        // check: d, e, and their links added to graphEdit
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{3, 4},
                graphEdit.getAddedVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{2, 3},
                graphEdit.getAddedEdges());

        // check: history unchanged
        List history = dbVisGraphMgr.getHistory();
        assertEquals(1, history.size());
        assertTrue(history.get(0) == vertex);
    }

    /**
     * Expand a vertex pager such that all unshown vertices are added. Verify
     * pager and its edge are removed after.
     */
    public void testExpandPagerVertFull() {
        int maxDegreePerVert = 3;

        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertex, maxDegreePerVert);

        // should have added b, c, & d, and their links (3 total), plus the pager.
        // here we just check the pager's page size and %, then move on
        Graph graph = dbVisGraphMgr.getGraph();
        Set vertices = graph.getVertices();
        Set pseudoVerts = getPseudoVertsOrEdges(vertices);
        assertEquals(1, pseudoVerts.size());

        ProxItemData pseudoVert1 = (ProxItemData) pseudoVerts.iterator().next();
        assertEquals(3, pseudoVert1.getPagerNumShown());
        assertEquals(5, pseudoVert1.getPagerNumTotal());
        assertEquals(maxDegreePerVert, pseudoVert1.getPageSize());

        // expand the pager and check results
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex((ProxSparseVertex) pseudoVert1, -1); // 2nd arg ignored for pagers

        // check: pager removed from graph
        vertices = graph.getVertices();
        pseudoVerts = getPseudoVertsOrEdges(vertices);
        assertEquals(0, pseudoVerts.size());
        assertNull(((Vertex) pseudoVert1).getGraph());

        // check: e, f, and their links added to graphEdit
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{4, 5},
                graphEdit.getAddedVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{3, 4, 5, 6},
                graphEdit.getAddedEdges());

        // check: history unchanged
        List history = dbVisGraphMgr.getHistory();
        assertEquals(1, history.size());
        assertTrue(history.get(0) == vertex);
    }

    public void testExpandYesVertPageNoEdgePageSingleStart() {
        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();    // empty graph & history
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        GraphEdit graphEdit = dbVisGraphMgr.expandVertex(vertex, 3);    // NB: maxDegreePerVert

        // check graph's vertices and edges
        Graph graph = dbVisGraphMgr.getGraph();
        Set vertices = graph.getVertices();
        Set pseudoVerts = getPseudoVertsOrEdges(vertices);
        assertEquals(1, pseudoVerts.size());

        ProxItemData pseudoVert1 = (ProxItemData) pseudoVerts.iterator().next();
        assertEquals(3, pseudoVert1.getPagerNumShown());
        assertEquals(5, pseudoVert1.getPagerNumTotal());

        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2, 3}, // a-d, no e f
                vertices);  // NB: ignores pseudo vertices
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2},
                graph.getEdges());

        // check graphEdit
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{1, 2, 3}, // b-d, no e f
                graphEdit.getAddedVertices());
        verifyGraphVertsOrEdges(dbVisGraphMgr, new int[]{0, 1, 2},
                graphEdit.getAddedEdges());

        // check history
        List history = dbVisGraphMgr.getHistory();
        assertEquals(1, history.size());
        assertTrue(history.get(0) == vertex);
    }

    public void testLabelAttrsForLinks() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        dbVisGraphMgr.setItemLabelAttribute(LINK_LABEL_ATTR_NAME, false);

        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);

        ProxItemData edgeAToB = dbVisGraphMgr.getVertOrEdgeForOID(EDGE_A_TO_B_OID, false);
        ProxItemData edgeCToA = dbVisGraphMgr.getVertOrEdgeForOID(EDGE_C_TO_A_OID, false);
        ProxItemData edgeAToD = dbVisGraphMgr.getVertOrEdgeForOID(EDGE_A_TO_D_OID, false);

        String label = edgeAToB.getLabel();
        assertEquals("a->b-label", label);

        label = edgeCToA.getLabel();
        assertEquals("c->a-label-2", label);

        label = edgeAToD.getLabel();
        assertEquals(null, label);
    }

    public void testLabelAttrsForObjects() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        dbVisGraphMgr.setItemLabelAttribute(OBJ_LABEL_ATTR_NAME, true);

        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        String label = vertexA.getLabel();
        assertEquals(null, label);

        dbVisGraphMgr.expandVertex(vertexA, 99);

        ProxItemData vertexB = dbVisGraphMgr.getVertOrEdgeForOID(OBJ_B_OID, true);
        ProxItemData vertexC = dbVisGraphMgr.getVertOrEdgeForOID(OBJ_C_OID, true);

        label = vertexA.getLabel();
        assertEquals("a-label", label);

        label = vertexB.getLabel();
        assertEquals("b-label-2", label);

        label = vertexC.getLabel();
        assertEquals(null, label);
    }

    public void testLabelAttrsForObjectsClear() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        String label;

        // first set it to a non-null one
        dbVisGraphMgr.setItemLabelAttribute(OBJ_LABEL_ATTR_NAME, true);

        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);
        label = vertexA.getLabel();
        assertEquals("a-label", label);

        // clear it and verify null
        dbVisGraphMgr.clear();
        dbVisGraphMgr.setItemLabelAttribute(null, true);
        vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        label = vertexA.getLabel();
        assertEquals(null, label);
    }

    public void testLabelAttrsForObjectsRefresh() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();

        // expand using OBJ_LABEL_ATTR_NAME for object labels, and check
        dbVisGraphMgr.setItemLabelAttribute(OBJ_LABEL_ATTR_NAME, true);

        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);

        ProxItemData vertexB = dbVisGraphMgr.getVertOrEdgeForOID(OBJ_B_OID, true);
        ProxItemData vertexC = dbVisGraphMgr.getVertOrEdgeForOID(OBJ_C_OID, true);
        assertEquals("a-label", vertexA.getLabel());
        assertEquals("b-label-2", vertexB.getLabel());
        assertEquals(null, vertexC.getLabel());

        // refresh using new OBJ_OS_ATTR_NAME for object labels, and check
        dbVisGraphMgr.setItemLabelAttribute(OBJ_OS_ATTR_NAME, true);
        dbVisGraphMgr.refreshItemAttributes(true, true);   // isObject, isLabel
        assertEquals("a", vertexA.getLabel());
        assertEquals("b", vertexB.getLabel());
        assertEquals("c", vertexC.getLabel());
    }

    public void testLabelAttrsForObjectsRefreshNullAttr() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();

        // expand using default (no) attr, then check refresh doesn't NPE
        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);
        try {
            dbVisGraphMgr.refreshItemAttributes(true, true);   // isObject, isLabel
        } catch (NullPointerException npe) {
            fail("threw NPE");
        }
    }

    public void testLabelAttrsForObjectsReset() {
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();
        String label;

        // first set it to a non-null one
        dbVisGraphMgr.setItemLabelAttribute(OBJ_LABEL_ATTR_NAME, true);

        ProxSparseVertex vertexA = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertexA, 99);
        label = vertexA.getLabel();
        assertEquals("a-label", label);

        // verify null
        dbVisGraphMgr.setItemLabelAttribute(null, true);
        label = vertexA.getLabel();
        assertEquals(null, label);

        // re-set it and verify still null
        dbVisGraphMgr.setItemLabelAttribute(OBJ_LABEL_ATTR_NAME, true);
        label = vertexA.getLabel();
        assertEquals(null, label);
    }

    public void testReExpandSameVertex() {
        // expand a
        DBVisGraphMgr dbVisGraphMgr = new DBVisGraphMgr();      // empty graph & history
        ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(OBJ_A_OID);
        dbVisGraphMgr.expandVertex(vertex, 3);                  // NB: maxDegreePerVert

        GraphEdit graphEdit = dbVisGraphMgr.expandVertex(vertex, 3);    // NB: maxDegreePerVert
        assertEquals(0, graphEdit.getAddedVertices().size());
        assertEquals(0, graphEdit.getAddedEdges().size());
    }

    /**
     * Asserts that the expected OIDs in expObjOrLinkOIDs match the actual ones
     * in actVertsOrEdges. NB: Ignores pseudo vertices.
     *
     * @param expObjOrLinkOIDs expected object or link OIDs
     * @param actVertsOrEdges  actual Vertex or Edge instances whose number and
     *                         value OIDs must match expObjOrLinkOIDs
     */
    private void verifyGraphVertsOrEdges(DBVisGraphMgr dbVisGraphMgr,
                                         int[] expObjOrLinkOIDs,
                                         Set actVertsOrEdges) {
        Set pseudoVertsOrEdges = getPseudoVertsOrEdges(actVertsOrEdges);
        actVertsOrEdges = new HashSet(actVertsOrEdges);     // o/w get java.lang.UnsupportedOperationException
        actVertsOrEdges.removeAll(pseudoVertsOrEdges);

        Set vertOIDInts = getOIDInts(dbVisGraphMgr, actVertsOrEdges);
        assertEquals(expObjOrLinkOIDs.length, vertOIDInts.size());

        // create Integer Set corresponding to expObjOrLinkOIDs
        Collection expObjOrLinkOIDsInts = new ArrayList();
        for (int expObjOrLinkOIDIdx = 0; expObjOrLinkOIDIdx < expObjOrLinkOIDs.length; expObjOrLinkOIDIdx++) {
            int expObjOrLinkOID = expObjOrLinkOIDs[expObjOrLinkOIDIdx];
            expObjOrLinkOIDsInts.add(new Integer(expObjOrLinkOID));
        }
        assertTrue(vertOIDInts.containsAll(expObjOrLinkOIDsInts));
    }

}
