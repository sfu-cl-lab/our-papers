/**
 * $Id: InitFromRPTPredictionsModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.init;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializes the class label attributes with samples of the predictions that come
 * from applying the RPT on the corresponding containers.
 */
public class InitFromRPTPredictionsModule implements RDNInitModule {

    protected static Logger log = Logger.getLogger(InitFromRPTPredictionsModule.class);

    Map<RPT, Container> modelsConts;
    Map<RPT, RPT> modelsToInitRPTs;

    public InitFromRPTPredictionsModule(Map<RPT, RPT> modelsToInitRPTs) {
        this.modelsToInitRPTs = new HashMap<RPT, RPT>(modelsToInitRPTs);
    }

    public void startup(Map<RPT, Container> modelsConts) {
        this.modelsConts = new HashMap<RPT, Container>(modelsConts);
    }

    public void seedLabels() {
        for (RPT rpt : modelsConts.keySet()) {
            final Container container = modelsConts.get(rpt);
            final String itemName = rpt.getClassLabel().getItemName();
            final String tempAttr = rpt.getClassLabel().getAttrName();

            // get predictions on this container
            RPT initRPT = modelsToInitRPTs.get(rpt);
            Assert.notNull(initRPT, "no initializing RPT found for model " + rpt);
            Predictions predictions = initRPT.apply(modelsConts.get(rpt));

            DB.beginScope();
            NST attrNST = DB.getObjectAttrs().getAttrDataNST(tempAttr);
            ResultSet resultSet = container.getItemNSTByName(true, itemName).selectRows("subg_id, item_id");
            while (resultSet.next()) {
                String subg_id = resultSet.getString(1);
                Integer item_id = resultSet.getOID(2);
                attrNST.insertRow("" + item_id + "," + Util.quote(predictions.getSampledClass(subg_id)));
            }
            DB.endScope();
        }
    }


    public void cleanup() {
    }
}
