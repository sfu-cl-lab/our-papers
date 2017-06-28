/**
 * $Id: RocCurveJApplet.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.python.core.PyList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;


/**
 * Applet to pop up a window that shows an ROC curve.  Needs to be
 * re-factored and integrated into the GUI.  Portions have been stolen
 * from the Sun Java tutorial on Graphics2D.
 */
public class RocCurveJApplet extends JApplet {

    private static Logger log = Logger.getLogger(RocCurveJApplet.class);

    private final static int maxCharHeight = 20;
    private final static int minFontSize = 6;

    private final static Color BG_COLOR = Color.white;
    private final static Color FG_COLOR = Color.black;

    private final static int WINDOW_H = 400;
    private final static int WINDOW_W = 600;

    private final static int marginLeft = 90;
    private final static int marginRight = 30;
    private final static int marginTop = 30;
    private final static int marginBottom = 60;

    private final static int yTickLength = 6;
    private final static int yNumTicks = 5;
    private final static int yHorLabOffset = 20 + yTickLength;
    private final static int yVertLabOffset = 4;
    private final static int yHorAxLabOffset = 45;

    private final static int xTickLength = 6;
    private final static int xNumTicks = 4;
    private final static int xHorLabOffset = 2;
    private final static int xVertLabOffset = 12 + xTickLength;
    private final static int xVertAxLabOffset = 35;


    private double[][] points; // list of two element arrays that contain points


    private RocCurveJApplet(double[][] points) {
        this.points = points;
        setBackground(BG_COLOR);
        setForeground(FG_COLOR);
    }

    /**
     * An embarassing hack to load a set of pre-computed ROC points
     * and display them using showCurveWindow.
     * todo: remove this method
     *
     * @param pyList
     */
    public static void showCurveWindow(PyList pyList) {
        double[][] points = convertListToDoubleArray(pyList);
        showCurveWindow(points);
    }

