/**
 * $Id: ContinuousEpanechnikovProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ContinuousEpanechnikovProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.probdistributions;

import kdl.prox.util.Assert;

/**
 * Uses an Epanechnikov kernel function instead of a Gaussiam to compute the
 * prob. of a value. SmoothedProbability changes too
 */
public class ContinuousEpanechnikovProbDistribution extends ContinuousProbDistribution {

    private static final double DEFAULT_KERNEL_WIDTH = 3.0;    //window width for kernel density estimate
    private double kernelWidth = DEFAULT_KERNEL_WIDTH;

    /**
     * Epanechnikov Kernel function
     */
    private double epanechnikovKernelFunction(double x0, double xi) {
        double dist = Math.abs(x0 - xi);

        if (dist > kernelWidth) {         // returns 0 if dist > kernel_width
            return 0;
        } else {
            dist = dist / kernelWidth;
            return 0.75 * (1.0 - dist * dist);
        }
    }


    public static double getDefaultKernelWidth() {
        return DEFAULT_KERNEL_WIDTH;
    }


    public double getKernelWidth() {
        return kernelWidth;
    }


    /**
     * Return a probability estimate for a value (based on frequency information).
     *
     * @param value the value of the observation
     */
    public double getProbability(Object value) {
        Assert.notNull(value, "Attribute value cannot be null.");

        double val = ((Double) value).doubleValue();
        double cummSum = 0.0;
        Object[] values = getDistinctValues();

        //--- get kernel function for each training value      //todo can sort values and terminate early
        for (int i = 0; i < values.length; i++) {
            double temp = epanechnikovKernelFunction(val, ((Double) values[i]).doubleValue());
            cummSum += temp;
        }

        double result = cummSum / (getTotalNumValues() * kernelWidth);
        return result;
    }


    /**
     * Smoothes the probability
     *
     * @param value the value of the observation
     */
    public double getSmoothedProbability(Object value) {
        double prob = getProbability(value);
        double defaultProb = 0.01 / getTotalNumValues();

        if (prob <= 0) {
            return defaultProb;
        } else {
            return prob;
        }
    }


    public void setWindowWidth(double kernelWidth) {
        this.kernelWidth = kernelWidth;
    }
}