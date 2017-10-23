/**
 * $Id: TFM02Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM02Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGraphTestingUtil;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;


/**
 * Tests TFM02.
 *
 * @see TFM02
 */
public class TFM02Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM02Test.class);

    private QGUtil qgUtil;
    private Attributes attrs;

    private NST aConsolidatedObjTempSGINST;
    private NST aConsolidatedLinkTempSGINST;


    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        qgUtil = new QGUtil(null);
        DB.beginScope();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        DB.insertObject(1);  // a (subg 1)
        DB.insertObject(2);  // b (subg 1)
        DB.insertObject(3);  // a (subg 2)
        DB.insertObject(4);  // b (subg 2)
        DB.insertObject(5);  // a (subg 3)
        DB.insertObject(6);  // b (subg 3)
        DB.insertObject(7);  // a (subg 4)
        DB.insertObject(8);  // b (subg 4)
        DB.insertObject(9);  // a (subg 5)
        DB.insertObject(10); // b (subg 5)
        DB.insertObject(11); // b (subg 5)

        attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST objAttr1DataNST = attrs.getAttrDataNST("attr1");
        objAttr1DataNST.insertRow(new String[]{"1", "A"});
        objAttr1DataNST.insertRow(new String[]{"2", "B"});
        objAttr1DataNST.insertRow(new String[]{"3", "A"});
        objAttr1DataNST.insertRow(new String[]{"4", "B"});
        objAttr1DataNST.insertRow(new String[]{"5", "A"});
        objAttr1DataNST.insertRow(new String[]{"6", "B"});
        objAttr1DataNST.insertRow(new String[]{"7", "A"});
        objAttr1DataNST.insertRow(new String[]{"8", "B"});
        objAttr1DataNST.insertRow(new String[]{"9", "A"});
        objAttr1DataNST.insertRow(new String[]{"10", "B"});
        objAttr1DataNST.insertRow(new String[]{"11", "B"});

        DB.insertLink(1, 1, 2);   // x (subg 1)
        DB.insertLink(2, 1, 2);   // y (subg 1)
        DB.insertLink(3, 1, 2);   // y (subg 1)
        DB.insertLink(4, 3, 4);   // x (subg 2)
        DB.insertLink(5, 4, 3);   // y (subg 2)
        DB.insertLink(6, 4, 3);   // y (subg 2)
        DB.insertLink(7, 5, 6);   // x (subg 3)
        DB.insertLink(8, 7, 8);   // x (subg 4)
        DB.insertLink(9, 7, 8);   // y (subg 4)
        DB.insertLink(10, 9, 10); // x (subg 5)
        DB.insertLink(11, 9, 11); // x (subg 5)
        DB.insertLink(12, 10, 9); // y (subg 5)
        DB.insertLink(13, 10, 9); // y (subg 5)
        DB.insertLink(14, 10, 9); // y (subg 5)
        DB.insertLink(15, 9, 9); // x (subg 5 -- self loop from A9)

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST linkAttr1DataNST = attrs.getAttrDataNST("attr1");
        linkAttr1DataNST.insertRow(new String[]{"1", "X"});
        linkAttr1DataNST.insertRow(new String[]{"2", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"3", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"4", "X"});
        linkAttr1DataNST.insertRow(new String[]{"5", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"6", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"7", "X"});
        linkAttr1DataNST.insertRow(new String[]{"8", "X"});
        linkAttr1DataNST.insertRow(new String[]{"9", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"10", "X"});
        linkAttr1DataNST.insertRow(new String[]{"11", "X"});
        linkAttr1DataNST.insertRow(new String[]{"12", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"13", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"14", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"15", "X"});

        // create input
        List aObjBObjALinkBLinkTempSGINSTs = makeAObjALinkTempSGINSTs();
        aConsolidatedObjTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(0);
        aConsolidatedLinkTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(1);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAttribute("attr1");
        DB.getLinkAttrs().deleteAttribute("attr1");
        qgUtil.release();
        DB.endScope();
        TestUtil.closeTestConnection();
    }


    /**
     * Three subgraphs with As connected to Bx via X links (Y links will
     * be brought in by transformation)
     *
     * @return
     * @
     */
    private List makeAObjALinkTempSGINSTs() {
        NST aObjTempSGINST = makeTempSGINST();
        aObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        aObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        aObjTempSGINST.insertRow(new String[]{"3", "2", "A"});
        aObjTempSGINST.insertRow(new String[]{"4", "2", "B"});
        aObjTempSGINST.insertRow(new String[]{"5", "3", "A"});
        aObjTempSGINST.insertRow(new String[]{"6", "3", "B"});
        aObjTempSGINST.insertRow(new String[]{"7", "4", "A"});
        aObjTempSGINST.insertRow(new String[]{"8", "4", "B"});
        aObjTempSGINST.insertRow(new String[]{"9", "5", "A"});
        aObjTempSGINST.insertRow(new String[]{"10", "5", "B"});
        aObjTempSGINST.insertRow(new String[]{"11", "5", "B"});
        aObjTempSGINST.insertRow(new String[]{"9", "5", "B"}); //same as A, but B

        NST aLinkTempSGINST = makeTempSGINST();
        aLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"7", "3", "X"});
        aLinkTempSGINST.insertRow(new String[]{"8", "4", "X"});
        aLinkTempSGINST.insertRow(new String[]{"10", "5", "X"});
        aLinkTempSGINST.insertRow(new String[]{"11", "5", "X"});
        aLinkTempSGINST.insertRow(new String[]{"15", "5", "X"}); //self loop on A

        List aObjALinkTempSGINSTs = new ArrayList();
        aObjALinkTempSGINSTs.add(aObjTempSGINST);
        aObjALinkTempSGINSTs.add(aLinkTempSGINST);
        return aObjALinkTempSGINSTs;
    }


    private NST makeTempSGINST() {
        return SGIUtil.createTempSGINST();
    }


    /**
     * Brings in subgraphs 1 and 4, but subgraph 3 has been expanded
     * so that each new subgraph has one of the new Y links
     *
     * @
     */
    public void testDirAtoBNoAnnot() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, null, "A", "B", "true");
        TFM02 tfm02 = new TFM02();
        List objLinkTempSGINSTs = tfm02.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                null, null, new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"7", "4", "A"});
        expObjTempSGINST.insertRow(new String[]{"8", "4", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"3", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"8", "4", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "4", "Y"});
        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Brings in only subraph 1 (4 doesn't match the annotations)
     *
     * @
     */
    public void testDirAtoBAnnotated() {
        // prepare query
        // edge: Y [2..]
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A", "B", "true");
        TFM02 tfm02 = new TFM02();
        List objLinkTempSGINSTs = tfm02.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                null, null, new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Only subgraph 2 matches
     *
     * @
     */
    public void testDirBtoA() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, 2);     // [2..2]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "B", "A", "true");
        TFM02 tfm02 = new TFM02();
        List objLinkTempSGINSTs = tfm02.execTFMExecInternal(qgUtil,
                qgEdgeY, "B", "A",
                null, null, new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"3", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"4", "2", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "Y"});
        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Only subgraph 5 matches, but some Bs need to be removed
     *
     * @
     */
    public void testRemoveExtraBs() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(3, 3);     // [3,3]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "B", "A", "true");
        TFM02 tfm02 = new TFM02();
        List objLinkTempSGINSTs = tfm02.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                null, null, new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"9", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"10", "5", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"10", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"13", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"14", "5", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Brings in only subraph 5
     *
     * @
     */
    public void testUnDirAnnotated() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(3, 3);     // [3..3]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A", "B", "false");
        TFM02 tfm02 = new TFM02();
        List objLinkTempSGINSTs = tfm02.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                null, null, new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"9", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"10", "5", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"10", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"13", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"14", "5", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }

}
