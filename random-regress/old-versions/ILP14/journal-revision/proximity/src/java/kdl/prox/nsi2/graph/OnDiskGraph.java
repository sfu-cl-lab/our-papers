/**
 * $Id: OnDiskGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This Graph implementation leaves everyting in the Monet side: for every call to getNeighbors, there
 * is a read from Monet.
 * It is very slow!
 */
public class OnDiskGraph implements Graph {
    private static Logger log = Logger.getLogger(OnDiskGraph.class);
    private boolean isDirected;

    public OnDiskGraph(boolean isDirected) {
        this.isDirected = isDirected;
    }

    public Set<Node> getNeighbors(Integer id) {
        Set<Node> neighbors = new HashSet<Node>();

        NST linkNST = DB.getLinkNST();
        String forwardNeighborBAT = Connection.executeAndSave(linkNST.getNSTColumn("o2_id") +
                ".semijoin(" + linkNST.getNSTColumn("o1_id") + ".uselect(oid(" + id + ")).mirror())");

        for (Integer neighborId : MonetUtil.read(forwardNeighborBAT).toOIDList(1)) {
            neighbors.add(new Node(neighborId, 1.0));
        }

        Connection.releaseSavedVar(forwardNeighborBAT);

        if (!this.isDirected) {
            String backwardNeighborBAT = Connection.executeAndSave(linkNST.getNSTColumn("o1_id") +
                    ".semijoin(" + linkNST.getNSTColumn("o2_id") + ".uselect(oid(" + id + ")).mirror())");

            for (Integer neighborId : MonetUtil.read(backwardNeighborBAT).toOIDList(1)) {
                neighbors.add(new Node(neighborId, 1.0));
            }

            Connection.releaseSavedVar(backwardNeighborBAT);
        }

        linkNST.release();
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
