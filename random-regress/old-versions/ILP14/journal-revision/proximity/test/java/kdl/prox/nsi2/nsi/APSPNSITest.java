/**
 * $Id: APSPNSITest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: APSPNSITest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.nsi2.nsi;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.nsi2.graph.InMemoryGraph;


/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: May 21, 2007
 * Time: 4:07:26 PM
 */
public class APSPNSITest extends TestCase {
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildItalianGraph();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAPSP() {
        APSPNSI apsp = new APSPNSI(new InMemoryGraph(false));

        int n = DB.getObjectNST().getRowCount() + 1; // there is no object 0
        int[][] distances = new int[n][n];
        //                        -  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
        distances[0] = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        distances[1] = new int[]{-1, 0, 1, 3, 2, 3, 3, 3, 3, 4, 4, 5, 5, 4, 4, 5};
        distances[2] = new int[]{-1, 1, 0, 2, 1, 2, 2, 2, 2, 3, 3, 4, 4, 3, 3, 4};
        distances[3] = new int[]{-1, 3, 2, 0, 1, 2, 2, 2, 2, 3, 3, 4, 4, 3, 3, 4};
        distances[4] = new int[]{-1, 2, 1, 1, 0, 1, 1, 1, 1, 2, 2, 3, 3, 2, 2, 3};
        distances[5] = new int[]{-1, 3, 2, 2, 1, 0, 2, 2, 2, 3, 3, 4, 3, 2, 1, 2};
        distances[6] = new int[]{-1, 3, 2, 2, 1, 2, 0, 1, 2, 3, 2, 3, 2, 1, 2, 2};
        distances[7] = new int[]{-1, 3, 2, 2, 1, 2, 1, 0, 2, 3, 1, 2, 2, 2, 3, 3};
        distances[8] = new int[]{-1, 3, 2, 2, 1, 2, 2, 2, 0, 1, 1, 2, 2, 3, 3, 3};
        distances[9] = new int[]{-1, 4, 3, 3, 2, 3, 3, 3, 1, 0, 2, 3, 3, 4, 4, 4};
        distances[10] = new int[]{-1, 4, 3, 3, 2, 3, 2, 1, 1, 2, 0, 1, 1, 2, 3, 2};
        distances[11] = new int[]{-1, 5, 4, 4, 3, 4, 3, 2, 2, 3, 1, 0, 2, 3, 4, 3};
        distances[12] = new int[]{-1, 5, 4, 4, 3, 3, 2, 2, 2, 3, 1, 2, 0, 1, 2, 1};
        distances[13] = new int[]{-1, 4, 3, 3, 2, 2, 1, 2, 3, 4, 2, 3, 1, 0, 1, 1};
        distances[14] = new int[]{-1, 4, 3, 3, 2, 1, 2, 3, 3, 4, 3, 4, 2, 1, 0, 1};
        distances[15] = new int[]{-1, 5, 4, 4, 3, 2, 2, 3, 3, 4, 2, 3, 1, 1, 1, 0};

        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                assertEquals(distances[i][j], (int) apsp.distance(i, j));
            }
        }

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
    }


}
