/**
 * $Id: RPTJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;

import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import kdl.prox.app.GUI2App;
import kdl.prox.db.DB;
import kdl.prox.gui2.RPTNodeView;
import org.jdom.input.SAXBuilder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A JFrame that displays a saved RPT model as a tree, using Jung.
 */
public class RPTJFrame extends JFrame {
    /**
     * the graph
     */
    SparseTree graph;

    /**
     * the visual component and renderer for the graph
     */

    final ScalingControl scaler = new CrossoverScalingControl();
    double currentscale = 1.0;
    VisualizationViewer vv;
    DecisionTreeLayout layout;
    RptRenderer renderer;
    int disty = 110;    //y distance between each level of the tree
    int distx = 40;   //x distnace between each node at second to last level
    Dimension windowSize = new Dimension(950, 600); //Size of the window being displayed

    /**
     * @param rptFile an XML file containing the report to be modeled
     * @throws Exception if it is an unknown RPT format
     */
    public RPTJFrame(File rptFile) throws Exception {
        super("Graphing RPT: " + rptFile + " in " + DB.description());

        //When the window is closed it will dispose of itself
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Creates and sets the menu bar at the top of the screent
        JMenuBar menuBar = makeJMenuBar();
        this.setJMenuBar(menuBar);

        openRPT(rptFile);
        centerGraph();

        this.pack();
        this.setVisible(true);
    }

    private void openRPT(File rptFile) throws Exception {
        // find doctype, and create appropriate type of RPT to save its root
        RPTNodeView rootNode;
        String docType;
        docType = new SAXBuilder(false).build(rptFile).getDocType().getSystemID();
        if (docType.equals("rpt2.dtd")) {
            kdl.prox.model2.rpt.RPT rpt = new kdl.prox.model2.rpt.RPT();
            rpt.load(rptFile.getAbsolutePath());
            rootNode = rpt.getRootNode();
//        } else if (docType.equals("rpt.dtd")) {
//            kdl.prox.old.model.classifiers.RPT rpt = new kdl.prox.old.model.classifiers.RPT("model read from " + rptFile);
//            rpt.readFromXML(rptFile.getAbsolutePath());
//            rootNode = rpt.getRoot();
        } else {
            throw new Exception("Unknown RPT format: " + docType);
        }

        // Creates the SparseTree graph out of the generated rpt
        SparseTree graph = GraphGenerator.getGraph(rootNode);


        layout = new DecisionTreeLayout(graph, distx, disty);        // The layout the graph will use
        renderer = new RptRenderer();                                // Renderer used
        vv = new VisualizationViewer(layout, renderer, windowSize);  // JUNG's viewer
        vv.setPickSupport(new ShapePickSupport()); //Determines which node or edge the mouse is over
        vv.setToolTipFunction(new DefaultToolTipFunction());
        vv.setBackground(Color.WHITE);

        //Create the panel that allows zooming and scrolling
        //the scroll bars currently do not assess the size of the
        //graph properly
        this.getContentPane().add(new Scroll(vv));
        vv.setGraphMouse(new DefaultModalGraphMouse());

        //Adds the zooming controls to the bottom of the window
        JPanel zoomControls = makeControls();
        this.getContentPane().add(zoomControls, BorderLayout.SOUTH);
    }

    /**
     * Centers the graph on the root node
     */
    public void centerGraph() {
        //Sets the graph to the default positioning and view
        vv.getLayoutTransformer().setToIdentity();
        vv.getViewTransformer().setToIdentity();
    }

    /**
     * Centers the graph then zooms in or out so that the entire graph
     * fits on the screen
     */
    public void zoomToFit() {
        centerGraph();

        double vertScale = (vv.getSize().getHeight() / layout.getHeight() * .9);
        double horScale = (vv.getSize().getWidth() / (layout.getWidth() * 2) * .9);

        scaler.scale(vv, (float) Math.min(vertScale, horScale), vv.getCenter());

    }

    /**
     * Saves the graph to a jpeg
     * Currently trying to set the transformations back to as they
     * were does not work so the graph becomes offset when the
     * picture is generated
     */
    public void saveImage() {
        File file = GUI2App.getFileFromUser(true);
        if (file == null) {
            return;     // cancelled
        }
        //Saves the initial size of the Visualization Viewer
        Dimension oldSize = vv.getSize();

        //Sets the visualization to a size that will encompass the entire graph
        int width = layout.getWidth() * 2;
        int height = layout.getHeight();
        vv.setSize(width, height);

        //Sets the graph to the default positioning and view
        vv.getLayoutTransformer().setToIdentity();
        vv.getViewTransformer().setToIdentity();

        //Center the graph
        Point2D rootOffset = layout.getLocation(layout.getRootVertex());
        rootOffset = vv.transform(rootOffset);
        double xMov = rootOffset.getX();
        xMov = (width / 2) - xMov;
        vv.getLayoutTransformer().setTranslate(xMov, 0);

        //Paints the VV to a Bufferd Image
        BufferedImage bi = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bi.createGraphics();
        vv.paint(graphics);
        graphics.dispose();
        //saves the Buffered Image to disk
        try {
            ImageIO.write(bi, "jpeg", file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //reverts the VV to its previous size
        //and centers the graph
        vv.setSize(oldSize);
        vv.getLayoutTransformer().setToIdentity();
    }

    /**
     * Creates a menu bar with a save option and a close option
     *
     * @return The menu bar used at the top of the window
     */
    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();

        JMenu fileJMenu = new JMenu("File");
        AbstractAction saveImageAction = new AbstractAction("Save as JPEG image...") {
            public void actionPerformed(ActionEvent e) {
                saveImage();
            }
        };
        saveImageAction.putValue(Action.SHORT_DESCRIPTION, "Save Tree as Image");   // sets tooltip
        fileJMenu.add(saveImageAction);

        fileJMenu.add(new JSeparator());    // ----

        AbstractAction closeAction = new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        closeAction.putValue(Action.SHORT_DESCRIPTION, "Close this window");   // sets tooltip
        fileJMenu.add(closeAction);

        jMenuBar.add(fileJMenu);

        JMenu viewJMenu = new JMenu("View");

        //zoom to fit
        AbstractAction zoomToFitAction = new AbstractAction("Zoom to Fit") {
            public void actionPerformed(ActionEvent e) {
                zoomToFit();
            }
        };
        zoomToFitAction.putValue(Action.SHORT_DESCRIPTION, "View the entire graph");   // sets tooltip
        viewJMenu.add(zoomToFitAction);
        //Center Graph
        AbstractAction centerGraphAction = new AbstractAction("Center on Root") {
            public void actionPerformed(ActionEvent e) {
                centerGraph();
            }
        };
        centerGraphAction.putValue(Action.SHORT_DESCRIPTION, "Center on the root node");
        viewJMenu.add(centerGraphAction);

        jMenuBar.add(viewJMenu);
        return jMenuBar;
    }

    /**
     * Creates the controls used to zoom in and out
     *
     * @return a JPanel containing the zoom controls
     */
    private JPanel makeControls() {
        JPanel controls = new JPanel();

        scaler.scale(vv, .8f, vv.getCenter());

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
                currentscale *= 1.1f;
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 0.9f, vv.getCenter());
                currentscale *= 0.9f;
            }
        });

        controls.add(plus);
        controls.add(minus);

        return controls;
    }

}



