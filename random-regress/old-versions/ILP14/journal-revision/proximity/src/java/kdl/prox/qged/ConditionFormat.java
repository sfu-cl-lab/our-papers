/**
 * $Id: ConditionFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Format that knows how to parse and print condition Elements (aka 'condEleChild'
 * Elements) objects, as wrapped by CondEleWrapper. The format (from the
 * Proximity tutorial on QGraph):
 * <p/>
 * The general form of a condition is
 * <pre><em>    attribute operator value</em></pre>
 * where
 * <ul>
 * <li><em>attribute</em> is the name of an attribute for this database entity (object or link)
 * <li><em>value</em> is a legal value for <em>attribute</em>
 * <li><em>operator</em> is one of =, &gt;=, &lt;=, and &lt;&gt;.
 * </ul>
 * The &gt; and &lt; operators are not permitted; use boolean combinations of
 * conditions (see below) for these operators.
 * <p/>
 * Surround vertex, edge, and attribute names containing spaces with single quotes.
 * <p/>
 * Query elements can only have a single condition statement, however the condition statement may include boolean combinations of conditions. Proximity supports disjunctive normal form for operator combination. For example, to match all research project pages at Cornell enter
 * <pre><code>    AND(label=ResearchProject,school=Cornell).</pre></code>
 * Proximity also supports conditions that require only that an attribute exists rather than specifying its value. The syntax for this type of condition is
 * <pre><code>    exists(attribute)</pre></code>
 */
public class ConditionFormat extends QGEdFormat {

    private static final Logger log = Logger.getLogger(ConditionFormat.class);


    /**
     * This create a string from an &lt;or&gt; element
     *
     * @param toAppendTo
     * @param condChildEle - an or element
     */
    private static void appendOrString(StringBuffer toAppendTo, Element condChildEle) {
        List testChildren = condChildEle.getChildren();
        Iterator childrenIterator = testChildren.iterator();
        toAppendTo.append("or(");
        while (childrenIterator.hasNext()) {
            Element currentChild = (Element) childrenIterator.next();
            String currentName = currentChild.getName();
            if (currentName.equals("and")) {
                appendAndString(toAppendTo, currentChild);
            } else if (currentName.equals("not")) {
                appendNotString(toAppendTo, currentChild);
            } else {
                appendTestString(toAppendTo, currentChild);
            }
            //If there are more children then we need to add a comma
            if (childrenIterator.hasNext()) {
                toAppendTo.append(" , ");
            }
        }
        toAppendTo.append(")");
    }

    /**
     * This creates a string from an &lt;and&gt; condition element
     *
     * @param toAppendTo
     * @param condChildEle - and element we are creating a string for
     */
    private static void appendAndString(StringBuffer toAppendTo, Element condChildEle) {
        List testChildren = condChildEle.getChildren();
        Iterator childrenIterator = testChildren.iterator();
        toAppendTo.append("and(");
        while (childrenIterator.hasNext()) {
            Element currentChild = (Element) childrenIterator.next();
            String currentName = currentChild.getName();
            if (currentName.equals("not")) {
                appendNotString(toAppendTo, currentChild);
            } else {
                appendTestString(toAppendTo, currentChild);
            }
            //If there are more children then we need to add a comma
            if (childrenIterator.hasNext()) {
                toAppendTo.append(" , ");
            }
        }
        toAppendTo.append(")");
    }

    /**
     * This creates a string from a &lt;not&gt; condition element
     *
     * @param toAppendTo
     * @param condChildEle - the not Element
     */
    private static void appendNotString(StringBuffer toAppendTo, Element condChildEle) {
        Element testEle = condChildEle.getChild("test");
        toAppendTo.append("not (");
        appendTestString(toAppendTo, testEle);
        toAppendTo.append(")");
    }

