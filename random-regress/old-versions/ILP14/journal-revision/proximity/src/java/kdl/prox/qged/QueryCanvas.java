/**
 * $Id: QueryCanvas.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kdl.prox.qgraph2.AbsQueryChild;
import kdl.prox.qgraph2.AbstractQuery;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.QueryIterHandler;
import kdl.prox.qgraph2.QueryIterHandlerEmptyAdapter;
import kdl.prox.qgraph2.QueryIterator;
import kdl.prox.qgraph2.Subquery;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * A PCanvas that knows how to create, display, and edit QGraph queries. For
 * now this implementation uses Piccolo-style user attributes to associate
 * domain objects (QGraph elements) with PNodes, rather than extending PNode
 * with subclasses. We use the following naming convention to help identify
 * PNode types, based on the QGraph element each node has associated with it
 * (see QG_ELEMENT_KEY): PEdge (a PNode representing a QGEdge), PVertex
 * (represents a QGVertex), or PSubquery (represents a Subquery).
 */
public class QueryCanvas extends PCanvas {

    private static final Logger log = Logger.getLogger(QueryCanvas.class);

    private static final String LABEL_GROUP_KEY = "LABEL_GROUP_KEY";    // LabelGroup for PEdge, PVertex, or PSubquery
    private static final String QG_ELEMENT_KEY = "QG_ELEMENT_KEY";      // QGraph element (QGItem or Subquery) for PEdge, PVertex, or PSubquery
    private static final String VERTEX1_PNODE_KEY = "VERTEX1_PNODE_KEY";    // vertex1 PNode for PEdge
    private static final String VERTEX2_PNODE_KEY = "VERTEX2_PNODE_KEY";    // vertex2 ""
    private static final String ARROW_HEAD_KEY = "ARROW_HEAD_KEY";      // arrowHead PNode for PEdge

    public static final int VERT_DIAM = 50;
    private static final int LINE_HEIGHT = 16;      // todo base on font
    private static final int MULTI_EDGE_GAP = 10;   // perpendicular separation between multiple edges
    private static final int SELF_LOOP_SIZE = 20;   // size of self-loop box

    private final Map qgEleToPNodeMap = new HashMap();  // maps QGraph elements (AbsQueryChild or AbstractQuery instances) to PNodes


    public QueryCanvas() {
    }

    public QueryCanvas(Query query) {
        loadQuery(query);
    }

    /**
     * Called both internally (when a Query or Subquery in loadQuery()) and
     * externally when creating a new Query.
     *
     * @param abstractQuery
     */
    public void addAbstractQuery(AbstractQuery abstractQuery) {
        if (abstractQuery instanceof Query) {
            // Query is special: only need mapping (no creation, no label group)
            qgEleToPNodeMap.put(abstractQuery, getLayer());
        } else {    // Subquery
            Subquery subquery = (Subquery) abstractQuery;
            addAbsQueryChild(subquery);
        }
    }

