/**
 * $Id: TestSampleSelection.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.db.Container;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Follows Breiman, Friedman, Olshen and Stone Section 3.4.1
 * Applies each tree in the pruning sequence to the testContainer
 * and picks the tree with the best score If scores are tied,
 * the smaller, less complex tree is picked.
 */

public class TestSampleSelection implements TreeSelectionModule {

    private static Logger log = Logger.getLogger(TestSampleSelection.class);

    Container testContainer;

    public TestSampleSelection(Container container) {
        testContainer = container;
    }

    public RPTNode chooseTree(List<PruningRPTNode> treeList, RPTState state) {
        log.info("Choosing tree with container " + testContainer.getName());
        PruningRPTNode bestTree = treeList.get(0);
        double bestScore = Double.POSITIVE_INFINITY;
        for (PruningRPTNode tree : treeList) {
            RPT testRPT = new RPT().learnFromRPTNode(tree, state.classLabel);
            Predictions newPreds = testRPT.apply(testContainer);
            newPreds.setTrueLabels(testContainer, state.classLabel);
            double score = newPreds.getZeroOneLoss();
            if (Double.compare(score, bestScore) < 0) {
                bestTree = tree;
                bestScore = score;
            } else if (Double.compare(score, bestScore) == 0) {
                if (tree.getLeafCount() < bestTree.getLeafCount()) {
                    bestTree = tree;
                }
            }
        }
        return bestTree;
    }
}
