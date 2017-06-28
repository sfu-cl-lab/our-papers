/**
 * $Id: AbstractQuery.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: AbstractQuery.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import kdl.prox.util.Assert;
import org.jdom.Element;

import java.io.Serializable;
import java.util.*;


/**
 * Abstract superclass to Query and Subquery. Represents a (possibly augmented)
 * query. Contains QGEdge and QGVertex instances that represent a QGraph 2.0 query.
 * We say "augmented" because it can contain QGCompVertex instances (consolidated
 * nodes). Global QGItem name uniqueness is guaranteed if the addXX() methods are
 * used. Works with the AbsQueryChild interface: All classes that can be contained
 * by me (QGItem - QGVertex and QGEdge - and Subquery) must implement
 * AbsQueryChild.
 */
public abstract class AbstractQuery implements Serializable {

    /**
     * The <constraint> Element associated with me. null if none specified.
     * NB: This is non-null only during creation in
     * QueryGraph2CompOp.queryFromGraphQueryEle(). All other users should
     * access my constraints instead.
     */
    private Element constEle = null;

    /**
     * My QGConstraint instances; Managed by addConstraint / removeConstraint
     */
    private List constraints = new ArrayList();


    /**
     * My QGEdge instances. Managed by addEdge().
     */
    private List edges = new ArrayList();

    /**
     * List of links to be added
     */
    private List addLinks = new ArrayList();


    /**
     * List of elements to be fetched from other containers while processing the query
     */
    private Map cachedItems = new HashMap();


    /**
     * My Subquery instances. Managed by addSubquery().
     */
    private List subqueries = new ArrayList();

    /**
     * My QGVertex instances. Managed by addVertex().
     */
    private List vertices = new ArrayList();

    /**
     Full-arg consructor. Sets my constEle to constEle.

     public AbstractQuery(Element constEle) {
     this.constEle = constEle;
     }*/


    /**
     * A cacophonic sounding method for adding an update edge to the query
     *
     * @param addLink
     */
    public void addAddLink(QGAddLink addLink) {
        addLinks.add(addLink);
        addLink.setParentAQuery(this);
    }

    public List addLinks() {
        return addLinks;
    }

    public void addCachedItem(String itemName, String containerName) {
        Assert.condition(!cachedItems.containsKey(itemName), itemName + " already in cache list");
        cachedItems.put(itemName, containerName);
    }

    /**
     * Adds constraint to my constraints, and sets its parent to me.
     */
    public void addConstraint(QGConstraint qgConstraint) {
        constraints.add(qgConstraint);
        qgConstraint.setParentAQuery(this);
    }


    /**
     * Adds edge to my edges, and sets its parent to me. Throws
     * IllegalArgumentException if qgEdge already in my edges.
     */
    public void addEdge(QGEdge qgEdge) {
        Assert.condition(isUniqueQGItem(qgEdge), "item not unique: " + qgEdge);
        boolean isAdded = edges.add(qgEdge);
        Assert.condition(isAdded, "qgEdge already in edges: " + qgEdge);
        qgEdge.setParentAQuery(this);
    }


    /**
     * Adds subquery to my subqueries, and sets its parent to me. Throws
     * IllegalArgumentException if subquery already in my subqueries.
     */
    public void addSubquery(Subquery subquery) {
        Assert.condition(isUniqueQGItems(subquery), "items not unique: " + subquery);
        boolean isAdded = subqueries.add(subquery);
        Assert.condition(isAdded, "subquery already in subqueries: " + subquery);
        subquery.setParentAQuery(this);
    }


    /**
     * Adds vertex to my vertices, and sets its parent to me. Throws
     * IllegalArgumentException if vertex already in my vertices. NB: It is the
     * caller's responsibility to call vertex.setParentAQuery() to this
     * AbstractQuery.
     */
    public void addVertex(QGVertex vertex) {
        Assert.condition(isUniqueQGItem(vertex), "item not unique: " + vertex);
        boolean isAdded = vertices.add(vertex);
        Assert.condition(isAdded, "vertex already in vertices: " + vertex);
        vertex.setParentAQuery(this);
    }


    public Map cachedItems() {
        return cachedItems;
    }

    public Element constEle() {
        return constEle;
    }


    public List constraints() {
        return constraints;
    }


