/**
 * $Id: DB.java 3696 2007-11-01 18:45:45Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.db;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.FilterFactory;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTTypeEnum;
import kdl.prox.gui2.ProxURL;
import kdl.prox.monet.Connection;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.PopulateDB;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * The top-level object-oriented view of a Proximity Three database hosted on
 * Monet.
 * All the methods are static. To use, first call open(), and then refer to
 * the static class's methods. Call close() when done.
 */

public final class DB {
    private static final Logger log = Logger.getLogger(DB.class);

    private static int counter = -1;    // used by generateTempContainerName()

    public static final String CONT_ATTR_NST_NAME = "prox_cont_attr";
    public static final String CONT_NST_NAME = "prox_container";
    public static final String LINK_ATTR_NST_NAME = "prox_link_attr";
    public static final String LINK_NST_NAME = "prox_link";
    public static final String OBJ_ATTR_NST_NAME = "prox_obj_attr";
    public static final String OBJ_NST_NAME = "prox_object";

    /**
     * Name of the schema log BAT (not NST).
     */
    private static final String SCHEMA_LOG_BAT_NAME = "prox_schema_log";

    /**
     * Schema version. History (please keep up-to-date):
     * <p/>
     * ~2003-08-01: 1.0 (initial schema)
     * 2003-10-07: 1.1 (removed support for subgraph item attributes)
     * 2003-10-07: 1.2 (added link_id column to LINK_NST)
     * 2003-10-31: 1.3 (change from nested BATs to STR tables)
     * 2003-11-04: 1.4 (prox_schema_log table now has head of type date)
     * 2004-04-07: 1.5 (dropped isShared column in prox_container)
     * 2004-12-16: 1.6 (dropped subg column in container, added object and link cols, and container id)
     */
    public static final int SCHEMA_MAJOR_VERSION = 1;
    public static final int SCHEMA_MINOR_VERSION = 6;

    // cached structures
    private static Attributes objAttrs;
    private static Attributes linkAttrs;

    private DB() {
    }


    /**
     * Attach a particular attribute to a table, on a given column, and save it
     * Note: Works only with attributes with the [id, value] format.
     * Note: Doesn't do LEFT OUTER JOIN --> no join on NULLs
     *
     * @param toTable
     * @param attrs
     * @param onColumn
     * @param attrName
     * @return a new NST with the old columns plus the new attribute values in newColName
     */
    private static NST attachAttribute(NST toTable, Attributes attrs, String onColumn, String attrName) {
        String newColName;
        int asIndex = attrName.toUpperCase().indexOf(" AS ");
        if (asIndex != -1) {
            newColName = attrName.substring(asIndex + 4);
            attrName = attrName.substring(0, asIndex);
        } else {
            newColName = attrName;
        }
        NST attrTable = attrs.getAttrDataNST(attrName);
        Assert.condition(attrTable.isColumnExists("id"), "Attribute table must have column 'id'");
        Assert.condition(attrTable.isColumnExists("value"), "Attribute table must have column 'value'");
        return toTable.join(attrTable, onColumn + " = id", toTable.getNSTColumnNamesAsString() + ", value AS " + newColName);
    }

    public static NST attachLinkAttribute(NST toTable, String onColumn, String attrName) {
        return attachAttribute(toTable, getLinkAttrs(), onColumn, attrName);
    }

    public static NST attachObjectAttribute(NST toTable, String onColumn, String attrName) {
        return attachAttribute(toTable, getObjectAttrs(), onColumn, attrName);
    }


    public static void beginScope() {
        Connection.beginScope();
    }

    /**
     * Called by createNewTempContainer(), tries to create a new container in
     * parentContainer named newContName. Returns true if succeeded, and
     * false if not. Based on File.checkAndCreate().
     */
    private static boolean checkAndCreateContainer(Container parentContainer,
                                                   String newContName) {
        if (null != parentContainer.getChild(newContName)) {
            return false;   // already exists
        } else {
            parentContainer.createChild(newContName);
            return true;
        }
    }

