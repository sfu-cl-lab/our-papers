/**
 * $Id: Connection.java 3696 2007-11-01 18:45:45Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Connection.java 3696 2007-11-01 18:45:45Z schapira $
 */

package kdl.prox.monet;

import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * Represents a connection to a Monet database. Usage:
 * <p/>
 * o open a connection via open()
 * o work with it via executeCommand() or executeQuery()
 * o call close()
 * <p/>
 * Allows to execute commands and queries
 * Actual handling of sessions is left to MonetStream objects
 */
public class Connection {

    private static Logger log = Logger.getLogger(Connection.class);

    public static int NEW_MONET_PORT_LIMIT = 40000;

    /**
     * Flag that indicates whether to print out error messages if there are
     * unreleased variables when a connection is being closed, and show an error
     * if an unexisting variable is being unreleased.
     * Set to false by default.
     */
    private static boolean IN_STRICT_MODE = false;

    /**
     * Flag that indicates whether to actually send commands to Monet to release
     * unused variables.
     * If set to true, a comand var_name:=nil will be sent right away, to tell Monet to
     * release the memory.
     * If set to false, the variable names will be re-used soon,
     * but until then Monet will continue allocating its memory. The benefit is that there is a whole
     * operation that is saved, which is important when performance is an issue (as in a web server)
     * Note that NSTTest.testReleaseInternal will fail if this is set to false
     * Set to true by default.
     */
    public final static boolean RELEASE_FROM_MONET = true;


    /**
     * Connection-related variables. Set by open method.
     */
    private static final String PROX_MONET_USER = "prox";
    private static String host;
    private static int port;

    /**
     * Saves open/closed status
     */
    private static boolean isOpen = false;

    /**
     * The stream used to communicate with the Monet server
     * executeCommand and executeQuery block to read all the resulting rows
     * Use new MonetConnection(MonetConnection) to create a new connection,
     * with a separate TCP-IP connection, to run concurrent queries
     */
    private static MonetStream monetStream = null;


    /**
     * A Stack of ScopeS. Each stack element corresponds to Monet table 'scope'
     * Added-to by beginScope(), removed-from by endScope(). Should contain only
     * one element when the connection is closed. Used by releaseSavedVar().
     * <p/>
     */
    private static Stack<Scope> scopeStack = new Stack<Scope>();


    /**
     * Keeps track of variables and the scope in which they are defined
     */
    private static Map<String, Scope> varNameScopeMap = new HashMap<String, Scope>();


    // used for getTempVarName. We keep a pool of un-used variable names
    // The pool is implemented as an ArrayList of strings like var_1, var_2, var_3
    // When a new variable is created (via executeAndSave), we get the first available
    //   var_n from the ArrayList; if the list is empty, we add TEMP_POOL_GROW_SPEED
    //   new elements to the list, starting from tempVarCounter --and increase tempVarCounter
    // Upon release of a variable, its name is put back in the list.
    // This is done so that Monet doesn't keep around unused var_names in its
    // view_var_name table, which would cause slowdowns as the size of the list
    // grows --it's impemented as a linked list.
    private static int tempVarCounter = 0;
    private static int TEMP_POOL_GROW_SPEED = 200;
    private static List<String> tempVarPool = new ArrayList<String>(TEMP_POOL_GROW_SPEED);

    private static ConnectionPool connectionPool;

    private Connection() {
    }


    public static synchronized void beginScope() {
        scopeStack.push(new Scope());
    }

    /**
     * Disconnects from the database server, freeing up all used resources
     *
     * @
     */
    public static synchronized void close() {
        if (isOpen == false) {
            return; //do nothing, already closed
        }

        // commit, just in case, to write out persistent but unsaved tables
        commit();

        // verify only one scope left
        if (scopeStack.size() != 1) {
            log.fatal("should only have one scope left on close; you " +
                    "forgot to call endScope(): " + scopeStack.size());
            printScopeStack();
            // throw new MonetException("disconnecting with unclosed scope(s)");
        } else {
            Scope scope = scopeStack.peek();
            if (IN_STRICT_MODE) {
                scope.printUnreleasedVars();
            }
            endScopeInternal();
        }

        // re-initialize scopes
        scopeStack.clear();

        // close the stream
        monetStream.close();
        monetStream = null;

        // close the pool
        connectionPool.close();

        isOpen = false;
    }

