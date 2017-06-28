/**
 * $Id: SGIUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.util;

import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTTypeEnum;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * Defines static utilities to manipulate SGI tables.
 * Used by qGraph Transformations
 */
public class SGIUtil {

    private static Logger log = Logger.getLogger(SGIUtil.class);

    private SGIUtil() {
        // disallow instances
    }


    /**
     * Copies into a new SGI table rows from fromSGI where a given filter
     * is matched. If filter is null, copies all the rows.
     *
     * @param intoSGI
     * @param fromSGI
     * @param filterDef
     */
    public static void copySelectedRowsIntoSGITable(NST intoSGI, NST fromSGI, String filterDef) {

        Assert.notNull(intoSGI, "null intoSGI");
        Assert.notNull(fromSGI, "null fromSGI");

        NST toKeepSGI;
        if (filterDef != null) {
            toKeepSGI = fromSGI.filter(filterDef);
        } else {
            toKeepSGI = fromSGI;
        }
        intoSGI.insertRowsFromNST(toKeepSGI);
        if (filterDef != null) {
            toKeepSGI.release();
        }
    }


    /**
     * Same as copyRowsIntoSGITableWithNewName, but inserts only distinct values
     * for itemIdColName, subgIdColName
     *
     * @param intoSGI
     * @param fromSGI
     * @param itemIdColName
     * @param subgIdColName
     * @param newNameValue
     */
    public static void copyDistinctRowsIntoSGITableWithNewName(NST intoSGI,
                                                               NST fromSGI,
                                                               String itemIdColName,
                                                               String subgIdColName,
                                                               String newNameValue) {
        Assert.notNull(fromSGI, "null fromSGI");
        Assert.stringNotEmpty(itemIdColName, "empty itemIdColName");
        Assert.stringNotEmpty(subgIdColName, "empty subgIdColName");

        // Do a group by these two columns, and filter distinct
        fromSGI.groupBy(itemIdColName + "," + subgIdColName, "distinct_group_id");
        NST distinctNST = fromSGI.distinct("distinct_group_id");

        // and now copy
        copyRowsIntoSGITableWithNewName(intoSGI, distinctNST, itemIdColName,
                subgIdColName, newNameValue);
        distinctNST.release();
    }


    /**
     * Inserts into a tempSGI table rows that come from
     * the item_id and subg_id columns of another tempSGI NST, and
     * name is a new name given in newNameValue
     *
     * @param intoSGI
     * @param fromSGI
     * @param itemIdColName
     * @param subgIdColName
     * @param newNameValue
     */
    public static void copyRowsIntoSGITableWithNewName(NST intoSGI,
                                                       NST fromSGI,
                                                       String itemIdColName,
                                                       String subgIdColName,
                                                       String newNameValue) {

        Assert.notNull(intoSGI, "null intoSGI");
        Assert.notNull(fromSGI, "null fromSGI");
        Assert.stringNotEmpty(itemIdColName, "empty itemIdColName");
        Assert.stringNotEmpty(subgIdColName, "empty subgIdColName");
        Assert.stringNotEmpty(newNameValue, "empty new name");

        NST tempSGI = fromSGI.project(itemIdColName + "," + subgIdColName);
        tempSGI.addConstantColumn("name", "str", newNameValue);
        intoSGI.insertRowsFromNST(tempSGI);
        tempSGI.release();
    }

    /**
     * Creates a tempSGI table.
     *
     * @return
     * @
     */
    public static NST createTempSGINST() {
        return new NST(NSTTypeEnum.SGI_NST_COL_NAMES, NSTTypeEnum.SGI_NST_COL_TYPES);
    }

    /**
     * Filters an NST so that only rows where the value of the given colName
     * is within the range are preserved.
     * -1 in max means no upper limit
     *
     * @param theNST
     * @param colName
     * @param min
     * @param max
     * @return
     */
    public static NST getRowsWithinRange(NST theNST, String colName, int min, int max) {
        Assert.notNull(theNST, "empty NST");
        Assert.stringNotEmpty(colName, "empty colName");
        Assert.condition(min > -1 || max > -1, "Both ends are -1");

        // create the filter
        if (min != -1 && max == -1) {
            return theNST.filter(colName + " GE " + min);
        } else if (min == -1 && max != -1) {
            return theNST.filter(colName + " LE " + max);

        } else { // min != -1 && max != -1)
            return theNST.filter(colName + " BETWEEN " + min + "-" + max);

        }
    }

    /**
     * Gets a tempSGI NST and returns a filtered NST such that
     * only objects/links with a given name (in the subgraph, that is) are present.
     *
     * @param tempObjSGINST
     * @param name
     * @return
     * @throws kdl.prox.monet.MonetException
     */
    public static NST getSubgraphItemsWithName(NST tempObjSGINST, String name) {
        return tempObjSGINST.filter("name EQ '" + name + "'");
    }

    /**
     * Pulls subgraphs from a consolidated vertex into a new consolidated vertex.
     * Gets the subgraphs from origSubgNST that are specified in newSubgNST
     * (maybe with duplicates, if a given subgraph from origSubgNST should now
     * be copied over to more than one new subgraph).
     * <p/>
     * Works by doing a join of the two NSTs based on
     * origSubgNST.subg_id = newSubgNST.baseColumnNameOnNewSubgNST
     * <p/>
     * The new subgraphs that will be copied over get the new subg_id specified
     * in subgIDRecodeColumnName
     *
     * @param origSubgNST
     * @param recodeNST
     * @param destVertexNST
     */
    public static void pullSubgraphsFromConsolidatedVertexIntoNewVertex(NST origSubgNST,
                                                                        NST recodeNST,
                                                                        NST destVertexNST) {
        NST recodedNST = origSubgNST.join(recodeNST, "subg_id EQ old_id", "item_id, new_id, name");
        destVertexNST.insertRowsFromNST(recodedNST);
        recodedNST.release();
    }
}
