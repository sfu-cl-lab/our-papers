/**
 * $Id: RptVertexIcon.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;

import kdl.prox.model2.common.probdistributions.ProbDistribution;

import javax.swing.*;
import java.awt.*;

/**
 * Draws the actual icon (rectangle that will contain text + class label distributions)
 * for each node in the graph (both splits and leaves).
 */
public class RptVertexIcon implements Icon {

    private static Color SPLIT_COLOR = new Color(204, 204, 255);

    int w, h, barThickness, barPosition;
    Boolean leaf;
    RptColors colorDist;
    ProbDistribution probDist;
    RptVertex v;

    public RptVertexIcon(RptVertex r) {
        this.probDist = r.getClassLabelDistributionView();
        this.w = r.getIconSize().width;
        this.h = r.getIconSize().height;
        this.barThickness = r.getBarThickness();
        this.barPosition = r.getBarPosition();
        this.leaf = r.isLeaf();
        this.colorDist = r.getColorMap();
        this.v = r;
    }

    /**
     * Draw the icon at the specified location.  Icon implementations
     * may use the Component argument to get properties useful for
     * painting, e.g. the foreground or background color.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D graphics2D = (Graphics2D) g;

        Rectangle rect = new Rectangle(h, w);
        x = x - w / 2;
        y = y - h / 2;
        int currXPos = x;
        int currYPos = y;

        //Creates the icon's white background which the text and distribution is drawn ontop of
        Rectangle background = new Rectangle(rect.x + x, rect.y + y, w, h);
        graphics2D.setColor(v.leaf ? Color.WHITE : SPLIT_COLOR);
        graphics2D.fill(background);
        graphics2D.draw(background);

        //Draws the distribution graph on each icon
        Object[] distinctVals = probDist.getDistinctValues();
        for (int valIdx = 0; valIdx < distinctVals.length; valIdx++) {
            Object value = distinctVals[valIdx];
            double prob = probDist.getLaplaceCorrProbability(value);
            int barWidth = (int) (prob * w);
            Rectangle currRect = new Rectangle(rect.x + currXPos,
                    rect.y + currYPos + barPosition, barWidth, barThickness);
            currXPos += barWidth;
            graphics2D.setColor(colorDist.getColor(value));
            graphics2D.fill(currRect);
            graphics2D.draw(currRect);
        }

        graphics2D.setStroke(new BasicStroke(1));
        graphics2D.setColor(Color.BLACK);
        // Draws the icon's border
        graphics2D.drawRect(rect.x + x, rect.y + y, w, h);
    }

    /**
     * Returns the icon's width.
     *
     * @return 0
     *         Should be an int specifying the fixed width of the icon.
     *         Returning the actual icon's width was resulting in each
     *         icon being offset by that much
     */
    public int getIconWidth() {
        return 0;
    }

    /**
     * Returns the icon's height.
     *
     * @return 0
     *         Should be an int specifying the fixed height of the icon.
     *         Returning the actual icon's width was resulting in each
     *         icon being offset by that much
     */
    public int getIconHeight() {
        if (leaf)
            return -h + 80;
        else return 0;
    }

}