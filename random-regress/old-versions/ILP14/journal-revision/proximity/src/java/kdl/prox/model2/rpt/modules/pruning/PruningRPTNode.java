/**
 * $Id: PruningRPTNode.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.model2.rpt.RPTNode;

public class PruningRPTNode extends RPTNode {

    double alpha; //Alpha level used to prune this tree

    public PruningRPTNode(RPTNode source) {
        super();
        split = source.getSplit();
        classLabelDistribution = source.getClassLabelDistribution();
        if (source.getYesBranch() != null) {
            yesBranch = source.getYesBranch().copy();
        }
        if (source.getYesBranch() != null) {
            noBranch = source.getNoBranch().copy();
        }
    }

    public double getAlpha() {
        return alpha;
    }

    public PruningRPTNode setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }
}
