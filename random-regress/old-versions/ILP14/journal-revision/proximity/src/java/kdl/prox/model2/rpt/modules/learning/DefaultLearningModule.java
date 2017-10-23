/**
 * $Id: DefaultLearningModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.learning;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ContinuousProbDistribution;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.aggregatorselection.DefaultAggregatorSelectionModule;
import kdl.prox.model2.rpt.modules.aggregatorselection.RPTAggregatorSelectionModule;
import kdl.prox.model2.rpt.modules.scoring.ChiSquareScore;
import kdl.prox.model2.rpt.modules.scoring.RMSEScore;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import kdl.prox.model2.rpt.modules.significance.DefaultSignificanceModule;
import kdl.prox.model2.rpt.modules.significance.RPTSignificanceModule;
import kdl.prox.model2.rpt.modules.splitting.DefaultSplittingModule;
import kdl.prox.model2.rpt.modules.splitting.RPTSplittingModule;
import kdl.prox.model2.rpt.modules.splitting.Split;
import kdl.prox.model2.rpt.modules.stopping.DefaultStoppingModule;
import kdl.prox.model2.rpt.modules.stopping.RPTStoppingModule;
import kdl.prox.model2.rpt.util.BranchingUtil;
import kdl.prox.model2.rpt.util.RPTUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Default learning Module implements a recursive splitting tree-building algorithm.
 */
public class DefaultLearningModule implements RPTLearningModule {

    private static Logger log = Logger.getLogger(DefaultLearningModule.class);

    private Random rand = new Random();

    // reconfigurable modules
    public RPTScoringModule scoringModule;          // instantiated at time of learning. See learn()
    public RPTSignificanceModule significanceModule;
    public RPTSplittingModule splittingModule;
    public RPTStoppingModule stoppingModule;
    public RPTAggregatorSelectionModule aggModule;

    public DefaultLearningModule() {
        aggModule = new DefaultAggregatorSelectionModule();
        splittingModule = new DefaultSplittingModule();
        stoppingModule = new DefaultStoppingModule();
        significanceModule = new DefaultSignificanceModule();
    }

    /**
     * Repeatedly find the best fs to split on, and recursively learn the yes and no branches
     *
     * @param currentState
     * @return an RPTNode with the split and the class distribution, and the children set
     */
    public RPTNode learn(RPTState currentState) {
        // set the corresponding scoring module, unless it's already set
        if (scoringModule == null) {
            scoringModule = (currentState.classLabel.isContinuous() ? new RMSEScore() : new ChiSquareScore());
        }

        // Find probability distribution for this node and create the node
        NST currentSubgIDs = currentState.subgIDs;
        NST classDistrNST = currentState.classLabel.getSourceTable().join(currentSubgIDs, "subg_id = subg_id", "value, weight");
        ProbDistribution probDistribution;
        if (currentState.classLabel.isContinuous()) {
            probDistribution = new ContinuousProbDistribution(classDistrNST);
        } else {
            probDistribution = new DiscreteProbDistribution(classDistrNST);
        }
        RPTNode node = new RPTNode(probDistribution);

        // stop here?
        if (stoppingModule.isStop(currentState)) {
            return node;
        }

        // find the best split for this node
        log.info(RPTUtil.getDepthIndentation(currentState.depth) + "Choosing split for " + currentSubgIDs.getRowCount() + " subgs");
        Split split = splittingModule.chooseSplit(currentState, scoringModule);

        // recurse on the branches if not null
        if (split != null && significanceModule.isSignificant(currentState, split.getScore())) {
            log.info(RPTUtil.getDepthIndentation(currentState.depth) + "Chose split " + split);
            //Extract this into expandNode
            node.setSplit(split.getFs());
            currentState.featureSettingList.remove(split.getFs());
            NST[] branchSubgIDs = BranchingUtil.getBranchSubgIDs(split.getFs(), currentState);

            node.setYesBranch(learn(new RPTState(currentState, currentState.depth + 1, branchSubgIDs[0])));
            node.setNoBranch(learn(new RPTState(currentState, currentState.depth + 1, branchSubgIDs[1])));
            branchSubgIDs[0].release();
            branchSubgIDs[1].release();
            currentState.featureSettingList.add(split.getFs());
        }

        return node;
    }

