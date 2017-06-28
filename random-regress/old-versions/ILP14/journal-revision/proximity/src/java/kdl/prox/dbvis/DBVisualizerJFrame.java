/**
 * $Id: DBVisualizerJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantVertexAspectRatioFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexSizeFunction;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.EllipseVertexShapeFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.decorators.VertexStrokeFunction;
import edu.uci.ics.jung.visualization.AbstractLayout;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.SimpleGraphMouse;
import edu.uci.ics.jung.visualization.StaticLayout;
import edu.uci.ics.jung.visualization.VertexLocationFunction;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import kdl.prox.app.GUI2App;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.gui2.BrowserJFrame;
import kdl.prox.gui2.ColorManager;
import kdl.prox.gui2.GUIContentGenerator;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * JFrame that allows browsing a Proximity database, starting with an object OID.
 */
public class DBVisualizerJFrame extends JFrame implements RadiiInfo {

    private static final Logger log = Logger.getLogger(DBVisualizerJFrame.class);
    public static final int LABEL_MAX = 25;            // max length of vertex and edge labels before truncation
    private static final int COLOR_VALUES_MAX = 25;     // max number of top object color histogram values to use
    public static final int VERTEX_DIAMETER = 20;
    private static final Stroke DOTTED_STROKE = new BasicStroke(1.0f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
            new float[]{1.0f, 3.0f}, 0f);               // a stroke for a dotted line: 1 pixel width, round caps, round joins, and an array of {1.0f, 3.0f}
    private static final Stroke SOLID_STROKE = new BasicStroke(1.0f);

    // static so that settings persist across all DBVisualizerJFrame instances:
    private static boolean IS_USE_OID_IF_NO_OBJ_LABEL_ATTR = true;     // used when dbVisGraphMgr's object label attribute is null: true -> show OID; false -> show nothing (no label)
    private static boolean IS_USE_OID_IF_NO_LINK_LABEL_ATTR;    // "" for links


    // colors and legends
    private JFrame legendJFrame;
    private ColorManager colorManager = null;

    // static versions of DBVisGraphMgr IVs. used with above static variables to
    // persist UI settings
    private static String LINK_LABEL_ATTR_NAME = null;  // link attribute name for vertex labels. null if none
    private static String OBJ_LABEL_ATTR_NAME = null;   // object ""
    private static String OBJ_COLOR_ATTR_NAME = null;   // object attribute name for vertex colors. null if none

    private DBVisGraphMgr dbVisGraphMgr;
    private VisualizationViewer vv;
    private AbstractLayout layout;

    // IVs corresponding to the most recently-performed layout (see layoutGraph()):
    private Vertex centerVertex = null;
    private Map vertexToPolarCoordMap = null;
    private VertexLocationFunction vertLocFcn = null;

    private JTextField inputJTextField;
    private JLabel messageJLabel;

    private AbstractAction setLinkLabelAction;
    private AbstractAction setObjectColorAction;
    private AbstractAction setObjectLabelAction;
    private AbstractAction showLegendAction;
    private JCheckBoxMenuItem animateJCheckboxMenuItem = new JCheckBoxMenuItem("Animate Layouts", true);


    public DBVisualizerJFrame() {
        super("Database " + DB.description());

        makeActions();
        setJMenuBar(makeJMenuBar());

        dbVisGraphMgr = new DBVisGraphMgr(LINK_LABEL_ATTR_NAME,
                OBJ_LABEL_ATTR_NAME, OBJ_COLOR_ATTR_NAME);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeControlJPanel(), BorderLayout.NORTH);
        getContentPane().add(makeGraphJComponent(), BorderLayout.CENTER);
        getContentPane().add(makeMessageJLabel(), BorderLayout.SOUTH);

        setSize(500, 550);
        updateActions();
        setVisible(true);

        addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
                // ignored
            }

            public void componentMoved(ComponentEvent e) {
                // ignored
            }

            public void componentResized(ComponentEvent e) {
                layoutGraph(true);  // keep current center, don't animate
            }

            public void componentShown(ComponentEvent e) {
                // ignored
            }

        });


        addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                if (legendJFrame != null) {
                    legendJFrame.setVisible(false);
                }
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });
    }

    /**
     * Called when the user selects an attribute to use for object colors,
     * computes colors to use based on the attribute's value histogram. Uses
     * the first value column, and uses only the top COLOR_VALUES_MAX values.
     *
     * @param attrName
     * @param attrs
     */
    private void computeColorsFromAttribute(String attrName, Attributes attrs) {
        // get top values from histogram
        List values = new ArrayList();
        NST attrDataNST = attrs.getAttrDataNST(attrName);
        String colName = attrDataNST.getNSTColumn(1).getName();
        NST histNST = attrDataNST.getColumnHistogramAsNST(colName, true, false, true);// first value column (0 = ID)
        ResultSet resultSet = histNST.selectRows("*", "value", "0-" + COLOR_VALUES_MAX);
        while (resultSet.next()) {
            String value = resultSet.getString(1);
            values.add(value);
        }

        // save as color map
        String[] uniqueNames = (String[]) values.toArray(new String[0]);
        colorManager = new ColorManager(uniqueNames);
    }

    private void browseObject(final ProxSparseVertex vertex, boolean isAttributes) {
        GUI2App gui2App = GUI2App.getOrMakeGUI2App();
        gui2App.makeNewBrowserJFrame("item:/objects/" + vertex.getOID().intValue() +
                (isAttributes ? "!" + GUIContentGenerator.ATTR_VAL_PARAM : ""));
    }

    private void expandVertex(ProxSparseVertex vertex, boolean disableAnimation) {
        ProxSparseVertex expandedVertex = (vertex.isPseudo() ? // because expandVertex() actually expands the paged vertex if given a pager
                dbVisGraphMgr.getPagedVertexFromPager(vertex) : vertex);    // NB: must be done before expandVertex()
        try {
            BrowserJFrame.showWaitCursor(this, true);
            GraphEdit graphEdit = dbVisGraphMgr.expandVertex(vertex, 5);    // NB: disconnects vertex, which breaks getPagedVertexFromPager()
            Integer vertexOID = expandedVertex.getOID();

            if (dbVisGraphMgr.getHistory().size() == 1) {
                centerVertex = expandedVertex;      // only center the first one
            }
            layoutGraph(disableAnimation || graphEdit.isNoChanges());

            if (!graphEdit.isNoChanges()) {
                Graph graph = dbVisGraphMgr.getGraph();
                messageJLabel.setText("expanded object " + vertexOID + " - added " +
                        graphEdit.getAddedVertices().size() + " vertices and " +
                        graphEdit.getAddedEdges().size() + " edges. total: " +
                        graph.getVertices().size() + ", " + graph.getEdges().size());
            } else {
                messageJLabel.setText("expanded object " + vertexOID + " - no vertices or edges added");
            }
            inputJTextField.setText(vertexOID.toString());
        } finally {
            BrowserJFrame.showWaitCursor(this, false);
        }
    }

    /**
     * @param dbColorValue
     * @return Paint corresponding to dbColorValue, which must not be null
     */
    private Color getColorForAttrValue(String dbColorValue) {
        if (colorManager == null) {
            return null;
        } else {
            Color color = colorManager.getColor(dbColorValue);
            if (color == null) {
                return Color.GRAY;
            } else {
                return color;
            }
        }
    }

    public static String getCurrentLinkLabelAttrName() {
        return LINK_LABEL_ATTR_NAME;
    }

    public static String getCurrentObjectLabelAttrName() {
        return OBJ_LABEL_ATTR_NAME;
    }

    /**
     * todo integrate with GUIContentGenerator.objectLabelLength, but it's an instance (not class) var :-(
     *
     * @param vertOrEdge
     * @return String to use as a label for vertex. truncates if exceeds LABEL_MAX
     */
    private String getLabelForVertOrEdge(ProxItemData vertOrEdge) {
        boolean isObject = (vertOrEdge instanceof Vertex);
        String labelAttr = dbVisGraphMgr.getItemLabelAttribute(isObject);
        if (vertOrEdge.isPager()) {
            NumberFormat numberFormat = NumberFormat.getPercentInstance();
            numberFormat.setMinimumFractionDigits(2);

            int numShown = vertOrEdge.getPagerNumShown();
            int numTotal = vertOrEdge.getPagerNumTotal();
            int numHidden = numTotal - numShown;
            double percentHidden = (double) numHidden / numTotal;
            String prettyPercentHidden = numberFormat.format(percentHidden);
            return "[" + numHidden + "/" + numTotal + " (" + prettyPercentHidden + ")]";
        } else if (labelAttr == null) {
            boolean isUseOIDIfNoLabelAttr = (isObject ? DBVisualizerJFrame.IS_USE_OID_IF_NO_OBJ_LABEL_ATTR :
                    DBVisualizerJFrame.IS_USE_OID_IF_NO_LINK_LABEL_ATTR);
            if (isUseOIDIfNoLabelAttr) {
                Integer oidForVert = vertOrEdge.getOID();
                if (oidForVert != null) {
                    return oidForVert.toString() + "@0";
                } else {    // happens (for example) for "pseudo" links between pager vertices and their "paged" vertices
                    return "??";
                }
            } else {
                return "";
            }
        } else {
            String dbLabel = vertOrEdge.getLabel();
            String label = (dbLabel != null ? dbLabel : "??");
            if (label.length() > LABEL_MAX) {
                label = label.substring(0, LABEL_MAX) + "...";
            }
            return label;
        }
    }

    private String getToolTipTextForVertOrEdge(ProxItemData vertOrEdge) {
        String vertOrEdgeLabel = getLabelForVertOrEdge(vertOrEdge);
        Integer oid = vertOrEdge.getOID();
        String oidStr = (oid == null ? "" : " (" + oid.toString() + "@0)");
        return vertOrEdgeLabel + oidStr;
    }

    public Map getVertexToPolarCoordMap() {
        return vertexToPolarCoordMap;
    }

    public VisualizationViewer getVisualizationViewer() {
        return vv;
    }

    /**
     * Called when the user types an object OID (integer) into inputJTextField,
     * parses it, makes it the graph center, and re-lays out the graph.
     *
     * @param oidStr
     */
    public void graphFromStartingOID(String oidStr) {
        if (oidStr.length() == 0) {
            messageJLabel.setText("no input. type an integer object OID");
            return;
        }

        try {
            Integer oidInt = new Integer(oidStr);
            if (!dbVisGraphMgr.isObjectOIDValid(oidInt)) {
                messageJLabel.setText("no object in db for id: " + oidInt);
                return;
            }

            dbVisGraphMgr.clear();
            ProxSparseVertex vertex = dbVisGraphMgr.addVertexForObjID(oidInt);

//            centerVertex = vertex;
//            vertexToPolarCoordMap = null;
//            vertLocFcn = null;

            expandVertex(vertex, true);
        } catch (NumberFormatException e) {
            messageJLabel.setText("couldn't parse input as integer: " + oidStr);
        }
    }

    /**
     * Uses current centerVertex, honoring animateJCheckboxMenuItem.
     *
     * @param disableAnimation if true causes animation flag (animateJCheckboxMenuItem)
     *                         to be ignored
     */
    private void layoutGraph(boolean disableAnimation) {
        final Dimension size = vv.getSize();
        if (centerVertex == null) {
            layout.initialize(size);
        } else {
            // at this point, the graph and/or centerVertex has been modified (due to
            // expand or center actions, respectively), but my vertexToPolarCoordMap
            // and vertLocFcn have not yet been updated
            final VertexLocationFunction oldVertLocFcn = vertLocFcn;
            final Graph graph = layout.getGraph();
            vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(centerVertex,
                    DBVisualizerJFrame.makeVertextComparator());
            vertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                    vertexToPolarCoordMap, size, DBVisualizerJFrame.VERTEX_DIAMETER);
            if (!disableAnimation && animateJCheckboxMenuItem.isSelected()) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        layoutGraphUsingAnimation(graph, size, oldVertLocFcn, vertLocFcn);
                    }
                });
                thread.start();
            } else {
                // don't animate, simply position all vertices in the end state
                layout.initialize(size, vertLocFcn);
            }
        }
        vv.repaint();
    }

    /**
     * Animate the transition from startVertLocFcn to endVertLocFcn, using
     * a LayoutStateTransitionFunction. Runs in the current Thread.
     *
     * @param size
     * @param startVertLocFcn
     * @param endVertLocFcn
     */
    private void layoutGraphUsingAnimation(Graph graph, Dimension size,
                                           VertexLocationFunction startVertLocFcn,
                                           VertexLocationFunction endVertLocFcn) {
        // step 1/2: animate through each intermediate state. note that, because
        // calcIntermediateStates() only uses startVertLocFcn to calculate
        // intermediate steps, vertices in endVertLocFcn do not get moved. those
        // are handled in step 2/2
        int numSteps = 20;
        LinearStateTransFcn linearStateTransFcn = new LinearStateTransFcn();
        List vertLocFcnList = linearStateTransFcn.calcIntermediateStates(graph,
                startVertLocFcn, endVertLocFcn, numSteps);
        for (int vertLocFcnIdx = 0; vertLocFcnIdx < vertLocFcnList.size(); vertLocFcnIdx++) {
            VertexLocationFunction vertLocFcn = (VertexLocationFunction) vertLocFcnList.get(vertLocFcnIdx);
            // NB: following initialize() call throws NPE if the VertexLocationFunction
            // does not contain locations for *all* vertices. in the case of
            // expanding a vertex, the intermediate location functions have
            // locations only for vertices in startVertLocFcn, but the graph has
            // already been updated with the newly-expanded vertices. to work
            // around this, we use a StaticLayout subclass that skips null locations
            // (see initializeLocation() override in this file)

            // do the layout (does all vertices in graph)
            layout.initialize(size, vertLocFcn);
            repaintInEDT();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        // step 2/2: finally, position vertices that are in endVertLocFcn but not
        // in startVertLocFcn, i.e., those skipped by the initializeLocation()
        // override. we do this the lazy way by positioning *all* vertices in
        // endVertLocFcn
        layout.initialize(size, endVertLocFcn);
        repaintInEDT();
    }

    private void makeActions() {
        setLinkLabelAction = new AbstractAction("Set Link Labels...") {
            public void actionPerformed(ActionEvent e) {
                setItemLabels(false);
            }
        };
        setLinkLabelAction.putValue(Action.SHORT_DESCRIPTION,
                "Configure link label content.");

        setObjectColorAction = new AbstractAction("Set Object Colors...") {
            public void actionPerformed(ActionEvent e) {
                setObjectColors();
            }
        };
        setObjectColorAction.putValue(Action.SHORT_DESCRIPTION,
                "Configure object color content.");

        setObjectLabelAction = new AbstractAction("Set Object Labels...") {
            public void actionPerformed(ActionEvent e) {
                setItemLabels(true);
            }
        };
        setObjectLabelAction.putValue(Action.SHORT_DESCRIPTION,
                "Configure object label content.");

        showLegendAction = new AbstractAction("Show Color Legend") {
            public void actionPerformed(ActionEvent e) {
                showLegend(colorManager);
            }
        };
        showLegendAction.putValue(Action.SHORT_DESCRIPTION,
                "Show the object color legend.");
    }

    /**
     * Contents: inputJTextField, layoutJButton
     *
     * @return a JPanel that contains controls for the UI
     */
    private JPanel makeControlJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(1, 1));

        JTextField inputJTextField = makeInputJTextField();
        jPanel.add(inputJTextField, 0);

        return jPanel;
    }

    private VisualizationViewer makeGraphJComponent() {
        final Graph graph = dbVisGraphMgr.getGraph();

        PluggableRenderer pr = new PluggableRenderer();
        pr.setEdgeShapeFunction(new EdgeShape.QuadCurve());     // so that multi-edges don't overlap
        pr.setVertexShapeFunction(new EllipseVertexShapeFunction(new ConstantVertexSizeFunction(VERTEX_DIAMETER),
                new ConstantVertexAspectRatioFunction(1.0f)));

        // set up vetex and edge labels
        pr.setVertexStringer(new VertexStringer() {
            public String getLabel(ArchetypeVertex vertex) {
                Assert.condition(vertex instanceof ProxItemData,
                        "vertex wasn't a ProxItemData: " + vertex);
                return getLabelForVertOrEdge((ProxItemData) vertex);
            }
        });
        pr.setEdgeStringer(new EdgeStringer() {
            public String getLabel(ArchetypeEdge edge) {
                Assert.condition(edge instanceof ProxItemData,
                        "edge wasn't a ProxItemData: " + edge);
                return getLabelForVertOrEdge((ProxItemData) edge);
            }
        });

        // set up vertex paint (colors)
        pr.setVertexPaintFunction(new VertexPaintFunction() {

            public Paint getFillPaint(Vertex vertex) {
                Paint fillPaintForVertex = Color.RED;
                ProxItemData proxItemData = ((ProxItemData) vertex);
                if (proxItemData.isPseudo()) {
                    fillPaintForVertex = Color.GRAY;
                } else {
                    String currColorAttr = dbVisGraphMgr.getObjColorAttribute();    // null if same
                    if (currColorAttr != null) {
                        String dbColorValue = ((ProxItemData) vertex).getColor();
                        if (dbColorValue != null) {
                            fillPaintForVertex = getColorForAttrValue(dbColorValue);
                        }
                    }
                }
                return fillPaintForVertex;
            }

            public Paint getDrawPaint(Vertex v) {
                return Color.BLACK;  //To change body of implemented methods use File | Settings | File Templates.
            }

        });

        layout = new StaticLayout(graph) {

            protected void initializeLocation(Vertex v, Coordinates coord, Dimension d) {
                // handle the case of no location for v by skipping it (see note
                // in layoutGraphUsingAnimation())
                if (vertex_locations.getLocation(v) != null) {
                    super.initializeLocation(v, coord, d);
                }
            }

        };
        vv = new VisualizationViewer(layout, pr);
        vv.getModel().setRelaxerThreadSleepTime(2000);  // magic number helps avoid 100% CPU with more than a few nodes (from https://sourceforge.net/forum/message.php?msg_id=3248935)
        vv.setBackground(Color.white);
        vv.addPreRenderPaintable(DBVisualizerJFrame.makeRadiiPaintable(this));
        vv.setToolTipFunction(new DefaultToolTipFunction() {

            public String getToolTipText(Edge edge) {
                Assert.condition(edge instanceof ProxItemData,
                        "edge wasn't a ProxItemData: " + edge);
                return getToolTipTextForVertOrEdge((ProxItemData) edge);
            }

            public String getToolTipText(Vertex vertex) {
                Assert.condition(vertex instanceof ProxItemData,
                        "vertex wasn't a ProxItemData: " + vertex);
                return getToolTipTextForVertOrEdge((ProxItemData) vertex);
            }

        });

        // set up picking (selection) of nodes and edges
        vv.setPickSupport(new ShapePickSupport());
        vv.setGraphMouse(new SimpleGraphMouse(vv) {

            // NB: must be called via both pressed *and* released to cover JDK
            // differences in defining isPopupTrigger(), esp. JDK 1.5 on Windows
            private void handlePopup(MouseEvent e) {
                PickSupport pickSupport = vv.getPickSupport();
                Vertex vertex = pickSupport.getVertex(e.getX(), e.getY());
                if (!(vertex instanceof ProxSparseVertex)) {
                    return;     // for example when pressing an edge
                }

                if (e.isPopupTrigger()) {
                    showPopupForVertex((ProxSparseVertex) vertex, e);
                }
            }

            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!SwingUtilities.isLeftMouseButton(e) || e.isAltDown() ||
                        e.isControlDown() || e.isMetaDown() || e.isShiftDown()) {   // tests to work around Mac issue - was treating control-click as 'click' *and* popup trigger
                    return;
                }

                PickSupport pickSupport = vv.getPickSupport();
                Vertex vertex = pickSupport.getVertex(e.getX(), e.getY());
                if (!(vertex instanceof ProxSparseVertex)) {
                    return;     // for example when pressing an edge
                }

                ProxSparseVertex proxSparseVertex = (ProxSparseVertex) vertex;
                expandVertex(proxSparseVertex, false);
            }

            // disable dragging because 1) dragging isn't (yet) constrained to
            // radii, and 2) otherwise newly-expanded vertices drag immediately
            // after expansion
            public void mouseDragged(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                handlePopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                handlePopup(e);
            }

        });

        pr.setVertexStrokeFunction(new VertexStrokeFunction() {

            private Stroke heavy = new BasicStroke(5);
            private Stroke medium = new BasicStroke(3);


            public Stroke getStroke(Vertex vertex) {
                ProxItemData proxItemData = ((ProxItemData) vertex);
                PickedState pickedState = vv.getPickedState();
                if (proxItemData.isPseudo()) {
                    return DOTTED_STROKE;
                } else if (pickedState.isPicked(vertex)) {
                    return heavy;
                } else if (dbVisGraphMgr.isVertexOnHistory(vertex)) {
                    return medium;
                } else {
                    return SOLID_STROKE;
                }
            }

        });

        pr.setEdgeStrokeFunction(new EdgeStrokeFunction() {

            public Stroke getStroke(Edge edge) {
                ProxItemData proxItemData = ((ProxItemData) edge);
                if (proxItemData.isPseudo()) {
                    return DOTTED_STROKE;
                } else {
                    return SOLID_STROKE;
                }
            }

        });

        return vv;
    }

    private JMenu makeGraphJMenu() {
        JMenu jMenu = new JMenu("Graph");
        jMenu.add(showLegendAction);
        jMenu.add(new JSeparator());
        jMenu.add(setLinkLabelAction);
        jMenu.add(setObjectColorAction);
        jMenu.add(setObjectLabelAction);
        jMenu.add(new JSeparator());
        jMenu.add(animateJCheckboxMenuItem);
        return jMenu;
    }

    private JTextField makeInputJTextField() {
        inputJTextField = new JTextField();
        inputJTextField.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                graphFromStartingOID(inputJTextField.getText());
            }
        });
        inputJTextField.setToolTipText("Type an object OID as an integer, then hit Enter.");
        return inputJTextField;
    }

    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(makeGraphJMenu());
        return jMenuBar;
    }

    private JComponent makeMessageJLabel() {
        messageJLabel = new JLabel("type object ID to graph; click nodes to expand; right-click nodes for menu");   // NB: without any text here the label is initially squashed to zero height
        messageJLabel.setToolTipText("Shows selected node or edge information.");
        messageJLabel.setBorder(new LineBorder(Color.BLACK));
        return messageJLabel;
    }

    public static VisualizationViewer.Paintable makeRadiiPaintable(final RadiiInfo radiiInfo) {
        return new VisualizationViewer.Paintable() {

            public void paint(Graphics g) {
                // draw radii circles, if possible
                Map vertexToPolarCoordMap = radiiInfo.getVertexToPolarCoordMap();
                VisualizationViewer vv = radiiInfo.getVisualizationViewer();
                if (vertexToPolarCoordMap == null) {
                    return;
                }

                int maxRho = RadialLayoutHelper.getMaxRho(vertexToPolarCoordMap);
                Layout layout = vv.getModel().getGraphLayout();
                Dimension currentSize = layout.getCurrentSize();
                int radiusSize = (int) RadialLayoutHelper.getRadius(currentSize,
                        DBVisualizerJFrame.VERTEX_DIAMETER, vertexToPolarCoordMap);
                int centerX = RadialLayoutHelper.getCenterX(layout.getCurrentSize(),
                        DBVisualizerJFrame.VERTEX_DIAMETER);
                int centerY = RadialLayoutHelper.getCenterY(layout.getCurrentSize(),
                        DBVisualizerJFrame.VERTEX_DIAMETER);
                g.setColor(Color.PINK);
                for (int radius = 1; radius <= maxRho; radius++) {
                    int x = centerX - (radius * radiusSize);
                    int y = centerY - (radius * radiusSize);
                    int diameter = radiusSize * 2 * radius;
                    g.drawOval(x, y, diameter, diameter);
                }
            }

            public boolean useTransform() {
                return false;
            }

        };
    }

    public static Comparator makeVertextComparator() {
        return new Comparator() {  // compares OIDs to get relatively deterministic layouts

            public int compare(Object o1, Object o2) {
                ProxSparseVertex psv1 = ((ProxSparseVertex) o1);
                ProxSparseVertex psv2 = ((ProxSparseVertex) o2);
                Integer v1Datum = psv1.getOID();
                Integer v2Datum = psv2.getOID();
                int result;
                if (v1Datum == null && v2Datum == null) {
                    result = 0;
                } else if (v1Datum == null) {
                    result = -1;
                } else if (v2Datum == null) {
                    result = 1;
                } else {
                    result = v1Datum.compareTo(v2Datum);
                }
                return result;
            }
        };
    }

    private void refreshAttributes(boolean isObject, boolean isLabel) {
        try {
            BrowserJFrame.showWaitCursor(this, true);
            dbVisGraphMgr.refreshItemAttributes(isObject, isLabel);
        } finally {
            BrowserJFrame.showWaitCursor(this, false);
        }
    }

    private void repaintInEDT() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {   // refresh in the EDT

                public void run() {
                    vv.repaint();
                }
            });
        } catch (InterruptedException e1) {
            // ignore
        } catch (InvocationTargetException e1) {
            // ignore
        }
    }

    private void setItemLabelAttribute(String itemLabelAttrName, boolean isObject) {
        dbVisGraphMgr.setItemLabelAttribute(itemLabelAttrName, isObject);
        if (isObject) {
            OBJ_LABEL_ATTR_NAME = itemLabelAttrName;
        } else {
            LINK_LABEL_ATTR_NAME = itemLabelAttrName;
        }
    }

    /**
     * Shows the attribute configuration dialog for labels of objects or links,
     * depending on isObject. Choices include:
     * <ul>
     * <li>&lt;None&gt; (no label)
     * <li>&lt;OID&gt; (OID)
     * <li>&lt;Attribute&gt; (an object or link attribute, chosen from a list)
     * </ul>
     *
     * @param isObject
     */
    private void setItemLabels(boolean isObject) {
        List choices = new ArrayList();     // attribute names (Strings)

        // add 'special' items (not attribute names)
        choices.add("<No Label>");  // 0
        choices.add("<OID>");       // 1

        // fill choices based on isObject
        Attributes attrs = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        List attrNames = attrs.getAttributeNames();
        Collections.sort(attrNames);
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();
            // todo xx filter out by type - only single-column str attrs OK
//            String typeDef = attrs.getAttrTypeDef(attrName);  // NB: slow!
            choices.add(attrName);  // 2+
        }

        // set initialValueIdx
        int initialValueIdx;
        String currLabelAttr = dbVisGraphMgr.getItemLabelAttribute(isObject);
        if (currLabelAttr == null) {
            boolean isUseOIDIfNoLabelAttr = (isObject ? DBVisualizerJFrame.IS_USE_OID_IF_NO_OBJ_LABEL_ATTR :
                    DBVisualizerJFrame.IS_USE_OID_IF_NO_LINK_LABEL_ATTR);
            if (isUseOIDIfNoLabelAttr) {
                initialValueIdx = 1;    // OID label
            } else {
                initialValueIdx = 0;    // no label
            }
        } else {                        // attribute label
            initialValueIdx = choices.indexOf(currLabelAttr);   // todo xx if -1: ok?;
        }

        // get their choice and save
        int selItemIdx = AttrListDialog.showDialog(messageJLabel, null,
                (isObject ? "object" : "link") + " attributes:", "Choose Attribute",
                (String[]) choices.toArray(new String[0]), initialValueIdx, null);
        String itemStr = (isObject ? "object" : "link");
        String message;
        if (selItemIdx == -1) {         // canel or no selection
            message = itemStr + " label attribute unchanged";
        } else if (selItemIdx == 0) {   // no label
            setItemLabelAttribute(null, isObject);
            if (isObject) {
                DBVisualizerJFrame.IS_USE_OID_IF_NO_OBJ_LABEL_ATTR = false;
            } else {
                DBVisualizerJFrame.IS_USE_OID_IF_NO_LINK_LABEL_ATTR = false;
            }
            message = itemStr + " label attribute set to none";
        } else if (selItemIdx == 1) {   // OID label
            setItemLabelAttribute(null, isObject);
            if (isObject) {
                DBVisualizerJFrame.IS_USE_OID_IF_NO_OBJ_LABEL_ATTR = true;
            } else {
                DBVisualizerJFrame.IS_USE_OID_IF_NO_LINK_LABEL_ATTR = true;
            }
            message = itemStr + " label attribute set to OID";
        } else {                        // attribute label
            String selAttrName = (String) choices.get(selItemIdx);
            setItemLabelAttribute(selAttrName, isObject);
            message = itemStr + " label attribute set to '" + selAttrName +
                    "' (applies to subsequent expands)";
        }
        messageJLabel.setText(message);
        refreshAttributes(isObject, true);  // refresh item labels
        vv.repaint();   // for no label and OID cases (possible to apply immediately)
    }

    private void setObjectColorAttribute(String objColorAttrName) {
        dbVisGraphMgr.setObjectColorAttribute(objColorAttrName);
        DBVisualizerJFrame.OBJ_COLOR_ATTR_NAME = objColorAttrName;
    }

    /**
     * Shows the attribute configuration dialog for colors of objects. Choices
     * include:
     * <ul>
     * <li>&lt;Same&gt; (same color for all vertices)
     * <li>&lt;Attribute&gt; (an object or link attribute, chosen from a list)
     * </ul>
     */
    private void setObjectColors() {
        List choices = new ArrayList();     // attribute names (Strings)

        // add 'special' items (not attribute names)
        choices.add("<Same Color>");    // 0

        // fill choices based on isObject
        Attributes attrs = DB.getObjectAttrs();
        List attrNames = attrs.getAttributeNames();
        Collections.sort(attrNames);
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();
            // todo xx filter out by type - only single-column attrs OK
//            String typeDef = attrs.getAttrTypeDef(attrName);  // NB: slow!
            choices.add(attrName);      // 1+
        }

        // set initialValueIdx
        int initialValueIdx;
        String currColorAttr = dbVisGraphMgr.getObjColorAttribute();
        if (currColorAttr == null) {
            initialValueIdx = 0;        // same color
        } else {
            initialValueIdx = choices.indexOf(currColorAttr);
        }

        // get their choice and save
        int selItemIdx = AttrListDialog.showDialog(messageJLabel, null,
                "object attributes:", "Choose Attribute",
                (String[]) choices.toArray(new String[0]), initialValueIdx, null);
        String message;
        if (selItemIdx == -1) {         // canel or no selection
            message = "object color attribute unchanged";
        } else if (selItemIdx == 0) {   // no label
            setObjectColorAttribute(null);
            message = "object color attribute set to all same";
            colorManager = null;
        } else {                        // attribute label
            String selAttrName = (String) choices.get(selItemIdx);
            setObjectColorAttribute(selAttrName);
            computeColorsFromAttribute(selAttrName, attrs);     // sets DBVisualizerJFrame.COLOR_MANAGER
            message = "object color attribute set to '" + selAttrName + "'";
        }
        messageJLabel.setText(message);
        refreshAttributes(true, false);     // refresh object colors
        vv.repaint();       // for same color case (possible to apply immediately)
        updateActions();    // legend availability might have changed
    }

    public void showLegend(ColorManager colorManager) {
        if (legendJFrame == null) {
            legendJFrame = new JFrame("Legend");
        }
        legendJFrame.setContentPane(colorManager.getColorLegend());
        legendJFrame.pack();
        legendJFrame.setVisible(true);
    }

    private void showPopupForVertex(final ProxSparseVertex vertex, MouseEvent e) {
        JPopupMenu popup = null;
        if (vertex.isPager()) {
//            popup = new JPopupMenu();
//            popup.add(new AbstractAction("Add next page") {
//                public void actionPerformed(ActionEvent e) {
//                    ProxSparseVertex pagedVertex = dbVisGraphMgr.getPagedVertexFromPager(vertex);
//                    messageJLabel.setText("broken: can't add to: " + pagedVertex); // todo xx
//                }
//            });
//            // todo xx more choices, including +5, +10, +all (#); shrink?; +5 most connected, ...
        } else if (!vertex.isPseudo()) {
            popup = new JPopupMenu();
            popup.add(new AbstractAction("Move to center") {
                public void actionPerformed(ActionEvent e) {
                    centerVertex = vertex;
                    layoutGraph(false);
                }
            });
            popup.add(new AbstractAction("Restart from vertex") {
                public void actionPerformed(ActionEvent e) {
                    dbVisGraphMgr.clear();
                    dbVisGraphMgr.addVertex(vertex);
                    expandVertex(vertex, true);
                }
            });
            popup.add(new AbstractAction("Browse object") {
                public void actionPerformed(ActionEvent e) {
                    browseObject(vertex, false);
                }
            });
            popup.add(new AbstractAction("Browse object attributes") {
                public void actionPerformed(ActionEvent e) {
                    browseObject(vertex, true);
                }
            });
        }
        if (popup != null) {
            popup.show(vv, e.getX(), e.getY());
        }
    }

    private void updateActions() {
//        PickedState pickedState = vv.getPickedState();
//        Set pickedVerts = pickedState.getPickedVertices();
//        Set pickedEdges = pickedState.getPickedEdges();
//        layoutAction.setEnabled(true);  // always enabled
//        setLinkLabelAction.setEnabled(true);      // always enabled
//        setObjectColorAction.setEnabled(true);    // always enabled
//        setObjectLabelAction.setEnabled(true);    // always enabled
        showLegendAction.setEnabled(colorManager != null);
    }

}
