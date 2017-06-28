/**
 * $Id: RDN.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rdn.modules.db.DefaultDBModule;
import kdl.prox.model2.rdn.modules.db.RDNDBModule;
import kdl.prox.model2.rdn.modules.init.DefaultInitModule;
import kdl.prox.model2.rdn.modules.init.RDNInitModule;
import kdl.prox.model2.rdn.modules.listeners.RDNListenerModule;
import kdl.prox.model2.rdn.modules.statistic.DefaultStatisticModule;
import kdl.prox.model2.rdn.modules.statistic.RDNStatisticModule;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.python.core.PyDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RDN implements a Relational Dependency Network
 * <p/>
 * J.ÊNeville and D.ÊJensen. Relational dependency networks.
 * Journal of Machine Learning Research, 8, 2007.
 * <p/>
 * RDNs are a relational extension to dependency networks. Dependency networks are
 * collections of conditional probability distributions learned independently from
 * data.  If the data are large enough then the combination of the CPDs specifies
 * a coherent joint distribution.
 * <p/>
 * Functionality is provided through extendable modules. Please see rdn.modules
 * package for more details. The ListenerModules allow operations to run at specific
 * iterations in the Gibbs chain. This can be used for updating the database, saving
 * a sample from the joint distribution, updating a posterior distribution, or
 * logging progress. See rdn.modules.listeners for some useful defaults.
 */

public class RDN {

    private static Logger log = Logger.getLogger(RDN.class);

    public RPTAttrNameMapper attrNameMapper = new RPTAttrNameMapper();
    public RPTTableCacher tableCacher = new RPTTableCacher();
    public RDNDBModule dbModule = new DefaultDBModule();
    public RDNInitModule initModule = new DefaultInitModule();
    public RDNStatisticModule statisticModule = new DefaultStatisticModule();
    public List<RDNListenerModule> listeners = new ArrayList<RDNListenerModule>();

    /**
     * Creates an RDN with the default modules.
     * These modules are public, so you can set them directly
     */
    public RDN() {
    }

    public void addListener(RDNListenerModule listener) {
        listeners.add(listener);
    }

    public void removeListener(RDNListenerModule listener) {
        listeners.remove(listener);
    }

    /**
     * Apply the RDN to a set of containers one for each class label.
     *
     * @param modelsToTestConts
     * @param maxIterations
     * @return
     */
    public Map<RPT, Predictions> apply(Map<RPT, Container> modelsToTestConts, int maxIterations) {
        DB.beginScope();
        startupModules(modelsToTestConts);

        // seed the class labels
        initModule.seedLabels();

        // for each gibbs iteration, apply each model on its container
        // sample the predictions, save them in the database,
        // and record them for RDN's prediction
        // The NST caches allows us to share featuresettings between calls to RPT.apply,
        // but the tables for the changed class labels need to be invalidated after an update to the DB!
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            for (RPT rpt : modelsToTestConts.keySet()) {
                Container testContainer = modelsToTestConts.get(rpt);

                NSTCache rptCache = tableCacher.getCache(rpt);

                Predictions predictions = rpt.apply(testContainer, rptCache);
                Map sampledClasses = predictions.getSampledClasses();
                dbModule.update(rpt, sampledClasses, iteration);
                statisticModule.recordPrediction(rpt, sampledClasses, iteration);

                tableCacher.invalidateCachesWithString(rpt.getClassLabel().getAttrName());
            }

            // announce completion of cycle to the listeners
            announceCycle(iteration);
        }

        Map<RPT, Predictions> predictions = statisticModule.getPredictions();

        cleanupModules();
        DB.endScope();

        return predictions;
    }

    public Map<RPT, Predictions> apply(PyDictionary modelsToTestConts, int maxIterations) {
        return apply(Util.mapFromPyDict(modelsToTestConts), maxIterations);
    }


    private void announceCycle(int iteration) {
        for (RDNListenerModule listener : listeners) {
            listener.cycle(iteration);
        }
    }

    private void cleanupModules() {
        statisticModule.cleanup();
        initModule.cleanup();
        dbModule.cleanup();
        tableCacher.cleanup();
        attrNameMapper.cleanup();
        for (RDNListenerModule listener : listeners) {
            listener.cleanup();
        }
    }

    private void startupModules(Map<RPT, Container> modelsToTestConts) {
        attrNameMapper.startup(modelsToTestConts);
        tableCacher.startup(modelsToTestConts);
        dbModule.startup(modelsToTestConts);
        initModule.startup(modelsToTestConts);
        statisticModule.startup(modelsToTestConts);
        for (RDNListenerModule listener : listeners) {
            listener.startup(modelsToTestConts);
        }
    }


}
