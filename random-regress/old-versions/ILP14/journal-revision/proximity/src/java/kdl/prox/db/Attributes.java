/**
 * $Id: Attributes.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Attributes.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTColumn;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.script.AddAttribute;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Represents a set of attributes at some level of containment in a Proximity
 * database. Is actually an object-oriented wrapper for AttrNSTs. In this class
 * we refer to specific attributes by name.
 */
public class Attributes {

    private static Logger log = Logger.getLogger(Attributes.class);

    private NST attrNST;
    private String attrNSTName;

    Attributes(String attrNSTName) {
        Assert.notNull(attrNSTName, "attrNSTName");
        this.attrNSTName = attrNSTName;
        this.attrNST = new NST(attrNSTName);
        // specify that data column is of type BAT
        // see NST documentation for an explanation of this
        this.attrNST.getNSTColumn("data").setDelimitAsBATName(true);
    }


    /**
     * Deletes all the values stored in an attribute
     *
     * @param attrName
     * @
     */
    public void clearAttributeValues(String attrName) {
        NST attrDataNST = getAttrDataNST(attrName); // throws error if not found
        attrDataNST.deleteRows();
    }


    /**
     * Creates new attribute named newAttrName from oldAttrName. This two arg
     * overload: a) copies all columns (i.e., works on single- and multi-column
     * attributes), and b) uses the same type definition as oldAttrName.
     *
     * @param oldAttrName
     * @param newAttrName
     * @
     */
    public void copyAttribute(String oldAttrName, String newAttrName) {
        log.debug("copyAttribute: " + oldAttrName + ", " + newAttrName);
        if (isAttributeDefined(newAttrName)) {
            deleteAttribute(newAttrName);
        }
        String typeDef = getAttrTypeDef(oldAttrName);
        defineAttribute(newAttrName, typeDef);
        NST oldAttrNST = getAttrDataNST(oldAttrName);
        NST newAttrNST = getAttrDataNST(newAttrName);
        newAttrNST.insertRowsFromNST(oldAttrNST);
        oldAttrNST.release();
        newAttrNST.release();
    }


    /**
     * Creates new attribute named newAttrName from oldAttrName. This three arg
     * overload: a) copies only one column (i.e., works only on single-column
     * attributes), and b) uses newAttrType for the new attribute's type definition.
     *
     * @param oldAttrName
     * @param newAttrName
     * @param newAttrType
     */
    public void copyAttribute(String oldAttrName, String newAttrName,
                              DataTypeEnum newAttrType) {
        Assert.condition(isSingleValued(oldAttrName), "only works on single-" +
                "column attributes");

        // create the new attribute's NST
        NST oldAttrNST = getAttrDataNST(oldAttrName);
        NST oldAttrCopyNST = oldAttrNST.copy();
        String valueCol = oldAttrNST.getNSTColumn(1).getName();     // might not be 'value'
        oldAttrCopyNST.castColumn(valueCol, newAttrType.toString());

        // define and insert the new attribute's values
        defineAttribute(newAttrName, newAttrType.toString());
        NST newAttrNST = getAttrDataNST(newAttrName);
        newAttrNST.insertRowsFromNST(oldAttrCopyNST);

        // clean up
        oldAttrCopyNST.release();
    }

