/**
 * $Id: RunSchemaAnalysisImpl.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import kdl.prox.db.SchemaAnalysis;
import org.apache.log4j.Logger;

/**
 * xx
 */
public class RunSchemaAnalysisImpl extends AbstractRunFileImpl {

    private static Logger log = Logger.getLogger(RunSchemaAnalysisImpl.class);


    public RunSchemaAnalysisImpl(SchemaAnalysis schemaAnalLE) {
        super(schemaAnalLE);
    }

    /**
     * Given a Set of StringS, returns a comma-separated string
     * with those strings.
     *
     * @param aSet
     * @return
     */
    private static String convertSetToString(Set aSet) {
        StringBuffer htmlSB = new StringBuffer();
        Iterator setIter = aSet.iterator();
        while (setIter.hasNext()) {
            String thisString = (String) setIter.next();
            htmlSB.append(thisString.trim());
            if (setIter.hasNext()) {
                htmlSB.append(", ");
            }
        }
        return htmlSB.toString();
    }

    public static String getDBSchemaHTML(SchemaAnalysis schemaAnalLE) {
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<html><body>\n");
        htmlSB.append("<h2>Schema Analysis: Report</h2>\n");
        htmlSB.append("<h2>Object Types</h2>\n");
        htmlSB.append(getItemsSchemaHTML(schemaAnalLE, true));
        htmlSB.append("<h2>Link Types</h2>\n");
        htmlSB.append(getItemsSchemaHTML(schemaAnalLE, false));
        htmlSB.append("</body></html>\n");
        return htmlSB.toString();
    }

    /**
     * Returns the schema analysis for objects or links
     *
     * @param schemaAnalLE
     * @param isObject
     * @return
     */
    private static String getItemsSchemaHTML(SchemaAnalysis schemaAnalLE, boolean isObject) {
        StringBuffer htmlSB = new StringBuffer();
        htmlSB.append("<table border=1 valign=top>\n");
        htmlSB.append("  <tr>\n");
        htmlSB.append("    <td><strong>Type</strong></td>\n");
        if (!isObject) {
            htmlSB.append("    <td><strong>Connects Types...</strong></td>\n");
        }
        htmlSB.append("    <td><strong>Associated Attributes</strong></td>\n");
        htmlSB.append("  </tr>\n");
        log.info("Getting " + (isObject ? "object" : "link") + " types");
        Set itemTypes = schemaAnalLE.getItemTypes(isObject);
        Iterator objTypesIter = itemTypes.iterator();
        while (objTypesIter.hasNext()) {
            String typeName = (String) objTypesIter.next();
            log.info("\tGetting attributes for " + typeName);
            String assocAttrs = convertSetToString(schemaAnalLE.getAttributeTypes(typeName, isObject));
            htmlSB.append("  <tr>\n");
            htmlSB.append("    <td><strong>");
            htmlSB.append(typeName);
            htmlSB.append("</strong></td>\n");
            if (!isObject) {
                log.info("\tGetting link connections for " + typeName);
                String connectsTypes = convertSetToString(schemaAnalLE.getLinkConnectedObjStatsAsStringSet(typeName));
                htmlSB.append("    <td>");
                htmlSB.append(connectsTypes);
                htmlSB.append("</td>\n");
            }
            htmlSB.append("    <td>");
            htmlSB.append(assocAttrs);
            htmlSB.append("</td>\n");
            htmlSB.append("  </tr>\n");
        }
        htmlSB.append("</table>\n");
        htmlSB.append("<p>\n");
        return htmlSB.toString();
    }

    public String getInputObjectString() {
        if (inputObject instanceof File) {
            return ((File) inputObject).getName();
        } else {
            return "<QGraph Query>";
        }
    }

    /**
     * Runs the query.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        try {
            startLog4jRouting();
            fireChange("status", null, "starting schema analysis");
            String dbSchemaHTML = getDBSchemaHTML((SchemaAnalysis) inputObject);
            fireChange("schemaHTML", null, dbSchemaHTML);
        } finally {
            stopLog4jRouting();
            fireChange("status", null, "finished schema analysis");
        }
    }

}
