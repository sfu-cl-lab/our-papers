/**
 * $Id: SchemaAnalysisJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.app.GUI2App;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.db.SchemaAnalysis;
import org.apache.log4j.Logger;
import spin.Spin;
import spin.demo.Assert;
import spin.off.ListenerSpinOver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Performs a schema analysis of the entire database in three steps, each with
 * a different content pane:
 * <pre>
 *  1. Get object and link type attribute names.
 *  2. Perform analysis in separate thread, showing output as it happens.
 *  3. Show HTML results.
 * </pre>
 */
public class SchemaAnalysisJFrame extends JFrame implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(SchemaAnalysisJFrame.class);

    private JComboBox objectJComboBox;
    private JComboBox linkJComboBox;
    private JTextArea statusJTextArea;
    private JEditorPane jEditorPane;
    private Action analyzeAction;
    private Action cancelAction;
    private Action saveAction;


    public SchemaAnalysisJFrame() {
        super("Schema Analysis of " + DB.description());
        makeActions();
        getContentPane().setBackground(Color.white);
        getContentPane().add(makeAttributeInputJPanel(), BorderLayout.NORTH);   // add to NORTH instead of CENTER so that the form is packed nicely
        pack();
        setVisible(true);
    }

    private void analyze() {
        Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());
        statusJTextArea = new JTextArea();
        statusJTextArea.setEditable(false);
        contentPane.add(new JScrollPane(statusJTextArea), BorderLayout.CENTER);
        pack();

        String objAttrName = (String) objectJComboBox.getSelectedItem();
        String linkAttrName = (String) linkJComboBox.getSelectedItem();
        SchemaAnalysis schemaAnalLE = new SchemaAnalysis(objAttrName, linkAttrName);

        Spin.setDefaultOffEvaluator(new ListenerSpinOver());  // Automatically spin-over all listeners
        RunFileBean runSchemaAnalysisBean = new RunSchemaAnalysisImpl(schemaAnalLE);
        RunFileBean runSchemaAnalysisBeanOff = (RunFileBean) Spin.off(runSchemaAnalysisBean);

        runSchemaAnalysisBeanOff.addPropertyChangeListener(this);  // ListenerSpinOver will spin-over us automatically
        try {
            runSchemaAnalysisBeanOff.start();
        } catch (Exception exc) {
            StringWriter stringWriter = new StringWriter();
            exc.printStackTrace(new PrintWriter(stringWriter));
            statusJTextArea.setText("There was an error running the file:\n" +
                    "    " + runSchemaAnalysisBeanOff.getInputObjectString() + "\n\n" +
                    stringWriter.toString());
        }
    }

    private void cancel() {
        setVisible(false);
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 400);
    }

    private void makeActions() {
        analyzeAction = new AbstractAction("Analyze") {
            public void actionPerformed(ActionEvent e) {
                analyze();
            }
        };
        cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
        saveAction = new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                saveHTML();
            }
        };
    }

    private JPanel makeAnalyzeActionJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.LINE_AXIS));
        jPanel.add(new JButton(analyzeAction));
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
        Attributes attributes = (isObject ? DB.getObjectAttrs() :
                DB.getLinkAttrs());
        List attrNames = attributes.getAttributeNames();
        Collections.sort(attrNames);
        for (int nameIdx = 0; nameIdx < attrNames.size(); nameIdx++) {
            String attrName = (String) attrNames.get(nameIdx);
            jComboBox.addItem(attrName);
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
        jPanel.add(makeAnalyzeActionJPanel());
        jPanel.setBackground(Color.white);
        return jPanel;
    }

    private JPanel makeLabelJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.LINE_AXIS));
        jPanel.setBackground(Color.white);
        JLabel jLabel = new JLabel("Choose Attributes for Object and Link Types:");
        jLabel.setBackground(Color.white);
        jPanel.add(jLabel);
        jPanel.add(Box.createHorizontalGlue());
        return jPanel;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Assert.onEDT();
        String propertyName = evt.getPropertyName();
        if ("status".equals(propertyName)) {
            String status = (String) evt.getNewValue();
            statusJTextArea.append("Status: " + status + "\n");
        } else if ("output".equals(propertyName)) {
            String output = (String) evt.getNewValue();
            statusJTextArea.append(output);
        } else if ("schemaHTML".equals(propertyName)) {
            showSchemaHTML((String) evt.getNewValue());
        } else {
            log.warn("unknown property name: '" + propertyName + "'");
        }
    }

    private void saveHTML() {
        File file = GUI2App.getFileFromUser(true);
        if (file == null) {
            return;     // cancelled
        }

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(jEditorPane.getText());
            JOptionPane.showMessageDialog(this, "File Saved");
        } catch (IOException ioExc) {
            JOptionPane.showMessageDialog(this, ioExc.toString(),
                    "Error Saving HTML", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException ioExc) {
                    // ignore
                }
            }
        }
    }

    private void showSchemaHTML(String schemaHTML) {
        Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());
        jEditorPane = new JEditorPane();
        jEditorPane.setContentType("text/html");
        jEditorPane.setEditable(false);
        jEditorPane.setText(schemaHTML);
        jEditorPane.select(0, 0);   // scroll to top

        JToolBar jToolbar = new JToolBar(JToolBar.HORIZONTAL);
        jToolbar.setFloatable(false);
        jToolbar.add(saveAction);
        contentPane.add(jToolbar, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(jEditorPane), BorderLayout.CENTER);
        pack();
    }

}