    /**
     * Example: every object has a score, and you want to examine the 500 objects
     * with the highest scores.  This method prints info to help you find the score cutoff
     * which will give you exactly or around 500 objects.
     * Attribute can be of any type (as long as it can be sorted)
     *
     * @param attrName
     * @param numItemsWanted
     * @param bottomN
     */
    public void cutoffForTopNVals(String attrName, int numItemsWanted, boolean bottomN) {
        NST attrDataNST = getAttrDataNST(attrName);
        String columnToSort = "value";
        // See what the value is at exactly the cutoff we want
        NST scoreNST = attrDataNST.sort(columnToSort, "*");
        int totalRows = scoreNST.getRowCount();
        int rowWanted;
        if (bottomN) {  // bottom scores
            rowWanted = Math.min(numItemsWanted - 1, totalRows - 1);
        } else {  // top scores
            rowWanted = Math.max(0, totalRows - numItemsWanted);
        }
        String range = rowWanted + "-" + rowWanted;
        ResultSet rs = scoreNST.selectRows("*", columnToSort, range);

        String cutoffLoose;
        boolean hasRows = rs.next();

        if (hasRows) {
            cutoffLoose = rs.getString(1);
        } else {
            log.warn("Got 0 rows in Attributes.cutoffForTopNVals()");
            scoreNST.release();
            return;
        }

        // How many items will this cutoff and the next smaller give us?
        NST tooManyNST, tooFewNST;
        if (bottomN) {
            tooManyNST = scoreNST.filter(columnToSort + " LE '" + cutoffLoose + "'");
            tooFewNST = scoreNST.filter(columnToSort + " LT '" + cutoffLoose + "'");
        } else {
            tooManyNST = scoreNST.filter(columnToSort + " GE '" + cutoffLoose + "'");
            tooFewNST = scoreNST.filter(columnToSort + " GT '" + cutoffLoose + "'");
        }
        log.debug("Cutoff of " + cutoffLoose + " gives " + tooManyNST.getRowCount() + " items");

        if (numItemsWanted > totalRows) {
            log.debug("This is all the items we have");
            scoreNST.release();
            tooManyNST.release();
            tooFewNST.release();
            return;
        }

        // What is the next smaller?
        if (bottomN) {
            int lastRow = Math.max(tooFewNST.getRowCount() - 1, 0);
            rs = scoreNST.selectRows(columnToSort + " LT '" + cutoffLoose + "'",
                    "sortedScore, " + columnToSort, lastRow + "-" + lastRow);
        } else {
            rs = scoreNST.selectRows(columnToSort + " GT '" + cutoffLoose + "'",
                    "sortedScore, " + columnToSort, "0-0");
        }
        hasRows = rs.next();
        String cutoffTight;
        if (hasRows) {
            cutoffTight = rs.getString(2);
            log.debug("Next cutoff of " + cutoffTight + " gives only " + tooFewNST.getRowCount() + " items");
        } else {
            log.debug("There is no tighter cutoff than this");
        }
        scoreNST.release();
        tooManyNST.release();
        tooFewNST.release();
    }

    /**
     * Creates a new attribute named attrName with type typeDef. Types are
     * defined with this syntax:
     * <pre>
     * "columnName0:dataType0, columnName1:dataType1, ..., columnNameN-1:dataTypeN-1" </pre>
     * For example this definition:
     * <pre>
     * "year:int, gross:int" </pre>
     * Defines an attribute with two value columns: 'year', which is an int,
     * and 'gross', which is also an int. Note that the dataTypeN values above
     * must be valid Monet data types. Get these from DataTypeEnum, e.g.,
     * <pre>
     * DataTypeEnum.BIT.toString() </pre>
     * As a convenience, the first column's name (columnName0 above) can be
     * omitted, which indicates the standard single-column name ("value") is to
     * be used. For example, the following two definitions are equivalent:
     * <pre>
     * "value:int", "int"</pre>
     *
     * @param attrName
     * @param typeDef
     * @
     * @see #getAttrTypeDef(String)
     */
    public void defineAttribute(String attrName, String typeDef) {
        log.debug("defineAttribute(): " + attrName + ": " + typeDef);
        Assert.notNull(attrName, "null attrName");
        attrName = attrName.toLowerCase();
        if (isAttributeDefined(attrName)) {
            throw new MonetException("attribute already defined with name: " +
                    attrName);
        }
        defineAttributeInternal(attrName, typeDef);
    }


