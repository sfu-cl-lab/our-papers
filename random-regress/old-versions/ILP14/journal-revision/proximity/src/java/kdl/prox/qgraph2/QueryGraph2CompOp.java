/**
 * $Id: QueryGraph2CompOp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryGraph2CompOp.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.gui2.ProxURL;
import kdl.prox.qgraph2.tfm.TFMApp;
import kdl.prox.qgraph2.tfm.TFMExec;
import kdl.prox.qgraph2.tfm.TFMList;
import kdl.prox.qgraph2.tfm.Transformation;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A concrete subclass that executes graph query specified by the input
 * <graph-query> element. This version implements most of the "2.0" QGraph
 * language. (See developer docs for more information.)
 * <p/>
 * The main entry point to query transformation graph analysis and (optional)
 * execution is via the static queryGraph() method. Alternatively, applications
 * that have (or need to get) a graph query Element can call one of the two
 * constructors - the three-arg non-executing one, or the four-arg executing
 * one. For the non-executing cases it is up to the caller to choose and execute
 * a path via execTGPath().
 * <p/>
 * Processing steps: The steps taken by the constructor are:
 * <p/>
 * 1) Convert the graph-query Element into a QGraph 2.0 query (Query).
 * 2) Build the query transformation graph (TFMGraph) based on the starting
 * Query, using available transformations (Transformation).
 * 3) Choose a (preferrably optimal) path (TGPath) through the TFMGraph
 * to execute.
 * 4) (Optionally) execute a TGPath by generating and executing SQL
 * corresponding to each TGEdge. Only performed if isExecute is true.
 */
public class QueryGraph2CompOp {

    private static Logger log = Logger.getLogger(QueryGraph2CompOp.class);

    /**
     * The TransformationS to be used for query processing. Set by constructor.
     */
    private TFMList tfmList;

    /**
     * My "result" instances that may be of use to callers after processing is
     * complete. NB: null if not set. Set by constructor.
     */
    private Query query = null;
    private TFMGraph tfmGraph = null;
    private TGPath tgPath = null;


    /**
     * Three-arg non-executing overload, runs steps 1-3 of the processing,
     * setting my tfmGraph and tfmPath IVs, but not calling execTGPath().
     * Callers can use my "result" accessors for post-processing purposes
     * (graphing, etc.)
     *
     * @param graphQueryEle
     * @throws Exception
     */
    public QueryGraph2CompOp(Element graphQueryEle) throws Exception {
        prepareForExecution(graphQueryEle);   // do steps 1-3
        log.info("* skipping execution");
        log.info("* query: done");
    }


    /**
     * Four-arg executing overload, runs all steps (1-4) of the processing,
     * including choosing a path and calling execTGPath() on it. Callers can
     * use my "result" accessors for post-processing purposes (graphing, etc.)
     *
     * @param graphQueryEle
     * @param inputContainer  -- null if the entire database
     * @param outputContainer
     * @throws Exception
     */
    public QueryGraph2CompOp(Element graphQueryEle,
                             Container inputContainer, String outputContainer) throws Exception {
        prepareForExecution(graphQueryEle);   // do steps 1-3
        if (tgPath != null) {
            log.info("* query step 4/4: executing path");
            Container cont = execTGPath(tgPath, query.cachedItems(), inputContainer, outputContainer);
            // Create links if any
            createLinks(cont, query.addLinks());
            // Set the query as an attribute. Get the original query, not the graphQueryEle, which has been modified
            cont.setQuery(QueryXMLUtil.graphQueryEleToXML(QueryXMLUtil.queryToGraphQueryEle(query)));
        }
        log.info("* query: done");
    }


    public QueryGraph2CompOp() {
    }


    /**
     * Step 2/4 called from constructor, builds the query transformation graph
     * (TFMGraph) based on the starting Query, using available transformations
     * (Transformation). Throws Exception if problems.
     */
    private TFMGraph buildTFMGraph() throws Exception {
        TGVertex startVertex = new TGVertex(query);
        TFMGraph tfmGraph = new TFMGraph(startVertex);        // return value. filled next
        expandTGVertex(tfmGraph, startVertex);
        return tfmGraph;
    }


