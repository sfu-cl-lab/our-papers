/**
 * $Id: ProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ProbDistribution.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.probdistributions;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.jdom.Element;

import java.util.*;


/**
 * Defines a  probability distribution, with methods to update it (add and remove Value)
 * and methods to compute probability of a given value or draw samples.
 * <p/>
 * A probability distribution is essentially a map between values (keys) and weights.
 * Can either be discrete or continuous;
 */
public abstract class ProbDistribution {

    protected HashMap attrValueCountMap = new HashMap();  // value -> weight
    protected double totalNumValues;

    public ProbDistribution() {
    }

    public ProbDistribution(ProbDistribution distribution) {
        this(distribution, 1.0);
    }

    public ProbDistribution(ProbDistribution distribution, double weight) {
        Object[] values = distribution.getDistinctValues();
        for (int valueIdx = 0; valueIdx < values.length; valueIdx++) {
            Object value = values[valueIdx];
            addAttributeValue(value, distribution.getCount(value) * weight);
        }
    }

    public ProbDistribution(Element xmlEle) {
        Element classLabelDistrEle = xmlEle.getChild("class-label-distr");
        List classLabelInfos = classLabelDistrEle.getChildren("class-label-info");
        for (int classLabelInfoIdx = 0; classLabelInfoIdx < classLabelInfos.size(); classLabelInfoIdx++) {
            Element clInfo = (Element) classLabelInfos.get(classLabelInfoIdx);
            double value = Double.parseDouble(clInfo.getAttributeValue("count"));
            addAttributeValue(clInfo.getAttributeValue("class"), value);
        }
    }

    /**
     * One-arg overload.  Passes 1.0 as the weight for the two-arg version.
     *
     * @param value the value of the observation
     */
    public ProbDistribution addAttributeValue(Object value) {
        addAttributeValue(value, 1.0);
        return this;
    }

    /**
     * Adds a new value to the current distribution.
     *
     * @param value  the value of the observation
     * @param weight the weight of this observation, typically the number of times it has been observed
     */
    public ProbDistribution addAttributeValue(Object value, double weight) {
        double count = getCount(value);  // may be zero
        count += weight;
        attrValueCountMap.put(value, new Double(count));
        totalNumValues += weight;
        return this;
    }


    public ProbDistribution adjustCount(Object value, double incrProb) {
        attrValueCountMap.put(value, getCount(value) * incrProb);
        return this;
    }

