/**
 * $Id: QGItem.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QGItem.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;


/**
 * Abstract superclass to QGEdge and QGVertex, defines useful IVs and methods.
 * NB: This should probably be named ProxElement or GraphElement, but "element" is
 * too easily confused with XML and collection elements. So we use "item", which
 * could be confused with the data (as opposed to graph) Item, but too bad. The
 * names of this and subclasses start with QG (for "QGraph") to differentiate them
 * from vertices and edges in transformation (or other) graphs. Implements
 * AbsQueryChild because QGItems (QGVertices and QGEdges) can be stored in an
 * AbstractQuery. Implements Comparable so that lists of QGItems can be sorted
 * (used by Query.equalsQuery()).
 * <p/>
 * Regarding processing asterisk support: See my asteriskSource below.
 */
public abstract class QGItem implements AbsQueryChild, Comparable, Serializable {

    protected static Logger log = Logger.getLogger(QGItem.class);


    /**
     * My Annotation. null if none specified.
     */
    private Annotation annotation;

    /**
     * Used by classes that support asterisks (see isSupportsAsterisks(), holds my
     * source annotated QGVertex's name and the name of the annotated item in the QGVertex,
     * and the actual annotation on the vertex,
     * or null if I have no asterisk or if asterisks aren't supported.
     * Asterisks are used to indicate that an edge or consolidated vertex was absorbed
     * and needs further processing to finish.
     * See asteriskSource() and setAsteriskSource().
     * <p/>
     * If the asterisk source is a non-consolidated vertex, the name of the vertex
     * and the actual annotation are those in the original query. But if the asterisk
     * source is a consolidated vertex (probably derived from a sub-query), we need
     * to store the actual item on which the annotation should be checked, and the
     * annotation of the consolidated vertex, not on the original item in the original
     * query.
     * <p/>
     * Regarding what is stored: We save the String name of the source annotated
     * non-consolidated QGVertex that was the origin of this asterisk. This value
     * is used by Transformation execution. The storing of a String (rather than a
     * pointer to the actual vertex) allows our serialization-based copy method to
     * not* copy too much - if we stored the pointer, all kinds of objects would
     * be saved when following the pointer back to its Query.
     */
    private String asteriskSource = null;
    private String asteriskSourceItemName = null;
    private int asteriskSourceAnnotMax;

    /**
     * My <condition> element's first child, which is an <or>, <and>, <not>, or
     * <test> element. null if none specified. Set by constructor.
     */
    private Element condEleChild = null;

    /**
     * A sorted List of my names. We use a List, rather than a single name, so that
     * ConsQGVertex instances can save their component names. The List is sorted to
     * make compareTo() simpler. Initially filled by constructor and managed by
     * addName(). NB: Conditions apply to the number of names based on whether I'm
     * consolidated or not: ConsQGVertexS have one or more names. Non-ConsQGVertexS
     * have exactly one name. This condition is enforced by addAllNames().
     */
    private List names = new ArrayList();

    /**
     * The Subquery or Query that contains me. Set by setParentAQuery(). null if
     * not set.
     */
    private AbstractQuery parentAQuery = null;


    /**
     * Three-arg constructor. Saves args in IVs. We assume name is valid. A null
     * condEle means there is no test. Pass null for annotEle to specify none.
     * This String overload takes a single name which is added to my names.
     */
    QGItem(String name, Element condEle, Element annotEle)
            throws IllegalArgumentException {
        this(nameListForName(name), condEle, annotEle);
    }


    /**
     * Three-arg constructor. This List overload takes a List of names, each of
     * which is added to my names. NB: In order to make serialization-based
     * copying more efficient, condEleChild.detach() should be called by the
     * caller. It is not done here because it causes ConcurrentModificationException
     * while iterating in QueryGraph2CompOp.queryFromQueryBodyEles().
     */
    QGItem(List nameList, Element condEle, Element annotEle)
            throws IllegalArgumentException {
        if (condEle != null) {
            condEleChild = (Element) condEle.getChildren().get(0);
//			condEleChild.detach();
        }
        this.annotation = (annotEle != null ? new Annotation(annotEle) : null);
        // add the names
        addAllNames(nameList);
        Collections.sort(names);
    }


    /**
     * Adds all names in nameList to my names, first ensuring that it's ok to
     * do so (see names IV docs for conditions).
     *
     * @param nameList
     */
    private void addAllNames(List nameList) {
        Assert.condition((this instanceof ConsQGVertex) ||
                (names.size() + nameList.size() == 1),
                "too many names for vertex type: " + this + ", " + nameList.size());
        names.addAll(nameList);
    }


