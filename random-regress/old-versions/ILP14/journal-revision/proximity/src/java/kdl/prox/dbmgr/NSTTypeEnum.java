/**
 * $Id: NSTTypeEnum.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id
 */

/**
 * An enumeration of different types of NSTs.
 * When creating an NST, the constructor has to hit the database
 * to get the types (and maybe names) of the columns to make up the NST.
 * The enumeration here provided column names and types for different
 * common types of NSTs
 * These static variables can be used in the call to the NST constructor
 * so that it doesn't have to hit the database
 * To create a new "shortcut", make sure that you include both column names
 * and types
 */
package kdl.prox.dbmgr;


public class NSTTypeEnum {

    /**
     * NSTs for the Attribute Structure
     */
    public static final String[] ATTR_NST_COL_NAMES =
            new String[]{"name", "data_type", "data"};

    public static final String[] ATTR_NST_COL_TYPES =
            new String[]{"str", "str", "str"};


    /**
     * NSTs for Containers
     */
    public static final String[] CONT_NST_COL_NAMES =
            new String[]{"id", "name", "parent", "object", "link", "subg_attr"};

    public static final String[] CONT_NST_COL_TYPES =
            new String[]{"oid", "str", "oid", "str", "str", "str"};


    /**
     * NSTs for Links
     */
    public static final String[] LINK_NST_COL_NAMES =
            new String[]{"link_id", "o1_id", "o2_id"};

    public static final String[] LINK_NST_COL_TYPES =
            new String[]{"oid", "oid", "oid"};


    /**
     * NSTs for Objects
     */
    public static final String[] OBJ_NST_COL_NAMES =
            new String[]{"id"};

    public static final String[] OBJ_NST_COL_TYPES =
            new String[]{"oid"};


    /**
     * NSTs for subgraph items
     */
    public static final String[] SGI_NST_COL_NAMES =
            new String[]{"item_id", "subg_id", "name"};

    public static final String[] SGI_NST_COL_TYPES =
            new String[]{"oid", "oid", "str"};


}
