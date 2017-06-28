package kdl.prox.sample;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;

import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: neville
 * Date: Oct 2, 2006
 * Time: 11:12:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class SnowballSamplerTest extends TestCase {
    private Container container;
    private int numSamples;
    private int numObjects;


    public SnowballSamplerTest() {
        numSamples = 2;
        numObjects = 10;
    }


    public void setUp() {
        TestUtil.initDBOncePerAllTests();

        // connect to the database
        TestUtil.openTestConnection();

        // initialize the database
        DB.deleteTempContainers();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        // create a new container that contains objects from two disconnected subgraphs
        // of the following form:
        //
        // X   Z   Y
        // O-------O
        container = DB.createNewTempContainer();

        for (int i = 0; i < numObjects; ++i) {
            DB.insertObject(i);
        }
        for (int i = 0; i < numObjects; ++i) {
            int subgraphOID;
            Subgraph subgraph;

            // create a new subgraph and insert the created objects and link into it
            // add object with four other linked objects (objs 0-4 are linked, objs 5-9 are linked)
            subgraphOID = i;
            subgraph = container.getSubgraph(subgraphOID);
            subgraph.insertObject(i, "X");
            if (i < 5) {
                for (int j = 0; j < 5; j++) {
                    if (j != i) {
                        subgraph.insertObject(j, "Y");
                    }
                }
            } else {
                for (int j = 5; j < 10; j++) {
                    if (j != i) {
                        subgraph.insertObject(j, "Y");
                    }
                }
            }
        }

    }


    public void tearDown() {
        TestUtil.closeTestConnection();
    }

    public void testSample() {
        SnowballSampler sampleContainer = new SnowballSampler(container,
                numSamples,
                "Samples",
                "X");

        //get sample containers and check the contents
        Container samples = DB.getRootContainer().getChild("Samples");
        Container sample0 = samples.getChild("0");
        Container sample1 = samples.getChild("1");
        boolean sample0HasLowNums = true;

        NST itemNST = sample0.getItemNST(true);
        ResultSet rs = itemNST.selectRows("name EQ 'X'", "item_id", "*");
        HashSet subgIDs = new HashSet();
        while (rs.next()) {
            subgIDs.add(new Integer(rs.getOID(1)));
        }

        //this test doesn't alwasy work because the items are ordered randomly
        // before being added to the snowballs one at a time, if the two samples
        // are initialized with items from the same cluster then the samples do
        // not pass the tests below TODO: need to rewrite test to account for this

//        Assert.condition(subgIDs.size()==5,"sample0 should only have 5 subgraphs, it has: " + subgIDs.size());
//        if(subgIDs.contains(new Integer(0))){
//            Assert.condition(subgIDs.contains(new Integer(1)),"missing subgraph 1");
//            Assert.condition(subgIDs.contains(new Integer(2)),"missing subgraph 2");
//            Assert.condition(subgIDs.contains(new Integer(3)),"missing subgraph 3");
//            Assert.condition(subgIDs.contains(new Integer(4)),"missing subgraph 4");
//        }
//        else{
//            sample0HasLowNums = false;
//            Assert.condition(subgIDs.contains(new Integer(5)),"missing subgraph 5");
//            Assert.condition(subgIDs.contains(new Integer(6)),"missing subgraph 6");
//            Assert.condition(subgIDs.contains(new Integer(7)),"missing subgraph 7");
//            Assert.condition(subgIDs.contains(new Integer(8)),"missing subgraph 8");
//            Assert.condition(subgIDs.contains(new Integer(9)),"missing subgraph 9");
//        }
//
//        itemNST = sample1.getItemNST(true);
//        rs = itemNST.selectRows("name EQ 'X'", "item_id", "*");
//        subgIDs = new HashSet();
//        while(rs.next()){
//            subgIDs.add(new Integer(rs.getOID(1)));
//        }
//        Assert.condition(subgIDs.size()==5,"sample1 should only have 5 subgraphs");
//        if(!sample0HasLowNums){
//            Assert.condition(subgIDs.contains(new Integer(0)),"missing subgraph 0");
//            Assert.condition(subgIDs.contains(new Integer(1)),"missing subgraph 1");
//            Assert.condition(subgIDs.contains(new Integer(2)),"missing subgraph 2");
//            Assert.condition(subgIDs.contains(new Integer(3)),"missing subgraph 3");
//            Assert.condition(subgIDs.contains(new Integer(4)),"missing subgraph 4");
//        }
//        else{
//            Assert.condition(subgIDs.contains(new Integer(5)),"missing subgraph 5");
//            Assert.condition(subgIDs.contains(new Integer(6)),"missing subgraph 6");
//            Assert.condition(subgIDs.contains(new Integer(7)),"missing subgraph 7");
//            Assert.condition(subgIDs.contains(new Integer(8)),"missing subgraph 8");
//            Assert.condition(subgIDs.contains(new Integer(9)),"missing subgraph 9");
//        }

    }

}