    /**
     * Step 3/4 called from constructor, chooses a (preferrably optimal) path
     * (TGPath) through the TFMGraph to execute. Reutrns null if there are no
     * solutions.
     * <p/>
     * todo for now we simply return the shortest. also: calculating *all* paths is expensive
     */
    private TGPath chooseTGPath(TFMGraph tfmGraph) throws Exception {
        List tgPaths = QueryGraph2CompOp.tgPathsFromTFMGraph(tfmGraph);    // returns only solutions
        if (tgPaths.isEmpty()) {
            return null;
        } else {
            // have at least one solution, so pick shortest one (for now)
            TGPath shortestTGPath = (TGPath) tgPaths.get(0);        // return value. corrected next in loop if necessary
            Iterator tgPathIter = tgPaths.iterator();
            while (tgPathIter.hasNext()) {
                TGPath tgPath = (TGPath) tgPathIter.next();
                if (tgPath.numEdges() < shortestTGPath.numEdges())
                    shortestTGPath = tgPath;
            }
            return shortestTGPath;
        }
    }


    /**
     * Creates update edges after the query is run
     *
     * @param outputContainer
     * @param addLinksList
     */
    protected void createLinks(Container outputContainer, List addLinksList) {
        if (addLinksList.size() > 0) {
            log.info("Adding links to database");
        }
        for (int edgeIdx = 0; edgeIdx < addLinksList.size(); edgeIdx++) {
            QGAddLink qgAddLink = (QGAddLink) addLinksList.get(edgeIdx);
            NST vertex1NST = outputContainer.getItemNSTByName(true, qgAddLink.getVertex1Name());
            NST vertex2NST = outputContainer.getItemNSTByName(true, qgAddLink.getVertex2Name());
            NST joinNST = vertex1NST.join(vertex2NST, "A.subg_id EQ B.subg_id", "A.item_id, B.item_id");
            joinNST.renameColumn("A.item_id", "from").renameColumn("B.item_id", "to");
            NST fromTo = joinNST.distinct("from, to");

            // Create the links, with the specified attribute
            String attrName = qgAddLink.getAttrName();
            fromTo.addConstantColumn("attr_" + attrName, "str", qgAddLink.getAttrValue());
            DB.createLinks(fromTo);
            log.info("-> Adding add-link " + qgAddLink + ":" + fromTo.getRowCount() + " links created");

            fromTo.release();
            joinNST.release();
            vertex2NST.release();
            vertex1NST.release();
        }
    }


    /**
     * Step 4/4 called from constructor, executes the TGPath by generating and
     * executing MIL corresponding to each TGEdge. Saves results into a new
     * collection named containerName. Assumes that collection does not already
     * exist. Throws Exception if problems.
     */
    public Container execTGPath(TGPath tgPath, Map cachedItems, Container sourceContainer, String destContainerName)
            throws Exception {
        Assert.condition(tgPath != null, "null tgPath");

        Container container = DB.getRootContainer();
        Assert.condition(!container.hasChild(destContainerName), "container already exists: '" +
                destContainerName + "'");

        log.debug("* execTGPath(): " + tgPath + ", " + destContainerName);

        TempTableMgr tempTableMgr = new TempTableMgr();
        QGUtil qgUtil = new QGUtil(sourceContainer);

        // pre-fetch cached edges
        List cached = fetchCachedEdges(tgPath, cachedItems, tempTableMgr);

        // execute each Transformation
        int numEdges = tgPath.edges().size();
        int tfmNum = 0;                 // counter used in loop
        String lastConsolidatedVertex = null;       // save the name of the last consolidated vertex
        Iterator tgEdgeIter = tgPath.edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            String name = ((ConsQGVertex) tgEdge.tfmExec().qgItems().get(0)).catenatedName();
            boolean isCached = cached.contains(name);
            if (tgEdge.vertex().query().isConsolidated()) {
                lastConsolidatedVertex = ((ConsQGVertex) tgEdge.tfmExec().qgItems().get(0)).catenatedName();
            }

            log.info("** " + (isCached ? "fetching" : "executing") + " transformation " + ++tfmNum + "/" + numEdges +
                    ": " + tgEdge.transformation().number() + ":" +
                    tgEdge.tfmApp().argString(true));
            printTGEdge(tgEdge);

            String tempTableName;
            if (isCached) {
                tempTableName = name;
            } else {
                tempTableName = tgEdge.transformation().execTFMExec(tgEdge.tfmExec(),
                        tgEdge.tfmApp(), query, qgUtil, tempTableMgr);
            }

            if (tempTableName != null) {
                NST objTempSGINST = tempTableMgr.getNSTForVertex(tempTableName, true);
                NST linkTempSGINST = tempTableMgr.getNSTForVertex(tempTableName, false);
                log.info("                            " + tfmNum + "/" + numEdges +
                        ": " + objTempSGINST.getRowCount() + " objects, " +
                        linkTempSGINST.getRowCount() + " links");
                DB.commit();
            }
        }

