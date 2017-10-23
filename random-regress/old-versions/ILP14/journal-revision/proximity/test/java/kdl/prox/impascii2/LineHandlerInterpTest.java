/**
 * $Id: LineHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: LineHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;


/**
 * Tests the middle level of smart ascii processing: Interpreting line types
 * in order.
 */
public class LineHandlerInterpTest extends TestCase {

    private static final Logger log = Logger.getLogger(LineHandlerInterpTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testCountHandler() {
        BufferedReader bufferedReader = null;
        try {
            CountLineHandler countHandler = new CountLineHandler();
            URL resURL = getClass().getResource("test-sa.txt");
            Object content = resURL.getContent();
            InputStream inputStream = (InputStream) content;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            Interpreter.readLines(bufferedReader, countHandler);
            assertEquals(6, countHandler.getNumAttrs());
            assertEquals(1, countHandler.getNumComments());
            assertEquals(2, countHandler.getNumLinks());
            assertEquals(3, countHandler.getNumObjects());
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

}


/**
 * Simple handler that just counts the types of lines.
 */
class CountLineHandler implements InterpreterLineHandler {

    private int numAttrs = 0;
    private int numComments = 0;
    private int numLinks = 0;
    private int numObjects = 0;

    public void doAttribute(String name, String value) {
        numAttrs++;
    }

    public void doComment(String comment) {
        numComments++;
    }

    public void doLink(String o1Name, String o2Name) {
        numLinks++;
    }

    public void doObject(String name) {
        numObjects++;
    }

    public int getNumAttrs() {
        return numAttrs;
    }

    public int getNumComments() {
        return numComments;
    }

    public int getNumLinks() {
        return numLinks;
    }

    public int getNumObjects() {
        return numObjects;
    }

}
