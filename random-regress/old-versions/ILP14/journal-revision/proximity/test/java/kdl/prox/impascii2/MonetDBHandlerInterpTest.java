/**
 * $Id: MonetDBHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: MonetDBHandlerInterpTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.app.ImportAsciiGraph;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Tests the highest level of smart ascii processing using a real Monet
 * database: Proximity database event handling.
 * <p/>
 * todo merge verifyXX() methods with those of kdl.prox.app.ImportProx2XMLAppTest
 */
public class MonetDBHandlerInterpTest extends TestCase {

    private static final Logger log = Logger.getLogger(MonetDBHandlerInterpTest.class);
    private static final String NAME_OBJ_STR_ATTR = "nn";
    private static final String OBJ_INT_ATTR = "i";
    private static final String OBJ_STR_ATTR = "s";

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // setup database. see testMonetDBHandler() docs for overview of
        // database before and after the import
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.insertObject(0);
        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.deleteAllAttributes();
        objectAttrs.defineAttribute(NAME_OBJ_STR_ATTR, "str");
        objectAttrs.defineAttribute(OBJ_STR_ATTR, "str");
        objectAttrs.defineAttribute(OBJ_INT_ATTR, "int");

        NST attrDataNST = objectAttrs.getAttrDataNST(NAME_OBJ_STR_ATTR);
        attrDataNST.insertRow(new String[]{"0", "a"});

        attrDataNST = objectAttrs.getAttrDataNST(OBJ_INT_ATTR);
        attrDataNST.insertRow(new String[]{"0", "0"});

        Attributes linkAttrs = DB.getLinkAttrs();
        linkAttrs.defineAttribute(OBJ_STR_ATTR, "str");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    /**
     * Tests importing test file into real Monet database. Uses following data:
     * <p/>
     * o initial state:
     * <p/>
     * <pre>
     * objs:  {0}
     * links: {}
     *   attrs:
     *     obj:
     *     nn: {0:'a'}
     *     s: {}
     *     n: {0:0}
     *   link:
     *     s: {}
     * <pre/>
     * <p/>
     * <p/>
     * o final (expected) state:
     * <p/>
     * <pre>
     * objs:  {0, 1, 2}
     * links: {0: [0, 2], 1: [1, 2]}
     * attrs:
     * obj:
     * nn: {0:'a', 1:'b', 2:'c'}
     * s: {0:'a', 1:'b', }
     * n: {0:0, 1:1, 2:2}
     * link:
     * s: {0:'a->c', 1:'b->c'}
     * <pre/>
     */
    public void testMonetDBHandler() {
        BufferedReader bufferedReader = null;
        try {
            URL resURL = getClass().getResource("test-sa.txt");
            Object content = resURL.getContent();
            InputStream inputStream = (InputStream) content;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            ImportAsciiGraph.doImport(bufferedReader, NAME_OBJ_STR_ATTR);
            verifyObjects(new String[]{"0", "1", "2"});
            verifyLinks(new String[][]{
                    {"0", "0", "2"},
                    {"1", "1", "2"}});
            verifyAttribute("nn", true, new String[][]{
                    {"0", "a"},
                    {"1", "b"},
                    {"2", "c"}});
            verifyAttribute(OBJ_STR_ATTR, true, new String[][]{
                    {"0", "a"},
                    {"1", "b"}});
            verifyAttribute(OBJ_INT_ATTR, true, new String[][]{
                    {"0", "0"},
                    {"1", "1"},
                    {"2", "2"}});
            verifyAttribute(OBJ_STR_ATTR, false, new String[][]{
                    {"0", "a->c"},
                    {"1", "b->c"}});
        } catch (Exception exc) {
            log.error("error testing", exc);
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


    private void verifyAttribute(String attrName, boolean isObject,
                                 String[][] itemIDValues) {
        // fill expectedIDValues
        List expectedIDValues = new ArrayList();    // catenation of itemID + value
        for (int idValIdx = 0; idValIdx < itemIDValues.length; idValIdx++) {
            String[] itemIDValue = itemIDValues[idValIdx];
            if (itemIDValue.length != 0) {
                String itemID = itemIDValue[0];
                String value = itemIDValue[1];
                expectedIDValues.add(itemID + value);
            }
        }

        // fill actualIDValues
        List actualIDValues = new ArrayList();      // catenation of itemID + value
        Attributes itemAttrs = (isObject ?
                DB.getObjectAttrs() : DB.getLinkAttrs());
        NST attrDataNST = itemAttrs.getAttrDataNST(attrName);
        ResultSet resultSet = attrDataNST.selectRows("id, value");
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            String value = resultSet.getString(2);
            actualIDValues.add(itemID + value);
        }

        // compare
        assertEquals(expectedIDValues.size(), actualIDValues.size());
        assertTrue(expectedIDValues.containsAll(actualIDValues));
    }


    /**
     * Verifies the database contains exactly the passed link data.
     *
     * @param linkSelfO1O2IDs an array of String[], each of which contains a
     *                        linkID, o1ID, and o2ID
     * @
     */
    private void verifyLinks(String[][] linkSelfO1O2IDs) {
        // fill expectedLinkIDs
        List expectedLinkIDs = new ArrayList();     // catenation of linkID + o1ID + o2ID
        for (int linkIdx = 0; linkIdx < linkSelfO1O2IDs.length; linkIdx++) {
            String[] linkIDs = linkSelfO1O2IDs[linkIdx];
            String linkID = linkIDs[0];
            String o1ID = linkIDs[1];
            String o2ID = linkIDs[2];
            expectedLinkIDs.add(linkID + o1ID + o2ID);
        }

        // fill actualLinkIDs
        List actualLinkIDs = new ArrayList();   // catenation of linkID + o1ID + o2ID
        NST linkNST = DB.getLinkNST();
        ResultSet resultSet = linkNST.selectRows();   // "o1_id", "o2_id"
        while (resultSet.next()) {
            int linkID = resultSet.getOID(1);
            int o1ID = resultSet.getOID(2);
            int o2ID = resultSet.getOID(3);
            actualLinkIDs.add(linkID + "" + o1ID + "" + o2ID);
        }

        // compare
        assertEquals(expectedLinkIDs.size(), actualLinkIDs.size());
        assertTrue(expectedLinkIDs.containsAll(actualLinkIDs));
    }


    /**
     * Verifies the database contains exactly the passed object data.
     *
     * @param objIDs an array of objectIDs
     * @throws kdl.prox.monet.MonetException
     */
    private void verifyObjects(String[] objIDs) {
        NST objectNST = DB.getObjectNST();
        ResultSet resultSet = objectNST.selectRows();  // "ID"
        List expectedIDStrs = Arrays.asList(objIDs);

        // fill actualIDStrs
        List actualIDInts = resultSet.toOIDList(1);
        List actualIDStrs = new ArrayList();
        for (Iterator actualIDIntIter = actualIDInts.iterator(); actualIDIntIter.hasNext();) {
            Integer actualIDInt = (Integer) actualIDIntIter.next();
            actualIDStrs.add(actualIDInt.toString());
        }

        // compare
        assertEquals(expectedIDStrs.size(), actualIDStrs.size());
        assertTrue(expectedIDStrs.containsAll(actualIDStrs));
    }

}
