/**
 * $Id: DefaultInitModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.init;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes the class labels randomly, drawing samples from the RPT's class label distribution
 */
public class DefaultInitModule implements RDNInitModule {

    protected static Logger log = Logger.getLogger(DefaultInitModule.class);

    Map<RPT, Container> modelsConts;


    public void startup(Map<RPT, Container> modelsConts) {
        this.modelsConts = new HashMap<RPT, Container>(modelsConts);
    }

    public void seedLabels() {
        for (RPT rpt : modelsConts.keySet()) {
            final Container container = modelsConts.get(rpt);
            final String itemName = rpt.getClassLabel().getItemName();
            final String tempAttr = rpt.getClassLabel().getAttrName();

            RPTNode rootNode = rpt.getRootNode();
            DiscreteProbDistribution distribution = (DiscreteProbDistribution) rootNode.getClassLabelDistribution();
            NST attrNST = DB.getObjectAttrs().getAttrDataNST(tempAttr);

            List ids = container.getItemNSTByName(true, itemName).selectRows("item_id").toOIDList(1);
            for (int idIdx = 0; idIdx < ids.size(); idIdx++) {
                Integer id = (Integer) ids.get(idIdx);
                attrNST.insertRow("" + id + "," + Util.quote(distribution.sample()));
            }
        }
    }


    public void cleanup() {
    }
}
