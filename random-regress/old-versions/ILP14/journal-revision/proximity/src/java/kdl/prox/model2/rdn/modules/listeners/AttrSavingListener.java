/**
 * $Id: AttrSavingListener.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.listeners;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.rdn.RDN;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Simply logs every x cycles
 */
public class AttrSavingListener implements RDNListenerModule {

    private static Logger log = Logger.getLogger(RDN.class);

    public static int DEFAULT_SAVE_FREQUENCY = 5;
    public static String DEFAULT_ATTR_PREFIX = "rdn_iter";

    protected int saveFrequency;
    protected String attrPrefix;

    Map<RPT, Container> modelsConts;


    public AttrSavingListener() {
        this(DEFAULT_SAVE_FREQUENCY, DEFAULT_ATTR_PREFIX);
    }

    public AttrSavingListener(int frequency) {
        this(frequency, DEFAULT_ATTR_PREFIX);
    }

    public AttrSavingListener(String prefix) {
        this(DEFAULT_SAVE_FREQUENCY, prefix);
    }

    public AttrSavingListener(int frequency, String prefix) {
        this.saveFrequency = frequency;
        this.attrPrefix = prefix;
    }

    public void startup(Map<RPT, Container> modelsConts) {
        this.modelsConts = new HashMap<RPT, Container>(modelsConts);
    }


    public void cycle(int iteration) {
        if ((iteration % saveFrequency) == 0) {
            log.info("Saving attributes from RDN Iteration: " + iteration);
            for (RPT rpt : modelsConts.keySet()) {
                String currentAttr = rpt.getClassLabel().getAttrName();
                String savedAttr = attrPrefix + "_" + iteration + "_" + currentAttr;
                log.info("  " + savedAttr);
                DB.getObjectAttrs().copyAttribute(currentAttr, savedAttr);
            }
        }
    }

    public void cleanup() {
    }
}
