/**
 * $Id: Container.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Container.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTTypeEnum;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Represents a container. Is actually an object-oriented wrapper for the
 * database's (one) ContainerNST. In this class we refer to specific children
 * by name.  NB: Unlike other OO wrappers of NSTs, all Container containerNST
 * IVs point to the *same* table in the database. This is because there is only
 * one container NST, unlike, say, Attributes' NST. NB: Container (child) names
 * are *not* case sensitive. This is consistent with our handling of attribute
 * names.
 */
public class Container {

    private static Logger log = Logger.getLogger(Container.class);

    public static final String QUERY_ATTR_NAME = "qgraph_query";


    private NST containerNST;

    /**
     * The OID of the container I represent. Indexes into my containerNST.
     * Set to -1 if I represent the virutal root container.
     */
    private int oid;
    private int parentOID;      // my parent's OID. -1 if it represents the root container

    private String containerName;
    private int thisContainerRowID;


    /**
     * Two-arg constructor for creating the special root virtual container.
     */
    public Container() {
        this(-1);
    }


    /**
     * Full-arg constructor.
     *
     * @param containerOID
     */
    public Container(int containerOID) {
        containerNST = new NST(DB.CONT_NST_NAME);
        if (containerOID != -1) {
            Assert.condition(isContainerIdDefined(containerOID),
                    "container doesn't exist: " + containerOID);
        }
        oid = containerOID;
        // specify that the columns for objects, links, and attributes are of type BAT
        // see NST documentation for an explanation of this
        containerNST.setIsBATNameColumn(NSTTypeEnum.CONT_NST_COL_NAMES[3], true);
        containerNST.setIsBATNameColumn(NSTTypeEnum.CONT_NST_COL_NAMES[4], true);
        containerNST.setIsBATNameColumn(NSTTypeEnum.CONT_NST_COL_NAMES[5], true);

        // get container attributes
        if (isRootContainer()) {
            containerName = null;
        } else {
            ResultSet resultSet = containerNST.selectRows("id = " + oid, "*", "*");
            Assert.condition(resultSet.next(), "container not found: " + oid);
            containerName = resultSet.getString(2);
            // set parentOID
            // can't use getOID() because top-level containers have 'nil' for parent, and getOID() does not like to convert nil to an oid
            if (resultSet.isColumnNil(3)) {
                parentOID = -1;
            } else {
                parentOID = resultSet.getOID(3);
            }
            // read as BATs
            // see NST documentation for an explanation of this
            thisContainerRowID = resultSet.getOID(0);
        }
    }

    public void changeItemName(String from, String to, boolean isObject) {
        getItemNST(isObject).replace("name = '" + from + "'", "name", Util.quote(to));
    }


    /**
     * Overload with no filter for IDs of subgraphs to be copied.
     *
     * @param origCont
     * @return the number of subgraphs that were copied over
     * @
     */
    public int copySubgraphsFromContainer(Container origCont) {
        return copySubgraphsFromContainer(origCont, null);
    }


    /**
     * Copies the specified set of subgraph from copyFromCont into me
     * <p/>
     * It recodes the original subg_ids so that they start from the current subg_id + 1
     * <p/>
     * Copy involves two steps: copying objects/links and copying attributes.
     * In both cases, subgraph IDs from the copyFromCont need to be recoded, so that
     * they don't overlap with the my subgraphs. Recoding is only necessary if this
     * container has  subgraphs of its own.
     *
     * @param copyFromCont
     * @param subgraphIdsNST an NST that lists IDs of the subgraphs in copyFromCont that must be copied (in subg_id col)
     * @return the number of subgraphs that were copied over
     * @
     */
    public int copySubgraphsFromContainer(Container copyFromCont, NST subgraphIdsNST) {
        Assert.notNull(copyFromCont, "null copy container");

        // return if nothing to copy
        if (copyFromCont.getSubgraphCount() == 0) {
            return 0;
        }

        int originalNumberSubgraphs = getSubgraphCount();

        // get recoding shift
        int recodingIDShift = 0;
        if (originalNumberSubgraphs > 0) {
            recodingIDShift = utilGetRecodingIDShift();
        }

        // first step: copy objects and links, with recoded ids
        utilInsertRecodedItems(copyFromCont, subgraphIdsNST, recodingIDShift, true);
        utilInsertRecodedItems(copyFromCont, subgraphIdsNST, recodingIDShift, false);

        // second step: copy the attribute values, with recoding too
        // todo works only with single-column attributes
        utilInsertRecodedAttributeValues(copyFromCont, subgraphIdsNST, recodingIDShift);

        int newNumberSubgraphs = getSubgraphCount();
        return (newNumberSubgraphs - originalNumberSubgraphs);
    }


