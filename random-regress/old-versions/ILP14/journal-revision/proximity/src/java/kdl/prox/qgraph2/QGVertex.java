/**
 * $Id: QGVertex.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QGVertex.java 3658 2007-10-15 16:29:11Z schapira $

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
import org.jdom.Element;


/**
 * Helper class that represents a graph query vertex. Does not define
 * isSupportsAsterisks() and thus does not support asterisks.
 */
public class QGVertex extends QGItem {

    /**
     * The QGEdge instances linked (in or out) to me. Managed by addEdge().
     */
    private Set edges = new HashSet();


    /**
     * Full-arg constructor. String overload.
     */
    public QGVertex(String name, Element condEle, Element annotEle)
            throws IllegalArgumentException {
        super(name, condEle, annotEle);
    }


    /**
     * Full-arg constructor. List overload.
     */
    QGVertex(List nameList, Element condEle, Element annotEle)
            throws IllegalArgumentException {
        super(nameList, condEle, annotEle);
    }


    /**
     * Adds qgEdge to my edges. Throws IllegalArgumentException if qgEdge already
     * in my edges.
     */
    public void addEdge(QGEdge qgEdge) {
        boolean isAdded = edges.add(qgEdge);
        Assert.condition(isAdded, "qgEdge already in edges: " + qgEdge);
    }


    /**
     * Returns a copy of my edges.
     */
    public Set edges() {
        return Collections.unmodifiableSet(edges);
    }


    /**
     * @param otherQGVertex other vertex to find edges between. pass this vertex
     *                      to find self-loops
     * @return List of QGEdge instances between me and otherQGVertex
     */
    public List getEdgesBetween(QGVertex otherQGVertex) {
        List qgEdges = new ArrayList();
        if (this == otherQGVertex) {
            // self-loop case
            qgEdges.addAll(selfLoops());
        } else {
            qgEdges.addAll(edges());
            qgEdges.retainAll(otherQGVertex.edges());
        }
        return qgEdges;
    }


    /**
     * Removes qgEdge from my edges. Throws IllegalArgumentException if qgEdge
     * not in my edges.
     */
    public void removeEdge(QGEdge qgEdge) {
        boolean isRemoved = edges.remove(qgEdge);
        Assert.condition(isRemoved, "qgEdge not in edges: " + qgEdge);
    }


    /**
     * Utility that returns my self loop edges. The result is empty if there are
     * none.
     */
    public Set selfLoops() {
        Set selfLoopQGEdges = new HashSet();	// return value. filled next
        Iterator qgEdgeIter = edges.iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            if (qgEdge.isSelfLoop())
                selfLoopQGEdges.add(qgEdge);
        }
        return selfLoopQGEdges;
    }


    /**
     * Object method. TEST
     */
    public String toString() {
        String condStr = (condEleChild() != null ? "c" : "");
        String asteriskStr = (isAsterisked() ? "*" + asteriskSourceToString() : "");
        return "[" + getClass().getName() + '@' + Integer.toHexString(hashCode()) +
                ": '" + catenatedName() + "'" + annotationString() + condStr +
                asteriskStr + "]";
    }


    /**
     * Returns my neighbors
     *
     * @return
     */
    public Set neighbors() {
        Set neighbors = new HashSet();
        for (Iterator iter = edges.iterator(); iter.hasNext();) {
            QGEdge edge = (QGEdge) iter.next();
            neighbors.add(edge.otherVertex(this));
        }
        return neighbors;
    }
}
