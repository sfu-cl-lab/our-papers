/**
 * $Id: RDNCanvas.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import kdl.prox.qged.QueryCanvas;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;


/**
 * A PCanvas that knows how to display and re-arrange RDNs (Relational Dependency
 * Networks).
 * <p/>
 * Terminology: The canvas shows an RDN using the following terms and nodes:
 * <ul>
 * <li><i>type</i> (either object or link): Referenced by name. Shown as a
 * rectangle with a label at the top naming the type. Corners are rounded if a
 * link type. The rectangle contains one (todo xx zero?) or more circles, one
 * for each dependency variable.
 * <li><i>variable</i>: Referenced by type name and variable name (on that type).
 * Shown as a circle within the containing type, with a label inside naming the
 * variable.
 * <li><i>count</i>: Referenced by type name. Shown as a small solid black circle
 * in the containing type's upper-left hand corner. Represents the degree/count
 * of the type.
 * <li><i>arc</i>: Referenced by Arc instance. Shown as a line with an arrow head
 * pointing from the source to the dependent variable. The source can be another
 * variable (including the same variable - shown as a self loop) or a count
 * (i.e., the type itself).
 * </ul>
 * <p/>
 * Usage: There are two top-level methods to 'drill down' from: getTypes() and
 * getArcs(). From those you can get PNodes and PTexts (labels) for any element
 * mentioned above.
 */
public class RDNCanvas extends PCanvas {

    private static final Logger log = Logger.getLogger(RDNCanvas.class);
    private static final String RECT_DOT_KEY = "RECT_DOT";      // client prop key for rects to get dot PNodes
    private static final String LABEL_KEY = "LABEL_KEY";        // client prop key for rects and circles to get label PTexts
    private static final String LINE_HEAD1_KEY = "LINE_HEAD1";  // client prop key for lines to get arrow head 1 PNodes
    private static final String LINE_HEAD2_KEY = "LINE_HEAD2";  // "" 2 ""
    private static final String LINE_CIRCLE1_KEY = "LINE_CIRCLE1";  // client prop key for lines to get circle 1 PNodes
    private static final String LINE_CIRCLE2_KEY = "LINE_CIRCLE2";  // "" 2 ""

    private static final float DOT_DIAMETER = 10;
    private static final float CIRCLE_DIAMETER = 50;
    private static final double SELF_LOOP_GAP = 20;     // space beyond edge for position of loop's vertical portion

    private Map featureClassMap;    // maps feature item name (String) -> class item name (String); i.e., reverse of input to constructor

    private List types;             // item types (Strings)
    private Map typeVarMap;         // maps item type (String) -> List of variable names (Strings)
    private List arcs;              // arcs (Arcs)

    private Map typeRectMap;        // maps item type (String) -> rectangle (PNode)
    private Map typeVarCircleMap;   // maps catenated type.variable (String) -> circle (PNode)
    private Map arcLineMap;         // maps Arc -> line (PNode)


    /**
     * @param rptDocuments
     * @param classFeatureItemMap Map of class label item name (String) -> List of synonym
     *                            Strings as found in features. used for self-loops. for
     *                            example, {actor: [movie_actor]} means that feature item
     */
    public RDNCanvas(Document[] rptDocuments, Map classFeatureItemMap) {
        loadRDN(rptDocuments, classFeatureItemMap);
    }

    /**
     * Clears my IVs and canvas.
     */
    private void clear() {
        // reset IVs
        featureClassMap = Collections.EMPTY_MAP;    // will be replaced by call to reverseClassFeatureItemMap()

        types = new ArrayList();
        typeVarMap = new HashMap();
        arcs = new ArrayList();

        typeRectMap = new HashMap();
        typeVarCircleMap = new HashMap();
        arcLineMap = new HashMap();

        // clear canvas
        getLayer().removeAllChildren();
    }

    /**
     * @param bounds
     * @param point  in bounds' coordinate system
     * @return TOP, LEFT, BOTTOM, or RIGHT (from SwingConstants) indicating which
     *         side of bounds point is closest to
     */
    private int findClosestSide(PBounds bounds, Point2D point) {
        double top = point.getY();
        double left = point.getX();
        double bottom = bounds.getHeight() - top;
        double right = bounds.getWidth() - left;
        if ((top < left) && (top < right) && (top < bottom)) {
            return SwingConstants.TOP;
        } else if ((left < right) && (left < top) && (left < bottom)) {
            return SwingConstants.LEFT;
        } else if ((bottom < left) && (bottom < top) && (bottom < right)) {
            return SwingConstants.BOTTOM;
        } else {
            return SwingConstants.RIGHT;
        }
    }

