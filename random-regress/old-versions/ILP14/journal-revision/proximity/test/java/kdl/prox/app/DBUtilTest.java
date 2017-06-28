/**
 * $Id: DBUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DBUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;


public class DBUtilTest extends TestCase {

    Logger log = Logger.getLogger(DBUtilTest.class);


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);
        DB.insertObject(4);
        DB.insertObject(5);
        DB.insertObject(6);
        DB.insertObject(7);
        DB.insertObject(8);
        DB.insertObject(9);
        DB.insertObject(10);

        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 3);
        DB.insertLink(4, 2, 4);
        DB.insertLink(5, 5, 7);
        DB.insertLink(6, 5, 6);
        DB.insertLink(7, 8, 9);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testIsDBConsistent() {
        NST objectNST = DB.getObjectNST();
        NST linkNST = DB.getLinkNST();
        assertTrue(DBUtil.isDBConsistent(objectNST, linkNST));

        DB.insertLink(8, 1, 12321);
        assertFalse(DBUtil.isDBConsistent(objectNST, linkNST));
    }
}
