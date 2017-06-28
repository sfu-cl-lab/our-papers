/**
 * $Id: TFM09Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM09Test.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Tests TFM09. Since most of the code is shared by TFM02 and TFM09,
 * and TFM02Test already tests most of the shared functionality,
 * this class only tests the extended functionality provided by TFM09
 * (removing asterisks)
 *
 * @see TFM09
 */
public class TFM09Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM09Test.class);

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
        DB.insertObject(3);  // b (subg 1)
        DB.insertObject(4);  // a (subg 2)
        DB.insertObject(5);  // b (subg 2)
        DB.insertObject(6);  // b (subg 2)
        DB.insertObject(7);  // a (subg 3)
        DB.insertObject(8);  // a (subg 4)
        DB.insertObject(9);  // b (subg 4)

        attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST objAttr1DataNST = attrs.getAttrDataNST("attr1");
        objAttr1DataNST.insertRow(new String[]{"1", "A"});
        objAttr1DataNST.insertRow(new String[]{"2", "B"});
        objAttr1DataNST.insertRow(new String[]{"3", "B"});
        objAttr1DataNST.insertRow(new String[]{"4", "A"});
        objAttr1DataNST.insertRow(new String[]{"5", "B"});
        objAttr1DataNST.insertRow(new String[]{"6", "B"});
        objAttr1DataNST.insertRow(new String[]{"7", "A"});
        objAttr1DataNST.insertRow(new String[]{"8", "A"});
        objAttr1DataNST.insertRow(new String[]{"9", "B"});

        DB.insertLink(1, 1, 2);   // x (subg 1)
        DB.insertLink(2, 1, 3);   // x (subg 1)
        DB.insertLink(3, 1, 2);   // y (subg 1)
        DB.insertLink(4, 1, 3);   // y (subg 1)
        DB.insertLink(5, 4, 5);   // x (subg 2)
        DB.insertLink(6, 4, 6);   // x (subg 2)
        DB.insertLink(7, 4, 5);   // y (subg 2)
        DB.insertLink(8, 1, 1);   // x (subg 1) --self loop on A1
        DB.insertLink(9, 8, 9);   // x (subg 4)

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST linkAttr1DataNST = attrs.getAttrDataNST("attr1");
        linkAttr1DataNST.insertRow(new String[]{"1", "X"});
        linkAttr1DataNST.insertRow(new String[]{"2", "X"});
        linkAttr1DataNST.insertRow(new String[]{"3", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"4", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"5", "X"});
        linkAttr1DataNST.insertRow(new String[]{"6", "X"});
        linkAttr1DataNST.insertRow(new String[]{"7", "Y"});
        linkAttr1DataNST.insertRow(new String[]{"8", "X"});
        linkAttr1DataNST.insertRow(new String[]{"9", "X"});

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
        aObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        aObjTempSGINST.insertRow(new String[]{"1", "1", "B"}); // --1 is both A and B
        aObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        aObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        aObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        aObjTempSGINST.insertRow(new String[]{"7", "3", "A"});
        aObjTempSGINST.insertRow(new String[]{"8", "4", "A"});
        aObjTempSGINST.insertRow(new String[]{"9", "4", "B"});

        NST aLinkTempSGINST = makeTempSGINST();
        aLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"8", "1", "X"});
        aLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        aLinkTempSGINST.insertRow(new String[]{"9", "4", "X"});

        List aObjALinkTempSGINSTs = new ArrayList();
        aObjALinkTempSGINSTs.add(aObjTempSGINST);
        aObjALinkTempSGINSTs.add(aLinkTempSGINST);
        return aObjALinkTempSGINSTs;
    }


    private NST makeTempSGINST() {
        return SGIUtil.createTempSGINST();
    }


    /**
     * X,Y
     * A -----> B [2..]
     * Only subgraph 1 matches, because subgraph 2 doesn't have enough Bs
     * But, the B1 (equal to A1) is removed, because there is no Y self-loop for A1
     *
     * @
     */
    public void testRemoveExtraBs() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(1, -1);     // [1..], needed because vertex has annot
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A", "B", "true");
        Annotation bAsteriskedAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(2, -1));     // [2..])
        TFM09 tfm09 = new TFM09();
        List objLinkTempSGINSTs = tfm09.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                bAsteriskedAnnot, "B",
                new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);

        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


    /**
     * X,Y
     * A -----> B [0..]
     * Subgraph 1 matches, but the B1 (equal to A1) is removed, because there is no Y self-loop for A1
     * Subgraph 2 matches, but b6 disappears
     * Subgraph 3 matches
     * Subgraph 4 matches, but without b9
     *
     * @
     */
    public void testZeroAnnot() {
        // prepare query
        Element edgeCondEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element edgeAnnotEle = QGraphTestingUtil.makeAnnotationElement(1, -1);     // [1..], needed because vertex has annot
        QGEdge qgEdgeY = new QGEdge("Y", edgeCondEle, edgeAnnotEle, "A", "B", "true");
        Annotation bAsteriskedAnnot = new Annotation(QGraphTestingUtil.makeAnnotationElement(0, -1));     // [2..])
        TFM09 tfm09 = new TFM09();
        List objLinkTempSGINSTs = tfm09.execTFMExecInternal(qgUtil,
                qgEdgeY, "A", "B",
                bAsteriskedAnnot, "B",
                new String[]{"X"}, aConsolidatedObjTempSGINST, aConsolidatedLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- only the first subgraph matches
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"7", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"8", "4", "A"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);

        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }
}
