/**
 * $Id: SampleNSTTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: SampleNSTTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.sample;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SampleNSTTest extends TestCase {
    protected Logger log = Logger.getLogger(SampleNST.class);
    protected Container container;
    protected int subgId1;
    protected int subgId2;
    protected int subgId3;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getRootContainer().deleteAllChildren();

        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);
        DB.insertObject(4);
        DB.insertObject(5);
        DB.insertObject(6);
        DB.insertObject(7);
        DB.insertObject(8);
        DB.insertObject(9);
        DB.insertObject(10);
        DB.insertObject(11);
        DB.insertObject(12);
        DB.insertObject(13);
        DB.insertObject(14);
        DB.insertObject(15);
        DB.insertObject(16);
        DB.insertObject(17);
        DB.insertObject(18);
        DB.insertObject(19);
        DB.insertObject(20);
        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 3, 2);
        DB.insertLink(3, 4, 3);
        DB.insertLink(4, 2, 5);
        DB.insertLink(5, 2, 6);
        DB.insertLink(6, 3, 7);
        DB.insertLink(7, 8, 3);
        DB.insertLink(8, 10, 4);
        DB.insertLink(9, 5, 7);
        DB.insertLink(10, 8, 9);

        DB.beginScope();
    }

    public void testSample() {
        List samples = SampleNST.sample(DB.getObjectNST(), 3, 4); // should give us four groups of three, all disjoint
        assertEquals(4, samples.size());

        List sampledIds = new ArrayList();
        for (int i = 0; i < 4; i++) {
            NST nst = (NST) samples.get(i);
            assertEquals(3, nst.getRowCount());

            ResultSet rs = nst.selectRows();
            while (rs.next()) {
                Integer id = new Integer(rs.getOID(1));
                assertTrue(!sampledIds.contains(id));
                sampledIds.add(id);
            }
        }
        assertEquals(12, sampledIds.size());

        NST nst = SampleNST.sample(DB.getLinkNST(), 5);
        assertEquals(5, nst.getRowCount());

        sampledIds = new ArrayList();
        int highCount = 0;
        ResultSet rs = nst.selectRows();
        while (rs.next()) {
            Integer id = new Integer(rs.getOID(1));
            assertTrue(!sampledIds.contains(id));
            sampledIds.add(id);
            if (id.intValue() > 5) {
                highCount++;
            }
        }
        assertTrue(highCount > 0); // this can fail, but it's really unlikely to sample five numbers between 1 and 10 and get 1-5

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        DB.endScope();
        TestUtil.closeTestConnection();
    }
}
