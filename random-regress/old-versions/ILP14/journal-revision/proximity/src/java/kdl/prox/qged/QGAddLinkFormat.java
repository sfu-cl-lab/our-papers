/**
 * $Id: QGAddLinkFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qged;

import kdl.prox.qgraph2.QGAddLink;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.List;

/**
 * Format that knows how to parse and print QGAddLink objects.
 * The format is vertex1, vertex2, attrname, attrvalue
 */
public class QGAddLinkFormat extends QGEdFormat {

    private static final Logger log = Logger.getLogger(QGAddLinkFormat.class);


    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof QGAddLink)) {
            throw new IllegalArgumentException("can only format QGAddLink " +
                    "objects: " + obj + ", " + obj.getClass());
        }

        QGAddLink qgAddLink = (QGAddLink) obj;
        toAppendTo.append(qgAddLink.getVertex1Name());
        toAppendTo.append(", ");
        toAppendTo.append(qgAddLink.getVertex2Name());
        toAppendTo.append(", ");
        toAppendTo.append(qgAddLink.getAttrName());
        toAppendTo.append(", ");
        toAppendTo.append(Util.quote(qgAddLink.getAttrValue()));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        List elts = Util.splitQuotedString(source, ',');
        // did we see all four required parts of the test string?
        if (elts.size() != 4) {
            String message = "Invalid add-link: " +
                    source + ". \nShould" +
                    " be in the form <vertex1>, <vertex2>, <attrname>, <attrvalue>" +
                    ", \n all separated by commas";
            return parseFailed(message, pos, 0);    // todo position
        }


        pos.setIndex(source.length());
        String vertex1Name = (String) elts.get(0);
        String vertex2Name = (String) elts.get(1);
        String attrName = (String) elts.get(2);
        String attrVal = (String) elts.get(3);
        return new QGAddLink(vertex1Name.trim(), vertex2Name.trim(),
                attrName.trim(), Util.unQuote(attrVal.trim()));
    }

}
