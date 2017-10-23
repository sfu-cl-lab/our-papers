/**
 * $Id: GUIContentGeneratorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.gui2;

import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;

/**
 * GUIContentGeneratorTest
 * User: mhay
 * Email: mhay@cs.umass.edu
 * Date: Apr 2, 2004 11:10:22 AM
 */
public class GUIContentGeneratorTest extends TestCase {

    private Container c1;
    private Container c2;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.insertObject(0);
        DB.insertObject(1);
        DB.insertLink(0, 0, 1);

        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
        c1 = rootContainer.createChild("c1");
        int subgId = 1;
        Subgraph subgraph = c1.getSubgraph(subgId);
        subgraph.insertObject(0, "obj 0 from c1:s1");
        subgraph.insertObject(1, "obj 0 from c1:s1");  // duplicate name
        subgraph.insertObject(1, "obj 1 from c1:s1");
        subgraph.insertObject(0, "obj 0 from c1:s1 again");
        subgraph.insertLink(0, "link 0");
        subgraph.insertLink(0, "link 0 again");

        subgId = 2;
        subgraph = c1.getSubgraph(subgId);
        subgraph.insertObject(0, "obj 0 from c1:s2");

        c2 = rootContainer.createChild("c2");
        subgId = 1;
        subgraph = c2.getSubgraph(subgId);
        subgraph.insertObject(0, "obj 0 from c2:s1");

        new GUIContentGenerator();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testGetUniqueNamesInContainer() {
        // The correct answer is the set of unique names across all subgraphs
        // in the container
        Set correctNames = new HashSet();
        correctNames.add("obj 0 from c1:s1");
        correctNames.add("obj 1 from c1:s1");
        correctNames.add("obj 0 from c1:s1 again");
        correctNames.add("obj 0 from c1:s2");

        ProxURL containerURL = new ProxURL("subg:/containers/c1");
        Container container = containerURL.getContainer(false);
        String[] names = GUIContentGenerator.getUniqueNamesInContainer(container);
        TestUtil.verifyCollections(names, correctNames);
    }


}
