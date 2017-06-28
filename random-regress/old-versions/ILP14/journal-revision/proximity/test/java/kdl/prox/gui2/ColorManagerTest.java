/**
 * $Id: ColorManagerTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.gui2;

import java.awt.Color;
import junit.framework.TestCase;

public class ColorManagerTest extends TestCase {

    private static double PRECISION = 0.0001;
    private float[] colors = null;


    public void testIt() {
        String[] values = {"o1", "o2", "o3", "o4", "o5", "o6"};
        ColorManager manager = new ColorManager(values);

        Color color = manager.getColor(values[0]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(1, colors[2], PRECISION);

        color = manager.getColor(values[1]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(1, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

        color = manager.getColor(values[2]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(1, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

        color = manager.getColor(values[3]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(1 - (3 / 6.0), colors[2], PRECISION);

        color = manager.getColor(values[4]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(1 - (3 / 6.0), colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

        color = manager.getColor(values[5]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(1 - (3 / 6.0), colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);
    }


    public void testAgain() {
        String[] values = {"o1", "o2", "o3", "o4", "o5"};
        ColorManager manager = new ColorManager(values);

        Color color = manager.getColor(values[0]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(1, colors[2], PRECISION);

        color = manager.getColor(values[1]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(1, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

        color = manager.getColor(values[2]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(1, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

        color = manager.getColor(values[3]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(0, colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(1 - (3 / 5.0), colors[2], PRECISION);

        color = manager.getColor(values[4]);
        colors = color.getRGBColorComponents(colors);
        assertEquals(1 - (3 / 5.0), colors[0], PRECISION);
        assertEquals(0, colors[1], PRECISION);
        assertEquals(0, colors[2], PRECISION);

    }


}
