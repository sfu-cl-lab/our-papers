/**
 * $Id: DefaultInitTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DefaultInitTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn.modules.init;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rdn.RDNTestUtil;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultInitTest extends TestCase {

    static Logger log = Logger.getLogger(DefaultInitTest.class);

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


    public void testInit() {
        // clear the attributes
        NST movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName()).deleteRows();
        NST studioLabelNST = DB.getObjectAttrs().getAttrDataNST(studioRPTree.getClassLabel().getAttrName()).deleteRows();

        // and now apply using the original RPTs
        DefaultInitModule initModule = new DefaultInitModule();
        initModule.startup(modelsToTestContainers);
        initModule.seedLabels();

        // at the end, the tables need to only have valid options
        movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName());
        List movieLabels = movieLabelNST.filter("value DISTINCT ROWS").selectRows("value").toStringList(1);
        if (movieLabels.size() == 1) {
            assertTrue("jan".equals(movieLabels.get(0)) || "feb".equals(movieLabels.get(0)));
        } else if (movieLabels.size() == 2) {
            assertTrue("jan".equals(movieLabels.get(0)) || "feb".equals(movieLabels.get(0)));
            assertTrue("jan".equals(movieLabels.get(1)) || "feb".equals(movieLabels.get(1)));
        } else {
            fail("More than two values found. Should be at most 'jan' or 'feb'");
        }

        studioLabelNST = DB.getObjectAttrs().getAttrDataNST(studioRPTree.getClassLabel().getAttrName());
        List studioLabels = studioLabelNST.filter("value DISTINCT ROWS").selectRows("value").toStringList(1);
        if (studioLabels.size() == 1) {
            assertTrue("red".equals(studioLabels.get(0)) || "blue".equals(studioLabels.get(0)));
        } else if (studioLabels.size() == 2) {
            assertTrue("red".equals(studioLabels.get(0)) || "blue".equals(studioLabels.get(0)));
            assertTrue("red".equals(studioLabels.get(1)) || "blue".equals(studioLabels.get(1)));
        } else {
            fail("More than two values found. Should be at most 'blue' or 'red'");
        }
    }

}
