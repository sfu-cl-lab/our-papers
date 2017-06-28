/**
 * $Id: TFM18Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM18Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.Annotation;
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
 * Tests TFM18.
 *
 * @see TFM18
 */
public class TFM18Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM18Test.class);

    private QGUtil qgUtil;
    private Attributes attrs;

    private NST aObjTempSGINST;
    private NST bObjTempSGINST;
    private NST aLinkTempSGINST;
    private NST bLinkTempSGINST;


    /**
     * Sets up the input subgraphs. For the A vertex, we have three subgraphs, based
     * on a1, a4, and a16 each with a few corresponding Ds.
     * For the B vertex, we have fours subgraphs, based on b7, b11,  b14, and b18
     * with a few Cs each.
     * a1 is connected to b7  by three links   (11,12,13)
     * b11 by one   link    (14)
     * b14 by one   link    (15)
     * a4 is connected to b7  by one   link    (16)
     * b11 by four  links   (17,18,19, 20)
     * b18 by two   links   (21, 22)
     * a16 is not connected to any Bs
     * <p/>
     * Subgraph 4 has a4 connecte4d to b18, which in turn has a4 in its subgraph
     * <p/>
     * The other elements are
     * Ds   : d2, d3, d5, d6, d17
     * Cs   : c8, c9, c10, c12, c13, c15
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        qgUtil = new QGUtil(null);
        DB.beginScope();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        for (int objId = 1; objId < 18; objId++) {
            DB.insertObject(objId);
        }

        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 4, 5);
        DB.insertLink(4, 4, 6);
        DB.insertLink(5, 7, 8);
        DB.insertLink(6, 7, 9);
        DB.insertLink(7, 7, 10);
        DB.insertLink(8, 11, 12);
        DB.insertLink(9, 11, 13);
        DB.insertLink(10, 14, 15);
        DB.insertLink(11, 1, 7);
        DB.insertLink(12, 1, 7);
        DB.insertLink(13, 1, 7);
        DB.insertLink(14, 1, 11);
        DB.insertLink(15, 1, 14);
        DB.insertLink(16, 4, 7);
        DB.insertLink(17, 4, 11);
        DB.insertLink(18, 4, 11);
        DB.insertLink(19, 4, 11);
        DB.insertLink(20, 4, 11);
        DB.insertLink(21, 16, 17);
        DB.insertLink(22, 4, 18);
        DB.insertLink(23, 4, 18);
        DB.insertLink(24, 18, 4);

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST linkAttr1DataNST = attrs.getAttrDataNST("attr1");
        linkAttr1DataNST.insertRow(new String[]{"1", "X"});
        linkAttr1DataNST.insertRow(new String[]{"2", "X"});
        linkAttr1DataNST.insertRow(new String[]{"3", "X"});
        linkAttr1DataNST.insertRow(new String[]{"4", "X"});
        linkAttr1DataNST.insertRow(new String[]{"5", "X"});
        linkAttr1DataNST.insertRow(new String[]{"6", "X"});
        linkAttr1DataNST.insertRow(new String[]{"7", "X"});
        linkAttr1DataNST.insertRow(new String[]{"8", "X"});
        linkAttr1DataNST.insertRow(new String[]{"9", "X"});
        linkAttr1DataNST.insertRow(new String[]{"10", "X"});
        linkAttr1DataNST.insertRow(new String[]{"11", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"12", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"13", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"14", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"15", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"16", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"17", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"18", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"19", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"20", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"21", "X"});
        linkAttr1DataNST.insertRow(new String[]{"22", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"23", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"24", "X"});

        // create input
        List aObjBObjALinkBLinkTempSGINSTs = makeAObjBObjALinkBLinkTempSGINSTs();
        aObjTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(0);
        bObjTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(1);
        aLinkTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(2);
        bLinkTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(3);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getLinkAttrs().deleteAttribute("attr1");
        qgUtil.release();
        DB.endScope();
        TestUtil.closeTestConnection();
    }


    /**
     * Creates input subg tables
     */
    private List makeAObjBObjALinkBLinkTempSGINSTs() {
        NST aObjTempSGINST = makeTempSGINST();
        aObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        aObjTempSGINST.insertRow(new String[]{"2", "1", "D"});
        aObjTempSGINST.insertRow(new String[]{"3", "1", "D"});
        aObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        aObjTempSGINST.insertRow(new String[]{"5", "2", "D"});
        aObjTempSGINST.insertRow(new String[]{"6", "2", "D"});
        aObjTempSGINST.insertRow(new String[]{"16", "3", "A"});
        aObjTempSGINST.insertRow(new String[]{"17", "3", "D"});

        NST bObjTempSGINST = makeTempSGINST();
        bObjTempSGINST.insertRow(new String[]{"7", "1", "B"});
        bObjTempSGINST.insertRow(new String[]{"8", "1", "C"});
        bObjTempSGINST.insertRow(new String[]{"9", "1", "C"});
        bObjTempSGINST.insertRow(new String[]{"10", "1", "C"});
        bObjTempSGINST.insertRow(new String[]{"11", "2", "B"});
        bObjTempSGINST.insertRow(new String[]{"12", "2", "C"});
        bObjTempSGINST.insertRow(new String[]{"13", "2", "C"});
        bObjTempSGINST.insertRow(new String[]{"14", "3", "B"});
        bObjTempSGINST.insertRow(new String[]{"15", "3", "C"});
        bObjTempSGINST.insertRow(new String[]{"18", "4", "B"});
        bObjTempSGINST.insertRow(new String[]{"4", "4", "C"});

        NST aLinkTempSGINST = makeTempSGINST();
        aLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"3", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"21", "3", "X"});
        NST bLinkTempSGINST = makeTempSGINST();
        bLinkTempSGINST.insertRow(new String[]{"5", "1", "X"});
        bLinkTempSGINST.insertRow(new String[]{"6", "1", "X"});
        bLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});
        bLinkTempSGINST.insertRow(new String[]{"8", "2", "X"});
        bLinkTempSGINST.insertRow(new String[]{"9", "2", "X"});
        bLinkTempSGINST.insertRow(new String[]{"10", "3", "X"});
        bLinkTempSGINST.insertRow(new String[]{"24", "4", "X"});

        List aObjBObjALinkBLinkTempSGINSTs = new ArrayList();
        aObjBObjALinkBLinkTempSGINSTs.add(aObjTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(bObjTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(aLinkTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(bLinkTempSGINST);
        return aObjBObjALinkBLinkTempSGINSTs;
    }


    private NST makeTempSGINST() {
        return SGIUtil.createTempSGINST();
    }


    /**
     * Tests the query
     * A < ------- >  B
     * [1..]     [1..]
     * <p/>
     * Expected Results
     * ----------------
     * <p/>
     * Subg1: a1, b7, b11, b14 (and their connected elements)
     * Subg2: a4, b7, b11
     *
     * @
     */
    public void testEdgeNoMaxSQMinOne() {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(1, -1);     // [1..]
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        Annotation vertexAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));     // [1..]
        TFM18 tfm18 = new TFM18();
        List objLinkTempSGINSTs = tfm18.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B", vertexAnnot,
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"7", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"9", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"10", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"11", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"12", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"13", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"14", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"15", "1", "C"});

        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"9", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"11", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"12", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"13", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"18", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"6", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"10", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"11", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"13", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"14", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"15", "1", "Y"});

        expLinkTempSGINST.insertRow(new String[]{"3", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"16", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"17", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"18", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"19", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"20", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"22", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"23", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"24", "2", "X"});
        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Tests the query
     * A < --------- >  B
     * [1..2]     [1..]
     * <p/>
     * Expected Results
     * ----------------
     * <p/>
     * Subg1: a1, b11, b14 (and their connected elements)
     * Subg2: a4, b7
     *
     * @
     */
    public void testEdgeMaxTwoSQMinOne() {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(1, 2);     // [1..]
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        Annotation vertexAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));     // [1..]
        TFM18 tfm18 = new TFM18();
        List objLinkTempSGINSTs = tfm18.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B", vertexAnnot,
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"11", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"12", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"13", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"14", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"15", "1", "C"});

        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"9", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"18", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"10", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"14", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"15", "1", "Y"});

        expLinkTempSGINST.insertRow(new String[]{"3", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"16", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"22", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"23", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"24", "2", "X"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Tests the query
     * A < --------- >  B
     * [1..1]     [2..]
     * <p/>
     * Expected Results
     * ----------------
     * <p/>
     * Subg1: a1, b11, b14 (and their connected elements)
     *
     * @
     */
    public void testEdgeMaxOneSQMinTwo() {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(1, 1);
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        Annotation vertexAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(2, -1));
        TFM18 tfm18 = new TFM18();
        List objLinkTempSGINSTs = tfm18.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B", vertexAnnot,
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"11", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"12", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"13", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"14", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"15", "1", "C"});
        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"10", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"14", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"15", "1", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Tests the query
     * A < --------- >  B
     * [2..2]     [1..]
     * <p/>
     * Expected Results
     * ----------------
     * <p/>
     * Subg1: a4, b18 (and their connected elements)
     * a4 should appear twice, once as A, once as C
     *
     * @
     */
    public void testEdgeMinTwoMaxTwoSQMinOne() {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(2, 2);
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        Annotation vertexAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));
        TFM18 tfm18 = new TFM18();
        List objLinkTempSGINSTs = tfm18.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B", vertexAnnot,
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"4", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"6", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"18", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "1", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"22", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"23", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"24", "1", "X"});
        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * Tests the query
     * A < ------- >  B
     * [4..]     [0..]
     * <p/>
     * Expected Results
     * ----------------
     * <p/>
     * Subg1: a1 (and its connected elements)
     * Subg2: a4, b11
     * Subg3: a16 (and its connected elements)
     *
     * @
     */
    public void testEdgeMinFourSQMinZero() {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(4, -1);
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        Annotation vertexAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(0, -1));
        TFM18 tfm18 = new TFM18();
        List objLinkTempSGINSTs = tfm18.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B", vertexAnnot,
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "D"});
        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "D"});
        expObjTempSGINST.insertRow(new String[]{"11", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"12", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"13", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"16", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"17", "3", "D"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"3", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"9", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"17", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"18", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"19", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"20", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"21", "3", "X"});
        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }

}
