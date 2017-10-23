/**
 * $Id: AttributeValue.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttributeValue.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import java.util.Arrays;
import java.util.List;
import kdl.prox.util.Assert;


/**
 * Helper class used by AttributeValues, stores a particular attribute name and
 * value(s), i.e., a single row.
 *
 * @see AttributeValues
 */
public class AttributeValue {

    private String name;
    private String[] values;


    public AttributeValue(String name, String[] values) {
        Assert.stringNotEmpty(name, "empty name");
        Assert.notNull(values, "values null");
        this.name = name;
        this.values = values;
    }


    public AttributeValue(String name, List values) {
        Assert.stringNotEmpty(name, "empty name");
        Assert.notNull(values, "values null");
        this.name = name;
        this.values = new String[values.size()];
        values.toArray(this.values);
    }


    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof AttributeValue) {
            AttributeValue anotherAttrVal = (AttributeValue) anObject;
            return (name.equalsIgnoreCase(anotherAttrVal.getName()) &&
                    Arrays.equals(values, anotherAttrVal.getValues()));
        }
        return false;
    }


    public String getName() {
        return name;
    }


    public String[] getValues() {
        return values;
    }


    /**
     * Returns the hashCode for this item
     */
    public int hashCode() {
        return name.hashCode() +
                values.hashCode();
    }


    public int getColumnCount() {
        return values.length;
    }


    public String toString() {
        String listString = Arrays.asList(values).toString();
        listString = listString.substring(1, listString.length() - 1); // remove []s
        return name + ": " +
                (values.length > 1 ? "[" : "") + listString + (values.length > 1 ? "]" : "");
    }


}
