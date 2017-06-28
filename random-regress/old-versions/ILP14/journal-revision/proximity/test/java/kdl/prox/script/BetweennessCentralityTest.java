/**
 * $Id: BetweennessCentralityTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */


package kdl.prox.script;

import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;


public class BetweennessCentralityTest extends TestCase {

    private static Logger log = Logger.getLogger(BetweennessCentralityTest.class);

    private Container sourceContainer;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

        /**
         * Test db looks like this:
         *
         *                A
         *                ^
         *                |
         *             D->E->B<-F
         *                ^
         *                |
         *                C
         *
         * So undirected betweeness scores should be: A=C=D=0, E=9, B=10, F=4
         * Directed betweeness scores should be: A=B=C=D=F=0, E=3
         *  Note: The betweenness centrality for a node v counts the number of shortest paths
         *  between all other nodes in the graph that contain v. It does not count paths of
         *  length 1 (because there is no interior node).
         *
         * Container looks like this (5 subgraphs):
         *
         *                A         A--E
         *                |         B--E
         *             D--E--B      C--E
         *                |         D--E
         *                C
         *
         * So betweeness scores should be: A=B=C=D=0, E=6
         */

        TestUtil.openTestConnection();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getRootContainer().deleteAllChildren();

        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);
        DB.insertObject(4);
        DB.insertObject(5);
        DB.insertObject(6);

        DB.insertLink(1, 1, 5);
        DB.insertLink(2, 5, 2);
        DB.insertLink(3, 3, 5);
        DB.insertLink(4, 4, 5);
        DB.insertLink(5, 6, 2);
        DB.insertLink(6, 4, 5); //duplicate link shouldn't change score

        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"5", "1", "E"});
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"5", "2", "E"});
        objectNST.insertRow(new String[]{"2", "2", "B"});
        objectNST.insertRow(new String[]{"5", "3", "E"});
        objectNST.insertRow(new String[]{"3", "3", "C"});
        objectNST.insertRow(new String[]{"5", "4", "E"});
        objectNST.insertRow(new String[]{"4", "4", "D"});
        objectNST.insertRow(new String[]{"5", "5", "E"});
        objectNST.insertRow(new String[]{"5", "1", "A"});
        objectNST.insertRow(new String[]{"5", "2", "B"});
        objectNST.insertRow(new String[]{"5", "3", "C"});
        objectNST.insertRow(new String[]{"5", "4", "D"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "link"});
        linkNST.insertRow(new String[]{"2", "2", "link"});
        linkNST.insertRow(new String[]{"3", "3", "link"});
        linkNST.insertRow(new String[]{"4", "4", "link"});
        linkNST.insertRow(new String[]{"1", "5", "link"});
        linkNST.insertRow(new String[]{"2", "5", "link"});
        linkNST.insertRow(new String[]{"3", "5", "link"});
        linkNST.insertRow(new String[]{"4", "5", "link"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testBetweennessCentralityUndirected() {
        String bcAttrName = "test-bc-attr";
        SNA.computeBetweennessCentrality(null, bcAttrName, true);

        // check bc attribute
        NST bcAttrDataNST = DB.getObjectAttrs().getAttrDataNST(bcAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0));
        expectedMap.put(new Integer(2), new Double(4));
        expectedMap.put(new Integer(3), new Double(0));
        expectedMap.put(new Integer(4), new Double(0));
        expectedMap.put(new Integer(5), new Double(9));
        expectedMap.put(new Integer(6), new Double(0));
        ResultSet resultSet = bcAttrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double bc = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(bc));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);

    }

    public void testBetweennessCentralityDirected() {
        String bcAttrName = "test-bc-attr";
        SNA.computeBetweennessCentrality(null, bcAttrName, false);

        // check bc attribute
        NST bcAttrDataNST = DB.getObjectAttrs().getAttrDataNST(bcAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0));
        expectedMap.put(new Integer(2), new Double(0));
        expectedMap.put(new Integer(3), new Double(0));
        expectedMap.put(new Integer(4), new Double(0));
        expectedMap.put(new Integer(5), new Double(3));
        expectedMap.put(new Integer(6), new Double(0));
        ResultSet resultSet = bcAttrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double bc = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(bc));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);

    }


    public void testBetweennessCentralityOnSource() {
        String bcAttrName = "test-bc-attr";
        SNA.computeBetweennessCentrality(sourceContainer, bcAttrName, true);

        // check bc attribute
        NST bcAttrDataNST = DB.getObjectAttrs().getAttrDataNST(bcAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0));
        expectedMap.put(new Integer(2), new Double(0));
        expectedMap.put(new Integer(3), new Double(0));
        expectedMap.put(new Integer(4), new Double(0));
        expectedMap.put(new Integer(5), new Double(6));
        ResultSet resultSet = bcAttrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double bc = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(bc));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);

    }


}
