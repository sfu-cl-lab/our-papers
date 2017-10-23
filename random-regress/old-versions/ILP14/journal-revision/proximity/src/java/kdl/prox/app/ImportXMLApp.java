/**
 * $Id: ImportXMLApp.java 3712 2007-11-06 14:10:01Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

/**
 * Application that imports an entire database or part of it from an XML file.
 * The twin of ExportXMLApp. See src/xml/prox3db.dtd for the data format.
 * <p/>
 * NB: Requires Xerces2 Java Parser 2.5.0 Release (or better), which <B>must</B>
 * be ahead of the xerces.jar that comes with JDOM in the class path. todo true?
 *
 * @see ExportXMLApp
 */
public class ImportXMLApp {

    private static final Logger log = Logger.getLogger(ImportXMLApp.class);

    private HashMap containerIdMappings = new HashMap();

    // import counters
    protected int numAttributes = 0;
    protected int numAttrValues = 0;
    protected int numContainers = 0;
    protected int numLinks = 0;
    protected int numObjects = 0;
    protected int numSubgraphItems = 0;
    protected int numSubgraphs = 0;

    /**
     * Do nothing. Just make the convertXMLToText method available
     */
    public ImportXMLApp() {

    }

    /**
     * Full-arg constructor. Uses standard (Xerces2) SAX parsing in order to
     * better scale (over DOM) to very large files.
     *
     * @param inputFile ProxDataML (XML) file to import from. see
     *                  proximity3/src/xml/prox3db.dtd for the data format
     * @param noChecks  when set to true, it allows the addition of new objects, links, and attr vals
     * @throws Exception
     */
    public ImportXMLApp(File inputFile, boolean noChecks) throws Exception {
        Assert.notNull(inputFile, "null inputFile");
        log.info("* importing database from " + inputFile + (noChecks ? " NOT CHECKING FOR DUPLICATES " : ""));

        File tempDir = null;
        try {
//            File tempDir = ImportUtil.getTempDir(true);
            tempDir = File.createTempFile("prox-xml-import", "");
            if (!tempDir.delete())
                throw new IOException();
            if (!tempDir.mkdir())
                throw new IOException();
            final String tempDirPath = tempDir.getPath();

            // convert XML to Text
            // As a side effect, fills in the containerIdMappings map, as it parses the XML
            convertXMLToText(inputFile, noChecks, tempDirPath);

            // load text and convert to prox
            new ImportTextApp(tempDirPath);

            // apply container mappings to container attributes
            applyContainerIDMappings(tempDirPath);

            DB.commit();

        } catch (Exception e) {
            throw e;
        } finally {
            ImportUtil.deleteDir(tempDir);
        }

        // done
        log.info("* done importing; counts: " + numObjects +
                " objects, " + numLinks + " links, " + numAttributes +
                " attributes, " + numAttrValues + " attribute values, " +
                numContainers + " containers, " + numSubgraphItems + " subgraph items");
        if (containerIdMappings.size() > 0) {
            log.info("* NOTE: RECODED THE FOLLOWING CONTAINER IDs");
            Vector v = getReverseSortedContainerIDMappings(containerIdMappings);
            for (int i = 0; i < v.size(); i++) {
                String contID = (String) v.elementAt(i);
                String newContID = (String) containerIdMappings.get(contID);
                log.info("  - " + contID + " to " + newContID);
            }
        }

    }

