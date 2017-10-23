/**
 * $Id: QGraphEditorApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import java.io.File;
import javax.swing.JFrame;
import kdl.prox.qged.QGraphEditorJFrame;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;


/**
 * A driver application for QGraphEditorJFrame that allows users to edit
 * queries when no database is available.
 */
public class QGraphEditorApp {

    private static final Logger log = Logger.getLogger(QGraphEditorApp.class);


    /**
     * @param args [queryFileName]
     */
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        // set proxDB and queryFile
        File queryFile = null;
        if ((args.length == 1) && (args[0].length() > 0)) {
            queryFile = new File(args[0]);
        }

        // check queryFile if necessary
        if ((queryFile != null) && !queryFile.exists()) {
            System.err.println("couldn't find query file: " + queryFile);
            System.exit(1);            // abnormal
        }

        // show the frame
        Util.initProxApp();        // configures Log4J
        try {
            QGraphEditorJFrame qgEdJFrame = new QGraphEditorJFrame(queryFile);
            qgEdJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } catch (Exception exc) {
            log.error("error starting appplication", exc);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + QGraphEditorApp.class.getName() +
                " [queryFileName]\n" +
                "\tqueryFileName: optional query file to open\n");
    }

}
