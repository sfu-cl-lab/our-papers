/**
 * $Id: NSTBrowserApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import kdl.prox.db.DB;
import kdl.prox.monet.MonetException;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * A simple application that opens the passed prox db and shows the NSTBrowser
 * for it.
 */
public class NSTBrowserApp {

    private static Logger log = Logger.getLogger(NSTBrowserApp.class);


    /**
     * Args: hostAndPort
     */
    public static void main(String[] args) {

        // check args -- error message has to go to system out because Log4J
        // hasn't been initialized yet
        if (args.length != 1) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        Util.initProxApp();

        try {
            log.info("opening database...");
            Util.initProxApp();
            DB.open(hostAndPort);
            log.info("opening database: done");
            NSTBrowserJFrame nstBrowserJFrame = new NSTBrowserJFrame();

            // catch closing by exiting
            nstBrowserJFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    try {
                        DB.close();
                        System.exit(0);
                    } catch (MonetException monExc) {
                        log.error("error closing database", monExc);
                        System.exit(-1);
                    }
                }
            });

            nstBrowserJFrame.pack();
            nstBrowserJFrame.setVisible(true);
        } catch (MonetException monExc) {
            log.error("error opening database", monExc);
        }
    }


    private static void printUsage() {
        System.out.println("Usage: java " + NSTBrowserApp.class.getName() + " hostAndPort\n" +
                "\thostAndPort: <host>:<port>\n");
    }


}