    /**
     * Row-level method that adds a child to me with the specified name.
     * Returns the new Container.
     *
     * @param contName
     * @
     */
    public Container createChild(String contName) {
        return createChild(-1, contName);
    }


    /**
     * Internal method that creates a new child.
     *
     * @param containerID is only provided for calls from ImportXMLApps, to import the container IDs too.
     * @param contName
     * @
     */
    private Container createChild(int containerID, String contName) {
        Assert.notNull(contName, "container name null");
        Assert.assertValidName(contName);

        contName = contName.toLowerCase();
        Assert.condition(!hasChild(contName), "already have child named: " + contName);

        if (containerID == -1) {
            containerID = getNextContainerID();
        }

        // create anonymous (unnamed) subgObj and subgLink and SubgAttrNST for new container
        NST objNST = new NST(NSTTypeEnum.SGI_NST_COL_NAMES, NSTTypeEnum.SGI_NST_COL_TYPES);
        NST linkNST = new NST(NSTTypeEnum.SGI_NST_COL_NAMES, NSTTypeEnum.SGI_NST_COL_TYPES);
        NST subgAttrNST = new NST(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES);
        String objNSTName = objNST.save();
        String linkNSTName = linkNST.save();
        String subgAttrsNSTName = subgAttrNST.save();

        // store them in the table
        containerNST.insertRow(new String[]{containerID + "", contName, oid + "",
                objNSTName, linkNSTName, subgAttrsNSTName});

        // and now release them
        objNST.release();
        linkNST.release();
        subgAttrNST.release();

        return getChild(contName);
    }


    /**
     * Creates a child container whose SGI object and link items are initialized
     * with the ones passed in. The rows of the SGI tables are copied into the
     * new container's tables (the SGI tables can be release later)
     */
    public Container createChildFromTempSGINSTs(String containerName,
                                                NST objTempSGINST,
                                                NST linkTempSGINST) {
        return createChildFromTempSGINSTs(-1, containerName, objTempSGINST,
                linkTempSGINST);
    }


    /**
     * Creates a child container whose SGI object and link items are initialized
     * with the ones passed in.
     * This overload takes also a containerID (used for ImportXMLApp)
     */
    public Container createChildFromTempSGINSTs(int containerID,
                                                String containerName,
                                                NST objTempSGINST,
                                                NST linkTempSGINST) {
        Assert.notNull(objTempSGINST, "null objTempSGINST");
        Assert.notNull(linkTempSGINST, "null linkTempSGINST");

        Container cont = createChild(containerID, containerName);
        cont.getItemNST(true).insertRowsFromNST(objTempSGINST);
        cont.getItemNST(false).insertRowsFromNST(linkTempSGINST);
        return cont;
    }


    /**
     * Deletes all children of this container.
     * Calls deleteChild() on children in getChildrenNames().
     *
     * @
     */
    public void deleteAllChildren() {
        for (Iterator childNameIter = getChildrenNames().iterator(); childNameIter.hasNext();) {
            String childName = (String) childNameIter.next();
            deleteChild(childName);
        }
    }

    /**
     * Removes a child container, also recursively removing its children, and all
     * attributes, subgraphs, etc.
     *
     * @param contName
     * @
     */
    public void deleteChild(String contName) {
        Assert.notNull(contName, "container name null");
        Container theChild = getChild(contName);
        Assert.notNull(theChild, "Child doesn't exist: " + contName);

        theChild.deleteAllChildren();
        theChild.getSubgraphAttrs().deleteAllAttributes();
        theChild.getItemNST(true).delete();
        theChild.getItemNST(false).delete();
        new NST(theChild.getSubgAttrBatName()).delete();
        containerNST.deleteRows("id = " + theChild.getOid());
    }


    /**
     * Gets the container with the specified name, which is a child of me
     *
     * @param contName
     * @return child Container named contName. Returns null if not found.
     */
    public Container getChild(String contName) {
        Assert.notNull(contName, "container name null");
        contName = contName.toLowerCase();
        ResultSet theChildRow = containerNST.selectRows("parent = " + oid + " AND name = '" + contName + "'",
                "id", "*");
        Container childCont;
        if (theChildRow.next()) {
            childCont = new Container(theChildRow.getOID(1));
        } else {
            childCont = null;
        }
        return childCont;
    }

