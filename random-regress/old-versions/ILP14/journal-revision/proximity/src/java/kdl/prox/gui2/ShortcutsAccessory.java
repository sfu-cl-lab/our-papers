/**
 * $Id: ShortcutsAccessory.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ShortcutsAccessory.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * <code>JFileChooser</code> accessory for management of shortcuts to frequently
 * accessed directories and files. Published by JavaWorld at:
 * http://www.javaworld.com/javaworld/jw-08-2002/jw-0830-jfile.html
 * Used with permission.
 *
 * @author Slav Boleslawski
 */
public class ShortcutsAccessory extends JPanel {
    private static final int TOOLTIP_DISMISS_DELAY = 2000;
    private static final int TOOLTIP_INITIAL_DELAY = 300;

    private JFileChooser chooser;
    private String initialTitle;
    private String applicationName;
    private int originalInitialDelay;
    private int originalDismissDelay;
    private JButton addButton;
    private JButton deleteButton;
    private JButton aliasButton;
    private JList list;
    private JScrollPane listScrollPane;
    private JTextField aliasField;
    private DefaultListModel model;
    private boolean shortcutsChanged;


    public ShortcutsAccessory(JFileChooser chooser, String applicationName) {
        super();
        this.chooser = chooser;
        this.applicationName = applicationName;
        updateTitle();
        setGUI();
        addListeners();
    }

