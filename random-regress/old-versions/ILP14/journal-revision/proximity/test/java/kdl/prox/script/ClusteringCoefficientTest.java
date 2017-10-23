/**
 * $Id: ClusteringCoefficientTest.java 3658 2007-10-15 16:29:11Z schapira $
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


public class ClusteringCoefficientTest extends TestCase {

    private static Logger log = Logger.getLogger(ClusteringCoefficientTest.class);

    private Container sourceContainer;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

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
        DB.insertObject(7);

        DB.insertLink(1, 1, 3);
        DB.insertLink(2, 2, 3);
        DB.insertLink(3, 1, 5);
        DB.insertLink(4, 1, 4);
        DB.insertLink(5, 4, 1);
        DB.insertLink(6, 4, 7);
        DB.insertLink(7, 6, 4);
        DB.insertLink(8, 4, 5);
        DB.insertLink(9, 7, 6);
        DB.insertLink(10, 1, 1);

        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"4", "1", "A"});
        objectNST.insertRow(new String[]{"5", "1", "A"});
        objectNST.insertRow(new String[]{"6", "1", "A"});
        objectNST.insertRow(new String[]{"7", "1", "A"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"3", "1", "A"});
        linkNST.insertRow(new String[]{"4", "1", "A"});
        linkNST.insertRow(new String[]{"5", "1", "A"});
        linkNST.insertRow(new String[]{"6", "1", "A"});
        linkNST.insertRow(new String[]{"7", "1", "A"});
        linkNST.insertRow(new String[]{"8", "1", "A"});
        linkNST.insertRow(new String[]{"9", "1", "A"});
        linkNST.insertRow(new String[]{"10", "1", "A"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testClusteringCoefficient() {
        // test it
        String outputAttrName = "test_clustering_coefficient";
        SNA.computeClusteringCoefficient(null, outputAttrName); // null -> input is entire db
        NST attrDataNST = DB.getObjectAttrs().getAttrDataNST(outputAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0.3333333333333333));
        expectedMap.put(new Integer(3), new Double(0.0));
        expectedMap.put(new Integer(4), new Double(0.3333333333333333));
        expectedMap.put(new Integer(5), new Double(1.0));
        expectedMap.put(new Integer(6), new Double(1.0));
        expectedMap.put(new Integer(7), new Double(1.0));
        ResultSet resultSet = attrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);
    }

    public void testClusteringCoefficientOnContainer() {
        // test it
        String outputAttrName = "test_clustering_coefficient";
        SNA.computeClusteringCoefficient(sourceContainer, outputAttrName);
        NST attrDataNST = DB.getObjectAttrs().getAttrDataNST(outputAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(1.0));
        expectedMap.put(new Integer(4), new Double(0.3333333333333333));
        expectedMap.put(new Integer(5), new Double(1.0));
        expectedMap.put(new Integer(6), new Double(1.0));
        expectedMap.put(new Integer(7), new Double(1.0));
        ResultSet resultSet = attrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);
    }


}
