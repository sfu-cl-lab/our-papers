/**
 * $Id: ConnectionTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ConnectionTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.monet;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;


/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class ConnectionTest extends TestCase {

    // no IVs


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Tests ending a scope on a new connection - should not be allowed.
     *
     * @throws Exception
     */
    public void testEndScopeNoBegin() throws Exception {
        Connection.open(TestUtil.HOST, TestUtil.getWorkingPort()); // creates new Connection
        try {
            DB.endScope();
            fail("should not be able to end 'top-level' scope");
        } catch (MonetException monExc) { // ignore }
            Connection.close();
        }
    }


    /**
     * Does a 'blue-sky' test of one properly-paired calls to begin and end
     * scope. Also checks that the scope stack is empty after disconnecting.
     *
     * @throws Exception
     */
    public void testOKPairBeginEndScope() throws Exception {
        Connection.open(TestUtil.HOST, TestUtil.getWorkingPort()); // creates new Connection
        assertEquals(1, Connection.getScopeStack().size());
        DB.beginScope();
        assertEquals(2, Connection.getScopeStack().size());
        DB.endScope();
        assertEquals(1, Connection.getScopeStack().size());
        Connection.close();
        assertEquals(0, Connection.getScopeStack().size());
    }


    /**
     * Tests that releases of variables can be done in an inner scope
     *
     * @throws Exception
     */
    public void testNestedScopeDeletesVariables() throws Exception {
        boolean currMode = Connection.setStrict(true);
        Connection.open(TestUtil.HOST, TestUtil.getWorkingPort()); // creates new Connection
        DB.beginScope();
        String varName1 = Connection.executeAndSave("1"); // created in scope 1
        DB.beginScope();
        Connection.executeAndSave("2");
        Connection.executeAndSave("3");
        assertEquals(2, Connection.getVarNameCountInScope()); // only in scope 2
        Connection.releaseSavedVar(varName1);
        DB.endScope();
        assertEquals(2, Connection.getScopeStack().size()); // scope 1 & initial remaining
        assertEquals(0, Connection.getVarNameCountInScope()); // variable from scope 1 was deleted
        DB.endScope();
        Connection.close();
        Connection.setStrict(currMode);
    }

    public void testSingleValueRead() {
        Connection.open(TestUtil.HOST, TestUtil.getWorkingPort()); // creates new Connection
        assertEquals("a", Connection.readValue("print('a')"));
        assertEquals("abbb", Connection.readValue("print(\"abbb\")"));
        assertEquals(3, Integer.parseInt(Connection.readValue("print(3)")));
        assertEquals((float) 3.0, Float.parseFloat(Connection.readValue("print(2+1)")), 0.0);
        Connection.close();
    }


}
