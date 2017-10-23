/**
 * $Id: TFM22Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM22Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGraphTestingUtil;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;


/**
 * Tests TFM21.
 *
 * @see TFM21
 */
public class TFM22Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM22Test.class);

    private QGUtil qgUtil;

    private NST actObjTempSGINST;
    private NST actLinkTempSGINST;

    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        qgUtil = new QGUtil(null);

        // create some objects, links, and their attributes
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        DB.insertObject(1);   // a (subg 1)
        DB.insertObject(2);   // b (subg 1)
        DB.insertObject(3);   // c (subg 1)
        DB.insertObject(4);   // a (subg 2)
        DB.insertObject(5);   // b (subg 2)
        DB.insertObject(6);   // c (subg 2)
        DB.insertObject(7);   // c (subg 3)
        DB.insertObject(8);   // c (subg 3)
        DB.insertObject(9);   // c (subg 3)

        DB.insertLink(1, 1, 2); // a1  b2   (subg 1)
        DB.insertLink(2, 2, 3); // a2  c3   (subg 1)
        DB.insertLink(3, 2, 3); // a2  c3   (subg 1) // income not defined
        DB.insertLink(4, 4, 5); // a4  b5   (subg 2)
        DB.insertLink(5, 5, 6); // b5  c6   (subg 2)
        DB.insertLink(6, 5, 6); // b5  c6   (subg 2)
        DB.insertLink(7, 5, 6); // b5  c6   (subg 2)
        DB.insertLink(8, 7, 8); // a7  b8   (subg 3)
        DB.insertLink(9, 8, 9); // b8  c9   (subg 3)
        DB.insertLink(10, 8, 9); // b8  c9   (subg 3)
        DB.insertLink(11, 8, 10); // b8  c10   (subg 3)
        DB.insertLink(12, 8, 10); // b8  c10   (subg 3)
        DB.insertLink(13, 8, 10); // b8  c10   (subg 3)

        Attributes attrs = DB.getLinkAttrs();
        attrs.defineAttribute("income", "int");
        NST linkAttr1DataNST = attrs.getAttrDataNST("income");
        linkAttr1DataNST.insertRow(new String[]{"1", "10"});
        linkAttr1DataNST.insertRow(new String[]{"2", "5"});
        linkAttr1DataNST.insertRow(new String[]{"4", "10"});
        linkAttr1DataNST.insertRow(new String[]{"5", "5"});
        linkAttr1DataNST.insertRow(new String[]{"6", "6"});
        linkAttr1DataNST.insertRow(new String[]{"7", "12"});
        linkAttr1DataNST.insertRow(new String[]{"8", "10"}); //all for subg 3
        linkAttr1DataNST.insertRow(new String[]{"9", "5"});
        linkAttr1DataNST.insertRow(new String[]{"10", "12"});
        linkAttr1DataNST.insertRow(new String[]{"11", "5"});
        linkAttr1DataNST.insertRow(new String[]{"12", "7"});
        linkAttr1DataNST.insertRow(new String[]{"13", "12"});
        linkAttr1DataNST.release();

        // create input
        List tempSGINSTs = makeAObjALinkTempSGINSTs();
        actObjTempSGINST = (NST) tempSGINSTs.get(0);
        actLinkTempSGINST = (NST) tempSGINSTs.get(1);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getLinkAttrs().deleteAllAttributes();
        qgUtil.release();
        TestUtil.closeTestConnection();
    }


    /**
     * Prepare the data
     * The SGI table has three subgraphs:
     * - subgraph 1 has A2 with id >= ids of other b1, <= ids of b3, b4
     * - subgraph 2 has A8 with id >= that all Bs
     * - subgraph 3 has A11 with id <= than B11
     * - subgraph 4 only has an A [only works when annot is 0..]
     *
     * @return
     * @throws kdl.prox.monet.MonetException
     */
    private List makeAObjALinkTempSGINSTs()  {
        NST objTempSGINST = makeTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"3", "1", "C"});
        objTempSGINST.insertRow(new String[]{"4", "2", "A"});
        objTempSGINST.insertRow(new String[]{"5", "2", "B"});
        objTempSGINST.insertRow(new String[]{"6", "2", "C"});

        NST linkTempSGINST = makeTempSGINST();
        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "1", "Y"});
        linkTempSGINST.insertRow(new String[]{"3", "1", "Y"});
        linkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        linkTempSGINST.insertRow(new String[]{"5", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"6", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"7", "2", "Y"});

        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(objTempSGINST);
        tempSGINSTs.add(linkTempSGINST);
        return tempSGINSTs;
    }


    private NST makeTempSGINST()  {
        return SGIUtil.createTempSGINST();
    }

    /**
     * Subg 2 stays completely; subg 1 disappears.
     * An annotation [2..], so that subg 1 is removed
     *
     * @throws kdl.prox.monet.MonetException
     */
    public void testRemovesWholeSubgraphs()  {
        TFM22 tfm22 = new TFM22();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge",
                "X", "income", "Y", "income");
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(2, -1));     // [2..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "Y", annot, true);
        List objLinkTempSGINSTs = tfm22.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, false, new String[]{"C"}); // C is the end annotated vertex
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay, but without y7
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);

        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        // the test above doesn't check subgraph ids
        // finally, make sure that the subg_id hasn't changed (i.e, it's still 2)
        ResultSet resultSet = actObjTempSGINST.selectRows("*", "subg_id", "*");
        while (resultSet.next()) {
            assertEquals(2, resultSet.getOID(1));
        }

        actObjTempSGINST.release();
    }

    /**
     * As testRemovesWhileSubgraphs, but add subgraph 3, in which one of the Cs
     * disappears but not the other
     *
     * @throws kdl.prox.monet.MonetException
     */
    public void testRemovesPartialSubgraphsWithGT()  {
        actObjTempSGINST.insertRow(new String[]{"7", "3", "A"});
        actObjTempSGINST.insertRow(new String[]{"8", "3", "B"});
        actObjTempSGINST.insertRow(new String[]{"9", "3", "C"});
        actObjTempSGINST.insertRow(new String[]{"10", "3", "C"});

        actLinkTempSGINST.insertRow(new String[]{"8", "3", "X"});
        actLinkTempSGINST.insertRow(new String[]{"9", "3", "Y"});
        actLinkTempSGINST.insertRow(new String[]{"10", "3", "Y"});
        actLinkTempSGINST.insertRow(new String[]{"11", "3", "Y"});
        actLinkTempSGINST.insertRow(new String[]{"12", "3", "Y"});
        actLinkTempSGINST.insertRow(new String[]{"13", "3", "Y"});

        TFM22 tfm22 = new TFM22();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("gt",
                "X", "income", "Y", "income");
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(2, -1));     // [2..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "Y", annot, true);
        List objLinkTempSGINSTs = tfm22.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, false, new String[]{"C"}); // C is the end annotated vertex
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay, but without y7
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"4", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"7", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"8", "3", "B"});
        expObjTempSGINST.insertRow(new String[]{"10", "3", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"8", "3", "X"});
        expLinkTempSGINST.insertRow(new String[]{"11", "3", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "3", "Y"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

    }
}
