/**
 * $Id: Estimator.java 3703 2007-11-02 16:06:44Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc.estimators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ProbDistribution;

import java.util.HashMap;

/**
 * An estimator is a mechanism for adding values to a probability distribution, and then computing the
 * probability of each one of the different values
 */
public interface Estimator {

    /**
     * Save the values observed in the valueNST in the targetDistribution
     * The NST is expected to have [subg_id, value]
     *
     * @param valueNST
     * @param targetDistribution
     */
    public void recordObservations(NST valueNST, ProbDistribution targetDistribution);

    /**
     * Given an NST with a set of observed values per subgraph, and a probability distribution which presumably saves
     * precomputed probabilities of those values, it returns a map <subgid -> probability> for each subgraph.
     * For example, if the NST has the values
     * <p/>
     * subg_id value
     * ------- -----
     * 1  a
     * 1  b
     * 2  c
     * <p/>
     * and the prob. distribution is {a:30%, b: 60%, c:10%}, a possible result in the map, from an estimator that
     * multiplies probabilities, would be:
     * <p/>
     * 1 -> 30% * 60%
     * 2 -> 10%
     *
     * @param subgAttrNST       with [subg_id, value]
     * @param observedProbDistr
     * @param randSeed
     * @return a <subgid->probability> map
     */
    public HashMap<String, Double> getSmoothedProbsGivenAttribute(NST subgAttrNST, ProbDistribution observedProbDistr, int randSeed);

}