    /**
     * During the parsing of the XML file, a mapping is created between containerIDs in the source
     * and new containerIDs for those Ids that already exist in the DB. For example, if your DB already
     * has a container 1, and you want to import a new one with the ID, it is placed in the map with a new ID 2.
     * When importing container attributes, then, we must go through that mapping and apply it to the attribute values,
     * to make sure that the correspondence still holds. In the example above, attribute values for container 1 in the
     * XML file will be set for (the new) container 2.
     */
    private void applyContainerIDMappings(String path) throws IOException {
        // apply mappings to container attributes LOADED IN THIS IMPORT!
        NST loadedAttrsNST = ImportUtil.loadNST(path, ImportUtil.ATTRIBUTES_FILE_NAME,
                "name, item-type, data-type, data-file", "str, str, str, str");
        ResultSet resultSet = loadedAttrsNST.selectRows("name, item-type");
        while (resultSet.next()) {
            String name = resultSet.getString(1);
            String itemType = resultSet.getString(2);
            if ("C".equals(itemType)) {
                // traverse in reverse order! Otherwise, we'd overwrite lower mappings with new ones
                // (ie, if the mappings have 1 -> 2 and 2 -> 3, make sure that we first change all 2 -> 3 and then
                //  the 1 -> 2!)
                Vector v = getReverseSortedContainerIDMappings(containerIdMappings);
                for (int i = 0; i < v.size(); i++) {
                    String contID = (String) v.elementAt(i);
                    String newContID = (String) containerIdMappings.get(contID);
                    NST attrDataNST = DB.getContainerAttrs().getAttrDataNST(name);
                    attrDataNST.replace("id EQ " + contID, "id", newContID);
                }

            }
        }
        loadedAttrsNST.release();
    }

    /**
     * Converts an XML file into a set of text files that can be imported via ImportTextApp
     *
     * @param inputFile
     * @param noChecks
     * @param tempDirPath
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public void convertXMLToText(File inputFile, boolean noChecks, String tempDirPath)
            throws ParserConfigurationException, SAXException, IOException {
        log.info("* converting xml to bulk import text files; dir: " + tempDirPath);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);       // todo set to false because it was giving a Memory Out of Bounds error, but it should be true
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputFile, new InputParser(tempDirPath, noChecks));
    }

    /**
     * Reverses the keySet
     *
     * @param containerIdMappings
     * @return
     */
    private Vector getReverseSortedContainerIDMappings(HashMap containerIdMappings) {
        Vector v = new Vector(containerIdMappings.keySet());
        Collections.sort(v, new Comparator() {

            public int compare(Object o1, Object o2) {
                return Integer.parseInt((String) o2) - Integer.parseInt((String) o1);
            }
        });
        return v;
    }

    /**
     * See printUsage() for args: hostAndPort inputFile [isCalcStatsOnly]
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }
        String hostAndPort = args[0];
        String inputFileArg = args[1];
        boolean noChecks = false;
        if (args.length == 3) {
            noChecks = "TRUE".equals(args[2].toUpperCase());
        }
        Util.initProxApp();        // configures Log4J

        try {
            DB.open(hostAndPort);
            new ImportXMLApp(new File(inputFileArg), noChecks);
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();    // NB: crucial because we need to commit
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + ImportXMLApp.class.getName() +
                " hostAndPort inputFile\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tinputFile: Location of input file\n" +
                "\tallow new objects/links/attrs [true | FALSE]\n");
    }

    private class InputParser extends DefaultHandler {
        private static final int INFO_MODULO_COUNT = 500000;

        // names of format and data files created during parsing. names of av_N
        // and si_N files are based on numAttributes and numContainers, respectively
        // Path where the files must be written
        private String path;
        private static final String AV_FILE_PREFIX = "av_";
        private static final String SI_FILE_PREFIX = "si_";

        private int nextAVFileNum = 0;          // counter for av_N file names

        // state for current <ATTR-VALUE> elements
        private StringBuffer currAttrValueSB = null;
        private String currAttrItemID = null;

        // state for <ATTRIBUTE> elements
        private Writer attributesDataWriter = null;
        private Writer currAVDataWriter = null;

        // state for <CONTAINER> elements
        private Writer containersDataWriter = null;
        private Stack currSIObjectDataWriterStack = new Stack();
        private Stack currSILinkDataWriterStack = new Stack();
        private Stack currContainerParentStack = new Stack();
        private Stack currSIAttributesWriterStack = new Stack();

        // state for <LINK> elements
        private Writer linksDataWriter = null;

        // state for <OBJECT> elements
        private Writer objectsDataWriter = null;

        private boolean noChecks;
        private int nextContainerID = DB.getRootContainer().getNextContainerID();

        public InputParser(String tempDir, boolean noChecks) {
            this.path = tempDir;
            this.noChecks = noChecks;
        }

        /**
         * DefaultHandler method.
         *
         * @param buf
         * @param offset
         * @param len
         */
        public void characters(char[] buf, int offset, int len) {
            // for <COL-VALUE>:
            StringBuffer intermediateResult = new StringBuffer();
            intermediateResult.append(buf, offset, len);
            currAttrValueSB.append(ImportUtil.getCleanAttrValue(intermediateResult));   // NB: does replacement of 'bad' chars
        }

