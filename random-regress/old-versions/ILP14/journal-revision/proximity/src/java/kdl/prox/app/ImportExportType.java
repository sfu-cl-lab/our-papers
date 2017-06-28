/**
 * $Id: ImportExportType.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * An enumeration of Monet's data types. NB: Our convention is to use upper case
 * names, even though Monet's types are lower case. This is because some
 * lower case names are Java reserved words (e.g., 'int'). However, the myName
 * value is the actual Monet data type.
 */
package kdl.prox.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Typesafe enum used by ExportXMLApp and ImportXMLApp when users want to export
 * or import only one aspect of a database.
 *
 * @see ExportXMLApp
 * @see ImportXMLApp
 */
public class ImportExportType {

    private static List ioTypeEnums = new ArrayList();    // all known ExportTypeEnumS. filled by constructor

    public static final ImportExportType OBJECT_ATTRIBUTE = new ImportExportType("object-attribute");
    public static final ImportExportType LINK_ATTRIBUTE = new ImportExportType("link-attribute");
    public static final ImportExportType CONTAINER_ATTRIBUTE = new ImportExportType("container-attribute");
    public static final ImportExportType CONTAINER = new ImportExportType("container");

    private final String myName; // for debug only


    private ImportExportType(String name) {
        myName = name;
        ioTypeEnums.add(this);
    }

    public String toString() {
        return myName;
    }

    /**
     * Returns the ImportExportType for exportType. Returns null if none found.
     * NB: Ignores case.
     *
     * @param exportType
     * @return
     */
    public static ImportExportType enumForType(String exportType) {
        for (Iterator ioTypeEnumIter = ioTypeEnums.iterator(); ioTypeEnumIter.hasNext();) {
            ImportExportType ioType = (ImportExportType) ioTypeEnumIter.next();
            if (ioType.myName.equalsIgnoreCase(exportType)) {
                return ioType;    // found
            }
        }
        return null;    // not found
    }

}
