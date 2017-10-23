/**
 * $Id: Proximity.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.script;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import kdl.prox.app.GUI2App;
import kdl.prox.app.ImportProx2XMLApp;
import kdl.prox.app.NSTBrowserJFrame;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbvis.DBVisualizerJFrame;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.sample.SampleContainer;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import kdl.prox.monet.MonetException;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * The class whose instances are made available to Jython scripts as the
 * variable "prox". There are two sources of documentation for scripters: 1) the
 * JavaDoc comments below for each method, and 2) the separate detailed user
 * documentation html.<P>
 * <p/>
 * Note: Some of the arguments below take parameters of type PyFunction or
 * PyList. These are the Java programmer names for native python functions and
 * lists, respectively. Just pass the function or list directly as you would for
 * any other use.<P>
 * <p/>
 * Briefly, methods in this class are called like any python method, e.g.:
 * <p/>
 * <BLOCKQUOTE><PRE>
 * collName = 'Movie neighborhoods r1'
 * prox.deleteCollection(collName)
 * prox.queryGraph("graph-query-movie-1d.xml", collName)
 * </PRE></BLOCKQUOTE>
 * <p/>
 * Note: you cannot access additional arguments passed to Proximity's Jython
 * script runner (script.bat or script.sh, which call the
 * <CODE>kdl.prox.app.PythonScript</CODE> application) via the standard
 * <CODE>sys.argv</CODE> variable. Instead you can get them via
 * <CODE>prox.getArgs()</CODE>, which is set to contain the args when you run
 * the script.
 *
 * @see Proximity#getArgs
 * @see kdl.prox.app.PythonScript
 */
public class Proximity {

    private Logger log = Logger.getLogger(Proximity.class);
    private PythonInterpreter pyInterpreter;
    private File scriptFile;
    private String[] args;

    // quick ways to get handles to important structures
    public NST objectNST;
    public NST linkNST;
    public Container rootContainer;
    public Attributes objectAttrs;
    public Attributes linkAttrs;
    public Attributes containerAttrs;

    /**
     * Static initializer that does the one-time only intialization of Jython.
     * Needed because we pass different PySystemStates to
     * RunFileJFrame.initJython().
     */
    static {
        PySystemState.initialize();
    }


    /**
     * Full-arg constructor for use with Jython, saves command line arguments
     *
     * @param scriptFile
     * @param args
     */
    public Proximity(File scriptFile, String[] args) {
        this.scriptFile = scriptFile;
        this.args = args;

        // create the interpreter
        // pass a new PySystemState so that output from each interpreter will be
        // redirected for that interpreter. the default is all interpreters
        // sharing the same output
        pyInterpreter = new PythonInterpreter(null, new PySystemState());

        // initialize public handles
        objectNST = DB.getObjectNST();
        linkNST = DB.getLinkNST();
        rootContainer = DB.getRootContainer();
        objectAttrs = DB.getObjectAttrs();
        linkAttrs = DB.getLinkAttrs();
        containerAttrs = DB.getContainerAttrs();

        pyInterpreter.set("prox", this); // set the 'prox' variable for scripts;

        // initialize the internal methods
        initKDL();
    }

    /**
     * Constructor for use with the Proximity Interpreter in the gui
     */
    public Proximity() {
        this(null, null);
    }

    /**
     * Creates a new attribute using an attribute creation language.
     * Please see documentation for AddAttribute for more info
     *
     * @param attributes   - attributes from which the new one will be created
     * @param newAttrName  - name of the new attribute
     * @param attrFunction - function used to create the new attribute
     * @
     */
    public void addAttribute(Attributes attributes, String newAttrName,
                             String attrFunction) {
        new AddAttribute().addAttribute(attributes, newAttrName, attrFunction);
    }

    public void addAttribute(Attributes attributes, PyList attrPyList, String newAttrName,
                             String attrFunction) {
        List attrList = Util.listFromPyList(attrPyList);

        AddAttribute addAttribute = new AddAttribute(attrList);
        addAttribute.addAttribute(attributes, newAttrName, attrFunction);
    }

