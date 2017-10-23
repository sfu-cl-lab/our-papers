/**
 * $Id: TFM21Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM21Test.java 3658 2007-10-15 16:29:11Z schapira $
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
public class TFM21Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM21Test.class);

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

        DB.insertObject(1);   // B (subg 1)
        DB.insertObject(2);   // A (subg 1)
        DB.insertObject(3);   // B (subg 1)
        DB.insertObject(4);   // B (subg 1)
        DB.insertObject(5);   // B (subg 2)
        DB.insertObject(6);   // B (subg 2)
        DB.insertObject(7);   // B (subg 2)
        DB.insertObject(8);   // A (subg 2)
        DB.insertObject(9);   // C (subg 1)
        DB.insertObject(10);  // C (subg 2)
        DB.insertObject(11);  // A (subg 3)
        DB.insertObject(12);  // B (subg 3)
        DB.insertObject(13);  // C (subg 3)
        DB.insertObject(14);  // A (subg 4)
        DB.insertObject(15);  // B (subg 5)
        DB.insertObject(16);  // B (subg 5)
        DB.insertObject(17);  // A (subg 5) and C (subg 5)
        DB.insertObject(18);  // C (subg 5)
        DB.insertObject(19);  // C (subg 5)

        DB.insertLink(1, 2, 1); // a2  b1   (subg 1)
        DB.insertLink(2, 2, 3); // a2  b3   (subg 1)
        DB.insertLink(3, 2, 4); // a2  b4   (subg 1)
        DB.insertLink(4, 8, 5); // a8  b5   (subg 2)
        DB.insertLink(5, 8, 6); // a8  b6   (subg 2)
        DB.insertLink(6, 8, 7); // a8  b7   (subg 2)
        DB.insertLink(7, 2, 9); // a2  c9   (subg 1)
        DB.insertLink(8, 8, 10); // a8  c10  (subg 2)
        DB.insertLink(9, 11, 13); // a11 c13  (subg 3)
        DB.insertLink(10, 11, 12); // a11 b12  (subg 3)
        DB.insertLink(11, 17, 15); // a17 b16  (subg 5)
        DB.insertLink(12, 17, 16); // a17 b16  (subg 5) both Y and X in 5
        DB.insertLink(13, 15, 18); // b15 c18  (subg 5)
        DB.insertLink(14, 16, 19); // b16 c19  (subg 5)
        DB.insertLink(15, 18, 18); // c18 c18  (subg 5)

        Attributes attrs = DB.getObjectAttrs();
        attrs.defineAttribute("income", "int");
        NST objAttr1DataNST = attrs.getAttrDataNST("income");
        objAttr1DataNST.insertRow(new String[]{"2", "10"});
        objAttr1DataNST.insertRow(new String[]{"8", "20"});
        objAttr1DataNST.release();

        attrs.defineAttribute("salary", "int");
        objAttr1DataNST = attrs.getAttrDataNST("salary");
        objAttr1DataNST.insertRow(new String[]{"1", "20"});
        objAttr1DataNST.insertRow(new String[]{"3", "15"});
        objAttr1DataNST.insertRow(new String[]{"4", "6"});
        objAttr1DataNST.insertRow(new String[]{"5", "5"});
        objAttr1DataNST.insertRow(new String[]{"6", "10"});
        objAttr1DataNST.insertRow(new String[]{"7", "25"});
        objAttr1DataNST.insertRow(new String[]{"17", "25"});
        objAttr1DataNST.insertRow(new String[]{"18", "35"});
        objAttr1DataNST.insertRow(new String[]{"19", "20"});
        objAttr1DataNST.release();

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
        DB.getObjectAttrs().deleteAttribute("income");
        DB.getObjectAttrs().deleteAttribute("salary");
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
     * @
     */
    private List makeAObjALinkTempSGINSTs()  {
        NST objTempSGINST = makeTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "B"});
        objTempSGINST.insertRow(new String[]{"2", "1", "A"});
        objTempSGINST.insertRow(new String[]{"3", "1", "B"});
        objTempSGINST.insertRow(new String[]{"4", "1", "B"});
        objTempSGINST.insertRow(new String[]{"9", "1", "C"});
        objTempSGINST.insertRow(new String[]{"5", "2", "B"});
        objTempSGINST.insertRow(new String[]{"6", "2", "B"});
        objTempSGINST.insertRow(new String[]{"7", "2", "B"});
        objTempSGINST.insertRow(new String[]{"8", "2", "A"});
        objTempSGINST.insertRow(new String[]{"10", "2", "C"});
        objTempSGINST.insertRow(new String[]{"11", "3", "A"});
        objTempSGINST.insertRow(new String[]{"12", "3", "B"});
        objTempSGINST.insertRow(new String[]{"13", "3", "C"});
        objTempSGINST.insertRow(new String[]{"14", "4", "A"});
        objTempSGINST.insertRow(new String[]{"15", "5", "B"});
        objTempSGINST.insertRow(new String[]{"16", "5", "B"});
        objTempSGINST.insertRow(new String[]{"17", "5", "A"});
        objTempSGINST.insertRow(new String[]{"18", "5", "C"});
        objTempSGINST.insertRow(new String[]{"19", "5", "C"});
        objTempSGINST.insertRow(new String[]{"17", "5", "C"}); //17 is both A and C!

        NST linkTempSGINST = makeTempSGINST();
        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"3", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"7", "1", "Y"});
        linkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        linkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        linkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        linkTempSGINST.insertRow(new String[]{"8", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"9", "3", "Y"});
        linkTempSGINST.insertRow(new String[]{"10", "3", "X"});
        linkTempSGINST.insertRow(new String[]{"11", "5", "Y"});
        linkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        linkTempSGINST.insertRow(new String[]{"12", "5", "X"}); //both Y and X!
        linkTempSGINST.insertRow(new String[]{"13", "5", "X"});
        linkTempSGINST.insertRow(new String[]{"14", "5", "X"});
        linkTempSGINST.insertRow(new String[]{"15", "5", "Z"}); //self-link from-to 18

        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(objTempSGINST);
        tempSGINSTs.add(linkTempSGINST);
        return tempSGINSTs;
    }


    private NST makeTempSGINST()  {
        return SGIUtil.createTempSGINST();
    }


    /**
     * Comapres the income to the salary (just two made-up names)
     * <p/>
     * Subg   id  type  income  salary   (A.income GE b.salary?)
     * ---------------------------------------------------------
     * 1      1      B      --      20   false
     * 1      2      A      10      --   --
     * 1      3      B      --      15   false
     * 1      4      B      --       6   true
     * <p/>
     * 2      5      B      --       5   true
     * 2      6      B      --      10   true
     * 2      7      B      --      25   false
     * 2      8      A      20      --   --
     * <p/>
     * 3 ---> no data. removed
     *
     * @
     */
    public void testAttrsGE()  {
        TFM21 tfm21 = new TFM21();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge",
                "A", "income", "B", "salary");
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));     // [1..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "B", annot, false);
        List objLinkTempSGINSTs = tfm21.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{"X"}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results -- see above for expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"2", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"4", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"9", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "Y"});

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


    /**
     * Subg 2 stays completely; subg 1 keeps a2, x1, b1 y7 c9
     * Subg 5 should also stay (we can't make comparisons on Bs that have Depending Cs
     *
     * @
     */
    public void testIdsGE()  {
        TFM21 tfm21 = new TFM21();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge",
                "A", null, "B", null);
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));     // [1..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "B", annot, false);
        List objLinkTempSGINSTs = tfm21.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{"X"}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"2", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"1", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"9", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"15", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"16", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"17", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"17", "5", "C"});
        expObjTempSGINST.insertRow(new String[]{"18", "5", "C"});
        expObjTempSGINST.insertRow(new String[]{"19", "5", "C"});
        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"11", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"13", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"14", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"15", "5", "Z"});
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


    /**
     * Subg 2 stays completely; subg 1 keeps a2, x1, b1 y7 c9; subg 3 only keeps a11 and c13
     * Subg 4 should be brought in, since there are no Bs in it, and therefore no comparison can be made
     * Subg 5 should also stay (we can't make comparisons on Bs that have Depending Cs
     *
     * @
     */
    public void testIdsGEZeroAnnot()  {
        TFM21 tfm21 = new TFM21();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge", "A", null, "B", null);
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(0, -1));     // [0..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "B", annot, false);
        List objLinkTempSGINSTs = tfm21.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{"X"}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"2", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"1", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"9", "1", "C"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});
        expObjTempSGINST.insertRow(new String[]{"11", "3", "A"});
        expObjTempSGINST.insertRow(new String[]{"13", "3", "C"});
        expObjTempSGINST.insertRow(new String[]{"14", "4", "A"});
        expObjTempSGINST.insertRow(new String[]{"15", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"16", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"17", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"17", "5", "C"});
        expObjTempSGINST.insertRow(new String[]{"18", "5", "C"});
        expObjTempSGINST.insertRow(new String[]{"19", "5", "C"});
        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"7", "1", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"9", "3", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"11", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"13", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"14", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"15", "5", "Z"});
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


    /**
     * Subg 2 stays completely; subg 1 disappears.
     * Same as testAttrsGE, but in this case with an annotation [2..], so that subg 1 is removed
     *
     * @
     */
    public void testRemovesSubgraphs()  {
        TFM21 tfm21 = new TFM21();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge",
                "A", "income", "B", "salary");
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(2, -1));     // [2..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "B", annot, false);
        List objLinkTempSGINSTs = tfm21.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{"X"}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay
        // this is both in the object and link tables
        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"5", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"10", "2", "C"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"4", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "X"});
        expLinkTempSGINST.insertRow(new String[]{"8", "2", "Y"});

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
     * Tests relationship between As and Cs (which are assumed to be marked as [0..]
     * When the Cs disappear, the links that connect to them should also disappear.
     * This is in particular true of some links that play two differnet roles in a subgraph, as in
     * a1 -- x1 -- b1 -- x1 -- c1
     * <p/>
     * If the c1 disappears, so should the second x1, but not the first.
     * <p/>
     * Compare using attributes, so only subgraph 5 is tested
     * C18 should stay, and with it link 13 and 15
     * C19 should stay, and with it link 14
     * C17, which is actually the same as A17, should go away, and with it link X12 (but not A17, nor link Y12)
     * A17 and B15 and B16 stay.
     * <p/>
     * Test with [1..], so that other subgraphs disappear.
     */
    public void testSubgraphs() {
        TFM21 tfm21 = new TFM21();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ne", "A", "salary", "C", "salary");
        Annotation annot = new Annotation(QGraphTestingUtil.makeAnnotationElement(1, -1));     // [1..]
        QGConstraint qgConstraint = new QGConstraint(condTestEle, "C", annot, false);
        List objLinkTempSGINSTs = tfm21.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{"X"}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"15", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"16", "5", "B"});
        expObjTempSGINST.insertRow(new String[]{"17", "5", "A"});
        expObjTempSGINST.insertRow(new String[]{"18", "5", "C"});
        expObjTempSGINST.insertRow(new String[]{"19", "5", "C"});
        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"11", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"12", "5", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"13", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"14", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"15", "5", "Z"});
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
