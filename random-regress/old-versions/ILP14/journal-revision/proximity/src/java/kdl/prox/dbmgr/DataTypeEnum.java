/**
 * $Id: DataTypeEnum.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * An enumeration of Monet's data types. NB: Our convention is to use upper case
 * names, even though Monet's types are lower case. This is because some
 * lower case names are Java reserved words (e.g., 'int'). However, the myName
 * value is the actual Monet data type.
 */

package kdl.prox.dbmgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class DataTypeEnum {
    private static List dataTypeEnums = new ArrayList();    // all known DataTypeEnumS. filled by constructor

    public static final DataTypeEnum BIT = new DataTypeEnum("bit");
    public static final DataTypeEnum CHR = new DataTypeEnum("chr");
    public static final DataTypeEnum DATE = new DataTypeEnum("date");   // from the date module
    public static final DataTypeEnum DBL = new DataTypeEnum("dbl");
    public static final DataTypeEnum FLT = new DataTypeEnum("flt");
    public static final DataTypeEnum INT = new DataTypeEnum("int");
    public static final DataTypeEnum LNG = new DataTypeEnum("lng");
    public static final DataTypeEnum OID = new DataTypeEnum("oid");
    public static final DataTypeEnum STR = new DataTypeEnum("str");
    public static final DataTypeEnum BAT = new DataTypeEnum("bat");
    public static final DataTypeEnum TIMESTAMP = new DataTypeEnum("timestamp"); // from the date module

    private final String myName; // for debug only


    private DataTypeEnum(String name) {
        myName = name;
        dataTypeEnums.add(this);
    }


    public String toString() {
        return myName;
    }


    /**
     * Returns the DataTypeEnum for dataType. Returns null if none found.
     * NB: Ignores case.
     *
     * @param dataType
     * @return
     */
    public static DataTypeEnum enumForType(String dataType) {
        // void is equal to oid
        if (dataType.equalsIgnoreCase("void")) {
            dataType = "oid";
        }
        if (dataType.equalsIgnoreCase("wrd")) {
            dataType = "int";
        }
        for (Iterator dataTypeEnumIter = dataTypeEnums.iterator(); dataTypeEnumIter.hasNext();) {
            DataTypeEnum dataTypeEnum = (DataTypeEnum) dataTypeEnumIter.next();
            if (dataTypeEnum.myName.equalsIgnoreCase(dataType)) {
                return dataTypeEnum;    // found
            }
        }
        return null;    // not found
    }


}
