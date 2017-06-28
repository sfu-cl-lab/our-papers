/**
 * $Id: LayoutAnimationTest.java 3658 2007-10-15 16:29:11Z schapira $
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
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


/**
 * Tests animating radial layouts. We use the following test cases (see makeGraphN()):
 * <p/>
 * <pre>
 * case 1: 4   5               case 4:            4   5
 *          \ /                                    \ /
 *           2        4   5             2           2
 *           |         \ /              |           |
 *          (1)   to   (2)             (1)   to    (1)
 *           |          |               |           |
 *           3          1               3           3
 *                      |
 *                      3
 * --
 * case 2:  2                                      1
 *          |                                      |
 *         (1)   to   (2)      case 5: (1)   to   (2)
 *          |          |                |          |
 *          3          1                2          3
 *                     |                |
 *                     3                3
 * --
 * case 3:           5   6
 *                    \ /
 *          2          2
 *          |          |
 *         (1)   to   (1)
 *          |          |
 *          3          3
 *          |          |
 *          4          4
 * </pre>
 * todo xx other cases: 2, 3, 5
 * todo xx check *all* linear states?
 */
public class LayoutAnimationTest extends TestCase {

    private static final Logger log = Logger.getLogger(LayoutAnimationTest.class);
    private static final double DELTA = 0.01;
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
    private static final Dimension SIZE = new Dimension(100 + DBVisualizerJFrame.VERTEX_DIAMETER,
            100 + DBVisualizerJFrame.VERTEX_DIAMETER);


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();   // for log4j
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void addEdge(Graph graph, Vertex v1, Vertex v2) {
        graph.addEdge(new DirectedSparseEdge(v1, v2));
    }

    private void checkIntermStates(VertexLocationFunction startVertLocFcn,
                                   VertexLocationFunction endVertLocFcn,
                                   Graph graph, int numIntermediateStates) {
        LinearStateTransFcn linearStateTransFcn = new LinearStateTransFcn();
        List vertLocFcnList = linearStateTransFcn.calcIntermediateStates(graph,
                startVertLocFcn, endVertLocFcn, numIntermediateStates);
        assertEquals(numIntermediateStates, vertLocFcnList.size());
        for (Iterator vertIter = graph.getVertices().iterator(); vertIter.hasNext();) {
            Vertex vertex = (Vertex) vertIter.next();
            Point2D startCoords = LinearStateTransFcn.getStartLocation(vertex,
                    startVertLocFcn, endVertLocFcn);    // in case it's not in startVertLocFcn (expand case)
            Point2D endCoords = endVertLocFcn.getLocation(vertex);

            assertNotNull(startCoords);
            assertNotNull(endCoords);

            double deltaXOverall = endCoords.getX() - startCoords.getX();
            double deltaYOverall = endCoords.getY() - startCoords.getY();
            double deltaX = deltaXOverall / (numIntermediateStates + 1);    // + 1 for start and end
            double deltaY = deltaYOverall / (numIntermediateStates + 1);    // ""
            for (int intermStateIdx = 0; intermStateIdx < vertLocFcnList.size(); intermStateIdx++) {
                VertexLocationFunction vertLocFcn = (VertexLocationFunction) vertLocFcnList.get(intermStateIdx);
                Point2D vertLoc = vertLocFcn.getLocation(vertex);

                assertNotNull(vertLoc);
                assertEquals(startCoords.getX() + ((intermStateIdx + 1) * deltaX), vertLoc.getX(), DELTA);
                assertEquals(startCoords.getY() + ((intermStateIdx + 1) * deltaY), vertLoc.getY(), DELTA);
            }
        }
    }

    /**
     * @param graph
     * @param label
     * @return first Vertex in graph whose LABEL_KEY equals() label. returns null if none found
     *         <p/>
     *         todo move this and other graph helpers from this and others to util?
     */
    private Vertex getVertexForLabel(Graph graph, String label) {
        java.util.Set vertices = graph.getVertices();
        for (java.util.Iterator vertIter = vertices.iterator(); vertIter.hasNext();) {
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

    private Graph makeGraph1() {
        Graph graph = new SparseGraph();

        Vertex v1 = makeVertex(graph, "1");
        Vertex v2 = makeVertex(graph, "2");
        Vertex v3 = makeVertex(graph, "3");
        Vertex v4 = makeVertex(graph, "4");
        Vertex v5 = makeVertex(graph, "5");

        addEdge(graph, v1, v2);
        addEdge(graph, v1, v3);
        addEdge(graph, v2, v4);
        addEdge(graph, v2, v5);

        return graph;
    }

    private Graph makeGraph4a() {   // case 4 "from" graph
        Graph graph = new SparseGraph();

        Vertex v1 = makeVertex(graph, "1");
        Vertex v2 = makeVertex(graph, "2");
        Vertex v3 = makeVertex(graph, "3");

        addEdge(graph, v1, v2);
        addEdge(graph, v1, v3);

        return graph;
    }

    /**
     * @param graph as returned by makeGraph4a
     */
    private void makeGraph4b(Graph graph) { // case 4 "to" graph
        Vertex v2 = getVertexForLabel(graph, "2");

        Vertex v4 = makeVertex(graph, "4");
        Vertex v5 = makeVertex(graph, "5");

//        addEdge(graph, v2, v4);
        addEdge(graph, v4, v2);
//        addEdge(graph, v2, v5);
        addEdge(graph, v5, v2);
    }

    private Vertex makeVertex(edu.uci.ics.jung.graph.Graph graph, String label) {
        SparseVertex vertex = new SparseVertex() {
            public String toString() {
                return (String) getUserDatum(LABEL_KEY);
            }
        };
        vertex.addUserDatum(LABEL_KEY, label, UserData.SHARED);
        graph.addVertex(vertex);
        return vertex;
    }

    public void testLayoutAnimationCase1() {
        Graph graph = makeGraph1();

        // lay it out
        Vertex startVertex = getVertexForLabel(graph, "1");
        Map vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex, VERT_COMPARATOR);
        VertexLocationFunction startVertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, SIZE, DBVisualizerJFrame.VERTEX_DIAMETER);

        // simulate recenter on 2, lay it out, get end location snapshot
        startVertex = getVertexForLabel(graph, "2");
        vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex, VERT_COMPARATOR);
        VertexLocationFunction endVertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, SIZE, DBVisualizerJFrame.VERTEX_DIAMETER);

        // get and check the intermediate states
        checkIntermStates(startVertLocFcn, endVertLocFcn, graph, 1);
        checkIntermStates(startVertLocFcn, endVertLocFcn, graph, 2);
    }

    public void testLayoutAnimationCase4() {
        Graph graph = makeGraph4a();

        // lay it out
        Vertex startVertex = getVertexForLabel(graph, "1");
        Map vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex, VERT_COMPARATOR);
        VertexLocationFunction startVertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, SIZE, DBVisualizerJFrame.VERTEX_DIAMETER);

        // simulate expand of 2, lay it out, get end location snapshot
        makeGraph4b(graph);
        vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(startVertex, VERT_COMPARATOR);
        VertexLocationFunction endVertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, SIZE, DBVisualizerJFrame.VERTEX_DIAMETER);

        // get and check the intermediate states
        checkIntermStates(startVertLocFcn, endVertLocFcn, graph, 1);
        checkIntermStates(startVertLocFcn, endVertLocFcn, graph, 4);
    }

}
