/**
 * $Id: AttrType.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttrType.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * Represents one of an attribute's value columns. Instances are created by
 * Attributes.getTypes().
 *
 * @see Attributes#getTypes(String)
 */
public class AttrType {

    /**
     * Class-based log4j logger.
     */
    private static Logger log = Logger.getLogger(AttrType.class);

    /**
     * The column's name. null if no name (possible for single-valued
     * attributes whose default column name is 'value'). Set by constructor.
     */
    private String name;

    /**
     * The column's data type. Set by constructor.
     */
    private DataTypeEnum dataTypeEnum;


    /**
     * Full-arg constructor. Saves args in IVs.
     *
     * @param name
     * @param dataTypeEnum
     */
    AttrType(String name, DataTypeEnum dataTypeEnum) {
        if ((name == null) || (name.length() == 0)) {
            name = "value";
        }
        this.name = name.toLowerCase();
        this.dataTypeEnum = dataTypeEnum;
    }


    /**
     * Returns my dataTypeEnum.
     *
     * @return
     */
    public DataTypeEnum getDataTypeEnum() {
        return dataTypeEnum;
    }


    /**
     * Returns my name.
     *
     * @return
     */
    public String getName() {
        return name;
    }


    /**
     * Object method.
     */
    public String toString() {
        return "[" + name + " : " + dataTypeEnum + "]";
    }

    /**
     * Converts an attribute definition (e.g. year:int, salary:float) to two string with column names
     * and types (e.g., "year, salary", "int, float")
     *
     * @param attrType
     * @return
     */
    public static String[] attrTypeDefToColNamesAndTypes(String attrType) {
        String[] attrColNameTypes = new String[2];
        attrColNameTypes[0] = "id";
        attrColNameTypes[1] = "oid";
        List attrTypeDefs = attrTypeListForTypeDef(attrType);
        Iterator attrTypeDefsIter = attrTypeDefs.iterator();
        while (attrTypeDefsIter.hasNext()) {
            AttrType thisAttrType = (AttrType) attrTypeDefsIter.next();
            attrColNameTypes[0] += "," + thisAttrType.getName();
            attrColNameTypes[1] += "," + thisAttrType.getDataTypeEnum().toString();
        }
        return attrColNameTypes;
    }

    /**
     * Utility that converts an attribute type definition string to a List of
     * AttrType instances. See Attributes.defineAttribute() for typeDef's
     * syntax.
     *
     * @param typeDef
     * @return
     * @throws IllegalArgumentException if typeDef invalid
     * @see AttrType
     * @see Attributes#defineAttribute(String, String)
     */
    public static List attrTypeListForTypeDef(String typeDef)
            throws IllegalArgumentException {
        ArrayList attrTypes = new ArrayList(); // return value
        ArrayList attrNames = new ArrayList(); // names of attributes, to enforce uniqueness

        // tokenize column definitions
        StringTokenizer columnTokenizer = new StringTokenizer(typeDef, ",");
        while (columnTokenizer.hasMoreTokens()) {
            String columnToken = columnTokenizer.nextToken().trim();
            // try to tokenize into firstToken and secondToken
            StringTokenizer dataTokenizer = new StringTokenizer(columnToken, ":");
            String firstToken = null;       // null if no 1st token. set next
            String secondToken = null;      // "" 2nd ""
            try {
                firstToken = dataTokenizer.nextToken().trim();
            } catch (NoSuchElementException nseExc) {
                // ignore
            }
            try {
                secondToken = dataTokenizer.nextToken().trim();
            } catch (NoSuchElementException nseExc) {
                // ignore
            }
            Assert.condition(!dataTokenizer.hasMoreTokens(), "expected at " +
                    "most two tokens: '" + columnToken + "'");
            Assert.notNull(firstToken, "expected at least one token: '" +
                    columnToken + "'");
            String columnName = (secondToken == null ? null : firstToken);
            String dataType = (secondToken == null ? firstToken : secondToken);
            DataTypeEnum dataTypeEnum = DataTypeEnum.enumForType(dataType);
            Assert.notNull(dataTypeEnum, "invalid data type: '" + dataType + "'");
            // Create the AttrType here. Then get its name, because it could be 
            // originally null and then changed to 'value'
            AttrType attrType = new AttrType(columnName, dataTypeEnum);
            if (attrNames.contains(attrType.getName())) {
                throw new IllegalArgumentException("Column name used more than once: " +
                        attrType.getName());
            } else {
                attrNames.add(attrType.getName());
            }
            attrTypes.add(attrType);
        }
        // At least one column should have been defined
        Assert.condition(attrTypes.size() != 0, "no type defs found: '" +
                typeDef + "'");
        return attrTypes;
    }


}
