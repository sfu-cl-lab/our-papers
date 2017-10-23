/**
 * $Id: CreateVertexModeEvtHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import java.awt.Cursor;
import java.awt.geom.Point2D;
import kdl.prox.qgraph2.Subquery;


/**
 * Recall behavior, from QGraphEditorJFrame:
 * o To create a vertex: Click on the background (creates vertex in query) or
 * on a subquery (creates vertex in subquery). Cursor: Shows a cross-hair
 * cursor before the click. Highlight: None. Rubber-band: None.
 */
public class CreateVertexModeEvtHandler extends PBasicInputEventHandler {

    private QGraphEditorJFrame qGraphEditorJFrame;


    public CreateVertexModeEvtHandler(QGraphEditorJFrame qGraphEditorJFrame) {
        this.qGraphEditorJFrame = qGraphEditorJFrame;
    }

    public void mouseClicked(PInputEvent piEvent) {
        super.mouseClicked(piEvent);
        PNode pickedNode = piEvent.getPickedNode();
        if ((pickedNode instanceof PCamera) || QueryCanvas.isPSubquery(pickedNode)) {
            Point2D position = piEvent.getPosition();
            Subquery subquery = (Subquery) QueryCanvas.getAbsQueryChild(pickedNode); // null if none (Query)
            if (qGraphEditorJFrame.getQuery() != null) {
                qGraphEditorJFrame.addNewQGVertex(subquery, position);
            }

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
        if ((pickedNode instanceof PCamera) || QueryCanvas.isPSubquery(pickedNode)) {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            qGraphEditorJFrame.getQueryCanvas().setCursor(Cursor.getDefaultCursor());
        }
    }

}
