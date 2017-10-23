/**
 * $Id: RDNTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RDNTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.rdn.modules.listeners.LoggingListener;
import kdl.prox.model2.rdn.modules.statistic.DefaultStatisticModule;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RDNTest extends TestCase {

    static Logger log = Logger.getLogger(RDNTest.class);

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

    public void testRDN() {
        final RDN rdn = new RDN();
        rdn.statisticModule = new DefaultStatisticModule(0, 0);
        rdn.addListener(new LoggingListener());
        final Map<RPT, Predictions> predictionMap = rdn.apply(modelsToTestContainers, 10);

        //get predictionMap and check that they're in the right ballpark
        Predictions moviePredictions = predictionMap.get(movieRPTree);
        log.info("predictionMap for movies " + moviePredictions);
        assertEquals("jan", moviePredictions.getInferredClass("1@0"));
        assertEquals("jan", moviePredictions.getInferredClass("2@0"));
        assertEquals("jan", moviePredictions.getInferredClass("3@0"));
        assertEquals("feb", moviePredictions.getInferredClass("4@0"));
        assertEquals("feb", moviePredictions.getInferredClass("5@0"));
        assertEquals("feb", moviePredictions.getInferredClass("6@0"));

        Predictions studioPredictions = predictionMap.get(studioRPTree);
        log.info("predictionMap for studios " + studioPredictions);
        assertEquals("red", studioPredictions.getInferredClass("10@0"));
        assertEquals("blue", studioPredictions.getInferredClass("20@0"));
    }

}
