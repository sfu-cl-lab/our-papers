/**
 * $Id: Subquery.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: Subquery.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import kdl.prox.util.Assert;


/**
 * A concrete subquery class. Created either from a <graph-query> xml file.
 * Implements AbsQueryChild because Subqueries can be stored in an AbstractQuery.
 */
public class Subquery extends AbstractQuery implements AbsQueryChild {

    private Annotation annotation = null;   // null if none specified
    private AbstractQuery parentAQuery;     // Subquery or Query that contains me


    public Subquery(AbstractQuery parentAQuery) {
        Assert.condition(parentAQuery != null, "parentAQuery null");
        this.parentAQuery = parentAQuery;
    }

    public Annotation annotation() {
        return annotation;
    }

    public void deleteAnnotation() {
        this.annotation = null;
    }

    /**
     * @param isRecurse as passed to AbstractQuery.vertices()
     * @return a Set of QGEdges that cross my boundary
     */
    public Set getEdgesCrossingBoundary(boolean isRecurse) {
        Set crossingEdges = new HashSet();
        // we iterate over my vertices (not my edges) because some edges that
        // cross my boundary might be contained elsewhere
        for (Iterator qgVertexIter = vertices(isRecurse).iterator(); qgVertexIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertexIter.next();
            Set qgEdges = qgVertex.edges();
            for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                if (qgEdge.isCrossesSubqBoundsEdge()) {
                    crossingEdges.add(qgEdge);
                }
            }
        }
        return crossingEdges;
    }

    /**
     * @return a Set of QGEdges that cross my boundary
     */
    public Set getEdgesCrossingBoundaryToParent() {
        Set crossingEdges = new HashSet();
        // we iterate over my vertices (not my edges) because some edges that
        // cross my boundary might be contained elsewhere
        for (Iterator qgVertexIter = vertices(false).iterator(); qgVertexIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertexIter.next();
            Set qgEdges = qgVertex.edges();
            for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                QGVertex otherVertex = qgEdge.otherVertex(qgVertex);
                if (qgEdge.isCrossesSubqBoundsEdge() &&
                        otherVertex.parentAQuery() == parentAQuery) {
                    crossingEdges.add(qgEdge);
                }
            }
        }
        return crossingEdges;
    }

    /**
     * @return a Set of QGVertices that have edges that cross my boundary into me
     */
    public Set getAnchorsInParent() {
        Set crossingEdges = new HashSet();
        // we iterate over my vertices (not my edges) because some edges that
        // cross my boundary might be contained elsewhere
        for (Iterator qgVertexIter = vertices(false).iterator(); qgVertexIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertexIter.next();
            Set qgEdges = qgVertex.edges();
            for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                QGVertex otherVertex = qgEdge.otherVertex(qgVertex);
                if (qgEdge.isCrossesSubqBoundsEdge() &&
                        otherVertex.parentAQuery() == parentAQuery) {
                    crossingEdges.add(otherVertex);
                }
            }
        }
        return crossingEdges;
    }

    /**
     * Returns true if I'm annotated, and false o/w.
     */
    public boolean isAnnotated() {
        return (annotation != null);
    }

    public AbstractQuery parentAQuery() {
        return parentAQuery;
    }

    public Query rootQuery() {
        return parentAQuery.rootQuery();
    }

    /**
     * AbsQueryChild method. Sets my parentAQuery to parentAQuery. Pass null to
     * clear it.
     */
    public void setParentAQuery(AbstractQuery parentAQuery) {
        this.parentAQuery = parentAQuery;
    }

    public void setAnnotation(Annotation annot) {
        this.annotation = annot;
    }

}