    /**
     * This creates a string from a &lt;test&gt; condition element
     *
     * @param toAppendTo
     * @param condChildEle - the test Element
     */
    private static void appendTestString(StringBuffer toAppendTo, Element condChildEle) {
        Element opEle = condChildEle.getChild("operator");
        String op = opEle.getText();
        if (op.equals("exists")) {
            Element attrEle = condChildEle.getChild("attribute-name");
            toAppendTo.append("exists(");
            toAppendTo.append(attrEle.getText());
            toAppendTo.append(")");
        } else {
            if (op.equals("eq")) {
                op = "=";
            }
            if (op.equals("le")) {
                op = "<=";
            }
            if (op.equals("ge")) {
                op = ">=";
            }
            if (op.equals("lt")) {
                op = "<";
            }
            if (op.equals("gt")) {
                op = ">";
            }
            if (op.equals("ne")) {
                op = "<>";
            }

            //Get out attribute-name
            Element attrEle = condChildEle.getChild("attribute-name");
            String attrName = attrEle.getText();
            if (attrName.indexOf(" ") > 0) {
                attrName = "'" + attrName + "'";
            }

            //Get out value
            Element valueEle = condChildEle.getChild("value");
            String value = valueEle.getText();
            if (value.indexOf(" ") > 0) {
                value = "'" + value + "'";
            }
            toAppendTo.append(attrName);
            toAppendTo.append(" ");
            toAppendTo.append(op);
            toAppendTo.append(" ");
            toAppendTo.append(value);
        }
    }

