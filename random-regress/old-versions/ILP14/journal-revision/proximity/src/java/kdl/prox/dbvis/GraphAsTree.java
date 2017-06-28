/**
 * $Id: GraphAsTree.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 * Helper 'view' of an arbitrary graph as a tree. Used to do BFS and to compute
 * widths.
 */
public class GraphAsTree {

    private static final Logger log = Logger.getLogger(GraphAsTree.class);
    private Vertex startVertex;
    private Map successors = new HashMap();     // maps a parent Vertex to a List of its children, independent of edge direction. filled by computeSuccessors()


    public GraphAsTree(Vertex startVertex) {
        this(startVertex, null);
    }

    /**
     * Overload that takes non-null vertComparator.
     *
     * @param startVertex
     * @param vertComparator for sorting children. null if no sorting needed. takes two Vertex instances
     */
    public GraphAsTree(Vertex startVertex, final Comparator vertComparator) {
        this.startVertex = startVertex;
        computeSuccessors(startVertex, null, vertComparator);
    }

    /**
     * Calculates successors for every vertex reachable from vertex, using
     * breadth-first seach.
     *
     * @param vertex
     * @param visitedVerts   null if vertex is start vertex
     * @param vertComparator for sorting children. null if no sorting needed. takes two Vertex instances
     */
    private void computeSuccessors(Vertex vertex, Set visitedVerts,
                                   Comparator vertComparator) {
        if (visitedVerts == null) {
            visitedVerts = new HashSet();
        }
        visitedVerts.add(vertex);
        Set neighborsSet = new HashSet(vertex.getNeighbors());
        neighborsSet.removeAll(visitedVerts);
        ArrayList neighbors = new ArrayList(neighborsSet);
        // sort vertex's neighbors for deterministic order, if necessary
        if (vertComparator != null) {
            Collections.sort(neighbors, vertComparator);
        }
//        log.warn("computeSuccessors(): " + vertex + ":" + visitedVerts + ", " + neighbors);
        successors.put(vertex, neighbors);
        visitedVerts.addAll(neighbors);

        for (Iterator childIter = neighbors.iterator(); childIter.hasNext();) {
            Vertex childVert = (Vertex) childIter.next();
            computeSuccessors(childVert, visitedVerts, vertComparator);
        }
    }

    public Graph getGraph() {
        return (Graph) startVertex.getGraph();
    }

    /**
     * @return height in the sense of trees: maximium depth of a tree, where depth
     *         is # edges between a vertex and the root
     */
    public int getHeight() {
        return 0;
    }

    /**
     * Gets vertex's filtered successors. Assumes computeSuccessors() has been called.
     *
     * @param vertex
     * @return my neighborhood, but filtered to not include cycles, loops, etc.
     */
    public List getSuccessors(Vertex vertex) {
//        return vertex.getSuccessors();  // NB: doesn't handle general graphs
        return (List) successors.get(vertex);
    }

    /**
     * @param vertex
     * @return the width of vertex in my graph. returns 1 for leaves. width is
     *         defined for trees, and is the number of leaves taht are reachable
     *         from a given node
     */
    public int getWidth(Vertex vertex) {
        // for now simply walk the entire graph every time I'm called. later:
        // can do this once, saving values on each vertex. NB: might cause
        // synchronization problems if underlying graph changes
        // NB: for now I simply assume that my graph is a tree, and use vertex's
        // successors(). will fail for more general directed cyclic graph case
        List successors = getSuccessors(vertex);
        boolean isLeaf = successors.size() == 0;
//        log.warn("getWidth(): " + vertex + ", " + successors + ", " + isLeaf);
        if (isLeaf) {   // leaf?
            return 1;
        }

        // recurse
        int width = 0;
        for (Iterator succIter = successors.iterator(); succIter.hasNext();) {
            Vertex succVert = (Vertex) succIter.next();
            width += getWidth(succVert);
        }
        return width;
    }

}
