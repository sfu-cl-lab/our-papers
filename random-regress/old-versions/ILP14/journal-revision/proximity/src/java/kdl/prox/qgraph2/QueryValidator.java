/**
 * $Id: QueryValidator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QueryValidator.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

*/

package kdl.prox.qgraph2;

import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.*;


/**
 * A helper class used by QueryGraph2CompOp to validate newly-created QueryS.
 * The constructor does the checking.
 * <p/>
 * Our approach to saving validation errors is to store each invalid aspect as
 * a string in a List, then throw a ValidationError if there are any strings.
 * In other words, we don't stop processing at the first exception.
 * <p/>
 * Following are the specific things checked, and where they are checked. They
 * are referred to by name (e.g., "b.4.a") in the methods that check them.
 * <p/>
 * <p/>
 * a. constraints not enforced by the DTD:
 * <p/>
 * 1. <!ELEMENT directed (#PCDATA)>
 * <!-- possible values: "true", "false" -->
 * ... checked by QGEdge()
 * <p/>
 * 2. <!ELEMENT numeric-annotation (min, max?)>
 * <!-- min and max are whole numbers, and min <= max. valid combinations: [i..j], [i..], or [i] -->
 * ... checked by Annotation()
 * <p/>
 * 3. <!ELEMENT operator (#PCDATA)>
 * <!-- possible values: "eq", "ge", "le", , "ne", "exists" -->
 * ... checked by checkTestEle()
 * <p/>
 * 4. <!ELEMENT test (operator, %operand;, (%operand;)?)>
 * <!-- test operands should be consistent: two items, or an attribute-name and a value -->
 * <!-- operator "exists" requires a single operand -->
 * ... checked by checkTestEle()
 * <p/>
 * 5. <!ELEMENT constraint %dnf;>
 * <!-- constraints should only test between pairs of items (two vertices or two edges)
 * We use DNF in the DTD for consistency, but in reality only <AND>s can be used in constraints
 * as binary operators (a single TEST elements is OK, but if there are two or more, they have to
 * be combined with <AND>)
 * Also, no constrainsts involving two annotated edges are allowed.
 * -->
 * ... checked by checkTestEleConst() and checkDNFEle
 * <p/>
 * 6. <!ELEMENT condition %dnf;>
 * <!-- conditions should only compare attribute names and values -->
 * ... checked by checkTestEleCond()
 * <p/>
 * <p/>
 * b. from Hannah's well-formed.txt:
 * <p/>
 * 1. Query must remain connected (without negated elements). This can be
 * tested by making a copy, removing from it all negated elements
 * (vertices, edges, and subqueries), and testing whether the copy is
 * connected.
 * ... checked by checkNonNegatedConnComps()
 * <p/>
 * 2. Every query must have at least one vertex.
 * ... checked by checkAtLeastOneVertex()
 * <p/>
 * 3. Every edge must have vertices at both ends.
 * ... checked by checkEdge()
 * <p/>
 * 4. The inner structure of a subquery should be a well-formed query by
 * itself. Two corollaries:
 * a. subquery must remain connected (without boundary edges)
 * ... checked by checkNonNegatedConnComps()
 * b. single edge is not a legal subquery
 * ... checked by checkAtLeastOneVertex()
 * <p/>
 * 5. No edge or constraint between two annotated elements:
 * a. at most one of any two adjacent vertices can be annotated
 * ... checked by checkEdge()
 * b. no constraint between two vertices (or edges) that are both annotated
 * ... checked by checkConstEleChild()
 * c. no numeric annotation allowed on a vertex adjacent to an annotated subquery
 * ... checked by checkEdge()
 * d. no edge can connect one annotated subquery to another
 * ... checked by checkEdge()
 * <p/>
 * 6. An edge incident to an annotated element must itself be annotated (default [1..]).
 * a. edge incident to an annotated vertex must be annotated
 * ... checked by checkEdge()
 * b. boundary edge of an annotated subquery must be annotated
 * ... checked by checkEdge()
 * <p/>
 * 7. Constraint within a subquery can only reference vertices/edges within
 * that subquery and enclosed subqueries
 * Actually, as of 2004.1.6, constraints can only reference vertices/edges within the same subquery
 * ... checked by checkTestEleConst()
 * <p/>
 * 8. No annotation allowed on boundary vertex of subquery.
 * ... checked by checkEdge()
 * <p/>
 * <p/>
 * c. other items:
 * <ul>
 * <li>1. annotated vertices require all incident edges to be annotated (no UA cases)
 * ... checked by checkAnnotVertex()
 * <li>2. verify that at least one of every edge's vertices is in the same parent
 * AbstractQuery as the edge itself
 * ... checked by checkEdge()
 * <li>3. verify no nested subqueries
 * ... checked by checkNestedSubquery()
 * <li>4. verify no duplicate item names
 * ... checked by checkDuplicateItemNames()
 * <li>5. edge min annotation cannot be 0. in theory they are allowed, but our implementation cannot handle them
 * ... checked by checkEdge()
 * <li>6. edge and vertex names cannot be empty (i.e., "")
 * ... checked by checkForEmptyName()
 * </ul>
 */
