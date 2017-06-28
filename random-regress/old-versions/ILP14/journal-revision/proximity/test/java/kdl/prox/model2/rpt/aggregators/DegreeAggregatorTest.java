/**
 * $Id: DegreeAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DegreeAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.aggregators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.ItemSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class DegreeAggregatorTest extends TestCase {

    static Logger log = Logger.getLogger(DegreeAggregatorTest.class);

    Container container;
    private NSTCache cache;

    protected void setUp() throws Exception {
        super.setUp();

        Aggregator.setNumThresholds(2);
        cache = new NSTCache();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 1, B");
        container.getObjectsNST().insertRow("4, 1, B");
        container.getObjectsNST().insertRow("5, 1, C");
        container.getObjectsNST().insertRow("6, 2, A");
        container.getObjectsNST().insertRow("7, 2, B");
        container.getObjectsNST().insertRow("8, 2, B");
        container.getObjectsNST().insertRow("9, 2, C");
        container.getObjectsNST().insertRow("10, 3, C");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        cache.clear();
        TestUtil.closeTestConnection();
    }


    public void testIt() {
        Source source = new ItemSource("B").init(container, cache);
        log.info(new DegreeAggregator(source).computeTables(cache));
        AggregatorTestUtil.verifyValues(cache, new DegreeAggregator(source).computeTables(cache),
                new String[]{
                        "degree([B])>=2",
                        "degree([B])>=0"},
                new String[][]{
                        {"1@0.true", "2@0.true", "3@0.false"},
                        {"1@0.true", "2@0.true", "3@0.true"}});
    }

}
