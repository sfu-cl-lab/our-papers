/**
 * $Id: GUI2Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: GUI2Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.utils.Pair;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbvis.ProxDirectedSparseEdge;
import kdl.prox.dbvis.ProxItemData;
import kdl.prox.dbvis.ProxSparseVertex;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * Tests GUIContentGenerator.
 */
public class GUI2Test extends TestCase {

    private static Logger log = Logger.getLogger(GUI2Test.class);
    private static final int NUM_ROWS_PER_PAGE = 4;

    private GUIContentGenerator guiContentGen;
    private Container c1;
    private Container c2;
    private Container c3;
    private int s1;
    private int s2;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.insertObject(0);
        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertLink(0, 0, 1);
        DB.insertLink(1, 0, 2);

        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.deleteAllAttributes();
        Attributes linkAttrs = DB.getLinkAttrs();
        linkAttrs.deleteAllAttributes();

        createTestAttrs(DB.getObjectAttrs(), 0);

        objectAttrs.defineAttribute("label", "str");
        NST attrDataNST = objectAttrs.getAttrDataNST("label");
        attrDataNST.insertRow(new String[]{"0", "movie1"});
        attrDataNST.insertRow(new String[]{"2", "actor2"});
        linkAttrs.defineAttribute("label", "str");
        attrDataNST = linkAttrs.getAttrDataNST("label");
        attrDataNST.insertRow(new String[]{"0", "director-Of"});

        objectAttrs.defineAttribute("actor-degree", "int");
        attrDataNST = objectAttrs.getAttrDataNST("actor-degree");
        attrDataNST.insertRow(new String[]{"0", "3"});
        attrDataNST.insertRow(new String[]{"2", "3"});
        attrDataNST.insertRow(new String[]{"2", "5"});

        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
        DB.getContainerAttrs().deleteAllAttributes();
        c1 = rootContainer.createChild("c1");
        c2 = rootContainer.createChild("c2");
        c3 = c1.createChild("c3");
        s1 = 1;
        s2 = 2;
        Subgraph subgraph = c1.getSubgraph(s1);
        subgraph.insertObject(0, "obj 0");
        subgraph.insertObject(1, "obj 1");
        subgraph.insertObject(0, "obj 0 again");
        subgraph.insertLink(0, "link 0");
        subgraph.insertLink(0, "link 0 again");
        subgraph = c1.getSubgraph(s2);
        subgraph.insertObject(0, "obj 0");

        Attributes containerAttrs = DB.getContainerAttrs();
        containerAttrs.defineAttributeIfNotExists(Container.QUERY_ATTR_NAME, "str");
        containerAttrs.getAttrDataNST(Container.QUERY_ATTR_NAME).insertRow(c1.getOid() + ", '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE graph-query SYSTEM \"graph-query.dtd\">\n" +
                "\n" +
                "<graph-query name=\"Test Query\">\n" +
                "  <description>a test query</description>\n" +
                "  <query-body>\n" +
                "    <vertex name=\"vertex1\" />\n" +
                "  </query-body>\n" +
                "</graph-query>'");

        guiContentGen = new GUIContentGenerator();
        guiContentGen.resetRowsPerPage();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    private NST createPagerNST() {
        NST nst = new NST("id", "str");
        nst.insertRow(new String[]{"0"});
        nst.insertRow(new String[]{"1"});
        nst.insertRow(new String[]{"2"});
        nst.insertRow(new String[]{"3"});
        nst.insertRow(new String[]{"4"});
        nst.insertRow(new String[]{"5"});
        nst.insertRow(new String[]{"6"});
        nst.insertRow(new String[]{"7"});
        nst.insertRow(new String[]{"8"});
        nst.insertRow(new String[]{"9"});
        return nst;
    }


