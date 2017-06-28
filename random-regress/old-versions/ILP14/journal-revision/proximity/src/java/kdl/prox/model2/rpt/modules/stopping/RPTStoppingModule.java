/**
 * $Id: RPTStoppingModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.stopping;

import kdl.prox.model2.rpt.RPTState;

public interface RPTStoppingModule {

    public boolean isStop(RPTState state);

}
