/**
 * $Id: ColumnValueRandomFilter.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Implements a filter based on the value of a column. It gets the distinct values of a column
 * and picks a certain number of them randomly.
 * It is a rather expensive filter, because it has to get the distinct values of the column,
 * sample from that, and then join again with the column.
 */
public class ColumnValueRandomFilter implements Filter {

    String column;
    int randomSize;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnValueRandomFilter.class);


    /**
     * Full-arg constructor
     *
     * @param column
     * @param value
     */
    public ColumnValueRandomFilter(String column, int value) {
        Assert.stringNotEmpty(column, "Empty column");
        this.column = column;
        this.randomSize = value;
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");

        // do a select on the BAT that column
        NSTColumn theColumn = onNST.getNSTColumn(column);
        String batName = theColumn.getBATName();

        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".join(");
        milSB.append("[rnd](");
        milSB.append(batName);
        milSB.append(".reverse().kunique()).sample(");
        milSB.append(randomSize);
        milSB.append(")");
        milSB.append(")");

        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return column + "_random_" + randomSize + "_value";
    }

}
