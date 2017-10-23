/**
 * $Id: QueryGenApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryGenApp.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.family;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * Application that generates a set of qgraph2 query (xml) files from a
 * <query-family> (xml) file. Recall the overall process (from readme.txt):
 * <p/>
 * 1. make '<family name>.fam.xml' (1)
 * 2. run QueryGenApp              (creates N 'query_<description>.xml' files)
 * 3. edit '<family name>.fam.dat' (1)
 * 4. edit 'query_<description>.mat'      (N)
 * 5. run FamilyTestApp
 * <p/>
 * <p/>
 * Specfically, this app:
 * <p/>
 * o Creates backup directory for all existing files, except the family file,
 * and moves the files to it.
 * o Creates a new query file for all variations in the family.
 * <p/>
 * Note: Uses the query family file's directory for all generation, etc.
 */
public class QueryGenApp {

    private static Logger log = Logger.getLogger(QueryGenApp.class);


    /**
     * Full-arg constructor.
     *
     * @param queryFamilyFileName
     */
    public QueryGenApp(String queryFamilyFileName, String outputDirectoryName)
            throws Exception {

        Assert.notNull(queryFamilyFileName, "queryFamilyFileName null");
        Assert.notNull(outputDirectoryName, "outputDirectoryName null");
		
        // validate parameters
        File queryFamilyFile = new File(queryFamilyFileName);
        if (!queryFamilyFile.exists()) {
            throw new IllegalArgumentException("query family file not found: " +
                    queryFamilyFile);
        }
        if (!queryFamilyFileName.endsWith(".fam.xml")) {
            throw new IllegalArgumentException("query family file does not: " +
                    "end with '.fam.xml: " + queryFamilyFile);
        }

        File outputDirectoryFile = new File(outputDirectoryName);
        if (!outputDirectoryFile.exists()) {
            throw new IllegalArgumentException("output directory not found: " +
                    outputDirectoryFile);
        }
        if (!outputDirectoryFile.isDirectory()) {
            throw new IllegalArgumentException("output directory is a file: " +
                    outputDirectoryFile);
        }
		
        // read the specification and build in-memory query instances
        Element familyEle;
        try {
            SAXBuilder saxBuilder = new SAXBuilder(true);	// validating
            Document document = saxBuilder.build(queryFamilyFile);
            familyEle = document.getRootElement();
        } catch (JDOMException jdExc) {
            Throwable bestThrowable = (jdExc.getCause() != null ?
                    jdExc.getCause() : jdExc);
            log.fatal("problem parsing xml file", bestThrowable);
            throw jdExc;
        }
        QueryGen queryGen = new QueryGen(familyEle); 

        // move files to backup directory
        File bakDirFile = new File(outputDirectoryFile, "BAK");
        if (!bakDirFile.exists()) {
            log.info("* created backup directory: " + bakDirFile);
            bakDirFile.mkdir();
        }
        File[] oldFiles = outputDirectoryFile.listFiles();
        log.info("* moving files to backup directory: " + bakDirFile.getPath());
        for (int i = 0; i < oldFiles.length; i++) {
            File oldFile = oldFiles[i];
            if (isBackupFile(oldFile)) {
                File newFile = new File(bakDirFile, oldFile.getName());
                log.info("  Moving " + oldFile.getName());
                oldFile.renameTo(newFile);
            }
        }
        
        // create empty family .dat file
        File datFile = createDatStubFile(outputDirectoryFile, queryFamilyFile);
        log.info("* creating '.dat' stub file: " + datFile);
        
        // save queries
        Map queryInstances = queryGen.getQueryInstances();
        Iterator queryInstIter = new ArrayList(queryInstances.keySet()).iterator();
        log.info("* saving query instance and match stub files:");
        while (queryInstIter.hasNext()) {
            String rootEleName = "graph-query";
            String instanceName = (String) queryInstIter.next();
            Element queryInstanceEle = new Element(rootEleName);     // filled next
            queryInstanceEle.setAttribute("name", instanceName);
            Element descriptionEle = new Element("description");
            descriptionEle.addContent(instanceName);
            queryInstanceEle.addContent(descriptionEle);  // add description node
            queryInstanceEle.addContent((Element) queryInstances.get(instanceName)); //body
            Document instanceDocument = new Document();             // doc to save
            instanceDocument.setDocType(new DocType(rootEleName,
                    rootEleName + ".dtd"));
            instanceDocument.setRootElement(queryInstanceEle);
            BufferedWriter writer = null;
            try {
                File queryFile = new File(outputDirectoryFile,
                        "query" + instanceName + ".xml");
                writer = new BufferedWriter(new FileWriter(queryFile));
                new XMLOutputter(Format.getPrettyFormat()).output(instanceDocument, writer);
                File matFile = createMatStubFile(outputDirectoryFile, queryFile);   // creates empty family .mat file
                log.info("  " + queryFile.getName());
                log.info("  " + matFile.getName());
            } catch (IOException ex) {
                log.error("error writing file " + instanceName, ex);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
        log.debug("* wrote " + queryInstances.size() + " queries");
    }


    /**
     * Called by constructor, creates a ".dat" stub file for queryFamilyFile.
     * Recall:
     * <p/>
     * o query family file format:   '<family name>.fam.xml'
     * o family dataset file format: '<family name>.fam.dat'
     * <p/>
     * I.e., we replace the final ".xml" with ".dat"
     *
     * @param queryFamilyFile
     * @return created File
     * @throws IOException
     */
    private File createDatStubFile(File outputDirectoryFile, File queryFamilyFile)
            throws IOException {
        String famFileName = queryFamilyFile.getName();
        String famFileNameNoXML = famFileName.substring(0, famFileName.length() - 4);
        File datFile = new File(outputDirectoryFile, famFileNameNoXML + ".dat");
        createStubFile(datFile);
        return datFile;
    }


    /**
     * Called by constructor, creates a ".mat" stub file for queryFile. Recall:
     * <p/>
     * o query instance file file format: 'query_*.xml'
     * o query data match file format   : 'query_<description>.mat'
     * <p/>
     * I.e., we replace the final ".xml" with ".mat"
     *
     * @return created File
     * @throws IOException
     */
    private File createMatStubFile(File outputDirectoryFile, File queryFile)
            throws IOException {
        String famFileName = queryFile.getName();
        String famFileNameNoXML = famFileName.substring(0, famFileName.length() - 4);
        File matFile = new File(outputDirectoryFile, famFileNameNoXML + ".mat");
        createStubFile(matFile);
        return matFile;
    }


    /**
     * Called by createDatStubFile() and createMatStubFile(), creates a new
     * empty file named file. NB: Overwrites existing files without warning.
     *
     * @param file
     * @throws IOException
     */
    private void createStubFile(File file) throws IOException {
        BufferedWriter writer = null;
        try {
            file.delete();      /** todo warn first? */
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("");   /** todo necessary to create empty file? */
        } finally {
            writer.close();
        }

    }


    /**
     * Called by constructor, returns true if file should be moved to the backup
     * directory. Returns false it not. Recall the types of files involved:
     * <p/>
     * o query family file: user-created -> don't move
     * ('<family name>.fam.xml')
     * <p/>
     * o query instance files: generated -> move
     * ('query_*.xml')
     * <p/>
     * o family dataset file: stub generated, user-completed -> move
     * ('<family name>.fam.dat')
     * <p/>
     * o query data match files: stub generated, user-completed -> move
     * ('query_<description>.mat')
     * <p/>
     * o everything else: don't move
     *
     * @param file
     * @return
     */
    private boolean isBackupFile(File file) {
        String fileName = file.getName();
//      boolean isFamilyFile = fileName.endsWith(".fam.xml");
        boolean isQueryFile = fileName.startsWith("query_") &&
                fileName.endsWith(".xml");
        boolean isDatFile = fileName.endsWith(".fam.dat");
        boolean isMatFile = fileName.endsWith(".mat");
        return !file.isDirectory() && (isQueryFile || isDatFile || isMatFile);
    }


    /**
     * See printUsage() for details.
     * <p/>
     * args: queryFamilyFile outputDirectory
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }
        Util.initProxApp();
        String queryFamilyFileName = args[0];
        String outputDirectoryName = args[1];
        new QueryGenApp(queryFamilyFileName, outputDirectoryName);
    }


    /**
     * Called by main() when args wrong, prints message.
     */
    private static void printUsage() {
        System.out.println("Usage: java " + QueryGenApp.class.getName() +
                " queryFamilyFile outputDirectory\n" +
                "\tqueryFamilyFile: the query family (xml) description file\n" +
                "\toutputDirectory: the directory where all the xml query files should be saved.\n");
    }

}
