/**
 * $Id: ChiSquareTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ChiSquareTest.java 3658 2007-10-15 16:29:11Z schapira $
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
public class ChiSquareTest extends TestCase {

    private static final Logger log = Logger.getLogger(ChiSquareTest.class);

    protected void setUp() throws Exception {
        super.setUp();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testGStat() {
//        String rowHeadings[] = {"A", "B"};
//        String colHeadings[] = {"true", "false"};
//        double values[][] = {{10, 20}, {30, 40}};
//        ContingencyTable c = new ContingencyTable(rowHeadings, colHeadings, values);
//
//        assertTrue(StatUtil.roundDouble(c.gStatistic(), 1) == StatUtil.roundDouble(0.793651, 1)); //used chi-square values and rounded

        NST nst = new NST("label, weight, match", "str, dbl, bit");
        for (int i = 0; i < 10; i++) {
            nst.insertRow("A,1,true");
        }
        for (int i = 0; i < 20; i++) {
            nst.insertRow("B,1,true");
        }
        for (int i = 0; i < 30; i++) {
            nst.insertRow("A,1,false");
        }
        for (int i = 0; i < 40; i++) {
            nst.insertRow("B,1,false");
        }

        String gStat = Connection.readValue("gStatAndDF" + "(" +
                nst.getNSTColumn("label") + "," +
                nst.getNSTColumn("weight") + "," +
                nst.getNSTColumn("match") +
                ").print()").split(",")[0];


        assertTrue(StatUtil.roundDouble(Double.parseDouble(gStat), 1) == StatUtil.roundDouble(0.793651, 1)); //used chi-square values and rounded
    }

}
