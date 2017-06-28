/**
 * $Id: NSTTest.java 3710 2007-11-05 19:24:51Z afast $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.monet.Connection;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test NST class
 *
 * @see NST
 */
public class NSTTest extends TestCase {

    private static Logger log = Logger.getLogger(NSTTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        Connection.beginScope();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }

    /**
     * Helper routine that creates an NST and fills in some rows
     *
     * @return
     * @
     */
    private NST createAddressNST() {
        String[] columnNames = new String[]{"man", "address"};
        String[] columnTypes = new String[]{"str", "str"};
        NST nst = new NST(columnNames, columnTypes);
        nst.insertRow(new String[]{"john", "Amherst"});
        nst.insertRow(new String[]{"paul", "Pelham"});
        nst.insertRow(new String[]{"paul", "Northampton"});
        nst.insertRow(new String[]{"matthew", "Jerusalem"});
        return nst;
    }

    /**
     * Helper routine that creates an NST and fills in some rows
     *
     * @return
     * @
     */
    private NST createPhoneNST() {
        String[] columnNames = new String[]{"name", "phone"};
        String[] columnTypes = new String[]{"str", "int"};
        NST nst = new NST(columnNames, columnTypes);
        nst.insertRow(new String[]{"john", "324"});
        nst.insertRow(new String[]{"paul", "471"});
        nst.insertRow(new String[]{"john", "253"});
        return nst;
    }

    /**
     * Helper routine that creates another NST and fills in some rows
     * Used to test joins with more than column
     *
     * @return
     * @
     */
    private NST createOtherPhoneNST() {
        String[] columnNames = new String[]{"name", "phone"};
        String[] columnTypes = new String[]{"str", "int"};
        NST nst = new NST(columnNames, columnTypes);
        nst.insertRow(new String[]{"john", "253"});
        nst.insertRow(new String[]{"paul", "333"});
        nst.insertRow(new String[]{"john", "444"});
        return nst;
    }


    // test that ending a scope doesn't delete the BAT that was added to an NST as a column
    public void testAddColumnWithinScope() {
        NST nst = createAddressNST();
        Connection.beginScope();
        nst.addDistinctCountColumn("man", "address", "addr_cnt");
        nst.addDistinctCountColumn("man", "address", "addr_cnt2");
        assertEquals(4, nst.selectRows("addr_cnt").toStringList(1).size());
        Connection.endScope();
        assertEquals(4, nst.selectRows("addr_cnt").toStringList(1).size());
        nst.release();
    }

