/**
 * $Id: GUIContentGenerator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.Graph;
import kdl.prox.app.NSTBrowserJFrame;
import kdl.prox.db.AttributeValue;
import kdl.prox.db.AttributeValues;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTColumn;
import kdl.prox.dbvis.DBVisualizerJFrame;
import kdl.prox.dbvis.ProxDirectedSparseEdge;
import kdl.prox.dbvis.ProxSparseVertex;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This is the central class that knows how to generate HTML for GUI screens,
 * based on a Proximity URL.
 */
public class GUIContentGenerator {

    private static Logger log = Logger.getLogger(GUIContentGenerator.class);

    public static final String ATTR_VAL_PARAM = "ATTRVALS";             // parameter indicating attribute values of the thing should be shown
    public static final String SORT_BY_COUNT_PARAM = "SORT_BY_COUNT";   // parameter controlling histogram sort
    public static final String SORT_BY_VALUE_PARAM = "SORT_BY_VALUE";   // ""
    private static final int DEFAULT_ROWS_PER_PAGE = 200;
    private static final int DEFAULT_PAGES_PER_PAGER = 5;

    static final String TABLE_PROPERTIES = "style=\"text-align: left; " +
            "width: 90%;\" border=\"2\" cellspacing=\"2\" cellpadding=\"2\"";   // for <table> tags

    private int rowsPerPage = GUIContentGenerator.DEFAULT_ROWS_PER_PAGE;

    // attributes to use as labels in place of
    private String objAttrName = null;
    private String linkAttrName = null;
    private int objectLabelLength = 25; //25 characters max length; limit
    private int linkLabelLength = 25; //25 characters max length; limit


    public GUIContentGenerator() {
    }


    /**
     * Adds attribute histogram information to htmlSB, using the passed value
     * column information.
     *
     * @param htmlSB
     * @param proxURL
     * @param attrName
     * @param attrNST
     * @param valColName
     */
    private void addAttrHistogramHTML(StringBuffer htmlSB, ProxURL proxURL,
                                      String attrName, NST attrNST, String valColName) {

        DataTypeEnum valColType = attrNST.getNSTColumn(valColName).getType();

        boolean isContainerAttrs = proxURL.isContainerAttrs();
        boolean isObjectAttrs = proxURL.isObjectAttrs();
        String parameter = proxURL.getParameter();
        boolean isSortByValue = GUIContentGenerator.SORT_BY_VALUE_PARAM.equals(parameter);  // defaults to sort by count

        // Get the histogram, order
        NST histNST = attrNST.getColumnHistogramAsNST(valColName);
        NST sortedNST;
        if (isSortByValue) {
            sortedNST = histNST.sort(valColName, "*");
        } else {
            sortedNST = histNST.sort("count DESC", "*");
        }
        Pager pager = new Pager(sortedNST, proxURL.getPageNum(), rowsPerPage);
        ResultSet resultSet = pager.getResultSet(valColName + ", count");

        addHistogramTableStart(htmlSB, proxURL, valColName, valColType);
        while (resultSet.next()) {
            String value = resultSet.getString(1); // the first col is the order
            String count = resultSet.getString(2);
            htmlSB.append("<tr>\n");

            htmlSB.append("<td>");
            if (isContainerAttrs) {
                htmlSB.append(value);
            } else {
                htmlSB.append("<a href=\"filter:/");
                htmlSB.append(isObjectAttrs ? "objects/" : "links/");
                htmlSB.append(attrName);
                htmlSB.append("/");
                htmlSB.append(valColName);
                htmlSB.append("/");
                htmlSB.append((valColType == DataTypeEnum.STR || valColType == DataTypeEnum.DATE)
                        ? "'" + value + "'" : value);
                htmlSB.append("\">");
                htmlSB.append(value);
                htmlSB.append("</a>");
            }
            htmlSB.append("</td>\n");

            htmlSB.append("<td>");
            htmlSB.append(count);
            htmlSB.append("</td>\n");
            htmlSB.append("</tr>\n");
        }
        htmlSB.append("</table>\n");
        htmlSB.append(getPagerHTML(pager, proxURL));
        histNST.release();
        sortedNST.release();
    }


    private void addHistogramTableStart(StringBuffer htmlSB, ProxURL proxURL,
                                        String valColName, DataTypeEnum valColType) {
        htmlSB.append("<br>\n");
        htmlSB.append("<strong>column: ");
        htmlSB.append(valColName);
        htmlSB.append(" (<em>");
        htmlSB.append(valColType);
        htmlSB.append("</em>)</strong>\n");
        htmlSB.append("<table ");
        htmlSB.append(GUIContentGenerator.TABLE_PROPERTIES);
        htmlSB.append(">\n");
        htmlSB.append("<tr>\n");
        htmlSB.append("<td><a href=\"");
        htmlSB.append(proxURL.getProtocol());
        htmlSB.append(":");
        htmlSB.append(proxURL.getAddress());
        htmlSB.append("!");
        htmlSB.append(GUIContentGenerator.SORT_BY_VALUE_PARAM);
        htmlSB.append("\">value</a></td>\n");
        htmlSB.append("<td><a href=\"");
        htmlSB.append(proxURL.getProtocol());
        htmlSB.append(":");
        htmlSB.append(proxURL.getAddress());
        htmlSB.append("!");
        htmlSB.append(GUIContentGenerator.SORT_BY_COUNT_PARAM);
        htmlSB.append("\">count</a></td>\n");
        htmlSB.append("</tr>\n");
    }

