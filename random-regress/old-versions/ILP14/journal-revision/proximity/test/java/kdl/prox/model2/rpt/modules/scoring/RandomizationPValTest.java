/**
 * $Id: RandomizationPValTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RandomizationPValTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.modules.scoring;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.aggregators.ModeAggregator;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import kdl.prox.model2.rpt.modules.significance.RandomizationSignificanceModuleTest;
import kdl.prox.model2.rpt.util.FSGenerationUtil;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.script.AddAttribute;
import org.apache.log4j.Logger;

/**
 * kdl.prox.model2.rpt
 * User: afast
 * Date: Feb 14, 2007
 * $Id: RandomizationPValTest.java 3658 2007-10-15 16:29:11Z schapira $
 */
public class RandomizationPValTest extends TestCase {
    private static final Logger log = Logger.getLogger(RandomizationSignificanceModuleTest.class);

    private NSTCache cache;
    private RPTState rptState;
    Source genderSource;
    Source ageSource;
    AttributeSource labelSource;
    AttributeSource randomLabelSource;
    AttributeSource randomAttrSource;

    protected void setUp() throws Exception {
        super.setUp();

        Aggregator.setNumThresholds(2);
        rptState = new RPTState();
        cache = rptState.nstCache;

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

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

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("3, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("5, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("8, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("9, f");

        DB.getObjectAttrs().defineAttribute("age", "int");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("1, 40");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("4, 45");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("7, 10");

        container.getSubgraphAttrs().deleteAllAttributes();
        container.getSubgraphAttrs().defineAttribute("label", "str");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("1, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("2, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("3, -");

        // a true, non-random label (mode(gender)='m') predicts it
        labelSource = new AttributeSource("label");
        labelSource.init(container, cache);
        rptState.classLabel = labelSource;

        // also create a random class label
        new AddAttribute().addRandomBinaryAttribute(container, true, "A", "randomClass");
        randomLabelSource = new AttributeSource("A", "randomClass");
        randomLabelSource.init(container, cache);

        // also create a random attribute
        new AddAttribute().addRandomBinaryAttribute(container, true, "B", "randomAttr");
        randomAttrSource = new AttributeSource("B", "randomAttr");
        randomAttrSource.init(container, cache);

        // create the FS tables for gender and age
        genderSource = new AttributeSource("B", "gender");
        ageSource = new AttributeSource("A", "age");
        rptState.featureSettingList = FSGenerationUtil.createFSTables(container,
                new Source[]{genderSource, ageSource, randomAttrSource}, rptState);

        // use all the subgraphs
        rptState.subgIDs = container.getObjectsNST().project("subg_id").distinct().addConstantColumn("weight", "dbl", "1");

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rptState.nstCache.clear();
        TestUtil.closeTestConnection();
    }

    // compute the score of a real attr and a random attr. Random should never be better
    public void testRandomizedFeature() {
        RandomizationPValScore randomizationPVal = new RandomizationPValScore(new ChiSquareScore());

        // Get the score for mode(gender)=m, which is 0.0506
        FeatureSetting significantFS = new UnfilteredFeatureSetting(genderSource, new ModeAggregator(genderSource), "m");
        FeatureSetting randomFS = new UnfilteredFeatureSetting(randomAttrSource, new ModeAggregator(randomAttrSource), "1");

        NST fsNST = cache.getTable(significantFS.toString());
        RPTScoringModule significantFSScore = randomizationPVal.compute(rptState, labelSource.getSourceTable(), rptState.subgIDs, fsNST, null);
        fsNST = cache.getTable(randomFS.toString());
        RPTScoringModule randomFSScore = randomizationPVal.compute(rptState, labelSource.getSourceTable(), rptState.subgIDs, fsNST, null);

        log.info(significantFSScore.toString() + '\t' + randomFSScore.toString());
        assertTrue(significantFSScore.isBetterThan(randomFSScore));
    }
}
