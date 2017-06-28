/**
 * $Id: DefaultTreeSelectionModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;

import java.util.List;

public class DefaultTreeSelectionModule implements TreeSelectionModule {
    //Does the simplest, non-trivial thing returns the first pruned tree
    // index 0 always contains the non-pruned tree.
    public RPTNode chooseTree(List<PruningRPTNode> treeList, RPTState currentState) {
        return (RPTNode) treeList.get(1);
    }
}
