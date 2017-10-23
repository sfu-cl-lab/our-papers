/**
 * $Id: ImportProx2XMLApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ImportProx2XMLApp.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.Writer;


/**
 * This is an application that imports an entire Proximity2 database from a
 * single ProxDataML XML file, as created by Proximity2's ExportXMLApp. See
 * proximity3/src/xml/prox2db.dtd for the data format.
 * <p/>
 * This version uses Monet's ascii_io module to speed up importing. We do this
 * via these steps:
 * <p/>
 * 1. generate 'format' and 'data' text files corresponding the xml file
 * 2. load those files using the ascii_io module
 * 3. convert those loaded NSTs into Proximity Three NSTs
 * <p/>
 * Notes:
 * o erases existing database! (e.g., can't load xml file on top of data)
 * <p/>
 * todo Requires Xerces2 Java Parser 2.5.0 Release (or better), which <B>must</B> be ahead of the xerces.jar that comes with JDOM in the class path. Otherwise xx. <- is this true?
 */
public class ImportProx2XMLApp {

    private static final Logger log = Logger.getLogger(ImportProx2XMLApp.class);
    private static final int INFO_MODULO_COUNT = 500000;

    // names of format and data files created during parsing. names of av_N
    // and si_N files are based on numAttributes and numContainers, respectively
    private static final String AV_FILE_PREFIX = "av_";
    private static final String SI_FILE_PREFIX = "si_";

    private static final String ATTRIBUTES_FILE_NAME = "attributes";
    private static final String COLLECTIONS_FILE_NAME = "collections";
    private static final String LINKS_FILE_NAME = "links";
    private static final String OBJECTS_FILE_NAME = "objects";


    private static final String LINK_NST_NAME = "prox_link";
    private static final String OBJ_NST_NAME = "prox_object";

    private int nextAVFileNum = 0;          // counter for av_N file names
    private int nextSIFileNum = 0;          // counter for si_N file names

    // state for current <ATTR-VALUE> elements
    private StringBuffer currAttrValueSB = null;
    private String currAttrItemID = null;

    // state for <ATTRIBUTE> elements
    private Writer attributesDataWriter = null;
    private Writer currAVDataWriter = null;

    // state for <COLLECTION> elements
    private Writer collectionsDataWriter = null;
    private Writer currSIObjectDataWriter = null;
    private Writer currSILinkDataWriter = null;

    // state for <LINK> elements
    private Writer linksDataWriter = null;

    // state for <OBJECT> elements
    private Writer objectsDataWriter = null;

    // import counters
    protected int numAttributes = 0;
    protected int numAttrValues = 0;
    protected int numContainers = 0;
    protected int numLinks = 0;
    protected int numObjects = 0;
    protected int numSubgraphItems = 0;
    protected int numSubgraphs = 0;
    protected boolean isCalcStatsOnly;

    private String tempDirPath;

    /**
     * Two-arg overload that does the import (does not just compute statistics).
     *
     * @param inputFileArg
     */
    public ImportProx2XMLApp(String inputFileArg) throws Exception {
        this(inputFileArg, false);
    }

