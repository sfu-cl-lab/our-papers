/**
 * $Id: ZoneNSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

import kdl.prox.db.DB;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.util.ConversionUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * The Zone NSI works by assigning each node a label corresponding to one of k zones
 * The zones are chosen by randomly flooding from k seed nodes until all nodes have been labeled.
 * This process is repeated d times, resulting in a vector of d labels for each node.
 *
 * It is fast to index the graph.
 * Zone NSI does not provide an accurate estimate of distance, but works well when used for searching.
 *
 * The distance measure is the proportion of zone matches that two nodes share.
 *
 */
public class ZoneNSI implements NSI {
    private static Logger log = Logger.getLogger(ZoneNSI.class);
    private Random rand = new Random();
    private Map<Integer, int[]> nodeZones = new HashMap<Integer, int[]>(); // nodeId -> [d]

    /**
     * Create a Zone NSI
     * @param k     number of different possible labels
     * @param d     length of each vector
     * @param r     number of neighbors to flood to at each iteration
     * @param graph the view of the graph
     */
    public ZoneNSI(int k, int d, int r, Graph graph) {
     /*Each node will be labelled with a vector of discrete values
      * start with graph G = (V,E), |V| = n
      * choose a k for the number of different (discrete!) labels L = { 0, 1, 2, ..., k-1 }
      * choose a d for the length of the label vector
      *
      * for (i = 0 to d-1):
      * nodes_expanded = {}
      * nodes_labelled = { k randomly selected nodes }
      * nodes_unlabelled = V - nodes_labelled
      *
      * // randomly assign "seed" labels
      * foreach v in nodes_labelled {
      * label[v, i] = L[j]
      * j ++
      *
      * while (|nodes_unlabelled| > 0):
      * v = randomly selected node in nodes_labelled
      * nodes_labelled = nodes_labelled - v
      * nodes_expanded = nodes_expanded + v
      * foreach u in neighbors(v)
      * if u in nodes_unlabelled
      * label[u, i] = label[v, i]
      * nodes_unlabelled = nodes_unlabelled - u
      * nodes_labelled = nodes_labelled + u
      * 
      * addendum: to minimize bias, we now expand labelled nodes in "tiers", holding the nodes
      * of each generation in nodes_labelled_next
      */
        List<Integer> idList = DB.getObjectNST().selectRows().toOIDList("id");

        for (Integer id : idList) {
            this.nodeZones.put(id, new int[d]);
        }
        annotateGraph(k, d, r, graph);
    }

    public ZoneNSI(int k, int d, Graph graph) {
        this(k, d, 1, graph);
    }

    protected ZoneNSI() {
    }

    private void annotateGraph(int k, int d, int r, Graph graph) {
        log.info("annotating " + k + " zones, " + d + " dimensions, " + r + " floods for zone");
        for (int dim = 0; dim < d; dim++) {
            log.debug("\tdim " + (dim + 1));
            Map<Integer, Integer> labels = annotateDimension(k, r, graph);

            for (Map.Entry<Integer, Integer> e : labels.entrySet()) {
                this.nodeZones.get(e.getKey())[dim] = e.getValue();
            }
        }
    }

    protected Map<Integer, Integer> annotateDimension(int k, int r, Graph graph) {
        List<Integer> idList = DB.getObjectNST().selectRows().toOIDList("id");
        int nodeCount = idList.size();
        Map<Integer, Integer> labels = new HashMap<Integer, Integer>();

        // seed k nodes
        int currLabel = 0;
        while (labels.size() < k) {
            Integer candidate = idList.get(this.rand.nextInt(idList.size()));
            if (!labels.containsKey(candidate)) {
                labels.put(candidate, currLabel++);
            }
        }

        List<Integer> nodesLabelled = new ArrayList<Integer>(labels.keySet());
        List<Integer> nodesLabelledNext = new ArrayList<Integer>();

        int iterCount = 0;
        while ((nodeCount - labels.size()) > 0) {
            iterCount++;
            if (iterCount % 10 == 0) {
                log.debug("\t\tann iter " + iterCount + " (" + (nodeCount - labels.size()) + ")");
            }

            Collections.shuffle(nodesLabelled);
            for (Integer nodeId : nodesLabelled) {
                Integer label = labels.get(nodeId);

                List<Integer> unlabelledNeighbors = new ArrayList<Integer>();
                Set<Integer> neighborIds = new HashSet<Integer>(ConversionUtils.nodesToIntegers(graph.getNeighbors(nodeId)));

                for (Integer neighborId : neighborIds) {
                    if (!labels.containsKey(neighborId)) {
                        unlabelledNeighbors.add(neighborId);
                    }
                }

                // randomly label r neighbors
                for (int i = 0; i < r && i < unlabelledNeighbors.size(); i++) {
                    Integer neighborId = unlabelledNeighbors.remove(this.rand.nextInt(unlabelledNeighbors.size()));
                    labels.put(neighborId, label);
                    nodesLabelledNext.add(neighborId);
                }

                if (unlabelledNeighbors.size() > 0) {
                    nodesLabelledNext.add(nodeId);
                }
            }

            nodesLabelled = nodesLabelledNext;
            nodesLabelledNext = new ArrayList<Integer>();
        } // while unlabelled
        log.debug("\t\tann iter " + iterCount + " (done)");

        return labels;
    }

    public double distance(Integer nodeId1, Integer nodeId2) {
        int[] ann1 = this.nodeZones.get(nodeId1);
        int[] ann2 = this.nodeZones.get(nodeId2);

        if (ann1.length != ann2.length) {
            log.debug("annotations for nodes " + nodeId1 + " and " + nodeId2 + " differ in size:");
            log.debug(" " + nodeId1 + "(" + ann1.length + ": " + ann1.toString());
            log.debug(" " + nodeId2 + "(" + ann2.length + ": " + ann2.toString());
        }

        int size = ann1.length;
        double matches = 0;
        for (int i = 0; i < size; i++) {
            if (ann1[i] == ann2[i]) {
                matches += 1;
            }
        }
        return 1 - (matches / size);
    }
}