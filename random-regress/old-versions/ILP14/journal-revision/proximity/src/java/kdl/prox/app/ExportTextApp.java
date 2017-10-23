/**
 * $Id: ExportTextApp.java 3784 2007-11-19 19:43:06Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ExportTextApp.java 3784 2007-11-19 19:43:06Z schapira $
 */

package kdl.prox.app;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * An application to dump a database into text files, which can later be imported with the ImportTextApp.
 * It saves all the content (objects, links, attributes, and containers with their attributes) into the given
 * directory.
 * All the files are saved with the .data extension, which is what ImportTextApp expects.
 * <p/>
 * Note: It does not save user-created NSTs; only the Proximity tables.
 *
 * @see ImportXMLApp
 */
public class ExportTextApp {

    private static final Logger log = Logger.getLogger(ExportTextApp.class);

    private static final String ATTRIBUTES_FILE_NAME = "attributes";
    private static final String CONTAINERS_FILE_NAME = "containers";
    private static final String LINKS_FILE_NAME = "links";
    private static final String OBJECTS_FILE_NAME = "objects";

    private String outputDir;
    private String filePathSeparator;

    /**
     * @param outputDir directory to export to.
     * @throws Exception
     */
    public ExportTextApp(String outputDir) {
        Assert.stringNotEmpty(outputDir, "null outputDir");
        this.outputDir = outputDir;
        filePathSeparator = System.getProperty("file.separator");
        if (!new File(outputDir).exists()) {
            throw new IllegalArgumentException("Path not found: " + outputDir);
        }

        log.info("* exporting database to " + outputDir);
        log.info("  Exporting object table");
        DB.getObjectNST().tofile(this.outputDir + filePathSeparator + OBJECTS_FILE_NAME + ".data");

        log.info("  Exporting link table");
        DB.getLinkNST().tofile(this.outputDir + filePathSeparator + LINKS_FILE_NAME + ".data");

        log.info("  Exporting attributes");
        exportAttributes("O");
        exportAttributes("L");
        exportAttributes("C");

        log.info("  Exporting containers");
        exportContainer(DB.getRootContainer());
        log.info("* done exporting");
    }

    /**
     * Goes through the list of attributes and exports them to files
     * saving their name and types in the generic attributes.data file.
     *
     * @param attrType
     */
    private void exportAttributes(String attrType) {
        Attributes attrs;
        if (attrType.equalsIgnoreCase("O")) {
            attrs = DB.getObjectAttrs();
        } else if (attrType.equalsIgnoreCase("L")) {
            attrs = DB.getLinkAttrs();
        } else if (attrType.equalsIgnoreCase("C")) {
            attrs = DB.getContainerAttrs();
        } else {
            throw new IllegalArgumentException("Unknown attributype " + attrType);
        }

        List attributeNames = attrs.getAttributeNames();
        for (int attrIdx = 0; attrIdx < attributeNames.size(); attrIdx++) {
            String attrName = (String) attributeNames.get(attrIdx);
            log.info(attrName);

            String filename = attrType + "_attr_" + attrName + ".data";
            String type = attrs.getAttrTypeDef(attrName);
            writeInFile(ATTRIBUTES_FILE_NAME + ".data",
                    attrName + "\t" +
                            attrType + "\t" +
                            type + "\t" +
                            filename);

            NST table = attrs.getAttrDataNST(attrName);
            table.tofile(outputDir + filePathSeparator + filename);
            table.release();
        }
    }

    /**
     * Exports a container, its objects and links, and its attributes
     * And then it recurses over its children
     *
     * @param cont
     */
    private void exportContainer(Container cont) {
        if (!cont.isRootContainer()) {
            String containerFileName = "si_" + cont.getOid();
            String containerAttributesFileName = containerFileName + "_attrs";
            log.info("     " + containerFileName);

            // dump object and link tables
            cont.getObjectsNST().tofile(outputDir + filePathSeparator + containerFileName + ".objects.data");
            cont.getLinksNST().tofile(outputDir + filePathSeparator + containerFileName + ".links.data");

            // dump attributes
            Attributes attrs = cont.getSubgraphAttrs();
            writeInFile(containerAttributesFileName + ".data", null); //save it, even if empty
            List attributeNames = attrs.getAttributeNames();
            for (int attrIdx = 0; attrIdx < attributeNames.size(); attrIdx++) {
                String attrName = (String) attributeNames.get(attrIdx);
                String filename = containerFileName + "_attr_" + attrName + ".data";
                String type = attrs.getAttrTypeDef(attrName);
                writeInFile(containerAttributesFileName + ".data", attrName + "\t" + type + "\t" + filename);

                NST table = attrs.getAttrDataNST(attrName);
                table.tofile(outputDir + filePathSeparator + filename);
                table.release();
            }

            // save in the containers table
            writeInFile(CONTAINERS_FILE_NAME + ".data",
                    cont.getOid() + "\t" +
                            cont.getName() + "\t" +
                            cont.getParentOid() + "\t" +
                            containerAttributesFileName + "\t" +
                            containerFileName);
        }

        // and iterate over the children
        List allChildrenNames = cont.getChildrenNames();
        for (int childIdx = 0; childIdx < allChildrenNames.size(); childIdx++) {
            String childName = (String) allChildrenNames.get(childIdx);
            exportContainer(cont.getChild(childName));
        }
    }


    /**
     * See printUsage() for args: hostAndPort outputDir
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("wrong number of args (" + args.length + ")");
            ExportTextApp.printUsage();
            return;
        }
        String hostAndPort = args[0];
        String outputDirPath = args[1];
        Util.initProxApp();        // configures Log4J

        try {
            DB.open(hostAndPort);
            new ExportTextApp(outputDirPath);
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();    // NB: crucial because we need to commit
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + ExportTextApp.class.getName() +
                " hostAndPort outputDir\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\toutputDir: Path to the directory where the output files should be saved\n");
    }

    /**
     * Utility method: Appends a line to a file.
     * If text is empty, it doesn't write anything (used to CREATE a file)
     *
     * @param fileName
     * @param text
     */
    private void writeInFile(String fileName, String text) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputDir + filePathSeparator + fileName, true));
            if (text != null) {
                out.write(text + "\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
