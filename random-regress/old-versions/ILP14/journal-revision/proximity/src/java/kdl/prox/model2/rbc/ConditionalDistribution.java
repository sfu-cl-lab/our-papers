/**
 * $Id: ConditionalDistribution.java 3703 2007-11-02 16:06:44Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ContinuousProbDistribution;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.rbc.estimators.Estimator;
import kdl.prox.model2.util.XMLUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class implements a Conditional Probability Distribution: a map between the different labels in the class,
 * and distributions of values for a given attribute.
 * <p/>
 * It also contains an estimator, which is a 'mechanism' for adding observations into the prob. distribution, and
 * computing probabilities of particular values.
 */
public class ConditionalDistribution {

    private static Logger log = Logger.getLogger(ConditionalDistribution.class);

    private Estimator estimator;
    private HashMap<String, ProbDistribution> labelToDistrMap;


    public ConditionalDistribution(Estimator est) {
        labelToDistrMap = new HashMap<String, ProbDistribution>();
        estimator = est;
    }

    public ConditionalDistribution(Estimator est, Element xmlEle) {
        estimator = est;
        labelToDistrMap = new HashMap<String, ProbDistribution>();
        List<Element> elements = xmlEle.getChildren("conditional-distr-info");
        for (Element ele : elements) {
            String label = ele.getChildText("label");
            labelToDistrMap.put(label, XMLUtil.readProbDistributionFromXML(ele));
        }
    }

    /**
     * For each distinct class label, create a ProbDistribution of the observed values for the attribute.
     * For example, given the NST
     * class  value
     * -----  -----
     * +      a
     * +      b
     * +      a
     * -      c
     * -      a
     * <p/>
     * This creates a map with two probability distributions, one for + and one for -:
     * <p/>
     * + => {a,b,a}
     * - => {c,a}
     * <p/>
     * It is in fact up to the estimator to decide how to store the observed values in the corresponding prob. distributions.
     * Some estimators (e.g., Multinomial, Avg) will keep all the observed values, others (e.g, Mode) will keep only one value.
     * *
     * The NST is expected to have the columns [subg_id, value, class]
     *
     * @param classValueNST
     * @param isContinuous
     */
    public ConditionalDistribution recordClassAndValueDistribution(NST classValueNST, boolean isContinuous) {
        List<String> classLabels = classValueNST.selectRows("class DISTINCT ROWS", "class").toStringList(1);
        for (String label : classLabels) {
            if (!labelToDistrMap.containsKey(label)) {
                labelToDistrMap.put(label, isContinuous ? new ContinuousProbDistribution() : new DiscreteProbDistribution());
            }
            ProbDistribution distribution = labelToDistrMap.get(label);
            estimator.recordObservations(classValueNST.filter("class EQ '" + label + "'", "subg_id, value"), distribution);
        }
        return this;
    }


    /**
     * Returns a set with all the labels that we know of
     *
     * @return
     */
    public Set<String> getAllLabels() {
        return labelToDistrMap.keySet();
    }

    /**
     * Returns the probability distribution stored for the given label
     *
     * @param label
     * @return
     */
    public ProbDistribution getProbDistributionForLabel(String label) {
        return labelToDistrMap.get(label);
    }


    /**
     * Returns the probability for each subgraph of belonging to a particular class label given observed values for an attribute.
     * <p/>
     * It finds the prob. distribution for the class label that was recorded in recordClassAndValueDistribution, and then
     * uses the estimator to compute a probability for each subgraph based on the observed values and the recorded distribution.
     * <p/>
     * For example, if the method is called for the label '+', and the recorded distribution for that label is
     * <p/>
     * + => {a,b,a}
     * <p/>
     * and, for example, the sourceNST is
     * <p/>
     * subg_id value
     * ------- -----
     * 1  a
     * 1  b
     * 2  c
     * <p/>
     * <p/>
     * Then the probabilities of each subgraph being '+' is, for example (depending on the estimator)
     * <p/>
     * 1 -> 2/3
     * 2 -> 0.00000000001
     * <p/>
     * In fact, it's the estimator that does most of the job, unless it's an unseen label, in which case we return a
     * default marginal probability given the know classes.
     *
     * @param classValue
     * @param sourceNST
     * @param randSeed
     * @return map from <string -> double> , subgids to probabilitites
     */
    public HashMap<String, Double> getSmoothedProbsForLabelGivenAttribute(String classValue, NST sourceNST, int randSeed) {
        if (labelToDistrMap.containsKey(classValue)) {
            ProbDistribution distribution = labelToDistrMap.get(classValue);
            return estimator.getSmoothedProbsGivenAttribute(sourceNST, distribution, randSeed);
        } else {
            // if classVal is missing, return marginal prob given known classes ==> P(A) = sum_C P(A|C) * P(C)
            return getDefaultProbabilitiesFromNST(sourceNST, randSeed);
        }
    }

    private HashMap<String, Double> getDefaultProbabilitiesFromNST(NST attrNST, int randSeed) {
        //get current maps for all known classes, and get total counts for classes
        double[] classCounts = new double[labelToDistrMap.size()];
        double totalCount = 0;
        HashMap<String, Double>[] maps = new HashMap[labelToDistrMap.size()];
        int i = 0;
        for (ProbDistribution probDistribution : labelToDistrMap.values()) {
            HashMap<String, Double> probMap = estimator.getSmoothedProbsGivenAttribute(attrNST, probDistribution, randSeed);
            maps[i] = probMap;
            double classCount = probDistribution.getTotalNumValues();
            classCounts[i] = classCount;
            totalCount += classCount;
            i++;
        }
        //cycle over instances and compute the marginal prob
        HashMap<String, Double> margMap = new HashMap<String, Double>();
        for (String subgID : maps[0].keySet()) {
            double totalProb = 0;
            if (totalCount > 0) {
                for (int clIdx = 0; clIdx < maps.length; clIdx++) {
                    HashMap<String, Double> map = maps[clIdx];
                    double currProb = (map.get(subgID)).doubleValue();
                    log.debug("currProb " + currProb);
                    //multiply in P(C)
                    double classProb = classCounts[clIdx] / totalCount;
                    log.debug("clProb " + classProb + " tc " + totalCount + " cc " + classCounts[clIdx]);
                    currProb *= classProb;
                    totalProb += currProb;
                }
                totalProb = Math.max(0.0000001 / totalCount, totalProb);
            } else {
                totalProb = 0.00000001;
            }
            log.debug("marg " + subgID + " " + totalProb);
            margMap.put(subgID, new Double(totalProb));
        }
        return margMap;
    }


    public boolean isEmpty() {
        return labelToDistrMap.isEmpty();
    }

    public String toString() {
        String s = "";
        for (String val : labelToDistrMap.keySet()) {
            s += " " + val + ":" + labelToDistrMap.get(val).toString();
        }
        return s.substring(1);
    }

    public Element toXML() {
        Element condDistr = new Element("conditional-distr");
        List<String> labels = new ArrayList<String>(labelToDistrMap.keySet());
        Collections.sort(labels);
        for (String label : labels) {
            Element distr = new Element("conditional-distr-info");
            distr.addContent(new Element("label").addContent(label));
            distr.addContent(labelToDistrMap.get(label).toXML());
            condDistr.addContent(distr);
        }
        return condDistr;
    }

}
