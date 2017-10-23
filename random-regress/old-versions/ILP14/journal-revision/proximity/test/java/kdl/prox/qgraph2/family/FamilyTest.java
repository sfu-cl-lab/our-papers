/**
 * $Id: FamilyTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: FamilyTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.family;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.Iterator;
import java.util.List;


public class FamilyTest extends TestCase {

    private static Logger log = Logger.getLogger(FamilyTest.class);

    protected void setUp() {
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void takeDown() {
        TestUtil.closeTestConnection();
    }

    public void testFamilies() throws Exception {
        URL queryFamilyDirURL = getClass().getResource("data/families/");
        List<QueryTestFailure> failureList = FamilyTestApp.testAllFamilies(queryFamilyDirURL.getPath(), new QGraphQueryTester());

        // print failures
        if (failureList.size() != 0) {
            log.info("* families result summary");
            Iterator failureListIterator = failureList.iterator();
            while (failureListIterator.hasNext()) {
                log.warn(failureListIterator.next().toString());
            }
        }

        assertEquals(0, failureList.size());
    }

}
