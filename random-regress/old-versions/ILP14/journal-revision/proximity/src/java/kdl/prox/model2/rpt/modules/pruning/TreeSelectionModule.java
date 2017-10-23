/**
 * $Id: TreeSelectionModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;

import java.util.List;

/**
 * kdl.prox.model2.rpt.modules.pruning
 * User: afast
 * Date: Mar 26, 2007
 * $Id: TreeSelectionModule.java 3658 2007-10-15 16:29:11Z schapira $
 */
public interface TreeSelectionModule {

    public RPTNode chooseTree(List<PruningRPTNode> treeList, RPTState currentState);

}