    /**
     * Chew up the specified character from the token stream.
     *
     * @param condTokenizer the token stream
     * @param condName      the name of the condition that is trying to chew the
     *                      character.  Used by the error message.
     * @param toBeChewed    the character/string to be chewed
     */
    private void chewChar(StringTokenizer condTokenizer, String condName,
                          String toBeChewed) throws ParseException {
        String str;
        boolean seenChar = false;
        while (!seenChar) {
            if (!condTokenizer.hasMoreTokens()) {
                String message = "Error: ran out of tokens while looking \"" +
                        toBeChewed + "\". This likely means you are missing " +
                        "\"" + toBeChewed + "\"";
                throw new ParseException(message, 0);   // todo position
            }
            str = condTokenizer.nextToken();
            if (str.equals(toBeChewed)) {
                seenChar = true;
            } else if (!str.equals(" ")) {
                String message = "ERROR: " + condName +
                        " is missing a \"" +
                        toBeChewed + "\".";
                throw new ParseException(message, 0);   // todo position
            }
        }
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof CondEleWrapper)) {
            throw new IllegalArgumentException("can only format " +
                    "CondEleWrapper objects: " + obj + ", " + obj.getClass());
        }

        CondEleWrapper condEleWrapper = (CondEleWrapper) obj;
        Element condEleChild = condEleWrapper.getCondEleChild();
        if (condEleChild != null) {
            String eleName = condEleChild.getName();
            if (eleName.equals("or")) {
                appendOrString(toAppendTo, condEleChild);
            } else if (eleName.equals("and")) {
                appendAndString(toAppendTo, condEleChild);
            } else if (eleName.equals("not")) {
                appendNotString(toAppendTo, condEleChild);
            } else { //This is a test
                appendTestString(toAppendTo, condEleChild);
            }
        }

        pos.setBeginIndex(0);
        pos.setEndIndex(toAppendTo.length());
        return toAppendTo;
    }

    /**
     * parse an and condition.  and can take multiple tests to and together.
     * Syntax: and(<test>, <test>*, <not*>).
     * The test can either have exists or attribute-value tests
     * (no OR as this is disjunctive normal form).
     *
     * @param condTokenizer current token string starting with and
     * @param condElement   Jdom element to add the and condition to
     */
    private void parseAnd(StringTokenizer condTokenizer, Element condElement)
            throws ParseException {
        Vector testData;
        String str;

        Element andEle = new Element("and");

        // loop until you see the closing parenthesis of this and
        boolean seenCloseParen = false;
        while (!seenCloseParen) {
            if (!condTokenizer.hasMoreTokens()) {
                String message = "Error: ran out of tokens before processing " +
                        "of and condition finished.  This likely means that " +
                        "the AND condition is missing a closing parenthesis.";
                throw new ParseException(message, 0);   // todo position
            }
            testData = new Vector();
            // get the current test string and skip over , or spaces
            while (testData.size() < 1) {
                str = condTokenizer.nextToken();
                // if we see the close paren, we are done with the and
                if (str.equals(")")) {
                    seenCloseParen = true;
                    break;
                }
                if (!str.equals(" ") && !str.equals("(") && !str.equals(",")) {
                    testData.add(str);
                }
            }

            // parse the and-body. it can only be not, exists,
            // or a test on an attribute name.
            if (!seenCloseParen) {
                String firstToken = (String) testData.get(0);
                if (firstToken.equalsIgnoreCase("not")) {
                    parseNot(condTokenizer, andEle);
                } else if (firstToken.equalsIgnoreCase("exists")) {
                    parseExists(condTokenizer, andEle);
                } else if (firstToken.equalsIgnoreCase("or")) {
                    String message = "Error: Disjunctive normal form does" +
                            "not allow ORs to occur inside ANDs";
                    throw new ParseException(message, 0);   // todo position
                } else {
                    parseTest(firstToken, andEle);
                }
            }
        }

        condElement.addContent(andEle);
    }

    private void parseExists(StringTokenizer condTokenizer, Element condElement)
            throws ParseException {
        Vector testData;
        String str;

        // Create an JDOM element for this test
        Element testEle = new Element("test");

        Element opEle = new Element("operator");
        opEle.addContent("exists");

        // find the attribute name and skip over the open paren
        testData = new Vector();
        while (testData.size() < 1) {
            str = condTokenizer.nextToken();
            if (!str.equals(" ") && !str.equals("(")) {
                testData.add(str);
            }
        }

        String operand = (String) testData.get(0);

        Element attrEle = new Element("attribute-name");
        attrEle.addContent(operand);

        testEle.addContent(opEle);
        testEle.addContent(attrEle);

        condElement.addContent(testEle);

        // chew up the closing parenthesis (exists requires a paren around
        // the attribute name that is being tested
        chewChar(condTokenizer, "Exists", ")");
    }

    /**
     * Parse a negation condition.  Syntax: not(<test>).  In this case, the
     * test body can either be an exists or a normal attribute-value test.
     *
     * @param condTokenizer the tokenized list of condition (starting with not)
     * @param condElement   the Jdom element to add this condition to
     */
    private void parseNot(StringTokenizer condTokenizer, Element condElement)
            throws ParseException {
        Vector testData;
        String str;

        Element notEle = new Element("not");

        // loop through the tokens until you get the interior of the not
        // and skip over open parenthesis
        testData = new Vector();
        while (testData.size() < 1) {
            str = condTokenizer.nextToken();
            if (!str.equals(" ") && !str.equals("(")) {
                testData.add(str);
            }
        }

        // split based on whether this is an exists or regular test
        String firstToken = (String) testData.get(0);
        if (firstToken.equalsIgnoreCase("exists")) {
            parseExists(condTokenizer, notEle);
        } else {
            parseTest(firstToken, notEle);
        }

        // chew up the final parenthesis
        chewChar(condTokenizer, "Not", ")");

        condElement.addContent(notEle);
    }

    /**
     * NB: Callers must use code like the following to utilize the returned
     * Element (as wrapped by CondEleWrapper):
     * <pre>
     * item.deleteCondEleChild();
     * item.setCondition(newElement);
     * Element condEleChild = item.condEleChild();
     * condEleChild.detach();
     * </pre>
     *
     * @param source
     * @param pos
     * @return
     */
    public Object parseObject(String source, ParsePosition pos) {
        Element newElement = new Element("condition");  // temp element to hold results

        // parse the string and add children as appropriate to newElement.
        // first, remove the spaces from the condition just in case
        String label = source;
        //label = label.replaceAll(" ", "");
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
                //log.debug("Next token:" + token);
            }

        }
        label = noSpaces;
        //log.debug(label);
        StringTokenizer condTokenizer = new StringTokenizer(label, "(),", true);
        // check to see if there is actually a condition or not

        // if the user entered a blank condition string, simply return
        if (!condTokenizer.hasMoreTokens()) {
            pos.setErrorIndex(0);
            return null;
        }

        String firstToken = condTokenizer.nextToken();

        // compare first to the possible values
        try {
            if (firstToken.equalsIgnoreCase("or")) {
                parseOr(condTokenizer, newElement);
            } else if (firstToken.equalsIgnoreCase("and")) {
                parseAnd(condTokenizer, newElement);
            } else if (firstToken.equalsIgnoreCase("not")) {
                parseNot(condTokenizer, newElement);
            } else if (firstToken.equalsIgnoreCase("exists")) {
                parseExists(condTokenizer, newElement);
            } else {
                parseTest(firstToken, newElement);
            }
        } catch (ParseException e) {
            return parseFailed(e.getMessage(), pos, e.getErrorOffset());
        }


        // did we use up all the tokens?  If not, there was a syntax error
        // so delete the condition and throw an error
        if (condTokenizer.hasMoreTokens()) {
            String leftover = "";
            while (condTokenizer.hasMoreTokens()) {
                leftover += condTokenizer.nextToken();
            }
            String errorMessage = "Parse error: there were tokens left over " +
                    "starting at " + leftover + ".  Likely there is an extra " +
                    "parenthesis.";
            return parseFailed(errorMessage, pos, source.indexOf(leftover));
        } else {
            pos.setIndex(source.length());
            Element condEleChild = (Element) newElement.getChildren().iterator().next();
            return new CondEleWrapper(condEleChild);
        }
    }

    /**
     * parse an or condition in disjunctive normal form.
     * Syntax: or(<test>*, <and>*, <not>*)
     *
     * @param condTokenizer the current token stream starting with or
     * @param condElement   Jdom element to add the or condition to
     */
    private void parseOr(StringTokenizer condTokenizer, Element condElement)
            throws ParseException {
        Vector testData;
        String str;

        Element orEle = new Element("or");

        // loop until you see a closing parenthesis
        boolean seenCloseParen = false;
        while (!seenCloseParen) {
            if (!condTokenizer.hasMoreTokens()) {
                String message = "Error: ran out of tokens before processing " +
                        "of and condition finished.  This likely means that " +
                        "the OR condition is missing a closing parenthesis.";
                throw new ParseException(message, 0);
            }

            testData = new Vector();
            while (testData.size() < 1) {
                str = condTokenizer.nextToken();
                // if we see the close paren, we are done with the and
                if (str.equals(")")) {
                    seenCloseParen = true;
                    break;
                }
                if (!str.equals(" ") && !str.equals("(") && !str.equals(",")) {
                    testData.add(str);
                }
            }

            // or can contain and, not, exists, or a general test
            if (!seenCloseParen) {
                String firstToken = (String) testData.get(0);
                if (firstToken.equalsIgnoreCase("and")) {
                    parseAnd(condTokenizer, orEle);
                } else if (firstToken.equalsIgnoreCase("not")) {
                    parseNot(condTokenizer, orEle);
                } else if (firstToken.equalsIgnoreCase("exists")) {
                    parseExists(condTokenizer, orEle);
                } else {
                    parseTest(firstToken, orEle);
                }
            }
        }

        condElement.addContent(orEle);
    }

    /**
     * Parse a test condition that specifies an operator, the name of
     * the attribute to be tested, and the value to test on.
     * Syntax: <attribute-name> <operator> <value> where <operator> = {<=|>=|=}
     *
     * @param testString  contains the entire test condition in one string
     * @param condElement Jdom element that this test is being added to
     */
    private void parseTest(String testString, Element condElement)
            throws ParseException {
        String attribute, operator, value;

        //log.debug("Test:" + testString);

        // did the user put a space between the testString and the operator?
        // if not, split it up now.
        StringTokenizer testTokens = new StringTokenizer(testString,
                "=><", true);

        // did we see all three required parts of the test string?
        if (testTokens.countTokens() < 3) {
            String message = "Invalid test condition: " +
                    testString + ". Test should" +
                    " be in the form " +
                    " <attribute-name> " +
                    "<operator> " +
                    "<value>";
            throw new ParseException(message, 0);
        }

        // the first token is always attribute
        attribute = testTokens.nextToken();
        attribute = attribute.replaceAll("'", "");
        //log.debug("New Attribute:" + attribute);

        // the = is only one token but <= and >= are two.
        if (testTokens.countTokens() == 2) {
            operator = testTokens.nextToken();
        } else {
            operator = testTokens.nextToken() + testTokens.nextToken();
        }

        // the remaining token is the value
        value = testTokens.nextToken();
        //Need to strip the quotes from value if they exist
        value = value.replaceAll("'", "");

        Element testEle = new Element("test");
        Element opEle = new Element("operator");
        if (operator.equals("=")) {
            opEle.addContent("eq");
        }
        if (operator.equals("<=")) {
            opEle.addContent("le");
        }
        if (operator.equals(">=")) {
            opEle.addContent("ge");
        }
        if (operator.equals("<>")) {
            opEle.addContent("ne");
        }
        if (operator.equals("<")) {
            opEle.addContent("lt");
        }
        if (operator.equals(">")) {
            opEle.addContent("gt");
        }

        Element attrEle = new Element("attribute-name");
        attrEle.addContent(attribute);

        Element valueEle = new Element("value");
        valueEle.addContent(value);

        testEle.addContent(opEle);
        testEle.addContent(attrEle);
        testEle.addContent(valueEle);

        condElement.addContent(testEle);
    }

}
