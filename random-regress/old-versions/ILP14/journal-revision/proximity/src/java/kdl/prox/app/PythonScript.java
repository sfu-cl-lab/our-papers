/**
 * $Id: PythonScript.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: PythonScript.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Authors: Matthew Cornell, cornell@cs.umass.edu
 *          Agustin Schapira, schapira@cs.umass.edu
 *
 * Purpose: Wrapper class for launching PythonScriptRunner. It's cleaner for users
 *          because it places all applications in one package. Also in this package
 *          are GUI, Import, Query.
 */

package kdl.prox.app;

import java.io.File;
import kdl.prox.db.DB;
import kdl.prox.script.Proximity;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInstance;
import org.python.core.PyInteger;
import org.python.core.PyObject;


/**
 * Main application for executing a Proximity Python script file.
 *
 * @see Proximity for information about accessing application-specific arguments
 *      to scripts
 */
public class PythonScript {

    private static Logger log = Logger.getLogger(PythonScript.class);

    /**
     * Processes the input Jython file on the passed database name.
     * <p/>
     * Args: hostAndPort scriptName [applicationArgs]
     */
    public static void main(String[] args) {

        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length < 2) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        String scriptName = args[1];
        Util.initProxApp();

        // continue checking args
        File scriptFile = new File(scriptName);
        log.debug("main(): " + scriptName);
        if (scriptName.length() == 0) {
            System.out.println("scriptName was empty: '" + ", " + scriptName + "'");
            printUsage();
            System.exit(1);            // abnormal
        }

        // test scriptFile
        if (!scriptFile.exists()) {
            System.out.println("couldn't find script: " + scriptFile.getAbsolutePath());
            System.exit(1);            // abnormal
        }

        String[] newArgs = new String[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            newArgs[i - 2] = args[i];
        }

        int exitStatus = runScript(hostAndPort, scriptFile, newArgs);
        System.exit(exitStatus);    // NB: call to exit() shouldn't be required, but the program was hanging here when running scripts that a) called getUserYesNo(), and b) threw exceptions. I.e., works around a Jython 2.1a3 bug
    }


    private static void printUsage() {
        System.out.println("Usage: java " + PythonScript.class.getName() + " hostAndPort scriptName [applicationArgs]\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tscriptName: script file to run\n" +
                "\tapplicationArgs: optional arguments to script\n");
    }


    /**
     * @param hostAndPort
     * @param scriptFile
     * @param args
     * @return Termination status. By convention, a nonzero status code indicates
     *         abnormal termination.
     * @see java.lang.Runtime#exit(int)
     */
    private static int runScript(String hostAndPort, File scriptFile,
                                 String[] args) {
        int status = 0;     // conventional normal termination code
        Assert.notNull(scriptFile, "scriptFile null");
        try {
            log.info("* connecting to db");
            DB.open(hostAndPort);

            // create a Jython interpreter, create the Proximity instance for
            // scripts (opens the database), and declare it for the script
            Proximity proximity = new Proximity(scriptFile, args);

            // execute the script
            log.info("* executing script: " + scriptFile.getAbsolutePath());
            proximity.getInterpreter().execfile(scriptFile.getAbsolutePath());    // todo catch specific exceptions from Python interpreter?
            log.info("* done executing script");
        } catch (PyException pyExc) {
            if (Py.matchException(pyExc, Py.SystemExit)) {
                // NB: following strange value manipulation code is from Py.java:
                PyObject value = pyExc.value;
                if (value instanceof PyInstance) {
                    PyObject tmp = value.__findattr__("code");
                    if (tmp != null)
                        value = tmp;
                }
                if (value instanceof PyInteger) {
                    status = ((PyInteger) value).getValue();
                } else if (Py.None.equals(value)) {
                    // leave status 0
                } else {
                    log.warn("couldn't get exit status from Python system " +
                            "exit call ('" + value + "'); using: " + status);
                }
            } else {    // not a Python SystemExit
                System.out.println("* Python exception running script:" + pyExc);
                status = -1;
            }
        } catch (Exception exc) {
            System.out.println("* exception running script:" + exc);
            status = -1;
        } finally {
            DB.close();
        }
        return status;
    }


}
