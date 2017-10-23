/**
 * $Id: RPTScoringModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.scoring;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rpt.RPTState;

/**
 * Stores the score for a split
 */
public interface RPTScoringModule {

    public RPTScoringModule compute(RPTState state, NST labels, NST weights, NST matches, AttributeSource source);

    public boolean isBetterThan(RPTScoringModule other);

    public boolean isSignificant(double pval);

    public String toString();

    public double getScore();

    public double getSignificance();

}
