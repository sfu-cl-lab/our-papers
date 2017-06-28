package kdl.prox.nsi2.apps;

import kdl.prox.db.DB;
import kdl.prox.nsi2.search.SearchBreadthFirst;
import kdl.prox.nsi2.search.SearchBestFirst;
import kdl.prox.nsi2.graph.InMemoryGraph;
import kdl.prox.nsi2.nsi.APSPNSI;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: Jul 26, 2007
 * Time: 11:39:47 AM
  */
public class CentralityTest extends TestCase {

    private static Logger log = Logger.getLogger(CentralityTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildItalianGraph();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testCloseCentralRanking() {
        List<Integer> correct = new ArrayList<Integer>();
        correct.add(4);
        correct.add(6);
        correct.add(8);    // tie
        correct.add(7);    //
        correct.add(10);
        correct.add(5);    // tie
        correct.add(13);   //
        correct.add(12);
        correct.add(14);   // tie
        correct.add(2);    //
        correct.add(3);    // tie
        correct.add(15);    //
        correct.add(9);
        correct.add(11);
        correct.add(1);
        log.info("       correct: " + correct);

        InMemoryGraph inMemoryGraph = new InMemoryGraph(false);
        SearchBreadthFirst search = new SearchBreadthFirst(inMemoryGraph);
        List<Integer> ranking = Centrality.closeCentralRanking(DB.getObjectNST().copy(), DB.getObjectNST().getRowCount(), search);

        log.info("ranking search: " + ranking);
        assertEquals(ranking.get(0), correct.get(0));
        assertEquals(ranking.get(1), correct.get(1));
        assertEquals(ranking.get(4),  correct.get(4));
        assertEquals(ranking.get(7),  correct.get(7));
        assertEquals(ranking.get(12),  correct.get(12));
        assertEquals(ranking.get(13),  correct.get(13));
        assertEquals(ranking.get(14),  correct.get(14));

        APSPNSI apsp =  new APSPNSI(inMemoryGraph);
        ranking = Centrality.closeCentralRanking(DB.getObjectNST().copy(), DB.getObjectNST().getRowCount(), apsp);
        log.info("   ranking nsi: " + ranking);
        assertEquals(ranking.get(0), correct.get(0));
        assertEquals(ranking.get(1), correct.get(1));
        assertEquals(ranking.get(4),  correct.get(4));
        assertEquals(ranking.get(7),  correct.get(7));
        assertEquals(ranking.get(12),  correct.get(12));
        assertEquals(ranking.get(13),  correct.get(13));
        assertEquals(ranking.get(14),  correct.get(14));

    }

    public void testBetweenCentralRanking() {
/*        List<Integer> correct = new ArrayList<Integer>();
        correct.add(4);
        correct.add(10);
        correct.add(8);
        correct.add(2);
        correct.add(6);
        correct.add(12);
        correct.add(13);
        correct.add(5);
        correct.add(7);
        correct.add(14);
        correct.add(15);
        correct.add(3); // last four are tied for 0
        correct.add(9);
        correct.add(11);
        correct.add(1);
*/
        //hard to test since searches only return one path, not all
        InMemoryGraph inMemoryGraph = new InMemoryGraph(false);
        APSPNSI apsp =  new APSPNSI(inMemoryGraph);
        SearchBestFirst search = new SearchBestFirst(apsp, inMemoryGraph);

        List<int[]> pairs = new ArrayList<int[]>();
        for (int i = 1; i<=15; i++) {
            for (int j = 1; j<=15; j++) {
                pairs.add(new int[]{i, j});
            }
        }
        List<Integer> ranking = Centrality.betweenCentralRanking(pairs, search);
        log.debug(ranking);
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
