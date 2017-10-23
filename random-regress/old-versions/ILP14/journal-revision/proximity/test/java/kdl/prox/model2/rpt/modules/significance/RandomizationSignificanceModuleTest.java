/**
 * $Id: RandomizationSignificanceModuleTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.modules.significance;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.aggregators.NopAggregator;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.ChiSquareScore;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import kdl.prox.model2.rpt.util.FSGenerationUtil;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

public class RandomizationSignificanceModuleTest extends TestCase {

    private static final Logger log = Logger.getLogger(RandomizationSignificanceModuleTest.class);

    private NSTCache cache;
    private RPTState rptState;
    Source genderSource;
    AttributeSource labelSource;
    AttributeSource randomLabelSource;

    protected void setUp() throws Exception {
        super.setUp();

        Aggregator.setNumThresholds(2);

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        Container container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 2, A");
        container.getObjectsNST().insertRow("4, 2, B");
        container.getObjectsNST().insertRow("5, 3, A");
        container.getObjectsNST().insertRow("6, 3, B");
        container.getObjectsNST().insertRow("1, 4, A");
        container.getObjectsNST().insertRow("2, 4, B");
        container.getObjectsNST().insertRow("3, 5, A");
        container.getObjectsNST().insertRow("4, 5, B");
        container.getObjectsNST().insertRow("5, 6, A");
        container.getObjectsNST().insertRow("6, 6, B");
        container.getObjectsNST().insertRow("1, 7, A");
        container.getObjectsNST().insertRow("2, 7, B");
        container.getObjectsNST().insertRow("3, 8, A");
        container.getObjectsNST().insertRow("4, 8, B");
        container.getObjectsNST().insertRow("5, 9, A");
        container.getObjectsNST().insertRow("6, 9, B");
        container.getObjectsNST().insertRow("1,10, A");
        container.getObjectsNST().insertRow("2,10, B");
        container.getObjectsNST().insertRow("3,11, A");
        container.getObjectsNST().insertRow("4,11, B");
        container.getObjectsNST().insertRow("5,12, A");
        container.getObjectsNST().insertRow("6,12, B");

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("4, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, f");

        container.getSubgraphAttrs().deleteAllAttributes();
        container.getSubgraphAttrs().defineAttribute("label", "str");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 1, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 2, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 3, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 4, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 5, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 6, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 7, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 8, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow(" 9, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("10, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("11, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("12, -");


        container.getSubgraphAttrs().defineAttribute("random", "str");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 1, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 2, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 3, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 4, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 5, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 6, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 7, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 8, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow(" 9, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow("10, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow("11, -");
        container.getSubgraphAttrs().getAttrDataNST("random").insertRow("12, -");

        rptState = new RPTState();
        cache = rptState.nstCache;

        // a true, non-random label (mode(B.gender)='m') predicts it
        labelSource = new AttributeSource("label");
        labelSource.init(container, cache);

        // also create a random class label. All -, so there can be no connection
        randomLabelSource = new AttributeSource("random");
        randomLabelSource.init(container, cache);

        // create the FS tables for gender and prepare RPTState
        genderSource = new AttributeSource("B", "gender");
        rptState.classLabel = labelSource;
        rptState.featureSettingList = FSGenerationUtil.createFSTables(container, new Source[]{genderSource}, rptState);
        rptState.subgIDs = container.getObjectsNST().project("subg_id").distinct().addConstantColumn("weight", "dbl", "1");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rptState.nstCache.clear();
        TestUtil.closeTestConnection();
    }

    public void testBasicThresholds() {
        RandomizationSignificanceModule randomizeSplittingModule = new RandomizationSignificanceModule();
        assertTrue(randomizeSplittingModule.isSignificant(rptState, new ChiSquareScore("500.0,1")));
        assertFalse(randomizeSplittingModule.isSignificant(rptState, new ChiSquareScore("500.0,1000")));
    }

    // compute the score of a real class label. Randomize should never be better
    public void testTrueClassLabel() {
        RandomizationSignificanceModule randomizeSplittingModule = new RandomizationSignificanceModule();
        randomizeSplittingModule.setpVal(0.06);

        // Get the score for mode(gender)=m, which is 0.0506
        FeatureSetting significantFS = new UnfilteredFeatureSetting(genderSource, new NopAggregator(genderSource), "m");
        NST fsNST = cache.getTable(significantFS.toString());
        RPTScoringModule significantFSScore = new ChiSquareScore().compute(rptState, labelSource.getSourceTable(), rptState.subgIDs, fsNST, null);

        // set the class label to the random attr.
        // It should never be able to find anything better than the true class label's score
        rptState.classLabel = randomLabelSource;
        assertTrue(randomizeSplittingModule.isSignificant(rptState, significantFSScore));
    }

    // compute the score of a random class label. When using the true label, it should be beaten
    // at least some of the time
    public void testRandomClassLabel() {
        RandomizationSignificanceModule randomizeSplittingModule = new RandomizationSignificanceModule();
        randomizeSplittingModule.setpVal(11); // so that it passes score's test

        // Get the score for a random label
        FeatureSetting significantFS = new UnfilteredFeatureSetting(genderSource, new NopAggregator(genderSource), "m");
        NST fsNST = cache.getTable(significantFS.toString());
        RPTScoringModule significantFSScore = new ChiSquareScore().compute(rptState, randomLabelSource.getSourceTable(), rptState.subgIDs, fsNST, null);

        // set the class label to the random attr.
        // It should never be able to find anything better than the true class label's score
        rptState.classLabel = labelSource;
        assertFalse(randomizeSplittingModule.isSignificant(rptState, significantFSScore));
    }
}