    /**
     * If prox_schema_log exists (meaning that it's a Proximity DB,
     * check that its max value is SCHEMA_MAJOR_VERSION.SCHEMA_MINOR_VERSION
     */
    private static void checkSchemaVersion() {
        if (MonetUtil.isBATExists(SCHEMA_LOG_BAT_NAME)) {
            StringBuffer milSB = new StringBuffer();
            milSB.append("bat(\"");
            milSB.append(SCHEMA_LOG_BAT_NAME);
            milSB.append("\").max().print();");
            ResultSet resultSet = Connection.executeQuery(milSB.toString());
            resultSet.next();
            if (!resultSet.isColumnNil(0)) {
                String currentVersion = resultSet.getString(0);
                String expectedVersion = SCHEMA_MAJOR_VERSION + "." + SCHEMA_MINOR_VERSION;
                if (!expectedVersion.equals(currentVersion)) {
                    log.fatal("Proximity database does not have the latest schema.");
                    log.fatal("\texpected [" + expectedVersion + ']' +
                            " but found [" + currentVersion + "] instead.");
                    log.fatal("Please, run the schema-change scripts provided in");
                    log.fatal("\t $PROX_HOME/resources/ and try again.");
                    throw new MonetException("Wrong schema");
                }
            }
        }
    }


    /**
     * Called by methods that want empty a database. Often paired with calls
     * to initEmptyDB(). Removes all BATs from database. WARNING! This method
     * will remove all your data.
     *
     * @
     * @see #initEmptyDB
     */
    public static void clearDB() {
        log.debug("* clearing database");

        if (MonetUtil.isBATExists(OBJ_NST_NAME)) {
            getObjectNST().delete();
        }
        if (MonetUtil.isBATExists(LINK_NST_NAME)) {
            getLinkNST().delete();
        }
        if (MonetUtil.isBATExists(CONT_NST_NAME)) {
            getRootContainer().deleteAllChildren();
            getContainerNST().delete();
        }
        if (MonetUtil.isBATExists(OBJ_ATTR_NST_NAME)) {
            getObjectAttrs().deleteAllAttributes();
            getObjectAttrNST().delete();
        }
        if (MonetUtil.isBATExists(LINK_ATTR_NST_NAME)) {
            getLinkAttrs().deleteAllAttributes();
            getLinkAttrNST().delete();
        }
        if (MonetUtil.isBATExists(CONT_ATTR_NST_NAME)) {
            getContainerAttrs().deleteAllAttributes();
            getContainerAttrNST().delete();
        }
        MonetUtil.deleteBATIfExists(SCHEMA_LOG_BAT_NAME);

        List userNSTs = ls();
        for (int id = 0; id < userNSTs.size(); id++) {
            String name = (String) userNSTs.get(id);
            try {
                new NST(name).delete();
            } catch (Exception e) {
                log.warn("Could not delete user's NST named " + name + ": " + e);
            }
        }

        commit();
        log.info("* Database cleared");
    }

    public static void close() {
        Connection.close();
    }

    /**
     * Combines column names from a filterDef and an extra list of attributes. Returns unique values.
     *
     * @param filterDef
     * @param extraAttrsList
     * @return a set of unique column/attribute names
     */
    private static Set combineColumnLists(String filterDef, String extraAttrsList) {
        Set attrNamesSet = new HashSet();
        String[] filterColList = FilterFactory.getFilterColumns(filterDef).split(",");
        for (int i = 0; i < filterColList.length; i++) {
            if (filterColList[i].trim().length() > 0) {
                attrNamesSet.add(filterColList[i].trim());
            }
        }
        String[] attrColList = extraAttrsList.split(",");
        for (int i = 0; i < attrColList.length; i++) {
            if (attrColList[i].trim().length() > 0) {
                attrNamesSet.add(attrColList[i].trim());
            }
        }
        return attrNamesSet;
    }

    /**
     * Commits any dirty (modified) BATs that haven't been saved. Also called
     * by close().
     */
    public static void commit() {
        Connection.commit();
    }

