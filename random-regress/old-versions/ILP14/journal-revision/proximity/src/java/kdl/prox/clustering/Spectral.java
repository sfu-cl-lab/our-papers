package kdl.prox.clustering;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTColumn;
import kdl.prox.monet.ResultSet;
import kdl.prox.script.SNA;
import kdl.prox.util.Assert;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Spectral Clusterer
 */
public class Spectral {

    private static final Logger log = Logger.getLogger(Spectral.class);
    public static final String ORIGINAL_OBJECT_NAME_IN_CLUSTER = "orig_object";
    public static final String CLUSTER_OBJECT_NAME_IN_CLUSTER = "cluster_object";

    private int minObjLimit;
    private double stabilityThresh = 0.06;


    public Spectral(int minObjLimit) {
        this.minObjLimit = minObjLimit;
    }

    public Spectral(int minObjLimit, double stabilityThr) {
        this(minObjLimit);
        stabilityThresh = stabilityThr;
    }


    /**
     * Cluster a container into another
     *
     * @param inputContainer
     * @param outputContName
     * @return the container with the clusters
     */
    public Container clusterContainerIntoSubgraphs(Container inputContainer, String outputContName) {
        Assert.notNull(inputContainer, "Input container is null");
        Assert.notNull(outputContName, "Output container name is null");
        Assert.condition(!DB.getRootContainer().hasChild(outputContName), "Container <" + outputContName + "> already exists.");

        DB.beginScope();

        // find connected components first, so we cluster the smallest possible object sets
        log.debug("finding connected components...");
        String connectedCompContName = DB.generateTempContainerName();
        Container connectedContainer = SNA.computeConnectedComponents(inputContainer, connectedCompContName);

        // partition the connected components until they are too small, or the eigenvectors become too unstable
        // and then delete the temporary container of connected components
        log.debug("partitioning connected components...");
        List<List<Integer>> clusterList = partitionConnectedComponents(connectedContainer);
        DB.getRootContainer().deleteChild(connectedCompContName);

        // and create the clustered container with one subgraph per cluster
        log.debug("adding clusters to the output container...");
        Container container = saveClustersInSubgraphs(inputContainer, clusterList, outputContName);

        DB.endScope();

        return container;
    }

    /**
     * Cluster and add a cluster object to each sub-graph
     *
     * @param container
     * @param outputContName
     * @param typeAttribute:  the object attribute where to store the type of the new objects
     * @return the container with the clusters and a cluster object
     */
    public Container clusterContainerIntoSubgsWithClustObjs(Container container, String outputContName, String typeAttribute) {

        //cluster container
        Container clusterContainer = clusterContainerIntoSubgraphs(container, outputContName);

        DB.beginScope();

        NST objNST = DB.getObjectNST();
        NST objtypeDataNST = DB.getObjectAttrs().getAttrDataNST(typeAttribute);

        //add a clusterObjName object to each subg, starting from the next obj ID in the database
        int nextInsertId = objNST.max("id") + 1;
        NST containerObjNST = clusterContainer.getItemNST(true);
        NST newInsertsNST = containerObjNST.filter("subg_id DISTINCT ROWS", "subg_id");
        newInsertsNST.addNumberColumn("newItemID", nextInsertId).addConstantColumn("newObjType", "str", CLUSTER_OBJECT_NAME_IN_CLUSTER);
        containerObjNST.insertRowsFromNST(newInsertsNST.project("newItemID, subg_id, newObjType"));

        // and create the new objects, with the objecttype set to clusterObjName
        objNST.insertRowsFromNST(newInsertsNST.project("newItemID"));
        objtypeDataNST.insertRowsFromNST(newInsertsNST.project("newItemID, newObjType"));

        DB.endScope();

        return clusterContainer;
    }


