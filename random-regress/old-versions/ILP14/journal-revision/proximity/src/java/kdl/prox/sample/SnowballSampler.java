package kdl.prox.sample;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: neville
 * Date: Oct 2, 2006
 * Time: 9:38:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class SnowballSampler {
    protected static Logger log = Logger.getLogger(SnowballSampler.class);
    private Container container;
    private String sampleContainerName;
    private int numFolds;
    private int stepSize;
    private String coreItemName;

    /**
     * Samples the graph represented by the container by starting with random objects
     * and 'snowballing' out to add all related objects.
     */
    public SnowballSampler(Container container,
                           int numFolds,
                           String sampleContainerName,
                           String coreItemName) {

        this.container = container;
        this.numFolds = numFolds;
        this.sampleContainerName = sampleContainerName;
        this.coreItemName = coreItemName;
        log.info("Starting snowbll sampler...");

        //find out number of subg incontainer so we can add them to samples in chunks below
        int numSubgs = container.getSubgraphCount();
        this.stepSize = numSubgs / (numFolds * 2);

        sample();

        DB.commit();
    }

    private void sample() {
        Container sampleContainer;
        Container rootContainer = DB.getRootContainer();
        NST linkNST = container.getItemNST(false);
        NST objectNST = container.getItemNST(true);

        //create sample containers
        if (rootContainer.hasChild(sampleContainerName))
            rootContainer.deleteChild(sampleContainerName);
        sampleContainer = rootContainer.createChild(sampleContainerName);

        // loop over each of the samples to be created
        Container[] sampleContainers = new Container[numFolds];
        for (int sampleNumber = 0; sampleNumber < numFolds; ++sampleNumber) {
            sampleContainer.createChild(String.valueOf(sampleNumber));
            sampleContainers[sampleNumber] = sampleContainer.getChild(String.valueOf(sampleNumber));
        }

        //setup open lists for each fold
        LinkedList[] openLists = new LinkedList[numFolds];
        for (int i = 0; i < openLists.length; i++) {
            openLists[i] = new LinkedList();
        }
        //setup member lists for each fold
        HashSet[] memberLists = new HashSet[numFolds];
        for (int i = 0; i < memberLists.length; i++) {
            memberLists[i] = new HashSet();
        }

        //get random ordering of subgraphs
        NST subgNST = objectNST.filter("name EQ '" + coreItemName + "'");
        subgNST.addRandomSortColumn("rsort");
        ResultSet rs = subgNST.selectRows("rsort, subg_id, item_id");

        //cycle over each fold and assign subgraph either from open list of from random list
        boolean stillAdding = true;
        while (stillAdding) {
            for (int i = 0; i < openLists.length; i++) {
                LinkedList openList = openLists[i];
                HashSet memberList = memberLists[i];
                for (int j = 0; j < stepSize; j++) {         //add to each sample in increments
                    boolean addedSubgraph = false;

                    //try to add subgraph from openList
//                log.info("openList " + openList.toString());
                    for (Iterator itr = openList.iterator(); itr.hasNext();) {
                        Integer subgID = (Integer) itr.next();

                        addedSubgraph = addSubgIfOk(i, subgID, openLists, memberList, memberLists, sampleContainers, objectNST);
                        if (addedSubgraph) {
//                        log.info("adding to sample from open list " + i + ":" + subgID);
                            break;
                        }

                    }

                    //otherwise add subgraph from random ordering
                    if (!addedSubgraph) {
                        while (rs.next()) {
                            Integer subgID = new Integer(rs.getOID(2));
                            addedSubgraph = addSubgIfOk(i, subgID, openLists, memberList, memberLists, sampleContainers, objectNST);
                            if (addedSubgraph) {
//                            log.info("adding to sample from random list " + i + ":" + subgID);
                                break;
                            }
                        }
                    }

                    //if still haven't added a subgraph then we can stop, no more subgraphs left to add
                    if (!addedSubgraph) {
                        stillAdding = false;
                    }
                }
            }

        }

        linkNST.release();
        objectNST.release();
    }

    boolean addSubgIfOk(int i, Integer subgID, LinkedList[] openLists, HashSet memberList,
                        HashSet[] memberLists, Container[] sampleContainers, NST objectNST) {
        //check if subgraph is already in another fold (or in this fold)
        boolean isOkToAdd = true;
        for (int j = 0; j < memberLists.length; j++) {
            HashSet otherMemberList = memberLists[j];

//                log.info("looking at " + subgID);
//                log.info(j + ":" + otherMemberList.toString());

            LinkedList otherOpenList = openLists[j];
            if (otherMemberList.contains(subgID)) {
                //if(otherMemberList.contains(subgID) || (i!=j && otherOpenList.contains(subgID))){
                isOkToAdd = false;
                break;
            }
        }

        if (isOkToAdd) {
            //add subgraph to appropriate sample
            NST selSubgNST = objectNST.filter("subg_id EQ " + subgID.toString());
            sampleContainers[i].copySubgraphsFromContainer(container, selSubgNST);

            //find neighbors and add to openlist
            //NB: assumes that all neighbors in subgraph are 1 link away
            NST selObjsNST = selSubgNST.filter("name NE '" + coreItemName + "'");
            NST joinObjsNST = selObjsNST.join(objectNST, "A.item_id EQ B.item_id");
            ResultSet joinRS = joinObjsNST.selectRows("B.name EQ '" + coreItemName + "'", "B.subg_id, B.item_id", "*");
            while (joinRS.next()) {
                Integer linkedSubgID = new Integer(joinRS.getOID(1));
                //only add if it's not in there already
                if (!openLists[i].contains(linkedSubgID)) {
                    openLists[i].addLast(linkedSubgID);
                }
            }

            //bookkeeping
            openLists[i].remove(subgID);
            memberList.add(subgID);
//            log.info(memberList.toString());
//            log.info(openLists[i].toString());
            joinObjsNST.release();
            selObjsNST.release();
            selSubgNST.release();

            return true;
        } else {
            return false;
        }
    }

}