    /**
     * Receives an NST with from and to columns, with oids of objects
     * that need to be connected, and it creates links between them.
     * For any column in the NST whose name begins with attr_, this method will create/append
     * the corresponding link attribute with the values in the table.
     * For example, a fromTo NST with columns [from, to, attr_link_type, xx] will take
     * the values from attr_link_type and save them in the link attribute link_type
     * <p/>
     * Returns an NST with the link ids
     *
     * @param fromTo: an NST with two columns, named from and to, with the end points for the links, and possibly othher columns with name attr_*
     * @return the original NST, but with a link_id column, listing the links that were created
     */

    public static NST createLinks(NST fromTo) {
        // create the links
        NST linksNST = DB.getLinkNST();
        int nextLinkID = linksNST.max("link_id") + 1;
        fromTo.addNumberColumn("link_id", nextLinkID);
        NST newLinks = fromTo.project("link_id, from, to");
        linksNST.insertRowsFromNST(newLinks);
        newLinks.release();
        linksNST.release();

        // Create the attribute values for all those columns whose names start with attr_
        Attributes attributes = DB.getLinkAttrs();
        for (int colIdx = 0; colIdx < fromTo.getColumnCount(); colIdx++) {
            String colName = fromTo.getNSTColumn(colIdx).getName();
            if (colName.startsWith("attr_")) {

                String attrName = colName.substring(5);
                NST newAttrValues = fromTo.project("link_id, " + colName).renameColumns("id, value");
                if (attributes.isAttributeDefined(attrName)) {
                    NST attrDataNST = attributes.getAttrDataNST(attrName);
                    newAttrValues.castColumn("value", attrDataNST.getNSTColumn("value").getType().toString());
                    attrDataNST.insertRowsFromNST(newAttrValues);
                    attrDataNST.release();
                } else {
                    DataTypeEnum type = fromTo.getNSTColumn(colIdx).getType();
                    attributes.defineAttributeWithData(attrName, type.toString(), newAttrValues);
                }
                newAttrValues.release();

            }

        }

        return fromTo;
    }

    /**
     * Utility akin to File.createTempFile(), returns a new temporary container
     * in a standard 'system' location.
     *
     * @return
     */
    public static Container createNewTempContainer() {
        Container tempParentContainer = getTempParentContainer();
        String newContName;
        do {
            newContName = generateTempContainerName();
        } while (!checkAndCreateContainer(tempParentContainer, newContName));
        return tempParentContainer.getChild(newContName);
    }

    /**
     * Creates a persistent Prox NST.
     *
     * @param columnNames
     * @param columnTypes
     */
    private static void createProxNST(String[] columnNames,
                                      String[] columnTypes,
                                      String nstName) {
        NST nst = new NST(columnNames, columnTypes);
        nst.save(nstName);
        nst.release();
    }

    /**
     * Called by initEmptyDB(), creates the schema log BAT, makes it persistent,
     * and inserts a row for the current date and schema.
     *
     * @
     */
    private static void createSchemaLogBAT() {
        StringBuffer milSB = new StringBuffer();
        milSB.append("bat(date,str).persists(true)");
        milSB.append(".insert(date(\"");
        milSB.append(new java.sql.Date(System.currentTimeMillis()));
        milSB.append("\"), \"");
        milSB.append(SCHEMA_MAJOR_VERSION);
        milSB.append(".");
        milSB.append(SCHEMA_MINOR_VERSION);
        milSB.append("\").rename(\"");
        milSB.append(SCHEMA_LOG_BAT_NAME);
        milSB.append("\");");
        Connection.executeCommand(milSB.toString());
    }


    public static void deleteAllData() {
        getObjectNST().deleteRows();
        getLinkNST().deleteRows();
        getObjectAttrs().deleteAllAttributes();
        getLinkAttrs().deleteAllAttributes();
        getRootContainer().deleteAllChildren();
    }

