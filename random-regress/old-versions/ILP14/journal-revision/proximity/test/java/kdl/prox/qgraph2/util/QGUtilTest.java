/**
 * $Id: QGUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QGUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;


/**
 * Tests TFM21.
 *
 * @see kdl.prox.qgraph2.tfm.TFM21
 */
public class QGUtilTest extends TestCase {

    private static Logger log = Logger.getLogger(QGUtilTest.class);

    protected void setUp() throws Exception {
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    /**
     * Thest that self-loops are not duplicated in undirected consolidation of vertices
     */
    public void testSelfLoopInUndirectedJoin() {
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        DB.insertObject(1);  // A
        DB.insertObject(2);  // B
        DB.insertObject(3);  // C

        DB.insertLink(0, 1, 2);
        DB.insertLink(1, 1, 3);
        DB.insertLink(2, 1, 1);

        String[] columnNames = new String[]{"o_id"};
        String[] columnTypes = new String[]{"oid"};
        NST aObjs = new NST(columnNames, columnTypes);
        aObjs.insertRow(new String[]{"1"});
        aObjs.insertRow(new String[]{"2"});
        aObjs.insertRow(new String[]{"3"});

        QGUtil qgUtil = new QGUtil(null);
        NST result = qgUtil.getObjectsConnectedViaUndirectedLinks(aObjs, aObjs, DB.getLinkNST());
        assertEquals(5, result.getRowCount());

        int[] idCount = new int[3];
        ResultSet resultSet = result.selectRows("*", "link_id", "*");
        while (resultSet.next()) {
            int linkID = resultSet.getOID(1);
            assertTrue(linkID >= 0);
            assertTrue(linkID <= 2);
            idCount[linkID]++;
        }
        assertEquals(2, idCount[0]);
        assertEquals(2, idCount[1]);
        assertEquals(1, idCount[2]);

        result.release();
        aObjs.release();
        qgUtil.release();
    }

    /**
     * Tests a special condition in reCheckLinks
     * If the original subgraph was
     *
     *   1 --> 2
     *     |
     *     --> 3
     *
     * and (3) disappears, it's easy to remove the second link (just make sure that at least
     * one of the ends is (2) and you're done). However, that method doesn't work if the original
     * subgraph was
     *
     *   1 --> 1
     *     |
     *     --> 3
     *
     * in that case, both links have at least one end in (1)! So, we need to find a better method to
     * reCheckLinks. This test verifies that the second subgraph does indeed remove the second link
     */
    /*
    public void testRecheckLinksWithLoop() {
        DB.insertObject(1);  // A
        DB.insertObject(2);  // B
        DB.insertObject(3);  // C

        DB.insertLink(0, 1, 2);
        DB.insertLink(1, 1, 3);
        DB.insertLink(2, 1, 1);

        NST objTempSGINST = SGIUtil.createTempSGINST(DB.getProxDBMgr());
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"1", "1", "B"});
        objTempSGINST.insertRow(new String[]{"3", "1", "B"});
        NST linkTempSGINST = SGIUtil.createTempSGINST(DB.getProxDBMgr());
        linkTempSGINST.insertRow(new String[]{"1", "1", "LT"});
        linkTempSGINST.insertRow(new String[]{"2", "1", "LT"});


        QGUtil qgUtil = new QGUtil(proxDB, null);
        NST newLinkTempSGINST = SGIUtil.createTempSGINST(DB.getProxDBMgr());
        qgUtil.reCheckLinks(linkTempSGINST, objTempSGINST, "B", new String[]{"LT"},newLinkTempSGINST);

        // the new table should only contain the link named "2"
        ResultSet resultSet = newLinkTempSGINST.selectRows("item_id");
        List itemsList = resultSet.toStringList(1);
        assertEquals(1, itemsList.size());
        assertTrue(itemsList.contains("2"));

        qgUtil.release();
        objTempSGINST.release();
        linkTempSGINST.release();
        newLinkTempSGINST.release();
    }
    */
}
