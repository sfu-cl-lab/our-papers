/**
 * $Id: TFM01Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.tfm;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.QGraphTestingUtil;
import kdl.prox.qgraph2.util.QGUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Tests TFM01.
 */
public class TFM01Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM01Test.class);

    private QGUtil qgUtil;
    private QGUtil qgUtilOnSource;
    private QGUtil qgUtilOnEmptySource;
    private Attributes attrs;
    Container sourceContainer;
    Container emptySourceContainer;


    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        attrs = DB.getObjectAttrs();
        attrs.defineAttribute("attr1", "str");
        attrs.defineAttribute("attr2", "str");
        attrs.defineAttribute("attr3", "int");

        /**
         * insert this data
         * 1   F  A  2
         * 2   M  B  3
         * 3   F  -  4
         * 4   M  -  2
         * 5   F  C  1
         * 5   F  -  -
         */
        DB.getObjectNST().deleteRows();
        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);
        DB.insertObject(4);
        DB.insertObject(5);

        NST attr1DataNST = attrs.getAttrDataNST("attr1");
        attr1DataNST.insertRow(new String[]{"1", "F"});
        attr1DataNST.insertRow(new String[]{"2", "M"});
        attr1DataNST.insertRow(new String[]{"3", "F"});
        attr1DataNST.insertRow(new String[]{"4", "M"});
        attr1DataNST.insertRow(new String[]{"5", "F"});
        attr1DataNST.insertRow(new String[]{"5", "F"});
        attr1DataNST.release();

        NST attr2DataNST = attrs.getAttrDataNST("attr2");
        attr2DataNST.insertRow(new String[]{"1", "A"});
        attr2DataNST.insertRow(new String[]{"2", "B"});
        attr2DataNST.insertRow(new String[]{"5", "C"});
        attr2DataNST.release();

        NST attr3DataNST = attrs.getAttrDataNST("attr3");
        attr3DataNST.insertRow(new String[]{"1", "2"});
        attr3DataNST.insertRow(new String[]{"2", "3"});
        attr3DataNST.insertRow(new String[]{"3", "4"});
        attr3DataNST.insertRow(new String[]{"4", "2"});
        attr3DataNST.insertRow(new String[]{"5", "1"});
        attr3DataNST.release();

        DB.getRootContainer().deleteAllChildren();
        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "2", "B"});
        objectNST.insertRow(new String[]{"3", "3", "C"});
        emptySourceContainer = DB.getRootContainer().createChild("cont-2");

        qgUtil = new QGUtil(null);
        qgUtilOnSource = new QGUtil(sourceContainer);
        qgUtilOnEmptySource = new QGUtil(emptySourceContainer);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        attrs.deleteAttribute("attr1");
        attrs.deleteAttribute("attr2");
        attrs.deleteAttribute("attr3");
        DB.getObjectNST().deleteRows();
        qgUtil.release();
        qgUtilOnSource.release();
        qgUtilOnEmptySource.release();
        TestUtil.closeTestConnection();
    }


    /**
     * attr1 = F and exists attr2
     * Expected results
     * 1
     * 5
     */
    public void testAND() {
        Element testEle1 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element testEle2 = QGraphTestingUtil.makeTestElement("attr2", "exists", null);
        Element condEle = QGraphTestingUtil.combineTestElements(testEle1, testEle2, "AND");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }

    /**
     * attr1 = F and exists attr2, on source
     * Expected results
     * 1
     */
    public void testANDOnSource() {
        Element testEle1 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element testEle2 = QGraphTestingUtil.makeTestElement("attr2", "exists", null);
        Element condEle = QGraphTestingUtil.combineTestElements(testEle1, testEle2, "AND");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtilOnSource, inQGVertex);
        int[] expectedItems = new int[]{1};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }

    /**
     * attr1 = F and exists attr2, on source
     * Expected results
     */
    public void testANDOnEmptySource() {
        Element testEle1 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element testEle2 = QGraphTestingUtil.makeTestElement("attr2", "exists", null);
        Element condEle = QGraphTestingUtil.combineTestElements(testEle1, testEle2, "AND");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtilOnEmptySource, inQGVertex);
        int[] expectedItems = new int[]{};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * attr1 = F and exists attr2
     * Expected results
     * 1
     * 2
     * 3
     * 5
     */
    public void testOR() {
        Element testEle1 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element testEle2 = QGraphTestingUtil.makeTestElement("attr2", "exists", null);
        Element condEle = QGraphTestingUtil.combineTestElements(testEle1, testEle2, "or");   // NB: lower case
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 2, 3, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();

        Element testEle3 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element testEle4 = QGraphTestingUtil.makeTestElement("attr2", "exists", null);
        Element condEle2 = QGraphTestingUtil.combineTestElements(testEle4, testEle3, "or");   // NB: lower case
        inQGVertex = new QGVertex("name1", condEle2, null);
        tfm01 = new TFM01();
        nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        expectedItems = new int[]{1, 2, 3, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * exists attr2
     * Expected results
     * 1
     * 2
     * 5
     */
    public void testExists() {
        Element condEle = QGraphTestingUtil.makeConditionElement("attr2", "exists", null);
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 2, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * No condition.
     * Expected results:
     * 1
     * 2
     * 3
     * 4
     * 5
     */
    public void testNoCondition() {
        QGVertex inQGVertex = new QGVertex("name1", null, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 2, 3, 4, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }

    /**
     * No condition, on source container
     * Expected results:
     * 1
     * 2
     * 3
     */
    public void testNoConditionOnSource() {
        QGVertex inQGVertex = new QGVertex("name1", null, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtilOnSource, inQGVertex);
        int[] expectedItems = new int[]{1, 2, 3};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }

    /**
     * No condition, on source container
     * Expected results:
     */
    public void testNoConditionOnEmptySource() {
        QGVertex inQGVertex = new QGVertex("name1", null, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtilOnEmptySource, inQGVertex);
        int[] expectedItems = new int[]{};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * not attr1 = F
     * Expected results
     * 2
     * 4
     */
    public void testNOT() {
        Element testEle1 = QGraphTestingUtil.makeTestElement("attr1", "eq", "F");
        Element condEle = QGraphTestingUtil.combineTestElements(testEle1, null, "NOT");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{2, 4};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * attr3 >= 3
     * Expected results
     * 2
     * 3
     */
    public void testOneConditionGE() {
        Element condEle = QGraphTestingUtil.makeConditionElement("attr3", "ge", "3");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{2, 3};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * attr3 > 3
     * Expected results
     * 3
     */
    public void testOneConditionGT() {
        Element condEle = QGraphTestingUtil.makeConditionElement("attr3", "gt", "3");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{3};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * attr3 <= 3
     * Expected results
     * 1
     * 2
     * 4
     * 5
     */
    public void testOneConditionLE() {
        Element condEle = QGraphTestingUtil.makeConditionElement("attr3", "le", "3");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 2, 4, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    /**
     * attr3 < 3
     * Expected results
     * 1
     * 4
     * 5
     */
    public void testOneConditionLT() {
        Element condEle = QGraphTestingUtil.makeConditionElement("attr3", "lt", "3");
        QGVertex inQGVertex = new QGVertex("name1", condEle, null);
        TFM01 tfm01 = new TFM01();
        NST nst = tfm01.execTFMExecInternal(qgUtil, inQGVertex);
        int[] expectedItems = new int[]{1, 4, 5};
        validateResultList(nst, expectedItems, inQGVertex.catenatedName());
        nst.release();
    }


    private void validateResultList(NST nst, int[] expectedItems, String expectedItemName) {
        List itemIDs = new ArrayList();
        Set subgIDs = new HashSet();
        Set names = new HashSet();
        assertEquals(expectedItems.length, nst.getRowCount());
        ResultSet resultSet = nst.selectRows();
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            int subgID = resultSet.getOID(2);
            String name = resultSet.getString(3);
            itemIDs.add(new Integer(itemID));
            subgIDs.add(new Integer(subgID));
            names.add(name);
        }
        assertEquals(expectedItems.length, itemIDs.size());
        for (int i = 0; i < expectedItems.length; i++) {
            Integer id = new Integer(expectedItems[i]);
            assertTrue(itemIDs.contains(id));
        }

        assertEquals(expectedItems.length, subgIDs.size());

        if (expectedItems.length > 0) {
            assertEquals(1, names.size());
            assertTrue(names.contains(expectedItemName));
        }
    }


}
