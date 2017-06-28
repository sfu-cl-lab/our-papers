/**
 * $Id: LogicalConnectorEnum.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * An enumeration of possible connectors for ColumnValueFilter conditions. 
 * NB: Our convention is to use upper case names, 
 */

package kdl.prox.dbmgr;

import java.util.ArrayList;
import java.util.List;


public class LogicalConnectorEnum {
    // all known connectors
    private static List logicalConnectorEnums = new ArrayList();

    public static final LogicalConnectorEnum AND = new LogicalConnectorEnum("and");
    public static final LogicalConnectorEnum OR = new LogicalConnectorEnum("or");

    private final String myName; // for debug only

    private LogicalConnectorEnum(String name) {
        myName = name;
        logicalConnectorEnums.add(this);
    }


    public String toString() {
        return myName;
    }

}
