/**
 * $Id: SubgraphJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.visualization.*;
import kdl.prox.app.GUI2App;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbvis.*;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import spin.Spin;
import spin.off.ListenerSpinOver;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;


/**
 * A JFrame that knows how to display subgraphs based on a subgraph Proximity
 * URL, using JUNG. Runs in a separate thread.
 */
public class SubgraphJFrame extends JFrame implements PropertyChangeListener, RadiiInfo, ColorManagerInfo {

    private static Logger log = Logger.getLogger(SubgraphJFrame.class);

    private boolean isGraphing;     // "busy" flag
    private ProxURL subgURL;        // needed for prev and next feature
    private ColorManager colorManager = null;
    private JFrame legendJFrame;
    private VisualizationViewer vv;
    private AbstractLayout layout;
    private RunFileBean graphSubgraphBean;  // bean where the computation will be run

    // labels
    private String linkLabelAttrName = null;
    private String objectLabelAttrName = null;

    // IVs corresponding to the most recently-performed layout (see layoutGraph()):
    private Vertex centerVertex = null;
    private Map vertexToPolarCoordMap = null;

    // UI IVs
    private JLabel messageJLabel;

    private Action browseSubgraphAction;
    private Action nextAction;
    private Action prevAction;
    private Action showLegendAction;
    private Action setLinkLabelAction;
    private Action setObjectLabelAction;


    /**
     * Creates a JFrame that contains a nicely colored and laid out subgraph.
     *
     * @param subgURL
     */
    public SubgraphJFrame(ProxURL subgURL) {
        super();

        // build UI
        makeActions();
        setJMenuBar(makeJMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeNavigationBar(), BorderLayout.NORTH);
        getContentPane().add(makeGraphJComponent(new SparseGraph()), BorderLayout.CENTER);  // saves graph in layout
        getContentPane().add(makeMessageJLabel(), BorderLayout.SOUTH);
        setSize(getPreferredSize());
        setVisible(true);
        addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
                // ignored
            }

            public void componentMoved(ComponentEvent e) {
                // ignored
            }

            public void componentResized(ComponentEvent e) {
                layoutGraph();
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

        goToSubgURL(subgURL);
    }

    private void browseObject(final ProxSparseVertex vertex, boolean isAttributes) {
        GUI2App gui2App = GUI2App.getOrMakeGUI2App();
        gui2App.makeNewBrowserJFrame("item:/objects/" + vertex.getOID().intValue() +
                (isAttributes ? "!" + GUIContentGenerator.ATTR_VAL_PARAM : ""));
    }

