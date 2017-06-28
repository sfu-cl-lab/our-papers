/**
 * $Id: PreferencesJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Panel to set preferences for the GUI.
 * Allows user to choose attributes to use as labels in place of object and link IDs
 */
public class PreferencesJFrame extends JFrame {

    private static Logger log = Logger.getLogger(PreferencesJFrame.class);

    private GUIContentGenerator guiContentGenerator;
    private JComboBox objectJComboBox;
    private JComboBox linkJComboBox;
    private Action okAction;
    private Action cancelAction;

    public static final String NO_LABEL_STRING = "--no label--";


    public PreferencesJFrame(GUIContentGenerator guiContentGenerator) {
        super("Preferences for " + DB.description());
        this.guiContentGenerator = guiContentGenerator;
        makeActions();
        getContentPane().setBackground(Color.white);
        getContentPane().add(makeAttributeInputJPanel(), BorderLayout.NORTH);   // add to NORTH instead of CENTER so that the form is packed nicely
        pack();
        setVisible(true);
    }

    private void cancel() {
        setVisible(false);
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 400);
    }

    private void makeActions() {
        okAction = new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                savePreferences();
            }
        };
        cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
    }

    private JPanel makePreferencesActionJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.LINE_AXIS));
        jPanel.add(new JButton(okAction));
        jPanel.add(new JButton(cancelAction));
        jPanel.setBackground(Color.white);
        return jPanel;
    }

    private JPanel makeAttributeChooser(boolean isObject) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.LINE_AXIS));
        jPanel.add(new JLabel(isObject ? "Object:" : "Link:"));
        JComboBox jComboBox = new JComboBox();
        jComboBox.setBackground(Color.white);
        if (isObject) {
            objectJComboBox = jComboBox;
        } else {
            linkJComboBox = jComboBox;
        }
        jComboBox.addItem(NO_LABEL_STRING);
        Attributes attributes = (isObject ? DB.getObjectAttrs() :
                DB.getLinkAttrs());
        List attrNames = attributes.getAttributesOfType(DataTypeEnum.STR);  // todo slow!
        for (int nameIdx = 0; nameIdx < attrNames.size(); nameIdx++) {
            String attrName = (String) attrNames.get(nameIdx);
            jComboBox.addItem(attrName);
        }
        String selectedAttr = (isObject ?
                guiContentGenerator.getLabelAttributeForObjects() :
                guiContentGenerator.getLabelAttributeForLinks());
        if (selectedAttr != null) {
            jComboBox.setSelectedItem(selectedAttr);
        }
        jPanel.setBackground(Color.white);
        jPanel.add(jComboBox);
        return jPanel;
    }

    /**
     * @return top-level panel for step 1/3
     */
    private JPanel makeAttributeInputJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
        jPanel.add(makeLabelJPanel());
        jPanel.add(makeAttributeChooser(true));
        jPanel.add(makeAttributeChooser(false));
        jPanel.add(makePreferencesActionJPanel());
        jPanel.setBackground(Color.white);
        return jPanel;
    }

    private JPanel makeLabelJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.LINE_AXIS));
        jPanel.setBackground(Color.white);
        JLabel jLabel = new JLabel("Choose Attribute to use as labels for Objects and Links:");
        jLabel.setBackground(Color.white);
        jPanel.add(jLabel);
        jPanel.add(Box.createHorizontalGlue());
        return jPanel;
    }

    private void savePreferences() {
        String objAttrName = (String) objectJComboBox.getSelectedItem();
        String linkAttrName = (String) linkJComboBox.getSelectedItem();
        guiContentGenerator.setLabelAttributeForObjects(objAttrName);
        guiContentGenerator.setLabelAttributeForLinks(linkAttrName);
        setVisible(false);
    }

}