    /**
     * An embarassing hack to load a set of pre-computed ROC points
     * and display them using showCurveWindow.
     * todo: remove this method
     *
     * @param pyList
     * @return
     */
    private static double[][] convertListToDoubleArray(PyList pyList) {
        List list = Util.listFromPyList(pyList);
        Assert.condition(list.size() % 2 == 0, "List is of odd size.");
        double[][] points = new double[(list.size() / 2)][2];
        int i = 0;
        double lastPointOne = 0;
        double lastPointTwo = 0;
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Double point = (Double) iter.next();
            points[i][0] = point.doubleValue();
            Assert.condition(lastPointOne <= points[i][0], "out of order");

            point = (Double) iter.next();
            points[i][1] = point.doubleValue();
            Assert.condition(lastPointTwo <= points[i][1], "out of order (point two)");
            lastPointOne = points[i][0];
            lastPointTwo = points[i][1];
            i++;
        }
        return points;
    }

    public static void showCurveWindow(double[][] points) {
        JFrame jf = new JFrame("ROC");
        jf.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JApplet curveApplet = new RocCurveJApplet(points);

        jf.getContentPane().add("Center", curveApplet);
        jf.pack();
        jf.setSize(new Dimension(WINDOW_W, WINDOW_H));
        jf.setVisible(true);
        ;

    }


    public static void main(String s[]) {
        double[][] points = new double[][]{
                {0.0, 0.0},
                {0.01, 0.5},
                {0.05, 0.7},
                {0.1, 0.85},
                {0.2, 0.9},
                {0.3, 0.95},
                {0.5, 0.99},
                {1.0, 1.0},
        };

        showCurveWindow(points);
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pickFont(g2, "Filled and Stroked GeneralPath", getSize().width); // side-effect sets font
        AffineTransform oldTrans = g2.getTransform(); // save a copy of the original

        Dimension d = getSize();
        int xTL = marginLeft;
        int yTL = marginTop;
        int xBL = marginLeft;
        int yBL = d.height - marginBottom;
        int xBR = d.width - marginRight;
        int yBR = d.height - marginBottom;
        int rectWidth = xBR - marginLeft;
        int rectHeight = yBR - marginTop;

        // draw border around plot area
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Rectangle2D.Double(xTL, yTL, rectWidth, rectHeight));

        // y-axis tick marks, labels
        double yStep = (yBL - yTL) / (yNumTicks * 1.0);
        for (int i = 0; i <= yNumTicks; i++) {
            int y = new Double(yTL + (i * yStep)).intValue();
            g2.draw(new Line2D.Double(xBL, y, xBL - yTickLength, y));

            String label = Double.toString(Math.round(100 * (1 - 1.0 / yNumTicks * i)) / 100.0);
            g2.drawString(label, xBL - yHorLabOffset, y + yVertLabOffset);
        }

        // x-axis tick marks, labels
        double xStep = (xBR - xBL) / (xNumTicks * 1.0);
        for (int i = 0; i <= xNumTicks; i++) {
            int x = new Double(xBL + (i * xStep)).intValue();
            g2.draw(new Line2D.Double(x, yBL, x, yBL + xTickLength));

            String label = Double.toString(Math.round(100 * 1.0 / xNumTicks * i) / 100.0);
            g2.drawString(label, x - xHorLabOffset, yBL + xVertLabOffset);
        }

        // draw axis labels
        g2.drawString("false positive rate fp/(fp+tn)", xBL, yBL + xVertAxLabOffset);
        g2.rotate(Math.toRadians(90), xBL - yHorAxLabOffset, yTL);
        g2.drawString("true positive rate tp/(tp+fn)", xBL - yHorAxLabOffset, yTL);
        g2.setTransform(oldTrans);

        // draw curve
        g2.setPaint(Color.blue);
        Dimension windowDim = getSize();
        Dimension plotDim = new Dimension(windowDim.width - (marginLeft + marginRight),
                windowDim.height - (marginTop + marginBottom));
        g2.translate(marginLeft, marginTop);
        AffineTransform marginTrans = g2.getTransform();

        // flip y-axis to behave like conventional cartesian by scaling & tranlating
        g2.translate(0, plotDim.height);
        g2.scale(1.0, -1.0);

        // we can't use a transform cause it will effect the stroke, so scale by hand
        double[][] pointsScaled = new double[points.length][2];
        for (int i = 0; i < points.length; i++) {
            pointsScaled[i] = new double[]{points[i][0] * plotDim.width, points[i][1] * plotDim.height};
        }

        double auc = 0.0;
        for (int i = 1; i < points.length; i++) {
            // calc new segment for ROC curve and draw it
            g2.draw(new Line2D.Double(pointsScaled[i - 1][0], pointsScaled[i - 1][1],
                    pointsScaled[i][0], pointsScaled[i][1]));

            // auc calcs
            auc += 0.5 * (points[i][0] - points[i - 1][0]) * (points[i][1] + points[i - 1][1]);
        }

        // print auc
        g2.setTransform(marginTrans);
        auc = Math.round(100 * auc) / 100.0;
        g2.drawString("AUC = " + auc, plotDim.width / 3, plotDim.height / 2);

        g2.setTransform(oldTrans);
    }

    private FontMetrics pickFont(Graphics2D g2, String longString, int xSpace) {
        boolean fontFits = false;
        Font font = g2.getFont();
        FontMetrics fontMetrics = g2.getFontMetrics();
        int size = font.getSize();
        String name = font.getName();
        int style = font.getStyle();

        while (!fontFits) {
            if ((fontMetrics.getHeight() <= maxCharHeight)
                    && (fontMetrics.stringWidth(longString) <= xSpace)) {
                fontFits = true;
            } else {
                if (size <= minFontSize) {
                    fontFits = true;
                } else {
                    g2.setFont(font = new Font(name, style, --size));
                    fontMetrics = g2.getFontMetrics();
                }
            }
        }
        return fontMetrics;
    }
}
