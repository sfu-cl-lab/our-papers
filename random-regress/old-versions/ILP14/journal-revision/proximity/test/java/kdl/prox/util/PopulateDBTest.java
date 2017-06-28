/**
 * $Id: PopulateDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: PopulateDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.util;

import java.util.HashSet;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;


public class PopulateDBTest extends TestCase {

    private static Logger log = Logger.getLogger(PopulateDBTest.class);
    private static final String CORE_ATTR_NAME = "CORE_ATTR_NAME";
    private static final String CONT_LINK_ATTR_NAME = "CONT_LINK_ATTR_NAME";
    private static final String TEST_CONTAINER = "TEST_CONTAINER";
    private static final String SUBG_ATTR_NAME = "SUBG_ATTR_NAME";

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testAttributes() {
        Attributes objectAttrs = DB.getObjectAttrs();
        Attributes linkAttrs = DB.getLinkAttrs();
        objectAttrs.deleteAllAttributes();
        linkAttrs.deleteAllAttributes();
        PopulateDB.populateDB(getClass(), "attributes.txt");


        assertTrue(objectAttrs.isAttributeDefined(CORE_ATTR_NAME));

        HashSet itemOIDValueSet = new HashSet();
        NST attrDataNST = objectAttrs.getAttrDataNST(CORE_ATTR_NAME);
        ResultSet resultSet = attrDataNST.selectRows();
        while (resultSet.next()) {
            String itemOID = resultSet.getString(1);
            String value = resultSet.getString(2);
            itemOIDValueSet.add(itemOID + "|" + value);
        }
        TestUtil.verifyCollections(new String[]{"20@0|+", "21@0|+"},
                itemOIDValueSet);


        assertTrue(linkAttrs.isAttributeDefined(CONT_LINK_ATTR_NAME));

        itemOIDValueSet = new HashSet();
        attrDataNST = linkAttrs.getAttrDataNST(CONT_LINK_ATTR_NAME);
        resultSet = attrDataNST.selectRows();
        while (resultSet.next()) {
            String itemOID = resultSet.getString(1);
            String value = resultSet.getFloat(2) + "";
            itemOIDValueSet.add(itemOID + "|" + value);
        }
        TestUtil.verifyCollections(new String[]{"1@0|10.0", "10@0|11.0", "4@0|12.0", "6@0|12.0",
                                                "8@0|14.0"},
                itemOIDValueSet);
    }

    public void testContainers() {
        DB.getRootContainer().deleteAllChildren();
        PopulateDB.populateDB(getClass(), "containers.txt");

        Container testCont = DB.getRootContainer().getChild(TEST_CONTAINER);
        assertNotNull(testCont);

        HashSet itemOIDValueSet = new HashSet();
        Attributes subgraphAttrs = testCont.getSubgraphAttrs();
        assertTrue(subgraphAttrs.isAttributeDefined(SUBG_ATTR_NAME));
        NST attrDataNST = subgraphAttrs.getAttrDataNST(SUBG_ATTR_NAME);
        ResultSet resultSet = attrDataNST.selectRows();
        while (resultSet.next()) {
            String itemOID = resultSet.getString(1);
            double value = resultSet.getDouble(2);
            itemOIDValueSet.add(itemOID + "|" + value + "");
        }
        TestUtil.verifyCollections(new String[]{"1@0|1.0", "2@0|1.0", "4@0|1.0", "3@0|2.0"},
                itemOIDValueSet);

        HashSet itemSubgNameSet = new HashSet();
        resultSet = testCont.getObjectsNST().selectRows();
        while (resultSet.next()) {
            String itemOID = resultSet.getString(1);
            String subgID = resultSet.getString(2);
            String name = resultSet.getString(3);
            itemSubgNameSet.add(itemOID + "|" + subgID + "|" + name);
        }
        TestUtil.verifyCollections(new String[]{"20@0|0@0|CORE_NAME", "4@0|1@0|OBJ_NAME", "5@0|1@0|OBJ_NAME"},
                itemSubgNameSet);

        itemSubgNameSet = new HashSet();
        resultSet = testCont.getLinksNST().selectRows();
        while (resultSet.next()) {
            String itemOID = resultSet.getString(1);
            String subgID = resultSet.getString(2);
            String name = resultSet.getString(3);
            itemSubgNameSet.add(itemOID + "|" + subgID + "|" + name);
        }
        TestUtil.verifyCollections(new String[]{"1@0|0@0|LINK_NAME", "2@0|0@0|LINK_NAME", "3@0|1@0|LINK_NAME", "4@0|1@0|LINK_NAME"},
                itemSubgNameSet);

    }

    public void testLinks() {
        NST linkNST = DB.getLinkNST();
        linkNST.deleteRows();
        PopulateDB.populateDB(getClass(), "links.txt");

        HashSet linkO1O2Set = new HashSet();
        ResultSet resultSet = linkNST.selectRows();
        while (resultSet.next()) {
            String linkOID = resultSet.getString(1);
            String o1OID = resultSet.getString(2);
            String o2OID = resultSet.getString(3);
            linkO1O2Set.add(linkOID + "|" + o1OID + "|" + o2OID);
        }
        TestUtil.verifyCollections(new String[]{"0@0|1@0|2@0", "5@0|6@0|6@0"},
                linkO1O2Set);
    }

    public void testLinksBadIds() {
        PopulateDB.LinkLineParser linkBlockParser = new PopulateDB.LinkLineParser();
        assertEquals(null, linkBlockParser.parseLineInternal("1 xx 3"));
        assertEquals(null, linkBlockParser.parseLineInternal("1 3"));
        assertEquals(null, linkBlockParser.parseLineInternal("1 3 3 4"));
    }

    public void testObjects() {
        NST objectNST = DB.getObjectNST();
        objectNST.deleteRows();
        PopulateDB.populateDB(getClass(), "objects.txt");
        TestUtil.verifyCollections(new String[]{"1@0", "2@0", "3@0", "5@0", "11@0", "13@0", "15@0"},
                new HashSet(objectNST.selectRows().toStringList(1)));
    }

    public void testObjectsBadIds() {
        PopulateDB.ObjectLineParser objectBlockParser = new PopulateDB.ObjectLineParser();
        assertEquals(null, objectBlockParser.parseLineInternal("1sdfs"));
        assertEquals(null, objectBlockParser.parseLineInternal("[1sdf]"));
        assertEquals(null, objectBlockParser.parseLineInternal("1}2"));
        assertEquals(null, objectBlockParser.parseLineInternal("1.2"));
        assertEquals(null, objectBlockParser.parseLineInternal("[1,,3]"));
        assertEquals(null, objectBlockParser.parseLineInternal("[1]"));
    }

    public void testObjectsGoodIds() {
        PopulateDB.ObjectLineParser objectBlockParser = new PopulateDB.ObjectLineParser();
        TestUtil.verifyCollections(new int[]{1}, objectBlockParser.parseLineInternal("1"));
        TestUtil.verifyCollections(new int[]{1, 2, 3}, objectBlockParser.parseLineInternal("[1-3]"));
        TestUtil.verifyCollections(new int[]{3}, objectBlockParser.parseLineInternal("[3-3]"));
        TestUtil.verifyCollections(new int[]{}, objectBlockParser.parseLineInternal("[3-1]"));
        TestUtil.verifyCollections(new int[]{1, 2, 3}, objectBlockParser.parseLineInternal("[1,2,3]"));
        TestUtil.verifyCollections(new int[]{1, 3}, objectBlockParser.parseLineInternal("[1,3]"));
        TestUtil.verifyCollections(new int[]{1, 3}, objectBlockParser.parseLineInternal("[3,1]"));
        TestUtil.verifyCollections(new int[]{3, 3}, objectBlockParser.parseLineInternal("[3,3]"));
    }

    public void testObjectsNoClosing() {
        NST objectNST = DB.getObjectNST();
        objectNST.deleteRows();
        try {
            PopulateDB.populateDB(getClass(), "objects-noclose.txt");
            fail("Should have complained about missing }");
        } catch (IllegalArgumentException exc) {
            // expected
        }
    }

}
