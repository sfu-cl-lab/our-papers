/**
 * $Id: SubgraphVisTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbvis.ProxSparseVertex;
import kdl.prox.util.PopulateDB;
import org.apache.log4j.Logger;


public class SubgraphVisTest extends TestCase {

    private static final Logger log = Logger.getLogger(SubgraphVisTest.class);
    private static final String TEST_CONTAINER = "TEST_CONTAINER";
    private static final String SUBG_0_URL = "subg:/containers/" + TEST_CONTAINER + "/" + "0";
    private static final String SUBG_1_URL = "subg:/containers/" + TEST_CONTAINER + "/" + "1";
    private static final String SUBG_2_URL = "subg:/containers/" + TEST_CONTAINER + "/" + "2";


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testFindCenterVertex() {
        DB.getRootContainer().deleteAllChildren();
        PopulateDB.populateDB(getClass(), "subgraph-vis-containers.txt");

        // test subgraph 0
        Graph graph = new SparseGraph();
        ProxURL subgURL = new ProxURL(SUBG_0_URL);
        GUIContentGenerator.getGraphForSubgURL(subgURL, graph);
        ProxSparseVertex vertex = (ProxSparseVertex) SubgraphJFrame.findCenterVertex(graph, subgURL);
        assertTrue(vertex.getOID().intValue() == 270);

        // test subgraph 1
        subgURL = new ProxURL(SUBG_1_URL);
        GUIContentGenerator.getGraphForSubgURL(subgURL, graph);
        vertex = (ProxSparseVertex) SubgraphJFrame.findCenterVertex(graph, subgURL);
        assertTrue(vertex.getOID().intValue() == 260);

        // test subgraph 2
        subgURL = new ProxURL(SUBG_2_URL);
        GUIContentGenerator.getGraphForSubgURL(subgURL, graph);
        vertex = (ProxSparseVertex) SubgraphJFrame.findCenterVertex(graph, subgURL);
        assertTrue(vertex.getOID().intValue() == 278);
    }

}
