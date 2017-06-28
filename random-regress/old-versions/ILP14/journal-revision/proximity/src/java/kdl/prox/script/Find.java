/**
 * $Id: Find.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * Supports finding objects based on string single-column attribute values.
 */
public class Find {

    private static Logger log = Logger.getLogger(Find.class);


    /**
     * Overload that searches on all string attributes.
     *
     * @param pattern
     * @return Map that maps attribute names to a List of matching object IDs (Integers)
     */
    public static Map findObjects(String pattern) {
        Attributes objectAttrs = DB.getObjectAttrs();
        List attrNames = objectAttrs.getAttributesOfType(DataTypeEnum.STR);
        return findObjects(pattern, attrNames);
    }

    /**
     * Overload that searches on a given attribute
     */
    public static Map findObjects(String pattern, String attrName) {
        ArrayList attrNames = new ArrayList();
        attrNames.add(attrName);
        return findObjects(pattern, attrNames);
    }

    /**
     * Returns object IDs that match pattern, in a set of attributes.
     *
     * @param pattern
     * @param attrNames
     * @return
     */

    public static Map findObjects(String pattern, List attrNames) {
        HashMap hits = new HashMap();

        int usedAttributes = 0;
        List ignoredAttributes = new ArrayList();
        Attributes objectAttrs = DB.getObjectAttrs();
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();

            if (!objectAttrs.isAttributeDefined(attrName)) {
                log.warn("Ignoring non defined attribute: " + attrName);
                ignoredAttributes.add(attrName);
                continue;
            }
            if (!objectAttrs.getAttrTypeDef(attrName).equalsIgnoreCase(DataTypeEnum.STR.toString())) {
                log.warn("Ignoring non STR attribute: " + attrName);
                ignoredAttributes.add(attrName);
                continue;
            }

            usedAttributes++;
            NST attrDataNST = objectAttrs.getAttrDataNST(attrName);
            String valueColumnName = attrDataNST.getNSTColumn(1).getName();
            String filterDef = valueColumnName + " LIKE '" + pattern + "'";
            NST hitsNST = attrDataNST.filter(filterDef, "ID");
            ResultSet resultSet = hitsNST.selectRows();
            List oidList = resultSet.toOIDList(1);
            if (oidList.size() != 0) {
                hits.put(attrName, oidList);
            }
            hitsNST.release();
        }

        if (usedAttributes == 0) {
            throw new IllegalArgumentException("All attribute names were ignored," +
                    "because they are either not defined, or not of type 'string'");
        }


        return hits;
    }

}
