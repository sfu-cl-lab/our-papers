/**
 * $Id: AttributeValues.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttributeValues.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.util.Assert;


/**
 * Used by Attributes.getAttrValsForOID(), contains a table of attribute values.
 *
 * @see Attributes#getAttrValsForOID
 * @see AttributeValue
 */
public class AttributeValues {

    private List attrValues;    //  List of AttributeValue instances (my rows)
    private Attributes attributes;
    private int columnCount;    // max # rows. NB: not all values use all rows
    private int rowCount;


    /**
     * Full-arg constructor.
     *
     * @param attrValues List of AttributeValue instances
     * @param attributes Attributes that attrValues came from. useful for looking up value types for attributes
     * @see AttributeValue
     */
    public AttributeValues(List attrValues, Attributes attributes) {
        Assert.notNull(attrValues, "null attrValues");
        Assert.notNull(attributes, "null attributes");

        this.attrValues = attrValues;
        this.attributes = attributes;
        columnCount = computeColumnCount();
        rowCount = attrValues.size();
    }


    /**
     * Returns the max number of data columns in my attrValues.
     *
     * @return
     */
    private int computeColumnCount() {
        int maxCount = 0;
        Iterator attrValueIter = attrValues.iterator();
        while (attrValueIter.hasNext()) {
            AttributeValue attrVal = (AttributeValue) attrValueIter.next();
            if (attrVal.getColumnCount() > maxCount) {
                maxCount = attrVal.getColumnCount();
            }
        }
        return maxCount;
    }


    public boolean contains(AttributeValue attributeValue) {
        return attrValues.contains(attributeValue);
    }


    public Attributes getAttributes() {
        return attributes;
    }


    public AttributeValue getAttributeValue(int row) {
        return (AttributeValue) attrValues.get(row);
    }


    /**
     * Returns the AttributeValue instances in me whose name is attrName,
     * ignoring case.
     *
     * @param attrName
     * @return
     */
    public List getAttrValsForName(String attrName) {
        List matchingAttrVals = new ArrayList();
        Iterator attrValueIter = attrValues.iterator();
        while (attrValueIter.hasNext()) {
            AttributeValue attrVal = (AttributeValue) attrValueIter.next();
            if (attrVal.getName().equalsIgnoreCase(attrName)) {
                matchingAttrVals.add(attrVal);
            }
        }
        return matchingAttrVals;
    }


    /**
     * @return a List of AttributeValue instances (my rows)
     * @see AttributeValue
     */
    public List getAttrValues() {
        return attrValues;
    }


    public int getColumnCount() {
        return columnCount;
    }


    public int getRowCount() {
        return rowCount;
    }


    /**
     * Returns a Set of unique attribute names (StringS) in me.
     *
     * @return
     */
    public Set getUniqueAttrNames() {
        Set uniqueAttrNames = new HashSet();
        Iterator attrValueIter = attrValues.iterator();
        while (attrValueIter.hasNext()) {
            AttributeValue attrVal = (AttributeValue) attrValueIter.next();
            uniqueAttrNames.add(attrVal.getName());
        }
        return uniqueAttrNames;
    }


}
