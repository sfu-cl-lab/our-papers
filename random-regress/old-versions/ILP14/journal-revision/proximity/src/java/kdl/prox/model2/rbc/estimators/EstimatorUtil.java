/**
 * $Id: EstimatorUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc.estimators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.ProbDistribution;


public class EstimatorUtil {

    // Save the prob. distributions for each observed value in an NST [value, flt]
    /**
     * Create an NST [subg_id, prob] with the probabilities of each value in each subgraph.
     * For example, if the subgAttrNST is
     * <p/>
     * subg_id value
     * ------- -----
     * 1 a
     * 1 a
     * 1 b
     * 2 c
     * <p/>
     * and the probs are {a: 0.5, b:0.3, c:0.2}
     * <p/>
     * then the resulting NST is, obviously:
     * <p/>
     * subg_id prob
     * ------- -----
     * 1 0.5
     * 1 0.5
     * 1 0.3
     * 2 0.2
     *
     * @param subgAttrNST
     * @param probDist
     * @return
     */
    public static NST subgProbDistrToNST(NST subgAttrNST, ProbDistribution probDist) {
        NST probNST = new NST("value, prob", subgAttrNST.getNSTColumn("value").getType() + ", flt");
        for (String value : subgAttrNST.getDistinctColumnValues("value")) {
            probNST.insertRow(value + ", " + probDist.getSmoothedProbability(value));
        }

        // now join it with the subgAttrNST => [subg_id, prob]
        NST subgProbNST = subgAttrNST.join(probNST, "A.value = B.value", "subg_id, prob");

        probNST.release();
        return subgProbNST;
    }

}
