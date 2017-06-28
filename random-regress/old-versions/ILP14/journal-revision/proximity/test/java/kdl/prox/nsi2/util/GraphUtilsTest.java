/**
 * $Id: GraphUtilsTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: GraphUtilsTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.nsi2.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.datagen2.structure.SyntheticGraphForestFire;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.nsi2.graph.InMemoryGraph;
import kdl.prox.nsi2.graph.InMemoryWeightedGraph;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: May 21, 2007
 * Time: 4:22:20 PM
 */
public class GraphUtilsTest extends TestCase {

    private static Logger log = Logger.getLogger(GraphUtilsTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildItalianGraph();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testChooseRandomNodes() {
        List<Integer> rands = GraphUtils.chooseRandomNodes(5);
        assertEquals(5, rands.size());
    }

    public void testChooseRandomNodePairs() {
        List<int[]> rands = GraphUtils.chooseRandomNodePairs(3);
        assertEquals(3, rands.size());

        Set<int[]> rands1 = new HashSet<int[]>(GraphUtils.chooseRandomNodePairs(5));
        Set<int[]> rands2 = new HashSet<int[]>(GraphUtils.chooseRandomNodePairs(5));
        rands1.retainAll(rands2);
        assertFalse(rands1.size() == 5); // 1/(210!/5!) chance of erroneously failing.
    }

    public void testFlood() {
        InMemoryGraph inMemoryGraph = new InMemoryGraph(false);
        Map<Integer, Integer> dists = GraphUtils.flood(4, Integer.MAX_VALUE, inMemoryGraph);

        Map<Integer, Integer> expectedDists = new HashMap<Integer, Integer>();
        expectedDists.put(1, 2);
        expectedDists.put(2, 1);
        expectedDists.put(3, 1);
        expectedDists.put(4, 0);
        expectedDists.put(5, 1);
        expectedDists.put(6, 1);
        expectedDists.put(7, 1);
        expectedDists.put(8, 1);
        expectedDists.put(9, 2);
        expectedDists.put(10, 2);
        expectedDists.put(11, 3);
        expectedDists.put(12, 3);
        expectedDists.put(13, 2);
        expectedDists.put(14, 2);
        expectedDists.put(15, 3);
        TestUtil.verifyMap(expectedDists, dists);
    }

    public void testFloodWeighted() {
        InMemoryWeightedGraph inMemoryWeightedGraph = new InMemoryWeightedGraph(false);
        Map<Integer, Double> dists = GraphUtils.floodWeighted(4, inMemoryWeightedGraph);

        Map<Integer, Double> expectedDists = new HashMap<Integer, Double>();
        expectedDists.put(1, 2.0);
        expectedDists.put(2, 1.0);
        expectedDists.put(3, 1.0);
        expectedDists.put(4, 0.0);
        expectedDists.put(5, 4.0); //longer to avoid high weighted link
        expectedDists.put(6, 1.0);
        expectedDists.put(7, 1.0);
        expectedDists.put(8, 1.0);
        expectedDists.put(9, 2.0);
        expectedDists.put(10, 2.0);
        expectedDists.put(11, 3.0);
        expectedDists.put(12, 3.0);
        expectedDists.put(13, 2.0);
        expectedDists.put(14, 3.0); //longer to avoid high weighted link
        expectedDists.put(15, 3.0);
        TestUtil.verifyDoubleMap(expectedDists, dists);
    }

    public void testPrims() {
        DB.clearDB();
        DB.initEmptyDB();
        new SyntheticGraphForestFire(1000, .35, .2);
        GraphUtils.assignLinkWeightsExponential(.1);
        InMemoryWeightedGraph inMemoryWeightedGraph = new InMemoryWeightedGraph(false);
        GraphUtils.prims(inMemoryWeightedGraph);

        InMemoryGraph inMemoryGraph = new InMemoryGraph(false);
        Map<Integer, Map<Integer, Double>> mst = GraphUtils.prims(inMemoryGraph);
        double mstWeight = 0.0;
        for (Integer node : mst.keySet()) {
            for (Double weight : mst.get(node).values()) {
                mstWeight += weight;
            }
        }
        mstWeight /= 2.0;
        assertEquals(999.0, mstWeight, 0.0001);
    }

    private void buildItalianGraph() {
        // test db is taken from Borgatti's "Centrality and Network Flow" (Social Networks 2005)
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getRootContainer().deleteAllChildren();

        for (int i = 1; i <= 15; i++) {
            DB.insertObject(i);
        }

        int linkId = 1;
        DB.insertLink(linkId++, 1, 2);
        DB.insertLink(linkId++, 2, 4);
        DB.insertLink(linkId++, 3, 4);
        DB.insertLink(linkId++, 4, 5);
        DB.insertLink(linkId++, 4, 7);
        DB.insertLink(linkId++, 4, 8);
        DB.insertLink(linkId++, 5, 14);
        DB.insertLink(linkId++, 6, 4);
        DB.insertLink(linkId++, 6, 7);
        DB.insertLink(linkId++, 7, 10);
        DB.insertLink(linkId++, 8, 9);
        DB.insertLink(linkId++, 8, 10);
        DB.insertLink(linkId++, 10, 11);
        DB.insertLink(linkId++, 10, 12);
        DB.insertLink(linkId++, 12, 13);
        DB.insertLink(linkId++, 12, 15);
        DB.insertLink(linkId++, 13, 14);
        DB.insertLink(linkId++, 13, 6);
        DB.insertLink(linkId++, 14, 15);
        DB.insertLink(linkId, 15, 13); // note that this link is nearly invisible in the paper!

        // this is just for reference in the original paper
        DB.getObjectAttrs().defineAttribute("name", "str");
        NST attrDataNST = DB.getObjectAttrs().getAttrDataNST("name"); // id, value
        attrDataNST.insertRow("1,pazzi");
        attrDataNST.insertRow("2,salviati");
        attrDataNST.insertRow("3,acciaiuol");
        attrDataNST.insertRow("4,medici");
        attrDataNST.insertRow("5,barbadori");
        attrDataNST.insertRow("6,ridolfi");
        attrDataNST.insertRow("7,tornabuon");
        attrDataNST.insertRow("8,albizzi");
        attrDataNST.insertRow("9,ginori");
        attrDataNST.insertRow("10,guadagni");
        attrDataNST.insertRow("11,lambertes");
        attrDataNST.insertRow("12,bischeri");
        attrDataNST.insertRow("13,strozzi");
        attrDataNST.insertRow("14,castellan");
        attrDataNST.insertRow("15,peruzzi");

        //not in paper, for testing graph with weights
        DB.getLinkAttrs().defineAttributeOrClearValuesIfExists("weight", "dbl");
        attrDataNST = DB.getLinkAttrs().getAttrDataNST("weight"); // id, value
        attrDataNST.insertRow("1,1.0");
        attrDataNST.insertRow("2,1.0");
        attrDataNST.insertRow("3,1.0");
        attrDataNST.insertRow("4,1000.0");
        attrDataNST.insertRow("5,1.0");
        attrDataNST.insertRow("6,1.0");
        attrDataNST.insertRow("7,1.0");
        attrDataNST.insertRow("8,1.0");
        attrDataNST.insertRow("9,1.0");
        attrDataNST.insertRow("10,1.0");
        attrDataNST.insertRow("11,1.0");
        attrDataNST.insertRow("12,1.0");
        attrDataNST.insertRow("13,1.0");
        attrDataNST.insertRow("14,1.0");
        attrDataNST.insertRow("15,1.0");
        attrDataNST.insertRow("16,1.0");
        attrDataNST.insertRow("17,1.0");
        attrDataNST.insertRow("18,1.0");
        attrDataNST.insertRow("19,1.0");
        attrDataNST.insertRow("20,1.0");
    }

/*    public void testScratch(){
    SyntheticGraphForestFire f = new SyntheticGraphForestFire(1000000, .35, .2);
    EmbeddedGraph graph = new EmbeddedGraph(false);

    Set<Node> neighbors = new HashSet<Node>();
    neighbors.addAll(graph.getNeighbors(5));
    neighbors.addAll(graph.getNeighbors(52));
    neighbors.addAll(graph.getNeighbors(43));
    neighbors.addAll(graph.getNeighbors(365));

    List<Integer> nodes = new ArrayList<Integer>();
    nodes.add(5);
    nodes.add(52);
    nodes.add(43);
    nodes.add(365);

    assertEquals(neighbors.size(), graph.getNeighbors(nodes).size());
}*/


}