    public ProbDistribution normalize() {
        double currTotal = getTotalNumValues();
        double newTotal = 0.0;
        Iterator iterator = attrValueCountMap.keySet().iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            double newCount = getCount(value) / currTotal;
            attrValueCountMap.put(value, newCount);
            newTotal += newCount;
        }
        totalNumValues = newTotal;
        return this;
    }

    /**
     * Reads the distribution from an NST. Assumes the NST has a 'value' column
     * If  there is a weight column in the NST, this method uses it. Otherwise,
     * it gets the histogram (to read only a few lines) and uses addAttributeValue with a weight equivalent
     * to the count of each value in the histogram.
     */
    public ProbDistribution addAttributeValuesFromNST(NST filteredNST) {
        NST aggregateNST = (filteredNST.isColumnExists("weight") ?
                filteredNST.aggregate("sum", "value", "weight") :
                filteredNST.getColumnHistogramAsNST("value"));
        ResultSet resultSet = aggregateNST.selectRows();
        while (resultSet.next()) {
            Object value = resultSet.getString(1);
            double cnt = resultSet.getDouble(2);
            addAttributeValue(value, cnt);
        }

        aggregateNST.release();
        return this;
    }

    /**
     * Removes all values, clearing the distribution
     */
    public void clear() {
        attrValueCountMap = new HashMap();
        totalNumValues = 0.0;
    }

    /**
     * Returns an array of the attribute values.
     * If argument is true, does return isUseWeight values, and
     * then returned array will have size getTotalNumValues.
     * N.B. If weights are non-integer, strange things may happen when isUseWeight==true.
     *
     * @param isUseWeight true if function should return isUseWeight values
     */
    public Object[] getAllValues(boolean isUseWeight) {
        Object[] keys = attrValueCountMap.keySet().toArray();
        Object[] results;

        if (isUseWeight) {
            results = new Object[(int) totalNumValues];
            int index = 0;
            for (int i = 0; i < keys.length; i++) {
                double weight = ((Double) attrValueCountMap.get(keys[i])).doubleValue();
                Assert.condition(weight % 1 == 0,
                        "weight is a non-integer value, strange things may happen");
                for (int j = 0; j < weight; j++) {
                    results[index] = keys[i];
                    index++;
                }
            }
        } else {
            results = keys;
        }
        return results;
    }

    /**
     * Returns the set of counts corresponding to a given attribute value.
     *
     * @param value the value of the observation
     */
    public double getCount(Object value) {
        if (value == null || !(attrValueCountMap.containsKey(value)))
            return 0.0;
        else
            return ((Double) attrValueCountMap.get(value)).doubleValue();
    }


    /**
     * Returns an array with all the distinct values in the map
     *
     * @return
     */
    public Object[] getDistinctValues() {
        return getAllValues(false);
    }

    /**
     * Returns the map that stores this distribution: key --> count
     *
     * @return
     */
    public Map getDistributionMap() {
        return attrValueCountMap;
    }


    /**
     * Returns a probability estimate for a value.
     * Different whether Discrete or continuous
     */
    public abstract double getLaplaceCorrProbability(Object value);

    public abstract double getProbability(Object value);

    public abstract double getSmoothedProbability(Object value);


    /**
     * Returns the number of values in estimator.
     */
    public double getTotalNumValues() {
        return totalNumValues;
    }


    /**
     * Combines two distributions, adding the counts when there are overlaps.
     *
     * @param distribution
     * @return a new distribution with this + the other distribution
     */
    public ProbDistribution merge(ProbDistribution distribution) {
        Assert.condition(distribution.getClass().equals(this.getClass()), "Cannot merge distributions of different kinds");
        try {
            ProbDistribution newDistr = getClass().newInstance();
            // First put all the values from me
            Object[] values = getDistinctValues();
            for (int valueIdx = 0; valueIdx < values.length; valueIdx++) {
                Object value = values[valueIdx];
                newDistr.addAttributeValue(value, getCount(value));
            }
            // and now add / combine the values from the other distr
            values = distribution.getAllValues(false);
            for (int valueIdx = 0; valueIdx < values.length; valueIdx++) {
                Object value = values[valueIdx];
                newDistr.addAttributeValue(value, distribution.getCount(value));
            }
            return newDistr;
        } catch (Exception e) { // Instantiation or IllegalAccess
            e.printStackTrace();
            throw new IllegalArgumentException("Error creating new distribution");
        }
    }


    /**
     * Remove a value
     */
    public void removeAttributeValue(Object value, double weight) {
        if (!attrValueCountMap.containsKey(value)) {
            return;
        } else {
            double count = getCount(value);
            count -= weight;
            if (count > 0) {
                attrValueCountMap.put(value, new Double(count));
            } else {
                attrValueCountMap.remove(value);
            }
            totalNumValues -= weight;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        Map distMap = getDistributionMap();
        for (Iterator valItr = distMap.keySet().iterator(); valItr.hasNext();) {
            Object val = valItr.next();
            Object count = distMap.get(val);
            sb.append(val + ": " + count);
            if (valItr.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Saves this distribution in a XML format, with a set of <class-label-info> items
     *
     * @return the XML element
     */
    public Element toXML() {
        Element classLabelDistrEle = new Element("class-label-distr");
        classLabelDistrEle.addContent(XMLUtil.createElementWithValue("distr-class", getClass().getName()));
        Object[] classLabelValues = getDistinctValues();
        Arrays.sort(classLabelValues);
        for (int classLabelIdx = 0; classLabelIdx < classLabelValues.length; classLabelIdx++) {
            Object classLabel = classLabelValues[classLabelIdx];
            Element clInfo = new Element("class-label-info");
            clInfo.setAttribute("class", classLabel.toString());
            clInfo.setAttribute("count", getCount(classLabel) + "");
            classLabelDistrEle.addContent(clInfo);
        }
        return classLabelDistrEle;
    }

}

