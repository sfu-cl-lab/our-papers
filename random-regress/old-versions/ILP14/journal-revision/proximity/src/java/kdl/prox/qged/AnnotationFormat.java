/**
 * $Id: AnnotationFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kdl.prox.qgraph2.Annotation;
import org.apache.log4j.Logger;

/**
 * Format that knows how to parse and print Annotation objects.
 * Recall the three valid annotation formats: [i..j], [i..], or [i].
 * NB: We don't allow the [i..j] format if i==j; instead use [i] format.
 */
public class AnnotationFormat extends QGEdFormat {

    private static final Logger log = Logger.getLogger(AnnotationFormat.class);


    /**
     * Formats Annotation objects in the standard format: [i..j], [i..], or [i].
     *
     * @param obj
     * @param toAppendTo
     * @param pos
     * @return
     */
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof Annotation)) {
            throw new IllegalArgumentException("can only format Annotation " +
                    "objects: " + obj + ", " + obj.getClass());
        }

        Annotation annotation = (Annotation) obj;
        int annotMin = annotation.annotMin();
        int annotMax = annotation.annotMax();
        toAppendTo.append("[");
        toAppendTo.append(annotMin);
        if (annotMin != annotMax) {
            toAppendTo.append("..");
            if (annotMax != -1) {
                toAppendTo.append(annotMax);
            }
        }
        toAppendTo.append("]");
        pos.setBeginIndex(0);
        pos.setEndIndex(toAppendTo.length());
        return toAppendTo;
    }

    /**
     * Parses Annotation objects using the standard format: [i..j], [i..], or [i].
     *
     * @param source
     * @param pos
     * @return
     */
    public Object parseObject(String source, ParsePosition pos) {
        Pattern pattern = Pattern.compile("" +
                "\\[" + // [
                "(\\d+)" + // one or more digits - group 1
                "(\\.\\.)?" + // optional .. - group 2
                "(\\d*)" + // zero or more digits - group 3
                "\\]");    // ]
        Matcher matcher = pattern.matcher(source);
        if (!matcher.matches()) {
            return parseFailed("input didn't match pattern: [i..j], [i..], or [i]",
                    pos, 0);
        }

        String leftStr = matcher.group(1);      // always an integer
        String dotDotStr = matcher.group(2);    // null or ".." 
        String rightStr = matcher.group(3);     // an integer or ""
        boolean isDotDot = ("..".equals(dotDotStr));
        int minVal = Integer.parseInt(leftStr);
        int maxVal;    // set next
        try {
            maxVal = Integer.parseInt(rightStr);
        } catch (NumberFormatException e) {
            // no right integer
            if (isDotDot) {
                maxVal = -1;
            } else {
                maxVal = minVal;
            }
        }
        if (isDotDot && (minVal == maxVal)) {
            return parseFailed("must use [i] instead of [i..i]", pos,
                    source.indexOf("..") + 2);
        } else {
            pos.setIndex(source.length());
            return new Annotation(minVal, maxVal);
        }
    }

}
