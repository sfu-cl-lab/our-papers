/**
 * $Id: GraphAsTreeTest.java 3658 2007-10-15 16:29:11Z schapira $
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


public class GraphAsTreeTest extends TestCase {

    private static final Logger log = Logger.getLogger(GraphAsTreeTest.class);
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

    private void checkSuccessors(GraphAsTree graphAsTree, String label,
                                 String[] expSuccLabels) {
        Graph graph = graphAsTree.getGraph();
        Vertex vertex = getVertexForLabel(graph, label);
        List actSuccessors = graphAsTree.getSuccessors(vertex);
        List expSuccessors = makeVertexListFromLabels(graph, expSuccLabels);
        assertEquals(expSuccessors.size(), actSuccessors.size());
        assertTrue(expSuccessors.containsAll(actSuccessors));
    }

    private void checkWidth(GraphAsTree graphAsTree, String label, int expWidth) {
        Graph graph = graphAsTree.getGraph();
        Vertex vertex = getVertexForLabel(graph, label);
        int actWidth = graphAsTree.getWidth(vertex);
        assertEquals(expWidth, actWidth);
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
                return (String) getUserDatum(LABEL_KEY);
            }
        };
        vertex.addUserDatum(LABEL_KEY, label, UserData.SHARED);
        return vertex;
    }

    private List makeVertexListFromLabels(Graph graph, String[] labels) {
        ArrayList arrayList = new ArrayList();
        for (int labelIdx = 0; labelIdx < labels.length; labelIdx++) {
            String label = labels[labelIdx];
            Vertex vertex = getVertexForLabel(graph, label);
            arrayList.add(vertex);
        }
        return arrayList;
    }

    public void testGraphStructure() {
        Graph graph = makeGraph();
        Vertex startVertex = getVertexForLabel(graph, "2");
        GraphAsTree graphAsTree = new GraphAsTree(startVertex, VERT_COMPARATOR);
        checkSuccessors(graphAsTree, "2", new String[]{"1", "3", "6"});
        checkSuccessors(graphAsTree, "1", new String[]{"5"});
        checkSuccessors(graphAsTree, "6", new String[]{"7"});
        checkSuccessors(graphAsTree, "5", new String[]{"4"});
        checkSuccessors(graphAsTree, "3", new String[]{});
        checkSuccessors(graphAsTree, "7", new String[]{"8"});
        checkSuccessors(graphAsTree, "4", new String[]{});
        checkSuccessors(graphAsTree, "8", new String[]{});
    }

    public void testGraphWidth() {
        Graph graph = makeGraph();
        Vertex startVertex = getVertexForLabel(graph, "2");
        GraphAsTree graphAsTree = new GraphAsTree(startVertex, VERT_COMPARATOR);
        checkWidth(graphAsTree, "2", 3);
        checkWidth(graphAsTree, "6", 1);
        checkWidth(graphAsTree, "3", 1);
        checkWidth(graphAsTree, "4", 1);
        checkWidth(graphAsTree, "7", 1);
        checkWidth(graphAsTree, "8", 1);
        checkWidth(graphAsTree, "1", 1);
        checkWidth(graphAsTree, "5", 1);
    }

    public void testTreeStructure() {
        Graph graph = makeTreeMixedDirections();
        Vertex startVertex = getVertexForLabel(graph, "2");
        GraphAsTree graphAsTree = new GraphAsTree(startVertex, VERT_COMPARATOR);
        checkSuccessors(graphAsTree, "2", new String[]{"1", "6"});
        checkSuccessors(graphAsTree, "1", new String[]{"5"});
        checkSuccessors(graphAsTree, "6", new String[]{"3", "7"});
        checkSuccessors(graphAsTree, "5", new String[]{});
        checkSuccessors(graphAsTree, "3", new String[]{"4"});
        checkSuccessors(graphAsTree, "7", new String[]{"8"});
        checkSuccessors(graphAsTree, "4", new String[]{});
        checkSuccessors(graphAsTree, "8", new String[]{});
    }

    public void testTreeWidth() {
        Graph graph = makeTreeMixedDirections();
        Vertex startVertex = getVertexForLabel(graph, "2");
        GraphAsTree graphAsTree = new GraphAsTree(startVertex, VERT_COMPARATOR);
        checkWidth(graphAsTree, "2", 3);
        checkWidth(graphAsTree, "6", 2);
        checkWidth(graphAsTree, "3", 1);
        checkWidth(graphAsTree, "4", 1);
        checkWidth(graphAsTree, "7", 1);
        checkWidth(graphAsTree, "8", 1);
        checkWidth(graphAsTree, "1", 1);
        checkWidth(graphAsTree, "5", 1);
    }

}
