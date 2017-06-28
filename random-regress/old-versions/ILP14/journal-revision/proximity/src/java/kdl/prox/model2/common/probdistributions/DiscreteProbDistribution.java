/**
 * $Id: DiscreteProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DiscreteProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.probdistributions;

import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import kdl.prox.util.stat.StatUtil;
import org.jdom.Element;

import java.util.Iterator;


/**
 * Defines a Discrete Probability Distribution.  Observations are discrete and probability
 * returned is simply the Maximum Likelihood Estimate (smoothed for non-zero
 * probabilities).
 */
public class DiscreteProbDistribution extends ProbDistribution {

    public DiscreteProbDistribution() {
        super();
    }

    public DiscreteProbDistribution(ProbDistribution distr) {
        super(distr);
    }

    public DiscreteProbDistribution(NST sourceNST) {
        super();
        addAttributeValuesFromNST(sourceNST);
    }

    /**
     * Creates an instance of this class from the info in an XML element of type <class-label-distr>
     *
     * @param xmlEle
     */
    public DiscreteProbDistribution(Element xmlEle) {
        super(xmlEle);
    }

    /**
     * Draw a random value from the observed distribution
     */
    public Object drawSample() {
        double cutoff = StatUtil.nextDouble();
        double total = 0.0;

        Iterator valueIter = attrValueCountMap.keySet().iterator();
        while (valueIter.hasNext()) {
            Object value = valueIter.next();

            double prob = getProbability(value);
            total += prob;
            if (total > cutoff) {
                return value;
            }
        }
        Assert.condition(false, "Probabilities do not sum to 1.0");
        return null;
    }

    /**
     * Return a Laplace-corrected probability estimate, which adjusts for zero-counts.
     * Use Laplace corrected frequency estimates. If there are 'n' examples
     * of a class 'C', with 'N' total examples, the frequency estimate for
     * P(C)=n/N. The Laplace corrected estimate is (n+1)/N+|C|.
     * Note: this causes problems when missing values are not random
     * For example, in iterative classification when all values are initially
     * unknown this skews probabilities to default distribution. Use
     * getSmoothedProbability() instead.
     *
     * @param value the value of the observation
     */
    public double getLaplaceCorrProbability(Object value) {
        double numerator = getCount(value) + 1.0;
        double denominator = getTotalNumValues() + attrValueCountMap.size();
        double prob = numerator / denominator;
        return prob;

    }

    /**
     * Returns the value with the highest probability
     *
     * @return a string with the label
     */
    public String getHighestProbabilityValue() {
        double maxProb = -1.0;
        String maxValue = "";
        Object[] labels = getDistinctValues();
        for (int labelIdx = 0; labelIdx < labels.length; labelIdx++) {
            String value = (String) labels[labelIdx];
            double prob = getProbability(value);
            if ((prob > maxProb) || ((prob == maxProb) && (value.compareTo(maxValue) < 0))) {
                maxProb = prob;
                maxValue = value;
            }
        }
        return maxValue;
    }

    /**
     * Return a probability estimate for a value (based on frequency information).
     *
     * @param value the value of the observation
     */
    public double getProbability(Object value) {
        double numerator = getCount(value);
        double denominator = getTotalNumValues();
        return numerator / denominator;
    }

    public String sample() {
        Assert.condition(getTotalNumValues() > 0, "No elements to sample");

        double cutoff = StatUtil.nextDouble();
        double total = 0.0;

        Object[] values = getAllValues(false);
        for (int valueIdx = 0; valueIdx < values.length; valueIdx++) {
            String value = (String) values[valueIdx];
            double prob = getProbability(value);
            total += prob;
            if (total > cutoff) {
                return value;
            }
        }
        Assert.condition(false, "Probabilities do not sum to 1.0: " + total + "," + cutoff);
        return null;
    }

    /**
     * Return a smoothed probability estimate for a value, which adjusts for zero-counts.
     * returns 0.01/m where of m=total instance count if frequency is zero.
     *
     * @param value the value of the observation
     */
    public double getSmoothedProbability(Object value) {
        double numerator = getCount(value);
        if (numerator == 0) numerator = 0.01;
        double denominator = getTotalNumValues();
        return numerator / denominator;
    }
}