    /**
     * Deletes objects/links specified in the NST
     *
     * @param itemsNST
     * @param isObject
     */
    private static void deleteItems(NST itemsNST, boolean isObject) {
        String idColumn = (isObject ? "id" : "link_id");

        // Go through the attributes and delete them
        String filterDef = "id IN " + itemsNST.getNSTColumn(idColumn).getBATName();
        Attributes attributes = isObject ? DB.getObjectAttrs() : DB.getLinkAttrs();
        List attributeNames = attributes.getAttributeNames();
        for (int attrNameIdx = 0; attrNameIdx < attributeNames.size(); attrNameIdx++) {
            String attrName = (String) attributeNames.get(attrNameIdx);
            attributes.getAttrDataNST(attrName).deleteRows(filterDef);
        }

        // And finally delete the items
        filterDef = idColumn + " IN " + itemsNST.getNSTColumn(idColumn).getBATName();
        (isObject ? DB.getObjectNST() : DB.getLinkNST()).deleteRows(filterDef);
    }

    /**
     * Deletes links specified in filterDef, and their attribute values
     *
     * @param filterDef
     */
    public static void deleteLinks(String filterDef) {
        NST linksNST = getLinks(filterDef);
        deleteItems(linksNST, false);
        linksNST.release();
    }

    /**
     * Deletes objects specified in the filterDef. Deletes attribute values for those objects as well,
     * and all links to/from those objects.
     * NOTE: Use with caution, as links are also deleted!
     *
     * @param filterDef
     */
    public static void deleteObjects(String filterDef) {
        NST objectsNST = getObjects(filterDef);

        // first of all, delete the links from/to these objects
        NST o1 = getLinkNST().intersect(objectsNST, "o1_id = id");
        NST o2 = getLinkNST().intersect(objectsNST, "o2_id = id");
        NST o1Ando2 = o1.union(o2, "link_id");
        deleteItems(o1Ando2, false);

        // and now delete the objects
        deleteItems(objectsNST, true);

        o1.release();
        o2.release();
        o1Ando2.release();
        objectsNST.release();
    }


    /**
     * Deletes all temporary containers created by createNewTempContainer().
     *
     * @
     */
    public static void deleteTempContainers() {
        Container tempParentContainer = getTempParentContainer();
        tempParentContainer.deleteAllChildren();
        getRootContainer().deleteChild("tmp");
    }


    public static String description() {
        String description = "";
        if (isOpen()) {
            description = "[" + Connection.getHost() + ", " + Connection.getPort() + "]";
        }
        return description;
    }


    /**
     * Called by createNewTempContainer(), returns a candidate random name for a new
     * temporary container. May or may not be unused. Based on File.generateFile().
     *
     * @return a string with temp_ followed by a random number
     */
    public static String generateTempContainerName() {
        if (counter == -1) {
            counter = new Random().nextInt() & 0xffff;
        }
        counter++;
        String prefix = "temp_";
        return prefix + Integer.toString(counter);
    }


    public static void endScope() {
        Connection.endScope();
    }


    /**
     * Given an NST, it find the first column that ends with 'id' and returns it
     */
    private static String findIdColumn(NST theNST) {
        String idCol = "";

        List colNames = theNST.getNSTColumnNames();
        for (int colIdx = 0; colIdx < colNames.size(); colIdx++) {
            String colName = (String) colNames.get(colIdx);
            if (colName.endsWith("id")) {
                idCol = colName;
                break;
            }
        }

        Assert.stringNotEmpty(idCol, "Could not find an id Column for NST: " + colNames);
        return idCol;
    }


    /**
     * Overload that returns getAttrsForItems only  with attributes specified in the filter string.
     *
     * @param itemsNST  the items for which to get attributes
     * @param attrs     the attributes structure where values are stored
     * @param filterDef a filter to apply to the resulting NST
     * @return an NST with the objects/links columns, plus a column for each attribute in the filterDef.
     * @see DB#getAttrsForItems
     */
    public static NST getAttrsForItems(NST itemsNST, Attributes attrs, String filterDef) {
        return getAttrsForItems(itemsNST, attrs, filterDef, "");
    }

