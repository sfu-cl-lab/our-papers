/**
 * $Id: ImportAsciiGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import kdl.prox.db.DB;
import kdl.prox.impascii2.Interpreter;
import kdl.prox.impascii2.MonetDBHandler;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

/**
 * Main application for interpreting a Proximity "Smart ASCII" graph file.
 * For documentation and examples, see the directory proximity3/doc/user/impascii2/.
 */
public class ImportAsciiGraph {

    private static Logger log = Logger.getLogger(ImportAsciiGraph.class);


    /**
     * Main programmatic entry point for interpreting a smart ascii file.
     *
     * @param bufferedReader
     * @param nameObjStrAttr an 'str' object attribute in proxDB that is used to
     *                       look up smart ascii ii nicknames. see ObjDict for more info
     * @return MonetDBHandler for accessing creation statistics
     * @throws Exception
     * @see kdl.prox.impascii2.ObjDict
     */
    public static MonetDBHandler doImport(BufferedReader bufferedReader,
                                          String nameObjStrAttr) throws Exception {
        MonetDBHandler monetDBHandler = new MonetDBHandler(nameObjStrAttr);
        Interpreter.readLines(bufferedReader, monetDBHandler);
        return monetDBHandler;
    }

    /**
     * See printUsage() for args:
     * <p/>
     * hostAndPort inputFile nameObjStrAttr
     *
     * @param args
     */
    public static void main(String[] args) {
        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length != 3) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        String inputFileName = args[1];
        String nameObjStrAttr = args[2];
        Util.initProxApp();
        try {
            File inputFile = new File(inputFileName);
            if (!inputFile.exists()) {
                throw new Exception("file doesn't exist: " + inputFile);
            }

            DB.open(hostAndPort);
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            log.info("* starting import...");
            MonetDBHandler monetDBHandler = ImportAsciiGraph.doImport(bufferedReader, nameObjStrAttr);
            log.info("* import done; counts: " + monetDBHandler.getNumObjects() +
                    " objects, " + monetDBHandler.getNumLinks() + " links, and " +
                    monetDBHandler.getNumAttrs() + " attributes");
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + ImportAsciiGraph.class.getName() +
                " hostAndPort inputFile nameObjStrAttr\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tinputFile: smart ASCII graph input file\n" +
                "\tnameObjStrAttr: name of the object string attribute to " +
                "use for looking up smart ascii object names\n");
    }

}
