/**
 * $Id: PredictionsTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: PredictionsTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.predictions;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.model2.common.probdistributions.ContinuousProbDistribution;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import org.apache.log4j.Logger;

public class PredictionsTest extends TestCase {

    static Logger log = Logger.getLogger(PredictionsTest.class);

    Predictions predictions;

    protected void setUp() throws Exception {
        super.setUp();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        predictions = new Predictions();
        predictions.setTrueLabel("1", "+");
        predictions.setTrueLabel("2", "+");
        predictions.setTrueLabel("3", "+");
        predictions.setTrueLabel("4", "-");
    }


    public void testRMSE() {
        predictions = new Predictions();
        predictions.setTrueLabel("1", "1.0");
        predictions.setTrueLabel("2", "3.0");
        predictions.setTrueLabel("3", "2.0");
        predictions.setTrueLabel("4", "1.0");

        ContinuousProbDistribution c1 = new ContinuousProbDistribution();
        ContinuousProbDistribution c2 = new ContinuousProbDistribution();

        c1.addAttributeValue("1.0", 1.0).addAttributeValue("3.0", 1.0);
        c2.addAttributeValue("2.0", 1.0).addAttributeValue("1.0", 1.0);

        predictions.setPrediction("1", c1);
        predictions.setPrediction("2", c1);

        predictions.setPrediction("3", c2);
        predictions.setPrediction("4", c2);

        // x = FP / total neg, y = TP / total pos
        assertEquals(0.790569415, predictions.getRMSE(), 0.00001);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testAUC() {
        predictions = new Predictions();
        predictions.setTrueLabel("1", "+");
        predictions.setTrueLabel("2", "-");
        predictions.setPrediction("1",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 1.0));
        predictions.setPrediction("2",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 2.0));

        // x = FP / total neg, y = TP / total pos
        double[][] actPoints = predictions.genRocPoints("+");
        double[][] expectedPoints = {{0.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}};
        assertROCArraysEqual(expectedPoints, actPoints);
    }

    public void testConditionalLogLikelihood() {
        assertEquals(0.0, predictions.getConditionalLogLikelihood(), 0.0001); // no predictions

        predictions.setPrediction("1",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0));
        assertEquals(Math.log(1.0), predictions.getConditionalLogLikelihood(), 0.0001); // the prob of the true value is 1.0

        predictions.setPrediction("2",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 4.0).addAttributeValue("-", 4.0));
        assertEquals(Math.log(1.0) + Math.log(0.5),
                predictions.getConditionalLogLikelihood(), 0.0001); // one true value has prob 1.0, the other 0.5

        // one true value has prob 1.0, the other 0.5, and the other 0.0, which is ignored
        predictions.setPrediction("3",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("-", 4.0));
        assertEquals(Math.log(1.0) + Math.log(0.5),
                predictions.getConditionalLogLikelihood(), 0.0001);
    }


    public void testgetInferredClass() {
        predictions.setPrediction("1",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 2.0));
        predictions.setPrediction("2",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 4.0));
        predictions.setPrediction("3",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 4.0));

        assertEquals("+", predictions.getInferredClass("1"));
        assertEquals("-", predictions.getInferredClass("2"));
        assertEquals("-", predictions.getInferredClass("3"));
    }

    public void testZeroOneLoss() {
        assertEquals(-99.0, predictions.getZeroOneLoss(), 0.0001); // no predictions

        predictions.setPrediction("1",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 2.0));
        assertEquals(0.0, predictions.getZeroOneLoss(), 0.0001); // the only prediction is correct

        predictions.setPrediction("2",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 4.0));
        assertEquals(0.5, predictions.getZeroOneLoss(), 0.0001); // one is good and the other is bad

        predictions.setPrediction("3",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 4.0));
        assertEquals(0.6666, predictions.getZeroOneLoss(), 0.0001); // one is good and two are bad

        predictions.setPrediction("5",
                (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValue("+", 3.0).addAttributeValue("-", 4.0));
        assertEquals(0.6666, predictions.getZeroOneLoss(), 0.0001); // an unknown subgID doesn't make a difference
    }

    private void assertROCArraysEqual(double[][] expectedPoints, double[][] actPoints) {
        assertEquals(expectedPoints.length, actPoints.length);
        for (int i = 0; i < expectedPoints.length; i++) {
            double[] exp = expectedPoints[i];
            double[] act = actPoints[i];
            assertEquals(exp[0], act[0], 0.0001);
            assertEquals(exp[1], act[1], 0.0001);
        }
    }
}