    /**
     * Creates GUI for this accessory.
     */
    private void setGUI() {
        setBorder(new TitledBorder(" Shortcuts "));
        setLayout(new BorderLayout());

        model = createModel();
        list = new JList(model) {
            public String getToolTipText(MouseEvent me) {
                if (model.size() == 0)
                    return null;

                Point p = me.getPoint();
                Rectangle bounds = list.getCellBounds(model.size() - 1,
                        model.size() - 1);
                int lastElementBaseline = bounds.y + bounds.height;
                //Is the mouse pointer below the last element in the list?
                if (lastElementBaseline < p.y)
                    return null;

                int index = list.locationToIndex(p);
                if (index == -1) // for compatibility with Java 1.3 and earlier versions
                    return null;

                Shortcut shortcut = (Shortcut) model.get(index);
                String path = shortcut.getPath();
                if (shortcut.hasAlias())
                    return path;

                FontMetrics fm = list.getFontMetrics(list.getFont());
                int textWidth = SwingUtilities.computeStringWidth(fm, path);
                if (textWidth <= listScrollPane.getSize().width)
                    return null;

                return path;
            }
        };
        list.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                                                          Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Shortcut shortcut = (Shortcut) value;
                String name = shortcut.getDisplayName();
                JLabel label = new JLabel(name);
                label.setBorder(new EmptyBorder(0, 3, 0, 3));
                label.setOpaque(true);
                if (!isSelected) {
                    label.setBackground(list.getBackground());
                    label.setForeground(shortcut.getColor());
                } else {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                return label;
            }
        });
        listScrollPane = new JScrollPane(list);

        originalInitialDelay = ToolTipManager.sharedInstance().getInitialDelay();
        originalDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);
        ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY);
        ToolTipManager.sharedInstance().registerComponent(list);

        add(listScrollPane, BorderLayout.CENTER);
        JPanel southPanel = new JPanel();
        southPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));
        addButton = new JButton("Add");
        addButton.setToolTipText("Add the current directory/file to Shortcuts");
        deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete a shortcut");
        aliasButton = new JButton("Set");
        aliasButton.setToolTipText("Set an alias for a shortcut");
        southPanel.add(Box.createHorizontalGlue());
        southPanel.add(addButton);
        southPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        southPanel.add(deleteButton);
        southPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        southPanel.add(new JLabel("  Alias:"));
        southPanel.add(Box.createRigidArea(new Dimension(2, 0)));
        aliasField = new JTextField(10);
        aliasField.setToolTipText("Shortcut for selection. Format: " +
                "[<color>#]<name>, e.g., 'docs', 'red#docs'");
        aliasField.setMaximumSize(aliasField.getPreferredSize());
        southPanel.add(aliasField);
        southPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        southPanel.add(aliasButton);
        southPanel.add(Box.createHorizontalGlue());
        add(southPanel, BorderLayout.SOUTH);

        int southPanelWidth = southPanel.getPreferredSize().width;
        Dimension size = new Dimension(southPanelWidth, 0);
        //Makes sure the accessory is not resized with addition of entries
        //longer than the current accessory width.
        setPreferredSize(size);
        setMaximumSize(size);
    }

    /**
     * Adds all listeners required by this accessory.
     */

    private void addListeners() {
        //Updates chooser's title
        chooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                if (propertyName.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY))
                    updateTitle();
            }
        });
        //Saves shortcuts when the chooser is disposed
        chooser.addAncestorListener(new AncestorListener() {
            public void ancestorRemoved(AncestorEvent e) {
                ToolTipManager.sharedInstance().setDismissDelay(originalDismissDelay);
                ToolTipManager.sharedInstance().setInitialDelay(originalInitialDelay);
                if (shortcutsChanged)
                    saveShortcuts();
            }

            public void ancestorAdded(AncestorEvent e) {
            }

            public void ancestorMoved(AncestorEvent e) {
            }
        });
        //Sets chooser's current directory or file and updates the Alias field
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex == -1)
                    return;

                Shortcut shortcut = (Shortcut) model.get(selectedIndex);
                String alias = shortcut.getAlias();
                String path = shortcut.getPath();
                String color = shortcut.getColorString();

                String aliasText = alias;
                if (!color.equals("black"))
                    aliasText = color + '#' + alias;
                aliasField.setText(aliasText);
                File file = new File(path);
                if (file.isFile())
                    chooser.setSelectedFile(file);
                else {
                    chooser.setCurrentDirectory(file);
                    chooser.setSelectedFile(null);
                }
            }
        });
        //Adds/deletes/edits a shortcut
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String command = ae.getActionCommand();
                if (command.equals("Delete")) {
                    int ind = list.getSelectedIndex();
                    if (ind == -1)
                        return;

                    aliasField.setText("");
                    model.remove(ind);
                }
                if (command.equals("Add")) {
                    String path;
                    File file = chooser.getSelectedFile();
                    if (file != null)
                        path = file.getAbsolutePath();
                    else {
                        File dir = chooser.getCurrentDirectory();
                        path = dir.getAbsolutePath();
                    }
                    insertShortcut(new Shortcut("", path, "black"));
                }
                if (command.equals("Set"))
                    setAlias();
                list.clearSelection();
                chooser.setSelectedFile(null);
                shortcutsChanged = true;
            }
        };
        addButton.addActionListener(actionListener);
        deleteButton.addActionListener(actionListener);
        aliasButton.addActionListener(actionListener);
        aliasField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    setAlias();
                    shortcutsChanged = true;
                }
            }
        });
    }

    /**
     * Creates/edits/deletes an alias for a shortcut.
     */
    private void setAlias() {
        int ind = list.getSelectedIndex();
        if (ind == -1) {
            list.requestFocus();
            return;
        }

        Shortcut shortcut = (Shortcut) model.get(ind);
        String text = aliasField.getText().trim();
        if (text.length() == 0) { //alias removed
            shortcut.setAlias("");
            shortcut.setColor("black");
            model.remove(ind);
            insertShortcut(new Shortcut("", shortcut.getPath(), ""));
            return;
        }

        String color = "black";
        String alias = text;
        int hashIndex = text.indexOf("#");
        if (hashIndex != -1) {
            alias = text.substring(hashIndex + 1);
            color = text.substring(0, hashIndex);
        }
        shortcut.setAlias(alias);
        shortcut.setColor(color);
        aliasField.setText("");
        model.remove(ind);
        insertShortcut(new Shortcut(alias, shortcut.getPath(), color));
    }

    /**
     * Inserts a new shortcut into the list so that list's alphabetical order
     * is preserved.
     */

    private void insertShortcut(Shortcut newShortcut) {
        if (model.getSize() == 0) {
            model.addElement(newShortcut);
            return;
        }

        //Checks if newShortcut already exists
        for (int i = 0; i < model.getSize(); i++) {
            Shortcut shortcut = (Shortcut) model.get(i);
            if (shortcut.getPath().equalsIgnoreCase(newShortcut.getPath()))
                return;
        }

        int insertIndex = 0;
        String newName = newShortcut.getName();
        for (int i = 0; i < model.getSize(); i++) {
            Shortcut shortcut = (Shortcut) model.get(i);
            String name = shortcut.getName();
            if (name.compareToIgnoreCase(newName) <= 0)
                insertIndex = i + 1;
            else
                break;
        }
        model.insertElementAt(newShortcut, insertIndex);
    }

    /**
     * Creates a DefaultListModel and populates it with shortcuts read from a file
     * in user's home directory.
     */
    private DefaultListModel createModel() {
        DefaultListModel listModel = new DefaultListModel();
        try {
            String filePath = System.getProperty("user.home") +
                    System.getProperty("file.separator") + applicationName +
                    ".accessory.dirs";
            File file = new File(filePath);
            if (!file.exists())
                return listModel;

            BufferedReader in = new BufferedReader(new FileReader(file));
            String buf = null;
            while ((buf = in.readLine()) != null) {
                if (buf.startsWith("//"))	// Ignores lines with comments
                    continue;
                int commaIndex = buf.indexOf(",");
                if (commaIndex == -1)
                    throw new IOException("Incorrect format of a " +
                            file.getPath() + " file");
                String colorAndAlias = buf.substring(0, commaIndex).trim();
                String alias;
                String color;
                int hashIndex = colorAndAlias.indexOf("#");
                if (hashIndex != -1) {
                    alias = colorAndAlias.substring(hashIndex + 1);
                    color = colorAndAlias.substring(0, hashIndex);
                } else {
                    alias = colorAndAlias;
                    color = "black";
                }
                String path = buf.substring(commaIndex + 1).trim();
                Shortcut shortcut = new Shortcut(alias, path, color);
                listModel.addElement(shortcut);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return listModel;
    }

    /**
     * Saves the shortcuts list to a file in user's home directory.
     */
    private void saveShortcuts() {
        try {
            String filePath = System.getProperty("user.home") +
                    System.getProperty("file.separator") + applicationName +
                    ".accessory.dirs";
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
            out.println("//Directory Shortcuts for " + applicationName +
                    " [" + new Date().toString() + ']');
            for (int i = 0; i < model.size(); i++) {
                Shortcut shortcut = (Shortcut) model.get(i);
                String alias = shortcut.getAlias();
                String path = shortcut.getPath();
                String color = shortcut.getColorString();
                out.println(color + '#' + alias + ',' + path);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Displays the current directory path in the title bar of JFileChooser.
     */
    private void updateTitle() {
        if (initialTitle == null)
        //chooser.getDialogTitle() returns null, so the title is retrieved from UI
            initialTitle = chooser.getUI().getDialogTitle(chooser);
        chooser.setDialogTitle(initialTitle + " (" +
                chooser.getCurrentDirectory().getPath() + ")");
    }

    public static void main(String[] args) {
        try {
//            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null,
                "After JFileChooser starts, please click Add,\n" +
                "then select a new shortcut and type red#shortcut in the Alias field\n" +
                "and press Enter or click Set to see how to set an alias and color.",
                "Demo of ShortcutsAccessory", JOptionPane.INFORMATION_MESSAGE);
        JFileChooser chooser = new JFileChooser();
        ShortcutsAccessory shortcuts = new ShortcutsAccessory(chooser, "demo");
        chooser.setAccessory(shortcuts);
        Dimension d = new Dimension(700, 400);
        chooser.setMinimumSize(d);
        chooser.setPreferredSize(d);
        int resultType = chooser.showOpenDialog(null);
        if (resultType == JFileChooser.APPROVE_OPTION) {
            System.out.println("user approved: " + chooser.getSelectedFile());
        } else {
            System.out.println("user didn't approve");
        }
        System.exit(0);
    }
}

/**
 * This class defines a shortcut object in the list.
 */

class Shortcut {
    private String alias;
    private String path;
    private Color color;

    public Shortcut(String alias, String path, String color) {
        this.alias = alias;
        this.path = path;
        this.color = parseColor(color);
    }

    public boolean hasAlias() {
        return (alias.length() > 0);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String newAlias) {
        alias = newAlias;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        if (hasAlias())
            return alias;
        return path;
    }

    /**
     * Formats shortcut's name for display.
     * <p/>
     * This method can be modified to meet other display format expectations.
     */

    public String getDisplayName() {
        if (hasAlias())
            return '[' + alias + ']';
        return path;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = parseColor(color);
    }

    public String getColorString() {
        return colorToString(color);
    }

    /**
     * Converts color to string.
     * <p/>
     * Some colors defined in Color are used as is (for instance, Color.blue).
     * Green, teal and yellow colors are defined in this method.
     * Other colors are represented as an RGB hexadecimal string (without
     * the alpha component).
     */

    private String colorToString(Color color) {
        if (color == Color.blue)
            return "blue";
        else if (color == Color.cyan)
            return "cyan";
        else if (color == Color.gray)
            return "gray";
        else if (color == Color.magenta)
            return "magenta";
        else if (color == Color.orange)
            return "orange";
        else if (color == Color.pink)
            return "pink";
        else if (color == Color.red)
            return "red";
        else if (color == Color.black)
            return "black";
        String fullColorStr = Integer.toHexString(color.getRGB());
        //The first two digits in fullColorStr are ignored in colorStr (alpha component)
        String colorStr = fullColorStr.substring(2);
        if (colorStr.equals("339933"))
            return "green";
        else if (colorStr.equals("cccc33"))
            return "yellow";
        else if (colorStr.equals("66cc99"))
            return "teal";
        return colorStr;
    }

    private Color parseColor(String colorString) {
        try {
            int rgb = Integer.parseInt(colorString, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) {
        }
        if (colorString.equals("blue"))
            return Color.blue;
        if (colorString.equals("cyan"))
            return Color.cyan;
        if (colorString.equals("gray"))
            return Color.gray;
        if (colorString.equals("green"))
            return new Color(0x33, 0x99, 0x33);
        if (colorString.equals("magenta"))
            return Color.magenta;
        if (colorString.equals("orange"))
            return Color.orange;
        if (colorString.equals("pink"))
            return Color.pink;
        if (colorString.equals("red"))
            return Color.red;
        if (colorString.equals("teal"))
            return new Color(0x66, 0xcc, 0x99);
        if (colorString.equals("yellow"))
            return new Color(0xcc, 0xcc, 0x33);
        return Color.black;
    }

    public String toString() {
        return "[" + alias + "," + path + "," + colorToString(color) + "]";
    }
}
