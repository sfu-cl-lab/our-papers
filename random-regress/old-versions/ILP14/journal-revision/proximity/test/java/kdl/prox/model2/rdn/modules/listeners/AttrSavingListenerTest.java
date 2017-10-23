/**
 * $Id: AttrSavingListenerTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttrSavingListenerTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn.modules.listeners;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.model2.rdn.RDN;
import kdl.prox.model2.rdn.RDNTestUtil;
import kdl.prox.model2.rdn.modules.statistic.DefaultStatisticModule;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class AttrSavingListenerTest extends TestCase {

    static Logger log = Logger.getLogger(AttrSavingListenerTest.class);

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

        String prefix = "test_attrs";
        rdn.addListener(new AttrSavingListener(prefix));
        rdn.apply(modelsToTestContainers, 10);

        assertTrue(DB.getObjectAttrs().isAttributeDefined(prefix + "_0_rdn_temp_" + movieRPTree.getClassLabel().getAttrName()));
        assertTrue(DB.getObjectAttrs().isAttributeDefined(prefix + "_5_rdn_temp_" + movieRPTree.getClassLabel().getAttrName()));
        assertTrue(DB.getObjectAttrs().isAttributeDefined(prefix + "_0_rdn_temp_" + studioRPTree.getClassLabel().getAttrName()));
        assertTrue(DB.getObjectAttrs().isAttributeDefined(prefix + "_5_rdn_temp_" + studioRPTree.getClassLabel().getAttrName()));


    }

}
