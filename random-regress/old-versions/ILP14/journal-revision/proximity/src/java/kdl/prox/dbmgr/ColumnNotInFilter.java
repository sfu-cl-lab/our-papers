/**
 * $Id: ColumnNotInFilter.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Implements a filter based on a column from another BAT
 * Corresponds to a WHERE NOT IN in SQL
 */

public class ColumnNotInFilter implements Filter {

    /**
     * - The column in the NST that the filter is applied to
     */
    private String column;
    private String otherColumnBATName;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnNotInFilter.class);


    /**
     * Shortcut
     *
     * @param column
     * @param otherNST
     * @param otherColumn
     */
    public ColumnNotInFilter(String column, NST otherNST, String otherColumn) {
        this(column, otherNST.getNSTColumn(otherColumn).getBATName());
    }


    /**
     * Full-arg constructor
     *
     * @param column
     * @param otherBAT
     */
    public ColumnNotInFilter(String column, String otherBAT) {
        Assert.stringNotEmpty(column, "Null column");
        Assert.stringNotEmpty(otherBAT, "Null other BAT");
        // The column from the NST that this is applied to
        this.column = column;
        otherColumnBATName = otherBAT;
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");
        //do a join between the column on this NST, and the BAT from the other NST
        StringBuffer milSB = new StringBuffer();
        NSTColumn theColumn = onNST.getNSTColumn(column);
        milSB.append(theColumn.getBATName());
        milSB.append(".reverse().kdiff(");
        milSB.append(otherColumnBATName + ".reverse()");
        milSB.append(").reverse()");
        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return column + "_not_in_" + otherColumnBATName;
    }

}
