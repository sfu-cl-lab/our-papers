/**
 * $Id: InitFromRPTPredictionsTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: InitFromRPTPredictionsTest.java 3658 2007-10-15 16:29:11Z schapira $
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

public class InitFromRPTPredictionsTest extends TestCase {

    static Logger log = Logger.getLogger(InitFromRPTPredictionsTest.class);

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
        // clear values
        NST movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName()).deleteRows();
        NST studioLabelNST = DB.getObjectAttrs().getAttrDataNST(studioRPTree.getClassLabel().getAttrName()).deleteRows();

        // and now apply using the original RPTs
        HashMap initializers = new HashMap();
        initializers.put(studioRPTree, studioRPTree);
        initializers.put(movieRPTree, movieRPTree);
        InitFromRPTPredictionsModule initModule = new InitFromRPTPredictionsModule(initializers);
        initModule.startup(modelsToTestContainers);
        initModule.seedLabels();

        // at the end, the tables need to look like the original tables
        movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName());
        studioLabelNST = DB.getObjectAttrs().getAttrDataNST(studioRPTree.getClassLabel().getAttrName());

        // the results depend on chance. Test all possibilities.
        // Things are set for movie RPT when id = 1, 3, 4, 5 (they all have year = 94 or 95)
        // for the other ids, make sure they are defined, and that the values are correct
        TestUtil.verifyCollections(new String[]{"1@0.jan", "3@0.jan", "4@0.feb", "5@0.feb"}, movieLabelNST.filter("id != 6 AND id != 2"));

        // Studio 7 and 8 can either be red or blue --no influences between RPTs here, since it's only init time
        List<String> studio7Color = studioLabelNST.selectRows("id = 7", "value").toStringList("value");
        List<String> studio8Color = studioLabelNST.selectRows("id = 8", "value").toStringList("value");
        assertEquals(1, studio7Color.size());
        assertEquals(1, studio8Color.size());
        assertTrue(studio7Color.get(0).equals("red") || studio7Color.get(0).equals("blue"));
        assertTrue(studio8Color.get(0).equals("red") || studio8Color.get(0).equals("blue"));

        // Month 2 and 6 can either be jan or feb --no influences between RPTs here, since it's only init time
        List<String> movie2Month = movieLabelNST.selectRows("id = 2", "value").toStringList("value");
        List<String> movie6Month = movieLabelNST.selectRows("id = 6", "value").toStringList("value");
        assertEquals(1, movie2Month.size());
        assertEquals(1, movie6Month.size());
        assertTrue(movie2Month.get(0).equals("jan") || movie2Month.get(0).equals("feb"));
        assertTrue(movie6Month.get(0).equals("jan") || movie6Month.get(0).equals("feb"));
    }

}
