/**
 * $Id: StringDBHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: StringDBHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


/**
 * Tests the highest level of smart ascii processing using StringS: Proximity
 * database event handling.
 */
public class StringDBHandlerInterpTest extends TestCase {

    private static final Logger log = Logger.getLogger(StringDBHandlerInterpTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testStringDBHandler() {
        String[] expAttrDefs = new String[]{
            "a|s|a", "b|s|b", "b|i|1", "c|i|2", "a->c|s|a->c", "b->c|s|b->c"};
        String[] expLinkNames = new String[]{"a->c", "b->c"};
        String[] expObjectNames = new String[]{"a", "b", "c"};

        BufferedReader bufferedReader = null;
        try {
            StringDBHandler stringDBHandler = new StringDBHandler();
            URL resURL = getClass().getResource("test-sa.txt");
            Object content = resURL.getContent();
            InputStream inputStream = (InputStream) content;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Interpreter.readLines(bufferedReader, stringDBHandler);
            List attrDefs = stringDBHandler.getAttrDefs();
            List objectNames = stringDBHandler.getObjectNames();
            List linkNames = stringDBHandler.getLinkNames();

            assertEquals(expAttrDefs.length, attrDefs.size());
            assertTrue(attrDefs.containsAll(Arrays.asList(expAttrDefs)));

            assertEquals(expLinkNames.length, linkNames.size());
            assertTrue(linkNames.containsAll(Arrays.asList(expLinkNames)));

            assertEquals(expObjectNames.length, objectNames.size());
            assertTrue(objectNames.containsAll(Arrays.asList(expObjectNames)));

        } catch (IOException ioExc) {
            log.error("error testing", ioExc);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public void testDBHandlerBadFile() {
        BufferedReader bufferedReader = null;
        try {
            StringDBHandler stringDBHandler = new StringDBHandler();
            URL resURL = getClass().getResource("test-bad-sa.txt");
            Object content = resURL.getContent();
            InputStream inputStream = (InputStream) content;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Interpreter.readLines(bufferedReader, stringDBHandler);
            fail("should fail on bad file");
        } catch (IOException ioExc) {
            log.error("error testing", ioExc);
        } catch (IllegalArgumentException iaExc) {
            // ignored
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

}


/**
 * Simple handler that catenates types as Strings.
 */
class StringDBHandler implements DBHandler {

    private String currLinkOrObjName = null;

    private List attrDefs = new ArrayList();
    private List linkNames = new ArrayList();
    private List objectNames = new ArrayList();


    public void addAttribute(String name, String value) {
        attrDefs.add(currLinkOrObjName + "|" + name + "|" + value);
    }

    public void addLink(String o1Name, String o2Name) {
        String linkName = o1Name + "->" + o2Name;
        linkNames.add(linkName);
        currLinkOrObjName = linkName;
    }

    public void addObject(String name) {
        objectNames.add(name);
        currLinkOrObjName = name;
    }

    public List getAttrDefs() {
        return attrDefs;
    }

    public List getLinkNames() {
        return linkNames;
    }

    public List getObjectNames() {
        return objectNames;
    }

}
