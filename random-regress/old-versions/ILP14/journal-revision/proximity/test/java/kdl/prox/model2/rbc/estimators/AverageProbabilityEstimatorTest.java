/**
 * $Id: AverageProbabilityEstimatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc.estimators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class AverageProbabilityEstimatorTest extends TestCase {

    private static Logger log = Logger.getLogger(AverageProbabilityEstimatorTest.class);
    private static double tolerance = 0.000000001;

    private NST trainNST;
    private Estimator estimator = new AverageProbabilityEstimator();


    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        DB.beginScope();

        trainNST = new NST("subg_id, class, value", "oid, str, str");
        trainNST.insertRow("0, +, a");
        trainNST.insertRow("0, +, b");
        trainNST.insertRow("0, +, b");
        trainNST.insertRow("0, +, c");
        trainNST.insertRow("1, -, d");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        DB.endScope();
        TestUtil.closeTestConnection();
    }

    public void testRecord() {
        DiscreteProbDistribution allDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST, allDistr);
        assertEquals(4, allDistr.getDistinctValues().length);
        assertEquals(1.0, allDistr.getCount("a"));
        assertEquals(2.0, allDistr.getCount("b"));
        assertEquals(1.0, allDistr.getCount("c"));
        assertEquals(1.0, allDistr.getCount("d"));


        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        assertEquals(3, posDistr.getDistinctValues().length);
        assertEquals(1.0, posDistr.getCount("a"));
        assertEquals(2.0, posDistr.getCount("b"));
        assertEquals(1.0, posDistr.getCount("c"));

        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);
        assertEquals(1, negDistr.getDistinctValues().length);
        assertEquals(1.0, negDistr.getCount("d"));
    }

    public void testGetSmoothedProbs() {
        NST testNST = new NST("subg_id, value", "oid, str");
        testNST.insertRow("0, a"); // ==> 0.25 + , 0.0025 - (0.01 smoothed for unseen d, over 4 observed values)
        testNST.insertRow("1, d"); // ==> 1.00 -,  0.01   + (0.01 smoothed for unseed, over 1 observed value)

        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);

        HashMap<String, Double> posProbs = estimator.getSmoothedProbsGivenAttribute(testNST, posDistr, 1);
        HashMap<String, Double> negProbs = estimator.getSmoothedProbsGivenAttribute(testNST, negDistr, 1);

        assertEquals(0.25, posProbs.get("0@0"), tolerance);
        assertEquals(0.0025, posProbs.get("1@0"), tolerance);
        assertEquals(0.01, negProbs.get("0@0"), tolerance);
        assertEquals(1.0, negProbs.get("1@0"), tolerance);
    }

    public void testGetSmoothedProbsWithMoreRows() {
        NST testNST = new NST("subg_id, value", "oid, str");
        testNST.insertRow("0, a"); // ==> 0.0625 / 2 + , 0.0025 - (0.01 smoothed for unseen d, over 4 observed values)
        testNST.insertRow("0, a");
        testNST.insertRow("1, d"); // ==> 1.00 -,  0.0001 / 2  + (0.01 smoothed for unseed, over 1 observed value)


        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);

        HashMap<String, Double> posProbs = estimator.getSmoothedProbsGivenAttribute(testNST, posDistr, 1);
        HashMap<String, Double> negProbs = estimator.getSmoothedProbsGivenAttribute(testNST, negDistr, 1);

        assertEquals(0.03125, posProbs.get("0@0"), tolerance);
        assertEquals(0.0025, posProbs.get("1@0"), tolerance);
        assertEquals(0.00005, negProbs.get("0@0"), tolerance);
        assertEquals(1.0, negProbs.get("1@0"), tolerance);
    }

}