    /**
     * @param proxURLStr
     * @return List of ProxAction instances to be used at the top of the page
     *         showing proxURLStr
     */
    public List getActionsForURL(final String proxURLStr) {
        ArrayList actionList = new ArrayList();
        final ProxURL proxURL = new ProxURL(proxURLStr);
        String[] components = proxURL.getAddressComponents();
        String protocol = proxURL.getProtocol();
        String parameter = proxURL.getParameter();
        if ("db".equals(protocol)) {
            String firstComponent = proxURL.getFirstAddressComponent();
            if (firstComponent == null) {   // todo why is this test here?
                actionList.add(new ProxAction("graph") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        DBVisualizerJFrame dbVisualizerJFrame = new DBVisualizerJFrame();
                        dbVisualizerJFrame.setVisible(true);
                    }
                });
                actionList.add(new ProxAction("analyze schema") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.runSchemaAnalysis();
                    }
                });
                actionList.add(new ProxAction("browse tables") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        NSTBrowserJFrame nstBrowser = new NSTBrowserJFrame();
                        nstBrowser.setVisible(true);
                    }
                });
            }
        } else if ("attr".equals(protocol)) {
            actionList.add(new ProxAction("delete") {
                public void performAction(BrowserJFrame browserJFrame) {
                    browserJFrame.deleteAttribute();
                }
            });
            actionList.add(new ProxAction("export") {
                public void performAction(BrowserJFrame browserJFrame) {
                    browserJFrame.exportAttribute();
                }
            });
        } else if ("attrdefs".equals(protocol)) {
            if (!"containers".equals(proxURL.getFirstAddressComponent())) {
                actionList.add(new ProxAction("create new") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        String url = browserJFrame.getCurrentProxURL();
                        ProxURL proxURL = new ProxURL(url);
                        Attributes attributes = proxURL.getAttributes(false);
                        new AttrCreationJFrame(attributes);
                    }
                });
            }
        } else if ("cont".equals(protocol)) {
            if (components.length == 2) { // root
                actionList.add(new ProxAttrsAction("view query", false));
                actionList.add(new ProxAttrsAction("attrs", false));
                actionList.add(new ProxAction("delete", false));
                actionList.add(new ProxAction("query", false));
                actionList.add(new ProxAction("thumbs", false));
            } else if (!ATTR_VAL_PARAM.equals(parameter)) {
                Container container = proxURL.getContainer(false); // only enabled if query for this cont is not null
                actionList.add(new ProxAction("view query", container.getQuery() != null) {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.showContainerQuery();
                    }
                });
                actionList.add(new ProxAttrsAction("attrs", true));
                actionList.add(new ProxAction("delete") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.deleteContainer();
                    }
                });
                actionList.add(new ProxAction("query") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.runQuery();
                    }
                });
                actionList.add(new ProxAction("thumbs") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        new SubgraphThumbsJFrame(proxURLStr);
                    }
                });
            }
        } else if ("item".equals(protocol)) {
            if (!ATTR_VAL_PARAM.equals(parameter)) {
                actionList.add(new ProxAttrsAction("attrs", true));

                String firstComponent = proxURL.getFirstAddressComponent();
                if ("objects".equals(firstComponent)) {
                    actionList.add(new ProxAction("graph", true) {
                        public void performAction(BrowserJFrame browserJFrame) {
                            String url = browserJFrame.getCurrentProxURL();
                            ProxURL proxURL = new ProxURL(url);
                            int itemOID = proxURL.getItemOID();
                            DBVisualizerJFrame dbVisualizerJFrame = new DBVisualizerJFrame();
                            dbVisualizerJFrame.graphFromStartingOID(itemOID + "");
                        }
                    });
                }
            }
        } else if ("subg".equals(protocol)) {
            if (!ATTR_VAL_PARAM.equals(parameter)) {
                actionList.add(new ProxAttrsAction("attrs", true));
                actionList.add(new ProxAction("graph") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.graphSubgraph();
                    }
                });

                // add the up/prev/next actions
                final String containerURL = proxURL.getAddressSansLastComponent();
                final int prev = GUIContentGenerator.getNextOrPrevSubgraphOID(proxURL, false);
                final int next = GUIContentGenerator.getNextOrPrevSubgraphOID(proxURL, true);
                final String subgPrefix = "subg:" + containerURL + "/";

                actionList.add(new ProxAction("prev", prev != -1) {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.goTo(subgPrefix + prev);
                    }
                });
                actionList.add(new ProxAction("next", next != -1) {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.goTo(subgPrefix + next);
                    }
                });
                actionList.add(new ProxAction("up") {
                    public void performAction(BrowserJFrame browserJFrame) {
                        browserJFrame.goTo("cont:" + containerURL);
                    }
                });
            }
        }
        return actionList;
    }

    public String getActionsHTML(List actionList) {
        StringBuffer sb = new StringBuffer();
        for (int actionIdx = 0; actionIdx < actionList.size(); actionIdx++) {
            ProxAction proxAction = (ProxAction) actionList.get(actionIdx);
            sb.append("[");
            if (proxAction.getIsEnabled()) {
                sb.append("<a href=\"");
                sb.append(BrowserJFrame.ACTION_URL_PATTERN);
                sb.append(actionIdx);
                sb.append("\">");
                sb.append(proxAction.getName());
                sb.append("</a>");
            } else {
                sb.append(proxAction.getName());
            }
            sb.append("]&nbsp;");
        }
        return sb.toString();
    }

    private String getAttributeBodyHTML(ProxURL proxURL) {
        String attrName = proxURL.getLastAddressComponent();
        Attributes attributes = proxURL.getAttributes(true);
        List attrTypes = attributes.getTypes(attrName);
        String attrTypeDef = attributes.getAttrTypeDef(attrName);
        NST attrDataNST = attributes.getAttrDataNST(attrName);
        List nstColumns = attrDataNST.getNSTColumns();
        int numRows = attrDataNST.getRowCount();
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<strong>Attribute: ");
        htmlSB.append(attrName);
        htmlSB.append("</strong><br>\n");
        htmlSB.append("<br>\n");
        htmlSB.append("row count: ");
        htmlSB.append(numRows);
        htmlSB.append("<br>\n");
        htmlSB.append("column count: ");
        htmlSB.append(attrTypes.size());
        htmlSB.append(" (");
        htmlSB.append(attrTypeDef);
        htmlSB.append(")<br>\n");
        for (int nstColIdx = 1; nstColIdx < nstColumns.size(); nstColIdx++) {   // skip ID column
            NSTColumn nstColumn = (NSTColumn) nstColumns.get(nstColIdx);
            String valColName = nstColumn.getName();
            addAttrHistogramHTML(htmlSB, proxURL, attrName, attrDataNST, valColName);
        }
        return htmlSB.toString();
    }


    private String getAttributesDefsBodyHTML(ProxURL proxURL) {
        String address = proxURL.getAddress();
        Attributes attributes = proxURL.getAttributes(false);
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<strong>Attribute Definitions</strong><br>\n");
        htmlSB.append("<br>\n");

        // for each attribute name, save the corresponding HTML in a List so
        // that we can sort it before adding to htmlSB. NB: it is much faster to
        // use 'internal' Attributes.getAttrNST() method than to call
        // getAttributeNames() then getAttrTypeDef() on each name
        List attrDefList = new ArrayList();
        NST attrNST = attributes.getAttrNST();
        ResultSet resultSet = attrNST.selectRows("name, data_type");
        while (resultSet.next()) {
            String attrName = resultSet.getString(1);
            String attrTypeDef = resultSet.getString(2);
            StringBuffer attrDefSB = new StringBuffer();
            attrDefSB.append("<a href=\"attr:");
            attrDefSB.append(address);
            attrDefSB.append("/");
            attrDefSB.append(attrName);
            attrDefSB.append("\">");
            attrDefSB.append(attrName);
            attrDefSB.append("</a> <em>");
            attrDefSB.append(attrTypeDef);
            attrDefSB.append("</em><br>\n");
            attrDefList.add(attrDefSB.toString());
        }
        Collections.sort(attrDefList);

        // add attrDefList to htmlSB
        for (Iterator attrDefIter = attrDefList.iterator(); attrDefIter.hasNext();) {
            String attrDef = (String) attrDefIter.next();
            htmlSB.append(attrDef);
        }

        return htmlSB.toString();
    }


    private String getAttrValsBodyHTML(Attributes attrs, int oid) {
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<strong>Attribute Values:</strong><br>\n");
        AttributeValues attrVals = attrs.getAttrValsForOID(oid, null);
        Iterator attrValueIter = attrVals.getAttrValues().iterator();
        while (attrValueIter.hasNext()) {
            AttributeValue attributeValue = (AttributeValue) attrValueIter.next();
            htmlSB.append(attributeValue);
            htmlSB.append("<br>\n");
        }
        return htmlSB.toString();
    }


    public String getBodyHTMLForURL(String proxURLStr) {
        ProxURL proxURL = new ProxURL(proxURLStr);
        String protocol = proxURL.getProtocol();
        if ("db".equals(protocol)) {
            String firstComponent = proxURL.getFirstAddressComponent();
            if (firstComponent == null) {
                return getTopBodyHTML();
            } else if ("objects".equals(firstComponent)) {
                return getItemSummaryHTML(proxURL, true);
            } else if ("links".equals(firstComponent)) {
                return getItemSummaryHTML(proxURL, false);
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + firstComponent);
            }
        } else if ("attr".equals(protocol)) {
            return getAttributeBodyHTML(proxURL);
        } else if ("attrdefs".equals(protocol)) {
            return getAttributesDefsBodyHTML(proxURL);
        } else if ("cont".equals(protocol)) {
            return getContainerBodyHTML(proxURL);
        } else if ("filter".equals(protocol)) {
            String firstComponent = proxURL.getFirstAddressComponent();
            if ("objects".equals(firstComponent) || "links".equals(firstComponent)) {
                return getFilteredItemBodyHTML(proxURL);
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + firstComponent);
            }
        } else if ("item".equals(protocol)) {
            String firstComponent = proxURL.getFirstAddressComponent();
            if ("objects".equals(firstComponent)) {
                return getItemBodyHTML(proxURL, true);
            } else if ("links".equals(firstComponent)) {
                return getItemBodyHTML(proxURL, false);
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + firstComponent);
            }
        } else if ("subg".equals(protocol)) {
            return getSubgraphBodyHTML(proxURL);
        } else {
            throw new IllegalArgumentException("can't handle protocol: " +
                    protocol);
        }
    }

    private String getContainerBodyHTML(ProxURL proxURL) {
        String parameter = proxURL.getParameter();
        if (parameter == null) {
            return getContainerSummaryBodyHTML(proxURL);
        } else if (ATTR_VAL_PARAM.equals(parameter)) {
            Container container = proxURL.getContainer(false);
            return getAttrValsBodyHTML(DB.getContainerAttrs(),
                    container.getOid());
        } else {
            throw new IllegalArgumentException("cant' handle parameter: " + parameter);
        }
    }


    private String getContainerSummaryBodyHTML(ProxURL proxURL) {
        Container container = proxURL.getContainer(false);
        if (!container.isRootContainer() && container.getSubgraphCount() > 0) {
            return getContainerWithSubgraphsSummaryBodyHTML(container, proxURL);
        } else {
            return getContainerWithoutSubgraphsSummaryBodyHTML(container, proxURL);
        }
    }


    /**
     * Container with subgraphs. Paging for subgraphs, not for containers
     *
     * @param container
     * @param proxURL
     * @return
     * @
     */
    private String getContainerWithSubgraphsSummaryBodyHTML(Container container, ProxURL proxURL) {
        StringBuffer htmlSB = new StringBuffer();
        Iterator childNameIter = container.getChildrenNames().iterator();
        while (childNameIter.hasNext()) {
            String childName = (String) childNameIter.next();
            htmlSB.append("<a href=\"cont:");
            htmlSB.append(proxURL.getAddress());
            htmlSB.append("/");
            htmlSB.append(childName);
            htmlSB.append("\">");
            htmlSB.append(childName);
            htmlSB.append("</a><br>\n");
        }
        NST subgIdNST = container.getDistinctSubgraphOIDs();
        NST sortedSubgIDNST = subgIdNST.sort("subg_id", "subg_id");
        Pager pager = new Pager(sortedSubgIDNST, proxURL.getPageNum(), rowsPerPage);
        ResultSet resultSet = pager.getResultSet("subg_id");
        if (pager.getNumRows() > 0) {
            htmlSB.append("<strong>Subgraphs (");
            htmlSB.append(pager.getNumRows());
            htmlSB.append("):</strong><br>\n");
            while (resultSet.next()) {
                int subgOID = resultSet.getOID(1);
                htmlSB.append("<a href=\"subg:");
                htmlSB.append(proxURL.getAddress());
                htmlSB.append("/");
                htmlSB.append(subgOID);
                htmlSB.append("\">");
                htmlSB.append(subgOID);
                htmlSB.append("</a> &nbsp;");
            }
            htmlSB.append(getPagerHTML(pager, proxURL));
        }
        subgIdNST.release();
        sortedSubgIDNST.release();
        return htmlSB.toString();
    }


    /**
     * Container without subgraphs.
     *
     * @param container
     * @param proxURL
     * @return
     * @
     */
    private String getContainerWithoutSubgraphsSummaryBodyHTML(Container container, ProxURL proxURL) {
        StringBuffer htmlSB = new StringBuffer();
        NST containerNST = container.getChildrenNST();
        Pager pager = new Pager(containerNST, proxURL.getPageNum(), rowsPerPage);
        ResultSet resultSet = pager.getResultSet("name");
        while (resultSet.next()) {
            String childName = resultSet.getString(1);
            htmlSB.append("<a href=\"cont:");
            htmlSB.append(proxURL.getAddress());
            htmlSB.append("/");
            htmlSB.append(childName);
            htmlSB.append("\">");
            htmlSB.append(childName);
            htmlSB.append("</a><br>\n");
        }
        containerNST.release();
        htmlSB.append(getPagerHTML(pager, proxURL));

        return htmlSB.toString();
    }


    private String getFilteredItemBodyHTML(ProxURL proxURL) {
        List filterSpec = proxURL.getFilterSpec();
        String typeName = (String) filterSpec.get(0);
        String attrName = (String) filterSpec.get(1);
        String colName = (String) filterSpec.get(2);
        String attrVal = (String) filterSpec.get(3);
        boolean isObject = "objects".equals(typeName);
        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        NST attrDataNST = attributes.getAttrDataNST(attrName);
        NST filteredNST = attrDataNST.filter(colName + " EQ " + attrVal);
        NST uniqueNST = filteredNST.distinct("id");
        if (!isObject) {
            uniqueNST.renameColumn("id", "link_id"); // for use in getItemPageWithLabels...
        }

        String page = getItemPageWithLabelsAndPager(proxURL, uniqueNST, isObject);
        filteredNST.release();
        uniqueNST.release();
        return page;
    }

    private String getFilteredItemLocation(ProxURL proxURL) {
        List filterSpec = proxURL.getFilterSpec();
        String typeName = (String) filterSpec.get(0);
        String attrName = (String) filterSpec.get(1);
        String colName = (String) filterSpec.get(2);
        String attrVal = (String) filterSpec.get(3);

        StringBuffer htmlSB = new StringBuffer();
        if ("objects".equals(typeName)) {
            htmlSB.append("Filtered Objects: ");
        } else if ("links".equals(typeName)) {
            htmlSB.append("Filtered Links: ");
        } else {
            throw new IllegalArgumentException("filter protocol can't handle first " +
                    "component: " + typeName);
        }
        htmlSB.append(attrName);
        htmlSB.append("[");
        htmlSB.append(colName);
        htmlSB.append("] = ");
        htmlSB.append(attrVal);
        return htmlSB.toString();
    }


    /**
     * In the subgraph table we don't store information about exactly which
     * objects a particular link is connecting. For that information we query
     * the database-wide link table. The problem is that the same objectID can
     * appear more than once in a subgraph; if so, we cannot tell which instance
     * of that objectID is to be connected by the given link.
     * <p/>
     * We resolve this by showing the two (or more) instances of the objectID,
     * and connecting all of them with multiple copies of the same link.
     *
     * @param proxURL subgraph URL to create the graph from
     * @param graph   Graph to fill with objects and links in the subgraph specified
     *                by subgURL. NB: cleared first!
     */
    public static void getGraphForSubgURL(ProxURL proxURL, Graph graph) {
        graph.removeAllVertices();
        graph.removeAllEdges();

        // process the objects. save mappings of object IDs to a *Set* of
        // ProxVertex instances. we use a Set because multiple instances of the
        // same object id can have different names
        Subgraph subgraph = proxURL.getSubgraph();
        Map objectIDToVerticesMap = new HashMap();
        NST subgObjectNST = subgraph.getSubgObjectNST();
        ResultSet objectRS = subgObjectNST.selectRows("item_id, name");
        while (objectRS.next()) {
            int itemID = objectRS.getOID(1);
            String name = objectRS.getString(2);
            Integer itemIDInt = new Integer(itemID);
            ProxSparseVertex proxVertex = new ProxSparseVertex(itemIDInt, name);
            List vertices = (List) objectIDToVerticesMap.get(itemIDInt);
            if (vertices == null) {
                vertices = new ArrayList();
                objectIDToVerticesMap.put(itemIDInt, vertices);
            }
            proxVertex.setName(name);
            vertices.add(proxVertex);
            graph.addVertex(proxVertex);
        }
        subgObjectNST.release();

        // process the links, but first join with the global link table to get
        // the ends
        NST subgLinkNST = subgraph.getSubgLinkNST();
        NST linksAndNames = DB.getLinkNST().join(subgLinkNST, "link_id = item_id");
        ResultSet linkRS = linksAndNames.selectRows("link_id, o1_id, o2_id, name");
        while (linkRS.next()) {
            int itemID = linkRS.getOID(1);
            int o1ID = linkRS.getOID(2);
            int o2ID = linkRS.getOID(3);
            String name = linkRS.getString(4);
            List o1ProxVertices = (List) objectIDToVerticesMap.get(new Integer(o1ID));
            List o2ProxVertices = (List) objectIDToVerticesMap.get(new Integer(o2ID));
            Assert.notNull(o1ProxVertices, "couldn't find o1_id for link: " +
                    itemID + ", " + name);
            Assert.notNull(o2ProxVertices, "couldn't find o2_id for link: " +
                    itemID + ", " + name);

            Iterator o1ProxVertsIter = o1ProxVertices.iterator();
            while (o1ProxVertsIter.hasNext()) {
                ProxSparseVertex o1ProxVert = (ProxSparseVertex) o1ProxVertsIter.next();
                Iterator o2ProxVertsIter = o2ProxVertices.iterator();
                while (o2ProxVertsIter.hasNext()) {
                    ProxSparseVertex o2ProxVert = (ProxSparseVertex) o2ProxVertsIter.next();
                    ProxDirectedSparseEdge proxEdge = new ProxDirectedSparseEdge(o1ProxVert,
                            o2ProxVert, new Integer(itemID), name);
                    proxEdge.setName(name);
                    graph.addEdge(proxEdge);
                }
            }
        }

        linksAndNames.release();
        subgLinkNST.release();
    }


    /**
     * @param proxURL
     * @param isObject
     * @return HTML for an object or link
     * @
     */
    private String getItemBodyHTML(ProxURL proxURL, boolean isObject) {
        String parameter = proxURL.getParameter();
        if (parameter == null) {
            return getItemSummaryBodyHTML(proxURL, isObject);
        } else if (ATTR_VAL_PARAM.equals(parameter)) {
            int itemID = proxURL.getItemOID();
            Attributes attrs = (isObject ?
                    DB.getObjectAttrs() : DB.getLinkAttrs());
            return getAttrValsBodyHTML(attrs, itemID);
        } else {
            throw new IllegalArgumentException("can't handle parameter: " + parameter);
        }
    }


    /**
     * Returns the body of an object or link item.
     *
     * @param proxURL
     * @param isObject
     */
    private String getItemSummaryBodyHTML(ProxURL proxURL, boolean isObject) {
        StringBuffer htmlSB = new StringBuffer();

        if (isObject) {
            htmlSB.append(getObjectSummaryBodyHTML(proxURL));
        } else {
            htmlSB.append(getLinkSummaryBodyHTML(proxURL));
        }

        return htmlSB.toString();
    }


    private String getItemSummaryHTML(ProxURL proxURL, boolean isObject) {
        NST itemNST = (isObject ? DB.getObjectNST() :
                DB.getLinkNST());
        return getItemPageWithLabelsAndPager(proxURL, itemNST, isObject);
    }

    private String getItemPageWithLabelsAndPager(ProxURL proxURL, NST itemNST, boolean isObject) {
        String idColName = (isObject ? "id" : "link_id");
        NST labeledIDs = getLabel(itemNST, idColName, isObject);
        Pager pager = new Pager(labeledIDs, proxURL.getPageNum(), rowsPerPage);

        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("# ");
        htmlSB.append(isObject ? "objects" : "links");
        htmlSB.append(": ");
        htmlSB.append(pager.getNumRows());
        htmlSB.append("<br>\n");
        htmlSB.append("<br>\n");
        htmlSB.append("<strong>IDs:</strong><br>\n");

        ResultSet resultSet = pager.getResultSet(idColName + ", label");
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            htmlSB.append("<a href=\"item:/");
            htmlSB.append(isObject ? "objects" : "links");
            htmlSB.append("/");
            htmlSB.append(itemID);
            htmlSB.append("\">");
            if (resultSet.isColumnNil(2)) {
                htmlSB.append(itemID);
            } else {
                htmlSB.append(resultSet.getString(2));
            }
            htmlSB.append("</a> &nbsp;");
        }
        htmlSB.append(getPagerHTML(pager, proxURL));

        labeledIDs.release();
        return htmlSB.toString();
    }


    public String getLabelAttributeForObjects() {
        return objAttrName;
    }

    public String getLabelAttributeForLinks() {
        return linkAttrName;
    }

    /**
     * Returns an NST made up of original NST columns + label.
     * <p/>
     * Finds the appropriate label for each BUN and does a left outer join, to keep those IDs that don't have
     * the value of the attribute defined.
     *
     * @param itemsNST
     * @param colName  the name of the column to join with (id, link_id, item_id)
     * @param isObject
     */
    private NST getLabel(NST itemsNST, String colName, boolean isObject) {
        String attributeForLabel = (isObject ? objAttrName : linkAttrName);

        if (attributeForLabel == null) {
            NST retNST = itemsNST.copy();
            retNST.addConstantColumn("label", "str", null);
            return retNST;
        }

        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        NST attrDataNST = attributes.getAttrDataNST(attributeForLabel);
        NST uniqueAttrDataNST = attrDataNST.filter("id DISTINCT ROWS"); // get only one
        uniqueAttrDataNST.renameColumn("id", "internal_attr_id");       // so that it doesn't interfere with objects' id
        uniqueAttrDataNST.renameColumn("value", "label");               // so that there are no conflicts with other cols named Value
        NST retNST = itemsNST.leftOuterJoin(uniqueAttrDataNST, colName + " = internal_attr_id");

        int validLength = (isObject ? objectLabelLength : linkLabelLength);
        if (validLength != -1) {
            retNST.addSubstringColumn("label", "labelshort", 0, validLength);
            retNST.removeColumn("label").renameColumn("labelshort", "label");
        }
        retNST.removeColumn("internal_attr_id");

        uniqueAttrDataNST.release();
        attrDataNST.release();

        return retNST;
    }


    /**
     * Returns the label for a given item, or null if no label attribute has been
     * set, or the item doesn't have an assigned attribute value.
     *
     * @param itemOID
     * @param isObject
     */
    private String getLabelForItem(int itemOID, boolean isObject) {
        String label = null;
        String attributeForLabel = (isObject ? objAttrName : linkAttrName);
        if (attributeForLabel != null) {
            Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
            NST attrDataNST = attributes.getAttrDataNST(attributeForLabel);
            ResultSet resultSet = attrDataNST.selectRows("id = " + itemOID, "*");
            if (resultSet.next()) {
                label = resultSet.getString(2);
                int validLength = (isObject ? objectLabelLength : linkLabelLength);
                if (validLength != -1 && label.length() > validLength) {
                    label = label.substring(0, validLength);
                }
            }
        }
        return label;
    }

    /**
     * Returns information about a particular link
     *
     * @param proxURL
     */
    private String getLinkSummaryBodyHTML(ProxURL proxURL) {
        StringBuffer htmlSB = new StringBuffer();
        NST linkNST = DB.getLinkNST();
        int itemId = proxURL.getItemOID();
        ResultSet resultSet = linkNST.selectRows("link_id = " + itemId, "o1_id, o2_id");
        Assert.condition(resultSet.next(), "Link not found: " + itemId);
        int o1_id = resultSet.getOID(1);
        int o2_id = resultSet.getOID(2);
        htmlSB.append("<strong>From</strong>: ");
        htmlSB.append("<a href=\"item:/objects/");
        htmlSB.append(o1_id);
        htmlSB.append("\">");
        htmlSB.append(o1_id);
        String objectLabel = getLabelForItem(o1_id, true);
        if (objectLabel != null) {
            htmlSB.append(" (");
            htmlSB.append(objectLabel);
            htmlSB.append(")");
        }
        htmlSB.append("</a><br>\n");
        htmlSB.append("<strong>To</strong>: ");
        htmlSB.append("<a href=\"item:/objects/");
        htmlSB.append(o2_id);
        htmlSB.append("\">");
        htmlSB.append(o2_id);
        objectLabel = getLabelForItem(o2_id, true);
        if (objectLabel != null) {
            htmlSB.append(" (");
            htmlSB.append(objectLabel);
            htmlSB.append(")");
        }
        htmlSB.append("</a>");

        return htmlSB.toString();
    }


    /**
     * @param proxURLStr
     * @return location HTML to be used as a heading to show where proxURLStr is
     */
    String getLocationHTMLForURL(String proxURLStr) {
        ProxURL proxURL = new ProxURL(proxURLStr);
        StringBuffer htmlSB = new StringBuffer();
        String[] components = proxURL.getAddressComponents();
        if (components.length == 0) {       // just /
            return "<h2>Database</h2>";
        }

        String protocol = proxURL.getProtocol();
        htmlSB.append("<h2>");
        if ("cont".equals(protocol)) {
            htmlSB.append("Container: ");
            if (components.length == 2) {
                htmlSB.append("Root");
            } else {
                for (int compIdx = 2; compIdx < components.length; compIdx++) { // skip 'containers'
                    String compName = components[compIdx];
                    htmlSB.append(compName);
                    htmlSB.append(" ");
                }
            }
        } else if ("filter".equals(protocol)) {
            htmlSB.append(getFilteredItemLocation(proxURL));
        } else if ("item".equals(protocol)) {
            boolean isObject = "objects".equals(components[1]);
            htmlSB.append(isObject ? "Object: " : "Link: ");
            int itemOID = proxURL.getItemOID();
            htmlSB.append(itemOID);
            String objectOrLinkLabel = getLabelForItem(itemOID, isObject);
            if (objectOrLinkLabel != null) {
                htmlSB.append(" (");
                htmlSB.append(objectOrLinkLabel);
                htmlSB.append(")");
            }
        } else if ("attrdefs".equals(protocol)) {
            if ("objects".equals(components[1])) {
                htmlSB.append("Object Attributes");
            } else if ("links".equals(components[1])) {
                htmlSB.append("Link Attributes");
            } else if ("containers".equals(components[1])) {
                htmlSB.append("Container Attributes");
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + components[1]);
            }
        } else if ("attr".equals(protocol)) {
            if ("objects".equals(components[1])) {
                htmlSB.append("Object Attribute");
            } else if ("links".equals(components[1])) {
                htmlSB.append("Link Attribute");
            } else if ("containers".equals(components[1])) {
                htmlSB.append("Container Attribute");
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + components[1]);
            }
        } else if ("subg".equals(protocol)) {
            htmlSB.append("Subgraph: ");
            for (int compIdx = 2; compIdx < components.length; compIdx++) { // skip 'containers'
                String compName = components[compIdx];
                htmlSB.append(compName);
                htmlSB.append(" ");
            }
        } else if ("db".equals(protocol)) {
            if ("objects".equals(components[1])) {
                htmlSB.append("Objects");
            } else if ("links".equals(components[1])) {
                htmlSB.append("Links");
            } else if ("schema".equals(components[1])) {
                htmlSB.append("Schema Analysis");
            } else {
                throw new IllegalArgumentException("can't handle first " +
                        "component: " + protocol + ", " + components[1]);
            }
        } else {
            throw new IllegalArgumentException("can't handle protocol: " +
                    protocol);
        }
        htmlSB.append("</h2>");
        return htmlSB.toString();
    }

    /**
     * Utility that helps with calculating next and previous subgraphs.
     *
     * @param subgraphURL
     * @param isGoNext    true if "next"; false if "previous"
     * @return previous subgraph OID. returns -1 if can't go back any further
     */
    public static int getNextOrPrevSubgraphOID(ProxURL subgraphURL, boolean isGoNext) {
        NST containerObjectsNST = subgraphURL.getContainer(true).getObjectsNST();
        int currSubgId = Integer.parseInt(subgraphURL.getLastAddressComponent());
        final int prev = containerObjectsNST.filter("subg_id < " + currSubgId, "subg_id").max("subg_id");
        final int next = containerObjectsNST.filter("subg_id > " + currSubgId, "subg_id").min("subg_id");
        return (isGoNext ? next : prev);
    }


    /**
     * Returns information about a given object
     *
     * @param proxURL
     */
    private String getObjectSummaryBodyHTML(ProxURL proxURL) {
        // give error if object doesn't exist
        int itemId = proxURL.getItemOID();
        Assert.condition(DB.isObjectExists(itemId), "Object not found: " + itemId);

        NST connectedNST = DB.getObjectsConnectedTo(itemId);
        NST labeledIDs = getLabel(connectedNST, "id", true);

        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<strong>Connected to objects</strong><br>\n");
        Pager pager = new Pager(labeledIDs, proxURL.getPageNum(), rowsPerPage);
        ResultSet resultSet = pager.getResultSet();
        while (resultSet.next()) {
            int oid = resultSet.getOID(1);
            htmlSB.append("<a href=\"item:/objects/");
            htmlSB.append(oid);
            htmlSB.append("\">");
            if (resultSet.isColumnNil(2)) {
                htmlSB.append(oid);
            } else {
                htmlSB.append(resultSet.getString(2));
            }
            htmlSB.append("</a> &nbsp;");
        }
        htmlSB.append("\n");
        htmlSB.append(getPagerHTML(pager, proxURL));
        connectedNST.release();
        labeledIDs.release();
        return htmlSB.toString();
    }


    public String getPagerHTML(Pager pager, ProxURL proxURL) {
        if (pager.getNumPages() <= 1) {
            return "";
        }

        int currPageNum = pager.getPageNum();
        int totPageNum = pager.getNumPages();
        int prevPageNum = pager.getPrevPageNum();
        int nextPageNum = pager.getNextPageNum();
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<hr>Page ");
        htmlSB.append(pager.getPageNum());
        htmlSB.append("/");
        htmlSB.append(pager.getNumPages());


        htmlSB.append("&nbsp;");
        if (prevPageNum != -1) {
            htmlSB.append("<FONT COLOR=BLUE SIZE=+1>");
            htmlSB.append("<a href=\"");
            htmlSB.append(proxURL.getURLForPageNum(prevPageNum));
            htmlSB.append("\">Previous</a></FONT>");
        }

        // previous pages
        htmlSB.append("<FONT COLOR=BLACK>");
        for (int prevPageIdx = currPageNum - DEFAULT_PAGES_PER_PAGER; prevPageIdx < currPageNum; prevPageIdx++) {
            if (prevPageIdx > 0) {
                htmlSB.append("&nbsp;<a href=\"");
                htmlSB.append(proxURL.getURLForPageNum(prevPageIdx));
                htmlSB.append("\">");
                htmlSB.append(prevPageIdx);
                htmlSB.append("</a>");
            }
        }
        htmlSB.append("</FONT>");

        htmlSB.append("&nbsp;<FONT COLOR=RED SIZE=+1>");
        htmlSB.append(currPageNum);
        htmlSB.append("</FONT>");

        // next pages
        htmlSB.append("<FONT COLOR=BLACK>");
        for (int nextPageIdx = currPageNum + 1; nextPageIdx <= currPageNum + DEFAULT_PAGES_PER_PAGER; nextPageIdx++) {
            if (nextPageIdx <= totPageNum) {
                htmlSB.append("&nbsp;<a href=\"");
                htmlSB.append(proxURL.getURLForPageNum(nextPageIdx));
                htmlSB.append("\">");
                htmlSB.append(nextPageIdx);
                htmlSB.append("</a>");
            }
        }
        htmlSB.append("</FONT>");

        htmlSB.append("&nbsp;");
        if (nextPageNum != -1) {
            htmlSB.append("<FONT COLOR=BLUE SIZE=+1>");
            htmlSB.append("<a href=\"");
            htmlSB.append(proxURL.getURLForPageNum(nextPageNum));
            htmlSB.append("\">Next</a></FONT>");
        }
        return htmlSB.toString();
    }

    private String getSubgraphBodyHTML(ProxURL proxURL) {
        String parameter = proxURL.getParameter();
        if (parameter == null) {
            return getSubgraphSummaryBodyHTML(proxURL);
        } else if (ATTR_VAL_PARAM.equals(parameter)) {
            Container container = proxURL.getContainer(true);
            Subgraph subgraph = proxURL.getSubgraph();
            return getAttrValsBodyHTML(container.getSubgraphAttrs(),
                    subgraph.getSubgID());
        } else {
            throw new IllegalArgumentException("cant' handle parameter: " + parameter);
        }
    }

    private NST getSubgraphItemsWithLabel(NST subgObjectNST, NST subgLinkNST) {
        NST objectLabeledNST = getLabel(subgObjectNST, "item_id", true);
        objectLabeledNST.addConstantColumn("type", "str", "O");

        NST linkLabeledNST = getLabel(subgLinkNST, "item_id", false);
        linkLabeledNST.addConstantColumn("type", "str", "L");

        objectLabeledNST.insertRowsFromNST(linkLabeledNST);

        linkLabeledNST.release();

        return objectLabeledNST;
    }


    private String getSubgraphSummaryBodyHTML(ProxURL proxURL) {
        Subgraph subgraph = proxURL.getSubgraph();
        NST subgObjectNST = subgraph.getSubgObjectNST();
        NST subgLinkNST = subgraph.getSubgLinkNST();

        NST objectsAndLinksNST = getSubgraphItemsWithLabel(subgObjectNST, subgLinkNST);
        Pager pager = new Pager(objectsAndLinksNST, proxURL.getPageNum(), rowsPerPage);

        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("# objects: ");
        htmlSB.append(subgObjectNST.getRowCount());
        htmlSB.append("<br>\n");
        htmlSB.append("# links: ");
        htmlSB.append(subgLinkNST.getRowCount());
        htmlSB.append("<br><br>\n");
        htmlSB.append("<strong>Items:</strong><br>\n");

        ResultSet resultSet = pager.getResultSet("item_id, name, type, label");
        while (resultSet.next()) {
            int objID = resultSet.getOID(1);
            String name = resultSet.getString(2);
            String type = resultSet.getString(3);
            htmlSB.append("&nbsp;");
            htmlSB.append(name);
            htmlSB.append((type.equals("O") ? " (O)" : " (L)"));
            htmlSB.append(": ");
            htmlSB.append("<a href=\"item:/");
            htmlSB.append((type.equals("O") ? "objects/" : "links/"));
            htmlSB.append(objID);
            htmlSB.append("\">");
            if (resultSet.isColumnNil(4)) {
                htmlSB.append(objID);
            } else {
                htmlSB.append(resultSet.getString(4));
            }
            htmlSB.append("</a><br>\n");
        }
        htmlSB.append(getPagerHTML(pager, proxURL));
        subgObjectNST.release();
        subgLinkNST.release();
        objectsAndLinksNST.release();
        return htmlSB.toString();
    }


    private String getTopBodyHTML() {
        return "<table " + GUIContentGenerator.TABLE_PROPERTIES + ">\n" +
                "<tr>\n" +
                "<td><a href=\"cont:/containers\">Containers</a></td>\n" +
                "<td><a href=\"attrdefs:/containers\">Container Attributes</a></td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td><a href=\"db:/objects\">Objects</a></td>" +
                "<td><a href=\"attrdefs:/objects\">Object Attributes</a></td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td><a href=\"db:/links\">Links</a></td>" +
                "<td><a href=\"attrdefs:/links\">Link Attributes</a></td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "<br>\n" +
                "<br>\n" +
                "<br>\n" +
                "<br>\n" +
                getTopBodyVersionHTML();
    }

    String getTopBodyVersionHTML() {
        URL iconURL = getClass().getResource("images/small-logo.gif");
        String proxVersion = Util.getCoreVersion();
        String guiVersion = Util.getGUIVersion();
        int schemaMajorVer = Util.getSchemaMajorVersion();
        int schemaMinorVer = Util.getSchemaMinorVersion();
        return "<p style=\"text-align: center;\">" +
                "<img alt=\"Proximity Logo\" src=\"" + iconURL + "\"><br>" +
                "Connection: <strong>" + DB.description() + "</strong><br>\n" +
                "Versions: Core: <strong>" + proxVersion +
                "</strong>, Schema: <strong>" + schemaMajorVer + "." +
                schemaMinorVer + "</strong>, GUI: <strong>" + guiVersion +
                "</strong><br>\n" +
                "http://kdl.cs.umass.edu/proximity/" +
                "</p>";
    }

    /**
     * @param container
     * @return unique names in container, across all objects in all subgraphs
     */
    public static String[] getUniqueNamesInContainer(Container container) {
        NST containerObjectsNST = container.getItemNST(true);
        ResultSet rs = containerObjectsNST.selectRows("name DISTINCT ROWS", "name", "*");
        String[] names = new String[rs.getRowCount()];
        int i = 0;
        while (rs.next()) {
            names[i] = rs.getString(1);
            i++;
        }
        return names;
    }

    public void resetRowsPerPage() {
        rowsPerPage = GUIContentGenerator.DEFAULT_ROWS_PER_PAGE;
    }


    public void setLabelAttributeForObjects(String attrName) {
        if (PreferencesJFrame.NO_LABEL_STRING.equals(attrName)) {
            objAttrName = null;
        } else {
            objAttrName = attrName;
        }
    }


    public void setLabelAttributeForLinks(String attrName) {
        if (PreferencesJFrame.NO_LABEL_STRING.equals(attrName)) {
            linkAttrName = null;
        } else {
            linkAttrName = attrName;
        }
    }


    public void setLabelLengthForObjects(int length) {
        objectLabelLength = length;
    }


    public void setLabelLengthForLinks(int length) {
        linkLabelLength = length;
    }


    public void setRowsPerPage(int newRowsPerPage) {
        rowsPerPage = newRowsPerPage;
    }

}
