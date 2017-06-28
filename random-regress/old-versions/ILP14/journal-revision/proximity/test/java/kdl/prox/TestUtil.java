/**
 * $Id: TestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 */
package kdl.prox;

import junit.framework.Assert;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Helper class that defines static methods useful to testing.
 */
public class TestUtil {

    private static Logger log = Logger.getLogger(TestUtil.class.getName());

    /**
     * true if I've called initTestDB(). Used by initDBOncePerAllTests()
     */
    private static boolean isCalledInitDB = false;

    /**
     * Test DB connection vars.
     */
    public static final String HOST = "localhost";
    public static final int OLDPORT = 60000;
    public static final int NEWPORT = 40000;
    public static int working_port = -1;


    private TestUtil() {
    }


    public static void closeTestConnection() {
        DB.close();
    }


    /**
     * Helper that returns a List of period-delimited ('.') String catenations
     * of all columns for all rows in nst.
     *
     * @param nst
     * @return
     * @
     */
    public static List<String> getDelimStringListForNST(NST nst) {
        int numCols = nst.getNSTColumnNames().size();
        List<String> attrValList = new ArrayList<String>();
        ResultSet resultSet = nst.selectRows();
        while (resultSet.next()) {
            StringBuffer catSB = new StringBuffer();
            for (int colNum = 1; colNum <= numCols; colNum++) {
                String value = resultSet.getString(colNum);
                catSB.append(value);
                catSB.append('.');
            }
            catSB.setLength(catSB.length() - 1);    // get rid of final '.'
            attrValList.add(catSB.toString());
        }
        return attrValList;
    }


    /**
     * Finds whether there is a Monet listening on port 40000, or on port 60000
     *
     * @return
     */
    public static int getWorkingPort() {
        if (working_port == -1) {
            try {
                DB.open(HOST + ":" + NEWPORT, true); //no ckeck
                working_port = NEWPORT;
            } catch (MonetException exc) {
                DB.open(HOST + ":" + OLDPORT, true); //no ckeck
                working_port = OLDPORT;
            } finally {
                DB.close();
            }
        }
        return working_port;
    }


    public static String hostAndPort() {
        return HOST + ":" + getWorkingPort();
    }

