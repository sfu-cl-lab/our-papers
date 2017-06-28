/**
 * $Id: NSTUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.dbmgr;

import kdl.prox.db.DB;
import kdl.prox.monet.Connection;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;

/**
 * A series of utilites to make the processing of internal NST structures easier
 */
public class NSTUtil {

    private static Logger log = Logger.getLogger(NSTUtil.class);

    private NSTUtil() {
    }

    /**
     * From a colList definition, it returns an array of column names. Format:
     * <p/>
     * - list of column names, separated by commas
     * (e.g.  col1, col2, col3)
     * <p/>
     * - * returns all the columns in this NST
     *
     * @param nst
     * @param colList
     */
    public static String[] colListToArray(NST nst, String colList) {
        colList = colList.trim();
        if (colList.length() == 0) {
            return new String[]{};
        }
        if ("*".equals(colList)) {
            colList = nst.getNSTColumnNamesAsString();
        }
        String[] cols = colList.split(",");
        for (int i = 0; i < cols.length; i++) {
            cols[i] = cols[i].trim();
        }
        return cols;
    }


    /**
     * From a colListSped, it returns a map between old column names, and new column names. Format:
     * <p/>
     * - list of column names, separated by commas, with optional AS modifiers
     * (e.g.  col1, col2 AS xx, col3)
     * <p/>
     * <p/>
     * - * returns all the columns in this NST, with their current names
     *
     * @param nst
     * @param colListSpec
     * @return an array with two elements per row, one for the old name and one for the AS name
     */
    public static String[][] colListToNewNameMap(NST nst, String colListSpec) {
        colListSpec = colListSpec.trim();
        if (colListSpec.length() == 0) {
            return new String[][]{};
        }
        if ("*".equals(colListSpec)) {
            colListSpec = nst.getNSTColumnNamesAsString();
        }
        String[] cols = colListSpec.split(",");
        String[][] map = new String[cols.length][2];
        for (int i = 0; i < cols.length; i++) {
            String oldColName = cols[i].trim();
            String newColName = oldColName;
            int asIndex = oldColName.toUpperCase().indexOf(" AS ");
            if (asIndex != -1) {
                oldColName = oldColName.substring(0, asIndex);
                newColName = newColName.substring(asIndex + 4);
            }
            map[i][0] = oldColName;
            map[i][1] = newColName;
        }
        return map;
    }

    protected static String[] combineJoinedNSTColNames(NST thisNST, NST otherNST) {
        // create a new array with the names of columns for the new NST
        List myColumnNames = thisNST.getNSTColumnNames();
        List otherColumnNames = otherNST.getNSTColumnNames();
        String[] newColumnNames = new String[myColumnNames.size() + otherColumnNames.size()];
        int idx = 0;
        // Rename first NST's columns to A. format if they are repeated in second NST
        for (int i = 0; i < myColumnNames.size(); i++) {
            String colName = (String) myColumnNames.get(i);
            if (otherColumnNames.contains(colName)) {
                newColumnNames[idx] = "A." + colName;
            } else {
                newColumnNames[idx] = colName;
            }
            idx++;
        }
        // Rename second NST's columns to B. format if they are repeated in first NST
        for (int i = 0; i < otherColumnNames.size(); i++) {
            String colName = (String) otherColumnNames.get(i);
            if (myColumnNames.contains(colName)) {
                newColumnNames[idx] = "B." + colName;
            } else {
                newColumnNames[idx] = colName;
            }
            idx++;
        }
        return newColumnNames;
    }

    /**
     * Used by join, intersect, and those ops the required a col1 EQ col2 paramter
     *
     * @param filterDef
     * @return a string array
     */
    protected static String[] getJoinColumns(String filterDef) {
        // First of all, get the names of the columns to join
        String[] filterCols = filterDef.trim().split("(\\s)+");
        if (filterCols.length == 1) {
            return new String[]{filterCols[0], filterCols[0]};
        }
        if (filterCols.length != 3 ||
                ComparisonOperatorEnum.enumForString(filterCols[1].toUpperCase()) != ComparisonOperatorEnum.EQ) {
            throw new IllegalArgumentException("Only the EQ operator is allowed for joins. " +
                    "Specify col1 EQ col2: " + filterDef);
        }
        return new String[]{filterCols[0], filterCols[2]};
    }