        // Remove all the cached edges that are still in the tempTableMgr, except for the consolidated one!
        List toRemove = new ArrayList();
        Set vertexNames = tempTableMgr.getVertexNames();
        for (Iterator iterator = vertexNames.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if (cached.contains(name) && !name.equals(lastConsolidatedVertex)) {
                toRemove.add(name);
            }
        }
        for (int removeIdx = 0; removeIdx < toRemove.size(); removeIdx++) {
            String name = (String) toRemove.get(removeIdx);
            tempTableMgr.clearNSTForVertex(name);
        }

        // make sure only one temp table left in tempTableMgr
        vertexNames = tempTableMgr.getVertexNames();
        if (vertexNames.size() != 1) {
            throw new Error("number of temp sgi tables left != 1: " + vertexNames.size() + " " + vertexNames);
        }

        // copy final temp table to collection, creating it first
        String finalConsVertName = (String) vertexNames.iterator().next();
        NST objTempSGINST = tempTableMgr.getNSTForVertex(finalConsVertName, true);
        NST linkTempSGINST = tempTableMgr.getNSTForVertex(finalConsVertName, false);
        Container cont = container.createChildFromTempSGINSTs(destContainerName,
                objTempSGINST, linkTempSGINST);
        DB.commit();    // persist container

        // release the last temp table (createChildFromTempSGINSTs makes copies)
        tempTableMgr.clearNSTForVertex((String) vertexNames.toArray()[0]);
        qgUtil.release();

