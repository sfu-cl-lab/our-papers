/**
 * $Id: Correlation.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.util;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTUtil;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.monet.Connection;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: afast
 * Date: Mar 12, 2007
 * Time: 3:58:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Correlation {

    private static Logger log = Logger.getLogger(Correlation.class);


    public Correlation() {
    }


    public static double computeCorrelation(NST pairsNST, String attr1Name, String attr2Name, boolean isContinuous) {

        if (isContinuous) {
            return computeCorrelation(pairsNST, null, null);
        }

        DB.beginScope();

        //otherwise, discrete attributes
        Object[] rowHeaders = DB.getObjectAttrs().getAttrDataNST(attr1Name).getDistinctColumnValues("value").toArray();
        Object[] colHeaders = DB.getObjectAttrs().getAttrDataNST(attr2Name).getDistinctColumnValues("value").toArray();

        DB.endScope();

        return computeCorrelation(pairsNST, rowHeaders, colHeaders);

    }

    //pairsNST: attr1, attr2
    public static double computeCorrelation(NST pairsNST, Object[] rowHeaders, Object[] colHeaders) {

        if (rowHeaders != null && colHeaders != null) {

            List<String> contBatList = new ArrayList<String>();

            //loop through attr values, and filter the pairs to grab cell counts
            //create a bat of bats to pass to Monet
            for (Object val2 : colHeaders) {
                NST val2NST = pairsNST.filter("attr2 EQ '" + val2.toString() + "'");
                List<Double> counts = new ArrayList<Double>();
                for (Object val1 : rowHeaders) {
                    NST val1NST = val2NST.filter("attr1 EQ '" + val1.toString() + "'");
                    counts.add(val1NST.getRowCount() * 1.0);
                    val1NST.release();
                }
                contBatList.add(MonetUtil.createBATFromCollection(DataTypeEnum.DBL, counts));
                val2NST.release();
            }

            String contBats = MonetUtil.create("oid", DataTypeEnum.STR.toString());
            int i = 0;
            for (String val : contBatList) {
                Connection.executeCommand(contBats + ".insert(" + i++ + "@0, " + "str(" + NSTUtil.normalizeName(val) + "))");
            }

            double chisq = Double.parseDouble(Connection.readValue(contBats + ".chiSq().print();"));

            //release all intermediate BATs
            for (String val : contBatList) {
                Connection.releaseSavedVar(val);
            }
            Connection.releaseSavedVar(contBats);

            if (chisq > 0.0) {
                return StatUtil.contingencyCoefficient(chisq, pairsNST.getRowCount(), rowHeaders.length, colHeaders.length);
            } else {
                return 0.0;
            }
        } else {
            List<Double> x = pairsNST.selectRows("attr1").toDoubleList("attr1");
            List<Double> y = pairsNST.selectRows("attr2").toDoubleList("attr2");
            return continuousCorrelation(x, y);
        }
    }

    //calculate Pearsons continuous correlation coefficient
    public static double continuousCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size())
            return -1.0;
        double n = x.size();
        double sum_sq_x = 0.0;
        double sum_sq_y = 0.0;
        double sum_coproduct = 0.0;
        double mean_x = x.get(0);
        double mean_y = y.get(0);
        for (int i = 1; i < n; i++) {
            double sweep = (i - 1.0) / i;
            double delta_x = x.get(i) - mean_x;
            double delta_y = y.get(i) - mean_y;
            sum_sq_x = sum_sq_x + (delta_x * delta_x * sweep);
            sum_sq_y = sum_sq_y + (delta_y * delta_y * sweep);
            sum_coproduct = sum_coproduct + (delta_x * delta_y * sweep);
            mean_x = mean_x + (delta_x / i);
            mean_y = mean_y + (delta_y / i);
        }
        double pop_sd_x = Math.sqrt(sum_sq_x / n);
        double pop_sd_y = Math.sqrt(sum_sq_y / n);
        double cov_x_y = sum_coproduct / n;
        double correlation = cov_x_y / (pop_sd_x * pop_sd_y);
        return Math.abs(correlation);
    }

    public static NST getAllPairs(Container cont, String objName1, String attrName1, String objName2, String attrName2) {
        Assert.notNull(cont, "Container is null!");
        Assert.stringNotEmpty(objName1, "objName1 is empty!");
        Assert.stringNotEmpty(objName2, "objName2 is empty!");
        Assert.stringNotEmpty(attrName1, "attrName1 is empty!");
        Assert.stringNotEmpty(attrName2, "attrName2 is empty!");

        DB.beginScope();
        String attrType1 = DB.getObjectAttrs().getAttrDataNST(attrName1).project("value").getNSTColumnTypesAsString();
        String attrType2 = DB.getObjectAttrs().getAttrDataNST(attrName2).project("value").getNSTColumnTypesAsString();
        DB.endScope();

        log.debug("attrType1: " + attrType1 + ", attrType2: " + attrType2);


        NST pairsNST = new NST("attr1,attr2", attrType1 + "," + attrType2);
        DB.beginScope();

        NST allObj1NST = cont.getItemNSTByName(true, objName1);
        NST allObj2NST = cont.getItemNSTByName(true, objName2);

        NST allAttr1NST = DB.getAttrsForItems(allObj1NST, DB.getObjectAttrs(), attrName1 + " != nil", attrName1).project("subg_id," + attrName1);
        NST allAttr2NST = DB.getAttrsForItems(allObj2NST, DB.getObjectAttrs(), attrName2 + " != nil", attrName2).project("subg_id," + attrName2);

        pairsNST.insertRowsFromNST(allAttr1NST.join(allAttr2NST, "A.subg_id = B.subg_id", "A." + attrName1 + ",B." + attrName2));

        DB.endScope();

        return pairsNST;
    }

    public static NST getAllPairs(Container cont, String objName1, String attrName1, String objName2, String attrName2, int numSample) {
        Assert.notNull(cont, "Container is null!");
        Assert.stringNotEmpty(objName1, "objName1 is empty!");
        Assert.stringNotEmpty(objName2, "objName2 is empty!");
        Assert.stringNotEmpty(attrName1, "attrName1 is empty!");
        Assert.stringNotEmpty(attrName2, "attrName2 is empty!");

        DB.beginScope();
        String attrType1 = DB.getObjectAttrs().getAttrDataNST(attrName1).project("value").getNSTColumnTypesAsString();
        String attrType2 = DB.getObjectAttrs().getAttrDataNST(attrName2).project("value").getNSTColumnTypesAsString();
        DB.endScope();

        log.debug("attrType1: " + attrType1 + ", attrType2: " + attrType2);


        NST pairsNST = new NST("attr1,attr2", attrType1 + "," + attrType2);
        DB.beginScope();

        NST allObj1NST = cont.getItemNSTByName(true, objName1);
        NST allObj2NST = cont.getItemNSTByName(true, objName2);

        NST allAttr1NST = DB.getAttrsForItems(allObj1NST, DB.getObjectAttrs(), attrName1 + " != nil", attrName1).project("subg_id," + attrName1);
        NST allAttr2NST = DB.getAttrsForItems(allObj2NST, DB.getObjectAttrs(), attrName2 + " != nil", attrName2).project("subg_id," + attrName2);

        pairsNST.insertRowsFromNST(allAttr1NST.join(allAttr2NST, "A.subg_id = B.subg_id", "A." + attrName1 + ",B." + attrName2));

        DB.endScope();

        return pairsNST.addRandomColumn("rand").filter("rand RANDOM " + numSample);
    }

    public double getAutocorrelationThroughItem(RPTState state, String itemName) {

        DB.beginScope();

        // filteredContNST will get an NST of itemID, subgID, name
        // hard-coded to only use Objects as items
        //todo: do we want links to be used as items?
        NST filteredContNST = state.trainContainer.getItemNSTByName(true, itemName);
        //log.debug("filteredContNST");
        //filteredContNST.print();

        //select subg-subg pairs that share the same named item (e.g. actors)
        NST pathNST = filteredContNST.join(filteredContNST, "A.item_id  = B.item_id", "A.subg_id, B.subg_id");
        pathNST.renameColumns("subg1_id, subg2_id");
        //log.debug("pathNST");
        //pathNST.print();

        //select all pairs of subgraphs such that subg_id1<subg_id2
        NST filteredPathNST = pathNST.filter("subg1_id < subg2_id");
        //log.debug("filteredPathNST");
        //filteredPathNST.print();

        //if there are no rows then we set autocorrelation to 0
        double ac = 0.0;
        int numPaths = filteredPathNST.getRowCount();
        if (numPaths > 0) {
            //use the filtered paths to find total counts of each subgraph (in either column
            //of the subgraph pairs

            //add duplicate columns to enable aggregation
            filteredPathNST.addCopyColumn("subg1_id", "subg1_id_count");
            filteredPathNST.addCopyColumn("subg2_id", "subg2_id_count");

            //count the number of times subgraphs appear on either side of pair, using pathNST as base table
            //to ensure subgraphs with zero counts on a given side remain
            //call to distinct since base table duplicates rows
            NST subg1Counts = filteredPathNST.aggregate("count", "subg1_id", "subg1_id_count", pathNST).distinct();
            NST subg2Counts = filteredPathNST.aggregate("count", "subg2_id", "subg2_id_count", pathNST).distinct();

            //log.debug("subgCounts");
            //subg1Counts.print();
            //subg2Counts.print();

            //join counts of both sides and add total counts together
            NST subgCounts = subg1Counts.join(subg2Counts, "A.subg1_id = B.subg2_id", "subg1_id, subg1_id_count, subg2_id_count");
            //log.debug("subgCounts");
            //subgCounts.print();

            subgCounts = subgCounts.addArithmeticColumn("subg1_id_count + subg2_id_count", "int", "subg_count").project("subg1_id, subg_count");
            subgCounts.renameColumns("subg_id, count");
            //log.debug("total subgCounts");
            //subgCounts.print();

            //join these counts to the original subgraph pairs
            //subg1_id, subg2_id, count1, count2

            NST pathNSTWithCounts1 = filteredPathNST.join(subgCounts, "A.subg1_id = B.subg_id", "subg1_id, subg2_id, count");
            pathNSTWithCounts1.renameColumn("count", "count1");
            //log.debug("pathNSTWithCounts1");
            //pathNSTWithCounts1.print();

            NST pathNSTWithCounts = pathNSTWithCounts1.join(subgCounts, "A.subg2_id = B.subg_id", "subg1_id, subg2_id, count1, count");
            pathNSTWithCounts.renameColumn("count", "count2");
            //log.debug("pathNSTWithCounts");
            //pathNSTWithCounts.print();

            //get class label attribute values per subg
            //subg_id, value
            NST attrNST = state.nstCache.getTable("src" + state.classLabel.toString());
            //log.debug("attrNST");
            //attrNST.print();

            NST subgIdsCountsAndValsNST1 = pathNSTWithCounts.join(attrNST, "A.subg1_id = subg_id", "subg1_id, subg2_id, count1, count2, value");
            subgIdsCountsAndValsNST1.renameColumn("value", "value1");

            NST subgIdPairsCountsAndValsNST = subgIdsCountsAndValsNST1.join(attrNST, "A.subg2_id = subg_id", "subg1_id, subg2_id, count1, count2, value1, value");
            subgIdPairsCountsAndValsNST.renameColumn("value", "value2");

            log.debug("subgIdPairsCountsAndValsNST (" + subgIdPairsCountsAndValsNST.getRowCount() + ")");
            //subgIdPairsCountsAndValsNST.print();
            //log.debug("numPaths: " + numPaths);

            Object[] rowHeaders = DB.getObjectAttrs().getAttrDataNST(state.classLabel.getAttrName()).getDistinctColumnValues("value").toArray();
            Object[] colHeaders = DB.getObjectAttrs().getAttrDataNST(state.classLabel.getAttrName()).getDistinctColumnValues("value").toArray();

            //add weight column = (1 / (count1 + 1.0)) + (1 / (count2 + 1.0))

            if (subgIdPairsCountsAndValsNST.getRowCount() > 10000) {
                subgIdPairsCountsAndValsNST = subgIdPairsCountsAndValsNST.addRandomColumn("rand").filter("rand < " + 10000.0 / subgIdPairsCountsAndValsNST.getRowCount(), "*");
            }


            subgIdPairsCountsAndValsNST.addConstantColumn("one", "dbl", "1.0");
            subgIdPairsCountsAndValsNST.addArithmeticColumn("count1 + one", "dbl", "count1PlusOne");
            subgIdPairsCountsAndValsNST.addArithmeticColumn("count2 + one", "dbl", "count2PlusOne");
            subgIdPairsCountsAndValsNST.addArithmeticColumn("one / count1PlusOne", "dbl", "count1Portion");
            subgIdPairsCountsAndValsNST.addArithmeticColumn("one / count2PlusOne", "dbl", "count2Portion");
            subgIdPairsCountsAndValsNST.addArithmeticColumn("count1Portion + count2Portion", "dbl", "weight");

            NST pairsWithWeightsNST = subgIdPairsCountsAndValsNST.project("value1, value2, weight");

            ac = computeCorrelationWithWeights(pairsWithWeightsNST, rowHeaders, colHeaders);

            DB.endScope();
            if (Double.isNaN(ac) || Double.isInfinite(ac)) {
                ac = 0.0;
            }
        }
        log.debug("setting autocorrelation to " + ac + " for " + itemName);
        return ac;

    }


    //pairsWithWeightNST: value1, value2, weight
    public double computeCorrelationWithWeights(NST pairsWithWeightNST, Object[] rowHeaders, Object[] colHeaders) {
        List<String> contBatList = new ArrayList<String>();

        //loop through attr values, and filter the pairs to grab cell counts
        //create a bat of bats to pass to Monet
        for (Object val2 : colHeaders) {
            NST val2NST = pairsWithWeightNST.filter("value2 EQ '" + val2.toString() + "'");
            List<Double> counts = new ArrayList<Double>();
            for (Object val1 : rowHeaders) {
                NST weightNST = val2NST.filter("value1 EQ '" + val1.toString() + "'").aggregate("sum", "value1", "weight");
                if (weightNST.getRowCount() == 0) {
                    counts.add(0.0);
                } else {
                    ResultSet rs = weightNST.selectRows("value1 EQ '" + val1.toString() + "'", "weight");
                    rs.next();
                    counts.add(rs.getDouble(1));
                }
            }
            contBatList.add(MonetUtil.createBATFromCollection(DataTypeEnum.DBL, counts));
        }

        String contBats = MonetUtil.create("oid", DataTypeEnum.STR.toString());
        int i = 0;
        for (Iterator iterator = ((Collection) contBatList).iterator(); iterator.hasNext();) {
            String val = iterator.next().toString();
            Connection.executeCommand(contBats + ".insert(" + i++ + "@0, " + "str(" + NSTUtil.normalizeName(val) + "))");
        }

//        double chisq = Double.parseDouble(Connection.readValue(contBats + ".chiSq().print();"));
        double chisq = Double.parseDouble(Connection.readValue(contBats + ".gStat().print();"));
        if (chisq > 0.0) {
            //get sum of all cells in contingency table (total weight)
            double totalWeight = Double.parseDouble(Connection.readValue(pairsWithWeightNST.getNSTColumn("weight").getBATName() + ".sum().print()"));
            return StatUtil.contingencyCoefficient(chisq, totalWeight, rowHeaders.length, colHeaders.length);
        } else {
            return 0.0;
        }
    }

}
