/**
 * $Id: ChiSquareScore.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.scoring;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.util.Assert;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

/**
 * A scoring class for a Chi-Square statistic
 */
public class ChiSquareScore implements RPTScoringModule {

    protected static Logger log = Logger.getLogger(ChiSquareScore.class);

    private double gStat;
    private double pVal;

    // allow creation of a template score
    public ChiSquareScore() {
    }

    public ChiSquareScore(String scoreStr) {
        String[] stats = scoreStr.split(",");
        gStat = Double.parseDouble(stats[0]);
        pVal = StatUtil.chiSquareP(gStat, Integer.parseInt(stats[1]));
    }

    public RPTScoringModule compute(RPTState state, NST labels, NST weights, NST matches, AttributeSource source) {
        return new ChiSquareScore(
                ScoreUtil.computeStatistic("gStatAndDF", labels, weights, matches));
    }

    public boolean isBetterThan(RPTScoringModule other) {
        if (other == null) {
            return true;
        }
        Assert.condition(other instanceof ChiSquareScore, "cannot compare score of different types");
        ChiSquareScore otherScore = (ChiSquareScore) other;
        return ((pVal < otherScore.pVal ||
                (pVal == otherScore.pVal && gStat > otherScore.gStat)));
    }

    public boolean isSignificant(double cutoff) {
        return pVal < cutoff;
    }

    public String toString() {
        return "gStat=" + gStat + ", pVal=" + pVal;
    }

    public double getScore() {
        return gStat;
    }

    public double getSignificance() {
        return pVal;
    }

}
