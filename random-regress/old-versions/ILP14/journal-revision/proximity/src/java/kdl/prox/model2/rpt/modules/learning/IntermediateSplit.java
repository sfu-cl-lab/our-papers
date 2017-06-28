/**
 * $Id: IntermediateSplit.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.learning;

import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.modules.splitting.Split;

/**
 * Structure to return intermediate split information for RDN learning
 */
public class IntermediateSplit {

    public Split split;
    public RPTNode node;
    public RPTState state;

    public IntermediateSplit(Split split, RPTNode node, RPTState state) {
        this.split = split;
        this.node = node;
        this.state = state;
    }


    public Split getSplit() {
        return split;
    }

    public RPTNode getNode() {
        return node;
    }

    public RPTState getState() {
        return state;
    }
}
