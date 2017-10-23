/**
 * $Id: QGVertexTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qgraph2;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qged.QueryCanvasTest;
import org.apache.log4j.Logger;


public class QGVertexTest extends TestCase {

    private static final Logger log = Logger.getLogger(QGVertexTest.class);


    protected void setUp() throws Exception {
        super.setUp();
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetEdgesBetVerts() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(),
                "mult-edge-query.xml");
        QGVertex qgVert1 = (QGVertex) query.qgItemForName("Vertex1");
        QGVertex qgVert2 = (QGVertex) query.qgItemForName("Vertex2");
        QGVertex qgVert3 = (QGVertex) query.qgItemForName("Vertex3");

        QGEdge qgEdge1 = (QGEdge) query.qgItemForName("Edge1");
        QGEdge qgEdge2 = (QGEdge) query.qgItemForName("Edge2");
        QGEdge qgEdge3 = (QGEdge) query.qgItemForName("Edge3");
        QGEdge qgEdge4 = (QGEdge) query.qgItemForName("Edge4");

        List actQGEdges11 = qgVert1.getEdgesBetween(qgVert1);
        List actQGEdges12 = qgVert1.getEdgesBetween(qgVert2);
        List actQGEdges13 = qgVert1.getEdgesBetween(qgVert3);
        List actQGEdges22 = qgVert2.getEdgesBetween(qgVert2);
        List actQGEdges23 = qgVert2.getEdgesBetween(qgVert3);

        List expQGEdges11 = Arrays.asList(new QGEdge[] {qgEdge1});
        List expQGEdges12 = Arrays.asList(new QGEdge[] {qgEdge2});
        List expQGEdges13 = Arrays.asList(new QGEdge[] {});
        List expQGEdges22 = Arrays.asList(new QGEdge[] {});
        List expQGEdges23 = Arrays.asList(new QGEdge[] {qgEdge3, qgEdge4});

        assertListsEquals(expQGEdges11, actQGEdges11);
        assertListsEquals(expQGEdges12, actQGEdges12);
        assertListsEquals(expQGEdges13, actQGEdges13);
        assertListsEquals(expQGEdges22, actQGEdges22);
        assertListsEquals(expQGEdges23, actQGEdges23);
    }

    /**
     * Asserts equals ingoring order.
     *
     * @param list1
     * @param list2
     */
    private void assertListsEquals(List list1, List list2) {
        assertEquals(list1.size(), list2.size());
        assertTrue(list1.containsAll(list2));
    }

}
