/**
 * $Id: StochasticModeEstimatorTest.java 3658 2007-10-15 16:29:11Z schapira $
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

public class StochasticModeEstimatorTest extends TestCase {

    private static Logger log = Logger.getLogger(StochasticModeEstimatorTest.class);
    private static double tolerance = 0.000000001;

    private NST trainNST;
    private Estimator estimator = new ModeEstimator();


    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        DB.beginScope();

        trainNST = new NST("subg_id, class, value", "oid, str, str");
        trainNST.insertRow("0, +, a"); // only have one per subgraph, since they're going to be picked randomly
        trainNST.insertRow("0, +, a");
        trainNST.insertRow("0, +, a");
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
        assertEquals(2, allDistr.getDistinctValues().length);
        assertEquals(1.0, allDistr.getCount("a"));
        assertEquals(1.0, allDistr.getCount("d"));


        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        assertEquals(1, posDistr.getDistinctValues().length);
        assertEquals(1.0, posDistr.getCount("a"));

        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);
        assertEquals(1, negDistr.getDistinctValues().length);
        assertEquals(1.0, negDistr.getCount("d"));
    }

    public void testGetSmoothedProbs() {
        NST testNST = new NST("subg_id, value", "oid, str");
        testNST.insertRow("0, a"); // ==> 1 + , 0.01 - (0.01 smoothed for unseen a/d, over 1 observed values)
        testNST.insertRow("1, d"); // ==> 1.00 -,  0.01   + (0.01 smoothed for unseed, over 1 observed value)

        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);

        HashMap<String, Double> posProbs = estimator.getSmoothedProbsGivenAttribute(testNST, posDistr, 1);
        HashMap<String, Double> negProbs = estimator.getSmoothedProbsGivenAttribute(testNST, negDistr, 1);

        assertEquals(1.00, posProbs.get("0@0"), tolerance);
        assertEquals(0.01, posProbs.get("1@0"), tolerance);
        assertEquals(0.01, negProbs.get("0@0"), tolerance);
        assertEquals(1.0, negProbs.get("1@0"), tolerance);
    }

    public void testGetSmoothedProbsWithMoreRows() {
        NST testNST = new NST("subg_id, value", "oid, str");
        testNST.insertRow("0, b"); // 0.01 + , 0.01 -
        testNST.insertRow("0, b");
        testNST.insertRow("0, b");
        testNST.insertRow("1, d"); // ==> 1.00 -,  0.01


        DiscreteProbDistribution posDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '+'"), posDistr);
        DiscreteProbDistribution negDistr = new DiscreteProbDistribution();
        estimator.recordObservations(trainNST.filter("class = '-'"), negDistr);

        HashMap<String, Double> posProbs = estimator.getSmoothedProbsGivenAttribute(testNST, posDistr, 1);
        HashMap<String, Double> negProbs = estimator.getSmoothedProbsGivenAttribute(testNST, negDistr, 1);

        assertEquals(0.01, posProbs.get("0@0"), tolerance);
        assertEquals(0.01, posProbs.get("1@0"), tolerance);
        assertEquals(0.01, negProbs.get("0@0"), tolerance);
        assertEquals(1.0, negProbs.get("1@0"), tolerance);
    }

}
