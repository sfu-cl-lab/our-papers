/**
 * $Id: ObjDictTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ObjDictTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;


/**
 * Tests ObjDict.
 *
 * @see kdl.prox.impascii2.ObjDict
 */
public class ObjDictTest extends TestCase {

    private static String LINK_ATTR_NAME = "LINK_ATTR_NAME";
    private static String OBJ_ATTR_INT_TYPE_NAME = "OBJ_ATTR_INT_TYPE_NAME";
    private static String OBJ_ATTR_MULTI_TYPE_NAME = "OBJ_ATTR_MULTI_TYPE_NAME";
    private static String OBJ_ATTR_STR_TYPE_NAME = "OBJ_ATTR_STR_TYPE_NAME";
    private static String UNDEF_ATTR_NAME = "UNDEF_ATTR_NAME";


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();

        // define needed attributes, ignoring if they already exist
        Attributes linkAttrs = DB.getLinkAttrs();
        Attributes objectAttrs = DB.getObjectAttrs();

        linkAttrs.deleteAllAttributes();
        objectAttrs.deleteAllAttributes();

        linkAttrs.defineAttribute(LINK_ATTR_NAME, "str");
        objectAttrs.defineAttribute(OBJ_ATTR_INT_TYPE_NAME, "int");
        objectAttrs.defineAttribute(OBJ_ATTR_MULTI_TYPE_NAME, "c1:int, c2:str");
        objectAttrs.defineAttribute(OBJ_ATTR_STR_TYPE_NAME, "str");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    /**
     * Returns an ObjDict with the following data table:
     * <p/>
     * id | name
     * ---+-----
     * 0 | a
     * 1 | b
     * 1 | c
     * 2 | d
     * 3 | d
     * <p/>
     * i.e:
     * <p/>
     * 0     -> "a"        // legal
     * 1     -> "b", "c"   // legal
     * 2, 3  -> "d"        // illegal, but unenforced
     *
     * @throws kdl.prox.monet.MonetException
     */
    private ObjDict newTestObjDict()  {
        ObjDict objDict = new ObjDict(OBJ_ATTR_STR_TYPE_NAME);
        objDict.putIDForName("a", 0);
        objDict.putIDForName("b", 1);
        objDict.putIDForName("c", 1);
        objDict.putIDForName("d", 2);
        objDict.putIDForName("d", 3);
        return objDict;
    }


    public void testBadNonNullConstArgs() {
        try {
            new ObjDict(UNDEF_ATTR_NAME);
            fail("undefined attribute should fail");
        } catch (Exception e) {
        }

        try {
            new ObjDict(LINK_ATTR_NAME);
            fail("undefined object attribute should fail");
        } catch (Exception e) {
        }

        try {
            new ObjDict(OBJ_ATTR_MULTI_TYPE_NAME);
            fail("multi-valued object attribute should fail");
        } catch (Exception e) {
        }

        try {
            new ObjDict(OBJ_ATTR_INT_TYPE_NAME);
            fail("non-str-valued object attribute should fail");
        } catch (Exception e) {
        }
    }


    public void testDelete()  {
        ObjDict objDict = newTestObjDict();
        objDict.deleteIDForName("a");
        try {
            objDict.deleteIDForName("bad-name");
            fail("deleting non-existent name should fail");
        } catch (Exception e) {
        }
    }


    public void testGet()  {
        ObjDict objDict = newTestObjDict();

        Set badIDs = objDict.getIDsForName("bad-name");
        Set aIDs = objDict.getIDsForName("a");
        Set bIDs = objDict.getIDsForName("b");
        Set cIDs = objDict.getIDsForName("c");
        Set dIDs = objDict.getIDsForName("d");

        assertEquals(0, badIDs.size());
        assertEquals(1, aIDs.size());
        assertEquals(1, bIDs.size());
        assertEquals(1, cIDs.size());
        assertEquals(2, dIDs.size());

        assertTrue(aIDs.contains(new Integer(0)));
        assertTrue(bIDs.contains(new Integer(1)));
        assertTrue(cIDs.contains(new Integer(1)));
        assertTrue(dIDs.contains(new Integer(2)));
        assertTrue(dIDs.contains(new Integer(3)));
    }


    public void testNullConstArgs() {
        try {
            new ObjDict(null);
            fail("null arg(s) should fail");
        } catch (Exception e) {
        }

        try {
            new ObjDict("xx");
            fail("null arg(s) should fail");
        } catch (Exception e) {
        }

        try {
            new ObjDict(null);
            fail("null arg(s) should fail");
        } catch (Exception e) {
        }
    }


}
