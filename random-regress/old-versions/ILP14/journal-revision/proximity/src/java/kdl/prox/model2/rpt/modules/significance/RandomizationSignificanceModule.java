/**
 * $Id: RandomizationSignificanceModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.significance;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import kdl.prox.model2.rpt.util.RPTUtil;
import kdl.prox.monet.Connection;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * The default splitting module computes contingency tables for all possible splits and finds the one
 * with the best gStatistic.
 */
public class RandomizationSignificanceModule implements RPTSignificanceModule {

    protected static Logger log = Logger.getLogger(RandomizationSignificanceModule.class);

    protected int maxSamples = 50;     // the number of pseudo-samples to create
    protected double pVal = 0.05;      // pVal for significance of the original score, before randomization
    protected double threshold = 0.9;  // the ratio of times that the original score has to be better than random scores in order to be considered significant

    /**
     * Checks whether the given origMaxScore is significant by using randomization
     *
     * @param state
     * @return true if this origMaxScore is significant
     */
    public boolean isSignificant(RPTState state, RPTScoringModule origMaxScore) {
        if (!origMaxScore.isSignificant(pVal)) {
            return false;
        }

        List fsList = state.featureSettingList;
        NST labelNST = state.classLabel.getSourceTable();
        NST weightNST = state.subgIDs;

        int betterScoreCount = 0;
        double betterScoreCountThreshold = (getMaxSamples() - (getMaxSamples() * getThreshold()));

        for (int i = 0; i < getMaxSamples(); i++) {
            Connection.beginScope();
            log.info(RPTUtil.getDepthIndentation(state.depth + 1) + "Creating pseudo-sample " + i);
            NST randomizedClassNST = labelNST.randomize("subg_id", "value");

            // find the best
            RPTScoringModule maxScore = null;
            for (int fsIdx = 0; fsIdx < fsList.size(); fsIdx++) {
                FeatureSetting featureSetting = (FeatureSetting) fsList.get(fsIdx);
                NST fsNST = state.nstCache.getTable(featureSetting.toString());
                RPTScoringModule newScore = origMaxScore.compute(state, randomizedClassNST, weightNST, fsNST, null);
                if (newScore.isBetterThan(maxScore)) {
                    maxScore = newScore;
                }
            }

            if (maxScore.isBetterThan(origMaxScore)) {
                betterScoreCount++;
            }

            Connection.endScope();

            // we've exceeded the threshold. no need to test any more pseudo-samples
            if (betterScoreCount > betterScoreCountThreshold) {
                return false;
            }
        }

        return true;
    }


    public int getMaxSamples() {
        return maxSamples;
    }

    public double getpVal() {
        return pVal;
    }


    public double getThreshold() {
        return threshold;
    }

    public RPTSignificanceModule setMaxSamples(int maxSamples) {
        this.maxSamples = maxSamples;
        return this;
    }

    public RPTSignificanceModule setpVal(double pVal) {
        this.pVal = pVal;
        return this;
    }

    public RPTSignificanceModule setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

}