    /**
     * Creates a new attribute, or deletes its data NST if it already exists.
     *
     * @param attrName
     * @param typeDef
     * @
     */
    public void defineAttributeOrClearValuesIfExists(String attrName, String typeDef) {
        log.debug("defineAttributeOrClearValuesIfExists(): " + attrName + ": " + typeDef);
        Assert.notNull(attrName, "null attrName");

        attrName = attrName.toLowerCase();

        ResultSet resultSet = attrNST.selectRows("name = '" + attrName + "'", "data_type", "*");
        if (!resultSet.next()) {
            defineAttributeInternal(attrName, typeDef);
        } else {
            String currentTypeDef = resultSet.getString(1);
            if (!currentTypeDef.equals(typeDef)) {
                throw new MonetException("Attribute already defined with a different typeDef: " +
                        attrName + ", " + currentTypeDef);
            }
            // Could call getAttrDataNST here, but there would be checking
            // of the existence of the Attribute, another filter, etc.
            // The call is inlined therefore to make it faster
            // read as BATs
            // see NST documentation for an explanation of this
            int rowID = resultSet.getOID(0);
            String batName = attrNST.getColumnValueAsBATName("data", rowID);
            NST attrDataNST = new NST(batName);
            attrDataNST.deleteRows();
            attrDataNST.release();
        }
    }

    /**
     * Creates a new attribute ONLY if it doesn't already exist.
     *
     * @param attrName
     * @param typeDef
     */
    public void defineAttributeIfNotExists(String attrName, String typeDef) {
        log.debug("defineAttributeOrClearValuesIfExists(): " + attrName + ": " + typeDef);
        Assert.notNull(attrName, "null attrName");

        attrName = attrName.toLowerCase();
        ResultSet resultSet = attrNST.selectRows("name = '" + attrName + "'", "data_type", "*");
        if (!resultSet.next()) {
            defineAttributeInternal(attrName, typeDef);
        }
    }


    /**
     * Fast internal version, that doesn't verify that the attribute doesn't already exist.
     * Use with caution.
     *
     * @param attrName
     * @param typeDef
     * @
     */
    private void defineAttributeInternal(String attrName, String typeDef) {
        Assert.assertValidName(attrName);
        List attrTypeDefs = AttrType.attrTypeListForTypeDef(typeDef);

        // two step process
        // create a new NST with name with rows for each column in the typeDef
        // insert a row in the attrNST pointing to that NST

        // Create arrays of strings, with names and data types of each column
        String[] attrNames = new String[attrTypeDefs.size() + 1];
        String[] attrTypes = new String[attrTypeDefs.size() + 1];
        int index = 0;
        attrNames[index] = "id";
        attrTypes[index++] = "oid";
        Iterator attrTypeDefsIter = attrTypeDefs.iterator();
        while (attrTypeDefsIter.hasNext()) {
            AttrType attrType = (AttrType) attrTypeDefsIter.next();
            String colName = attrType.getName();
            Assert.assertValidName(colName);
            attrNames[index] = colName;
            attrTypes[index++] = attrType.getDataTypeEnum().toString();
        }

        // create AttrDataNST for new attribute's data
        NST attrDataNST = new NST(Util.join(attrNames, ","), Util.join(attrTypes, ","));
        String nstName = attrDataNST.save();
        attrNST.insertRow(new String[]{attrName, typeDef, nstName});
        attrDataNST.release();
    }

    /**
     * Creates a new attribute with values based on the evaluation of an expression
     *
     * @param attrName
     * @param expression
     */
    public void defineAttributeWithExpression(String attrName, String expression) {
        new AddAttribute().addAttribute(this, attrName, expression);
    }


