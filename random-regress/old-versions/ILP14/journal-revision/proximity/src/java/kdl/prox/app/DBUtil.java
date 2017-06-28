/**
 * $Id: DBUtil.java 3661 2007-10-15 16:48:12Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information) .
 */

package kdl.prox.app;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * A simple database utility application that creates Proximity tables, indexes,
 * etc. in the passed database.
 */
public class DBUtil {

    private static Logger log = Logger.getLogger(DBUtil.class);
    private static String hostAndPort;


    /**
     * Clears the database
     *
     * @throws Exception
     */
    private static void clearDB() throws Exception {
        try {
            log.info("* connecting to db");
            DB.open(hostAndPort);
            log.info("* database opened; initializing Prox tables");
            DB.clearDB();
            log.info("* tables removed");
        } finally {
            log.info("* disconnecting from db");
            DB.close();
            log.info("* done");
        }
    }


    /**
     * Initialize a database
     */
    private static void initDB() throws Exception {
        try {
            log.info("* connecting to db");
            DB.open(hostAndPort);
            log.info("* database opened; initializing Prox tables");
            DB.initEmptyDB();
            log.info("* tables initialized");
        } finally {
            log.info("* disconnecting from db");
            DB.close();
            log.info("* done");
        }
    }


    /**
     * Checks that the link table doesn't have links that connect non-existing objects
     *
     * @param objectNST
     * @param linkNST
     * @return
     */
    public static boolean isDBConsistent(NST objectNST, NST linkNST) {
        NST nst1 = linkNST.intersect(objectNST, "o1_id = id", "link_id");
        NST nst2 = linkNST.intersect(objectNST, "o2_id = id", "link_id");

        int linkRowCount = linkNST.getRowCount();

        boolean isOK = ((nst1.getRowCount() == linkRowCount) &&
                (nst2.getRowCount() == linkRowCount));

        nst1.release();
        nst2.release();

        return isOK;
    }


    /**
     * See printUsage() for args:
     * <p/>
     * hostAndPort command [command ...]
     *
     * @param args
     */
    public static void main(String[] args) {
        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length < 2) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        // Loop through the list of commands and make sure that they are valid
        for (int i = 1; i < args.length; i++) {
            String command = args[i];
            if ((command.compareToIgnoreCase("initDB") != 0) &&
                    (command.compareToIgnoreCase("init-db") != 0) &&
                    (command.compareToIgnoreCase("clearDB") != 0) &&
                    (command.compareToIgnoreCase("clear-db") != 0) &&
                    (command.compareToIgnoreCase("testDB") != 0) &&
                    (command.compareToIgnoreCase("test-db") != 0) &&
                    (command.compareToIgnoreCase("view-schema") != 0) &&
                    (command.compareToIgnoreCase("schema") != 0) &&
                    (command.compareToIgnoreCase("view-stats") != 0)) {
                System.out.println("Error: Invalid command " + command);
                printUsage();
                return;
            }
        }

        // initialize Proximity Application
        hostAndPort = args[0];
        Util.initProxApp();

        // Loop through each argument and execute the corresponding function
        for (int i = 1; i < args.length; i++) {
            String command = args[i];
            try {
                if ((command.compareToIgnoreCase("clearDB") == 0) || (command.compareToIgnoreCase("clear-db") == 0)) {
                    clearDB();
                }
                if ((command.compareToIgnoreCase("initDB") == 0) || (command.compareToIgnoreCase("init-db") == 0)) {
                    initDB();
                }
                if ((command.compareToIgnoreCase("testDB") == 0) || (command.compareToIgnoreCase("test-db") == 0)) {
                    testDB();
                }
                if ((command.compareToIgnoreCase("schema") == 0) || (command.compareToIgnoreCase("view-schema") == 0)) {
                    printSchemaLog();
                }
                if ((command.compareToIgnoreCase("view-stats") == 0)) {
                    printStats();
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.fatal("Exception found while executing command " + command +
                        ": " + e);
                return;
            }
        }

    }


    /**
     * Prints the database's schema log. The schema log is a list of the
     * database's schema versions, each with the date on which that version
     * was installed or upgraded.
     */
    private static void printSchemaLog() {
        try {
            DB.open(hostAndPort);
            List schemaLog = DB.getSchemaLog();
            System.out.println("* print schema log: database contains following schema " +
                    "date(s) and version(s):");
            for (int logNum = 0; logNum < schemaLog.size(); logNum++) {
                String logEntry = (String) schemaLog.get(logNum);
                System.out.println("  " + logEntry);
            }
        } finally {
            DB.close();
            log.info("* print schema log: done");
        }
    }


    /**
     * Prints the database's statistics:
     * number of objects,
     * number of links,
     * number of containers,
     * number of object | link | container attributes
     */
    private static void printStats() {
        try {
            DB.open(hostAndPort);
            int objectCount = DB.getObjectNST().getRowCount();
            int linkCount = DB.getLinkNST().getRowCount();
            int containerCount = DB.getRootContainer().getChildrenCount();
            int objAttrCount = DB.getObjectAttrs().getAttrNST().getRowCount();
            int linkAttrCount = DB.getLinkAttrs().getAttrNST().getRowCount();
            int contAttrCount = DB.getContainerAttrs().getAttrNST().getRowCount();
            System.out.println("* print statistics: database containes the following: \n" +
                    "\t - " + objectCount + " objects and " +
                    objAttrCount + " object attributes, \n" +
                    "\t - " + linkCount + " links and " +
                    linkAttrCount + " link attributes, \n" +
                    "\t - " + containerCount + " containers and " +
                    contAttrCount + " container attributes");
        } finally {
            DB.close();
            log.info("* print stats: done");
        }
    }


    /**
     * Called by main() when args wrong, prints message.
     */
    private static void printUsage() {
        System.out.println("Usage: java " + DBUtil.class.getName() +
                " hostAndPort command [command ...] \n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tcommand(s):   list of commands to execute. Valid commands:\n" +
                "\t\tclear-db: clear database\n" +
                "\t\tinit-db: initialize database\n" +
                "\t\ttest-db: test database connection and print Proximity versions\n" +
                "\t\tview-schema: print schema log\n" +
                "\t\tview-stats: print statistics\n");
    }


    /**
     * Prints Proximity version information, tries connecting to the database,
     * and disconnects.
     */
    private static void testDB() {
        try {
            log.info("* test db: Proximity versions: Core: " + Util.getCoreVersion() +
                    ", GUI: " + Util.getGUIVersion());
            log.info("* test db: trying to connect to database...");
            DB.open(hostAndPort);
            System.out.println("* Connected to database");
            log.info("* test db: connected to database");
        } finally {
            log.info("* test db: disconnecting from database...");
            DB.close();
            log.info("* test db: done");
        }
    }


}
