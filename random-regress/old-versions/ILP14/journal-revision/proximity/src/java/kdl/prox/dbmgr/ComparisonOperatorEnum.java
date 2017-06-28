/**
 * $Id: ComparisonOperatorEnum.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * An enumeration of possible comparison operators for ColumnValueFilter. 
 * NB: Our convention is to use upper case names, 
 */

package kdl.prox.dbmgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ComparisonOperatorEnum {
    // all known connectors
    private static List ComparisonOperatorEnums = new ArrayList();

    public static final ComparisonOperatorEnum EQ = new ComparisonOperatorEnum("eq");
    public static final ComparisonOperatorEnum GE = new ComparisonOperatorEnum("ge");
    public static final ComparisonOperatorEnum GT = new ComparisonOperatorEnum("gt");
    public static final ComparisonOperatorEnum LE = new ComparisonOperatorEnum("le");
    public static final ComparisonOperatorEnum LT = new ComparisonOperatorEnum("lt");
    public static final ComparisonOperatorEnum NE = new ComparisonOperatorEnum("ne");

    // these are private --added to the enum List, but only can be used here, as aliases
    private static final ComparisonOperatorEnum EQSIGN = new ComparisonOperatorEnum("=", "eq");
    private static final ComparisonOperatorEnum EQSIGNDBL = new ComparisonOperatorEnum("==", "eq");
    private static final ComparisonOperatorEnum GESIGN = new ComparisonOperatorEnum(">=", "ge");
    private static final ComparisonOperatorEnum GTSIGN = new ComparisonOperatorEnum(">", "gt");
    private static final ComparisonOperatorEnum LESIGN = new ComparisonOperatorEnum("<=", "le");
    private static final ComparisonOperatorEnum LTSIGN = new ComparisonOperatorEnum("<", "lt");
    private static final ComparisonOperatorEnum NESIGN = new ComparisonOperatorEnum("!=", "ne");

    private final String myName;
    private String equivalent;

    private ComparisonOperatorEnum(String name) {
        myName = name;
        equivalent = name;
        ComparisonOperatorEnums.add(this);
    }

    /**
     * To create aliases, such as ComparisonOperatorEnum.EQSign = ("=", "eq")
     *
     * @param name
     * @param corresponds
     */
    private ComparisonOperatorEnum(String name, String corresponds) {
        this(name);
        equivalent = corresponds;
    }

    public String toString() {
        return myName;
    }


    /**
     * Returns the ComparisonOperatorEnum for the string. Returns null if none found.
     * Looks at the names, and if found it returns the comparator. If the comparator
     * has a canonical equivalent, it returns it.
     * <p/>
     * NB: Ignores case.
     *
     * @param name
     * @return
     */
    public static ComparisonOperatorEnum enumForString(String name) {
        Iterator compOperIter = ComparisonOperatorEnums.iterator();
        while (compOperIter.hasNext()) {
            ComparisonOperatorEnum compOperEnum = (ComparisonOperatorEnum) compOperIter.next();
            if (compOperEnum.myName.equalsIgnoreCase(name)) {
                if (compOperEnum.myName.equals(compOperEnum.equivalent)) {
                    return compOperEnum;
                } else {
                    return enumForString(compOperEnum.equivalent);    // found
                }
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + name);
    }

}
