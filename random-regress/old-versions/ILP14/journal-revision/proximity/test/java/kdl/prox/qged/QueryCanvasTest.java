/**
 * $Id: QueryCanvasTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qgraph2.AbstractQuery;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.QueryIterHandler;
import kdl.prox.qgraph2.QueryIterHandlerEmptyAdapter;
import kdl.prox.qgraph2.QueryIterator;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.qgraph2.Subquery;
import org.apache.log4j.Logger;
import org.jdom.Element;


public class QueryCanvasTest extends TestCase {

    private static final Logger log = Logger.getLogger(QueryCanvasTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Utility that loads a Query from a file relative to a class.
     *
     * @param rezClass      Class that queryFileName is located relative to
     * @param queryFileName name of file to load, relative to rezClass
     * @return a Query loaded from queryFileName, located relative to this class
     * @throws Exception
     */
    public static Query loadQueryFromFile(Class rezClass, String queryFileName) throws Exception {
        URL testXMLFileURL = rezClass.getResource(queryFileName);
        File queryFile = new File(testXMLFileURL.getFile());
        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        Query query = QueryXMLUtil.graphQueryEleToQuery(graphQueryEle);
        return query;
    }

    /**
     * Tests that QueryCanvas accurately reflects the structure of various test
     * queries.
     */
    public void testQueryCanvas() throws Exception {
        verifyQueryCanvas("frequent_coauthor.xml");
        verifyQueryCanvas("hot100-neighborhood.qg2.xml");
    }

    private void verifyQGItem(QueryCanvas queryCanvas, QGItem qgItem) {
        // check returned node
        PNode pNode = queryCanvas.getPNode(qgItem);
        assertNotNull(pNode);

        // check parents match
        AbstractQuery parentAQ = qgItem.parentAQuery();
        PNode parentAQPNode = queryCanvas.getPNode(parentAQ);
        assertEquals(parentAQPNode, pNode.getParent());

        // check label group
        LabelGroup labelGroup = QueryCanvas.getLabelGroup(pNode);
        assertNotNull(labelGroup);
        Annotation annotation = qgItem.annotation();
        assertEquals((annotation == null ? null : annotation.annotationString()),
                labelGroup.getAnnotationText());
        assertEquals(new ConditionFormat().format(new CondEleWrapper(qgItem.condEleChild())),
                labelGroup.getConditionText());
        assertEquals(qgItem.firstName(), labelGroup.getName());
        assertEquals(pNode.getParent(), labelGroup.getParent());
    }

    /**
     * Tests that the GUI accurately reflects the structure of passed query.
     * Also tests label groups. Does not check constraints.
     *
     * @param queryFileName file in this test class's directory
     * @throws Exception if trouble reading file
     */
    private void verifyQueryCanvas(String queryFileName) throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(), queryFileName);
        final QueryCanvas queryCanvas = new QueryCanvas(query);

        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                verifyQGItem(queryCanvas, qgEdge);

                // ceck PNode has endpoints corresponding to qgEdge's endpoints
                PNode pEdge = queryCanvas.getPNode(qgEdge);
                PNode pVertex1 = queryCanvas.getPNode(qgEdge.vertex1());
                PNode pVertex2 = queryCanvas.getPNode(qgEdge.vertex2());
                assertEquals(pVertex1, QueryCanvas.getPVertex1(pEdge));
                assertEquals(pVertex2, QueryCanvas.getPVertex2(pEdge));

                // check that PNode has arrow only if isDirected
                PPath arrowHead = QueryCanvas.getArrowHead((PPath) pEdge);  // todo dangerous!
                if (qgEdge.isDirected()) {
                    assertNotNull(arrowHead);
                } else {
                    assertNull(arrowHead);
                }
            }

            public void startAbstractQuery(AbstractQuery absQuery) {
                PNode pNode = queryCanvas.getPNode(absQuery);
                if (absQuery instanceof Query) {
                    assertTrue(pNode instanceof PLayer);
                } else {    // absQuery instanceof Subquery
                    Subquery subquery = (Subquery) absQuery;
                    assertNotNull(pNode);

                    // check parents match
                    AbstractQuery parentAQ = subquery.parentAQuery();
                    PNode parentAQPNode = queryCanvas.getPNode(parentAQ);
                    assertEquals(parentAQPNode, pNode.getParent());

                    // test PNode contains child PNodes corresponding to
                    // absQuery's children
                    Set qgItems = absQuery.qgItems(false);  // todo xx isRecurse
                    for (Iterator qgItemIter = qgItems.iterator(); qgItemIter.hasNext();)
                    {
                        QGItem childQGItem = (QGItem) qgItemIter.next();
                        PNode childPNode = queryCanvas.getPNode(childQGItem);
                        assertTrue(pNode.indexOfChild(childPNode) != -1);
                    }

                    // check label group
                    LabelGroup labelGroup = QueryCanvas.getLabelGroup(pNode);
                    assertNotNull(labelGroup);
                    Annotation annotation = subquery.annotation();
                    assertEquals((annotation == null ? null : annotation.annotationString()),
                            labelGroup.getAnnotationText());
                    assertEquals(pNode.getParent(), labelGroup.getParent());
                }
            }

            public void vertex(QGVertex qgVertex) {
                verifyQGItem(queryCanvas, qgVertex);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }

}
