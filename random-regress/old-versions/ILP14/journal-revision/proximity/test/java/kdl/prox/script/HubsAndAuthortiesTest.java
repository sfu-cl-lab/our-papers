/**
 * $Id: HubsAndAuthortiesTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */


package kdl.prox.script;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;


public class HubsAndAuthortiesTest extends TestCase {

    private static Logger log = Logger.getLogger(HubsAndAuthortiesTest.class);

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

        DB.insertLink(1, 1, 5);
        DB.insertLink(2, 1, 2);
        DB.insertLink(3, 1, 3);
        DB.insertLink(4, 4, 2);
        DB.insertLink(5, 4, 3);
        DB.insertLink(6, 3, 4);

        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "1", "A"});
        objectNST.insertRow(new String[]{"3", "1", "A"});
        objectNST.insertRow(new String[]{"4", "1", "A"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"2", "1", "A"});
        linkNST.insertRow(new String[]{"3", "1", "A"});
        linkNST.insertRow(new String[]{"4", "1", "A"});
        linkNST.insertRow(new String[]{"5", "1", "A"});
        linkNST.insertRow(new String[]{"6", "1", "A"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testHubsAndAuths() {
        String hubAttrName = "test-hub-attr";
        String authAttrName = "test-auth-attr";
        SNA.computeHubsAndAuthorities(null, 3, hubAttrName, authAttrName);

        // check hub attribute
        NST hubAttrDataNST = DB.getObjectAttrs().getAttrDataNST(hubAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0.6335957142456294));
        expectedMap.put(new Integer(2), new Double(2.7736221637007337E-10));
        expectedMap.put(new Integer(3), new Double(2.7736221637007337E-10));
        expectedMap.put(new Integer(4), new Double(0.3664042846449216));
        expectedMap.put(new Integer(5), new Double(2.7736221637007337E-10));
        expectedMap.put(new Integer(6), new Double(2.7736221637007337E-10));
        ResultSet resultSet = hubAttrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);

        // check authority attribute
        NST authAttrDataNST = DB.getObjectAttrs().getAttrDataNST(authAttrName);
        expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(5.117375700176887E-6));
        expectedMap.put(new Integer(2), new Double(0.41720116597459855));
        expectedMap.put(new Integer(3), new Double(0.41720116597459855));
        expectedMap.put(new Integer(4), new Double(1.2632585907561913E-9));
        expectedMap.put(new Integer(5), new Double(0.16558743203614398));
        expectedMap.put(new Integer(6), new Double(5.117375700176887E-6));
        resultSet = authAttrDataNST.selectRows();
        actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);
    }

    public void testHubsAndAuthsOnSource() {
        String hubAttrName = "test-hub-attr";
        String authAttrName = "test-auth-attr";
        SNA.computeHubsAndAuthorities(sourceContainer, 3, hubAttrName, authAttrName);

        // check hub attribute
        NST hubAttrDataNST = DB.getObjectAttrs().getAttrDataNST(hubAttrName);
        Map expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(0.49999999813735485));
        expectedMap.put(new Integer(2), new Double(1.8626451422920631E-9));
        expectedMap.put(new Integer(3), new Double(1.8626451422920631E-9));
        expectedMap.put(new Integer(4), new Double(0.49999999813735485));
        ResultSet resultSet = hubAttrDataNST.selectRows();
        Map actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);

        // check authority attribute
        NST authAttrDataNST = DB.getObjectAttrs().getAttrDataNST(authAttrName);
        expectedMap = new HashMap();
        expectedMap.put(new Integer(1), new Double(1.574419041651076E-5));
        expectedMap.put(new Integer(2), new Double(0.4999921241795601));
        expectedMap.put(new Integer(3), new Double(0.4999921241795601));
        expectedMap.put(new Integer(4), new Double(7.45046323805392E-9));
        resultSet = authAttrDataNST.selectRows();
        actualMap = new HashMap();
        while (resultSet.next()) {
            int objOID = resultSet.getOID(1);
            double coeff = resultSet.getDouble(2);
            actualMap.put(new Integer(objOID), new Double(coeff));
        }
        TestUtil.verifyDoubleMap(expectedMap, actualMap);
    }

    // test that normalize works when the values are large and with many decimals
    // some sums were being represented with scientific notation and that was breaking the
    // calls 
    public void testNormalize() {
        NST nst = new NST("col1, col2", "dbl, dbl");
        nst.insertRow("0.4354353453451, 24545435435.345");
        SNA.normalizeNST(nst);
    }


}