public class QueryValidator {

    private static Logger log = Logger.getLogger(QueryValidator.class);

    /**
     * A List of Strings specifying all invalid aspects of my Query. Filled by
     * walkQuery() and friends via addErrorString().
     */
    private List errorStrings = new ArrayList();


    /**
     * Full-arg constructor. Checks query's validity. Throws QGValidationError
     * if invalid. Otherwise (valid) returns normally.
     *
     * @param query the Query whose validity is to be checked
     * @throws QGValidationError if invalid
     */
    public QueryValidator(Query query) throws QGValidationError {
        // fill errorStrings
        walkQuery(query);
        checkDuplicateItemNames(query);
        try {
            checkNonNegatedConnComps(query);
        } catch (Exception exc) {
            addErrorString("internal error copying query: " + exc);
        }

        // check errorStrings
        if (errorStrings.size() != 0) {
            throw new QGValidationError(query, errorStrings);
        }
    }


    /**
     * Called by checkXX() (and other) methods, adds errorString to my
     * errorStrings.
     *
     * @param errorString
     */
    private void addErrorString(String errorString) {
        Assert.condition((errorString != null) && (errorString.length() != 0),
                "null or empty errorString");
        // continue
//        log.error("validation error: " + errorString);
        errorStrings.add(errorString);
    }


