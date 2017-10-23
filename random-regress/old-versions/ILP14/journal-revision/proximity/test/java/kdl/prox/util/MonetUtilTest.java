/**
 * $Id: MonetUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: MonetUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 */
package kdl.prox.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.monet.Connection;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;


/**
 * Tests MonetUtil.
 * <p/>
 * todo other DataTypeEnum types: bat, bit, chr, date, dbl, flt, int, lng, oid, str, timestamp, null
 * delimit done?:  n    n    y    y     y    n    n    n    y    y      n        y
 * undelimit done?:  y    n    n    n     n    n    n    n    y    y      n        n
 * todo test methods that need the database?: getRowCount(), getTailType(), isBATExists(), isBATPersists(), isMonetTypeDefined(), setIsPersists()
 */
public class MonetUtilTest extends TestCase {

    private static Logger log = Logger.getLogger(MonetUtilTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testDelimitChrValue() {
        validateDelimitedValue(DataTypeEnum.CHR, "x", "'x'");
    }


    public void testDelimitDateValue() {
        validateDelimitedValue(DataTypeEnum.DATE, "x", "date(\"x\")");
    }


    public void testDelimitDblValue() {
        validateDelimitedValue(DataTypeEnum.DBL, "x", "dbl(x)");
    }


    public void testDelimitNullValue() {
        validateDelimitedValue(DataTypeEnum.BIT, null, "bit(nil)");
        validateDelimitedValue(DataTypeEnum.CHR, null, "chr(nil)");
        validateDelimitedValue(DataTypeEnum.DATE, null, "date(nil)");
        validateDelimitedValue(DataTypeEnum.DBL, null, "dbl(nil)");
        validateDelimitedValue(DataTypeEnum.FLT, null, "flt(nil)");
        validateDelimitedValue(DataTypeEnum.INT, null, "int(nil)");
        validateDelimitedValue(DataTypeEnum.LNG, null, "lng(nil)");
        validateDelimitedValue(DataTypeEnum.OID, null, "oid(nil)");
        validateDelimitedValue(DataTypeEnum.STR, null, "str(nil)");
        validateDelimitedValue(DataTypeEnum.TIMESTAMP, null, "timestamp(nil)");
    }


    public void testDelimitOIDValue() {
        validateDelimitedValue(DataTypeEnum.OID, "x", "oid(x)");
    }


    public void testDelimitStrValue() {
        validateDelimitedValue(DataTypeEnum.STR, "x", "\"" + "x" + "\"");
        validateDelimitedStrValue('\b', 'b');
        validateDelimitedStrValue('\t', 't');
        validateDelimitedStrValue('\n', 'n');
        validateDelimitedStrValue('\f', 'f');
        validateDelimitedStrValue('\r', 'r');
        validateDelimitedStrValue('\"', '"');
        validateDelimitedStrValue('\\', '\\');
    }

    public void testMinMax() {
        String inputBAT = MonetUtil.create("int, int");
        assertEquals(-1, MonetUtil.min(inputBAT));
        assertEquals(-1, MonetUtil.max(inputBAT));
        MonetUtil.insert(inputBAT, "1", "1");
        MonetUtil.insert(inputBAT, "1", "2");
        MonetUtil.insert(inputBAT, "1", "2");
        assertEquals(1, MonetUtil.min(inputBAT));
        assertEquals(2, MonetUtil.max(inputBAT));
    }

    public void testReadHistogram() {
        String inputBAT = MonetUtil.create("int, int");
        MonetUtil.insert(inputBAT, "1", "1");
        MonetUtil.insert(inputBAT, "1", "2");
        MonetUtil.insert(inputBAT, "1", "2");
        // 1:1, 2:2
        ResultSet resultSet = MonetUtil.readHistogram(inputBAT);
        assertEquals(2, resultSet.getRowCount());
        while (resultSet.next()) {
            int head = resultSet.getInt(0);
            int tail = resultSet.getInt(1);
            assertTrue((head == 1 && tail == 1) || (head == 2 && tail == 2));
        }

        // for the reverse: 1:3
        resultSet = MonetUtil.readHistogram(inputBAT + ".reverse()");
        assertEquals(1, resultSet.getRowCount());
        while (resultSet.next()) {
            int head = resultSet.getInt(0);
            int tail = resultSet.getInt(1);
            assertTrue((head == 1 && tail == 3));
        }
    }


    public void testRemoveIdenticalHeadTail() {
        String inputBAT = MonetUtil.create("int, int");
        MonetUtil.insert(inputBAT, "1", "1");
        MonetUtil.insert(inputBAT, "1", "2");

        String outputBAT = MonetUtil.removeIdenticalHeadTail(inputBAT);
        ResultSet resultSet = MonetUtil.read(outputBAT);
        assertEquals(1, resultSet.getRowCount());
        while (resultSet.next()) {
            assertEquals(1, resultSet.getInt(0));
            assertEquals(2, resultSet.getInt(1));
        }

        Connection.releaseSavedVar(inputBAT);
        Connection.releaseSavedVar(outputBAT);
    }

    public void testReplace() {
        String inputBAT = MonetUtil.create("int, int");
        MonetUtil.insert(inputBAT, "1", "1");
        MonetUtil.insert(inputBAT, "2", "2");
        MonetUtil.insert(inputBAT, "3", "2");
        MonetUtil.replace(inputBAT, "2", "3");
        ResultSet resultSet = MonetUtil.read(inputBAT);
        assertEquals(3, resultSet.getRowCount());
        while (resultSet.next()) {
            int head = resultSet.getInt(0);
            int tail = resultSet.getInt(1);
            assertTrue((head == 1 && tail == 1) || (head == 2 && tail == 3) || (head == 3 && tail == 3));
        }
    }


    public void testUndelimitOIDValue() {
        validateUndelimitedValue(DataTypeEnum.OID, "123@0", "123");
    }


    /**
     * Tests MonetUtil.undelimitValue().
     */
    public void testUndelimitStrValue() {
        validateUndelimitedValue(DataTypeEnum.STR, "\"" + "x" + "\"", "x");
        validateUnelimitedStrValue('\b', 'b');
        validateUnelimitedStrValue('\t', 't');
        validateUnelimitedStrValue('\n', 'n');
        validateUnelimitedStrValue('\f', 'f');
        validateUnelimitedStrValue('\r', 'r');
        validateUnelimitedStrValue('\"', '"');
        validateUnelimitedStrValue('\\', '\\');

    }


    private void validateDelimitedStrValue(char inputChar, char delimitChar) {
        String inputStr = "x" + inputChar + "y";
        String expectedOutputStr = '"' + "x" + '\\' + delimitChar + "y" + '"';
        validateDelimitedValue(DataTypeEnum.STR, inputStr, expectedOutputStr);
    }


    private void validateDelimitedValue(DataTypeEnum dataTypeEnum, String inputStr,
                                        String expOutStr) {
        String outputStr = MonetUtil.delimitValue(inputStr, dataTypeEnum);
        assertEquals(expOutStr, outputStr);
    }


    private void validateUnelimitedStrValue(char inputChar, char delimitChar) {
        String inputStr = '"' + "x" + '\\' + delimitChar + "y" + '"';
        String expectedOutputStr = "x" + inputChar + "y";
        validateUndelimitedValue(DataTypeEnum.STR, inputStr, expectedOutputStr);
    }


    private void validateUndelimitedValue(DataTypeEnum dataTypeEnum,
                                          String inputStr, String expOutStr) {
        String outputStr = MonetUtil.undelimitValue(inputStr, dataTypeEnum);
        assertEquals(expOutStr, outputStr);
    }


}