    /**
     * Partition the connected components into smaller clusters
     * until each cluster becomes too small, or the eigenvectors too unstable
     *
     * @param connectedCompContainer
     */
    private List<List<Integer>> partitionConnectedComponents(Container connectedCompContainer) {
        // cycle over subgraphs in the connected components
        NST objNST = connectedCompContainer.getItemNST(true);
        NST linkNST = connectedCompContainer.getItemNST(false);
        List subgList = connectedCompContainer.getSubgraphOIDs();
        List<List<Integer>> clusterList = new ArrayList<List<Integer>>();
        for (int subgIdx = 0; subgIdx < subgList.size(); subgIdx++) {
            Integer subgID = (Integer) subgList.get(subgIdx);
            log.debug("partitioning subg " + subgID + "...");

            // recursively partition this dataset until the clusters are too small (minObjLimit) or the eigenvectors become unstable
            NST subgObjNST = objNST.filter("subg_id EQ " + subgID, "item_id");
            NST subgLinkNST = linkNST.filter("subg_id EQ " + subgID, "item_id");
            List<List<Integer>> ccClusterList = recursivelyPartition(subgObjNST, subgLinkNST);
            clusterList.addAll(ccClusterList);

            subgLinkNST.release();
            subgObjNST.release();
        }
        return clusterList;
    }

    /**
     * Recursively partitions the dataset, returns a list of lists where each list
     * corresponds to a cluster
     *
     * @param objNST
     * @param linkNST
     * @return
     */
    private List<List<Integer>> recursivelyPartition(NST objNST, NST linkNST) {
        //map ids to list of consective integers //todo would this be faster in MIL?
        HashMap<Integer, Integer> indicesToObjIDMap = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> objIDToIndicesMap = new HashMap<Integer, Integer>();
        ResultSet resultSet = objNST.selectRows();
        int index = 0;
        while (resultSet.next()) {
            int objID = resultSet.getOID(1);
            indicesToObjIDMap.put(new Integer(index), new Integer(objID));
            objIDToIndicesMap.put(new Integer(objID), new Integer(index));
            index++;
        }
        // if numObjs is too low then stop partitioning
        if (index < minObjLimit) {
            List<List<Integer>> clusterList = new ArrayList<List<Integer>>();
            List<Integer> cluster = new ArrayList<Integer>();
            cluster.addAll(objIDToIndicesMap.keySet());
            clusterList.add(cluster);
            return clusterList;
        }

        //create weight and diagonal matrix and solve it
        EigenVectorSolver solver = new EigenVectorSolver(objIDToIndicesMap, linkNST);
        DoubleMatrix1D secondSmEVector = solver.solveEigensystem();

        // bin vector values and check stats about the eigenvector to see if we should stop partitioning
        int numBins = (int) Math.round((Math.log(secondSmEVector.size()) / Math.log(2)) + 1);
        DoubleMatrix1D sortedEvectorVals = secondSmEVector.viewSorted();
        double stabilityVal = solver.calculateEigenvectorStability(sortedEvectorVals, numBins);

        //if the eigenvector is stable then search for the best cut to return
        if (stabilityVal <= stabilityThresh) {
            DB.beginScope();
            double minVal = sortedEvectorVals.get(0);
            double maxVal = sortedEvectorVals.get(sortedEvectorVals.size() - 1);
            double[] binVals = solver.getBinnedEVectorValues(numBins, minVal, maxVal);
            double[] results = solver.calculateMinNormCut(secondSmEVector, binVals);
            double cutThreshold = results[0];
            double minNCut = results[1];
            log.debug("cutsize " + minNCut + ", for theshold " + cutThreshold + ", with stability " + stabilityVal);

            // create new subgraphs corresponding to the threshold
            NST objsANST = new NST("item_id", "oid");
            NST objsBNST = new NST("item_id", "oid");
            for (int i = 0; i < secondSmEVector.size(); i++) {
                double currEVecVal = secondSmEVector.get(i);
                if (currEVecVal <= cutThreshold) {
                    objsANST.insertRow(indicesToObjIDMap.get(i) + "");
                } else {
                    objsBNST.insertRow(indicesToObjIDMap.get(i) + "");
                }
            }

            // find links for each partition
            NST origLinkNST = DB.getLinkNST().intersect(linkNST, "link_id = item_id");

            NSTColumn aColumn = objsANST.getNSTColumn("item_id");
            NSTColumn bColumn = objsBNST.getNSTColumn("item_id");
            NST aLinkNST = origLinkNST.filter("o1_id IN " + aColumn + " AND o2_id IN " + aColumn, "link_id AS item_id");
            NST bLinkNST = origLinkNST.filter("o1_id IN " + bColumn + " AND o2_id IN " + bColumn, "link_id AS item_id");

            List<List<Integer>> clustersA = recursivelyPartition(objsANST, aLinkNST);
            List<List<Integer>> clustersB = recursivelyPartition(objsBNST, bLinkNST);
            List<List<Integer>> allClusters = new ArrayList<List<Integer>>();
            if (clustersA != null) {
                allClusters.addAll(clustersA);
            }
            if (clustersB != null) {
                allClusters.addAll(clustersB);
            }
            DB.endScope();
            return allClusters;

        } else {
            log.debug("stopped partitioning with stability " + stabilityVal + " and size " + index);
            List<List<Integer>> clusterList = new ArrayList<List<Integer>>();
            List<Integer> cluster = new ArrayList<Integer>();
            cluster.addAll(objIDToIndicesMap.keySet());
            clusterList.add(cluster);
            return clusterList;
        }
    }

