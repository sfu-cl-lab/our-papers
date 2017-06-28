/**
 * $Id: QueryValidatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qgraph2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;


public class QueryValidatorTest extends TestCase {

    private static final Logger log = Logger.getLogger(QueryValidatorTest.class);


    protected void setUp() throws Exception {
        super.setUp();
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @param queryFileName
     * @return a Query loaded from queryFileName, located relative to this class
     * @throws Exception
     */
    private Query loadQueryFromFile(String queryFileName) throws Exception {
        URL testXMLFileURL = getClass().getResource(queryFileName);
        File queryFile = new File(testXMLFileURL.getFile());
        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        Query query = QueryXMLUtil.graphQueryEleToQuery(graphQueryEle);
        return query;
    }

    public void testBadConstraints() throws Exception {
        try {
            Query query = loadQueryFromFile("test-query-bad.xml");  // constraint starts with <test>, not <and>
            new QueryValidator(query);
            fail("query is invalid - contains <OR>");
        } catch (IllegalArgumentException illegalArg) {
            // expected
        }
        try {
            Query query = loadQueryFromFile("test-query-bad-constraints.xml");  // constraint starts with <test>, not <and>
            new QueryValidator(query);
            fail("query is invalid - constraint across two subqueries");
        } catch (QGValidationError illegalArg) {
            // expected
        }
    }

    public void testBadEdges() throws Exception {
        Query query = loadQueryFromFile("bad-edges.xml");
        // one good and four bad edges :
        //   3 edges that refer to non-existent vertex names
        //   1 edge with max = 0 as annotation
        try {
            new QueryValidator(query);
            fail("query contains bad edges");
        } catch (QGValidationError qgValidationError) {
            List errorList = qgValidationError.getErrorList();
            assertEquals(4, errorList.size());
            String prefix = "one or both edge vertices are invalid. ";
            assertEquals("edge min numeric annotation cannot be 0. Edge: v1-v2-zero-min",
                    errorList.get(0));
            assertEquals(prefix + "Vertex 1: v1 (ok), Vertex 2: v2bad (bad)",
                    errorList.get(1));
            assertEquals(prefix + "Vertex 1: v1bad (bad), Vertex 2: v2 (ok)",
                    errorList.get(2));
            assertEquals(prefix + "Vertex 1: v1bad (bad), Vertex 2: v2bad (bad)",
                    errorList.get(3));
        }

        Set disconnectedEdges = query.getDisconnectedEdges();
        assertEquals(3, disconnectedEdges.size());
        assertTrue(disconnectedEdges.contains(query.qgItemForName("v1-v2bad")));
        assertTrue(disconnectedEdges.contains(query.qgItemForName("v1bad-v2")));
        assertTrue(disconnectedEdges.contains(query.qgItemForName("v1bad-v2bad")));
    }

    public void testBadCachedElements() throws Exception {
        try {
            Query query = loadQueryFromFile("bad-caches.xml");
            new QueryValidator(query);
            fail("query is invalid - contains non-existing cached element");
        } catch (QGValidationError qgValidationError) {
            // expected
        }
    }

    public void testEmptyEdgeName() throws Exception {
        try {
            Query query = loadQueryFromFile("no-edge-names-query.xml");
            new QueryValidator(query);
            fail("query is invalid - contains empty names");
        } catch (QGValidationError qgValidationError) {
            // expected
        }
    }

    public void testEmptyVertexName() throws Exception {
        try {
            Query query = loadQueryFromFile("no-vert-names-query.xml");
            new QueryValidator(query);
            fail("query is invalid - contains empty names");
        } catch (QGValidationError qgValidationError) {
            // expected
        }
    }

    public void testNestedSubquery() throws Exception {
        Query query = loadQueryFromFile("nested-subqueries.qg2.xml");
        try {
            new QueryValidator(query);
            fail("query contains nested subqueries");
        } catch (QGValidationError qgValidationError) {
            List errorList = qgValidationError.getErrorList();
            assertEquals(2, errorList.size());
            assertTrue(((String) errorList.get(0)).startsWith("no edge can " +
                    "connect one annotated subquery to another"));
            assertTrue(((String) errorList.get(1)).startsWith("found a " +
                    "subquery whose parent was a itself a subquery (nested " +
                    "subqueries not supported)"));
        }
    }

    public void testOKConstraints() throws Exception {
        Query query = loadQueryFromFile("test-query.xml");  // constraint OK - starts with <and>, contain <test> elements
        new QueryValidator(query);      // should not throw Exception
        Object[] consts = query.constraints().toArray();
        assertEquals(2, consts.length);
    }

    public void testUniqueItems() throws Exception {
        Query query = loadQueryFromFile("test-query.xml");  // item names unique (won't load otherwise)
        QGItem qgVertex2 = query.qgItemForName("vertex2");
        qgVertex2.setFirstName("vertex1");  // not unique
        try {
            new QueryValidator(query);      // should not throw Exception
            fail("query contains nested subqueries");
        } catch (QGValidationError qgValidationError) {
            List errorList = qgValidationError.getErrorList();
            assertEquals(3, errorList.size());
            assertTrue(((String) errorList.get(0)).startsWith("constraint: " +
                    "unknown item"));
            assertTrue(((String) errorList.get(1)).startsWith("constraint: " +
                    "unknown item"));
            assertTrue(((String) errorList.get(2)).startsWith("found 1 duplicate"));
        }
    }

}
