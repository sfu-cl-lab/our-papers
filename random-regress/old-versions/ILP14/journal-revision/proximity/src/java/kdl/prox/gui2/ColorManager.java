/**
 * $Id: ColorManager.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ColorManager assigns a unique color to each name.  It attempts to
 * choose a set of colors that are far apart from one another in the
 * color spectrum.
 */
public class ColorManager {

    private Color[] colors;
    private List names;
    private float total;


    public ColorManager(String[] uniqueNames) {
        Arrays.sort(uniqueNames);
        names = new ArrayList(uniqueNames.length);
        total = uniqueNames.length;
        colors = new Color[uniqueNames.length];
        for (int i = 0; i < uniqueNames.length; i++) {
            Color color = getColor(i);
            names.add(i, uniqueNames[i]);
            colors[i] = color;
        }
    }

    private Color getColor(int i) {
        float r = 0;
        float g = 0;
        float b = 0;
        if (i % 3 == 1) {
            r = 1 - ((i - 1) / total);
        } else if (i % 3 == 2) {
            g = 1 - ((i - 2) / total);
        } else {
            b = 1 - (i / total);
        }
        return new Color(r, g, b);
    }

    /**
     * @param name
     * @return Color for name. returns null if not found
     */
    public Color getColor(String name) {
        int nameIdx = names.indexOf(name);
        if (nameIdx == -1) {
            return null;
        } else {
            return colors[nameIdx];
        }
    }

    public ColorLegend getColorLegend() {
        String[] names = getNames();
        Color[] colors = new Color[names.length];
        for (int i = 0; i < names.length; i++) {
            colors[i] = getColor(names[i]);
        }

        return new ColorLegend(colors, names);
    }

    public String[] getNames() {
        return (String[]) names.toArray(new String[]{});
    }


}
