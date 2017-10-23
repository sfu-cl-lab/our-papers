/**
 * $Id: BooleanFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.FieldPosition;
import java.text.ParsePosition;


/**
 * Format that knows how to parse and print Boolean objects as 'true' and 'false'.
 */
public class BooleanFormat extends QGEdFormat {

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof Boolean)) {
            throw new IllegalArgumentException("can only format Annotation " +
                    "objects: " + obj + ", " + obj.getClass());
        }

        Boolean booleanObj = (Boolean) obj;
        toAppendTo.append(booleanObj.booleanValue() ? "true" : "false");
        pos.setBeginIndex(0);
        pos.setEndIndex(toAppendTo.length());
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if ("true".equalsIgnoreCase(source) || "false".equalsIgnoreCase(source)) {
            pos.setIndex(source.length());
            return new Boolean(source);
        } else {
            return parseFailed("input was neither 'true' or 'false'", pos, 0);
        }
    }

}