    /**
     * Implements the betweenness centrality algorithm for unweighted graphs,
     * as described in "A Faster Algorithm for Betweenness Centrality",
     * U. Brandes, Journal of Mathematical Sociology 25(2):163-177, 2001.
     * <p/>
     * The betweenness centrality for a node v counts the number of shortest paths
     * between all other nodes in the graph that contain v. Note that a path of
     * length 1 does not contribute to these scores because there is no interior node.
     *
     * @param newAttrName  attribute to save betweeenness values to
     * @param isUndirected whether to consider the edges of the graph as undirected
     */
    public void addBetweennessCentralityAttribute(String newAttrName, boolean isUndirected) {
        addBetweennessCentralityAttribute(newAttrName, isUndirected, null);
    }

    /**
     * Implements the betweenness centrality algorithm for unweighted graphs,
     * as described in "A Faster Algorithm for Betweenness Centrality",
     * U. Brandes, Journal of Mathematical Sociology 25(2):163-177, 2001.
     * <p/>
     * The betweenness centrality for a node v counts the number of shortest paths
     * between all other nodes in the graph that contain v. Note that a path of
     * length 1 does not contribute to these scores because there is no interior node.
     *
     * @param newAttrName    attribute to save betweeenness values to
     * @param isUndirected   whether to consider the edges of the graph as undirected
     * @param inputContainer null if query is to be run against the entire
     *                       database. if root container, will run against
     *                       entire db
     */
    public void addBetweennessCentralityAttribute(String newAttrName, boolean isUndirected, Container inputContainer) {
        if (inputContainer != null && inputContainer.isRootContainer()) {
            inputContainer = null;
        }
        SNA.computeBetweennessCentrality(inputContainer, newAttrName, isUndirected);
    }

    /**
     * Creates a new dbl attribute that contains clustering coefficients for
     * items in the entire database.
     *
     * @param newAttrName name of the new attribute. will contain a single dbl
     *                    value column for each object OID in the input that has
     *                    at least two neighbors. error if it already exists
     */
    public void addClusterCoeffAttribute(String newAttrName) {
        addClusterCoeffAttribute(newAttrName, null);
    }


    /**
     * Creates a new dbl attribute that contains clustering coefficients for
     * items in the collection or the entire database.
     *
     * @param newAttrName    name of the new attribute. will contain a single dbl
     *                       value column for each object OID in the input that has
     * @param inputContainer null if query is to be run against the entire
     *                       database. if root container, will run against
     *                       entire db
     */
    public void addClusterCoeffAttribute(String newAttrName, Container inputContainer) {
        if (inputContainer != null && inputContainer.isRootContainer()) {
            inputContainer = null;
        }
        SNA.computeClusteringCoefficient(inputContainer, newAttrName);
    }


    /**
     * Creates a new constant attribute for items in the collection.  See documentation in
     * AddAttribute for more info.
     *
     * @param container
     * @param isObject
     * @param itemName
     * @param newAttrName
     * @param newAttrValue
     */
    public void addConstantAttribute(Container container, boolean isObject, String itemName, String newAttrName, Object newAttrValue) {
        new AddAttribute().addConstantAttribute(container, isObject, itemName, newAttrName, newAttrValue);
    }


    /**
     * Creates a new attribute that represetns the number of days between two date attributes.
     * Subtracting <dateAttrName2> from <dateAttrName1>.
     *
     * @param attributes    - attributes where the current date attribute exists
     * @param newAttrName   - name of the new attribute
     * @param dateAttrName1 - name of the 1st existing date attribute
     * @param dateAttrName2 - name of the 2nd existing date attribute
     * @
     */
    public void addDateDiffAttributeFromTwoDates(Attributes attributes, String newAttrName, String dateAttrName1, String dateAttrName2) {
        AddAttribute addAttribute = new AddAttribute();
        addAttribute.addDateDiffAttributeFromTwoDates(attributes, newAttrName, dateAttrName1, dateAttrName2);
    }

