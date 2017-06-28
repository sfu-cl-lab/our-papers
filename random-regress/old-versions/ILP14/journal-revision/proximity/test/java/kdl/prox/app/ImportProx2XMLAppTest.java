/**
 * $Id: ImportProx2XMLAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ImportProx2XMLAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ImportProx2XMLAppTest extends TestCase {

    Logger log = Logger.getLogger(ImportProx2XMLAppTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testImportEmptyDB() throws Exception {
        URL testXMLFileURL = getClass().getResource("prox2db-empty.xml");
        new ImportProx2XMLApp(testXMLFileURL.getFile());
    }


    public void testImportNonEmptyDB() throws Exception {
        // import the test database
        URL testXMLFileURL = getClass().getResource("prox2db-testdb.xml");
        new ImportProx2XMLApp(testXMLFileURL.getFile());

        // check the resulting database
        verifyObjects(new String[]{"0", "1", "2", "3", "4"});
        verifyLinks(new String[][]{
                {"0", "1", "2"},
                {"1", "0", "3"},
                {"2", "1", "3"},
                {"3", "1", "2"},
                {"4", "3", "4"}});
        verifyAttribute("name", "O", "VARCHAR", new String[][]{
                {"3", "Matthew Cornell"},
                {"2", "David Jensen"},
                {"4", "our place (an already existing obj)"}});
        verifyAttribute("obj-type", "O", "VARCHAR", new String[][]{
                {"3", "person"},
                {"2", "person"},
                {"1", "research group"}});
        verifyAttribute("office-num", "O", "INTEGER", new String[][]{
                {"3", "218"},
                {"2", "238"}});
        verifyAttribute("double-test", "O", "DOUBLE", new String[][]{
                {"3", "3"},
                {"2", "2.1000000000000001"}});  // NB: values match those in file and take into account double precision math changes in Monet version 4.3.14
        verifyAttribute("date-test", "O", "DATE", new String[][]{
                {"3", "2003-08-25"},
                {"2", "1962-10-22"}});
        verifyAttribute("link-test", "L", "VARCHAR", new String[][]{
                {"3", "this looks like an object ref: _[xx],_but it isn_t because it_s inside a link attribute"}});
        verifyAttribute("link-type", "L", "VARCHAR", new String[][]{
                {"2", "member"},
                {"3", "member"},
                {"4", "lives-at"}});
        verifyAttribute("nickname", "O", "VARCHAR", new String[][]{
                {"1", "kdl"},
                {"2", "dj"},
                {"3", "me"},
                {"4", "home"}});
        verifyAttribute("sa.implied", "L", "INTEGER", new String[][]{
                {"0", "1"},
                {"1", "1"}});
        verifyAttribute("sa.undefined", "O", "INTEGER", new String[][]{{}});

        veryifyCollection("test-coll", new String[]{"0"},
                new String[][]{
                        {"0", "0", "#test"},
                        {"0", "1", "kdl"},
                        {"0", "2", "dj"},
                        {"0", "3", "me"},
                        {"0", "4", "home"}},
                new String[][]{
                        {"0", "0", "0:implied:kdl->dj"},
                        {"0", "1", "1:implied:#test->me"},
                        {"0", "2", "2:kdl->me"},
                        {"0", "3", "3:kdl->dj"},
                        {"0", "4", "4:me->home"}});
        veryifyCollection("qg2test", new String[]{"1", "2", "3"},
                new String[][]{
                        {"1", "1", "group"},
                        {"2", "1", "group"},
                        {"3", "1", "group"},
                        {"1", "2", "person"},
                        {"2", "3", "person"},
                        {"3", "2", "person"}},
                new String[][]{
                        {"1", "0", "group-person"},
                        {"2", "2", "group-person"},
                        {"3", "3", "group-person"}});
    }


    public void testStatsOnly() throws Exception {
        URL testXMLFileURL = getClass().getResource("prox2db-testdb.xml");
        String testXMLFile = testXMLFileURL.getFile();
        ImportProx2XMLApp importXMLApp = new ImportProx2XMLApp(testXMLFile, true);  //compute stats only

        assertEquals(10, importXMLApp.numAttributes);
        assertEquals(22, importXMLApp.numAttrValues);
        assertEquals(2, importXMLApp.numContainers);
        assertEquals(5, importXMLApp.numLinks);
        assertEquals(5, importXMLApp.numObjects);
        assertEquals(19, importXMLApp.numSubgraphItems);
        assertEquals(4, importXMLApp.numSubgraphs);
    }


    /**
     * Verifies the database contains exactly the passed attribute data.
     *
     * @param attrName
     * @param itemType     Proximity2 item type
     * @param dataType     Proximity2 data type
     * @param itemIDValues array of String[], each of which contains an itemID
     *                     and a value
     * @
     */
    private void verifyAttribute(String attrName, String itemType,
                                 String dataType, String[][] itemIDValues) {
        // fill expectedIDValues
        List expectedIDValues = new ArrayList();    // catenation of itemID + value
        for (int idValIdx = 0; idValIdx < itemIDValues.length; idValIdx++) {
            String[] itemIDValue = itemIDValues[idValIdx];
            if (itemIDValue.length != 0) {
                String itemID = itemIDValue[0];
                String value = itemIDValue[1];
                expectedIDValues.add(itemID + value);
            }
        }

        // fill actualIDValues
        List actualIDValues = new ArrayList();      // catenation of itemID + value
        boolean isObject = ImportProx2XMLApp.isObjectItemType(itemType);
        Attributes itemAttrs = (isObject ?
                DB.getObjectAttrs() : DB.getLinkAttrs());
        NST attrDataNST = itemAttrs.getAttrDataNST(attrName);
        ResultSet resultSet = attrDataNST.selectRows("id, value");
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            String value = resultSet.getString(2);
            actualIDValues.add(itemID + value);
        }

        // verify type
        DataTypeEnum expectedDataTypeEnum = ImportProx2XMLApp.dataTypeForProx2DataType(dataType);
        String expectedTypeDef = expectedDataTypeEnum.toString();
        String actualTypeDef = itemAttrs.getAttrTypeDef(attrName);
        assertEquals(expectedTypeDef, actualTypeDef);

        // compare
        assertEquals(expectedIDValues.size(), actualIDValues.size());
        assertTrue(expectedIDValues.containsAll(actualIDValues));
    }


    /**
     * Verifies the database contains exactly the passed collection data.
     *
     * @param contName
     * @param subgOIDs              list of expected subgraph OIDs
     * @param objSubgItemTypeNames  an array of String[], each of which contains
     *                              an expected subgID, itemID, and name for objects
     * @param linkSubgItemTypeNames "" for links
     */
    private void veryifyCollection(String contName, String[] subgOIDs,
                                   String[][] objSubgItemTypeNames,
                                   String[][] linkSubgItemTypeNames) {
        Container container = DB.getRootContainer().getChild(contName);

        // verify subgraph OIDs
        List expectedSubgOIDs = Arrays.asList(subgOIDs);
        List actualSubgOIDInts = container.getSubgraphOIDs();    // IntegerS
        List actualSubgOIDs = new ArrayList();
        for (Iterator actualSubgOIDIntIter = actualSubgOIDInts.iterator();
             actualSubgOIDIntIter.hasNext();) {
            Integer actualSubgOIDInt = (Integer) actualSubgOIDIntIter.next();
            actualSubgOIDs.add(actualSubgOIDInt.toString());
        }
        assertEquals(expectedSubgOIDs.size(), actualSubgOIDs.size());
        assertTrue(expectedSubgOIDs.containsAll(actualSubgOIDs));

        // verify subgraph contents
        NST objSharedItemNST = container.getItemNST(true);
        NST linkSharedItemNST = container.getItemNST(false);
        veryifSubgraphItems(objSubgItemTypeNames, objSharedItemNST);
        veryifSubgraphItems(linkSubgItemTypeNames, linkSharedItemNST);
    }


    /**
     * Verifies the database contains exactly the passed link data.
     *
     * @param linkSelfO1O2IDs an array of String[], each of which contains a
     *                        linkID, o1ID, and o2ID
     * @
     */
    private void verifyLinks(String[][] linkSelfO1O2IDs) {
        // fill expectedLinkIDs
        List expectedLinkIDs = new ArrayList();     // catenation of linkID + o1ID + o2ID
        for (int linkIdx = 0; linkIdx < linkSelfO1O2IDs.length; linkIdx++) {
            String[] linkIDs = linkSelfO1O2IDs[linkIdx];
            String linkID = linkIDs[0];
            String o1ID = linkIDs[1];
            String o2ID = linkIDs[2];
            expectedLinkIDs.add(linkID + o1ID + o2ID);
        }

        // fill actualLinkIDs
        List actualLinkIDs = new ArrayList();   // catenation of linkID + o1ID + o2ID
        NST linkNST = DB.getLinkNST();
        ResultSet resultSet = linkNST.selectRows();   // "o1_id", "o2_id"
        while (resultSet.next()) {
            int linkID = resultSet.getOID(1);
            int o1ID = resultSet.getOID(2);
            int o2ID = resultSet.getOID(3);
            actualLinkIDs.add(linkID + "" + o1ID + "" + o2ID);
        }

        // compare
        assertEquals(expectedLinkIDs.size(), actualLinkIDs.size());
        assertTrue(expectedLinkIDs.containsAll(actualLinkIDs));
    }


    /**
     * Verifies the database contains exactly the passed object data.
     *
     * @param objIDs an array of objectIDs
     * @
     */
    private void verifyObjects(String[] objIDs) {
        NST objectNST = DB.getObjectNST();
        ResultSet resultSet = objectNST.selectRows();  // "ID"
        List expectedIDStrs = Arrays.asList(objIDs);

        // fill actualIDStrs
        List actualIDInts = resultSet.toOIDList(1);
        List actualIDStrs = new ArrayList();
        for (Iterator actualIDIntIter = actualIDInts.iterator(); actualIDIntIter.hasNext();) {
            Integer actualIDInt = (Integer) actualIDIntIter.next();
            actualIDStrs.add(actualIDInt.toString());
        }

        // compare
        assertEquals(expectedIDStrs.size(), actualIDStrs.size());
        assertTrue(expectedIDStrs.containsAll(actualIDStrs));
    }


    /**
     * Called by veryifyCollection(), verifies the passed shared item NST
     * contains the expected subgraph items.
     *
     * @param subgIDItemIDNames expected subgraph items. an array of String[],
     *                          each of which contains an expected subgID, itemID, and name
     * @param sharedItemNST     actual data NST
     */
    private void veryifSubgraphItems(String[][] subgIDItemIDNames, NST sharedItemNST) {
        // fill expectedSubgIDItemIDName
        List expectedSubgIDItemIDName = new ArrayList();    // catenation of subgID, itemID, name
        for (int subgIdx = 0; subgIdx < subgIDItemIDNames.length; subgIdx++) {
            String[] subgIDItemIDName = subgIDItemIDNames[subgIdx];
            String subgID = subgIDItemIDName[0];
            String itemID = subgIDItemIDName[1];
            String name = subgIDItemIDName[2];
            expectedSubgIDItemIDName.add(subgID + itemID + name);
        }

        // fill actualLinkIDs
        List actualLinkIDs = new ArrayList();   // catenation of linkID + o1ID + o2ID
        ResultSet resultSet = sharedItemNST.selectRows();   // ""item_id", "subg_id", "name"
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            int subgID = resultSet.getOID(2);
            String name = resultSet.getString(3);
            actualLinkIDs.add(subgID + "" + itemID + "" + name);
        }

        // compare
        assertEquals(expectedSubgIDItemIDName.size(), actualLinkIDs.size());
        assertTrue(expectedSubgIDItemIDName.containsAll(actualLinkIDs));
    }


}
