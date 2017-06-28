/**
 * $Id: APSPNSI.java 3658 2007-10-15 16:29:11Z schapira $
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
 * The All Pairs Shortest Path NSI stores the exact graph distance for all pairs of nodes.  As a result, it provides
 * exact distances, but can be extremely slow, even for moderately-sized graphs.
 */
public class APSPNSI implements NSI {
    private static Logger log = Logger.getLogger(APSPNSI.class);

    private Map<Integer, Map<Integer, Integer>> distances = new HashMap<Integer, Map<Integer, Integer>>(); // node1 -> { node2 -> dist }

    public APSPNSI(Graph graph) {
        log.info("creating APSP NSI");
        List<Integer> nodeIds = DB.getObjectNST().selectRows().toOIDList("id");

        log.debug("flooding distances for " + nodeIds.size() + " nodes");
        int nodeCount = 0;
        for (Integer nodeId : nodeIds) {
            if (++nodeCount % 100 == 0) {
                log.debug("flooded " + nodeCount);
            }
            Map<Integer, Integer> floodMap = GraphUtils.flood(nodeId, Integer.MAX_VALUE, graph);
            if (floodMap.size() < nodeIds.size()) {
                log.debug("graph is not connected! (flooded " + floodMap.size() + ", have " + nodeIds.size() + ")");
            }
            Map<Integer, Integer> distMap = new HashMap<Integer, Integer>();

            for (Map.Entry<Integer, Integer> me : floodMap.entrySet()) {
                Integer targetId = me.getKey();
                if (nodeId >= targetId) {
                    distMap.put(targetId, me.getValue());
                }
            }
            this.distances.put(nodeId, distMap);
        }
    }

    public double distance(Integer n1, Integer n2) {
        if (n1 >= n2) {
            return this.distances.get(n1).get(n2).doubleValue();
        } else {
            return this.distances.get(n2).get(n1).doubleValue();
        }

    }
}

