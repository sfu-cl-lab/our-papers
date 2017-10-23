/**
 * $Id: SampleContainerTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.sample;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.ProxItem;
import kdl.prox.db.Subgraph;
import kdl.prox.util.Assert;

public class SampleContainerTest extends TestCase {

    private Container container;
    private HashMap subgraphHashMap;
    private int numSamples;
    private int numSubgraphs;


    public SampleContainerTest() {
        numSamples = 10;
        numSubgraphs = 95;
        subgraphHashMap = new HashMap();
    }


    public SampleContainerTest(int numSamples, int numSubgraphs) {
        Assert.condition(numSamples > 0, "numSamples not positive");
        Assert.condition(numSubgraphs > 0, "numSubgraphs not positive");

        this.numSamples = numSamples;
        this.numSubgraphs = numSubgraphs;
        this.subgraphHashMap = new HashMap();
    }


    public void setUp()  {
        TestUtil.initDBOncePerAllTests();

        // connect to the database
        TestUtil.openTestConnection();

        // initialize the database
        DB.deleteTempContainers();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();

        // create a new container that will contain NUM_SUBGRAPHS subgraphs
        // of the following form:
        //
        // X   Z   Y
        // O-------O
        container = DB.createNewTempContainer();

        for (int i = 0; i < numSubgraphs; ++i) {
            int subgraphOID;
            Subgraph subgraph;

            // add two new objects and a link between them in the database
            DB.insertObject(2 * i);
            DB.insertObject(2 * i + 1);
            DB.insertLink(i, 2 * i, 2 * i + 1);

            // create a new subgraph and insert the created objects and link into it
            // todo: improve efficiency by using something other than getSubgraph
            subgraphOID = i;
            subgraph = container.getSubgraph(subgraphOID);
            subgraph.insertObject(2 * i, "X");
            subgraph.insertObject(2 * i + 1, "Y");
            subgraph.insertLink(i, "Z");

            // initialize the subgraph to "not accounted for" for use in the
            // testAllAttributes() method to check that each subgraph has only been inserted
            // into one sample
            subgraphHashMap.put(new Integer(subgraphOID), "not accounted for");
        }

    }


    public void tearDown()  {
        TestUtil.closeTestConnection();
    }


    // checks for the following conditions:
    // - each sample is the correct size
    // - each subgraph is inserted into one, and only one, sample
    public void testSample()  {
        Comparator proxItemComparator =
                new Comparator() {

                    public int compare(Object o1, Object o2) {
                        Integer id1;
                        Integer id2;
                        ProxItem pi1 = (ProxItem) o1;
                        ProxItem pi2 = (ProxItem) o2;

                        id1 = new Integer(pi1.getOid());
                        id2 = new Integer(pi2.getOid());

                        return id1.compareTo(id2);
                    }

                };
        Container childContainer;
        Iterator sampleNamesIterator;
        List sampleNames;
        SampleContainer sampleContainer = new SampleContainer(container,
                numSamples,
                "Samples");

        childContainer = container.getChild("Samples");
        sampleNames = childContainer.getChildrenNames();
        sampleNamesIterator = sampleNames.iterator();

        while (sampleNamesIterator.hasNext()) {
            Container sample =
                    childContainer.getChild((String) sampleNamesIterator.next());
            Iterator subgraphOIDsIterator;
            List subgraphOIDs = sample.getSubgraphOIDs();
            double averageSampleSize = numSubgraphs / (double) numSamples;
            int expectedSampleSize;
            int lowerBound;
            int sampleNumber = Integer.parseInt(sample.getName());
            int upperBound;

            // check that each sample contains the correct number of subgraphs
            lowerBound = (int) Math.round(sampleNumber * averageSampleSize);
            upperBound = (int) Math.round((sampleNumber + 1) * averageSampleSize);
            expectedSampleSize = upperBound - lowerBound;
            Assert.condition(subgraphOIDs.size() == expectedSampleSize,
                    "sample not expected size");

            subgraphOIDsIterator = subgraphOIDs.iterator();

            while (subgraphOIDsIterator.hasNext()) {
                List containerSubgraphLinks;
                List sampleSubgraphLinks;
                List containerSubgraphObjects;
                List sampleSubgraphObjects;
                String condition;
                String conditionToTest = "not accounted for";
                Subgraph containerSubgraph;
                Subgraph sampleSubgraph;
                int subgraphOID = ((Integer) subgraphOIDsIterator.next()).intValue();

                condition = (String) subgraphHashMap.get(new Integer(subgraphOID));

                // check that the current subgraphOID has not already been inserted
                // into another sample
                Assert.condition(conditionToTest.equals(condition),
                        "subgraph already accounted for");

                // get the objects and links of the original subgraph, and the copy
                // of that subgraph inserted into a sample for comparison
                containerSubgraph = container.getSubgraph(subgraphOID);
                sampleSubgraph = sample.getSubgraph(subgraphOID);
                containerSubgraphObjects = containerSubgraph.getObjects();
                sampleSubgraphObjects = sampleSubgraph.getObjects();
                containerSubgraphLinks = containerSubgraph.getLinks();
                sampleSubgraphLinks = sampleSubgraph.getLinks();

                // sort the objects and links by ID so that for each subgraph, the
                // objects and links are presented in the same order
                Collections.sort(containerSubgraphObjects, proxItemComparator);
                Collections.sort(sampleSubgraphObjects, proxItemComparator);
                Collections.sort(containerSubgraphLinks, proxItemComparator);
                Collections.sort(sampleSubgraphLinks, proxItemComparator);

                // check that all the objects and links of the current subgraph
                // were copied into the sample
                Assert.condition(listEquals(containerSubgraphObjects,
                        sampleSubgraphObjects),
                        "objects not equal: " + subgraphOID);
                Assert.condition(listEquals(containerSubgraphLinks,
                        sampleSubgraphLinks),
                        "links not equal: " + subgraphOID);

                // mark that we have already accounted for this subgraph
                subgraphHashMap.put(new Integer(subgraphOID), "accounted for");
            }

        }

    }


    // helper method that does a 'deep' comparison of lists a and b
    private static boolean listEquals(List a, List b) {
        Iterator aIterator;
        Iterator bIterator;

        if (a.size() != b.size())
            return false;

        aIterator = a.iterator();
        bIterator = b.iterator();

        while (aIterator.hasNext()) {
            Object aObject = aIterator.next();
            Object bObject = bIterator.next();

            if (!aObject.equals(bObject))
                return false;
        }

        return true;
    }

}