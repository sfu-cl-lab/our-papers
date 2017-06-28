/**
 * $Id: SyntheticIIDTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Util;
import kdl.prox.util.stat.NormalDistribution;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: btaylor
 * Date: Apr 23, 2007
 * Time: 2:35:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class SyntheticIIDTest extends TestCase {

    private static final Logger log = Logger.getLogger(SyntheticIIDTest.class);


    public void setUp() throws Exception {
        super.setUp();
        Util.initProxApp();
        TestUtil.openTestConnection();
        DB.clearDB();
        DB.initEmptyDB();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testFixedIIDStructure() {
        int numSObjs = 2;
        int numTObjs = 2;

        // generate subgraphs
        Integer[][] objSpec = new Integer[][]{{numSObjs, numTObjs}};
        new SyntheticGraphIID(objSpec);
        checkDB(numSObjs, numTObjs);


    }

    public void testIIDStructure() {
        int numSObjs = 2;
        int numTObjs = 2;

        // generate structure
        Object[][] degreeDistribs = new Object[][]{
                {1.0, new NormalDistribution((double) numTObjs, 0.0000001)}}; // tiny stdDev ensures deterministic selection of degree
        new SyntheticGraphIID(numSObjs, degreeDistribs);
        checkDB(numSObjs, numTObjs);
    }


    private void checkDB(int numSObjs, int numTObjs) {
        // check database. NB: following tests don't depend on order of S and T
        // creation. we check links and object types by first collecting which
        // IDs correspond to Ss (2 of them) and Ts (4 of them), then go through
        // each link and remove S (o1) and T (o2) IDs, verifying that we use
        // them all up when done. note that this requires us to duplicate Ss
        // because each S participates in two links. to do so we build sIDs and
        // tIDs, then remove based on links
        assertEquals(numSObjs + numSObjs * numTObjs, DB.getObjectNST().getRowCount());
        assertEquals(numSObjs * numTObjs, DB.getLinkNST().getRowCount());

        List<Integer> sIDs = new ArrayList<Integer>();
        List<Integer> tIDs = new ArrayList<Integer>();
        NST attrDataNST = DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName());
        ResultSet resultSet = attrDataNST.selectRows();
        while (resultSet.next()) {
            Integer id = resultSet.getOID(1);
            String value = resultSet.getString(2);
            if (value.equals(SyntheticGraphIID.getCoreObjName())) {
                sIDs.add(id);
                sIDs.add(id);
            } else if (value.equals(SyntheticGraphIID.getPeriphObjName())) {
                tIDs.add(id);
            } else {
                fail("invalid object type: " + value);
            }
        }

        resultSet = DB.getLinkNST().selectRows();
        while (resultSet.next()) {
            Integer o1ID = resultSet.getOID(2);
            Integer o2ID = resultSet.getOID(3);
            boolean isRemoved = sIDs.remove(o1ID);
            assertTrue(isRemoved);
            isRemoved = tIDs.remove(o2ID);
            assertTrue(isRemoved);
        }
        assertTrue(sIDs.isEmpty());
        assertTrue(tIDs.isEmpty());
    }

}