    /**
     * @param graph
     * @param subgURL
     * @return the Vertex in graph that is most central, based on a priori knowledge
     *         of how containers are made (via QGraph). returns null if none found
     */
    public static Vertex findCenterVertex(Graph graph, ProxURL subgURL) {
        Set vertices = graph.getVertices();
        if (vertices.size() == 0) {
            return null;
        } else {
            Subgraph subgraph = subgURL.getSubgraph();
            Container container = subgraph.getParentContainer();
            List contLabelOrderList = makeLabelOrderingFromHistogram(container);
            Map subgLabelVertsMap = makeSubgLabelVertsMap(graph);
            Vertex centerVertex = getVertexWithLowestLabel(contLabelOrderList, subgLabelVertsMap);
            return centerVertex;
        }
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    // todo integrate with GUIContentGenerator.objectLabelLength, but it's an instance (not class) var :-(
    private String getLabelForVertOrEdge(ProxItemData proxItemData) {
        String dbLabel = proxItemData.getLabel();
        String label = (dbLabel != null ? dbLabel : "??");
        if (label.length() > DBVisualizerJFrame.LABEL_MAX) {
            label = label.substring(0, DBVisualizerJFrame.LABEL_MAX) + "...";
        }
        return label;
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

    private String getToolTipTextForVertOrEdge(ProxItemData vertOrEdge) {
        String vertOrEdgeLabel = getLabelForVertOrEdge(vertOrEdge);
        Integer oid = vertOrEdge.getOID();
        String oidStr = (oid == null ? "" : " (" + oid.toString() + "@0)");
        return vertOrEdgeLabel + oidStr;
    }

    /**
     * @param contLabelOrderList
     * @param subgLabelVertsMap
     * @return first Vertex in subgLabelVertsMap whose label best matches the
     *         ordering in contLabelOrderList. returns null if none found
     */
    private static Vertex getVertexWithLowestLabel(List contLabelOrderList,
                                                   Map subgLabelVertsMap) {
        for (Iterator labelIter = contLabelOrderList.iterator(); labelIter.hasNext();) {
            String label = (String) labelIter.next();
            List verticesWithLabel = (List) subgLabelVertsMap.get(label);
            if (verticesWithLabel != null && verticesWithLabel.size() != 0) {
                return (Vertex) verticesWithLabel.get(0);
            }
        }
        return null;
    }

    public Map getVertexToPolarCoordMap() {
        return vertexToPolarCoordMap;
    }

    public VisualizationViewer getVisualizationViewer() {
        return vv;
    }

    private void goToSubgURL(ProxURL subgURL) {
        setTitle("Subgraph " + subgURL);
        this.subgURL = subgURL;
        isGraphing = true;      // cleared on property change
        updateActions();

        // spin off a thread that will prepare the graph. the UI gets constructed
        // when the thread's done
        Spin.setDefaultOffEvaluator(new ListenerSpinOver());  // automatically spin-over all listeners
        graphSubgraphBean = new RunGraphSubgraphImpl(subgURL, layout.getGraph());

        RunFileBean graphSubgraphBeanOff = (RunFileBean) Spin.off(graphSubgraphBean);
        graphSubgraphBeanOff.addPropertyChangeListener(this);  // ListenerSpinOver will spin-over us automatically
        try {
            graphSubgraphBeanOff.start();
        } catch (Exception exc) {
            StringWriter stringWriter = new StringWriter();
            exc.printStackTrace(new PrintWriter(stringWriter));
            messageJLabel.setText("There was an error preparing the subgraph " +
                    "for display:\n" + stringWriter.toString());
        }
    }

    // todo xx move to Spin so that doesn't happen in current (EDT) thread (update message via prop change)
    private void layoutGraph() {
        final Dimension size = vv.getSize();
        if (centerVertex == null) {
            layout.initialize(size);
        } else {
            final Graph graph = layout.getGraph();

            // calculate polar coords based on the graph via an intermediate rooted tree
            try {
                vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(centerVertex,
                        DBVisualizerJFrame.makeVertextComparator());
            } catch (IllegalArgumentException exc) {
                // graph not connected. todo xx better solution?
                JOptionPane.showMessageDialog(null, "Graph Not Connected");
                return;
            }

            // convert polar to rectangular, then update vertex locations
            VertexLocationFunction vertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                    vertexToPolarCoordMap, size, DBVisualizerJFrame.VERTEX_DIAMETER);
            layout.initialize(size, vertLocFcn);
        }
        vv.repaint();
    }

    private void makeActions() {
        browseSubgraphAction = new AbstractAction("Browse Subgraph") {
            public void actionPerformed(ActionEvent e) {
                GUI2App.getOrMakeGUI2App().makeNewBrowserJFrame(subgURL.toString());
            }
        };
        browseSubgraphAction.putValue(Action.SHORT_DESCRIPTION,
                "Browse this subgraph.");

        nextAction = new AbstractAction("Next") {
            public void actionPerformed(ActionEvent e) {
                showNextOrPrevSubgraph(true);
            }
        };
        nextAction.putValue(Action.SHORT_DESCRIPTION,
                "Go to the next subgraph.");

        prevAction = new AbstractAction("Prev") {
            public void actionPerformed(ActionEvent e) {
                showNextOrPrevSubgraph(false);
            }
        };
        prevAction.putValue(Action.SHORT_DESCRIPTION,
                "Go to the previous subgraph.");

        setLinkLabelAction = new AbstractAction("Set Link Labels...") {
            public void actionPerformed(ActionEvent e) {
                setItemLabels(false);
            }
        };
        setLinkLabelAction.putValue(Action.SHORT_DESCRIPTION,
                "Configure link label content.");

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

    private VisualizationViewer makeGraphJComponent(Graph graph) {
        PluggableRenderer pr = new PluggableRenderer();
        pr.setEdgeShapeFunction(new EdgeShape.QuadCurve());     // so that multi-edges don't overlap
        pr.setVertexShapeFunction(new EllipseVertexShapeFunction(new ConstantVertexSizeFunction(DBVisualizerJFrame.VERTEX_DIAMETER),
                new ConstantVertexAspectRatioFunction(1.0f)));

        // set up vetex and edge labels
        pr.setVertexStringer(new VertexStringer() {
            public String getLabel(ArchetypeVertex vertex) {
                kdl.prox.util.Assert.condition(vertex instanceof ProxItemData,
                        "vertex wasn't a ProxItemData: " + vertex);
                return getLabelForVertOrEdge((ProxItemData) vertex);
            }
        });
        pr.setEdgeStringer(new EdgeStringer() {
            public String getLabel(ArchetypeEdge edge) {
                kdl.prox.util.Assert.condition(edge instanceof ProxItemData,
                        "edge wasn't a ProxItemData: " + edge);
                return getLabelForVertOrEdge((ProxItemData) edge);
            }
        });
        pr.setVertexPaintFunction(SubgraphJFrame.makeVertexPaintFunction(this));

        layout = new StaticLayout(graph);
        vv = new VisualizationViewer(layout, pr);
        vv.getModel().setRelaxerThreadSleepTime(2000);  // magic number helps avoid 100% CPU with more than a few nodes (from https://sourceforge.net/forum/message.php?msg_id=3248935)
//        vv.setBackground(Color.white);
        vv.setBackground(Color.decode("#FFFFCC"));      // light yellow
        vv.addPreRenderPaintable(DBVisualizerJFrame.makeRadiiPaintable(this));

        vv.setToolTipFunction(new DefaultToolTipFunction() {

            public String getToolTipText(Edge edge) {
                kdl.prox.util.Assert.condition(edge instanceof ProxItemData,
                        "edge wasn't a ProxItemData: " + edge);
                return getToolTipTextForVertOrEdge((ProxItemData) edge);
            }

            public String getToolTipText(Vertex vertex) {
                kdl.prox.util.Assert.condition(vertex instanceof ProxItemData,
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
                // todo xx what to do on clicks?:
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

        return vv;
    }

    private JMenu makeGraphJMenu() {
        JMenu jMenu = new JMenu("Graph");
        jMenu.add(showLegendAction);
        jMenu.add(setObjectLabelAction);
        jMenu.add(setLinkLabelAction);
        jMenu.add(new JSeparator());
        jMenu.add(nextAction);
        jMenu.add(prevAction);
        jMenu.add(new JSeparator());
        jMenu.add(browseSubgraphAction);
        return jMenu;
    }

    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(makeGraphJMenu());
        return jMenuBar;
    }

    /**
     * @param container source of labels and counts, via histogram based on subgraph
     *                  item name
     * @return list of subgraph names ordered for use in selecting central vertices.
     *         based on name histogram
     */
    private static List makeLabelOrderingFromHistogram(Container container) {
        List labelCounts = new ArrayList();     // ea. ele: List of [label, count]
        NST objectNST = container.getItemNST(true);
        ResultSet nameColHistogramRS = objectNST.getColumnHistogram("name");
        while (nameColHistogramRS.next()) {
            String label = nameColHistogramRS.getString(0);
            int count = nameColHistogramRS.getInt(1);
            List labelCountList = new ArrayList();
            labelCountList.add(label);
            labelCountList.add(new Integer(count));
            labelCounts.add(labelCountList);
        }
        Collections.sort(labelCounts, new Comparator() {
            public int compare(Object labelCount1, Object labelCount2) {
                List labelCount1List = (List) labelCount1;
                List labelCount2List = (List) labelCount2;
                Integer count1 = (Integer) labelCount1List.get(1);
                Integer count2 = (Integer) labelCount2List.get(1);
                return count1.intValue() - count2.intValue();
            }
        });

        List orderedLabels = new ArrayList();
        for (Iterator labelCountIter = labelCounts.iterator(); labelCountIter.hasNext();) {
            List labelCount = (List) labelCountIter.next();     // [label, count]
            String label = (String) labelCount.get(0);
            orderedLabels.add(label);
        }
        return orderedLabels;
    }

    /**
     * @param graph
     * @return subgLabelVertsMap. keys: label names; values: List of Vertex
     *         instances in graph with that label
     */
    private static Map makeSubgLabelVertsMap(Graph graph) {
        HashMap subgLabelVertsMap = new HashMap();
        for (Iterator vertIter = graph.getVertices().iterator(); vertIter.hasNext();) {
            ProxSparseVertex vertex = (ProxSparseVertex) vertIter.next();
            String label = vertex.getLabel();
            List labelVerts = (List) subgLabelVertsMap.get(label);
            if (labelVerts == null) {
                labelVerts = new ArrayList();
                subgLabelVertsMap.put(label, labelVerts);
            }
            labelVerts.add(vertex);
        }
        return subgLabelVertsMap;
    }

    private JComponent makeMessageJLabel() {
        messageJLabel = new JLabel("<status here>");    // NB: without any text here the label is initially squashed to zero height
        messageJLabel.setToolTipText("Shows status while showing subgraph.");
        messageJLabel.setBorder(new LineBorder(Color.BLACK));
        return messageJLabel;
    }

    private JToolBar makeNavigationBar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.add(prevAction);
        jToolBar.add(nextAction);
        return jToolBar;
    }

    /**
     * @param colorManagerInfo
     * @return VertexPaintFunction suitable for use in PluggableRenderer.setVertexPaintFunction()
     *         that colors based on colorManager
     */
    public static VertexPaintFunction makeVertexPaintFunction(final ColorManagerInfo colorManagerInfo) {
        VertexPaintFunction vpf = new VertexPaintFunction() {

            public Paint getFillPaint(Vertex vertex) {
                Paint fillPaintForVertex = Color.RED;
                ProxItemData proxItemData = ((ProxItemData) vertex);
                if (proxItemData.isPseudo()) {      // possible?
                    fillPaintForVertex = Color.GRAY;
                } else {
                    ColorManager colorManager = colorManagerInfo.getColorManager();
                    Color currColorAttr = colorManager.getColor(proxItemData.getName());   // null if none
                    if (currColorAttr != null) {
                        fillPaintForVertex = currColorAttr;
                    }
                }
                return fillPaintForVertex;
            }

            public Paint getDrawPaint(Vertex v) {
                return Color.BLACK;  //To change body of implemented methods use File | Settings | File Templates.
            }

        };
        return vpf;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        spin.demo.Assert.onEDT();
        String propertyName = evt.getPropertyName();
        if ("status".equals(propertyName)) {
            String status = (String) evt.getNewValue();
            messageJLabel.setText("Status: " + status + "\n");
        } else if ("graph".equals(propertyName)) {
            Graph graph = vv.getGraphLayout().getGraph();
            // get the colors
            String[] names = ((RunGraphSubgraphImpl) graphSubgraphBean).getNames();
            colorManager = new ColorManager(names);
            // set the labels, if any set by default
            if (linkLabelAttrName != null) {
                setItemLabelAttribute(linkLabelAttrName, false);
            }
            if (objectLabelAttrName != null) {
                setItemLabelAttribute(objectLabelAttrName, true);
            }
            // find the center of the graph, and show it
            centerVertex = SubgraphJFrame.findCenterVertex(graph, subgURL);
            layoutGraph();

            isGraphing = false;
            updateActions();
        } else {
            log.warn("unknown property name: '" + propertyName + "'");
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
        choices.add("<OID>");       // 0
        choices.add("<item name>");       // 1

        // fill choices based on isObject
        Attributes attrs = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        List attrNames = attrs.getAttributeNames();
        Collections.sort(attrNames);
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();
            // todo xx filter out by type - only single-column str attrs OK
            choices.add(attrName);  // 2+
        }

        // set initialValueIdx
        int initialValueIdx = 0;
        String currLabelAttr = isObject ? objectLabelAttrName : linkLabelAttrName;
        if (currLabelAttr == null) {
            initialValueIdx = 1;
        } else {
            initialValueIdx = choices.indexOf(currLabelAttr);   // todo xx if -1: ok?;
        }

//        // get their choice and save
        int selItemIdx = AttrListDialog.showDialog(messageJLabel, null,
                (isObject ? "object" : "link") + " attributes:", "Choose Attribute",
                (String[]) choices.toArray(new String[0]), initialValueIdx, null);
        String itemStr = (isObject ? "object" : "link");
        String message;
        if (selItemIdx == -1) {         // canel or no selection
            message = itemStr + " label attribute unchanged";
        } else if (selItemIdx == 0) {   // no label
            setItemLabelAttribute(null, isObject);
            message = itemStr + " label attribute set to OID";
        } else if (selItemIdx == 1) {   // name
            setItemLabelAttributeToName(isObject);
            message = itemStr + " label attribute set to <name>";
        } else {                        // attribute label
            String selAttrName = (String) choices.get(selItemIdx);
            setItemLabelAttribute(selAttrName, isObject);
            message = itemStr + " label attribute set to '" + selAttrName +
                    "' (applies to subsequent expands)";
        }
        messageJLabel.setText(message);
        vv.repaint();   // for no label and OID cases (possible to apply immediately)
    }

    private void setItemLabelAttribute(String attrName, boolean isObject) {
        final Graph graph = layout.getGraph();
        Set vertsOrEdges = isObject ? graph.getVertices() : graph.getEdges();
        NST attrNST = null;
        if (attrName != null) {
            Attributes attrs = isObject ? DB.getObjectAttrs() : DB.getLinkAttrs();
            attrNST = attrs.getAttrDataNST(attrName);
        }
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            if (attrName != null) {
                List values = attrNST.filter("id = " + vertOrEdge.getOID()).selectRows("value").toStringList(1);
                if (values.size() == 0) {
                    vertOrEdge.setLabel("??");
                } else {
                    vertOrEdge.setLabel(Util.join(values, ","));
                }
            } else {
                vertOrEdge.setLabel(vertOrEdge.getOID() + "@0");
            }
        }
        if (isObject) {
            objectLabelAttrName = attrName;
        } else {
            linkLabelAttrName = attrName;
        }
    }

    private void setItemLabelAttributeToName(boolean isObject) {
        final Graph graph = layout.getGraph();
        Set vertsOrEdges = isObject ? graph.getVertices() : graph.getEdges();
        for (Iterator vertOrEdgeIter = vertsOrEdges.iterator(); vertOrEdgeIter.hasNext();) {
            ProxItemData vertOrEdge = (ProxItemData) vertOrEdgeIter.next();
            vertOrEdge.setLabel(vertOrEdge.getName());
        }
        if (isObject) {
            objectLabelAttrName = null;
        } else {
            linkLabelAttrName = null;
        }
    }


    public void showLegend(ColorManager colorManager) {
        if (legendJFrame == null) {
            legendJFrame = new JFrame("Legend");
        }
        legendJFrame.setContentPane(colorManager.getColorLegend());
        legendJFrame.pack();
        legendJFrame.setVisible(true);
    }


    /**
     * Graphs the next or previous subgraph in my container, based on isGoNext.
     *
     * @param isGoNext true if "next"; false if "previous"
     */
    private void showNextOrPrevSubgraph(boolean isGoNext) {
        int nextOrPrevOID = GUIContentGenerator.getNextOrPrevSubgraphOID(subgURL, isGoNext);
        Assert.condition(nextOrPrevOID != -1, "can't go next or previous");

        String containerURL = subgURL.getAddressSansLastComponent();
        String subgPrefix = "subg:" + containerURL + "/";
        goToSubgURL(new ProxURL(subgPrefix + nextOrPrevOID));
    }

    private void showPopupForVertex(final ProxSparseVertex vertex, MouseEvent e) {
        JPopupMenu popup = null;
        if (vertex.isPager()) {
            // ignore pagers
        } else if (!vertex.isPseudo()) {
            popup = new JPopupMenu();
            popup.add(new AbstractAction("Move to center") {
                public void actionPerformed(ActionEvent e) {
                    centerVertex = vertex;
                    layoutGraph();
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
            popup.add(new AbstractAction("Browse database from object") {
                public void actionPerformed(ActionEvent e) {
                    Integer itemOID = vertex.getOID();
                    DBVisualizerJFrame dbVisualizerJFrame = new DBVisualizerJFrame();
                    dbVisualizerJFrame.graphFromStartingOID(itemOID.toString());
                }
            });
        }
        if (popup != null) {
            popup.show(vv, e.getX(), e.getY());
        }
    }

    private void updateActions() {
        boolean isOKGoNext = GUIContentGenerator.getNextOrPrevSubgraphOID(subgURL, true) != -1;
        boolean isOKGoPrev = GUIContentGenerator.getNextOrPrevSubgraphOID(subgURL, false) != -1;
//        browseSubgraphAction.setEnabled(true);      // always enabled
        nextAction.setEnabled(isOKGoNext && !isGraphing);
        prevAction.setEnabled(isOKGoPrev && !isGraphing);
//        showLegendAction.setEnabled(true);      // always enabled
    }

}