    protected static String getNSTColumnInfoAsString(NST nst, String infoType) {
        Assert.condition(!nst.isReleased(), "illegal operation for released NST");
        String colList = "";
        boolean isName = "name".equalsIgnoreCase(infoType);
        for (int i = 0; i < nst.getColumnCount(); i++) {
            NSTColumn thisColumn = nst.getNSTColumn(i);
            colList = colList + (isName ? thisColumn.getName() : thisColumn.getType().toString()) + ",";
        }
        return colList.substring(0, colList.length() - 1);
    }

    public static NST getTopRows(NST table, String baseColName, String endColName, String sortCols, String range) {
        Assert.condition(!table.isColumnExists("group_id") && !table.isColumnExists("pair_cnt"),
                "NST cannot have columns group_id or pair_cnt");

        log.debug("Grouping and counting");
        table.groupBy(baseColName + "," + endColName).addCountColumn("group_id", "pair_cnt");

        log.debug("Sorting for faster selections");
        NST toOptimize = table.sort(baseColName + ", pair_cnt DESC," + sortCols, "*").addNumberColumn("row_num");
        NST sortedGroups = NSTUtil.optimize(toOptimize);
        toOptimize.release();

        log.debug("Getting top Rows");
        NSTColumn baseCol = sortedGroups.getNSTColumn(baseColName);
        NSTColumn cntCol = sortedGroups.getNSTColumn(endColName);
        NST k = new NST("key", "oid");
        NSTColumn addCol = k.getNSTColumn("key");

        StringBuffer loopSB = new StringBuffer();
        loopSB.append("var x:=" + cntCol + ".semijoin(" + baseCol + ".select($h)).sort();");
        loopSB.append("var y:= x.reverse().kunique().slice(" + range + ").reverse().mirror();");
        loopSB.append(addCol + ".insert(y);");
        MonetUtil.batLoop(baseCol + ".reverse().kunique()", loopSB.toString());

        log.debug("Keeping only the top groups");
        NST topGroups = sortedGroups.filter("xxx KEYIN " + (k.getNSTColumn("key")));
        log.debug("top groups: " + topGroups.describe());

        log.debug("Final intersect");
        NST topRowsNST = table.intersect(topGroups, "group_id").removeColumn("group_id");
        log.debug("final: " + topRowsNST.describe());

        table.removeColumn("group_id");
        table.removeColumn("pair_cnt");
        sortedGroups.release();
        topGroups.release();

        return topRowsNST;
    }

    /**
     * A rowlist is Random if it starts with ?
     *
     * @param rowList
     */
    protected static boolean isRandomRowList(String rowList) {
        return rowList.startsWith("?");
    }

    protected static boolean isColumnNumeric(NST nst, String colName) {
        DataTypeEnum type = nst.getNSTColumn(colName).getType();
        return (type == DataTypeEnum.DBL || type == DataTypeEnum.FLT || type == DataTypeEnum.INT || type == DataTypeEnum.LNG);
    }