    /**
     * Forces calling of noteRectBoundsChange() before it happens later via the
     * AWT event dispatch thread (via paintDirtyRegions()). We do this so that all
     * bounds are up-do-date (see PROPERTY_FULL_BOUNDS on rects below). Otherwise
     * unit tests fail.
     */
    void forceRectBoundsChanges() {
        for (Iterator typeIter = types.iterator(); typeIter.hasNext();) {
            String type = (String) typeIter.next();
            noteRectBoundsChange(type);
        }
    }

    /**
     * Top-level entry point for arcs.
     *
     * @return List of Arc instances, one for each arc
     */
    public List getArcs() {
        return arcs;
    }

    /**
     * @param type
     * @return List of Arc instances that are connected to any variable or dot in type
     */
    private Set getArcsForType(String type) {
        HashSet theArcs = new HashSet();
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            if (type.equals(arc.getType1()) || type.equals(arc.getType2())) {
                theArcs.add(arc);
            }
        }
        return theArcs;
    }

    /**
     * @param type
     * @param variable
     * @return List of Arc instances that connect to or from variable
     */
    private Set getArcsForVar(String type, String variable) {
        HashSet theArcs = new HashSet();
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            if (type.equals(arc.getType1()) &&
                    (((variable == null) && (arc.isFromDegree())) ||
                            (variable.equals(arc.getVar1())))) {
                theArcs.add(arc);
            } else if (type.equals(arc.getType2()) &&
                    (((variable == null) && (arc.getVar2() == null)) ||
                            (variable.equals(arc.getVar2())))) {
                theArcs.add(arc);
            }
        }
        return theArcs;
    }

    /**
     * @param arc
     * @return first arrow head for arc. never null
     */
    public PPath getArrowHead1ForArc(Arc arc) {
        PPath line = getLineForArc(arc);
        return (PPath) line.getClientProperty(LINE_HEAD1_KEY);
    }

    /**
     * @param arc
     * @return second arrow head for arc. null if none (i.e., if not bidirectional)
     */
    public PPath getArrowHead2ForArc(Arc arc) {
        PPath line = getLineForArc(arc);
        return (PPath) line.getClientProperty(LINE_HEAD2_KEY);
    }

    public PNode getCircle1ForArc(Arc arc) {
        PPath line = getLineForArc(arc);
        return (PNode) line.getClientProperty(LINE_CIRCLE1_KEY);
    }

    public PNode getCircle2ForArc(Arc arc) {
        PPath line = getLineForArc(arc);
        return (PNode) line.getClientProperty(LINE_CIRCLE2_KEY);
    }

    /**
     * @param type     name of type, as returned by getTypes()
     * @param variable name of variable on type, as returned by getVariables()
     * @return circle PNode for variable on type
     */
    public PNode getCircleForVar(String type, String variable) {
        return (PNode) typeVarCircleMap.get(type + "." + variable);
    }

    /**
     * @param type name of type, as returned by getTypes()
     * @return dot PNode for typeRect
     */
    public PNode getDotForType(String type) {
        PNode rect = getRectForType(type);
        return (PNode) rect.getClientProperty(RECT_DOT_KEY);
    }

    /**
     * @param pNode as returned by getRectForType() and getCircleForVar()
     * @return label PText for pNode
     */
    public PText getLabel(PNode pNode) {
        return (PText) pNode.getClientProperty(LABEL_KEY);
    }

    public PPath getLineForArc(Arc arc) {
        return (PPath) arcLineMap.get(arc);
    }

    /**
     * @param type
     * @return Color to use as rect background for the type indicated by typeIdx
     */
    private Color getRectColorForType(String type) {
        List types = getTypes();
        int typeIdx = types.indexOf(type);
        Assert.condition(typeIdx != -1, "couldn't find type: " + type + ", " + types);

        Color[] colors = new Color[]{
                Color.PINK,
                Color.YELLOW,
                Color.ORANGE,
                Color.LIGHT_GRAY,
                Color.GREEN,
                Color.CYAN,
                Color.DARK_GRAY,
                Color.MAGENTA,
                Color.RED,
                Color.WHITE,
                Color.BLUE,
        };
        return colors[typeIdx % colors.length];
    }

    /**
     * @param type name of type, as returned by getTypes()
     * @return rectangle PNode for type. returns null if none found
     */
    public PPath getRectForType(String type) {
        return (PPath) typeRectMap.get(type);
    }

    /**
     * Top-level entry point for types.
     *
     * @return List of Strings, one for each type
     */
    public List getTypes() {
        return types;   // NB: not a copy
    }

    /**
     * @param type
     * @return Strings, one for each variable on type
     */
    public List getVariables(String type) {
        return (List) typeVarMap.get(type);
    }

    /**
     * @param arc
     * @return true if a) arc's end-points are both inside the same rectangle, and
     *         b) (at least part) of arc extends beyond that rect's bounds. used
     *         by unit tests to determine visual correctness
     */
    public boolean isArcPassesOutsideRect(Arc arc) {
        if (!arc.getType1().equals(arc.getType2())) {
            return false;
        }

        String type = arc.getType1();
        PPath rect = getRectForType(type);
        PPath line = getLineForArc(arc);
        PBounds rectBounds = rect.getGlobalBounds();
        PBounds lineBounds = line.getGlobalBounds();
        boolean isRectContainsLine = rectBounds.contains(lineBounds);
        // todo xx bug: lineBounds is incorrect here in some cases (if canvas not realized?):
        return !isRectContainsLine;
    }

    /**
     * Similar to findClosestSide()
     *
     * @param bounds
     * @param point  in bounds' coordinate system
     * @return true if point is closest to the left side of bounds. returns
     *         false if closest to the right
     * @see RDNCanvas#findClosestSide
     */
    private boolean isLeftClosestSide(PBounds bounds, Point2D point) {
        return point.getX() < (bounds.getWidth() / 2) + bounds.getX();
    }

    private void layoutRDN() {
        // we perform the layout in two steps: first we lay out the children of
        // each rectangle (i.e., layout circles) in order to calculate the
        // correct (auto-sized) dimensions of the rects. then second, we lay out 
        // the rectangles themselves in my layer

        // step 1/2: lay out circles within each rect, collecting rects for step
        // 2/2 (collecting just rects skips arcs, etc.)
        List rects = new ArrayList();
        for (Iterator typeIter = types.iterator(); typeIter.hasNext();) {
            String type = (String) typeIter.next();
            PNode rect = getRectForType(type);
            PNode dot = getDotForType(type);
            PBounds dotBounds = dot.getBounds();
            dot.localToGlobal(dotBounds);
            rects.add(rect);

            // collect rect's circle PNodes (skips dots, labels, etc.)
            List circles = new ArrayList();
            List variables = getVariables(type);
            for (Iterator varIter = variables.iterator(); varIter.hasNext();) {
                String var = (String) varIter.next();
                PNode circle = getCircleForVar(type, var);
                circles.add(circle);
            }

            List point2Ds = PNodeLayout.calcGridLayout(rect, circles, 10, 20);
            double xOffset = dotBounds.getX();
            double yOffset = dotBounds.getY() + dotBounds.getHeight() + 10;
            for (int childIdx = 0; childIdx < circles.size(); childIdx++) {
                PNode circle = (PNode) circles.get(childIdx);
                Point2D point2D = (Point2D) point2Ds.get(childIdx);
                circle.localToGlobal(point2D);
                point2D.setLocation(point2D.getX() + xOffset,
                        point2D.getY() + yOffset);
                circle.setGlobalTranslation(point2D);
            }
        }

        // step 2/2: lay out rects themselves
        List point2Ds = PNodeLayout.calcGridLayout(getLayer(), rects, 20, 20);
        for (int childIdx = 0; childIdx < rects.size(); childIdx++) {
            PNode rect = (PNode) rects.get(childIdx);
            Point2D point2D = (Point2D) point2Ds.get(childIdx);
            rect.setGlobalTranslation(new Point2D.Double(point2D.getX(), point2D.getY()));
        }
    }

    /**
     * @param rptDocuments
     * @param classFeatureItemMap ""
     */
    public void loadRDN(Document[] rptDocuments, Map classFeatureItemMap) {
        clear();
        featureClassMap = reverseClassFeatureItemMap(classFeatureItemMap);
        loadRDNInternal(rptDocuments);
        layoutRDN();
    }

    /**
     * Called by loadRDN(), does the actual creation of data structures and
     * PNodes corresponding to rptElements.
     *
     * @param rptDocuments
     */
    private void loadRDNInternal(Document[] rptDocuments) {
        // we use three passes to create data structures and PNodes. we use
        // multiple passes so that prior objects can be created before needed

        for (int rptDocumentIdx = 0; rptDocumentIdx < rptDocuments.length; rptDocumentIdx++) {
            // handle class label - allows creating everything but arcs
            Document rptDocument = rptDocuments[rptDocumentIdx];
            RPTSourceInfo info = RPTReader.getClassLabelSourceInfo(rptDocument.getDocType().getSystemID(), rptDocument.getRootElement());
            saveTypeAndVar(info.item, info.attr);
            makeTypeAndVarPNodes(info.item, info.attr, info.isObject);
        }

        // pass 2/3: create arcs (via handling features), but not their PNodes.
        // (we need to complete Arc creation so that bidirectional arcs can be
        // merged)
        for (int rptDocumentIdx = 0; rptDocumentIdx < rptDocuments.length; rptDocumentIdx++) {
            // handle class label - allows creating everything but arcs
            Document rptDocument = rptDocuments[rptDocumentIdx];
            RPTSourceInfo classLabelInfo = RPTReader.getClassLabelSourceInfo(rptDocument.getDocType().getSystemID(), rptDocument.getRootElement());

            List nodeEles = RPTReader.getRPTNodeList(rptDocument.getDocType().getSystemID(), rptDocument.getRootElement());
            for (Iterator nodeEleIter = nodeEles.iterator(); nodeEleIter.hasNext();) {
                Element nodeEle = (Element) nodeEleIter.next();
                RPTSourceInfo splitInfo = RPTReader.getSplitSourceInfo(rptDocument.getDocType().getSystemID(), nodeEle);
                boolean isMapped = false;
                Object newFeatureName = featureClassMap.get(splitInfo.item);
                if (newFeatureName != null) {
                    splitInfo.item = (String) newFeatureName;
                    isMapped = true;
                }
                Arc arc = new Arc(splitInfo.item, splitInfo.attr, classLabelInfo.item, classLabelInfo.attr, isMapped);  // type1, var1 (feature) -> type2, var2 (class label), isMapped
                saveArc(arc);
            }
        }

        // step 3/3: create arc PNodes
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            makeArcPNodes(arc);
        }
    }

    private void makeArcPNodes(Arc arc) {
        // create line if necessary
        PPath line = getLineForArc(arc);
        if (line == null) {
            line = PPath.createLine(0, 0, 0, 0);    // bounds fixed later
            PPath arrowHead1 = PPath.createEllipse(0, 0, 0, 0);     // bounds fixed later
            PPath arrowHead2 = (arc.isBiDirectional() ?
                    PPath.createEllipse(0, 0, 0, 0) : null);        // bounds fixed later

            line.setPickable(false);
            line.setPaint(null);    // for self-loop
            arrowHead1.setPickable(false);
            arrowHead1.setPaint(Color.BLACK);
            if (arrowHead2 != null) {
                arrowHead2.setPickable(false);
                arrowHead2.setPaint(Color.BLACK);
            }

            PNode circle1;
            if (arc.isFromDegree()) {   // from dot, not var
                circle1 = getDotForType(arc.getType1());
            } else {
                circle1 = getCircleForVar(arc.getType1(), arc.getVar1());
            }
            PNode circle2 = getCircleForVar(arc.getType2(), arc.getVar2());

            // create circles if necessary (some RDNs don't have class labels
            // for all variables)
            if (circle1 == null) {
                String type = arc.getType1();
                String variable = arc.getVar1();
                PPath rect = getRectForType(type);
                saveTypeAndVar(type, variable);
                circle1 = makeCircleForVar(type, variable, rect);
            }
            if (circle2 == null) {
                String type = arc.getType2();
                String variable = arc.getVar2();
                PPath rect = getRectForType(type);
                saveTypeAndVar(type, variable);
                circle2 = makeCircleForVar(type, variable, rect);
            }

            getLayer().addChild(line);
            line.addChild(arrowHead1);
            if (arrowHead2 != null) {
                line.addChild(arrowHead2);
            }

            arcLineMap.put(arc, line);
            line.addClientProperty(LINE_HEAD1_KEY, arrowHead1);
            if (arrowHead2 != null) {
                line.addClientProperty(LINE_HEAD2_KEY, arrowHead2);
            }
            line.addClientProperty(LINE_CIRCLE1_KEY, circle1);
            line.addClientProperty(LINE_CIRCLE2_KEY, circle2);
        }
    }

    private PNode makeCircleForVar(final String type, final String variable, PPath rect) {
        PNode circle = PPath.createEllipse(0, 0, CIRCLE_DIAMETER, CIRCLE_DIAMETER);   // location fixed later
        PText circleLabel = new PText(variable);

        circleLabel.setPickable(false);
        circleLabel.setOffset((CIRCLE_DIAMETER - circleLabel.getWidth()) / 2,
                (CIRCLE_DIAMETER / 2) - (circleLabel.getHeight() / 2));

        rect.addChild(circle);
        circle.addChild(circleLabel);

        typeVarCircleMap.put(type + "." + variable, circle);
        circle.addClientProperty(LABEL_KEY, circleLabel);

        // add listener that resizes containing rect, and updates links
        circle.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent arg0) {
                        noteCircleBoundsChange(type, variable);
                    }
                });
        return circle;
    }

    private PPath makeRectForType(final String type, boolean isObject) {
        PPath rect;
        Color rectColor = getRectColorForType(type);

        // NB: following PPath values are arbitrary (auto-sized based on
        // contents, via noteCircleBoundsChange())
        if (isObject) {
            rect = PPath.createRectangle(0, 0, 1, 1);   // NB: using all 0s doesn't work for some reason
        } else {
            rect = new PPath();
            rect.setPathTo(new RoundRectangle2D.Double(0, 0, 100, 100, 16, 16));    // NB: can't use 1 for w/h or get strange results
        }
        PText rectLabel = new PText(type);
        PPath dot = PPath.createEllipse(0, 0, DOT_DIAMETER, DOT_DIAMETER);  // location fixed later

        rect.setPaint(rectColor);
        rectLabel.setPickable(false);
        rectLabel.setOffset(DOT_DIAMETER * 3, DOT_DIAMETER);
        dot.setPickable(false);
        dot.setOffset(DOT_DIAMETER, DOT_DIAMETER);
        dot.setPaint(Color.BLACK);

        getLayer().addChild(rect);
        rect.addChild(rectLabel);
        rect.addChild(dot);

        typeRectMap.put(type, rect);
        rect.addClientProperty(RECT_DOT_KEY, dot);
        rect.addClientProperty(LABEL_KEY, rectLabel);

        // add listener that updates links
        rect.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent arg0) {
                        noteRectBoundsChange(type);
                    }
                });
        return rect;
    }

    private void makeTypeAndVarPNodes(final String type, final String variable,
                                      boolean isObject) {
        // create rectangle if necessary
        PPath rect = getRectForType(type);
        if (rect == null) {
            rect = makeRectForType(type, isObject);
        }

        // create circle if necessary
        PNode circle = getCircleForVar(type, variable);
        if (circle == null) {
            makeCircleForVar(type, variable, rect);
        }
    }

    /**
     * Called when the bounds of a circle change, updates containing rect and
     * arc lines.
     *
     * @param type     item type for circle
     * @param variable variable for circle
     */
    private void noteCircleBoundsChange(String type, String variable) {
        // update containing rect's bounds
        PNode rect = getRectForType(type);
        shrinkBoundsToFitPNode(rect, type, 10);

        // update connected arcs shafts and heads
        updateArcs(getArcsForVar(type, variable));
    }

    /**
     * Called when the bounds of a rect change, updates connected arcs.
     *
     * @param type
     */
    private void noteRectBoundsChange(String type) {
        // update connected arcs shafts and heads
        updateArcs(getArcsForType(type));
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
    private static double offsetToPerimeters(Point2D r1c, Point2D r2c,
                                             boolean isUpdateR1) {
        // calculate theta
        double thetaDX = (isUpdateR1 ? r2c.getX() - r1c.getX() : r1c.getX() - r2c.getX());
        double thetaDY = (isUpdateR1 ? r2c.getY() - r1c.getY() : r1c.getY() - r2c.getY());
        double theta = Math.atan2(thetaDX, thetaDY);

        // calculate dx, dy
        double radius = CIRCLE_DIAMETER / 2.0;
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
     * Reverses input Map. Recall that the input maps a String to a List of
     * Strings. This method returns a new Map in which each String in the Lists
     * becomes a key, and each value is the (single) former key. For example, if
     * the input Map is: {movie: [studio_movie, actor_movie]} then the output of
     * this method would be: {studio_movie: movie, actor_movie: movie}.
     *
     * @param classFeatureItemMap
     * @return Map that's a 'reverse' of the input
     * @throws IllegalArgumentException if tries to map the same key to different values
     */
    private Map reverseClassFeatureItemMap(Map classFeatureItemMap) {
        HashMap featureClassItemMap = new HashMap();
        Set keys = classFeatureItemMap.keySet();
        for (Iterator classNameIter = keys.iterator(); classNameIter.hasNext();) {
            String className = (String) classNameIter.next();
            List featureNames = (List) classFeatureItemMap.get(className);
            for (Iterator featureNameIter = featureNames.iterator(); featureNameIter.hasNext();) {
                String featureName = (String) featureNameIter.next();
                Object existClassName = featureClassItemMap.get(featureName);
                Assert.isNull(existClassName, "conflicting feature name: " +
                        featureName + ", " + existClassName);

                featureClassItemMap.put(featureName, className);
            }
        }
        return featureClassItemMap;
    }

    private void saveArc(Arc arc) {
        // before saving we check if a reversed version of arc is in my arcs. if
        // there is one this means it should be marked bidrectional. o/w we save
        // arc. note that the following loop also checks if arc is already in arcs
        Arc reverseArc = null;
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc oldArc = (Arc) arcIter.next();
            if (arc.equals(oldArc)) {   // NB: depends on Arc.equals() being content-based
                return;                 // skip duplicate arcs
            } else if (arc.isReverseOf(oldArc)) {
                reverseArc = oldArc;
                break;      // NB: assumes there's only one reversed arc
            }
        }

        if (reverseArc != null) {
            reverseArc.setBiDirectional(true);
        } else {
            arcs.add(arc);
        }
    }

    /**
     * Saves type in my types and variable in my types and typeVarMap.
     *
     * @param type
     * @param variable
     */
    private void saveTypeAndVar(String type, String variable) {
        if (!types.contains(type)) {
            types.add(type);
        }
        List variables = getVariables(type);
        if (variables == null) {
            variables = new ArrayList();
            typeVarMap.put(type, variables);
        }
        variables.add(variable);
    }

    /**
     * Like QueryCanvas.shrinkBoundsToFitPNode(), but only looks at circles, dot,
     * and rect label, and doesn't recurse.
     *
     * @param rect
     * @param type
     * @param indent
     */
    private void shrinkBoundsToFitPNode(PNode rect, String type, int indent) {
        PBounds bounds = new PBounds();     // global bounds of: circles, dot, and rect label

        // add circles
        List variables = getVariables(type);
        for (Iterator varIter = variables.iterator(); varIter.hasNext();) {
            String var = (String) varIter.next();
            PNode circle = getCircleForVar(type, var);
            bounds.add(circle.localToGlobal(circle.getBounds()));
        }

        // add dot and title
        PNode dot = getDotForType(type);
        PText rectLabel = getLabel(rect);
        bounds.add(dot.localToGlobal(dot.getBounds()));
        bounds.add(rectLabel.localToGlobal(rectLabel.getBounds()));

        // done
        bounds.setRect(bounds.getX() - indent, bounds.getY() - indent,
                bounds.getWidth() + 2 * indent, bounds.getHeight() + 2 * indent);
        rect.setBounds(rect.globalToLocal(bounds));
    }

    /**
     * Updates the arrow head(s) of arc.
     *
     * @param arc
     * @param startPt
     * @param endPt
     */
    private void updateArcArrowHeads(Arc arc, Point2D startPt, Point2D endPt) {
        // draw arrowhead 1 (never null)
        PPath arrowHead1 = getArrowHead1ForArc(arc);
        float[][] xsAndYs = new float[0][];
        xsAndYs = QueryCanvas.calcArrowHead(startPt, endPt, 14, 25, 0.8);
        if (xsAndYs != null) {
            arrowHead1.setPathToPolyline(xsAndYs[0], xsAndYs[1]);
            arrowHead1.closePath();
        }

        // draw arrowhead 1 (null if not bidirectional)
        PPath arrowHead2 = getArrowHead2ForArc(arc);
        if (arrowHead2 != null) {
            xsAndYs = QueryCanvas.calcArrowHead(endPt, startPt, 14, 25, 0.8); // NB: reversed start and end
            if (xsAndYs != null) {
                arrowHead2.setPathToPolyline(xsAndYs[0], xsAndYs[1]);
                arrowHead2.closePath();
            }
        }
    }

    /**
     * @param arcs Set of Arc instances
     */
    private void updateArcs(Set arcs) {
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            Point2D[] stPtEndPt = updateArcShaft(arc);
            updateArcArrowHeads(arc, stPtEndPt[0], stPtEndPt[1]);
        }
    }

    /**
     * Updates the endpoints of the node (line or triangle) corresponding to arc.
     *
     * @param arc
     * @return Point2D[] of two Point2D instances: startPt and endPt. to be used
     *         for arrow head orientation
     */
    private Point2D[] updateArcShaft(Arc arc) {
        PPath line = getLineForArc(arc);
        PNode circle1 = getCircle1ForArc(arc);
        PNode circle2 = getCircle2ForArc(arc);
        Point2D r1c = circle1.getBounds().getCenter2D();
        Point2D r2c = circle2.getBounds().getCenter2D();
        PNode rect1 = circle1.getParent();
        if (arc.isSelfLoop()) {
            // self loop: must loop outside rectangle 
            return updateArcShaftSelfLoop(rect1, circle1, line);
        } else if (arc.isMapped() && arc.getType1().equals(arc.getType2())) {
            // mapped arcs within the same type rectangle: must loop outside
            return updateArcShaftMappedLoop(rect1, circle1, circle2, line, arc);
        } else {
            // not self loop or mapped: straight line OK
            return updateArcShaftStraightLine(circle1, r1c, circle2, r2c, line, arc);
        }
    }

    /**
     * Handles the case of drawing a line from circle1 to circle2 that extends
     * beyond the rect's bounds. For now we use straight line segments, and
     * compute routing based on whether each circle is closer to the left or right
     * side, and whether circle2 is above or below circle1. Here are the rules:
     * <ul>
     * <li>The line is drawn using three segments, i.e., it is defined by four points.
     * <li>Segment one is drawn horizontally from circle1, and goes out to a fixed
     * distance beyond the rectangle's boundary. It is drawn to the left if the
     * circle is closer to the left. Otherwise it is drawn to the right.
     * <li>Segment two is drawn vertically and has a fixed length. It is drawn
     * upward if circle2 is above circle1. Otherwise it is drawn downward.
     * <li>Segment three is drawn to the center of circle2.
     * </ul>
     * <p/>
     * Here's an example showing the four points in the case when circle1 (the
     * dot) is closest to the left, and when circle1 is above circle2:
     * <pre>
     * p2    p1
     *     +-------------+
     *  x--+-x Actor     |
     *  |  |             |
     *  x. |    ,+--.    |
     *    `.   / has \   |
     * p3  |`x( Award )  |
     *     |   \     /   |
     *     | p4 '---'    |
     *     |             |
     *     +-------------+
     * </pre>
     *
     * @param rect1
     * @param circle1
     * @param circle2
     * @param line
     * @param arc
     * @return
     */
    private Point2D[] updateArcShaftMappedLoop(PNode rect1, PNode circle1,
                                               PNode circle2, PPath line, Arc arc) {
        PBounds rectBounds = rect1.getGlobalBounds();
        double rectLeft = rectBounds.getX();
        double rectRight = rectBounds.getX() + rectBounds.getWidth();

        PBounds circle1Bounds = circle1.getGlobalBounds();
        double circle1CenterX = circle1Bounds.getCenterX();
        double circle1CenterY = circle1Bounds.getCenterY();

        PBounds circle2Bounds = circle2.getGlobalBounds();
        double circle2CenterX = circle2Bounds.getCenterX();
        double circle2CenterY = circle2Bounds.getCenterY();

        boolean isC1ClosestLeft = isLeftClosestSide(rect1.getBounds(),
                circle1.localToParent(circle1.getBounds().getCenter2D()));
        boolean isC1ABoveC2 = circle1CenterY < circle2CenterY;

        // create points, then set their xs and ys based on circle1's closest side
        // and on whether circle1 is above circle2
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        Point2D.Double p3 = new Point2D.Double();
        Point2D.Double p4 = new Point2D.Double();

        p1.setLocation(circle1CenterX, circle1CenterY);     // is offset to perimeter below after setting p2

        if (isC1ClosestLeft) {
            p2.setLocation(rectLeft - SELF_LOOP_GAP, circle1CenterY);
        } else {        // !isC1ClosestLeft
            p2.setLocation(rectRight + SELF_LOOP_GAP, circle1CenterY);
        }

        if (isC1ABoveC2) {
            p3.setLocation(p2.getX(), p2.getY() + CIRCLE_DIAMETER);     // y offset is arbtrary
        } else {    // !isC1ABoveC2
            p3.setLocation(p2.getX(), p2.getY() - CIRCLE_DIAMETER);     // ""
        }

        p4.setLocation(circle2CenterX, circle2CenterY);

        // offset p1 and p4
        if (!arc.isFromDegree()) {
            offsetToPerimeters(p1, p2, true);
        }
        offsetToPerimeters(p3, p4, false);

        // compute curve and return
        line.setPathToPolyline(new Point2D[]{p1, p2, p3, p4});  // global coords
        return new Point2D[]{p3, p4};
    }

    /**
     * Handles the case of drawing a self loop. Note that we require the line to
     * extend beyond the rect's bounds. For now we draw a triangle around circle1.
     * We do so by computing points relative to the closest side of the rect. Here
     * are the triangle's three points for the case when the loop should be to
     * the right (by convention the arrow is always from p3 to p1):
     * <pre>
     *    ,---.     x p2
     *   /     \  / |
     *  (   + p1)x  |
     *   \     /  ^ |
     *    `---'     x p3
     * </pre>
     *
     * @param rect1
     * @param circle1
     * @param line
     * @return
     */
    private Point2D[] updateArcShaftSelfLoop(PNode rect1, PNode circle1, PPath line) {
        PBounds rectBounds = rect1.getGlobalBounds();
        double rectLeft = rectBounds.getX();
        double rectTop = rectBounds.getY();
        double rectRight = rectBounds.getX() + rectBounds.getWidth();
        double rectBottom = rectBounds.getY() + rectBounds.getHeight();

        PBounds circleBounds = circle1.getGlobalBounds();
        double circleLeft = circleBounds.getX();
        double circleTop = circleBounds.getY();
        double circleRight = circleBounds.getX() + circleBounds.getWidth();
        double circleBottom = circleBounds.getY() + circleBounds.getHeight();
        double circleCenterX = circleBounds.getCenterX();
        double circleCenterY = circleBounds.getCenterY();

        // create points, then set their xs and ys based on closest side
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        Point2D.Double p3 = new Point2D.Double();

        int closestSide = findClosestSide(rect1.getBounds(),
                circle1.localToParent(circle1.getBounds().getCenter2D()));
        if (closestSide == SwingConstants.TOP) {
            p1.setLocation(circleCenterX, circleTop);
            p2.setLocation(circleLeft, rectTop - SELF_LOOP_GAP);
            p3.setLocation(circleRight, rectTop - SELF_LOOP_GAP);
        } else if (closestSide == SwingConstants.LEFT) {
            p1.setLocation(circleLeft, circleCenterY);
            p2.setLocation(rectLeft - SELF_LOOP_GAP, circleTop);
            p3.setLocation(rectLeft - SELF_LOOP_GAP, circleBottom);
        } else if (closestSide == SwingConstants.BOTTOM) {
            p1.setLocation(circleCenterX, circleBottom);
            p2.setLocation(circleLeft, rectBottom + SELF_LOOP_GAP);
            p3.setLocation(circleRight, rectBottom + SELF_LOOP_GAP);
        } else {    // closestSide == SwingConstants.RIGHT
            p1.setLocation(circleRight, circleCenterY);
            p2.setLocation(rectRight + SELF_LOOP_GAP, circleTop);
            p3.setLocation(rectRight + SELF_LOOP_GAP, circleBottom);
        }

        // compute curve and return
        line.setPathToPolyline(new Point2D[]{p1, p2, p3});  // global coords
        line.closePath();
        return new Point2D[]{p3, p1};
    }

    /**
     * Handles the case of drawing a straight line from circle1 to circle2. Note
     * that we do not require the line to extend beyond the rect's bounds.
     *
     * @param circle1
     * @param r1c
     * @param circle2
     * @param r2c
     * @param line
     * @param arc
     * @return
     */
    private Point2D[] updateArcShaftStraightLine(PNode circle1, Point2D r1c,
                                                 PNode circle2, Point2D r2c,
                                                 PPath line, Arc arc) {
        circle1.localToGlobal(r1c);
        circle2.localToGlobal(r2c);

        line.globalToLocal(r1c);
        line.globalToLocal(r2c);
        if (!arc.isFromDegree()) {
            offsetToPerimeters(r1c, r2c, true);
        }
        offsetToPerimeters(r1c, r2c, false);

        Point2D.Double startPt = new Point2D.Double(r1c.getX(), r1c.getY());
        Point2D.Double endPt = new Point2D.Double(r2c.getX(), r2c.getY());
        line.setPathTo(new Line2D.Double(startPt, endPt));

        return new Point2D[]{startPt, endPt};
    }

}
