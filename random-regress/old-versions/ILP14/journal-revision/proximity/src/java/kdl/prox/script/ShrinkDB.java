/**
 * $Id: ShrinkDB.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;


/**
 * Defines shrinkDB() and helpers.
 *
 * @see #shrinkDB
 */
public class ShrinkDB {

    private static Logger log = Logger.getLogger(ShrinkDB.class);


    private static void removeAttributes(Attributes attributes, NST filterNST) {
        List attrNames = attributes.getAttributeNames();
        for (Iterator attrNameIter = attrNames.iterator(); attrNameIter.hasNext();) {
            String attrName = (String) attrNameIter.next();
            NST attrDataNST = attributes.getAttrDataNST(attrName);
            attrDataNST.deleteRows("id NOTIN " + filterNST.getNSTColumn("id").getBATName());
        }
    }

    /**
     * Treats keepContainer as a view of the database (i.e., only considers its
     * object and link IDs) and uses it to delete objects, links, and attribute
     * values that are <b>not</b> in the database. In other words, keeps the
     * following:
     * <ul>
     * <li>object: if ID listed in keepContainer
     * <li>link: if ID listed in keepContainer
     * <li>object attribute value: if object ID listed in keepContainer
     * <li>link attribute value: if link ID listed in keepContainer
     * </ul>
     * Notes:
     * <ul>
     * <li>Does not automatically keep a link's o1 and o2 object; you must
     * explicitly list object IDs to keep in the container.
     * <li>It is possible to end up with orphaned objects (objects with no links).
     * Ditto for orphaned links (links whose objects don't exist in the DB).
     * </ul>
     *
     * @param keepContainer
     */
    public static void shrinkDB(Container keepContainer) {
        log.debug("* shrinking database " + DB.description() + " using " + keepContainer);
        DB.beginScope();

        NST[] objectAndLinkFilterNSTs = keepContainer.getObjectAndLinkFilterNSTs();
        NST objFilterNST = objectAndLinkFilterNSTs[0];
        NST linkFilterNST = objectAndLinkFilterNSTs[1];

        // remove objects
        NST objectNST = DB.getObjectNST();
        objectNST.deleteRows("id NOTIN " + objFilterNST.getNSTColumn("id").getBATName());

        // remove links
        NST linkNST = DB.getLinkNST();
        linkNST.deleteRows("link_id NOTIN " + linkFilterNST.getNSTColumn("id").getBATName());

        // remove object and link attributes
        removeAttributes(DB.getObjectAttrs(), objFilterNST);
        removeAttributes(DB.getLinkAttrs(), linkFilterNST);

        // clean up
        DB.commit();
        DB.endScope();
    }

}
