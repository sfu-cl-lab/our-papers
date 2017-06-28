/**
 * $Id: Predictions.java 3784 2007-11-19 19:43:06Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.common.predictions;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ContinuousProbDistribution;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Stores predictions for a set of subgraphs, along with their true class labels.
 * Provides methods for computing the error of the predictions
 */
public class Predictions {

    protected static Logger log = Logger.getLogger(Predictions.class);

    // Maps that store the true labels for subgraphs, and their predicted class distributions
    Map<String, String> subgToTrueClassLabelMap;
    Map<String, ProbDistribution> subgToClassLabelDistrMap;

    public Predictions() {
        subgToClassLabelDistrMap = new HashMap<String, ProbDistribution>();
        subgToTrueClassLabelMap = new HashMap<String, String>();
    }

    /**
     * Compares two predictions, returning the count of different predictions
     *
     * @param other
     */
    public double compare(final Predictions other) {
        return new PredictionsIterator() {
            public void eachSubg(String subgID, String trueValue, ProbDistribution classLabelDistr) {
                String thisPrediction = getInferredClass(subgID);
                String otherPrediction = other.getInferredClass(subgID);
                counter += thisPrediction.equals(otherPrediction) ? 0.0 : 1.0;
            }
        }.counter;
    }

    /**
     * Returns a 2D array of roc points
     */
    public double[][] genRocPoints(String className) {

        // rank the PredictionData objects by prob score for the given class
        List<String> ranking = new ArrayList<String>(subgToTrueClassLabelMap.keySet());
        if (ranking.size() == 0) {
            return new double[0][0];
        }
        Collections.sort(ranking, new PredictionDataComparator(className));

        // we'll return a list made up of arrays of size two [x][y]
        double[][] points = new double[ranking.size() + 1][2];

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        // first pass --- call them all negs
        ListIterator<String> pdIter = ranking.listIterator();
        while (pdIter.hasNext()) {
            String subgID = pdIter.next();
            String trueClass = subgToTrueClassLabelMap.get(subgID);
            if (className.equals(trueClass)) {
                fn++;
            } else {
                tn++;
            }
        }
        points[0] = new double[]{0, 0};
        //log.info("\n ROC point " + points[0][0] + " " + points[0][1]);

        // now step through, adjusting the totals as needed
        int idx = 1;
        pdIter = ranking.listIterator();
        while (pdIter.hasNext()) {
            String subgID = pdIter.next();
            String trueClass = subgToTrueClassLabelMap.get(subgID);
            if (className.equals(trueClass)) {
                tp++;
                fn--;
            } else {
                fp++;
                tn--;
            }
            points[idx] = new double[]{1.0 * fp / (fp + tn), 1.0 * tp / (tp + fn)};
            idx++;
        }

        return points;
    }

    /**
     * Returns area under the ROC curve for predictions
     */
    public double getAUC(String className) {
        double auc = 0.0;
        double[][] points = genRocPoints(className);
        for (int i = 1; i < points.length; i++) {
            auc += 0.5 * (points[i][0] - points[i - 1][0]) * (points[i][1] + points[i - 1][1]);
        }
        return auc;
    }


    /**
     * Returns the list of all the subgraphs for which we have predictions
     *
     * @return
     */
    public Set<String> getAllPredictedSubgraphs() {
        return subgToClassLabelDistrMap.keySet();
    }


    /**
     * Retuns the Conditional Log Likelihood for this set of predictions
     *
     * @return a double with the CLL
     */
    public double getConditionalLogLikelihood() {
        return new PredictionsIterator() {
            public void eachSubg(String subgID, String trueValue, ProbDistribution classLabelDistr) {
                if (classLabelDistr.getCount(trueValue) == 0) {
                    log.warn("getConditionalLogLikelihood: Encountered unknown class label.");
                } else {
                    counter += Math.log(classLabelDistr.getProbability(trueValue));
                }
            }
        }.counter;
    }

    /**
     * Returns the inferred class for a given subgraph ID
     *
     * @param subgID
     * @return the class with the highest probability in the class distribution
     */
    public String getInferredClass(String subgID) {
        ProbDistribution classLabelDistr = subgToClassLabelDistrMap.get(subgID);
        Assert.notNull(classLabelDistr, "inferred class for subgraph not found");
        Assert.condition(classLabelDistr instanceof DiscreteProbDistribution, "inferred class is only computed for discrete distributions");
        return ((DiscreteProbDistribution) classLabelDistr).getHighestProbabilityValue();
    }

    public ProbDistribution getProbDistribution(String subgID) {
        return subgToClassLabelDistrMap.get(subgID);
    }

    public double getSubgProbability(String subgID, String value) {
        return subgToClassLabelDistrMap.get(subgID).getProbability(value);
    }

    public String getTrueClass(String subgID) {
        return subgToTrueClassLabelMap.get(subgID);
    }

