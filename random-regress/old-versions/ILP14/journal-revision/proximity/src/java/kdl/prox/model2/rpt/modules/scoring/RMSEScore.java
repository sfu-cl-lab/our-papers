/**
 * $Id: RMSEScore.java 3658 2007-10-15 16:29:11Z schapira $
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
 * A scoring class for a Chi-Square statistic
 */
public class RMSEScore implements RPTScoringModule {

    protected static Logger log = Logger.getLogger(RMSEScore.class);

    private double rmseNoSplit;
    private double rmse;
    private double d;
    private double dhat;

    public RMSEScore() {
    }

    private RMSEScore(String scoreStr) {
        String[] stats = scoreStr.split(",");
        rmseNoSplit = Double.parseDouble(stats[0]);
        rmse = Double.parseDouble(stats[1]);
        d = Double.parseDouble(stats[2]);
        dhat = Double.parseDouble(stats[3]);
    }

    public RPTScoringModule compute(RPTState state, NST labels, NST weights, NST matches, AttributeSource source) {
        return new RMSEScore(ScoreUtil.computeStatistic("rmse", labels, weights, matches));
    }

    public boolean isBetterThan(RPTScoringModule other) {
        if (other == null) {
            return true;
        }
        Assert.condition(other instanceof RMSEScore, "cannot compare score of different types");
        RMSEScore otherScore = (RMSEScore) other;
        return ((rmse < otherScore.rmse));
    }

    public boolean isSignificant(double cutoff) {
//        return d > Math.sqrt(-0.5 * Math.log(cutoff/2.0))*dhat;
        return rmse < rmseNoSplit;
    }

    public String toString() {
        return "RMSE=" + rmse + ", isSig(0.05)=" + isSignificant(0.05);
    }

    public double getScore() {
        return rmse;
    }

    public double getSignificance() {
        return -1.0;
    }

}
