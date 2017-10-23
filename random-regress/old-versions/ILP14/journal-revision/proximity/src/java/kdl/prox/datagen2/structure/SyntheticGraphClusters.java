/**
 * $Id: SyntheticGraphClusters.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import cern.jet.random.Binomial;
import cern.jet.random.engine.RandomEngine;
import kdl.prox.db.DB;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Creates a cluster graph
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphClusters {
    private static Logger log = Logger.getLogger(SyntheticGraphClusters.class);

    // lattice graph model
    public SyntheticGraphClusters(int n, int mean, double stdev, double probIn, double probOut) {
        log.info("creating cluster graph with n: " + n + ", mean: " + mean + ", stdev: " + stdev + ", probIn: " + probIn + ", probOut: " + probOut);

        SyntheticGraphUtil.createObjects(n);
        List<Integer> nodeIds = DB.getObjectNST().selectRows().toOIDList("id");

        log.debug("have " + nodeIds.size() + " objects");

        //Map<Integer, Integer> clusters = new HashMap<Integer, Integer>(); // nodeId -> clusterId
        List<List<Integer>> clusters = new ArrayList<List<Integer>>();
        final Random rand = new Random();

        int nodeIndex = 0;
        while (nodeIndex < nodeIds.size()) {

            int c = (int) Math.round(stdev * rand.nextGaussian()) + mean;
            if (c < 1) {
                continue;
            }

            // don't let go past the last node index, and don't let us have scraps at the end
            int nodeIndexEnd = nodeIndex + c;
            int leftover = nodeIds.size() - nodeIndexEnd;

            log.debug("c: " + c + ", nodeIndex: " + nodeIndex + ", nodeIndexEnd: " + nodeIndexEnd + ", leftover: " + leftover);
            if (leftover < 0) {
                nodeIndexEnd = nodeIds.size();
                log.debug("cluster too big (cluster " + c + ", leftover " + leftover + ")");
            } else if (leftover <= (mean - 3 * stdev)) {
                nodeIndexEnd += leftover;
                c += leftover;
                log.debug("leftover too small (cluster " + c + ", leftover " + leftover + ")");
            }

            List<Integer> cluster = nodeIds.subList(nodeIndex, nodeIndexEnd);
            clusters.add(cluster);
            //for (Integer nodeId : nodeIds.subList(nodeIndex, nodeIndex + c)) {
            //clusters.put(nodeId, currClusterId);
            //}

            nodeIndex += c;
        }
        log.debug("assigned " + clusters.size() + " clusters");

        RandomEngine randEng = new RandomEngine() {
            public int nextInt() {
                return rand.nextInt();
            }
        };

        List<Object[]> links = new ArrayList<Object[]>();
        int currLinkId = 0;
        List<Object[]> clusterAttr = new ArrayList<Object[]>();
        int currClustId = 0;

        List<Integer> connected = new ArrayList<Integer>();
        connected.add(clusters.get(0).get(0));

        for (List<Integer> cluster : clusters) {
            log.debug("linking cluster " + currClustId + " (" + cluster.size() + ")");
            int clusterSize = cluster.size();
            int clusterComplementSize = n - clusterSize;
            Binomial binIn = new Binomial(clusterSize - 1, probIn, randEng);
            Binomial binOut = new Binomial(clusterComplementSize, probOut, randEng);
            int clusterOutLinks = 0;
            for (Integer nodeId : cluster) {
                int degreeIn = Math.max(binIn.nextInt(), 1);
                List<Integer> neighborsInCluster = sampleList(degreeIn, cluster, Collections.singletonList(nodeId));
                for (Integer neighbor : neighborsInCluster) {
                    links.add(new Integer[]{currLinkId++, nodeId, neighbor});
                }

                int degreeOut = binOut.nextInt();
                clusterOutLinks += degreeOut;
                List<Integer> neighborsOutCluster = sampleList(degreeOut, nodeIds, cluster);
                for (Integer neighbor : neighborsOutCluster) {
                    links.add(new Integer[]{currLinkId++, nodeId, neighbor});
                }

                clusterAttr.add(new Integer[]{nodeId, currClustId});
            }
            if (clusterOutLinks == 0) {
                log.debug("connected lonely cluster");
                Integer nodeId = cluster.get(rand.nextInt(cluster.size()));
                links.add(new Integer[]{currLinkId++, nodeId, sampleList(1, connected, new ArrayList()).get(0)});
            }
            connected.addAll(cluster);
            currClustId++;
        }
        log.debug("created " + links.size() + " links");

        Object[][] data = new Object[links.size()][];
        for (int i = 0; i < links.size(); i++) {
            //log.debug("created link: " + Arrays.toString(links.get(i)));
            data[i] = links.get(i);
        }
        DB.getLinkNST().fastInsert(data);

        Object[][] dataClust = new Object[clusterAttr.size()][];
        for (int i = 0; i < clusterAttr.size(); i++) {
            dataClust[i] = clusterAttr.get(i);
        }
        DB.getObjectAttrs().defineAttributeIfNotExists("cluster", "int");
        DB.getObjectAttrs().getAttrDataNST("cluster").deleteRows().fastInsert(dataClust);
    }


    private List<Integer> sampleList(int num, List<Integer> items, List exclude) {
        List<Integer> shuffled = new ArrayList<Integer>(items);
        shuffled.removeAll(exclude);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, num);
    }


}
