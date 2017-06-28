/**
 * $Id: Query.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Query.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import kdl.prox.db.DB;
import kdl.prox.monet.MonetException;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;


/**
 * Application that executes a Proximity qGraph2 query xml file.
 */
public class Query {

    private static Logger log = Logger.getLogger(Query.class);

    /**
     * Runs a QGraph 2.0 query on a Proximity database.
     * <p/>
     * Args: hostAndPort queryXMLFile collectionName [inputContainer]
     * <p/>
     * The syntax for inputContainer is a path delimited by '/' characters,
     * starting with '/', e.g.:
     * <p/><pre>
     *      /c1/c2  -- container c2 under container c1 under the root container
     * </pre><p/>
     * Pass the empty string or '/' for inputContainer to run on the entire database.
     */
    public static void main(String[] args) {
        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length != 3 && args.length != 4) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        String queryFileName = args[1];
        String outputContainerName = args[2];
        String inputContainerPath = (args.length == 4 ? args[3] : null); // entire db by default
        Util.initProxApp();
        log.debug("main(): " + queryFileName + ", " + outputContainerName +
                (inputContainerPath != null ? ", " + inputContainerPath : ""));
        if ((queryFileName.length() == 0) || (outputContainerName.length() == 0)) {
            log.fatal("one of the two args was empty: '" + queryFileName + "', '" +
                    outputContainerName + "'");
            printUsage();
            System.exit(-1);
        }
        try {
            DB.open(hostAndPort);
            QueryGraph2CompOp.runQuery(new File(queryFileName), inputContainerPath, outputContainerName);
        } catch (MonetException monExc) {
            log.error("error running query", monExc);
        } finally {
            DB.close();
        }
    }


    private static void printUsage() {
        System.out.println("Usage: java " + Query.class.getName() +
                " hostAndPort queryXMLFile containerName [inputContainer]\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tqueryXMLFile - QGraph 2.0 query XML file; either " +
                " relative to the working directory, or absolute\n" +
                "\tcontainerName: name of the container to create and save the " +
                "results in\n" +
                "\t[inputContainer]: optional path to the container to run the " +
                "query on (e.g. /c1/c2) -- default: entire db");
    }

}