    /**
     * Calls initTestDB() if it hasn't been called already. Returns true if
     * initDB() was called.
     * <p/>
     * NB: setUp() methods should call initDBOncePerAllTests(), even if they
     * don't need the db to run the test. This is so that log4j init happens
     * only once. Otherwise, multiple appenders get added, which results in ugly
     * duplicate log messages. besides, initDBOncePerAllTests() isn't *that*
     * slow :-)
     *
     * @return whether or not initDB() was called
     */
    public static boolean initDBOncePerAllTests() {
        if (!isCalledInitDB) {
            initTestDB();
//            isCalledInitDB = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Initializes log4j, *clears* the localhost database on the test port, then
     * disconnets. Caution: Deletes the contents of the database!
     */
    private static void initTestDB() {
        Util.initProxApp();
        log.warn("* initTestDB(): clearing and initializing database");
        try {
            openTestConnection();
            DB.clearDB();
            DB.initEmptyDB();
            closeTestConnection();
        } catch (MonetException monExc) {
            Assert.fail("couldn't connect to database: " + monExc);
        }
    }

    private static List<Integer> intArrayToIntegerList(int[] ints) {
        List<Integer> arrayList = new ArrayList<Integer>();
        for (int i : ints) {
            arrayList.add(i);
        }
        return arrayList;
    }


    /**
     * Opens a connection for the (test) localhost database on port 60000.
     */
    public static void openTestConnection() {
        closeTestConnection();
        DB.open(hostAndPort(), true); //no ckeck
    }

    /**
     * Helper that compares Double values in the two maps, given a fixed
     * delta.
     *
     * @param expectedMap
     * @param actualMap
     */
    public static void verifyDoubleMap(Map<Integer, Double> expectedMap, Map<Integer, Double> actualMap) {
        Assert.assertEquals(expectedMap.size(), actualMap.size());
        for (Integer keyInt : expectedMap.keySet()) {
            Double expValDouble = expectedMap.get(keyInt);
            Double actualValDouble = actualMap.get(keyInt);
            Assert.assertNotNull(actualValDouble);
            Assert.assertEquals(expValDouble, actualValDouble, 0.0001);
        }
    }

    /**
     * Compares the content of the two Elements. Caveats:
     * <ul>
     * <li>only compares element names, and children (recursively)
     * <li>does not compare attributes
     * <li>does no sorting - compares children in the order passed
     * </ul>
     *
     * @param expectedEle
     * @param actualEle
     */
    public static void verifyElements(Element expectedEle, Element actualEle) {
        // test text
        Assert.assertEquals(expectedEle.getText().trim(), actualEle.getText().trim());

/*
        // test attributes. NB: non-deterministic order
        List expAttrs = expectedEle.getAttributes();
        List actAttrs = actualEle.getAttributes();
        Assert.assertEquals(expAttrs.size(), actAttrs.size());
        for (int attrIdx = 0; attrIdx < expAttrs.size(); attrIdx++) {
            Attribute expAttr = (Attribute) expAttrs.get(attrIdx);
            Attribute actAttr = (Attribute) actAttrs.get(attrIdx);
            Assert.assertEquals(expAttr.getName(), actAttr.getName());
            Assert.assertEquals(expAttr.getValue(), actAttr.getValue());
        }
*/

        // test children. NB: non-deterministic order
        List expChildren = expectedEle.getChildren();
        List actChildren = actualEle.getChildren();
        Assert.assertEquals(expChildren.size(), actChildren.size());
        for (int childIdx = 0; childIdx < expChildren.size(); childIdx++) {
            Element expChild = (Element) expChildren.get(childIdx);
            Element actChild = (Element) actChildren.get(childIdx);
            TestUtil.verifyElements(expChild, actChild);
        }
    }

    /**
     * Object[] overload, with an NST. Helper that asserts that NST contains only the passed
     * expectedEles.
     *
     * @param expectedEles
     * @param nst
     * @see #getDelimStringListForNST
     */
    public static void verifyCollections(Object[] expectedEles, NST nst) {
        List<String> valList = TestUtil.getDelimStringListForNST(nst);
        verifyCollections(expectedEles, new HashSet<String>(valList));
    }

    /**
     * Object[] overload. Helper that asserts actualEles contains only the passed
     * expectedEles. Often used with getDelimStringListForNST().
     *
     * @param expectedEles
     * @param actualEles
     * @see #getDelimStringListForNST
     */
    public static void verifyCollections(Object[] expectedEles, Collection actualEles) {
        verifyCollections(Arrays.asList(expectedEles), actualEles);
    }

    public static void verifyCollections(int[] expectedInts, int[] actualInts) {
        List expectedList = intArrayToIntegerList(expectedInts);
        List actualList = intArrayToIntegerList(actualInts);
        verifyCollections(expectedList, actualList);
    }

    /**
     * General collection overload.
     *
     * @param expectedEles
     * @param actualSet
     */
    public static void verifyCollections(Collection expectedEles, Collection actualSet) {
        Assert.assertEquals(expectedEles.size(), actualSet.size());
        for (Object element : expectedEles) {
            Assert.assertTrue("expected " + element + " not found in actual set: " + actualSet, actualSet.contains(element));
        }
    }

    /**
     * Checks a list -=- order counts
     *
     * @param correct
     * @param suspect
     */
    public static void verifyList(List correct, List suspect) {
        log.debug("suspect:" + suspect);
        log.debug("correct:" + correct);
        Assert.assertEquals(correct.size(), suspect.size());
        for (int i = 0; i < correct.size(); i++) {
            Assert.assertEquals(correct.get(i), suspect.get(i));
        }
    }

    /**
     * Generic Map verification
     *
     * @param expectedMap
     * @param actualMap
     */

    public static void verifyMap(Map expectedMap, Map actualMap) {
        Assert.assertEquals(expectedMap.size(), actualMap.size());
        for (Object o : expectedMap.keySet()) {
            Object expVal = expectedMap.get(o);
            Object actualVal = actualMap.get(o);
            Assert.assertNotNull(actualVal);
            Assert.assertEquals(expVal, actualVal);
        }
    }
}
