/**
 * $Id: DefaultPruningModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;

/**
 * The most basic pruning plug-in: do nothing
 */
public class DefaultPruningModule implements RPTPruningModule {

    public RPTNode prune(RPTNode root, RPTState state) {
        return root;
    }

}