    /**
     * Implements the iterative version of the hubs and authorities algorithm,
     * as described in "Authoritative sources in a hyperlinked environment",
     * J. Kleinberg, Proc. ACM-SIAM Symposium on Discrete Algorithms, 1998.
     * Creates two object attributes. This overload runs on the entire database.
     *
     * @param numIterations number of iterations to run; the paper says that 20
     *                      is sufficient for most applications
     * @param hubAttrName   attribute to save hub values to
     * @param authAttrName  attribute to save authority values to
     */
    public void addHubsAndAuthoritiesAttributes(int numIterations,
                                                String hubAttrName, String authAttrName) {
        addHubsAndAuthoritiesAttributes(numIterations, hubAttrName, authAttrName, null);
    }


    /**
     * Implements the iterative version of the hubs and authorities algorithm,
     * as described in "Authoritative sources in a hyperlinked environment",
     * J. Kleinberg, Proc. ACM-SIAM Symposium on Discrete Algorithms, 1998.
     * Creates two object attributes. This overload runs on the input container
     * only.
     *
     * @param numIterations  number of iterations to run; the paper says that 20
     *                       is sufficient for most applications
     * @param hubAttrName    attribute to save hub values to
     * @param authAttrName   attribute to save authority values to
     * @param inputContainer null if query is to be run against the entire
     *                       database. if root container, will run against
     *                       entire db
     */
    public void addHubsAndAuthoritiesAttributes(int numIterations,
                                                String hubAttrName, String authAttrName,
                                                Container inputContainer) {
        if (inputContainer != null && inputContainer.isRootContainer()) {
            inputContainer = null;
        }
        SNA.computeHubsAndAuthorities(inputContainer, numIterations,
                hubAttrName, authAttrName);
    }

    /**
     * Adds a new attribute for objects or links (depending on isObject)
     * that contains the IDs of the items in the corresponding table
     * Can be used to have qGraph conditions that limit the range of a vertex/edge
     * to a set of IDs in the database
     *
     * @param newAttrName
     * @param isObject
     */
    public void addIDAttribute(String newAttrName, boolean isObject) {
        new AddAttribute().addIDAttribute(newAttrName, isObject);
    }

	/**
	 * Creates a new random attribute for all objects or links.
	 *
	 * @param newAttrName
	 * @param isObject
	 */
	public void addRandomAttribute(String newAttrName, boolean isObject) {
        new AddAttribute().addRandomAttribute(newAttrName, isObject);
	}


    /**
     * Creates a new random attribute for items in the collection.  See documentation in
     * AddAttribute for more info.
     *
     * @param container
     * @param isObject
     * @param itemName
     * @param newAttrName
     */
    public void addRandomAttribute(Container container, boolean isObject, String itemName, String newAttrName) {
        new AddAttribute().addRandomAttribute(container, isObject, itemName, newAttrName);
    }


    /**
     * Creates a new year attribute by extracting the year from an existing attribute of type 'date'.
     *
     * @param attributes   - attributes where the current date attribute exists
     * @param newAttrName  - name of the new attribute
     * @param dateAttrName - name of the existing date attribute
     * @
     */
    public void addYearAttributeFromDate(Attributes attributes, String newAttrName, String dateAttrName) {
        new AddAttribute().addYearAttributeFromDate(attributes, newAttrName, dateAttrName);
    }

    /**
     * Opens a new database browser window for the object with ID objectID.
     *
     * @param objectID
     */
    public void browse(int objectID) {
        GUI2App gui2App = GUI2App.getOrMakeGUI2App();
        gui2App.makeNewBrowserJFrame("item:/objects/" + objectID);
    }

    /**
     * Opens a new database browser window for the container with path containerPath.
     *
     * @param containerPath
     * @see #getContainer(String)
     */
    public void browse(String containerPath) {
        GUI2App gui2App = GUI2App.getOrMakeGUI2App();
        gui2App.makeNewBrowserJFrame("cont:/containers/" + containerPath);
    }

