/**
 * $Id: Filter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;



/**
 * Represents a filter that can be applied to a NST for certain operations,
 * such as select or delete.
 * <p/>
 * The main method of a class that implements the Filter interface is
 * getApplyCmd
 * This command should return the name of a BAT whose
 * HEAD = unique IDs of rows in the destination NST that match the filter
 * TAIL = HEAD
 * The resulting BAT is used in the NST class to create a new filtered NST,
 * or to delete or select rows. Those methods in the NST class take the filter and
 * semijoin with it all the columns in the NST.
 */
public interface Filter {

    /**
     * Returns a string with all the commands that need to be executed
     * to apply the filter
     *
     * @param onNST
     * @return
     * @
     */
    public abstract String getApplyCmd(NST onNST);


    /**
     * Returns a name that can be used as part of an MIL variable
     *
     * @return
     */
    public abstract String getMILDescription();

}