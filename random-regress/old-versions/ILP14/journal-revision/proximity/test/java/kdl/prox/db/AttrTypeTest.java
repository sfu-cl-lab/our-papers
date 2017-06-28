/**
 * $Id: AttrTypeTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttrTypeTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.DataTypeEnum;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class AttrTypeTest extends TestCase {

    // no IVs


    protected void setUp() {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }


    protected void tearDown() {
    }


    public void testEmpty() throws Exception {
        validate(" ", false);   // no: no 1st column
    }


    public void testOne() throws Exception {
        validate("value:int", true);    // ok
    }


    public void testOneEmptySecond() throws Exception {
        validate("year:int, ", false);  // no: no 2nd column
    }


    public void testOneNoNameOkType() throws Exception {
        validate("int", true);  // ok
    }


    public void testOneNoNameBadType() throws Exception {
        validate("xx", false);  // no: invalid type
    }


    public void testTwo() throws Exception {
        validate("year:int, gross:int", true);    // ok
    }


    public void testTwoWhitespace() throws Exception {
        validate(" year: int, gross :int", true);   // ok
    }


    public void testTwoNoNames() throws Exception {
        validate("int, int", false);    // no: only first column can be unnamed
    }

    public void testTwoSameNames() throws Exception {
        validate("a:int,a:int", false);  // no: can't both have the same name
    }

    public void testTwoSameNamesDiffCase() throws Exception {
        validate("a:int,A:int", false);  // no: can't both have the same name
    }

    public void testTwoSecondNamed() throws Exception {
        validate("int, val2: int", true);   // ok
    }


    public void testOneNoNameIsValue() throws Exception {
        // One with null, the other with the empty string
        AttrType attrType = new AttrType("", DataTypeEnum.enumForType("int"));
        if (!attrType.getName().equals("value")) {
            fail("The name of the attributeType should be 'value', not " + attrType.getName());
        }
        attrType = new AttrType(null, DataTypeEnum.enumForType("int"));
        if (!attrType.getName().equals("value")) {
            fail("The name of the attributeType should be 'value', not " + attrType.getName());
        }
    }

    /**
     * Helper method that tries to convert typeDef via
     * Attributes.attrTypeListForTypeDef(), honoring isShouldBeValid, which specifies
     * whether typeDef is a valid definition or not.
     *
     * @param typeDef
     * @param isShouldBeValid
     */
    private void validate(String typeDef, boolean isShouldBeValid) {
        try {
            AttrType.attrTypeListForTypeDef(typeDef);
            if (!isShouldBeValid) {
                fail("typeDef should not be valid: '" + typeDef);
            }
        } catch (IllegalArgumentException iaExc) {
            if (isShouldBeValid) {
                fail("typeDef should be valid: '" + typeDef + "': " + iaExc);
            }
        }
    }


}
