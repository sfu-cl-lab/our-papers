/**
 * $Id: RPTTableCacher.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn;

import kdl.prox.db.Container;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This utility class maps the RPT's class labels to temporary attributes,
 * and re-maps the learned feature settings in each RPT's to use those temporary attributes.
 * <p/>
 * When done, cleanup() restores the original feature settings and removes the created attributes.
 */
public class RPTTableCacher {

    protected static Logger log = Logger.getLogger(RPTTableCacher.class);

    private HashMap<RPT, NSTCache> modelsCaches = new HashMap<RPT, NSTCache>();


    public void startup(Map<RPT, Container> modelsConts) {
        for (RPT rpt : modelsConts.keySet()) {
            modelsCaches.put(rpt, new NSTCache());
        }
    }


    public void cleanup() {
        for (RPT rpt : modelsCaches.keySet()) {
            modelsCaches.get(rpt).clear();
        }
    }

    public NSTCache getCache(RPT rpt) {
        return modelsCaches.get(rpt);
    }

    public void invalidateCachesWithString(String attrName) {
        for (RPT rpt : modelsCaches.keySet()) {
            modelsCaches.get(rpt).invalidateTablesWithString(attrName);
        }
    }
}
