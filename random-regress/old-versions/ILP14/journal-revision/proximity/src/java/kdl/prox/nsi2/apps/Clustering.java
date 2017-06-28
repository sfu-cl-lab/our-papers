/**
 * $Id: Clustering.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.apps;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.nsi2.graph.ArtificialGraph;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.graph.Node;
import kdl.prox.nsi2.nsi.NSI;
import kdl.prox.nsi2.search.Search;
import kdl.prox.nsi2.util.GraphUtils;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * The Clustering class consists of various methods to cluster a graph.  The two major clustering techniques
 * are kmeans and betweenness components, which are made tractable by the use of NSIs.
 */
public class Clustering {
    private static final Logger log = Logger.getLogger(Clustering.class);
    private static final Random rand = new Random();

    private static Map<Integer, Integer> closest(Collection<Integer> nodes, Collection<Integer> beacons, NSI nsi) {
        Map<Integer, Integer> nodeToBeacon = new HashMap<Integer, Integer>();
        for (Integer node : nodes) {
            List<Integer> closestIds = new ArrayList<Integer>();
            double closestDist = Double.MAX_VALUE;
            for (Integer beacon : beacons) {
                double d = nsi.distance(node, beacon);
                if (d == closestDist) {
                    closestIds.add(beacon);
                } else if (d < closestDist) {
                    closestIds.clear();
                    closestIds.add(beacon);
                    closestDist = d;
                }
            }
            if (closestIds.size() == 1) {
                nodeToBeacon.put(node, closestIds.get(0));
            } else {
                nodeToBeacon.put(node, closestIds.get(rand.nextInt(closestIds.size())));
            }
        }
        return nodeToBeacon;
    }

    /**
     * Performs k-medoids using NSIs for distance approximation. NB: If actual searching is desired,
     * use the SearchNSI with the desired search method.
     * @param k Number of clusters
     * @param distAss NSI to use for estimating distances in assigning nodes to clusters
     * @param distCent NSI to use for estimating distances in recomuputing centroid/medoids
     * @param iterationMax Maximum number of iterations to perform
     * @return Map corresponding to clusters: centroidId --> cluster members (List<Integer>)
     */
    public static Map<Integer, List<Integer>> kmeans(int k, NSI distAss, NSI distCent, int iterationMax) {
        List<Integer> nodes = DB.getObjectNST().selectRows().toOIDList("id");
        List<Integer> seeds = StatUtil.sampleList(nodes, k);

        // return Map
        Map<Integer, List<Integer>> centroidToCluster = new HashMap<Integer, List<Integer>>();

        log.debug("assigning " + nodes.size() + " nodes to " + seeds.size() + " clusters");
        Map<Integer, Integer> nodeToCentroid = closest(nodes, seeds, distAss);

        int iteration = 0;
        while (iteration++ < iterationMax) {
            log.debug("k-means iteration " + iteration);

            log.debug("creating cluster lists");
            centroidToCluster.clear();
            for (Map.Entry<Integer, Integer> mapent : nodeToCentroid.entrySet()) {
                Integer node = mapent.getKey();
                Integer cent = mapent.getValue();
                if (centroidToCluster.containsKey(cent)) {
                    centroidToCluster.get(cent).add(node);
                } else {
                    List<Integer> cluster = new ArrayList<Integer>();
                    cluster.add(node);
                    centroidToCluster.put(cent, cluster);
                }
            }
            log.debug("divided up into " + centroidToCluster.size() + " clusters");

            log.debug("recomputing centroids");
            Set<Integer> centroidsOld = centroidToCluster.keySet();
            Set<Integer> centroidsUnchanged = new HashSet<Integer>();
            Set<Integer> centroidsChanged = new HashSet<Integer>();
            Map<Integer, Integer> centroidsNewToOld = new HashMap<Integer, Integer>();
            for (Integer centroidOld : centroidsOld) {
                Integer centroidNew = centroid(centroidToCluster.get(centroidOld), distCent, 1000, centroidOld);
                if (centroidNew.equals(centroidOld)) {
                    centroidsUnchanged.add(centroidNew);
                } else {
                    centroidsChanged.add(centroidNew);
                }
                centroidsNewToOld.put(centroidNew, centroidOld);
            }
            log.debug(centroidsChanged.size() + " / " + centroidsOld.size() + " centroids changed");

            if (centroidsChanged.size() < 0.03 * centroidToCluster.size()) {
                // converged!
                return centroidToCluster;
            } else {
                // need to recheck any node in a cluster with a changed centroid against all centroids
                List<Integer> nodesChanged = new ArrayList<Integer>();
                for (Integer centroidNew : centroidsChanged) {
                    nodesChanged.addAll(centroidToCluster.get(centroidsNewToOld.get(centroidNew)));
                }
                log.debug("(re)assigning " + nodesChanged.size() + " nodes to " + centroidsNewToOld.size() + " clusters");
                Map<Integer, Integer> nodeToCentroidChanged = closest(nodesChanged, centroidsNewToOld.keySet(), distAss);
                nodeToCentroid.putAll(nodeToCentroidChanged);

                // need to check the rest of the nodes to see if they're closer to new centroids
                for (Integer centroidOld : centroidsUnchanged) {
                    List<Integer> cluster = centroidToCluster.get(centroidOld);

                    List<Integer> centroidsChangedPlus = new ArrayList<Integer>(centroidsChanged);
                    centroidsChangedPlus.add(centroidOld);

                    log.debug("(re)assigning " + cluster.size() + " nodes to " + centroidsChangedPlus.size() + " clusters");
                    Map<Integer, Integer> nodeToCentroidUnchanged = closest(cluster, centroidsChangedPlus, distAss);
                    nodeToCentroid.putAll(nodeToCentroidUnchanged);
                }

            }
        }

        // if we made it here, we never converged!
        log.debug("kmeans did not converge after " + iterationMax + " iterations");
        return centroidToCluster;
    }

