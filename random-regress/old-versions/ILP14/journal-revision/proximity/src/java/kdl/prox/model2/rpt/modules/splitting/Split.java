/**
 * $Id: Split.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.splitting;

import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import org.apache.log4j.Logger;

/**
 * A scoring class for a Chi-Square statistic
 */
public class Split {

    protected static Logger log = Logger.getLogger(Split.class);

    private FeatureSetting fs;
    private RPTScoringModule score;

    public Split(FeatureSetting fs, RPTScoringModule score) {
        this.fs = fs;
        this.score = score;
    }

    public FeatureSetting getFs() {
        return fs;
    }

    public RPTScoringModule getScore() {
        return score;
    }

    public String toString() {
        return "FS=" + fs.toString() + ", SCORE=" + score.toString();
    }

}
