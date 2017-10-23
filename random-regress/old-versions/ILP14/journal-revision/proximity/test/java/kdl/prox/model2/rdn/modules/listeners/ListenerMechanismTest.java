/**
 * $Id: ListenerMechanismTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ListenerMechanismTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn.modules.listeners;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.rdn.RDN;
import kdl.prox.model2.rdn.RDNTestUtil;
import kdl.prox.model2.rdn.modules.statistic.DefaultStatisticModule;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ListenerMechanismTest extends TestCase {

    static Logger log = Logger.getLogger(ListenerMechanismTest.class);

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

    public void testListener() {
        final RDN rdn = new RDN();
        rdn.statisticModule = new DefaultStatisticModule(0, 0);

        RDNListenerTester listener1 = new RDNListenerTester();
        RDNListenerTester listener2 = new RDNListenerTester();
        rdn.addListener(listener1);
        rdn.addListener(listener2);

        assertEquals(0, listener1.numStartupCalls);
        assertEquals(0, listener2.numStartupCalls);
        assertEquals(0, listener1.numCycleCalls);
        assertEquals(0, listener2.numCycleCalls);
        assertEquals(0, listener1.numCleanupCalls);
        assertEquals(0, listener2.numCleanupCalls);

        rdn.apply(modelsToTestContainers, 2);

        assertEquals(1, listener1.numStartupCalls);
        assertEquals(1, listener2.numStartupCalls);
        assertEquals(2, listener1.numCycleCalls);
        assertEquals(2, listener2.numCycleCalls);
        assertEquals(1, listener1.numCleanupCalls);
        assertEquals(1, listener2.numCleanupCalls);

    }

    static class RDNListenerTester implements RDNListenerModule {
        public int numStartupCalls = 0;
        public int numCycleCalls = 0;
        public int numCleanupCalls = 0;

        public void startup(Map<RPT, Container> modelsConts) {
            numStartupCalls++;
        }

        public void cycle(int iteration) {
            numCycleCalls++;
        }

        public void cleanup() {
            numCleanupCalls++;
        }
    }

}