    /**
     * Returns average zero-one loss for the predictions
     */
    public double getZeroOneLoss() {
        PredictionsIterator iter = new PredictionsIterator() {
            public void eachSubg(String subgID, String trueValue, ProbDistribution classLabelDistr) {
                Assert.condition(classLabelDistr instanceof DiscreteProbDistribution, "inferred class is only computed for discrete distributions");
                String inferredLabel = ((DiscreteProbDistribution) classLabelDistr).getHighestProbabilityValue();
                counter += trueValue.equals(inferredLabel) ? 0.0 : 1.0;
            }
        };
        return iter.total == 0 ? (-99.0) : (iter.counter / iter.total);
    }

    public double getRMSE() {
        PredictionsIterator iter = new PredictionsIterator() {
            public void eachSubg(String subgID, String trueValue, ProbDistribution classLabelDistr) {
                Assert.condition(classLabelDistr instanceof ContinuousProbDistribution, "mean is only computed for continuoous distributions");
                double inferredMean = ((ContinuousProbDistribution) classLabelDistr).getMean();
                counter += Math.pow((inferredMean - Double.parseDouble(trueValue)), 2);
            }

        };
        return iter.total == 0 ? (0.0) : Math.sqrt(iter.counter / iter.total);
    }


    public String getSampledClass(String subgID) {
        ProbDistribution classLabelDistr = subgToClassLabelDistrMap.get(subgID);
        Assert.notNull(classLabelDistr, "information for subgraph not found");
        Assert.condition(classLabelDistr instanceof DiscreteProbDistribution, "inferred class is only computed for discrete distributions");
        Assert.condition(classLabelDistr.getTotalNumValues() > 0, "No elements to sample");

        return ((DiscreteProbDistribution) classLabelDistr).sample();
    }

    /**
     * Samples for each subgraph and returns the classes
     *
     * @return
     */
    public Map<String, String> getSampledClasses() {
        Map<String, String> sampledClasses = new HashMap<String, String>();
        Set<String> subgs = subgToClassLabelDistrMap.keySet();
        for (Iterator<String> subgIter = subgs.iterator(); subgIter.hasNext();) {
            String subgId = subgIter.next();
            sampledClasses.put(subgId, getSampledClass(subgId));
        }
        return sampledClasses;
    }


    public Predictions normalizeForAll() {
        for (ProbDistribution distr : subgToClassLabelDistrMap.values()) {
            distr.normalize();
        }
        return this;
    }


    /**
     * Removes the predictions for a subgraph
     *
     * @param subgID
     * @return
     */
    public Predictions removePrediction(String subgID) {
        subgToClassLabelDistrMap.remove(subgID);
        return this;
    }

    /**
     * Saves the prediction in an attribute
     * The saved ids are subgraph ids, so this attribute should be saved as a subgraph attr.
     *
     * @param attrs
     * @param attrName
     */
    public void savePredictions(Attributes attrs, String attrName) {
        attrs.defineAttributeOrClearValuesIfExists(attrName, "str");
        NST attrDataNST = attrs.getAttrDataNST(attrName);
        String[][] data = new String[subgToClassLabelDistrMap.keySet().size()][2];
        List<String> keys = new ArrayList<String>(subgToClassLabelDistrMap.keySet());
        for (int subgIdIdx = 0; subgIdIdx < keys.size(); subgIdIdx++) {
            data[subgIdIdx][0] = keys.get(subgIdIdx);
            data[subgIdIdx][1] = getInferredClass(data[subgIdIdx][0]);
        }
        attrDataNST.fastInsert(data);
        attrDataNST.release();
    }

    /**
     * Saves the prediction in an attribute, with their probabilities
     * The saved ids are subgraph ids, so this attribute should be saved as a subgraph attr.
     *
     * @param attrs
     * @param attrName
     */
    public void savePredictionsWithProbs(Attributes attrs, String attrName) {
        attrs.defineAttributeOrClearValuesIfExists(attrName, "val:str, prob:dbl");
        NST attrDataNST = attrs.getAttrDataNST(attrName);
        for (String subgId : subgToClassLabelDistrMap.keySet()) {
            ProbDistribution distribution = subgToClassLabelDistrMap.get(subgId);
            for (Object val : distribution.getDistinctValues()) {
                attrDataNST.insertRow(subgId + "," + val + ", " + distribution.getProbability(val));
            }
        }
        attrDataNST.release();
    }

    /**
     * Adds to the current set of predictions from another set, merging the distributions for all those
     * subgraphs that are already defined in me.
     *
     * @param predictions
     */
    public Predictions setPredictions(Predictions predictions) {
        Iterator<String> iterator = predictions.subgToClassLabelDistrMap.keySet().iterator();
        while (iterator.hasNext()) {
            String subgID = iterator.next();
            ProbDistribution newDistr = predictions.subgToClassLabelDistrMap.get(subgID);
            setPrediction(subgID, newDistr);
        }
        return this;
    }

