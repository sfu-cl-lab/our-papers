/**
 * $Id: AttributesTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttributesTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTTypeEnum;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Tests Attributes class.
 */
public class AttributesTest extends TestCase {

    Logger log = Logger.getLogger(AttributesTest.class);

    NST attrNST;
    String attrNSTName = "test_attr";
    Attributes attrs;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        attrNST = new NST(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES);
        attrNST.save("test_attr");
        attrs = new Attributes(attrNSTName);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        attrNST.delete();
        TestUtil.closeTestConnection();
    }

    public void testBadNames() {
        String dataType = "str";
        String attrName = "xx";
        for (int charIdx = 0; charIdx < Assert.INVALID_NAME_CHARS.length(); charIdx++)
        {
            char badChar = Assert.INVALID_NAME_CHARS.charAt(charIdx);
            try {
                attrs.defineAttribute("xx" + badChar + "xx", dataType);
                fail("attribute names should not include " + badChar);
            } catch (IllegalArgumentException e) {
                // ignore
            }
            try {
                attrs.defineAttribute(attrName, "value" + badChar + ":str");
                attrs.deleteAttribute(attrName);
                fail("attribute column names should not include " + badChar);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }

    public void testCopyAttribute() {
        // create attribute to copy
        String dataType = "year:int,gross:flt";
        String attrName = "attr1";
        attrs.defineAttribute(attrName, dataType);
        NST attrNST = attrs.getAttrDataNST(attrName);
        String[] row1 = new String[]{"1", "1995", "1000.01"};
        String[] row2 = new String[]{"2", "1996", "100.02"};
        attrNST.insertRow(row1);
        attrNST.insertRow(row2);
        attrNST.release();

        // copy into new attribute (and delete original to test 'deep' copy)
        String attrName2 = "attr2";
        attrs.copyAttribute(attrName, attrName2);
        attrs.deleteAttribute(attrName);

        attrNST = attrs.getAttrDataNST(attrName2);
        assertEquals(2, attrNST.getRowCount());
        ResultSet rs = attrNST.selectRows();
        Set results = new HashSet();
        while (rs.next()) {
            results.add(rs.getOID(1) + " " + rs.getInt(2) + " " + rs.getFloat(3));
        }
        assertTrue(results.contains("1 1995 1000.01"));
        assertTrue(results.contains("2 1996 100.02"));
        attrs.deleteAttribute(attrName2);
        attrNST.release();
    }


    public void testCopyAttrNewTypeMultiCol() {
        // first verify it fails on multi-column attrs
        String dataType = "year:int,gross:flt";
        String attrName = "attr1";
        String attrName2 = "attr2";
        attrs.defineAttribute(attrName, dataType);
        try {
            attrs.copyAttribute(attrName, attrName2, DataTypeEnum.STR);
            fail("should have failed for multi-column attribute");
        } catch (IllegalArgumentException iaExc) {
            // expected
        }
    }


    /**
     * Tests copying a single-column int attribute to a str one.
     *
     * @
     */
    public void testCopyAttrNewTypeOneColIntToStr() {
        // create str attribute to copy
        String attrName = "attr1";
        String dataType = "int";
        attrs.defineAttribute(attrName, dataType);
        NST attrNST = attrs.getAttrDataNST(attrName);
        attrNST.insertRow(new String[]{"1", "11"});
        attrNST.insertRow(new String[]{"2", "22"});
        attrNST.release();

        // copy into new str attribute, deleting original to test 'deep' copy
        String attrName2 = "attr2";
        attrs.copyAttribute(attrName, attrName2, DataTypeEnum.STR);
        attrs.deleteAttribute(attrName);

        // check results
        attrNST = attrs.getAttrDataNST(attrName2);
        assertEquals(2, attrNST.getRowCount());
        ResultSet rs = attrNST.selectRows();
        Set results = new HashSet();
        while (rs.next()) {
            results.add(rs.getOID(1) + " " + rs.getString(2));
        }
        assertTrue(results.contains("1 11"));
        assertTrue(results.contains("2 22"));
        attrs.deleteAttribute(attrName2);
        attrNST.release();
    }


    public void testDefineAttributeWithExpression() {
        DB.getObjectNST().deleteRows();
        DB.getObjectNST().insertRow("1").insertRow("2").insertRow("3");
        attrs.defineAttribute("base", "str");
        attrs.getAttrDataNST("base").insertRow("1, 'a'").insertRow("2, 'b'");
        String attrName = "test-1";
        attrs.defineAttributeWithExpression(attrName, "5 + 3");
        NST attrNST = attrs.getAttrDataNST(attrName);
        assertEquals(2, attrNST.getRowCount());
        assertEquals("id", attrNST.getNSTColumn(0).getName());
        assertEquals("value", attrNST.getNSTColumn(1).getName());
        List list = attrNST.selectRows("value").toStringList(1);
        assertTrue(list.contains("8"));
        attrs.deleteAttribute(attrName);
    }


    public void testDefineAttributeWithData() {
        // create attribute data NST
        NST tempNST = new NST("blah, x", "oid, int");
        tempNST.insertRow(new String[]{"1", "10"});
        tempNST.insertRow(new String[]{"2", "20"});
        String dataType = "int";
        String attrName = "attr1";
        attrs.defineAttributeWithData(attrName, dataType, tempNST);
        tempNST.release();

        NST attrNST = attrs.getAttrDataNST(attrName);
        assertEquals(2, attrNST.getRowCount());
        assertEquals("id", attrNST.getNSTColumn(0).getName());
        assertEquals("value", attrNST.getNSTColumn(1).getName());
        attrs.deleteAttribute(attrName);
    }


    public void testGetAttrDataNSTFailsIfNotDefined() throws Exception {
        try {
            attrs.getAttrDataNST("attr1");
            fail("Returned data NST for inexistent attribute --invalid");
        } catch (Exception e) {
        }
    }


    public void testGetAttrDataNSTWorksIfOK() throws Exception {
        attrs.defineAttribute("attr1", "int");
        NST attrDataNST = attrs.getAttrDataNST("attr1");
        attrDataNST.release();
    }


    public void testGetTypeDef() throws Exception {
        String dataType = "year:int,gross:flt";
        attrs.defineAttribute("attr1", dataType);
        if (!attrs.getAttrTypeDef("attr1").equalsIgnoreCase(dataType)) {
            fail("Invalid data type");
        }
    }


    public void testDefineFailIfExists() throws Exception {
        attrs.defineAttribute("attr1", "int");
        try {
            attrs.defineAttribute("attr1", "str");
            fail("Defined same attribute twice");
        } catch (Exception e) {
        }
    }


    public void testDefineFailIfWrongType() throws Exception {
        try {
            attrs.defineAttribute("attr1", "xx");
            fail("Attribute of invalid type 'xx' should not have been define");
        } catch (Exception e) {
        }
    }


    public void testDefineSuccessIfOK() throws Exception {
        attrs.defineAttribute("attr2", "int");
        if (!attrs.isAttributeDefined("attr2")) {
            fail("Defined attribute attr1 not found in Attributes");
        }
    }


    public void testDefineSuccessIfOKUpperCase() throws Exception {
        attrs.defineAttribute("UPPER_ATTR", "int");
        if (!attrs.isAttributeDefined("UPPER_ATTR")) {
            fail("Defined attribute UPPER_ATTR not found in Attributes");
        }
    }


    public void testDeleteFailsIfNoAttribute() throws Exception {
        try {
            attrs.deleteAttribute("attr1");
            fail("Deleted inexistent attribute --invalid");
        } catch (Exception e) {
        }
    }


    public void testDeleteWorksIfOK() throws Exception {
        attrs.defineAttribute("attr1", "int");
        attrs.deleteAttribute("attr1");
        if (attrs.isAttributeDefined("attr1")) {
            fail("Deleted attribute is still defined");
        }
    }


    public void testDeleteLike() throws Exception {
        attrs.defineAttribute("attr1", "int");
        attrs.defineAttribute("attr2", "int");
        attrs.defineAttribute("HattR2", "int");
        assertEquals(2, attrs.deleteAttributesWithPrefix("attr"));
        if (attrs.isAttributeDefined("attr1")) {
            fail("Deleted attribute is still defined");
        }
        if (attrs.isAttributeDefined("attr2")) {
            fail("Deleted attribute is still defined");
        }
        attrs.deleteAttribute("HattR2");
    }


    public void testGetAttributesOfType() {
        attrs.defineAttribute("int_attr", "int");
        attrs.defineAttribute("val_int_attr", "value:int");
        attrs.defineAttribute("str_attr", "str");
        attrs.defineAttribute("val_str_attr", "value:str");
        attrs.defineAttribute("str_int_attr", "a:str, b:int");

        List attrNames = attrs.getAttributesOfType(DataTypeEnum.INT);
        assertEquals(2, attrNames.size());
        assertTrue(attrNames.contains("int_attr"));
        assertTrue(attrNames.contains("val_int_attr"));

        attrNames = attrs.getAttributesOfType(DataTypeEnum.STR);
        assertEquals(2, attrNames.size());
        assertTrue(attrNames.contains("str_attr"));
        assertTrue(attrNames.contains("val_str_attr"));

        attrNames = attrs.getAttributesOfType(DataTypeEnum.BIT);
        assertEquals(0, attrNames.size());
    }


    public void testGetLike() throws Exception {
        attrs.defineAttribute("attr1", "int");
        attrs.defineAttribute("ATTR2", "int");
        attrs.defineAttribute("Xattr3", "int");
        attrs.defineAttribute("att_r4", "int");
        List attrNames = attrs.getAttributesWithPrefix("attr");
        assertEquals(2, attrNames.size());

        assertTrue(attrNames.contains("attr1"));
        assertTrue(attrNames.contains("attr2"));

        assertEquals(0, attrs.getAttributesWithPrefix("blah").size());
    }

}
