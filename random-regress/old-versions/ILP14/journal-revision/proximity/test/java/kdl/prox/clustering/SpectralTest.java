package kdl.prox.clustering;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.ProxLink;
import kdl.prox.db.ProxObj;
import kdl.prox.db.Subgraph;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class SpectralTest extends TestCase {

    private Logger log = Logger.getLogger(SpectralTest.class);

    private String objName = Spectral.ORIGINAL_OBJECT_NAME_IN_CLUSTER;
    private String linkName = "link";


    protected void setUp() throws Exception {
        super.setUp();

        // set up test data
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getRootContainer().deleteAllChildren();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testClusterDepth1() {
        // add objects and links to a single subgraph
        Container container = DB.getRootContainer().createChild("test-cont");
        int subgId = 1;
        Subgraph sub = container.getSubgraph(subgId);
        // create objects and links
        for (int i = 0; i < 8; i++) {
            DB.insertObject(i);
            sub.insertObject(i, objName);
        }
        //make a clique between the first four objects
        int linkID = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                DB.insertLink(linkID, i, j);
                sub.insertLink(linkID, linkName);
                linkID++;
            }
        }
        //make a clique between the last four objects
        for (int i = 4; i < 8; i++) {
            for (int j = i + 1; j < 8; j++) {
                DB.insertLink(linkID, i, j);
                sub.insertLink(linkID, linkName);
                linkID++;
            }
        }
        //add one link between clusters
        DB.insertLink(linkID, 3, 4);
        sub.insertLink(linkID, linkName);

        //cluster the container and check the cluster subgraph membership
        String outputContName = "clustered container";
        Container outputContainer = new Spectral(6).clusterContainerIntoSubgraphs(container, outputContName);

        //test its contents
        assertEquals(outputContainer.getSubgraphCount(), 2);
        List subgIDs = outputContainer.getSubgraphOIDs();
        for (int subgIDIdx = 0; subgIDIdx < subgIDs.size(); subgIDIdx++) {
            Integer subgID = (Integer) subgIDs.get(subgIDIdx);
            Subgraph subgraph = outputContainer.getSubgraph(subgID.intValue());

            //test the objects
            boolean hasObj0 = false;
            List objects = subgraph.getObjects();
            assertEquals(objects.size(), 4);
            Set<Integer> itemIDs = new HashSet<Integer>();
            for (int objIdx = 0; objIdx < objects.size(); objIdx++) {
                ProxObj proxObj = (ProxObj) objects.get(objIdx);
                int itemID = proxObj.getOid();
                if (itemID == 0) {
                    hasObj0 = true;
                }
                itemIDs.add(new Integer(itemID));
                String itemName = proxObj.getName();
                assertEquals(objName, itemName);
            }
            if (hasObj0) {
                TestUtil.verifyCollections(new Integer[]{new Integer(0), new Integer(1),
                        new Integer(2), new Integer(3)}, itemIDs);
            } else {
                TestUtil.verifyCollections(new Integer[]{new Integer(4), new Integer(5),
                        new Integer(6), new Integer(7)}, itemIDs);
            }

            //test the links
            List links = subgraph.getLinks();
            assertEquals(6, links.size());
            Set<Integer> linkIDs = new HashSet<Integer>();
            for (int linkIdx = 0; linkIdx < links.size(); linkIdx++) {
                ProxLink proxLink = (ProxLink) links.get(linkIdx);
                linkID = proxLink.getOid();
                linkIDs.add(new Integer(linkID));
                String lkName = proxLink.getName();
                assertEquals(linkName, lkName);
            }
            if (hasObj0) {
                TestUtil.verifyCollections(new Integer[]{new Integer(0), new Integer(1),
                        new Integer(2), new Integer(3),
                        new Integer(4), new Integer(5)}, linkIDs);
            } else {
                TestUtil.verifyCollections(new Integer[]{new Integer(6), new Integer(7),
                        new Integer(8), new Integer(9),
                        new Integer(10), new Integer(11)}, linkIDs);
            }
        }
    }

    public void testClusterDepth2() {
        // add objects and links to a single subgraph
        Container container2 = DB.getRootContainer().createChild("test container2");
        int subgId = 1;
        Subgraph sub = container2.getSubgraph(subgId);
        // create objects and links
        for (int i = 10; i < 22; i++) {
            DB.insertObject(i);
            sub.insertObject(i, objName);
        }
        //make a clique between the first four objects
        int linkID = 20;
        for (int i = 10; i < 14; i++) {
            for (int j = i + 1; j < 14; j++) {
                DB.insertLink(linkID, i, j);
                sub.insertLink(linkID, linkName);
                linkID++;
            }
        }
        //make a clique between the 2nd four objects
        for (int i = 14; i < 18; i++) {
            for (int j = i + 1; j < 18; j++) {
                DB.insertLink(linkID, i, j);
                sub.insertLink(linkID, linkName);
                linkID++;
            }
        }
        //make a clique between the last four objects
        for (int i = 18; i < 22; i++) {
            for (int j = i + 1; j < 22; j++) {
                DB.insertLink(linkID, i, j);
                sub.insertLink(linkID, linkName);
                linkID++;
            }
        }
        //add one link between clusters
        DB.insertLink(linkID, 13, 14);
        sub.insertLink(linkID++, linkName);
        //add two links between clusters
        DB.insertLink(linkID, 11, 18);
        sub.insertLink(linkID++, linkName);
        DB.insertLink(linkID, 12, 19);
        sub.insertLink(linkID, linkName);

        //cluster the container and check the cluster subgraph membership
        String outputContName = "clustered container2";
        Container outputContainer = new Spectral(6).clusterContainerIntoSubgraphs(container2, outputContName);

        //test its contents
        assertEquals(outputContainer.getSubgraphCount(), 3);
        List subgIDs = outputContainer.getSubgraphOIDs();
        for (int subgIDIdx = 0; subgIDIdx < subgIDs.size(); subgIDIdx++) {
            Integer subgID = (Integer) subgIDs.get(subgIDIdx);
            Subgraph subgraph = outputContainer.getSubgraph(subgID.intValue());
            log.debug("tesing subgraph " + subgID);

            //test the objects
            boolean hasObj10 = false;
            boolean hasObj14 = false;
            List objects = subgraph.getObjects();
            assertEquals(objects.size(), 4);
            Set<Integer> itemIDs = new HashSet<Integer>();
            for (int objIdx = 0; objIdx < objects.size(); objIdx++) {
                ProxObj proxObj = (ProxObj) objects.get(objIdx);
                int itemID = proxObj.getOid();
                if (itemID == 10) {
                    hasObj10 = true;
                }
                if (itemID == 14) {
                    hasObj14 = true;
                }
                itemIDs.add(new Integer(itemID));
                String itemName = proxObj.getName();
                assertEquals(objName, itemName);
            }
            if (hasObj10) {
                TestUtil.verifyCollections(new Integer[]{new Integer(10), new Integer(11),
                        new Integer(12), new Integer(13)}, itemIDs);
            } else if (hasObj14) {
                TestUtil.verifyCollections(new Integer[]{new Integer(14), new Integer(15),
                        new Integer(16), new Integer(17)}, itemIDs);
            } else {
                TestUtil.verifyCollections(new Integer[]{new Integer(18), new Integer(19),
                        new Integer(20), new Integer(21)}, itemIDs);
            }

            //test the links
            List links = subgraph.getLinks();
            Set<Integer> linkIDs = new HashSet<Integer>();
            for (int linkIdx = 0; linkIdx < links.size(); linkIdx++) {
                ProxLink proxLink = (ProxLink) links.get(linkIdx);
                int currLinkID = proxLink.getOid();
                linkIDs.add(new Integer(currLinkID));
                String lkName = proxLink.getName();
                assertEquals(linkName, lkName);
            }
            if (hasObj10) {
                TestUtil.verifyCollections(new Integer[]{new Integer(20), new Integer(21),
                        new Integer(22), new Integer(23),
                        new Integer(24), new Integer(25)}, linkIDs);
            } else if (hasObj14) {
                TestUtil.verifyCollections(new Integer[]{new Integer(26), new Integer(27),
                        new Integer(28), new Integer(29),
                        new Integer(30), new Integer(31)}, linkIDs);
            } else {
                TestUtil.verifyCollections(new Integer[]{new Integer(32), new Integer(33),
                        new Integer(34), new Integer(35),
                        new Integer(36), new Integer(37)}, linkIDs);
            }
        }

    }

}
