/**
 * $Id: SampleContainer.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: SampleContainer.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.sample;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.ColumnInFilter;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;


public class SampleContainer {

    private Container container;
    private String childContainerName;
    private int numFolds;


    public SampleContainer(Container container,
                           int numFolds,
                           String childContainerName) {
        Assert.condition(!container.hasChild("train"), "train already exists");
        Assert.condition(!container.hasChild("test"), "test already exists");

        this.container = container;
        this.numFolds = numFolds;
        this.childContainerName = childContainerName;

        // If a null or empty childContainerName was passed, make training and
        // test folds for cross-validation.  Otherwise, create samples in a
        // child container, whose name is childContainerName.
        if (childContainerName == null || childContainerName.length() == 0)
            crossValidation();
        else
            sample();

        DB.commit();
    }


    private void assignSampleNumbers() {
        Attributes attributes = container.getSubgraphAttrs();
        Iterator iter;
        List subgraphOIDs = container.getSubgraphOIDs();
        NST subgraphAttributeNST;
        double averageTestSize = (double) subgraphOIDs.size() / numFolds;

        if (attributes.isAttributeDefined("sampleNumber"))
            attributes.deleteAttribute("sampleNumber");

        // create the attribute sampleNumber on subgraphs that will indicate
        // what sample the subgraph should be placed into
        attributes.defineAttribute("sampleNumber", "int");
        subgraphAttributeNST = attributes.getAttrDataNST("sampleNumber");

        // shuffle the subgraph OIDs randomly, and get an iterator to this
        // shuffled list
        Collections.shuffle(subgraphOIDs);
        iter = subgraphOIDs.iterator();

        for (int sampleNumber = 0; sampleNumber < numFolds; ++sampleNumber) {
            int lowerBound = (int) Math.round(sampleNumber * averageTestSize);
            int upperBound = (int) Math.round((sampleNumber + 1) * averageTestSize);

            // add the current sampleNumber to the sampleNumber attribute
            // for each subgraph to be placed in the currentSampleNumber
            for (int subgraphOIDIndex = lowerBound;
                 subgraphOIDIndex < upperBound;
                 ++subgraphOIDIndex) {
                String[] subgraphAttributeEntry = new String[2];

                subgraphAttributeEntry[0] = iter.next().toString();
                subgraphAttributeEntry[1] = String.valueOf(sampleNumber);

                subgraphAttributeNST.insertRow(subgraphAttributeEntry);
            }

        }

    }


    private void crossValidation() {
        Attributes attributes = container.getSubgraphAttrs();
        Container testContainer;
        Container trainContainer;
        NST linkNST = container.getItemNST(false);
        NST objectNST = container.getItemNST(true);
        NST subgraphAttributeNST;
        NST[] foldLinks = new NST[numFolds];
        NST[] foldObjects = new NST[numFolds];

        // create two child containers named train and test, deleting each of them
        // if they already exist
        if (container.hasChild("train"))
            container.deleteChild("train");

        trainContainer = container.createChild("train");

        if (container.hasChild("test"))
            container.deleteChild("test");

        testContainer = container.createChild("test");

        // assign sample numbers to the subgraphs, and get the NST for the
        // sampleNumber attribute
        assignSampleNumbers();
        subgraphAttributeNST = attributes.getAttrDataNST("sampleNumber");

        // temporary NSTs to hold the object and links for each training set
        for (int foldNumber = 0; foldNumber < numFolds; ++foldNumber) {
            foldObjects[foldNumber] = SGIUtil.createTempSGINST();
            foldLinks[foldNumber] = SGIUtil.createTempSGINST();
        }

        // loop over each of the test sets to be created
        for (int testNumber = 0; testNumber < numFolds; ++testNumber) {
            ColumnInFilter columnInFilter;
            NST filteredLinkNST;
            NST filteredObjectNST;
            NST subgraphNST;

            // get the NST that contains all subgraphs with the current test set number
            subgraphNST = subgraphAttributeNST.filter("value EQ " + String.valueOf(testNumber));

            // get only the objects and links that correspond to a subgraph whose
            // sample number matches the current test set number
            String filterDef = "subg_id IN " + subgraphNST.getNSTColumn("id").getBATName();
            filteredObjectNST = objectNST.filter(filterDef);
            filteredLinkNST = linkNST.filter(filterDef);

            testContainer.createChildFromTempSGINSTs(String.valueOf(testNumber),
                    filteredObjectNST,
                    filteredLinkNST);

            for (int trainNumber = 0; trainNumber < numFolds; ++trainNumber) {
                if (testNumber == trainNumber)
                    continue;

                foldObjects[trainNumber].insertRowsFromNST(filteredObjectNST);
                foldLinks[trainNumber].insertRowsFromNST(filteredLinkNST);
            }
            filteredObjectNST.release();
            filteredLinkNST.release();
            subgraphNST.release();

        }

        // create the training sets from the temporary NSTs
        for (int trainNumber = 0; trainNumber < numFolds; ++trainNumber) {
            trainContainer.createChildFromTempSGINSTs(String.valueOf(trainNumber),
                    foldObjects[trainNumber],
                    foldLinks[trainNumber]);
        }

        for (int trainNumber = 0; trainNumber < numFolds; ++trainNumber) {
            foldObjects[trainNumber].release();
            foldLinks[trainNumber].release();
        }
    }


    private void sample() {
        Attributes attributes = container.getSubgraphAttrs();
        Container childContainer;
        NST linkNST = container.getItemNST(false);
        NST objectNST = container.getItemNST(true);
        NST subgraphAttributeNST;

        // assign sample numbers to the subgraphs, and get the NST for the
        // sampleNumber attribute
        assignSampleNumbers();
        subgraphAttributeNST = attributes.getAttrDataNST("sampleNumber");

        // creates a new child container with the given child container name,
        // deleting it if it already exists
        if (container.hasChild(childContainerName))
            container.deleteChild(childContainerName);

        childContainer = container.createChild(childContainerName);

        // loop over each of the samples to be created
        for (int sampleNumber = 0; sampleNumber < numFolds; ++sampleNumber) {
            NST filteredLinkNST;
            NST filteredObjectNST;
            NST subgraphNST;

            // get the NST that contains all subgraphs with the current test set number
            subgraphNST = subgraphAttributeNST.filter("value EQ " + String.valueOf(sampleNumber));

            // get only the objects and links that correspond to a subgraph whose
            // sample number matches the current test set number
            String filterDef = "subg_id IN " + subgraphNST.getNSTColumn("id").getBATName();
            filteredObjectNST = objectNST.filter(filterDef);
            filteredLinkNST = linkNST.filter(filterDef);

            childContainer.createChildFromTempSGINSTs(String.valueOf(sampleNumber),
                    filteredObjectNST,
                    filteredLinkNST);

            filteredObjectNST.release();
            filteredLinkNST.release();
            subgraphNST.release();
        }
        linkNST.release();
        objectNST.release();
        subgraphAttributeNST.release();
    }

}