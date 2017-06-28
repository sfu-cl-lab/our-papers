/**
 * $Id: QuinlanInfoTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
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
import kdl.prox.model2.rpt.util.FSGenerationUtil;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.script.AddAttribute;
import org.apache.log4j.Logger;

public class QuinlanInfoTest extends TestCase {

    private static final Logger log = Logger.getLogger(QuinlanInfoTest.class);

    private NSTCache cache;
    private RPTState rptState;
    Source genderSource;
    Source ageSource;
    AttributeSource labelSource;
    AttributeSource randomLabelSource;

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
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, f");
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

        // create the FS tables for gender and age
        genderSource = new AttributeSource("B", "gender");
        ageSource = new AttributeSource("A", "age");
        rptState.featureSettingList = FSGenerationUtil.createFSTables(container, new Source[]{genderSource, ageSource}, rptState);

        // use all the subgraphs
        rptState.subgIDs = container.getObjectsNST().project("subg_id").distinct().addConstantColumn("weight", "dbl", "1");

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rptState.nstCache.clear();
        TestUtil.closeTestConnection();
    }

    // compute the score of a real class label.
    public void testTrueClassLabel() {
        // Get the score for mode(gender)=m, which is 0.0506??
        FeatureSetting significantFS = new UnfilteredFeatureSetting(genderSource, new ModeAggregator(genderSource), "m");
        NST fsNST = cache.getTable(significantFS.toString());
        RPTScoringModule quinlanInfoScore = new QuinlanInfoScore().compute(rptState, labelSource.getSourceTable(), rptState.subgIDs, fsNST, null);
        assertEquals(((QuinlanInfoScore) quinlanInfoScore).measure, 0.6365, 0.0001);
    }

}
