/**
 * $Id: TFM20Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM20Test.java 3658 2007-10-15 16:29:11Z schapira $
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
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGraphTestingUtil;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;


/**
 * Tests TFM20.
 *
 * @see TFM20
 */
public class TFM20Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM20Test.class);

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

        DB.insertObject(1);  // A (subg 1: A and B, and subg 2 A)
        DB.insertObject(2);  // C (subg 2 B)

        Attributes attrs = DB.getObjectAttrs();
        attrs.defineAttribute("income", "int");
        NST objAttr1DataNST = attrs.getAttrDataNST("income");
        objAttr1DataNST.insertRow(new String[]{"1", "10"});
        objAttr1DataNST.release();

        attrs.defineAttribute("salary", "int");
        objAttr1DataNST = attrs.getAttrDataNST("salary");
        objAttr1DataNST.insertRow(new String[]{"1", "15"});
        objAttr1DataNST.insertRow(new String[]{"2", "10"});
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
     * The SGI table has two subgraphs:
     * - subgraph 1 has A and B of equal ids
     * - subgraph 2 has A and B of different ids
     *
     * @return
     * @
     */
    private List makeAObjALinkTempSGINSTs()  {
        NST objTempSGINST = makeTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"1", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "2", "B"});

        NST linkTempSGINST = makeTempSGINST();
        linkTempSGINST.insertRow(new String[]{"7", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"7", "2", "X"});

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
     * Subg   A.id  A.income  B.id  B.salary   (A.income GE b.salary?)
     * ---------------------------------------------------------------
     * 1      1     10        1     15          false
     * 2      1     10        2      5          true
     * <p/>
     * Should only keep SUBG2 then.
     *
     * @
     */
    public void testAttrsGE()  {
        TFM20 tfm20 = new TFM20();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ge",
                "A", "income", "B", "salary");
        QGConstraint qgConstraint = new QGConstraint(condTestEle, false);
        List objLinkTempSGINSTs = tfm20.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay
        // this is both in the object and link tables
        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "2", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "X"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
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
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();
    }


    /**
     * Comapres the income to the salary (just two made-up names)
     * <p/>
     * Subg   A.id  A.income  B.id  B.salary   (A.income GT b.salary?)
     * ---------------------------------------------------------------
     * 1      1     10        1     15          false
     * 2      1     10        2     10          fase
     * <p/>
     * Should remove both then.
     *
     * @
     */
    public void testAttrsGT()  {
        TFM20 tfm20 = new TFM20();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("gt",
                "A", "income", "B", "salary");
        QGConstraint qgConstraint = new QGConstraint(condTestEle, false);
        List objLinkTempSGINSTs = tfm20.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay
        // this is both in the object and link tables
        // make expected results
        NST expObjTempSGINST = makeTempSGINST();

        NST expLinkTempSGINST = makeTempSGINST();

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        actObjTempSGINST.release();
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();
    }


    /**
     * In subg 1 A and B are the same object. Should only keep subg 2.
     *
     * @
     */
    public void testIdsNE()  {
        TFM20 tfm20 = new TFM20();
        Element condTestEle = QGraphTestingUtil.makeConstraintTestElement("ne",
                "A", null, "B", null);
        QGConstraint qgConstraint = new QGConstraint(condTestEle, false);
        List objLinkTempSGINSTs = tfm20.execTFMExecInternal(qgUtil, actObjTempSGINST, actLinkTempSGINST,
                qgConstraint, true, new String[]{}); //isVertices
        NST actObjTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST actLinkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // We'd expect subgraph 1 to be gone, and subg 2 to stay
        // this is both in the object and link tables
        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "2", "B"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"7", "2", "X"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
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
        actLinkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();
    }


}
