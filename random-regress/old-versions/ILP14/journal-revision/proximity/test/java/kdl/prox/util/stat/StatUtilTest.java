/**
 * $Id: StatUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */
package kdl.prox.util.stat;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;

public class StatUtilTest extends TestCase {

    private static Logger log = Logger.getLogger(StatUtilTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }


    //don't need db to test
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAverage() throws Exception {
        Double[] list = {new Double(0.1), new Double(0.2), new Double(0.3)};
        double ave = StatUtil.average(list);
        assertEquals(ave, .2, 0.001);

        list = null;
        try {
            ave = StatUtil.average(list);
            assertEquals(1, 0); // i.e. we shouldn't reach this line
        } catch (Exception e) {
            // expected behavior.
            log.info("Got expected exception: " + e);
        }
    }

    /**
     * Tests taken from: http://www.itl.nist.gov/div898/handbook/eda/section3/eda3674.htm
     *
     * @throws Exception
     */
    public void testChiSquareP() throws Exception {
        assertEquals(0.05, StatUtil.chiSquareP(24.996, 15), 0.01);
        assertEquals(0.05, StatUtil.chiSquareP(18.307, 10), 0.01);
    }

    public void testVariance() throws Exception {
        Double[] list = {new Double(0.1), new Double(0.2), new Double(0.3)};

        double var = StatUtil.variance(list);
        assertEquals(var, .02 / 3, 0.001);
    }

    public void testGStat3D() {
        int[][][] counts = new int[2][2][3];
        counts[0][0][0] = 2;
        counts[0][0][1] = 1;
        counts[0][0][2] = 4;

        counts[0][1][0] = 3;
        counts[0][1][1] = 0;
        counts[0][1][2] = 1;

        counts[1][0][0] = 1;
        counts[1][0][1] = 1;
        counts[1][0][2] = 2;

        counts[1][1][0] = 0;
        counts[1][1][1] = 0;
        counts[1][1][2] = 4;

        double g2 = 0;
        g2 += 2 * Math.log(12 / 15.0);
        g2 += 1 * Math.log(1);
        g2 += 4 * Math.log(44 / 30.0);
        g2 += 3 * Math.log(18 / 15.0);
//        g2 += 0 * Math.log(0);
        g2 += 1 * Math.log(11 / 25.0);
        g2 += 1 * Math.log(2);
        g2 += 1 * Math.log(1);
        g2 += 2 * Math.log(22 / 36.0);
//        g2 += 0 * Math.log(/);
//        g2 += 0 * Math.log(/);
        g2 += 4 * Math.log(44 / 30.0);
        g2 *= 2;

        assertEquals(g2, StatUtil.gStatistic(counts), 0.00001);
    }

    public void testGStat3DWithZeroRow() {
        int[][][] counts = new int[2][2][3];
        counts[0][0][0] = 2;
        counts[0][0][1] = 0;
        counts[0][0][2] = 4;

        counts[0][1][0] = 3;
        counts[0][1][1] = 0;
        counts[0][1][2] = 1;

        counts[1][0][0] = 1;
        counts[1][0][1] = 1;
        counts[1][0][2] = 2;

        counts[1][1][0] = 0;
        counts[1][1][1] = 1;
        counts[1][1][2] = 4;

        double g2 = 0;
        g2 += 2 * Math.log(12 / 15.0);
        // g2 += 0 * Math.log(0);
        g2 += 3 * Math.log(18 / 15.0);
        g2 += 1 * Math.log(2);

        // g2 += 0 * Math.log(0);
        // g2 += 0 * Math.log(0);
        g2 += 1 * Math.log(1);
        g2 += 1 * Math.log(1);


        g2 += 4 * Math.log(44 / 30.0);
        g2 += 1 * Math.log(11 / 25.0);
        g2 += 2 * Math.log(22 / 36.0);
        g2 += 4 * Math.log(44 / 30.0);
        g2 *= 2;

        assertEquals(g2, StatUtil.gStatistic(counts), 0.00001);
    }

    public void testGStat3DWithZeroMarginal() {
        int[][][] counts = new int[2][2][3];
        counts[0][0][0] = 0;
        counts[0][1][0] = 0;
        counts[1][0][0] = 1;
        counts[1][1][0] = 5;

        counts[0][0][1] = 0;
        counts[0][1][1] = 0;
        counts[1][0][1] = 8;
        counts[1][1][1] = 3;

        counts[0][0][2] = 0;
        counts[0][1][2] = 0;
        counts[1][0][2] = 2;
        counts[1][1][2] = 4;

        /*
        Because of the zero marginals, gStat MUST be 0 --no correlations
         */
        double g2 = 0;
        log.info(StatUtil.gStatistic(counts));
        assertEquals(g2, StatUtil.gStatistic(counts), 0.00001);
    }


