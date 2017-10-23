/**
 * $Id: RPTTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RPTTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.modules.learning.DefaultLearningModule;
import kdl.prox.model2.rpt.modules.significance.DefaultSignificanceModule;
import kdl.prox.model2.rpt.modules.splitting.DefaultSplittingModule;
import kdl.prox.model2.rpt.modules.stopping.DefaultStoppingModule;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Tests that we can find all the appropriate Aggregator classes
 * Modify tests as new classes are created
 */
public class RPTTest extends TestCase {

    private static final Logger log = Logger.getLogger(RPTTest.class);

    Container container;
    Source genderSource;
    Source ageSource;
    Source heightSource;
    AttributeSource labelSource;
    AttributeSource label2Source;
    AttributeSource label3Source;
    private RPT rpt;

    protected void setUp() throws Exception {
        super.setUp();

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

        DB.getObjectAttrs().defineAttribute("height", "flt");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("1, 5.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("1, 6.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("4, 5.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("4, 6.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("7, 6.4");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("7, 7.4");

        container.getSubgraphAttrs().deleteAllAttributes();
        container.getSubgraphAttrs().defineAttribute("label", "str");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("1, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("2, -");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("3, -");

        container.getSubgraphAttrs().defineAttribute("label2", "int");
        container.getSubgraphAttrs().getAttrDataNST("label2").insertRow("1, 0");
        container.getSubgraphAttrs().getAttrDataNST("label2").insertRow("2, 1");
        container.getSubgraphAttrs().getAttrDataNST("label2").insertRow("3, 1");

        container.getSubgraphAttrs().defineAttribute("label3", "dbl");
        container.getSubgraphAttrs().getAttrDataNST("label3").insertRow("1, 0.0");
        container.getSubgraphAttrs().getAttrDataNST("label3").insertRow("2, 10.0");
        container.getSubgraphAttrs().getAttrDataNST("label3").insertRow("3, 12.0");

        genderSource = new AttributeSource("B", "gender");
        ageSource = new AttributeSource("A", "age");
        heightSource = new AttributeSource("A", "height");
        labelSource = new AttributeSource("label");
        label2Source = new AttributeSource("label2");
        label3Source = new AttributeSource("label3");

        // Create rpt with deterministic choices
        rpt = new RPT();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testApplyEmptyRPT() {
        // build the same RPT as in testEmptyRPT, and apply it.
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource});
        Predictions predictions = rpt.apply(container);
        // set the expected true values, and make sure that the zeroOneLoss is 0.0
        predictions.setTrueLabel("1@0", "-").setTrueLabel("2@0", "-").setTrueLabel("3@0", "-");
        assertEquals(0.0, predictions.getZeroOneLoss(), 0.00001);
    }

    public void testApplyLevelOneRPT() {
        // build the same RPT as in testLevelOneRPT, and apply it.
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});
        Predictions predictions = rpt.apply(container);
        // set the expected true values, and make sure that the zeroOneLoss is 0.0
        predictions.setTrueLabel("1@0", "+").setTrueLabel("2@0", "-").setTrueLabel("3@0", "-");
        assertEquals(0.0, predictions.getZeroOneLoss(), 0.00001);
    }

