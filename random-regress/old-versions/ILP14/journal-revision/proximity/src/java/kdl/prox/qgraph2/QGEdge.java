/**
 * $Id: QGEdge.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QGEdge.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import kdl.prox.util.Assert;
import org.jdom.Element;


/**
 * Helper class that represents a graph query edge. Regarding vertex names vs.
 * vertex instances: Both representations are managed for two reasons: 1) When
 * an edge is created we can't depend on both of its vertices having been created
 * (due to the order of them in the XML file). So the code does a two-pass creation,
 * first creating edges with just names, then connecting up the edge by setting
 * its vertices based on those names. The second reason: 2) when qGraph is
 * creating intermediate representations of the query, the QGVertex may be a
 * CONSOLIDATED vertex that inside holds more than one element. When collapsing
 * a series of elements into a consolidated vertex, qGraph changes the edge so
 * that its end is now the new consolidated vertex; however, it needs to preserve
 * the name of the element to which the original edge was pointing to.
 */
public class QGEdge extends QGItem {

    /**
     * true if I'm directed; false if not.
     */
    private boolean isDirected;

    /**
     * QGVertex instances representing my vertex1 and vertex2. Initially null
     * (and not required by constructor) because we can't get them until all
     * QGVertex objects have been instantiated. Therefore it's *crucial* that
     * these be set via initQGVerticesFromQGItems() before being used.
     */
    private QGVertex vertex1 = null;
    private QGVertex vertex2 = null;

    /**
     * Names of vertices that I relate. Saved because needed by
     * initQGVerticesFromQGItems() when it's called to set vertex1 and
     * vertex2. NB: Clients should access vertices directly, instead of names,
     * when possible. NB: *Not* transient so that original vertex names can be
     * used by SQL. NB: direction is always FROM 1 TO 2!
     */
    private String vertex1Name;
    private String vertex2Name;


    /**
     * Full-arg constructor. Saves args in IVs. Throws IllegalArgumentException if
     * directedStr is invalid. NB: Make sure you call
     * initQGVerticesFromQGItems() after instantiating!
     */
    public QGEdge(String name, Element condEle, Element annotEle,
                  String vertex1Name, String vertex2Name, String directedStr)
            throws IllegalArgumentException {
        super(name, condEle, annotEle);
        Assert.condition(vertex1Name != null, "vertex1Name null");
        Assert.condition(vertex2Name != null, "vertex2Name null");
        Assert.condition(directedStr != null, "directedStr null");
        // continue
        this.vertex1Name = vertex1Name;
        this.vertex2Name = vertex2Name;
        // set my direction
        if (directedStr.equals("true"))
            this.isDirected = true;
        else if (directedStr.equals("false"))
            this.isDirected = false;
        else
            throw new IllegalArgumentException("invalid directed: '" +
                    directedStr + "' - wasn't one of: 'true', 'false'");
    }


    public boolean isDirected() {
        return isDirected;
    }


    /**
     * Utility that returns true if I cross a Subquery boundary, i.e., if my
     * vertices have different parent AbstractQuerys. Returns false o/w (i.e, if
     * does not cross a boundary).
     */
    public boolean isCrossesSubqBoundsEdge() {
        // NB: following test assumes that my parent AbstractQuery is
        // the same at least one of my vertices
        AbstractQuery v1ParentAQuery = vertex1().parentAQuery();
        AbstractQuery v2ParentAQuery = vertex2().parentAQuery();
        return (v1ParentAQuery != v2ParentAQuery);
    }


    /**
     * Returns true if I'm a self loop, i.e., if I link a vertex to itself.
     */
    public boolean isSelfLoop() {
        return (vertex1 == vertex2);
    }


    /**
     * QGItem method. Returns true because QGEdges can be asterisked.
     */
    public boolean isSupportsAsterisks() {
        return true;
    }


    /**
     * Returns vertex1 if vertex == vertex2, returns vertex2 if
     * vertex == vertex1, and throws IllegalArgumentException o/w.
     */
    public QGVertex otherVertex(QGVertex vertex) {
        if (vertex == vertex1)
            return vertex2;
        else if (vertex == vertex2)
            return vertex1;
        else
            throw new IllegalArgumentException("vertex neither vertex1 or 2: " +
                    vertex + ", " + this);
    }

    public void setDirected(boolean directed) {
        isDirected = directed;
    }


    /**
     * Sets my vertex1 to vertex. NB: Does *not* change my name; must be done
     * by caller. (The reason for this is compilcated and is tied up in how
     * query execution takes place.)
     *
     * @param vertex
     */
    public void setVertex1(QGVertex vertex) {
        Assert.condition(vertex != null, "vertex null");
        this.vertex1 = vertex;
    }


    /**
     * Utility used to help connect my vertices based on names, sets my vertex1Name to
     * vertexName.
     */
    public void setVertex1Name(String vertexName) {
        Assert.condition(vertexName != null, "vertex name null");
        // continue
        this.vertex1Name = vertexName;
    }


    /**
     * Sets my vertex2 to vertex. NB: Does *not* change my name; must be done
     * by caller. (The reason for this is compilcated and is tied up in how
     * query execution takes place.)
     *
     * @param vertex
     */
    public void setVertex2(QGVertex vertex) {
        Assert.condition(vertex != null, "vertex null");
        this.vertex2 = vertex;
    }


    /**
     * Utility used to help connect my vertices based on names, sets my vertex2Name to
     * vertexName.
     */
    public void setVertex2Name(String vertexName) {
        Assert.condition(vertexName != null, "vertex name null");
        // continue
        this.vertex2Name = vertexName;
    }


    public QGVertex vertex1() {
        return vertex1;
    }


    /**
     * NB: Should only be used by internal utilities. Instead use vertex1()
     * to get the object, not just the name.
     *
     * @return my vertex1 name
     */
    public String vertex1Name() {
        return vertex1Name;
    }


    public QGVertex vertex2() {
        return vertex2;
    }


    /**
     * NB: Should only be used by internal utilities. Instead use vertex2()
     * to get the object, not just the name.
     *
     * @return my vertex2 name
     */
    public String vertex2Name() {
        return vertex2Name;
    }


    /**
     * Object method. TEST
     */
    public String toString() {
        String dirStr = (isDirected ? "->" : "-");
        String condStr = (condEleChild() != null ? "c" : "");
        String asteriskStr = (isAsterisked() ? "*" + asteriskSourceToString() : "");
        return "[" + getClass().getName() + '@' + Integer.toHexString(hashCode()) +
                ": '" + catenatedName() + "' " + vertex1 + dirStr + vertex2 + " " +
                annotationString() + condStr + asteriskStr + "]";
    }


}