    /**
     * Given an NST with items and an Attribute, it joins the NST with the tables for the
     * requested item and returns a new NST with columns for all the attrs. The resulting NST has the internal
     * columns from the itemsNST table, plus all attributes mentioned in the filterDef and extraAttrsList.
     * <p/>
     * Examples:
     * <p/>
     * object_type = 'movie' or object_type = 'person'
     * link_type = 'directed_in' and o1_id = 13 and link_color = 'red'
     * <p/>
     * The joins are performed as left outer joins, retaining nil values in the NST. To eliminate nil values, add
     * conditions such as 'link_color != nil'
     * <p/>
     *
     * @param itemsNST       the items for which to get attributes
     * @param attrs          the attributes structure where values are stored
     * @param extraAttrsList a comma-separated list of names of attributes that should be included in the NST
     * @param filterDef      a filter to apply to the resulting NST
     * @return an NST with the objects/links columns, plus a column for each requested attribute. Contains nil values
     */
    public static NST getAttrsForItems(NST itemsNST, Attributes attrs, String filterDef, String extraAttrsList) {
        // Expand list of attrnames
        if ("*".equals(extraAttrsList.trim())) {
            extraAttrsList = attrs.getAttributeNamesAsString();
        }

        // Get the list of attributes to request, from the filterDef and the extraAttrsList.
        // Ignore columns that already belong to the items NST. Ignore duplicates
        Set attrNamesSet = combineColumnLists(filterDef, extraAttrsList);
        attrNamesSet.removeAll(itemsNST.getNSTColumnNames());

        // now get the attributes
        NST currNST = joinItemsWithAttrs(itemsNST, attrs, Util.join(attrNamesSet, ","));

        // Filter if necessary
        if (!"*".equals(filterDef)) {
            NST newNST = currNST.filter(filterDef);
            currNST.release();
            currNST = newNST;
        }

        return currNST;
    }


    public static Container getContainer(String containerPath) {
        ProxURL proxURL = new ProxURL("cont:/containers/" + containerPath);
        return proxURL.getContainer(false);
    }

    public static Attributes getContainerAttrs() {
        return new Attributes(CONT_ATTR_NST_NAME);
    }

    private static NST getContainerAttrNST() {
        return new NST(CONT_ATTR_NST_NAME);
    }

    public static NST getContainerNST() {
        return new NST(CONT_NST_NAME);
    }


    public static Attributes getLinkAttrs() {
        if (linkAttrs == null) {
            linkAttrs = new Attributes(LINK_ATTR_NST_NAME);
        }
        return linkAttrs;
    }

    private static NST getLinkAttrNST() {
        return new NST(LINK_ATTR_NST_NAME);
    }

    public static NST getLinkNST() {
        return new NST(LINK_NST_NAME);
    }

    /**
     * Returns an NST with the links that match a given condition on attributes
     * and internal columns such as id, o1_id, o2_id. See DB#getItems
     */
    public static NST getLinks(String filterDef) {
        return getAttrsForItems(getLinkNST(), getLinkAttrs(), filterDef);
    }

    public static NST getLinks(String filterDef, String attrList) {
        return getAttrsForItems(getLinkNST(), getLinkAttrs(), filterDef, attrList);
    }

    public static Attributes getObjectAttrs() {
        if (objAttrs == null) {
            objAttrs = new Attributes(OBJ_ATTR_NST_NAME);
        }
        return objAttrs;
    }

    private static NST getObjectAttrNST() {
        return new NST(OBJ_ATTR_NST_NAME);
    }

    public static NST getObjectNST() {
        return new NST(OBJ_NST_NAME);
    }

    /**
     * Returns an NST with the objects that match a given condition on attributes
     * and internal columns such as id. See DB#getItems
     */
    public static NST getObjects(String filterDef) {
        return getAttrsForItems(getObjectNST(), getObjectAttrs(), filterDef);
    }

    public static NST getObjects(String filterDef, String attrList) {
        return getAttrsForItems(getObjectNST(), getObjectAttrs(), filterDef, attrList);
    }

    /**
     * Returns the list of objects that are connected to itemId
     * Connected means in o2_id of links where o1_id = itemID, and reverse
     *
     * @param itemId
     * @return an NST with an id column
     */
    public static NST getObjectsConnectedTo(int itemId) {
        NST linkNST = getLinkNST();

        NST startingNST = linkNST.filter("o1_id = " + itemId, "o2_id");
        NST endingNST = linkNST.filter("o2_id = " + itemId, "o1_id");
        NST connectedNST = startingNST.union(endingNST, "o2_id");
        startingNST.release();
        endingNST.release();

        connectedNST.renameColumn("o2_id", "id");
        return connectedNST;
    }

