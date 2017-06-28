/**
 * $Id: CmdCompletionTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CmdCompletionTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.script;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;


public class CmdCompletionTest extends TestCase {

    private static Logger log = Logger.getLogger(CmdCompletionTest.class);

    private PythonInterpreter pyInterpreter;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        pyInterpreter = new PythonInterpreter();
    }

    protected void tearDown()  {
        TestUtil.closeTestConnection();
    }

    // dummy methods used for testRecursionWithMultipleReturnTypes
    public String foo() {
        return null;
    }

    public Object foo(int a) {
        return null;
    }

    public void testInteger() {
        pyInterpreter.exec("a=1");
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a");
        verifyCompletions(completionList, null);
    }

    public void testNoVariable() {
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a");
        verifyCompletions(completionList, null);
    }

    public void testRecursionNoArgs() {
        pyInterpreter.set("a", DB.getRootContainer());
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a.getLinksNST()");
        verifyCompletions(completionList, NST.class);
    }

    public void testRecursionVoidType() {
        List completionList = CmdCompletion.getMethods(pyInterpreter, "DB.clearDB()");
        verifyCompletions(completionList, null);
    }

    public void testRecursionWithArg() {
        pyInterpreter.set("a", DB.getRootContainer());
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a.getLinksNST(\"a\")");
        verifyCompletions(completionList, NST.class);
    }

    public void testRecursionWithMultipleReturnTypes() {
        pyInterpreter.set("a", this);
        List completionList = CmdCompletion.getMethods(pyInterpreter, "DB.foo()");
        verifyCompletions(completionList, null);
    }

    public void testRecursionWithStringWithDot() {
        pyInterpreter.set("a", DB.getRootContainer());
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a.getLinksNST(\"a.\")");
        verifyCompletions(completionList, NST.class);
    }

    public void testString() {
        pyInterpreter.set("a", new String("hi"));
        List completionList = CmdCompletion.getMethods(pyInterpreter, "a");
        verifyCompletions(completionList, null);
    }

    private void verifyCompletions(List actMethods, Class aClass) {
        if (aClass == null) {
            assertEquals(0, actMethods.size());
        } else {
            Method[] methods = aClass.getMethods();
            List expMethods = Arrays.asList(methods);
            assertEquals(expMethods.size(), actMethods.size());
            assertTrue(expMethods.containsAll(actMethods));
        }
    }

}
