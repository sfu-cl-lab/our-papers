/**
 * $Id: DefaultStatisticModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.statistic;

import kdl.prox.db.Container;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The default statistic module allows uses to specify a burnIn time, to let the RDN move through
 * space before starting to record, and a skip time, to record every s iterations.
 */
public class DefaultStatisticModule implements RDNStatisticModule {

    protected static Logger log = Logger.getLogger(DefaultStatisticModule.class);

    public static int DEFAULT_BURNIN = 5;
    public static int DEFAULT_SKIP = 2;

    public int burnInSteps;
    public int skipSteps;

    private Map<RPT, Predictions> predictionMap;


    public DefaultStatisticModule() {
        this(DEFAULT_BURNIN, DEFAULT_SKIP);
    }

    public DefaultStatisticModule(int burnIn, int skipNum) {
        this.burnInSteps = burnIn;
        this.skipSteps = skipNum;
    }

    /**
     * Initialize empty set of predictions for each NST
     *
     * @param modelsToConts
     */
    public void startup(Map<RPT, Container> modelsToConts) {
        predictionMap = new HashMap<RPT, Predictions>();
        for (RPT rpt : modelsToConts.keySet()) {
            predictionMap.put(rpt, new Predictions());
        }
    }

    public void recordPrediction(RPT rpt, Map<String, String> rptClasses, int iteration) {
        if (isRecordIteration(iteration)) {
            Predictions predictions = predictionMap.get(rpt);

            for (Iterator<String> classIter = rptClasses.keySet().iterator(); classIter.hasNext();) {
                String subgId = classIter.next();
                if (predictions.getProbDistribution(subgId) == null) {
                    predictions.setPrediction(subgId, new DiscreteProbDistribution());
                }
                predictions.getProbDistribution(subgId).addAttributeValue(rptClasses.get(subgId));
            }
        }
    }

    public Map<RPT, Predictions> getPredictions() {
        return predictionMap;
    }

    public void cleanup() {
    }


    protected boolean isRecordIteration(int iteration) {
        if (iteration < burnInSteps) {
            return false;
        } else if (iteration == burnInSteps) {
            return true;
        }

        int firstOne = burnInSteps + skipSteps + 1;
        return (iteration - firstOne) % (skipSteps + 1) == 0;
    }

    public void setBurnInSteps(int burnInSteps) {
        this.burnInSteps = burnInSteps;
    }

    public void setSkipSteps(int skipSteps) {
        this.skipSteps = skipSteps;
    }

}
