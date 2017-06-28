/**
 * $Id: CorrelationTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CorrelationTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rpt.RPTState;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: afast
 * Date: Mar 12, 2007
 * Time: 3:40:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class CorrelationTest extends TestCase {

    private Logger log = Logger.getLogger(CorrelationTest.class);
    private Container testContainer;

    private static String OBJ_NAME = "S";
    private static String CORE_NAME = "M";
    private static String DUMMY_ATTR_NAME = "dummy-attr";
    private static String CLASS_ATTR_NAME = "class-attr";

    private int subgOID1;
    private int subgOID2;
    private int subgOID3;
    private int subgOID4;
    private int subgOID5;

    List discItemAttrList;
    List contItemAttrList;

    /**
     * Creates subgraphs, objects, and attributes
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {

        super.setUp();

        // connect to DB
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        DB.beginScope();

        // make container
        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
        String realChildName = "test-container";
        testContainer = rootContainer.createChild(realChildName);

        // insert subgraphs
        subgOID1 = 0;
        Subgraph subgraph1 = testContainer.getSubgraph(subgOID1);

        subgOID2 = 1;
        Subgraph subgraph2 = testContainer.getSubgraph(subgOID2);

        subgOID3 = 2;
        Subgraph subgraph3 = testContainer.getSubgraph(subgOID3);

        subgOID4 = 3;
        Subgraph subgraph4 = testContainer.getSubgraph(subgOID4);

        subgOID5 = 4;
        Subgraph subgraph5 = testContainer.getSubgraph(subgOID5);

        // insert multiple objects to each subgraph
        subgraph1.insertObject(1, OBJ_NAME);
        subgraph1.insertObject(2, OBJ_NAME);
        subgraph1.insertObject(3, CORE_NAME);

        subgraph2.insertObject(1, OBJ_NAME);
        subgraph2.insertObject(4, CORE_NAME);

        subgraph3.insertObject(1, OBJ_NAME);
        subgraph3.insertObject(5, CORE_NAME);

        subgraph4.insertObject(2, OBJ_NAME);
        subgraph4.insertObject(6, CORE_NAME);

        subgraph5.insertObject(1, OBJ_NAME);
        subgraph5.insertObject(2, OBJ_NAME);
        subgraph5.insertObject(7, CORE_NAME);

        // define attributes
        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.deleteAllAttributes();
        objectAttrs.defineAttribute(DUMMY_ATTR_NAME, "str");
        objectAttrs.defineAttribute(CLASS_ATTR_NAME, "str");

        // assign attribute values to objects

        NST attrDataNST;
        attrDataNST = objectAttrs.getAttrDataNST(DUMMY_ATTR_NAME);
        attrDataNST.deleteRows();
        attrDataNST.insertRow("1,red");
        attrDataNST.insertRow("2,blue");

        attrDataNST = objectAttrs.getAttrDataNST(CLASS_ATTR_NAME);
        attrDataNST.deleteRows();
        attrDataNST.insertRow("3,1");
        attrDataNST.insertRow("4,0");
        attrDataNST.insertRow("5,1");
        attrDataNST.insertRow("6,1");
        attrDataNST.insertRow("7,0");

        testContainer.getSubgraphAttrs().defineAttribute(CLASS_ATTR_NAME, "int");
        testContainer.getSubgraphAttrs().getAttrDataNST(CLASS_ATTR_NAME).insertRow("0, 1");
        testContainer.getSubgraphAttrs().getAttrDataNST(CLASS_ATTR_NAME).insertRow("1, 1");
        testContainer.getSubgraphAttrs().getAttrDataNST(CLASS_ATTR_NAME).insertRow("2, 1");
        testContainer.getSubgraphAttrs().getAttrDataNST(CLASS_ATTR_NAME).insertRow("3, 0");
        testContainer.getSubgraphAttrs().getAttrDataNST(CLASS_ATTR_NAME).insertRow("4, 0");

        DB.endScope();

    }


    protected void tearDown() throws Exception {
        super.tearDown();    //To change body of overridden methods use File | Settings | File Templates.
        TestUtil.closeTestConnection();
    }

/*    public void testComputeCorrelation() {
        List<Object[]> pairsList = new ArrayList<Object[]>();
        pairsList.add(new Object[]{0, 1});
        pairsList.add(new Object[]{0, 0});
        pairsList.add(new Object[]{0, 0});
        pairsList.add(new Object[]{1, 1});

        Correlation ac = new Correlation();
        double score = ac.computeCorrelation(pairsList, new Object[]{0, 1}, new Object[]{0, 1});
        assertEquals(score, 0.7764577787044229, 0.000001);
    }
*/

    public void testComputeCorrelationNST() {
        NST pairsNST = new NST("attr1,attr2", "int,int");
        pairsNST.insertRow("0,0");
        pairsNST.insertRow("1,0");
        pairsNST.insertRow("1,0");
        pairsNST.insertRow("1,1");
        pairsNST.insertRow("1,1");
        pairsNST.insertRow("1,1");
        pairsNST.insertRow("2,1");
        pairsNST.insertRow("2,1");
        pairsNST.insertRow("0,2");
        pairsNST.insertRow("0,2");
        pairsNST.insertRow("1,2");
        pairsNST.insertRow("2,2");
        pairsNST.insertRow("2,2");
        pairsNST.insertRow("2,2");

        double score = Correlation.computeCorrelation(pairsNST, new Object[]{0, 1, 2}, new Object[]{0, 1, 2});
        assertEquals(score, 0.6191725848471512, 0.000001);

        pairsNST = new NST("attr1,attr2", "dbl,dbl");
        pairsNST.insertRow("0,0");
        pairsNST.insertRow("1,0.98");
        pairsNST.insertRow(".5,.52");
        pairsNST.insertRow(".35,.4");
        pairsNST.insertRow(".2,.2");
        pairsNST.insertRow(".8,.79");

        score = Correlation.computeCorrelation(pairsNST, null, null);
        assertEquals(score, 0.9976413541057616, 0.000001);

    }

    public void testGetAllPairs() {

        Container container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 1, B");
        container.getObjectsNST().insertRow("4, 2, A");
        container.getObjectsNST().insertRow("5, 2, B");
        container.getObjectsNST().insertRow("6, 2, B");
        container.getObjectsNST().insertRow("7, 3, A");
        container.getObjectsNST().insertRow("8, 3, B");
        container.getObjectsNST().insertRow("9, 3, B");

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("1, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("3, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("4, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("5, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("7, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("8, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("9, f");

        NST pairsNST = Correlation.getAllPairs(container, "A", "gender", "B", "gender");
        pairsNST.print();

        double score = Correlation.computeCorrelation(pairsNST, "gender", "gender", false);
        assertEquals(score, 1.0, 0.000001);
    }

    public void testGetAutocorrelationThroughItem() {
        RPTState state = new RPTState();
        state.trainContainer = testContainer;
        state.classLabel = new AttributeSource(CLASS_ATTR_NAME);
        state.classLabel.init(testContainer, state.nstCache);

        Correlation ac = new Correlation();
        double score = ac.getAutocorrelationThroughItem(state, OBJ_NAME);
        assertEquals(score, 0.4658683266205697, 0.000000001);

    }


}
