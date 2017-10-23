/**
 * $Id: Assert.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Assert.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.util;


/**
 * Utility class that defines static assertion methods.
 */
public class Assert {
    public static final String INVALID_NAME_CHARS = "/!?#<>";     // characters that are not allowed in container names

    // no IVs

    /**
     * Throws IllegalArgumentException if name is not valid.
     *
     * @param name
     */
    public static void assertValidName(String name) {
        String isInvalidNameStr = Assert.isInvalidName(name);
        condition(isInvalidNameStr == null, isInvalidNameStr);
    }

    /**
     * One arg overload, throws IllegalArgumentException (containing
     * failMessage) if isTrueCondition is false. Does nothing if it is true.
     * Used to assert some important condition is true, for example:
     * <p/>
     * Assert.condition(threshold >= 0, "threshold must be positive");
     */
    public static void condition(boolean isTrueCondition, String failMessage)
            throws IllegalArgumentException {
        if (!isTrueCondition) {
            throw new IllegalArgumentException(failMessage);
        }
    }


    /**
     * @param name
     * @return top-level method to determine if name is valid.
     *         Used to see if a container name, attribute name, or attribute column name
     *         doesn't have invalid characters. returns
     *         a non-null String (sentence) if name is <em>not</em> valid. In
     *         this case the String describes why that is the case. Returns null
     *         if the name <em>is</em> valid. Valid container names must be non-null and
     *         non-empty StringS that don't contain any characters in
     *         Container.INVALID_NAME_CHARS
     */
    public static String isInvalidName(String name) {
        if ((name == null) || "".equals(name)) {
            return "Name '" + name + "' was null or empty."; // invalid
        }
        for (int charIdx = 0; charIdx < Assert.INVALID_NAME_CHARS.length(); charIdx++) {
            char badChar = Assert.INVALID_NAME_CHARS.charAt(charIdx);
            if (name.lastIndexOf(badChar) != -1) {
                return "Name '" + name + "' had one of " +
                        "the following invalid characters: '" +
                        Assert.INVALID_NAME_CHARS + "'.";    // invalid
            }
        }
        return null;    // valid
    }

    /**
     * Throws IllegalArgumentException if object is not null. Does nothing o/w.
     *
     * @param object object to test
     * @param arg    used in the message if not null
     * @throws IllegalArgumentException
     */
    public static void isNull(Object object, String arg)
            throws IllegalArgumentException {
        condition(object == null, arg);
    }


    /**
     * Throws IllegalArgumentException if object is null. Does nothing o/w.
     *
     * @param object object to test
     * @param arg    used in the message if null
     * @throws IllegalArgumentException
     */
    public static void notNull(Object object, String arg)
            throws IllegalArgumentException {
        condition(object != null, arg);
    }


    /**
     * Throws IllegalArgumentException if string is null or empty. Does nothing
     * o/w.
     *
     * @param string string to test
     * @param arg    used in the message if null
     * @throws IllegalArgumentException
     */
    public static void stringNotEmpty(String string, String arg)
            throws IllegalArgumentException {
        condition((string != null) && (string.length() != 0), arg);
    }


}
