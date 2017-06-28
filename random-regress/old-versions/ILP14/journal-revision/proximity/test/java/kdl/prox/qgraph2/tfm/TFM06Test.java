/**
 * $Id: TFM06Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM06Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.QGraphTestingUtil;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;


/**
 * Tests TFM06.
 *
 * @see TFM06
 */
public class TFM06Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM06Test.class);

    private QGUtil qgUtil;
    private QGUtil qgUtilOnSource;
    private Attributes attrs;
    Container sourceContainer;

    private NST aConsolidatedObjTempSGINST;
    private NST aConsolidatedLinkTempSGINST;


    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        DB.beginScope();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        DB.insertObject(1);  // A (subg 1)
        DB.insertObject(2);  // C (subg 1)
        DB.insertObject(3);  // B (subg 1)
        DB.insertObject(4);  // B (subg 1)
        DB.insertObject(5);  // C (subg 2)
        DB.insertObject(6);  // B (subg 2)
        DB.insertObject(7);  // A (subg 2)
        DB.insertObject(8);  // B (subg 2)
        DB.insertObject(9);  // A (subg 3)
        DB.insertObject(10); // B (subg 3)
        DB.insertObject(11); // B (subg 3)
        DB.insertObject(12); // A (subg 4)
        DB.insertObject(13); // C (subg 4)
        DB.insertObject(14); // B (subg 4)

        /*
        subg1: A1, C2   ... B3, B4, B8
        subg2: A7, C5   ... B6
        subg3: A9       ... B10, B11
        subg4: A12, C13 ... B14
        subg5: A15, C16
        */
        attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST objAttr1DataNST = attrs.getAttrDataNST("attr1");
        objAttr1DataNST.insertRow(new String[]{"1", "A"});
        objAttr1DataNST.insertRow(new String[]{"2", "C"});
        objAttr1DataNST.insertRow(new String[]{"3", "B"});
        objAttr1DataNST.insertRow(new String[]{"4", "B"});
        objAttr1DataNST.insertRow(new String[]{"5", "C"});
        objAttr1DataNST.insertRow(new String[]{"6", "B"});
        objAttr1DataNST.insertRow(new String[]{"7", "A"});
        objAttr1DataNST.insertRow(new String[]{"8", "B"});
        objAttr1DataNST.insertRow(new String[]{"9", "A"});
        objAttr1DataNST.insertRow(new String[]{"10", "B"});
        objAttr1DataNST.insertRow(new String[]{"11", "B"});
        objAttr1DataNST.insertRow(new String[]{"12", "A"});
        objAttr1DataNST.insertRow(new String[]{"13", "C"});
        objAttr1DataNST.insertRow(new String[]{"14", "B"});
        objAttr1DataNST.insertRow(new String[]{"15", "A"});
        objAttr1DataNST.insertRow(new String[]{"16", "C"});

        DB.insertLink(1, 1, 2);   // A1  to C2  (subg 1)
        DB.insertLink(2, 1, 3);   // A1  to B3
        DB.insertLink(3, 1, 3);   // A1  to B3
        DB.insertLink(4, 1, 3);   // A1  to B3
        DB.insertLink(5, 1, 4);   // A1  to B4
        DB.insertLink(6, 1, 4);   // A1  to B4
        DB.insertLink(7, 7, 6);   // A7  to B6   (subg 2)
        DB.insertLink(8, 7, 6);   // A7  to B6   (subg 2)
        DB.insertLink(9, 7, 6);   // A7  to B6   (subg 2)
        DB.insertLink(10, 7, 5);  // A7  to C5   (subg 2)
        DB.insertLink(11, 1, 8);  // A1  to B8   (subg 1?)
        DB.insertLink(12, 10, 9);   // B10 to A9   (subg 3)
        DB.insertLink(13, 10, 9);   // B10 to A9   (subg 3)
        DB.insertLink(14, 11, 9);   // B11 to A9   (subg 3)
        DB.insertLink(15, 11, 9);   // B11 to A9   (subg 3)
        DB.insertLink(16, 12, 13);  // A12 to C13  (subg 4)
        DB.insertLink(17, 14, 12);  // B14 to A12  (subg 4)
        DB.insertLink(18, 12, 14);  // A12 to B14  (subg 4)
        DB.insertLink(19, 12, 14);  // A12 to B14  (subg 4)
        DB.insertLink(20, 14, 12);  // B14 to A12  (subg 4)
        DB.insertLink(21, 14, 12);  // A15 to C15  (subg 5)

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST linkAttr1DataNST = attrs.getAttrDataNST("attr1");
        linkAttr1DataNST.insertRow(new String[]{"1", "X"});
        linkAttr1DataNST.insertRow(new String[]{"2", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"3", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"4", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"5", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"6", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"7", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"8", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"9", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"10", "X"});
        linkAttr1DataNST.insertRow(new String[]{"11", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"12", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"13", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"14", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"15", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"16", "X"});
        linkAttr1DataNST.insertRow(new String[]{"17", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"18", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"19", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"20", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"21", "X"});
        // create input
        List aObjBObjALinkBLinkTempSGINSTs = makeAObjALinkTempSGINSTs();
        aConsolidatedObjTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(0);
        aConsolidatedLinkTempSGINST = (NST) aObjBObjALinkBLinkTempSGINSTs.get(1);

        DB.getRootContainer().deleteAllChildren();
        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "2", "B"});
        objectNST.insertRow(new String[]{"3", "3", "C"});
        objectNST.insertRow(new String[]{"4", "3", "D"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "Z"});
        linkNST.insertRow(new String[]{"2", "1", "Z"});
        linkNST.insertRow(new String[]{"3", "1", "Z"});
        linkNST.insertRow(new String[]{"5", "1", "Z"});
        linkNST.insertRow(new String[]{"6", "1", "Z"});

        qgUtil = new QGUtil(null);
        qgUtilOnSource = new QGUtil(sourceContainer);

    }


    protected void tearDown() throws Exception {
        super.tearDown();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAttribute("attr1");
        DB.getLinkAttrs().deleteAttribute("attr1");
        qgUtil.release();
        qgUtilOnSource.release();
        DB.endScope();
        TestUtil.closeTestConnection();
    }


    /**
     * Three subgraphs:
     * <p/>
     * 1. With A connected to a C, links to two Bs with [2..] links -- match
     * 2. With A connected to a C, links to a single B with [2..] links - no match
     * 3. With a A, links from B to A with [2..] links - match reverse
     *
     * @return
     * @
     */
    private List makeAObjALinkTempSGINSTs() {
        NST aObjTempSGINST = makeTempSGINST();
        aObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        aObjTempSGINST.insertRow(new String[]{"2", "1", "C"});
        aObjTempSGINST.insertRow(new String[]{"7", "2", "A"});
        aObjTempSGINST.insertRow(new String[]{"5", "2", "C"});
        aObjTempSGINST.insertRow(new String[]{"9", "3", "A"});
        aObjTempSGINST.insertRow(new String[]{"12", "4", "A"});
        aObjTempSGINST.insertRow(new String[]{"13", "4", "C"});
        aObjTempSGINST.insertRow(new String[]{"15", "5", "A"});
        aObjTempSGINST.insertRow(new String[]{"16", "5", "C"});

        NST aLinkTempSGINST = makeTempSGINST();
        aLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"10", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"16", "4", "X"});
        aLinkTempSGINST.insertRow(new String[]{"21", "5", "X"});
        List aObjALinkTempSGINSTs = new ArrayList();
        aObjALinkTempSGINSTs.add(aObjTempSGINST);
        aObjALinkTempSGINSTs.add(aLinkTempSGINST);
        return aObjALinkTempSGINSTs;
    }


    private NST makeTempSGINST() {
        return SGIUtil.createTempSGINST();
    }


    public void testDirAtoB() {
        // prepare query
        // edge: Y [2..]
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A",
                "B", "true");
        // vertex B [2..]
        Element vertexCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "B");
        Element vertexAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGVertex qgVertexB = new QGVertex("B", vertexCondEle, vertexAnnotEle);
        TFM06 tfm06 = new TFM06();
        List objLinkTempSGINSTs = tfm06.execTFMExecInternal(qgUtil,
                "A", qgEdgeY, qgVertexB,
                aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "1", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"5", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"6", "1", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }

    /**
     * Just like testDirAtoB, but Y links 4 is not there
     *
     * @
     */
    public void testDirAtoBOnSource() {
        // prepare query
        // edge: Y [2..]
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A",
                "B", "true");
        // vertex B [2..]
        Element vertexCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "B");
        Element vertexAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGVertex qgVertexB = new QGVertex("B", vertexCondEle, vertexAnnotEle);
        TFM06 tfm06 = new TFM06();
        List objLinkTempSGINSTs = tfm06.execTFMExecInternal(qgUtilOnSource,
                "A", qgEdgeY, qgVertexB,
                aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "1", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"5", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"6", "1", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    public void testDirBtoA() {
        // prepare query
        // edge: Y [2..]
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "B", "A", "true");
        // vertex B [2..]
        Element vertexCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "B");
        Element vertexAnnotEle = QGraphTestingUtil.makeAnnotationElement(2, -1);     // [2..]
        QGVertex qgVertexB = new QGVertex("B", vertexCondEle, vertexAnnotEle);
        TFM06 tfm06 = new TFM06();
        List objLinkTempSGINSTs = tfm06.execTFMExecInternal(qgUtil,
                "A", qgEdgeY, qgVertexB,
                aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"9", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"10", "3", "B"});
        expObjTempSGINST.insertRow(new String[]{"11", "3", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"12", "3", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"13", "3", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"14", "3", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"15", "3", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    public void testUndirected() {
        // prepare query
        // edge: Y [4..4], undirected!
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(4, 4);     // [4..4]
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A", "B", "false");
        // vertex B [1..1]
        Element vertexCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "B");
        Element vertexAnnotEle = QGraphTestingUtil.makeAnnotationElement(1, 1);     // [1..1]
        QGVertex qgVertexB = new QGVertex("B", vertexCondEle, vertexAnnotEle);
        TFM06 tfm06 = new TFM06();
        List objLinkTempSGINSTs = tfm06.execTFMExecInternal(qgUtil,
                "A", qgEdgeY, qgVertexB,
                aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"12", "4", "A"});
        expObjTempSGINST.insertRow(new String[]{"13", "4", "C"});
        expObjTempSGINST.insertRow(new String[]{"14", "4", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"16", "4", "X"});
        expLinkTempSGINST.insertRow(new String[]{"17", "4", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"18", "4", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"19", "4", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"20", "4", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    public void testZeroAnnot() {
        // prepare query
        // edge: Y [3..]
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(3, -1);
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A",
                "B", "true");
        // vertex B [0..]
        Element vertexCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "B");
        Element vertexAnnotEle = QGraphTestingUtil.makeAnnotationElement(0, -1);     // [2..]
        QGVertex qgVertexB = new QGVertex("B", vertexCondEle, vertexAnnotEle);
        TFM06 tfm06 = new TFM06();
        List objLinkTempSGINSTs = tfm06.execTFMExecInternal(qgUtil,
                "A", qgEdgeY, qgVertexB,
                aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"9", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"12", "4", "A"});
        expObjTempSGINST.insertRow(new String[]{"13", "4", "C"});
        expObjTempSGINST.insertRow(new String[]{"15", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"16", "5", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"10", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"9", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"16", "4", "X"});
        expLinkTempSGINST.insertRow(new String[]{"21", "5", "X"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


}
