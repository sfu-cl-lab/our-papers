/**
 * $Id: ModeEstimator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc.estimators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * This estimator adds only the most frequent value from each subgraph to the distribution
 * In order to estimate the probability per subgraph, it gets the probability of the most common value per subgraph.
 * For example, if the NST has the values
 * <p/>
 * subg_id value
 * ------- -----
 * 1  a
 * 1  b
 * 1  b
 * 2  c
 * <p/>
 * then prob. distribution is {b:50%, c:50%}. Also, if the subgAttrNSt is
 * <p/>
 * * subg_id value
 * ------- -----
 * 1  a
 * 2  b
 * 2  b
 * 2  c
 * <p/>
 * then the resulting probabilities per subgraph are:
 * <p/>
 * 1 -> 0.005 (unknown a / 2 possible values)
 * 2 -> 0.5   (known b)
 */
public class ModeEstimator implements Estimator {

    private static Logger log = Logger.getLogger(ModeEstimator.class);
    private static final double underflowMinProb = 1e-40;


    /**
     * Only save in the distribution the most frequent value per subpgrah.
     *
     * @param attrNST
     * @param observedProbDist
     */
    public void recordObservations(NST attrNST, ProbDistribution observedProbDist) {
        NST aggregate = attrNST.aggregate("mode", "subg_id", "value");
        observedProbDist.addAttributeValuesFromNST(aggregate);
        aggregate.release();
    }

    /**
     * Get the probs for the most common value observed in each subgraph.
     *
     * @param subgAttrNST
     * @param observedProbDistr
     * @param randSeed
     * @return
     */
    public HashMap<String, Double> getSmoothedProbsGivenAttribute(NST subgAttrNST, ProbDistribution observedProbDistr, int randSeed) {
        // aggregate first (getting the mode) and then get the probability for that most common element
        NST aggregateNST = subgAttrNST.aggregate("mode", "subg_id", "value");
        NST subgProbNST = EstimatorUtil.subgProbDistrToNST(aggregateNST, observedProbDistr);

        HashMap<String, Double> probMap = new HashMap<String, Double>();
        ResultSet resultSet = subgProbNST.selectRows("subg_id, prob");
        while (resultSet.next()) {
            probMap.put(resultSet.getString(1), new Double(Math.max(resultSet.getDouble(2), underflowMinProb)));
        }

        subgProbNST.release();
        aggregateNST.release();

        return probMap;
    }

}
