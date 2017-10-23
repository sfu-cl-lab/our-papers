/**
 * $Id: RunFileJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RunFileJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import org.apache.log4j.Logger;
import spin.demo.Assert;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * A JFrame that displays output from a RunFileBean as it runs.
 */
public class RunFileJFrame extends JFrame implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(RunFileJFrame.class);
    private JTextArea jTextArea;

    public RunFileJFrame(RunFileBean runFileBean) {
        super("Running " + runFileBean.getInputObjectString());
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        contentPane.add(new JScrollPane(jTextArea), BorderLayout.CENTER);
        setSize(600, 400);      // todo use getPreferredSize()
        setLocation(25, 25);    // todo caller should do
        setVisible(true);
        runFileBean.addPropertyChangeListener(this);  // ListenerSpinOver will spin-over us automatically
        try {
            runFileBean.start();
        } catch (Exception exc) {
            StringWriter stringWriter = new StringWriter();
            exc.printStackTrace(new PrintWriter(stringWriter));
            jTextArea.setText("There was an error running the file:\n" +
                    "    " + runFileBean.getInputObjectString() + "\n\n" +
                    stringWriter.toString());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Assert.onEDT();
        String propertyName = evt.getPropertyName();
        if ("status".equals(propertyName)) {
            String status = (String) evt.getNewValue();
            jTextArea.append("Status: " + status + "\n");
        } else if ("output".equals(propertyName)) {
            String output = (String) evt.getNewValue();
            jTextArea.append(output);
        } else {
            log.warn("unknown property name: '" + propertyName + "'");
        }
    }

}