    /**
     * getNextSplit returns the location and details of the best split at a given
     * location in the tree without performing recursion.
     *
     * @param node
     * @param state
     * @return
     */
    public IntermediateSplit getNextSplit(RPTNode node, RPTState state) {

        DB.beginScope();

        if (node.isLeaf()) {
            //Do the work
            if (stoppingModule.isStop(state)) {
                return null;
            }
            return new IntermediateSplit(splittingModule.chooseSplit(state, scoringModule), node, state);
        }

        // find the specified node and create the appropriate RPTState to expand that node
        // by getting the right subgraphs and removing possible feature settings

        Assert.condition(!node.isLeaf(), "Cannot split a leaf node");
        FeatureSetting split = node.split;
        NST[] branchSubgIDs = BranchingUtil.getBranchSubgIDs(split, state);

        RPTState leftRptState = new RPTState(state, state.depth + 1, branchSubgIDs[0]);
        leftRptState.featureSettingList.remove(split);

        RPTState rightRptState = new RPTState(state, state.depth + 1, branchSubgIDs[1]);
        rightRptState.featureSettingList.remove(split);

        IntermediateSplit leftSplit = getNextSplit(node.getYesBranch(), leftRptState);
        IntermediateSplit rightSplit = getNextSplit(node.getNoBranch(), rightRptState);

        DB.endScope();

        if (leftSplit == null && rightSplit == null) {
            return null;
        }

        if (leftSplit != null) {
            if (rightSplit == null) {
                return leftSplit;
            } else if (leftSplit.getSplit().getScore().isBetterThan(rightSplit.getSplit().getScore())) {
                return leftSplit;
            } else if (rightSplit.getSplit().getScore().isBetterThan(leftSplit.getSplit().getScore())) {
                return rightSplit;
            } else {
                return rand.nextDouble() > 0.5 ? leftSplit : rightSplit;
            }
        }

        //else
        return rightSplit;
    }


    /**
     * Applies an ItermediateSplit at a specific location in the tree. Used in conjunction with getNextSplit().
     *
     * @param split
     */
    public void applySplit(IntermediateSplit split) {
        Assert.condition(split.getNode() != null, "Split must be created by getNextSplit.");
        Assert.condition(split.getSplit().getFs() != null, "Feature Setting must not be null");

        RPTNode node = split.getNode();
        RPTState state = split.getState();

        node.setSplit(split.getSplit().getFs());
        state.featureSettingList.remove(split.getSplit().getFs());
        NST[] branchSubgIDs = BranchingUtil.getBranchSubgIDs(split.getSplit().getFs(), state);

        node.setYesBranch(createNodeFromBranchIDs(state, branchSubgIDs[0]));
        node.setNoBranch(createNodeFromBranchIDs(state, branchSubgIDs[1]));
    }

    /**
     * Utility method for applySplit to create an RPTNode from an IntermediateSplit.
     *
     * @param state
     * @param branchSubgIDs
     * @return
     */
    private RPTNode createNodeFromBranchIDs(RPTState state, NST branchSubgIDs) {
        RPTState newState = new RPTState(state, state.depth + 1, branchSubgIDs);

        // Find probability distribution for this node and create the node
        NST currentSubgIDs = newState.subgIDs;
        NST classDistrNST = newState.classLabel.getSourceTable().join(currentSubgIDs, "subg_id = subg_id", "value, weight");
        ProbDistribution probDistribution;
        if (newState.classLabel.isContinuous()) {
            probDistribution = new ContinuousProbDistribution(classDistrNST);
        } else {
            probDistribution = new DiscreteProbDistribution(classDistrNST);
        }
        return new RPTNode(probDistribution);
    }


    public RPTAggregatorSelectionModule getAggregatorSelectionModule() {
        return aggModule;
    }

}
