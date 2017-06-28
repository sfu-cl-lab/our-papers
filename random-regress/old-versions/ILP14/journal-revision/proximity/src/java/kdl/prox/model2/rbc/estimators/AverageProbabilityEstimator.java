/**
 * $Id: AverageProbabilityEstimator.java 3658 2007-10-15 16:29:11Z schapira $
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
 * This estimator adds all values to the probability distribution.
 * In order to estimate the probability per subgraph, it gets the probability for each observed value in the subgraph,
 * multiplies them all together, and then divided by the number of observed values. For example, if the NST has the values
 * <p/>
 * subg_id value
 * ------- -----
 * 1  a
 * 1  b
 * 2  c
 * <p/>
 * and the prob. distribution is {a:30%, b: 60%, c:10%}, the probs. per subgraph would be
 * <p/>
 * 1 -> 30% * 60%
 * 2 -> 10%
 */
public class AverageProbabilityEstimator implements Estimator {

    private static Logger log = Logger.getLogger(AverageProbabilityEstimator.class);
    private static final double underflowMinProb = 1e-40;

    /**
     * Records all observed values in the distribution
     *
     * @param attrNST
     * @param observedProbDist
     */
    public void recordObservations(NST attrNST, ProbDistribution observedProbDist) {
        observedProbDist.addAttributeValuesFromNST(attrNST);
    }

    /**
     * Multiply probs for each value observed in each subgraph.
     *
     * @param subgAttrNST
     * @param observedProbDistr
     * @param randSeed
     * @return
     */
    public HashMap<String, Double> getSmoothedProbsGivenAttribute(NST subgAttrNST, ProbDistribution observedProbDistr, int randSeed) {
        // compute probs. for each value in each subgraph,
        // and aggregate by subg_id, multiplying, and count the values
        NST subgProbNST = EstimatorUtil.subgProbDistrToNST(subgAttrNST, observedProbDistr);
        NST aggregateNST = subgProbNST.aggregate("prod", "subg_id", "prob");
        NST countNST = subgProbNST.aggregate("count", "subg_id", "prob");
        NST allNST = aggregateNST.join(countNST, "A.subg_id = B.subg_id", "A.subg_id AS subg_id, A.prob as prob, B.prob AS cnt");

        HashMap<String, Double> probMap = new HashMap<String, Double>();
        ResultSet resultSet = allNST.selectRows("subg_id, prob, cnt");
        while (resultSet.next()) {
            probMap.put(resultSet.getString(1), new Double(Math.max(resultSet.getDouble(2) / resultSet.getDouble(3), underflowMinProb)));
        }

        subgProbNST.release();
        aggregateNST.release();
        countNST.release();
        allNST.release();

        return probMap;
    }

}
