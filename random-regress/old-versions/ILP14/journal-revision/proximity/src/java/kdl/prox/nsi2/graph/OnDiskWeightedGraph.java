/**
 * $Id: OnDiskWeightedGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This Graph implementation leaves everyting in the Monet side: for every call to getNeighbors, there
 * is a read from Monet.
 * It is very slow!
 */
public class OnDiskWeightedGraph implements Graph {
    private static Logger log = Logger.getLogger(OnDiskWeightedGraph.class);
    private boolean isDirected;
    private NST linkNST;

    public OnDiskWeightedGraph(boolean isDirected) {
        this.isDirected = isDirected;
        this.linkNST = DB.getLinks("*", "weight");
    }

    public Set<Node> getNeighbors(Integer id) {
        Set<Node> neighbors = new HashSet<Node>();

        NST forwardNeighbors = linkNST.filter("o1_id = " + id);
        ResultSet rs = forwardNeighbors.selectRows();
        while(rs.next()) {
            neighbors.add(new Node(rs.getOID(3), rs.getDouble(4)));
        }
        forwardNeighbors.release();

        if (!this.isDirected) {
            NST backwardNeighbors = linkNST.filter("o2_id = " + id);
            rs = backwardNeighbors.selectRows();
            while(rs.next()) {
                neighbors.add(new Node(rs.getOID(2), rs.getDouble(4)));
            }
            backwardNeighbors.release();
        }

        return neighbors;
    }

    public Set<Node> getNeighbors(Collection<Integer> ids) {
        Set<Node> neighbors = new HashSet<Node>();

        for (Integer id : ids) {
            neighbors.addAll(getNeighbors(id));
        }

        return neighbors;
    }
}
