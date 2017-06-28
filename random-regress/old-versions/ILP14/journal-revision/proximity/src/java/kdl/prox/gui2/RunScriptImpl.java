/**
 * $Id: RunScriptImpl.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RunScriptImpl.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import kdl.prox.db.DB;
import kdl.prox.script.Proximity;
import org.python.core.PyFile;
import org.python.util.PythonInterpreter;


/**
 * Concrete implementation that runs a Jython script, taking care of routing
 * log4j (via parent) and print output (implemented here) to interested parties
 * via property changes.
 * <p/>
 * todo would be nice to show % of script run, probably via % of file read. see comment in start() for where
 * todo would be nice to support cancel(), which would also be implemented via special stream that throws an InterruptedException on next read when the flag is set
 */
public class RunScriptImpl extends AbstractRunFileImpl {

    private PythonInterpreter pythonInterp;

    public RunScriptImpl(File scriptFile) {
        super(scriptFile);
    }

    public String getInputObjectString() {
        return ((File) inputObject).getName();
    }

    /**
     * Initializes the jython interpreter, sets the 'prox' variable, and
     * re-routes the interpreter's out and err streams.
     */
    private void initJython
            () {
        Proximity proximity = new Proximity((File) inputObject, null);
        pythonInterp = proximity.getInterpreter();

        // redirect output to jTextPane
        PrintStream runScriptPrintStream = new PrintStream(runFileOutStream);
        pythonInterp.setErr(new PyFile(runScriptPrintStream, "<stderr>"));
        pythonInterp.setOut(new PyFile(runScriptPrintStream, "<stdout>"));
    }

    /**
     * Runs the script.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        BufferedReader bufferedReader = null;
        try {
            initJython();
            startLog4jRouting();
            FileReader fileReader = new FileReader((File) inputObject);
            bufferedReader = new BufferedReader(fileReader);
            fireChange("status", null, "starting running script: " + inputObject);
            pythonInterp.execfile(((File) inputObject).getAbsolutePath());      // todo call overload that allows us to monitor progress via reads, like ProgressMonitorInputStream
        } finally {
            stopLog4jRouting();
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            DB.commit();
            fireChange("status", null, "finished running script");
        }
    }

}
    