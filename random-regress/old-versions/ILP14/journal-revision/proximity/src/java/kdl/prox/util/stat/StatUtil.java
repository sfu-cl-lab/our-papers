/**
 * $Id: StatUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util.stat;

import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * StatUtil contains various useful static statistical utility methods for modeling.
 */
public final class StatUtil {

    private static Logger log = Logger.getLogger(StatUtil.class);

    /**
     * Random instance used in various methods.
     */
    private static Random r = new Random();

    public static final double sqRoot5 = Math.pow(5, 0.5);
    public static final double sqRoot2Pi = Math.pow((2 * Math.PI), 0.5);
    private static double epsilon = 0.000001;

    /**
     * Doesn't let anyone instantiate this class.
     */
    private StatUtil() {
    }

    /**
     * Computes Cramer's V statistic for non-square tables. This is similar to phi for square tables.
     * r is defined as the min(|columns|, |rows|).
     *
     * @param gStat
     * @param n
     * @param r
     * @return
     */
    public static double cramerV(double gStat, double n, double r) {
        return Math.sqrt(gStat / (n * (r - 1)));
    }

    /**
     * Utility method to compute the maximum dof for which a test of suitable power
     * for the given effect size can be run.
     *
     * @param sampleSize
     * @param effectSize
     * @param powerThreshold
     * @param pVal
     * @return
     */
    public static long dofThresholdForEffect(double sampleSize, double effectSize, double powerThreshold, double pVal) {
        double power = 1.0;
        long currDof = 0;
        while (power > powerThreshold) {
            currDof++;
            try {
                power = StatUtil.power(pVal, sampleSize, effectSize, currDof);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return currDof - 1;
    }

    /**
     * Method to compare two doubles for equality.  Note:
     * value == otherValue
     * can sometimes evaluate to false even when the numbers are equal
     * because of the limited precision of floating point arithmetic.
     *
     * @param value
     * @param otherValue
     * @return true if value equals otherValue
     */
    public static boolean equalDoubles(double value, double otherValue) {
        return equalDoubles(value, otherValue, epsilon);
    }

    public static boolean equalDoubles(double value, double otherValue, double epsilon) {
        return Math.abs(value - otherValue) < epsilon;
    }


    public static double inverseChiSquareCDF(double alpha, long dof) throws Exception {
        return inverseChiSquareCDF(alpha, dof, epsilon);
    }

    /**
     * DOUBLE PRECISION FUNCTION PPND16 (P, IFAULT)
     * <p/>
     * http://lib.stat.cmu.edu/apstat/241
     * <p/>
     * ALGORITHM AS241  APPL. STATIST. (1988) VOL. 37, NO. 3
     * <p/>
     * Produces the normal deviate Z corresponding to a given lower
     * tail area of P; Z is accurate to about 1 part in 10**16.
     * <p/>
     * The hash sums below are the sums of the mantissas of the
     * coefficients.   They are included for use in checking
     * transcription.
     *
     * @param P = cdf value to invert.
     */
    public static double inverseStandardNormalCDF(double P) {
        double x = 0.0;

        double ZERO = 0.0;
        double ONE = 1.0;
        double HALF = 0.5;
        double SPLIT1 = 0.425, SPLIT2 = 5.0;
        double CONST1 = 0.180625, CONST2 = 1.6;

        // Coefficients for P close to 0.5

        double A0 = 3.3871328727963666080;
        double A1 = 133.14166789178437745;
        double A2 = 1971.5909503065514427;
        double A3 = 13731.693765509461125;
        double A4 = 45921.953931549871457;
        double A5 = 67265.770927008700853;
        double A6 = 33430.575583588128105;
        double A7 = 2509.0809287301226727;
        double B1 = 42.313330701600911252;
        double B2 = 687.18700749205790830;
        double B3 = 53941.960214247511077;
        double B4 = 21213.794301586595867;
        double B5 = 39307.895800092710610;
        double B6 = 28729.085735721942674;
        double B7 = 5226.4952788528545610;
        //HASH SUM AB    55.88319 28806 14901 4439
        //Coefficients for P not close to 0, 0.5 or 1.
        double C0 = 1.42343711074968357734;
        double C1 = 4.63033784615654529590;
        double C2 = 5.76949722146069140550;
        double C3 = 3.64784832476320460504;
        double C4 = 1.27045825245236838258;
        double C5 = .241780725177450611770;
        double C6 = .0227238449892691845833;
        double C7 = .000774545014278341407640;
        double D1 = 2.05319162663775882187;
        double D2 = 1.67638483018380384940;
        double D3 = .689767334985100004550;
        double D4 = .148103976427480074590;
        double D5 = .0151986665636164571966;
        double D6 = .000547593808499534494600;
        double D7 = .00000000105075007164441684324;
        //HASH SUM CD    49.33206 50330 16102 89036
        //Coefficients for P near 0 or 1.
        double E0 = 6.65790464350110377720;
        double E1 = 5.46378491116411436990;
        double E2 = 1.78482653991729133580;
        double E3 = .296560571828504891230;
        double E4 = .0265321895265761230930;
        double E5 = .00124266094738807843860;
        double E6 = .0000271155556874348757815;
        double E7 = .000000201033439929228813265;
        double F1 = .599832206555887937690;
        double F2 = .136929880922735805310;
        double F3 = .0148753612908506148525;
        double F4 = .000786869131145613259100;
        double F5 = .0000184631831751005468180;
        double F6 = .000000142151175831644588870;
        double F7 = .00000000000000204426310338993978564;
        //HASH SUM EF    47.52583 31754 92896 71629

        double Q = P - HALF;
        if (Math.abs(Q) <= SPLIT1) {
            double R = CONST1 - Q * Q;
            x = Q * (((((((A7 * R + A6) * R + A5) * R + A4) * R + A3)
                    * R + A2) * R + A1) * R + A0) /
                    (((((((B7 * R + B6) * R + B5) * R + B4) * R + B3)
                            * R + B2) * R + B1) * R + ONE);
        } else {
            double R = 0.0;
            if (Q < ZERO) {
                R = P;
            } else {
                R = ONE - P;
            }
            if (R <= ZERO) {
                x = ZERO;
                //should throw exception here.
            }
            R = Math.sqrt(-Math.log(R));
            if (R <= SPLIT2) {
                R = R - CONST2;
                x = (((((((C7 * R + C6) * R + C5) * R + C4) * R + C3)
                        * R + C2) * R + C1) * R + C0) /
                        (((((((D7 * R + D6) * R + D5) * R + D4) * R + D3)
                                * R + D2) * R + D1) * R + ONE);
            } else {
                R = R - SPLIT2;
                x = (((((((E7 * R + E6) * R + E5) * R + E4) * R + E3)
                        * R + E2) * R + E1) * R + E0) /
                        (((((((F7 * R + F6) * R + F5) * R + F4) * R + F3)
                                * R + F2) * R + F1) * R + ONE);
            }
            if (Q < ZERO) x = -x;
        }

        return x;
    }

    public static double nextDouble() {
        return r.nextDouble();
    }

    public static double nextGaussian() {
        return r.nextGaussian();
    }

    public static int nextInt() {
        return r.nextInt();
    }


    /**
     * From http://people.scs.fsu.edu/~burkardt/m_src/prob/chi_square_cdf_inv.m
     * <p/>
     * //// CHI_SQUARE_CDF_INV inverts the Chi squared PDF.
     * //
     * //  Modified:
     * //
     * //    11 October 2004
     * //
     * //  Reference:
     * //
     * //    Best and Roberts,
     * //    The Percentage Points of the Chi-Squared Distribution,
     * //    Algorithm AS 91,
     * //    Applied Statistics,
     * //    Volume 24, Number ?, pages 385-390, 1975.
     * //
     * //  Parameters:
     * //
     * //    Input, real CDF, a value of the chi-squared cumulative
     * //    probability density function.
     * //    0.000002 <= CDF <= 0.999998.
     * //
     * //    Input, real A, the parameter of the chi-squared
     * //    probability density function.  0 < A.
     * //
     * //    Output, real X, the value of the chi-squared random deviate
     * //    with the property that the probability that a chi-squared random
     * //    deviate with parameter A is less than or equal to PPCHI2 is P.
     * //
     * //
     *
     * @param alpha
     * @param dof
     * @param epsilon
     * @return
     * @throws Exception
     */
    public static double inverseChiSquareCDF(double alpha, long dof, double epsilon) throws Exception {
        double cdf = 1 - alpha;
        double ch = 0.0;

        double aa = 0.6931471806;
        double c1 = 0.01;
        double c2 = 0.222222;
        double c3 = 0.32;
        double c4 = 0.4;
        double c5 = 1.24;
        double c6 = 2.2;
        double c7 = 4.67;
        double c8 = 6.66;
        double c9 = 6.73;
        double c10 = 13.32;
        double c11 = 60.0;
        double c12 = 70.0;
        double c13 = 84.0;
        double c14 = 105.0;
        double c15 = 120.0;
        double c16 = 127.0;
        double c17 = 140.0;
        double c18 = 175.0;
        double c19 = 210.0;
        double c20 = 252.0;
        double c21 = 264.0;
        double c22 = 294.0;
        double c23 = 346.0;
        double c24 = 420.0;
        double c25 = 462.0;
        double c26 = 606.0;
        double c27 = 672.0;
        double c28 = 707.0;
        double c29 = 735.0;
        double c30 = 889.0;
        double c31 = 932.0;
        double c32 = 966.0;
        double c33 = 1141.0;
        double c34 = 1182.0;
        double c35 = 1278.0;
        double c36 = 1740.0;
        double c37 = 2520.0;
        double c38 = 5040.0;
        double cdf_max = 0.999998;
        double cdf_min = 0.000002;
        double e = epsilon;
        double it_max = 25;

        if (cdf < cdf_min || cdf > cdf_max) {
            log.warn("cdf out of range");
        }


        double xx = 0.5 * dof;
        double c = xx - 1.0;

        //  Compute Log ( Gamma ( A/2 ) ).

        double g = gammaLn(dof / 2.0);

        //  Starting approximation for small chi-squared.

        if (dof < -c5 * Math.log(cdf)) {
            ch = Math.pow(cdf * xx * Math.exp(g + xx * aa), 1.0 / xx);

            if (ch < epsilon) {
                return ch;
            }
            // Starting approximation for A less than or equal to 0.32.
        } else if (dof <= c3) {

            ch = c4;
            double a2 = Math.log(1.0 - cdf);

            while (true) {

                double q = ch;
                double p1 = 1.0 + ch * (c7 + ch);
                double p2 = ch * (c9 + ch * (c8 + ch));

                double t = -0.5 + (c7 + 2.0 * ch) / p1 - (c9 + ch * (c10 + 3.0 * ch)) / p2;

                ch = ch - (1.0 - Math.exp(a2 + g + 0.5 * ch + c * aa) * p2 / p1) / t;

                if (Math.abs(q / ch - 1.0) <= c1) {
                    break;
                }

            }
            //
            //  Call to algorithm AS 111.
            //  Note that P has been tested above.
            //  AS 241 could be used as an alternative.
            //
        } else {

            double x2 = inverseStandardNormalCDF(cdf);
            //
            //  Starting approximation using Wilson and Hilferty estimate.
            //
            double p1 = c2 / dof;
            ch = dof * Math.pow((x2 * Math.sqrt(p1) + 1.0 - p1), 3);
            //
            //  Starting approximation for P tending to 1.
            //
            if (c6 * dof + 6.0 < ch) {
                ch = -2.0 * (Math.log(1.0 - cdf) - c * Math.log(0.5 * ch) + g);
            }

        }
        //
        //  Call to algorithm AS 239 and calculation of seven term Taylor series.
        //

        for (int i = 1; i <= it_max; i++) {
            double q = ch;
            double p1 = 0.5 * ch;
            double p2 = cdf - gammaIncomplete(xx, p1);
            double t = p2 * Math.exp(xx * aa + g + p1 - c * Math.log(ch));
            double b = t / ch;
            double a2 = 0.5 * t - b * c;

            double s1 = (c19 + a2 * (c17 + a2 * (c14 + a2 * (c13 + a2 * (c12 + a2 * c11))))) / c24;

            double s2 = (c24 + a2 * (c29 + a2 * (c32 + a2 * (c33 + a2 * c35)))) / c37;

            double s3 = (c19 + a2 * (c25 + a2 * (c28 + a2 * c31))) / c37;

            double s4 = (c20 + a2 * (c27 + a2 * c34) + c * (c22 + a2 * (c30 + a2 * c36))) / c38;

            double s5 = (c13 + c21 * a2 + c * (c18 + c26 * a2)) / c37;

            double s6 = (c15 + c * (c23 + c16 * c)) / c38;

            ch = ch + t * (1.0 + 0.5 * t * s1 - b * c * (s1 - b * (s2 - b * (s3 - b * (s4 - b * (s5 - b * s6))))));

            if (e < Math.abs(q / ch - 1.0)) {
                return ch;
            }

        }

        //Didn't finish within ITMAX, check to see how close we are
        double newPVal = chiSquareP(ch, dof);
        if (!equalDoubles(alpha, newPVal, epsilon)) {
            log.warn("Chi-square didn't converge. Returning anyways because usually this ispretty close.");
        }
        //throw new Exception("InverseChiSquareCDF didn't converge: Either a is too large, or ITMAX is too small.");
        return ch;
    }

    /**
     * Returns a double rounded to i decimal places.
     */
    public static double roundDouble(double num, int i) {
        double mul = Math.pow(10, (double) i);
        double result = (double) (Math.round(num * mul));
        result /= mul;
        return result;
    }

    /**
     * Returns the degrees of freedom associated with the given contingency table.
     */
    public static int degreesOfFreeedom(double[][] table) {
        if (table.length == 0) return 0;
        int result = (table.length - 1) * (table[0].length - 1);
        return result;
    }

    /**
     * Returns the g-statistic calculated on the given contingency table.
     */
    public static double gStatistic(double[][] table) {

        int rowCount = table.length;
        if (rowCount == 0) return 0;
        int colCount = table[0].length;

        double[] rowMargins = new double[rowCount];
        for (int i = 0; i < rowMargins.length; i++)
            rowMargins[i] = 0;
        double[] colMargins = new double[colCount];
        for (int j = 0; j < colMargins.length; j++)
            colMargins[j] = 0;
        double n = 0;
        double g = 0;

        //calculate marginal sums
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                rowMargins[i] += table[i][j];
                colMargins[j] += table[i][j];
                n += table[i][j];
            }
        }

        //double values can be arbitrarily small and still produce significant g-stats
        //for now we'll hack the code to return 0 if the total weight is less than 5
        //but need to figure out to deal with this correctly in the future
        /*
        if( n<5.0 ){
            cat.debug("jneville: total sample size too small for g-statistic " + n);
            return 0.0;
        }
        */

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                double tmp1 = safeDivide(rowMargins[i] * colMargins[j], n);
                double tmp2 = safeDivide(table[i][j], tmp1);
                g += table[i][j] * safeLog(tmp2);
            }
        }

        return 2 * g;
    }

    /**
     * G^2 statistic is defined as:
     * G^2 = 2 * \sum_{ijk} N_{ijk} ln [(N_{ijk} * N_{k})/(N_{ik} * N_{jk})]
     * where
     * N_{ijk} is the value stored at counts[i][j][k] and something like
     * N_{k} is equal to \sum_{ij} N_{ijk}.
     * <p/>
     * See Spirtes, Glymour, Scheines, 2000
     *
     * @param counts
     * @return g-statistic
     */
    public static double gStatistic(int[][][] counts) {
        //
        int x = 0;
        int aSize = counts.length;
        int bSize = counts[0].length;
        int cSize = counts[0][0].length;

        int[][] n_ik = new int[aSize][cSize];
        int[][] n_jk = new int[bSize][cSize];
        int[] n_k = new int[cSize];

        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i].length; j++) {
                for (int k = 0; k < counts[i][j].length; k++) {
                    n_ik[i][k] += counts[i][j][k];
                    n_jk[j][k] += counts[i][j][k];
                    n_k[k] += counts[i][j][k];
                }
            }
        }


        double g = 0.0;
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i].length; j++) {
                for (int k = 0; k < counts[i][j].length; k++) {
//                    log.debug(i + " " + j + " " + k + " " +
//                            counts[i][j][k] + " " + n_k[k] + " " + n_ik[i][k] +
//                    " " + n_jk[j][k]);
                    g += counts[i][j][k] *
//                            safeLog(safeDivide(
//                                    (double) (counts[i][j][k] * n_k[k]),
//                                    (double) (n_ik[i][k] * n_jk[j][k])));
                            (
                                    (safeLog(counts[i][j][k]) + safeLog(n_k[k])) -
                                            (safeLog(n_ik[i][k]) + safeLog(n_jk[j][k]))
                            );
                }
            }
        }

        g *= 2;
        Assert.condition(!Double.isNaN(g), "double is NaN");
        return g;
    }

    /**
     * Returns the p-value associated with <x> using a chi-square distribution
     * with <dof>.
     */
    public static double chiSquareP(double x, long dof) {
        Assert.condition(!Double.isNaN(x), "double is NaN");
        double result = 1.0;
        if (dof == 0) return result;
        if (!equalDoubles(x, 0)) {
            if (x < 0) x *= -1.0;

            try {
                result = 1.0 - gammaIncomplete((dof * 0.5), (x * 0.5));
            } catch (Exception e) {
                log.debug("Error in chiSquareP: " + e + "  x=" + x + ",dof=" + dof);
            }
        }
        return result;
    }

    /**
     * Returns Pearson's corrected contingency coefficient.
     * From: Applied Statistics by Lothar Sachs, pp.482-3
     */
    public static double contingencyCoefficient(double chi2, double n, int r, int c) {
        //cat.debug("chi^2=" + chi2 + " n=" + n + " r=" + r + " c=" + c);
        double cc = Math.sqrt(chi2 / (n + chi2));
        double ccMax;
        if (r < c)
            ccMax = Math.sqrt((r - 1) / (double) r);
        else
            ccMax = Math.sqrt((c - 1) / (double) c);
        double ccCorr = cc / ccMax;
        return ccCorr;
    }

    /**
     * Return the maximum value from a list of values.
     *
     * @param values
     * @return
     */
    public static double maximumValue(double[] values) {
        double maxVal = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > maxVal) {
                maxVal = values[i];
            }
        }
        return maxVal;
    }

    /**
     * Return the minimum value from a list of values.
     *
     * @param values
     * @return
     */
    public static double minimumValue(double[] values) {
        double minVal = Double.POSITIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < minVal) {
                minVal = values[i];
            }
        }
        return minVal;
    }

    /**
     * Utilizes the power approximation supplied in:
     * G.ÊW. Milligan. A computer program for calculating power of the chi-square test.
     * Educational and Psychological Measurement, 39(3):681Ð684, 1979.
     * <p/>
     * lambda = effectSize^2 * sampleSize (from Cohen Chapter 12).
     *
     * @param alpha
     * @param sampleSize
     * @param effectSize
     * @param dof
     * @return
     */
    public static double power(double alpha, double sampleSize, double effectSize, long dof) throws Exception {
        double x = inverseChiSquareCDF(alpha, dof);
        double lambda = Math.pow(effectSize, 2) * sampleSize;

        double factor = 2 * (dof + (2 * lambda)) / (9 * Math.pow(dof + lambda, 2));

        double top = Math.pow((x / (dof + lambda)), 0.333333) - 1 + (factor);
        double zz = top / Math.sqrt(factor);
        double z = Math.abs(zz);

        double c1 = 0.196854;
        double c2 = 0.115194;
        double c3 = 0.000344;
        double c4 = 0.019527;


        double power = 0.5 / Math.pow((1 + (c1 * z) + (c2 * Math.pow(z, 2)) + (c3 * Math.pow(z, 3)) + (c4 * Math.pow(z, 4))), 4);
        if (Double.compare(zz, 0.0) < 0) {
            power = 1 - power;
        }
        return power;

    }

    /**
     * signed g statistic from prox 2
     *
     * @param table
     * @return
     */
    public static double signedGStatistic(double[][] table) {
        if (table.length != 2 || table[0].length != 2) {
            System.err.println("Error in signedGStatistic: table size wrong");
            return -199.9;
        }

        int rowCount = table.length;
        int colCount = table[0].length;
        double[] rowMargins = new double[rowCount];
        for (int i = 0; i < rowMargins.length; i++)
            rowMargins[i] = 0;
        double[] colMargins = new double[colCount];
        for (int j = 0; j < colMargins.length; j++)
            colMargins[j] = 0;
        double n = 0;
        double g = 0;

        //calculate marginal sums
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                rowMargins[i] += table[i][j];
                colMargins[j] += table[i][j];
                n += table[i][j];
            }
        }
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                double tmp1 = safeDivide(rowMargins[i] * colMargins[j], n);
                double tmp2 = safeDivide(table[i][j], tmp1);
                g += table[i][j] * safeLog(tmp2);
            }
        }

        //find mass to get sign
        double mass = (table[0][0] + table[1][1]) / n;
        int sign = mass > 0.5 ? +1 : -1;
        //return signed g
        return 2 * g * sign;
    }

    /**
     * Returns zero if x is zero, otherwise, returns ln x.
     */
    public static double safeLog(double x) {
        double result = 0;
        if (!equalDoubles(x, 0)) result = Math.log(x);
        return result;
    }

    /**
     * Divide two numbers, returning zero if the denominator is zero.
     */
    public static double safeDivide(double num, double denom) {
        double result = 0;
        if (!equalDoubles(denom, 0)) result = num / denom;
        return result;
    }


    public static double gammaIncomplete(double a, double x) throws Exception {
        double result = 0.0;
        if (x < 0.0 || a <= 0.0) {
            throw new Exception("Invalid arguments in routine gammaIncomplete.");
        }

        if (equalDoubles(x, 0)) return 0.0;
        double gln = gammaLn(a);
        //double gln = 1.0;
        int itmax = 1000;
        double eps = 3e-7;

        if (x < (a + 1.0)) {  //Use the series representation.
            double ap = a;
            double sum = 1.0 / a;
            double del = sum;
            for (int n = 1; n <= itmax; n++) {
                ap += 1.0;
                del *= x / ap;
                sum += del;
                if (Math.abs(del) < (eps * Math.abs(sum))) {
                    result = sum * Math.exp(-x + a * Math.log(x) - gln);
                    return result;
                }
            }
            throw new Exception("Series didn't converge: Either a is too large, or ITMAX is too small.");
        } else {  //Use the continued fraction representation
            double fpMin = 1.0e-30;
            double b = x + 1.0 - a;
            double c = 1.0 / fpMin;
            double d = 1.0 / b;
            double h = d;
            double an, del;
            int i = 1;
            for (; i <= itmax; i++) {
                an = -i * (i - a);
                b += 2.0;
                d = an * d + b;
                if (Math.abs(d) < fpMin) d = fpMin;
                c = b + an / c;
                if (Math.abs(c) < fpMin) c = fpMin;
                d = 1.0 / d;
                del = d * c;
                h *= del;
                if (Math.abs(del - 1.0) < eps) break;
            }
            if (i > itmax)
                throw new Exception("Continued Fraction didn't converge: Either a is too large, or ITMAX is too small.");
            result = Math.exp(-x + a * Math.log(x) - gln) * h;
            return 1 - result;
        }
    }

    /**
     * Returns the probability function at x using a normal distribution
     * (e.g. a gaussian)
     *
     * @param mean  mean of the gaussian
     * @param sigma standard deviation of the gaussian
     * @param x     the variable
     * @return
     */
    static public double gaussian(double mean, double sigma, double x) {
        double multiplier, exponent;

        multiplier = 1.0 / (sigma * Math.sqrt(2.0 * Math.PI));
        exponent = -((x - mean) * (x - mean)) / (2.0 * sigma * sigma);

        return (multiplier * Math.exp(exponent));
    }

    public static double gammaLn(double x) {
        double[] cof = {76.18009172947146, -86.50532032941677,
                24.01409824083091, -1.231739572450155,
                0.1208650973866179e-2, -0.5395239384953e-5};
        double xx = x;
        double y = x;
        double tmp = x + 5.5;
        double ser = 1.000000000190015;
        tmp -= (xx + 0.5) * Math.log(tmp);
        for (int j = 0; j <= 5; j++)
            ser += cof[j] / ++y;
        return -tmp + Math.log(2.5066282746310005 * ser / xx);
    }

    /**
     * Compute the phi-square measure of association.
     *
     * @param gStat
     * @param n
     * @return
     */
    public static double phiSquare(double gStat, double n) {
        return (gStat) / n;
    }

    /**
     * Method to compute the effect size of a gStat given the dof and n.
     * Use method from Bishop, Fienberg and Holland: ES = gStat - (dof -1) / n
     *
     * @param gStat
     * @param dof
     * @param n
     * @return
     */
    public static double phiSquareWithDOFCorrection(double gStat, double n, double dof) {
        return (gStat - (dof - 1)) / n;
    }

    /**
     * Return a random element from choiceSeq vector.
     */
    public static Object randomChoice(List choiceSeq) {
        if (choiceSeq.size() == 0) return null;
        int elt = Math.abs(r.nextInt()) % choiceSeq.size();
        return choiceSeq.get(elt);
    }

    /**
     * Return a randomly ordered list.
     */
    public static Object[] randomlyOrder(Object[] choiceSeq) {
        Object[] results = new Object[choiceSeq.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = choiceSeq[i];
        }
        Object tmpObj;
        int randPosition;
        for (int i = 0; i < results.length; i++) {
            randPosition = i + r.nextInt(results.length - i);
            tmpObj = results[i];
            results[i] = results[randPosition];
            results[randPosition] = tmpObj;
        }
        return results;
    }

    /**
     * Compute the average of an array.
     */
    public static double average(Double[] list) throws Exception {
        if (list.length == 0) {
            throw new Exception("Can't take average of a list of length 0");
        }
        double sum = 0;

        for (int i = 0; i < list.length; i++) {
            sum += list[i].doubleValue();
        }
        return sum / list.length;
    }

    /**
     * Compute the variance of an array.
     */
    public static double variance(Double[] list) throws Exception {
        double average = average(list);
        double sum = 0;

        for (int i = 0; i < list.length; i++) {
            sum += Math.pow(list[i].doubleValue() - average, 2);
        }
        return sum / list.length;
    }

    /**
     * Given a map from labels to numbers (e.g. counts or probabilities). We want
     * the top N labels--i.e. need to sort the labels by their numbers, and return
     * -those with highest numbers
     * -those alphabetically first in case of ties
     *
     * @param vals -- keys = labels, values = numbers
     * @return todo: write test
     */
    public static List topNValues(Map vals, int n) {
        SortedMap maxFreqLabels = new TreeMap();
        Iterator cp = vals.entrySet().iterator();
        while (cp.hasNext()) {
            Map.Entry e = (Map.Entry) cp.next();
            Object count = e.getValue();

            ArrayList labelsThisScore;
            if (maxFreqLabels.containsKey(count)) {
                labelsThisScore = (ArrayList) maxFreqLabels.get(count);
            } else {
                labelsThisScore = new ArrayList();
            }
            labelsThisScore.add(e.getKey());

            maxFreqLabels.put(count, labelsThisScore);
        }

        // TreeMaps (being SortedMaps) guarantee that we'll get things out in order of the keys (ascending).
        // We need descending instead, so get all values out, then reverse list and take top n.
        ArrayList orderedClassVals = new ArrayList(maxFreqLabels.values());
        Collections.reverse(orderedClassVals);
        ArrayList retval = new ArrayList();
        for (int i = 0; i < orderedClassVals.size() && retval.size() < Math.min(n, vals.keySet().size()); i++) {
            ArrayList labelsThisScore = (ArrayList) orderedClassVals.get(i);
            Collections.sort(labelsThisScore);  // makes them alphabetical--by first name
            for (int j = 0; j < labelsThisScore.size() && retval.size() < Math.min(n, vals.keySet().size()); j++) {
                retval.add(labelsThisScore.get(j));
            }
        }

        return retval;

    }

    public static void setRandomSeed(int seed) {
        r = new Random(seed);
    }

    public static List<Integer> sampleList(List<Integer> lst, int num) {
        int sz = lst.size();
        if (sz <= num) {
            return new ArrayList<Integer>(lst);
        } else if (num < lst.size() / 10) {
            Set<Integer> sample = new HashSet<Integer>();
            while (sample.size() < num) {
                sample.add(lst.get(r.nextInt(lst.size())));
            }
            return new ArrayList<Integer>(sample);
        } else {
            List<Integer> sample = new ArrayList<Integer>(lst);
            while (sample.size() > num) {
                sample.remove(r.nextInt(sample.size()));
            }
            return sample;
        }
    }

}