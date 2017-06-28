/**
 * $Id: SumAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: SumAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.aggregators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class SumAggregatorTest extends TestCase {

    static Logger log = Logger.getLogger(SumAggregatorTest.class);

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
        container.getObjectsNST().insertRow("4, 2, A");
        container.getObjectsNST().insertRow("5, 2, B");
        container.getObjectsNST().insertRow("6, 2, B");
        container.getObjectsNST().insertRow("7, 3, A");
        container.getObjectsNST().insertRow("8, 3, B");
        container.getObjectsNST().insertRow("9, 3, B");

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("height", "flt");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("2, 5.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("3, 6.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("5, 5.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("6, 6.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("8, 6.4");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("9, 7.4");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        cache.clear();
        TestUtil.closeTestConnection();
    }


    public void testFlt() {
        Source source = new AttributeSource("B", "height").init(container, cache);
        AggregatorTestUtil.verifyValues(cache, new SumAggregator(source).computeTables(cache),
                new String[]{
                        "sum([B.height])>=12",
                        "sum([B.height])>=11.1800003"},
                new String[][]{
                        {"1@0.true", "2@0.false", "3@0.true"},
                        {"1@0.true", "2@0.true", "3@0.true"}});
    }

}