    /**
     * Full-arg constructor. Uses standard (Xerces2) SAX parsing in order to
     * better scale (over DOM) to very large files.
     *
     * @param inputFileArg    ProxDataML (XML) file to import from. see
     *                        proximity3/src/xml/prox2db.dtd for the data format
     * @param isCalcStatsOnly pass true to compute statistics only (not import data)
     * @throws Exception
     */
    public ImportProx2XMLApp(String inputFileArg, boolean isCalcStatsOnly) throws Exception {
        Assert.stringNotEmpty(inputFileArg, "null inputFileArg");
        this.isCalcStatsOnly = isCalcStatsOnly;
        String action = (isCalcStatsOnly ? "getting statistics" :
                "importing database");
        log.info("* " + action + " from: " + inputFileArg);

        File tempDir = ImportUtil.getTempDir();
        this.tempDirPath = tempDir.getPath();

        // create format and data files via parser, clearing temp dir if necessary
        log.info("* converting xml to bulk import text files; dir: " + tempDir);

        File inputFile = new File(inputFileArg);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);       // todo set to false because it was giving a Memory Out of Bounds error, but it should be true
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputFile, new InputParser(tempDirPath));

        // load data and convert to prox, if necessary
        if (!isCalcStatsOnly) {
            log.info("* importing text files");
            convertFilesToNSTs();
            DB.commit();
        }

        // done
        ImportUtil.deleteDir(tempDir);
        log.info("* done " + action + "; counts: " + numObjects +
                " objects, " + numLinks + " links, " + numAttributes +
                " attributes, " + numAttrValues + " attribute values, " +
                numContainers + " containers, " + numSubgraphs +
                " subgraphs, " + numSubgraphItems + " subgraph items");
    }

    /**
     * Goes through the list of attributes, loads each attribute data file,
     * and creates a new attribute in ObjAttr or LinkAttr
     *
     * @param attrsNST
     */
    private void convertLoadedAttributesToProx(NST attrsNST) throws IOException {
        int nstSize = attrsNST.getRowCount();
        String[] attrNames = new String[nstSize];
        boolean[] attrIsObject = new boolean[nstSize];
        String[] attrTypes = new String[nstSize];
        String[] attrFiles = new String[nstSize];
        int rowCnt = 0;
        ResultSet dataRS = attrsNST.selectRows();
        while (dataRS.next()) {
            attrNames[rowCnt] = dataRS.getString(1);
            attrIsObject[rowCnt] = (dataRS.getString(2).equalsIgnoreCase("O"));
            attrTypes[rowCnt] = dataRS.getString(3);
            attrFiles[rowCnt] = dataRS.getString(4);
            rowCnt++;
        }
        // delete existing attributes
        kdl.prox.db.Attributes objectAttrs = DB.getObjectAttrs();
        kdl.prox.db.Attributes linkAttrs = DB.getLinkAttrs();
        objectAttrs.deleteAllAttributes();
        linkAttrs.deleteAllAttributes();
        // now go through the array loading the av_* files, and creating the attribute
        for (int i = 0; i < nstSize; i++) {
            String dataFileName = attrFiles[i];
            log.info("  Loading attribute: " + dataFileName);
            NST attrDataNST = ImportUtil.loadNST(tempDirPath, dataFileName);
            if (attrIsObject[i]) {
                objectAttrs.defineAttributeWithData(attrNames[i], attrTypes[i],
                        attrDataNST);
            } else {
                linkAttrs.defineAttributeWithData(attrNames[i], attrTypes[i],
                        attrDataNST);
            }
            attrDataNST.release();
        }
        attrsNST.release();
    }

    /**
     * Loads each collection and loads it into the Prox DB.
     *
     * @param colsNST
     */
    private void convertLoadedCollectionsToProx(NST colsNST) throws IOException {
        int NSTSize = colsNST.getRowCount();
        String[] colsNames = new String[NSTSize];
        String[] colsFiles = new String[NSTSize];
        int rowCnt = 0;
        ResultSet dataRS = colsNST.selectRows();
        while (dataRS.next()) {
            colsNames[rowCnt] = dataRS.getString(1);
            colsFiles[rowCnt] = dataRS.getString(2);
            rowCnt++;
        }
        // delete existing containers
        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
        // now go through the array loading the si_* files, and creating the containers
        for (int i = 0; i < NSTSize; i++) {
            String dataFileName = colsFiles[i];
            log.info("  Loading collection: " + dataFileName);
            NST objectsDataNST = ImportUtil.loadNST(tempDirPath, dataFileName, dataFileName + ".objects");
            NST linksDataNST = ImportUtil.loadNST(tempDirPath, dataFileName, dataFileName + ".links");
            rootContainer.createChildFromTempSGINSTs(colsNames[i],
                    objectsDataNST, linksDataNST);
            objectsDataNST.release();
            linksDataNST.release();
        }
        colsNST.release();
    }

    /**
     * Called by constructor, converts data loaded from text files into
     * real Proximity Three schema tables.
     */
    private void convertFilesToNSTs() throws IOException {
        log.info("  Loading object table");
        NST objsNST = ImportUtil.loadNST(tempDirPath, OBJECTS_FILE_NAME);
        if (objsNST != null) {
            convertLoadedItemsTableToProx(objsNST, OBJ_NST_NAME);
            objsNST.release();
        }

        log.info("  Loading link table");
        NST linkNST = ImportUtil.loadNST(tempDirPath, LINKS_FILE_NAME);
        if (linkNST != null) {
            convertLoadedItemsTableToProx(linkNST, LINK_NST_NAME);
            linkNST.release();
        }

        log.info("  Loading attributes");
        NST attrsNST = ImportUtil.loadNST(tempDirPath, ATTRIBUTES_FILE_NAME);
        if (attrsNST != null) {
            convertLoadedAttributesToProx(attrsNST);
            attrsNST.release();
        }

        log.info("  Loading collections");
        NST collNST = ImportUtil.loadNST(tempDirPath, COLLECTIONS_FILE_NAME);
        if (collNST != null) {
            convertLoadedCollectionsToProx(collNST);
            collNST.release();
        }
    }

    /**
     * Loads the prox_object or prox_link tables
     * It works by deleting the existing tables, and creating a new prox_object
     * or prox_link with the passed in NST
     *
     * @param dataNST
     */
    public static void convertLoadedItemsTableToProx(NST dataNST, String tableName) {
        Assert.stringNotEmpty(tableName, "empty tableName");
        // make it persistent, and rename it, first deleting the old
        new NST(tableName).delete();
        DB.commit();
        dataNST.save(tableName);
    }


    /**
     * Called by startAttribute(), returns the Proximity3 type def corresponding
     * to the Proximity2 type in dataType.
     *
     * @param dataType Proximity2 data type - "BIGINT" "DATE" "DATETIME" "DOUBLE" "INTEGER" "VARCHAR"
     * @return
     */
    static DataTypeEnum dataTypeForProx2DataType(String dataType) {
        if (dataType.equalsIgnoreCase("BIGINT")) {
            return DataTypeEnum.LNG;
        } else if (dataType.equalsIgnoreCase("DATE")) {
            return DataTypeEnum.DATE;
        } else if (dataType.equalsIgnoreCase("DATETIME")) {
            return DataTypeEnum.TIMESTAMP;
        } else if (dataType.equalsIgnoreCase("DOUBLE")) {
            return DataTypeEnum.DBL;
        } else if (dataType.equalsIgnoreCase("INTEGER")) {
            return DataTypeEnum.INT;
        } else if (dataType.equalsIgnoreCase("VARCHAR")) {
            return DataTypeEnum.STR;
        } else {
            throw new IllegalArgumentException("Proximity2 data type (" +
                    dataType + ") was not one of: BIGINT, DATE, DATETIME, " +
                    "DOUBLE, INTEGER, VARCHAR");
        }
    }


    /**
     * Returns my currAttrValueSB as a String, with bad chars replaced by ones
     * that Monet doesn't mind. Destructively modifies my currAttrValueSB.
     *
     * @return
     */
    private String getCleanAttrValue() {
        for (int charIdx = 0; charIdx < currAttrValueSB.length(); charIdx++) {
            char theChar = currAttrValueSB.charAt(charIdx);
            if (isBadChar(theChar)) {
                currAttrValueSB.setCharAt(charIdx, '_');
            }
        }
        return currAttrValueSB.toString();
    }

    /**
     * @param theChar
     * @return true if theChar is one that causes Monet errors
     */
    private boolean isBadChar(char theChar) {
        // todo replace occurrances of COLUMN_DELIMITER with '_'!?
        return (theChar == '\'') || (theChar == '\"') || (theChar == '\n');
    }

    /**
     * Returns true if should compute statistics only (not import data). Returns
     * false o/w.
     *
     * @return
     */
    private boolean isCalcStatsOnly() {
        return isCalcStatsOnly;
    }

    /**
     * Returns the file name of the next av_N or si_N file to write to.
     *
     * @param isAVFile true if want next av_N file. false if si_N file
     * @return
     */
    private String getNextAVOrSIFileName(boolean isAVFile) {
        StringBuffer fileNameSB = new StringBuffer();
        if (isAVFile) {
            fileNameSB.append(AV_FILE_PREFIX);
        } else {
            fileNameSB.append(SI_FILE_PREFIX);
        }
        if (isAVFile) {
            fileNameSB.append(nextAVFileNum);
        } else {
            fileNameSB.append(nextSIFileNum);
        }
        return fileNameSB.toString();
    }

    /**
     * Returns true if itemType is "O", false if not.
     *
     * @param itemType
     * @return
     */
    static boolean isObjectItemType(String itemType) {
        return itemType.equalsIgnoreCase("O");
    }

    /**
     * See printUsage() for args: hostAndPort inputFile [isCalcStatsOnly]
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            // check args. error message goes to system out because Log4J hasn't
            // been initialized yet
            if ((args.length != 2) && (args.length != 3)) {
                System.out.println("wrong number of args (" + args.length + ")");
                printUsage();
                return;
            }
            String hostAndPort = args[0];
            String inputFileArg = args[1];
            boolean isCalcStatsOnly = (args.length == 3 ?
                    Boolean.valueOf(args[2]).booleanValue() : false);
            Util.initProxApp();        // configures Log4J
            DB.open(hostAndPort);
            new ImportProx2XMLApp(inputFileArg, isCalcStatsOnly);
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();    // NB: crucial because we need to commit
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + ImportProx2XMLApp.class.getName() +
                " hostAndPort inputFile [isCalcStatsOnly]\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tinputFile: Location of input file\n" +
                "\tisCalcStatsOnly: optional (true or false): only calculate " +
                "statistics, not loading data\n");
    }


    private class InputParser extends DefaultHandler {
        private String path;

        public InputParser(String path) {
            this.path = path;

        }

        /**
         * DefaultHandler method.
         *
         * @param buf
         * @param offset
         * @param len
         */
        public void characters(char[] buf, int offset, int len) {
            if (!isCalcStatsOnly()) {
                // for <ATTR-VALUE>:
                currAttrValueSB.append(buf, offset, len);   // NB: replacement of 'bad' chars handled in getCleanAttrValue()
            }
        }

        /**
         * DefaultHandler method. Handles ending of <ATTRIBUTE> and <COLLECTION>
         * elements by clearing currAttrDataNST or currItemNST, respectively.
         *
         * @param namespaceURI
         * @param localName
         * @param qName
         */
        public void endElement(String namespaceURI, String localName, String qName)
                throws SAXException {
            try {
                if (qName.equalsIgnoreCase("ATTR-VALUE")) {
                    numAttrValues++;
                    if (!isCalcStatsOnly()) {
                        String value = getCleanAttrValue();
                        ImportUtil.writeLine(currAVDataWriter, new String[]{currAttrItemID + "@0", value});
                        currAttrItemID = null;
                        currAttrValueSB = null;
                        if ((numAttrValues % INFO_MODULO_COUNT) == 0) {
                            log.info("    " + numAttrValues);
                        }
                    }
                } else if (qName.equalsIgnoreCase("ATTRIBUTE")) {
                    numAttributes++;
                    nextAVFileNum++;
                    if (!isCalcStatsOnly()) {
                        currAVDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("ATTRIBUTES")) {
                    if (!isCalcStatsOnly()) {
                        attributesDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("COLLECTION")) {
                    numContainers++;
                    nextSIFileNum++;
                    if (!isCalcStatsOnly()) {
                        currSIObjectDataWriter.close();
                        currSILinkDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("COLLECTIONS")) {
                    if (!isCalcStatsOnly()) {
                        collectionsDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("ITEM")) {
                    numSubgraphItems++;
                    if (!isCalcStatsOnly()) {
                        if ((numSubgraphItems % INFO_MODULO_COUNT) == 0) {
                            log.info("    " + numSubgraphItems);
                        }
                    }
                } else if (qName.equalsIgnoreCase("LINK")) {
                    numLinks++;
                    if (!isCalcStatsOnly()) {
                        if ((numLinks % INFO_MODULO_COUNT) == 0) {
                            log.info("    " + numLinks);
                        }
                    }
                } else if (qName.equalsIgnoreCase("LINKS")) {
                    if (!isCalcStatsOnly()) {
                        linksDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("OBJECT")) {
                    numObjects++;
                    if (!isCalcStatsOnly()) {
                        if ((numObjects % INFO_MODULO_COUNT) == 0) {
                            log.info("    " + numObjects);
                        }
                    }
                } else if (qName.equalsIgnoreCase("OBJECTS")) {
                    if (!isCalcStatsOnly()) {
                        objectsDataWriter.close();
                    }
                } else if (qName.equalsIgnoreCase("SUBGRAPH")) {
                    numSubgraphs++;
                    if (!isCalcStatsOnly()) {
                        if ((numSubgraphs % INFO_MODULO_COUNT) == 0) {
                            log.info("    " + numSubgraphs);
                        }
                    }
                }
            } catch (Exception exc) {
                throw new SAXException(exc);
            }
        }

        /**
         * DefaultHandler method.
         *
         * @param namespaceURI
         * @param localName
         * @param qName
         * @param attributes
         */
        public void startElement(String namespaceURI, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (isCalcStatsOnly()) {
                return;
            }

            try {
                if (qName.equalsIgnoreCase("ATTR-VALUE")) {
                    currAttrItemID = attributes.getValue("ITEM-ID");
                    currAttrValueSB = new StringBuffer();
                } else if (qName.equalsIgnoreCase("ATTRIBUTE")) {
                    String attrName = attributes.getValue("NAME");
                    String itemType = attributes.getValue("ITEM-TYPE");     // Proximity2 item type
                    String dataType = attributes.getValue("DATA-TYPE");     // Proximity2 data type
                    DataTypeEnum dataTypeEnum = ImportProx2XMLApp.dataTypeForProx2DataType(dataType);    // throws if invalid
                    String newAVFile = getNextAVOrSIFileName(true);
                    ImportUtil.createFormatFile(path, newAVFile,
                            new String[][]{{"id", "oid"},
                                    {"value", dataTypeEnum.toString()}});
                    currAVDataWriter = ImportUtil.getDataWriter(path, newAVFile);
                    ImportUtil.writeLine(attributesDataWriter,
                            new String[]{attrName, itemType, dataTypeEnum.toString(),
                                    newAVFile});
                    log.info("  attribute: " + attrName + ", " + itemType + ", " +
                            dataType);
                } else if (qName.equalsIgnoreCase("ATTRIBUTES")) {
                    ImportUtil.createFormatFile(path, ATTRIBUTES_FILE_NAME,
                            new String[][]{{"name", "str"},
                                    {"item-type", "str"},
                                    {"data-type", "str"},
                                    {"data-file", "str"}});
                    attributesDataWriter = ImportUtil.getDataWriter(path, ATTRIBUTES_FILE_NAME);
                    log.info("  attributes");
                } else if (qName.equalsIgnoreCase("COLLECTION")) {
                    String collectionName = attributes.getValue("NAME");
                    String newSIFileName = getNextAVOrSIFileName(false); // si_N format
                    ImportUtil.createFormatFile(path, newSIFileName,
                            new String[][]{{"item_id", "oid"},
                                    {"subg_id", "oid"},
                                    {"name", "str"}});
                    currSIObjectDataWriter = ImportUtil.getDataWriter(path, newSIFileName + ".objects");
                    currSILinkDataWriter = ImportUtil.getDataWriter(path, newSIFileName + ".links");
                    ImportUtil.writeLine(collectionsDataWriter,
                            new String[]{collectionName, newSIFileName});
                    log.info("  collection: " + collectionName);
                } else if (qName.equalsIgnoreCase("COLLECTIONS")) {
                    ImportUtil.createFormatFile(path, COLLECTIONS_FILE_NAME,
                            new String[][]{{"name", "str"},
                                    {"data-file", "str"}});
                    collectionsDataWriter = ImportUtil.getDataWriter(path, COLLECTIONS_FILE_NAME);
                    log.info("  containers");
                } else if (qName.equalsIgnoreCase("ITEM")) {
                    String subgID = attributes.getValue("SUBG-ID") + "@0";
                    String itemID = attributes.getValue("ITEM-ID") + "@0";
                    String itemType = attributes.getValue("ITEM-TYPE");     // Proximity2 item type
                    String name = attributes.getValue("NAME");
                    boolean isObject = ImportProx2XMLApp.isObjectItemType(itemType);
                    if (isObject) {
                        ImportUtil.writeLine(currSIObjectDataWriter,
                                new String[]{itemID, subgID, name});
                    } else {
                        ImportUtil.writeLine(currSILinkDataWriter,
                                new String[]{itemID, subgID, name});
                    }
                } else if (qName.equalsIgnoreCase("LINK")) {
                    String linkIDStr = attributes.getValue("ID") + "@0";
                    String o1ID = attributes.getValue("O1-ID") + "@0";
                    String o2ID = attributes.getValue("O2-ID") + "@0";
                    ImportUtil.writeLine(linksDataWriter, new String[]{linkIDStr, o1ID, o2ID});
                } else if (qName.equalsIgnoreCase("LINKS")) {
                    ImportUtil.createFormatFile(path, LINKS_FILE_NAME,
                            new String[][]{{"link_id", "oid"},
                                    {"o1_id", "oid"},
                                    {"o2_id", "oid"}});
                    linksDataWriter = ImportUtil.getDataWriter(path, LINKS_FILE_NAME);
                    log.info("  links");
                } else if (qName.equalsIgnoreCase("OBJECT")) {
                    String objID = attributes.getValue("ID") + "@0";
                    ImportUtil.writeLine(objectsDataWriter, new String[]{objID});
                } else if (qName.equalsIgnoreCase("OBJECTS")) {
                    ImportUtil.createFormatFile(path, OBJECTS_FILE_NAME,
                            new String[][]{{"id", "oid"}});
                    objectsDataWriter = ImportUtil.getDataWriter(path, OBJECTS_FILE_NAME);
                    log.info("  objects");
                }
            } catch (Exception exc) {
                throw new SAXException(exc);
            }
        }

    }

}
