/**
 * $Id: RptDirectedEdge.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;

public class RptDirectedEdge extends DirectedSparseEdge {
    Boolean yesBranch = false;
    double weight = 1;

    public RptDirectedEdge(Vertex from, Vertex to, Boolean yesBranch) {
        super(from, to);
        this.yesBranch = yesBranch;
    }

    public RptDirectedEdge(Vertex from, Vertex to, double weight, Boolean yesBranch) {
        super(from, to);
        this.weight = weight;
        this.yesBranch = yesBranch;
    }

    public double getWeight() {
        return weight;
    }

    public Boolean isYes() {
        return yesBranch;
    }

    public String toString() {
        if (yesBranch)
            return "Yes";
        else return "No";
    }
}