    public void testApplyLevelOneRPTContinuous() {
        // build the same RPT as in testLevelOneRPT, and apply it.

        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.05);
        learnModule.stoppingModule = new DefaultStoppingModule().setMaxDepth(2);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, label3Source, new Source[]{genderSource, ageSource, heightSource});
        Predictions predictions = rpt.apply(container);
        // set the expected true values, and make sure that the zeroOneLoss is 0.0
        predictions.setTrueLabel("1@0", "0.0").setTrueLabel("2@0", "10.0").setTrueLabel("3@0", "12.0");
        assertEquals(0.0, predictions.getRMSE(), 0.00001);
    }

    /**
     * Tests that the probabilities of class labels is adjusted when a given subgraph is split
     * into several branches (because it has missing values for one or more of the splits)
     */
    public void testApplyWithWeightsSingleLevel() {

        RPT rpt = new RPT();
        // build an RPT. The top-level split is on gender
        // build the same RPT as in testLevelOneRPT, and apply it.
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        // manually set the distributions, to be
        // on the left: + .9 , - .1  (100 instances)
        // on the right: + .2 , - .8 (60  instances)
        // the yes proportion is 0.625 (100 / 160)
        ProbDistribution allDistr = new DiscreteProbDistribution().addAttributeValue("+", 140).addAttributeValue("-", 20);
        ProbDistribution leftDistr = new DiscreteProbDistribution().addAttributeValue("+", 90).addAttributeValue("-", 10);
        ProbDistribution noDistr = new DiscreteProbDistribution().addAttributeValue("+", 15).addAttributeValue("-", 45);
        rpt.root.classLabelDistribution = allDistr;
        rpt.root.getYesBranch().classLabelDistribution = leftDistr;
        rpt.root.getNoBranch().classLabelDistribution = noDistr;

        // now test it on a container that doesn't have anything for gender
        Container testcontainer = DB.createNewTempContainer();
        testcontainer.getObjectsNST().insertRow("1, 1, A");
        testcontainer.getObjectsNST().insertRow("22, 1, B"); // not related to any gender attribute.
        testcontainer.getSubgraphAttrs().defineAttribute("label", "str");
        testcontainer.getSubgraphAttrs().getAttrDataNST("label").insertRow("1, +");
        Predictions predictions = rpt.apply(testcontainer);

        // The probability of + is (105 / 160) = (.625 * .9) + (0.375 * .25) = 0.654
        // The probability of + is ( 55 / 160) = (.625 * .1) + (0.375 * .75) = 0.344
        ProbDistribution distribution = predictions.getProbDistribution("1@0");
        assertEquals(0.656, distribution.getProbability("+"), 0.01);
        assertEquals(0.344, distribution.getProbability("-"), 0.01);
    }

    public void testApplyLevelTwoRPTContinuousWithWeights() {
        // build the same RPT as in testLevelOneRPT, and apply it.
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.stoppingModule = new DefaultStoppingModule().setMaxDepth(2);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, label3Source, new Source[]{genderSource, ageSource, heightSource});
        rpt.print();

        DB.getObjectAttrs().getAttrDataNST("gender").deleteRows("id = 5");
        Predictions predictions = rpt.apply(container);
        ProbDistribution distribution1 = predictions.getProbDistribution("1@0");
        ProbDistribution distribution2 = predictions.getProbDistribution("2@0");
        ProbDistribution distribution3 = predictions.getProbDistribution("3@0");
        assertEquals(1.0, distribution1.getCount(0.0) / distribution1.getTotalNumValues(), 0.01);
        assertEquals(0.5, distribution2.getCount(0.0) / distribution2.getTotalNumValues(), 0.01);
        assertEquals(0.5, distribution2.getCount(10.0) / distribution2.getTotalNumValues(), 0.01);
        assertEquals(0.0, distribution2.getCount(12.0) / distribution2.getTotalNumValues(), 0.01);
        assertEquals(1.0, distribution3.getCount(12.0) / distribution3.getTotalNumValues(), 0.01);
    }


    public void testEmptyRPT() {
        // with such low p-val, no feature is good enough for a split
        // build the same RPT as in testLevelOneRPT, and apply it.
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});
        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
    }

    public void testLevelOneRPT() {
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.35);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
        rpt.print();
        assertTrue("count([B.gender]=f)>=1".equals(rootNode.split.toString()));
        assertEquals(0.3333, rootNode.getClassLabelDistribution().getProbability("+"), 0.0001);
        assertEquals(0.6667, rootNode.getClassLabelDistribution().getProbability("-"), 0.0001);

        assertNotNull(rootNode.yesBranch);
        assertEquals("-", ((DiscreteProbDistribution) rootNode.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.yesBranch.split);

        assertNotNull(rootNode.noBranch);
        assertEquals("+", ((DiscreteProbDistribution) rootNode.noBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.noBranch.split);
    }

    public void testLevelOneMissingData() {
        // add a new subgraph, without any information about it
        container.getObjectsNST().insertRow("10, 4, A");
        container.getObjectsNST().insertRow("11, 4, B");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("4, -");

        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
        assertTrue("count([B.gender]=f)>=1".equals(rootNode.split.toString()));
        assertEquals(0.25, rootNode.getClassLabelDistribution().getProbability("+"), 0.0001);
        assertEquals(0.75, rootNode.getClassLabelDistribution().getProbability("-"), 0.0001);

        assertNotNull(rootNode.yesBranch);
        assertEquals("-", ((DiscreteProbDistribution) rootNode.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.yesBranch.split);

        assertNotNull(rootNode.noBranch);
        assertEquals("+", ((DiscreteProbDistribution) rootNode.noBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.noBranch.split);
    }

    public void testLevelOneRPTIntLabel() {
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, label2Source, new Source[]{genderSource});

        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
        assertTrue("count([B.gender]=f)>=1".equals(rootNode.split.toString()));
        assertEquals(0.3333, rootNode.getClassLabelDistribution().getProbability("0"), 0.0001);
        assertEquals(0.6667, rootNode.getClassLabelDistribution().getProbability("1"), 0.0001);

        assertNotNull(rootNode.yesBranch);
        assertEquals("1", ((DiscreteProbDistribution) rootNode.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.yesBranch.split);

        assertNotNull(rootNode.noBranch);
        assertEquals("0", ((DiscreteProbDistribution) rootNode.noBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.noBranch.split);
    }

    public void testLevelOneRPTMissingClassLabel() {
        // this test should be identical to testLevelOneRPT, even with the new subgraph, because
        // there isn't any class label defined for the subg
        container.getObjectsNST().insertRow("10, 8, A");
        container.getObjectsNST().insertRow("11, 8, B");
        container.getObjectsNST().insertRow("12, 8, B");

        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
        assertTrue("count([B.gender]=f)>=1".equals(rootNode.split.toString()));
        assertEquals(0.3333, rootNode.getClassLabelDistribution().getProbability("+"), 0.0001);
        assertEquals(0.6667, rootNode.getClassLabelDistribution().getProbability("-"), 0.0001);

        assertNotNull(rootNode.yesBranch);
        assertEquals("-", ((DiscreteProbDistribution) rootNode.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.yesBranch.split);

        assertNotNull(rootNode.noBranch);
        assertEquals("+", ((DiscreteProbDistribution) rootNode.noBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.noBranch.split);
    }

    public void testLevelTwoRPT() {
        addAttrsForLevelTwoTree();

        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.35);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        verifyLevelTwoTree(rpt);
    }

    public void testReadFromXML() {
        RPT rpt = new RPT().load(getClass().getResource("2dtree-test.xml").getFile());

        assertEquals(rpt.classLabel.toString(), new AttributeSource("core_page", "pagetype").toString());

        RPTNode rootNode = rpt.getRootNode();
        assertNotNull(rootNode);
        assertEquals("count([linked_to_page.page_num_inlinks]=1)>=1", rootNode.split.toString());
        assertEquals("Other", ((DiscreteProbDistribution) rootNode.getClassLabelDistribution()).getHighestProbabilityValue());

        RPTNode yesBranch = rootNode.getYesBranch();
        assertNotNull(yesBranch);
        assertEquals("degree([linked_to_page])>=1", yesBranch.split.toString());
        assertEquals("Other", ((DiscreteProbDistribution) yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());

        RPTNode noBranch = rootNode.getNoBranch();
        assertNotNull(noBranch);
        assertEquals("count([linked_from_page.page_num_outlinks]=147)>=1", noBranch.split.toString());
        assertEquals(46.0, noBranch.getClassLabelDistribution().getCount("Faculty"), 0.000001);

        RPTNode noYesBranch = noBranch.getYesBranch();
        assertNotNull(noYesBranch);
        assertNull(noYesBranch.split);
        assertEquals(39.0, noYesBranch.getClassLabelDistribution().getCount("Student"), 0.000001);

        RPTNode noNoBranch = noBranch.getNoBranch();
        assertNotNull(noNoBranch);
        assertNull(noNoBranch.split);
        assertEquals(71.0, noNoBranch.getClassLabelDistribution().getCount("Student"), 0.000001);
    }

    public void testWriteToXML() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        File outFile = new File(System.getProperty("java.io.tmpdir", "tmp"), "test-dump.xml");

        RPT rpt = new RPT().load(inFile.getPath());
        rpt.save(outFile.getPath());

        String expectedFileCont = Util.readStringFromFile(inFile);
        String actualFileCont = Util.readStringFromFile(outFile);
        assertEquals(expectedFileCont, actualFileCont);

        outFile.delete();
    }

    public void testReadTemporalXML() {
        RPT rpt = new RPT().load(getClass().getResource("test-temporal-tree.xml").getFile());
        RPTNode temporalNode = rpt.getRootNode().getYesBranch().getYesBranch();

        assertNotNull(temporalNode);
        assertEquals("count([Disclosure.disclosure_type][[year]+-4<=[Disclosure.disclosure_year]<=[year]+-1]=CUSTCOMP)>=2", temporalNode.split.toString());

    }

    public void testTemporalWriteToXML() {
        File inFile = new File(getClass().getResource("test-temporal-tree.xml").getFile());
        File outFile = new File(System.getProperty("java.io.tmpdir", "tmp"), "test-dump.xml");

        RPT rpt = new RPT().load(inFile.getPath());
        rpt.save(outFile.getPath());

        String expectedFileCont = Util.readStringFromFile(inFile);
        String actualFileCont = Util.readStringFromFile(outFile);
        assertEquals(expectedFileCont, actualFileCont);

        outFile.delete();
    }

    public void testGetLeafCount() {
        //Test a depth 1 tree
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        RPTNode rootNode = rpt.getRootNode();
        assertEquals(2, rootNode.getLeafCount());

        //Test a depth 2 tree
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        rpt = new RPT().load(inFile.getPath());
        rootNode = rpt.getRootNode();
        assertEquals(4, rootNode.getLeafCount());
    }

    public void testGetDepth() {
        //Test a depth 1 tree
        DefaultLearningModule learnModule = new DefaultLearningModule();
        learnModule.significanceModule = new DefaultSignificanceModule().setpVal(0.06);
        learnModule.splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        rpt.learningModule = learnModule;
        rpt.learn(container, labelSource, new Source[]{genderSource, ageSource, heightSource});

        RPTNode rootNode = rpt.getRootNode();
        assertEquals(1, rootNode.getDepth());

        //Test a depth 2 tree
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        rpt = new RPT().load(inFile.getPath());
        rootNode = rpt.getRootNode();
        assertEquals(2, rootNode.getDepth());
    }

    public void testResubstitutionError() {
        //Test a depth 2 tree
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();
        assertEquals(0.1779497098646035, rootNode.getTreeResubError(rootNode.getInstanceCount()), 0.0001);
        assertEquals(0.2736944, rootNode.getNodeResubError(rootNode.getInstanceCount()), 0.0001);
    }

    public void testCopy() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();
        RPTNode copyNode = rootNode.copy();

        assertNotSame(rootNode, copyNode);
        assertNotSame(rootNode.yesBranch, copyNode.yesBranch);
        assertEquals(rootNode.yesBranch.yesBranch.classLabelDistribution.getTotalNumValues(), copyNode.yesBranch.yesBranch.classLabelDistribution.getTotalNumValues());
        assertSame(rootNode.yesBranch.yesBranch.classLabelDistribution, copyNode.yesBranch.yesBranch.classLabelDistribution);

    }

    public void testCollapse() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();
        rootNode.collapse();

        assertEquals(true, rootNode.isLeaf());
        assertNull(rootNode.yesBranch);
        assertEquals(0, rootNode.getDepth());
    }

    private void addAttrsForLevelTwoTree() {
        // with these new values, the best first split is by age, and only then by gender
        container.getObjectsNST().insertRow("10, 4, A");
        container.getObjectsNST().insertRow("11, 4, B");
        container.getObjectsNST().insertRow("12, 4, B");
        container.getObjectsNST().insertRow("13, 5, A");
        container.getObjectsNST().insertRow("14, 5, B");
        container.getObjectsNST().insertRow("15, 5, B");
        container.getObjectsNST().insertRow("16, 6, A");
        container.getObjectsNST().insertRow("17, 6, B");
        container.getObjectsNST().insertRow("18, 6, B");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("12,m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("14,f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("15,f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("17,f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("18,f");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("4, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("5, +");
        container.getSubgraphAttrs().getAttrDataNST("label").insertRow("6, +");
    }

    private void verifyLevelTwoTree(RPT rpt) {
        RPTNode rootNode = rpt.getRootNode();

        rpt.print();
        assertNotNull(rootNode);
        assertTrue("nop([A.age])=40".equals(rootNode.split.toString()));
        assertEquals(0.6667, rootNode.getClassLabelDistribution().getProbability("+"), 0.0001);
        assertEquals(0.3333, rootNode.getClassLabelDistribution().getProbability("-"), 0.0001);

        assertNotNull(rootNode.yesBranch);
        assertEquals("+", ((DiscreteProbDistribution) rootNode.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNull(rootNode.yesBranch.split);

        assertNotNull(rootNode.noBranch);
        assertEquals(0.5, rootNode.noBranch.getClassLabelDistribution().getProbability("+"), 0.0001);
        assertEquals(0.5, rootNode.noBranch.getClassLabelDistribution().getProbability("-"), 0.0001);
        log.info(rootNode.noBranch.split.toString());
        assertTrue("count([B.gender]=f)>=1".equals(rootNode.noBranch.split.toString()));
        assertNotNull(rootNode.noBranch.yesBranch);
        assertEquals("-", ((DiscreteProbDistribution) rootNode.noBranch.yesBranch.getClassLabelDistribution()).getHighestProbabilityValue());
        assertNotNull(rootNode.noBranch.noBranch);
        assertNull(rootNode.noBranch.noBranch.split);
        assertEquals("+", ((DiscreteProbDistribution) rootNode.noBranch.noBranch.getClassLabelDistribution()).getHighestProbabilityValue());
    }
}
