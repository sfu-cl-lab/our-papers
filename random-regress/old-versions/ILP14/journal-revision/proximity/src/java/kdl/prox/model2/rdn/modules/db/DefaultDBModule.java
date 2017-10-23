/**
 * $Id: DefaultDBModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.db;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A DB module hides all the complexity of managing attributes for the RDN, updating their values
 * after each iteration, etc.
 * <p/>
 * This DB module has a cache of current predictions for each RPT,
 * to avoid going to Monet for every single update.
 */
public class DefaultDBModule implements RDNDBModule {

    protected static Logger log = Logger.getLogger(DefaultDBModule.class);

    Map<RPT, Container> modelsConts;
    Map<RPT, Map<String, String>> modelsToCurrLabelsCache;


    public void startup(Map<RPT, Container> modelsConts) {
        this.modelsConts = new HashMap<RPT, Container>(modelsConts);

        // init cache to empty
        modelsToCurrLabelsCache = new HashMap<RPT, Map<String, String>>();
        for (RPT model : modelsConts.keySet()) {
            modelsToCurrLabelsCache.put(model, new HashMap<String, String>());
        }
    }

    public void update(RPT rpt, Map<String, String> subgClasses, int iteration) {
        final Container container = modelsConts.get(rpt);
        final String itemName = rpt.getClassLabel().getItemName();
        final String attrName = rpt.getClassLabel().getAttrName();

        NST allItemsNST = container.getItemNSTByName(true, itemName);
        NST attrNST = DB.getObjectAttrs().getAttrDataNST(attrName);
        Map<String, String> currentClasses = modelsToCurrLabelsCache.get(rpt);

        for (Iterator<String> classIter = subgClasses.keySet().iterator(); classIter.hasNext();) {
            String subgId = classIter.next();
            final String sampleClass = subgClasses.get(subgId);
            final String currentClass = currentClasses.get(subgId);
            if (currentClass == null || !currentClass.equals(sampleClass)) {
                NST itemsNST = allItemsNST.filter("subg_id = '" + subgId + "'", "item_id");
                attrNST.replace("id IN " + itemsNST.getNSTColumn("item_id"), "value", Util.quote(sampleClass));
                itemsNST.release();
                currentClasses.put(subgId, sampleClass);
            }
        }
        attrNST.release();
        allItemsNST.release();
    }

    public void cleanup() {
    }
}
