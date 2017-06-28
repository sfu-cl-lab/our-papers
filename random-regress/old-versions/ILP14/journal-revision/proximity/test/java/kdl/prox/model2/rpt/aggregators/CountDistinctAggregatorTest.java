/**
 * $Id: CountDistinctAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CountDistinctAggregatorTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.aggregators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.monet.Connection;
import org.apache.log4j.Logger;

/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class CountDistinctAggregatorTest extends TestCase {

    static Logger log = Logger.getLogger(CountDistinctAggregatorTest.class);

    Container container;
    private NSTCache cache;

    protected void setUp() throws Exception {
        super.setUp();

        cache = new NSTCache();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        Connection.beginScope();

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
        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("3, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("5, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("8, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("9, f");

        DB.getObjectAttrs().defineAttribute("age", "flt");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("1, 1");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("4, 2");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("7, 0");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }


    public void testString() {
        Source source = new AttributeSource("B", "gender").init(container, cache);
        AggregatorTestUtil.verifyValues(cache, new CountDistinctAggregator(source).computeTables(cache),
                new String[]{
                        "count_distinct([B.gender])>=1",
                        "count_distinct([B.gender])>=2"},
                new String[][]{
                        {"1@0.true", "2@0.true", "3@0.true"},
                        {"1@0.false", "2@0.true", "3@0.false"}});
    }

}