    public int getChildrenCount() {
        return containerNST.getRowCount("parent = " + oid);
    }


    /**
     * Returns a List of child Container's names (Strings)
     *
     * @return
     * @
     */
    public List getChildrenNames() {
        ResultSet resultSet = containerNST.selectRows("parent = " + oid, "name", "*");
        return resultSet.toStringList(1);
    }


    /**
     * Returns a filtered NST, with the rows of the containterNST for my children
     *
     * @return
     * @
     */
    public NST getChildrenNST() {
        return containerNST.filter("parent EQ " + oid);
    }


    /**
     * Returns an NST that contains the distinct IDs of non-empty subgraphs in the
     * container.
     * <p/>
     * Caller should release.
     */
    public NST getDistinctSubgraphOIDs() {
        NST objSubgs = getItemNST(true).project("subg_id");
        NST linkSubgs = getItemNST(false).project("subg_id");
        NST retNST = objSubgs.union(linkSubgs, "subg_id");
        objSubgs.release();
        linkSubgs.release();
        return retNST;
    }


    /**
     * Gets either the link or object NST for this container --depending on
     * the value of isObject.
     * <p/>
     * The NST has three columns, as specified in NSTTypeEnum.SGI_NST_COL_NAMES:
     * [item_id subg_id name]
     *
     * @param isObject
     * @return ItemNST
     */
    public NST getItemNST(boolean isObject) {
        Assert.condition(!isRootContainer(), "Operation not valid on root container");
        String batName = (isObject ? getSubgObjBatName() : getSubgLinkBatName());
        return new NST(batName);
    }


    /**
     * Gets either the link or object NST for this container --depending on
     * the value of isObject, and then filters to get only those with a given names
     * <p/>
     *
     * @param isObject
     * @param itemNames a comma-separated list of names
     * @return ItemNST
     */
    public NST getItemNSTByName(boolean isObject, String itemNames) {
        // Build the condition string
        List conditions = new ArrayList();
        String[] names = itemNames.split(",");
        for (int i = 0; i < names.length; i++) {
            String itemName = names[i].trim();
            conditions.add("name EQ '" + itemName + "'");
        }
        String filterDef = Util.join(conditions, " OR ");

        // Get them
        NST itemNST = getItemNST(isObject);
        NST filterNST = itemNST.filter(filterDef);
        itemNST.release();

        return filterNST;
    }

    /**
     * Returns links that match a given condition
     *
     * @param filterDef
     * @return an NST with the links (item_id, subg_id, name] and the values of any attributes mentioned in the filterDef
     */
    public NST getLinks(String filterDef) {
        return DB.getAttrsForItems(getItemNST(false), DB.getLinkAttrs(), filterDef);
    }

    public NST getLinks(String filterDef, String attrList) {
        return DB.getAttrsForItems(getItemNST(false), DB.getLinkAttrs(), filterDef, attrList);
    }

    public NST getLinksNST() {
        return getItemNST(false);
    }

    public NST getLinksNSTByName(String itemNames) {
        return getItemNSTByName(false, itemNames);
    }


    /**
     * Returns my name. Returns <b>null</b> if I'm the root container.
     */
    public String getName() {
        return containerName;
    }

    public int getNextContainerID() {
        return containerNST.max(NSTTypeEnum.CONT_NST_COL_NAMES[0]) + 1;
    }


    /**
     * Returns objects that match a given condition
     *
     * @param filterDef
     * @return an NST with the objects (item_id, subg_id, name] and the values of any attributes mentioned in the filterDef
     */
    public NST getObjects(String filterDef) {
        return DB.getAttrsForItems(getItemNST(true), DB.getObjectAttrs(), filterDef);
    }

    public NST getObjects(String filterDef, String attrList) {
        return DB.getAttrsForItems(getItemNST(true), DB.getObjectAttrs(), filterDef, attrList);
    }

    public NST getObjectsNST() {
        return getItemNST(true);
    }

    public NST getObjectsNSTByName(String itemNames) {
        return getItemNSTByName(true, itemNames);
    }


