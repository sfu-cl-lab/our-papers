/**
 * $Id: EditorQueryIterator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2;

import java.util.Iterator;
import java.util.List;
import kdl.prox.util.Assert;

/**
 * Title: Editor Query Iterator
 * Description: A query editor which completes subqueries before edges in order to properly
 * draw edges in yFiles which require all nodes to be present before any edges are
 * drawn.
 * Created: Jan 28, 2004
 *
 * @author afast
 *         <p/>
 *         $Id: EditorQueryIterator.java 3658 2007-10-15 16:29:11Z schapira $
 */
public class EditorQueryIterator extends QueryIterator {

    /**
     * Starts iteration over query. Calls appropriate methods on my
     * queryIterHandler as elements (QGVertexS, QGEdgeS, and SubqueryS) are
     * encountered.
     * <p/>
     * <p/>
     * Notes:
     * <p/>
     * o startAbstractQuery() and endAbstractQuery() are called on the passed Query
     * o NB: For now interation order among elements is fixed: vertices, edges,
     * then subqueries.
     * <p/>
     * <p/>
     * Throws Exception if my queryIterHandler is not set.
     *
     * @param query the Query to iterate over. must not be null
     */
    public void iterate(Query query) {
        Assert.condition(query != null, "null query");
        Assert.condition(queryIterHandler != null, "null queryIterHandler");
// continue
        iterateInternal(query);
    }

    private void iterateInternal(AbstractQuery abstractQuery) {
        abstractQueryStack.add(0, abstractQuery);		// push
        queryIterHandler.startAbstractQuery(abstractQuery);
//Need to do subqueries first so that all edges have both nodes to connect to
        // do subqueries
        List subqueries = abstractQuery.subqueries(false);	// isRecurse
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            iterateInternal(subquery);
        }
        // do vertices
        List qgItems = abstractQuery.vertices(false);	// isRecurse
        Iterator qgItemIter = qgItems.iterator();
        queryIterHandler.startVertices();
        while (qgItemIter.hasNext()) {
            QGVertex qgVertex = (QGVertex) qgItemIter.next();
            queryIterHandler.vertex(qgVertex);
        }

// do edges
        qgItems = abstractQuery.edges(false);			// isRecurse
        qgItemIter = qgItems.iterator();
        queryIterHandler.startEdges();
        while (qgItemIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgItemIter.next();
            queryIterHandler.edge(qgEdge);
        }
        // do constraints
        List constraints = abstractQuery.constraints();
        Iterator constIter = constraints.iterator();
        queryIterHandler.startConstraints();
        while (constIter.hasNext()) {
            QGConstraint qgConstraint = (QGConstraint) constIter.next();
            queryIterHandler.constraint(qgConstraint);
        }
        queryIterHandler.endAbstractQuery(abstractQuery);
        abstractQueryStack.remove(0);		// pop

    }

}
