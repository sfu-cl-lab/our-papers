/**
 * $Id: FilterFactoryTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: FilterFactoryTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.dbmgr;

import junit.framework.TestCase;
import kdl.prox.TestUtil;


/**
 * Tests Attributes.attrTypeListForTypeDef() and its AttrType helper class.
 */
public class FilterFactoryTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAndOr() throws Exception {
        Filter filter = FilterFactory.getFilter("COL1 EQ 1 AND COL2 LIKE 'a'");
        assertTrue(filter instanceof CompositeFilter);
        CompositeFilter cFilter = (CompositeFilter) filter;
        assertTrue(cFilter.getFilter1() instanceof ColumnValueFilter);
        assertTrue(cFilter.getFilter2() instanceof ColumnValueLikeFilter);
        assertTrue(cFilter.getConnector() == LogicalConnectorEnum.AND);

        filter = FilterFactory.getFilter("COL1 EQ 1 OR COL2 LIKE 'a' AND COL3 NOTIN bat1");
        assertTrue(filter instanceof CompositeFilter);
        cFilter = (CompositeFilter) filter;
        assertTrue(cFilter.getFilter1() instanceof CompositeFilter);
        assertTrue(cFilter.getFilter2() instanceof ColumnNotInFilter);
        assertTrue(cFilter.getConnector() == LogicalConnectorEnum.AND);
        cFilter = (CompositeFilter) cFilter.getFilter1();
        assertTrue(cFilter.getFilter1() instanceof ColumnValueFilter);
        assertTrue(cFilter.getFilter2() instanceof ColumnValueLikeFilter);
        assertTrue(cFilter.getConnector() == LogicalConnectorEnum.OR);

        try {
            FilterFactory.getFilter("AND");
            fail("Expression beginning with AND should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("OR");
            fail("Expression beginning with OR should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testComparison() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 EQ 1");
        assertTrue(filter instanceof ColumnValueFilter);
        filter = FilterFactory.getFilter("COL2 EQ -1.3");
        assertTrue(filter instanceof ColumnValueFilter);
        filter = FilterFactory.getFilter("COL2 EQ .1");
        assertTrue(filter instanceof ColumnValueFilter);
        filter = FilterFactory.getFilter("COL2 EQ '1'");
        assertTrue(filter instanceof ColumnValueFilter);
        filter = FilterFactory.getFilter("COL2 EQ col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 >= col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 <= col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 == col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 = col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 > col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        filter = FilterFactory.getFilter("COL2 < col2");
        assertTrue(filter instanceof ColumnComparisonFilter);
        try {
            FilterFactory.getFilter("col2 EQ");
            fail("EQ without second value should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testDistinct() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 DISTINCT ROWS");
        assertTrue(filter instanceof ColumnDistinctFilter);
        try {
            FilterFactory.getFilter("DISTINCT");
            fail("DISTINCT without a first col should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testGetFilterColumns() {
        assertEquals("a", FilterFactory.getFilterColumns("a = 3"));
        assertEquals("a", FilterFactory.getFilterColumns("a >= 3"));
        assertEquals("a", FilterFactory.getFilterColumns("a NOTIN xx"));
        assertEquals("a", FilterFactory.getFilterColumns("a NOTIN xx AND a IN yy"));
        assertEquals("a", FilterFactory.getFilterColumns("a KEYIN xx AND a IN yy"));
        assertEquals("a", FilterFactory.getFilterColumns("a DISTINCT ROWS"));
        assertEquals("a,b", FilterFactory.getFilterColumns("a DISTINCT ROWS AND b < 3"));
        assertEquals("a,b", FilterFactory.getFilterColumns("a < b"));
        assertEquals("a", FilterFactory.getFilterColumns("a < 'b'"));
    }

    public void testIn() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 in B_12");
        assertTrue(filter instanceof ColumnInFilter);
        filter = FilterFactory.getFilter("COL2 NOTIN B_12");
        assertTrue(filter instanceof ColumnNotInFilter);
        try {
            FilterFactory.getFilter("col1 In");
            fail("IN without a second col should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("col1 notin");
            fail("NOTIN without a second col should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("col1 In 123");
            fail("IN with a number should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("col1 notin 123");
            fail("NOTIN with a number should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("col1 keyin 123");
            fail("keyin with a number should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testLike() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 LIKE 'B%'");
        assertTrue(filter instanceof ColumnValueLikeFilter);
        ColumnValueLikeFilter columnValueLikeFilter = (ColumnValueLikeFilter) filter;
        assertEquals("B%", columnValueLikeFilter.getLikeString());
        try {
            FilterFactory.getFilter("col1 LIKE ");
            fail("LIKE without a second col should give an error");
        } catch (Exception e) {
            // ignore
        }
        try {
            FilterFactory.getFilter("col1 LIKE 13");
            fail("LIKE should be followed by a quoted argument");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testNil() {
        NST names = new NST("name", "str").insertRow("a").insertRow("nil");
        NST phones = new NST("name, phone", "str, str").insertRow("a, 10");
        NST join = names.leftOuterJoin(phones, "name = name", "phone");
        assertEquals(1, join.getRowCount("phone = nil"));
        assertEquals(0, join.getRowCount("phone = 'nil'"));
    }

    public void testRange() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 BETWEEN 4-6");
        assertTrue(filter instanceof ColumnValueRangeFilter);
        try {
            FilterFactory.getFilter("BETWEEN as");
            fail("RANDOM without a range should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testRandom() throws Exception {
        Filter filter = FilterFactory.getFilter("COL2 RANDOM 4");
        assertTrue(filter instanceof ColumnValueRandomFilter);
        try {
            FilterFactory.getFilter("RANDOM as");
            fail("RANDOM with a string literal should give an error");
            FilterFactory.getFilter("RANDOM 'as'");
            fail("RANDOM with a string should give an error");
        } catch (Exception e) {
            // ignore
        }
    }

    public void testStar() throws Exception {
        assertNull(FilterFactory.getFilter("*"));
    }

}