    /**
     * Returns an NST that combines both the object and link NSTs. It adds a column to them, with
     * O or L, to indicate the type
     * Caller should release
     */
    public NST getObjectsAndLinksNST() {
        NST objectsNST = getObjectsNST();
        NST linksNST = getLinksNST();

        NST copyObjects = objectsNST.copy();
        copyObjects.addConstantColumn("type", "str", "O");

        NST copyLinks = linksNST.copy();
        copyLinks.addConstantColumn("type", "str", "L");

        NST objectsAndLinksNST = copyObjects.copy();
        objectsAndLinksNST.insertRowsFromNST(copyLinks);

        objectsNST.release();
        linksNST.release();
        copyObjects.release();
        copyLinks.release();

        return objectsAndLinksNST;
    }


    /**
     * Utility method to help routines that want to apply to either the entire
     * database or a given container. It flattens down this container (getting the
     * distinct objects and links in its subgraphs) and returns an array of two NSTs,
     * which list the ids of objects and links that are found in this container, respectively.
     * Callers can then use these BATs to filter their input to the corresponding
     * objects and links.
     * <p/>
     * Callers should release the variables!
     */
    public NST[] getObjectAndLinkFilterNSTs() {
        NST objFilterNST;
        NST linkFilterNST;
        if (getSubgraphCount() > 0) {
            objFilterNST = getItemNST(true).filter("item_id DISTINCT ROWS", "item_id").renameColumn("item_id", "id");
            linkFilterNST = getItemNST(false).filter("item_id DISTINCT ROWS", "item_id").renameColumn("item_id", "id");
        } else {
            objFilterNST = new NST("id", "oid");
            linkFilterNST = new NST("id", "oid");
        }

        return new NST[]{objFilterNST, linkFilterNST};
    }


    /**
     * Returns my OID.
     */
    public int getOid() {
        return oid;
    }


    /**
     * Returns a Container for my parent.
     */
    public Container getParent() {
        Assert.condition(!isRootContainer(),
                "Operation not valid on root container");
        return new Container(parentOID);
    }

    public int getParentOid() {
        return parentOID;
    }

    /**
     * Returns the value of the QUERY_ATTR_NAME attribute (used to store the query that generated the container)
     * Returns null if attribute doesn't exist, or isn't defined for this container
     */
    public String getQuery() {
        Attributes containerAttrs = DB.getContainerAttrs();
        if (!containerAttrs.isAttributeDefined(QUERY_ATTR_NAME)) {
            return null;
        }

        NST attrDataNST = containerAttrs.getAttrDataNST(QUERY_ATTR_NAME);
        ResultSet resultSet = attrDataNST.selectRows("id = " + getOid(), "value", "*");
        if (resultSet.next()) {
            return resultSet.getString(1);
        } else {
            return null;
        }
    }


    /**
     * Returns a subgraph object for a particular subgraph in the container
     */
    public Subgraph getSubgraph(int subgOID) {
        Assert.condition(!isRootContainer(), "Operation not valid on root container");
        return new Subgraph(this, subgOID, getSubgObjBatName(), getSubgLinkBatName());
    }


    /**
     * Returns the attributes for subgraphs under this container.
     *
     * @return
     * @
     * @see Attributes
     */
    public Attributes getSubgraphAttrs() {
        Assert.condition(!isRootContainer(), "Operation not valid on root container");
        return new Attributes(getSubgAttrBatName());
    }

    /**
     * Return the name of the BATs that store the obj/link/attr
     * Returns a varialbe whose content is the name of that BAT (a str, not a bat)
     * The variable doesn't need to be release later.
     */
    protected String getSubgAttrBatName() {
        return containerNST.getColumnValueAsBATName(NSTTypeEnum.CONT_NST_COL_NAMES[5],
                thisContainerRowID);
    }

    protected String getSubgLinkBatName() {
        return containerNST.getColumnValueAsBATName(NSTTypeEnum.CONT_NST_COL_NAMES[4],
                thisContainerRowID);
    }

    protected String getSubgObjBatName() {
        return containerNST.getColumnValueAsBATName(NSTTypeEnum.CONT_NST_COL_NAMES[3],
                thisContainerRowID);
    }


    /**
     * Returns the number of subgraphs in me. More efficient than calling
     * getSubgraphOIDs().size().
     */
    public int getSubgraphCount() {
        NST distinctSubgraphOIDs = getDistinctSubgraphOIDs();
        int subgCount = distinctSubgraphOIDs.getRowCount();
        distinctSubgraphOIDs.release();
        return subgCount;
    }


