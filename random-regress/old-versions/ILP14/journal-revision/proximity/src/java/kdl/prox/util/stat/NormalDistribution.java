/**
 * $Id: NormalDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.util.stat;

import java.util.Random;


/**
 * Concrete Distribution subclass that implements a normal distribution.
 */
public class NormalDistribution extends Distribution {

    private final Random random = new Random();
    private double mean;
    private double stdDev;


    /**
     * @param mean
     * @param stdDev standard deviation
     */
    public NormalDistribution(double mean, double stdDev) {
        this.mean = mean;
        this.stdDev = stdDev;
    }

    public double getRandomNumber() {
        return (random.nextGaussian() * stdDev) + mean;
    }

}
