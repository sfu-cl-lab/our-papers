/**
 * $Id: DefaultSignificanceModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.significance;

import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import org.apache.log4j.Logger;

/**
 * The default splitting module computes contingency tables for all possible splits and finds the one
 * with the best gStatistic.
 */
public class DefaultSignificanceModule implements RPTSignificanceModule {

    protected static Logger log = Logger.getLogger(DefaultSignificanceModule.class);

    protected double pVal = 0.05;

    public boolean isSignificant(RPTState state, RPTScoringModule score) {
        return score.isSignificant(pVal);
    }

    public double getpVal() {
        return pVal;
    }

    public RPTSignificanceModule setpVal(double pVal) {
        this.pVal = pVal;
        return this;
    }
}
