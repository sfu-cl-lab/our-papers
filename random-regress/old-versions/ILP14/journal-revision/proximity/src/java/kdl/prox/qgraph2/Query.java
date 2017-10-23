/**
 * $Id: Query.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: Query.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.util.Assert;


/**
 * A concrete query class. Created either from a <graph-query> xml file or via the
 * application of a Transformation. Also defines testing and editing commands that
 * are useful to Transformation instances.
 */
public class Query extends AbstractQuery {

    private String name;
    private String description;


    public Query(String name, String description) {
        this.name = name;
        this.description = description;
    }


    /**
     * TFM edit utility that "absorbs" qgEdgeY and qgVertexB (the "absorbee") into
     * qgEdgeY's other vertex (the "absorber"). Handles edges related to the two
     * vertices in this way:
     * <p/>
     * o edges between the two vertices (other than qgEdgeY) are changed into
     * self loops on the absorber
     * <p/>
     * o all other qgVertexB edges are connected to the absorber
     */
    /*
     Here's a diagram (adapted from transformation #6) that demonstrates this:


                  X                                *B  . .  X
              ,- - - -,                               '   '
            ,           ,                            :     v
         ,-+-.         ,-+-.                          ,---.
   W    / ,-. \       /     \    Z              W    / ,-. \   *B
- - - -( ( A ) )-----(   B   )- - - -   =>   - - - -( ( *B) )- - - -
        \ `-' /   Y   \     /                        \ `-' /    Z
         `---'         `---'                          `---'
                       [i..j]

     Here we are absorbing Y and B into A. X edges (0 or more) become 0 or more
     self loops. Z edges (0 or more) are connected to A. W edges (0 or more) are
     left alone.

     Throws IllegalArgumentException if qgEdgeY is not in qgVertexB, or if Y
     crosses a subquery's boundary.
     */
    public void absorbEdgeAndVertex(QGEdge qgEdgeY, QGVertex qgVertexB) {
        Assert.condition(qgVertexB.edges().contains(qgEdgeY),
                "qgVertexB doesn't contain qgEdgeY: " + qgVertexB + ", " + qgEdgeY);
        Assert.condition((qgEdgeY.vertex1() == qgVertexB) ||
                (qgEdgeY.vertex2() == qgVertexB),
                "qgEdgeY doesn't contain qgVertexB: " + qgEdgeY + ", " + qgVertexB);
        Assert.condition(!qgEdgeY.isCrossesSubqBoundsEdge(),
                "qgEdgeY crosses a subquery's boundary: " + qgEdgeY);
        // continue
        QGVertex qgVertexA = qgEdgeY.otherVertex(qgVertexB);
        Assert.condition(qgVertexA != qgVertexB, "trying to absorb self loop");
        // continue: remove Y from its parent, and from B and A
        qgEdgeY.parentAQuery().removeEdge(qgEdgeY);
        qgVertexB.removeEdge(qgEdgeY);
        qgVertexA.removeEdge(qgEdgeY);	// safe because qgVertexA != qgVertexB (from above)
        // reconnect X and Z edges. we do this in two steps because we can't
        // remove the X or Z from B while iterating over B's edges (causes a
        // java.util.ConcurrentModificationException). thus the first pass does
        // the reconnecting of Xs and Zs, but does not remove the X or Z from B;
        // instead it saves the edge on bQGEdgesToRemove and lets the second
        // step do the removal
        Set bQGEdgesToRemove = new HashSet();	// B edges to remove; set in next loop
        Iterator edgeIter = qgVertexB.edges().iterator();
        while (edgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) edgeIter.next();
            bQGEdgesToRemove.add(qgEdge);
            if (qgEdge.otherVertex(qgVertexB) != qgVertexA)	// we have a Z...
                qgVertexA.addEdge(qgEdge);					// ...so add Z to A. leave the edge's parent as-is
            // change the X's or Z's B end to A (may make self loop)
            if (qgEdge.vertex1() == qgVertexB)
                qgEdge.setVertex1(qgVertexA);
            else
                qgEdge.setVertex2(qgVertexA);
        }
        // pass 2:
        edgeIter = bQGEdgesToRemove.iterator();
        while (edgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) edgeIter.next();
            // remove X or Z from B
            qgVertexB.removeEdge(qgEdge);
        }
        //	remove B from its parent
        qgVertexB.parentAQuery().removeVertex(qgVertexB);
    }


    /**
     * Top-level method called after constructor, returns results of connected
     * components calculation.
     *
     * @return a List of Sets, one Set for each connected component in me
     */
    public List connectedComponents() {
        ConnectedComponentHelper connCompHelper = new ConnectedComponentHelper(this);
        return connCompHelper.connectedComponents();
    }


    /**
     * @return List of all AbsQueryChild instances in me (QGItem and Subquery
     *         only), recursively. does not include this Query
     */
    public List getAbsQueryChildren() {
        final ArrayList absQueryChildren = new ArrayList();

        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                absQueryChildren.add(qgEdge);
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (abstractQuery instanceof Subquery) {
                    absQueryChildren.add(abstractQuery);
                }
            }

            public void vertex(QGVertex qgVertex) {
                absQueryChildren.add(qgVertex);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(this);

        return absQueryChildren;
    }


    public String getDescription() {
        return description;
    }


    public String getName() {
        return name;
    }


    /**
     * Object method. Returns true if I "equal" (in the transformation expansion
     * sense) query. Works by leveraging the fact that all items in a query
     * (including ConsQGVertex instances) are comparable and can be sorted: We sort
     * items from the two queries by name, then compare them one-by-one. We also
     * test asterisked items by verifying they have the same source. Finally, we
     * also check that the total number of subqueries is the same.
     * <p/>
     * todo define hashCode()!
     */
    public boolean equals(Object anObject) {
        if (this == anObject)
            return true;
        if (!(anObject instanceof Query))
            return false;
        // continue: compare Querys. first get inputs to check. "1" is this
        // (me), and "2" is query arg
        Query query = (Query) anObject;
        List allQGItems1 = new ArrayList(qgItems(true));		// isRecurse. sorted next (needs a List)
        List allQGItems2 = new ArrayList(query.qgItems(true));	// ""
        // check raw numbers, including subqueries
        List allSubqueries1 = subqueries(true);			// isRecurse
        List allSubqueries2 = query.subqueries(true);	// ""
        if (allQGItems1.size() != allQGItems2.size())
            return false;
        else if (allSubqueries1.size() != allSubqueries2.size())
            return false;
        // continue: sort and compare
        Collections.sort(allQGItems1);
        Collections.sort(allQGItems2);
        for (int qgItemIndex = 0; qgItemIndex < allQGItems1.size(); qgItemIndex++) {
            QGItem qgItem1 = (QGItem) allQGItems1.get(qgItemIndex);
            QGItem qgItem2 = (QGItem) allQGItems2.get(qgItemIndex);
            if (!qgItem1.equals(qgItem2))
                return false;		// different
            else if (!qgItem1.isSupportsAsterisks())
                continue;			// skip non-asterisked items
            // continue: items are same name and class, possibly with asterisks,
            // so check asterisk sources
            if (!qgItem1.isSameAsteriskSource(qgItem2))
                return false;	// asterisk source mismatch
        }
        // continue: compare size of constraint list and their contents
        List allConstraints1 = new ArrayList(constraints());
        List allConstraints2 = new ArrayList(query.constraints());
        if (allConstraints1.size() != allConstraints2.size())
            return false;
        for (int qgItemIndex = 0; qgItemIndex < allConstraints1.size(); qgItemIndex++) {
            QGConstraint qgConst1 = (QGConstraint) allConstraints1.get(qgItemIndex);
            if (!allConstraints2.contains(qgConst1))
                return false;		// different
        }
        // continue: all tests passed
        return true;
    }


    /**
     * TFM edit utility that "flattens" subquery by moving its items up to its
     * parent, then removing subquery from the parent.
     */
    public void flattenSubquery(Subquery subquery) {
        AbstractQuery parentAQuery = subquery.parentAQuery();
        // move subquery's items to parentAQuery
        Iterator qgItemIter = subquery.qgItems(false).iterator();	// isRecurse
        while (qgItemIter.hasNext()) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            if (qgItem instanceof QGEdge) {
                subquery.removeEdge((QGEdge) qgItem);
                parentAQuery.addEdge((QGEdge) qgItem);		// sets its parent
            } else {	// qgItem instanceof QGVertex
                subquery.removeVertex((QGVertex) qgItem);
                parentAQuery.addVertex((QGVertex) qgItem);	// sets its parent
            }
        }
        // remove subquery from its parent
        parentAQuery.removeSubquery(subquery);
    }


    /**
     * Returns true if I contain only a single non-annotated ConsQGVertex with no
     * asterisk, and I contain no subqueries or edges, nor constraints
     */
    public boolean isConsolidated() {
        if ((vertices(false).size() != 1) || // isRecurse
                (edges(false).size() != 0) || // ""
                (subqueries(false).size() != 0))	// ""
            return false;
        // continue: if we have constraints left, we're not done
        if (constraints().size() != 0)
            return false;
        // continue: we have a single QGVertex, and no subqueries nor constraints
        QGVertex finalQGVertex = (QGVertex) vertices(false).iterator().next();	// isRecurse
        return ((finalQGVertex instanceof ConsQGVertex) &&
                (finalQGVertex.annotation() == null) &&
                (!finalQGVertex.isAsterisked()));
    }

    public int numEdges() {
        return edges(true).size();
    }

    public int numVertices() {
        return vertices(true).size();
    }

    /**
     * TFM edit utility that removes QGEdge from its parent, and from its vertices.
     * NB: Not named removeEdge() - causes infinite loops!
     */
    public void removeEdgeFromParent(QGEdge qgEdge) {
        qgEdge.parentAQuery().removeEdge(qgEdge);	// remove from edges
        qgEdge.vertex1().removeEdge(qgEdge);		// remove from one vertex
        if (qgEdge.vertex2() != qgEdge.vertex1())	// might be a self loop
            qgEdge.vertex2().removeEdge(qgEdge);
    }


    /**
     * TFM edit utility that replaces currQGVertex in its containing AbstractQuery
     * with newQGVertex by changing all links to it to instead point to
     * newQGVertex.
     */
    public void replaceQGVertex(QGVertex currQGVertex, QGVertex newQGVertex) {
        AbstractQuery parentAQuery = currQGVertex.parentAQuery();
        // remove currQGVertex from its parent and add newQGVertex to the parent
        parentAQuery.removeVertex(currQGVertex);
        parentAQuery.addVertex(newQGVertex);		// sets its parent, checking name uniqueness
        // change all links from currQGVertex to newQGVertex
        Iterator edgeIter = currQGVertex.edges().iterator();
        while (edgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) edgeIter.next();
            if (qgEdge.vertex1() == currQGVertex)
                qgEdge.setVertex1(newQGVertex);
            if (qgEdge.vertex2() == currQGVertex)
                qgEdge.setVertex2(newQGVertex);
            newQGVertex.addEdge(qgEdge);			// sets its parent
        }
    }


    /**
     * AbstractQuery method.
     */
    public Query rootQuery() {
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    
//
// nested top-level classes follow
//


    /**
     * Helper class called by connectedComponents() that helps compute a Query's
     * connected components.
     * <p/>
     * Usage: Make a new instance then call connectedComponents() on it.
     * <p/>
     * Algorithm: We use this basic connected components algorhtm:
     * <p/>
     * Make-Set(x): Creates a new set {x}. x must not be in any other set.
     * <p/>
     * Union(x, y): Combine the set that contains x with the set that
     * contains y.
     * <p/>
     * Find-Set(x): Find-Set(x) = Find-Set(y) when x and y are in the same
     * set.
     * <p/>
     * Connected-Components(G):
     * for each vertex v in graph G
     * do Make-Set(v)
     * for each edge (u, v) in graph G
     * do if Find-Set(u) != Find-Set(v)
     * then Union(u, v)
     */
    static class ConnectedComponentHelper {

        /**
         * My Query. Set by constructor.
         */
        private Query query;

        /**
         * A List of my connected components. Each List member is a Set of
         * QGVertexS. Filled and managed by findConnComps().
         */
        private List connCompList = new ArrayList();


        /**
         * Finds query's connected components and saves them in my IVs.
         *
         * @param query
         */
        ConnectedComponentHelper(Query query) {
            Assert.condition(query != null, "null query");
            // continue
            this.query = query;
            findConnComps();
        }


        /**
         * Top-level method called after constructor, returns results of connected
         * components calculation.
         *
         * @return
         */
        List connectedComponents() {
            return connCompList;
        }


        /**
         * Called by constructor, finds my query's connected components.
         */
        void findConnComps() {
            Iterator qgVertIter = query.vertices(true).iterator();	// isRecurse
            while (qgVertIter.hasNext()) {
                QGVertex qgVertex = (QGVertex) qgVertIter.next();
                makeSet(qgVertex);
            }
            Iterator qgEdgeIter = query.edges(true).iterator();		// isRecurse
            while (qgEdgeIter.hasNext()) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                Set vertex1Set = findSet(qgEdge.vertex1());
                Set vertex2Set = findSet(qgEdge.vertex2());
                if (vertex1Set != vertex2Set) {
                    union(vertex1Set, vertex2Set);
                }
            }
        }


        /**
         * Called by findConnComps(), returns the Set in my connCompList that
         * contains qgVertex.
         *
         * @param qgVertex
         * @return
         */
        Set findSet(QGVertex qgVertex) {
            Iterator setIter = connCompList.iterator();
            while (setIter.hasNext()) {
                Set set = (Set) setIter.next();
                if (set.contains(qgVertex))
                    return set;
            }
            throw new RuntimeException("no set found: " + qgVertex);	// should never happen
        }


        /**
         * Called by findConnComps(), adds to my connCompList a new Set containing
         * qgVertex.
         *
         * @param qgVertex
         */
        void makeSet(QGVertex qgVertex) {
            Set set = new HashSet();
            set.add(qgVertex);
            connCompList.add(set);
        }


        /**
         * Called by findConnComps(), merges vertex2Set into vertex1Set in my
         * connCompList, deleting the former.
         *
         * @param vertex1Set
         * @param vertex2Set
         */
        void union(Set vertex1Set, Set vertex2Set) {
            vertex1Set.addAll(vertex2Set);
            connCompList.remove(vertex2Set);
        }


    }


}