    /**
     * Creates a new atribute of type typeDef, and uses the dataNST passed in as
     * the storage of values (attrDataNST is not copied but used directly; it is made
     * persistent as well)
     *
     * @param attrName
     * @param typeDef
     * @param attrDataNST
     * @
     */
    public void defineAttributeWithData(String attrName, String typeDef,
                                        NST attrDataNST) {
        log.debug("defineAttributeWithData(): " + attrName + ": " + typeDef +
                ":" + attrDataNST);

        Assert.notNull(attrName, "null attrName");
        Assert.notNull(attrDataNST, "null attrDataNST");
        attrName = attrName.toLowerCase();

        if (isAttributeDefined(attrName)) {
            throw new MonetException("attribute already defined with name: " +
                    attrName);
        }

        // two step process
        // 1. check that the columns in the given NST match the typeDef
        //    b. rename columns from the dataNST to match the type definition
        // 2. insert a row in the attrNST pointing to that NST
        List attrTypeDefs = AttrType.attrTypeListForTypeDef(typeDef);
        Assert.condition(attrTypeDefs.size() == attrDataNST.getNSTColumnNames().size() - 1,
                "given and expected column types do not match");

        Assert.condition(attrDataNST.getNSTColumn(0).getType() == DataTypeEnum.OID,
                "first column in the NST should be of type oid");
        // first, make sure that the first column is "id"
        boolean isRenamedColumns = false;
        String origIDName = attrDataNST.getNSTColumn(0).getName();
        if (!"id".equals(origIDName)) {
            attrDataNST.renameColumn(origIDName, "id");
            isRenamedColumns = true;
        }

        Iterator attrTypeDefsIter = attrTypeDefs.iterator();
        int colCnt = 1;
        while (attrTypeDefsIter.hasNext()) {
            AttrType attrType = (AttrType) attrTypeDefsIter.next();
            NSTColumn nstColumn = attrDataNST.getNSTColumn(colCnt++);
            // data type
            String dataNSTColType = nstColumn.getType().toString();
            Assert.condition(attrType.getDataTypeEnum().toString().equals(dataNSTColType),
                    "specified types do not correspond to the given types: " +
                            attrType + ", " + dataNSTColType);
            // column name
            String dataNSTColName = nstColumn.getName();
            if (!dataNSTColName.equals(attrType.getName())) {
                attrDataNST.renameColumn(dataNSTColName, attrType.getName());
                isRenamedColumns = true;
            }
        }

        // make the given NST persistent and insert the definition
        if (isRenamedColumns) {
            NST attrDataNSTCopy = attrDataNST.copy(); //save it into the top-level BAT
            attrDataNST = attrDataNSTCopy;
        }
        String nstName = attrDataNST.save();
        attrDataNST.makeWritable();
        attrNST.insertRow(new String[]{attrName, typeDef, nstName});
        if (isRenamedColumns) {
            // release the one we just created
            attrDataNST.release();
        }
    }


    /**
     * Dangerous method that deletes all of my attributes. Primarily used for
     * unit test cleanup.
     */
    public void deleteAllAttributes() {
        Iterator attrNameIterator = getAttributeNames().iterator();
        while (attrNameIterator.hasNext()) {
            String attrName = (String) attrNameIterator.next();
            deleteAttribute(attrName);
        }
    }


    /**
     * Deletes the attribute named attrName.
     *
     * @param attrName
     */
    public void deleteAttribute(String attrName) {
        if (!isAttributeDefined(attrName)) {
            throw new MonetException("no attribute defined with name: " + attrName);
        }
        deleteAttributeInternal(attrName);
    }


    /**
     * Deletes an attribute if it exists; doesn't complain if it doesn't exist.
     *
     * @param attrName
     */
    public void deleteAttributeIfExists(String attrName) {
        if (!isAttributeDefined(attrName)) {
            return;
        }
        deleteAttributeInternal(attrName);
    }

    private void deleteAttributeInternal(String attrName) {
        attrName = attrName.toLowerCase();
        NST attrDataNST = getAttrDataNST(attrName);
        attrNST.deleteRows("name = '" + attrName + "'");
        attrDataNST.delete();
    }