    /**
     * Returns a lst with the names of the Proximity NSTs
     */
    public static List getProxNSTNames() {
        List proxNSTNames = new ArrayList();
        proxNSTNames.add(OBJ_NST_NAME);
        proxNSTNames.add(LINK_NST_NAME);
        proxNSTNames.add(CONT_NST_NAME);
        proxNSTNames.add(OBJ_ATTR_NST_NAME);
        proxNSTNames.add(LINK_ATTR_NST_NAME);
        proxNSTNames.add(CONT_ATTR_NST_NAME);
        proxNSTNames.add(SCHEMA_LOG_BAT_NAME);
        return proxNSTNames;
    }

    /**
     * @return my root Container
     * @
     */
    public static Container getRootContainer() {
        return new Container();      // NB: special root container constructor
    }

    /**
     * Returns this database's schema log, which is an ordered history of the
     * schema versions and the dates they were applied. For now simply returns
     * a descriptive String for each log entry. The List is empty if there is
     * no schema log BAT.
     *
     * @return List containing one String for each log entry. each String
     *         contains the date and version. empty if no schema log table found
     */
    public static List getSchemaLog() {
        List logEntries = new ArrayList();
        if (MonetUtil.isBATExists(SCHEMA_LOG_BAT_NAME)) {
            // found schema log BAT -> retrieve log
            ResultSet resultSet = MonetUtil.read("bat(\"" + SCHEMA_LOG_BAT_NAME + "\")");
            while (resultSet.next()) {
                String date = resultSet.getString(0);
                String version = resultSet.getString(1);
                logEntries.add(date + ": " + version);
            }
        } else {
            // no schema log BAT -> leave result empty
        }
        return logEntries;
    }

    /**
     * Called by createNewTempContainer(), returns the temporary 'system' container
     * that is to hold randomly-named child containers. Currently we simply use
     * a 'tmp' container under the root.
     *
     * @return
     * @
     */
    public static Container getTempParentContainer() {
        String tempParentContName = "tmp";
        Container rootContainer = getRootContainer();
        Container tempParentCont = rootContainer.getChild(tempParentContName);
        if (tempParentCont == null) {
            tempParentCont = rootContainer.createChild(tempParentContName);
        }
        return tempParentCont;
    }

    /**
     * Called by methods that want to initialize an empty database, i.e., one
     * with no Proximity tables. Often paired with calls to clearDB().
     * <p/>
     * Assumes that the database has been clear'ed (ie, that the prox_* tables
     * do not exist). If that condition is not true, then an error will be generated
     * and some BATs might stay as permanent but not referenced to by any other BATs
     * or variables.
     *
     * @
     * @see #clearDB
     */
    public static void initEmptyDB() {
        log.debug("* initializing Proximity database");

// object
        createProxNST(NSTTypeEnum.OBJ_NST_COL_NAMES, NSTTypeEnum.OBJ_NST_COL_TYPES, OBJ_NST_NAME);
        createProxNST(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES, OBJ_ATTR_NST_NAME);

// link
        createProxNST(NSTTypeEnum.LINK_NST_COL_NAMES, NSTTypeEnum.LINK_NST_COL_TYPES, LINK_NST_NAME);
        createProxNST(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES, LINK_ATTR_NST_NAME);

// container
        createProxNST(NSTTypeEnum.CONT_NST_COL_NAMES, NSTTypeEnum.CONT_NST_COL_TYPES, CONT_NST_NAME);
        createProxNST(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES, CONT_ATTR_NST_NAME);

        createSchemaLogBAT();
        log.info("* Proximity database initialized");
    }

