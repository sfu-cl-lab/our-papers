/**
 * $Id: AttributeGenerator.java 3726 2007-11-07 19:50:35Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.attributes;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rdn.RDN;
import kdl.prox.model2.rdn.modules.listeners.LoggingListener;
import kdl.prox.model2.rdn.modules.statistic.DefaultStatisticModule;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.python.core.PyDictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods to generate a fixed structure along with attribute values that follow a given distribution.
 * To use, first create an instance of this class, call the structure generation method of choice once,
 * then call the attribute creation method.
 * <p/>
 * Note that all attributes generated are 'str' (string) ones. Non-string attributes are currently unsupported.
 * <p/>
 */
public class AttributeGenerator {

    private static final Logger log = Logger.getLogger(AttributeGenerator.class);

    private Map<File, RPT> queriesToModels = new HashMap<File, RPT>();
    private Map<RPT, Container> modelsToContainers = new HashMap<RPT, Container>();
    private List<String> attrsToDelete;

    public AttributeGenerator(PyDictionary queriesToRPTs, int maxIters) {
        this(Util.mapFromPyDict(queriesToRPTs), maxIters);
    }

    public AttributeGenerator(Map<File, RPT> queriesToRPTs, int maxIters) {
        Assert.condition(queriesToRPTs.keySet().size() > 0, "Must specify at least one query and its RPT");
        queriesToModels = queriesToRPTs;

        // Execute queries into temp containers, run RDN, save its predictions, and delete temp containers
        runQueriesAndSaveContainers();
        Map<RPT, Predictions> predictionsMap = runRDN(maxIters);
        savePredictions(predictionsMap);
        deleteTempContainersAndAttrs();
    }

    /**
     * Delete the containers and attributes that were created
     */
    private void deleteTempContainersAndAttrs() {
        Container rootContainer = DB.getRootContainer();
        for (RPT model : modelsToContainers.keySet()) {
            rootContainer.deleteChild(modelsToContainers.get(model).getName());
        }
        for (String attrName : attrsToDelete) {
            DB.getObjectAttrs().deleteAttribute(attrName);
        }
    }

    /**
     * Run the queries and save the results in temporary containers
     */
    private void runQueriesAndSaveContainers() {
        for (File queryFile : queriesToModels.keySet()) {
            String containerName = DB.generateTempContainerName();
            QueryGraph2CompOp.runQuery(queryFile, null, containerName);

            // Save the resulting container in modelsToContainers
            RPT model = queriesToModels.get(queryFile);
            Container container = DB.getRootContainer().getChild(containerName);
            modelsToContainers.put(model, container);
        }
    }

    /**
     * Run the RDN with the given RPTs on the temp containers
     *
     * @param maxIterations specifies the length of the Gibbs chain
     * @return the predictions the RDN makes
     */
    private Map<RPT, Predictions> runRDN(int maxIterations) {
        // define the attributes for the class labels if they don't exist
        attrsToDelete = new ArrayList<String>();
        for (RPT model : modelsToContainers.keySet()) {
            String attrName = model.getClassLabel().getAttrName();
            String attrType = model.getClassLabel().getType().toString();
            if (!DB.getObjectAttrs().isAttributeDefined(attrName)) {
                DB.getObjectAttrs().defineAttribute(attrName, attrType);
                attrsToDelete.add(attrName);
            }
        }

        RDN rdn = new RDN();
        rdn.statisticModule = new DefaultStatisticModule(0, 0);
        rdn.addListener(new LoggingListener());
        return rdn.apply(modelsToContainers, maxIterations);
    }


    /**
     * Save attributes with the convention: ItemName_AttrName_label
     *
     * @param predictionMap
     */
    private void savePredictions(Map<RPT, Predictions> predictionMap) {
        for (RPT rpt : modelsToContainers.keySet()) {
            Container container = modelsToContainers.get(rpt);
            Predictions predictions = predictionMap.get(rpt);

            final String itemName = rpt.getClassLabel().getItemName();
            final String rptAttrName = rpt.getClassLabel().getAttrName();
            final String attrName = itemName + "_" + rptAttrName + "_label";
            String attrType = DB.getObjectAttrs().getAttrTypeDef(rpt.getClassLabel().getAttrName());
            DB.getObjectAttrs().defineAttributeOrClearValuesIfExists(attrName, attrType);

            // get a list of all items of the rpt class label in the container
            NST allItemsNST = container.getItemNSTByName(true, itemName);
            NST attrNST = DB.getObjectAttrs().getAttrDataNST(attrName);

            for (String subgId : predictions.getSampledClasses().keySet()) {
                final String sampleClass = predictions.getInferredClass(subgId);
                NST itemsNST = allItemsNST.filter("subg_id = '" + subgId + "'", "item_id");
                List ids = itemsNST.selectRows().toOIDList(1);  // this only returns 1 item, but it is in a list
                attrNST.insertRow(ids.get(0) + "," + sampleClass);
                itemsNST.release();
            }
            attrNST.release();
            allItemsNST.release();
        }
    }

}