    /**
     * Called by walkQuery(), checks c.1.
     *
     * @param qgVertex vertex to check
     */
    private void checkAnnotVertex(QGVertex qgVertex) {
        if (!qgVertex.isAnnotated())
            return;
        // continue
        Iterator qgEdgeIter = qgVertex.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdgeY = (QGEdge) qgEdgeIter.next();
            if (!qgEdgeY.isAnnotated())
                addErrorString("annotated vertices require all incident " +
                        "edges to be annotated. Vertex: " + qgVertex.firstName() +
                        "Edge: " + qgEdgeY.firstName());
        }
    }


    /**
     * Called by walkQuery(), checks b.2. If invalid then adds to my
     * errorStrings.
     *
     * @param abstractQuery
     */
    private void checkAtLeastOneVertex(AbstractQuery abstractQuery) {
        int numVerts = abstractQuery.vertices(false).size();    // isRecurse
        if (numVerts == 0)
            addErrorString((abstractQuery instanceof Query ? "query" : "subquery") +
                    " has zero vertices, but should have at least one");
    }


    /**
     * Called by walkQuery(), checks condEleChild via checkDNFEle().
     *
     * @param condEleChild <condition> element child to check
     */
    private void checkCondEleChild(Query query, Element condEleChild) {
        checkDNFEle(query, condEleChild, true);    // isCondEle
    }


    private void checkConstraintItems(Query query, String item1Name,
                                      String item2Name, String errorPrefix) {
        QGItem qgItem1 = query.qgItemForName(item1Name);
        QGItem qgItem2 = query.qgItemForName(item2Name);

        // check that they exist
        if (qgItem1 == null) {
            addErrorString(errorPrefix + ": unknown item: " + item1Name);
        }
        if (qgItem2 == null) {
            addErrorString(errorPrefix + ": unknown item: " + item2Name);
        }

        if ((qgItem1 != null) && (qgItem2 != null)) {
            // check that they are of the same type
            if (qgItem1.getClass() != qgItem2.getClass()) {
                addErrorString(errorPrefix + ": inconsistent operands: " +
                        item1Name + "(" + qgItem1.getClass() + ")" + ", " +
                        item2Name + "(" + qgItem2.getClass() + ")" +
                        ". constraints should only test equality " +
                        "between pairs of vertex or edge labels, with no mixing");
            }

            // Check that only one is annotated, at most
            if (qgItem1.isAnnotated() && qgItem2.isAnnotated()) {
                addErrorString(errorPrefix + ": only one item in the " +
                        "constraint can be annotated: " + item1Name);
            }

            // Check that if one is annotated, the other one does not belong to an annotated sub-query
            if (qgItem1.isAnnotated() && (qgItem2.parentAQuery() instanceof Subquery)) {
                Subquery subquery = (Subquery) qgItem2.parentAQuery();
                if (subquery.annotation() != null) {
                    addErrorString(errorPrefix + ": only one item in the " +
                            "constraint can be annotated: " + item1Name + " is annotated, and " +
                            item2Name + " is in an annotated sub-query");
                }
            }
            if (qgItem2.isAnnotated() && (qgItem1.parentAQuery() instanceof Subquery)) {
                Subquery subquery = (Subquery) qgItem1.parentAQuery();
                if (subquery.annotation() != null) {
                    addErrorString(errorPrefix + ": only one item in the " +
                            "constraint can be annotated: " + item2Name + " is annotated, and " +
                            item1Name + " is in an annotated sub-query");
                }
            }

            // check that, if one is annotated,
            // they are vertices on the same sub-query, or the annotated item is [0..], or if
            // they are edges, the annotated edge is annotated with [0..]
            if (qgItem1.isAnnotated() || qgItem2.isAnnotated()) {
                QGItem annotatedItem = (qgItem1.isAnnotated() ? qgItem1 : qgItem2);

                if (annotatedItem instanceof QGEdge) {
                    QGEdge annotatedEdge = (QGEdge) annotatedItem;
                    QGVertex annotatedVertex = null;
                    if (annotatedEdge.vertex1().isAnnotated()) {
                        annotatedVertex = annotatedEdge.vertex1();
                    } else if (annotatedEdge.vertex2().isAnnotated()) {
                        annotatedVertex = annotatedEdge.vertex2();
                    }
                    if (annotatedVertex == null || annotatedVertex.annotation().annotMin() != 0) {
                        addErrorString(errorPrefix + ": In a constraint " +
                                "involving an annotated link, at least of the " +
                                "end vertices has to be annotated with [0..]: " +
                                item1Name + ", " + item2Name);
                    }
                } else if (annotatedItem instanceof QGVertex) {
                    // check that they are in the same sub-query, or that at least the annot item
                    // is [0..]
                    Annotation annotation = annotatedItem.annotation();
                    if (qgItem1.parentAQuery() != qgItem2.parentAQuery() && annotation.annotMin() != 0) {
                        addErrorString(errorPrefix + ": In a constraint that " +
                                "spans multiple sub-queries, one of the items " +
                                "has to be annotated with [0..]: " + item1Name +
                                ", " + item2Name);
                    }
                }
            } else {
                // if they are not annotated, they have to be in the same sub-query
                if (qgItem1.parentAQuery() != qgItem2.parentAQuery()) {
                    addErrorString(errorPrefix + ": In a constraint that spans " +
                            "multiple sub-queries, one of the items has to be " +
                            "annotated with [0..]: " + item1Name + ", " + item2Name);
                }
            }
        }
    }


    /**
     * Called by checkCondEleChild() and checkCondEleChild(), checks dnfEle,
     * which is either an <or>, <and>, <not>, or <test>. recall:
     * <p/>
     * <or> contains one or more <and> elements
     * <and> contains one or more <not> or <test> elements
     * <not> contains one <test> element
     * <p/>
     * Eventually calls checkTestEle() on <test> elements.
     *
     * @param dnfEle element to check
     */
    private void checkDNFEle(Query query, Element dnfEle, boolean isCondEle) {
        if ((dnfEle.getName().equals("or")) || (dnfEle.getName().equals("and"))) {
            // recurse on children.
            List children = dnfEle.getChildren();
            Iterator childIter = children.iterator();
            while (childIter.hasNext()) {
                Element childElement = (Element) childIter.next();
                checkDNFEle(query, childElement, isCondEle);
            }
        } else if (dnfEle.getName().equals("not")) {
            Element testEle = (Element) dnfEle.getChildren().get(0);
            checkDNFEle(query, testEle, isCondEle);
        } else {        // <test>
            Element testEle = dnfEle;
            List childEles = testEle.getChildren();
            Element operatorEle = (Element) childEles.get(0);
            Element operand1Ele = (Element) childEles.get(1);
            Element operand2Ele = null;
            if (childEles.size() > 2) {
                operand2Ele = (Element) childEles.get(2);
            }
            checkTestEle(query, operatorEle, operand1Ele, operand2Ele, isCondEle);
        }
    }


    /**
     * Checks c.4, updating errorStrings if invalid.
     *
     * @param abstractQuery
     */
    private void checkDuplicateItemNames(AbstractQuery abstractQuery) {
        Set qgItems = abstractQuery.qgItems(true);  // includes items with duplicate names

        List allQGItemNames = new ArrayList();  // ex: a, b, b
        Collections.sort(allQGItemNames);
        for (Iterator qgItemIter = qgItems.iterator(); qgItemIter.hasNext();) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            allQGItemNames.add(qgItem.firstName());
        }

        Set uniqueQGItemNames = new HashSet(allQGItemNames);    // ex: a, b
        if (allQGItemNames.size() != uniqueQGItemNames.size()) {
            addErrorString("found " + (allQGItemNames.size() - uniqueQGItemNames.size()) +
                    " duplicate item name(s): " + allQGItemNames);
        }
    }


    /**
     * Called by walkQuery(), checks: b.3, b.5.a, b.5.c, b.5.d, b.6.a, b.6.b,
     * b.8, c.2, c5. If invalid then adds to my errorStrings.
     *
     * @param qgEdge the edge to check
     */
    private void checkEdge(QGEdge qgEdge) {
        QGVertex qgVertex1 = qgEdge.vertex1();
        QGVertex qgVertex2 = qgEdge.vertex2();

        // check b.3
        if ((qgVertex1 == null) || (qgVertex2 == null)) {
            addErrorString("one or both edge vertices are invalid. Vertex 1: " +
                    qgEdge.vertex1Name() + " (" + (qgVertex1 == null ? "bad" : "ok") +
                    "), Vertex 2: " + qgEdge.vertex2Name() + " (" +
                    (qgVertex2 == null ? "bad" : "ok") + ")");
            return;
        }

        // check b.5.a
        if (qgVertex1.isAnnotated() && qgVertex2.isAnnotated())
            addErrorString("at most one of any two adjacent vertices can be " +
                    "annotated. Vertex 1: " + qgVertex1.firstName() +
                    ", Vertex 2: " + qgVertex2.firstName());
        // check b.5.c and check b.5.d
        AbstractQuery v1ParentAQ = qgVertex1.parentAQuery();
        AbstractQuery v2ParentAQ = qgVertex2.parentAQuery();
        if (qgEdge.isCrossesSubqBoundsEdge()) {
            // b.5.c
            boolean isV1Invalid = (qgVertex1.isAnnotated() && (v2ParentAQ instanceof Subquery) &&
                    ((Subquery) v2ParentAQ).isAnnotated());
            boolean isV2Invalid = (qgVertex2.isAnnotated() && (v1ParentAQ instanceof Subquery) &&
                    ((Subquery) v1ParentAQ).isAnnotated());
            if (isV1Invalid || isV2Invalid)
                addErrorString("no numeric annotation allowed on a vertex " +
                        "adjacent to an annotated subquery: " +
                        (isV1Invalid ? qgVertex1.firstName() : qgVertex2.firstName()));
            // b.5.d
            boolean isV1AnnotSubq = ((v1ParentAQ instanceof Subquery) &&
                    ((Subquery) v1ParentAQ).isAnnotated());
            boolean isV2AnnotSubq = ((v2ParentAQ instanceof Subquery) &&
                    ((Subquery) v2ParentAQ).isAnnotated());
            if (isV1AnnotSubq && isV2AnnotSubq)
                addErrorString("no edge can connect one annotated subquery " +
                        "to another: " + qgEdge.firstName());
        }
        // check b.6.a
        if ((qgVertex1.isAnnotated() || qgVertex2.isAnnotated()) &&
                !qgEdge.isAnnotated())
            addErrorString("edge incident to an annotated vertex must be " +
                    "annotated: " + qgEdge.firstName());
        // check b.6.b
        if (qgEdge.isCrossesSubqBoundsEdge() && !qgEdge.isAnnotated())
            addErrorString("boundary edge of an annotated subquery must be " +
                    "annotated: " + qgEdge.firstName());
        // check b.8
        if (qgEdge.isCrossesSubqBoundsEdge()) {
            boolean isV1Invalid = ((v1ParentAQ instanceof Subquery) && qgVertex1.isAnnotated());
            boolean isV2Invalid = ((v2ParentAQ instanceof Subquery) && qgVertex2.isAnnotated());
            if (isV1Invalid || isV2Invalid)
                addErrorString("no annotation allowed on boundary vertex of " +
                        "subquery: " + (isV1Invalid ? qgVertex1.firstName() :
                        qgVertex2.firstName()));
        }
        // check c.2
        AbstractQuery edgeParentAQ = qgEdge.parentAQuery();
        if ((edgeParentAQ != v1ParentAQ) && (edgeParentAQ != v2ParentAQ)) {
            addErrorString("at least one of every edge's vertices is in the " +
                    "same parent query/subquery as the edge itself. Edge: " +
                    qgEdge.firstName());
        }

        // check c.5
        if (qgEdge.isAnnotated()) {
            if (qgEdge.annotation().annotMin() == 0) {
                addErrorString("edge min numeric annotation cannot be 0. Edge: " +
                        qgEdge.firstName());
            }
        }
    }


    private void checkForEmptyName(QGItem qgItem) {
        if ((qgItem.firstName() != null) && (qgItem.firstName().length() == 0)) {
            addErrorString("vertex or edge name cannot be empty");
        }
    }


    /**
     * Checks c.3, updating errorStrings if invalid.
     *
     * @param abstractQuery
     */
    private void checkNestedSubquery(AbstractQuery abstractQuery) {
        if ((abstractQuery instanceof Subquery) &&
                (((Subquery) abstractQuery).parentAQuery() instanceof Subquery)) {
            addErrorString("found a subquery whose parent was a itself a " +
                    "subquery (nested subqueries not supported)");
        }
    }


    /**
     * Called by constructor, checks b.1. If invalid then adds to my errorStrings.
     *
     * @param query
     * @throws Exception if problems copying query
     */
    private void checkNonNegatedConnComps(Query query) throws Exception {
        Query queryCopy = (Query) (ObjectCloner.deepCopy(query));    // throws Exception
        removeNegatedElements(queryCopy);
        if (!isConnected(queryCopy)) {
            addErrorString("query (without negated elements) is not connected");
        }
    }


    private void checkQGConstraints(List qgConstraints, Query query) {
        for (Iterator qgConstIter = qgConstraints.iterator(); qgConstIter.hasNext();) {
            QGConstraint qgConstraint = (QGConstraint) qgConstIter.next();
            String item1Name = qgConstraint.item1Name();
            String item2Name = qgConstraint.item2Name();
            String item1AttrName = qgConstraint.item1AttrName();
            String item2AttrName = qgConstraint.item2AttrName();
            String operator = qgConstraint.operator();

            // check operator
            if (!operator.equals("eq") && !operator.equals("ge") &&
                    !operator.equals("gt") && !operator.equals("le") &&
                    !operator.equals("lt") && !operator.equals("ne"))
                addErrorString("constraint operator wasn't one of: 'eq', " +
                        "'ge', 'gt', 'le', 'lt', 'ne', or 'exists': " + operator);

            // check attrNames are consistent - both IDs or both attributes
            if (((item1AttrName == null) && (item2AttrName != null)) ||
                    ((item1AttrName != null) && (item2AttrName == null))) {
                addErrorString("constraint had inconsistent operands: " +
                        item1AttrName + ", " + item2AttrName + ". operands " +
                        "should be consistent with each other: two <item>S, " +
                        "or an <attribute-name> then a <value>");
            }

            // check items themselves
            checkConstraintItems(query, item1Name, item2Name, "constraint");
        }
    }


    /**
     * Called by checkDNFEle(), checks testEle' contents. Checks a.3 and a.4.
     * If invalid then adds to my errorStrings. Recall that the <test> element
     * contains an <operator> followed by two operands, which are either an
     * <attribute-name> -  <value> combination, or two <item>S.
     *
     * @param operatorEle operator element
     * @param operand1Ele first operand element
     * @param operand2Ele second element
     * @param isCondEle   true if this <test> element comes from a <condition> element. false if from a <constraint>
     */
    private void checkTestEle(Query query, Element operatorEle, Element operand1Ele,
                              Element operand2Ele, boolean isCondEle) {
        // check operator
        String operatorStr = operatorEle.getText();
        if (!operatorStr.equals("eq") &&
                !operatorStr.equals("ge") && !operatorStr.equals("gt") &&
                !operatorStr.equals("le") && !operatorStr.equals("lt") &&
                !operatorStr.equals("ne") &&
                !operatorStr.equals("exists"))
            addErrorString("operator wasn't one of: 'eq', 'ge', 'gt', 'le', " +
                    "'lt', 'ne', or 'exists': " + operatorStr);
        // continue: check operands
        // if operator is exists, then there should be a single operand, an attribute-name
        // otherwise, there should be two operands
        if (operatorStr.equals("exists")) {
            if (operand2Ele != null) {
                addErrorString("exists operator takes a single operand");
            }
            if (!operand1Ele.getName().equals("attribute-name")) {
                addErrorString("exists operator required an <attribute-name>");
            }
        } else {
            if (operand2Ele == null) {
                addErrorString(operatorStr + " operator requires two operands");
            } else {
                if (operand1Ele.getName().equals("attribute-name") &&
                        operand2Ele.getName().equals("value")) {
                    // ok
                } else if (operand1Ele.getName().equals("item") &&
                        operand2Ele.getName().equals("item")) {
                    // ok
                } else {
                    addErrorString("inconsistent operands: " + operand1Ele + ", " +
                            operand2Ele + ". operands should be consistent with each " +
                            "other: two <item>S, or an " +
                            "<attribute-name> then a <value>");
                }
            }
        }
        if (isCondEle) {
            if (operand2Ele != null) {
                checkTestEleCond(operand1Ele, operand2Ele);
            }
        } else {
            checkTestEleConst(query, operand1Ele, operand2Ele);
        }
    }


    /**
     * Called by checkTestEle(), checks a.6. If invalid then adds to my
     * errorStrings.
     *
     * @param operand1Ele
     * @param operand2Ele
     */
    private void checkTestEleCond(Element operand1Ele, Element operand2Ele) {
        if (operand1Ele.getName().equals("attribute-name") &&
                operand2Ele.getName().equals("value")) {
            // ok
        } else {
            addErrorString("inconsistent condition operands: " + operand1Ele +
                    ", " + operand2Ele + ". conditions should only compare " +
                    "attribute names and values");
        }
    }


    /**
     * Called by checkTestEle(), checks a.5. If invalid then adds to my
     * errorStrings.
     * <p/>
     * a. Both elements are items
     * b. Both elements are of the same type (edge or vertex)
     * c. At most one element is annotated, and if so it has to be a vertex
     *
     * @param operand1Ele
     * @param operand2Ele
     */
    private void checkTestEleConst(Query query, Element operand1Ele, Element operand2Ele) {
        // element types ok (checked in checkTestEle), so check that values actually refer to edges or labels
        String item1Name = operand1Ele.getChild("item-name").getText();
        String item2Name = operand2Ele.getChild("item-name").getText();
        checkConstraintItems(query, item1Name, item2Name, "<constraint> element");

        // now make sure that it's comparing both IDs or both attribute-names
        Element item1AttrNameEle = operand1Ele.getChild("attribute-name");
        Element item2AttrNameEle = operand2Ele.getChild("attribute-name");
        if ((item1AttrNameEle == null && item2AttrNameEle != null) ||
                (item1AttrNameEle != null && item2AttrNameEle == null)) {
            addErrorString("constraint had inconsistent operands: " +
                    item1Name + "." + (item1AttrNameEle == null ? "id" : item1AttrNameEle.getText()) + ", " +
                    item2Name + "." + (item2AttrNameEle == null ? "id" : item2AttrNameEle.getText()) +
                    ". constraints should only test equality " +
                    "between pairs of IDs or attribute-names, with no mixing");
        }
    }


    /**
     * Called by checkNonNegatedConnComps(), returns true if query is connected,
     * i.e, if it has only one connected component. Returns false o/w.
     *
     * @param query Query to check
     * @return true if query is connected. false if not
     */
    private boolean isConnected(Query query) {
        return (query.connectedComponents().size() == 1);
    }


    /**
     * Called by checkNonNegatedConnComps(), permantly removes negated elements
     * (vertices, edges, and subqueries) from query.
     *
     * @param query Query to check
     */
    private void removeNegatedElements(Query query) {
        // our approach is to manage a list of elements to remove - vertices,
        // edges, and subqueries. note that we handle subquery recursion here,
        // which means we manually put all its (and its children's) nodes and
        // edges on the list. once we have the list we remove all elements from
        // query, doing subqueries last so that their children can be removed
        // without a no-parent exception
        //
        // step 1/3: fill elementsToRemove
        final List elementsToRemove = new ArrayList();    // vertices, edges, and subqueries to remove. SubqueryS are added at the end. filled by iterator
        final QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            // no IVs

            public void edge(QGEdge qgEdge) {
                if (isNegated(qgEdge) || isInNegatedSubquery())
                    elementsToRemove.add(0, qgEdge);        // at start
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (((abstractQuery instanceof Subquery) && (isNegated(abstractQuery)))
                        || isInNegatedSubquery())
                    elementsToRemove.add(abstractQuery);    // at end
            }

            public void vertex(QGVertex qgVertex) {
                if (isNegated(qgVertex) || isInNegatedSubquery())
                    elementsToRemove.add(0, qgVertex);        // at start
            }

            /**
             * helper
             *
             * @param vertEdgeOrSubq
             * @return true if vertEdgeOrSubq has a [0..] annoation, false if not
             */
            boolean isNegated(Object vertEdgeOrSubq) {
                Annotation annotation = null;    // set next
                if (vertEdgeOrSubq instanceof QGItem)
                    annotation = ((QGItem) vertEdgeOrSubq).annotation();
                else if (vertEdgeOrSubq instanceof Subquery)
                    annotation = ((Subquery) vertEdgeOrSubq).annotation();
                else
                    throw new IllegalArgumentException("vertEdgeOrSubq wrong type: " + vertEdgeOrSubq);
                // continue
                return ((annotation != null) && (annotation.annotMin() == 0));
            }

            /**
             * helper
             *
             * @return true if I'm currently in a negated Subquery, false if not
             */
            boolean isInNegatedSubquery() {
                Iterator abstractQueryIter = queryIter.abstractQueryStack().iterator();
                while (abstractQueryIter.hasNext()) {
                    AbstractQuery abstractQuery = (AbstractQuery) abstractQueryIter.next();
                    if ((abstractQuery instanceof Subquery) && isNegated(abstractQuery))
                        return true;    // are in a negated Subquery
                }
                return false;    // not in a negated Subquery
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
        // step 2/3: remove elements in elementsToRemove. recall that SubqueryS
        // are at the end, which means we remove their edges and vertices before
        // we remove the SubqueryS themselves. otherwise we get exceptions
        Iterator elementIter = elementsToRemove.iterator();
        while (elementIter.hasNext()) {
            Object element = elementIter.next();
            if (element instanceof QGVertex) {
                QGVertex theVertex = (QGVertex) element;
                theVertex.parentAQuery().removeVertex((QGVertex) element);
            } else if (element instanceof QGEdge) {
                QGEdge theEdge = (QGEdge) element;
                theEdge.parentAQuery().removeEdge((QGEdge) element);
            } else {    // Subquery
                query.removeSubquery((Subquery) element);
            }
        }
        // step 3/3: remove broken edges. do so by checking if each edge's
        // vertices are still in the query
        List vertices = query.vertices(true);                // isRecurse
        Iterator edgeIter = query.edges(true).iterator();    // ""
        while (edgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) edgeIter.next();
            if (!vertices.contains(qgEdge.vertex1()) ||
                    !vertices.contains(qgEdge.vertex2()))
                qgEdge.parentAQuery().removeEdge(qgEdge);
        }
    }


    /**
     * Called by constructor, checks all constraints that can be checked by a
     * single pass through the query. See this class's docs for which ones can
     * be checked this way.
     *
     * @param query being validated
     */
    private void walkQuery(final Query query) {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            // Verify that the edge connects valid vertices
            public void addLink(QGAddLink qgAddLink) {
                String vertex1Name = qgAddLink.getVertex1Name();
                String vertex2Name = qgAddLink.getVertex2Name();
                if (query.qgItemForName(vertex1Name) == null) {
                    addErrorString("Add edge for non-existing vertex: " + vertex1Name);
                }
                if (query.qgItemForName(vertex2Name) == null) {
                    addErrorString("Add edge for non-existing vertex: " + vertex2Name);
                }
            }

            public void cachedItem(String itemName, String containerName) {
                if (query.qgItemForName(itemName) == null) {
                    addErrorString("Cache for non-existing element: " + itemName);
                }
                // do not check container
            }


            public void edge(QGEdge qgEdge) {
                Element condEleChild = qgEdge.condEleChild();
                if (condEleChild != null)
                    checkCondEleChild(query, condEleChild);
                checkEdge(qgEdge);
                checkForEmptyName(qgEdge);
            }

            // checks constraints here
            public void startAbstractQuery(AbstractQuery abstractQuery) {
                checkQGConstraints(abstractQuery.constraints(), abstractQuery.rootQuery());
                checkAtLeastOneVertex(abstractQuery);
                checkNestedSubquery(abstractQuery);
            }

            public void vertex(QGVertex qgVertex) {
                Element condEleChild = qgVertex.condEleChild();
                if (condEleChild != null)
                    checkCondEleChild(query, condEleChild);
                checkAnnotVertex(qgVertex);
                checkForEmptyName(qgVertex);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }


}
