/**
 * $Id: AttrCreationJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import kdl.prox.db.Attributes;
import kdl.prox.script.AddAttribute;
import org.apache.log4j.Logger;

/**
 * Creates an attribute with the given expression
 */
public class AttrCreationJFrame extends JFrame {

    private static Logger log = Logger.getLogger(AttrCreationJFrame.class);

    private Attributes attrs;
    private JTextField attrNameTextField;
    private JTextArea attrFunctionTextArea;
    private JCheckBox attrIsInsertCheckBox;
    private Action createAction;
    private Action cancelAction;


    public AttrCreationJFrame(Attributes attrs) {
        super("Create New Attribute");
        this.attrs = attrs;

        makeActions();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeAttrSpecsPanel(), BorderLayout.CENTER);
        getContentPane().add(makeButtonsPanel(), BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    private void cancel() {
        setVisible(false);
    }

    private void create() {
        String name = attrNameTextField.getText().trim();
        String function = attrFunctionTextArea.getText().trim().replaceAll("\r?\n", " ");
        AddAttribute addAttribute = new AddAttribute(attrIsInsertCheckBox.isSelected());
        BrowserJFrame.showWaitCursor(this, true);
        try {
            addAttribute.addAttribute(attrs, name, function);
            JOptionPane.showMessageDialog(null, "Attribute created");
            setVisible(false);
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(null, "Error creating attribute: " + exc);
        }
        BrowserJFrame.showWaitCursor(this, false);
    }

    private void makeActions() {
        createAction = new AbstractAction("Create Attribute") {
            public void actionPerformed(ActionEvent e) {
                create();
            }
        };
        cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
    }

    private JPanel makeAttrSpecsPanel() {
        JPanel attrSpecsPanel = new JPanel();
        attrSpecsPanel.setLayout(new SpringLayout());

        attrNameTextField = new JTextField();
        attrFunctionTextArea = new JTextArea(4, 50);
        attrIsInsertCheckBox = new JCheckBox("Insert new values if attribute already exists", true);

        attrSpecsPanel.add(new JLabel("Attr Name: "));
        attrSpecsPanel.add(attrNameTextField);
        attrSpecsPanel.add(new JLabel("Function: "));
        attrSpecsPanel.add(attrFunctionTextArea);
        attrSpecsPanel.add(new JLabel("Behavior: "));
        attrSpecsPanel.add(attrIsInsertCheckBox);

        SpringUtilities.makeCompactGrid(attrSpecsPanel,
                3, 2, //rows, cols
                6, 6,        //initX, initY
                6, 6);       //xPad, yPad
        return attrSpecsPanel;
    }

    private JPanel makeButtonsPanel() {
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));

        buttonsPanel.add(new JButton(createAction));
        buttonsPanel.add(new JButton(cancelAction));
        return buttonsPanel;
    }

}
