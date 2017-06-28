/**
 * $Id: ItemSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ItemSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.sources;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.util.NSTCache;

/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class ItemSourceTest extends TestCase {

    Container container;
    NSTCache cache = new NSTCache();

    protected void setUp() throws Exception {
        super.setUp();
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
        TestUtil.closeTestConnection();
    }


    public void testGetSourceTable() {
        Source source = new ItemSource("B").init(container, cache);
        assertEquals(5, source.getSourceTable().getRowCount());
    }

    public void testGetValues() {
        Source source = new ItemSource("B").init(container, cache);
        assertEquals(1, source.getDistinctValues().size());
        assertTrue(source.getDistinctValues().contains("B"));
    }

    public void testGetSubgraph() {
        Source source = new ItemSource("B").init(container, cache);
        assertEquals(3, source.getDistinctSubgraphs().getRowCount()); // must contain an entry for subg 3, with no Bs
    }

}