    /**
     * Used by Transformations that call absorbEdgeAndVertex() but not
     * replaceQGVertex() after, adds all of qgItem's names to my names, then sorts
     * my names.
     */
    public void addNames(QGItem qgItem) {
        Assert.condition(qgItem != null, "qgItem null");
        // continue
        addAllNames(qgItem.names());
        Collections.sort(names);
    }


    /**
     * Returns my Annotation.
     */
    public Annotation annotation() {
        return annotation;
    }


    /**
     * Utility method that returns a pretty string for my annotation. Returns ""
     * if null.
     */
    public String annotationString() {
        return (annotation() == null ? "" : annotation().annotationString());
    }


    /**
     * Utility that asserts isSupportsAsterisks().
     */
    private void assertSupportsAsterisks() {
        Assert.condition(isSupportsAsterisks(), "doesn't support asterisks");
    }


    /**
     * Returns my asteriskSource. Throws IllegalArgumentException if I don't
     * support asterisks.
     */
    public String asteriskSource() {
        assertSupportsAsterisks();
        return asteriskSource;
    }


    /**
     * Returns the upper bound of the annotation on the asterisk
     *
     * @return
     */
    public int asteriskSourceAnnotMax() {
        assertSupportsAsterisks();
        return asteriskSourceAnnotMax;
    }


    /**
     * Returns the name of the asterisked item in the asteriskSource vertex.
     * Throws IllegalArgumentException if I don't support asterisks.
     */
    public String asteriskSourceItemName() {
        assertSupportsAsterisks();
        return asteriskSourceItemName;
    }


    /**
     * Shows the name of the asterisk source QGVertex, and the name of the  asterisked
     * item within that vertex, if any.
     *
     * @return
     */
    public String asteriskSourceToString() {
        if (!isAsterisked()) {
            return "";
        }
        if (asteriskSource().equals(asteriskSourceItemName())) {
            return asteriskSource() + "[" + asteriskSourceAnnotMax() + "]";
        } else {
            return asteriskSource() + "(" + asteriskSourceItemName() + ")"
                    + " [" + asteriskSourceAnnotMax() + "]";
        }
    }


    /**
     * Returns the catenation of my names.
     */
    public String catenatedName() {
        StringBuffer namesSB = new StringBuffer();	// filled next
        Iterator nameIter = names.iterator();
        while (nameIter.hasNext()) {
            String name = (String) nameIter.next();
            namesSB.append(name);
            namesSB.append(".");
        }
        if (namesSB.length() != 0)
            namesSB.setLength(namesSB.length() - 1);	// remove final "."
        return namesSB.toString();
    }


    /**
     * Clears the asterisk source
     */
    public void clearAsteriskSource() {
        this.asteriskSource = null;
    }


    /**
     * Comparable method. Two QGItems are compared based on their catenated names,
     * which separate components by ".". Thus, we can simply delegate the
     * comparison to String.
     */
    private int compareTo(QGItem qgItem) {
        return catenatedName().compareTo(qgItem.catenatedName());
    }


    /**
     * Compares this QGItem to another Object.  If the Object is a QGItem,
     * this function behaves like <code>compareTo(String)</code>. Otherwise,
     * it throws a <code>ClassCastException</code> (as QGItems are comparable
     * only to other QGItems).
     */
    public int compareTo(Object o) {
        return compareTo((QGItem) o);
    }


    /**
     * Returns my condEleChild.
     */
    public Element condEleChild() {
        return condEleChild;
    }

    /**
     * Eliminate the annotation on this QGItem
     */
    public void deleteAnnotation() {
        this.annotation = null;
    }

    /**
     * Eliminate the condEleChild on this QGItem
     */
    public void deleteCondEleChild() {
        this.condEleChild = null;
    }


    /**
     * Object method. Returns true if object is a QGItem whose names equals mine,
     * via set difference. Also, asterisk sources must be equal. Returns false o/w.
     * <p/>
     * todo define hashCode()!
     */
    public boolean equals(Object anObject) {
        if (this == anObject)
            return true;
        if (!(anObject instanceof QGItem))
            return false;
        if (getClass() != anObject.getClass())
            return false;		// different class
        // continue: compare QGItems
        QGItem qgItem = (QGItem) anObject;
        Set difference = new HashSet(names);		// corrected next
        difference.removeAll(qgItem.names());
        boolean isDifferentAsterisks = (isSupportsAsterisks() &&
                !isSameAsteriskSource(qgItem));
        return (difference.isEmpty() && !isDifferentAsterisks);
    }


