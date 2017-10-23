/**
 * $Id: RDNLayoutTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import java.awt.geom.Point2D;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


public class RDNLayoutTest extends TestCase {

    private static final Logger log = Logger.getLogger(RDNLayoutTest.class);
    private static final int CIRCLE_DIAM = 50;
    private static final int X_SPACING = 20;
    private static final int Y_SPACING = 30;


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private PNode makeTestCircle() {
        return PPath.createEllipse(0, 0, CIRCLE_DIAM - 1, CIRCLE_DIAM - 1);
    }

    public void test1NodeInLayer() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        pLayer.addChild(makeTestCircle());

        List actPoint2Ds = PNodeLayout.calcGridLayout(pLayer,
                pLayer.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    public void test2NodesInLayer() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(0, CIRCLE_DIAM + Y_SPACING),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        pLayer.addChild(makeTestCircle());
        pLayer.addChild(makeTestCircle());

        List actPoint2Ds = PNodeLayout.calcGridLayout(pLayer,
                pLayer.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    public void test2Rects1CircleEach() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(0, CIRCLE_DIAM + Y_SPACING),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        final PNode rect1 = PPath.createRectangle(0, 0, 1, 1);   // arbitrary values
        final PNode rect2 = PPath.createRectangle(0, 0, 1, 1);   // arbitrary values
        pLayer.addChild(rect1);
        pLayer.addChild(rect2);

        PNode circle1 = makeTestCircle();
        PNode circle2 = makeTestCircle();
        rect1.addChild(circle1);
        rect2.addChild(circle2);

        List actPoint2Ds = PNodeLayout.calcGridLayout(pLayer,
                pLayer.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    public void test3NodesInLayer() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(CIRCLE_DIAM + X_SPACING, 0),
            new Point2D.Double(0, CIRCLE_DIAM + Y_SPACING),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        pLayer.addChild(makeTestCircle());
        pLayer.addChild(makeTestCircle());
        pLayer.addChild(makeTestCircle());

        List actPoint2Ds = PNodeLayout.calcGridLayout(pLayer,
                pLayer.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    public void test4NodesInRect() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(CIRCLE_DIAM + X_SPACING, 0),
            new Point2D.Double(0, CIRCLE_DIAM + Y_SPACING),
            new Point2D.Double(CIRCLE_DIAM + X_SPACING, CIRCLE_DIAM + Y_SPACING),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        PNode parentRect = PPath.createRectangle(0, 0, 1, 1);   // arbitrary values
        pLayer.addChild(parentRect);

        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());

        List actPoint2Ds = PNodeLayout.calcGridLayout(parentRect,
                parentRect.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    public void test5NodesInRect() {
        Point2D.Double[] expPoint2Ds = new Point2D.Double[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(CIRCLE_DIAM + X_SPACING, 0),
            new Point2D.Double(0, CIRCLE_DIAM + Y_SPACING),
            new Point2D.Double(CIRCLE_DIAM + X_SPACING, CIRCLE_DIAM + Y_SPACING),
            new Point2D.Double(0, (CIRCLE_DIAM * 2) + Y_SPACING),
        };

        PCanvas pCanvas = new PCanvas();
        PLayer pLayer = pCanvas.getLayer();
        PNode parentRect = PPath.createRectangle(0, 0, 1, 1);   // arbitrary values
        pLayer.addChild(parentRect);

        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());
        parentRect.addChild(makeTestCircle());

        List actPoint2Ds = PNodeLayout.calcGridLayout(parentRect,
                parentRect.getChildrenReference(), X_SPACING, Y_SPACING);
        validatePoint2Ds(expPoint2Ds, actPoint2Ds);
    }

    private void validatePoint2Ds(Point2D.Double[] expPoint2Ds, List actPoint2Ds) {
//        log.warn("\n\t" + Arrays.asList(expPoint2Ds) + "\n\t" + actPoint2Ds);

        final double DELTA = 0.05;
        assertEquals(expPoint2Ds.length, actPoint2Ds.size());

        for (int pointIdx = 0; pointIdx < expPoint2Ds.length; pointIdx++) {
            Point2D.Double expPoint2D = expPoint2Ds[pointIdx];
            Point2D.Double actPoint2D = (Point2D.Double) actPoint2Ds.get(pointIdx);
            assertEquals(expPoint2D.getX(), actPoint2D.getX(), DELTA);
            assertEquals(expPoint2D.getY(), actPoint2D.getY(), DELTA);
        }
    }

}
