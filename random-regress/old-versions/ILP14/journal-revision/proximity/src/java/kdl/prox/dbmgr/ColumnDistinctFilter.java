/**
 * $Id: ColumnDistinctFilter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

/**
 * Implements a filter that finds distinct values in a column
 * Corresponds to a DISTINCT in SQL
 * <p/>
 * It only works on one column at a time. If you want to do use it on multiple
 * columns, you can always do a GROUP BY on those columns and then select
 * DISTINCT group_ids.
 */

public class ColumnDistinctFilter implements Filter {

    // The column in the NST that the filter is applied to
    private String column;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnDistinctFilter.class);


    /**
     * Full-arg constructor
     *
     * @param column
     */
    public ColumnDistinctFilter(String column) {
        Assert.stringNotEmpty(column, "Null other column");
        this.column = column;
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");
        // simply do a kunique on the reverse (kunique = unique on head values)
        StringBuffer milSB = new StringBuffer();
        NSTColumn theColumn = onNST.getNSTColumn(column);
        milSB.append(theColumn.getBATName());
        milSB.append(".reverse().kunique().reverse()");
        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return "distinct_" + column;
    }


}
