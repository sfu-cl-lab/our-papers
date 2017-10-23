/**
 * $Id: ObjDict.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: ObjDict.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell. All Rights Reserved.

Status: Implementing.

*/


package kdl.prox.impascii2;

import kdl.prox.db.AttrType;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Provides a nice interface to an attribute that acts as an object-name
 * dictionary, i.e., nicknames.  The attribute's item_id column holds the id of
 * the object, and its value column holds the object's nickname. Regarding 1:N
 * and M:1 mappings (id:name), we have the following cases:
 * <p/>
 * o 1:N - multiple nicknames per id: legal
 * <p/>
 * o M:1 - multiple ids per nickname: illegal (ambiguous)
 * <p/>
 * NB: Currently Proximity does not enforce mappings like this, and neither
 * does this API.
 */
public class ObjDict {

    /**
     * The Data NST for the ObjDict attribute. Set by the constructor
     */
    private NST attrDataNST = null;

    private Attributes objectAttributes;


    /**
     * Full-arg constructor.
     *
     * @param attrName
     * @throws kdl.prox.monet.MonetException
     */
    public ObjDict(String attrName) {
        Assert.notNull(attrName, "attrName");

        // check that attrName exists
        objectAttributes = DB.getObjectAttrs();
        if (!objectAttributes.isAttributeDefined(attrName))
            throw new IllegalArgumentException("undefined dictionary " +
                    "attribute: " + attrName);

        // check that attrName holds a single string value
        Assert.condition(objectAttributes.isSingleValued(attrName),
                "not exactly one attribute value column: " + attrName);

        List attrTypes = objectAttributes.getTypes(attrName);
        AttrType attrType = (AttrType) attrTypes.iterator().next();
        if (attrType.getDataTypeEnum() != DataTypeEnum.STR) {
            throw new IllegalArgumentException("dictionary attribute item " +
                    "type not a " + DataTypeEnum.STR + ": " + attrName + ", " +
                    attrType.getDataTypeEnum());
        }

        attrDataNST = objectAttributes.getAttrDataNST(attrName);
    }


    /**
     * Deletes all rows in my attrTableName whose values are objName.
     * Regarding multiple nicknames, this method deletes *all* rows with
     * the passed nickname, even if the name mapped to multiple objects (which
     * is illegal). Throws IllegalArgumentException if no row was deleted.
     *
     * @param objName
     */
    public void deleteIDForName(String objName) {
        String filterDef = "value = '" + objName + "'";
        int prevRows = attrDataNST.getRowCount(filterDef);
        attrDataNST.deleteRows(filterDef);
        int afterRows = attrDataNST.getRowCount(filterDef);
        Assert.condition(afterRows < prevRows, "no rows deleted");
    }


    /**
     * Returns a Set of object IDs (IntegerS) that are associated with
     * objName. The List is empty if none are.
     * <p/>
     * todo use http://pcj.sourceforge.net/ ?
     *
     * @param objName
     */
    public Set getIDsForName(String objName) {
        ResultSet resultSet = attrDataNST.selectRows("value = '" + objName + "'", "id", "*");
        List oidList = resultSet.toOIDList(1);
        return new HashSet(oidList);
    }


    /**
     * Adds new association of objName and objID.
     *
     * @param objName
     * @param objID   todo check objID first?
     */
    public void putIDForName(String objName, int objID) {
        Assert.stringNotEmpty(objName, "objName");
        attrDataNST.insertRow(new String[]{objID + "", objName});
    }


}

