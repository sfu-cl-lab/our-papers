/**
 * $Id: RPTAttrNameMapperTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RPTAttrNameMapperTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.aggregators.AverageAggregator;
import kdl.prox.model2.rpt.aggregators.SumAggregator;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class RPTAttrNameMapperTest extends TestCase {

    static Logger log = Logger.getLogger(RPTAttrNameMapperTest.class);

    RPT rpt1, rpt2;

    public void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("bb", "str");
        DB.getObjectAttrs().defineAttribute("success", "flt");
        DB.getObjectAttrs().defineAttribute("salary", "flt");
        DB.getObjectAttrs().defineAttribute("height", "flt");

        // set the isSingleValue and isContinuous here, so that the applies? method of the Aggregators work
        final Source class1Label = new AttributeSource("movie", "bb").setIsContinuous(true).setIsSingleValue(false);
        final Source class2Label = new AttributeSource("studio", "success").setIsContinuous(true).setIsSingleValue(false);
        final Source class1Split = new AttributeSource("actor", "height").setIsContinuous(true).setIsSingleValue(false);
        final Source class2Split = new AttributeSource("actor", "salary").setIsContinuous(true).setIsSingleValue(false);

        // First RPT has a split on studio.success and second on actor.height
        RPTNode rpt1Root = new RPTNode();
        rpt1Root.setSplit(new UnfilteredFeatureSetting(class2Label, new SumAggregator(class2Label), "10"));
        RPTNode rpt1Left = new RPTNode();
        rpt1Left.setSplit(new UnfilteredFeatureSetting(class1Split, new AverageAggregator(class1Split), "10"));
        rpt1Root.setYesBranch(rpt1Left);
        rpt1 = new RPT().learnFromRPTNode(rpt1Root, (AttributeSource) class1Label);

        // Second RPT has a split on actor.salary and the second on movie.bb
        RPTNode rpt2Root = new RPTNode();
        rpt2Root.setSplit(new UnfilteredFeatureSetting(class2Split, new SumAggregator(class2Split), "10"));
        RPTNode rpt2Left = new RPTNode();
        rpt2Left.setSplit(new UnfilteredFeatureSetting(class1Label, new AverageAggregator(class1Label), "10"));
        rpt2Root.setYesBranch(rpt2Left);
        rpt2 = new RPT().learnFromRPTNode(rpt2Root, (AttributeSource) class2Label);

    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testStartup() {
        final RPTAttrNameMapper mapper = new RPTAttrNameMapper();

        HashMap<RPT, Container> modelsToConts = new HashMap<RPT, Container>();
        modelsToConts.put(rpt1, DB.getRootContainer());
        modelsToConts.put(rpt2, DB.getRootContainer());
        mapper.startup(modelsToConts);

        Source newSource1 = new AttributeSource("studio", RPTAttrNameMapper.ATTR_PREFIX + "success");
        FeatureSetting fs1 = rpt1.getLearnedSplits().get(0);
        FeatureSetting fs2 = rpt1.getLearnedSplits().get(1);
        log.info(fs1.getSource());
        log.info(fs2.getSource());
        assertTrue(fs1.getSource().equals(newSource1) || fs2.getSource().equals(newSource1));

        Source newSource2 = new AttributeSource("movie", RPTAttrNameMapper.ATTR_PREFIX + "bb");
        fs1 = rpt2.getLearnedSplits().get(0);
        fs2 = rpt2.getLearnedSplits().get(1);
        assertTrue(fs1.getSource().equals(newSource2) || fs2.getSource().equals(newSource2));

        assertEquals(RPTAttrNameMapper.ATTR_PREFIX + "bb", rpt1.getClassLabel().getAttrName());
        assertEquals(RPTAttrNameMapper.ATTR_PREFIX + "success", rpt2.getClassLabel().getAttrName());

        assertTrue(DB.getObjectAttrs().isAttributeDefined(RPTAttrNameMapper.ATTR_PREFIX + "bb"));
        assertTrue(DB.getObjectAttrs().isAttributeDefined(RPTAttrNameMapper.ATTR_PREFIX + "success"));
    }


    public void testCleanup() {
        final RPTAttrNameMapper mapper = new RPTAttrNameMapper();

        HashMap<RPT, Container> modelsToConts = new HashMap<RPT, Container>();
        modelsToConts.put(rpt1, DB.getRootContainer());
        modelsToConts.put(rpt2, DB.getRootContainer());
        mapper.startup(modelsToConts);
        mapper.cleanup();

        Source newSource1 = new AttributeSource("studio", "success");
        FeatureSetting fs1 = rpt1.getLearnedSplits().get(0);
        FeatureSetting fs2 = rpt1.getLearnedSplits().get(1);
        assertTrue(fs1.getSource().equals(newSource1) || fs2.getSource().equals(newSource1));

        Source newSource2 = new AttributeSource("movie", "bb");
        fs1 = rpt2.getLearnedSplits().get(0);
        fs2 = rpt2.getLearnedSplits().get(1);
        assertTrue(fs1.getSource().equals(newSource2) || fs2.getSource().equals(newSource2));

        assertEquals("bb", rpt1.getClassLabel().getAttrName());
        assertEquals("success", rpt2.getClassLabel().getAttrName());

        assertFalse(DB.getObjectAttrs().isAttributeDefined(RPTAttrNameMapper.ATTR_PREFIX + "bb"));
        assertFalse(DB.getObjectAttrs().isAttributeDefined(RPTAttrNameMapper.ATTR_PREFIX + "success"));

    }
}