    /**
     * Utility that returns the first name in my names.
     *
     * @return
     */
    public String firstName() {
        return (String) (names.iterator().next());
    }


    /**
     * Utility that returns true if I'm annotated, and false o/w.
     */
    public boolean isAnnotated() {
        return (annotation != null);
    }


    /**
     * Utility that returns true if qgItem has an asterisk, i.e, if it can be
     * asterisked, and its asteriskSource() != null. Returns false o/w.
     */
    public boolean isAsterisked() {
        return (isSupportsAsterisks()) && (asteriskSource() != null);
    }


    /**
     * Utility that returns true if qgItem and I have the same asteriskSource.
     * Returns false o/w. Does not test whether qgItem and I are of the same class.
     * Throws IllegalArgumentException if I don't support asterisks.
     */
    public boolean isSameAsteriskSource(QGItem qgItem) {
        assertSupportsAsterisks();
        // continue
        return (((asteriskSource == null) && (qgItem.asteriskSource() == null)) ||
                (((asteriskSource != null) && (qgItem.asteriskSource() != null)) &&
                asteriskSource.equals(qgItem.asteriskSource())) &&
                asteriskSourceItemName.equals(qgItem.asteriskSourceItemName()));
    }


    /**
     * Returns true if my class supports asterisks. Returns false o/w. This (the
     * default) version returns false, disabling asterisks. Subclasses that support
     * asterisks must define this method to return true. We use a static method to
     * allow "turning on" asterisk support on a per-class basis. This allows us to
     * put all asterisk-related code here in QGItem. If we used the alternative
     * approach - define an interface that each specific subclass must implement,
     * we'd have to duplicate code in each subclass, or create a delagate. It seems
     * worth the tradeoff of wasted space for instances that don't support
     * asterisks... Tough call without multiple inheritance.
     */
    public boolean isSupportsAsterisks() {
        return false;
    }


    /**
     * Called by constructor, returns a List of one item - name.
     */
    private static List nameListForName(String name) {
        List nameList = new ArrayList();	// return value. name added next
        nameList.add(name);
        return nameList;
    }


    /**
     * Returns a copy of my names.
     */
    public List names() {
        return Collections.unmodifiableList(names);
    }


    /**
     * AbsQueryChild method. Returns my parentAQuery.
     */
    public AbstractQuery parentAQuery() {
        return parentAQuery;
    }


    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }


    /**
     * Sets my asteriskSource to a vertex that has a single name, and
     * it therefore can set the asteriskedItemName to the first name of the vertex
     *
     * @param annotQGVertex
     */
    public void setAsteriskSource(QGVertex annotQGVertex) {
        Assert.notNull(annotQGVertex, "null vertex");
        Assert.condition(annotQGVertex.names().size() == 1, "QGVertex has more than" +
                "one name : " + annotQGVertex);
        setAsteriskSource(annotQGVertex, annotQGVertex.firstName());
    }


    /**
     * Sets my asteriskSource to the specified itemName in annotQGVertex.
     * Throws IllegalArgumentException if I don't support asterisks, or if
     * annotQGVertex does not have an annotation.
     */
    public void setAsteriskSource(QGVertex annotQGVertex, String itemName) {
        assertSupportsAsterisks();
        Assert.notNull(annotQGVertex, "null vertex");
        Assert.notNull(annotQGVertex.annotation(),
                "annotQGVertex not a Vertex with an annotation");
        Assert.stringNotEmpty(itemName, "empty item name");
        Assert.condition(annotQGVertex.names().contains(itemName), "item not in " +
                "vertex: " + itemName);
        this.asteriskSource = annotQGVertex.catenatedName();
        this.asteriskSourceItemName = itemName;
        this.asteriskSourceAnnotMax = annotQGVertex.annotation().annotMax();
    }


    /**
     * Updates the value of the condEleChild by passing in a ConditionElement
     */
    public void setCondition(Element condEle) {
        if (condEle != null) {
            condEleChild = (Element) condEle.getChildren().get(0);
            //condEleChild.detach();
        }
    }

    /**
     * Used by the QueryEditor to set the first name for a QGItem
     * The first name should be updated on an edit rather than appended
     * NOTE: An editor query is always unconsolidated and will only ever
     * have one name.
     */

    public void setFirstName(String newName) {
        names.set(0, newName);
    }


    /**
     * AbsQueryChild method. Sets my parentAQuery to parentAQuery. Works with
     * AbstractQuery.addXX() methods. Pass null to clear it.
     */
    public void setParentAQuery(AbstractQuery parentAQuery) {
        this.parentAQuery = parentAQuery;
    }


}
