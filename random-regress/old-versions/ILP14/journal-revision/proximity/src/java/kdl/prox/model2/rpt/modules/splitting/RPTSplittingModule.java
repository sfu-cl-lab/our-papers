/**
 * $Id: RPTSplittingModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.splitting;

import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;

/**
 * A splitting module is responsible for finding a good feature setting that splits a [subg_id, class] NST
 */
public interface RPTSplittingModule {

    public Split chooseSplit(RPTState state, RPTScoringModule scoringMethod);

}