    public void testAddConstantColumn() {
        NST nst = createPhoneNST();
        nst.addConstantColumn("test", "int", "5");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("test").getType());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "test", "*");
        while (resultSet.next()) {
            int val = resultSet.getInt(1);
            assertEquals(5, val);
        }
        nst.release();
    }

    public void testAddConstantColumnAndInsert() {
        NST nst = createPhoneNST();
        nst.addConstantColumn("test", "int", "5");
        nst.insertRow(new String[]{"a", "32", "5"}); // insert the correct constant value. Just make sure it's the same

        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("test").getType());

        // Make sure that the numbers are right
        assertEquals(4, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("test");
        while (resultSet.next()) {
            int val = resultSet.getInt(1);
            assertEquals(5, val);
        }
        nst.release();
    }

    public void testAddConditionColumn() {
        NST phoneNST = createPhoneNST();
        phoneNST.addConditionColumn("name = 'john'", "isJohn");
        TestUtil.verifyCollections(new String[]{"324.true", "253.true", "471.false"}, phoneNST.project("phone, isJohn"));
        phoneNST.addConditionColumn("phone > 324", "isLarge");
        TestUtil.verifyCollections(new String[]{"324.false", "253.false", "471.true"}, phoneNST.project("phone, isLarge"));
    }

    public void testAddConstantArithmeticColumn() {
        NST nst = createPhoneNST();
        String offset = "1";
        nst.addArithmeticColumn("phone + " + offset, "int", "test");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("test").getType());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "phone,test", "*");
        while (resultSet.next()) {
            int phone = resultSet.getInt(1);
            int test = resultSet.getInt(2);
            assertEquals(phone + Integer.parseInt(offset), test);
        }

        nst.addArithmeticColumn("name + 'a'", "str", "test2");
        assertEquals(3, nst.getRowCount());
        resultSet = nst.selectRows("*", "name,test2", "*");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            String test = resultSet.getString(2);
            assertTrue(test.equals(name + "a"));
        }
        nst.release();

    }

    public void testAddArithmeticColumn() {
        NST nst = createPhoneNST();
        nst.addArithmeticColumn("phone * phone", "int", "test");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("test").getType());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "phone,test", "*");
        while (resultSet.next()) {
            int phone = resultSet.getInt(1);
            int test = resultSet.getInt(2);
            assertEquals(phone * phone, test);
        }
        nst.release();
    }

    public void testAddCopyColumnNoCast() {
        NST nst = createPhoneNST();     // "str", "int"
        nst.addCopyColumn("phone", "telefono");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("telefono"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("telefono").getType());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "telefono", "*");
        List telefonos = new ArrayList();
        while (resultSet.next()) {
            int val = resultSet.getInt(1);
            telefonos.add(new Integer(val));
        }
        assertEquals(3, telefonos.size());
        assertTrue(telefonos.contains(new Integer(253)));
        assertTrue(telefonos.contains(new Integer(471)));
        assertTrue(telefonos.contains(new Integer(324)));
        nst.release();
    }

    public void testAddCopyColumnWithCast() {
        NST nst = createPhoneNST();     // "str", "int"
        nst.addCopyColumn("phone", "telefono");
        nst.orderColumns("phone, telefono, name");
        nst.castColumn("telefono", "str");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue("phone".equals(nst.getNSTColumn(0).getName()));
        assertTrue("telefono".equals(nst.getNSTColumn(1).getName()));
        assertTrue("name".equals(nst.getNSTColumn(2).getName()));
        assertEquals("str", nst.getNSTColumn("telefono").getType().toString());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "telefono", "*");
        List telefonos = new ArrayList();
        while (resultSet.next()) {
            String val = resultSet.getString(1);
            telefonos.add(val);
        }
        assertEquals(3, telefonos.size());
        assertTrue(telefonos.contains("253"));
        assertTrue(telefonos.contains("471"));
        assertTrue(telefonos.contains("324"));
        nst.release();
    }

    public void testAddCountColumn() {
        NST nst = createPhoneNST();
        nst.addCountColumn("name", "name_cnt");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("name_cnt"));

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("name, name_cnt");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            int cnt = resultSet.getInt(2);
            assertTrue((name.equals("john") && cnt == 2) || (name.equals("paul") && cnt == 1));
        }
        nst.release();
    }

    public void testAddDistinctCountColumn() {
        NST nst = createAddressNST();
        nst.addDistinctCountColumn("man", "address", "addr_cnt");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("address"));
        assertTrue(columnNames.contains("addr_cnt"));

        // Make sure that the numbers are right
        assertEquals(4, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("man, addr_cnt");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            int cnt = resultSet.getInt(2);
            assertTrue((name.equals("john") && cnt == 1) ||
                    (name.equals("paul") && cnt == 2) ||
                    (name.equals("matthew") && cnt == 1));
        }
        nst.release();
    }

    public void testAddKeyColumn() {
        NST nst = createPhoneNST();
        nst.addKeyColumn("key");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("key"));

        // Make sure that the numbers are right
        ResultSet resultSet = nst.selectRows("*", "key", "*");
        while (resultSet.next()) {
            assertEquals(resultSet.getOID(0), resultSet.getOID(1));
        }
        nst.release();
    }

    public void testAddNilColumn() {
        NST nst = createPhoneNST();
        nst.addConstantColumn("nmbr", "str", null);
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("nmbr"));

        // Make sure that the strings are right
        List resultList = nst.selectRows("*", "nmbr", "*").toStringList(1);
        assertEquals(3, resultList.size());
        assertEquals(null, resultList.get(0));
        assertEquals(null, resultList.get(1));
        assertEquals(null, resultList.get(2));
        nst.release();
    }

    public void testAddNumberColumn() {
        NST nst = createPhoneNST();
        nst.addNumberColumn("nmbr");
        List numbers = nst.selectRows("nmbr").toOIDList(1);
        assertEquals(3, numbers.size());
        assertTrue(numbers.contains(new Integer(0)));
        assertTrue(numbers.contains(new Integer(1)));
        assertTrue(numbers.contains(new Integer(2)));
        nst.release();
    }

    public void testAddRandomColumn() {
        NST nst = createPhoneNST();
        nst.addRandomColumn("test");
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.FLT, nst.getNSTColumn("test").getType());

        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "phone,test", "*");
        while (resultSet.next()) {
            double test = resultSet.getDouble(2);
            log.debug(test + ""); //I don't know how to unit random numbers,  but the results look good to me
        }
        nst.release();
    }

    public void testAddRandomBinaryColumn() {
        NST nst = createPhoneNST();
        nst.addRandomBinaryColumn("test", 0.5);
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("test"));
        assertEquals(DataTypeEnum.INT, nst.getNSTColumn("test").getType());
        // Make sure that the numbers are right
        assertEquals(3, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "phone,test", "*");
        while (resultSet.next()) {
            double test = resultSet.getDouble(2);
            log.debug(test + ""); //I don't know how to unit random numbers,  but the results look good to me
        }
        nst.release();
    }

    public void testAddSubstringColumn() {
        NST nst = createPhoneNST();
        nst.addSubstringColumn("name", "shortName", 0, 2);

        // test columns
        List columnNames = nst.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("shortName"));
        assertEquals(DataTypeEnum.STR, nst.getNSTColumn("shortName").getType());

        // test rows
        List resultList = nst.selectRows("shortName").toStringList(1);
        assertEquals(3, resultList.size());
        assertEquals("jo", resultList.get(0));
        assertEquals("pa", resultList.get(1));
        assertEquals("jo", resultList.get(2));
        nst.release();
    }

    public void testAggregate() {
        NST phone = createPhoneNST();
        NST aggrNST = phone.aggregate("sum", "name", "phone");
        TestUtil.verifyCollections(new String[]{"john.577", "paul.471"}, aggrNST);

        NST baseNST = new NST("name, phone", "str,int").insertRow("john, 0").insertRow("paul, 0").insertRow("mary, 0");
        aggrNST = phone.aggregate("sum", "name", "phone", baseNST);
        TestUtil.verifyCollections(new String[]{"john.577", "paul.471", "mary.0"}, aggrNST);

        // test mode
        phone.insertRow(new String[]{"john", "324"});
        aggrNST = phone.aggregate("mode", "name", "phone");
        TestUtil.verifyCollections(new String[]{"john.324", "paul.471"}, aggrNST);
    }

    /**
     * Expected results
     * john 253 john 253
     * john 253 paul 333
     * john 252 john 444
     * paul 471 john 253
     * paul 471 paul 333
     * paul 417 john 444
     * john 324 john 253
     * john 324 paul 333
     * john 324 john 444
     *
     * @
     */
    public void testCross() {
        // Create an NST
        NST phone = createPhoneNST();
        NST phone2 = createOtherPhoneNST();
        NST join = phone.cross(phone2, "A.name, A.phone, B.name AS other");

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("A.name"));
        assertTrue(columnNames.contains("A.phone"));
        assertTrue(columnNames.contains("other"));

        // make sure it has the right rows
        assertEquals(9, join.getRowCount());
        List rowList = new ArrayList();
        ResultSet resultSet = join.selectRows();
        while (resultSet.next()) {
            String all = resultSet.getString(1) + resultSet.getString(2) +
                    resultSet.getString(3);
            rowList.add(all);
        }
        assertEquals(9, rowList.size());
        assertTrue(rowList.contains("john" + "253" + "john"));
        assertTrue(rowList.contains("john" + "253" + "paul"));
        assertTrue(rowList.contains("john" + "253" + "john"));
        assertTrue(rowList.contains("paul" + "471" + "john"));
        assertTrue(rowList.contains("paul" + "471" + "paul"));
        assertTrue(rowList.contains("paul" + "471" + "john"));
        assertTrue(rowList.contains("john" + "324" + "john"));
        assertTrue(rowList.contains("john" + "324" + "paul"));
        assertTrue(rowList.contains("john" + "324" + "john"));

        join.release();
        phone2.release();
        phone.release();
    }

    public void testCopyAllCols() {
        NST nst = createPhoneNST();
        NST copiedNST = nst.copy();
        nst.release();

        // check columns
        List columnNames = copiedNST.getNSTColumnNames();
        assertEquals(2, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));

        // check rows
        ResultSet resultSet = copiedNST.selectRows("*", "phone", "*");
        List numbers = resultSet.toStringList(1);
        assertEquals(3, numbers.size());
        assertTrue(numbers.contains("253"));
        assertTrue(numbers.contains("324"));
        assertTrue(numbers.contains("471"));

        copiedNST.release();
    }

    public void testCreateFromList() {
        NST phoneNST = createPhoneNST();
        List namesList = phoneNST.selectRows("name").toStringList(1);
        NST namesNST = new NST("name", "str").insertRows(namesList);
        assertEquals(phoneNST.getRowCount(), namesNST.getRowCount());
        assertEquals(1, namesNST.getNSTColumns().size());
        assertEquals(DataTypeEnum.STR, namesNST.getNSTColumn("name").getType());
        namesNST.release();
        phoneNST.release();

        ArrayList oidList = new ArrayList();
        oidList.add("0");
        oidList.add("1");
        oidList.add("2");
        NST oidNST = new NST("id", "oid").insertRows(oidList);
        List actualList = oidNST.selectRows("id").toOIDList(1);
        assertEquals(3, actualList.size());
        assertTrue(actualList.contains(new Integer(0)));
        assertTrue(actualList.contains(new Integer(1)));
        assertTrue(actualList.contains(new Integer(2)));
    }

    public void testDeleteAllRemovesAll() {
        NST nst = createPhoneNST();
        nst.deleteRows();
        if (nst.getRowCount() != 0) {
            fail("After deleteRows(), the NST should have zero rows");
        }
        nst.release();
    }

    /**
     * On 4.6.2, after a delete the properties of a BAT are incorrectly kept as keyed and sorted. There is a fix
     * in insertRowsFromNST that fixes that, by inserting values that are certainly OID.
     * This test verifies that the OIDs of the inserted rows are correct
     */
    public void testDeleteAndInsert() {
        // Create NST with a few rows, delete some
        NST nst = new NST("a", "oid").insertRow("1").insertRow("2");
        nst.insertRow("3").insertRow("4");
        nst.deleteRows("a GE 3");

        // After the delete, insert from another NST
        NST nst2 = new NST("a", "oid").insertRow("7").insertRow("8");
        nst.insertRowsFromNST(nst2);

        // Verify that the NST has head Ids 1,2, 3,4
        int min = (Integer) nst.selectRows("a = 1", "*").toOIDList(0).get(0);
        List headList = nst.selectRows("*").toOIDList(0);
        assertEquals(4, headList.size());
        assertTrue(headList.contains(new Integer(0 + min)));
        assertTrue(headList.contains(new Integer(1 + min)));
        assertTrue(headList.contains(new Integer(2 + min)));
        assertTrue(headList.contains(new Integer(3 + min)));
    }

    public void testDeleteWithFilterRemovesMatch() {
        NST nst = createPhoneNST();
        nst.deleteRows("name = 'john'");
        assertEquals(1, nst.getRowCount());
        nst.release();
    }

    public void testDifference() {
        NST nst1 = createPhoneNST();
        NST nst2 = createPhoneNST();
        nst2.deleteRows("name = 'paul'");

        NST diffNST = nst1.difference(nst2, "name EQ name");
        List namesList = diffNST.selectRows("name").toStringList(1);
        assertEquals(1, namesList.size());
        assertTrue(namesList.contains("paul"));
    }

    public void testDistinct() {
        NST nst1 = createPhoneNST();
        NST diffNST = nst1.distinct("name");
        assertEquals(2, diffNST.getRowCount()); //john, paul

        diffNST = nst1.distinct("name, phone");
        assertEquals(3, diffNST.getRowCount()); //john, john, paul

        diffNST = nst1.distinct("*");
        assertEquals(3, diffNST.getRowCount()); //john, john, paul

        nst1.removeColumn("phone");
        diffNST = nst1.distinct("*");
        assertEquals(2, diffNST.getRowCount()); //john, paul
    }

    public void testFastInsert() {
        NST nst = new NST("id, value", "int, int");
        nst.insertRow("0,0");
        Object[][] data = new Object[100000][2];
        for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
            data[rowIdx][0] = new Integer(rowIdx + 1);
            data[rowIdx][1] = new Integer((rowIdx + 1) * 2);
        }
        nst.fastInsert(data);
        assertEquals(100001, nst.getRowCount());
        assertEquals("10", nst.selectRows("id = 5", "value", "*").toStringList(1).get(0));
        assertEquals("200", nst.selectRows("id = 100", "value", "*").toStringList(1).get(0));
        assertEquals("200000", nst.selectRows("id = 100000", "value", "*").toStringList(1).get(0));
    }

    public void testFilterColNames() {
        NST nst = createPhoneNST();
        NST subsetNST = nst.filter("name = 'john'", "name, phone AS tel");
        assertEquals(3, nst.getRowCount());

        assertEquals(2, subsetNST.getNSTColumnNames().size());
        assertEquals("name", subsetNST.getNSTColumnNames().get(0));
        assertEquals("tel", subsetNST.getNSTColumnNames().get(1));
        assertEquals(2, subsetNST.getRowCount());
        assertEquals(1, subsetNST.getRowCount("tel = 253"));
        subsetNST.release();
        nst.release();
    }

    public void testGetDistinctColumnValuesBinned() {
        NST nst = createPhoneNST();
        List list = nst.getDistinctColumnValuesBinned("phone", 2);
        assertEquals(2, list.size());
        assertTrue(list.contains("324"));
        assertTrue(list.contains("471"));

        list = nst.getDistinctColumnValuesBinned("phone", 3);
        assertEquals(3, list.size());
        assertTrue(list.contains("324"));
        assertTrue(list.contains("471"));
        assertTrue(list.contains("253"));

        list = nst.getDistinctColumnValuesBinned("name", 1);
        assertEquals(1, list.size());
        assertTrue(list.contains("paul"));  // find the last one [john -> paul]
    }

    public void testGetTopRows() {
        NST nst = new NST("a,b,c,d", "int, int, int, int");
        nst.insertRow("1, 1, 20, 2");
        nst.insertRow("1, 1, 10, 3");
        nst.insertRow("1, 1,  9, 1");
        nst.insertRow("1, 2, 19, 1");
        nst.insertRow("1, 2, 19, 1");
        nst.insertRow("1, 2, 19, 1");
        nst.insertRow("1, 3,  9, 1");
        nst.insertRow("1, 3,  8, 1");
        nst.insertRow("1, 3,  7, 1");
        nst.insertRow("1, 3,  6, 1");
        nst.insertRow("1, 3,  5, 1");
        nst.insertRow("2, 1,  9, 1");
        nst.insertRow("2, 1,  7, 1");
        nst.insertRow("2, 2,  7, 1");
        NST topRows = NSTUtil.getTopRows(nst, "a", "b", "c DESC,d", "0,1");
        topRows.print();
    }

    public void testGetDistinctValueCount() {
        NST nst = createPhoneNST();
        int phoneCount = nst.getDistinctColumnValuesCount("phone");
        int nameCount = nst.getDistinctColumnValuesCount("name");
        assertEquals(phoneCount, 3);
        assertEquals(nameCount, 2);
        nst.release();
    }

    public void testGroupBy() {
        // Create an NST
        NST phone = createPhoneNST();
        NST address = createAddressNST();
        NST join = phone.join(address, "name = man");
        NST groupBy = join;
        groupBy.groupBy("name, phone");

        // test rows and columns
        List columnNames = groupBy.getNSTColumnNames();
        assertEquals(5, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("address"));
        assertTrue(columnNames.contains("group_id"));
        // make sure that group_id is the last column
        assertEquals(columnNames.get(4), "group_id");
        // make sure that it has 4 rows
        assertEquals(4, groupBy.getRowCount());

        // make sure that the map for john has two values,
        // and the map for paul has a single one (both rows should get the same group_id)
        Set johnSet = new HashSet();
        Set paulSet = new HashSet();
        ResultSet resultSet = groupBy.selectRows("name, group_id");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            Integer group = new Integer(resultSet.getOID(2));
            if (name.equals("john")) {
                johnSet.add(group);
            } else {
                paulSet.add(group);
            }
        }
        assertEquals(2, johnSet.size());
        assertEquals(1, paulSet.size());
        phone.release();
        address.release();
        groupBy.release();
    }

    // tests that groupBy works when the groups are not sorted
    public void testGroupByAgain() {
        NST nst = new NST("a, b, c", "int, int, int");
        nst.insertRow("3,		  9,		  11");
        nst.insertRow("3,		  9,		  11");
        nst.insertRow("3,		  9,		  10");
        nst.insertRow("4,		  12,		  14");
        nst.insertRow("4,		  12,		  14");
        nst.insertRow("1,		  1,		  8");
        nst.insertRow("1,		  1,		  4");
        nst.insertRow("1,		  1,		  4");
        nst.insertRow("1,		  1,		  3");
        nst.insertRow("1,		  1,		  3");
        nst.insertRow("1,		  1,		  3");
        nst.insertRow("2,		  7,		  6");
        nst.insertRow("2,		  7,		  6");
        nst.insertRow("2,		  7,		  6");
        nst.insertRow("4,		  12,		  14");
        nst.insertRow("4,		  12,		  14");
        nst.insertRow("3,		  9,		  10");

        nst.groupBy("a,b,c");
        nst.addCountColumn("group_id", "group_cnt");
        NST counts = nst.distinct("group_id").project("group_id, group_cnt");
        TestUtil.verifyCollections(new Object[]{
                "1@0.2",
                "2@0.2",
                "3@0.4",
                "4@0.3",
                "5@0.2",
                "6@0.1",
                "7@0.3"}, counts);
    }

    public void testGroupByDeeper() {
        NST nst = new NST("col1, col2, col3", "int, int, str");
        Object[][] data = new Object[10000][3];

        // Insert rows. The values are
        //  0  0  A
        //  0  1  B
        //  0  2  A
        //  0  3  B
        //  0  ....
        //  0 100 A
        //  .......
        //100 100 A
        for (int rowIdx = 0; rowIdx < 100; rowIdx++) {
            for (int row2Idx = 0; row2Idx < 100; row2Idx++) {
                data[(rowIdx * 100) + row2Idx][0] = new Integer(rowIdx);
                data[(rowIdx * 100) + row2Idx][1] = new Integer(row2Idx);
                data[(rowIdx * 100) + row2Idx][2] = ((row2Idx % 2) == 0) ? "A" : "B";
            }
        }
        nst.fastInsert(data);

        // A group by the first column should return 100 different values
        // as should a group by the second column
        assertEquals(100, nst.groupBy("col1").projectDistinct("group_id").getRowCount());
        assertEquals(100, nst.groupBy("col2").projectDistinct("group_id").getRowCount());

        // A group by both columns, however, should return 10000 distinct rows
        assertEquals(10000, nst.groupBy("col1, col2").projectDistinct("group_id").getRowCount());

        // AS group by the third column should give two values
        assertEquals(2, nst.groupBy("col3").projectDistinct("group_id").getRowCount());

        // And a group by the first, third should give 200 element (0-100 times A,B)
        assertEquals(200, nst.groupBy("col1, col3").projectDistinct("group_id").getRowCount());

        // test also that aggregate works well here
        // for example, the counts of each col1,col3 pair should be 50 (200 * 50 = 10000)
        assertEquals(200, nst.groupBy("col1, col3").aggregate("count", "group_id", "col3").getRowCount());
        assertEquals(50, nst.groupBy("col1, col3").aggregate("count", "group_id", "col3").min("col3"));
        assertEquals(50, nst.groupBy("col1, col3").aggregate("count", "group_id", "col3").max("col3"));
        assertEquals(10000.0, nst.groupBy("col1, col3").aggregate("count", "group_id", "col3").sum("col3"), 0);

        // and for a group by the first column, the totals should be 100 (100 * 100 = 10000)
        assertEquals(100, nst.groupBy("col1").aggregate("count", "group_id", "col1").getRowCount());
        assertEquals(100, nst.groupBy("col1").aggregate("count", "group_id", "col1").min("col1"));
        assertEquals(100, nst.groupBy("col1").aggregate("count", "group_id", "col1").max("col1"));

    }

    // Test that group_by works fine even if the rows are sync'ed but not in the same order
    // and if the oids don't start from 0
    public void testGroupByWithMixedRows() {
        NST nst = new NST("name, phone, addr", "str,int,str");
        nst.insertRow("mary, 324, a1");
        nst.insertRow("john, 324, a1");
        nst.insertRow("paul, 471, a2");
        nst.insertRow("anne, 471, a2");
        nst.insertRow("john, 253, a3");
        nst.insertRow("john, 253, a4");

        // delete some rows in the middle
        nst.deleteRows("name = 'mary'");
        nst.deleteRows("name = 'anne'");

        // Screw things up: the columns are still sync'ed, but the rows are not
        // in the same order
        nst.addKeyColumn("key");
        int paulKey = nst.filter("name = 'paul'", "key").min("key");
        int john1Key = nst.filter("addr = 'a3'", "key").min("key");
        int john2Key = nst.filter("addr = 'a1'", "key").min("key");
        int john3Key = nst.filter("addr = 'a4'", "key").min("key");
        nst.removeColumn("key");
        Connection.executeCommand(nst.getNSTColumn("phone") + ".delete()");
        Connection.executeCommand(nst.getNSTColumn("phone") + ".insert(" + paulKey + "@0, 471)");
        Connection.executeCommand(nst.getNSTColumn("phone") + ".insert(" + john1Key + "@0, 253)");
        Connection.executeCommand(nst.getNSTColumn("phone") + ".insert(" + john2Key + "@0, 324)");
        Connection.executeCommand(nst.getNSTColumn("phone") + ".insert(" + john3Key + "@0, 253)");

        nst.groupBy("name, phone");

        // Since verifyCollections checks sets, there should only be one copy of each
        TestUtil.verifyCollections(new Object[]{
                "paul.471.a2.1@0",
                "john.253.a3.2@0",
                "john.253.a4.2@0",
                "john.324.a1.3@0"}, nst);
    }

    public void testImplicitFilterRowCount() {
        NST nst = createPhoneNST();
        NST subsetNST = nst.filter("name = 'john'");
        assertEquals(2, subsetNST.getRowCount());
        assertEquals(1, subsetNST.getRowCount("phone = 253"));
        assertEquals(3, nst.getRowCount());
        subsetNST.release();
        nst.release();
    }


    public void testImplicitFilterSelect() {
        // Create an NST
        NST nst = createPhoneNST();
        NST subsetNST = nst.filter("name = 'john'");
        ResultSet resultSet = subsetNST.selectRows();
        int i = 0;
        while (resultSet.next()) {
            i++;
        }
        assertEquals(2, i);
        resultSet = subsetNST.selectRows("phone = 253", "*");
        i = 0;
        while (resultSet.next()) {
            i++;
        }
        assertEquals(1, i);
        subsetNST.release();
        nst.release();
    }

    public void testInsertWithString() {
        NST phoneNST = createPhoneNST();
        phoneNST.insertRow("'peter', 417");
        phoneNST.insertRow("mary, 427");
        phoneNST.insertRow("'me, you', 422");
        assertEquals(1, phoneNST.filter("name EQ 'peter'").getRowCount());
        assertEquals(1, phoneNST.filter("name EQ 'mary'").getRowCount());
        assertEquals(1, phoneNST.filter("name EQ 'me, you'").getRowCount());
    }

    public void testInsertFromNST() {
        NST nst = createPhoneNST();
        NST nst2 = createPhoneNST();
        nst.insertRowsFromNST(nst2);
        // Check the columns
        List columnNames = nst.getNSTColumnNames();
        assertEquals(2, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        // check the rows
        assertEquals(6, nst.getRowCount());
        ResultSet resultSet = nst.selectRows("*", "phone", "*");
        Set uniquePhones = new HashSet(resultSet.toStringList(1));
        assertEquals(3, uniquePhones.size());
        assertTrue(uniquePhones.contains("253"));
        assertTrue(uniquePhones.contains("471"));
        assertTrue(uniquePhones.contains("324"));
        // Assert that the keys are unique after insert
        assertTrue(MonetUtil.isUniqueHeads(nst.getNSTColumn(0).getBATName()));
        // Assert if doesn't add any columns
        assertEquals(2, nst2.getNSTColumnNames().size());

        nst2.release();
        nst.release();
    }

    public void testDoubleInsert() {
        NST nst = createPhoneNST();
        NST nst2 = createPhoneNST();
        // Screw things up: the columns are still sync'ed, but the rows are not
        // in the same order
        Connection.executeCommand(nst2.getNSTColumn("phone") + ".delete()");
        Connection.executeCommand(nst2.getNSTColumn("phone") + ".insert(2@0, 471)");
        Connection.executeCommand(nst2.getNSTColumn("phone") + ".insert(3@0, 253)");
        Connection.executeCommand(nst2.getNSTColumn("phone") + ".insert(1@0, 324)");

        nst.insertRowsFromNST(nst2);
        // Since verifyCollections checks sets, there should only be one copy of each
        TestUtil.verifyCollections(new Object[]{"john.324", "john.253", "paul.471"}, nst);
    }

    public void testIntersect() {
        // Create an NST
        NST phone = createPhoneNST();
        NST address = createAddressNST();
        NST intersect = address.intersect(phone, "man EQ name");

        // make sure it has the right columns
        List columnNames = intersect.getNSTColumnNames();
        assertEquals(2, columnNames.size());
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("address"));

        // make sure it has the right rows
        assertEquals(3, intersect.getRowCount());
        List rowList = new ArrayList();
        ResultSet resultSet = intersect.selectRows();
        while (resultSet.next()) {
            String all = resultSet.getString(1) + resultSet.getString(2);
            rowList.add(all);
        }
        assertEquals(3, rowList.size());
        assertTrue(rowList.contains("john" + "Amherst"));
        assertTrue(rowList.contains("paul" + "Pelham"));
        assertTrue(rowList.contains("paul" + "Northampton"));

        intersect.release();
        address.release();
        phone.release();
    }

    /**
     * Expected results
     * john 253 Amherst
     * paul 471 Pelham
     */
    public void testIntersectDouble() {
        NST address = new NST("name, phone, address", "str, int, str");
        address.insertRow(new String[]{"john", "253", "Amherst"});
        address.insertRow(new String[]{"john", "555", "Amherst"});
        address.insertRow(new String[]{"paul", "471", "Pelham"});
        address.insertRow(new String[]{"paul", "999", "Northampton"});
        address.insertRow(new String[]{"matthew", "999", "Jerusalem"});

        NST phone = new NST("name, phone", "str, int");
        phone.insertRow(new String[]{"john", "324"});
        phone.insertRow(new String[]{"john", "253"});
        phone.insertRow(new String[]{"paul", "471"});

        NST join = address.intersect(phone, "name EQ name AND phone EQ phone");

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(3, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("address"));

        // make sure it has the right rows
        assertEquals(2, join.getRowCount());
        List rowList = new ArrayList();
        ResultSet resultSet = join.selectRows();
        while (resultSet.next()) {
            String all = resultSet.getString(1) + resultSet.getString(2) +
                    resultSet.getString(3);
            rowList.add(all);
        }
        assertEquals(2, rowList.size());
        assertTrue(rowList.contains("john" + "253" + "Amherst"));
        assertTrue(rowList.contains("paul" + "471" + "Pelham"));

        join.release();
        address.release();
        phone.release();
    }

    /**
     * Expected results
     * john 253 john Amherst
     * paul 471 paul Pelham
     * john 324 john Amherst
     *
     * @
     */
    public void testJoin() {
        // Create an NST
        NST phone = createPhoneNST();
        NST address = createAddressNST();
        NST join = phone.join(address, "name = man");

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(4, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("address"));

        // make sure it has the right rows
        assertEquals(4, join.getRowCount());
        List rowList = new ArrayList();
        ResultSet resultSet = join.selectRows();
        while (resultSet.next()) {
            String all = resultSet.getString(1) + resultSet.getString(2) +
                    resultSet.getString(3) + resultSet.getString(4);
            rowList.add(all);
        }
        assertEquals(4, rowList.size());
        assertTrue(rowList.contains("john" + "253" + "john" + "Amherst"));
        assertTrue(rowList.contains("paul" + "471" + "paul" + "Pelham"));
        assertTrue(rowList.contains("paul" + "471" + "paul" + "Northampton"));
        assertTrue(rowList.contains("john" + "324" + "john" + "Amherst"));

        join.release();
        address.release();
        phone.release();
    }

    public void testJoinColNames() {
        // Create an NST
        NST phone = createPhoneNST();
        NST address = createAddressNST();
        NST join = phone.join(address, "name = man", "name, phone as digits, man, address AS street");

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(4, columnNames.size());
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("digits"));
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("street"));
    }


    /**
     * Tests a weird condition that generated an error when using Monet 4.6.2
     * When the result of a first join (forward NST in test below) returns 0 rows,
     * then inserting into that NST works, but then future joins (or intersects) using that NST fail.
     * This condition was fixed with a test in the NST join constructor, which simply returns a new NST
     * if the join has no rows.
     */
    public void testJoinEmpty() {
        DB.getLinkNST().deleteRows();
        DB.insertLink(7, 10);
        DB.insertLink(8, 9);
        DB.insertLink(8, 10);
        DB.insertLink(10, 11);
        DB.insertLink(10, 12);

        NST linkNST = DB.getLinkNST();

        NST nodeIdNST = new NST("id", "oid").insertRow("9");
        String firstCol = "o1_id", secondCol = "o2_id";
        NST forward = nodeIdNST.join(linkNST, "id EQ " + firstCol, secondCol).renameColumn(secondCol, "id");
        NST reverse = nodeIdNST.join(linkNST, "id EQ " + secondCol, firstCol); // id
        forward.insertRowsFromNST(reverse);
        //        [SHOWING head,id WHERE * LIMIT *]
        //        [ 0@0,	  8@0		  ]

        NST nodeIdNST1 = new NST("id", "oid").insertRow("10");
        String firstCol1 = "o2_id", secondCol1 = "o1_id";
        NST forward1 = nodeIdNST1.join(linkNST, "id EQ " + firstCol1, secondCol1).renameColumn(secondCol1, "id");
        NST reverse1 = nodeIdNST1.join(linkNST, "id EQ " + secondCol1, firstCol1); // id
        forward1.insertRowsFromNST(reverse1);
        //        [SHOWING head,id WHERE * LIMIT *]
        //        [ 0@0,	  7@0		  ]
        //        [ 1@0,	  8@0		  ]
        //        [ 2@0,	  11@0		  ]
        //        [ 3@0,	  12@0		  ]

        // The intersection should have 1 row, the 8@0
        NST intersection = forward.intersect(forward1, "id EQ id");
        assertEquals(1, intersection.getRowCount());
    }

    public void testJoinUnless() {
        NST l = createPhoneNST();
        NST r = createPhoneNST();

        NST j = l.joinUnless(r, "name = name", "phone = phone", "A.name, A.phone, B.phone AS other");
        j.print();
        TestUtil.verifyCollections(new String[]{
                "john.324.253",
                "john.253.324"
        }, j);
    }


    /**
     * Expected results
     * john 253 john Amherst
     * paul 471 paul Pelham
     * john 324 john Amherst
     *
     * @
     */
    public void testJoinWithMethod() {
        // Create an NST
        NST phone = createPhoneNST();
        NST address = createAddressNST();
        NST join = phone.join(address, "name EQ man", "*");

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(4, columnNames.size());
        assertTrue(columnNames.contains("man"));
        assertTrue(columnNames.contains("phone"));
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("address"));

        // make sure it has the right rows
        TestUtil.verifyCollections(new String[]{"john.253.john.Amherst",
                "paul.471.paul.Pelham",
                "john.324.john.Amherst",
                "paul.471.paul.Northampton"}, join);

        join.release();

        // join with itself to make sure that names get re-written
        join = phone.join(phone, "name EQ name", "*");

        // make sure it has the right columns
        columnNames = join.getNSTColumnNames();
        assertEquals(4, columnNames.size());
        assertTrue(columnNames.contains("A.name"));
        assertTrue(columnNames.contains("A.phone"));
        assertTrue(columnNames.contains("B.name"));
        assertTrue(columnNames.contains("B.phone"));

        join.release();
        address.release();
        phone.release();
    }

    /**
     * Expected results
     * john 253 john 253
     *
     * @
     */
    public void testJoinTwoColumns() {
        // Create an NST
        NST phone = createPhoneNST();
        NST phone2 = createOtherPhoneNST();
        NST join = phone.join(phone2, new String[]{"name", "phone"}, new String[]{"name", "phone"});

        // make sure it has the right columns
        List columnNames = join.getNSTColumnNames();
        assertEquals(4, columnNames.size());
        assertTrue(columnNames.contains("A.name"));
        assertTrue(columnNames.contains("A.phone"));
        assertTrue(columnNames.contains("B.name"));
        assertTrue(columnNames.contains("B.phone"));

        // make sure it has the right rows
        assertEquals(1, join.getRowCount());
        List rowList = new ArrayList();
        ResultSet resultSet = join.selectRows();
        while (resultSet.next()) {
            String all = resultSet.getString(1) + resultSet.getString(2) +
                    resultSet.getString(3) + resultSet.getString(4);
            rowList.add(all);
        }
        assertEquals(1, rowList.size());
        assertTrue(rowList.contains("john" + "253" + "john" + "253"));

        join.release();

        join = phone.join(phone2, new String[]{"name", "phone"}, new String[]{"name", "phone"}, "A.name, B.phone");
        columnNames = join.getNSTColumnNames();
        assertEquals(2, columnNames.size());
        assertTrue(columnNames.contains("A.name"));
        assertTrue(columnNames.contains("B.phone"));

        join.release();
        phone2.release();
        phone.release();


    }

    public void testJoinTwoColumnsWithAsNames() {
        // Create an NST
        NST phone = createPhoneNST();
        NST phone2 = createOtherPhoneNST();

        NST join = phone.join(phone2, new String[]{"name", "phone"}, new String[]{"name", "phone"}, "A.name AS name1, B.phone AS tel");
        List columnNames = join.getNSTColumnNames();
        assertEquals(2, columnNames.size());
        assertTrue(columnNames.contains("name1"));
        assertTrue(columnNames.contains("tel"));

        join.release();
        phone2.release();
        phone.release();


    }

    /**
     * Tests the leftOuterJoin() method of the NST class
     *
     * @
     */
    public void testLeftOuterJoin() {
        List rowList = new ArrayList();
        NST addressNST = createAddressNST();
        NST outerJoinNST;
        NST phoneNST = createPhoneNST();
        ResultSet resultSet;
        String[] addressColsToJoin = {"man"};
        String[] newColNames = {"man", "address", "name", "phone"};
        String[] phoneColsToJoin = {"name"};

        outerJoinNST = addressNST.leftOuterJoin(phoneNST,
                addressColsToJoin,
                phoneColsToJoin);

        resultSet = outerJoinNST.selectRows();

        while (resultSet.next())
            rowList.add(resultSet.getString(1) + " " +
                    resultSet.getString(2) + " " +
                    resultSet.getString(3) + " " +
                    resultSet.getString(4));

        assertTrue(rowList.contains("john Amherst john 253"));
        assertTrue(rowList.contains("john Amherst john 324"));
        assertTrue(rowList.contains("paul Pelham paul 471"));
        assertTrue(rowList.contains("paul Northampton paul 471"));
        assertTrue(rowList.contains("matthew Jerusalem null null"));
        assertTrue(rowList.size() == 5);

        addressNST.release();
        outerJoinNST.release();
        phoneNST.release();
    }

    public void testLeftOuterJoinWithStrings() {
        NST addressNST = createAddressNST();
        NST phoneNST = createPhoneNST();
        NST outerJoinNST = addressNST.leftOuterJoin(phoneNST, "man = name");

        ResultSet resultSet = outerJoinNST.selectRows();
        List rowList = new ArrayList();
        while (resultSet.next())
            rowList.add(resultSet.getString(1) + " " +
                    resultSet.getString(2) + " " +
                    resultSet.getString(3) + " " +
                    resultSet.getString(4));

        assertTrue(rowList.contains("john Amherst john 253"));
        assertTrue(rowList.contains("john Amherst john 324"));
        assertTrue(rowList.contains("paul Pelham paul 471"));
        assertTrue(rowList.contains("paul Northampton paul 471"));
        assertTrue(rowList.contains("matthew Jerusalem null null"));
        assertTrue(rowList.size() == 5);

        addressNST.release();
        phoneNST.release();
        outerJoinNST.release();
    }

    public void testLeftOuterJoinWithColNames() {
        NST addressNST = createAddressNST();
        NST phoneNST = createPhoneNST();
        NST outerJoinNST = addressNST.leftOuterJoin(phoneNST, "man = name", "man AS who, address AS addr, name, phone AS tel");

        assertEquals(4, outerJoinNST.getNSTColumnNames().size());
        assertEquals("who", outerJoinNST.getNSTColumnNames().get(0));
        assertEquals("addr", outerJoinNST.getNSTColumnNames().get(1));
        assertEquals("name", outerJoinNST.getNSTColumnNames().get(2));
        assertEquals("tel", outerJoinNST.getNSTColumnNames().get(3));
        ResultSet resultSet = outerJoinNST.selectRows();
        List rowList = new ArrayList();
        while (resultSet.next())
            rowList.add(resultSet.getString(1) + " " +
                    resultSet.getString(2) + " " +
                    resultSet.getString(3) + " " +
                    resultSet.getString(4));

        assertTrue(rowList.contains("john Amherst john 253"));
        assertTrue(rowList.contains("john Amherst john 324"));
        assertTrue(rowList.contains("paul Pelham paul 471"));
        assertTrue(rowList.contains("paul Northampton paul 471"));
        assertTrue(rowList.contains("matthew Jerusalem null null"));
        assertTrue(rowList.size() == 5);

        addressNST.release();
        phoneNST.release();
        outerJoinNST.release();
    }

    public void testLeftOuterJoinWithStringsAgain() {
        NST x = new NST("link_id, o1_id, o2_id", "oid, oid, oid");
        x.insertRow("0, 0, 1");
        x.insertRow("1, 0, 2");

        NST attrDataNST = new NST("id, value", "oid, str");
        attrDataNST.insertRow(new String[]{"0", "director-Of"});

        NST outerJoinNST = x.leftOuterJoin(attrDataNST, "link_id = id");
        outerJoinNST.print();
        ResultSet resultSet = outerJoinNST.selectRows("link_id, value");

        List rowList = new ArrayList();
        while (resultSet.next())
            rowList.add(resultSet.getOID(1) + " " + resultSet.getString(2));

        assertTrue(rowList.contains("0 director-Of"));
        assertTrue(rowList.contains("1 null"));
        assertTrue(rowList.size() == 2);

        outerJoinNST.release();
    }

    public void testMode() {
        // With integers
        NST nst = new NST("id, value", "oid, int");
        nst.insertRow("1, 10");
        nst.insertRow("1, 10");
        nst.insertRow("1, 12");
        nst.insertRow("2, 12");
        nst.insertRow("2, 10");
        nst.insertRow("2, 12");
        String[] expected = new String[]{"1@0.10", "2@0.12"};
        TestUtil.verifyCollections(expected, nst.aggregate("mode", "id", "value"));

        // With strings
        nst = new NST("id, value", "oid, str");
        nst.insertRow("1, aaa");
        nst.insertRow("1, aaa");
        nst.insertRow("1, aab");
        nst.insertRow("2, aab");
        nst.insertRow("2, aaa");
        nst.insertRow("2, aab");
        expected = new String[]{"1@0.aaa", "2@0.aab"};
        TestUtil.verifyCollections(expected, nst.aggregate("mode", "id", "value"));

        // With string heads
        nst = new NST("id, value", "str, str");
        nst.insertRow("xxx, aaa");
        nst.insertRow("xxx, aaa");
        nst.insertRow("xxx, aab");
        nst.insertRow("yyy, aab");
        nst.insertRow("yyy, aaa");
        nst.insertRow("yyy, aab");
        expected = new String[]{"xxx.aaa", "yyy.aab"};
        TestUtil.verifyCollections(expected, nst.aggregate("mode", "id", "value"));
    }

    public void testOrderColumns() {
        NST phoneNST = createPhoneNST();
        assertEquals("name", phoneNST.getNSTColumn(0).getName());
        assertEquals("phone", phoneNST.getNSTColumn(1).getName());
        phoneNST.orderColumns("phone, name");
        assertEquals("phone", phoneNST.getNSTColumn(0).getName());
        assertEquals("name", phoneNST.getNSTColumn(1).getName());
    }

    public void testProject() {
        NST nst = createPhoneNST();
        NST filteredNST = nst.filter("name = 'john'");
        NST copiedNST = filteredNST.project("phone");
        // release old nsts
        filteredNST.release();
        nst.release();

        // check columns
        List columnNames = copiedNST.getNSTColumnNames();
        assertEquals(1, columnNames.size());
        assertTrue(columnNames.contains("phone"));

        // check rows
        ResultSet resultSet = copiedNST.selectRows("*", "phone", "*");
        List numbers = resultSet.toStringList(1);
        assertEquals(2, numbers.size());
        assertTrue(numbers.contains("253"));
        assertTrue(numbers.contains("324"));

        //release
        copiedNST.release();
    }

    public void testRange() {
        NST phoneNST = createPhoneNST();
        NST allNST = phoneNST.range("*");
        assertEquals(phoneNST.getRowCount(), allNST.getRowCount());
        allNST.release();

        NST someNST = phoneNST.range("0-0");
        assertEquals(1, someNST.getRowCount());
        assertTrue(someNST.selectRows("name").toStringList(1).contains("john"));
        someNST.release();

        NST sortedNST = phoneNST.rangeSorted("phone", "1-1");
        assertEquals(1, sortedNST.getRowCount());
        assertTrue(sortedNST.selectRows("name").toStringList(1).contains("john"));
        assertTrue(sortedNST.selectRows("phone").toStringList(1).contains("324"));
        sortedNST.release();

        phoneNST.release();
    }

    /**
     * Tests the NST() overload that takes the tempVarNames arg.
     *
     * @
     */
    public void testRelease() {
        String newBATVarName = MonetUtil.create("oid, bit");
        NST newNST = new NST(new String[]{newBATVarName}, "column", "bit");
        assertFalse(newNST.isReleased());
        assertTrue(Connection.isVarNameDefined(newBATVarName));
        newNST.release();
        assertTrue(newNST.isReleased());
        assertFalse(Connection.isVarNameDefined(newBATVarName));
    }

    /**
     * Tests that internal tables are not released
     *
     * @
     */
    public void testReleaseInternal() {
        NST objectNST = DB.getObjectNST();
        objectNST.release();
        assertFalse(objectNST.isReleased());

        objectNST = DB.getObjectNST();
        objectNST.insertRow("1");
        objectNST.addCountColumn("id", "dummycol");
        String colName = objectNST.getNSTColumn("dummycol").getBATName();
        assertTrue(MonetUtil.getRowCount(colName) > 0);
        // make sure that it's released from Monet. Otherwise this test won't work!
        objectNST.release();
        assertFalse(objectNST.isReleased());
        if (Connection.RELEASE_FROM_MONET) {
            try {
                assertTrue(MonetUtil.getRowCount(colName) > 0);
                fail("Should have complained");
            } catch (Exception e) {
                // expected
            }
        }
    }

    public void testReplace() {
        NST nst = createPhoneNST();

        nst.replace("name = 'john'", "name", "'johnny'");
        List names = nst.selectRows().toStringList(1);
        assertEquals(3, names.size());
        assertFalse(names.contains("john"));
        assertTrue(names.contains("johnny"));
        assertTrue(names.contains("paul"));

        nst.replace("*", "name", "'johnny'");
        names = nst.selectRows().toStringList(1);
        assertEquals(3, names.size());
        assertTrue(names.contains("johnny"));
        assertFalse(names.contains("paul"));
        assertFalse(names.contains("john"));
        nst.release();

        nst = createPhoneNST();
        nst.addConstantColumn("name2", "str", "dude");
        nst.replace("name = 'john'", "name", "name2");
        names = nst.selectRows().toStringList(1);
        assertEquals(3, names.size());
        assertFalse(names.contains("john"));
        assertTrue(names.contains("dude"));
        assertTrue(names.contains("paul"));

        nst.release();
    }

    public void testReplaceWithExpr() {
        NST nst = createPhoneNST();
        nst.replace("name = 'john'", "phone", "phone * 2");
        TestUtil.verifyCollections(new String[]{"john.648", "paul.471", "john.506"}, nst);
        nst.replace("name = 'paul'", "name", "name + 'aaaaa'");
        TestUtil.verifyCollections(new String[]{"john.648", "paulaaaaa.471", "john.506"}, nst);

        NST nst1 = new NST("year1, year2", "date, date");
        nst1.insertRow("'2002-10-03', '2002-10-04'");
        nst1.insertRow("'2002-10-03', '2002-11-03'");
        nst1.addConstantColumn("diff", "int", "1");
        nst1.replace("*", "diff", "year1 diff year2");
        TestUtil.verifyCollections(new String[]{"2002-10-03.2002-10-04.-1", "2002-10-03.2002-11-03.-31"}, nst1);
    }

    public void testSave() {
        NST myNST = new NST("a,b", "int,int");
        try {
            myNST.save("prox_link");
            fail("Should have complained about existing NST");
        } catch (Exception e) {
            // ignore
        }

        myNST.save();

        myNST.save("myNST");
        assertTrue(MonetUtil.isBATExists("myNST"));
        assertTrue(MonetUtil.isBATExists("bat_myNST_a"));
        assertTrue(MonetUtil.isBATExists("bat_myNST_b"));

        myNST.save("myNST");
        assertTrue(MonetUtil.isBATExists("myNST"));
        assertTrue(MonetUtil.isBATExists("bat_myNST_a"));
        assertTrue(MonetUtil.isBATExists("bat_myNST_b"));

        myNST.save("anotherNST");
        System.out.println(DB.ls());
        assertTrue(!MonetUtil.isBATExists("myNST"));
        assertTrue(!MonetUtil.isBATExists("bat_myNST_a"));
        assertTrue(!MonetUtil.isBATExists("bat_myNST_b"));
        assertTrue(MonetUtil.isBATExists("anotherNST"));
        assertTrue(MonetUtil.isBATExists("bat_anotherNST_a"));
        assertTrue(MonetUtil.isBATExists("bat_anotherNST_b"));

        myNST.delete();
        assertTrue(!MonetUtil.isBATExists("anotherNST"));
        assertTrue(!MonetUtil.isBATExists("bat_anotherNST_a"));
        assertTrue(!MonetUtil.isBATExists("bat_anotherNST_b"));
    }

