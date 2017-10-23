/**
 * $Id: RBCTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.rbc.estimators.MultinomialEstimator;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;

/**
 */
public class RBCTest extends TestCase {

    private Logger log = Logger.getLogger(RBCTest.class);

    private static String trainContName = "TrainContainer";
    private static String testContName = "TestContainer";
    private static double tolerance = 0.00001;

    private Container trainContainer;


    /**
     * Training instances set up:
     * subg / class label / attrs
     * 1) +     .5
     * 2) +     .6, .7, N
     * 3) +     .3, .01, N, N
     * 4) -     Y, N
     * 5) -
     * 6) -
     * 7) no label  .4
     */
    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // cleanup
        DB.getRootContainer().deleteAllChildren();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getLinkAttrs().deleteAllAttributes();

        // create trainContainer and attributes
        trainContainer = DB.getRootContainer().createChild(trainContName);
        DB.getObjectAttrs().defineAttribute("coreAttr1", "str");
        DB.getObjectAttrs().defineAttribute("coreAttr2", "str"); // will be used as class label in test 4
        DB.getObjectAttrs().defineAttribute("perAttr1", "dbl");
        DB.getLinkAttrs().defineAttribute("relAttr1", "str");

        // create subgraphs
        // +, per: 0.5, rel: ?
        trainContainer.getObjectsNST().insertRow("0, 1, core");
        trainContainer.getObjectsNST().insertRow("1, 1, peripheral");
        trainContainer.getObjectsNST().insertRow("2, 1, peripheral");
        trainContainer.getLinksNST().insertRow("0, 1, relation");
        trainContainer.getLinksNST().insertRow("1, 1, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("0, +");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("0, A");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("1, 0.5");

        // +, per: 0.6, 0.7, rel: N
        trainContainer.getObjectsNST().insertRow("3, 2, core");
        trainContainer.getObjectsNST().insertRow("4, 2, peripheral");
        trainContainer.getObjectsNST().insertRow("5, 2, peripheral");
        trainContainer.getLinksNST().insertRow("2, 2, relation");
        trainContainer.getLinksNST().insertRow("3, 2, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("3, +");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("3, B");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("4, 0.6");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("4, 0.7");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("3, N");

        // + per: 0.3, 0.01, rel: N
        trainContainer.getObjectsNST().insertRow("6, 3, core");
        trainContainer.getObjectsNST().insertRow("7, 3, peripheral");
        trainContainer.getObjectsNST().insertRow("8, 3, peripheral");
        trainContainer.getObjectsNST().insertRow("9, 3, peripheral");
        trainContainer.getLinksNST().insertRow("4, 3, relation");
        trainContainer.getLinksNST().insertRow("5, 3, relation");
        trainContainer.getLinksNST().insertRow("6, 3, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("6, +");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("6, C");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("7, 0.3");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("8, 0.01");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("6, N");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("6, N");

        // -, per: ?, rel: Y, N
        trainContainer.getObjectsNST().insertRow("10, 4, core");
        trainContainer.getObjectsNST().insertRow("11, 4, peripheral");
        trainContainer.getObjectsNST().insertRow("12, 4, peripheral");
        trainContainer.getObjectsNST().insertRow("13, 4, peripheral");
        trainContainer.getLinksNST().insertRow("7, 4, relation");
        trainContainer.getLinksNST().insertRow("8, 4, relation");
        trainContainer.getLinksNST().insertRow("9, 4, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("10, -");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("10, B");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("8, Y");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("9, N");

        // -
        trainContainer.getObjectsNST().insertRow("14, 5, core");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("14, -");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("14, A");

        // -, per: ?, rel: ?
        trainContainer.getObjectsNST().insertRow("15, 6, core");
        trainContainer.getObjectsNST().insertRow("16, 6, peripheral");
        trainContainer.getObjectsNST().insertRow("17, 6, peripheral");
        trainContainer.getObjectsNST().insertRow("18, 6, peripheral");
        trainContainer.getObjectsNST().insertRow("19, 6, peripheral");
        trainContainer.getLinksNST().insertRow("10, 6, relation");
        trainContainer.getLinksNST().insertRow("11, 6, relation");
        trainContainer.getLinksNST().insertRow("12, 6, relation");
        trainContainer.getLinksNST().insertRow("13, 6, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr1").insertRow("15, -");

        // ?, per: 0.4, rel: ?
        trainContainer.getObjectsNST().insertRow("20, 7, core");
        trainContainer.getObjectsNST().insertRow("21, 7, peripheral");
        trainContainer.getLinksNST().insertRow("14, 7, relation");
        DB.getObjectAttrs().getAttrDataNST("coreAttr2").insertRow("20, A");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("21, 0.4");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testLoad() {
        RBC rbc = new RBC().load(getClass().getResource("rbc-test.xml").getFile());
        testCorrectlyLoaded(rbc);
    }

    public void testSave() {
        File inFile = new File(getClass().getResource("rbc-test.xml").getFile());
        File outFile = new File(inFile.getParent(), "test-dump.xml");

        RBC rbc = new RBC().load(inFile.getPath());
        rbc.save(outFile.getPath());

        String expectedFileCont = Util.readStringFromFile(inFile);
        String actualFileCont = Util.readStringFromFile(outFile);
        assertEquals(expectedFileCont, actualFileCont);

        RBC rbc1 = new RBC().load(outFile.getAbsolutePath());
        testCorrectlyLoaded(rbc1);

        outFile.delete();
    }


    public void testRBC1() {
        // test set
        Container testContainer = createTwoSubgTestContainer();

        final double pPos1 = 0.5;
        final double pNeg2 = 0.99337;
        execAndTestRBC(testContainer, pPos1, pNeg2);
    }

    /**
     * Here we add a value for perAttr1 to an instance with a '-' class label
     */
    public void testRBC2() {
        Container testContainer = createTwoSubgTestContainer();

        // add an attribute to the train case (subg 4 in the train container)
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("11, 0.25");

        final double pPos1 = 0.6666214;
        final double pNeg2 = 0.980262;
        execAndTestRBC(testContainer, pPos1, pNeg2);
    }

    /**
     * Here we use one test case with a class label that does not appear in the training data
     * and another with a discrete attribute value that does not appear in training data
     *
     * @
     */
    public void testRBC3() {
        Container testContainer = createTwoSubgTestContainer();

        // add an attribute to the train case
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("11, 0.25");

        // use unknown rel attribute in testing
        DB.getLinkAttrs().getAttrDataNST("relAttr1").replace("id = 111", "value", "'Z'"); // instead of Y

        final double pPos1 = 0.6666214;
        final double pNeg2 = 0.3318407;
        execAndTestRBC(testContainer, pPos1, pNeg2);
    }

    /**
     * Here we use coreAttr2 as the class label.
     */
    public void testRBC4() {
        Container testContainer = createTwoSubgTestContainer();

        // add an attribute to the train case
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("11, 0.25");

        // specify class label and attributes
        AttributeSource classLabel = new AttributeSource("core", "coreAttr2");
        ArrayList sourceList = new ArrayList();
        sourceList.add(new AttributeSource("relation", "relAttr1"));
        sourceList.add(new AttributeSource("peripheral", "perAttr1"));

        // run the test
        RBC rbc = new RBC();
        rbc.learn(trainContainer, classLabel, sourceList);
        Predictions predictions = rbc.apply(testContainer);

        // test predictions
        assertEquals(0.483376, predictions.getSubgProbability("1@0", "A"), tolerance);
        assertEquals(0.375148, predictions.getSubgProbability("1@0", "B"), tolerance);
        assertEquals(0.141474, predictions.getSubgProbability("1@0", "C"), tolerance);
//        assertEquals(0.407032, predictions.getSubgProbability("2@0", "A"), tolerance);
//        assertEquals(0.590613, predictions.getSubgProbability("2@0", "B"), tolerance);
        assertEquals(0.002354, predictions.getSubgProbability("2@0", "C"), tolerance);
    }


    /**
     * Test case in which we remove a core item from one of the subgraphs in the test containers
     * Since in this new model we don't really care about the core item in the class label (because, for example, the
     * class label might be a subgraph attribute!), we should be able to infer a label for a subgraph that has at least
     * one of the required items defined
     */
    public void testRBC5() {
        // test set
        Container testContainer = createTwoSubgTestContainer();
        testContainer.getObjectsNST().deleteRows("item_id = 110");

        final double pPos1 = 0.5;
        final double pNeg2 = 0.99337;
        execAndTestRBC(testContainer, pPos1, pNeg2);
    }


    /**
     * Test case in which we remove all items from one of the subgraphs in the test containers. Obviously, there should
     * be no predictions for it.
     */
    public void testRBC6() {
        // test set
        Container testContainer = createTwoSubgTestContainer();
        testContainer.getObjectsNST().deleteRows("subg_id = 2");
        testContainer.getLinksNST().deleteRows("subg_id = 2");

        // specify class label and attributes
        AttributeSource classLabel = new AttributeSource("core", "coreAttr1");
        ArrayList sourceList = new ArrayList();
        sourceList.add(new AttributeSource("relation", "relAttr1"));
        sourceList.add(new AttributeSource("peripheral", "perAttr1"));

        // run the test
        RBC rbc = new RBC();
        rbc.learn(trainContainer, classLabel, sourceList);
        Predictions predictions = rbc.apply(testContainer);

        // test predictions
        assertEquals(0.5, predictions.getSubgProbability("1@0", "+"), tolerance);
        assertEquals(1 - 0.5, predictions.getSubgProbability("1@0", "-"), tolerance);
        assertEquals(null, predictions.getProbDistribution("2@0"));
    }


    /**
     * Two subgraphs
     * <p/>
     * 1: per: 0.5
     * 2: per: 0.3, 0.9, rel: Y
     *
     * @return
     */
    private Container createTwoSubgTestContainer() {
        Container testContainer = DB.getRootContainer().createChild(testContName);

        testContainer.getObjectsNST().insertRow("100, 1, core");
        testContainer.getObjectsNST().insertRow("101, 1, peripheral");
        testContainer.getObjectsNST().insertRow("102, 1, peripheral");
        testContainer.getLinksNST().insertRow("100, 1, relation");
        testContainer.getLinksNST().insertRow("101, 1, relation");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("101, 0.5");

        testContainer.getObjectsNST().insertRow("110, 2, core");
        testContainer.getObjectsNST().insertRow("111, 2, peripheral");
        testContainer.getObjectsNST().insertRow("112, 2, peripheral");
        testContainer.getLinksNST().insertRow("110, 2, relation");
        testContainer.getLinksNST().insertRow("111, 2, relation");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("111, 0.3");
        DB.getObjectAttrs().getAttrDataNST("perAttr1").insertRow("111, 0.9");
        DB.getLinkAttrs().getAttrDataNST("relAttr1").insertRow("111, Y");
        return testContainer;
    }

    private void execAndTestRBC(Container testContainer, double pPos1, double pNeg2) {
        // specify class label and attributes
        AttributeSource classLabel = new AttributeSource("core", "coreAttr1");
        ArrayList sourceList = new ArrayList();
        sourceList.add(new AttributeSource("relation", "relAttr1"));
        sourceList.add(new AttributeSource("peripheral", "perAttr1"));

        // run the test
        RBC rbc = new RBC();
        rbc.learn(trainContainer, classLabel, sourceList);
        Predictions predictions = rbc.apply(testContainer);

        // test predictions
        assertEquals(pPos1, predictions.getSubgProbability("1@0", "+"), tolerance);
        assertEquals(1 - pPos1, predictions.getSubgProbability("1@0", "-"), tolerance);
        assertEquals(1.0 - pNeg2, predictions.getSubgProbability("2@0", "+"), tolerance);
        assertEquals(pNeg2, predictions.getSubgProbability("2@0", "-"), tolerance);
    }

    private void testCorrectlyLoaded(RBC rbc) {
        assertEquals(MultinomialEstimator.class.getName(), rbc.estimatorModule.getClass().getName());

        assertEquals("[core.coreAttr1]", rbc.classLabel.toString());
        assertEquals(0.5, rbc.classDist.getProbability("+"));
        assertEquals(0.5, rbc.classDist.getProbability("-"));

        assertEquals(2, rbc.sourceList.size());
        AttributeSource source1 = rbc.sourceList.get(0);
        AttributeSource source2 = rbc.sourceList.get(1);
        assertEquals("[relation.relAttr1]", source1.toString());
        assertEquals("[peripheral.perAttr1]", source2.toString());

        assertEquals("-:{Y: 1.0, N: 1.0} +:{N: 3.0}", rbc.sourceToCondDistMap.get(source1).toString());
        assertEquals("+:{0.699999988079071: 1.0, 0.009999999776482582: 1.0, 0.30000001192092896: 1.0, 0.6000000238418579: 1.0, 0.5: 1.0}", rbc.sourceToCondDistMap.get(source2).toString());

        assertEquals(true, rbc.isLearned);
    }

}
