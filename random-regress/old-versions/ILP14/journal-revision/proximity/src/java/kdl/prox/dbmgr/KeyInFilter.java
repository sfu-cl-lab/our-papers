/**
 * $Id: KeyInFilter.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Implements a filter for the head column of an NST,
 * based on the column from another BAT
 * Corresponds to a WHERE keID IN in (SQL)
 */

public class KeyInFilter implements Filter {

    /**
     * - The name of the BAT from the other NST to look at
     */
    private String otherColumnBATName;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(KeyInFilter.class);

    /**
     * Full-arg constructor
     *
     * @param otherNST
     * @param otherColumn
     */
    public KeyInFilter(NST otherNST, String otherColumn) {
        this(otherNST.getNSTColumn(otherColumn).getBATName());
    }


    public KeyInFilter(String batName) {
        Assert.stringNotEmpty(batName, "Null batName");
        otherColumnBATName = batName;
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");
        StringBuffer milSB = new StringBuffer();
        //do a join between the first column on this NST
        NSTColumn theColumn = onNST.getNSTColumn(0);
        milSB.append(theColumn.getBATName());
        milSB.append(".semijoin(");
        milSB.append(otherColumnBATName + ".reverse().kunique()");
        milSB.append(")");
        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return "key_in_" + otherColumnBATName;
    }

}
