/**
 * $Id: ContinuousProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ContinuousProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.probdistributions;

import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import org.jdom.Element;

/**
 * A continuous distribution assumes that the values come from a continuous domain,
 * and uses a Guassian density function to estimate the probability of a given value
 */
public class ContinuousProbDistribution extends ProbDistribution {

    public ContinuousProbDistribution() {
        super();
    }

    public ContinuousProbDistribution(ProbDistribution distr) {
        super(distr);
    }

    public ContinuousProbDistribution(NST sourceNST) {
        super();
        addAttributeValuesFromNST(sourceNST);
    }

    public ContinuousProbDistribution(Element xmlEle) {
        super(xmlEle);
    }


    public ProbDistribution addAttributeValue(Object value, double weight) {
        Double val = getDoubleFromObject(value);
        super.addAttributeValue(val, weight);
        return this;
    }

    private Double getDoubleFromObject(Object value) {
        Double val;
        if (value instanceof String) {
            val = new Double((String) value);
        } else if (value instanceof Float) {
            val = new Double(((Float) value).doubleValue());
        } else if (value instanceof Integer) {
            val = new Double(((Integer) value).doubleValue());
        } else if (value instanceof Double) {
            val = (Double) value;
        } else {
            throw new IllegalArgumentException("Value can only be of type String, Double, Integer, or Float");
        }
        return val;
    }


    /**
     * Returns the value (modulo delta) of a normal or gaussian probability
     * density function for x.
     * <p/>
     * gaussianPdf(x, mean, s) = 1/(s*sqrt(2*pi)) * e^(-((x-mean)^2)/(2s^2))
     *
     * @param x    the value for which you want a probability estimate
     * @param mean the mean of the gaussian distribution
     * @param sdev the standard deviation of the gaussian distribution
     */
    public double gaussianPdf(double x, double mean, double sdev) {
        Assert.condition(sdev != 0, "standard deviation (sdev) must be non-zero");
        double tmp1 = Math.pow(x - mean, 2.0);
        double tmp2 = 2 * Math.pow(sdev, 2.0);
        double result = tmp1 / tmp2;
        result = Math.exp(0 - result);
        // todo double check this fix
        // used to be: result *= 1 / Math.sqrt( 2 * Math.PI * sdev );
        result *= 1 / (Math.sqrt(2 * Math.PI) * sdev);
        return result;
    }


    /*
    * todo for now return smoothed prob, need to figure out what Laplace correction means for continuous attrs
    */
    public double getLaplaceCorrProbability(Object value) {
        return getSmoothedProbability(value);
    }

    /**
     * Returns the mean of the current distribution
     *
     * @return a double with (sum(value * count))/totValues
     */

    public double getMean() {
        double sumLabel = 0.0;

        Object[] labels = getDistinctValues();
        for (int labelIdx = 0; labelIdx < labels.length; labelIdx++) {
            Double value = getDoubleFromObject(labels[labelIdx]);
            sumLabel = sumLabel + (value.doubleValue() * getCount(value));

        }
        return 1.0 * (sumLabel / getTotalNumValues());
    }


    /**
     * Returns the probabilty of a value
     *
     * @param value the value of the observation
     */
    public double getProbability(Object value) {
        Assert.notNull(value, "null value");

        double val = getDoubleFromObject(value).doubleValue();
        double classCountTotal = getTotalNumValues();
        double sdev = 1 / Math.sqrt(classCountTotal);
        Object[] values = getDistinctValues();
        double[] gaussVec = new double[values.length];

        //--- get gaussian values for each training value
        for (int i = 0; i < values.length; i++) {
            gaussVec[i] = gaussianPdf(val, ((Double) values[i]).doubleValue(), sdev);
        }

        //--- find the weigthed mean of the gaussians
        double sum = 0.0;
        double count = 0.0;
        for (int i = 0; i < values.length; i++) {
            double tmpCount = getCount(values[i]);
            sum += gaussVec[i] * tmpCount;
            count += tmpCount;
        }
        double mean = values.length == 0 ? 0 : sum / count;

        return mean;
    }


    /**
     * Smoothes the probability
     * <p/>
     * todo find a better way to do this
     * Beta code: the method below is ad hoc. Use 0.01/N as the deafult prob
     * where N=number of examples in this distribution.
     * (Used to use 1/N where N=total # examples but is too inefficent to
     * poll all distributions in conditional estimator).
     * It prevents conditional probabilities of zero, but there are probably
     * better ways. (Emailed George John on 3/8/98.)
     *
     * @param value the value of the observation
     */
    public double getSmoothedProbability(Object value) {
        double prob = getProbability(value);
        double defaultProb = 0.01 / getTotalNumValues();

        double result = Math.max(defaultProb, prob);
        //todo need to figure out why this can return more than 1.0
        if (result > 1.0) {
            result = Math.min(1 - defaultProb, result);
        }

        return result;
    }
}