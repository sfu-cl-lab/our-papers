package kdl.prox.nsi2.search;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.nsi2.graph.InMemoryWeightedGraph;


/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: Jun 11, 2007
 * Time: 4:09:54 PM
 */
public class SearchUniformCostTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildItalianGraph();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUniformCostSearch() {
        InMemoryWeightedGraph inMemoryWeightedGraph = new InMemoryWeightedGraph(false);
        SearchUniformCost search = new SearchUniformCost(inMemoryWeightedGraph);

        SearchResults sr = search.search(4, 5);
        assertEquals(sr.pathlength(), 4.0, 0.001);
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


}
