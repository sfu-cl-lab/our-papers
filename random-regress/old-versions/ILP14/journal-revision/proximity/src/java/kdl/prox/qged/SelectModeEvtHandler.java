/**
 * $Id: SelectModeEvtHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolox.handles.PHandle;
import edu.umd.cs.piccolox.util.PBoundsLocator;
import edu.umd.cs.piccolox.util.PLocator;
import kdl.prox.qgraph2.AbsQueryChild;
import kdl.prox.qgraph2.QGEdge;

import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Recall behavior, from above:
 * o You can select either one AbsQueryChild, or nothing (no selection).
 * o Cursor: Always the default (arrow) cursor.
 * o To deselect (clear the selection): Click on the background.
 * o To select a vertex: Click inside the circle.
 * o To select an edge: Click on the line.
 * o To select a subquery: Click on a blank spot inside the rectangle (i.e.,
 * not on any of its vertices or edges).
 */
public class SelectModeEvtHandler extends PDragEventHandler {

    private PNodeDecorator pNodeDecorator = new QGEdPNodeDecorator();
    private SwingPropertyChangeSupport changes = new SwingPropertyChangeSupport(this);
    private PNode selectedPNode = null;     // null if no selection


    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }

    public PNode getSelection() {
        return selectedPNode;
    }

    public void mousePressed(PInputEvent piEvent) {
        super.mousePressed(piEvent);
        updateSelection(piEvent);
        if ((piEvent.getClickCount() == 2) && (selectedPNode != null)) {
            changes.firePropertyChange(QGraphEditorJFrame.EDIT_PROPERTY, null, selectedPNode);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }

    private void selectPNodeNoFire(PNode pNode) {
        pNodeDecorator.decoratePNode(pNode);
        selectedPNode = pNode;
    }

    /**
     * Selects pNode and fires QGraphEditorJFrame.SELECT_PROPERTY property. Does
     * nothing if it's already selected.
     *
     * @param pNode PNode to select. pass null (or call unselectAll()) to unselect
     */
    public void setSelection(PNode pNode) {
        updateSelectionInternal(pNode);
    }

    /**
     * Starts drag unless an edge is picked.
     *
     * @param piEvent
     * @return
     */
    protected boolean shouldStartDragInteraction(PInputEvent piEvent) {
        PNode pickedNode = piEvent.getPickedNode();
        return super.shouldStartDragInteraction(piEvent) &&
                (pickedNode != null) && !QueryCanvas.isPEdge(pickedNode);
    }

    public void unselectAll() {
        if (selectedPNode == null) {
            return;
        } else {
            PNode oldSelectedPNode = selectedPNode;
            unselectPNodeNoFire(selectedPNode);
            changes.firePropertyChange(QGraphEditorJFrame.SELECT_PROPERTY,
                    oldSelectedPNode, selectedPNode);
        }
    }

    private void unselectPNodeNoFire(PNode pNode) {
        pNodeDecorator.undecoratePNode(pNode);
        selectedPNode = null;
    }

    private void updateSelection(PInputEvent piEvent) {
        PNode pickedNode = piEvent.getPickedNode();
        updateSelectionInternal(pickedNode instanceof PCamera ? null : pickedNode);
    }

    // state change cases: old, new:
    // a) null, null: do nothing
    // b) null, pNode1: select pNode1
    // c) pNode1, null: deselect pNode1
    // d) pNode1, pNode1: do nothing
    // e) pNode1, pNode2: deselect pNode1, select pNode2
    private void updateSelectionInternal(PNode pickedNode) {
        if (pickedNode == selectedPNode) {
            return;
        }

        // case b, c, or e
        PNode oldSelectedPNode = selectedPNode;
        if (selectedPNode != null) {    // case c, e
            unselectPNodeNoFire(selectedPNode);
        }
        if (pickedNode != null) {       // case b, e
            selectPNodeNoFire(pickedNode);
        }
        changes.firePropertyChange(QGraphEditorJFrame.SELECT_PROPERTY,
                oldSelectedPNode, pickedNode);
    }

    //
    // inner classes
    //

    /**
     * Interface that knows how to decorate and undecorate PNodes with handles.
     */
    private interface PNodeDecorator {

        /**
         * Decorates node with handles. For standard behavior call:
         * PBoundsHandle.addBoundsHandlesTo(node);
         *
         * @param pNode
         */
        public void decoratePNode(PNode pNode);

        /**
         * Undecorates node with handles. For standard behavior call:
         * PBoundsHandle.removeBoundsHandlesFrom(node);
         *
         * @param pNode
         */
        public void undecoratePNode(PNode pNode);

    }


    /**
     * Standard PNodeDecorator for the query editor, uses non-resizing handles,
     * and adds handles at ends for non-self-loop edges, and in corners for self-
     * loop edges, vertices and subqueries. We handle self-loop edges specially
     * because they're somewhat folded-up, which makes identifying the selected
     * one difficult with just two handles. In this way they're more like a
     * rectangle than a line.
     */
    private static class QGEdPNodeDecorator implements PNodeDecorator {

        public void decoratePNode(PNode pNode) {
            AbsQueryChild absQueryChild = QueryCanvas.getAbsQueryChild(pNode);
            if ((absQueryChild instanceof QGEdge) && !(((QGEdge) absQueryChild).isSelfLoop())) {
                // if non-self-loop edge, add handles at either end
                PPath pEdge = (PPath) pNode;
                pNode.addChild(new PHandle(new PPathLocator(pEdge, true)));
                pNode.addChild(new PHandle(new PPathLocator(pEdge, false)));
            } else {
                // else (vertex or subquery) add handles at each corner
                pNode.addChild(new PHandle(PBoundsLocator.createNorthEastLocator(pNode)));
                pNode.addChild(new PHandle(PBoundsLocator.createNorthWestLocator(pNode)));
                pNode.addChild(new PHandle(PBoundsLocator.createSouthEastLocator(pNode)));
                pNode.addChild(new PHandle(PBoundsLocator.createSouthWestLocator(pNode)));
            }
        }

        public void undecoratePNode(PNode pNode) {
            // remove PHandles. NB: can't use PBoundsHandle.removeBoundsHandlesFrom()
            // because selectPNode() uses PHandles, not PBoundsHandles
            ArrayList handles = new ArrayList();
            Iterator i = pNode.getChildrenIterator();
            while (i.hasNext()) {
                PNode each = (PNode) i.next();
                if (each instanceof PHandle) {  // NB: this is the only line that's different from PBoundsHandle.removeBoundsHandlesFrom()
                    handles.add(each);
                }
            }
            pNode.removeChildren(handles);
        }

    }


    /**
     * A kind of PLocator used by QGEdPNodeDecorator to decorate the end-points
     * of a PPath.
     */
    private static class PPathLocator extends PLocator {

        private PPath pPath;
        private boolean isFirstPoint;   // true of PPath's first point should be used for locateX() and locateY(). false if the PPath's last point should be used


        public PPathLocator(PPath pPath, boolean isFirstPoint) {
            this.pPath = pPath;
            this.isFirstPoint = isFirstPoint;
        }

        public double locateX() {
            GeneralPath genPath = pPath.getPathReference();
            float[] coords = new float[6];
            PathIterator iterator = genPath.getPathIterator(null, 1.0);
            for (int segIdx = 0; !iterator.isDone(); iterator.next(), segIdx++) {
                iterator.currentSegment(coords);
                if (isFirstPoint) {
                    return coords[0];
                }
            }
            return coords[0];
        }

        public double locateY() {
            GeneralPath genPath = pPath.getPathReference();
            float[] coords = new float[6];
            PathIterator iterator = genPath.getPathIterator(null, 1.0);
            for (int segIdx = 0; !iterator.isDone(); iterator.next(), segIdx++) {
                iterator.currentSegment(coords);
                if (isFirstPoint) {
                    return coords[1];
                }
            }
            return coords[1];
        }

    }


}