//    FAILS!
//    public void testSaveDouble() {
//        NST myNST = new NST("a,b", "int,int");
//        myNST.save();
//        myNST.save();
//    }

    public void testSelectWithFilterAndRange() {
        // Create an NST
        NST nst = createPhoneNST();
        ResultSet s = nst.selectRows("name = 'john'", "phone", "0-0"); // 1 rows, 2 cols
        assertEquals(2, s.getColumnCount());
        List names = s.toStringList(1);
        assertEquals(1, names.size());
        assertTrue(names.contains("324"));
        nst.release();
    }


    public void testSelectOnSort() {
        // Create an NST
        NST nst = createPhoneNST();
        ResultSet resultSet = nst.sort("name, phone DESC", "*").filter("name = 'john'").selectRows("*", "*", "*", true);
        resultSet.next();
        String name = resultSet.getString("name");
        int phone = resultSet.getInt("phone");
        assertEquals("john", name);
        assertEquals(324, phone);
    }


    public void testSort() {
        NST phoneNST = createOtherPhoneNST();
        NST sortedNST = phoneNST.sort("name", "*");

        // Verify that the names and heads of the column are sorted
        ResultSet resultSet = sortedNST.selectRows("name");
        while (resultSet.next()) {
            int oid = resultSet.getOID(0);
            String name = resultSet.getString(1);
            assertTrue((oid == 0 && "john".equals(name)) ||
                    (oid == 1 && "john".equals(name)) ||
                    (oid == 2 && "paul".equals(name)));
        }

        // Verify that the heads of the other column are sorted (0@0, 1@0, etc.)
        resultSet = sortedNST.selectRows("phone");
        while (resultSet.next()) {
            int oid = resultSet.getOID(0);
            String phone = resultSet.getString(1);
            assertTrue((oid == 0 && "444".equals(phone)) ||
                    (oid == 1 && "253".equals(phone)) ||
                    (oid == 2 && "333".equals(phone)));
        }

        // test a subset of columns
        sortedNST = phoneNST.sort("name", "phone");

        // test reverse
        sortedNST = phoneNST.sort("name desc", "*");
        // Verify that the names and heads of the column are sorted
        resultSet = sortedNST.selectRows("name");
        while (resultSet.next()) {
            int oid = resultSet.getOID(0);
            String name = resultSet.getString(1);
            assertTrue((oid == 0 && "paul".equals(name)) ||
                    (oid == 1 && "john".equals(name)) ||
                    (oid == 2 && "john".equals(name)));
        }


    }

    public void testSortMultiple() {
        NST phoneNST = createOtherPhoneNST();
        NST sortedNST = phoneNST.sort("name, phone", "*");

        // Verify that the names and heads of the column are sorted
        ResultSet resultSet = sortedNST.selectRows("name, phone");
        while (resultSet.next()) {
            int oid = resultSet.getOID(0);
            String name = resultSet.getString(1);
            int phone = resultSet.getInt(2);
            assertTrue((oid == 0 && "john".equals(name) && phone == 253) ||
                    (oid == 1 && "john".equals(name) && phone == 444) ||
                    (oid == 2 && "paul".equals(name) && phone == 333));
        }
    }

    public void testTofile() {
        NST nst = createPhoneNST();
        String fileName = System.getProperty("java.io.tmpdir", "tmp") + "/" + "export-test";
        nst.tofile(fileName);
        String expectedString = "\"john\"\t324\n" +
                "\"paul\"\t471\n" +
                "\"john\"\t253\n";
        String actualString = Util.readStringFromFile(new File(fileName));
        assertEquals(expectedString, actualString);
    }

    public void testTofileOIDAsInt() {
        NST nst = new NST("head, tail", "oid, int");
        nst.insertRow(new String[]{"0", "1"});
        nst.insertRow(new String[]{"2", "3"});
        String fileName = System.getProperty("java.io.tmpdir", "tmp") + "/" + "export-test";
        nst.tofile(fileName);
        String expectedString = "0\t1\n" + "2\t3\n";
        String actualString = Util.readStringFromFile(new File(fileName));
        assertEquals(expectedString, actualString);
    }

    public void testUnion() {
        NST phoneNST = createPhoneNST();
        NST unionNST = phoneNST.union(phoneNST, "");
        assertEquals(phoneNST.getNSTColumnNames().size(), unionNST.getNSTColumnNames().size());
        assertEquals(phoneNST.getRowCount() * 2, unionNST.getRowCount());

        unionNST = phoneNST.union(phoneNST, "name");
        assertEquals(phoneNST.getNSTColumnNames().size(), unionNST.getNSTColumnNames().size());
        assertEquals(2, unionNST.getRowCount());

        unionNST = phoneNST.union(phoneNST, "name, phone");
        assertEquals(phoneNST.getNSTColumnNames().size(), unionNST.getNSTColumnNames().size());
        assertEquals(3, unionNST.getRowCount());
    }

    /*
    * An example use for MHay's getMatchingRows call
    public void testReduce() {
        NST a = new NST("a,b,c,d", "int, int, int, int").insertRow("1,1,3,4").insertRow("1,2,3,4");
        NST b = new NST("a,b,x,y", "int, int, int, int").insertRow("1,1,7,8").insertRow("2,2,3,4");

        NST matchingRows = a.getMatchingRows(b, new String[]{"a", "b"}, new String[]{"a", "b"});
        a.deleteRows("a KEYNOTIN " + matchingRows.getNSTColumn("first").getBATName());
        b.deleteRows("a KEYNOTIN " + matchingRows.getNSTColumn("second").getBATName());

        a.print();
        b.print();
    }
    */
}
