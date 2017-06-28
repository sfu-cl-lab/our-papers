/**
 * $Id: QGraphEditorJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolox.swing.PScrollPane;
import kdl.prox.app.GUI2App;
import kdl.prox.db.DB;
import kdl.prox.gui2.RunFileBean;
import kdl.prox.gui2.RunFileJFrame;
import kdl.prox.gui2.RunQueryImpl;
import kdl.prox.qgraph2.*;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;
import spin.Spin;
import spin.off.ListenerSpinOver;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;


/**
 * The main QGraph Editor JFrame. Has two kinds of mouse interaction modes:
 * Select, and Create. There is one Select mode, in which the user can select and
 * operate on zero or one AbsQueryChild (vertex, edge, or subquery). The operations
 * on the selected item include edit properties and delete. There are three
 * Create modes, one each for the three types of AbsQueryChild. Details follow.
 * <p/>
 * The one Select mode works as follows:
 * <ul>
 * <li>You can select either one AbsQueryChild, or nothing (no selection).
 * <li><i>Cursor:</i> Always the default (arrow) cursor.
 * <li>To deselect (clear the selection): Click on the background.
 * <li>To select a vertex: Click inside the circle.
 * <li>To select an edge: Click on the line.
 * <li>To select a subquery: Click on a blank spot inside the rectangle (i.e.,
 * not on any of its vertices or edges).
 * </ul>
 * <p/>
 * The three Create modes work as follows:
 * <ul>
 * <li>For all modes: There is no selection, except during edge and subquery creation.
 * <li>To create a <b>vertex</b>: Click on the background (creates vertex in query)
 * or on a subquery (creates vertex in subquery). <i>Cursor:</i> Shows a cross-hair
 * cursor before the click. <i>Highlight:</i> None. <i>Rubber-band:</i> None.
 * <li>To create an <b>edge</b>: Mouse-down on the first vertex, drag to the second
 * vertex, and mouse-up on it. <i>Cursor:</i> Shows an arrow cursor before the
 * mouse-down. <i>Highlight:</i> The first vertex (v1) is highlighted after
 * mouse-down, and candidate v2s are highlighted during dragging. <i>Rubber-band:</i>
 * A dashed line is shown during the drag.
 * <li>To create a <b>subquery</b>: Mouse-down on the background, drag to enclose
 * the desired vertices, and mouse-up to create the subquery around the surrounded
 * vertices. <i>Cursor:</i> Shows a cross-hair cursor before the click.
 * <i>Highlight:</i> The candidate vertices are highlighted during the drag. Note
 * that only non-subquery (i.e., blue) vertices are highlighted, not edges or
 * subqueries. <i>Rubber-band:</i> A dashed rectangle is shown during the drag.
 * </ul>
 * <p/>
 * UI: Contains two JTrees at the bottom, side-by-side: One showing the Query's
 * table model, and the other showing the current selection's table model. More
 * specifically, here's the containment:
 * <pre>
 * content pane (border layout)
 *   queryCanvas (maybe in scroll pane) - center
 *   infoJPanel (border layout)
 *     propsJPanel (grid layout - 1 row, 2 columns) - center
 *       queryPropsJPanel (vert box layout) - left
 *         queryJTable (in scroll pane)
 *         constButtonJPanel (horiz box layout)
 *           'Constraints' JLabel
 *           addConstJButton
 *           removeConstJButton
 *         addLinkButtonJPanel (horiz box layout)
 *           'addEdges' JLabel
 *           addAddLinkJButton
 *           removeAddLinkButton
 *       selectionJTable (in scroll pane) - right
 *     validationJComboBox - south
 * </pre>
 */
public class QGraphEditorJFrame extends JFrame implements TableModelListener {

    private static final Logger log = Logger.getLogger(QGraphEditorJFrame.class);
    public static final String SELECT_PROPERTY = "SELECT_PROPERTY"; // used by SelectModeEvtHandler. oldValue: previously-selected PNode. newValue: newly-selected PNode
    public static final String EDIT_PROPERTY = "EDIT_PROPERTY";     // "". oldValue: null. newValue: edited PNode

    private Query query = null;         // query currently being edited. null if no query
    private boolean isDirty = false;    // true if query has been modified
    private File queryFile = null;      // file corresponding to query. null if no query, or if new query
    private QueryCanvas queryCanvas;                        // canvas displaying query
    private JTable queryJTable = new QGEdJTable(0, 0);      // query's properties
    private JTable selectionJTable = new QGEdJTable(0, 0);  // current selection's properties
    private JComboBox validationJComboBox;      // shows query validation results
    private JCheckBoxMenuItem annotEdgesJCheckBoxMI;    // controls whether new edges get a [1..] annotation (if checked) or not

    private PInputEventListener currentPIEventListener;     // current listener. changed when modes entered. always set to one of the following:
    private SelectModeEvtHandler selectModeEvtHandler;          // handler for select mode
    private CreateEdgeModeEvtHandler edgeModeEvtHandler;        // "" create edge mode
    private CreateSubqModeEvtHandler subqModeEvtHandler;        // "" create subquery mode
    private CreateVertexModeEvtHandler vertexModeEvtHandler;    // "" create vertex mode

    private JRadioButtonMenuItem[] editModeJRadioButtons;   // order: select, vertex, edge, subquery
    private JToggleButton[] editModeJToggleButtons;         // ""

    private Action addAddLinkAction;
    private Action addConstraintAction;
    private Action closeFileAction;
    private Action deleteAQChildAction;
    private Action edgeModeAction;
    private Action exitEditorAction;
    private Action flipEdgeAction;
    private Action newFileAction;
    private Action openFileAction;
    private Action removeAddLinkAction;
    private Action removeConstraintAction;
    private Action runQueryAction;
    private Action saveFileAction;
    private Action saveFileAsAction;
    private Action selectModeAction;
    private Action selectNextItemAction;
    private Action selectPrevItemAction;
    private Action subqModeAction;
    private Action vertexModeAction;
    private Action zoom100PctAction;
    private Action zoomInAction;
    private Action zoomOutAction;
    private Action zoomToFitAction;


    public QGraphEditorJFrame() throws Exception {
        this((Element) null);
    }

    /**
     * @param queryFile query file to open. null if no query to open
     * @throws Exception if problems loading or creating the query
     */
    public QGraphEditorJFrame(File queryFile) throws Exception {
        this(QueryXMLUtil.graphQueryEleFromFile(queryFile));
        this.queryFile = queryFile;
    }

    public QGraphEditorJFrame(Element queryElement) throws Exception {
        // build UI
        addQueryJTableSelectionListeners();
        makeActions();
        setJMenuBar(makeJMenuBar());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeJToolBar(), BorderLayout.WEST);
        getContentPane().add(makeQueryCanvas(), BorderLayout.CENTER);
        getContentPane().add(makeInfoJPanel(), BorderLayout.SOUTH);

        makeModeEventHandlers();
        enterMode(selectModeEvtHandler);    // NB: matches default mode (select) in makeEditJMenu() and makeJToolBar()