    /**
     * Deletes all attributes whose name begin with the given prefix.
     * Returns the count of deleted attributes.
     *
     * @param prefix
     * @return
     */
    public int deleteAttributesWithPrefix(String prefix) {
        List attrsFound = getAttributesWithPrefix(prefix);
        int cnt = 0;
        for (Iterator iter = attrsFound.iterator(); iter.hasNext();) {
            String attrName = (String) iter.next();
            deleteAttributeInternal(attrName);
            cnt++;
        }
        return cnt;
    }

    /**
     * Returns true if the other attribute is based on the same table.
     *
     * @param other
     * @return
     */
    public boolean equals(Object other) {
        if (other instanceof Attributes) {
            Attributes otherAttr = (Attributes) other;
            String otherNSTName = MonetUtil.getBBPName(otherAttr.attrNSTName);
            String thisNSTName = MonetUtil.getBBPName(this.attrNSTName);
            return (otherNSTName.equals(thisNSTName));
        }
        return false;
    }


    /**
     * Returns an NST (AttrDataNST) for the named attribute.
     * <p/>
     * The attrDataNST has the following columns
     * <ul>
     * <li>id: oid of the item (object, link, container, subgraph) that the value applies to
     * <li>value: value
     * </ul>
     * If the attribute is multi-column, then instead of the value column there will
     * be several columns, named as specified by the typeDef for the attribute.
     *
     * @param attrName
     * @see #getTypes(String)
     */
    public NST getAttrDataNST(String attrName) {
        attrName = attrName.toLowerCase();

        ResultSet resultSet = attrNST.selectRows("name = '" + attrName + "'", "data", "*");
        if (!resultSet.next()) {
            throw new MonetException("no attribute defined with name: " + attrName);
        }
        // read as BATs
        // see NST documentation for an explanation of this
        int rowID = resultSet.getOID(0);
        String batName = attrNST.getColumnValueAsBATName("data", rowID);
        return new NST(batName);
    }


    /**
     * Returns a List of attribute names (Strings) in me.
     *
     * @return
     */
    public List getAttributeNames() {
        log.debug("getAttributeNames()");
        ResultSet resultSet = attrNST.selectRows("name");
        return resultSet.toStringList(1);
    }

    public String getAttributeNamesAsString() {
        StringBuffer sb = new StringBuffer();
        List attributeNames = getAttributeNames();
        for (int attrIdx = 0; attrIdx < attributeNames.size(); attrIdx++) {
            String attrName = (String) attributeNames.get(attrIdx);
            if (attrIdx > 0) {
                sb.append(",");
            }
            sb.append(attrName);
        }
        return sb.toString();
    }