    /**
     * Performs modal cluster reassignment after an initial clustering.
     * @param centroidToCluster Map corresponding to an initial clustering: centroidId --> cluster members (List<Integer>)
     * @param graph Graph abstraction (e.g., InMemoryGraph), needed to find neighbors of nodes
     * @return Map corresponding to revised clusters: centroidId --> cluster members (List<Integer>)
     */
    public static Map<Integer, List<Integer>> reviseClustersModal(Map<Integer, List<Integer>> centroidToCluster, Graph graph) {
        Map<Integer, Integer> nodeToCentroid = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, List<Integer>> e : centroidToCluster.entrySet()) {
            Integer centroid = e.getKey();
            for (Integer node : e.getValue()) {
                nodeToCentroid.put(node, centroid);
            }
        }

        int i = 0;
        int changedTotal = 0;
        int changed = 0;

        List<Integer> ranking = DB.getObjectNST().selectRows().toOIDList("id");
        Collections.shuffle(ranking);

        for (Integer node : ranking) {
            i++;

            Integer centroid = nodeToCentroid.get(node);

            Set<Node> neighbors = graph.getNeighbors(node);
            List<Object> neighborClusters = new ArrayList<Object>();
            for (Node neighbor : neighbors) {
                neighborClusters.add(nodeToCentroid.get(neighbor.id));
            }

            Integer modal = (Integer) mode(neighborClusters);
            if ((i < 100000000) && (modal != null) && (!modal.equals(centroid))) {
                nodeToCentroid.put(node, modal);
                changed++;
                changedTotal += i;
            }
        }
        log.debug("changed " + changed + " nodes");
        log.debug("avg rank changed " + (1.0 * changedTotal / changed) + " / " + i);

