/**
 * $Id: SubgraphThumbsJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.visualization.*;
import kdl.prox.db.Container;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbvis.*;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;


/**
 * Exploratory program that provides a visual sampling of subgraphs in a container
 * via thumbnails created using radial layout.
 */
public class SubgraphThumbsJFrame extends JFrame {

    private static final Logger log = Logger.getLogger(SubgraphThumbsJFrame.class);
    private static final int SUBG_COUNT = 9;
    private static final int SUBG_DIMENSION = 3;  // sqrt(9)
    private static final int THUMB_SIZE = 64;
    private static final int EMPTY_THUMB = -1;

    private ColorManager colorManager = null;   // null until set by makeThumbs()
    private JFrame legendJFrame;


    private JTable thumbJTable;       // thumbnail images, with help from SimpleCellRenderer
    private JProgressBar jProgressBar;

    private AbstractAction openSubgraphAction;
    private AbstractAction showLegendAction;


    /**
     * Randomly picks a fixed number of subgraphs from the passed Container, and
     * shows them in a new JFrame. Runs in the EDT for now.
     *
     * @param containerURLStr ProxURL for container to sample from
     * @throws IllegalArgumentException if containerURL doesn't refer to a valid Container
     */
    public SubgraphThumbsJFrame(String containerURLStr) {
        final ProxURL containerURL;
        final Container container;
        try {
            containerURL = new ProxURL(containerURLStr);
            container = containerURL.getContainer(false);
        } catch (Exception e) {
            log.error("No container found at location: " + containerURLStr);
            return;
        }

        makeActions();
        setTitle("Thumbnails: " + containerURLStr);
        setSize(450, 270);
        setJMenuBar(makeJMenuBar());

        // set up thumbJTable
        DefaultTableModel tableModel = new DefaultTableModel() {

            public Class getColumnClass(int mColIndex) {
                int rowIndex = 0;
                Object o = getValueAt(rowIndex, mColIndex);
                if (o == null) {
                    return Object.class;
                } else {
                    return o.getClass();
                }
            }

            public int getColumnCount() {
                return SUBG_DIMENSION;
            }

            public int getRowCount() {
                return SUBG_DIMENSION;
            }

        };
        thumbJTable = new JTable(tableModel) {  // content updated later

            public boolean isCellEditable(int rowIndex, int vColIndex) {
                return false;
            }
        };
        thumbJTable.setRowHeight(THUMB_SIZE);
        thumbJTable.setColumnSelectionAllowed(true);
        thumbJTable.setRowSelectionAllowed(true);
        thumbJTable.setDefaultRenderer(Value.class, new ValueCellRenderer());
        thumbJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        thumbJTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JTable jTable = (JTable) e.getSource();
                if (!jTable.isEditing() && e.getClickCount() == 2) {
                    Point p = e.getPoint();
                    int row = jTable.rowAtPoint(p);
                    int col = jTable.columnAtPoint(p);
                    openSubgraph(jTable, row, col);
                }
            }
        });
        thumbJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateActions();    // todo xx enough? see http://javaalmanac.com/egs/javax.swing.table/SelEvent.html
            }
        });
        getContentPane().add(thumbJTable, BorderLayout.CENTER);

        // set up jProgressBar
        jProgressBar = new JProgressBar();
        jProgressBar.setStringPainted(true);
        jProgressBar.setMinimum(0);
        jProgressBar.setMaximum(SUBG_COUNT - 1);
        getContentPane().add(jProgressBar, BorderLayout.SOUTH);
        updateActions();
        setVisible(true);

        // create the images
        Thread thread = new Thread(new Runnable() {
            public void run() {
                makeThumbs(container, containerURL);    // NB: takes time, so run in own thread to not hang GUI
                updateActions();
            }
        });
        thread.start();
    }

    /**
     * @return currently-selected Value, or null if there isn't one
     */
    private Value getSelectedValue() {
        int rowIndex = thumbJTable.getSelectedRow();
        int colIndex = thumbJTable.getSelectedColumn();
        try {
            return (Value) thumbJTable.getValueAt(rowIndex, colIndex);
        } catch (ArrayIndexOutOfBoundsException exc) {
            return null;
        }
    }

    private void makeActions() {
        openSubgraphAction = new AbstractAction("Open Selected Subgraph") {
            public void actionPerformed(ActionEvent e) {
                int rowIndex = thumbJTable.getSelectedRow();
                int colIndex = thumbJTable.getSelectedColumn();
                openSubgraph(thumbJTable, rowIndex, colIndex);
            }
        };
        openSubgraphAction.putValue(Action.SHORT_DESCRIPTION,
                "Open the graphical browser for the selected subgraph.");

        showLegendAction = new AbstractAction("Show Color Legend") {
            public void actionPerformed(ActionEvent e) {
                showLegend(colorManager);
            }
        };
        showLegendAction.putValue(Action.SHORT_DESCRIPTION,
                "Show the object color legend.");
    }

    private JMenu makeFileJMenu() {
        JMenu jMenu = new JMenu("File");
        jMenu.add(openSubgraphAction);
        jMenu.add(showLegendAction);
        return jMenu;
    }

    private BufferedImage makeFullSizeImage(VisualizationViewer vv) {
        int width = vv.getWidth();
        int height = vv.getHeight();
        BufferedImage thumbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbImage.createGraphics();
        Color bg = vv.getBackground();
        graphics.setColor(bg);
        graphics.fillRect(0, 0, width, height);
        vv.paint(graphics);
        graphics.dispose();
        return thumbImage;
    }

    private void makeIconsForSubgraphs(ColorManager colorManager, ProxURL containerURL,
                                       int[] subgOIDs) {   // subgOIDs contains EMPTY_THUMB values if ran out of subgraphs
        showMessage("creating thumbs");
        Dimension size = new Dimension(200, 200);
        for (int subgOIDIdx = 0; subgOIDIdx < subgOIDs.length; subgOIDIdx++) {
            int subgOID = subgOIDs[subgOIDIdx];
            if (subgOID == EMPTY_THUMB) {
                return;
            }

            int row = subgOIDIdx / SUBG_DIMENSION;
            int column = subgOIDIdx % SUBG_DIMENSION;
            ProxURL subgURL = new ProxURL("subg:" + containerURL.getAddress() +
                    "/" + subgOID);
            Graph graph = new SparseGraph();
            showMessage("calculating: " + subgOID, subgOIDIdx);
            GUIContentGenerator.getGraphForSubgURL(subgURL, graph);     // fills graph

            Icon imageIcon = makeThumbnailFromGraph(colorManager, graph, size, subgURL);
            setTableCell(new Value(subgOID, imageIcon, subgURL), row, column);
        }
    }

    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(makeFileJMenu());
        return jMenuBar;
    }

    /**
     * @param graph
     * @param size
     * @param subgURL
     * @return Icon for graph. returns null if graph is not connected
     */
    public Icon makeThumbnailFromGraph(final ColorManager colorManager, Graph graph,
                                       Dimension size, ProxURL subgURL) {
        // todo merge with SubgraphJFrame.makeGraphJComponent():
        PluggableRenderer pr = new PluggableRenderer();
        AbstractLayout layout = new StaticLayout(graph);
        final VisualizationViewer vv = new VisualizationViewer(layout, pr, size);
        vv.setSize(size);   // crucial!
        vv.getModel().setRelaxerThreadSleepTime(2000);  // magic number helps avoid 100% CPU with more than a few nodes (from https://sourceforge.net/forum/message.php?msg_id=3248935)
        vv.setBackground(Color.decode("#FFFFCC"));      // light yellow

        // lay out the graph
        ProxSparseVertex centerVertex = (ProxSparseVertex) SubgraphJFrame.findCenterVertex(graph,
                subgURL);
        Map vertexToPolarCoordMap;
        try {
            vertexToPolarCoordMap = RadialLayoutHelper.computePolarCoordinates(centerVertex,
                    DBVisualizerJFrame.makeVertextComparator());
        } catch (IllegalArgumentException exc) {
            // graph not connected
            return null;
        }

        VertexLocationFunction vertLocFcn = RadialLayoutHelper.convertPolarToRectangular(graph,
                vertexToPolarCoordMap, size, DBVisualizerJFrame.VERTEX_DIAMETER);
        layout.initialize(size, vertLocFcn);

        // set up appearance: vertex colors, and radii
        final Map finalVertexToPolarCoordMap = vertexToPolarCoordMap;
        pr.setVertexPaintFunction(SubgraphJFrame.makeVertexPaintFunction(new ColorManagerInfo() {
            public ColorManager getColorManager() {
                return colorManager;
            }
        }));

        vv.addPreRenderPaintable(DBVisualizerJFrame.makeRadiiPaintable(new RadiiInfo() {
            public Map getVertexToPolarCoordMap() {
                return finalVertexToPolarCoordMap;
            }

            public VisualizationViewer getVisualizationViewer() {
                return vv;
            }
        }));

        // create the image
        BufferedImage fullSizeImage = makeFullSizeImage(vv);
        BufferedImage thumbailImage = makeThumbnailImage(fullSizeImage,
                THUMB_SIZE, THUMB_SIZE);
        return new ImageIcon(thumbailImage);
    }

    private BufferedImage makeThumbnailImage(BufferedImage fullSizeImage, int maxW, int maxH) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        int w = fullSizeImage.getWidth();
        int h = fullSizeImage.getHeight();
        double ratio = Math.min(maxW / (double) w, maxH / (double) h);
        int newW = (int) (w * ratio), newH = (int) (h * ratio);
        BufferedImage result = gc.createCompatibleImage(newW, newH, fullSizeImage.getColorModel().getTransparency());
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawRenderedImage(fullSizeImage, AffineTransform.getScaleInstance(ratio, ratio));
        graphics.dispose();
        return result;
    }

    private void makeThumbs(Container container, ProxURL containerURL) {
        // get random subgraph OIDs
        showMessage("Picking " + SUBG_COUNT + " random subgraphs");
        int[] subgOIDs = selectRandomSubgraphOIDs(container, SUBG_COUNT);

        // create thumbnail images and labels
        String[] names = GUIContentGenerator.getUniqueNamesInContainer(container);
        colorManager = new ColorManager(names);
        makeIconsForSubgraphs(colorManager, containerURL, subgOIDs);
        showMessage("", 0);
    }

    private void openSubgraph(JTable jTable, int row, int col) {
        try {
            Value value = (Value) jTable.getValueAt(row, col);
            if (value != null) {
                new SubgraphJFrame(value.subgURL);
            }
        } catch (ArrayIndexOutOfBoundsException exc) {
            // ignore - happens when clicking in white space
        }
    }

    /**
     * @param container
     * @param numSubgraphs
     * @return selected subgraph OIDs. NB: might have fewer than numSubgraphs.
     *         a value of EMPTY_THUMB means "not found"
     */
    private int[] selectRandomSubgraphOIDs(Container container, int numSubgraphs) {
        int[] subgOIDs = new int[numSubgraphs];
        Arrays.fill(subgOIDs, EMPTY_THUMB);

        NST subgOIDsNST = container.getDistinctSubgraphOIDs();
        NST randSubgOIDsNST = subgOIDsNST.filter("subg_id RANDOM " + numSubgraphs);
        ResultSet resultSet = randSubgOIDsNST.selectRows();

        int subgIDX = 0;
        while (resultSet.next()) {
            int subgOID = resultSet.getOID(1);
            subgOIDs[subgIDX++] = subgOID;
        }

        return subgOIDs;
    }

    private void setTableCell(final Value value, final int row, final int column) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {   // refresh in the EDT

                public void run() {
                    thumbJTable.getModel().setValueAt(value, row, column);
                }
            });
        } catch (InterruptedException e1) {
            // ignore
        } catch (InvocationTargetException e1) {
            // ignore
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


    private void showMessage(String message) {
        showMessage(message, -1);
    }

    /**
     * Shows message in jProgressBar in EDT.
     *
     * @param message  string to set in jProgressBar
     * @param progress value to set in jProgressBar. -1 means only set text (message)
     */
    private void showMessage(final String message, final int progress) {
        if (SwingUtilities.isEventDispatchThread()) {
            showProgressInternal(message, progress);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {   // refresh in the EDT

                    public void run() {
                        showProgressInternal(message, progress);
                    }
                });
            } catch (InterruptedException e1) {
                // ignore
            } catch (InvocationTargetException e1) {
                // ignore
            }
        }
    }

    private void showProgressInternal(final String message, int progress) {
        jProgressBar.setString(message);
        if (progress != -1) {
            jProgressBar.setValue(progress);
        }
    }

    private void updateActions() {
        openSubgraphAction.setEnabled(getSelectedValue() != null);
        showLegendAction.setEnabled(colorManager != null);
    }

    //
    // from http://forum.java.sun.com/thread.jspa?threadID=578530&messageID=3132358
    //

    private static class Value {
        int subgOID;
        Icon imageIcon;
        ProxURL subgURL;

        Value(int subgOID, Icon imageIcon, ProxURL subgURL) {
            this.subgOID = subgOID;
            this.imageIcon = imageIcon;
            this.subgURL = subgURL;
        }

    }


    static class ValueCellRenderer extends JLabel implements TableCellRenderer {

        public ValueCellRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            Value val = (Value) value;
            if (val != null) {
                setText(val.subgOID + "");
                setIcon(val.imageIcon);
            } else {
                setText(null);
                setIcon(null);
            }
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }

    }


}