    /**
     * Create a new container for output, and create one subgraph per cluster
     *
     * @param container
     * @param clusterList
     * @param outputContName
     * @return the container with the clusters
     */
    private Container saveClustersInSubgraphs(Container container, List<List<Integer>> clusterList, String outputContName) {
        Container outputContainer = DB.getRootContainer().createChild(outputContName);
        NST objectsNST = outputContainer.getObjectsNST();
        NST linkNST = outputContainer.getLinksNST();

        // Get the original links, and their end-points
        NST origLinkNST = container.getLinksNST().distinct("item_id").join(DB.getLinkNST(), "A.item_id = B.link_id");
        for (int clIdx = 0; clIdx < clusterList.size(); clIdx++) {

            // add all the objects
            List<Integer> clusterObjs = clusterList.get(clIdx);
            for (Integer objID : clusterObjs) {
                objectsNST.insertRow(objID + "," + clIdx + ", " + ORIGINAL_OBJECT_NAME_IN_CLUSTER);
            }

            // add all the original links that are still connecting objects in this cluster
            NST contObjNST = objectsNST.filter("subg_id = " + clIdx);
            NST joinedNST2 = origLinkNST.join(contObjNST, "o1_id = item_id").join(contObjNST, "o2_id = item_id", "link_id, B.subg_id").distinct("link_id");
            joinedNST2.addConstantColumn("link_name", "str", "link");
            linkNST.insertRowsFromNST(joinedNST2);
        }

        return outputContainer;
    }


    private class EigenVectorSolver {
        private DoubleMatrix2D[] matrix;

        /**
         * Create weight and diagonal matrix, with a weight attribute on the links
         *
         * @param objIDToIndicesMap
         * @param linkNST
         * @return
         */
        public EigenVectorSolver(Map<Integer, Integer> objIDToIndicesMap, NST linkNST) {
            int numObjs = objIDToIndicesMap.size();
            double[][] adjacencyValues = new double[numObjs][numObjs];

            NST linkWtNST = DB.getLinkNST().join(linkNST, "link_id = item_id", "o1_id, o2_id");
            ResultSet resultSet = linkWtNST.selectRows();
            while (resultSet.next()) {
                int obj1ID = resultSet.getOID(1);
                int obj2ID = resultSet.getOID(2);
                int obj1Idx = (objIDToIndicesMap.get(new Integer(obj1ID))).intValue();
                int obj2Idx = (objIDToIndicesMap.get(new Integer(obj2ID))).intValue();
                adjacencyValues[obj1Idx][obj2Idx] += 1.0;
                adjacencyValues[obj2Idx][obj1Idx] += 1.0;
            }
            linkWtNST.release();

            SparseDoubleMatrix2D weightMatix = new SparseDoubleMatrix2D(adjacencyValues);

            //create diagonal matrix (entries are weighted sums: d(i)=sum over j w(i,j), non-diagonal elements are 0)
            //todo use matrix algebra functions to do this (if we can)
            double[][] diagonalValues = new double[numObjs][numObjs];
            for (int idx = 0; idx < adjacencyValues.length; idx++) {
                double sumNeighborWts = 0;
                for (int neighborIdx = 0; neighborIdx < adjacencyValues.length; neighborIdx++) {
                    sumNeighborWts += adjacencyValues[idx][neighborIdx];
                }
                diagonalValues[idx][idx] = sumNeighborWts;
            }
            SparseDoubleMatrix2D diagonalMatrix = new SparseDoubleMatrix2D(diagonalValues);

            matrix = new DoubleMatrix2D[]{weightMatix, diagonalMatrix};
        }


