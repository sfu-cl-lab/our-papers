/**
 * $Id: PolarToRectangularHelperTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


public class PolarToRectangularHelperTest extends TestCase {

    private static final Logger log = Logger.getLogger(PolarToRectangularHelperTest.class);
    private static final double DELTA = 0.01;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();   // for log4j
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void checkRectangularCoords(int rho, double theta, double width, double height,
                                        int maxRho, double expX, double expY) {
        double actX = PolarCoordinate.calcX(rho, theta, width, height, maxRho);
        double actY = PolarCoordinate.calcY(rho, theta, width, height, maxRho);
        assertEquals(expX, actX, DELTA);
        assertEquals(expY, actY, DELTA);

    }

    /**
     * Tests individual rho/theta values converted to x,y
     */
    public void testPolarToRectangularCoords() {
        double width = 100.0;
        double height = 100.0;
        checkRectangularCoords(0, 0.0, width, height, 0, 50.0, 50.0);
        checkRectangularCoords(0, 0.0, width, height, 10, 50.0, 50.0); //should remain in center, even if maxRho > 0
        checkRectangularCoords(0, 1.0, width, height, 0, 50.0, 50.0); //should remain in center, even if Theta > 0

        checkRectangularCoords(1, 0, width, height, 1, width, 50.0); // rho=1, maxrho=1, theta 0
        checkRectangularCoords(1, 0, width, height, 2, width * 0.75, 50.0); // rho=1, maxrho=2, theta 0

        checkRectangularCoords(1, Math.PI / 2, width, height, 1, width / 2, 0); // rho=1, maxrho=2, theta pi/2
        checkRectangularCoords(1, Math.PI / 2, width, height, 2, width / 2, height / 4); // rho=1, maxrho=2, theta pi/2

        checkRectangularCoords(1, Math.PI, width, height, 1, 0, height / 2); // rho=1, maxrho=2, theta pi
        checkRectangularCoords(1, Math.PI, width, height, 2, width / 4, height / 2); // rho=1, maxrho=2, theta pi

        checkRectangularCoords(1, Math.PI * 1.5, width, height, 1, width / 2, 100); // rho=1, maxrho=2, theta 1.5 pi
        checkRectangularCoords(1, Math.PI * 1.5, width, height, 2, width / 2, height * 0.75); // rho=1, maxrho=2, theta 1.5 pi

        checkRectangularCoords(1, Math.PI * 2, width, height, 1, width, 50.0); // rho=1, maxrho=1, theta 2pi
        checkRectangularCoords(1, Math.PI * 2, width, height, 2, width * 0.75, 50.0); // rho=1, maxrho=2, theta 2pi

        double expX = (width / 2) + ((width / 2) * Math.cos(Math.PI / 4));
        double expY = (width / 2) - ((width / 2) * Math.sin(Math.PI / 4));
        checkRectangularCoords(1, Math.PI / 4, width, height, 1, expX, expY); // rho=1, maxrho=1, theta 45d

        expX = (width / 2) + ((width / 4) * Math.cos(Math.PI / 4));
        expY = (width / 2) - ((width / 4) * Math.sin(Math.PI / 4));
        checkRectangularCoords(1, Math.PI / 4, width, height, 2, expX, expY); // rho=1, maxrho=2, theta 45d
    }

}
