/**
 * $Id: ResultSetTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ResultSetTest.java 3658 2007-10-15 16:29:11Z schapira $
 */
package kdl.prox.monet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;


/**
 * Tests internal ResultSet methods. NB: Actual operation of ResultSetS is
 * tested indirectly by others (look for ResultSet.next() callers).
 *
 * @see ResultSet
 */
public class ResultSetTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }


    // don't need db to test
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testReadAndBreak()  {
        ResultSet s = Connection.executeQuery("ls()");
        s.next();
        // execute another command, even if we're not done
        s = Connection.executeQuery("ls()");
        s.next();
    }


    public void testParseRowBackslashes() {
        String currentLine = "[ 303@0,  305@0,\t  \"stagename\",\t  \"Val\\\\\\\"erie Bonnier\"\t\t  ]";
        List expectedList = Arrays.asList(new String[]{"303@0", "305@0", "\"stagename\"", "\"Val\\\\\\\"erie Bonnier\""});

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


    public void testParseRowChar() {
        String currentLine = "[ 302@0,  304@0,\t  \"stagename\",\t  \'B\'\t\t  ]";
        List expectedList = Arrays.asList(new String[]{"302@0", "304@0", "\"stagename\"", "\'B\'"});

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


    public void testParseRowChars() {
        String currentLine = "[ 302@0,  304@0,\t  \"stagename\",\t  \'BC\'\t\t  ]";
        List expectedList = Arrays.asList(new String[]{"302@0", "304@0", "\"stagename\"", "\'BC\'"});   // todo would never occur in Monet, so is this the right output?

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


    public void testParseRowNewline() {
        String currentLine = "[8@0, 3@0, \"notes\", \"Ch,\\n Roger\"]";
        List expectedList = Arrays.asList(new String[]{"8@0", "3@0", "\"notes\"", "\"Ch,\\n Roger\""});

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


    public void testParseRowNoBackslashes() {
        String currentLine = "[ 302@0,  304@0,\t  \"stagename\",\t  \"Beverly Bonner\"\t\t  ]";
        List expectedList = Arrays.asList(new String[]{"302@0", "304@0", "\"stagename\"", "\"Beverly Bonner\""});

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


    public void testParseRowInt() {
        String currentLine = "[ 302@0,  304@0,\t  \"stagename\",\t  34\t\t  ]";
        List expectedList = Arrays.asList(new String[]{"302@0", "304@0", "\"stagename\"", "34"});

        List columnValueList = new ArrayList();
        ResultSet.parseRow(columnValueList, currentLine);
        assertEquals(expectedList, columnValueList);
    }


}
