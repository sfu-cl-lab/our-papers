/**
 * $Id: LineParserTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: LineParserTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;


/**
 * Tests the lowest level of smart ascii processing: Parsing of line types.
 */
public class LineParserTest extends TestCase {

    private LineParser parser;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        this.parser = new LineParser();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testAttributeLine() {
        String[][] attrLineAndVals = new String[][]{
            {"n:v", "n", "v"},
            {" n : v ", "n", "v"},
            {"n n:v", "n n", "v"},
            {"n:v:v", "n", "v:v"}};
        for (int lineIdx = 0; lineIdx < attrLineAndVals.length; lineIdx++) {
            String[] attrLineNameVal = attrLineAndVals[lineIdx];
            String line = attrLineNameVal[0];
            String name = attrLineNameVal[1];
            String value = attrLineNameVal[2];
            parser.parseLine(line);
            assertTrue(parser.isAttribute());
            assertEquals(name, parser.getAttributeName());
            assertEquals(value, parser.getAttributeValue());
        }
    }

    public void testBadLines() {
        String[] lines = new String[]{
            " # ",
            " *",
            "",
            " asfd ",
            "* o1 ->",
            "* -> o2",
            "* -> "};
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            try {
                parser.parseLine(line);
                fail("parsing bad line should fail: '" + line + "'");
            } catch (IllegalArgumentException iaExc) {
                // ignore
            }
        }
    }

    public void testBlankLines() {
        String[] lines = new String[]{
            "",
            " ",
            "\t"};
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            try {
                parser.parseLine(line);
                fail("parsing blank or whitespace lines should fail");
            } catch (IllegalArgumentException iaExc) {
                // ignore
            }
        }
    }

    public void testCommentLine() {
        String[][] commentLineAndVals = new String[][]{
            {"# comment", " comment"},
            {"#", ""}};
        for (int commentLineIdx = 0; commentLineIdx < commentLineAndVals.length;
             commentLineIdx++) {
            String[] commentLine = commentLineAndVals[commentLineIdx];
            String line = commentLine[0];
            String comment = commentLine[1];
            parser.parseLine(line);
            assertTrue(parser.isComment());
            assertEquals(comment, parser.getComment());
        }
    }

    public void testLinkLine() {
        String[][] linkLineAndVals = new String[][]{
            {"* o1 -> o2 ", "o1", "o2"},
            {"*o1->o2", "o1", "o2"}};
        for (int linkLineIdx = 0; linkLineIdx < linkLineAndVals.length;
             linkLineIdx++) {
            String[] linkLine = linkLineAndVals[linkLineIdx];
            String line = linkLine[0];
            String o1Name = linkLine[1];
            String o2Name = linkLine[2];
            parser.parseLine(line);
            assertTrue(parser.isLink());
            assertEquals(o1Name, parser.getLinkO1Name());
            assertEquals(o2Name, parser.getLinkO2Name());
        }
    }

    public void testObjectLine() {
        String[][] objectLineAndVals = new String[][]{
            {"* n ", "n"},
            {"* n n ", "n n"}};
        for (int objectLineIdx = 0; objectLineIdx < objectLineAndVals.length;
             objectLineIdx++) {
            String[] objectLine = objectLineAndVals[objectLineIdx];
            String line = objectLine[0];
            String name = objectLine[1];
            parser.parseLine(line);
            assertTrue(parser.isObject());
            assertEquals(name, parser.getObjectName());
        }
    }

    public void testMultipleParses() {
        parser.parseLine("an:attribute");
        parser.parseLine("# a comment");
        parser.parseLine("* a->link");
        parser.parseLine("* an object");
        parser.resetState();
        assertFalse(parser.isAttribute());
        assertFalse(parser.isComment());
        assertFalse(parser.isLink());
        assertFalse(parser.isObject());
    }

}