    public void testGstat3DWithNoZ() {
        int[][][] counts3D = new int[2][2][1];

        counts3D[0][0][0] = 7;
        counts3D[0][1][0] = 4;
        counts3D[1][0][0] = 4;
        counts3D[1][1][0] = 4;

        double[][] counts2D = new double[2][2];

        counts2D[0][0] = 7;
        counts2D[0][1] = 4;
        counts2D[1][0] = 4;
        counts2D[1][1] = 4;

        double stat1 = StatUtil.gStatistic(counts3D);
        double stat2 = StatUtil.gStatistic(counts2D);

        log.info("3D:" + stat1 + " 2D:" + stat2);

        assertEquals(stat1, stat2, 0.0000001);

    }

    public void testInverseNormalCDF() {
        assertEquals(0.0, StatUtil.inverseStandardNormalCDF(0.5), 0.00001);
        assertEquals(1.5, StatUtil.inverseStandardNormalCDF(0.9332), 0.0001);
        assertEquals(1.64, StatUtil.inverseStandardNormalCDF(0.9495), 0.0001);
        assertEquals(-2.91, StatUtil.inverseStandardNormalCDF(0.00181), 0.001);
        assertEquals(-2.91, StatUtil.inverseStandardNormalCDF(0.00181), 0.001);
    }

    public void testGetCriticalValue() throws Exception {

        double pVal = StatUtil.chiSquareP(7.0, 5);
        log.info("Inital pVal: " + pVal);
        double bestGuess = StatUtil.inverseChiSquareCDF(pVal, 5);
        assertEquals(7.0, bestGuess, 0.01);

        pVal = StatUtil.chiSquareP(15.25, 10);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(pVal, 10);
        assertEquals(15.25, bestGuess, 0.01);

        assertEquals(0.01, StatUtil.chiSquareP(9.210141451743192, 2), 0.0001);

        pVal = StatUtil.chiSquareP(9.5, 1);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(pVal, 1);
        assertEquals(9.5, bestGuess, 0.01);

        pVal = StatUtil.chiSquareP(3456, 3456);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(0.05, 3456);
        log.info("Found pVal: " + StatUtil.chiSquareP(bestGuess, 3456));
        assertEquals(3593.87733850047, bestGuess, 0.01);

        pVal = StatUtil.chiSquareP(4608, 4608);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(0.05, 4608);
        log.info("Found pVal: " + StatUtil.chiSquareP(bestGuess, 4608));
        assertEquals(4767.035670680271, bestGuess, 0.001);

        pVal = StatUtil.chiSquareP(12419.687702633044, 12288);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(0.2, 12288);
        log.info("Found pVal: " + StatUtil.chiSquareP(bestGuess, 12288));
        assertEquals(12419.687702633044, bestGuess, 0.001);

        pVal = StatUtil.chiSquareP(27845.632520204977, 27648);
        log.info("Inital pVal: " + pVal);
        bestGuess = StatUtil.inverseChiSquareCDF(0.2, 27648);
        log.info("Found pVal: " + StatUtil.chiSquareP(bestGuess, 27648));
        assertEquals(27845.632520204977, bestGuess, 0.001);
    }

    /**
     * Tests from:
     * <p/>
     * J. Cohen. Quantitative methods in psychology: A power primer.
     * Psychological Bulletin, 112(1):155–159, 1992.
     * <p/>
     * Additional Tests from:
     * <p/>
     * Cohen, J. Statistical power analysis for the behavioral sciences,
     * 2nd ed. Lawrence Erlbaum Associates, Inc., 1988. (chapter 7)
     */
    public void testPower() throws Exception {

        assertEquals(0.80, StatUtil.power(0.01, 40, 0.5, 1), 0.1);
        assertEquals(0.80, StatUtil.power(0.01, 130, 0.30, 1), 0.1);
        assertEquals(0.80, StatUtil.power(0.01, 1168, 0.10, 1), 0.1);

        assertEquals(0.80, StatUtil.power(0.01, 55, 0.5, 2), 0.1);
        assertEquals(0.80, StatUtil.power(0.01, 199, 0.30, 5), 0.1);
        assertEquals(0.80, StatUtil.power(0.01, 1546, 0.10, 3), 0.1);

        assertEquals(0.80, StatUtil.power(0.05, 964, 0.10, 2), 0.1);
        assertEquals(0.80, StatUtil.power(0.05, 39, 0.5, 2), 0.1);
        assertEquals(0.80, StatUtil.power(0.05, 133, 0.3, 2), 0.1);

        assertEquals(0.80, StatUtil.power(0.1, 618, 0.1, 1), 0.1);
        assertEquals(0.80, StatUtil.power(0.1, 124, 0.3, 6), 0.1);
        assertEquals(0.80, StatUtil.power(0.1, 42, 0.5, 5), 0.1);

        assertEquals(0.97, StatUtil.power(0.01, 25, 0.9, 1), 0.1);
        assertEquals(0.89, StatUtil.power(0.05, 45, 0.6, 3), 0.1);
        assertEquals(0.10, StatUtil.power(0.05, 50, 0.20, 16), 0.1);

        assertEquals(0.5557557222985495, StatUtil.power(0.2, 2000, 0.1, 192), 0.1);

    }

}
