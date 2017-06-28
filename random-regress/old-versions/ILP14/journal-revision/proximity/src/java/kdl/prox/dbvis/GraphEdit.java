/**
 * $Id: GraphEdit.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import java.util.HashSet;
import java.util.Set;


/**
 * Records changes to a Graph, i.e., Vertex and Edge instances added or removed.
 */
public class GraphEdit {

    private Set addedVertices = new HashSet();  // Vertex instances added
    private Set addedEdges = new HashSet();     // Edge instances added


    public Set getAddedEdges() {
        return addedEdges;
    }

    public Set getAddedVertices() {
        return addedVertices;
    }

    public boolean isNoChanges() {
        return (addedVertices.size() == 0) && (addedEdges.size() == 0);
    }

    /**
     * Records that edge was added to a graph.
     *
     * @param edge
     */
    public void noteAddedEdge(Edge edge) {
        addedEdges.add(edge);
    }

    /**
     * Records that vertex was added to a graph.
     *
     * @param vertex
     */
    public void noteAddedVertex(Vertex vertex) {
        addedVertices.add(vertex);
    }

}