    /**
     * Row-level method that creates a new link and returns its OID. This
     * overload creates the link with an arbitrary ID.
     *
     * @param o1OID
     * @param o2OID
     * @return new link's ID
     * @
     */
    public static int insertLink(int o1OID, int o2OID) {
        NST linkNST = getLinkNST();
        int nextInsertId = linkNST.max("link_id") + 1;
        linkNST.insertRow(new String[]{nextInsertId + "", o1OID + "", o2OID + ""});
        return nextInsertId;
    }

    /**
     * Row-level method that creates a new link and returns its OID. This
     * overload creates the link with the passed ID.
     *
     * @param linkOID
     * @param o1OID
     * @param o2OID
     * @
     */
    public static void insertLink(int linkOID, int o1OID, int o2OID) {
        NST linkNST = getLinkNST();
        linkNST.insertRow(new String[]{linkOID + "", o1OID + "", o2OID + ""});
    }

    /**
     * Row-level method that creates a new object and returns its OID. This
     * overload creates the object with an arbitrary ID.
     *
     * @return new object's ID
     * @
     */
    public static int insertObject() {
        NST objectNST = getObjectNST();
        int nextInsertId = objectNST.max("id") + 1;
        objectNST.insertRow(new String[]{nextInsertId + ""});
        return nextInsertId;
    }

    /**
     * Row-level method that creates a new object and returns its OID. This
     * overload creates the object with the passed ID.
     *
     * @param objectOID
     * @
     */
    public static void insertObject(int objectOID) {
        NST objectNST = getObjectNST();
        objectNST.insertRow(new String[]{objectOID + ""});
    }

    public static boolean isLinkExists(int linkId) {
        return (getLinkNST().getRowCount("link_id = " + linkId) > 0);
    }

    public static boolean isObjectExists(int objId) {
        return (getObjectNST().getRowCount("id = " + objId) > 0);
    }

    /**
     * @param nstName
     * @return true if nstName is one of the non-NST BATs in getProxNSTNames()
     */
    public static boolean isProxNSTBatName(String nstName) {
        return SCHEMA_LOG_BAT_NAME.equalsIgnoreCase(nstName);   // if more than one later, make a list
    }

    /**
     * Returns true if the name passed in corresponds to one of Proximity's NSTs.
     */
    public static boolean isProxNSTName(String nstName) {
        return getProxNSTNames().contains(nstName);
    }

    /**
     * @return true if the top-level Proximity tables are all defined, regardless
     *         of whether they're empty or not. returns false o/w
     * @see DB#getProxNSTNames
     */
    public static boolean isProxTablesDefined() {
        return MonetUtil.isBATExists(SCHEMA_LOG_BAT_NAME) &&
                MonetUtil.isBATExists(OBJ_NST_NAME) &&
                MonetUtil.isBATExists(LINK_NST_NAME) &&
                MonetUtil.isBATExists(CONT_NST_NAME) &&
                MonetUtil.isBATExists(OBJ_ATTR_NST_NAME) &&
                MonetUtil.isBATExists(LINK_ATTR_NST_NAME) &&
                MonetUtil.isBATExists(CONT_ATTR_NST_NAME);
    }

    /**
     * @return true if all of the top-level Proximity tables are defined and
     *         empty. returns false o/w. NB: not particularly fast
     */
    public static boolean isProxTablesEmpty() {
        return isProxTablesDefined() &&
                getObjectNST().getRowCount() == 0 &&
                getLinkNST().getRowCount() == 0 &&
                getContainerNST().getRowCount() == 0 &&
                getObjectAttrNST().getRowCount() == 0 &&
                getLinkAttrNST().getRowCount() == 0 &&
                getContainerAttrNST().getRowCount() == 0;
    }

    public static boolean isOpen() {
        return Connection.isOpen();
    }

