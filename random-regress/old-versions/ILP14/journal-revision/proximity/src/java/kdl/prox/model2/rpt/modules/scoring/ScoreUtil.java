/**
 * $Id: ScoreUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.scoring;

import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import org.apache.log4j.Logger;

/**
 * A routine that hides the computation of a particular statistic on the three NSTs
 */
public class ScoreUtil {

    protected static Logger log = Logger.getLogger(ScoreUtil.class);

    private ScoreUtil() {
    }

    public static String computeStatistic(String statistic, NST labels, NST weights, NST matches) {
        return Connection.readValue(statistic + "(" +
                labels.getTwoNSTColumns("subg_id", "value") + "," +
                weights.getTwoNSTColumns("subg_id", "weight") + "," +
                matches.getTwoNSTColumns("subg_id", "match") +
                ").print()");
    }
}