    /**
     * @param dataTypeEnum
     * @return a List of single-column attributes whose type is dataTypeEnum
     */
    public List getAttributesOfType(DataTypeEnum dataTypeEnum) {
        List matchingAttrNames = new ArrayList();   // return value
        List attrNames = getAttributeNames();
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();
            if (!isSingleValued(attrName)) {
                continue;
            }

            List attrTypeList = getTypes(attrName);
            for (Iterator attrTypeIter = attrTypeList.iterator(); attrTypeIter.hasNext();) {
                AttrType attrType = (AttrType) attrTypeIter.next();
                if (attrType.getDataTypeEnum() == dataTypeEnum) {
                    matchingAttrNames.add(attrName);
                }
            }
        }
        return matchingAttrNames;
    }


    /**
     * @param prefix
     * @return a List of attribute names that begin with prefix
     */
    public List getAttributesWithPrefix(String prefix) {
        List attrNames = new ArrayList();
        prefix = prefix.toLowerCase() + "%";
        ResultSet resultSet = attrNST.selectRows("name LIKE '" + prefix + "'", "name");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            attrNames.add(name);
        }
        return attrNames;
    }


    /**
     * Returns my attrNST. For internal use only --be careful.
     *
     * @return
     * @see NST
     */
    public NST getAttrNST() {
        return attrNST;
    }


    /**
     * Returns the type definition String of the named attribute. See
     * defineAttribute() for attrDef's syntax. For convenience you may want to
     * use getTypes, which parses the result of this method and returns AttrType
     * instances for each column.
     *
     * @param attrName
     * @see #getTypes(String)
     */
    public String getAttrTypeDef(String attrName) {
        if (!isAttributeDefined(attrName)) {
            throw new MonetException("no attribute defined with name: " + attrName);
        }
        attrName = attrName.toLowerCase();
        ResultSet resultSet = attrNST.selectRows("name EQ '" + attrName + "'", "data_type", "*");
        if (!resultSet.next()) {
            throw new MonetException("attribute not found: " + attrName);
        }
        return resultSet.getString(1);
    }


    /**
     * One-arg overload that passes null for attrNames.
     *
     * @param oid
     * @return
     * @
     */
    public AttributeValues getAttrValsForOID(int oid) {
        return getAttrValsForOID(oid, null);
    }


    /**
     * Two-arg overload. Returns a AttributeValues instance which contains all
     * attribute values in me for the thing (object, link, container, etc.) with
     * the passed oid. Works in two ways, depending on whether attrNames is
     * null: If attrNames is null, then returns values for <B>all</B>
     * attribtutes in me. If attrNames is non-null then limits the search to
     * those attributes.
     *
     * @param oid
     * @param attrNames
     * @return
     */
    public AttributeValues getAttrValsForOID(int oid, List attrNames) {
        List attrValues = new ArrayList();
        Iterator attrNameIterator = (attrNames == null ?
                getAttributeNames().iterator() : attrNames.iterator());
        while (attrNameIterator.hasNext()) {
            String attrName = (String) attrNameIterator.next();
            if (!isAttributeDefined(attrName)) {
                log.warn("skipping undefined attribute: " + attrName);
                continue;
            }
            NST attrDataNST = getAttrDataNST(attrName);     // checks name is defined
            ResultSet resultSet = attrDataNST.selectRows("id = " + oid, "*", "*");
            while (resultSet.next()) {
                int columnCount = resultSet.getColumnCount();
                String[] values = new String[columnCount - 2];      // don't store the head and oid columns
                for (int colNum = 2; colNum < columnCount; colNum++) {
                    values[colNum - 2] = resultSet.getString(colNum);
                }
                attrValues.add(new AttributeValue(attrName, values));
            }
            attrDataNST.release();
        }
        return new AttributeValues(attrValues, this);
    }


    /**
     * Returns a List of AttrType instances, one for each type defined for the
     * named attribute. This is a wrapper for getAttrTypeDef().
     *
     * @param attrName
     * @return
     * @see #getAttrTypeDef(String)
     * @see AttrType
     */
    public List getTypes(String attrName) {
        String typeDef = getAttrTypeDef(attrName);  // does toLower()
        return AttrType.attrTypeListForTypeDef(typeDef);
    }


    /**
     * Returns true if there is an attribute in me named attrName.
     *
     * @param attrName
     * @return
     */
    public boolean isAttributeDefined(String attrName) {
        Assert.notNull(attrName, "attrName");
        return (attrNST.getRowCount("name EQ '" + attrName.toLowerCase() + "'") > 0);
    }


    /**
     * Returns true if attrName has only one value column.
     *
     * @param attrName
     * @return
     * @
     */
    public boolean isSingleValued(String attrName) {
        return (getTypes(attrName).size() == 1);
    }

    public void rename(String oldName, String newName) {
        Assert.condition(!isAttributeDefined(newName), "Attribute already defined with name: " + newName);
        attrNST.replace("name = '" + oldName.toLowerCase() + "'", "name", Util.quote(newName.toLowerCase()));
    }


    /**
     * Object method.
     *
     * @return
     */
    public String toString() {
        return "[Attributes: " + attrNST + "]";
    }
}