    /**
     * Performs the join with attributes in the fastest possible way. See DB#getAttrsForItems
     */
    private static NST joinItemsWithAttrs(NST itemsNST, Attributes attrs, String attrList) {
        if (attrList.length() == 0) {
            return itemsNST.copy();
        }

        // Get the attributes
        String[] attrNames = attrList.split(",");
        NST[] attrNameNSTs = new NST[attrNames.length];
        for (int attrIdx = 0; attrIdx < attrNames.length; attrIdx++) {
            String attrName = attrNames[attrIdx].trim();
            attrNameNSTs[attrIdx] = attrs.getAttrDataNST(attrName);
        }

        // Sort the list of attributes by size. Put smaller ones first, so that join in this order is more efficient
        for (int attrIdx = 0; attrIdx < attrNames.length; attrIdx++) {
            for (int attrIdxJ = 0; attrIdxJ < attrNames.length - 1; attrIdxJ++) {
                if (attrNameNSTs[attrIdxJ].getRowCount() > attrNameNSTs[attrIdxJ + 1].getRowCount()) {
                    NST swapNST = attrNameNSTs[attrIdxJ];
                    attrNameNSTs[attrIdxJ] = attrNameNSTs[attrIdxJ + 1];
                    attrNameNSTs[attrIdxJ + 1] = swapNST;
                    String swapName = attrNames[attrIdxJ];
                    attrNames[attrIdxJ] = attrNames[attrIdxJ + 1];
                    attrNames[attrIdxJ + 1] = swapName;
                }
            }
        }

        // Now perform the joins in order
        String idCol = findIdColumn(itemsNST);
        NST currNST = itemsNST;
        String internalIDCol = "internal_id_col";
        currNST.renameColumn(idCol, internalIDCol);
        for (int attrId = 0; attrId < attrNames.length; attrId++) {
            String attrName = attrNames[attrId];
            NST attrDataNST = attrNameNSTs[attrId];
            NST newNST;
            newNST = currNST.leftOuterJoin(attrDataNST, internalIDCol + " EQ id");
            newNST.renameColumn("value", attrName.trim());
            newNST.removeColumn("id");
            currNST.release();
            currNST = newNST;
        }
        currNST.renameColumn(internalIDCol, idCol);
        return currNST;
    }

    /**
     * Returns a list of all the top-level NSTs in the system.
     * Considers BATs that do not begin with tmp_ nor bat_ (nor mapi_ nor monet_) and are persistent.
     */
    public static List ls() {
        String cmd = "view_bbp_name()" +
                ".kdiff(view_bbp_name().like_select(\"tmp_%\"))" +
                ".kdiff(view_bbp_name().like_select(\"bat_%\"))" +
                ".kdiff(view_bbp_kind().select(\"tran\"))" +
                ".print();";
        ResultSet resultSet = Connection.executeQuery(cmd);
        List<String> allNames = resultSet.toStringList(1);
        List<String> cleanNames = new ArrayList<String>();
        for (String name : allNames) {
            if (!name.startsWith("mapi") && !name.startsWith("monet")) {
                cleanNames.add(name);
            }
        }
        return cleanNames;
    }

    /**
     * Splits hostAndPort into consituent parts and calls other constructor.
     *
     * If the port number is <= NEW_MONET_PORT_LIMIT (40000) then use the new Monet protocol
     * otherwise, use the old Monet (4.6.2) protocol

     * @param hostAndPort
     * @
     */
    public static void open(String hostAndPort) {
        open(hostAndPort, false);
    }

    public static void open(String hostAndPort, boolean isNoSchemaCheck) {
        String[] hostAndPortEles = hostAndPort.split(":");
        Assert.condition(hostAndPortEles.length == 2 || hostAndPortEles.length == 3, "invalid hostAndPart " +
                "format; should be: <host>:<port>[:<connectionCount>]");

        String host = hostAndPortEles[0];
        int portInt = Integer.parseInt(hostAndPortEles[1]);
        int conn;
        if (hostAndPortEles.length == 3) {
            conn = Integer.parseInt(hostAndPortEles[2]);
        } else {
            conn = 1;
        }

        // localhost if no host is defined
        if (host.equals("")) {
            host = "localhost";
        }
        Connection.open(host, portInt, conn);

        // check schema version
        if (!isNoSchemaCheck) {
            checkSchemaVersion();
        }
    }

    /**
     * Populates the DB with information from a file.
     *
     * @param fileName
     * @see PopulateDB
     */
    public static void populateDB(String fileName) {
        PopulateDB.populateDB(fileName);
    }
}
