/**
 * $Id: TFM12Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM12Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.List;
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


/**
 * Tests TFM12.
 *
 * @see TFM12
 */
public class TFM12Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM12Test.class);

    private QGUtil qgUtil;
    private Attributes attrs;

    private NST aObjTempSGINST;
    private NST bObjTempSGINST;
    private NST aLinkTempSGINST;
    private NST bLinkTempSGINST;


    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        qgUtil = new QGUtil(null);

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        DB.insertObject(1);  // A (subg 1)
        DB.insertObject(3);  // B (subg 2)
        DB.insertObject(4);  // B (subg 3)
        DB.insertObject(5);  // C (subg 1)
        DB.insertObject(6);  // C (subg 3)

        DB.insertLink(1, 1, 3);
        DB.insertLink(2, 3, 1);
        DB.insertLink(3, 1, 3);
        DB.insertLink(4, 4, 1);
        DB.insertLink(7, 5, 1);
        DB.insertLink(8, 4, 6);

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute("attr1", "str");
        NST attr1DataNST = attrs.getAttrDataNST("attr1");
        attr1DataNST.insertRow(new String[]{"1", "Y"});
        attr1DataNST.insertRow(new String[]{"2", "Y"});
        attr1DataNST.insertRow(new String[]{"3", "Y"});
        attr1DataNST.insertRow(new String[]{"4", "Y"});
        attr1DataNST.insertRow(new String[]{"7", "X"});
        attr1DataNST.insertRow(new String[]{"8", "X"});
        attr1DataNST.release();

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
        attrs.deleteAttribute("attr1");

        aObjTempSGINST.release();
        bObjTempSGINST.release();
        aLinkTempSGINST.release();
        bLinkTempSGINST.release();

        qgUtil.release();
        TestUtil.closeTestConnection();
    }


    private List makeAObjBObjALinkBLinkTempSGINSTs()  {
        NST aObjTempSGINST = makeTempSGINST();
        aObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        aObjTempSGINST.insertRow(new String[]{"5", "1", "C"});

        NST bObjTempSGINST = makeTempSGINST();
        bObjTempSGINST.insertRow(new String[]{"3", "2", "B"});

        bObjTempSGINST.insertRow(new String[]{"4", "3", "B"});
        bObjTempSGINST.insertRow(new String[]{"6", "3", "C"});

        NST aLinkTempSGINST = makeTempSGINST();
        aLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});

        NST bLinkTempSGINST = makeTempSGINST();
        bLinkTempSGINST.insertRow(new String[]{"8", "3", "X"});


        List aObjBObjALinkBLinkTempSGINSTs = new ArrayList();
        aObjBObjALinkBLinkTempSGINSTs.add(aObjTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(bObjTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(aLinkTempSGINST);
        aObjBObjALinkBLinkTempSGINSTs.add(bLinkTempSGINST);
        return aObjBObjALinkBLinkTempSGINSTs;
    }


    private NST makeTempSGINST()  {
        return SGIUtil.createTempSGINST();
    }


    public void testDirectedAnnot()  {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(1, -1);     // [1..]
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "A", "B", "true");
        TFM12 tfm12 = new TFM12();
        List objLinkTempSGINSTs = tfm12.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B",
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"5", "1", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});

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


    public void testDirectedUnAnnot()  {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        QGEdge qgEdgeY = new QGEdge("Y", condEle, null, "A", "B", "true");
        TFM12 tfm12 = new TFM12();
        List objLinkTempSGINSTs = tfm12.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B",
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"5", "1", "C"});

        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"3", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});

        expLinkTempSGINST.insertRow(new String[]{"3", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "X"});

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
     * Should join SG 1 and SG2 .. SG3 is dropped because there aren't enough links
     *
     * @
     */
    public void testUnDirectedAnnot()  {
        // get results
        Element condEle = QGraphTestingUtil.makeConditionElement("attr1", "eq", "Y");
        Element annotEle = QGraphTestingUtil.makeAnnotationElement(3, 3);     // [3,3]
        QGEdge qgEdgeY = new QGEdge("Y", condEle, annotEle, "vertex1Name",
                "vertex2Name", "false");
        TFM12 tfm12 = new TFM12();
        List objLinkTempSGINSTs = tfm12.execTFMExecInternal(qgUtil, qgEdgeY,
                "A", "B",
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"5", "1", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "X"});

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
