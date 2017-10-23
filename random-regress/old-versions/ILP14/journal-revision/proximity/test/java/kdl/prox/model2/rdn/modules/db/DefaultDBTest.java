/**
 * $Id: DefaultDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DefaultDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn.modules.db;

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

public class DefaultDBTest extends TestCase {

    static Logger log = Logger.getLogger(DefaultDBTest.class);

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


    public void testDB() {
        DefaultDBModule dbModule = new DefaultDBModule();
        dbModule.startup(modelsToTestContainers);

        // update once, set all to the same
        HashMap<String, String> firstClasses = new HashMap<String, String>();
        firstClasses.put("1", "feb");
        firstClasses.put("2", "feb");
        firstClasses.put("3", "feb");
        firstClasses.put("4", "feb");
        firstClasses.put("5", "feb");
        firstClasses.put("6", "feb");
        dbModule.update(movieRPTree, firstClasses, 1);
        NST movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName());
        List valueList = movieLabelNST.filter("value DISTINCT ROWS").selectRows("value").toStringList(1);
        assertEquals(1, valueList.size());
        assertEquals("feb", valueList.get(0));

        // update again. Change 2 and 3, verify that we have correct values
        HashMap<String, String> secondClasses = new HashMap<String, String>();
        secondClasses.put("1", "feb");
        secondClasses.put("2", "jan");
        secondClasses.put("3", "jan");
        secondClasses.put("4", "feb");
        secondClasses.put("5", "feb");
        secondClasses.put("6", "feb");
        dbModule.update(movieRPTree, secondClasses, 1);
        movieLabelNST = DB.getObjectAttrs().getAttrDataNST(movieRPTree.getClassLabel().getAttrName());
        TestUtil.verifyCollections(
                new String[]{"1@0.feb", "2@0.jan", "3@0.jan", "4@0.feb", "5@0.feb", "6@0.feb"},
                movieLabelNST);
    }

}
