/**
 * $Id: FilterFactory.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.util.MonetUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Given a string description of a filter, a-la SQL, it
 * returns an actual instance of the Filter* hierarchy that implements it.
 * <p/>
 * Valid filters:
 * <ul>
 * <li>colName DISTINCT ROWS
 * <li>colName LIKE      value
 * <li>colName BETWEEN   lowerBound-upperBound
 * <li>colName IN        batVariableName
 * <li>colName NOTIN     batVariableName
 * <li>colName KEYIN     batVariableName
 * <li>colName KEYNOTIN  batVariableName
 * <li>colName OP        value
 * <li>colName OP        colName
 * <li>colName RANDOM    count
 * <li>* (no filter)
 * </ul>
 * OP is any of the types listed in ComparisonOperatorEnum (see)
 * <p/>
 * Filters can be combined with AND and OR, using infix notation, e.g.
 * col1 LIKE 'a' OR col2 EQ 'b' AND col3 IN batVar1
 * <p/>
 * The following rules apply to combined filters:
 * <ul>
 * <li>the DISTINCT ROWS filter cannot be combined with other filters
 * <li>No parenthesis are allowed, and the filters are processed in pairs in the
 * order in which they appear.
 * </ul>
 * For example, the filter a AND b OR c is processed as (a AND b) OR c.
 * Similarly, a AND b OR c AND d is processed as ((a AND b) OR c) AND d
 * </p>
 * Note that AND and OR will be interpreted as the logical connectors in positions
 * every three words AND EQ 7, refering to a column named AND, will generate an
 * error. But A EQ 6 AND and EQ 7 will work.
 *
 * @see ComparisonOperatorEnum
 */
public class FilterFactory {
    private static Logger log = Logger.getLogger(FilterFactory.class);

    // prevent sub-classing
    private FilterFactory() {
    }


    /**
     * Gets a string describing a filter, and returns the corresponding type of filter.
     *
     * @param filterDescr
     * @return a concrete instance of the Filter interface. The type depends on the filterDesc.
     */
    protected static Filter getFilter(String filterDescr) {
        filterDescr = filterDescr.trim();

        // no filter if "*"
        if ("*".equals(filterDescr)) {
            return null;
        }

        // split, removing empty spaces and respecting quoted sequences
        // loop and get filters (three words) and use AND or OR connectors to combine them
        Filter currFilter = null;
        int nextElt = 0;
        List words = Util.splitQuotedString(filterDescr, ' ');
        while (words.size() > nextElt) {
            String connector = ((String) words.get(nextElt)).trim().toUpperCase();
            if ("AND".equals(connector) || "OR".equals(connector)) {
                nextElt++;
                if (currFilter == null) {
                    throw new IllegalArgumentException("AND/OR cannot begin the filter");
                }
                Filter nextFilter = getNextFilter(words, nextElt);
                if ((currFilter instanceof ColumnDistinctFilter) ||
                        (nextFilter instanceof ColumnDistinctFilter)) {
                    throw new IllegalArgumentException("DISTINCT ROWS filter cannot be combined with others");
                }
                if ("AND".equals(connector)) {
                    currFilter = new CompositeFilter(currFilter, LogicalConnectorEnum.AND, nextFilter);
                } else {
                    currFilter = new CompositeFilter(currFilter, LogicalConnectorEnum.OR, nextFilter);
                }
            } else {
                currFilter = getNextFilter(words, nextElt);
            }
            nextElt += 3;
        }
        return currFilter;
    }

    /**
     * Returns the command necessary to apply the filter
     *
     * @param filterDef
     * @param target
     * @return a String with the MIL command
     */
    public static String getFilterCmd(String filterDef, NST target) {
        Filter filter = getFilter(filterDef);
        if (filter == null) {
            return null;
        } else {
            return filter.getApplyCmd(target);
        }
    }

