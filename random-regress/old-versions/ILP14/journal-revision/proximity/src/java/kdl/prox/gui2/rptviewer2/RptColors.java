package kdl.prox.gui2.rptviewer2;

import kdl.prox.model2.common.probdistributions.ProbDistribution;

import java.awt.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Jeff Cleveland
 * Date: Aug 27, 2007
 * Time: 2:56:07 PM
 */
public class RptColors {

    HashMap colorMap = new HashMap(); // shared by all instances, to use a consistent set of colors -> labels mapping


    public RptColors(RptVertex r) {
        Color[] DISC_COLORS = new Color[]{
                Color.red, Color.blue, Color.green, Color.yellow, Color.cyan,
                Color.magenta, Color.pink, Color.orange};

        Object[] distinctVals = r.getClassLabelDistributionView().getDistinctValues();
        for (int valIdx = 0; valIdx < distinctVals.length; valIdx++) {
            Object value = distinctVals[valIdx];
            colorMap.put(value, DISC_COLORS[valIdx]);
        }

    }

    public void setColorMap(ProbDistribution probDist) {

        Color[] DISC_COLORS = new Color[]{
                Color.red, Color.blue, Color.green, Color.yellow, Color.cyan,
                Color.magenta, Color.pink, Color.orange};

        Object[] distinctVals = probDist.getDistinctValues();
        for (int valIdx = 0; valIdx < distinctVals.length; valIdx++) {
            Object value = distinctVals[valIdx];
            colorMap.put(value, DISC_COLORS[valIdx]);
        }

    }

    public Color getColor(Object value) {
        Object color = colorMap.get(value);
        if (color == null)
            return Color.black;
        else
            return (Color) color;
    }

}
