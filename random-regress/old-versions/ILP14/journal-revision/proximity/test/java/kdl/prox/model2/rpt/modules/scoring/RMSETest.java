/**
 * $Id: RMSETest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RMSETest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.modules.scoring;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

/**
 * Tests that we can find all the appropriate Aggregator classes
 * Modify tests as new classes are created
 */
public class RMSETest extends TestCase {

    private static final Logger log = Logger.getLogger(RMSETest.class);

    protected void setUp() throws Exception {
        super.setUp();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testRMSE() {
        NST nst = new NST("label, weight, match", "dbl, dbl, bit");
        nst.insertRow("1.0, 1, true");
        nst.insertRow("2.0, 1, true");
        nst.insertRow("2.0, 1, true");
        nst.insertRow("3.0, 1, false");
        nst.insertRow("4.0, 1, false");
        nst.insertRow("4.0, 1, false");

        String scoreStr = Connection.readValue("rmse" + "(" +
                nst.getNSTColumn("label") + "," +
                nst.getNSTColumn("weight") + "," +
                nst.getNSTColumn("match") +
                ").print()");
        log.info(scoreStr);

        String[] stats = scoreStr.split(",");
        double rmseNoSplit = Double.parseDouble(stats[0]);
        double rmse = Double.parseDouble(stats[1]);
        double d = Double.parseDouble(stats[2]);
        double dhat = Double.parseDouble(stats[3]);

        assertTrue(StatUtil.roundDouble(rmseNoSplit, 5) == StatUtil.roundDouble(1.10554, 5));
        assertTrue(StatUtil.roundDouble(rmse, 6) == StatUtil.roundDouble(0.471405, 6));
        assertTrue(StatUtil.roundDouble(d, 1) == StatUtil.roundDouble(0.5, 1));
        assertTrue(StatUtil.roundDouble(dhat, 4) == StatUtil.roundDouble(0.57735, 4));
    }


}