    private void createTestAttrs(Attributes attributes, int oid) {
        attributes.deleteAllAttributes();
        attributes.defineAttribute("z1", "int");    // defined first, but last alphabetically
        attributes.defineAttribute("a1", "str");
        attributes.defineAttribute("a2", "v1:int, v2:str");

        NST attrDataNST = attributes.getAttrDataNST("a1");
        attrDataNST.insertRow(new String[]{oid + "", "val1"});
        attrDataNST.insertRow(new String[]{oid + "", "val2"});

        attrDataNST = attributes.getAttrDataNST("a2");
        attrDataNST.insertRow(new String[]{oid + "", "1", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val1"});
    }

    private String getPagerString(int from, int to, String urlPrefix) {
        StringBuffer sb = new StringBuffer();
        sb.append("<hr>Page ");
        sb.append(from);
        sb.append("/");
        sb.append(to);
        sb.append("&nbsp;");

        // previous
        if (from > 1) {
            sb.append("<FONT COLOR=BLUE SIZE=+1><a href=\"");
            sb.append(urlPrefix);
            sb.append("#");
            sb.append(from - 1);
            sb.append("\">Previous</a></FONT>");
        }
        // numbers
        sb.append("<FONT COLOR=BLACK>");
        for (int i = 1; i <= to; i++) {
            if (i == from) {
                sb.append("</FONT>&nbsp;<FONT COLOR=RED SIZE=+1>");
                sb.append(i);
                sb.append("</FONT><FONT COLOR=BLACK>");
            } else {
                sb.append("&nbsp;<a href=\"");
                sb.append(urlPrefix);
                sb.append("#");
                sb.append(i);
                sb.append("\">");
                sb.append(i);
                sb.append("</a>");
            }
        }
        sb.append("</FONT>&nbsp;");
        // next
        if (from < to) {
            sb.append("<FONT COLOR=BLUE SIZE=+1><a href=\"");
            sb.append(urlPrefix);
            sb.append("#");
            sb.append(from + 1);
            sb.append("\">Next</a></FONT>");
        }

        return sb.toString();
    }


    private String getSingleAttrExpBodyHTML(boolean isSortByValue, boolean isIncludeAllRows, String urlString) {
        String col1Val1Link = "<a href=\"filter:/objects/a2/v1/1\">1</a>";
        String col1Val2Link = "<a href=\"filter:/objects/a2/v1/2\">2</a>";
        String col2Val1Link = "<a href=\"filter:/objects/a2/v2/'val1'\">val1</a>";
        String col2Val2Link = "<a href=\"filter:/objects/a2/v2/'val2'\">val2</a>";
        StringBuffer resultSB = new StringBuffer();
        resultSB.append("<strong>Attribute: a2</strong><br>\n" +
                "<br>\n" +
                "row count: 4<br>\n" +
                "column count: 2 (v1:int, v2:str)<br>\n" +
                "<br>\n" +
                "<strong>column: v1 (<em>int</em>)</strong>\n" +
                "<table " + GUIContentGenerator.TABLE_PROPERTIES + ">\n" +
                "<tr>\n" +
                "<td><a href=\"attr:/objects/a2!" + GUIContentGenerator.SORT_BY_VALUE_PARAM + "\">value</a></td>\n" +
                "<td><a href=\"attr:/objects/a2!" + GUIContentGenerator.SORT_BY_COUNT_PARAM + "\">count</a></td>\n" +
                "</tr>\n");
        if (isSortByValue) {
            resultSB.append("<tr>\n" +
                    "<td>" + col1Val1Link + "</td>\n" +
                    "<td>1</td>\n" +
                    "</tr>\n");
            if (isIncludeAllRows) {
                resultSB.append("<tr>\n" +
                        "<td>" + col1Val2Link + "</td>\n" +
                        "<td>3</td>\n" +
                        "</tr>\n");
            }
        } else {
            resultSB.append("<tr>\n" +
                    "<td>" + col1Val2Link + "</td>\n" +
                    "<td>3</td>\n" +
                    "</tr>\n");
            if (isIncludeAllRows) {
                resultSB.append("<tr>\n" +
                        "<td>" + col1Val1Link + "</td>\n" +
                        "<td>1</td>\n" +
                        "</tr>\n");
            }
        }
        resultSB.append("</table>\n");
        if (!isIncludeAllRows) {
            resultSB.append(getPagerString(1, 2, urlString));
        }
        resultSB.append("<br>\n" +
                "<strong>column: v2 (<em>str</em>)</strong>\n" +
                "<table " + GUIContentGenerator.TABLE_PROPERTIES + ">\n" +
                "<tr>\n" +
                "<td><a href=\"attr:/objects/a2!" + GUIContentGenerator.SORT_BY_VALUE_PARAM + "\">value</a></td>\n" +
                "<td><a href=\"attr:/objects/a2!" + GUIContentGenerator.SORT_BY_COUNT_PARAM + "\">count</a></td>\n" +
                "</tr>\n");
        if (isSortByValue) {
            resultSB.append("<tr>\n" +
                    "<td>" + col2Val1Link + "</td>\n" +
                    "<td>1</td>\n" +
                    "</tr>\n");
            if (isIncludeAllRows) {
                resultSB.append("<tr>\n" +
                        "<td>" + col2Val2Link + "</td>\n" +
                        "<td>3</td>\n" +
                        "</tr>\n");
            }
        } else {
            resultSB.append("<tr>\n" +
                    "<td>" + col2Val2Link + "</td>\n" +
                    "<td>3</td>\n" +
                    "</tr>\n");
            if (isIncludeAllRows) {
                resultSB.append("<tr>\n" +
                        "<td>" + col2Val1Link + "</td>\n" +
                        "<td>1</td>\n" +
                        "</tr>\n");
            }
        }
        resultSB.append("</table>\n");
        if (!isIncludeAllRows) {
            resultSB.append(getPagerString(1, 2, urlString));
        }
        return resultSB.toString();
    }

    public void testContainerWithChildrenAndSubgsScreen() {
        String url = "cont:/containers/" + c1.getName();
        String expectedLocation = "<h2>Container: " + c1.getName() + " </h2>";
        String expectedBody = "<a href=\"cont:/containers/" + c1.getName() + "/" + c3.getName() + "\">" + c3.getName() + "</a><br>\n" +
                "<strong>Subgraphs (2):</strong><br>\n" +
                "<a href=\"subg:/containers/" + c1.getName() + "/" + s1 + "\">" + s1 + "</a> &nbsp;" +
                "<a href=\"subg:/containers/" + c1.getName() + "/" + s2 + "\">" + s2 + "</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testContainerWithChildrenAndSubgsScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "cont:/containers/" + c1.getName();
        String expectedLocation = "<h2>Container: " + c1.getName() + " </h2>";
        String expectedBody = "<a href=\"cont:/containers/" + c1.getName() + "/" + c3.getName() + "\">" + c3.getName() + "</a><br>\n" +
                "<strong>Subgraphs (2):</strong><br>\n" +
                "<a href=\"subg:/containers/" + c1.getName() + "/" + s1 + "\">" + s1 + "</a> &nbsp;" +
                getPagerString(1, 2, "cont:/containers/" + c1.getName());
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testContainerWithChildrenScreen() {
        String url = "cont:/containers";
        String expectedLocation = "<h2>Container: Root</h2>";
        String expectedBody = "<a href=\"cont:/containers/" + c1.getName() + "\">" + c1.getName() + "</a><br>\n" +
                "<a href=\"cont:/containers/" + c2.getName() + "\">" + c2.getName() + "</a><br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testContainerWithChildrenScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "cont:/containers";
        String expectedLocation = "<h2>Container: Root</h2>";
        String expectedBody = "<a href=\"cont:/containers/" + c1.getName() + "\">" + c1.getName() + "</a><br>\n" +
                getPagerString(1, 2, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testFilteredItemsScreen() {
        String url = "filter:/objects/actor-degree/value/3";
        String expectedLocation = "<h2>Filtered Objects: actor-degree[value] = 3</h2>";
        String expectedBody = "# objects: 2<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">0</a> &nbsp;" +
                "<a href=\"item:/objects/2\">2</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        // test that the same item doesn't appear more than once
        DB.getObjectAttrs().getAttrDataNST("actor-degree").insertRow("3, 3").insertRow("3, 3");
        url = "filter:/objects/actor-degree/value/3";
        expectedLocation = "<h2>Filtered Objects: actor-degree[value] = 3</h2>";
        expectedBody = "# objects: 3<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">0</a> &nbsp;" +
                "<a href=\"item:/objects/2\">2</a> &nbsp;" +
                "<a href=\"item:/objects/3\">3</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
        DB.getObjectAttrs().getAttrDataNST("actor-degree").deleteRows("id = 3");


        guiContentGen.setLabelAttributeForObjects("label");
        url = "filter:/objects/actor-degree/value/3";
        expectedLocation = "<h2>Filtered Objects: actor-degree[value] = 3</h2>";
        expectedBody = "# objects: 2<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">movie1</a> &nbsp;" +
                "<a href=\"item:/objects/2\">actor2</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "filter:/links/label/value/'director-Of'";
        expectedLocation = "<h2>Filtered Links: label[value] = 'director-Of'</h2>";
        expectedBody = "# links: 1<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/links/0\">0</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetActions() {
        verifyActionForURL("db:/", new String[]{"graph", "analyze schema", "browse tables"});
        verifyActionForURL("db:/objects", new String[]{});
        verifyActionForURL("db:/links", new String[]{});
        verifyActionForURL("attr:/objects/birth_yeardis", new String[]{"delete", "export"});
        verifyActionForURL("attrdefs:/objects", new String[]{"create new"});
        verifyActionForURL("attrdefs:/objects", new String[]{"create new"});
        verifyActionForURL("attrdefs:/containers", new String[]{});
        verifyActionForURL("cont:/containers", new String[]{"view query", "attrs", "delete", "query", "thumbs"},
                false);
        verifyActionForURL("cont:/containers/c1", new String[]{"view query", "attrs", "delete", "query", "thumbs"});
        verifyActionForURL("cont:/containers/c2", new String[]{"view query", "attrs", "delete", "query", "thumbs"},
                new boolean[]{false, true, true, true, true});
        verifyActionForURL("cont:/containers/c2!ATTRVALS", new String[]{});
        verifyActionForURL("item:/objects/4", new String[]{"attrs", "graph"});
        verifyActionForURL("item:/objects/4!ATTRVALS", new String[]{});
        verifyActionForURL("item:/links/6", new String[]{"attrs"});
        verifyActionForURL("item:/links/6!ATTRVALS", new String[]{});
        verifyActionForURL("subg:/containers/c1/1", new String[]{"attrs", "graph", "prev", "next", "up"}, new boolean[]{true, true, false, true, true});
        verifyActionForURL("subg:/containers/c1/2", new String[]{"attrs", "graph", "prev", "next", "up"}, new boolean[]{true, true, true, false, true});
        verifyActionForURL("subg:/containers/movie-clusters-1998/6!ATTRVALS", new String[]{});
    }

    public void testGetActionsHTML() {
        // no actions
        ProxAction[] actionsArray = new ProxAction[]{};
        String actualActionsHTML = guiContentGen.getActionsHTML(Arrays.asList(actionsArray));
        String expActionsHTML = "";
        assertEquals(expActionsHTML, actualActionsHTML);

        // one action
        actionsArray = new ProxAction[]{new ProxAction("graph")};
        actualActionsHTML = guiContentGen.getActionsHTML(Arrays.asList(actionsArray));
        expActionsHTML = "[<a href=\"action=0\">graph</a>]&nbsp;";
        assertEquals(expActionsHTML, actualActionsHTML);

        // two actions
        actionsArray = new ProxAction[]{new ProxAction("graph"), new ProxAction("schema")};
        actualActionsHTML = guiContentGen.getActionsHTML(Arrays.asList(actionsArray));
        expActionsHTML = "[<a href=\"action=0\">graph</a>]&nbsp;[<a href=\"action=1\">schema</a>]&nbsp;";
        assertEquals(expActionsHTML, actualActionsHTML);

        // one action disabled
        actionsArray = new ProxAction[]{new ProxAction("graph", false)};
        actualActionsHTML = guiContentGen.getActionsHTML(Arrays.asList(actionsArray));
        expActionsHTML = "[graph]&nbsp;";
        assertEquals(expActionsHTML, actualActionsHTML);
    }


    public void testGetGraphForSubgURL() {
        String proxURL = "subg:/containers/" + c1.getName() + "/" + s1;
        Graph graph = new SparseGraph();
        GUIContentGenerator.getGraphForSubgURL(new ProxURL(proxURL), graph);

        // test vertices
        String[] expVertStrs = new String[]{"0.obj 0", "1.obj 1", "0.obj 0 again"};     // <vert-str> = <oid>.<label>
        List actVertStrs = new ArrayList();     // filled next
        Set vertexSet = graph.getVertices();
        for (Iterator vertIter = vertexSet.iterator(); vertIter.hasNext();) {
            ProxSparseVertex vertex = (ProxSparseVertex) vertIter.next();
            actVertStrs.add(vertex.getOID() + "." + vertex.getLabel());
        }
        TestUtil.verifyCollections(expVertStrs, actVertStrs);

        // test edges
        String[] expLinkStrs = new String[]{"0.link 0|0.obj 0|1.obj 1",
                "0.link 0 again|0.obj 0|1.obj 1",
                "0.link 0|0.obj 0 again|1.obj 1",
                "0.link 0 again|0.obj 0 again|1.obj 1"};    // <link-str>|<o1-str>|<o2-str>, where <xx-str> is defined above in expVertStrs
        List actEdgeStrs = new ArrayList();     // filled next
        Set edgeSet = graph.getEdges();
        for (Iterator edgeIter = edgeSet.iterator(); edgeIter.hasNext();) {
            ProxDirectedSparseEdge edge = (ProxDirectedSparseEdge) edgeIter.next();
            Pair endpoints = edge.getEndpoints();
            ProxItemData vert1 = (ProxItemData) endpoints.getFirst();
            ProxItemData vert2 = (ProxItemData) endpoints.getSecond();
            actEdgeStrs.add(edge.getOID() + "." + edge.getLabel() + "|" +
                    vert1.getOID() + "." + vert1.getLabel() + "|" +
                    vert2.getOID() + "." + vert2.getLabel());
        }
        TestUtil.verifyCollections(expLinkStrs, actEdgeStrs);
    }


    public void testGetOneContainersAttrValsScreen() {
        createTestAttrs(DB.getContainerAttrs(), c1.getOid());
        String url = "cont:/containers/" + c1.getName() + "!" + GUIContentGenerator.ATTR_VAL_PARAM;
        String expectedLocation = "<h2>Container: " + c1.getName() + " </h2>";
        String expectedBody = "<strong>Attribute Values:</strong><br>\n" +
                "a1: val1<br>\n" +
                "a1: val2<br>\n" +
                "a2: [1, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val1]<br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneLinksAttrValsScreen() {
        createTestAttrs(DB.getLinkAttrs(), 0);
        String url = "item:/links/0!" + GUIContentGenerator.ATTR_VAL_PARAM;
        String expectedLocation = "<h2>Link: 0</h2>";
        String expectedBody = "<strong>Attribute Values:</strong><br>\n" +
                "a1: val1<br>\n" +
                "a1: val2<br>\n" +
                "a2: [1, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val1]<br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneLinksSummaryScreen() {
        String url = "item:/links/0";
        String expectedLocation = "<h2>Link: 0</h2>";
        String expectedBody = "<strong>From</strong>: <a href=\"item:/objects/0\">0</a><br>\n" +
                "<strong>To</strong>: <a href=\"item:/objects/1\">1</a>";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneLinksSummaryScreenWithLabel() {
        guiContentGen.setLabelAttributeForObjects("label");
        guiContentGen.setLabelAttributeForLinks("label");
        String url = "item:/links/0";
        String expectedLocation = "<h2>Link: 0 (director-Of)</h2>";
        String expectedBody = "<strong>From</strong>: <a href=\"item:/objects/0\">0 (movie1)</a><br>\n" +
                "<strong>To</strong>: <a href=\"item:/objects/1\">1</a>";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneLinksSummaryScreenWithLabelAndTrunc() {
        guiContentGen.setLabelAttributeForObjects("label");
        guiContentGen.setLabelAttributeForLinks("label");
        guiContentGen.setLabelLengthForObjects(3);
        guiContentGen.setLabelLengthForLinks(4);
        String url = "item:/links/0";
        String expectedLocation = "<h2>Link: 0 (dire)</h2>";
        String expectedBody = "<strong>From</strong>: <a href=\"item:/objects/0\">0 (mov)</a><br>\n" +
                "<strong>To</strong>: <a href=\"item:/objects/1\">1</a>";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneObjectsAttrValsScreen() {
        String url = "item:/objects/0!" + GUIContentGenerator.ATTR_VAL_PARAM;
        String expectedLocation = "<h2>Object: 0</h2>";
        String expectedBody = "<strong>Attribute Values:</strong><br>\n" +
                "a1: val1<br>\n" +
                "a1: val2<br>\n" +
                "a2: [1, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val1]<br>\n" +
                "label: movie1<br>\n" +
                "actor-degree: 3<br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneObjectsSummaryScreen() {
        String url = "item:/objects/0";
        String expectedLocation = "<h2>Object: 0</h2>";
        String objHeader = "<strong>Connected to objects</strong><br>";
        String expectedBody = objHeader + "\n" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;" +
                "<a href=\"item:/objects/2\">2</a> &nbsp;\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }

    public void testGetOneObjectsSummaryScreenWithLabel() {
        guiContentGen.setLabelAttributeForObjects("label");
        String url = "item:/objects/0";
        String expectedLocation = "<h2>Object: 0 (movie1)</h2>";
        String objHeader = "<strong>Connected to objects</strong><br>";
        String expectedBody = objHeader + "\n" +
                "<a href=\"item:/objects/2\">actor2</a> &nbsp;" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneObjectsSummaryScreenWithLabelAndTrunc() {
        guiContentGen.setLabelLengthForObjects(3);
        guiContentGen.setLabelAttributeForObjects("label");
        String url = "item:/objects/0";
        String expectedLocation = "<h2>Object: 0 (mov)</h2>";
        String objHeader = "<strong>Connected to objects</strong><br>";
        String expectedBody = objHeader + "\n" +
                "<a href=\"item:/objects/2\">act</a> &nbsp;" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testGetOneObjectsSummaryScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "item:/objects/0";
        String expectedLocation = "<h2>Object: 0</h2>";
        String objHeader = "<strong>Connected to objects</strong><br>";
        String expectedBody = objHeader + "\n" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;\n" +
                getPagerString(1, 2, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testItemsScreen() {
        String url = "db:/objects";
        String expectedLocation = "<h2>Objects</h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">0</a> &nbsp;" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;" +
                "<a href=\"item:/objects/2\">2</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "db:/links";
        expectedLocation = "<h2>Links</h2>";
        expectedBody = "# links: 2<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/links/0\">0</a> &nbsp;" +
                "<a href=\"item:/links/1\">1</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testItemsScreenWithLabel() {
        guiContentGen.setLabelAttributeForObjects("label");
        guiContentGen.setLabelAttributeForLinks("label");
        String url = "db:/objects";
        String expectedLocation = "<h2>Objects</h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">movie1</a> &nbsp;" +
                "<a href=\"item:/objects/2\">actor2</a> &nbsp;" +
                "<a href=\"item:/objects/1\">1</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "db:/links";
        expectedLocation = "<h2>Links</h2>";
        expectedBody = "# links: 2<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/links/0\">director-Of</a> &nbsp;" +
                "<a href=\"item:/links/1\">1</a> &nbsp;";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testItemsScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "db:/objects";
        String expectedLocation = "<h2>Objects</h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/objects/0\">0</a> &nbsp;" +
                getPagerString(1, 3, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "db:/links";
        expectedLocation = "<h2>Links</h2>";
        expectedBody = "# links: 2<br>\n" +
                "<br>\n" +
                "<strong>IDs:</strong><br>\n" +
                "<a href=\"item:/links/0\">0</a> &nbsp;" +
                getPagerString(1, 2, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    // todo test others xx
    public void testObjAttrsScreen() {
        String url = "attrdefs:/objects";
        String expectedLocation = "<h2>Object Attributes</h2>";
        String expectedBody = "<strong>Attribute Definitions</strong><br>\n" +
                "<br>\n" +
                "<a href=\"attr:/objects/a1\">a1</a> <em>str</em><br>\n" +
                "<a href=\"attr:/objects/a2\">a2</a> <em>v1:int, v2:str</em><br>\n" +
                "<a href=\"attr:/objects/actor-degree\">actor-degree</a> <em>int</em><br>\n" +
                "<a href=\"attr:/objects/label\">label</a> <em>str</em><br>\n" +
                "<a href=\"attr:/objects/z1\">z1</a> <em>int</em><br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testPager() {
        NST nst = createPagerNST();
        Pager pager = new Pager(nst, 1, NUM_ROWS_PER_PAGE);
        ProxURL proxURL = new ProxURL("db:/objects");  // arbitrary
        String actualHTML = guiContentGen.getPagerHTML(pager, proxURL);
        String expectedHTML = getPagerString(1, 3, "db:/objects");
        assertEquals(expectedHTML, actualHTML);

        pager = new Pager(nst, 2, NUM_ROWS_PER_PAGE);
        actualHTML = guiContentGen.getPagerHTML(pager, proxURL);
        expectedHTML = getPagerString(2, 3, "db:/objects");
        assertEquals(expectedHTML, actualHTML);

        pager = new Pager(nst, 3, NUM_ROWS_PER_PAGE);
        actualHTML = guiContentGen.getPagerHTML(pager, proxURL);
        expectedHTML = getPagerString(3, 3, "db:/objects");
        assertEquals(expectedHTML, actualHTML);

        pager = new Pager(nst, 1, 1000);
        actualHTML = guiContentGen.getPagerHTML(pager, proxURL);
        expectedHTML = "";
        assertEquals(expectedHTML, actualHTML);
        nst.release();
    }


    // todo also test nested container's attribute, and possibly a link attribute
    public void testSingleAttributeScreen() {
        String url = "attr:/objects/a2";        // default sort is by count
        String expectedLocation = "<h2>Object Attribute</h2>";
        String expectedBody = getSingleAttrExpBodyHTML(false, true, "");
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "attr:/objects/a2!" + GUIContentGenerator.SORT_BY_COUNT_PARAM;    // make default explicit
        expectedBody = getSingleAttrExpBodyHTML(false, true, "");               // sort by value = false, include all values
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "attr:/objects/a2!" + GUIContentGenerator.SORT_BY_VALUE_PARAM;
        expectedBody = getSingleAttrExpBodyHTML(true, true, "");
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSingleAttributeScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "attr:/objects/a2";        // default sort is by count
        String expectedLocation = "<h2>Object Attribute</h2>";
        String expectedBody = getSingleAttrExpBodyHTML(false, false, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "attr:/objects/a2!" + GUIContentGenerator.SORT_BY_COUNT_PARAM;    // make default explicit
        expectedBody = getSingleAttrExpBodyHTML(false, false, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);

        url = "attr:/objects/a2!" + GUIContentGenerator.SORT_BY_VALUE_PARAM;
        expectedBody = getSingleAttrExpBodyHTML(true, false, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSubgraphsAttrsScreen() {
        createTestAttrs(c1.getSubgraphAttrs(), s1);
        String url = "subg:/containers/" + c1.getName() + "/" + s1 + "!" +
                GUIContentGenerator.ATTR_VAL_PARAM;
        String expectedLocation = "<h2>Subgraph: " + c1.getName() + " " + s1 + " </h2>";
        String expectedBody = "<strong>Attribute Values:</strong><br>\n" +
                "a1: val1<br>\n" +
                "a1: val2<br>\n" +
                "a2: [1, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val2]<br>\n" +
                "a2: [2, val1]<br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSubgraphScreen() {
        String url = "subg:/containers/" + c1.getName() + "/" + s1;
        String expectedLocation = "<h2>Subgraph: " + c1.getName() + " " + s1 + " </h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "# links: 2<br>" +
                "<br>\n" +
                "<strong>Items:</strong><br>\n" +
                "&nbsp;obj 0 (O): <a href=\"item:/objects/0\">0</a><br>\n" +
                "&nbsp;obj 1 (O): <a href=\"item:/objects/1\">1</a><br>\n" +
                "&nbsp;obj 0 again (O): <a href=\"item:/objects/0\">0</a><br>\n" +
                "&nbsp;link 0 (L): <a href=\"item:/links/0\">0</a><br>\n" +
                "&nbsp;link 0 again (L): <a href=\"item:/links/0\">0</a><br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSubgraphScreenWithLabel() {
        guiContentGen.setLabelAttributeForObjects("label");
        guiContentGen.setLabelAttributeForLinks("label");
        String url = "subg:/containers/" + c1.getName() + "/" + s1;
        String expectedLocation = "<h2>Subgraph: " + c1.getName() + " " + s1 + " </h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "# links: 2<br>" +
                "<br>\n" +
                "<strong>Items:</strong><br>\n" +
                "&nbsp;obj 0 (O): <a href=\"item:/objects/0\">movie1</a><br>\n" +
                "&nbsp;obj 0 again (O): <a href=\"item:/objects/0\">movie1</a><br>\n" +
                "&nbsp;obj 1 (O): <a href=\"item:/objects/1\">1</a><br>\n" +
                "&nbsp;link 0 (L): <a href=\"item:/links/0\">director-Of</a><br>\n" +
                "&nbsp;link 0 again (L): <a href=\"item:/links/0\">director-Of</a><br>\n";
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSubgraphScreenWithPager() {
        guiContentGen.setRowsPerPage(1);
        String url = "subg:/containers/" + c1.getName() + "/" + s1;
        String expectedLocation = "<h2>Subgraph: " + c1.getName() + " " + s1 + " </h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "# links: 2<br>" +
                "<br>\n" +
                "<strong>Items:</strong><br>\n" +
                "&nbsp;obj 0 (O): <a href=\"item:/objects/0\">0</a><br>\n" +
                getPagerString(1, 5, url);
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }


    public void testSubgraphScreenWithPagerSecondPage() {
        guiContentGen.setRowsPerPage(1);
        String url = "subg:/containers/" + c1.getName() + "/" + s1;
        String expectedLocation = "<h2>Subgraph: " + c1.getName() + " " + s1 + " </h2>";
        String expectedBody = "# objects: 3<br>\n" +
                "# links: 2<br>" +
                "<br>\n" +
                "<strong>Items:</strong><br>\n" +
                "&nbsp;obj 1 (O): <a href=\"item:/objects/1\">1</a><br>\n" +
                getPagerString(2, 5, "subg:/containers/c1/1");
        verifyHTMLForURL(url + "#2", expectedLocation, expectedBody);
    }


    public void testTopScreen() {
        String url = "db:/";
        String expectedLocation = "<h2>Database</h2>";
        String expectedBody = "<table " + GUIContentGenerator.TABLE_PROPERTIES + ">\n" +
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
                guiContentGen.getTopBodyVersionHTML();  // don't test version information (trust it's too simple to break)
        verifyHTMLForURL(url, expectedLocation, expectedBody);
    }

    private void verifyActionForURL(String url, String[] expActionNames) {
        verifyActionForURL(url, expActionNames, true);
    }

    private void verifyActionForURL(String url, String[] expActionNames, boolean expIsEnabled) {
        boolean[] expIsEnabledArr = new boolean[expActionNames.length];
        for (int enabledIdx = 0; enabledIdx < expIsEnabledArr.length; enabledIdx++) {
            expIsEnabledArr[enabledIdx] = expIsEnabled;
        }
        verifyActionForURL(url, expActionNames, expIsEnabledArr);
    }

    private void verifyActionForURL(String url, String[] expActionNames, boolean[] expIsEnabled) {
        List actualActions = guiContentGen.getActionsForURL(url);
        assertEquals(expActionNames.length, actualActions.size());
        for (int actionIdx = 0; actionIdx < expActionNames.length; actionIdx++) {
            String expActionName = expActionNames[actionIdx];
            ProxAction actualAction = (ProxAction) actualActions.get(actionIdx);
            assertEquals(expActionName, actualAction.getName());
            assertEquals(expIsEnabled[actionIdx], actualAction.getIsEnabled());
        }
    }

    private void verifyHTMLForURL(String url, String expectedLocation,
                                  String expectedBody) {
        String actualLocation = guiContentGen.getLocationHTMLForURL(url);
        assertEquals(expectedLocation, actualLocation);

        String actualBody = guiContentGen.getBodyHTMLForURL(url);
        assertEquals(expectedBody, actualBody);
    }

}
