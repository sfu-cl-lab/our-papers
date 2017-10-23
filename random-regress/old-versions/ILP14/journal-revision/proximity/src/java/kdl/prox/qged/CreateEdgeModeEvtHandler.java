/**
 * $Id: CreateEdgeModeEvtHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolox.handles.PBoundsHandle;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import kdl.prox.qgraph2.AbstractQuery;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.QueryIterHandler;
import kdl.prox.qgraph2.QueryIterHandlerEmptyAdapter;
import kdl.prox.qgraph2.QueryIterator;
import kdl.prox.qgraph2.Subquery;


/**
 * Recall behavior, from QGraphEditorJFrame:
 * o To create an edge: Mouse-down on the first vertex, drag to the second
 * vertex, and mouse-up on it. Cursor: Shows an arrow cursor before the
 * mouse-down. Highlight: The first vertex (v1) is highlighted after
 * mouse-down, and candidate v2s are highlighted during dragging.
 * Rubber-band: A dashed line is shown during the drag.
 */
public class CreateEdgeModeEvtHandler extends PDragSequenceEventHandler {

    private QGraphEditorJFrame qGraphEditorJFrame;
    private PPath line = null;          // represents the edge that is currently being created. null if not dragging
    private PNode currPVertex2 = null;  // current candidate V2 PVertex under the mouse. null if none, or if not dragging


    public CreateEdgeModeEvtHandler(QGraphEditorJFrame qGraphEditorJFrame) {
        this.qGraphEditorJFrame = qGraphEditorJFrame;
    }

    protected void drag(PInputEvent piEvent) {
        super.drag(piEvent);
        updateLineAndV2Handles(piEvent);
    }

    protected void endDrag(PInputEvent piEvent) {
        super.endDrag(piEvent);
        qGraphEditorJFrame.getQueryCanvas().resetPickable(qGraphEditorJFrame.getQuery());
        qGraphEditorJFrame.getQueryCanvas().getLayer().removeChild(line);
        line = null;

        // create the edge
        PNode pVertex1 = piEvent.getPickedNode();
        PNode pVertex2 = piEvent.getInputManager().getMouseOver().getPickedNode();

        PBoundsHandle.removeBoundsHandlesFrom(pVertex1);
        if (currPVertex2 != null) {
            PBoundsHandle.removeBoundsHandlesFrom(currPVertex2);
        }
        currPVertex2 = null;

        QGVertex aqChild1 = (QGVertex) QueryCanvas.getAbsQueryChild(pVertex1);
        QGVertex aqChild2 = (QGVertex) QueryCanvas.getAbsQueryChild(pVertex2);
        if ((aqChild1 != null) && (aqChild2 != null)) {
            qGraphEditorJFrame.addNewQGEdge(aqChild1, aqChild2);
        }
    }

    /**
     * Updates the mouse cursor based on what the mouse is over.
     *
     * @param piEvent
     */
    public void mouseEntered(PInputEvent piEvent) {
        super.mouseEntered(piEvent);
        PNode mouseOverPNode = piEvent.getInputManager().getMouseOver().getPickedNode();
        if (QueryCanvas.isPVertex(mouseOverPNode)) {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)); // todo better cursor
        } else {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Sets the pickable of AbsQueryChild instances in query so that only
     * vertices are pickable.
     */
    private void setPickable() {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void edge(QGEdge qgEdge) {
                PNode pNode = qGraphEditorJFrame.getQueryCanvas().getPNode(qgEdge);
                pNode.setPickable(false);
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (abstractQuery instanceof Subquery) {
                    PNode pNode = qGraphEditorJFrame.getQueryCanvas().getPNode(abstractQuery);
                    pNode.setPickable(false);
                }
            }

            public void vertex(QGVertex qgVertex) {
                PNode pNode = qGraphEditorJFrame.getQueryCanvas().getPNode(qgVertex);
                pNode.setPickable(true);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(qGraphEditorJFrame.getQuery());
    }

    protected boolean shouldStartDragInteraction(PInputEvent piEvent) {
        return super.shouldStartDragInteraction(piEvent) &&
                QueryCanvas.isPVertex(piEvent.getPickedNode()); // aka isOverVertex()
    }

    protected void startDrag(PInputEvent piEvent) {
        setPickable();
        super.startDrag(piEvent);

        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);
        line = new PPath();
        line.setPickable(false);
        line.setStroke(dashed);

        qGraphEditorJFrame.getQueryCanvas().getLayer().addChild(line);
        updateLineAndV2Handles(piEvent);

        PNode pVertex1 = piEvent.getPickedNode();
        PBoundsHandle.addBoundsHandlesTo(pVertex1);
    }

    public void updateLineAndV2Handles(PInputEvent piEvent) {
        // update line based on V1 and candidate V2. if no V2 under mouse, use
        // current mouse position (enables 'snapping' to node centers)
        PNode pVertex1 = piEvent.getPickedNode();
        PNode pVertex2 = piEvent.getInputManager().getMouseOver().getPickedNode();
        Point2D startPt = pVertex1.getBounds().getCenter2D();
        pVertex1.localToGlobal(startPt);
        Point2D endPt;
        boolean isMouseOverPVert = QueryCanvas.isPVertex(pVertex2);
        if (isMouseOverPVert) {
            // use candidate V2's center (snaps)
            endPt = pVertex2.getBounds().getCenter2D();
            pVertex2.localToGlobal(endPt);
        } else {
            // use current mouse position (no snap)
            endPt = piEvent.getPosition();
        }
        Line2D line2D = new Line2D.Double(startPt, endPt);
        line.setPathTo(line2D);

        // update V2 handles
        if (currPVertex2 != pVertex2) {
            if ((currPVertex2 != null) && (currPVertex2 != pVertex1)) {
                PBoundsHandle.removeBoundsHandlesFrom(currPVertex2);
            }
            if (isMouseOverPVert && (pVertex2 != pVertex1)) {
                PBoundsHandle.addBoundsHandlesTo(pVertex2);
            }
            currPVertex2 = pVertex2;
        }
    }

}
