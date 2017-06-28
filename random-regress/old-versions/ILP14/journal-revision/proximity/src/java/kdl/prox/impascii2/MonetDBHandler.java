/**
 * $Id: MonetDBHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.impascii2;

import java.util.Set;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;


/**
 * The full handler for Proximity databases on Monet.
 */
public class MonetDBHandler implements DBHandler {

    private int currLinkOrObjID;
    private ObjDict objDict;
    private boolean isObject;

    private int numAttrs = 0;
    private int numLinks = 0;
    private int numObjects = 0;


    /**
     * Initializes me for the passed args.
     *
     * @param nameObjStrAttr an 'str' object attribute in proxDB that is used to look up smart ascii ii nicknames. see ObjDict for more info
     * @
     */
    public MonetDBHandler(String nameObjStrAttr) {
        this.objDict = new ObjDict(nameObjStrAttr);
    }


    public void addAttribute(String name, String value) {
        Attributes attributes;
        if (isObject) {
            attributes = DB.getObjectAttrs();
        } else {
            attributes = DB.getLinkAttrs();
        }
        NST attrDataNST = attributes.getAttrDataNST(name);
        attrDataNST.insertRow(new String[]{currLinkOrObjID + "", value});
        numAttrs++;
    }

    public void addLink(String o1Name, String o2Name) {
        int o1ID = getOrCreateObject(o1Name);
        int o2ID = getOrCreateObject(o2Name);
        isObject = false;
        currLinkOrObjID = DB.insertLink(o1ID, o2ID);
        numLinks++;
    }

    public void addObject(String name) {
        isObject = true;
        currLinkOrObjID = getOrCreateObject(name);
        numObjects++;
    }

    private int createObjForName(String name) {
        int newID = DB.insertObject();
        objDict.putIDForName(name, newID);
        return newID;
    }

    public int getNumAttrs() {
        return numAttrs;
    }

    public int getNumLinks() {
        return numLinks;
    }

    public int getNumObjects() {
        return numObjects;
    }

    private int getOrCreateObject(String name) {
        int objID;
        Set iDsForName = objDict.getIDsForName(name);
        if (iDsForName.size() == 0) {
            objID = createObjForName(name);
        } else if (iDsForName.size() == 1) {
            objID = ((Integer) iDsForName.iterator().next()).intValue();
        } else {
            throw new IllegalArgumentException("found more than one id for " +
                    "object name '" + name + "' in " + objDict);
        }
        return objID;
    }

}
