/**
 * $Id: ImportTextApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.app;

import kdl.prox.db.AttrType;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Application that imports a database from a set of text files in a given directory.
 * Valid files and formats are:
 * <p/>
 * objects.data
 * id, oid
 * <p/>
 * links.data
 * link_id, oid / o1_id, oid / o2_id, oid
 * <p/>
 * attributes.data
 * name, str / item-type, str [O,L, or C] / data-type, str /data-file, str [av_*]
 * <p/>
 * av_*.data and av_si_*.data
 * id,oid / value,int [or the format of the attribute, e.g. year,int // gross/float]
 * <p/>
 * containers.data
 * id,int / name,str / parent,int / attr-file,str (si_attrs_*] / data-file,str [si_*]
 * <p/>
 * si_attrs_*.data
 * name, str / data-type, str / data-file, str [av_si_*]
 * <p/>
 * si_*.data
 * item_id,oid / subg_id,oid / name,str
 * <p/>
 * <p/>
 * If an attribute or a container is defined, the corresponding av_, si_ and av_si_ files MUST exist
 *
 * @see ImportXMLApp
 */
public class ImportTextApp {

    private static final Logger log = Logger.getLogger(ImportTextApp.class);

    private static final String ATTRIBUTES_FILE_NAME = "attributes";
    private static final String CONTAINERS_FILE_NAME = "containers";
    private static final String LINKS_FILE_NAME = "links";
    private static final String OBJECTS_FILE_NAME = "objects";

    private static final String OBJECTS_COL_NAMES = "id";
    private static final String OBJECTS_COLS_TYPES = "oid";
    private static final String LINKS_COL_NAMES = "link_id, o1_id, o2_id";
    private static final String LINKS_COL_TYPES = "oid, oid, oid";
    private static final String ATTRIBUTES_COL_NAMES = "name, item-type, data-type, data-file";
    private static final String ATTRIBUTES_COL_TYPES = "str, str, str, str";
    private static final String CONTAINERS_COL_NAMES = "id, name, parent, attr-file, data-file";
    private static final String CONTAINERS_COLS_TYPES = "int, str,int,str,str";
    private static final String SUBG_COL_NAMES = "item_id, subg_id, name";
    private static final String SUBG_COL_TYPES = "oid, oid, str";
    private static final String SUBG_ATTRS_COL_NAMES = "name, data-type, data-file";
    private static final String SUBG_ATTRS_COL_TYPES = "str, str, str";


    private String inputDir;

