/**
 * $Id: RadiiInfo.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.util.Map;


/**
 * A means to communicate radii-drawing information to
 * DBVisualizerJFrame.makeRadiiPaintable().
 */
public interface RadiiInfo {

    /**
     * @return
     * @see RadialLayoutHelper#computePolarCoordinates
     */
    public Map getVertexToPolarCoordMap();

    public VisualizationViewer getVisualizationViewer();

}
