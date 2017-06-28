/**
 * $Id: GiniScore.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.scoring;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

/**
 * A scoring class for a Quinlan Information Measure
 */
public class GiniScore implements RPTScoringModule {

    protected static Logger log = Logger.getLogger(GiniScore.class);

    protected double measure;

    public GiniScore() {
    }

    private GiniScore(String scoreStr) {
        measure = Double.parseDouble(scoreStr);
    }

    public RPTScoringModule compute(RPTState state, NST labels, NST weights, NST matches, AttributeSource source) {
        return new GiniScore(ScoreUtil.computeStatistic("gini", labels, weights, matches));
    }

    public boolean isBetterThan(RPTScoringModule other) {
        if (other == null) {
            return true;
        }
        Assert.condition(other instanceof GiniScore, "cannot compare score of different types");
        GiniScore otherScore = (GiniScore) other;
        return (measure > otherScore.measure);
    }

    public boolean isSignificant(double cutoff) {
        return true;
    }

    public String toString() {
        return "measure=" + measure;
    }

    public double getScore() {
        return measure;
    }

    public double getSignificance() {
        return -1.0;
    }

}
