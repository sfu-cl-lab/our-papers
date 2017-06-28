/**
 * $Id: OnDiskGraphTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: OnDiskGraphTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.nsi2.graph;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.nsi2.util.ConversionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: May 17, 2007
 * Time: 3:56:16 PM
 */
public class OnDiskGraphTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildItalianGraph();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testDirected() {
        OnDiskGraph onDiskGraph = new OnDiskGraph(true);
        Set<Node> neighbors = onDiskGraph.getNeighbors(4);
        TestUtil.verifyCollections(new Integer[]{5, 7, 8}, ConversionUtils.nodesToIntegers(neighbors));
    }

    public void testUnDirected() {
        OnDiskGraph onDiskGraph = new OnDiskGraph(false);
        Set<Node> neighbors = onDiskGraph.getNeighbors(4);
        TestUtil.verifyCollections(new Integer[]{2, 3, 6, 5, 7, 8}, ConversionUtils.nodesToIntegers(neighbors));
    }

    public void testUnDirectedMultiple() {
        OnDiskGraph onDiskGraph = new OnDiskGraph(false);
        Set<Node> neighbors = onDiskGraph.getNeighbors(stringToIntegerList("4,5"));
        TestUtil.verifyCollections(new Integer[]{2, 3, 6, 4, 5, 7, 8, 14}, ConversionUtils.nodesToIntegers(neighbors));
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

    private Collection<Integer> stringToIntegerList(String s) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        String[] strings = s.split(",");
        for (String str : strings) {
            list.add(Integer.parseInt(str));
        }
        return list;
    }

}