    /**
     * Returns a comma-separated string with the list of columns involved in a filter
     * Doesn't check syntax: just assumes a list of AND/OR'ed filters
     *
     * @param filterDescr
     * @return a comma-separated list of columns involved in the filter
     */
    public static String getFilterColumns(String filterDescr) {
        filterDescr = filterDescr.trim();

        // no columns if "*"
        if ("*".equals(filterDescr)) {
            return "";
        }

        // split, removing empty spaces and respecting quoted sequences
        // loop and get filters (three words) and use AND or OR connectors to combine them
        int nextElt = 0;
        Set columnSet = new HashSet();
        List words = Util.splitQuotedString(filterDescr, ' ');
        while (words.size() > nextElt) {
            String connector = ((String) words.get(nextElt)).trim().toUpperCase();
            if ("AND".equals(connector) || "OR".equals(connector)) {
                nextElt++;
            }
            String col1Name = ((String) words.get(nextElt)).trim();
            String operator = ((String) words.get(nextElt + 1)).trim().toUpperCase();
            String col2Name = ((String) words.get(nextElt + 2)).trim();

            columnSet.add(col1Name);
            // Only add the second column if it's an OP
            if ("DISTINCT".equals(operator)) {
            } else if ("RANDOM".equals(operator)) {
            } else if ("BETWEEN".equals(operator)) {
            } else if ("LIKE".equals(operator)) {
            } else if ("IN".equals(operator) || "NOTIN".equals(operator)
                    || "KEYIN".equals(operator)
                    || "KEYNOTIN".equals(operator)) {
            } else if (!Util.isValueArgument(col2Name) && !col2Name.equalsIgnoreCase("nil")) {
                columnSet.add(col2Name);
            }
            nextElt += 3;
        }

        // join the columns with ,
        return Util.join(columnSet, ",");
    }

    /**
     * Returns true when the filter involves a KEYIN/NOTIN or IN/NOT operator.
     * In that case, the filter will most likely involve columns that don't belong to the same NST
     *
     * @param filterDescr
     * @return
     */
    public static boolean isInFilter(String filterDescr) {
        filterDescr = filterDescr.trim();

        // not if "*"
        if ("*".equals(filterDescr)) {
            return false;
        }

        // split, removing empty spaces and respecting quoted sequences
        // loop and get filters (three words) and use AND or OR connectors to combine them
        int nextElt = 0;
        List words = Util.splitQuotedString(filterDescr, ' ');
        while (words.size() > nextElt) {
            String connector = ((String) words.get(nextElt)).trim().toUpperCase();
            if ("AND".equals(connector) || "OR".equals(connector)) {
                nextElt++;
            }
            String operator = ((String) words.get(nextElt + 1)).trim().toUpperCase();

            if ("IN".equals(operator) || "NOTIN".equals(operator) || "KEYIN".equals(operator) || "KEYNOTIN".equals(operator)) {
                return true;
            }
            nextElt += 3;
        }

        // join the columns with ,
        return false;
    }


    /**
     * Returns either a columnValue filter (col1 EQ 12 or col1 EQ 'a'),
     * or a columnComparison filter (col1 EQ col2)
     */
    private static Filter getComparisonFilter(String col1Name, String operator, String col2Name) {
        if (col2Name == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + operator);
        }