    public static synchronized void commit() {
        verifyIsOpen();
        ResultSet resultSet = executeQuery("commit();");
        if (Util.milMessages.isDebugEnabled()) {
            while (resultSet.next()) {
                Util.milMessages.debug("#  " + resultSet.getLine());
            }
        }
    }


    public static synchronized void endScope() {
        Connection.verifyIsOpen();
        if (Connection.scopeStack.size() == 1) {
            throw new MonetException("tried to end 'top-level' scope");
        }
        Connection.endScopeInternal();
    }

    /**
     * Internal method that does the actual work of ending a scope. Internal
     * because does *not* check that the stack size == 1.
     *
     * @
     */
    private static synchronized void endScopeInternal() {
        Scope scope = scopeStack.pop();

        // release all the NSTs and clear the list
        for (Iterator<NST> nstIter = scope.nstList.iterator();
             nstIter.hasNext();) {
            nstIter.next().release();
        }
        scope.nstList.clear();

        // and release extra MIL variables -- created mostly by the old model/ code
        Map<String, VarNameInfo> varNameInfoMap = scope.getVarNameInfoMap();
        for (Iterator<String> varNameIter = varNameInfoMap.keySet().iterator();
             varNameIter.hasNext();) {
            String varName = varNameIter.next();
            releaseSavedVarInternal(varName);
            varNameScopeMap.remove(varName);
        }

    }

    /**
     * Executes a command and save the results in a variable. NB: Calls to this
     * method should be paired with corresponding calls to releaseSavedVar().
     * Typically this is done via calling beginScope(), executeAndSave(), then
     * endScope().
     *
     * @param cmd
     * @return variable name that contains the result of executing cmd
     */
    public static String executeAndSave(String cmd) {
        return executeAndSave(cmd, false);
    }

    /**
     * Executes a command and save the results in a variable. NB: Calls to this
     * method should be paired with corresponding calls to releaseSavedVar().
     * Typically this is done via calling beginScope(), executeAndSave(), then
     * endScope().
     *
     * @param cmd
     * @param isIgnore when true indicates that the variable shouldn't be tracked
     * @return variable name that contains the result of executing cmd
     */
    public static String executeAndSave(String cmd, boolean isIgnore) {
        Assert.stringNotEmpty(cmd, "null cmd");
        verifyIsOpen();

        String varName = getTempVarName(cmd, isIgnore);
        StringBuffer milSB = new StringBuffer();
        milSB.append("var ");
        milSB.append(varName);
        milSB.append(":=");
        milSB.append(cmd);
        executeCommand(milSB.toString());

        return varName;
    }

    /**
     * Executes a command that does not return rows
     *
     * @param cmd
     * @
     */
    public static synchronized void executeCommand(String cmd) {
        verifyIsOpen();
        Util.mil.debug(cmd);
        monetStream.write(cmd);
        readExtraLines("Command returned results");
    }

    /**
     * Runs a query, and returns a ResultSet to traverse the resulting rows
     *
     * @param query
     * @return
     * @
     */
    public static synchronized ResultSet executeQuery(String query) {
        verifyIsOpen();
        Util.mil.debug(query);
        monetStream.write(query);
        return new ResultSet(monetStream, query);
    }


    public static ResultSet executeQueryWithConnectionPool(String query) {
        verifyIsOpen();
        Util.mil.debug(query);

        MonetStream stream = connectionPool.getStream();
        if (stream == null) {
            return executeQuery(query);
        } else {
            synchronized (stream) {
                stream.write(query);
                return new ResultSet(stream, query);
            }
        }
    }