        // catch window closings in order to offer option of saving if dirty
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   // means we have to hide it ourselves
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                exitQueryEditor();
            }

        });

        // load the passed query, if necessary
        boolean isPerfectLayout = false;
        if (queryElement != null) {
            isPerfectLayout = loadQuery(queryElement);
        } else {
            newQuery();
        }

        // show it
        setSize(getPreferredSize());
        if (!isPerfectLayout) {
            doRandomLayout();
        }
        setVisible(true);
    }


    /**
     * Called when saving a query, adds an &lt;editor-data&gt; section to
     * graphQuery, which includes layout information from my queryCanvas.
     *
     * @param graphQueryEle
     */
    private void addEditorDataEle(Element graphQueryEle) {
        final Element editorDataEle = new Element("editor-data");

        // iterate over QGVertex instances in model, saving locations (ULC)
        // in editorDataEle
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryHandler = new QueryIterHandlerEmptyAdapter() {

            public void vertex(QGVertex qgVertex) {
                PNode vertexPNode = queryCanvas.getPNode(qgVertex);
                Point2D offset = vertexPNode.getGlobalTranslation();
                Element vertLocEle = new Element("vertex-location");
                vertLocEle.setAttribute("name", qgVertex.firstName());
                vertLocEle.setAttribute("x", String.valueOf(offset.getX()));
                vertLocEle.setAttribute("y", String.valueOf(offset.getY()));
                editorDataEle.addContent(vertLocEle);
            }

        };
        queryIter.setHandler(queryHandler);
        queryIter.iterate(query);

        graphQueryEle.addContent(editorDataEle);
    }

    private void addNewQGAddLink() {
        QGAddLink qgAddLink = new QGAddLink("vertex1", "vertex2", "attrname", "attrval");
        query.addAddLink(qgAddLink);
        queryJTable.tableChanged(new TableModelEvent(queryJTable.getModel()));

        // edit the cell
        int rowIndex = queryJTable.getModel().getRowCount() - 1;
        int colIndex = queryJTable.getModel().getColumnCount() - 1;
        scrollToCell(queryJTable, rowIndex, colIndex);      // for some reason the edit doesn't work if the cell isn't visible
        queryJTable.editCellAt(rowIndex, colIndex);
        ((JTextField) queryJTable.getEditorComponent()).selectAll();
        queryJTable.getEditorComponent().requestFocus();    // otherwise no insertion point (even though 'editing')

        updateValidationJComboBox();
    }

    private void addNewQGConstraint() {
        QGConstraint newConstraint = new QGConstraint();
        query.addConstraint(newConstraint);
        queryJTable.tableChanged(new TableModelEvent(queryJTable.getModel()));

        // edit new constraint's cell
        int rowIndex = query.constraints().size() + 2;
        int colIndex = queryJTable.getModel().getColumnCount() - 1;
        scrollToCell(queryJTable, rowIndex, colIndex);      // for some reason the edit doesn't work if the cell isn't visible
        queryJTable.editCellAt(rowIndex, colIndex);
        ((JTextField) queryJTable.getEditorComponent()).selectAll();
        queryJTable.getEditorComponent().requestFocus();    // otherwise no insertion point (even though 'editing')

        updateQGConstraint(newConstraint);
        updateValidationJComboBox();

        // todo bug enable "Remove" button:
//        updateActions();    // didn't work - getQueryJTableSelectedObject() is null here
//        removeConstraintAction.setEnabled(true);  // also didn't work for same reason
    }

    /**
     * Adds a new QGEdge connected between the passed QGVertices. Annotates
     * it with [1..] if annotEdgesJCheckBoxMI is checked (selected).
     *
     * @param qgVert1
     * @param qgVert2
     */
    public void addNewQGEdge(QGVertex qgVert1, QGVertex qgVert2) {
        // create the edge
        QGEdge qgEdge = new QGEdge(getNextUniqueQGItemName(false), null, null,
                qgVert1.firstName(), qgVert2.firstName(), "true");
        qgEdge.setVertex1(qgVert1);
        qgEdge.setVertex2(qgVert2);
        if (annotEdgesJCheckBoxMI.isSelected()) {
            qgEdge.setAnnotation(new Annotation(1, -1));
        }

        // add the edge to the appropriate AbstractQuery, and to its two vertices.
        // note that the logic to determine the parent is simple, and allows
        // the creation of invalid queries
        AbstractQuery targetAbsQuery;
        AbstractQuery qgVert1ParentAQ = qgVert1.parentAQuery();
        AbstractQuery qgVert2ParentAQ = qgVert2.parentAQuery();
        boolean isAQ1ParentOfAQ2 = ((qgVert2ParentAQ instanceof Subquery) &&
                (((Subquery) qgVert2ParentAQ).parentAQuery() == qgVert1ParentAQ));
        boolean isAQ2ParentOfAQ1 = ((qgVert1ParentAQ instanceof Subquery) &&
                (((Subquery) qgVert1ParentAQ).parentAQuery() == qgVert2ParentAQ));
        if (qgVert1ParentAQ == qgVert2ParentAQ) {
            targetAbsQuery = qgVert1ParentAQ;
        } else if (isAQ1ParentOfAQ2) {
            targetAbsQuery = qgVert1ParentAQ;
        } else if (isAQ2ParentOfAQ1) {
            targetAbsQuery = qgVert2ParentAQ;
        } else if ((qgVert1ParentAQ == query) || (qgVert2ParentAQ == query)) {
            log.warn("addNewQGEdge(): choosing arbitrary parent (query): " +
                    qgVert1 + " -> " + qgVert2);
            targetAbsQuery = query;
        } else {
            log.warn("addNewQGEdge(): choosing arbitrary parent (v1): " +
                    qgVert1 + " -> " + qgVert2);
            targetAbsQuery = qgVert1ParentAQ;
        }
        targetAbsQuery.addEdge(qgEdge);
        qgVert1.addEdge(qgEdge);
        if (qgVert1 != qgVert2) {   // in case it's a self-loop
            qgVert2.addEdge(qgEdge);
        }

        // add the edge to the canvas, and connect it up. note that we update
        // *all* links connected to either end of the new edge, because those
        // vertices might have multiple edges connected to them which would also
        // need updating
        queryCanvas.addAbsQueryChild(qgEdge);
        queryCanvas.connectPEdge(qgEdge);
        queryCanvas.updateLinksForQGVertex(qgEdge.vertex1());
        queryCanvas.updateLinksForQGVertex(qgEdge.vertex2());

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    /**
     * Adds a new QGVertex to either subquery (if non-null) or my query (if null)
     * at the passed position.
     *
     * @param subquery
     * @param position
     */
    public void addNewQGVertex(Subquery subquery, Point2D position) {
        // create the vertex
        QGVertex qgVertex = new QGVertex(getNextUniqueQGItemName(true), null, null);
        if (subquery != null) {
            subquery.addVertex(qgVertex);
        } else {
            query.addVertex(qgVertex);
        }
        queryCanvas.addAbsQueryChild(qgVertex);

        // position the new vertex
        PNode pNode = queryCanvas.getPNode(qgVertex);
        position.setLocation(position.getX() - (QueryCanvas.VERT_DIAM / 2),
                position.getY() - (QueryCanvas.VERT_DIAM / 2));
        pNode.setGlobalTranslation(position);   // NB: assumes piEvent comes from camera

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    /**
     * Adds to my query a new Subquery that contains the passed QGVertex
     * instances, as well as the appropriate edges between them. Each QGVertex
     * must not already be in a Subquery.
     *
     * @param qgVertices
     */
    public void addNewSubquery(List qgVertices) {
        // add the new Subquery
        Subquery subquery = new Subquery(query);
        subquery.setAnnotation(new Annotation(1, -1));
        query.addSubquery(subquery);
        queryCanvas.addAbsQueryChild(subquery);
        queryCanvas.getPNode(subquery).moveToBack();

        // move the vertices to the new subquery, ignoring edges for this first
        // pass. we move nodes in two steps - remove then add. this worked better
        // than changing parents (and handling label groups, etc.)
        for (Iterator qgVertIter = qgVertices.iterator(); qgVertIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertIter.next();
            Assert.condition(!(qgVertex.parentAQuery() instanceof Subquery),
                    "vertex already in a Subquery: " + qgVertex);
            Point2D vertPNodeOffset = queryCanvas.getPNode(qgVertex).getOffset();
            query.removeVertex(qgVertex);
            queryCanvas.removePNodesForAQChildren(Collections.singletonList(qgVertex));
            subquery.addVertex(qgVertex);
            queryCanvas.addAbsQueryChild(qgVertex);
            queryCanvas.getPNode(qgVertex).setOffset(vertPNodeOffset);  // restore vertex location
        }

        // move the edges. we do this in a second pass after all the vertices
        // because it was the easiest way to get node layering right
        Set qgEdges = qgEdgesFromQGVerticies(qgVertices);
        for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            boolean isShouldBeInSubq = qgVertices.contains(qgEdge.vertex1())
                    && qgVertices.contains(qgEdge.vertex2());
            if (isShouldBeInSubq && (qgEdge.parentAQuery() != subquery)) {
                query.removeEdge(qgEdge);
                queryCanvas.removePNodesForAQChildren(Collections.singletonList(qgEdge));
                subquery.addEdge(qgEdge);
                queryCanvas.addAbsQueryChild(qgEdge);
            }
            PPath pEdge = (PPath) queryCanvas.getPNode(qgEdge); // todo dangerous!?
            QueryCanvas.updateLink(pEdge);
            queryCanvas.connectPEdge(qgEdge);
        }

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    private void addQueryJTableSelectionListeners() {
        // get query property selection changes so we can update removeConstraintAction.
        // to get cell changes we have to register for both row and column changes
        queryJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;     // ignore extra messages
                }

                updateActions();
            }
        });
        queryJTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnMarginChanged(ChangeEvent e) {
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;     // ignore extra messages
                }

                updateActions();
            }

            public void columnAdded(TableColumnModelEvent e) {
            }

            public void columnMoved(TableColumnModelEvent e) {
            }

            public void columnRemoved(TableColumnModelEvent e) {
            }
        });
    }

    private void clearCanvasSelection() {
        selectModeEvtHandler.unselectAll();    // todo xx honor mode
    }

    private void clearSelectionTable() {
        selectionJTable.setModel(new DefaultTableModel(0, 0));
    }

    /**
     * Closes the current query, resetting my query and queryFile IVs. Honors
     * isDirty by asking first if user wants to save (if necessary).
     */
    private void closeQuery() throws Exception {
        if (!saveQueryIfModified()) {
            return;
        }

        query = null;
        queryFile = null;
        queryCanvas.clear();
        updateQueryJTable(null);
        clearSelectionTable();
        isDirty = false;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    /**
     * Deletes the currently-selected AbsQueryChild. If it's a QGItem then
     * removes it from the query. Otherwise (for a Subquery) 'flattens' it by
     * moving contained QGItems to parent (Query).
     */
    private void deleteAQChild() {
        // update the Query, then tell queryCanvas to reflect changes
        List removedAQChildren = new ArrayList();   // filled next. used to update canvas
        AbsQueryChild selAQChild = getSelectedAbsQueryChild();
        if (selAQChild instanceof QGVertex) {
            AbstractQuery parentAQ = selAQChild.parentAQuery();
            removeQGVertex((QGVertex) selAQChild, removedAQChildren);
            queryCanvas.removePNodesForAQChildren(removedAQChildren);

            // update bounds of containing subquery, if exists
            if (parentAQ instanceof Subquery) {
                queryCanvas.updateSubqueryBounds((Subquery) parentAQ);
            }
        } else if (selAQChild instanceof QGEdge) {
            QGEdge qgEdge = (QGEdge) selAQChild;
            removeQGEdge(qgEdge, removedAQChildren);
            queryCanvas.removePNodesForAQChildren(removedAQChildren);

            // update *all* links connected to either end of the deleted edge,
            // because those vertices might have multiple edges connected to
            // them which would also need updating
            queryCanvas.updateLinksForQGVertex(qgEdge.vertex1());
            queryCanvas.updateLinksForQGVertex(qgEdge.vertex2());
        } else {    // selAQChild instanceof Subquery
            deleteAQChildSubquery(selAQChild);
        }

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    private void deleteAQChildSubquery(AbsQueryChild selAQChild) {
        List removedAQChildren = new ArrayList();   // filled next. used to update canvas

        // fill removedAQChildren
        Subquery subquery = (Subquery) selAQChild;
        Set subqQGItems = subquery.qgItems(false);  // todo xx isRecurse
        removedAQChildren.add(subquery);
        removedAQChildren.addAll(subqQGItems);

        // save vertex node positions for later restoration
        HashMap qgVertexOffsetMap = new HashMap();  // maps a QGVertex to its offset (Point2D)
        for (Iterator qgItemIter = subqQGItems.iterator(); qgItemIter.hasNext();) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            PNode qgItemPNode = queryCanvas.getPNode(qgItem);
            if (qgItem instanceof QGVertex) {
                qgVertexOffsetMap.put(qgItem, qgItemPNode.getGlobalTranslation());
            }
        }

        // move the children from the subquery to the query, remove the
        // corresponding nodes, then add them back in at the top. NB: like
        // addNewQGSubquery(), we add all vertices first then edges (this was
        // the easiest way to get correct layering)
        query.flattenSubquery(subquery);
        queryCanvas.removePNodesForAQChildren(removedAQChildren);

        for (Iterator qgVertIter = qgVertexOffsetMap.keySet().iterator(); qgVertIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertIter.next();
            Point2D vertPNodeOffset = (Point2D) qgVertexOffsetMap.get(qgVertex);
            queryCanvas.addAbsQueryChild(qgVertex);
            queryCanvas.getPNode(qgVertex).setOffset(vertPNodeOffset);  // restore vertex location
        }

        // edge pass. NB: instead of iterating directly over QGEdges in
        // subqQGItems, we go through all QGEdges connected to vertices in
        // subqQGItems. this way we update all edges that are connected to
        // vertices, not just those moving from the subquery. o/w edge node
        // connections are left in an invalid state
        Set qgEdges = qgEdgesFromQGVerticies(qgVertexOffsetMap.keySet());
        for (Iterator qgEdgeIter = qgEdges.iterator(); qgEdgeIter.hasNext();) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            if (subqQGItems.contains(qgEdge)) {
                queryCanvas.addAbsQueryChild(qgEdge);
            }

            PPath pEdge = (PPath) queryCanvas.getPNode(qgEdge); // todo dangerous!?;
            QueryCanvas.updateLink(pEdge);
            queryCanvas.connectPEdge(qgEdge);
        }
    }

    /**
     * NB: must be called *after* the canvas is instantiated.
     */
    private void doRandomLayout() {
        // for now do random layout. todo better one, such as used by QueryLayoutAlgorithm?
        Random random = new Random();
        Set qgItems = query.qgItems(true);  // todo xx isRecurse
        for (Iterator qgItemIter = qgItems.iterator(); qgItemIter.hasNext();) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            PNode pNode = queryCanvas.getPNode(qgItem);
            int x = random.nextInt(getWidth() / 2);
            int y = random.nextInt(getHeight() / 2);
            pNode.setOffset(x, y);  // bug should translate to global?
        }
    }

    /**
     * Switches to the passed mode, updating related menu item and toolbar
     * button.
     */
    private void enterMode(PInputEventListener newModeEventHandler) {
        Cursor cursor = Cursor.getDefaultCursor();  // todo xx set based on newModeEventHandler and node under mouse
        queryCanvas.setCursor(cursor);
        clearCanvasSelection();
        queryCanvas.removeInputEventListener(currentPIEventListener);
        currentPIEventListener = newModeEventHandler;
        queryCanvas.addInputEventListener(currentPIEventListener);

        // update corresponding menu item and toobar button
        int buttonIdx;
        if (newModeEventHandler == selectModeEvtHandler) {
            buttonIdx = 0;
        } else if (newModeEventHandler == vertexModeEvtHandler) {
            buttonIdx = 1;
        } else if (newModeEventHandler == edgeModeEvtHandler) {
            buttonIdx = 2;
        } else {   // newModeEventHandler == subqModeEvtHandler
            buttonIdx = 3;
        }
        editModeJRadioButtons[buttonIdx].setSelected(true);
        editModeJToggleButtons[buttonIdx].setSelected(true);
    }

    /**
     * Closes and disposes of this frame <b>if</b> the user approves saving dirty
     * query (if necessary).
     */
    private void exitQueryEditor() {
        try {
            boolean isUserApproved = saveQueryIfModified();
            if (isUserApproved) {
                setVisible(false);
                dispose();   // needed to invoke setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            }
        } catch (Exception exc) {
            showError(this, "error saving query", exc);
        }
    }

    /**
     * Reverses the direction of the selected QGEdge.
     */
    private void flipEdgeDirection() {
        QGEdge qgEdge = (QGEdge) getSelectedAbsQueryChild();
        QGVertex oldQGVertex1 = qgEdge.vertex1();
        QGVertex oldQGVertex2 = qgEdge.vertex2();
        qgEdge.setVertex1(oldQGVertex2);
        qgEdge.setVertex1Name(oldQGVertex2.firstName());
        qgEdge.setVertex2(oldQGVertex1);
        qgEdge.setVertex2Name(oldQGVertex1.firstName());

        PPath pEdge = (PPath) queryCanvas.getPNode(qgEdge); // todo xx dangerous
        queryCanvas.connectPEdge(qgEdge);
        queryCanvas.updateQGEdgeArrowHead(pEdge, qgEdge);

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
    }

    /**
     * @param imageFileName name (not path) of image file, located in the
     *                      kdl.prox.qged.images package
     * @return an Icon for imageFileName
     */
    private Icon getIcon(String imageFileName) {
        Class rezClass = QueryCanvas.class;
        URL imageURL = rezClass.getResource("images/" + imageFileName);
        Assert.notNull(imageURL, "image not found: " + "images/" + imageFileName +
                ", relative to: " + rezClass);

        Image image = Toolkit.getDefaultToolkit().getImage(imageURL);
        return new ImageIcon(image);
    }

    /**
     * @return a unique QGItem name in my query
     */
    private String getNextUniqueQGItemName(boolean isVertex) {
        for (int itemNum = 1; ; itemNum++) {
            String itemName = (isVertex ? "Vertex" : "Edge") + "" + itemNum;
            if (query.qgItemForName(itemName) == null) {
                return itemName;
            }
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 500);
    }

    public Query getQuery() {
        return query;
    }

    public QueryCanvas getQueryCanvas() {
        return queryCanvas;
    }

    /**
     * @return the currently-selected object in my queryJTable. returns null if
     *         no selection
     */
    private Object getQueryJTableSelectedObject() {
        int rowIndex = queryJTable.getSelectedRow();        // assumes no column reordering, and single selection model
        int colIndex = queryJTable.getSelectedColumn();     // ""
        try {
            return queryJTable.getModel().getValueAt(rowIndex, colIndex);
        } catch (Exception e) { // work-around of initial error: java.lang.ArrayIndexOutOfBoundsException: -4
            return null;
        }
    }

    /**
     * @return currently selected AbsQueryChild. returns null if none
     */
    private AbsQueryChild getSelectedAbsQueryChild() {
        PNode selectedPNode = selectModeEvtHandler.getSelection();
        if (selectedPNode == null) {
            return null;
        } else {
            return QueryCanvas.getAbsQueryChild(selectedPNode);
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Sets locations of nodes based on editorDataEle.
     *
     * @param editorDataEle &lt;editor-data&gt; Element as returned from
     * @return isPerfectLayout - true if a) every entry found in editorDataEle
     *         was valid, and b) every QGVertex in query was found. returns false o/w
     */
    private boolean layoutQuery(Element editorDataEle) {
        boolean isSawBadVert = false;
        ArrayList qgVertsFoundInEle = new ArrayList();

        List children = editorDataEle.getChildren("vertex-location");
        for (Iterator childIter = children.iterator(); childIter.hasNext();) {
            Element vertLocEle = (Element) childIter.next();
            String vertName = vertLocEle.getAttributeValue("name");
            String vertX = vertLocEle.getAttributeValue("x");
            String vertY = vertLocEle.getAttributeValue("y");
            QGItem qgItem = query.qgItemForName(vertName);
            double newX = Double.parseDouble(vertX);
            double newY = Double.parseDouble(vertY);
            if (!(qgItem instanceof QGVertex)) {
                isSawBadVert = true;
                continue;
            }

            QGVertex qgVertex = (QGVertex) qgItem;
            qgVertsFoundInEle.add(qgVertex);
            PNode vertexPNode = queryCanvas.getPNode(qgVertex);
            vertexPNode.setGlobalTranslation(new Point2D.Double(newX, newY));
        }

        // now compare qgVertsFoundInEle to all vertices in my query to see if
        // any were skipped
        List allQGVerts = query.vertices(true);
        boolean isFoundAll = ((qgVertsFoundInEle.size() == allQGVerts.size()) &&
                qgVertsFoundInEle.containsAll(allQGVerts));
        return isFoundAll && !isSawBadVert;
    }

    /**
     * @param file
     * @return isPerfectLayout (as returned by layoutQuery())
     * @throws Exception
     */
    private boolean loadQuery(File file) throws Exception {
        this.queryFile = file;
        return loadQuery(QueryXMLUtil.graphQueryEleFromFile(file));
    }

    private boolean loadQuery(Element graphQueryEle) throws Exception {
        query = QueryXMLUtil.graphQueryEleToQuery(graphQueryEle);
        Element editorDataEle = QueryXMLUtil.getEditorDataElement(graphQueryEle);
        boolean isPerfectLayout = false;

        // catch disconnected edges, clean them up, and tell user
        Set disconnectedEdges = query.getDisconnectedEdges();
        if (disconnectedEdges.size() != 0) {
            StringBuffer messageSB = new StringBuffer();
            for (Iterator qgEdgeIter = disconnectedEdges.iterator(); qgEdgeIter.hasNext();) {
                QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
                removeQGEdge(qgEdge, new ArrayList());
                messageSB.append(qgEdge.firstName());
                messageSB.append(" (");
                messageSB.append(qgEdge.vertex1Name());
                messageSB.append("->");
                messageSB.append(qgEdge.vertex2Name());
                messageSB.append(")\n");
            }
            JOptionPane.showMessageDialog(this, messageSB.toString(),
                    "Found " + disconnectedEdges.size() + " Disconnected Edge(s)",
                    JOptionPane.WARNING_MESSAGE);
        }

        queryCanvas.loadQuery(query);
        if (editorDataEle != null) {
            isPerfectLayout = layoutQuery(editorDataEle);
            if (!isPerfectLayout) {
                log.warn("found editor data in query file, but some vertices " +
                        "were either skipped or not found");
            }
        }
        updateQueryJTable(query);
        clearSelectionTable();
        isDirty = false;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
        return isPerfectLayout;
    }

    private void makeActions() {
        final QGraphEditorJFrame thisQGEdJFrame = this;

        // addQGAddLinkAction
        addAddLinkAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                addNewQGAddLink();
            }
        };
        addAddLinkAction.putValue(Action.SHORT_DESCRIPTION, "Add a new add-link");

        // addConstraintAction
        addConstraintAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                addNewQGConstraint();
            }
        };
        addConstraintAction.putValue(Action.SHORT_DESCRIPTION, "Add new constraint");

        // closeFileAction
        closeFileAction = new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                try {
                    closeQuery();
                } catch (Exception exc) {
                    showError(thisQGEdJFrame, "error opening query", exc);
                }
            }
        };
        closeFileAction.putValue(Action.SHORT_DESCRIPTION, "Close current query");
        closeFileAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control W"));

        // deleteAQChildAction
        deleteAQChildAction = new AbstractAction("Delete Selected Object") {
            public void actionPerformed(ActionEvent e) {
                deleteAQChild();
            }
        };
        deleteAQChildAction.putValue(Action.SHORT_DESCRIPTION, "Delete " +
                "selected vertex, edge, or subquery");
        deleteAQChildAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("DELETE"));

        // edgeModeAction
        edgeModeAction = new AbstractAction("Edge Creation Mode") {
            public void actionPerformed(ActionEvent e) {
                enterMode(edgeModeEvtHandler);
            }
        };
        edgeModeAction.putValue(Action.SHORT_DESCRIPTION, "Enter edge " +
                "creation mode (drag from V1 to V2)");
        edgeModeAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control 3"));
        edgeModeAction.putValue(Action.SMALL_ICON, getIcon("Edge.gif"));

        // exitEditorAction
        exitEditorAction = new AbstractAction("Exit Query Editor") {
            public void actionPerformed(ActionEvent e) {
                exitQueryEditor();
            }
        };
        exitEditorAction.putValue(Action.SHORT_DESCRIPTION, "Exit editor by " +
                "closing the editor window");

        // flipEdgeAction
        flipEdgeAction = new AbstractAction("Flip Edge Direction") {
            public void actionPerformed(ActionEvent e) {
                flipEdgeDirection();
            }
        };
        flipEdgeAction.putValue(Action.SHORT_DESCRIPTION, "Reverses selected " +
                "edge's direction");
        flipEdgeAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control F"));

        // newFileAction
        newFileAction = new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) {
                try {
                    newQuery();
                } catch (Exception exc) {
                    showError(thisQGEdJFrame, "error creating query", exc);
                }
            }
        };
        newFileAction.putValue(Action.SHORT_DESCRIPTION, "Create new file");
        newFileAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control N"));

        // openFileAction
        openFileAction = new AbstractAction("Open...") {
            public void actionPerformed(ActionEvent e) {
                try {
                    openQuery();
                } catch (Exception exc) {
                    showError(thisQGEdJFrame, "error opening query", exc);
                }
            }
        };
        openFileAction.putValue(Action.SHORT_DESCRIPTION, "Open existing " +
                "query file");
        openFileAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control O"));

        // removeQGAddLinkAction
        removeAddLinkAction = new AbstractAction("Remove") {
            public void actionPerformed(ActionEvent e) {
                removeQGAddLink();
            }
        };
        removeAddLinkAction.putValue(Action.SHORT_DESCRIPTION, "Remove " +
                "selected add-link");

        // removeConstraintAction
        removeConstraintAction = new AbstractAction("Remove") {
            public void actionPerformed(ActionEvent e) {
                removeQGConstraint();
            }
        };
        removeConstraintAction.putValue(Action.SHORT_DESCRIPTION, "Remove " +
                "selected constraint");

        // runQueryAction
        runQueryAction = new AbstractAction("Run...") {
            public void actionPerformed(ActionEvent e) {
                runQuery();
            }
        };
        runQueryAction.putValue(Action.SHORT_DESCRIPTION, "Run current " +
                "query, prompting for output container name");
        runQueryAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control R"));

        // saveFileAction
        saveFileAction = new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveQuery(false);
                } catch (Exception exc) {
                    showError(thisQGEdJFrame, "error saving query", exc);
                }
            }
        };
        saveFileAction.putValue(Action.SHORT_DESCRIPTION, "Save query to " +
                "current file");
        saveFileAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control S"));

        // saveFileAsAction
        saveFileAsAction = new AbstractAction("Save As...") {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveQuery(true);
                } catch (Exception exc) {
                    showError(thisQGEdJFrame, "error saving query", exc);
                }
            }
        };
        saveFileAsAction.putValue(Action.SHORT_DESCRIPTION, "Save query to " +
                "selected file");

        // selectModeAction
        selectModeAction = new AbstractAction("Select Mode") {
            public void actionPerformed(ActionEvent e) {
                enterMode(selectModeEvtHandler);
            }
        };
        selectModeAction.putValue(Action.SHORT_DESCRIPTION, "Enter selection mode");
        selectModeAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control 1"));
        selectModeAction.putValue(Action.SMALL_ICON, getIcon("Arrow.gif"));

        // selectNextItemAction
        selectNextItemAction = new AbstractAction("Select Next Item") {
            public void actionPerformed(ActionEvent e) {
                selectNextItem(true);
            }
        };
        selectNextItemAction.putValue(Action.SHORT_DESCRIPTION,
                "Select next vertex, edge, or subquery");
        selectNextItemAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control RIGHT"));

        // selectPrevItemAction
        selectPrevItemAction = new AbstractAction("Select Previous Item") {
            public void actionPerformed(ActionEvent e) {
                selectNextItem(false);
            }
        };
        selectPrevItemAction.putValue(Action.SHORT_DESCRIPTION,
                "Select previous vertex, edge, or subquery");
        selectPrevItemAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control LEFT"));

        // subqModeAction
        subqModeAction = new AbstractAction("Subquery Creation Mode") {
            public void actionPerformed(ActionEvent e) {
                enterMode(subqModeEvtHandler);
            }
        };
        subqModeAction.putValue(Action.SHORT_DESCRIPTION, "Enter subquery " +
                "creation mode (drag marquee around vertices)");
        subqModeAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control 4"));
        subqModeAction.putValue(Action.SMALL_ICON, getIcon("Subquery.gif"));

        // vertexModeAction
        vertexModeAction = new AbstractAction("Vertex Creation Mode") {
            public void actionPerformed(ActionEvent e) {
                enterMode(vertexModeEvtHandler);
            }
        };
        vertexModeAction.putValue(Action.SHORT_DESCRIPTION, "Enter vertex " +
                "creation mode (click Inside background or subquery)");
        vertexModeAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control 2"));
        vertexModeAction.putValue(Action.SMALL_ICON, getIcon("Vertex.gif"));

        // zoom100PctAction
        zoom100PctAction = new AbstractAction("Reset Zoom") {
            public void actionPerformed(ActionEvent e) {
                queryCanvas.zoom100Pct();
            }
        };
        zoom100PctAction.putValue(Action.SHORT_DESCRIPTION, "Reset the " +
                "view's pan and zoom");
        zoom100PctAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control BACK_SPACE"));

        // zoomInAction
        zoomInAction = new AbstractAction("Zoom In") {
            public void actionPerformed(ActionEvent e) {
                queryCanvas.zoomIn();
            }
        };
        zoomInAction.putValue(Action.SHORT_DESCRIPTION, "Zoom in");
        zoomInAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control EQUALS"));

        // zoomOutAction
        zoomOutAction = new AbstractAction("Zoom Out") {
            public void actionPerformed(ActionEvent e) {
                queryCanvas.zoomOut();
            }
        };
        zoomOutAction.putValue(Action.SHORT_DESCRIPTION, "Zoom out");
        zoomOutAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control MINUS"));

        // zoomToFitAction
        zoomToFitAction = new AbstractAction("Zoom To Fit") {
            public void actionPerformed(ActionEvent e) {
                queryCanvas.zoomToFit();
            }
        };
        zoomToFitAction.putValue(Action.SHORT_DESCRIPTION, "Zoom to fit the " +
                "current query");
        zoomToFitAction.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke("control 0"));
    }

    private JPanel makeAddLinkButtonJPanel() {
        JPanel constButtonJPanel = new JPanel();
        constButtonJPanel.setLayout(new BoxLayout(constButtonJPanel, BoxLayout.X_AXIS));
        constButtonJPanel.add(Box.createGlue());
        constButtonJPanel.add(new JLabel("Add-links:"));
        constButtonJPanel.add(Box.createHorizontalStrut(5));
        constButtonJPanel.add(new JButton(addAddLinkAction));
        constButtonJPanel.add(new JButton(removeAddLinkAction));
        constButtonJPanel.add(Box.createGlue());
        return constButtonJPanel;
    }

    private JPanel makeConstButtonJPanel() {
        JPanel constButtonJPanel = new JPanel();
        constButtonJPanel.setLayout(new BoxLayout(constButtonJPanel, BoxLayout.X_AXIS));
        constButtonJPanel.add(Box.createGlue());
        constButtonJPanel.add(new JLabel("Constraints:"));
        constButtonJPanel.add(Box.createHorizontalStrut(5));
        constButtonJPanel.add(new JButton(addConstraintAction));
        constButtonJPanel.add(new JButton(removeConstraintAction));
        constButtonJPanel.add(Box.createGlue());
        return constButtonJPanel;
    }

    private JMenu makeEditJMenu() {
        JMenu jMenu = new JMenu("Edit");

        editModeJRadioButtons = new JRadioButtonMenuItem[]{
                new JRadioButtonMenuItem(selectModeAction),
                new JRadioButtonMenuItem(vertexModeAction),
                new JRadioButtonMenuItem(edgeModeAction),
                new JRadioButtonMenuItem(subqModeAction),
        };

        // clear icon (doesn't look good in menu)
        for (int buttonIdx = 0; buttonIdx < editModeJRadioButtons.length; buttonIdx++) {
            JRadioButtonMenuItem menuItem = editModeJRadioButtons[buttonIdx];
            menuItem.setIcon(null);
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(editModeJRadioButtons[0]);
        buttonGroup.add(editModeJRadioButtons[1]);
        buttonGroup.add(editModeJRadioButtons[2]);
        buttonGroup.add(editModeJRadioButtons[3]);

        jMenu.add(editModeJRadioButtons[0]);
        jMenu.add(editModeJRadioButtons[1]);
        jMenu.add(editModeJRadioButtons[2]);
        jMenu.add(editModeJRadioButtons[3]);

        jMenu.add(new JSeparator());    // ----
        jMenu.add(deleteAQChildAction);
        jMenu.add(flipEdgeAction);
        jMenu.add(selectNextItemAction);
        jMenu.add(selectPrevItemAction);
        jMenu.add(new JSeparator());    // ----
        annotEdgesJCheckBoxMI = new JCheckBoxMenuItem("Add [1..] To New Edges");
        jMenu.add(annotEdgesJCheckBoxMI);

        return jMenu;
    }

    private JMenu makeFileJMenu() {
        JMenu jMenu = new JMenu("File");
        // standard order: new open close save save-as
        jMenu.add(newFileAction);
        jMenu.add(openFileAction);
        jMenu.add(closeFileAction);
        jMenu.add(saveFileAction);
        jMenu.add(saveFileAsAction);
        jMenu.add(new JSeparator());    // ----
        jMenu.add(runQueryAction);
        jMenu.add(new JSeparator());    // ----
        jMenu.add(exitEditorAction);
        return jMenu;
    }

    private JPanel makeInfoJPanel() {
        JPanel infoJPanel = new JPanel();
        infoJPanel.setLayout(new BorderLayout());
        infoJPanel.add(makePropsJPanel(), BorderLayout.CENTER);
        validationJComboBox = new JComboBox() {
            // disable selections so that users always see the first item, which
            // lists the number of errors
            public void setSelectedIndex(int anIndex) {
                // disabled
            }

            public void setSelectedItem(Object anObject) {
                // disabled
            }
        };
        infoJPanel.add(validationJComboBox, BorderLayout.SOUTH);
        return infoJPanel;
    }

    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(makeFileJMenu());
        jMenuBar.add(makeEditJMenu());
        jMenuBar.add(makeViewJMenu());
        return jMenuBar;
    }

    private JToolBar makeJToolBar() {
        JToolBar jToolBar = new JToolBar(JToolBar.VERTICAL);

        editModeJToggleButtons = new JToggleButton[]{
                new JToggleButton(selectModeAction),
                new JToggleButton(vertexModeAction),
                new JToggleButton(edgeModeAction),
                new JToggleButton(subqModeAction),
        };

        // clear text (doesn't look good in toolbar)
        for (int buttonIDx = 0; buttonIDx < editModeJToggleButtons.length; buttonIDx++) {
            JToggleButton jToggleButton = editModeJToggleButtons[buttonIDx];
            jToggleButton.setText(null);
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(editModeJToggleButtons[0]);
        buttonGroup.add(editModeJToggleButtons[1]);
        buttonGroup.add(editModeJToggleButtons[2]);
        buttonGroup.add(editModeJToggleButtons[3]);

        jToolBar.add(editModeJToggleButtons[0]);    // todo xx use setAction() to disable text
        jToolBar.add(editModeJToggleButtons[1]);
        jToolBar.add(editModeJToggleButtons[2]);
        jToolBar.add(editModeJToggleButtons[3]);

        return jToolBar;
    }

    private void makeModeEventHandlers() {
        edgeModeEvtHandler = new CreateEdgeModeEvtHandler(this);
        subqModeEvtHandler = new CreateSubqModeEvtHandler(this);
        vertexModeEvtHandler = new CreateVertexModeEvtHandler(this);

        // set pSelectEvtHandler
        final QGraphEditorJFrame thisQGraphEditorJFrame = this;
        selectModeEvtHandler = new SelectModeEvtHandler();
        selectModeEvtHandler.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                String propName = evt.getPropertyName();
                if (QGraphEditorJFrame.SELECT_PROPERTY.equals(propName)) {
                    // the selection has changed, so update selectionJTable
                    AbsQueryChild selectedAQChild = getSelectedAbsQueryChild();
                    if (selectedAQChild == null) {   // no selection, or multiple selection
                        clearSelectionTable();
                    } else {                        // single selection
                        // set selectionJTable's mode based on aqChild
                        TableModel tableModel = QueryCanvas.getTableModel(selectedAQChild);
                        selectionJTable.setModel(tableModel);
                        tableModel.addTableModelListener(thisQGraphEditorJFrame);
                    }
                    updateActions();
                } else if (QGraphEditorJFrame.EDIT_PROPERTY.equals(propName)) {
                    // a PNode was edited, so edit the first cell in the selectionJTable
                    AbsQueryChild selectedQChild = getSelectedAbsQueryChild();
                    if (selectedQChild != null) {   // single selection
                        selectionJTable.editCellAt(1, 1);
                        ((JTextField) selectionJTable.getEditorComponent()).selectAll();
                        selectionJTable.getEditorComponent().requestFocus();    // otherwise no insertion point (even though 'editing')
                    }
                } else {
                    throw new IllegalArgumentException("unknown property: " + propName);
                }
            }

        });
    }

    private JPanel makePropsJPanel() {
        JPanel propsJPanel = new JPanel();
        propsJPanel.setLayout(new GridLayout(1, 2));
        propsJPanel.add(makeQueryPropsJPanel(), 0);
        propsJPanel.add(new JScrollPane(selectionJTable), 1);
        propsJPanel.setPreferredSize(new Dimension(0, 150));
        return propsJPanel;
    }

    private JScrollPane makeQueryCanvas() {
        queryCanvas = new QueryCanvas();
        queryCanvas.removeInputEventListener(queryCanvas.getPanEventHandler()); // todo xx don't remove, but need filter that doesn't pan when dragging in select mode
        return new PScrollPane(queryCanvas);
    }

    private JPanel makeQueryPropsJPanel() {
        JPanel queryPropsJPanel = new JPanel();
        queryPropsJPanel.setLayout(new BoxLayout(queryPropsJPanel, BoxLayout.Y_AXIS));
        queryPropsJPanel.add(new JScrollPane(queryJTable));
        queryPropsJPanel.add(makeConstButtonJPanel());
        queryPropsJPanel.add(makeAddLinkButtonJPanel());
        return queryPropsJPanel;
    }

    private JMenu makeViewJMenu() {
        JMenu jMenu = new JMenu("View");
        jMenu.add(zoomToFitAction);
        jMenu.add(zoomOutAction);
        jMenu.add(zoomInAction);
        jMenu.add(zoom100PctAction);
        return jMenu;
    }

    /**
     * Edits a new query, resetting my query and queryFile IVs. Honors
     * isDirty by asking first if user wants to save (if necessary).
     */
    private void newQuery() throws Exception {
        if (!saveQueryIfModified()) {
            return;
        }

        query = new Query("new query", "no description");
        queryFile = null;
        queryCanvas.clear();
        queryCanvas.addAbstractQuery(query);
        updateQueryJTable(query);
        clearSelectionTable();
        isDirty = false;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
        clearCanvasSelection();
    }

    /**
     * Asks the user for a query file then opens it. Honors isDirty by asking
     * first if user wants to save (if necessary).
     */
    private void openQuery() throws Exception {
        if (!saveQueryIfModified()) {
            return;
        }

        try {
            File file = GUI2App.getFileFromUser(this, false);
            if (file != null) {
                boolean isPerfectLayout = loadQuery(file);
                if (!isPerfectLayout) {
                    doRandomLayout();
                }
                updateActions();
            }
        } catch (Exception exc) {
            log.error("error loading query", exc);
        }
    }

    /**
     * @param qgVertices
     * @return Set of QGEdges connected to QGVertex instances in qgVertices
     */
    private Set qgEdgesFromQGVerticies(Collection qgVertices) {
        HashSet qgEdges = new HashSet();
        for (Iterator qgVertIter = qgVertices.iterator(); qgVertIter.hasNext();) {
            QGVertex qgVertex = (QGVertex) qgVertIter.next();
            for (Iterator qgEdgeIter = qgVertex.edges().iterator(); qgEdgeIter.hasNext();) {
                qgEdges.add(qgEdgeIter.next());
            }
        }
        return qgEdges;
    }

    private void removeQGAddLink() {
        Object selectedObj = getQueryJTableSelectedObject();
        QGAddLink qgAddLink = (QGAddLink) selectedObj;
        Assert.notNull(qgAddLink, "QGAddLink not null");  // shouldn't be possible due to updateActions()

        query.removeAddLink(qgAddLink);
        queryJTable.tableChanged(new TableModelEvent(queryJTable.getModel()));
        updateValidationJComboBox();
    }

    private void removeQGConstraint() {
        Object selectedObj = getQueryJTableSelectedObject();
        QGConstraint qgConstraint = (QGConstraint) selectedObj;
        Assert.notNull(qgConstraint, "qgConstraint not null");  // shouldn't be possible due to updateActions()

        query.removeConstraint(qgConstraint);
        queryJTable.tableChanged(new TableModelEvent(queryJTable.getModel()));
        updateValidationJComboBox();
    }

    /**
     * Remove qgEdge from its parent, and from all QGVertex instances it is
     * connected to.
     *
     * @param qgEdge
     * @param removedAQChildren removed AbsQueryChild instances are added to this
     */
    private void removeQGEdge(QGEdge qgEdge, List removedAQChildren) {
        qgEdge.parentAQuery().removeEdge(qgEdge);
        removedAQChildren.add(qgEdge);

        // remove the edge from each of the vertices it is connected to, each
        // of which might be null (if query had disconnected edges when loaded)
        QGVertex vertex1 = qgEdge.vertex1();
        QGVertex vertex2 = qgEdge.vertex2();
        if (vertex1 != null) {
            vertex1.removeEdge(qgEdge);
        }
        if ((vertex2 != null) && !vertex2.equals(vertex1)) {
            vertex2.removeEdge(qgEdge);
        }
    }

    /**
     * Remove the vertex and all edges to/from it.
     *
     * @param qgVertex
     * @param removedAQChildren removed AbsQueryChild instances are added to this
     */
    private void removeQGVertex(QGVertex qgVertex, List removedAQChildren) {
        qgVertex.parentAQuery().removeVertex(qgVertex);
        removedAQChildren.add(qgVertex);

        Set edges = new HashSet(qgVertex.edges());  // copy to avoid java.util.ConcurrentModificationException in following next()
        for (Iterator qgEdgeIter = edges.iterator(); qgEdgeIter.hasNext();) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            removeQGEdge(qgEdge, removedAQChildren);
        }
    }

    /**
     * Runs the current Query, prompting for the output container. Assumes
     * proxDB not null.
     */
    private void runQuery() {
        try {
            final String containerName = GUI2App.getContainerFromUser();
            if (containerName == null) {
                return;
            }

            Spin.setDefaultOffEvaluator(new ListenerSpinOver());  // automatically spin-over all listeners
            Element graphQueryEle = QueryXMLUtil.queryToGraphQueryEle(query);
            RunFileBean runScriptBean = new RunQueryImpl(graphQueryEle,
                    null, containerName);
            RunFileBean runScriptBeanOff = (RunFileBean) Spin.off(runScriptBean);
            new RunFileJFrame(runScriptBeanOff);    // shows itself
        } catch (Exception exc) {
            log.error("error processing query", exc);
            //todo xx add meaningful error handling code
        }
    }

    /**
     * Saves my query to either my queryFile (if isPromptyFirst is false), or to
     * a prompted-for File (if isPromptyFirst is true, or if my queryFile is null).
     *
     * @param isPromptFirst true if should ask user for new queryFile. false if
     *                      should use current queryFile
     */
    private void saveQuery(boolean isPromptFirst) throws Exception {
        // set queryFile according to isPromptFirst and whether current
        // queryFile is null
        if ((queryFile != null) && !isPromptFirst) {
            // use current queryFile
        } else {
            // prompt for new queryFile
            File file = GUI2App.getFileFromUser(this, true);
            if (file == null) {
                return;
            } else {            // user chose file
                queryFile = file;
            }
        }

        saveQueryInternal(queryFile);
    }

    /**
     * If my query is dirty then asks user if wants to save. Saves if yes.
     *
     * @return isUserApproved, which is false if the user canceled, which means
     *         there was no save, and the caller should not continue. returns
     *         true if saving was taken care of and caller should continue
     */
    private boolean saveQueryIfModified() throws Exception {
        if (isDirty) {
            int result = JOptionPane.showConfirmDialog(this, "Query is " +
                    "modified; save first?", "Confirm",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (result == JOptionPane.YES_OPTION) {
                File file = queryFile;
                if (file == null) {     // query hasn't been saved yet
                    file = GUI2App.getFileFromUser(this, true, "Save Changes To");
                    if (file == null) { // user canceled save
                        return false;
                    }
                }
                saveQueryInternal(file);
                return true;
            } else {    // result == JOptionPane.NO_OPTION
                // discard changes
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Saves my query to file. Does not honor isDirty.
     *
     * @throws Exception
     */
    private void saveQueryInternal(File file) throws Exception {
        Element graphQueryEle = QueryXMLUtil.queryToGraphQueryEle(query);
        addEditorDataEle(graphQueryEle);
        QueryXMLUtil.graphQueryEleToFile(graphQueryEle, file);
        isDirty = false;
        updateTitle();
        updateActions();
    }

    /**
     * @param jTable
     * @param rowIndex
     * @param colIndex
     */
    private void scrollToCell(JTable jTable, int rowIndex, int colIndex) {
        // Assumes jTable is contained in a JScrollPane. Scrolls the
        // cell (rowIndex, vColIndex) so that it is visible within the viewport.
        if (!(jTable.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) jTable.getParent();

        // This rectangle is relative to the jTable where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = jTable.getCellRect(rowIndex, colIndex, true);

        // The location of the viewport relative to the jTable
        Point pt = viewport.getViewPosition();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0)
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);

        // Scroll the area into view
        viewport.scrollRectToVisible(rect);
    }

    private void selectAQChild(AbsQueryChild absQueryChild) {
        PNode pNode = queryCanvas.getPNode(absQueryChild);
        selectModeEvtHandler.setSelection(pNode);
    }

    /**
     * Selects the next or previous item.
     *
     * @param isNext true if next item should be selected. false for previous
     */
    private void selectNextItem(boolean isNext) {
        List aqChildren = query.getAbsQueryChildren();
        AbsQueryChild selectedAQChild = getSelectedAbsQueryChild();
        if (selectedAQChild == null) {
            selectAQChild((AbsQueryChild) aqChildren.get(0));
        } else {
            int selAQChildIdx = aqChildren.indexOf(selectedAQChild);
            int newSelAQChildIdx = selAQChildIdx + (isNext ? 1 : -1);   // corrected next if necessary
            if (newSelAQChildIdx < 0) {
                newSelAQChildIdx = aqChildren.size() - 1;   // wrap around backward
            } else if (newSelAQChildIdx >= aqChildren.size()) {
                newSelAQChildIdx = 0;       // wrap around forward
            }
            selectAQChild((AbsQueryChild) aqChildren.get(newSelAQChildIdx));
        }
    }

    private static void showError(Component parent, String message, Exception exc) {
//        log.error(message + ": " + exc);
        JOptionPane.showMessageDialog(parent, message + ": " + exc, "Error!",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Handles property table changes.
     *
     * @param e
     */
    public void tableChanged(TableModelEvent e) {
        Object source = e.getSource();
        if (!(source instanceof QGObjTableModel)) {
            log.error("tableChanged(): unknown TableModel: " + source);
            return;
        }

        QGObjTableModel qgObjTableModel = (QGObjTableModel) source;
        Object qgObject = qgObjTableModel.getQGObject();

        if (qgObject instanceof AbsQueryChild) {
            // update corresponding LabelGroup (needed for all AbsQueryChild types)
            PNode pNode = queryCanvas.getPNode(qgObject);
            LabelGroup labelGroup = QueryCanvas.getLabelGroup(pNode);
            labelGroup.updateContent();
            queryCanvas.updateLabelGroup(pNode);

            // update arrow according to direction, if necessary
            int row = e.getFirstRow();
            int column = e.getColumn();
            Object value = qgObjTableModel.getValueAt(row, column);
            if ((qgObject instanceof QGEdge) && (value instanceof Boolean)) {
                QGEdge qgEdge = (QGEdge) qgObject;
                queryCanvas.updateQGEdgeArrowHead((PPath) pNode, qgEdge);   // todo dangerous
            }

        } else if (qgObject instanceof Query) {
            // update QGConstraint if necessary
            int row = e.getFirstRow();
            int column = e.getColumn();
            Object value = qgObjTableModel.getValueAt(row, column);
            if (value instanceof QGConstraint) {
                updateQGConstraint((QGConstraint) value);
            }
        }

        isDirty = true;
        updateTitle();
        updateActions();
        updateValidationJComboBox();
    }

    private void updateActions() {
        Object queryJTSelObj = getQueryJTableSelectedObject();
        AbsQueryChild selAQChild = getSelectedAbsQueryChild();

        addAddLinkAction.setEnabled(query != null);
        addConstraintAction.setEnabled(query != null);
        closeFileAction.setEnabled(query != null);
        deleteAQChildAction.setEnabled(selAQChild != null);
//        edgeModeAction.setEnabled(true);  // always enabled
//        exitEditorAction.setEnabled(true);  // always enabled
        flipEdgeAction.setEnabled(selAQChild instanceof QGEdge);
//        newFileAction.setEnabled(true);  // always enabled
//        openFileAction.setEnabled(true);  // always enabled
        removeAddLinkAction.setEnabled(queryJTSelObj instanceof QGAddLink);
        removeConstraintAction.setEnabled(queryJTSelObj instanceof QGConstraint);
        runQueryAction.setEnabled((query != null) && (DB.isOpen()));
        saveFileAction.setEnabled(query != null);
        saveFileAsAction.setEnabled(query != null);
//        selectModeAction.setEnabled(true);  // always enabled
        selectNextItemAction.setEnabled(query != null);
        selectPrevItemAction.setEnabled(query != null);
//        subqModeAction.setEnabled(true);  // always enabled
//        vertexModeAction.setEnabled(true);  // always enabled
        zoom100PctAction.setEnabled(query != null);
        zoomInAction.setEnabled(query != null);
        zoomOutAction.setEnabled(query != null);
        zoomToFitAction.setEnabled(query != null);
    }

    /**
     * Updates qgConstraint's annotation, isEdge, and parentAQuery using my query.
     * NB: The logic comes from QueryGraph2CompOp.extractContraints().
     *
     * @param qgConstraint
     */
    private void updateQGConstraint(QGConstraint qgConstraint) {
        QGItem item1 = query.qgItemForName(qgConstraint.item1Name());  // null if not found
        QGItem item2 = query.qgItemForName(qgConstraint.item2Name());  // ""
        qgConstraint.setParentAQuery(query);
        if (item1 == null || item2 == null) {
            // missing or unknown item. just continue; QueryValidator,
            // if called, will detect error and complain
        } else {
            qgConstraint.setIsEdgeConstraint((item1 instanceof QGEdge));
            if (item1.isAnnotated()) {
                qgConstraint.setAnnotation(item1.annotation());
            } else if (item2.isAnnotated()) {
                qgConstraint.setAnnotation(item2.annotation());
            } else {
                qgConstraint.setAnnotation(null);
            }
        }
    }

    /**
     * Sets my queryJTable's model to a new one for query.
     *
     * @param query Query to update my queryJTable for. pass null to clear
     */
    private void updateQueryJTable(Query query) {
        if (query == null) {
            queryJTable.setModel(new DefaultTableModel(0, 0));
        } else {
            TableModel tableModel = QueryCanvas.getTableModel(query);
            queryJTable.setModel(tableModel);
            tableModel.addTableModelListener(this);
        }
    }

    private void updateTitle() {
        String fileName = (queryFile == null ? "<no file>" : queryFile.getName());
        String queryName = (query == null ? "<no query>" : query.getName());
        String dirtyFlag = (this.isDirty ? "*" : "");
        String parent = (queryFile == null ? "" : "(" + queryFile.getParent() + ")");
        setTitle(dirtyFlag + queryName + " - " + fileName + "  " + parent +
                (DB.isOpen() ? " - " + DB.description() : ""));
    }

    /**
     * Updates my validationJComboBox based on my query and a validation
     * analysis.
     */
    private void updateValidationJComboBox() {
        ComboBoxModel comboBoxModel;
        if (query == null) {
            comboBoxModel = new DefaultComboBoxModel();     // clear it
        } else if (query.isEmpty()) {
            comboBoxModel = new DefaultComboBoxModel(new String[]{"Query Is Empty"});
        } else {
            try {
                new QueryValidator(query);      // throws Exception
                comboBoxModel = new DefaultComboBoxModel(new String[]{"Query Is Valid"});
            } catch (QGValidationError qgValidationError) {
                List errorList = qgValidationError.getErrorList();
                errorList.add(0, errorList.size() + " Validation Error" +
                        (errorList.size() > 1 ? "s" : ""));
                comboBoxModel = new DefaultComboBoxModel(errorList.toArray());
            }
        }
        validationJComboBox.setModel(comboBoxModel);
    }

}
