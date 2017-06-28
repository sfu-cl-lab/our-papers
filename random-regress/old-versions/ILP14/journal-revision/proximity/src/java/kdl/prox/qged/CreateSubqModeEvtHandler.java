/**
 * $Id: CreateSubqModeEvtHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolox.event.PSelectionEventHandler;
import kdl.prox.qgraph2.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Recall behavior, from QGraphEditorJFrame:
 * o To create a subquery: Mouse-down on the background, drag to enclose the
 * desired vertices, and mouse-up to create the subquery around the surrounded
 * vertices. Cursor: Same as vertex creation. Highlight: The candidate
 * vertices are highlighted during the drag. Note that only non-subquery
 * (i.e., blue) vertices are highlighted, not edges or subqueries. Rubber-band:
 * A dashed rectangle is shown during the drag.
 */
public class CreateSubqModeEvtHandler extends PSelectionEventHandler {

    private QGraphEditorJFrame qGraphEditorJFrame;


    public CreateSubqModeEvtHandler(QGraphEditorJFrame qGraphEditorJFrame) {
        super(qGraphEditorJFrame.getQueryCanvas().getLayer(), qGraphEditorJFrame.getQueryCanvas().getSelectableParents());
        this.qGraphEditorJFrame = qGraphEditorJFrame;
    }

    protected void endDrag(PInputEvent e) {
        super.endDrag(e);
        qGraphEditorJFrame.getQueryCanvas().resetPickable(qGraphEditorJFrame.getQuery());
        Collection selection = getSelection();
        unselectAll();

        // create the subquery
        ArrayList qgVertices = new ArrayList();
        for (Iterator pNodeIter = selection.iterator(); pNodeIter.hasNext();) {
            PNode pNode = (PNode) pNodeIter.next();
            qgVertices.add(QueryCanvas.getAbsQueryChild(pNode));
        }
        if (qgVertices.size() != 0) {
            qGraphEditorJFrame.addNewSubquery(qgVertices);
        }
    }

    /**
     * Updates the mouse cursor based on what the mouse is over.
     *
     * @param piEvent
     */
    public void mouseEntered(PInputEvent piEvent) {
        super.mouseEntered(piEvent);
        PNode pickedNode = piEvent.getPickedNode();
        if (pickedNode instanceof PCamera) {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Sets the pickable of AbsQueryChild instances in query so that only
     * non-subquery (i.e., blue) vertices are pickable.
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
                pNode.setPickable(qgVertex.parentAQuery() instanceof Query ?
                        true : false);
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(qGraphEditorJFrame.getQuery());
    }

    protected boolean shouldStartDragInteraction(PInputEvent piEvent) {
        PNode pickedNode = piEvent.getPickedNode();
        return super.shouldStartDragInteraction(piEvent) &&
                (qGraphEditorJFrame.getQuery() != null) &&
                (pickedNode instanceof PCamera) && piEvent.isLeftMouseButton(); // todo cleaner to do last with filter?
    }

    protected void startDrag(PInputEvent e) {
        setPickable();
        super.startDrag(e);
    }

}