    /**
     * Returns the host for the connection
     *
     * @return name of the host for the connection
     */
    public static String getHost() {
        verifyIsOpen();
        return host;
    }

    /**
     * Returns the port number used for the connection
     *
     * @return port number
     */
    public static int getPort() {
        verifyIsOpen();
        return port;
    }

    /**
     * Package-level access for unit tests.
     *
     * @return
     */
    protected static Stack<Scope> getScopeStack() {
        return scopeStack;
    }

    /**
     * Returns a unique variable name for be used with BATs
     *
     * @return
     */
    private static synchronized String getTempVarName(String cmd, boolean isIgnore) {
        if (tempVarPool.size() == 0) {
            for (int i = 0; i < TEMP_POOL_GROW_SPEED; i++, tempVarCounter++) {
                tempVarPool.add("var_" + tempVarCounter);
            }
            log.debug("Increasing temp. variable pool size; new size: " + tempVarCounter);
        }
        String varName = tempVarPool.remove(0);

        // save in current scope
        if (!isIgnore) {
            Scope scope = scopeStack.peek();
            scope.saveVariable(varName, cmd, isIgnore);
            varNameScopeMap.put(varName, scope);
        }

        return varName;

    }

    /**
     * Returns the number of variables defined in the current scope
     *
     * @return
     */
    public static synchronized int getVarNameCountInScope() {
        verifyIsOpen();
        Scope scope = scopeStack.peek();
        return scope.getVarNameInfoMap().size();
    }


    public static synchronized boolean isOpen() {
        return isOpen;
    }

    /**
     * Returns true if varName is in my varNameInfoMap, and false o/w.
     *
     * @param varName
     * @return
     */
    public static synchronized boolean isVarNameDefined(String varName) {
        verifyIsOpen();
        return varNameScopeMap.get(varName) != null;
    }

    /**
     * Full-arg constructor
     * Prepares a simple connection, and initializes resources
     * Streams to connect to the database server are created when executeCommand or Query are called
     *
     * If the port number is <= NEW_MONET_PORT_LIMIT (40000) then use the new Monet protocol
     * otherwise, use the old Monet (4.6.2) protocol
     *
     * @param host
     * @param port
     */
    public static synchronized void open(String host, int port) {
        open(host, port, 1);
    }

    public static synchronized void open(String host, int port, int connections) {
        if (isOpen) {
            throw new MonetException("A connection to the database is already open.");
        }

        Assert.stringNotEmpty(host, "host");
        Assert.condition(port != 0, "Port is 0");
        Connection.host = host;
        Connection.port = port;

        // By convention, we'll use Monet 4.18 on ports <= 40000
        if (port <= NEW_MONET_PORT_LIMIT) {
            monetStream = new BlockedModeStream(Connection.host, Connection.port);
        } else {
            monetStream = new UnblockedModeStream(Connection.host, Connection.port, PROX_MONET_USER);
        }

        // open a pool of connections
        connectionPool = new ConnectionPool(host, port, connections - 1);

        // Reset variable scopes
        tempVarPool = new ArrayList<String>(TEMP_POOL_GROW_SPEED);
        tempVarCounter = 0;



        isOpen = true;
        beginScope();
    }

    private static void printScopeStack() {
        Iterator<Scope> scopeStackIter = scopeStack.iterator();
        while (scopeStackIter.hasNext()) {
            Scope scope = scopeStackIter.next();
            log.debug("scope: " + scope + "; creation stack trace:");
            scope.printCreationStackTrace();
            log.debug("vars:");
            for (Iterator<String> varNameIter = scope.getVarNameInfoMap().keySet().iterator();
                 varNameIter.hasNext();) {
                String varName = varNameIter.next();
                log.debug("\t" + varName);
            }
        }
    }

