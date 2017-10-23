/**
 * $Id: BrowserJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.app.GUI2App;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.qged.QGraphEditorJFrame;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.rdnview.RDNJFrame;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import spin.Spin;
import spin.off.ListenerSpinOver;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Stack;


/**
 * Shows GUIContentGenerator pages (HTML), and supports following links,
 * browsing history, etc.
 */
public class BrowserJFrame extends JFrame {

    private static Logger log = Logger.getLogger(BrowserJFrame.class);
    public static final String ACTION_URL_PATTERN = "action=";

    private GUI2App gui2App;
    private GUIContentGenerator guiContentGen;
    private JEditorPane jEditorPane;
    private JTextField jURLField;
    private Stack<String> urlStack = new Stack<String>();   // holds prox URL strings. top is current page. empty if at home
    private List pageActionsList;           // ProxAction instances for top of current page

    private Action backAction;
    private Action editQueryAction;
    private Action graphRDNAction;
    private Action graphRPTAction;
    private Action homeAction;
    private Action newBrowserAction;
    private Action newQueryAction;
    private Action openInterpreterAction;
    private Action preferencesAction;
    private Action quitAction;
    private Action refreshAction;
    private Action runQueryAction;
    private Action runScriptAction;
    private Action urlChangeAction;
    private Action viewFileAction;


    /**
     * Creates (but does not show) a browser that shows the home screen. NB:
     * Should be accessed via GUI2App.makeNewBrowserJFrame(), which manages
     * all instances.
     *
     * @param gui2App
     */
    public BrowserJFrame(final GUI2App gui2App) {
        super("Proximity Database " + DB.description());
        this.gui2App = gui2App;
        guiContentGen = new GUIContentGenerator();
        // do nothing so that we can catch the last window's close, allowing us
        // to cancel exiting the app if there are modified editors open
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // call first so that actions exist when added to GUI components
        makeActions();
        // create the components and add the, to the contentPane
        java.awt.Container contentPane = getContentPane();
        JPanel NavigationBarPane = makeNavigationBarPane();
        contentPane.add(NavigationBarPane, BorderLayout.NORTH);
        jEditorPane = makeJEditorPane();
        contentPane.add(new JScrollPane(jEditorPane), BorderLayout.CENTER);
        setJMenuBar(makeJMenuBar());
        goHome();
        pack();
    }

    public void deleteAttribute() {
        ProxURL proxURL = new ProxURL(getCurrentProxURL());
        Attributes attributes = proxURL.getAttributes(true);
        String firstComponent = proxURL.getFirstAddressComponent();
        boolean isObjectAttr = "objects".equals(firstComponent);
        String attrName = proxURL.getLastAddressComponent();
        if (!GUI2App.isUserSaysOK(null, "Delete " + (isObjectAttr ? "object" : "link") +
                " attribute " + attrName + "?")) {
            // user didn't want to delete
        } else {
            attributes.deleteAttribute(attrName);
            goBack();
        }
    }

    public void deleteContainer() {
        ProxURL proxURL = new ProxURL(getCurrentProxURL());
        Container container = proxURL.getContainer(false);
        if (!GUI2App.isUserSaysOK(null, "Delete container " +
                container.getName() + "?")) {
            // user didn't want to delete container
        } else {
            container.getParent().deleteChild(container.getName());
            goBack();
        }
    }