    /**
     * Stores the class label distribution for a set of subgraphs, merging the distributions for all those
     * subgraphs that are already defined in me.
     * <p/>
     * The weight column in the subgIDs is ignored -- the relative counts of different distributions that
     * are later merged if a subgraph appears in more than one branch (due to missing values) will take
     * care of assigning the relative weight to each branch.
     *
     * @param subgIDs
     * @param classLabelDistribution
     */
    public Predictions setPredictions(NST subgIDs, ProbDistribution classLabelDistribution) {
        List<String> subgIDList = subgIDs.selectRows("subg_id").toStringList(1);
        for (int subgIdListIdx = 0; subgIdListIdx < subgIDList.size(); subgIdListIdx++) {
            String subgID = subgIDList.get(subgIdListIdx);
            try {
                Constructor<? extends ProbDistribution> constructor = classLabelDistribution.getClass().getConstructor(ProbDistribution.class);
                ProbDistribution newDistr = constructor.newInstance(classLabelDistribution);
                setPrediction(subgID, newDistr);
            } catch (Exception e) {
                throw new MonetException("Could not instantiate new " + classLabelDistribution.getClass());
            }
        }
        return this;
    }

    public void setPrediction(String subgID, ProbDistribution newDistr) {
        ProbDistribution currDistr = subgToClassLabelDistrMap.get(subgID);
        if (currDistr == null) {
            subgToClassLabelDistrMap.put(subgID, newDistr);
        } else {
            subgToClassLabelDistrMap.put(subgID, currDistr.merge(newDistr));
        }
    }

    /**
     * Sets the true label for a given subg
     *
     * @param subgID
     * @param label
     */
    public Predictions setTrueLabel(String subgID, String label) {
        subgToTrueClassLabelMap.put(subgID, label);
        return this;
    }

    /**
     * Sets the true labels for an entire container
     *
     * @param cont
     * @param trueClass
     */
    public Predictions setTrueLabels(Container cont, AttributeSource trueClass) {
        NSTCache cache = new NSTCache();
        trueClass.init(cont, cache);
        ResultSet resultSet = trueClass.getSourceTable().selectRows("subg_id, value");
        while (resultSet.next()) {
            String subgID = resultSet.getString(1);
            String value = resultSet.getString(2);
            subgToTrueClassLabelMap.put(subgID, value);
        }
        cache.clear();
        return this;
    }

    public int size() {
        return subgToClassLabelDistrMap.size();
    }

    public String toString() {
        return subgToClassLabelDistrMap.toString();
    }

    // comparator class for PredictionDataObjects --- TIES ARE RANDOMLY BROKEN
    private class PredictionDataComparator implements Comparator {
        private String classLabel;

        public PredictionDataComparator(String classLabel) {
            this.classLabel = classLabel;
        }

        public int compare(Object o1, Object o2) {
            ProbDistribution o1Distr = subgToClassLabelDistrMap.get(o1);
            if (o1Distr == null) {
                return 1;
            }
            double o1Prob = Math.log(o1Distr.getProbability(classLabel));

            ProbDistribution o2Distr = subgToClassLabelDistrMap.get(o2);
            if (o2Distr == null) {
                return -1;
            }
            double o2Prob = Math.log(o2Distr.getProbability(classLabel));

            if (o1Prob < o2Prob) {
                return 1;
            } else if (o1Prob > o2Prob) {
                return -1;
            } else {
                return Math.random() > 0.5 ? 1 : -1;
            }
        }
    }

    /**
     * Iterates over the set of subgraphs and, for those cases where both the true label and the prediction are
     * set, it calls the eachSubg method. The eachSubg is abstract --ie, to be defined for each need--, and it
     * receives the subgID, the true label, and the predicted class distribution.
     * eachSub implementations can access and modify the internal variable 'counter'. There is also a 'total' variable,
     * which automatically keeps track of the total number of times the eachSubg method is called.
     */
    private abstract class PredictionsIterator {
        public int total = 0;
        public double counter = 0;

        public abstract void eachSubg(String subgID, String trueValue, ProbDistribution classLabelDistr);

        public PredictionsIterator() {
            List<String> keys = new ArrayList<String>(subgToClassLabelDistrMap.keySet());
            for (int subgIdIdx = 0; subgIdIdx < keys.size(); subgIdIdx++) {
                String subgID = keys.get(subgIdIdx);
                String trueValue = subgToTrueClassLabelMap.get(subgID);
                ProbDistribution classLabelDistr = subgToClassLabelDistrMap.get(subgID);

                if (trueValue != null && classLabelDistr != null) {
                    total++;
                    eachSubg(subgID, trueValue, classLabelDistr);
                }
            }
        }
    }

}
