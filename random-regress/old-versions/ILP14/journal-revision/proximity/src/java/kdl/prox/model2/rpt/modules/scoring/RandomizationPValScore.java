/**
 * $Id: RandomizationPValScore.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.scoring;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.util.RPTUtil;
import kdl.prox.monet.Connection;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

/**
 * Computes the score by randomizing the feature setting table and getting the count of scores better than
 * without randomization
 */
public class RandomizationPValScore implements RPTScoringModule {

    protected static Logger log = Logger.getLogger(RandomizationPValScore.class);

    private RPTScoringModule scoringMethod;
    protected int maxSamples = 50;  // the number of pseudo-samples to create

    private double empPVal;


    public RandomizationPValScore(RPTScoringModule scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    private RandomizationPValScore(RPTScoringModule scoringMethod, double empPVal) {
        this.scoringMethod = scoringMethod;
        this.empPVal = empPVal;
    }

    public RPTScoringModule compute(RPTState state, NST labels, NST weights, NST matches, AttributeSource source) {
        int betterScoreCount = 0;

        RPTScoringModule origScore = scoringMethod.compute(state, labels, weights, matches, null);

        for (int i = 0; i <= maxSamples; i++) {
            Connection.beginScope();
            log.info(RPTUtil.getDepthIndentation(state.depth + 1) + "Creating pseudo-sample " + i);
            NST randomizedMatches = matches.randomize("subg_id", "match");
            RPTScoringModule currScore = scoringMethod.compute(state, labels, weights, randomizedMatches, null);

            if (!origScore.isBetterThan(currScore)) { //If currScore >= origScore
                betterScoreCount++;
            }
            Connection.endScope();
        }

        double empPVal = betterScoreCount * 1.0 / maxSamples;
        return new RandomizationPValScore(scoringMethod, empPVal);
    }

    public boolean isBetterThan(RPTScoringModule other) {
        if (other == null) {
            return true;
        }
        Assert.condition(other instanceof RandomizationPValScore, "cannot compare score of different types");
        RandomizationPValScore otherScore = (RandomizationPValScore) other;
        return ((empPVal < otherScore.empPVal));
    }

    public boolean isSignificant(double pVal) {
        return empPVal < pVal;
    }

    public RPTScoringModule setMaxSamples(int maxSamples) {
        this.maxSamples = maxSamples;
        return this;
    }

    public String toString() {
        return empPVal + "";
    }

    public double getScore() {
        return -1.0;
    }

    public double getSignificance() {
        return empPVal;
    }

}