    /**
     * Opens the query editor on either a new file or an existing one, depending
     * on isChooseFile (true -> choose existing, false -> new).
     *
     * @param isChooseFile
     */
    public void editQuery(boolean isChooseFile) {
        try {
            if (isChooseFile) {
                File file = GUI2App.getFileFromUser(false);
                if (file == null) {
                    return;     // cancelled
                }
                new QGraphEditorJFrame(file);  // sets size and shows itself
            } else {
                new QGraphEditorJFrame();  // ""
            }
        } catch (Exception exc) {
            log.error("error editing query", exc);
            JOptionPane.showMessageDialog(null, exc.getMessage(),
                    "Error Editing Query", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportAttribute() {
        ProxURL proxURL = new ProxURL(getCurrentProxURL());
        Attributes attrs = proxURL.getAttributes(true);
        String attrName = proxURL.getLastAddressComponent();

        File file = GUI2App.getFileFromUser(true);
        if (file == null) {
            return;     // cancelled
        }
        // save
        try {
            attrs.getAttrDataNST(attrName).tofile(file.getPath());
            JOptionPane.showMessageDialog(null, "Attribute exported to " + file.getPath());
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(null, "Error exporting attribute: " + exc);
        }

    }

    /**
     * @return the url String at the top of my urlStack. Returns null if at home.
     */
    public String getCurrentProxURL() {
        if (urlStack.empty()) {
            return null;    // at home
        } else {
            return urlStack.peek();
        }
    }

    public GUI2App getGui2App() {
        return gui2App;
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 600);
    }

    private void goBack() {
        Assert.condition(!urlStack.empty(), "stack empty");
        urlStack.pop();     // remove curent
        if (urlStack.empty()) {
            goHome();
        } else {
            showPage(urlStack.peek());
            updateActions();
        }
    }

    private void goHome() {
        urlStack.clear();
        showPage("db:/");
        updateActions();
    }

    public void goTo(String proxURL) {
        urlStack.push(proxURL);
        showPage(proxURL);
        updateActions();
    }

    /**
     * Prompts the user for a saved RDN (XML file) then shows it in the RDN
     * (graph) viewer.
     */
    public void graphRDN() {
        File file = GUI2App.getFileFromUser(false);
        if (file == null) {
            return;     // cancelled
        }

        try {
            new RDNJFrame(file);        // sets size and shows itself
        } catch (Exception exc) {
            log.error("error showning RDN", exc);
            JOptionPane.showMessageDialog(null, exc);
        }
    }

    /**
     * Prompts the user for a saved RPT model (XML file) then shows it in the
     * RPT tree viewer.
     */
    public void graphRPT() {
        File file = GUI2App.getFileFromUser(false);
        if (file == null) {
            return;     // cancelled
        }

        try {
            new kdl.prox.gui2.rptviewer2.RPTJFrame(file);        // sets size and shows itself
        } catch (Exception exc) {
            log.error("error showning RPT", exc);
            JOptionPane.showMessageDialog(null, exc);
        }
    }

    /**
     * Graphs a subgraph on a new window and thread.
     */
    public void graphSubgraph() {
        ProxURL proxURL = new ProxURL(getCurrentProxURL());
        new SubgraphJFrame(proxURL);
    }

    private void makeActions() {
        backAction = new AbstractAction("Back") {
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        };
        backAction.putValue(Action.SHORT_DESCRIPTION, "Go To Previous Page");
        graphRDNAction = new AbstractAction("Graph RDN...") {
            public void actionPerformed(ActionEvent e) {
                graphRDN();
            }
        };
        editQueryAction = new AbstractAction("Edit Query...") {
            public void actionPerformed(ActionEvent e) {
                editQuery(true);
            }
        };
        editQueryAction.putValue(Action.SHORT_DESCRIPTION, "Edit Existing Query");
        graphRDNAction.putValue(Action.SHORT_DESCRIPTION, "Graph an RDN Model");

        graphRPTAction = new AbstractAction("Graph RPT...") {
            public void actionPerformed(ActionEvent e) {
                graphRPT();
            }
        };

        homeAction = new AbstractAction("Home") {
            public void actionPerformed(ActionEvent e) {
                goHome();
            }
        };
        homeAction.putValue(Action.SHORT_DESCRIPTION, "Go To Home (Top) Page"); // sets tooltip
        newBrowserAction = new AbstractAction("New Window") {
            public void actionPerformed(ActionEvent e) {
                gui2App.makeNewBrowserJFrame(getCurrentProxURL());
            }
        };
        newBrowserAction.putValue(Action.SHORT_DESCRIPTION, "Open New Browser Window"); // sets tooltip
        newQueryAction = new AbstractAction("New Query") {
            public void actionPerformed(ActionEvent e) {
                editQuery(false);
            }
        };
        newQueryAction.putValue(Action.SHORT_DESCRIPTION, "Create New Query");
        newBrowserAction.putValue(Action.SHORT_DESCRIPTION,
                "Open New Browser Window Showing This Page");
        openInterpreterAction = new AbstractAction("Open Interpreter") {
            public void actionPerformed(ActionEvent e) {
                openInterpreter();
            }
        };
        openInterpreterAction.putValue(Action.SHORT_DESCRIPTION, "Open Interactive Interpreter Window");
        preferencesAction = new AbstractAction("Preferences") {
            public void actionPerformed(ActionEvent e) {
                new PreferencesJFrame(guiContentGen);   // shows itself. todo call show() here instead?
            }
        };
        preferencesAction.putValue(Action.SHORT_DESCRIPTION, "Show Preferences Dialog");
        quitAction = new AbstractAction("Quit") {
            public void actionPerformed(ActionEvent e) {
                gui2App.exit();
            }
        };
        quitAction.putValue(Action.SHORT_DESCRIPTION, "Quit GUI");
        refreshAction = new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        refreshAction.putValue(Action.SHORT_DESCRIPTION,
                "Redisplay This Page's Contents");
        runQueryAction = new AbstractAction("Run Query...") {
            public void actionPerformed(ActionEvent e) {
                runQuery(null);
            }
        };
        runQueryAction.putValue(Action.SHORT_DESCRIPTION, "Run Existing Query");
        runScriptAction = new AbstractAction("Run Script...") {
            public void actionPerformed(ActionEvent e) {
                runScript();
            }
        };
        runScriptAction.putValue(Action.SHORT_DESCRIPTION, "Run Existing Script File");
        urlChangeAction = new AbstractAction("Set URL") {
            public void actionPerformed(ActionEvent e) {
                JTextField urlTextField = (JTextField) e.getSource();
                goTo(urlTextField.getText());
            }
        };
        viewFileAction = new AbstractAction("View File ...") {
            public void actionPerformed(ActionEvent e) {
                viewFile();
            }
        };
        viewFileAction.putValue(Action.SHORT_DESCRIPTION, "View File Text");
    }

    private JEditorPane makeJEditorPane() {
        final JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setContentType("text/html");
        jEditorPane.setEditable(false);
        jEditorPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hlEvent) {
                String description = hlEvent.getDescription();
                ProxAction proxAction = null;
                if (description.startsWith(ACTION_URL_PATTERN)) {
                    String actionIDString = description.substring(ACTION_URL_PATTERN.length());
                    int actionID = Integer.parseInt(actionIDString);
                    if (actionID < pageActionsList.size()) {    // needed for strange Swing timing issue
                        proxAction = (ProxAction) pageActionsList.get(actionID);
                    }
                }

                try {
                    if (hlEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (proxAction != null) {
                            proxAction.performAction(BrowserJFrame.this);
                        } else {
                            goTo(description);
                        }
                    } else if (hlEvent.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                        if (proxAction != null) {
                            jEditorPane.setToolTipText(proxAction.getToolTipText());
                        } else {
                            jEditorPane.setToolTipText(description);
                        }
                    } else if (hlEvent.getEventType() == HyperlinkEvent.EventType.EXITED) {
                        jEditorPane.setToolTipText(null);
                    }
                } catch (Exception exc) {
                    log.error("error processing link", exc);
                    JOptionPane.showMessageDialog(null, exc);
                }
            }
        });
        return jEditorPane;
    }

    private JMenuBar makeJMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(makeFileJMenu());
        jMenuBar.add(makeQueryJMenu());
        jMenuBar.add(makeModelJMenu());
        jMenuBar.add(makeScriptJMenu());
        return jMenuBar;
    }

    private JToolBar makeJToolBar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.add(backAction);
        jToolBar.add(homeAction);
        jToolBar.add(refreshAction);
        jToolBar.setFloatable(false);
        return jToolBar;
    }

    private JPanel makeJURLPane() {
        JPanel URLBarPane = new JPanel();
        URLBarPane.setLayout(new BorderLayout());
        jURLField = new JTextField();
        jURLField.setAction(urlChangeAction);
        JLabel jURLLabel = new JLabel("Location  ");
        URLBarPane.add(jURLLabel, BorderLayout.LINE_START);
        URLBarPane.add(jURLField, BorderLayout.CENTER);
        return URLBarPane;
    }


    private JMenu makeFileJMenu() {
        JMenu jMenu = new JMenu("File");
        jMenu.add(newBrowserAction);
        jMenu.add(viewFileAction);
        jMenu.add(preferencesAction);
        jMenu.add(new JSeparator());    // ----
        jMenu.add(quitAction);
        return jMenu;
    }

    private JMenu makeModelJMenu() {
        JMenu jMenu = new JMenu("Model");
        jMenu.add(graphRPTAction);
        jMenu.add(graphRDNAction);
        return jMenu;
    }

    private JPanel makeNavigationBarPane() {
        JPanel navigationBarPane = new JPanel();
        navigationBarPane.setLayout(new GridLayout(2, 1));
        JToolBar jToolBar = makeJToolBar();
        JPanel jURLPane = makeJURLPane();
        navigationBarPane.add(jToolBar);
        navigationBarPane.add(jURLPane);
        return navigationBarPane;
    }

    private JMenu makeQueryJMenu() {
        JMenu jMenu = new JMenu("Query");
        jMenu.add(newQueryAction);
        jMenu.add(editQueryAction);
        jMenu.add(runQueryAction);
        return jMenu;
    }

    private JMenu makeScriptJMenu() {
        JMenu jMenu = new JMenu("Script");
        jMenu.add(runScriptAction);
        jMenu.add(openInterpreterAction);
        return jMenu;
    }

    public InterpreterJFrame openInterpreter() {
        InterpreterJFrame inter = new InterpreterJFrame();
        inter.setSize(inter.getPreferredSize());
        inter.setVisible(true);
        int x = (int) this.getSize().getWidth();
        int y = (int) this.getSize().getHeight();
        inter.setLocation(x, y);
        return inter;
    }


    /**
     * Redisplays my HTML without changing my urlStack.
     */
    private void refresh() {
        String currentProxURL = getCurrentProxURL();
        if (currentProxURL == null) {
            currentProxURL = "db:/";
        }
        showPage(currentProxURL);
    }

    public void runQuery() {
        Container container = null;
        String url = getCurrentProxURL();
        if (url != null) {
            ProxURL contProxURL = new ProxURL(url);
            container = contProxURL.getContainer(false);
        }
        runQuery(container);
    }

    private void runQuery(Container inputContainer) {
        File file = GUI2App.getFileFromUser(false);  // todo pass '*.xml' filter?
        if (file == null) {
            return;     // cancelled
        }

        String containerName = GUI2App.getContainerFromUser();
        if (containerName == null) {
            return;
        }

        Spin.setDefaultOffEvaluator(new ListenerSpinOver());  // Automatically spin-over all listeners
        RunFileBean runScriptBean = new RunQueryImpl(file,
                inputContainer, containerName);
        RunFileBean runScriptBeanOff = (RunFileBean) Spin.off(runScriptBean);
        new RunFileJFrame(runScriptBeanOff);    // shows itself. todo call show() here instead?
    }

    /**
     * Starts a schema analysis on my proxDB in a separate window and thread.
     */
    public void runSchemaAnalysis() {
        new SchemaAnalysisJFrame();   // shows itself. todo call show() here instead?
    }

    public void runScript() {
        File file = GUI2App.getFileFromUser(false);  // todo pass '*.py' filter?
        if (file == null) {
            return;     // cancelled
        }

        Spin.setDefaultOffEvaluator(new ListenerSpinOver());  // Automatically spin-over all listeners
        RunFileBean runScriptBean = new RunScriptImpl(file);
        RunFileBean runScriptBeanOff = (RunFileBean) Spin.off(runScriptBean);
        new RunFileJFrame(runScriptBeanOff);    // shows itself. todo call show() here instead?
    }

    /**
     * Finds the query attribute sometimes saved with a container, and opens the Query Editor with it.
     */
    public void showContainerQuery() {
        ProxURL proxURL = new ProxURL(getCurrentProxURL());
        Container container = proxURL.getContainer(false);
        String queryXML = container.getQuery();
        if (queryXML != null) {
            try {
                new QGraphEditorJFrame(QueryXMLUtil.graphQueryEleFromXML(queryXML));  // sets size and shows itself
            } catch (Exception exc) {
                log.error("error showing query", exc);
                JOptionPane.showMessageDialog(null, exc.getMessage(),
                        "Error Showing Page", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void showPage(String proxURL) {
        try {
            BrowserJFrame.showWaitCursor(this, true);
            pageActionsList = guiContentGen.getActionsForURL(proxURL); // save the list of actions for when users click
            String locationHTML = guiContentGen.getLocationHTMLForURL(proxURL);
            String actionsHTML = guiContentGen.getActionsHTML(pageActionsList);
            String bodyHTML = guiContentGen.getBodyHTMLForURL(proxURL);
            String pageHTML = "<html><body>\n" +
                    locationHTML + "\n" +
                    actionsHTML +
                    (actionsHTML.trim().length() > 0 ? "<p>\n" : "\n") +
                    bodyHTML + "\n" +
                    "</body></html>";
            jEditorPane.setText(pageHTML);
            jEditorPane.select(0, 0);   // scroll to top (for long lists)
            jURLField.setText(proxURL);
        } catch (Exception exc) {
            log.error("error showing page", exc);
            JOptionPane.showMessageDialog(null, exc.getMessage(),
                    "Error Showing Page", JOptionPane.WARNING_MESSAGE);
        } finally {
            BrowserJFrame.showWaitCursor(this, false);
        }
    }

    /**
     * Shows either the wait cursor or the default cursor, depending on isShow.
     *
     * @param component
     * @param isShow
     */
    public static void showWaitCursor(Component component, boolean isShow) {
        // use the invisible glasspane so that the mouse doesn't have to be
        // above the particular componet to change
        Assert.notNull(component, "null component");
        Cursor cursor = Cursor.getPredefinedCursor(isShow ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR);
        Component glassPane = SwingUtilities.getRootPane(component).getGlassPane();
        glassPane.setVisible(isShow);
        glassPane.setCursor(cursor);
    }

    public void viewFile() {
        File file = GUI2App.getFileFromUser(false);
        if (file == null) {
            return;     // cancelled
        }
        new ViewFileJFrame(file);   // shows itself. todo call show() here instead?
    }

    private void updateActions() {
        homeAction.setEnabled(!urlStack.empty());
        backAction.setEnabled(!urlStack.empty());
    }

}
