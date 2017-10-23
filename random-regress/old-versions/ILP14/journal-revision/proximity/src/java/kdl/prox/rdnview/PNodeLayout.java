/**
 * $Id: PNodeLayout.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * Utility class for laying out select PNodes in a Piccolo PCanvas.
 */
public class PNodeLayout {

    private static Logger log = Logger.getLogger(PNodeLayout.class);


    /**
     * @param childList
     * @return max width and height of all PNodes in childList
     */
    private static PDimension calcCellSize(List childList) {
        double maxWidth = 0;
        double maxHeight = 0;
        for (int childIdx = 0; childIdx < childList.size(); childIdx++) {
            PNode childPNode = (PNode) childList.get(childIdx);
            PBounds fullBounds = childPNode.getFullBounds();    // NB: must use fullbounds; otherwise some node sizes are invalid (incorrectly small)
            maxWidth = Math.max(maxWidth, fullBounds.getWidth());
            maxHeight = Math.max(maxHeight, fullBounds.getHeight());
        }
        return new PDimension(maxWidth, maxHeight);
    }

    /**
     * Main entry point for calculating a simple grid layout for children in
     * parentPNode that are listed in childList. The grid contains uniform cell
     * sizes based on the max width and max height of nodes in childList. The
     * grid is as square as possible given the number of nodes in childList.
     *
     * @param parentPNode
     * @param childList   List of PNodes that are children of parentPNode
     * @param xSpacing    X space between nodes. pass 0 for none
     * @param ySpacing    Y ""
     * @return List of Point2D instances, one for each PNode in childList. each
     *         point is the new layout location in global coordinates for its
     *         corresponding PNode in parentPNode
     */
    public static List calcGridLayout(PNode parentPNode, List childList,
                                      int xSpacing, int ySpacing) {
        List points = new ArrayList();
        PDimension cellSize = calcCellSize(childList);
        int numCols = (int) Math.round(Math.sqrt(childList.size()));
        for (int childIdx = 0; childIdx < childList.size(); childIdx++) {
            PNode childPNode = (PNode) childList.get(childIdx);
            Assert.condition(childPNode.getParent() == parentPNode,
                    "wrong parent: " + childPNode + ", " +
                    childPNode.getParent() + ", " + parentPNode);

            int col = childIdx % numCols;
            int row = childIdx / numCols;
            double x = col * cellSize.getWidth();
            double y = row * cellSize.getHeight();
            points.add(new Point2D.Double(x + (col == 0 ? 0 : xSpacing),
                    y + (row == 0 ? 0 : ySpacing)));
        }

        return points;
    }

}
