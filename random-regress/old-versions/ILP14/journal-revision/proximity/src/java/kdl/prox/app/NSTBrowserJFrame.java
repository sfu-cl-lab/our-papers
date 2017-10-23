/**
 * $Id: NSTBrowserJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.app;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTColumn;
import kdl.prox.gui2.BrowserJFrame;
import kdl.prox.gui2.Pager;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * NSTBrowserJFrame is a developer-oriented GUI that allows browsing NSTs,
 * starting from the top-level ('home') ones. NST rows are shown in a JTable.
 * Double-clicking a cell containing the name of an NST goes to NST. Some
 * standard WWW-like toolbar buttons are supported: 'Home', 'Refresh', 'Back'.
 * <p/>
 * todo use the 'Java look and feel graphics respository' for icons: http://developer.java.sun.com/developer/techDocs/hi/repository/ (see http://java.sun.com/docs/books/tutorial/uiswing/components/toolbar.html)
 */
public class NSTBrowserJFrame extends JFrame {

    private static final Logger log = Logger.getLogger(NSTBrowserJFrame.class);
    private static final int MAX_NUM_ROWS = 200;    // limits number of rows. useful for large tables (i.e., IMDB's links)

    /**
     * A List of NST instances, the last of which is the current one. Empty if
     * at the top-level home NSTs. Currently acts like a stack, i.e., users
     * can't go forward, only back.
     */
    private List nstList = new ArrayList();
    private NST filteredNST; // null unless filter is called. Cleared when filterBar clerned

    /**
     * The JTable that shows the current NST's contents. Its model is managed
     * by navigation commands.
     */
    private JTable nstJTable;
    private NSTBrowserModel nstTableModel;

    private Action clearFilterAction;
    private Action exportAction;
    private Action filterAction;
    private Action goBackAction;
    private Action goHomeAction;
    private Action nextAction;
    private Action prevAction;
    private Action refreshAction;

    private JTextField filterField;
    private JLabel statusBar;

