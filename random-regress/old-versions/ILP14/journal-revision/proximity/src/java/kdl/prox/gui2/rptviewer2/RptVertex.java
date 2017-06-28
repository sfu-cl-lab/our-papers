/**
 * $Id: RptVertex.java 3742 2007-11-08 16:45:30Z jkclevel $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;

import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseTree;
import kdl.prox.gui2.RPTNodeView;
import kdl.prox.model2.common.probdistributions.ProbDistribution;

import java.awt.*;

/**
 * A vertex is the JUNG counterpart of a node on the RPT. Contains extra info, such as
 * the distribution of labels, whether it comes from a YES/NO branch, and whether it's a leaf or not.
 */
public class RptVertex extends DirectedSparseVertex {

    Boolean yesChild = null;
    Boolean leaf = null;
    ProbDistribution classLabelDistribution = null;
    String tip = null;

    //Sets dimensions of the vertices icon
    int w = 100;
    int h = 80;
    int barThickness = 15;
    int barPosition = h - barThickness;
    int linesize = 16;
    int lineLength = 13;

    RptColors colorMap;

    /**
     * @param node Creates a vertex from the given node
     */
    public RptVertex(RPTNodeView node) {
        this.yesChild = false;
        this.classLabelDistribution = node.getClassLabelDistributionView();
        this.leaf = node.isLeaf();
        this.tip = node.toString();
        genIconDimensions();
    }

    /**
     * @param node     Creates a vertex from the given node
     * @param yesChild Should be true if a vertex is off a yes branch
     */
    public RptVertex(RPTNodeView node, Boolean yesChild) {
        this.yesChild = yesChild;
        this.classLabelDistribution = node.getClassLabelDistributionView();
        this.leaf = node.isLeaf();
        this.tip = node.toString();
        genIconDimensions();
    }

    public ProbDistribution getClassLabelDistributionView() {
        return classLabelDistribution;
    }

    public boolean isLeaf() {
        return (leaf);
    }

    public boolean isYes() {
        return (yesChild);
    }

    public Dimension getIconSize() {
        return new Dimension(isLeaf() ? w : w + (w / 2), h);
    }

    public int getBarThickness() {
        return barThickness;
    }

    public int getBarPosition() {
        return barPosition;
    }

    private void genIconDimensions() {
        if (leaf) {
            barPosition = 0;
            barThickness *= 2.5;
            lineLength *= 1.2;
            /*checks to see how large each leaf needs to be if we're wrapping text */
            int lines = 0;
            String[] s = distributionString();
            for (String value : s) {
                lines += (value.length() / lineLength);
                if (value.length() % lineLength != 0) {
                    lines++;
                }
            }
            h = barThickness + linesize *
                    Math.max(4, lines);
            w *= 1.2;
        }

    }

    /**
     * Each element of the returned string[] contains
     * a value of the vertex's classLabelDistribution and
     * that given ones count
     *
     * @return an array of strings, each containing one distribution label and value
     */
    private String[] distributionString() {

        Object[] distinctVals = classLabelDistribution.getDistinctValues();
        String[] s = new String[distinctVals.length];
        for (int valIdx = 0; valIdx < distinctVals.length; valIdx++) {
            Object value = distinctVals[valIdx];
            String val = value.toString();
            double prob = classLabelDistribution.getCount(value);
            prob = Math.rint(prob * 100.0) / 100.0;
            if (val != null) {
                s[valIdx] = (val + ": " + prob);
            }

        }
        return s;


    }

    public String toString() {
        String text = "<html>";
        if (!leaf) {
            text += tip;
            text += "<br>";
        }
        String[] s = distributionString();
        for (String value : s) {
            text += (value + "<br>");
        }
        text += "</html>";
        return text;
    }

    public String vertexString() {

        int linesToPrint = (h - barThickness) / linesize;

        String text = "<html>";
        if (leaf) {
            String[] s = distributionString();
            for (String dist : s) {
                for (int pos = 0; pos < dist.length(); pos += lineLength) {
                    if (pos + lineLength > dist.length()) {
                        text = text.concat(dist.substring(pos) + "<br>");
                    } else {
                        text = text.concat(dist.substring(pos, pos + lineLength) + "<br>");
                    }
                }
            }
        } else {
            int lineNum = 0;
            for (int i = 0; i <= tip.length() && lineNum < linesToPrint; i += lineLength) {
                lineNum++;
                if (i + lineLength <= tip.length())
                    text = text.concat(tip.substring(i, i + lineLength) + "<br>");
                else {
                    text = text.concat(tip.substring(i) + "<br>");
                }
            }
        }

        text = text.concat("</html>");
        return text;

    }

    public RptColors getColorMap() {
        if (colorMap == null) {
            return ((RptVertex) ((SparseTree) (this.getGraph())).getRoot()).getColorMap();
        }
        return colorMap;
    }

    public void setColorMap() {
        colorMap = new RptColors(this);
    }


}
