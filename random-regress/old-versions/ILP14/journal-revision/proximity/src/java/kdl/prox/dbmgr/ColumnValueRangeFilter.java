/**
 * $Id: ColumnValueRangeFilter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;


/**
 * Implements a filter based on the value of a column. It returns the rows between min and max
 */
public class ColumnValueRangeFilter implements Filter {

    String column;
    String lowerBound;
    String upperBound;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnValueRangeFilter.class);


    /**
     * Full-arg constructor
     *
     * @param column
     * @param lower
     * @param upper
     */
    public ColumnValueRangeFilter(String column, String lower, String upper) {
        Assert.stringNotEmpty(column, "Empty column");
        this.column = column;
        this.lowerBound = lower;
        this.upperBound = upper;
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");

        // do a select on the BAT that column
        NSTColumn theColumn = onNST.getNSTColumn(column);
        StringBuffer milSB = new StringBuffer();

        milSB.append(theColumn.getBATName());
        milSB.append(".uselect(");
        milSB.append(MonetUtil.delimitValue(lowerBound, theColumn.getType()));
        milSB.append(",");
        milSB.append(MonetUtil.delimitValue(upperBound, theColumn.getType()));
        milSB.append(")");

        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return column + "_RANGE" + lowerBound + "_" + upperBound + "_value";
    }

}