        /**
         * Calculate min normal cut
         *
         * @param secondSmEVector
         * @param binVals
         * @return
         */
        private double[] calculateMinNormCut(DoubleMatrix1D secondSmEVector, double[] binVals) {
            double minNCut = -1;
            List<Integer> minThreshIndexes = new Vector<Integer>();
            for (int i = 0; i < binVals.length; i++) {
                double currNCut = calculateNormCut(binVals[i], secondSmEVector, matrix[0]);
                if (currNCut <= minNCut || minNCut == -1) {
                    if (currNCut < minNCut) {
                        minThreshIndexes = new Vector<Integer>();
                    }
                    minNCut = currNCut;
                    minThreshIndexes.add(new Integer(i));
                }
            }
            int index = ((Integer) StatUtil.randomChoice(minThreshIndexes)).intValue();
            double thresh = binVals[index];
            return new double[]{thresh, minNCut};
        }

        /**
         * Calculate normal cut
         *
         * @param eValThresh
         * @param eVector
         * @param weightMatrix
         * @return
         */
        private double calculateNormCut(double eValThresh, DoubleMatrix1D eVector, DoubleMatrix2D weightMatrix) {
            List<Integer> objsA = new Vector<Integer>();
            List<Integer> objsB = new Vector<Integer>();
            List<Integer> objs = new Vector<Integer>();
            for (int i = 0; i < eVector.size(); i++) {
                double currEVecVal = eVector.get(i);
                if (currEVecVal <= eValThresh) {
                    objsA.add(new Integer(i));
                } else {
                    objsB.add(new Integer(i));
                }
                objs.add(new Integer(i));
            }
            double cutAB = calculateAssoc(objsA, objsB, weightMatrix);
            double assocAV = calculateAssoc(objsA, objs, weightMatrix);
            double assocBV = calculateAssoc(objsB, objs, weightMatrix);

            double ncut = 0.0;
            if (assocAV > 0) {
                ncut = ncut + (cutAB / assocAV);
            }
            if (assocBV > 0) {
                ncut = ncut + (cutAB / assocBV);
            }
            return ncut;
        }

        /**
         * Calculate association between a pair of objects
         *
         * @param objs1
         * @param objs2
         * @param weightMatrix
         * @return
         */
        private double calculateAssoc(List<Integer> objs1, List<Integer> objs2, DoubleMatrix2D weightMatrix) {
            //Return the summed weights over the links between objects1 and objects2
            double totalAssoc = 0.0;
            for (int o1Idx = 0; o1Idx < objs1.size(); o1Idx++) {
                int o1ID = (objs1.get(o1Idx)).intValue();
                for (int o2Idx = 0; o2Idx < objs2.size(); o2Idx++) {
                    int o2ID = (objs2.get(o2Idx)).intValue();
                    totalAssoc += weightMatrix.get(o1ID, o2ID);
                }

            }
            return totalAssoc;
        }

        /**
         * Calculate the stability of an eigenvector
         *
         * @param sortedEvectorVals
         * @param numBins
         * @return
         */
        public double calculateEigenvectorStability(DoubleMatrix1D sortedEvectorVals, int numBins) {
            // decide whether to partition or not
            // 	bin eigenvector values by value into XX evenly spaced bins
            //	take ratio of minimum bin frequency to maximim bin frequency
            double minVal = sortedEvectorVals.get(0);
            double maxVal = sortedEvectorVals.get(sortedEvectorVals.size() - 1);
            double step = (maxVal - minVal) / (numBins + 1.0);
            int binFreq = 0;
            double thresh = minVal + step;
            int maxFreq = -1;
            int minFreq = -1;

            // cycle through eigenvector values
            for (int i = 0; i < sortedEvectorVals.size(); i++) {
                // get evectorval
                double v = sortedEvectorVals.get(i);
                // if val is below threshold increment count for this bin
                if (v <= thresh) {
                    binFreq = binFreq + 1;
                }
                // otherwise check bin size, reset and continue
                else {
                    // check min,max before resetting
                    if (binFreq < minFreq || minFreq == -1) {
                        minFreq = binFreq;
                    }
                    if (binFreq > maxFreq || maxFreq == -1) {
                        maxFreq = binFreq;
                    }
                    // reset values
                    while (true) {
                        thresh = thresh + step;
                        if (v <= thresh) {
                            binFreq = 1;
                            break;
                        } else {
                            minFreq = 0;
                            if (thresh >= maxVal) {
                                break;
                            }
                        }
                    }
                }
            }
            // check last frequency
            if (binFreq < minFreq || minFreq == -1) {
                minFreq = binFreq;
            }
            if (binFreq > maxFreq) {
                maxFreq = binFreq;
            }
            return (1.0 * minFreq) / maxFreq; //stability
        }


