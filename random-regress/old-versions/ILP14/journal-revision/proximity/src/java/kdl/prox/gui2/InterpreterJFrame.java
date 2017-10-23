/**
 * $Id: InterpreterJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.db.DB;
import kdl.prox.script.Proximity;
import kdl.prox.script.TextPaneJythonConsole;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Proximity Interpreter provides ability to run python commands
 * from the GUI in Proximity
 */
public class InterpreterJFrame extends JFrame {

    private static final Logger log = Logger.getLogger(InterpreterJFrame.class);

    /**
     * Create a new InterpreterJFrame for the given database
     */

    public InterpreterJFrame() {
        super(DB.description());

        Proximity prox = new Proximity();

        // the console, and the scrollbars around it
        JTextPane jTextPane = new TextPaneJythonConsole(prox.getInterpreter());
        JScrollPane scrollPane = new JScrollPane(jTextPane);
        scrollPane.setPreferredSize(new Dimension(510, 320));
        scrollPane.setWheelScrollingEnabled(true);

        // put the scrollbar pane (with the console inside) in a panel
        JPanel interpreterPane = new JPanel();
        interpreterPane.setLayout(new BorderLayout());
        interpreterPane.add(scrollPane, BorderLayout.CENTER);

        this.setContentPane(interpreterPane);
        this.pack();
    }

}
