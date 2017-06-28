/**
 * $Id: ColumnComparisonFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ColumnComparisonFilterTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

/**
 * ColumnComparisonTest tests the ColumnComparisonClass
 */
public class ColumnComparisonFilterTest extends TestCase {
    /**
     * a db manager and an NST. Setup by setUp()
     */
    NST nst;

    static Logger log = Logger.getLogger(ColumnComparisonFilterTest.class);


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        createTestNST();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        nst.release();
        TestUtil.closeTestConnection();
    }


    /**
     * Test that constructor doesn't take null arguments
     *
     * @throws Exception
     */
    public void testNullArguments() throws Exception {
        try {
            new ColumnComparisonFilter(null, ComparisonOperatorEnum.EQ, "phone2");
            fail("Created a filter with an empty column name --invalid");
        } catch (Exception e) {
        }
        try {
            new ColumnComparisonFilter("s", null, "t");
            fail("Created a filter with a null operator --invalid");
        } catch (Exception e) {
        }
    }


    public void testApplyNullArguments() throws Exception {
        Filter filter = new ColumnComparisonFilter("a", ComparisonOperatorEnum.EQ, "a");
        try {
            filter.getApplyCmd(null);
            fail("Applied filter to a null nst --invalid");
        } catch (Exception e) {
        }
    }

    public void testDefaultConstructor() throws Exception {
        Filter filter = new ColumnComparisonFilter("phone1", "phone2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }


    public void testApplyFindsStrings() throws Exception {
        Filter filter = new ColumnComparisonFilter("name1", ComparisonOperatorEnum.EQ, "name2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testCompareStringsGT() throws Exception {
        Filter filter = new ColumnComparisonFilter("name1", ComparisonOperatorEnum.GT, "name2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testCompareStringsGE() throws Exception {
        Filter filter = new ColumnComparisonFilter("name1", ComparisonOperatorEnum.GE, "name2");
        assertEquals(2, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testCompareStringsLT() throws Exception {
        Filter filter = new ColumnComparisonFilter("name1", ComparisonOperatorEnum.LT, "name2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testCompareStringsLE() throws Exception {
        Filter filter = new ColumnComparisonFilter("name1", ComparisonOperatorEnum.LE, "name2");
        assertEquals(2, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }


    public void testApplyFindsints() throws Exception {
        Filter filter = new ColumnComparisonFilter("phone1", ComparisonOperatorEnum.EQ, "phone2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testIntLT() throws Exception {
        ColumnComparisonFilter filter = new ColumnComparisonFilter("phone1", ComparisonOperatorEnum.LT, "phone2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testIntLE() throws Exception {
        ColumnComparisonFilter filter = new ColumnComparisonFilter("phone1", ComparisonOperatorEnum.LE, "phone2");
        assertEquals(2, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testIntGT() throws Exception {
        ColumnComparisonFilter filter = new ColumnComparisonFilter("phone1", ComparisonOperatorEnum.GT, "phone2");
        assertEquals(1, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    public void testIntGE() throws Exception {
        ColumnComparisonFilter filter = new ColumnComparisonFilter("phone1", ComparisonOperatorEnum.GE, "phone2");
        assertEquals(2, MonetUtil.getRowCount(filter.getApplyCmd(nst)));
    }

    // Notice that the two phone columns have diff types. The filter should be able to handle this
    private void createTestNST() {
        String[] columnNames = new String[]{"name1", "name2", "phone1", "phone2"};
        String[] columnTypes = new String[]{"str", "str", "int", "dbl"};
        nst = new NST(columnNames, columnTypes);
        nst.insertRow(new String[]{"john", "john", "253", "276.0"});
        nst.insertRow(new String[]{"john", "paul", "471", "283.0"});
        nst.insertRow(new String[]{"john", "andy", "324", "324.0"});
    }
}
