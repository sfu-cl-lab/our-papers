/**
 * $Id: QueryGenTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryGenTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.family;

import java.util.HashSet;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;


public class QueryGenTest extends TestCase {

    /**
     * Class-based log4j category for logging.
     */
    private static Logger log = Logger.getLogger(QueryGenTest.class);


    /**
     * Public constructor, required by TestCase
     */
    public QueryGenTest(String name) {
        super(name);
    }


    protected void setUp() {
        TestUtil.initDBOncePerAllTests();
    }


    protected void takeDown() {
    }


    /**
     * Creates an Element object for the Edge, to be added to a Document
     *
     * @param name      : name of the edge
     * @param v1        : name of the first vertex
     * @param v2        : name of the second vertex
     * @param annotTest : value for the annot-test attribute
     * @param dirTest   : value for the dir-test attribute
     */
    private Element createEdgeElement(String name, String v1, String v2, String annotTest, String dirTest) {
        Element edgeEle = new Element("edge");
        edgeEle.setAttribute("name", name);
        edgeEle.setAttribute("v1", v1);
        edgeEle.setAttribute("v2", v2);
        if (annotTest != null) {
            edgeEle.setAttribute("annot-test", annotTest);
        }
        if (dirTest != null) {
            edgeEle.setAttribute("dir-test", dirTest);
        }

        return edgeEle;
    }


    /**
     * Creates a new Document, adds a root element of type <query-family>, and returns the root
     */
    private Element createFamilyElement() {
        Document document = new Document();
        Element newQueryFamilyEle = new org.jdom.Element("query-family");    // filled next
        newQueryFamilyEle.setAttribute("name", "auto-gen test");
        document.setRootElement(newQueryFamilyEle);

        return document.getRootElement();
    }


    /**
     * Creates an Element object for the Vertex, to be added to a Document
     *
     * @param name      : name of the edge
     * @param annotTest : value for the annot-test attribute
     */
    private Element createVertexElement(String name, String annotTest) {
        Element vertexEle = new Element("vertex");
        vertexEle.setAttribute("name", name);
        if (annotTest != null) {
            vertexEle.setAttribute("annot-test", annotTest);
        }

        return vertexEle;
    }


    /**
     * Test that the <query-family> specification is parsed and validated correctly
     */

    // Tests that an edge or vertex with an empty name is not accepted
    public void testNameNotNull() throws Exception {
        // test vertex
        Element root = createFamilyElement();
        root.addContent(createVertexElement("", null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
        // test edge
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("", "a", "b", null, null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }


    // Tests that nodes A and a are one and the same
    public void testNameIgnoreCase() throws Exception {
        // test that A and a are recognized as the same node
        // should throw an exception
        Element root = createFamilyElement();
        root.addContent(createVertexElement("A", null));
        root.addContent(createVertexElement("a", null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception because two vertices have the same name");
        } catch (Exception success) {
        }	// ignore
        // test that  edges X and x are recognized as the same
        // should  throw an exception
        root = createFamilyElement();
        root.addContent(createVertexElement("A", null));
        root.addContent(createVertexElement("B", null));
        root.addContent(createEdgeElement("X", "a", "b", null, null));
        root.addContent(createEdgeElement("X", "a", "b", null, null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception because the two edges have the same name");
        } catch (Exception success) {
        }	// ignore
        // test that an edge from node a goes from node A
        // should not throw an exception
        root = createFamilyElement();
        root.addContent(createVertexElement("A", null));
        root.addContent(createVertexElement("B", null));
        root.addContent(createEdgeElement("z", "a", "b", null, null));
        root.addContent(createEdgeElement("X", "b", "a", null, null));
        new QueryGen(root);
    }


    // Tests that a spec. cannot have two vertices with the same name. Should throw an exception
    public void testUniqueVertices() throws Exception {
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("a", null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }


    // Tests that a spec. cannot have two edges with the same name. Should throw an exception
    public void testUniqueEdges() throws Exception {
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", null, null));
        root.addContent(createEdgeElement("x", "a", "b", null, null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }


    // tests that a vertex can only have annot-test AX, A1, AN, or null
    public void testVertexAnnot() throws Exception {
        // null is OK. No exception
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        // AX is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "ax"));
        // A1 is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "a1"));
        // AN is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "an"));
        // AA is not OK. Should throw exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "AA"));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }


    // tests that an edge can only have annot-test null, A1, AN, AA, or AX
    public void testEdgeAnnot() throws Exception {
        // null is OK. No exception
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", null, null));
        // AX is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "AX", null));
        // A1 is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "A1", null));
        // AN is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "AN", null));
        // AA is OK; no exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "AA", null));
        // A- is not OK. Should throw exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "A-", null));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }

    // tests that an edge can only have dir-test null, DO, or DB
    public void testEdgeDir() throws Exception {
        // null is OK. No exception
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", null, null));
        new QueryGen(root);
        // DO is OK; no exception -- case insensitive
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "A1", "dO"));
        new QueryGen(root);
        // DB is OK; no exception -- case insensitive
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "A1", "Db"));
        new QueryGen(root);
        // Anything else is not OK. Should throw exception
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", null, "sdf"));
        try {
            new QueryGen(root);
            fail("Should raise an Exception");
        } catch (Exception success) {
        }	// ignore
    }

    /**
     * Test the generation of query instances
     */

    //test a single vertex
    public void testSingleVertex() throws Exception {
        // A single vertex, no A+. Should generate a single query
        // Since the query processor capitalizes all names, the result is _A
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        QueryGen qGen = new QueryGen(root);
        HashSet expectedResults = new HashSet();
        expectedResults.add("_A");
        if (!qGen.getQueryInstances().keySet().equals(expectedResults)) {
            fail("Query with a single vertex A generated wrong set of results: " + qGen.getQueryInstances());
        }
        // A single vertex, with AN. Should generate three queries: _A1inf, _A11, _A12
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "AN"));
        qGen = new QueryGen(root);
        expectedResults = new HashSet();
        expectedResults.add("_A1inf");
        expectedResults.add("_A11");
        expectedResults.add("_A12");
        if (!qGen.getQueryInstances().keySet().equals(expectedResults)) {
            fail("Query with a single vertex A (and annot AN) generated wrong set of results: " + qGen.getQueryInstances());
        }
        // A single vertex, with A1. Should generate one queries: _A1inf
        root = createFamilyElement();
        root.addContent(createVertexElement("a", "A1"));
        qGen = new QueryGen(root);
        expectedResults = new HashSet();
        expectedResults.add("_A1inf");
        if (!qGen.getQueryInstances().keySet().equals(expectedResults)) {
            fail("Query with a single vertex A (and annot A1) generated wrong set of results: " + qGen.getQueryInstances());
        }

    }

    /**
     * Test the generation of query instances
     */

    //test two vertices and an edge
    public void testSingleEdge() throws Exception {
        // A simple edge, directed
        Element root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", null, "do"));
        QueryGen qGen = new QueryGen(root);
        HashSet expectedResults = new HashSet();
        expectedResults.add("_A_B_X");
        expectedResults.add("_A_B_XD");
        if (!qGen.getQueryInstances().keySet().equals(expectedResults)) {
            fail("Query with a single directed edge X generated wrong set of results: " + qGen.getQueryInstances());
        }
        // A single edge, with A1, DB. Should generate: _A_B_X1inf, _A_B_X1infD, _A_B_X1infRD
        root = createFamilyElement();
        root.addContent(createVertexElement("a", null));
        root.addContent(createVertexElement("b", null));
        root.addContent(createEdgeElement("x", "a", "b", "A1", "db"));
        qGen = new QueryGen(root);
        expectedResults = new HashSet();
        expectedResults.add("_A_B_X1inf");
        expectedResults.add("_A_B_X1infD");
        expectedResults.add("_A_B_X1infRD");
        if (!qGen.getQueryInstances().keySet().equals(expectedResults)) {
            fail("Query with a single non-directed edge (and annot A1) generated wrong set of results: " + qGen.getQueryInstances());
        }

    }

}
