/**
 * $Id: TFM11Test.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM11Test.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;


/**
 * Tests TFM11.
 *
 * @see TFM11
 */
public class TFM11Test extends TestCase {

    private static Logger log = Logger.getLogger(TFM11Test.class);

    private QGUtil qgUtil;

    private NST actObjTempSGINST;
    private NST actLinkTempSGINST;

    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        qgUtil = new QGUtil(null);

        // create input
        List tempSGINSTs = makeTempSGINSTs();
        actObjTempSGINST = (NST) tempSGINSTs.get(0);
        actLinkTempSGINST = (NST) tempSGINSTs.get(1);
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        qgUtil.release();
        TestUtil.closeTestConnection();
    }


    /**
     * Prepare the data
     * The SGI table has two subgraphs:
     * - subgraph 1 has three As and two Bs
     * - subgraph 2 has two Bs and three As
     * <p/>
     * If the TFM11 on A is so that annot [1..2], then subgraph 1 should be
     * removed.
     *
     * @return
     * @
     */
    private List makeTempSGINSTs()  {
        NST objTempSGINST = makeTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "A"});
        objTempSGINST.insertRow(new String[]{"3", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"4", "1", "B"});
        objTempSGINST.insertRow(new String[]{"5", "2", "A"});
        objTempSGINST.insertRow(new String[]{"6", "2", "A"});
        objTempSGINST.insertRow(new String[]{"7", "2", "B"});
        objTempSGINST.insertRow(new String[]{"8", "2", "B"});
        objTempSGINST.insertRow(new String[]{"9", "2", "B"});

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


    public void testIt()  {
        TFM11 tfm11 = new TFM11();
        tfm11.execTFMExecInternal(actObjTempSGINST, actLinkTempSGINST, "A", 2); // name of the object, upper limit

        // the operations are performed in-place, so we should now look
        // at the contents of the SGI tables
        // We'd expect subgraph 1 to be gone, and subg 2 to stay
        // this is both in the object and link tables
        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"5", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"8", "2", "B"});
        expObjTempSGINST.insertRow(new String[]{"9", "2", "B"});

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
