/**
 * $Id: RDNStatisticModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.statistic;

import kdl.prox.db.Container;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rpt.RPT;

import java.util.Map;

/**
 * A statistic module keeps track of the predictions for classes coming out of the RPTs
 * and then returns predictions of its own after the application of the RDN is done
 */
public interface RDNStatisticModule {

    public void startup(Map<RPT, Container> modelsToConts);

    public void recordPrediction(RPT rpt, Map<String, String> rptClasses, int iteration);

    public Map<RPT, Predictions> getPredictions();

    public void cleanup();

}