        /**
         * DefaultHandler method.
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
                    String value = currAttrValueSB.toString();
                    ImportUtil.writeLine(currAVDataWriter, new String[]{currAttrItemID, value});
                    currAttrItemID = null;
                    currAttrValueSB = null;
                    if ((numAttrValues % INFO_MODULO_COUNT) == 0) {
                        log.info("    " + numAttrValues);
                    }
                } else if (qName.equalsIgnoreCase("ATTRIBUTE")) {
                    numAttributes++;
                    nextAVFileNum++;
                    currAVDataWriter.close();
                } else if (qName.equalsIgnoreCase("ATTRIBUTES")) {
                    attributesDataWriter.close();
                } else if (qName.equalsIgnoreCase("COL-VALUE")) {
                    // do nothing; COLUMN_DELIMITER adder in startElement
                } else if (qName.equalsIgnoreCase("CONTAINER")) {
                    numContainers++;
                    currContainerParentStack.pop();
                    ((Writer) currSIObjectDataWriterStack.pop()).close();
                    ((Writer) currSILinkDataWriterStack.pop()).close();
                    ((Writer) currSIAttributesWriterStack.pop()).close();
                } else if (qName.equalsIgnoreCase("CONTAINERS")) {
                    currContainerParentStack.pop();
                    Assert.condition(currContainerParentStack.size() == 0,
                            "Container parent stack should be empty");
                    Assert.condition(currSIObjectDataWriterStack.size() == 0,
                            "Container object writer stack should be empty");
                    Assert.condition(currSILinkDataWriterStack.size() == 0,
                            "Container link writer stack should be empty");
                    containersDataWriter.close();
                } else if (qName.equalsIgnoreCase("ITEM")) {
                    numSubgraphItems++;
                    if ((numSubgraphItems % INFO_MODULO_COUNT) == 0) {
                        log.info("    " + numSubgraphItems);
                    }
                } else if (qName.equalsIgnoreCase("LINK")) {
                    numLinks++;
                    if ((numLinks % INFO_MODULO_COUNT) == 0) {
                        log.info("    " + numLinks);
                    }
                } else if (qName.equalsIgnoreCase("LINKS")) {
                    linksDataWriter.close();
                } else if (qName.equalsIgnoreCase("OBJECT")) {
                    numObjects++;
                    if ((numObjects % INFO_MODULO_COUNT) == 0) {
                        log.info("    " + numObjects);
                    }
                } else if (qName.equalsIgnoreCase("OBJECTS")) {
                    objectsDataWriter.close();
                } else if (qName.equalsIgnoreCase("SUBG-ATTRIBUTE")) {
                    numAttributes++;
                    nextAVFileNum++;
                    currAVDataWriter.close();
                }
            } catch (Exception exc) {
                throw new SAXException(exc + "qName: " + qName);
            }
        }


        /**
         * Returns the right Attributes object for O,L, or C attributes
         *
         * @param itemType
         * @return
         */
        private kdl.prox.db.Attributes getAttributesForItemType(String itemType) {
            if ("C".equals(itemType)) {
                return DB.getContainerAttrs();
            } else if ("O".equals(itemType)) {
                return DB.getObjectAttrs();
            } else if ("L".equals(itemType)) {
                return DB.getLinkAttrs();
            } else {
                throw new IllegalArgumentException("invalid itemType, should be one " +
                        "of O,L, or C :" + itemType);
            }
        }

