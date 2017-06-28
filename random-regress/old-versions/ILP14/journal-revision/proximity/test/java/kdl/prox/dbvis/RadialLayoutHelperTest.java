/**
 * $Id: RadialLayoutHelperTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.visualization.VertexLocationFunction;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.gui2.GUIContentGenerator;
import kdl.prox.gui2.ProxURL;
import kdl.prox.gui2.SubgraphJFrame;
import kdl.prox.util.PopulateDB;
import org.apache.log4j.Logger;


/**
 * Tests RadialLayoutHelper, independent of any JUNG layout it might be used in.
 * In other words, tests the calculation of rho and theta for each vertex.
 */
public class RadialLayoutHelperTest extends TestCase {

    private static final Logger log = Logger.getLogger(RadialLayoutHelperTest.class);
    private static final double DELTA = 0.01;
    private static final String TEST_CONTAINER = "TEST_CONTAINER";
    private static final String SUBG_0_URL = "subg:/containers/" + TEST_CONTAINER + "/" + "0";
    private static final String LABEL_KEY = "LABEL_KEY";    // Vertex label (String)
    private static final Comparator VERT_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            Vertex v1 = ((Vertex) o1);
            Vertex v2 = ((Vertex) o2);
            String v1Datum = (String) v1.getUserDatum(LABEL_KEY);
            String v2Datum = (String) v2.getUserDatum(LABEL_KEY);
            return v1Datum.compareTo(v2Datum);
        }
    };


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();   // for log4j
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void checkRhoAndTheta(Graph graph, Map vertexToPolarCoordMap,
                                  String label, int expRadius, double expAngle) {
        Vertex vertex = getVertexForLabel(graph, label);
        PolarCoordinate polarCoord = RadialLayoutHelper.getPolarCoordinate(vertex,
                vertexToPolarCoordMap);
        assertNotNull(polarCoord);
//        log.warn(label + ": " + (expAngle / Math.PI) + " PI, " +
//                (actTheta.doubleValue() / Math.PI) + " PI; " + expRadius + ", " +
//                actRho);
        assertEquals(expRadius, polarCoord.getRho());
        assertEquals(expAngle, polarCoord.getTheta(), DELTA);
    }

    private void checkTreeRadiusAndAngle(Graph graph) {
        Vertex startVertex = getVertexForLabel(graph, "2");
        Map vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex, VERT_COMPARATOR);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "2", 0, 1.0 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "6", 1, 1.3333333333333333 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "7", 2, 1.5 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "8", 3, 1.5 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "3", 2, 1.1666666666666665 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "4", 3, 1.1666666666666665 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "1", 1, 0.3333333333333333 * Math.PI);
        checkRhoAndTheta(graph, vertexToPolarCoordMap, "5", 2, 0.3333333333333333 * Math.PI);
    }

    private void checkVertexCoords(Graph graph, VertexLocationFunction vertLocFcn,
                                   String label, double expX, double expY) {
        Vertex vertex = getVertexForLabel(graph, label);
        Point2D actLocation = vertLocFcn.getLocation(vertex);
        assertEquals(expX, actLocation.getX(), DELTA);
        assertEquals(expY, actLocation.getY(), DELTA);
    }

    /**
     * @param graph
     * @param label
     * @return first Vertex in graph whose LABEL_KEY equals() label. returns null if none found
     */
    private Vertex getVertexForLabel(Graph graph, String label) {
        Set vertices = graph.getVertices();
        for (Iterator vertIter = vertices.iterator(); vertIter.hasNext();) {
            Vertex vertex = (Vertex) vertIter.next();
            String existLabel = (String) vertex.getUserDatum(LABEL_KEY);
            if (existLabel != null) {
                if (existLabel.equals(label)) {
                    return vertex;
                }
            }
        }
        return null;
    }

    /**
     * <pre>
     *     2
     *    /|\
     *   1 | 6
     *  /  |/ \
     * 5   3   7
     *  \ /     \
     *   4       8
     * </pre>
     *
     * @return the above Graph
     */
    private Graph makeGraph() {
        Graph graph = new SparseGraph();

        Vertex v1 = makeVertex("1");
        Vertex v2 = makeVertex("2");
        Vertex v3 = makeVertex("3");
        Vertex v4 = makeVertex("4");
        Vertex v5 = makeVertex("5");
        Vertex v6 = makeVertex("6");
        Vertex v7 = makeVertex("7");
        Vertex v8 = makeVertex("8");

        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        graph.addVertex(v4);
        graph.addVertex(v5);
        graph.addVertex(v6);
        graph.addVertex(v7);
        graph.addVertex(v8);

        graph.addEdge(new DirectedSparseEdge(v1, v2));
        graph.addEdge(new DirectedSparseEdge(v2, v3));
        graph.addEdge(new DirectedSparseEdge(v2, v6));
        graph.addEdge(new DirectedSparseEdge(v1, v5));
        graph.addEdge(new DirectedSparseEdge(v4, v5));
        graph.addEdge(new DirectedSparseEdge(v3, v4));
        graph.addEdge(new DirectedSparseEdge(v6, v3));
        graph.addEdge(new DirectedSparseEdge(v7, v6));
        graph.addEdge(new DirectedSparseEdge(v8, v7));

        return graph;
    }

    /**
     * Returns a Graph containing the following tree. NB: all edges are directed
     * downward from the root.
     * <p/>
     * <pre>
     *     2
     *    / \
     *   1   6
     *  /   / \
     * 5   3   7
     *    /     \
     *   4       8
     * </pre>
     *
     * @return a tree based on CLR Figure 23.2 (see diagram above)
     */
    private Graph makeTreeDownwardDirected() {
        Graph graph = new SparseGraph();

        Vertex v1 = makeVertex("1");
        Vertex v2 = makeVertex("2");
        Vertex v3 = makeVertex("3");
        Vertex v4 = makeVertex("4");
        Vertex v5 = makeVertex("5");
        Vertex v6 = makeVertex("6");
        Vertex v7 = makeVertex("7");
        Vertex v8 = makeVertex("8");

        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        graph.addVertex(v4);
        graph.addVertex(v5);
        graph.addVertex(v6);
        graph.addVertex(v7);
        graph.addVertex(v8);

        graph.addEdge(new DirectedSparseEdge(v2, v1));
        graph.addEdge(new DirectedSparseEdge(v2, v6));
        graph.addEdge(new DirectedSparseEdge(v1, v5));
        graph.addEdge(new DirectedSparseEdge(v6, v3));
        graph.addEdge(new DirectedSparseEdge(v6, v7));
        graph.addEdge(new DirectedSparseEdge(v3, v4));
        graph.addEdge(new DirectedSparseEdge(v7, v8));

        return graph;
    }

    /**
     * @return same tree as in makeTreeDownwardDirected(), but with some edges
     *         directed upward
     */
    private Graph makeTreeMixedDirections() {
        Graph graph = new SparseGraph();

        Vertex v1 = makeVertex("1");
        Vertex v2 = makeVertex("2");
        Vertex v3 = makeVertex("3");
        Vertex v4 = makeVertex("4");
        Vertex v5 = makeVertex("5");
        Vertex v6 = makeVertex("6");
        Vertex v7 = makeVertex("7");
        Vertex v8 = makeVertex("8");

        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        graph.addVertex(v4);
        graph.addVertex(v5);
        graph.addVertex(v6);
        graph.addVertex(v7);
        graph.addVertex(v8);

        graph.addEdge(new DirectedSparseEdge(v1, v2));
        graph.addEdge(new DirectedSparseEdge(v2, v6));
        graph.addEdge(new DirectedSparseEdge(v1, v5));
        graph.addEdge(new DirectedSparseEdge(v6, v3));
        graph.addEdge(new DirectedSparseEdge(v7, v6));
        graph.addEdge(new DirectedSparseEdge(v3, v4));
        graph.addEdge(new DirectedSparseEdge(v8, v7));

        return graph;
    }

    private Vertex makeVertex(String label) {
        SparseVertex vertex = new SparseVertex() {
            public String toString() {
                String label = (String) getUserDatum(LABEL_KEY);
                return label;
            }
        };
        vertex.addUserDatum(LABEL_KEY, label, UserData.SHARED);
        return vertex;
    }

    public void testDisconnectedSubgraphs() {
        TestUtil.openTestConnection();
        DB.getRootContainer().deleteAllChildren();
        PopulateDB.populateDB(getClass(), "disconnected-subgraphs.txt");

        Graph graph = new SparseGraph();
        ProxURL subgURL = new ProxURL(SUBG_0_URL);
        GUIContentGenerator.getGraphForSubgURL(subgURL, graph); // fills graph
        ProxSparseVertex centerVertex = (ProxSparseVertex) SubgraphJFrame.findCenterVertex(graph, subgURL);
        try {
            RadialLayoutHelper.computePolarCoordinates(centerVertex,
                    DBVisualizerJFrame.makeVertextComparator());
            fail("disconnected graph should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exc) {
            // expected
        }
    }

    public void testGetMaxRho() {
        Graph graph = makeGraph();
        Vertex startVertex = getVertexForLabel(graph, "2");
        Map vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex,
                VERT_COMPARATOR);
        int actRho = RadialLayoutHelper.getMaxRho(vertexToPolarCoordMap);
        assertEquals(3, actRho);
    }


    public void testTreeDownwardDirected() {
        Graph graph = makeTreeDownwardDirected();
        checkTreeRadiusAndAngle(graph);
    }

    public void testTreeMixedDirections() {
        Graph graph = makeTreeMixedDirections();
        checkTreeRadiusAndAngle(graph);
    }

    /**
     * Tests X,Y calculations based on Rho, Theta.
     */
    public void testVertexLocationFunction() {
        int offset = (DBVisualizerJFrame.VERTEX_DIAMETER / 2);
        Graph graph = makeGraph();
        Vertex startVertex = getVertexForLabel(graph, "2");
        Dimension size = new Dimension(100 + DBVisualizerJFrame.VERTEX_DIAMETER,
                100 + DBVisualizerJFrame.VERTEX_DIAMETER);
        Map vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex,
                VERT_COMPARATOR);
        VertexLocationFunction vertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, size, DBVisualizerJFrame.VERTEX_DIAMETER);
        checkVertexCoords(graph, vertLocFcn, "2", 50.0 + offset, 50.0 + offset);
        checkVertexCoords(graph, vertLocFcn, "1", 58.33 + offset, 35.56 + offset);
        checkVertexCoords(graph, vertLocFcn, "3", 33.33 + offset, 50.0 + offset);
        checkVertexCoords(graph, vertLocFcn, "6", 58.33 + offset, 64.43 + offset);
        checkVertexCoords(graph, vertLocFcn, "5", 66.66 + offset, 21.13 + offset);
        checkVertexCoords(graph, vertLocFcn, "4", 75.0 + offset, 6.69 + offset);
        checkVertexCoords(graph, vertLocFcn, "7", 66.66 + offset, 78.86 + offset);
        checkVertexCoords(graph, vertLocFcn, "8", 75.0 + offset, 93.30 + offset);
    }

}
