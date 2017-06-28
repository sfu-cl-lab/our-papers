/**
 * $Id: ExportXMLApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ExportXMLApp.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.gui2.ProxURL;
import kdl.prox.util.Assert;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.*;
import java.util.List;


/**
 * Application that exports an entire database or part of it to an XML file for
 * later import. The twin of ImportXMLApp. NB: <B>Overwrites</B> existing file
 * if it exists. See src/xml/prox3db.dtd for the data format.
 * <p/>
 * NB: Requires Xerces2 Java Parser 2.5.0 Release (or better), which <B>must</B>
 * be ahead of the xerces.jar that comes with JDOM in the class path. Otherwise
 * the output file's DOCTYPE tag will be missing "PROX3DB":
 * <p/>
 * <!DOCTYPE PROX2DB SYSTEM "prox3db.dtd">
 *
 * @see ImportXMLApp
 */
public class ExportXMLApp {

    private static Logger log = Logger.getLogger(ExportXMLApp.class);

    /**
     * Overload that exports the entire database to an XML file.
     *
     * @param outputFile
     */
    public ExportXMLApp(File outputFile) throws Exception {
        this(outputFile, null, null);
    }

    /**
     * Full arg overload. Exports either the entire database or just the one
     * aspect specified by the two optional args.
     *
     * @param outputFile XML file to export to. <b>NB: will be overwritten if exists</b>
     * @param ioType     aspect to export. null if exporting entire database
     * @param exportSpec optional additional arg if ioType is non-null; names
     *                   the aspect of the database to export. specifically:<ul>
     *                   <li><code>ImportExportType.OBJECT_ATTRIBUTE</code>: object attribute name</code></li>
     *                   <li><code>ImportExportType.LINK_ATTRIBUTE</code>: link attribute name</li>
     *                   <li><code>ImportExportType.CONTAINER_ATTRIBUTE</code>: container attribute name</li>
     *                   <li><code>ImportExportType.CONTAINER</code>: container
     *                   name using unix-style path syntax. For example, to name
     *                   'c2' under 'c1' under the root: '/c1/c2'. Note that there
     */
    public ExportXMLApp(File outputFile,
                        ImportExportType ioType, String exportSpec) throws Exception {
        BufferedOutputStream outStream = null;  // null if not set
        try {
            // create an output stream
            if (outputFile.exists()) {
                log.warn("overwriting existing output file: " + outputFile);
            }
            outStream = new BufferedOutputStream(new FileOutputStream(outputFile));

            // setup Xerces2-J SAX output. NB: uses non-standard "additional" classes
            OutputFormat outputFormat = new OutputFormat();
            outputFormat.setIndent(1);
            outputFormat.setIndenting(true);
            outputFormat.setDoctype(null, "prox3db.dtd");
            XMLSerializer serializer = new XMLSerializer(outStream, outputFormat);
            ContentHandler contentHandler = serializer.asContentHandler();

            // do the export
            log.info("* exporting database to: " + outputFile);

            contentHandler.startDocument();
            contentHandler.startElement("", "", "PROX3DB", null);

            if (ioType == null) {
                saveObjects(contentHandler);
                saveLinks(contentHandler);
                saveAttributes(contentHandler);
                saveContainers(contentHandler);
            } else if (ioType == ImportExportType.CONTAINER) {
                ProxURL proxURL = new ProxURL("cont:/containers" + exportSpec);
                Container container = proxURL.getContainer(false);
                contentHandler.startElement("", "", "CONTAINERS", null);
                saveContainer(contentHandler, container);
                contentHandler.endElement("", "", "CONTAINERS");
            } else if (ioType == ImportExportType.LINK_ATTRIBUTE) {
                saveOneAttribute(contentHandler, "L", DB.getLinkAttrs(), exportSpec);
            } else if (ioType == ImportExportType.OBJECT_ATTRIBUTE) {
                saveOneAttribute(contentHandler, "O", DB.getObjectAttrs(), exportSpec);
            } else if (ioType == ImportExportType.CONTAINER_ATTRIBUTE) {
                saveOneAttribute(contentHandler, "C", DB.getContainerAttrs(), exportSpec);
            } else {
                throw new Exception("unknown ImportExportType: " + ioType);
            }

            contentHandler.endElement("", "", "PROX3DB");
            contentHandler.endDocument();

            log.info("* done exporting");
        } catch (Exception exc) {
            log.error("error: ", exc);
            throw exc;
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ioExc) {
                    log.error("error closing file", ioExc);
                }
            }
        }
    }

    /**
     * Closes open file readers
     *
     * @param bufferedReaders
     */
    private void closeReaders(BufferedReader[] bufferedReaders) {
        if (bufferedReaders == null) {
            return;
        }

        for (int idx = 0; idx < bufferedReaders.length; idx++) {
            BufferedReader bufferedReader = bufferedReaders[idx];
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.fatal("error closing reader for file: " + e);
                }
            }
        }
    }

    /**
     * Deletes temporary files
     *
     * @param fileNames
     */
    private void deleteFiles(String[] fileNames) {
        if (fileNames == null) {
            return;
        }

        for (int idx = 0; idx < fileNames.length; idx++) {
            String fileName = fileNames[idx];
            if (fileName != null) {
                File file = new File(fileName);
                if (!file.delete()) {
                    log.warn("Could not delete file " + fileName);
                }
            }
        }
    }

    /**
     * Creates an array of BufferedReades from a list of filenames
     *
     * @param fileNames
     * @return
     * @throws FileNotFoundException
     */
    private BufferedReader[] getReadersFromFileNames(String[] fileNames)
            throws FileNotFoundException {
        BufferedReader[] bufferedReaders = new BufferedReader[fileNames.length];
        for (int idx = 0; idx < fileNames.length; idx++) {
            String fileName = fileNames[idx];
            try {
                bufferedReaders[idx] = new BufferedReader(new FileReader(new File(fileName)));
            } catch (FileNotFoundException e) {
                log.fatal("cannot open file: " + fileName);
                throw e;
            }
        }
        return bufferedReaders;
    }

    /**
     * See printUsage() for args:
     * <p/>
     * hostAndPort inputFile nameObjStrAttr
     *
     * @param args
     */
    public static void main(String[] args) {
        Util.initProxApp();

        if ((args.length != 2) && (args.length != 4)) {
            log.fatal("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        String outputFileName = args[1];
        try {
            File outputFile = new File(outputFileName);
            DB.open(hostAndPort);
            if (args.length == 2) {
                new ExportXMLApp(outputFile);
            } else {
                String exportType = args[2];
                String exportSpec = args[3];
                new ExportXMLApp(outputFile,
                        ImportExportType.enumForType(exportType), exportSpec);
            }
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();
        }
    }

    private static void printUsage() {
        log.info("Usage: java " + ExportXMLApp.class.getName() +
                " hostAndPort outputFile [exportTypeEnum exportSpec]\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\toutputFile: XML file to save output to\n" +
                "\texportTypeEnum: optional arg that specifies a single " +
                "aspect of the database to export. to export entire database " +
                "don't pass this or the next arg. must be one of: " +
                "'object-attribute', 'link-attribute', 'container-attribute', or 'container'\n" +
                "\texportSpec: optional additional arg if exportTypeEnum is " +
                "non-null; names the aspect of the database to export. " +
                "specifically:'object-attribute': object attribute name; " +
                "'link-attribute': link attribute name; \n" +
                "'container-attribute': container attribute name; 'container': " +
                "container name using unix-style path syntax. for example, to " +
                "name 'c2' under 'c1' under the root: '/c1/c2'. Note that " +
                "there is no final '/'.\n");
    }

    /**
     * Exports a particular attribute
     *
     * @param contentHandler
     * @param tag
     * @param attrs
     * @param itemType
     * @throws SAXException
     * @throws IOException
     */
    private void saveAttribute(ContentHandler contentHandler, String tag,
                               Attributes attrs, String attrName, String itemType)
            throws SAXException, IOException {
        log.info("* saving attribute " + attrName);

        String[] outputFileNames = null;
        BufferedReader[] bufferedReaders = null;
        try {
            AttributesImpl attributes = new AttributesImpl();
            attributes.clear();
            attributes.addAttribute("", "", "NAME", "CDATA", attrName);
            if (itemType != null) {
                attributes.addAttribute("", "", "ITEM-TYPE", "CDATA", itemType);
            }
            attributes.addAttribute("", "", "DATA-TYPE", "CDATA", attrs.getAttrTypeDef(attrName));
            contentHandler.startElement("", "", tag, attributes);

            NST attrDataNST = attrs.getAttrDataNST(attrName);
            outputFileNames = ImportUtil.dumpNST(attrDataNST, "attr_" + attrName + "_");
            attrDataNST.release();
            bufferedReaders = getReadersFromFileNames(outputFileNames);
            String id;
            while ((id = bufferedReaders[0].readLine()) != null) {
//                id = MonetUtil.undelimitValue(id, DataTypeEnum.OID);
                attributes.clear();
                attributes.addAttribute("", "", "ITEM-ID", "CDATA", id);
                contentHandler.startElement("", "", "ATTR-VALUE", attributes);
                for (int idx = 1; idx < bufferedReaders.length; idx++) {
                    String value = Util.unQuote(bufferedReaders[idx].readLine());
                    contentHandler.startElement("", "", "COL-VALUE", null);
                    contentHandler.characters(value.toCharArray(), 0, value.length());
                    contentHandler.endElement("", "", "COL-VALUE");
                }
                contentHandler.endElement("", "", "ATTR-VALUE");
            }

            contentHandler.endElement("", "", tag);
        } finally {
            closeReaders(bufferedReaders);
            deleteFiles(outputFileNames);
        }
    }

    /**
     * Exports both object and links attributes, surrounded by ATTRIBUTES tag
     *
     * @param contentHandler
     * @throws SAXException
     * @throws IOException
     */
    private void saveAttributes(ContentHandler contentHandler)
            throws SAXException, IOException {
        contentHandler.startElement("", "", "ATTRIBUTES", null);
        saveAttributesOfType(contentHandler, "O", DB.getObjectAttrs());
        saveAttributesOfType(contentHandler, "L", DB.getLinkAttrs());
        saveAttributesOfType(contentHandler, "C", DB.getContainerAttrs());
        contentHandler.endElement("", "", "ATTRIBUTES");
    }

    /**
     * Exports all object/link/container attributes
     *
     * @param contentHandler
     * @param itemType
     * @param attrs
     */
    private void saveAttributesOfType(ContentHandler contentHandler,
                                      String itemType, Attributes attrs)
            throws IOException, SAXException {
        log.info("* saving " + itemType + " attributes");

        List attributeNames = attrs.getAttributeNames();
        for (int idx = 0; idx < attributeNames.size(); idx++) {
            String attrName = (String) attributeNames.get(idx);
            saveAttribute(contentHandler, "ATTRIBUTE", attrs, attrName, itemType);
        }
    }

    /**
     * Exports a given container, with its subgraph items, subgraph attributes,
     * and recurses on children
     *
     * @param contentHandler
     * @param container
     */
    private void saveContainer(ContentHandler contentHandler, Container container)
            throws SAXException, IOException {
        log.info("* saving container " + container.getName());

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "", "NAME", "CDATA", container.getName());
        attributes.addAttribute("", "", "ID", "CDATA", container.getOid() + "");
        contentHandler.startElement("", "", "CONTAINER", attributes);
        saveContainerSubgraphs(contentHandler, container);
        saveContainerSubgraphAttributes(contentHandler, container);
        saveContainerChildren(contentHandler, container);
        contentHandler.endElement("", "", "CONTAINER");
    }

    private void saveContainerChildren(ContentHandler contentHandler,
                                       Container container)
            throws SAXException, IOException {
        List childrenNames = container.getChildrenNames();
        for (int idx = 0; idx < childrenNames.size(); idx++) {
            String childName = (String) childrenNames.get(idx);
            Container child = container.getChild(childName);
            saveContainer(contentHandler, child);
        }
    }

    /**
     * Exports the root container, surrounded by CONTAINERS tag
     *
     * @param contentHandler
     * @throws SAXException
     * @throws IOException
     */
    private void saveContainers(ContentHandler contentHandler)
            throws SAXException, IOException {
        Container rootContainer = DB.getRootContainer();
        if (rootContainer.hasChildren()) {
            contentHandler.startElement("", "", "CONTAINERS", null);
            saveContainerChildren(contentHandler, rootContainer);
            contentHandler.endElement("", "", "CONTAINERS");
        }
    }

    private void saveContainerSubgraphAttributes(ContentHandler contentHandler,
                                                 Container container)
            throws SAXException, IOException {
        Attributes attrs = container.getSubgraphAttrs();
        List attributeNames = attrs.getAttributeNames();

        if (attributeNames.size() > 0) {
            contentHandler.startElement("", "", "SUBG-ATTRIBUTES", null);
            for (int idx = 0; idx < attributeNames.size(); idx++) {
                String attrName = (String) attributeNames.get(idx);
                saveAttribute(contentHandler, "SUBG-ATTRIBUTE", attrs, attrName, null);
            }
            contentHandler.endElement("", "", "SUBG-ATTRIBUTES");
        }

    }

    private void saveContainerSubgraphItems(ContentHandler contentHandler,
                                            Container container, boolean isObject)
            throws IOException, SAXException {

        String itemType = (isObject ? "O" : "L");
        String[] outputFileNames = null;
        BufferedReader[] bufferedReaders = null;
        try {
            AttributesImpl attributes = new AttributesImpl();

            outputFileNames = ImportUtil.dumpNST(container.getItemNST(isObject), "cont_");
            bufferedReaders = getReadersFromFileNames(outputFileNames);
            String itemId, subgId, name;
            while (((itemId = bufferedReaders[0].readLine()) != null) &&
                    ((subgId = bufferedReaders[1].readLine()) != null) &&
                    ((name = bufferedReaders[2].readLine()) != null)) {
                attributes.clear();
                attributes.addAttribute("", "", "SUBG-ID", "CDATA", subgId);
                attributes.addAttribute("", "", "ITEM-ID", "CDATA", itemId);
                attributes.addAttribute("", "", "ITEM-TYPE", "CDATA", itemType);
                attributes.addAttribute("", "", "NAME", "CDATA", Util.unQuote(name));
                contentHandler.startElement("", "", "ITEM", attributes);
                contentHandler.endElement("", "", "ITEM");
            }
        } finally {
            closeReaders(bufferedReaders);
            deleteFiles(outputFileNames);
        }
    }

    private void saveContainerSubgraphs(ContentHandler contentHandler,
                                        Container container)
            throws SAXException, IOException {
        if (container.getSubgraphCount() > 0) {
            contentHandler.startElement("", "", "SUBG-ITEMS", null);
            saveContainerSubgraphItems(contentHandler, container, true);
            saveContainerSubgraphItems(contentHandler, container, false);
            contentHandler.endElement("", "", "SUBG-ITEMS");
        }
    }

    /**
     * Exports the list of links
     *
     * @param contentHandler
     * @throws SAXException
     * @throws IOException
     */
    private void saveLinks(ContentHandler contentHandler) throws SAXException,
            IOException {
        log.info("* saving links");

        String[] outputFileNames = null;
        BufferedReader[] bufferedReaders = null;
        try {
            AttributesImpl attributes = new AttributesImpl();
            contentHandler.startElement("", "", "LINKS", attributes);

            outputFileNames = ImportUtil.dumpNST(DB.getLinkNST(), "link_");
            bufferedReaders = getReadersFromFileNames(outputFileNames);
            String oid, o1_id, o2_id;
            while (((oid = bufferedReaders[0].readLine()) != null) &&
                    ((o1_id = bufferedReaders[1].readLine()) != null) &&
                    ((o2_id = bufferedReaders[2].readLine()) != null)) {
                attributes.clear();
                attributes.addAttribute("", "", "ID", "CDATA", oid);
                attributes.addAttribute("", "", "O1-ID", "CDATA", o1_id);
                attributes.addAttribute("", "", "O2-ID", "CDATA", o2_id);
                contentHandler.startElement("", "", "LINK", attributes);
                contentHandler.endElement("", "", "LINK");
            }
            contentHandler.endElement("", "", "LINKS");
        } finally {
            closeReaders(bufferedReaders);
            deleteFiles(outputFileNames);
        }
    }

    /**
     * Exports the list of objects
     *
     * @param contentHandler
     * @throws SAXException
     */
    private void saveObjects(ContentHandler contentHandler) throws SAXException,
            IOException {
        log.info("* saving objects");

        String[] outputFileNames = null;
        BufferedReader[] bufferedReaders = null;
        try {
            AttributesImpl attributes = new AttributesImpl();
            contentHandler.startElement("", "", "OBJECTS", attributes);

            outputFileNames = ImportUtil.dumpNST(DB.getObjectNST(), "obj_");
            bufferedReaders = getReadersFromFileNames(outputFileNames);
            String oid;
            while ((oid = bufferedReaders[0].readLine()) != null) {
                attributes.clear();
                attributes.addAttribute("", "", "ID", "CDATA", oid);
                contentHandler.startElement("", "", "OBJECT", attributes);
                contentHandler.endElement("", "", "OBJECT");
            }

            contentHandler.endElement("", "", "OBJECTS");
        } finally {
            closeReaders(bufferedReaders);
            deleteFiles(outputFileNames);
        }
    }

    /**
     * Saves a single object or link attribute.
     *
     * @param contentHandler
     * @param itemType
     * @param attrs
     * @param attrName
     */
    private void saveOneAttribute(ContentHandler contentHandler, String itemType,
                                  Attributes attrs, String attrName) throws SAXException, IOException {
        Assert.condition(attrs.isAttributeDefined(attrName),
                "undefined attribute: " + attrName);
        contentHandler.startElement("", "", "ATTRIBUTES", null);
        saveAttribute(contentHandler, "ATTRIBUTE", attrs, attrName, itemType);
        contentHandler.endElement("", "", "ATTRIBUTES");
    }

}