        /**
         * Checks whether a container with that ID already exists. If so, it gets the next ID, and
         * saves it in a mapping, so that Container attributes can also be re-coded.
         *
         * @param containerID
         * @return
         */
        private String getValidContainerID(String containerID) {
            int containerOID = Integer.parseInt(containerID);
            String newContainerOID = containerID;
            if (containerIdMappings.containsValue(containerID) || Container.isContainerIdDefined(containerOID)) {
                if (!noChecks) {
                    throw new IllegalArgumentException("Container ID already exists " +
                            "in the database: " + containerID + ". You may use the noChecks flag if you want " +
                            "to automatically recode container IDs from your XML file.");
                }
                newContainerOID = nextContainerID + "";
                containerIdMappings.put(containerID, newContainerOID);
                nextContainerID++;
            }
            return newContainerOID;
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
            try {
                if (qName.equalsIgnoreCase("ATTR-VALUE")) {
                    currAttrItemID = attributes.getValue("ITEM-ID");
                    currAttrValueSB = new StringBuffer();
                } else if (qName.equalsIgnoreCase("ATTRIBUTE")) {
                    String attrName = attributes.getValue("NAME");
                    String itemType = attributes.getValue("ITEM-TYPE");     // item type (O, L, or C)
                    String attrDataType = attributes.getValue("DATA-TYPE").toLowerCase();   // data type. NB: for various reasons, might be upper case, but we require lower case
                    verifyAttributeDoesNotExist(itemType, attrName, attrDataType);
                    String newAVFile = AV_FILE_PREFIX + nextAVFileNum;
                    currAVDataWriter = ImportUtil.getDataWriter(path, newAVFile);
                    ImportUtil.writeLine(attributesDataWriter,
                            new String[]{attrName, itemType, attrDataType, newAVFile});
                    log.info("  attribute: " + attrName + ", " + itemType + ", " + attrDataType);
                } else if (qName.equalsIgnoreCase("ATTRIBUTES")) {
                    attributesDataWriter = ImportUtil.getDataWriter(path, ImportUtil.ATTRIBUTES_FILE_NAME);
                    log.info("  attributes");
                } else if (qName.equalsIgnoreCase("COL-VALUE")) {
                    if (currAttrValueSB.length() > 0) {
                        currAttrValueSB.append(ImportUtil.COLUMN_DELIMITER);
                    }
                } else if (qName.equalsIgnoreCase("CONTAINER")) {
                    String containerID = attributes.getValue("ID");
                    String containerName = attributes.getValue("NAME");
                    verifyContainerDoesNotExist(containerName);
                    containerID = getValidContainerID(containerID);
                    String parent = (String) currContainerParentStack.peek();
                    currContainerParentStack.push(containerID);
                    String newSIFileName = SI_FILE_PREFIX + containerID;
                    currSIObjectDataWriterStack.push(ImportUtil.getDataWriter(path, newSIFileName + ".objects"));
                    currSILinkDataWriterStack.push(ImportUtil.getDataWriter(path, newSIFileName + ".links"));
                    String newSIAttrFileName = SI_FILE_PREFIX + "attrs_" + containerID;
                    currSIAttributesWriterStack.push(ImportUtil.getDataWriter(path, newSIAttrFileName));
                    ImportUtil.writeLine(containersDataWriter,
                            new String[]{containerID,
                                    containerName,
                                    parent,
                                    newSIAttrFileName,
                                    newSIFileName});
                    log.info("  container: " + containerName);
                } else if (qName.equalsIgnoreCase("CONTAINERS")) {
                    currContainerParentStack.push("-1");
                    containersDataWriter = ImportUtil.getDataWriter(path, ImportUtil.CONTAINERS_FILE_NAME);
                    log.info("  containers");
                } else if (qName.equalsIgnoreCase("ITEM")) {
                    String subgID = attributes.getValue("SUBG-ID");
                    String itemID = attributes.getValue("ITEM-ID");
                    String itemType = attributes.getValue("ITEM-TYPE");     // item type
                    String name = attributes.getValue("NAME");
                    boolean isObject = itemType.equalsIgnoreCase("O");
                    if (isObject) {
                        ImportUtil.writeLine((Writer) currSIObjectDataWriterStack.peek(),
                                new String[]{itemID, subgID, name});
                    } else {
                        ImportUtil.writeLine((Writer) currSILinkDataWriterStack.peek(),
                                new String[]{itemID, subgID, name});
                    }
                } else if (qName.equalsIgnoreCase("LINK")) {
                    String linkIDStr = attributes.getValue("ID");
                    String o1ID = attributes.getValue("O1-ID");
                    String o2ID = attributes.getValue("O2-ID");
                    ImportUtil.writeLine(linksDataWriter, new String[]{linkIDStr, o1ID, o2ID});
                } else if (qName.equalsIgnoreCase("LINKS")) {
                    verifyLinkTableIsEmpty();
                    linksDataWriter = ImportUtil.getDataWriter(path, ImportUtil.LINKS_FILE_NAME);
                    log.info("  links");
                } else if (qName.equalsIgnoreCase("OBJECT")) {
                    String objID = attributes.getValue("ID");
                    ImportUtil.writeLine(objectsDataWriter, new String[]{objID});
                } else if (qName.equalsIgnoreCase("OBJECTS")) {
                    verifyObjectTableIsEmpty();
                    objectsDataWriter = ImportUtil.getDataWriter(path, ImportUtil.OBJECTS_FILE_NAME);
                    log.info("  objects");
                } else if (qName.equalsIgnoreCase("SUBG-ATTRIBUTE")) {
                    String attrName = attributes.getValue("NAME");
                    String dataType = attributes.getValue("DATA-TYPE");     // data type
                    String newAVFile = AV_FILE_PREFIX + SI_FILE_PREFIX + nextAVFileNum;
                    currAVDataWriter = ImportUtil.getDataWriter(path, newAVFile);
                    ImportUtil.writeLine((Writer) currSIAttributesWriterStack.peek(),
                            new String[]{attrName, dataType, newAVFile});
                    log.info("  subg attribute: " + attrName + ", " + dataType);
                }
            } catch (Exception exc) {
                throw new SAXException(exc);
            }
        }


