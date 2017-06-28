/**
 * $Id: DefaultStatisticTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DefaultStatisticTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn.modules.statistic;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rdn.RDNTestUtil;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DefaultStatisticTest extends TestCase {

    static Logger log = Logger.getLogger(DefaultStatisticTest.class);

    private RPT movieRPTree;
    private RPT studioRPTree;
    private Map modelsToTestContainers;

    public void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        RDNTestUtil.createDB();
        movieRPTree = RDNTestUtil.createMovieRPT();
        studioRPTree = RDNTestUtil.createStudioRPT();

        modelsToTestContainers = new HashMap();
        modelsToTestContainers.put(studioRPTree, DB.getContainer(RDNTestUtil.STUDIO_TEST_CONT_NAME));
        modelsToTestContainers.put(movieRPTree, DB.getContainer(RDNTestUtil.MOVIE_TEST_CONT_NAME));
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testStat() {
        DefaultStatisticModule statModule = new DefaultStatisticModule(1, 1);
        statModule.startup(modelsToTestContainers);

        // the first update should be ignored
        HashMap<String, String> classes = new HashMap<String, String>();
        classes.put("1", "feb");
        classes.put("2", "feb");
        classes.put("3", "feb");
        classes.put("4", "feb");
        classes.put("5", "feb");
        classes.put("6", "feb");
        statModule.recordPrediction(movieRPTree, classes, 0);
        Predictions predictions = statModule.getPredictions().get(movieRPTree);
        assertEquals(0, predictions.size());

        // now it should update
        classes.put("1", "feb");
        classes.put("2", "jan");
        classes.put("3", "jan");
        classes.put("4", "feb");
        classes.put("5", "feb");
        classes.put("6", "feb");
        statModule.recordPrediction(movieRPTree, classes, 1);
        predictions = statModule.getPredictions().get(movieRPTree);
        assertEquals(6, predictions.size());
        assertEquals(1.0, predictions.getProbDistribution("1").getCount("feb"));
        assertEquals(0.0, predictions.getProbDistribution("2").getCount("feb"));
        assertEquals(1.0, predictions.getProbDistribution("4").getCount("feb"));

        // this again should be ignored (step = 1)
        classes.put("1", "feb");
        classes.put("2", "jan");
        classes.put("3", "jan");
        classes.put("4", "feb");
        classes.put("5", "feb");
        classes.put("6", "feb");
        statModule.recordPrediction(movieRPTree, classes, 2);
        predictions = statModule.getPredictions().get(movieRPTree);
        assertEquals(6, predictions.size());
        assertEquals(1.0, predictions.getProbDistribution("1").getCount("feb"));
        assertEquals(0.0, predictions.getProbDistribution("2").getCount("feb"));
        assertEquals(1.0, predictions.getProbDistribution("4").getCount("feb"));

        // and this one should be recorded
        classes.put("1", "feb");
        classes.put("2", "feb");
        classes.put("3", "jan");
        classes.put("4", "feb");
        classes.put("5", "feb");
        classes.put("6", "feb");
        statModule.recordPrediction(movieRPTree, classes, 3);
        predictions = statModule.getPredictions().get(movieRPTree);
        assertEquals(6, predictions.size());
        assertEquals(2.0, predictions.getProbDistribution("1").getCount("feb"));
        assertEquals(1.0, predictions.getProbDistribution("2").getCount("feb"));
        assertEquals(1.0, predictions.getProbDistribution("2").getCount("jan"));
        assertEquals(2.0, predictions.getProbDistribution("4").getCount("feb"));
    }

    public void testRecord() {
        DefaultStatisticModule statModule = new DefaultStatisticModule(0, 0);
        assertTrue(statModule.isRecordIteration(0));
        assertTrue(statModule.isRecordIteration(1));
        assertTrue(statModule.isRecordIteration(2));
        assertTrue(statModule.isRecordIteration(3));
        assertTrue(statModule.isRecordIteration(4));
        assertTrue(statModule.isRecordIteration(5));

        // A different pair of burnIn and skipSteps
        statModule = new DefaultStatisticModule(1, 1);
        assertFalse(statModule.isRecordIteration(0));
        assertTrue(statModule.isRecordIteration(1));
        assertFalse(statModule.isRecordIteration(2));
        assertFalse(statModule.isRecordIteration(2)); // calling twice shouldn't affect it!
        assertTrue(statModule.isRecordIteration(3));
        assertTrue(statModule.isRecordIteration(3)); // calling twice shouldn't affect it!
        assertFalse(statModule.isRecordIteration(4));
        assertTrue(statModule.isRecordIteration(5));

        statModule = new DefaultStatisticModule(1, 0);
        assertFalse(statModule.isRecordIteration(0));
        assertTrue(statModule.isRecordIteration(1));
        assertTrue(statModule.isRecordIteration(2));
        assertTrue(statModule.isRecordIteration(3));
        assertTrue(statModule.isRecordIteration(4));
        assertTrue(statModule.isRecordIteration(5));


        statModule = new DefaultStatisticModule(1, 2);
        assertFalse(statModule.isRecordIteration(0));
        assertTrue(statModule.isRecordIteration(1));
        assertFalse(statModule.isRecordIteration(2));
        assertFalse(statModule.isRecordIteration(3));
        assertTrue(statModule.isRecordIteration(4));
        assertFalse(statModule.isRecordIteration(5));
        assertFalse(statModule.isRecordIteration(6));
        assertTrue(statModule.isRecordIteration(7));

    }

}