    /**
     * Verifies the internal consistency of an NST. Takes each column and joins it
     * with the others, making sure that each join returns the same number of rows.
     * Also, verifies that the HEADs of all columns have the same number of rows
     * and that the values are unique.
     * <p/>
     * We wrote this method because we were experiencing difficulties with NSTs
     * that were broken, in the sense that some columns didn't have the same set
     * of HEAD keys as the others. We later found out that this was caused by
     * a problem in the old ProxDBMgr.createTempNSTFromBATVars (it was using bbpname() to
     * get the name of a BAT instead of str(), which preserves the ~ used to indicate
     * that the BAT in question has to be reseversed). We have dedided to leave
     * the mehod in-place to be used for debugging purposes if one suspects that
     * there are problems with an NST.
     * <p/>
     * If the NST is broken, this method will write a FATAL line to the logger.
     * This is an expensive operation... only use for debugging, and then remove
     * the call
     *
     * @param nst
     */
    public static boolean isNSTConsistent(NST nst) {
        List columnList = nst.getNSTColumns();
        int col0NumRows = 0;
        String col0BatName = "";
        int columnIdx = 0;
        for (Iterator iterator = columnList.iterator(); iterator.hasNext();) {
            NSTColumn nstColumn = (NSTColumn) iterator.next();
            String columnBAT = nstColumn.getBATName();
            int numRows = MonetUtil.getRowCount(columnBAT);
            int numKeys = MonetUtil.getDistinctHeadValuesRowCount(columnBAT);
            if (numRows != numKeys) {
                log.fatal("NST -- keys not unique in column : " + nstColumn.getName());
                return false;
            }
            if (columnIdx == 0) {
                col0NumRows = numRows;
                col0BatName = columnBAT;
            } else {
                if (numRows != col0NumRows) {
                    log.fatal("NST -- BAT doesn't have the same number of rows as col0:  " +
                            nstColumn.getName() + " has " + numRows + ", vs " + col0NumRows);
                    return false;
                }
                int joinRows = MonetUtil.getRowCount(col0BatName + ".reverse().join(" + columnBAT + ")");
                if (joinRows != col0NumRows) {
                    log.fatal("NST -- BAT does not join well with col0:  " +
                            nstColumn.getName() + " has " + joinRows + " in the join, vs " + col0NumRows);
                    return false;
                }
            }
            columnIdx++;
        }

        return true;
    }

    public static boolean isNSTExists(String nstName) {
        return MonetUtil.isBATExists(nstName);
    }

    protected static boolean isCoreNST(NST nst) {
        return DB.isProxNSTName(Util.unQuote(nst.topBATName));
    }

    /**
     * Turns an NST's columns into synchronized BATs with void heads, for faster processing and joins
     *
     * @param toOptimize
     * @return
     */
    public static NST optimize(NST toOptimize) {
        NST nst = new NST();
        for (int colIdx = 0; colIdx < toOptimize.getColumnCount(); colIdx++) {
            NSTColumn nstColumn = toOptimize.getNSTColumn(colIdx);
            String newColumn = Connection.executeAndSave(nstColumn + ".sort().reverse().mark(0@0).reverse().copy()");
            nst.columnList.add(new NSTColumn(nstColumn.getName(), nstColumn.getType(), newColumn));
        }

        return nst;
    }

    public static String normalizeName(String arg) {
        if (!arg.startsWith("var") && arg.charAt(0) != '"') {
            arg = "\"" + arg + "\"";
        } else {
        }
        return arg;
    }

    /**
     * Given a row list, it returns the min and the max. Format:
     * <p/>
     * - from-to  : returns from, to
     * - ?size    : returns 0, size - 1 (use isRandomRowList to check this condition)
     * - *        : returns -1, -1 (which should be interpreted as 'all rows')
     *
     * @param rowList
     * @return int[]{from, to}
     */
    protected static int[] rowListToArray(String rowList) {
        rowList = rowList.trim();
        if ("*".equals(rowList)) {
            return new int[]{-1, -1};
        } else if (isRandomRowList(rowList)) {
            return new int[]{0, Integer.parseInt(rowList.substring(1)) - 1};
        } else {
            String[] rows = rowList.split("-");
            if (rows.length == 2) {
                return new int[]{Integer.parseInt(rows[0].trim()), Integer.parseInt(rows[1].trim())};
            } else {
                throw new IllegalArgumentException("Correct format for rows: from-to, or ?num, or *");
            }
        }
    }

    static String[] findColsInFilterDef(String filterDef) {
        String[] joinCols = filterDef.trim().split("(\\s)+");
        if (joinCols.length != 3 ||
                ComparisonOperatorEnum.enumForString(joinCols[1].toUpperCase()) != ComparisonOperatorEnum.EQ) {
            throw new IllegalArgumentException("Only the EQ operator is allowed for joins. " +
                    "Specify col1 EQ col2: " + filterDef);
        }
        if (joinCols[0].toUpperCase().startsWith("A.")) {
            joinCols[0] = joinCols[0].substring(2);
        }
        if (joinCols[2].toUpperCase().startsWith("B.")) {
            joinCols[2] = joinCols[2].substring(2);
        }

        return new String[]{joinCols[0], joinCols[2]};
    }
}
