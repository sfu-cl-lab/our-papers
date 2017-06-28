/**
 * $Id: ClearDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ClearDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;

import java.util.List;


public class ClearDBTest extends TestCase {

    protected void setUp() {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testClear() throws Exception {
        DB.open(TestUtil.hostAndPort(), false);
        DB.clearDB();
        TestUtil.closeTestConnection();
    }

    public void testGetSchemaLog() {
        DB.open(TestUtil.hostAndPort(), false);
        DB.clearDB();
        DB.initEmptyDB();
        List schemaLog = DB.getSchemaLog();
        assertEquals(1, schemaLog.size());  // the one just created by initEmptyDB()
        TestUtil.closeTestConnection();
    }

    public void testInit() throws Exception {
        DB.open(TestUtil.hostAndPort(), false);
        DB.clearDB();
        DB.initEmptyDB();
        TestUtil.closeTestConnection();
    }

    public void testInitTwice() throws Exception {
        DB.open(TestUtil.hostAndPort(), false);
        DB.clearDB();
        DB.initEmptyDB();
        DB.clearDB();
        DB.initEmptyDB();
        TestUtil.closeTestConnection();
    }

    public void testIsProxTablesDefined() {
        DB.open(TestUtil.hostAndPort(), false);
        DB.clearDB();
        assertFalse(DB.isProxTablesDefined());
        DB.initEmptyDB();
        assertTrue(DB.isProxTablesDefined());
        assertTrue(DB.isProxTablesEmpty());

        DB.insertObject();
        assertFalse(DB.isProxTablesEmpty());
        TestUtil.closeTestConnection();
    }

}
