/**
 * $Id: DefaultStoppingModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.stopping;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPTState;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * A basic stopping module: stops at a given depth, or when there are < minSubgraphs rows, and when there's a single class label
 */
public class DefaultStoppingModule implements RPTStoppingModule {

    private static Logger log = Logger.getLogger(DefaultStoppingModule.class);


    int maxDepth = 3;
    int minSubgraphs = 2;

    public DefaultStoppingModule() {
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMinSubgraphs() {
        return minSubgraphs;
    }

    public boolean isStop(RPTState state) {
        if (state.depth >= this.maxDepth) {
            return true;
        }

        int rowCount = state.subgIDs.getRowCount();
        if (rowCount < minSubgraphs) {
            return true;
        }

        NST classLabels = state.classLabel.getSourceTable();
        List labels = classLabels.intersect(state.subgIDs, "subg_id EQ subg_id").getDistinctColumnValues("value");
        if (labels.size() < 2) {
            return true;
        }

        return false;
    }

    public DefaultStoppingModule setMaxDepth(int depth) {
        this.maxDepth = depth;
        return this;
    }

    public DefaultStoppingModule setMinSubgraphs(int nMin) {
        this.minSubgraphs = nMin;
        return this;
    }

}