        Map<Integer, List<Integer>> revised = new HashMap<Integer, List<Integer>>();
        for (Map.Entry<Integer, Integer> e : nodeToCentroid.entrySet()) {
            Integer centroid = e.getValue();
            if (!revised.containsKey(centroid)) {
                revised.put(centroid, new ArrayList<Integer>());
            }
            revised.get(centroid).add(e.getKey());
        }
        return revised;
    }

    private static Object mode(List<Object> vals) {
        Map<Object, Integer> counts = new HashMap<Object, Integer>();
        for (Object val : vals) {
            if (!counts.containsKey(val)) {
                counts.put(val, 1);
            } else {
                counts.put(val, counts.get(val) + 1);
            }
        }

        int maxCount = 0;
        List<Object> maxVals = new ArrayList<Object>();
        for (Map.Entry<Object, Integer> e : counts.entrySet()) {
            int count = e.getValue();
            if (count > maxCount) {
                maxVals.clear();
                maxCount = count;
                maxVals.add(e.getKey());
            } else if (count == maxCount) {
                maxVals.add(e.getKey());
            }
        }
        if (maxVals.size() > 1) {
            return null;
        } else {
            return maxVals.get(0);
        }
    }

    private static Integer centroid(List<Integer> cluster, NSI nsi, int compares, Integer currentCent) {
        int clusterSize = cluster.size();

        if (clusterSize <= 2) {
            return cluster.get(rand.nextInt(clusterSize));
        }

        List<Integer> nodes = new ArrayList<Integer>();
        if (currentCent != null) {
            nodes.add(currentCent);
        }
        nodes.addAll(cluster);

        double winnerDistTotal = Double.MAX_VALUE;
        List<Integer> winnerNodes = new ArrayList<Integer>();
        for (Integer candidate : nodes) {
            double candidateDistTotal = 0.0;

            // a zero value could really tip the scales toward a node lucky enough to draw itself
            // as a comparison, so do some dancing to avoid using yourself as a target
            List<Integer> targets = StatUtil.sampleList(cluster, compares + 1);
            if (!targets.remove(candidate)) {
                targets.remove(0);
            }

            for (Integer target : targets) {
                candidateDistTotal += nsi.distance(candidate, target);

                // short circuit if we have no chance of winning
                if (candidateDistTotal > winnerDistTotal) {
                    break;
                }
            }
            if (candidateDistTotal == winnerDistTotal) {
                winnerNodes.add(candidate);
            } else if (candidateDistTotal < winnerDistTotal) {
                winnerNodes.clear();
                winnerNodes.add(candidate);
                winnerDistTotal = candidateDistTotal;
            }
        }

        return winnerNodes.get(rand.nextInt(winnerNodes.size()));
    }

    /**
     * Convert a clustering result in a map to an NST.
     * @param clusters Map corresponding to an initial clustering: centroidId --> cluster members (Collection<Integer>)
     * @return An NST corresponding to the clustering.  Will have two columns: id, centroid
     */
    public static NST toNST(Map<Integer, Collection<Integer>> clusters) {
        return toNST(clusters, null);
    }

    /**
     * Convert a clustering result in a map to an NST, adds distance to centroid for each node if nsi is specified.
     * @param clusters Map corresponding to an initial clustering: centroidId --> cluster members (Collection<Integer>)
     * @param nsi NSI used to estimate each nodes distance to its respective centroid. If null, will not add
     * distance to the resulting NST
     * @return An NST corresponding to the clustering.  Will have three columns: id, centroid, dist. If nsi is null,
     * NST will only consist of the first two columns.
     */
    public static NST toNST(Map<Integer, Collection<Integer>> clusters, NSI nsi) {

        List<Integer[]> tuples = new ArrayList<Integer[]>();
        for (Map.Entry<Integer, Collection<Integer>> e : clusters.entrySet()) {
            Integer centroid = e.getKey();
            for (Integer node : e.getValue()) {
                tuples.add(new Integer[]{node, centroid});
            }
        }

        Integer[][] data = new Integer[tuples.size()][];

        if (nsi == null) {
            int i = 0;
            for (Integer[] tuple : tuples) {
                data[i++] = tuple;
            }
            NST clusterNST = new NST("id,centroid", "oid,oid");
            clusterNST.fastInsert(data); // nodeId, controid
            return clusterNST;
        } else {
            int i = 0;
            for (Integer[] tuple : tuples) {
                Integer d = (int) nsi.distance(tuple[0], tuple[1]);
                data[i++] = new Integer[]{tuple[0], tuple[1], d};
            }
            NST clusterNST = new NST("id,centroid,dist", "oid,oid,int");
            clusterNST.fastInsert(data); // nodeId, controid, int
            return clusterNST;
        }
    }

    /**
     * Converts an NST corresponding to a clustering of nodes into a list of lists for each cluster
     * @param idclust NST with two columns: id, cluster
     * @return A list of lists for each cluster of nodes
     */
    public static List<List<Integer>> clusterListsFromNST(NST idclust) {
        Map<Integer, List<Integer>> cidToClust = new HashMap<Integer, List<Integer>>();

        ResultSet resultSet = idclust.selectRows();
        while (resultSet.next()) {
            Integer id = resultSet.getInt(0);
            Integer clust = resultSet.getInt(1);
            if (!cidToClust.containsKey(clust)) {
                cidToClust.put(clust, new ArrayList<Integer>());
            }
            cidToClust.get(clust).add(id);
        }
        return new ArrayList<List<Integer>>(cidToClust.values());
    }

    /**
     * Performs clustering by computing link betweenness centrality, and iteratively removing the most central link
     * and finding the connected components.
     * @param k Number of clusters
     * @param search Search method used to estimate link betweenness centrality. NB: Search method must
     * be able to return paths.  Breadth-first cannot be used.
     * @param num Number of searches to use for betweenness estimation.
     * @return Map corresponding to clusters: centroidId --> cluster members (List<Integer>), here centroid is
     * chosen arbitrarily
     */
    public static Map<Integer, List<Integer>> betweennessComponents(int k, Search search, int num) {

        List<Integer[]> linkRanking = Centrality.betweenCentralLinkRanking(num, search);

        NST linkNST = DB.getLinkNST().copy();
        Map<Integer, List<Integer>> compMap = GraphUtils.getComponentMap(DB.getObjectNST(), linkNST);
        log.debug("iter 0: " + linkNST.getRowCount() + " links,  " + compMap.size() + " components");

        int iter = 0;
        for (Integer[] pair : linkRanking) {
            Integer node1 = pair[0];
            Integer node2 = pair[1];

            linkNST.deleteRows("o1_id EQ '" + node1 + "' AND o2_id EQ '" + node2 + "'");
            linkNST.deleteRows("o2_id EQ '" + node1 + "' AND o1_id EQ '" + node2 + "'");
            compMap = GraphUtils.getComponentMap(DB.getObjectNST(), linkNST);
            int numComponents = compMap.size();
            log.debug("iter " + ++iter + ": " + linkNST.getRowCount() + " links,  " + numComponents + " components");

            if (k == numComponents) {
                break;
            }
        }

        return compMap;
    }

    /**
     * Performs clustering by iteratively computing link betweenness centrality, removing the most central link
     * and finding the connected components. This method should be used instead of betweennessComponents.  The
     * betweenness estimates here are more accurate since they are recomputed each iteration.
     * @param k Number of clusters
     * @param search Search method used to estimate link betweenness centrality. NB: Search method must
     * be able to return paths.  Breadth-first cannot be used.
     * @param num Number of searches to use for betweenness estimation.
     * @return Map corresponding to clusters: centroidId --> cluster members (List<Integer>), here centroid is
     * chosen arbitrarily
     */
    public static Map<Integer, List<Integer>> betweennessComponentsRecalc(int k, Search search, int num) {

        NST linkNST = DB.getLinkNST().copy();

        // make a neighbors hash out of the linkNST that we got
        Map<Integer, Set<Node>> neighborSets = new HashMap<Integer, Set<Node>>();

        ResultSet resultSet = linkNST.selectRows();
        while (resultSet.next()) {
            Integer node1 = resultSet.getOID("o1_id");
            Integer node2 = resultSet.getOID("o1_id");

            if (!neighborSets.containsKey(node1)) {
                neighborSets.put(node1, new HashSet<Node>());
            }
            if (!neighborSets.containsKey(node2)) {
                neighborSets.put(node2, new HashSet<Node>());
            }
            neighborSets.get(node1).add(new Node(node2, 1.0));
            neighborSets.get(node2).add(new Node(node1, 1.0));
        }

        ArtificialGraph graph = new ArtificialGraph(neighborSets);
        search.setGraph(graph);

        Map<Integer, List<Integer>> compMap = GraphUtils.getComponentMap(DB.getObjectNST(), linkNST);
        log.debug("iter 0: " + linkNST.getRowCount() + " links,  " + compMap.size() + " components");

        int iter = 0;
        while (linkNST.getRowCount() > 0) {
            compMap = GraphUtils.getComponentMap(DB.getObjectNST(), linkNST);
            int numComponents = compMap.size();
            log.debug("iter " + ++iter + ": " + linkNST.getRowCount() + " links,  " + numComponents + " components");
            if (k == numComponents) {
                break;
            }

            // get back Map with: nodeId -> [ nodes ]
            Integer champNode1 = 0;
            Integer champNode2 = 0;
            double champScore = 0;

            for (List<Integer> comp : compMap.values()) {
                if (comp.size() < 2) {
                    log.debug("skipping component of size " + comp.size());
                    continue;
                }

                // get the between for the comp
                int betPairCount = (int) Math.min((double) num, (double) comp.size() * (comp.size() - 1) / 2);
                Object[] elts = Centrality.betweenCentralLinkRanking(betPairCount, search, comp);

                Integer p1 = (Integer) elts[0];
                Integer p2 = (Integer) elts[1];
                double score = (Double) elts[2] * comp.size();

                log.debug("challenger from comp of size " + comp.size() + " has between " + score);
                if (score > champScore) {
                    log.debug("new champ!");
                    champNode1 = p1;
                    champNode2 = p2;
                    champScore = score;
                }
            }

            linkNST.deleteRows("o1_id EQ '" + champNode1 + "' AND o2_id EQ '" + champNode2 + "'");
            linkNST.deleteRows("o2_id EQ '" + champNode1 + "' AND o1_id EQ '" + champNode2 + "'");

            graph.links.get(champNode1).remove(new Node(champNode2, 1.0));
            graph.links.get(champNode2).remove(new Node(champNode1, 1.0));

            log.debug("");
        }
        linkNST.release();

        return compMap;
    }

    public static List[] getPairsList(int inCount, int outCount, NST clustersOldNST, Random r) {
        List<List<Integer>> clustersOld = clusterListsFromNST(clustersOldNST);
        List<Integer[]> pairsListIn = new ArrayList<Integer[]>();
        List<Integer[]> pairsListOut = new ArrayList<Integer[]>();

        for (int i = 0; i < inCount; i++) {
            // pick a random pair of nodes from a random cluster
            List<Integer> clust = clustersOld.get(r.nextInt(clustersOld.size()));
            Integer node1 = clust.get(r.nextInt(clust.size()));
            Integer node2 = clust.get(r.nextInt(clust.size()));
            while (node1.equals(node2)) {
                node2 = clust.get(r.nextInt(clust.size()));
            }

            pairsListIn.add(new Integer[]{node1, node2});
        }

        for (int i = 0; i < outCount; i++) {
            // pick a random pair of nodes from different random cluster
            List<Integer> clust1 = clustersOld.get(r.nextInt(clustersOld.size()));
            List<Integer> clust2 = clustersOld.get(r.nextInt(clustersOld.size()));
            while (clust1.equals(clust2)) {
                clust2 = clustersOld.get(r.nextInt(clustersOld.size()));
            }
            Integer node1 = clust1.get(r.nextInt(clust1.size()));
            Integer node2 = clust2.get(r.nextInt(clust2.size()));

            pairsListOut.add(new Integer[]{node1, node2});
        }
        return new List[]{pairsListIn, pairsListOut};
    }

    public static double[] pairwiseAccuracy(List<Integer[]>[] pairs, NST clustersNewNST) {
        Map<Integer, Integer> clustersNewMap = new HashMap<Integer, Integer>();
        ResultSet resultSet = clustersNewNST.selectRows();
        while (resultSet.next()) {
            clustersNewMap.put(resultSet.getInt(1), resultSet.getInt(2));
        }

        int correctIn = 0;
        int incorrectIn = 0;
        for (Integer[] pair : pairs[0]) {
            Integer node1 = pair[0];
            Integer node2 = pair[1];

            if (clustersNewMap.get(node1).equals(clustersNewMap.get(node2))) {
                correctIn++;
            } else {
                incorrectIn++;
            }
        }
        double accIn = 1.0 * correctIn / pairs[0].size();
        log.debug("same accuracy: " + correctIn + " correct, " + incorrectIn + " incorrect, acc: " + accIn);

        int correctOut = 0;
        int incorrectOut = 0;
        for (Integer[] pair : pairs[1]) {
            Integer node1 = pair[0];
            Integer node2 = pair[1];

            if (clustersNewMap.get(node1).equals(clustersNewMap.get(node2))) {
                incorrectOut++;
            } else {
                correctOut++;
            }
        }
        double accOut = 1.0 * correctOut / pairs[1].size();
        log.debug("different accuracy: " + correctOut + " correct, " + incorrectOut + " incorrect, acc: " + accOut);

        double acc = (1.0 * correctIn + correctOut) / (pairs[0].size() + pairs[1].size());
        log.debug("combined accuracy: " + (correctIn + correctOut) + " correct, " + (incorrectIn + incorrectOut) + " incorrect, acc: " + acc);

        return new double[]{accIn, accOut};
    }

    public static double[] pairwiseAccuracy(int inCount, int outCount, NST clustersOldNST, NST clustersNewNST, Random r) {
        List<List<Integer>> clustersOld = clusterListsFromNST(clustersOldNST);

        Map<Integer, Integer> clustersNewMap = new HashMap<Integer, Integer>();
        ResultSet resultSet = clustersNewNST.selectRows();
        while (resultSet.next()) {
            clustersNewMap.put(resultSet.getInt(1), resultSet.getInt(2));
        }

        int correctIn = 0;
        int incorrectIn = 0;
        for (int i = 0; i < inCount; i++) {
            // pick a random pair of nodes from a random cluster
            List<Integer> clust = clustersOld.get(r.nextInt(clustersOld.size()));
            Integer node1 = clust.get(r.nextInt(clust.size()));
            Integer node2 = clust.get(r.nextInt(clust.size()));
            while (node1.equals(node2)) {
                node2 = clust.get(r.nextInt(clust.size()));
            }

            if (clustersNewMap.get(node1).equals(clustersNewMap.get(node2))) {
                correctIn++;
            } else {
                incorrectIn++;
            }
        }
        double accIn = 1.0 * correctIn / inCount;
        log.debug("same accuracy: " + correctIn + " correct, " + incorrectIn + " incorrect, acc: " + accIn);

        int correctOut = 0;
        int incorrectOut = 0;
        for (int i = 0; i < inCount; i++) {
            // pick a random pair of nodes from different random cluster
            List<Integer> clust1 = clustersOld.get(r.nextInt(clustersOld.size()));
            List<Integer> clust2 = clustersOld.get(r.nextInt(clustersOld.size()));
            while (clust1.equals(clust2)) {
                clust2 = clustersOld.get(r.nextInt(clustersOld.size()));
            }
            Integer node1 = clust1.get(r.nextInt(clust1.size()));
            Integer node2 = clust2.get(r.nextInt(clust2.size()));

            if (clustersNewMap.get(node1).equals(clustersNewMap.get(node2))) {
                incorrectOut++;
            } else {
                correctOut++;
            }
        }
        double accOut = 1.0 * correctOut / outCount;
        log.debug("different accuracy: " + correctOut + " correct, " + incorrectOut + " incorrect, acc: " + accOut);

        double acc = (1.0 * correctIn + correctOut) / (inCount + outCount);
        log.debug("combined accuracy: " + (correctIn + correctOut) + " correct, " + (incorrectIn + incorrectOut) + " incorrect, acc: " + acc);

        return new double[]{accIn, accOut};
    }

}