    private static void readExtraLines(String errorString) {
        String currentLine = monetStream.readLine();
        String errorLines = "";
        while (currentLine.length() > 0) {
            if (currentLine.startsWith("#")) {
                log.debug(currentLine);
            } else {
                errorLines = currentLine + "\n";
            }
            currentLine = monetStream.readLine();
        }
        if (errorLines.length() > 0) {
            throw new MonetException(errorString + ":" + errorLines);
        }
    }

    /**
     * Runs a query that returns a single value, single column, and returns that value as a string
     * This is faster than creating the entire resultSet structure.
     *
     * @param query
     * @return a String (might be 'nil') with the read value
     */
    public static synchronized String readValue(String query) {
        verifyIsOpen();
        Util.mil.debug(query);
        monetStream.write(query);
        String currentLine = monetStream.readLine();
        readExtraLines("More than one row returned");
        Assert.stringNotEmpty(currentLine, "empty result");
        return Util.unQuote(currentLine.substring(1, currentLine.length() - 1).trim());
    }

    public static synchronized void registerInCurrentScope(NST nst) {
        Scope scope = scopeStack.peek();
        scope.saveNST(nst);
    }

    /**
     * Called after executeAndSave(), releases the passed varName. NB: This is
     * important because it releases <B>large</B> Monet resources.
     * If ignoreExists, do not show an error if the variable does not exist
     *
     * @param varName
     */
    public static synchronized void releaseSavedVar(String varName) {
        verifyIsOpen();
        if (varName == null) {
            return;
        }

        // remove it from its scope
        Scope scope = varNameScopeMap.get(varName);
        if (scope != null) {
            scope.removeVariable(varName);
            varNameScopeMap.remove(varName);
        }

        releaseSavedVarInternal(varName);
    }

    /**
     * Called by releaseSavedVar and by endScope
     * Sends the Monet command to release a variable. Doesn't remove it
     * from its scope, and it doesn't even check that it exists.
     *
     * @param varName
     * @
     */
    private static void releaseSavedVarInternal(String varName) {
        if (RELEASE_FROM_MONET) {
            StringBuffer milSB = new StringBuffer();
            milSB.append("var ");
            milSB.append(varName);
            milSB.append(":=nil");
            executeCommand(milSB.toString());
        }
//        if (varName.startsWith("var_") && tempVarPool.contains(varName)) {
//            throw new IllegalArgumentException("Releasing already released var " + varName);
//        }
        if (varName.startsWith("var_") && !tempVarPool.contains(varName)) {
            tempVarPool.add(Math.min(10, tempVarPool.size()), varName);
        }
    }

    public static synchronized String reserveVarName() {
        return getTempVarName("--reserve--", false);
    }


    public static synchronized boolean setStrict(boolean isStrict) {
        boolean currMode = IN_STRICT_MODE;
        IN_STRICT_MODE = isStrict;
        return currMode;
    }

    /**
     * Verifies that a connection is still open
     *
     * @ is connection is closed
     */
    private static synchronized void verifyIsOpen() {
        if (isOpen == false) {
            throw new MonetException("Connection has already been closed.");
        }
    }

    //
    // inner classes
    //

    /**
     * Used by beginScope()/endScope() above. Instances represent Monet BAT
     * variable scope.
     */
    static class Scope {

        StackTraceElement[] creationStackTrace;

        /**
         * Maps varNames (StringS) to VarNameInfoS.
         */
        private Map<String, VarNameInfo> varNameInfoMap = new HashMap<String, VarNameInfo>();
        public List<NST> nstList = new ArrayList<NST>();

        public Scope() {
            creationStackTrace = getCurrStack();
        }


        /**
         * Called by executeAndSave(), returns a stack trace for the current
         * Thread.
         *
         * @return
         */
        private StackTraceElement[] getCurrStack() {
            try {
                throw new Exception();
            } catch (Exception e) {
                return e.getStackTrace();
            }
        }


        public void printCreationStackTrace() {
            printStackTrace(creationStackTrace);
        }

