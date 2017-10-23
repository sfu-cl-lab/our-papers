/**
 * $Id: FSGenerationUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: FSGenerationUtilTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.aggregators.AggregatorTestUtil;
import kdl.prox.model2.rpt.aggregators.CountAggregator;
import kdl.prox.model2.rpt.aggregators.NopAggregator;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that we can find all the appropriate Aggregator classes
 * Modify tests as new classes are created
 */
public class FSGenerationUtilTest extends TestCase {

    private static final Logger log = Logger.getLogger(FSGenerationUtilTest.class);

    Container container;
    Container testContainer;
    private NSTCache cache;
    private RPTState rptState;

    protected void setUp() throws Exception {
        super.setUp();

        Aggregator.setNumThresholds(2);
        rptState = new RPTState();
        cache = rptState.nstCache;

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 1, B");
        container.getObjectsNST().insertRow("4, 2, A");
        container.getObjectsNST().insertRow("5, 2, B");
        container.getObjectsNST().insertRow("6, 2, B");
        container.getObjectsNST().insertRow("7, 3, A");
        container.getObjectsNST().insertRow("8, 3, B");
        container.getObjectsNST().insertRow("9, 3, B");

        testContainer = DB.createNewTempContainer();
        testContainer.getObjectsNST().insertRow("2, 1, B");
        testContainer.getObjectsNST().insertRow("6, 2, B");
        testContainer.getObjectsNST().insertRow("8, 3, B");


        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("3, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("5, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("8, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("9, f");

        DB.getObjectAttrs().defineAttribute("age", "int");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("1, 40");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("4, 45");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("7, 10");

        DB.getObjectAttrs().defineAttribute("height", "flt");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("1, 5.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("1, 6.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("4, 5.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("4, 6.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("7, 6.4");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("7, 7.4");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        DB.deleteTempContainers();
        TestUtil.closeTestConnection();
    }


    public void testAllAggregators() {
        Source genderSource = new AttributeSource("B", "gender");

        FSGenerationUtil FSGenerationUtil = new FSGenerationUtil();
        List tables = FSGenerationUtil.createFSTables(container, new Source[]{genderSource}, rptState);
        assertEquals(12, tables.size());

        List tableList = new ArrayList();
        for (int fsIdx = 0; fsIdx < tables.size(); fsIdx++) {
            FeatureSetting featureSetting = (FeatureSetting) tables.get(fsIdx);
            tableList.add(featureSetting.toString());
        }

        assertTrue(tableList.contains("prop([B.gender]=m)>=0"));
        assertTrue(tableList.contains("prop([B.gender]=m)>=0.5"));
        assertTrue(tableList.contains("prop([B.gender]=f)>=0"));
        assertTrue(tableList.contains("prop([B.gender]=f)>=0.5"));
        assertTrue(tableList.contains("count_distinct([B.gender])>=1"));
        assertTrue(tableList.contains("count_distinct([B.gender])>=2"));
        assertTrue(tableList.contains("mode([B.gender])=m"));
        assertTrue(tableList.contains("mode([B.gender])=f"));
        assertTrue(tableList.contains("count([B.gender]=m)>=0"));
        assertTrue(tableList.contains("count([B.gender]=m)>=1"));
        assertTrue(tableList.contains("count([B.gender]=f)>=0"));
        assertTrue(tableList.contains("count([B.gender]=f)>=1"));
    }

    public void testSelectedAggregators() {
        Source genderSource = new AttributeSource("B", "gender");
        Source ageSource = new AttributeSource("A", "age");
        Source heightSource = new AttributeSource("A", "height");
        Source[] sourceList = new Source[]{genderSource, ageSource, heightSource};


        List<String> invalidAggregatorNamesForCore = new ArrayList<String>();
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ModeAggregator");
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ProportionAggregator");

        List<String> validAggregatorNames = new ArrayList<String>();
        validAggregatorNames.add(CountAggregator.class.getName());
        validAggregatorNames.add(NopAggregator.class.getName());
        List tables = FSGenerationUtil.createFSTables(container, sourceList, rptState, validAggregatorNames, invalidAggregatorNamesForCore);

        // count doesn't apply on age, which has a single row per subgraph. instead, what applies is the NopAggregator
        AggregatorTestUtil.verifyValues(cache, tables,
                new String[]{
                        "count([B.gender]=f)>=0",
                        "count([B.gender]=f)>=1",
                        "count([B.gender]=m)>=0",
                        "count([B.gender]=m)>=1",
                        "nop([A.age])=10",
                        "nop([A.age])=40",
                        "nop([A.age])=45",
                        "count([A.height]>6.4000001)>=0",
                        "count([A.height]>6.4000001)>=1",
                        "count([A.height]>5.5)>=1",
                        "count([A.height]>5.5)>=2"},
                new String[][]{
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.false", "2@0.true", "3@0.true"},
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.true", "2@0.true", "3@0.false"},
                        {"1@0.false", "2@0.false", "3@0.true"},
                        {"1@0.true", "2@0.false", "3@0.false"},
                        {"1@0.false", "2@0.true", "3@0.false"},
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.true", "2@0.false", "3@0.true"},
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.false", "2@0.false", "3@0.true"}});
    }


    public void testGenerateFromList() {
        Source genderSource = new AttributeSource("B", "gender");
        Source[] sourceList = new Source[]{genderSource};

        List<String> invalidAggregatorNamesForCore = new ArrayList<String>();
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ModeAggregator");
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ProportionAggregator");

        List<String> validAggregatorNames = new ArrayList<String>();
        validAggregatorNames.add(CountAggregator.class.getName());
        List tables = FSGenerationUtil.createFSTables(container, sourceList, rptState, validAggregatorNames, invalidAggregatorNamesForCore);

        // now re-generate on the trainContainer, and make sure it doesn't give an error
        cache.clear();
        tables = FSGenerationUtil.createFSTables(testContainer, tables, rptState);

        AggregatorTestUtil.verifyValues(cache, tables,
                new String[]{
                        "count([B.gender]=m)>=0",
                        "count([B.gender]=m)>=1",
                        "count([B.gender]=f)>=0",
                        "count([B.gender]=f)>=1"},
                new String[][]{
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.true", "2@0.true", "3@0.false"},
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.false", "2@0.false", "3@0.true"}}); //note: this is the testContainer
    }
}