        log.info("-> found " + cont.getSubgraphCount() + " subgraphs");
        log.info("-> query results saved in container: " + destContainerName);
        return cont;
    }

    /**
     * Given a specific TGPath, it looks at the cachedItems from the Query and fetches
     * those that can be reused for this query, by placing them in the tempTableMgr.
     * <p/>
     * Puts all TGEdges that can be reused. For a TGEdge to be reused, all its elements
     * must be cached AND IN THE SAME CONTAINER. For example, for an edge Vertex1.Edge1.Vertex2,
     * all three of Vertex1, Vertex2, and Edge1 must be cached in the same container.
     *
     * @param tgPath
     * @param cachedItems
     * @param tempTableMgr
     */
    private List fetchCachedEdges(TGPath tgPath, Map cachedItems, TempTableMgr tempTableMgr) {
        ArrayList pathElements = new ArrayList();
        Iterator tgEdgeIter = tgPath.edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            String name = ((ConsQGVertex) tgEdge.tfmExec().qgItems().get(0)).catenatedName();
            pathElements.add(name);
        }
        return fetchCachedEdges(pathElements, cachedItems, tempTableMgr);
    }

    /**
     * Same as fetchCachedEdges but receives a list of pathElements instead of a tgPath. Makes testing easier
     *
     * @param pathElements
     * @param cachedItems
     * @param tempTableMgr
     */
    protected List fetchCachedEdges(List pathElements, Map cachedItems, TempTableMgr tempTableMgr) {
        ArrayList cachedList = new ArrayList();
        Iterator tgEdgeIter = pathElements.iterator();
        while (tgEdgeIter.hasNext()) {
            String name = (String) tgEdgeIter.next();
            boolean hasAll = true;
            String origContainer = null;
            String[] names = name.split("\\.");
            for (int i = 0; i < names.length; i++) {
                String thisName = names[i];
                String thisContainer = (String) cachedItems.get(thisName);
                if (i == 0) {
                    origContainer = thisContainer;
                }
                if (origContainer == null || thisContainer == null || !origContainer.equals(thisContainer)) {
                    hasAll = false;
                    break;
                }
            }

            if (hasAll) {
                Container theContainer = DB.getContainer(origContainer);
                NST objectNST = theContainer.getObjectsNSTByName(name.replace('.', ','));
                NST linkNST = theContainer.getLinksNSTByName(name.replace('.', ','));
                if (tempTableMgr.getNSTForVertex(name, true) != null) {
                    tempTableMgr.clearNSTForVertex(name);
                }
                tempTableMgr.putNSTForVertex(name, objectNST, linkNST);
                cachedList.add(name);
            }
        }
        return cachedList;
    }


    /**
     * Called by expandTGVertex(), expands tgVertex (which should have a Query but
     * no edges) by adding one level of TGEdges to it, each of which has a TGVertex
     * filled with a Query, but no edges. Eliminates duplication by checking for
     * equivalent TGVertexs in my TFMGraph, and linking to them instead
     * of new (duplicate) ones, if possible. Then recurses on the new vertices.
     * Throws Exception if problems.
     *
     * @return isFoundStopVertex : true if a solution is found
     */
    private boolean expandTGVertex(TFMGraph tfmGraph, TGVertex tgVertex) throws Exception {
        // if we have found a solution, stop creating the rest of the graph.
        // todo this is just a shotcut for the time being, so we can run complicated queries, but in the near future we'll need to find a more efficient way of finding all the paths and picking the best
        if (tgVertex.query().isConsolidated()) {
            return true;
        }
        // add TGEdges for all applicable Transformations, testing each
        // QGVertex in tgVertex's Query
        List newTGEdges = new ArrayList();        // TGEdges that contain new (i.e., non-duplicate) TGVertexs. filled below. used to recurse
        Iterator qgVertexIter = tgVertex.query().vertices(true).iterator();    // isRecurse
        while (qgVertexIter.hasNext()) {
            QGVertex qgVertex = (QGVertex) qgVertexIter.next();
            // see if any Transformations apply to qgVertex
            Iterator tfmIter = tfmList.transformations().iterator();
            while (tfmIter.hasNext()) {
                Transformation transformation = (Transformation) tfmIter.next();
                Set tfmApps = transformation.isApplicable(qgVertex);
                Iterator tfmAppIter = tfmApps.iterator();
                while (tfmAppIter.hasNext()) {
                    TFMApp tfmApp = (TFMApp) tfmAppIter.next();
                     /**/
                    // TEST:
//                    log.debug("-> applying tfm: " + transformation + ": " + tfmApp.argString(false) + ":");
//                    printQuery(tgVertex.query());

                    TFMExec tfmExec = transformation.applyTFMApp(tfmApp);    // throws Exception
                    QGItem firstQGItem = (QGItem) tfmExec.qgItems().iterator().next();
                    Query augQuery = firstQGItem.parentAQuery().rootQuery();
                     /**/
                    // TEST:
//                    log.debug("-> after:");
//                    printQuery(augQuery);

                    // create a new TGVertex and TGEdge, first checking if an
                    // equivalent Query exists
                    TGVertex newTGVertex;        // existing or new TGVertex for augQuery. set next
                    TGVertex dupTGVertex = tfmGraph.tgVertexEqualToQuery(augQuery);
                    if ((dupTGVertex != null) && tfmGraph.isAncestorTGVertex(dupTGVertex,
                            tgVertex)) {    // prevent a cycle!
                        log.fatal("trying to add a cycle!: " + tgVertex + ", " +
                                dupTGVertex);
                        printTFMGraph(tfmGraph);    // TEST
                        throw new Error("trying to add a cycle!: " + tgVertex +
                                ", " + dupTGVertex);
                    }
                    boolean isNewTGVertex;        // true if non-duplicate TGVertex created
                    if (dupTGVertex != null) {
                        newTGVertex = dupTGVertex;
                        isNewTGVertex = false;
                    } else {
                        newTGVertex = new TGVertex(augQuery);
                        isNewTGVertex = true;
                    }
                    // add the edge
                    TGEdge tgEdge = new TGEdge(transformation, tfmApp, tfmExec,
                            newTGVertex);
                    if (isNewTGVertex)
                        newTGEdges.add(tgEdge);
                    tgVertex.addEdge(tgEdge);
                }
            }
        }
        // recurse on new TGEdges' TGVertexs, *skipping* ones that link to
        // existing TGVertexs
        Iterator tgEdgeIter = newTGEdges.iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            TGVertex newTGVertex = tgEdge.vertex();
            if (expandTGVertex(tfmGraph, newTGVertex)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Internal constructor that does steps 1 through 3.
     *
     * @param graphQueryEle
     * @throws Exception
     */
    private void prepareForExecution(Element graphQueryEle)
            throws Exception {
        tfmList = new TFMList();
        log.debug("tfm list: " + tfmList.transformations());    // TEST
        // step 1/4: create a Query from the XML and validate it
        log.info("* query step 1/4: parsing and checking");
        query = QueryXMLUtil.graphQueryEleToQuery(graphQueryEle);

        // TEST:
        log.debug("-> input query:");
        printQuery();

        validateQuery();    // throws Exception

        // step 2/4: build the transformation graph
        log.info("* query step 2/4: building transformation graph");
        tfmGraph = buildTFMGraph();

        // TEST:
        log.debug("-> transformation graph:");
        printTFMGraph(tfmGraph);

        // step 3/4: choose a path through the transformation graph
        log.info("* query step 3/4: choosing transformation graph path");
        tgPath = chooseTGPath(tfmGraph);        // null if no solutions

        // TEST:
        log.debug("-> chosen path:");
        if (tgPath == null)
            log.fatal("no solution found with current transformations");
        else
            printTGPath(tgPath);
    }


    /**
     * TEST method that prints query's contents.
     */
    public void printQuery() {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {
            int level = -1;        // current subquery level. managed by start and end methods. -1 instead of 0 because initial start called on query itself

            public void addLink(QGAddLink addLink) {
                log.debug(addLink);
            }

            public void cachedItem(String itemName, String containerName) {
                log.debug(itemName + " from " + containerName);
            }

            public void constraint(QGConstraint qgConstraint) {
                log.debug(qgConstraint);
            }

            public void edge(QGEdge qgEdge) {
                log.debug(qgEdge);
            }

            public void endAbstractQuery(AbstractQuery abstractQuery) {
                level--;
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                level++;
                // level
                log.debug("==== " + level + " ====");
                // instance
                log.debug("aq: " + abstractQuery);
                // Subquery-specific: annotation and parent
                if (abstractQuery instanceof Subquery) {
                    Subquery subquery = (Subquery) abstractQuery;
                    log.debug("annot: " + subquery.annotation());
                    log.debug("parent: " + subquery.parentAQuery());
                }
            }

            public void startConstraints() {
                log.debug("constraints:");
            }

            public void startEdges() {
                log.debug("edges:");
            }

            public void startVertices() {
                log.debug("vertices:");
            }

            public void vertex(QGVertex qgVertex) {
                log.debug(qgVertex);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }


    /**
     * TEST method called by printTFMGraph().
     */
    static public void printTGEdge(TGEdge tgEdge) {
        String shortName = tgEdge.transformation().number() + ":" +
                tgEdge.tfmApp().argString(true);
        log.debug(tgEdge + " -> " + tgEdge.vertex() + " - " + shortName + ": " +
                tgEdge.transformation().getClass().getName() + ": " +
                tgEdge.tfmApp().argString(false));        // isJustName
    }


    /**
     * TEST method that prints tgPath's contents.
     */
    private void printTGPath(TGPath tgPath) {
        log.debug("==== " + tgPath + " ====");
        Iterator tgEdgeIter = tgPath.edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            printTGEdge(tgEdge);
        }
    }


    /**
     * TEST method that prints tfmGraph's contents.
     */
    private void printTFMGraph(TFMGraph tfmGraph) {
        // for now just print all vertices and their edges
        Set tgVertices = tfmGraph.tgVertices();
        Iterator tgVertexIter = tgVertices.iterator();
        while (tgVertexIter.hasNext()) {
            TGVertex tgVertex = (TGVertex) tgVertexIter.next();
            boolean isStart = (tgVertex == tfmGraph.startVertex());
            boolean isConsol = tgVertex.query().isConsolidated();
            boolean isDeadEnd = (tgVertex.edges().isEmpty() && !isConsol);
            String startVertexStr = (isStart ? "s" : "");
            String consolQueryStr = (isConsol ? "!" : "");
            String deadEndStr = (isDeadEnd ? "x" : "");
            log.debug("==== " + tgVertex + " " + startVertexStr + consolQueryStr +
                    deadEndStr + " ====");
            Iterator tgEdgeIter = tgVertex.edges().iterator();
            while (tgEdgeIter.hasNext()) {
                TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
                printTGEdge(tgEdge);
            }
        }
    }


    /**
     * "Result" accessor. Useful to callers when processing is complete.
     *
     * @return the input Query instance. null if errors.
     */
    public Query query() {
        return query;
    }


    /**
     * Top-level entry point that runs the query specified in the XML file,
     * saving the results into collection. Creates the collection. Errors if it
     * already exists. Returns the QueryGraph2CompOp instance whose final
     * results can be obtained via its query(), tfmGraph(), and tgPath()
     * methods.
     *
     * @param graphQueryEle       the query's top-level Element. use graphQueryEleForFile() to get from file
     * @param inputContainer      null if the query is to be run against the entire db
     * @param outputContainerName
     * @param isExecute
     * @return
     * @throws Exception
     */
    public static QueryGraph2CompOp queryGraph(Element graphQueryEle,
                                               Container inputContainer,
                                               String outputContainerName,
                                               boolean isExecute) throws Exception {
        Assert.condition(graphQueryEle != null, "graphQueryEle null");
        try {
            Element compInfoElement = graphQueryEle.getChild("description");
            String description = (compInfoElement == null ? "" :
                    graphQueryEle.getChildText("description"));
            log.info("   query information:\n" +
                    "\tname: '" + graphQueryEle.getAttributeValue("name") + "'\n" +
                    "\tdescription: '" + description + "'");
            if (isExecute) {
                log.info("* saving results to container: " + outputContainerName);
                return new QueryGraph2CompOp(graphQueryEle, inputContainer, outputContainerName);
            } else {
                return new QueryGraph2CompOp(graphQueryEle);
            }
        } catch (JDOMException jdExc) {
            Throwable bestThrowable = (jdExc.getCause() != null ?
                    jdExc.getCause() : jdExc);
            log.fatal("problem parsing xml file", bestThrowable);
            throw jdExc;
        }
    }

    /**
     * Runs the passed query. NB: Deletes the containerName if it exists without
     * prompting. Overload that uses a File to specify the query. This overload
     * is the one most commonly used.
     *
     * @param queryFile           file containing query
     * @param inputContainerPath  null if entire database
     * @param outputContainerName collection name to save results in. unused if !isExecute
     */
    public static void runQuery(File queryFile, String inputContainerPath, String outputContainerName) {
        Element graphQueryEle = null;
        try {
            log.info("* processing query: " + queryFile);
            graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        } catch (Exception exc) {
            log.error("error reading query file", exc);
        }
        queryGraphInternal(graphQueryEle, inputContainerPath, outputContainerName);
    }


    /**
     * Overload that uses a URL to specify the query instead of a File. This
     * overload is used by DataGenerator, which possibly loads XML files from
     * JAR files.
     *
     * @param fileURL
     * @param inputContainerPath
     * @param outputContainerName
     */
    public static void runQuery(URL fileURL, String inputContainerPath, String outputContainerName) {
        Element graphQueryEle = null;
        try {
            log.info("* processing query: " + fileURL);
            graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(fileURL);
        } catch (Exception exc) {
            log.error("error reading query file", exc);
        }
        queryGraphInternal(graphQueryEle, inputContainerPath, outputContainerName);
    }

    private static void queryGraphInternal(Element graphQueryEle, String inputContainerPath, String outputContainerName) {
        Container inputContainer = setupContainer(inputContainerPath, outputContainerName);

        try {
            // read the graph query xml file and pass it to queryGraph()
            queryGraph(graphQueryEle, inputContainer, outputContainerName, true);
            log.info("* done executing query");
        } catch (QGValidationError qgValErr) {
            log.error("error processing query:");
            List errorList = qgValErr.getErrorList();
            for (Iterator iterator = errorList.iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                log.error("  --> " + s);
            }
        } catch (Exception exc) {
            log.error("error processing query", exc);
        }
    }

    private static Container setupContainer(String inputContainerPath, String outputContainerName) {
        // get the input container, if not null. NB: we allow a syntax that is
        // different from the ProxURL syntax used internally. specifically,
        // '/' and '' are treated as representing the entire db
        Container inputContainer = null;
        try {
            if (inputContainerPath != null && inputContainerPath.length() > 0 &&
                    !("/".equals(inputContainerPath))) {
                ProxURL proxURL = new ProxURL("cont:/containers" + inputContainerPath);
                inputContainer = proxURL.getContainer(false);
            }
        } catch (IllegalArgumentException iaExc) {
            throw new IllegalArgumentException("invalid input container path: '" +
                    inputContainerPath + "'");
        }
        Container container = DB.getRootContainer();
        if (container.hasChild(outputContainerName)) {
            log.info("* deleting container: '" + outputContainerName + "'");
            container.deleteChild(outputContainerName);
        }
        return inputContainer;
    }

    /**
     * "Result" accessor. Useful to callers when processing is complete.
     *
     * @return the selected TGPath instance in my tfmGraph. null if errors.
     */
    public TGPath tgPath() {
        return tgPath;
    }


    /**
     * Called by chooseTGPath(), returns a List of TGPaths through tfmGraph that
     * are solutions, i.e., paths that end at a TGVertex that contains a
     * consolidated Query. The returned List is empty if there are no solutions.
     */
    public static List tgPathsFromTFMGraph(TFMGraph tfmGraph) {
        return tgPathsToSolTGVertex(tfmGraph.startVertex());
    }


    /**
     * Called by tgPathsFromTFMGraph(), returns a List of TGPaths starting from
     * tgVertex that lead to a solution. The returned List is empty if there are no
     * solutions from tgVertex.
     */
    private static List tgPathsToSolTGVertex(TGVertex tgVertex) {
        ArrayList tgPaths = new ArrayList();    // return value. filled next
        Iterator tgEdgeIter = tgVertex.edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            TGVertex childTGVertex = tgEdge.vertex();
            if (childTGVertex.query().isConsolidated()) {
                tgPaths.add(new TGPath(tgVertex, tgEdge));    // found a final edge!
            } else {
                List childTGPaths = tgPathsToSolTGVertex(childTGVertex);    // recurse
                Iterator tgPathIter = childTGPaths.iterator();
                while (tgPathIter.hasNext()) {
                    TGPath tgPath = (TGPath) tgPathIter.next();
                    tgPath.pushEdge(tgEdge);        // add my edge to the start
                }
                tgPaths.addAll(childTGPaths);
            }
        }
        return tgPaths;
    }


    /**
     * "Result" accessor. Useful to callers when processing is complete.
     *
     * @return the final TFMGraph instance. null if errors.
     */
    public TFMGraph tfmGraph() {
        return tfmGraph;
    }


    /**
     * Step 1/4 called from constructor, checks query's validity. Throws
     * Exception if invalid. Otherwise (valid) returns normally.
     *
     * @throws Exception if invalid
     */
    private void validateQuery() throws Exception {
        new QueryValidator(query);    // throws Exception
    }
}
