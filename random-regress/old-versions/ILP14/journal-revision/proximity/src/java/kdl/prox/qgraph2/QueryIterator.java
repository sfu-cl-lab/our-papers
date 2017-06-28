/**
 * $Id: QueryIterator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QueryIterator.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import kdl.prox.util.Assert;

import java.util.*;


/**
 * A Utility that simplifies recursively "walking" through a Query.
 * <p/>
 * <p/>
 * Typical usage (inspired by SAX):
 * <p/>
 * o new QueryIterator()
 * o call setHandler(QueryIterHandler)
 * o call iterate(Query)
 * <p/>
 * <p/>
 * Notes:
 * <p/>
 * o startAbstractQuery() and endAbstractQuery() are called on the passed Query
 * o see iterate() for order elements are processed in.
 */
public class QueryIterator {

    /**
     * My handler. null if not set. Set by setHandler().
     */
    protected QueryIterHandler queryIterHandler = null;

    /**
     * My recursion call stack. Managed by iterateInternal(). See
     * abstractQueryStack() for docs.
     */
    protected List abstractQueryStack = new ArrayList();


    /**
     * No-arg constructor.
     */
    public QueryIterator() {
        // does nothing
    }


    /**
     * Utility that can be called by handlers while within iterate(), returns
     * a List that acts as the stack of AbstractQueryS we're currently recursing
     * on. It starts out containing only the initial Query, then, as we recurse,
     * it contains the AbstractQueryS being processed with the deepest (newest)
     * at the start of the List and the shallowest (oldest, i.e., the original
     * Query) at the end.
     */
    public List abstractQueryStack() {
        return abstractQueryStack;
    }


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


    /**
     * Internal helper called by iterate().
     *
     * @param abstractQuery the AbstractQuery to iterate over
     */
    private void iterateInternal(AbstractQuery abstractQuery) {
        abstractQueryStack.add(0, abstractQuery);        // push
        queryIterHandler.startAbstractQuery(abstractQuery);

        // do vertices
        List qgItems = abstractQuery.vertices(false);    // isRecurse
        Iterator qgItemIter = qgItems.iterator();
        if (!qgItems.isEmpty()) {
            queryIterHandler.startVertices();
        }
        while (qgItemIter.hasNext()) {
            QGVertex qgVertex = (QGVertex) qgItemIter.next();
            queryIterHandler.vertex(qgVertex);
        }
        // do edges
        qgItems = abstractQuery.edges(false);            // isRecurse
        qgItemIter = qgItems.iterator();
        if (!qgItems.isEmpty()) {
            queryIterHandler.startEdges();
        }
        while (qgItemIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgItemIter.next();
            queryIterHandler.edge(qgEdge);
        }
        // do subqueries
        List subqueries = abstractQuery.subqueries(false);    // isRecurse
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            iterateInternal(subquery);
        }
        // do constraints
        List constraints = abstractQuery.constraints();
        Iterator constIter = constraints.iterator();
        if (!constraints.isEmpty()) {
            queryIterHandler.startConstraints();
        }
        while (constIter.hasNext()) {
            QGConstraint qgConstraint = (QGConstraint) constIter.next();
            queryIterHandler.constraint(qgConstraint);
        }
        // do addLinks
        List addLinksList = abstractQuery.addLinks();
        Iterator addLinksIter = addLinksList.iterator();
        while (addLinksIter.hasNext()) {
            QGAddLink qgAddLink = (QGAddLink) addLinksIter.next();
            queryIterHandler.addLink(qgAddLink);
        }
        // do cached items
        Map cachedItemMap = abstractQuery.cachedItems();
        Set cachedItemsSet = cachedItemMap.keySet();
        Iterator cachedItemsIter = cachedItemsSet.iterator();
        while (cachedItemsIter.hasNext()) {
            String itemName = (String) cachedItemsIter.next();
            queryIterHandler.cachedItem(itemName, (String) cachedItemMap.get(itemName));
        }

        queryIterHandler.endAbstractQuery(abstractQuery);
        abstractQueryStack.remove(0);        // pop
    }


    /**
     * Sets my queryIterHandler to queryIterHandler.
     *
     * @param queryIterHandler the new queryIterHandler. must not be null
     */
    public void setHandler(QueryIterHandler queryIterHandler) {
        Assert.condition(queryIterHandler != null, "null queryIterHandler");
        // continue
        this.queryIterHandler = queryIterHandler;
    }


}