    /**
     * Opens an NSTBrowser window for the given NST
     *
     * @param nst
     */
    public void browse(NST nst) {
        NSTBrowserJFrame nstBrowserJFrame = new NSTBrowserJFrame(nst);
        nstBrowserJFrame.pack();
        nstBrowserJFrame.setVisible(true);
    }

    /**
     * Creates a new container (child of root) where each subgraph is a connected
     * component in the entire database.
     *
     * @param outputContainerName name of the new attribute. will contain a single dbl
     *                            value column for each object OID in the input that has
     *                            at least two neighbors. error if it already exists
     */
    public void computeConnectedComponents(String outputContainerName) {
        computeConnectedComponents(outputContainerName, null);
    }


    /**
     * Creates a new container (child of root) where each subgraph is a connected
     * component in the inputContainer.
     *
     * @param outputContainerName name of the container (under root) to save
     *                            results to. must not exist
     * @param inputContainer      null if query is to be run against the entire
     *                            database. if root container, will run against
     *                            entire db
     */
    public void computeConnectedComponents(String outputContainerName,
                                           Container inputContainer) {
        if (inputContainer != null && inputContainer.isRootContainer()) {
            inputContainer = null;
        }
        SNA.computeConnectedComponents(inputContainer, outputContainerName);
    }

    /**
     * Copy an attribute value from one item within a subgraph to another
     *
     * @param container
     * @param isFromItemObject
     * @param fromItemName
     * @param fromAttrName
     * @param isToItemObject
     * @param toItemName
     * @param toAttrName
     */
    public void copyAttrFromItem(Container container,
                                 boolean isFromItemObject, String fromItemName, String fromAttrName,
                                 boolean isToItemObject, String toItemName, String toAttrName) {
        new AddAttribute().copyAttrFromItem(container,
                isFromItemObject, fromItemName, fromAttrName,
                isToItemObject, toItemName, toAttrName);
    }

    /**
     * Copy an attribute value from one item within a subgraph to the subgraph itself
     *
     * @param container
     * @param isObject
     * @param itemName
     * @param attrName
     * @param newSubgAttrName
     */
    public void copyAttrFromItemToSubgraph(Container container, boolean isObject, String itemName, String attrName,
                                           String newSubgAttrName) {
        new AddAttribute().copyAttrFromItemToSubgraph(container, isObject, itemName, attrName, newSubgAttrName);
    }

    /**
     * Copy a subgraph attribute to one of the items within that subgraph
     *
     * @param container
     * @param fromAttrName
     * @param toIsObject
     * @param toItem
     * @param toAttrName
     */
    public void copyAttrFromSubgraph(Container container, String fromAttrName, boolean toIsObject,
                                           String toItem, String toAttrName) {
        new AddAttribute().copyAttrFromSubgraphToItem(container, fromAttrName, toIsObject, toItem, toAttrName);
    }


    /**
     * Utility that defines a new type of attribute to add to the database.
     * Warns and does nothing if already defined.
     *
     * @param attrName new attribute's name
     * @param isObject true if new attribute applies to objects. false if applies to links
     * @param dataType new attribute's data type. must be a valid kdl.prox.gcf.DataTypeEnum value, e.g., DataTypeEnum.BIGINT.
     * @ if there were problems
     */
    public void defineAttribute(String attrName, boolean isObject,
                                DataTypeEnum dataType) {
        Assert.notNull(attrName, "attrName null");
        Assert.notNull(dataType, "dataType null");

        Attributes attributes = (isObject ? DB.getObjectAttrs() :
                DB.getLinkAttrs());

        if (attributes.isAttributeDefined(attrName)) {
            log.warn("defineAttribute(): attribute already defined: '" +
                    attrName + "'");
        } else {
            try {
                attributes.defineAttribute(attrName, dataType.toString());
                log.info("defined " + (isObject ? "object" : "link") +
                        " attribute '" + attrName + "'");
            } catch (Exception exc) {
                log.error("  error defining attribute: '" + attrName + "': " +
                        exc);
            }
        }
    }

