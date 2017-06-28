/**
 * $Id: ItemTypeEnum.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ItemTypeEnum.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

/**
 * Typesafe enum helper class used by ProxItem.java.
 */
public class ItemTypeEnum {

    /**
     * My name. Set by constructor.
     */
    private final String name;

    /**
     * The public instances.
     */
    public static final ItemTypeEnum LINK = new ItemTypeEnum("L");
    public static final ItemTypeEnum OBJECT = new ItemTypeEnum("O");


    /**
     * Private constructor.
     */
    private ItemTypeEnum(String name) {
        this.name = name;
    }


    /**
     * Object method. final so that it can't be redefined.
     */
    public final boolean equals(Object that) {
        return super.equals(that);
    }


    /**
     * Object method. final so that it can't be redefined.
     */
    public final int hashCode() {
        return super.hashCode();
    }


    /**
     * Object method.
     */
    public String toString() {
        return name;
    }


    /**
     * Utility that returns the appropriate static instance (LINK or OBJECT)
     * whose name equals string ("L" or "O"). Throws IllegalArgumentException if
     * string is invalid.
     */
    public static ItemTypeEnum valueOf(String string) throws IllegalArgumentException {
        string = string.toUpperCase();
        if (string.equals(LINK.toString()))
            return LINK;
        else if (string.equals(OBJECT.toString()))
            return OBJECT;
        else
            throw new IllegalArgumentException("invalid string '" + string +
                    "'. must be one of LINK or OBJECT");
    }


}
