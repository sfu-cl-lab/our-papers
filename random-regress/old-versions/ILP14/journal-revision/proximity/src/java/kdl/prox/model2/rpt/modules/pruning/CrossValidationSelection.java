/**
 * $Id: CrossValidationSelection.java 3658 2007-10-15 16:29:11Z schapira $
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
import kdl.prox.monet.Connection;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-validation Tree selection for pruning involves learning trees on cross-validation
 * samples.  This implementation assumes the currentState.trainContainer has train and test
 * samples nested within it. This procedure is very intensive. It requires learning
 * an additional tree on each fold using the same settings as the parent rpt.  A pruning
 * sequence for each fold is also created. To select a tree from the main pruning sequence,
 * the tree from each fold with the closest alpha value is used to estimate the error on the
 * test set for each fold.
 * <p/>
 * See Breiman, Friedman, Olshen and Stone section 3.4.2 for more details.
 */
public class CrossValidationSelection implements TreeSelectionModule {

    private static Logger log = Logger.getLogger(CrossValidationSelection.class);

    int numFolds = 5;
    RPT parentRPT;

    public CrossValidationSelection(RPT rpt, int folds) {
        parentRPT = rpt;
        numFolds = folds;
    }


    public RPTNode chooseTree(List<PruningRPTNode> treeList, RPTState currentState) {
        //First learn models on each fold and save the sequences.

        log.info("Choosing tree using cross-validation.");
        List<List<PruningRPTNode>> foldSequences = new ArrayList<List<PruningRPTNode>>();


        for (int fold = 0; fold < numFolds; fold++) {
            Connection.beginScope();
            RPT foldRPT = new RPT();

            //Copy the parent RPT except for pruning.
            foldRPT.learningModule = parentRPT.learningModule;
            //foldRPT.pruningModule is now the default pruning module. The pruning module belongs to the RPT.

            //Learn the fold RPT
            Container foldTrainContainer = currentState.trainContainer.getChild("train").getChild(fold + "");
            log.info("Learning tree for pruning on fold " + fold);
            foldRPT.learn(foldTrainContainer, currentState.classLabel, currentState.inputSources);
            RPTNode foldRoot = foldRPT.getRootNode();

            List<PruningRPTNode> foldPruningSequence = ((CostComplexityPruning) parentRPT.pruningModule).getPruningSequence(foldRoot);
            foldSequences.add(foldPruningSequence);
            Connection.endScope();
        }


        PruningRPTNode bestTree = treeList.get(0);
        int bestIndex = 0;
        double bestScore = Double.POSITIVE_INFINITY;

        //Pick the best tree using the cross validated trees.
        for (int i = 0; i < treeList.size(); i++) {
            PruningRPTNode mainTree = treeList.get(i);
            double score = Double.POSITIVE_INFINITY;

            log.info("Testing learned tree " + i + " (size = " + mainTree.getLeafCount() + ", alpha = " + mainTree.getAlpha() + ")");


            if (mainTree.getDepth() == 0) { //Just a root node, should be last in the list.
                score = mainTree.getTreeResubError(mainTree.getInstanceCount());
            } else {
                //Assume we are not last in the list.
                double targetAlpha = getMidpoint(mainTree.getAlpha(), treeList.get(i + 1).getAlpha());
                int predCount = 0;
                Predictions totalPreds = new Predictions();
                //find the best fold tree to estimate error.
                for (int fold = 0; fold < numFolds; fold++) {
                    Connection.beginScope();
                    //log.info("BATs: " + Connection.readValue("view_bbp_name().count().print()"));
                    //log.info("Loaded: " + Connection.readValue("view_bbp_status().select(\"load\").count().print()"));
                    //log.info("Mem: " + MonetUtil.getMemUsage());

                    log.info("\tFold " + fold);
                    RPTNode bestFoldTree = getBestFoldTree(foldSequences.get(fold), targetAlpha);
                    Container foldTestContainer = currentState.trainContainer.getChild("test").getChild(fold + "");
                    RPT testRPT = new RPT().learnFromRPTNode(bestFoldTree, currentState.classLabel);
                    Predictions foldPreds = testRPT.apply(foldTestContainer, currentState.nstCache);
                    predCount += foldPreds.size();

                    //This assumes subgraph IDs are unique across folds
                    totalPreds.setPredictions(foldPreds);
                    totalPreds.setTrueLabels(foldTestContainer, currentState.classLabel);

                    //log.info("BATs: " + Connection.readValue("view_bbp_name().count().print()"));
                    //log.info("Loaded: " + Connection.readValue("view_bbp_status().select(\"load\").count().print()"));
                    //log.info("Mem: " + MonetUtil.getMemUsage());
                    Connection.endScope();
                }
                if (totalPreds.size() != predCount) {
                    log.warn("SUBGRAPH IDS OVERLAP ACROSS PRUNING FOLDS.");
                }
                score = totalPreds.getZeroOneLoss();
            }

            if (Double.compare(score, bestScore) < 0) {
                bestTree = mainTree;
                bestIndex = i;
                bestScore = score;
            } else if (Double.compare(score, bestScore) == 0) {
                if (mainTree.getLeafCount() < bestTree.getLeafCount()) {
                    bestTree = mainTree;
                }
            }

        }
        log.info("Selected tree " + bestIndex + " (size = " + bestTree.getLeafCount() + ", alpha = " + bestTree.getAlpha() + ")");
        return bestTree;
    }


    /**
     * Since the alphas may not match up between the main tree and folds. Need to find the best
     * fold tree for the main tree alpha. This is the first tree with alpha >= target alpha
     *
     * @param pruningRPTNodes
     * @param targetAlpha
     * @return
     */
    public RPTNode getBestFoldTree(List<PruningRPTNode> pruningRPTNodes, double targetAlpha) {
        for (PruningRPTNode pruningRPTNode : pruningRPTNodes) {
            if (pruningRPTNode.getAlpha() >= targetAlpha) {
                return pruningRPTNode;
            }
        }
        return null;
    }

    /**
     * Get the geometic midpoint between the two values
     *
     * @param alphaLower
     * @param alphaUpper
     * @return
     */
    private double getMidpoint(double alphaLower, double alphaUpper) {
        return Math.sqrt(alphaLower * alphaUpper);
    }


}
