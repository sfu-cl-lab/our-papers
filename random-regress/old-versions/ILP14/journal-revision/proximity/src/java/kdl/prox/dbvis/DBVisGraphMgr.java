/**
 * $Id: DBVisGraphMgr.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Class that uses JUNG (http://jung.sourceforge.net/) to support incremental
 * exploratory visualization of a Proximity database. Operations are reflected
 * in a Graph instance. Graph changes are reflected in GraphEdit instances.
 * Also maitains a History of Vertex instances that were expanded. NB: When
 * expansion limits are reached adds special 'pager' vertices and edges to
 * represent remaining un-shown graph elements.
 * <p/>
 * See ProxSparseVertex regarding special <i>pseudo</i> and <i>pager</i> vertices
 * and edges.
 *
 * @see ProxSparseVertex
 */
public class DBVisGraphMgr {

    private static final Logger log = Logger.getLogger(DBVisGraphMgr.class);

    private Graph graph = new SparseGraph();
    private List history = new ArrayList();     // Vertex expand history. 0th is oldest, last is newest

    private String linkLabelAttrName = null;    // link attribute name for vertex labels. null if none
    private String objLabelAttrName = null;     // object ""
    private String objColorAttrName = null;     // object attribute name for vertex colors. null if none


    public DBVisGraphMgr() {
    }

    public DBVisGraphMgr(String linkLabelAttrName,
                         String objLabelAttrName, String objColorAttrName) {
        this();
        setItemLabelAttribute(linkLabelAttrName, false);
        setItemLabelAttribute(objLabelAttrName, true);
        setObjectColorAttribute(objColorAttrName);
    }

    /**
     * Adds a new regular (i.e., non-pseudo) Edge to my graph that represents
     * linkOID. o1Vertex and o2Vertex must both be in my graph.
     *
     * @param linkOID  pass null if unused
     * @param o1Vertex
     * @param o2Vertex
     * @return the new Edge
     */
    private ProxDirectedSparseEdge addEdgeForLinkID(Integer linkOID, Vertex o1Vertex, Vertex o2Vertex) {
        Assert.condition(o1Vertex.getGraph() == graph, "vertex not in graph: " + o1Vertex);
        Assert.condition(o2Vertex.getGraph() == graph, "vertex not in graph: " + o2Vertex);
        ProxDirectedSparseEdge edge = new ProxDirectedSparseEdge(o1Vertex, o2Vertex, linkOID);
        graph.addEdge(edge);
        return edge;
    }

    public ProxSparseVertex addVertex(ProxSparseVertex vertex) {
        graph.addVertex(vertex);
        return vertex;
    }

    /**
     * Adds a new Vertex to my graph that represents objOID. One-arg overload
     * that verifies Vertex's OID exists in object table.
     * <p/>
     * A note on label attributes: addVertexForObjID() does *not* retrieve label
     * (or any other) attributes - it simply initializes the graph, not touching
     * the DB at all. You must call expandVertex() for attributes to be retrieved.
     *
     * @param objOID
     * @return
     * @see #expandVertex
     */
    public ProxSparseVertex addVertexForObjID(Integer objOID) {
        return addVertexForObjID(objOID, true);
    }

    /**
     * Adds a new regular (i.e., non-pseudo) Vertex to my graph that represents objOID.
     *
     * @param objOID           OID of an object to add to my graph. pass null if
     *                         unused (isShouldCheckOID should be false, though)
     * @param isShouldCheckOID true if should verify Vertex's OID exists in object table. false o/w
     * @return the new Vertex
     * @throws IllegalArgumentException if objOID not in database
     */
    private ProxSparseVertex addVertexForObjID(Integer objOID, boolean isShouldCheckOID) {
        if (objOID != null) {
            Vertex existVert = (Vertex) getVertOrEdgeForOID(objOID, true);
            Assert.condition(existVert == null, "already have vertex " +
                    "for objOID: " + objOID);
        }

        if (isShouldCheckOID) {
            Assert.condition(isObjectOIDValid(objOID), "no object in db for " +
                    "id: " + objOID);
        }

        ProxSparseVertex vertex = new ProxSparseVertex(objOID);
        return addVertex(vertex);
    }

