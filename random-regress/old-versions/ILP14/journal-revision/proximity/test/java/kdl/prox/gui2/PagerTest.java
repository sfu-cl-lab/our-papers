/**
 * $Id: PagerTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: PagerTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.List;


public class PagerTest extends TestCase {

    private static Logger log = Logger.getLogger(PagerTest.class);
    private static final int NUM_ROWS_PER_PAGE = 4;

    private NST nst;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        nst = createPagerNST();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        nst.release();
        TestUtil.closeTestConnection();
    }


    private NST createPagerNST() {
        NST nst = new NST("id", "str");
        nst.insertRow(new String[]{"0"});
        nst.insertRow(new String[]{"1"});
        nst.insertRow(new String[]{"2"});
        nst.insertRow(new String[]{"3"});
        nst.insertRow(new String[]{"4"});
        nst.insertRow(new String[]{"5"});
        nst.insertRow(new String[]{"6"});
        nst.insertRow(new String[]{"7"});
        nst.insertRow(new String[]{"8"});
        nst.insertRow(new String[]{"9"});
        return nst;
    }


    public void testGetResultSet() {
        Pager pager = new Pager(nst, 1, NUM_ROWS_PER_PAGE);
        ResultSet resultSet = pager.getResultSet();
        List oidList = resultSet.toStringList(1);
        assertEquals(4, oidList.size());
        assertTrue(oidList.contains("0"));
        assertTrue(oidList.contains("1"));
        assertTrue(oidList.contains("2"));
        assertTrue(oidList.contains("3"));

        pager = new Pager(nst, 2, NUM_ROWS_PER_PAGE);
        resultSet = pager.getResultSet();
        oidList = resultSet.toStringList(1);
        assertEquals(4, oidList.size());
        assertTrue(oidList.contains("4"));
        assertTrue(oidList.contains("5"));
        assertTrue(oidList.contains("6"));
        assertTrue(oidList.contains("7"));

        pager = new Pager(nst, 3, NUM_ROWS_PER_PAGE);
        resultSet = pager.getResultSet();
        oidList = resultSet.toStringList(1);
        assertEquals(2, oidList.size());
        assertTrue(oidList.contains("8"));
        assertTrue(oidList.contains("9"));
    }


    public void testGetResultSetOnNSTPager() {
        Pager pager = new Pager(nst, 1, NUM_ROWS_PER_PAGE);
        ResultSet resultSet = pager.getResultSet();
        List oidList = resultSet.toStringList(1);
        assertEquals(4, oidList.size());
        assertTrue(oidList.contains("0"));
        assertTrue(oidList.contains("1"));
        assertTrue(oidList.contains("2"));
        assertTrue(oidList.contains("3"));

        pager = new Pager(nst, 2, NUM_ROWS_PER_PAGE);
        resultSet = pager.getResultSet();
        oidList = resultSet.toStringList(1);
        assertEquals(4, oidList.size());
        assertTrue(oidList.contains("4"));
        assertTrue(oidList.contains("5"));
        assertTrue(oidList.contains("6"));
        assertTrue(oidList.contains("7"));

        pager = new Pager(nst, 3, NUM_ROWS_PER_PAGE);
        resultSet = pager.getResultSet();
        oidList = resultSet.toStringList(1);
        assertEquals(2, oidList.size());
        assertTrue(oidList.contains("8"));
        assertTrue(oidList.contains("9"));
    }


    public void testGoodPageNums() {
        Pager pager = new Pager(nst, 1, NUM_ROWS_PER_PAGE);
        assertEquals(1, pager.getPageNum());
        assertEquals(-1, pager.getPrevPageNum());
        assertEquals(2, pager.getNextPageNum());

        pager = new Pager(nst, 2, NUM_ROWS_PER_PAGE);
        assertEquals(2, pager.getPageNum());
        assertEquals(1, pager.getPrevPageNum());
        assertEquals(3, pager.getNextPageNum());

        pager = new Pager(nst, 3, NUM_ROWS_PER_PAGE);
        assertEquals(3, pager.getPageNum());
        assertEquals(2, pager.getPrevPageNum());
        assertEquals(-1, pager.getNextPageNum());
    }


    public void testNumRowsPerPage() {
        Pager pager = new Pager(nst, 1, nst.getRowCount());
        assertEquals(1, pager.getNumPages());

        pager = new Pager(nst, 1, nst.getRowCount() + 1);
        assertEquals(1, pager.getNumPages());

        pager = new Pager(nst, 1, NUM_ROWS_PER_PAGE);
        assertEquals(3, pager.getNumPages());
    }


    public void testHasPrevNext() {
        Pager pager = new Pager(nst, 1, 5);
        assertFalse(pager.hasPrev());
        assertTrue(pager.hasNext());

        pager.next();
        assertTrue(pager.hasPrev());
        assertFalse(pager.hasNext());

        pager.prev();
        assertFalse(pager.hasPrev());
        assertTrue(pager.hasNext());
    }


    public void testPageNumOutOfBounds() {
        try {
            new Pager(nst, -1, NUM_ROWS_PER_PAGE);
            fail("should fail on bad page num");
        } catch (Exception e) {
            // ignore
        }

        try {
            new Pager(nst, 0, NUM_ROWS_PER_PAGE);
            fail("should fail on bad page num");
        } catch (Exception e) {
            // ignore
        }

        try {
            new Pager(nst, 4, NUM_ROWS_PER_PAGE);
            fail("should fail on bad page num");
        } catch (Exception e) {
            // ignore
        }
    }


}