        public List<String> getUnreleasedVars() {
            List<String> unreleased = new ArrayList<String>();
            for (Iterator<String> varNameIter = varNameInfoMap.keySet().iterator();
                 varNameIter.hasNext();) {
                String varName = varNameIter.next();
                VarNameInfo varNameInfo = getVarNameInfo(varName);
                if (!varNameInfo.getIsIgnore()) {
                    unreleased.add(varName);
                }
            }
            return unreleased;
        }


        public VarNameInfo getVarNameInfo(String varName) {
            return varNameInfoMap.get(varName);
        }


        public Map<String, VarNameInfo> getVarNameInfoMap() {
            return varNameInfoMap;
        }


        private void printStackTrace(StackTraceElement[] stackTrace) {
            for (int i = 2; i < stackTrace.length; i++) {
                String className = stackTrace[i].getClassName();
                if (className.startsWith("kdl")) {
                    log.warn("\tat " + stackTrace[i]);  // 'tat' format same as that used by Throwable.printStackTrace() to allow IDEs to easily browse to source location
                }
            }
        }


        public void printUnreleasedVars() {
            List<String> unreleasedVarNames = getUnreleasedVars();
            if (unreleasedVarNames.size() > 0) {
                log.warn(unreleasedVarNames.size() + " unreleased variables");
            }
            for (Iterator<String> unreleasedVarNameIter = unreleasedVarNames.iterator(); unreleasedVarNameIter.hasNext();)
            {
                String varName = unreleasedVarNameIter.next();
                VarNameInfo varNameInfo = getVarNameInfo(varName);
                log.warn("unreleased variable: '" + varNameInfo.getVarName() +
                        "' - allocated here:");
                StackTraceElement[] stackTrace = varNameInfo.getStackTrace();
                printStackTrace(stackTrace);
            }
        }


        public void removeVariable(String varName) {
            varNameInfoMap.remove(varName);
        }


        public void saveNST(NST nst) {
            nstList.add(nst);
        }

        public void saveVariable(String varName, String cmd, boolean isIgnore) {
            StackTraceElement[] currStack = getCurrStack();
            varNameInfoMap.put(varName, new VarNameInfo(varName, cmd,
                    currStack, isIgnore));
        }


    }


    /**
     * Helper class used by executeAndSave() and friends.
     */
    static class VarNameInfo {

        private String varName;
        private String cmd;
        private boolean isIgnore;
        private StackTraceElement[] stackTrace;


        public VarNameInfo(String varName, String cmd, StackTraceElement[] stackTrace,
                           boolean isIgnore) {
            Assert.stringNotEmpty(varName, "null varName");
            Assert.notNull(stackTrace, "null stackTrace");
            this.varName = varName;
            this.cmd = cmd;
            this.stackTrace = stackTrace;
            this.isIgnore = isIgnore;
        }


        public String getCmd() {
            return cmd;
        }


        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }


        public boolean getIsIgnore() {
            return isIgnore;
        }


        public String getVarName() {
            return varName;
        }


    }

    static class ConnectionPool {
        private int poolSize;
        private MonetStream[] monetStreams;
        private int lastStream = 0;

        public ConnectionPool(String host, int port, int size) {
            Assert.stringNotEmpty(host, "host");
            Assert.condition(port != 0, "Port is 0");

            poolSize = size;
            monetStreams = new MonetStream[poolSize];
            for (int i = 0; i < poolSize; i++) {
                if (port <= 40000) {
                    monetStreams[i] = new BlockedModeStream(host, port);
                } else {
                    monetStreams[i] = new UnblockedModeStream(host, port, PROX_MONET_USER);
                }
            }
        }

        public synchronized void close() {
            // close the streams
            for (MonetStream stream : monetStreams) {
                stream.close();
            }
        }

        public synchronized MonetStream getStream() {
            if (poolSize == 0) {
                return null;
            }
            lastStream = (lastStream + 1) % poolSize;
            return monetStreams[lastStream];
        }

    }
}