    public void addAbsQueryChild(final AbsQueryChild absQueryChild) {
        // create PNode for absQueryChild
        final PNode pNode = makePNodeForQGElement(absQueryChild);   // pickable by default
        qgEleToPNodeMap.put(absQueryChild, pNode);
        pNode.addAttribute(QG_ELEMENT_KEY, absQueryChild);

        // add the PNode to the appropriate parent
        AbstractQuery parentAbsQuery = absQueryChild.parentAQuery();
        PNode parentPNode = getPNode(parentAbsQuery);
        parentPNode.addChild(pNode);
        
        // create the LabelGroup for pNode. location is updated by updateLabelGroup()
        LabelGroup labelGroup = new LabelGroup(absQueryChild);
        pNode.addAttribute(LABEL_GROUP_KEY, labelGroup);
        
        // add the LabelGroup to the same parent as pNode
        pNode.getParent().addChild(labelGroup);

        // add listener that repositions the label group, resizes containing
        // subquery (if necessary), and updates links
        pNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent arg0) {
                        noteAQChildBoundsChange(absQueryChild, pNode);
                    }
                });
    }

    private static void assertIsQGElement(PNode pNode) {
        Assert.condition(isPEdge(pNode) || isPVertex(pNode) || isPSubquery(pNode),
                "pNode must be a PEdge, PVertex, or PSubquery: " + pNode);
    }

    /**
     * From: http://groups.google.com/groups?hl=en&lr=&threadm=5p5s2m%2498b%40hershey.cc.gatech.edu&rnum=9&prev=/groups%3Fhl%3Den%26lr%3D%26q%3Djava%2Bdraw%2Barrow%26btnG%3DSearch
     * Computes an arrowhead polygon, located at the second point.
     *
     * @param startPoint     first coordinate of the line
     * @param endPoint       second coordinate of the line (arrow
     *                       head point goes here)
     * @param arrowHeadLen   length of the sides of the arrow head
     * @param arrowHeadAngle angle between each side and the line in
     *                       degrees
     * @param arrowInset     percent inset for base of arrow [0..1]
     * @return xsAndYs - a 2-element array whose first element is float[4] xs and
     *         whose second element is float[4] ys. These are as passed to
     *         PPath.setPathToPolyline(float[] xp, float[] yp).
     */
    public static float[][] calcArrowHead(Point2D startPoint, Point2D endPoint,
                                          int arrowHeadLen, int arrowHeadAngle,
                                          double arrowInset) {
        double x1 = startPoint.getX();
        double y1 = startPoint.getY();
        double x2 = endPoint.getX();
        double y2 = endPoint.getY();
        
        /* length of the target line we are drawing arrowhead over */
        double dx, dy, len; 

        /* values for rotation needed to get from canonical line to target */
        double sin_theta, cos_theta;

        /* coordinates and values for canonical arrowhead placed on x axis */
        double pt1_x, pt1_y, pt2_x, pt2_y, pt3_x, pt3_y;
        double ah_len = (double) arrowHeadLen;
        double ah_angle = Math.PI * ((double) arrowHeadAngle) / 180.0;

        /* final arrowhead points transformed to match the line */
        double result1_x;
        double result1_y;
        double result2_x;
        double result2_y;
        double result3_x;
        double result3_y;

        /* figure out the length of the target line */
        dx = (double) (x2 - x1);
        dy = (double) (y2 - y1);
        len = Math.sqrt(dx * dx + dy * dy);

        /* bail out now if its zero length (since direction is not determined) */
        if (len == 0) return null;

        /* compute canonical arrow head points (as if on a line on x axis) 
         *
         *             1 
         *              \  
         *   +-----------2-------0----- x axis --->
         *              /
         *             3
         *
         * arrowhead is draw as a 4 point polygon (with pt0 at the tip)
         */
        pt1_x = len - ah_len * Math.cos(ah_angle);
        pt1_y = ah_len * Math.sin(ah_angle);
        pt2_x = len - (len - pt1_x) * arrowInset;
        pt2_y = 0;
        pt3_x = pt1_x;
        pt3_y = -pt1_y;

        /* sin and cos of rotation to get canonical from x axis to target */
        sin_theta = dy / len;
        cos_theta = dx / len;

        /* rotate and translate to get our final points */
        result1_x = (pt1_x * cos_theta - pt1_y * sin_theta + 0.5) + x1;
        result1_y = (pt1_x * sin_theta + pt1_y * cos_theta + 0.5) + y1;
        result2_x = (pt2_x * cos_theta - pt2_y * sin_theta + 0.5) + x1;
        result2_y = (pt2_x * sin_theta + pt2_y * cos_theta + 0.5) + y1;
        result3_x = (pt3_x * cos_theta - pt3_y * sin_theta + 0.5) + x1;
        result3_y = (pt3_x * sin_theta + pt3_y * cos_theta + 0.5) + y1;

        /* return the arrow head polygon */
        float xs[] = {(float) x2, (float) result1_x, (float) result2_x, (float) result3_x}; // todo precision!?
        float ys[] = {(float) y2, (float) result1_y, (float) result2_y, (float) result3_y};
        return new float[][]{xs, ys};
    }

    /**
     * Removes all my children and resets zoom/pan.
     */
    public void clear() {
        getLayer().removeAllChildren();
        zoom100Pct();
    }

    /**
     * Connects PEdge to its two PVertices.
     */
    public void connectPEdge(QGEdge qgEdge) {
        QGVertex qgVertex1 = qgEdge.vertex1();
        QGVertex qgVertex2 = qgEdge.vertex2();
        final PNode pVertex1 = getPNode(qgVertex1);
        final PNode pVertex2 = getPNode(qgVertex2);
        final PPath pEdge = (PPath) getPNode(qgEdge);   // todo dangerous!
        pEdge.addAttribute(VERTEX1_PNODE_KEY, pVertex1);
        pEdge.addAttribute(VERTEX2_PNODE_KEY, pVertex2);
    }

    /**
     * Connects each PEdge to its two PVertices.
     *
     * @param query
     */
    private void connectPEdges(Query query) {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                connectPEdge(qgEdge);
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (abstractQuery instanceof Subquery) {
                    PNode pNode = getPNode(abstractQuery);
                    pNode.moveToBack();
                }
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }

    /**
     * @param pNode
     * @return QGItem or Subquery that pNode represents. returns null if none
     */
    public static AbsQueryChild getAbsQueryChild(PNode pNode) {
        return (AbsQueryChild) pNode.getAttribute(QG_ELEMENT_KEY);
    }

    /**
     * @param pEdge
     * @return the arrowhead PPath corresponding to pEdge, which must be an edge.
     *         returns null if there is none
     */
    static PPath getArrowHead(PPath pEdge) {
        return (PPath) pEdge.getAttribute(ARROW_HEAD_KEY);
    }

    /**
     * @param pNode
     * @return the LabelGroup associated with pNode. returns null if none found
     */
    public static LabelGroup getLabelGroup(PNode pNode) {
        assertIsQGElement(pNode);
        return (LabelGroup) pNode.getAttribute(LABEL_GROUP_KEY);
    }

    /**
     * @param qgObject a QGraph item with a visual representation in me. either
     *                 a QGItem or Subquery
     * @return the PNode corresponding to qgObject. returns null if none found
     */
    public PNode getPNode(Object qgObject) {
        Assert.condition((qgObject instanceof QGItem) ||
                (qgObject instanceof Subquery) ||
                (qgObject instanceof Query),
                "qgObject must be a QGItem, Subquery, or Query: " + qgObject);
        return (PNode) qgEleToPNodeMap.get(qgObject);
    }

    /**
     * @param pEdge
     * @return pEdge corresponding to pNode's vertex1. returns null if not set
     */
    public static PNode getPVertex1(PNode pEdge) {
        return (PNode) pEdge.getAttribute(VERTEX1_PNODE_KEY);
    }

    /**
     * @param pEdge
     * @return PNode corresponding to pEdge's vertex2. returns null if not set
     */
    public static PNode getPVertex2(PNode pEdge) {
        return (PNode) pEdge.getAttribute(VERTEX2_PNODE_KEY);
    }

    /**
     * @return List of PNodes whose children should be selectable, as defined
     *         by PSelectionEventHandler
     */
    public List getSelectableParents() {
        List selectableParents = new ArrayList();
        selectableParents.add(getLayer());  // for top-level vertices, edges, and subqueries
        for (int childIdx = 0; childIdx < getLayer().getChildrenCount(); childIdx++) {
            PNode childPNode = getLayer().getChild(childIdx);
            if (isPSubquery(childPNode)) {
                selectableParents.add(childPNode);
            }
        }
        return selectableParents;
    }

    /**
     * The returned tables vary depending on object's type. In all cases the
     * table contains a property description in the left column, and the
     * corresponding value in the right column. Here are the three cases:
     * <p/>
     * a) Query:
     * <pre>
     * +--------------+-------+
     * | Type         | Query |
     * +--------------+-------+
     * | Name         | ...   |  ??
     * +--------------+-------+
     * | Description  | ...   |
     * +--------------+-------+
     * | Constraint 1 | ...   |  if any
     * +--------------+-------+
     * | Constraint 2 | ...   |
     * +--------------+-------+
     * | ...          |       |
     * +--------------+-------+
     * </pre>
     * b) Subquery:
     * <pre>
     * +--------------+---------+
     * | Type         | Subqery |
     * +--------------+---------+
     * | Annotation   | ...     |
     * +--------------+---------+
     * </pre>
     * c) QGItem:
     * <pre>
     * +--------------+-------+
     * | Type         | Edge  |  or 'Vertex'
     * +--------------+-------+
     * | Name         | ...   |
     * +--------------+-------+
     * | Annotation   | ...   |
     * +--------------+-------+
     * | Condition    | ...   |
     * +--------------+-------+
     * </pre>
     *
     * @param object a Query, Subquery, or QGItem whose content is being represented
     * @return a QGObjTableModel that contains content from object, represented
     *         as a properties sheet/inspector
     */
    public static QGObjTableModel getTableModel(Object object) {
        Assert.condition((object instanceof Query) ||
                (object instanceof Subquery) ||
                (object instanceof QGItem),
                "object must be a Query, Subquery, or QGItem: " + object +
                " (" + (object == null ? null : object.getClass()) + ")");
        if (object instanceof Query) {
            return new QueryTableModel((Query) object);
        } else if (object instanceof Subquery) {
            return new SubqueryTableModel((Subquery) object);
        } else {    // object instanceof QGItem
            return new QGItemTableModel((QGItem) object);
        }
    }

    public static boolean isPEdge(PNode pNode) {
        return (getAbsQueryChild(pNode) instanceof QGEdge);
    }

    public static boolean isPSubquery(PNode pNode) {
        return (getAbsQueryChild(pNode) instanceof Subquery);
    }

    public static boolean isPVertex(PNode pNode) {
        return (getAbsQueryChild(pNode) instanceof QGVertex);
    }

    public void loadQuery(Query query) {
        clear();

        // create PNodes for elements in query. note that we handle edges in
        // two passes. the first pass (done next) creates PNodes for corresponding
        // to QGEdges, but does not create vertex instances for them. the second
        // pass (done last) connects each edge to its two vertices
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                addAbsQueryChild(qgEdge);
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                addAbstractQuery(abstractQuery);
            }

            public void vertex(QGVertex qgVertex) {
                addAbsQueryChild(qgVertex);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);

        connectPEdges(query);
    }

    /**
     * @param absQueryChild (QGItem or Subquery)
     * @return QGraphPNode (also a PNode) of appropriate type based on absQueryChild
     */
    private PNode makePNodeForQGElement(AbsQueryChild absQueryChild) {
        PNode pNode;
        if (absQueryChild instanceof Subquery) {
            pNode = PPath.createRectangle(0, 0, 80, 60);    // arbitrary values (auto-sized based on contents, via updateSubqueryBounds())
        } else if (absQueryChild instanceof QGVertex) {
            pNode = PPath.createEllipse(0, 0, VERT_DIAM, VERT_DIAM);
            if (absQueryChild.parentAQuery() instanceof Subquery) {
                pNode.setPaint(Color.RED);
            } else {    // Query
                pNode.setPaint(Color.decode("#3388FF"));
            }
        } else if (absQueryChild instanceof QGEdge) {
            QGEdge qgEdge = (QGEdge) absQueryChild;

            // create arrow shaft
            pNode = PPath.createLine(50, 50, 50, 50);       // arbitrary values (set by updateLinksForQGVertex())
            pNode.setPaint(null);   // so that multiple self-loops won't overlap

            // create child arrow head if necessary
            updateQGEdgeArrowHead((PPath) pNode, qgEdge);
        } else {
            throw new IllegalArgumentException("can't handle AbsQueryChild " +
                    "subclass: " + absQueryChild);
        }
        return pNode;
    }

    /**
     * Called when the bounds of a PEdge, PVertex, or PSubquery change, updates
     * appropriate dependent PNodes, including label group, link, and containing
     * subquery.
     *
     * @param aqChild
     * @param pNode
     */
    private void noteAQChildBoundsChange(AbsQueryChild aqChild, final PNode pNode) {
        updateLabelGroup(pNode);
        if (aqChild instanceof QGVertex) {
            QGVertex qgVertex = (QGVertex) aqChild;
            AbstractQuery parentAbsQuery = aqChild.parentAQuery();
            updateLinksForQGVertex((QGVertex) aqChild);
            if (parentAbsQuery instanceof Subquery) {
                updateSubqueryBounds((Subquery) qgVertex.parentAQuery());
            }
        } else if (aqChild instanceof Subquery) {
            // call updateLink() on QGEdges in subquery that cross boundary
            Subquery subquery = (Subquery) aqChild;
            Set qgEdges = subquery.getEdgesCrossingBoundary(false);
            for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                PPath pEdge = (PPath) getPNode(qgEdge);     // todo dangerous!
                updateLink(pEdge);
            }
        }
    }

    /**
     * Offsets the values in r1c or r2c (depending on isUpdateR1) so that the
     * point is on the perimeter, instead of at the center.
     *
     * @param r1c        global center of circle 1
     * @param r2c        global center of circle 2
     * @param isUpdateR1 true if r1 should be updated
     * @return theta, the angle between the two pass points, in radians
     */
    private static double offsetToPerimeters(Point2D r1c, Point2D r2c, boolean isUpdateR1) {
        // calculate theta
        double thetaDX = (isUpdateR1 ? r2c.getX() - r1c.getX() : r1c.getX() - r2c.getX());
        double thetaDY = (isUpdateR1 ? r2c.getY() - r1c.getY() : r1c.getY() - r2c.getY());
        double theta = Math.atan2(thetaDX, thetaDY);

        // calculate dx, dy
        double radius = VERT_DIAM / 2.0;
        double dx = radius * Math.sin(theta);
        double dy = radius * Math.cos(theta);
        
        // update appropriate point
        if (isUpdateR1) {
            r1c.setLocation(r1c.getX() + dx, r1c.getY() + dy);
        } else {
            r2c.setLocation(r2c.getX() + dx, r2c.getY() + dy);
        }

        return theta;
    }

    /**
     * Remove PNodes from me corresponding to each AbsQueryChild in aqChildren.
     *
     * @param aqChildren AbsQueryChild instances
     */
    public void removePNodesForAQChildren(List aqChildren) {
        for (Iterator aqChildIter = aqChildren.iterator(); aqChildIter.hasNext();) {
            AbsQueryChild aqChild = (AbsQueryChild) aqChildIter.next();
            PNode pNode = getPNode(aqChild);
            if (pNode == null) {
                log.error("removePNodesForAQChildren(): no PNode found: " + aqChild);
            } else {
                LabelGroup labelGroup = QueryCanvas.getLabelGroup(pNode);
                pNode.removeFromParent();
                labelGroup.removeFromParent();
            }
        }
    }

    /**
     * Resets the pickable of all AbsQueryChild instances in my query back to
     * true. Needed because some modes' event handlers change pickable as needed.
     *
     * @param query
     */
    public void resetPickable(Query query) {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                PNode pNode = getPNode(qgEdge);
                pNode.setPickable(true);
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (abstractQuery instanceof Subquery) {
                    PNode pNode = getPNode(abstractQuery);
                    pNode.setPickable(true);
                }
            }

            public void vertex(QGVertex qgVertex) {
                PNode pNode = getPNode(qgVertex);
                pNode.setPickable(true);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }

    /**
     * Auto-sizes pNode to contain all its children.
     *
     * @param pNode
     * @param indent amount of space around pNode to preserve
     */
    public static void shrinkBoundsToFitPNode(PNode pNode, int indent) {
        PBounds bounds = pNode.getUnionOfChildrenBounds(null);
        bounds.setRect(bounds.getX() - indent, bounds.getY() - indent,
                bounds.getWidth() + 2 * indent, bounds.getHeight() + 2 * indent);
        pNode.setBounds(bounds);
    }

    /**
     * Updates the position of pNode's label group according to its QGraph
     * element's type.
     *
     * @param pNode
     */
    public void updateLabelGroup(PNode pNode) {
        assertIsQGElement(pNode);
        AbsQueryChild absQueryChild = getAbsQueryChild(pNode);
        PNode labelGroup = (PNode) pNode.getAttribute(LABEL_GROUP_KEY);
        PBounds bounds = pNode.getBounds();
        if (absQueryChild instanceof QGVertex) {
            // position group so that upper-left corner is centered on vertex
            Point2D point = new Point2D.Double(bounds.getX() + 4, // magic to center a bit
                    bounds.getY() + (pNode.getHeight() / 2) - (LINE_HEIGHT / 2));
            pNode.localToGlobal(point);
            labelGroup.setGlobalTranslation(point);
        } else if (absQueryChild instanceof QGEdge) {
            // position group centered on edge center
            Point2D point = new Point2D.Double(bounds.getCenterX() - (labelGroup.getWidth() / 2),
                    bounds.getCenterY() - (labelGroup.getHeight() / 2));
            pNode.localToGlobal(point);
            labelGroup.setGlobalTranslation(point);
        } else {    // Subquery
            // position group at bottom right corner
            Point2D point = new Point2D.Double(bounds.getX() + bounds.getWidth() - labelGroup.getWidth(),
                    bounds.getY() + bounds.getHeight() + -(LINE_HEIGHT * 2));   // todo should get from labelGroup
            pNode.localToGlobal(point);
            labelGroup.setGlobalTranslation(point);
        }
    }

    /**
     * Updates the endpoints of the link to connect the nodes it depends on.
     * Also updates the arrowhead's orientation, if there is one. Handles the
     * case of multiple edges between pEdge's two vertices by drawing parallel
     * lines between the nodes, like this:
     * <pre>
     *       ----->
     * ( + ) -----> ( + )
     *       ----->
     * </pre>
     * <p/>
     * Handles the case of (possibly multiple) self-loop edges by drawing edges
     * as squares looped and stacked under the vertex node, like this:
     * <pre>
     *   ( + )
     *     |  ^
     *     +--+
     *     |  ^
     *     +--+
     *     ...
     * </pre>
     *
     * @param pEdge a node with a QG_ITEM_KEY that's an edge QGItem
     */
    public static void updateLink(PPath pEdge) {
        PNode pVertex1 = getPVertex1(pEdge);
        PNode pVertex2 = getPVertex2(pEdge);
        if ((pVertex1 == null) || (pVertex2 == null)) {     // happens in early init when moving to front/back before everything created
            return;
        }

        QGEdge qgEdge = (QGEdge) getAbsQueryChild(pEdge);
        if (qgEdge.isSelfLoop()) {
            Point2D[] stPtEndPt = updateLinkShaftSelfLoop(pEdge, qgEdge, pVertex1);
            updateLinkArrowHead(pEdge, stPtEndPt[0], stPtEndPt[1]);
        } else {
            Point2D[] stPtEndPt = updateLinkShaftNoSelfLoop(pEdge, qgEdge,
                    pVertex1, pVertex2);
            updateLinkArrowHead(pEdge, stPtEndPt[0], stPtEndPt[1]);
        }
    }

    /**
     * Updates the arrow head of pEdge.
     *
     * @param pEdge
     * @param startPt
     * @param endPt
     */
    private static void updateLinkArrowHead(PPath pEdge,
                                            Point2D startPt, Point2D endPt) {
        // step 2/2: update arrow head, if necessary
        PPath arrowHead = getArrowHead(pEdge);
        if (arrowHead != null) {
            float[][] xsAndYs = QueryCanvas.calcArrowHead(startPt, endPt, 14, 25, 0.8);
            if (xsAndYs != null) {
                arrowHead.setPathToPolyline(xsAndYs[0], xsAndYs[1]);
                arrowHead.closePath();
            }
        }
    }

    /**
     * Updates all PEdges connected to qgVertex's PVertex.
     *
     * @param qgVertex
     */
    public void updateLinksForQGVertex(QGVertex qgVertex) {
        Set edges = qgVertex.edges();
        for (Iterator qgEdgeIter = edges.iterator(); qgEdgeIter.hasNext();) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            PPath pEdge = (PPath) getPNode(qgEdge);     // todo dangerous!
            updateLink(pEdge);
        }
    }

    /**
     * Updates the arrow shaft of pEdge in the non-self-loop case.
     *
     * @param pEdge
     * @param qgEdge
     * @param pVertex1
     * @param pVertex2
     * @return Point2D[] of two Point2D instances: startPt and endPt. to be used
     *         for arrow head orientation
     */
    private static Point2D[] updateLinkShaftNoSelfLoop(PPath pEdge, QGEdge qgEdge,
                                                       PNode pVertex1,
                                                       PNode pVertex2) {
        // normalize v1 and v2, since for shaft calculate the order doesn't
        // matter. we do this for consistency of angle calculations (trust me)
        QGVertex qgVertex1 = (QGVertex) QueryCanvas.getAbsQueryChild(pVertex1);
        QGVertex qgVertex2 = (QGVertex) QueryCanvas.getAbsQueryChild(pVertex2);
        boolean isSwapPVerts = qgVertex1.firstName().compareTo(qgVertex2.firstName()) > 0;
        if (isSwapPVerts) {
            PNode origPVertex1 = pVertex1;
            pVertex1 = pVertex2;
            pVertex2 = origPVertex1;
        }

        // we do this by setting the line for the non-multiple edge case, then
        // offsetting the edge according to its multiple edge position
        Point2D r1c = pVertex1.getBounds().getCenter2D();
        Point2D r2c = pVertex2.getBounds().getCenter2D();

        pVertex1.localToGlobal(r1c);
        pVertex2.localToGlobal(r2c);

        // offset points to circle perimeters, instead of centers
        /* double theta = */ offsetToPerimeters(r1c, r2c, true);
        double theta = offsetToPerimeters(r1c, r2c, false);
        theta -= (Math.PI / 2);     // magic correction for coordinate system

        pEdge.globalToLocal(r1c);
        pEdge.globalToLocal(r2c);

        List allQGEdges = qgEdge.vertex1().getEdgesBetween(qgEdge.vertex2());
        int qgEdgeIndex = allQGEdges.indexOf(qgEdge);
        if (allQGEdges.size() > 1) {
            qgEdgeIndex -= (allQGEdges.size() / 2);     // offset edges so that they're more centered in the parallel stack of them
        }
        double deltaX = (double) MULTI_EDGE_GAP * Math.sin(theta);  // x offset for multiple edges
        double deltaY = (double) MULTI_EDGE_GAP * Math.cos(theta);  // y ""
        Point2D.Double startPt = new Point2D.Double(r1c.getX() + deltaX * qgEdgeIndex,
                r1c.getY() + deltaY * qgEdgeIndex);
        Point2D.Double endPt = new Point2D.Double(r2c.getX() + deltaX * qgEdgeIndex,
                r2c.getY() + deltaY * qgEdgeIndex);
        pEdge.setPathTo(new Line2D.Double(startPt, endPt));

        if (isSwapPVerts) {
            return new Point2D[]{endPt, startPt};
        } else {
            return new Point2D[]{startPt, endPt};
        }
    }

    /**
     * Updates the arrow shaft of pEdge in the self-loop case. Recall from
     * updateLink() the drawing:
     * <pre>
     *   ( + )
     *     |  ^
     *     +--+
     *     |  ^
     *     +--+
     *     ...
     * </pre>
     * <p/>
     * The shaft is made up of  three line segments, based on four points (a
     * start point and three line-to points):
     * <ul>
     * <li>point 1: bottom tangent of vertex, centered horizontally
     * <li>point 2: SELF_LOOP_SIZE straight down from point 1
     * <li>point 3: SELF_LOOP_SIZE straight right from point 2
     * <li>point 4: SELF_LOOP_SIZE straight up from point 3
     * </ul>
     *
     * @param pEdge
     * @param qgEdge
     * @param pVertex1
     * @return Point2D[] of two Point2D instances: startPt and endPt. to be used
     *         for arrow head orientation
     */
    private static Point2D[] updateLinkShaftSelfLoop(PPath pEdge, QGEdge qgEdge,
                                                     PNode pVertex1) {
        // we do this by setting the line for the non-multiple edge case, then
        // offsetting the edge according to its multiple edge position
        Point2D r1c = pVertex1.getBounds().getCenter2D();
        pVertex1.localToGlobal(r1c);

        // offset point to circle perimeter - 6 o-clock (straight down)
        r1c.setLocation(r1c.getX(), r1c.getY() + (VERT_DIAM / 2.0));
        pEdge.globalToLocal(r1c);

        // create the points, using index
        List allQGEdges = qgEdge.vertex1().getEdgesBetween(qgEdge.vertex2());
        int qgEdgeIndex = allQGEdges.indexOf(qgEdge);
        Point2D point1 = new Point2D.Double(r1c.getX(), r1c.getY() + (qgEdgeIndex * SELF_LOOP_SIZE));
        Point2D point2 = new Point2D.Double(point1.getX(), point1.getY() + SELF_LOOP_SIZE);
        Point2D point3 = new Point2D.Double(point2.getX() + SELF_LOOP_SIZE, point2.getY());
        Point2D point4 = new Point2D.Double(point3.getX(), point3.getY() - SELF_LOOP_SIZE);
        pEdge.setPathToPolyline(new Point2D[]{point1, point2, point3, point4});

        return new Point2D[]{point3, point4};
    }

    /**
     * Adds or removes an arrowhead from pNode according to qgEdge.isDirected().
     *
     * @param pEdge
     * @param qgEdge
     */
    public void updateQGEdgeArrowHead(PPath pEdge, QGEdge qgEdge) {
        // we have two cases:
        // a) qgEdge isDirected() and pEdge doesn't have an arrowhead: add arrowhead
        // b) qgEdge is !isDirected() and pEdge has an arrowhead: remove arrowhead
        PPath arrowHead = getArrowHead(pEdge);
        boolean hasArrowHead = arrowHead != null;
        if (qgEdge.isDirected() && !hasArrowHead) {
            arrowHead = new PPath();
            arrowHead.setPickable(false);
            arrowHead.setPaint(Color.BLACK);
            pEdge.addChild(arrowHead);
            pEdge.addAttribute(ARROW_HEAD_KEY, arrowHead);
        } else if (!qgEdge.isDirected() && hasArrowHead) {
            pEdge.removeChild(arrowHead);
            pEdge.addAttribute(ARROW_HEAD_KEY, null);   // removes the attribute
        }
        QueryCanvas.updateLink(pEdge);
    }

    /**
     * Auto-sizes subquery's PSubquery to contain all its children.
     *
     * @param subquery
     */
    public void updateSubqueryBounds(Subquery subquery) {
        PNode subqPNode = getPNode(subquery);
        QueryCanvas.shrinkBoundsToFitPNode(subqPNode, 10);
    }

    public void zoom100Pct() {
        getCamera().setViewTransform(new AffineTransform());
    }

    public void zoomIn() {
        // todo xx should use visible scroll view
        double scaleFactor = getCamera().getScale() * 1.1;
        Point2D scalePoint = getLayer().getGlobalFullBounds().getCenter2D();
        getCamera().scaleViewAboutPoint(scaleFactor, scalePoint.getX(), scalePoint.getY());
    }

    public void zoomOut() {
        // todo xx should use visible scroll view
        double scaleFactor = getCamera().getScale() * 0.9;
        Point2D scalePoint = getLayer().getGlobalFullBounds().getCenter2D();
        getCamera().scaleViewAboutPoint(scaleFactor, scalePoint.getX(), scalePoint.getY());
    }

    public void zoomToFit() {
        getCamera().animateViewToCenterBounds(getLayer().getGlobalFullBounds(),
                true, 0);
    }

}