    /**
     * @param pattern
     * @return
     * @see Find#findObjects
     */
    public Map find(String pattern) {
        return Find.findObjects(pattern);
    }

    public Map find(String pattern, String attrName) {
        return Find.findObjects(pattern, attrName);
    }

    public Map find(String pattern, PyList attrNames) {
        return Find.findObjects(pattern, Util.listFromPyList(attrNames));
    }


    public PythonInterpreter getInterpreter() {
        return pyInterpreter;
    }

    /**
     * Finds all paths between two objects, ignoring link direction.
     *
     * @param containerName name of the container to store the paths
     * @param sId start node
     * @param tId end node
     * @param maxLength cut off search at this length
     * @return
     */
    //public int[] findPaths(String containerName, String sId, String tId, int maxLength) {
    //    return PathQuery.getPathsBiDir(proxDB, containerName, sId, tId, maxLength);
    //}

    /**
     * Overload that takes lists of start/end nodes to examine.
     *
     * @param containerName name of the container to store the paths
     * @param sIds start nodes
     * @param tIds end nodes
     * @param maxLength cut off search at this length
     * @return
     */
    //public List findPaths(String containerName, List sIds, List tIds, int maxLength) {
    //    return PathQuery.getPathsBiDir(proxDB, containerName, sIds, tIds, maxLength);
    //}


    /**
     * @return additional arguments passed to the script. useful for
     *         application- specific inputs
     */
    public String[] getArgs() {
        return args;
    }


    /**
     * Utility that allows you to easily get a container with syntax like:
     * <pre>
     * prox.getContainer('a/b')
     * </pre>
     * NB: The leading slash is not passed (it's added for you).
     *
     * @param containerPath path to container, as formatted fro ProxURL, but without
     *                      the leading 'cont:/containers/'
     * @return Container corresponding to containerPath
     * @see kdl.prox.gui2.ProxURL
     */
    public Container getContainer(String containerPath) {
        return DB.getContainer(containerPath);
    }


    /**
     * Utility that returns my dbName. NB: This is a hold-over from the JDBC-
     * based version (2.x). Now I return my connection's info. JavaBean
     * property.
     *
     * @return
     */
    public String getDbName() {
        return DB.description();
    }


    /**
     * Utility that returns my log. JavaBean property. Can be used to log errors
     * and info. Useful Category methods include debug(), error(), fatal(),
     * info(), and warn(). For example:
     * <p/>
     * prox.log.debug("debug info!")
     *
     * @return my log4j Category
     */
    public Logger getLog() {
        return log;
    }


    /**
     * Returns the absolute path of my script file. JavaBean property.
     *
     * @return absolute path of my script file
     */
    public String getScriptFile() {
        return scriptFile.getAbsolutePath();
    }


    /**
     * Shows a dialog to the user that allows inputting a string. Returns the
     * string if the user selected OK, returns null if canceled.
     *
     * @param prompt
     * @return
     */
    public String getStringFromUser(String prompt) {
        return JOptionPane.showInputDialog(null, prompt,
                "Type Input", JOptionPane.QUESTION_MESSAGE);
    }