        ComparisonOperatorEnum op = ComparisonOperatorEnum.enumForString(operator);
        if (col2Name.equalsIgnoreCase("nil")) {
            return new ColumnValueFilter(col1Name, op, MonetUtil.INTERNAL_NIL_STRING);
        } else if (Util.isValueArgument(col2Name)) {
            return new ColumnValueFilter(col1Name, op, Util.unQuote(col2Name));
        } else {
            return new ColumnComparisonFilter(col1Name, op, col2Name);
        }
    }

    /**
     * Returns a col DISTINCT operator
     */
    private static Filter getDistinctOperator(String col1Name, String col2Name) {
        if (!"ROWS".equals(col2Name.toUpperCase())) {
            throw new IllegalArgumentException("Operator DISTINCT must be followed by ROWS keyword");
        }
        return new ColumnDistinctFilter(col1Name);
    }

    /**
     * Creates a ColumnValueLikeFilter
     */
    private static Filter getLikeFilter(String col1Name, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        if (!Util.isQuoted(value)) {
            throw new IllegalArgumentException("LIKE operator requires a quoted argument");
        }
        return new ColumnValueLikeFilter(col1Name, Util.unQuote(value));
    }


    /**
     * Creates a ColumnInFilter (either in or notin)
     */
    private static Filter getInFilter(String col1Name, String operator, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        if (Util.isQuoted(value) || Util.isNumber(value)) {
            throw new IllegalArgumentException("IN operators require an unquoted string representing a BAT name");
        }
        if ("IN".equals(operator)) {
            return new ColumnInFilter(col1Name, value);
        } else {
            return new ColumnNotInFilter(col1Name, value);
        }
    }


    /**
     * Creates a ColumnKeyFilter
     */
    private static Filter getKeyInFilter(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        if (Util.isQuoted(value) || Util.isNumber(value)) {
            throw new IllegalArgumentException("KEYIN operator requires an unquoted string representing a BAT name");
        }
        return new KeyInFilter(value);
    }


    /**
     * Creates a ColumnKeyNotInFilter
     */
    private static Filter getKeyNotInFilter(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        if (Util.isQuoted(value) || Util.isNumber(value)) {
            throw new IllegalArgumentException("KEYNOTIN operator requires an unquoted string representing a BAT name");
        }
        return new KeyNotInFilter(value);
    }


    /**
     * Uses the next three words to build a simple filter
     */
    private static Filter getNextFilter(List words, int nextElt) {
        if (words.size() < nextElt + 3) {
            throw new IllegalArgumentException("Incomplete filter. Format. ");
        }
        String col1Name = ((String) words.get(nextElt)).trim();
        String operator = ((String) words.get(nextElt + 1)).trim().toUpperCase();
        String col2Name = ((String) words.get(nextElt + 2)).trim();

        if ("DISTINCT".equals(operator)) {
            return getDistinctOperator(col1Name, col2Name);
        } else if ("LIKE".equals(operator)) {
            return getLikeFilter(col1Name, col2Name);
        } else if ("BETWEEN".equals(operator)) {
            return getRangeFilter(col1Name, col2Name);
        } else if ("IN".equals(operator) || "NOTIN".equals(operator)) {
            return getInFilter(col1Name, operator, col2Name);
        } else if ("KEYIN".equals(operator)) {
            return getKeyInFilter(col2Name);
        } else if ("KEYNOTIN".equals(operator)) {
            return getKeyNotInFilter(col2Name);
        } else if ("RANDOM".equals(operator)) {
            return getRandomFilter(col1Name, col2Name);
        } else {
            return getComparisonFilter(col1Name, operator, col2Name);
        }
    }

    /**
     * Creates a ColumnValueRangeFilter
     */
    private static Filter getRangeFilter(String col1Name, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        List bounds = Util.splitQuotedString(value, '-');
        if (bounds.size() != 2) {
            throw new IllegalArgumentException("BETWEEN operator requires lower and upper bounds, separated by -");
        }
        // remove quotes, if necesary
        return new ColumnValueRangeFilter(col1Name,
                Util.unQuote((String) bounds.get(0)),
                Util.unQuote((String) bounds.get(1)));
    }

    /**
     * Creates a ColumnValueRandomFilter
     */
    private static Filter getRandomFilter(String col1Name, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator requires extra parameter: " + value);
        }
        if (!Util.isNumber(value)) {
            throw new IllegalArgumentException("RANDOM operator requires an integer argument");
        }
        return new ColumnValueRandomFilter(col1Name, new Integer(value).intValue());
    }
}
