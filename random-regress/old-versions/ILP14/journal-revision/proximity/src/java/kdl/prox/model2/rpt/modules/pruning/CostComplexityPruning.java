/**
 * $Id: CostComplexityPruning.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.pruning;

import kdl.prox.db.Container;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.RPTWalker;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * CostComplexityPruning implements the pruning algorithm described in the monograph
 * Classification and Regression Trees by Breiman, Friedman, Olshen and Stone.
 */
public class CostComplexityPruning implements RPTPruningModule {

    private static Logger log = Logger.getLogger(CostComplexityPruning.class);

    public TreeSelectionModule selectionModule = null;

    /**
     * This is the constructor to use testSample tree selection. Requires
     * a single sample to prune.
     *
     * @param testContainer
     */
    public CostComplexityPruning(Container testContainer) {
        selectionModule = new TestSampleSelection(testContainer);
    }

    /**
     * This is the constructor to use cross validation tree selection
     * Assumes the cross-validation folds are in respective train and test containers
     * nested within the main container e.g.,
     * <p/>
     * mainContainer - train
     * - fold 0
     * - fold 1
     * ...
     * -fold n
     * - test
     * - fold 0
     * - fold 1
     * ...
     * -fold n
     * <p/>
     * This structure can be created by kdl.prox.script.Proximity.sampleContainer()
     *
     * @param parentRPT
     * @param numFolds
     */
    public CostComplexityPruning(RPT parentRPT, int numFolds) {
        selectionModule = new CrossValidationSelection(parentRPT, numFolds);
    }

    public RPTNode prune(RPTNode root, RPTState state) {
        log.info("Full tree contains " + root.getLeafCount() + " leaves.");
        //First stage: Build a sequence of trees which are the minimum for a particular
        //complexity parameter.
        //The first tree in the list is the minimum sized tree for the root's error rate
        log.info("Building Pruning Sequence.");
        List<PruningRPTNode> pruningSequence = getPruningSequence(root);
        log.info("Found a pruning sequence of length " + pruningSequence.size());
        //Second stage: Pick the best tree from among the sequence.
        RPTNode prunedTree = selectionModule.chooseTree(pruningSequence, state);
        log.info("Pruned " + (root.getLeafCount() - prunedTree.getLeafCount()) + " leaves out of " + root.getLeafCount());
        return prunedTree;
    }

    /**
     * This methods returns a sequence of trees in the order of smallest alpha to largest.
     *
     * @param root
     * @return
     */
    public List<PruningRPTNode> getPruningSequence(RPTNode root) {
        List<PruningRPTNode> pruningSequence = new ArrayList<PruningRPTNode>();
        PruningRPTNode t1 = getT1(root, root.getInstanceCount());
        pruningSequence.add(t1);
        PruningRPTNode currTree = t1;
        while (currTree.getLeafCount() > 1) {
            PruningRPTNode prunedTree = findAndRemoveWeakestLink(currTree); //Alters a copy of currTree
            pruningSequence.add(prunedTree);
            currTree = prunedTree;
        }
        return pruningSequence;
    }

    /**
     * T1 is the smallest minimizing subtree of the learned tree (Proposition 3.7, pg 68)
     * This becomes the starting point for pruning. This is the smallest tree with
     * the same resubstitution error as the learned tree.
     * This method collapses any node whose node error is equal to the sum
     * of the node error of its leaves.
     *
     * @param root
     * @param totalInstanceCount
     * @return
     */
    public PruningRPTNode getT1(RPTNode root, double totalInstanceCount) {
        PruningRPTNode t1 = new PruningRPTNode(root);
        if (!root.isLeaf()) {
            t1.setNoBranch(getT1(t1.getNoBranch(), totalInstanceCount));
            t1.setYesBranch(getT1(t1.getYesBranch(), totalInstanceCount));

            if (t1.getYesBranch().isLeaf() && t1.getNoBranch().isLeaf()) {
                double leafError = t1.getYesBranch().getNodeResubError(totalInstanceCount) +
                        t1.getNoBranch().getNodeResubError(totalInstanceCount);
                double nodeError = t1.getNodeResubError(totalInstanceCount);
                if (Double.compare(leafError, nodeError) == 0) {
                    t1.collapse();
                }
            }
        }
        return t1.setAlpha(0.0);
    }

    /**
     * Follow the weakest link algorithm described in BOFS section 3.3
     * All nodes with tie scores are pruned from the tree.
     *
     * @param currTree
     * @return copy of currTree with the weakest link removed.
     */
    public PruningRPTNode findAndRemoveWeakestLink(PruningRPTNode currTree) {
        PruningRPTNode prunedTree = new PruningRPTNode(currTree);
        //use the walker to find the weakest link.
        CostComplexityWalker walker = new CostComplexityWalker(prunedTree);
        walker.walk(prunedTree);
        //Now remove the weakest links
        List<RPTNode> minNodeList = walker.getMinNodeList();
        for (RPTNode rptNode : minNodeList) {
            rptNode.collapse();
        }
        return prunedTree.setAlpha(walker.getMinScore());
    }

    public class CostComplexityWalker extends RPTWalker {

        List<RPTNode> minNodeList = new ArrayList<RPTNode>();
        double minScore = Double.POSITIVE_INFINITY;
        private RPTNode tree;

        public CostComplexityWalker(RPTNode currTree) {
            tree = currTree;
        }

        public void processNode(RPTNode node, int depth) {
            double nodeScore = scoreNode(node, tree);
            if (Double.compare(nodeScore, minScore) <= 0) {
                if (Double.compare(nodeScore, minScore) < 0) {
                    minNodeList.clear();
                }
                minNodeList.add(node);
                minScore = nodeScore;
            }
        }

        /**
         * This is the function g1(t) defined from equation 3.9 in BFOS.
         *
         * @param currNode
         * @return
         */
        public double scoreNode(RPTNode currNode, RPTNode currRoot) {
            if (currNode.isLeaf()) {
                return Double.POSITIVE_INFINITY;
            }
            return (currNode.getNodeResubError(currRoot.getInstanceCount()) - currNode.getTreeResubError(currRoot.getInstanceCount())) / (currNode.getLeafCount() - 1);
        }

        public List<RPTNode> getMinNodeList() {
            return minNodeList;
        }

        public double getMinScore() {
            return minScore;
        }

    }
}