    /**
     * Shows a dialog to the user that asks yesNoQuestion and has Yes and No
     * buttons. Returns true if the user selected Yes. Returns false if the
     * user selected No or canceled.
     *
     * @param yesNoQuestion
     * @return
     */
    public boolean getYesNoFromUser(String yesNoQuestion) {
        int result = JOptionPane.showConfirmDialog(null, yesNoQuestion,
                "Select Yes or No", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return (result == JOptionPane.YES_OPTION);
    }

    /**
     * Calls standard XML import app
     *
     * @param inputFile
     */
    public void importProx2XML(String inputFile) {
        try {
            new ImportProx2XMLApp(inputFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error: " + e);
            System.exit(-1);
        }
    }

    /**
     * Database utility that initializes an empty database by creating the
     * tables required to run Proximity.
     */
    public void initDB() {
        try {
            log.info("initializing db: creating Proximity tables");
            DB.initEmptyDB();
            log.info("initializing db: done");
        } catch (MonetException monExc) {
            log.error("error initialzing db", monExc);
        }
    }

    /**
     * Loads a series of default Python PROCS and import
     * Defined in script/proximity.py
     */
    public void initKDL() {
        try {
            String fileName = "proximity.py";
            Class clazz = Proximity.class;
            InputStream resourceAsStream = clazz.getResourceAsStream(fileName);
            pyInterpreter.execfile(resourceAsStream);
            resourceAsStream.close();
        } catch (Exception e) {
            log.error("error initializing prox. Init not performed; please, correct errors in init file", e);
        }
    }


    /**
     * Combines the contents of all input containers in the list and copiest
     * them to the destination container. Skips input container if it's equal
     * to destinationContainer.
     *
     * @param inputContainers
     * @param destContainer
     */
    public void mergeContainers(PyList inputContainers, Container destContainer) {
        List inputContList = Util.listFromPyList(inputContainers);
        Container.mergeContainers(inputContList, destContainer);
    }

    /**
     * Module that runs the query specified in the XML file against the entire database,
     * saving the results into outputContainerName.
     *
     * @param queryFileName       path (relative or absolute) of XML <graph-query>
     *                            file to execute. if not absolute, should be relative to Java working
     *                            directory
     * @param outputContainerName name of collection to save the query to. can be
     *                            null if !isExecute
     * @throws Exception
     */
    public void queryGraph(String queryFileName, String outputContainerName) throws Exception {
        queryGraph(queryFileName, outputContainerName, null);
    }

    /**
     * Module that runs the query specified in the XML file, saving the results
     * into outputContainerName. Creates the collection. Errors if it already exists.
     *
     * @param queryFileName       path (relative or absolute) of XML <graph-query>
     *                            file to execute. if not absolute, should be
     *                            relative to Java working directory
     * @param outputContainerName name of collection to save the query to. can be
     *                            null if !isExecute
     * @param inputContainer      null if query is to be run against the entire
     *                            database. if root container, will run against
     *                            entire db
     * @throws Exception
     */
    public void queryGraph(String queryFileName, String outputContainerName,
                           Container inputContainer) throws Exception {
        File queryFile = new File(queryFileName);
        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        if (inputContainer != null && inputContainer.isRootContainer()) {
            inputContainer = null;
        }
        QueryGraph2CompOp.queryGraph(graphQueryEle, inputContainer,
                outputContainerName, true);
    }

    /**
     * Jython interface to SampleContainer class to create samples for
     * numFolds-fold cross-validation
     *
     * @param containerName name of the container to sample from
     * @param numFolds      number of corresponding cross-validation train and test
     *                      sets
     * @
     */
    public void sampleContainer(String containerName, int numFolds) {
        sampleContainer(containerName, numFolds, null);
    }


    /**
     * Jython interface to SampleContainer class
     *
     * @param containerName name of the container to sample from
     * @param numFolds      number of samples or corresponding cross-validation
     *                      train and test sets to create
     * @param sampleName    If not empty, a child container with name sampleName
     *                      is created.  All samples are placed in this child
     *                      container.  Otherwise, train and test sets for
     *                      cross-validation are created.
     * @
     */
    public void sampleContainer(String containerName, int numFolds,
                                String sampleName) {
        Assert.stringNotEmpty(containerName, "containerName empty");
        Assert.condition(DB.getRootContainer().hasChild(containerName),
                containerName + " not in root container");
        Assert.condition(numFolds > 0, "numFolds not positive");

        new SampleContainer(DB.getRootContainer().getChild(containerName),
                numFolds,
                sampleName);
    }


    /**
     * See ShrinkDB class
     *
     * @param containerName
     */
    public void shrinkDB(String containerName) {
        ShrinkDB.shrinkDB(getContainer(containerName));
    }

    public void visualize(int oid) {
        DBVisualizerJFrame dbVisualizerJFrame = new DBVisualizerJFrame();
        dbVisualizerJFrame.graphFromStartingOID(oid + "");
    }

}

