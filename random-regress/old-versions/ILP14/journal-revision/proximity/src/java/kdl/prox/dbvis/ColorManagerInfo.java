/**
 * $Id: ColorManagerInfo.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import kdl.prox.gui2.ColorManager;


/**
 * A means to communicate vertex coloring information to
 * SubgraphJFrame.makeVertexPaintFunction().
 */
public interface ColorManagerInfo {

    public ColorManager getColorManager();

}