    /**
     * Returns a List of (Integer) OIDs of the subgraphs in me.
     *
     * @return
     * @see String
     *      todo use http://pcj.sourceforge.net/ or other primitive collection lib
     */
    public List getSubgraphOIDs() {
        Assert.condition(!isRootContainer(), "Operation not valid on root container");
        NST distinctSubgraphOIDs = getDistinctSubgraphOIDs();
        List list = distinctSubgraphOIDs.selectRows("subg_id").toOIDList(1);
        distinctSubgraphOIDs.release();
        return list;
    }

    public NST getSubgraphs(String filterDef) {
        return getSubgraphs(filterDef, "");
    }

    public NST getSubgraphs(String filterDef, String attrList) {
        NST distinctSubgraphOIDs = getDistinctSubgraphOIDs();
        NST attrsForSubg = DB.getAttrsForItems(distinctSubgraphOIDs, getSubgraphAttrs(), filterDef, attrList);
        distinctSubgraphOIDs.release();
        return attrsForSubg;
    }

    public NST getUniqueLinks() {
        return getUniqueItems(false);
    }

    private NST getUniqueItems(boolean isObject) {
        return getItemNST(isObject).filter("item_id DISTINCT ROWS");
    }

    public NST getUniqueObjects() {
        return getUniqueItems(true);
    }


    /**
     * Returns true if there is a child in me named contName.
     *
     * @param contName
     * @return
     * @
     */
    public boolean hasChild(String contName) {
        Assert.notNull(contName, "container name null");
        contName = contName.toLowerCase();
        Container childContainer = getChild(contName);
        return childContainer != null;
    }


    /**
     * @return true if I have any children
     * @
     */
    public boolean hasChildren() {
        return getChildrenNames().size() != 0;
    }


    /**
     * Returns true if the container NST has a row with the given ID
     */
    public static boolean isContainerIdDefined(int containerOID) {
        return DB.getRootContainer().containerNST.getRowCount("id = " + containerOID) > 0;
    }


    /**
     * Returns true if I represent the database's root Container.
     */
    public boolean isRootContainer() {
        return oid == -1;
    }


    /**
     * Makes this container a child of another container.
     *
     * @param parentContainer
     */
    public void makeChildOf(Container parentContainer) {
        Assert.condition(!isRootContainer(), "Root container cannot be made a child");
        containerNST.replace("id = " + oid, "parent", parentContainer.getOid() + "");
        parentOID = parentContainer.getOid();
    }


    /**
     * Combines the contents of all input containers in the list and copiest
     * them to the destination container. Skips input container if it's equal
     * to destinationContainer.
     * Works by repeatedly cally copySubgraphsFromContainer for every
     * container in inputContainersList
     *
     * @param inputContainersList
     * @param destContainer
     */
    public static void mergeContainers(List inputContainersList, Container destContainer) {
        Assert.notNull(inputContainersList, "inputContainersList null");
        Assert.notNull(destContainer, "destinationContainer null");

        for (Iterator contIter = inputContainersList.iterator(); contIter.hasNext();) {
            Container container = (Container) contIter.next();
            if (container.getOid() != destContainer.getOid()) {
                destContainer.copySubgraphsFromContainer(container);
            }
        }
    }

    public void rename(String newName) {
        Assert.condition(!isRootContainer(), "Root container cannot be renamed");
        containerNST.replace("id = " + oid, "name", Util.quote(newName));
        containerName = newName;
    }

    /**
     * Records a value in the QUERY_ATTR_NAME attribute for this container
     * Used by QGraph, to save the query that created the container.
     *
     * @param queryXML
     */
    public void setQuery(String queryXML) {
        DB.getContainerAttrs().defineAttributeIfNotExists("qgraph_query", "str");
        NST attrNST = DB.getContainerAttrs().getAttrDataNST(QUERY_ATTR_NAME);
        attrNST.deleteRows("id = " + getOid());
        attrNST.insertRow(new String[]{getOid() + "", queryXML});
    }


    /**
     * Adds a new column to an NST, with the recoded IDs for a given column
     * The new column is named new_colName
     *
     * @param origNST
     * @param colName
     * @param shift
     */
    private void utilAddRecodedIDToNST(NST origNST, String colName, int shift) {
        Assert.condition(shift >= 0, "startingID is negative: " + shift);
        origNST.castColumn(colName, "int");
        origNST.addArithmeticColumn(colName + " + " + shift, "int", "new_" + colName);
        origNST.castColumn("new_" + colName, "oid");
    }


