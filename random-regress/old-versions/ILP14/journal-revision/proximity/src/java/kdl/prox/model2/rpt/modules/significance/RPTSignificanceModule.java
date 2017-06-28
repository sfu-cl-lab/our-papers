/**
 * $Id: RPTSignificanceModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.significance;

import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;

/**
 * Returns true/false indicating whether a scode is significant or not
 */
public interface RPTSignificanceModule {

    public boolean isSignificant(RPTState state, RPTScoringModule score);

}
