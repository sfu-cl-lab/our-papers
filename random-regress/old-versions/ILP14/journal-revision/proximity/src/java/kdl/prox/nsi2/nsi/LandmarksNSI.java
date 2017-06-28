/**
 * $Id: LandmarksNSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

import kdl.prox.db.DB;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.util.GraphUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Landmarks NSI chooses a set of landmarks and stores the exact distance from every node to each landmark.
 * The distance measure takes the average of the best upper and lower bounds on actual distance. 
 */
public class LandmarksNSI implements NSI {
    private static Logger log = Logger.getLogger(LandmarksNSI.class);
    private List<Integer> landmarks;
    private Map<Integer, Map<Integer, Integer>> dists = new HashMap<Integer, Map<Integer, Integer>>();


    public LandmarksNSI(int numLandmarks, Graph graph) {
        this.landmarks = GraphUtils.chooseRandomNodes(numLandmarks);
        log.info("Creating landmark NSI with " + this.landmarks.size() + " landmarks");

        //initialize map of distances for all nodes
        for (Integer node : DB.getObjectNST().selectRows().toOIDList("id")) {
            dists.put(node, new HashMap<Integer, Integer>());
        }

        annotateGraph(graph);
    }

    private void annotateGraph(Graph graph) {
        //find the distances from each landmark to all nodes
        log.debug("calculating node-landmark distances...");
        for (Integer landmark : this.landmarks) {
            log.debug("flooding from landmark " + landmark);
            Map<Integer, Integer> floodMap = GraphUtils.flood(landmark, Integer.MAX_VALUE, graph);

            for (Integer node : floodMap.keySet()) {
                Integer dist = floodMap.get(node);
                dists.get(node).put(landmark, dist);
            }
        }
    }

    public double distance(Integer node1, Integer node2) {
        //returns the arithmetic mean of the best upper and lower bounds
        int bestLower = -1;
        int bestUpper = Integer.MAX_VALUE;

        Map<Integer, Integer> dists1 = dists.get(node1);
        Map<Integer, Integer> dists2 = dists.get(node2);

        for (Integer land1 : dists1.keySet()) {
            int dist1 = dists1.get(land1);

            for (Integer land2 : dists2.keySet()) {
                int dist2 = dists2.get(land2);

                int distMarks = dists.get(land1).get(land2);

                int upper = dist1 + distMarks + dist2;
                int lower = Math.max(1, distMarks - (dist1 + dist2));
                if (upper < bestUpper) {
                    bestUpper = upper;
                }
                if (lower > bestLower) {
                    bestLower = lower;
                }

                if (bestUpper == bestLower) {
                    break;
                }
            }
        }

        // for geometric mean, use:
        // double dist = Math.sqrt(bestUpper * bestLower);
        return (bestUpper + bestLower) / 2.0;
    }
}
