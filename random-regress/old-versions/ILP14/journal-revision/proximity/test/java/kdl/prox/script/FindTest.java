/**
 * $Id: FindTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;


public class FindTest extends TestCase {

    private static Logger log = Logger.getLogger(FindTest.class);
    private static final String STR_OBJ_ATTR1 = "obj_attr_1";
    private static final String STR_OBJ_ATTR2 = "obj_attr_2";
    private static final String INT_OBJ_ATTR = "int_attr";

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        Attributes attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();

        attrs.defineAttribute(STR_OBJ_ATTR1, "str");
        NST attrNST = attrs.getAttrDataNST(STR_OBJ_ATTR1);
        attrNST.insertRow(new String[]{"10", "Star Wars"});
        attrNST.insertRow(new String[]{"11", "Star Wars"});
        attrNST.insertRow(new String[]{"12", "Nine and a half weeks"});

        attrs.defineAttribute(STR_OBJ_ATTR2, "str");
        attrNST = attrs.getAttrDataNST(STR_OBJ_ATTR2);
        attrNST.insertRow(new String[]{"10", "Time to Start something"});
        attrNST.insertRow(new String[]{"11", "No time like the present"});

        attrs.defineAttribute(INT_OBJ_ATTR, "int");
        attrNST = attrs.getAttrDataNST(INT_OBJ_ATTR);
        attrNST.insertRow(new String[]{"10", "0"});
        attrNST.insertRow(new String[]{"11", "1"});
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testFind() {
        Map hits = Find.findObjects("Star Wars");
        // result: {STR_OBJ_ATTR1: {10, 11}}
        assertEquals(1, hits.keySet().size());
        List ids = (List) hits.get(STR_OBJ_ATTR1);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Integer(10)));
        assertTrue(ids.contains(new Integer(11)));

        hits = Find.findObjects("%Star");
        // result: {}
        assertEquals(0, hits.keySet().size());

        hits = Find.findObjects("%Star%");
        // result: {STR_OBJ_ATTR1: {10, 11}, STR_OBJ_ATTR2: {10}}
        assertEquals(2, hits.keySet().size());
        ids = (List) hits.get(STR_OBJ_ATTR1);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Integer(10)));
        assertTrue(ids.contains(new Integer(11)));

        ids = (List) hits.get(STR_OBJ_ATTR2);
        assertNotNull(ids);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(new Integer(10)));

        hits = Find.findObjects("Star%");
        // result: {STR_OBJ_ATTR1: {10, 11}}
        assertEquals(1, hits.keySet().size());
        ids = (List) hits.get(STR_OBJ_ATTR1);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Integer(10)));
        assertTrue(ids.contains(new Integer(11)));
    }

    public void testFindWithArg() {
        Map hits = Find.findObjects("Star Wars", STR_OBJ_ATTR1);
        // result: {STR_OBJ_ATTR1: {10, 11}}
        assertEquals(1, hits.keySet().size());
        List ids = (List) hits.get(STR_OBJ_ATTR1);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Integer(10)));
        assertTrue(ids.contains(new Integer(11)));

        try {
            hits = Find.findObjects("%Star", "x");
            fail("find with an undefined attribute should have thrown an exception");
        } catch (Exception e) {
            // ignore
        }

        try {
            hits = Find.findObjects("%Star", INT_OBJ_ATTR);
            fail("find with a non-STR attribute should have thrown an exception");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testFindWithArgList() {
        ArrayList argList = new ArrayList();
        argList.add(STR_OBJ_ATTR1);
        argList.add(INT_OBJ_ATTR);
        Map hits = Find.findObjects("Star Wars", argList);
        // result: {STR_OBJ_ATTR1: {10, 11}}
        assertEquals(1, hits.keySet().size());
        List ids = (List) hits.get(STR_OBJ_ATTR1);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Integer(10)));
        assertTrue(ids.contains(new Integer(11)));

        argList.clear();
        argList.add("z");
        try {
            hits = Find.findObjects("%Star", argList);
            fail("find with an undefined attribute should have thrown an exception");
        } catch (Exception e) {
            // ignore
        }

        argList.clear();
        argList.add(INT_OBJ_ATTR);
        try {
            hits = Find.findObjects("%Star", argList);
            fail("find with a non-STR attribute should have thrown an exception");
        } catch (Exception e) {
            // ignore
        }
    }
}