    /**
     * Clears my graph and history by removing all vertices, edges, and history
     * items.
     */
    public void clear() {
        graph.removeAllVertices();
        graph.removeAllEdges();
        history.clear();
    }

    /**
     * Resets all vertex or edge labels (depending on isObject) to null.
     *
     * @param isObject
     */
    private void clearItemLabelAttributes(boolean isObject) {
        Set vertsOrEdges = isObject ? graph.getVertices() : graph.getEdges();
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            vertOrEdge.setLabel(null);
        }
    }

    /**
     * Expands vertex in one of two ways, depending on whether it is a pager or
     * not. If not, it adds Vertex instances to my graph for every object
     * connected to vertex, i.e., adds its 1D neighborhood, including Edge
     * instances for each link. Honors vertex and edge paging limits, based on
     * maxDegreePerVert arg. In the pager case, expands the "real" Vertex on the
     * other end of vertex, adding the number of vertices stored in vertex's
     * pageSize(), and ignoring maxDegreePerVert.
     * <p/>
     * Note: This method disconnects vertex if it's a pager, which breaks
     * subsequent getPagedVertexFromPager() calls. Work-around: Call that method
     * *before* calling this one.
     * <p/>
     * todo maxNumEdgesPerVertPair
     *
     * @param vertex           A Vertex from my graph
     * @param maxDegreePerVert ignored if vertex is a pager
     * @return a GraphEdit instance that details Vertex and Edge instances added
     */
    public GraphEdit expandVertex(ProxSparseVertex vertex, int maxDegreePerVert) {
        Assert.condition(vertex.getGraph() == graph, "vertex not in graph: " + vertex);

        // choose the actual vertex to expand based on whether vertex is a pager
        boolean isPager = vertex.isPager();
        GraphEdit graphEdit = new GraphEdit();
        if (!isPager && isVertexOnHistory(vertex)) {
            return graphEdit;
        }

        ProxSparseVertex vertexToExpand;    // the actual Vertex whose OID we'll use to find links
        if (isPager) {
            vertexToExpand = getPagedVertexFromPager(vertex);
            maxDegreePerVert = vertex.getPageSize() + vertexToExpand.inDegree() +
                    vertexToExpand.outDegree() - 1;     // -1 for pager itself
        } else {
            vertexToExpand = vertex;
        }

        // get the links connected to vertex's object in either direction
        Integer oidInt = vertexToExpand.getOID();
        int objOID = oidInt.intValue();
        NST linkNST = DB.getLinkNST();
        NST o1o2NST = linkNST.filter("o1_id = " + objOID + " OR o2_id = " + objOID);  // objOID, "link_id, o1_id, o2_id", "*");

        // honor maxDegreePerVert by checking the number of unique object OIDs
        // in o1o2NST. if > maxDegreePerVert then limit o1o2NST to those links
        // involved in a subset of the first maxDegreePerVert unique object OIDs.
        // in this case we also add a special 'pager' vertex as an indicator
        NST[] distAndLimitNSTs = makeOIDMaxDegreeLimitNSTs(o1o2NST, objOID, maxDegreePerVert);
        if (distAndLimitNSTs != null) {
            NST distinctOIDsNST = distAndLimitNSTs[0];
            NST oidLimitNST = distAndLimitNSTs[1];
            int numLimitObjs = oidLimitNST.getRowCount() - 1;  // s/b maxDegreePerVert
            int numTotalObjs = distinctOIDsNST.getRowCount();
            NST limitedO1NST = o1o2NST.intersect(oidLimitNST, "o1_id EQ id");
            NST limitedO1o2NST = limitedO1NST.intersect(oidLimitNST, "o2_id EQ id");
            limitedO1NST.release();
            distinctOIDsNST.release();
            oidLimitNST.release();
            // todo xx o1o2NST.release();
            o1o2NST = limitedO1o2NST;

            ProxSparseVertex pagerVertex;
            if (isPager) {
                pagerVertex = vertex;
            } else {
                pagerVertex = addVertexForObjID(null, false);
                pagerVertex.setPager(Boolean.TRUE);        // isPseudo && isPager
                pagerVertex.setPageSize(maxDegreePerVert);
                graphEdit.noteAddedVertex(pagerVertex);

                ProxDirectedSparseEdge connPseudoEdge = addEdgeForLinkID(null,
                        vertexToExpand, pagerVertex);
                connPseudoEdge.setPager(Boolean.FALSE);    // isPseudo && !isPager
                graphEdit.noteAddedEdge(connPseudoEdge);
            }
            pagerVertex.setPagerNumShown(numLimitObjs);
            pagerVertex.setPagerNumTotal(numTotalObjs);
        } else if (isPager) {            // have a pager, but degree didn't get limited this time -> remove pager
            graph.removeVertex(vertex);     // removes its edge as well
        }

        // add the new Vertex and Edge objects
        ResultSet resultSet = o1o2NST.selectRows();
        while (resultSet.next()) {
            Integer linkOID = new Integer(resultSet.getOID(1));
            Integer o1OID = new Integer(resultSet.getOID(2));
            Integer o2OID = new Integer(resultSet.getOID(3));

            // create Vertex and Edge instances if don't exist
            Edge edge = (Edge) getVertOrEdgeForOID(linkOID, false);
            Vertex o1Vertex = (Vertex) getVertOrEdgeForOID(o1OID, true);
            Vertex o2Vertex = (Vertex) getVertOrEdgeForOID(o2OID, true);
            if (o1Vertex == null) {
                o1Vertex = addVertexForObjID(o1OID, false);
                graphEdit.noteAddedVertex(o1Vertex);
            }
            if (o2Vertex == null) {
                o2Vertex = addVertexForObjID(o2OID, false);
                graphEdit.noteAddedVertex(o2Vertex);
            }
            if (edge == null) {
                edge = addEdgeForLinkID(linkOID, o1Vertex, o2Vertex);
                graphEdit.noteAddedEdge(edge);
            }
        }

        // get object and link label attributes if necessary
        saveItemAttrs(o1o2NST, objLabelAttrName, true, true);
        saveItemAttrs(o1o2NST, linkLabelAttrName, false, true);

        // get object color attributes if necessary
        saveItemAttrs(o1o2NST, objColorAttrName, true, false);

        // done
        o1o2NST.release();
        if (!isPager) {
            history.add(vertexToExpand);
        }
        return graphEdit;
    }

    /**
     * @return my Graph. NB: Does not return a copy.
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * @return my history, a List of Vertex instances with the first one (0th)
     *         being the the oldest and the last being the newest. NB: Does not
     *         return a copy.
     */
    public List getHistory() {
        return history;
    }

    public String getItemLabelAttribute(boolean isObject) {
        if (isObject) {
            return objLabelAttrName;
        } else {
            return linkLabelAttrName;
        }
    }

    public String getObjColorAttribute() {
        return objColorAttrName;
    }

    /**
     * @param pageVertex
     * @return original ("paged") Vertex that pageVertex is showing % of
     */
    public ProxSparseVertex getPagedVertexFromPager(ProxSparseVertex pageVertex) {
        Edge incidentEdge = (Edge) pageVertex.getIncidentEdges().iterator().next();
        return (ProxSparseVertex) incidentEdge.getOpposite(pageVertex);
    }

    public ProxItemData getVertOrEdgeForOID(Integer itemOID, boolean isObject) {
        return DBVisGraphMgr.getVertOrEdgeForOID(graph, itemOID.intValue(), isObject);
    }

    /**
     * @param graph
     * @param itemOID
     * @param isObject
     * @return Vertex or Edge (as a UserDataContainer) in graph corresponding to
     *         itemOID. returns null if none found
     */
    public static ProxItemData getVertOrEdgeForOID(Graph graph, int itemOID, boolean isObject) {
        // NB: implementation could be sped up by saving in objOID->Vert Map
        Set verticesOrEdges = (isObject ? graph.getVertices() : graph.getEdges());
        for (Iterator vertOrEdgeIter = verticesOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            Integer oid = vertOrEdge.getOID();
            if ((oid != null) && (itemOID == oid.intValue())) {
                return vertOrEdge;
            }
        }
        return null;
    }

    /**
     * @param objOID
     * @return true if objOID in database. returns false o/w
     */
    public boolean isObjectOIDValid(Integer objOID) {
        NST objectNST = DB.getObjectNST();
        NST matchingObjOIDs = objectNST.filter("id EQ " + objOID);
        int rowCount = matchingObjOIDs.getRowCount();
        matchingObjOIDs.release();
        return rowCount == 1;
    }

    /**
     * @return true if vertex is in my history. returns false if not
     */
    public boolean isVertexOnHistory(Vertex vertex) {
        return history.contains(vertex);
    }

    /**
     * Creates and returns two NSTs to be used for filtering objects when maxDegreePerVert
     * is exceeded. NB: It's the caller's responsibility to release the returned
     * NSTs if non-null.
     *
     * @param o1o2NST
     * @param expVertexOID     OID of Vertex being expanded. it is excluded from the count
     * @param maxDegreePerVert
     * @return either an array of two NSTs ([uniqueNeighbors, someNeighbors]),
     *         or null if number of unique object OIDs in o1o2NST does not exceed
     *         maxDegreePerVert.
     */
    private NST[] makeOIDMaxDegreeLimitNSTs(NST o1o2NST, int expVertexOID,
                                            int maxDegreePerVert) {
        NST o1NST = o1o2NST.project("o1_id");
        NST o2NST = o1o2NST.project("o2_id");
        NST uniqueNeighbors = o1NST.union(o2NST);
        uniqueNeighbors.renameColumn("o1_id", "id");
        uniqueNeighbors.deleteRows("id EQ " + expVertexOID);

        int numObjs = uniqueNeighbors.getRowCount();
        if (numObjs > maxDegreePerVert) {
            NST someNeighbors = uniqueNeighbors.rangeSorted("id", "0-" + (maxDegreePerVert - 1));
            someNeighbors.insertRow(new String[]{expVertexOID + ""});
            return new NST[]{uniqueNeighbors, someNeighbors};
        } else {
            uniqueNeighbors.release();
            return null;
        }
    }

    /**
     * Re-sets vertex or link labels (based on isObject) by re-loading attribute
     * data from the database.
     *
     * @param isObject object vs. link
     * @param isLabel  label vs. color
     */
    public void refreshItemAttributes(boolean isObject, boolean isLabel) {
        // the approach we use here to get the o1o2NST is the simplest, but not
        // the most efficient: we iterate through all the graph's vertices/edges,
        // collect the OIDs (Integers), create a new NST containing them (sending
        // the data to the server - slow), then work from there. the other choice
        // is to change this class to manage a current "session" NST that is
        // updated with each expand or clear. two other solutions included creating
        // a file to bulk import from, and re-running expand on each vertex in
        // the history
        Assert.condition(isObject || isLabel, "setting link colors invalid");

        // collect the object or link OIDS
        ArrayList itemOIDStrs = new ArrayList();    // Strings, one for each OID
        Set vertsOrEdges = isObject ? graph.getVertices() : graph.getEdges();
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            if (vertOrEdge.isPseudo()) {
                continue;
            }

            Integer label = vertOrEdge.getOID();
            String value = label.toString() + "@0";
            itemOIDStrs.add(value);
        }

        String attrName = isObject ? (isLabel ? objLabelAttrName : objColorAttrName)
                : linkLabelAttrName;
        if (attrName == null) {
            return;
        }

        NST idNST = new NST("id", "oid").insertRows(itemOIDStrs);
        String idBatVar = idNST.getNSTColumn("id").getBATName();
        String filterDef = "id IN " + idBatVar;
        saveItemAttrsInternal(filterDef, attrName, isObject, isLabel);
        idNST.release();
    }

    /**
     * Retrieves object or link attribute infromation from o1o2NST, based on
     * isLabel, attrName, and isObject, and saves the last value (if any) in the
     * corresponding Vertex or Edge.
     *
     * @param o1o2NST  same columns as NSTTypeEnum.LINK_NST_COL_NAMES, filtered
     *                 to contain those links of interest. NB: the "link_id" column
     *                 is used if !isObject. o/w the o1_id and o2_id columns are used
     * @param attrName
     * @param isObject
     * @param isLabel  true if attrName is a label attribute. false if a color one
     */
    private void saveItemAttrs(NST o1o2NST, String attrName,
                               boolean isObject, boolean isLabel) {
        if (attrName == null) {
            return;
        }

        String linkIDBatVar = o1o2NST.getNSTColumn("link_id").getBATName();
        String o1IDBatVar = o1o2NST.getNSTColumn("o1_id").getBATName();
        String o2IDBatVar = o1o2NST.getNSTColumn("o2_id").getBATName();
        String filterDef = (isObject ? "id IN " + o1IDBatVar +
                " or id IN " + o2IDBatVar : "id IN " + linkIDBatVar);
        saveItemAttrsInternal(filterDef, attrName, isObject, isLabel);
    }

    /**
     * Sets label or color values for each vertex or edge (depending on isObject)
     * in resultSet.
     *
     * @param filterDef
     * @param attrName
     * @param isObject
     * @param isLabel
     */
    private void saveItemAttrsInternal(String filterDef, String attrName,
                                       boolean isObject, boolean isLabel) {
        Assert.condition(isObject || isLabel, "setting link colors invalid");

        Attributes attrs = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        NST attrDataNST = attrs.getAttrDataNST(attrName);
        NST filteredAttrDataNST = attrDataNST.filter(filterDef);
        ResultSet resultSet = filteredAttrDataNST.selectRows();
        while (resultSet.next()) {
            Integer itemOID = new Integer(resultSet.getOID(1));
            String itemValue = resultSet.getString(2);
            ProxItemData vertOrEdge = (ProxItemData) getVertOrEdgeForOID(itemOID, isObject);
            if (vertOrEdge == null) {
                log.error("couldn't find vertex or edge for item: " + attrName +
                        ", " + isObject + ", " + itemOID + ", " + itemValue +
                        ", " + vertOrEdge);
            } else if (isLabel) {
                vertOrEdge.setLabel(itemValue);
            } else {    // !isLabel -> isColor
                vertOrEdge.setColor(itemValue);
            }
        }
        filteredAttrDataNST.release();
    }

    /**
     * Sets the object or link attribute used for labels to attrName, based on
     * isObject. Results in user data being set for future expansions.
     *
     * @param attrName name of attribute to use for labels. pass null to clear
     * @param isObject
     * @throws IllegalArgumentException if attrName is not defined, or is multi-columned
     * @see #expandVertex
     */
    public void setItemLabelAttribute(String attrName, boolean isObject) {
        Attributes attrs = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        if (attrName != null) {
            Assert.condition(attrs.isAttributeDefined(attrName),
                    "undefined attribute: " + attrName);
            Assert.condition(attrs.isSingleValued(attrName),
                    "attribute not single-valued: " + attrName);
        }
        if (isObject) {
            objLabelAttrName = attrName;
        } else {
            linkLabelAttrName = attrName;
        }
        clearItemLabelAttributes(isObject);
    }

    /**
     * Sets the object attribute used for vertex color to attrName. Results in
     * user data being set for future expansions.
     *
     * @param attrName name of attribute to use for object colors. pass null to clear
     * @throws IllegalArgumentException if attrName is not defined, or is multi-columned
     */
    public void setObjectColorAttribute(String attrName) {
        if (attrName != null) {
            Attributes attrs = DB.getObjectAttrs();
            Assert.condition(attrs.isAttributeDefined(attrName),
                    "undefined attribute: " + attrName);
            Assert.condition(attrs.isSingleValued(attrName),
                    "attribute not single-valued: " + attrName);
        }
        objColorAttrName = attrName;
    }

}