    /**
     * Returns the first QGItem that equals qgItem in my. Returns null if none do.
     * This overload takes a QGItem.
     */
    QGItem duplicateQGItem(QGItem qgItem) {
        // check vertices
        QGItem duplicateQGItem = duplicateQGItem(qgItem, vertices);
        if (duplicateQGItem != null)
            return duplicateQGItem;
        // continue: check edges
        duplicateQGItem = duplicateQGItem(qgItem, edges);
        if (duplicateQGItem != null)
            return duplicateQGItem;
        // continue: recurse on my subqueries
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            duplicateQGItem = subquery.duplicateQGItem(qgItem);
            if (duplicateQGItem != null)
                return duplicateQGItem;
        }
        // continue: none found
        return null;
    }


    /**
     * Called by duplicateQGItem(QGItem), returns the first item in qgItems that
     * equals qgItemName. Returns null if none do. This overload takes a QGItem
     * and a Set of QGItem instances.
     */
    private QGItem duplicateQGItem(QGItem qgItem, List qgItems) {
        Iterator qgItemIter = qgItems.iterator();
        while (qgItemIter.hasNext()) {
            QGItem aQGItem = (QGItem) qgItemIter.next();
            if (qgItem.equals(aQGItem))
                return aQGItem;
        }
        // continue: none found
        return null;
    }


    /**
     * Returns QGEdge instances in me. If isRecurse is true then recurses on my
     * subqueries. Just returns my immediate edges o/w.
     * <p/>
     * todo use QueryIterator!
     */
    public List edges(boolean isRecurse) {
        if (!isRecurse)
            return Collections.unmodifiableList(edges);
        // continue: recurse
        List theEdges = new ArrayList(edges);       // returned value. filled next
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            theEdges.addAll(subquery.edges(true));  // isRecurse
        }
        // done
        return theEdges;
    }

    public Set getDisconnectedEdges() {
        HashSet disconnectedEdge = new HashSet();
        for (Iterator qgEdgeIter = edges.iterator(); qgEdgeIter.hasNext();) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            if ((qgEdge.vertex1() == null) || (qgEdge.vertex2() == null)) {
                disconnectedEdge.add(qgEdge);
            }
        }
        return disconnectedEdge;
    }


    /**
     * @return true if I have no items or subqueries. returns false o/w
     */
    public boolean isEmpty() {
        return (edges.size() == 0) &&
                (vertices.size() == 0) &&
                (subqueries.size() == 0);
    }


    /**
     * Utility that returns true if qgItem's names are globally unique within my
     * root Query. Returns false o/w.
     */
    private boolean isUniqueQGItem(QGItem qgItem) {
        Query rootQuery = rootQuery();
        QGItem dupQGItem = rootQuery.duplicateQGItem(qgItem);
        return (dupQGItem == null);
    }


    /**
     * Utility that returns true if all QGItem instances in subquery
     * are globally unique within my root Query. Returns false o/w.
     */
    private boolean isUniqueQGItems(Subquery subquery) {
        Set qgItems = subquery.qgItems(true);        // isRecurse
        Iterator qgItemIter = qgItems.iterator();
        while (qgItemIter.hasNext()) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            if (!isUniqueQGItem(qgItem))
                return false;    // found duplicate
        }
        // continue: none found
        return true;
    }

    /**
     Utility that returns true if the names of all QGItem instances in subquery
     are globally unique within my root Query. Returns false o/w.

     boolean isUniqueNames(Subquery subquery) {
     Set qgItems = subquery.qgItems(true);		// isRecurse
     Iterator qgItemIter = qgItems.iterator();
     while(qgItemIter.hasNext()) {
     QGItem qgItem = (QGItem)qgItemIter.next();
     if(!isUniqueName(qgItem))
     return false;	// found duplicate
     }
     // continue: none found
     return true;
     }*/


    /**
     * Returns QGItem instances in me. If isRecurse is true then recurses on my
     * subqueries. Just returns my edges and vertices o/w.
     */
    public Set qgItems(boolean isRecurse) {
        HashSet qgItems = new HashSet();    // returned value. filled next
        qgItems.addAll(edges);
        qgItems.addAll(vertices);
        // recurse, if necessary
        if (!isRecurse)
            return qgItems;
        // continue: recurse
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            qgItems.addAll(subquery.qgItems(true));        // isRecurse
        }
        // done
        return qgItems;
    }


    /**
     * Returns the first item in me that has exactly one name in its names, where
     * that name equals (honoring case) qgItemName. Checks in this order: vertices,
     * edges, then my subqueries (recurses). Returns null if no items have that
     * name. This overload takes a String.
     */
    public QGItem qgItemForName(String qgItemName) {
        // check vertices
        QGItem qgItemForName = qgItemForName(qgItemName, vertices);
        if (qgItemForName != null)
            return qgItemForName;
        // continue: check edges
        qgItemForName = qgItemForName(qgItemName, edges);
        if (qgItemForName != null)
            return qgItemForName;
        // continue: recurse on my subqueries
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            qgItemForName = subquery.qgItemForName(qgItemName);
            if (qgItemForName != null)
                return qgItemForName;
        }
        // continue: none found
        return null;
    }


    /**
     * Called by qgItemForName(String), returns the first item in me that has
     * exactly one name in its names, where that name equals (honoring case)
     * qgItemName. Returns null if no items have that name. This overload takes a
     * String and a Set of QGItem instances.
     */
    private QGItem qgItemForName(String qgItemName, List qgItems) {
        Iterator qgItemIter = qgItems.iterator();
        while (qgItemIter.hasNext()) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            if ((qgItem.names().size() == 1) &&
                    ((String) qgItem.names().get(0)).equals(qgItemName))
                return qgItem;
        }
        // continue: none found
        return null;
    }


    /**
     * Removes qgAddLink from me, and sets its parent to null. Throws
     * IllegalArgumentException if qgAddLink not in my add-links.
     */
    public void removeAddLink(QGAddLink qgAddLink) {
        boolean isRemoved = addLinks.remove(qgAddLink);
        Assert.condition(isRemoved, "qgAddLink not in constraints: " + qgAddLink);
        qgAddLink.setParentAQuery(null);
    }

    public void removeCachedItem(String itemName) {
        Assert.condition(cachedItems.containsKey(itemName), itemName + " not in cached list");
        cachedItems.remove(itemName);
    }

    /**
     * Removes qgConstraint from me, and sets its parent to null. Throws
     * IllegalArgumentException if qgConstraint not in my constraints.
     */
    public void removeConstraint(QGConstraint qgConstraint) {
        boolean isRemoved = constraints.remove(qgConstraint);
        Assert.condition(isRemoved, "qgConstraint not in constraints: " + qgConstraint);
        qgConstraint.setParentAQuery(null);
    }


    /**
     * Removes qgEdge from my edges, and sets its parent to null. Throws
     * IllegalArgumentException if qgEdge not in my edges.
     */
    public void removeEdge(QGEdge qgEdge) {
        boolean isRemoved = edges.remove(qgEdge);
        Assert.condition(isRemoved, "qgEdge not in edges: " + qgEdge);
        qgEdge.setParentAQuery(null);
    }


    /**
     * Removes subquery from my subqueries, and sets its parent to null. Throws
     * IllegalArgumentException if subquery not in my subqueries.
     */
    public void removeSubquery(Subquery subquery) {
        boolean isRemoved = subqueries.remove(subquery);
        Assert.condition(isRemoved, "subquery not in subqueries: " + subquery);
        subquery.setParentAQuery(null);
    }


    /**
     * Removes vertex from my vertices, and sets its parent to null. Throws
     * IllegalArgumentException if vertex not in my vertices.
     */
    public void removeVertex(QGVertex vertex) {
        boolean isRemoved = vertices.remove(vertex);
        Assert.condition(isRemoved, "vertex not in vertices: " + vertex);
        vertex.setParentAQuery(null);
    }


    /**
     * Abstract method that returns the top-level Query that contains me.
     */
    public abstract Query rootQuery();


    /**
     * Sets my constEle to constEle. NB: In order to make serialization-based
     * copying more efficient, constEle.detach() should be called by the
     * caller. It is not done here because it causes ConcurrentModificationException
     * while iterating in QueryGraph2CompOp.queryFromQueryBodyEles().
     */
    public void setConstEle(Element constEle) {
        this.constEle = constEle;
    }


    /**
     * Returns Subquery instances in me. If isRecurse is true then recurses on my
     * subqueries. Just returns my immediate subqueries o/w.
     * <p/>
     * todo use QueryIterator!
     */
    public List subqueries(boolean isRecurse) {
        if (!isRecurse)
            return Collections.unmodifiableList(subqueries);
        // continue: recurse
        List theSubqueries = new ArrayList(subqueries);    // returned value. filled next
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            theSubqueries.addAll(subquery.subqueries(true));    // isRecurse
        }
        // done
        return theSubqueries;
    }


    /**
     * Returns QGVertex instances in me. If isRecurse is true then recurses on my
     * subqueries. Just returns my immediate vertices o/w.
     * <p/>
     * todo use QueryIterator!
     */
    public List vertices(boolean isRecurse) {
        if (!isRecurse)
            return Collections.unmodifiableList(vertices);
        // continue: recurse
        List theVertices = new ArrayList(vertices);    // returned value. filled next
        Iterator subqIter = subqueries.iterator();
        while (subqIter.hasNext()) {
            Subquery subquery = (Subquery) subqIter.next();
            theVertices.addAll(subquery.vertices(true));    // isRecurse
        }
        // done
        return theVertices;
    }
}
