/**
 * $Id: AttributeValuesTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttributeValuesTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;


/**
 * Tests Attributes.getAttrValsForOID() and its helper classes.
 */
public class AttributeValuesTest extends TestCase {

    private static Logger log = Logger.getLogger(AttributeValuesTest.class);

    private Attributes objectAttrs;

    // NB: lowercasing of names required for some tests (see below)
    private static String OBJ_ATTR_INT_TYPE_NAME = "obj_attr_int_type_name";
    private static String OBJ_ATTR_MULTI_TYPE_NAME = "obj_attr_multi_type_name";
    private static String OBJ_ATTR_STR_TYPE_NAME = "obj_attr_str_type_name";


    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();

        // create test attributes
        objectAttrs = DB.getObjectAttrs();
        objectAttrs.deleteAllAttributes();
        objectAttrs.defineAttribute(OBJ_ATTR_INT_TYPE_NAME, "int");
        objectAttrs.defineAttribute(OBJ_ATTR_MULTI_TYPE_NAME, "c1:int, c2:str");
        objectAttrs.defineAttribute(OBJ_ATTR_STR_TYPE_NAME, "str");

        /**
         // insert this data:
         obj-id  int-val int-str-val str-val
         0       0
         0               0, "a"
         0               0, "b"
         0                           "a"
         0                           "b"
         1       1
         1                           "c"
         */
        NST attrDataNST;
        attrDataNST = objectAttrs.getAttrDataNST(OBJ_ATTR_INT_TYPE_NAME);
        attrDataNST.insertRow(new String[]{"0", "0"});
        attrDataNST.insertRow(new String[]{"1", "1"});
        attrDataNST.release();

        attrDataNST = objectAttrs.getAttrDataNST(OBJ_ATTR_MULTI_TYPE_NAME);
        attrDataNST.insertRow(new String[]{"0", "0", "a"});
        attrDataNST.insertRow(new String[]{"0", "0", "b"});
        attrDataNST.release();

        attrDataNST = objectAttrs.getAttrDataNST(OBJ_ATTR_STR_TYPE_NAME);
        attrDataNST.insertRow(new String[]{"0", "a"});
        attrDataNST.insertRow(new String[]{"0", "b"});
        attrDataNST.insertRow(new String[]{"1", "c"});
        attrDataNST.release();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        objectAttrs.deleteAllAttributes();
        TestUtil.closeTestConnection();
    }


    public void testAllAttributes()  {
        AttributeValues attrVals = objectAttrs.getAttrValsForOID(0);
        assertEquals(5, attrVals.getRowCount());
        assertEquals(2, attrVals.getColumnCount());
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_INT_TYPE_NAME,
                new String[]{"0"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_MULTI_TYPE_NAME,
                new String[]{"0", "a"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_MULTI_TYPE_NAME,
                new String[]{"0", "b"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_STR_TYPE_NAME,
                new String[]{"a"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_STR_TYPE_NAME,
                new String[]{"b"})));

        attrVals = objectAttrs.getAttrValsForOID(1);
        assertEquals(2, attrVals.getRowCount());
        assertEquals(1, attrVals.getColumnCount());
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_INT_TYPE_NAME,
                new String[]{"1"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_STR_TYPE_NAME,
                new String[]{"c"})));
    }


    public void testGetAttrValsForName()  {
        AttributeValues attrVals = objectAttrs.getAttrValsForOID(0);
        List attrVals1 = attrVals.getAttrValsForName(OBJ_ATTR_INT_TYPE_NAME);
        List attrVals2 = attrVals.getAttrValsForName(OBJ_ATTR_MULTI_TYPE_NAME);
        assertEquals(1, attrVals1.size());
        assertEquals(2, attrVals2.size());
    }


    public void testGetUniqueAttrNames()  {
        AttributeValues attrVals = objectAttrs.getAttrValsForOID(0);
        Set uniqueAttrNames = attrVals.getUniqueAttrNames();
        log.debug(attrVals + " -> " + uniqueAttrNames);
        assertEquals(3, uniqueAttrNames.size());
        // NB: following tests depend on attribute names being lower case:
        assertTrue(uniqueAttrNames.contains(OBJ_ATTR_INT_TYPE_NAME));
        assertTrue(uniqueAttrNames.contains(OBJ_ATTR_MULTI_TYPE_NAME));
        assertTrue(uniqueAttrNames.contains(OBJ_ATTR_STR_TYPE_NAME));
    }


    public void testSomeAttributes()  {
        List attrNames = Arrays.asList(new String[]{OBJ_ATTR_MULTI_TYPE_NAME});
        AttributeValues attrVals = objectAttrs.getAttrValsForOID(0, attrNames);
        assertEquals(2, attrVals.getRowCount());
        assertEquals(2, attrVals.getColumnCount());
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_MULTI_TYPE_NAME,
                new String[]{"0", "a"})));
        assertTrue(attrVals.contains(new AttributeValue(OBJ_ATTR_MULTI_TYPE_NAME,
                new String[]{"0", "b"})));

        attrVals = objectAttrs.getAttrValsForOID(1, attrNames);
        assertEquals(0, attrVals.getRowCount());
        assertEquals(0, attrVals.getColumnCount());
    }


}
