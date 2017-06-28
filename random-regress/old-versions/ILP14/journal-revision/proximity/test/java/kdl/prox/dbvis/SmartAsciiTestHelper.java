/**
 * $Id: SmartAsciiTestHelper.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import kdl.prox.app.ImportAsciiGraph;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.db.ItemTypeEnum;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.util.Assert;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;

/**
 * Defines utility methods that help use smart ASCII files for populating
 * databases for unit tests.
 */
public class SmartAsciiTestHelper {

    private static final Logger log = Logger.getLogger(SmartAsciiTestHelper.class);


    /**
     * Defines the attributes specified in attrDefs. For example:
     * <code>
     * <pre>
     * new String[][]{{"O", "str", "os"}, // object string attribute named 'os'
     *   {"O", "str", "names"},           // object "" 'names'
     *   {"L", "str", "ls"},              // link "" 'ls'
     * };
     * </pre>
     * </code>
     *
     * @param attrDefs nested String[] instances, each of which specifies an
     *                 attribute definition, via three elements in this order:
     *                 0: "O" or "L" (for object or link attr def),
     *                 1: attribute data type (String values from DataTypeEnum, e.g., "str")
     */
    private static void defineAttributes(String[][] attrDefs) {
        for (int attrDefTupleIdx = 0; attrDefTupleIdx < attrDefs.length; attrDefTupleIdx++) {
            String[] attrDef = attrDefs[attrDefTupleIdx];
            String itemType = attrDef[0];
            String dataType = attrDef[1];
            String attrName = attrDef[2];

            ItemTypeEnum itemTypeEnum = ItemTypeEnum.valueOf(itemType);
            Assert.notNull(DataTypeEnum.enumForType(dataType),
                    "bad data type: " + dataType);

            Attributes attributes;
            if (itemTypeEnum == ItemTypeEnum.OBJECT) {
                attributes = DB.getObjectAttrs();
            } else {
                attributes = DB.getLinkAttrs();
            }

            attributes.defineAttributeOrClearValuesIfExists(attrName, dataType);
        }
    }

    /**
     * Loads the specified smart ASCII file into the database, first defining
     * the attributes in attrDefs.
     *
     * @param inputFileURL smart ASCII file URL
     * @param attrDefs  attribute definitions, as documented in defineAttributes()
     * @see #defineAttributes
     */
    public static void loadDB(URL inputFileURL,
                                             String[][] attrDefs,
                                             String nameObjStrAttr) throws Exception {
        // clear database fully
        TestUtil.openTestConnection();
        DB.clearDB();
        DB.initEmptyDB();

        // and now load
        String fileName = inputFileURL.getFile();
        fileName = fileName.replaceAll("%20", " ");     // replace "%20" with " " for Windows bug - paths with spaces cause below smartAsciiFile.exists() -> false

        File inputFile = new File(fileName);

        log.warn("loadDB(): clearing DB then loading: " + inputFileURL);
        Assert.condition(inputFile.exists(), "file doesn't exist: " + inputFileURL);

        DB.clearDB();
        DB.initEmptyDB();
        defineAttributes(attrDefs);

        FileInputStream fileInputStream = new FileInputStream(inputFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        ImportAsciiGraph.doImport(bufferedReader, nameObjStrAttr);
    }

}