        public double[] getBinnedEVectorValues(int numBins, double minVal, double maxVal) {
            double[] binVals = new double[numBins];
            double step = (maxVal - minVal) / (numBins + 1.0);
            double binVal = minVal + step;
            for (int i = 0; i < numBins - 1; i++) {
                binVals[i] = binVal;
                binVal = binVal + step;
            }
            return binVals;
        }


        private DoubleMatrix1D solveEigensystem() {
            DoubleMatrix2D weightMatrix = matrix[0];
            DoubleMatrix2D diagonalMatrix = matrix[1];

            // solve eigenvalue system D^-.5(D-W)D^-.5 z=lambda z

            // compute D-W  //todo use matrix algebra functions to do this
            double[][] adjacencyValues = weightMatrix.toArray();
            double[][] diagonalValues = diagonalMatrix.toArray();
            int numObjs = adjacencyValues.length;
            double[][] dMinusWValues = new double[numObjs][numObjs];
            for (int idx1 = 0; idx1 < dMinusWValues.length; idx1++) {
                for (int idx2 = 0; idx2 < dMinusWValues[0].length; idx2++) {
                    dMinusWValues[idx1][idx2] = diagonalValues[idx1][idx2] - adjacencyValues[idx1][idx2];
                }
            }
            SparseDoubleMatrix2D dMinusWMatrix = new SparseDoubleMatrix2D(dMinusWValues);

            // compute D^(-0.5) //todo use matrix algebra functions to do this
            //  take square root of D, then 1/diag entries (otherwise 1/0->inf)
            double[][] dMinusHalfValues = new double[numObjs][numObjs];
            for (int idx1 = 0; idx1 < dMinusHalfValues.length; idx1++) {
                double dVal = diagonalValues[idx1][idx1];
                double newVal = Math.sqrt(dVal) > 0 ? 1.0 / Math.sqrt(dVal) : 0;
                dMinusHalfValues[idx1][idx1] = newVal;
            }
            SparseDoubleMatrix2D dMinusHalfMatrix = new SparseDoubleMatrix2D(dMinusHalfValues);

            // compute transformed eigensystem: D^-.5(D-W)D^-.5
            DoubleMatrix2D firstMultMatrix = dMinusHalfMatrix.zMult(dMinusWMatrix, null);
            DoubleMatrix2D transformedES = firstMultMatrix.zMult(dMinusHalfMatrix, null);

            // solve eigensystem D^-.5(D-W)D^-.5 z=lambda z for z
            EigenvalueDecomposition evalueDecomposition = new EigenvalueDecomposition(transformedES);
            DoubleMatrix2D zMatrix = evalueDecomposition.getV();
            DoubleMatrix1D evalues = evalueDecomposition.getRealEigenvalues();

            // get solution to original eigensystem, z=D^.5y
            DoubleMatrix2D yMatrix = dMinusHalfMatrix.zMult(zMatrix, null);

            // sort indexes of eigenvalues, find index of 2nd smallest eigenvalue
            DoubleMatrix1D sortedEValues = evalues.viewSorted();
            double secondSmEVal = sortedEValues.getQuick(1);
            int secondSmIdx = -1;
            for (int idx = 0; idx < numObjs; idx++) {
                double eval = evalues.getQuick(idx);
                if (eval == secondSmEVal) {
                    secondSmIdx = idx;
                }
            }

            //return eigenvector corresponding to the 2nd smallest eigenvalue
            return yMatrix.viewColumn(secondSmIdx);
        }
    }

}
