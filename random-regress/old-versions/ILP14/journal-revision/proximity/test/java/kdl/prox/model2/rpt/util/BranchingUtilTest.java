/**
 * $Id: BranchingUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: BranchingUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.ItemSource;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.aggregators.DegreeAggregator;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

/**
 * Tests that we can find all the appropriate Aggregator classes
 * Modify tests as new classes are created
 */
public class BranchingUtilTest extends TestCase {

    private static final Logger log = Logger.getLogger(BranchingUtilTest.class);

    private NSTCache cache;
    private RPTState rptState;

    protected void setUp() throws Exception {
        super.setUp();

        Aggregator.setNumThresholds(2);
        rptState = new RPTState();
        cache = rptState.nstCache;


        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testSimpleBranch() {
        UnfilteredFeatureSetting fs = createFakeFS();

        NST subgIDs = new NST("subg_id, weight", "oid,dbl");
        subgIDs.insertRow("0, 1").insertRow("1,1").insertRow("2,1").insertRow("3,1");
        rptState.subgIDs = subgIDs;

        NST splitNST = new NST("subg_id, match", "oid, bit");
        splitNST.insertRow("0, false").insertRow("1, true").insertRow("2, false").insertRow("3, true");
        cache.saveTable(fs.toString(), splitNST);

        NST[] branchSubgIDs = new BranchingUtil().getBranchSubgIDs(fs, rptState);
        NST yesBranch = branchSubgIDs[0];
        NST noBranch = branchSubgIDs[1];
        TestUtil.verifyCollections(new Object[]{"1@0.1", "3@0.1"}, yesBranch);
        TestUtil.verifyCollections(new Object[]{"0@0.1", "2@0.1"}, noBranch);
    }

    public void testOneMissingValues() {
        UnfilteredFeatureSetting fs = createFakeFS();

        NST subgIDs = new NST("subg_id, weight", "oid,dbl");
        subgIDs.insertRow("0, 1").insertRow("1,1").insertRow("2,1").insertRow("3,1");
        rptState.subgIDs = subgIDs;

        NST splitNST = new NST("subg_id, match", "oid, bit");
        splitNST.insertRow("1, true").insertRow("2, false").insertRow("3, true");
        cache.saveTable(fs.toString(), splitNST);

        NST[] branchSubgIDs = new BranchingUtil().getBranchSubgIDs(fs, rptState);
        NST yesBranch = branchSubgIDs[0];
        NST noBranch = branchSubgIDs[1];
        assertEquals("0.66666668653488159", yesBranch.selectRows("subg_id = 0", "weight").toStringList(1).get(0));
        assertEquals("1", yesBranch.selectRows("subg_id = 1", "weight").toStringList(1).get(0));
        assertEquals("1", yesBranch.selectRows("subg_id = 3", "weight").toStringList(1).get(0));

        assertEquals("1", noBranch.selectRows("subg_id = 2", "weight").toStringList(1).get(0));
        assertEquals("0.3333333432674408", noBranch.selectRows("subg_id = 0", "weight").toStringList(1).get(0));
    }

    public void testMultipleMissingValues() {
        UnfilteredFeatureSetting fs = createFakeFS();

        NST subgIDs = new NST("subg_id, weight", "oid,dbl");
        subgIDs.insertRow("0, 1").insertRow("1,1").insertRow("2,1").insertRow("3,1");
        rptState.subgIDs = subgIDs;

        NST splitNST = new NST("subg_id, match", "oid, bit");
        splitNST.insertRow("1, true").insertRow("3, true");
        cache.saveTable(fs.toString(), splitNST);

        // they all fall into the true branch
        NST[] branchSubgIDs = new BranchingUtil().getBranchSubgIDs(fs, rptState);
        NST yesBranch = branchSubgIDs[0];
        assertEquals("1", yesBranch.selectRows("subg_id = 0", "weight").toStringList(1).get(0));
        assertEquals("1", yesBranch.selectRows("subg_id = 1", "weight").toStringList(1).get(0));
        assertEquals("1", yesBranch.selectRows("subg_id = 2", "weight").toStringList(1).get(0));
        assertEquals("1", yesBranch.selectRows("subg_id = 3", "weight").toStringList(1).get(0));

    }

    private UnfilteredFeatureSetting createFakeFS() {
        Container container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 1, B");
        container.getObjectsNST().insertRow("4, 2, A");
        container.getObjectsNST().insertRow("5, 2, B");
        container.getObjectsNST().insertRow("6, 2, B");
        container.getObjectsNST().insertRow("7, 3, A");
        container.getObjectsNST().insertRow("8, 3, B");
        container.getObjectsNST().insertRow("9, 3, B");

        ItemSource source = new ItemSource("A");
        source.init(container, cache);
        return new UnfilteredFeatureSetting(source, new DegreeAggregator(source), "1");
    }
}
