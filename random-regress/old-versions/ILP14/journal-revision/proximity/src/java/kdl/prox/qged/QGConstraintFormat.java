/**
 * $Id: QGConstraintFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.StringTokenizer;
import kdl.prox.qgraph2.QGConstraint;
import org.apache.log4j.Logger;

/**
 * Format that knows how to parse and print QGConstraint objects. The format
 * (from the Proximity tutorial on QGraph):
 * <p/>
 * The general form of a parsed constraint is
 * <pre><em>    element1.attribute1   operator   element2.attribute2</em></pre>
 * where
 * <ul>
 * <li>element1 and element2 are the names of two vertices or two edges in the query
 * <li>attribute1 is the name of an attribute for element1
 * <li>attribute2 is the name of an attribute for element2
 * <li>operator is one of =, &lt;&gt;, &gt;=, &lt;=, &lt;, &gt; (note that operators
 * are converted to symbolic equivalents when saving to XML - eq, ne, etc.)
 * </ul>
 * For identity constraints the general form is
 * <pre><em>    element1   operator   element2</em></pre>
 * <p/>
 * Surround vertex, edge, and attribute names containing spaces with single quotes.
 * <p/>
 * Attribute1 and attribute2 must be comparable types, but need not be the same attribute name.
 */
public class QGConstraintFormat extends QGEdFormat {

    private static final Logger log = Logger.getLogger(QGConstraintFormat.class);


    private void appendItem(QGConstraint qgConstraint, StringBuffer toAppendTo,
                            boolean isItem1) {
        String itemName = (isItem1 ? qgConstraint.item1Name() : qgConstraint.item2Name());
        boolean isItemNameHasSpaces = itemName.indexOf(" ") != -1;
        if (isItemNameHasSpaces) {
            toAppendTo.append("'");
        }
        toAppendTo.append(itemName);
        if (isItemNameHasSpaces) {
            toAppendTo.append("'");
        }

        String attrName = (isItem1 ? qgConstraint.item1AttrName() :
                qgConstraint.item2AttrName());
        if (attrName != null) {
            boolean isAttrNameHasSpaces = attrName.indexOf(" ") != -1;
            toAppendTo.append(".");
            if (isAttrNameHasSpaces) {
                toAppendTo.append("'");
            }
            toAppendTo.append(attrName);
            if (isAttrNameHasSpaces) {
                toAppendTo.append("'");
            }
        }
    }

    private void appendOp(StringBuffer toAppendTo, String op) {
        toAppendTo.append(" ");
        if (op.equals("eq")) {
            toAppendTo.append("=");
        } else if (op.equals("le")) {
            toAppendTo.append("<=");
        } else if (op.equals("ge")) {
            toAppendTo.append(">=");
        } else if (op.equals("ne")) {
            toAppendTo.append("<>");
        } else if (op.equals("lt")) {
            toAppendTo.append("<");
        } else {    // (op.equals("gt")
            toAppendTo.append(">");
        }
        toAppendTo.append(" ");
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof QGConstraint)) {
            throw new IllegalArgumentException("can only format QGConstraint " +
                    "objects: " + obj + ", " + obj.getClass());
        }

        QGConstraint qgConstraint = (QGConstraint) obj;
        appendItem(qgConstraint, toAppendTo, true);
        appendOp(toAppendTo, qgConstraint.operator());
        appendItem(qgConstraint, toAppendTo, false);
        return toAppendTo;
    }

    /**
     * NB: It's the caller's responsibility to set isEdgeConstraint, annotation,
     * and annotItemName on the returned QGConstraint!
     *
     * @param source
     * @param pos
     * @return a QGConstraint instance
     */
    public Object parseObject(String source, ParsePosition pos) {
        // parse the string and add children as appropriate to newElement.
        // first, remove the spaces from the condition just in case
        String label = source;
        StringTokenizer spaceRemover = new StringTokenizer(label, " '", true);
        String noSpaces = "";
        while (spaceRemover.hasMoreTokens()) {
            String token = spaceRemover.nextToken();
            if (!token.equalsIgnoreCase(" ")) {
                if (token.equalsIgnoreCase("'")) {
                    token += spaceRemover.nextToken("'");
                    token += spaceRemover.nextToken("' ");
                }
                noSpaces += token;
            }

        }
        label = noSpaces;
        StringTokenizer testTokens = new StringTokenizer(label, "=><", true);
        if (testTokens.countTokens() == 0) {
            return parseFailed("no tokens", pos, 0);
        }


        // did we see all three required parts of the test string?
        if (testTokens.countTokens() < 3) {
            String message = "Invalid constraint: " +
                    source + ". Constraint should" +
                    " be in the form " +
                    " <item-name>[.<attrbute-name>] " +
                    "<operator> " +
                    "<item-name>[.<attrbute-name>]";
            return parseFailed(message, pos, 0);    // todo position
        }

        // the first token is item-name with or without attribute
        String item1 = testTokens.nextToken();
        String operator;
        String opString = "eq";

        // the = is only one token but <= and >= are two.
        if (testTokens.countTokens() == 2) {
            operator = testTokens.nextToken();
        } else {
            operator = testTokens.nextToken() + testTokens.nextToken();
        }

        // the remaining token is the value
        String item2 = testTokens.nextToken();


        if (operator.equals("=")) {
            opString = "eq";
        } else if (operator.equals("<=")) {
            opString = "le";
        } else if (operator.equals(">=")) {
            opString = "ge";
        } else if (operator.equalsIgnoreCase("<>")) {
            opString = "ne";
        } else if (operator.equals("<")) {
            opString = "lt";
        } else {    // operator.equals(">")
            opString = "gt";
        }


        // need to parse items to find item-names and attributes names
        String item1AttrName = null;
        StringTokenizer item1Tokens = new StringTokenizer(item1, ".", false);
        String item1Name = item1Tokens.nextToken();
        if (item1Tokens.countTokens() > 0) {    // this has an attribute
            item1AttrName = item1Tokens.nextToken();
        }

        String item2AttrName = null;
        StringTokenizer item2Tokens = new StringTokenizer(item2, ".", false);
        String item2Name = item2Tokens.nextToken();
        if (item2Tokens.countTokens() > 0) {    // this has an attribute
            item2AttrName = item2Tokens.nextToken();
        }


        QGConstraint qgConstraint = new QGConstraint();
        qgConstraint.setOperator(opString);
        qgConstraint.setItem1Name(removeQuotesIfNecessary(item1Name));
        qgConstraint.setItem2Name(removeQuotesIfNecessary(item2Name));
        qgConstraint.setItem1AttrName(removeQuotesIfNecessary(item1AttrName));
        qgConstraint.setItem2AttrName(removeQuotesIfNecessary(item2AttrName));
        // NB: we don't set isEdgeConstraint, annotation, or annotItemName

        pos.setIndex(source.length());
        return qgConstraint;
    }

    private String removeQuotesIfNecessary(String string) {
        if (string == null) {
            return string;
        }

        int startIdx = (string.startsWith("'") ? 1 : 0);
        int endIdx = (string.endsWith("'") ? string.length() - 1 : string.length());
        return string.subSequence(startIdx, endIdx).toString();
    }

}