    /**
     * One-arg constructor that displays the home NST list.
     *
     * @
     */
    public NSTBrowserJFrame() {
        super("??");    // title updated below

        // create the GUI
        makeActions();
        nstJTable = new JTable(getEmptyTableModel());   // model updated below by goToXX()
        nstJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nstJTable.setRowSelectionAllowed(false);
        nstJTable.addMouseListener(new MouseAdapter() {
            // handle selection changes and double-clicks by going to selected
            // Item. NB: we do both here for simplicity, instead of using
            // addListSelectionListener()
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    NST selectedNST = getNSTForCell(nstJTable.getSelectedRow(),
                            nstJTable.getSelectedColumn());
                    if (selectedNST != null) {
                        goToNST(selectedNST);
                    }
                }
            }
        });


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeTopBar(), BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(nstJTable), BorderLayout.CENTER);
        getContentPane().add(makeBottomBar(), BorderLayout.SOUTH);
        setSize(500, 400);
        setJMenuBar(makeJMenuBar());

        // go home (updates title)
        goHome();
    }

    public NSTBrowserJFrame(NST selectedNST) {
        this();
        goToNST(selectedNST);
    }

    private void clearFilterBar() {
        filterField.setText("");
        filteredNST = null;
    }

    private void export() {
        File file = GUI2App.getFileFromUser(true);
        if (file == null) {
            return;     // cancelled
        }
        // save
        try {
            getCurrentNST().tofile(file.getPath());
            JOptionPane.showMessageDialog(null, "NST exported to " + file.getPath());
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(null, "Error exporting NST: " + exc);
        }
    }

    private void filter(String filterText) {
        filteredNST = null;
        NST nst = getCurrentNST();
        if (filterText.length() > 0) {
            try {
                BrowserJFrame.showWaitCursor(this, true);
                nst = nst.filter(filterText);
                filteredNST = nst; // cleared in clearFilterBar
            } catch (Exception exc) {
                JOptionPane.showMessageDialog(null, "Error in filter: " + exc);
                return;
            } finally {
                BrowserJFrame.showWaitCursor(this, false);
            }
        } else {
            clearFilterBar();
        }
        if (nst != null) {
            nstTableModel = new NSTTableModel(nst);
            setTableModel(nstTableModel);
            updateStatusBar();
        }
    }

    /**
     * Returns the currently-displayed NST. Returns null if at the top-level
     * home NSTs.
     *
     * @return
     */
    private NST getCurrentNST() {
        if (nstList.size() == 0) {
            return null;
        } else if (filteredNST != null) {
            return filteredNST;
        } else {
            return (NST) nstList.get(nstList.size() - 1);
        }
    }


    /**
     * No-arg overload that passes null for message.
     *
     * @@return
     */
    static DefaultTableModel getEmptyTableModel() {
        return getEmptyTableModel(null);
    }


    /**
     * One-arg overload. JTable utility that returns a temporary model while
     * data being gathered, or when no data are available (such as when
     * initializing the GUI). message is an optional String that is the first
     * cell in the returned model. Pass null to have none.
     *
     * @@param message pass null if no message
     * @@return
     */
    static DefaultTableModel getEmptyTableModel(String message) {
        if (message != null) {
            return new DefaultTableModel(new String[][]{{message}},
                    new String[]{""});
        } else {
            return new DefaultTableModel(new String[][]{{}},
                    new String[]{});
        }
    }


    /**
     * Called by goHome(), returns a TableModel for the top-level home NSTs.
     *
     * @return
     */
    private NSTBrowserModel getHomeTableModel() {
        return new HomeTableModel();
    }

    /**
     * Called when user double-clicks a cell in nstJTable, tries to return an
     * NST corresponding to the text in the passed cell. Returns null if no
     * NST could be found.
     *
     * @param row
     * @param column
     * @return
     */
    private NST getNSTForCell(int row, int column) {
        // see if rawNSTName names a real BAT. NB: bat names can be top-level
        // proximity table names (see DB.getProxNSTNames()), or 'anonymous'
        // BAT names, of which there are two types floating around - those with
        // angle brackets in their names (e.g., "<tmp_22>"), and those without
        // (e.g., "tmp_22"). newer databases have the angle brackets, but some
        // older DBs don't (as a result of our switch from using bbpname() to
        // bat()). first we clean up rawNSTName by removing surrounding '"' and
        // '<' and '>' chars, if any
        String rawNSTName = (String) nstJTable.getValueAt(row, column);
        StringBuffer cleanNSTNameSB = new StringBuffer(rawNSTName);
        if (cleanNSTNameSB.charAt(0) == '\"') {
            cleanNSTNameSB.deleteCharAt(0);
        }
        if (cleanNSTNameSB.charAt(cleanNSTNameSB.length() - 1) == '\"') {
            cleanNSTNameSB.deleteCharAt(cleanNSTNameSB.length() - 1);
        }
        if (cleanNSTNameSB.charAt(0) == '<') {
            cleanNSTNameSB.deleteCharAt(0);
        }
        if (cleanNSTNameSB.charAt(cleanNSTNameSB.length() - 1) == '>') {
            cleanNSTNameSB.deleteCharAt(cleanNSTNameSB.length() - 1);
        }

        String cleanNSTName = cleanNSTNameSB.toString();
        boolean isBATExists = MonetUtil.isBATExists(cleanNSTName);
        if (!isBATExists) {
            return null;
        }

        // found a BAT, so try to get an NST for it, first handling special case
        // of non-NST BATs
        if (DB.isProxNSTBatName(rawNSTName)) {
            return null;
        }

        boolean isTopNST = DB.getProxNSTNames().contains(rawNSTName);
        String nstBATName = (isTopNST ? cleanNSTName : rawNSTName);
        NST nst = new NST(nstBATName);
        return nst;
    }

    public Dimension getPreferredSize() {
        return new Dimension(550, 325);
    }


    /**
     * Pops the current NST from my nstList.
     */
    private void goBack() {
        Assert.condition(!isAtHome(), "at home");
        nstList.remove(nstList.size() - 1);
        refresh();
        clearFilterBar();
        updateStatusBar();
    }


    /**
     * Sets the current NST to the home NST placeholder.
     */
    private void goHome() {
        nstList.clear();
        refresh();
        clearFilterBar();
        updateStatusBar();
    }


    /**
     * Goes to the specified nst by adding it to the end of my nstList then
     * setting my nstJTable's model to a new NSTTableModel for nst.
     *
     * @param nst
     */
    private void goToNST(NST nst) {
        try {
            nstList.add(nst);
            nstTableModel = new NSTTableModel(nst);
            setTableModel(nstTableModel);
            clearFilterBar();
            updateStatusBar();
        } catch (MonetException monExc) {
            // todo remove just-added nst from nstList?
            log.error("goToNST(): error: " + monExc);
        }
    }

    private boolean isAtHome() {
        return (nstList.size() == 0);
    }


    /**
     * Called by constructor, saves actions in IVs.
     */
    private void makeActions() {
        clearFilterAction = new AbstractAction("Remove Filter") {
            public void actionPerformed(ActionEvent e) {
                clearFilterBar();
                filter("");
            }
        };
        exportAction = new AbstractAction("Export to File") {
            public void actionPerformed(ActionEvent e) {
                export();
            }
        };
        filterAction = new AbstractAction("Filter NST") {
            public void actionPerformed(ActionEvent e) {
                JTextField filterTextField = (JTextField) e.getSource();
                filter(filterTextField.getText());
            }
        };
        goBackAction = new AbstractAction("Go Back") {
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        };
        goHomeAction = new AbstractAction("Go Home") {
            public void actionPerformed(ActionEvent e) {
                goHome();
            }
        };
        nextAction = new AbstractAction("Next Page") {
            public void actionPerformed(ActionEvent e) {
                next();
            }
        };
        prevAction = new AbstractAction("Previous Page") {
            public void actionPerformed(ActionEvent e) {
                prev();
            }
        };
        refreshAction = new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
    }


    private JPanel makeBottomBar() {
        statusBar = new JLabel();

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.add(makeNavigationBar(), BorderLayout.WEST);
        jPanel.add(statusBar, BorderLayout.CENTER);

        return jPanel;
    }

    /**
     * Called by constructor, returns the JToolBar to add to the GUI.
     *
     * @return
     */
    private JPanel makeFilterPane() {
        JPanel filterPane = new JPanel();
        filterPane.setLayout(new BorderLayout());
        filterField = new JTextField();
        filterField.setAction(filterAction);
        JLabel jFilterLabel = new JLabel("Filter  ");
        filterPane.add(jFilterLabel, BorderLayout.LINE_START);
        filterPane.add(filterField, BorderLayout.CENTER);
        filterPane.add(new JButton(clearFilterAction), BorderLayout.EAST);
        return filterPane;
    }


    private JMenuBar makeJMenuBar() {

        JMenu jFileMenu = new JMenu("File");
        jFileMenu.add(exportAction);
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(jFileMenu);
        return jMenuBar;
    }

    private JToolBar makeNavigationBar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.add(prevAction);
        jToolBar.add(nextAction);
        return jToolBar;
    }

    private JToolBar makeToolBar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.add(goBackAction);
        jToolBar.add(refreshAction);
        jToolBar.add(goHomeAction);
        return jToolBar;
    }

    private JPanel makeTopBar() {
        JPanel toolBarPane = new JPanel();
        toolBarPane.setLayout(new GridLayout(2, 1));
        toolBarPane.add(makeToolBar());
        toolBarPane.add(makeFilterPane());
        return toolBarPane;
    }


    private void next() {
        nstTableModel.nextPage();
        updateStatusBar();
        nstJTable.tableChanged(new TableModelEvent(nstJTable.getModel()));
    }

    private void prev() {
        nstTableModel.prevPage();
        updateStatusBar();
        nstJTable.tableChanged(new TableModelEvent(nstJTable.getModel()));
    }


    /**
     * Redisplays the current NST's contents.
     */
    private void refresh() {
        try {
            NST currNST = getCurrentNST();
            nstTableModel = currNST == null ? getHomeTableModel() :
                    new NSTTableModel(currNST);
            setTableModel(nstTableModel);
            updateStatusBar();
        } catch (MonetException monExc) {
            log.error("refresh(): error: " + monExc);
        }

    }


    private void setTableModel(TableModel tableModel) {
        nstJTable.setModel(tableModel);
        updateTitle();
        updateActions();
    }


    /**
     * Called by GUI when state changes, updates my actions accordingly.
     */
    private void updateActions() {
        goBackAction.setEnabled(!isAtHome());
        exportAction.setEnabled(getCurrentNST() != null);
        clearFilterAction.setEnabled(filteredNST != null);
        // goHomeAction: always enabled
        // refreshAction: always enabled
    }

    private void updateStatusBar() {
        nextAction.setEnabled(nstTableModel.hasNext());
        prevAction.setEnabled(nstTableModel.hasPrev());
        statusBar.setText("Page " + nstTableModel.getPageNum() + " of " + nstTableModel.getNumPages());
    }


    /**
     * Updates my title using the the current NST.
     */
    private void updateTitle() {
        NST currNST = getCurrentNST();
        int rowCount = (currNST == null ? 0 : currNST.getRowCount());
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);     // commas

        String prettyRowCount = numberFormat.format(rowCount);
        String limitMsg = " (" + prettyRowCount + " rows)";
        setTitle("NST: " + (currNST == null ? "Home" : currNST + "") +
                (currNST == null ? "" : limitMsg));
    }

    //
    // inner classes
    //
    abstract class NSTBrowserModel extends AbstractTableModel {
        public abstract int getPageNum();

        public abstract int getNumPages();

        public abstract boolean hasPrev();

        public abstract boolean hasNext();

        public abstract void nextPage();

        public abstract void prevPage();
    }


    /**
     * A TableModel representing the top-level home NSTs. It hsa one column
     * and six rows, one each for the six top-level home NSTs:
     * <p/>
     * prox_container
     * prox_cont_attr
     * prox_link
     * prox_link_attr
     * prox_object
     * prox_obj_attr
     * <p/>
     */
    class HomeTableModel extends NSTBrowserModel {

        // the list of top-level files
        List files;
        int pageNum;
        int numPages;

        public HomeTableModel() {
            files = DB.ls();
            int numRows = files.size();
            numPages = (numRows % MAX_NUM_ROWS == 0 ? numRows / MAX_NUM_ROWS :
                    (numRows / MAX_NUM_ROWS) + 1);
            pageNum = 1;
        }

        public int getRowCount() {
            int start = ((pageNum - 1) * MAX_NUM_ROWS);
            int end = Math.min(files.size(), start + MAX_NUM_ROWS);
            return end - start;
        }

        public int getColumnCount() {
            return 1;
        }

        public String getColumnName(int column) {
            return "name";
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getNumPages() {
            return numPages;
        }

        public Object getValueAt(int row, int column) {
            int offset = ((pageNum - 1) * MAX_NUM_ROWS);
            return files.get(offset + row);
        }

        public boolean hasPrev() {
            return pageNum > 1;
        }

        public boolean hasNext() {
            return pageNum < numPages;
        }

        public void nextPage() {
            pageNum += 1;
        }

        public void prevPage() {
            pageNum -= 1;
        }
    }


    /**
     * A TableModel of an NST's contents. Recall that an NST.getNSTColumns()
     * does not return an NSTColumn for the head OID, but NST.selectRows()
     * <B>does</B> include the head OID column.
     */
    class NSTTableModel extends NSTBrowserModel {

        private NST nst;
        private Pager pager;
        private List rows = new ArrayList();        // List of Strings, one per column


        public NSTTableModel(NST nst) {
            Assert.notNull(nst, "null nst");
            this.nst = nst;
            pager = new Pager(nst, 1, MAX_NUM_ROWS);

            readRows();

        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return nst.getNSTColumns().size() + 1;      // add head OID
        }

        public String getColumnName(int column) {
            if (column == 0) {
                return "-";     // head OID
            } else {
                NSTColumn nstColumn = nst.getNSTColumns().get(column - 1);
                return nstColumn.getName();
            }
        }

        public int getPageNum() {
            return pager.getPageNum();
        }

        public int getNumPages() {
            return pager.getNumPages();
        }

        public Object getValueAt(int row, int column) {
            List columnValList = (List) rows.get(row);
            return columnValList.get(column);
        }

        public boolean hasPrev() {
            return pager.hasPrev();
        }

        public boolean hasNext() {
            return pager.hasNext();
        }

        public void nextPage() {
            pager.next();
            readRows();
        }

        public void prevPage() {
            pager.prev();
            readRows();
        }

        private void readRows() {
            ResultSet resultSet = pager.getResultSet();
            rows.clear();
            while (resultSet.next()) {
                rows.add(resultSet.getColumnList());
            }
        }

    }


}