        private void verifyAttributeDoesNotExist(String itemType, String attrName, String attrType) {
            kdl.prox.db.Attributes attrs = getAttributesForItemType(itemType);
            boolean isAttributeDefined = attrs.isAttributeDefined(attrName);

            if (isAttributeDefined && !noChecks) {
                throw new IllegalArgumentException("Attribute already exists " +
                        "in the database: " + attrName + ". You may use the noChecks flag if you want to ignore this check.");
            }
            if (isAttributeDefined && noChecks && !attrType.equals(attrs.getAttrTypeDef(attrName))) {
                throw new IllegalArgumentException("Error: attribute already defined with a different type "
                        + attrs.getAttrTypeDef(attrName) + ", not " + attrType);
            }
        }

        private void verifyContainerDoesNotExist(String containerName) {
            // only check the name if the container being examined will be added at the root
            if (currContainerParentStack.size() == 1) {
                if (DB.getRootContainer().hasChild(containerName)) {
                    throw new IllegalArgumentException("Container name already exists " +
                            "in the database: " + containerName);
                }
            }
        }

        private void verifyLinkTableIsEmpty() throws Exception {
            if (noChecks) return;

            if (DB.getLinkNST().getRowCount() > 0) {
                throw new IllegalArgumentException("The link table is not empty; " +
                        "you cannot import new links into a database that already " +
                        "has links. You may use the noChecks flag if you want to ignore this check.");
            }
        }

        private void verifyObjectTableIsEmpty() throws Exception {
            if (noChecks) return;

            if (DB.getObjectNST().getRowCount() > 0) {
                throw new IllegalArgumentException("The object table is not empty; " +
                        "you cannot import new objects into a database that already " +
                        "has objects. You may use the noChecks flag if you want to ignore this check.");
            }
        }
    }

}