    /**
     * Returns the max subgraph id in this container
     * Deals with containers whose object,link and subgraph indx table are not in synch:
     * there may be empty subgraphs (listed in the idx table but with no rows in objs and links)
     * or there may be subgraph IDs in the object and link tables that are not listed in the idx
     * (this happens mostly in our test code, where we manipulate the object and link tables
     * directly, without creating the appropriate subgraphs first)
     */
    private int utilGetRecodingIDShift() {
        int maxSubgIDinObj = getItemNST(true).max("subg_id");
        int maxSubgIDinLink = getItemNST(false).max("subg_id");
        return Math.max(maxSubgIDinObj, maxSubgIDinLink) + 1;
    }


    /**
     * Copies attributes and their values from one container to this one
     *
     * @param copyFromCont
     * @param subgraphIdsNST
     * @param recodingIDShift
     */
    private void utilInsertRecodedAttributeValues(Container copyFromCont, NST subgraphIdsNST, int recodingIDShift) {
        Attributes myAttrs = getSubgraphAttrs();
        Attributes copyFromAttrs = copyFromCont.getSubgraphAttrs();

        // iterate over the attributes of the copyFromCont, define them here, and insert the values
        Iterator origAttributeNamesIter = copyFromAttrs.getAttributeNames().iterator();
        while (origAttributeNamesIter.hasNext()) {
            String attrName = (String) origAttributeNamesIter.next();

            // define it if it doesn't exist and get the data NST for the attribute
            if (!myAttrs.isAttributeDefined(attrName)) {
                myAttrs.defineAttribute(attrName, copyFromAttrs.getAttrTypeDef(attrName));
            }
            NST myAttrDataNST = myAttrs.getAttrDataNST(attrName);

            // get the attr vals table from the copyFromCont (with the filter, if any) and add a recoding column
            NST copyFromContAttrValuesNST;
            NST attrDataNST = copyFromAttrs.getAttrDataNST(attrName);
            if (subgraphIdsNST != null) {
                copyFromContAttrValuesNST = attrDataNST.intersect(subgraphIdsNST, "id EQ subg_id");
            } else {
                copyFromContAttrValuesNST = attrDataNST.copy();
            }
            utilAddRecodedIDToNST(copyFromContAttrValuesNST, "id", recodingIDShift);
            NST recodedCopyFromContAttrValuesNST = copyFromContAttrValuesNST.project("new_id, value");
            myAttrDataNST.insertRowsFromNST(recodedCopyFromContAttrValuesNST);
            recodedCopyFromContAttrValuesNST.release();
            copyFromContAttrValuesNST.release();
            attrDataNST.release();
            myAttrDataNST.release();
        }
    }


    /**
     * Inserts into the object/link tables the rows from another container,
     * with the subgIDs recoded according to recodingIDShift. If subgraphIdsBat
     * is not null, only rows with the given subg_id are inserted
     *
     * @param copyFromCont
     * @param subgraphIdsNST
     * @param recodingIDShift
     * @param isObjects
     */
    private void utilInsertRecodedItems(Container copyFromCont, NST subgraphIdsNST,
                                        int recodingIDShift, boolean isObjects) {
        // get my items table
        NST myItemSGINST = getItemNST(isObjects);

        // get the items table from the copyFromCont (with the filter, if any) and add a recoding column
        NST copyFromContItemSGINST;
        if (subgraphIdsNST != null) {
            copyFromContItemSGINST = copyFromCont.getItemNST(isObjects).intersect(subgraphIdsNST, "subg_id EQ subg_id");
        } else {
            NST itemNST = copyFromCont.getItemNST(isObjects);
            copyFromContItemSGINST = itemNST.copy();
            itemNST.release();
        }
        utilAddRecodedIDToNST(copyFromContItemSGINST, "subg_id", recodingIDShift);
        NST recodedCopyFromContItemSGINST = copyFromContItemSGINST.project("item_id, new_subg_id, name");
        // insert into items table
        myItemSGINST.insertRowsFromNST(recodedCopyFromContItemSGINST);
        copyFromContItemSGINST.release();
        recodedCopyFromContItemSGINST.release();
    }


}
