/**
 * $Id: ColumnValueLikeFilter.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Implements a filter based on the value of a column
 * Corresponds to the SQL 'like' command
 * Accepts % as wildcard
 */
public class ColumnValueLikeFilter implements Filter {

    // The column in the NST that the filter is applied to
    // and the expected like string
    private String column;

    private String likeString;


    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnValueLikeFilter.class);


    /**
     * Full-arg constructor
     *
     * @param column
     * @param likeString
     */
    public ColumnValueLikeFilter(String column, String likeString) {
        Assert.stringNotEmpty(column, "empty column");
        Assert.stringNotEmpty(likeString, "empty likeString");
        this.column = column;
        this.likeString = likeString;
    }


    /**
     * Private method that applies one condition on an NST
     *
     * @param onNST
     * @return
     * @
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");
        // Do a likeselect on the BAT for the specified column
        StringBuffer milSB = new StringBuffer();
        NSTColumn theColumn = onNST.getNSTColumn(column);
        milSB.append(theColumn.getBATName());
        milSB.append(".like_select(\"");
        milSB.append(likeString);
        milSB.append("\")");
        return milSB.toString();
    }

    public String getLikeString() {
        return likeString;
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return column + "_like_string";
    }
}