    /**
     * @param inputDir directory to import from.
     * @throws Exception
     */
    public ImportTextApp(String inputDir) throws Exception {
        Assert.stringNotEmpty(inputDir, "null inputDir");
        this.inputDir = inputDir;
        if (!new File(inputDir).exists()) {
            throw new IllegalArgumentException("Path not found: " + inputDir);
        }

        try {
            log.info("* importing database from " + inputDir);
            importAll();
            DB.commit();
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

        // done
        log.info("* done importing");
    }

    /**
     * Called by constructor, converts data loaded from text files into
     * real Proximity Three schema tables.
     */
    private void importAll() throws IOException {
        log.info("  Loading object table");
        NST objsNST = ImportUtil.loadNST(inputDir, OBJECTS_FILE_NAME, OBJECTS_COL_NAMES, OBJECTS_COLS_TYPES);
        if (objsNST != null) {
            importItems(objsNST, DB.OBJ_NST_NAME);
            objsNST.release();
        }

        log.info("  Loading link table");
        NST linkNST = ImportUtil.loadNST(inputDir, LINKS_FILE_NAME, LINKS_COL_NAMES, LINKS_COL_TYPES);
        if (linkNST != null) {
            importItems(linkNST, DB.LINK_NST_NAME);
            linkNST.release();
        }

        log.info("  Loading attributes");
        NST attrsNST = ImportUtil.loadNST(inputDir, ATTRIBUTES_FILE_NAME, ATTRIBUTES_COL_NAMES, ATTRIBUTES_COL_TYPES);
        if (attrsNST != null) {
            importAttributes(attrsNST);
            attrsNST.release();
        }

        log.info("  Loading containers");
        NST contsNST = ImportUtil.loadNST(inputDir, CONTAINERS_FILE_NAME, CONTAINERS_COL_NAMES, CONTAINERS_COLS_TYPES);
        if (contsNST != null) {
            importContainers(contsNST);
            contsNST.release();
        }
    }

    /**
     * Goes through the list of attributes, loads each attribute data file,
     * and creates a new attribute in ObjAttr or LinkAttr
     *
     * @param attrsNST
     */
    private void importAttributes(NST attrsNST) throws IOException {
        int nstSize = attrsNST.getRowCount();
        String[] attrNames = new String[nstSize];
        String[] attrItemTypes = new String[nstSize];
        String[] attrTypes = new String[nstSize];
        String[] attrFiles = new String[nstSize];
        int rowCnt = 0;
        ResultSet dataRS = attrsNST.selectRows();
        while (dataRS.next()) {
            attrNames[rowCnt] = dataRS.getString(1);
            attrItemTypes[rowCnt] = dataRS.getString(2);
            attrTypes[rowCnt] = dataRS.getString(3);
            attrFiles[rowCnt] = dataRS.getString(4);
            rowCnt++;
        }
        // now go through the array loading the av_* files, and creating the attribute
        for (int i = 0; i < nstSize; i++) {
            String dataFileName = attrFiles[i];
            log.info("  Loading attribute: " + dataFileName);
            Attributes attributes;
            if ("C".equals(attrItemTypes[i])) {
                attributes = DB.getContainerAttrs();
            } else if ("O".equals(attrItemTypes[i])) {
                attributes = DB.getObjectAttrs();
            } else if ("L".equals(attrItemTypes[i])) {
                attributes = DB.getLinkAttrs();
            } else {
                throw new IllegalArgumentException("invalid itemType, should be one " +
                        "of O,L, or C :" + attrItemTypes[i]);
            }

            String[] attrColNameTypes = AttrType.attrTypeDefToColNamesAndTypes(attrTypes[i]);
            NST attrDataNST = ImportUtil.loadNST(inputDir, dataFileName, attrColNameTypes[0], attrColNameTypes[1]);

            if (attributes.isAttributeDefined(attrNames[i])) {
                attributes.getAttrDataNST(attrNames[i]).insertRowsFromNST(attrDataNST);
            } else {
                attributes.defineAttributeWithData(attrNames[i], attrTypes[i], attrDataNST);
            }

            attrDataNST.release();
        }
    }

    /**
     * Reads each container and loads it into the Prox DB.
     *
     * @param contsNST
     */
    private void importContainers(NST contsNST) throws IOException {
        int NSTSize = contsNST.getRowCount();
        int[] colsIDs = new int[NSTSize];
        String[] colsNames = new String[NSTSize];
        int[] colsParents = new int[NSTSize];
        String[] attrsFiles = new String[NSTSize];
        String[] colsFiles = new String[NSTSize];
        int rowCnt = 0;
        ResultSet dataRS = contsNST.selectRows();
        while (dataRS.next()) {
            colsIDs[rowCnt] = dataRS.getInt(1);
            colsNames[rowCnt] = dataRS.getString(2);
            colsParents[rowCnt] = dataRS.getInt(3);
            attrsFiles[rowCnt] = dataRS.getString(4);
            colsFiles[rowCnt] = dataRS.getString(5);
            rowCnt++;
        }
        // now go through the array loading the si_* files, and creating the containers
        for (int i = 0; i < NSTSize; i++) {
            String dataFileName = colsFiles[i];
            log.info("  Loading container: " + dataFileName);

            NST objectsDataNST = ImportUtil.loadNST(inputDir, dataFileName + ".objects", SUBG_COL_NAMES, SUBG_COL_TYPES);
            NST linksDataNST = ImportUtil.loadNST(inputDir, dataFileName + ".links", SUBG_COL_NAMES, SUBG_COL_TYPES);
            NST siAttrsDataNST = ImportUtil.loadNST(inputDir, attrsFiles[i], SUBG_ATTRS_COL_NAMES, SUBG_ATTRS_COL_TYPES);

            Container parentContainer = new Container(colsParents[i]);
            Container childContainer = parentContainer.createChildFromTempSGINSTs(colsIDs[i], colsNames[i],
                    objectsDataNST, linksDataNST);

            Attributes subgraphAttrs = childContainer.getSubgraphAttrs();
            importContainerAttributes(subgraphAttrs, siAttrsDataNST);

            objectsDataNST.release();
            linksDataNST.release();
            siAttrsDataNST.release();
        }
        contsNST.release();
    }

    /**
     * Goes through the list of subgraph attributes, loads each attribute data file,
     * and creates a new attribute in the appropriate subgraph attributes object
     *
     * @param attrs
     * @param dataNST
     */
    private void importContainerAttributes(Attributes attrs, NST dataNST) throws IOException {
        Assert.notNull(attrs, "null attrs");

        int nstSize = dataNST.getRowCount();
        String[] attrNames = new String[nstSize];
        String[] attrTypes = new String[nstSize];
        String[] attrFiles = new String[nstSize];
        int rowCnt = 0;
        ResultSet dataRS = dataNST.selectRows();
        while (dataRS.next()) {
            attrNames[rowCnt] = dataRS.getString(1);
            attrTypes[rowCnt] = dataRS.getString(2);
            attrFiles[rowCnt] = dataRS.getString(3);
            rowCnt++;
        }
        // now go through the array loading the av_* files, and creating the attribute
        for (int i = 0; i < nstSize; i++) {
            String dataFileName = attrFiles[i];
            log.info("  Loading container attribute: " + dataFileName);
            String[] attrColNameTypes = AttrType.attrTypeDefToColNamesAndTypes(attrTypes[i]);
            NST attrDataNST = ImportUtil.loadNST(inputDir, dataFileName, attrColNameTypes[0], attrColNameTypes[1]);
            attrs.defineAttributeWithData(attrNames[i], attrTypes[i], attrDataNST);
            attrDataNST.release();
        }
        dataNST.release();
    }

    /**
     * Loads the prox_object or prox_link tables
     * It works by deleting the existing tables, and creating a new prox_object
     * or prox_link with the passed in NST
     *
     * @param dataNST
     */
    private static void importItems(NST dataNST, String tableName) {
        Assert.stringNotEmpty(tableName, "empty tableName");
        // It is OK to insert,
        // since we've already checked that they're empty (or that overwrite is OK, noChecks = true) in the validation of the XML
        NST itemNST = new NST(tableName);
        itemNST.insertRowsFromNST(dataNST);
        itemNST.release();
    }

    /**
     * See printUsage() for args: hostAndPort inputFile [isCalcStatsOnly]
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }
        String hostAndPort = args[0];
        String inputDirPath = args[1];
        Util.initProxApp();        // configures Log4J

        try {
            DB.open(hostAndPort);
            new ImportTextApp(inputDirPath);
        } catch (Exception exc) {
            System.out.println("error: " + exc);
            System.exit(-1);
        } finally {
            DB.close();    // NB: crucial because we need to commit
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java " + ImportTextApp.class.getName() +
                " hostAndPort inputFile\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tinputDir: Path to the directory where the files live\n");
    }

}
