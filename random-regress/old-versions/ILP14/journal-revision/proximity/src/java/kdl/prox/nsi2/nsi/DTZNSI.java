/**
 * $Id: DTZNSI.java 3658 2007-10-15 16:29:11Z schapira $
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The DTZ NSI, or distance-to-zone, computes a Zone NSI and then for each node, stores the shortest distance
 * to each other zone.  This is expensive to compute, both in terms of time and space.  However, the distance
 * estimates are very accurate.
 */
public class DTZNSI extends ZoneNSI implements NSI {
    private static Logger log = Logger.getLogger(DTZNSI.class);

    private int k;
    private int d;

    private Map<Integer, int[]> nodeZones = new HashMap<Integer, int[]>(); // nodeId -> [d]
    private Map<Integer, int[][]> nodeDistanceToZones = new HashMap<Integer, int[][]>(); // nodeId -> [d][k]

    public DTZNSI(int k, int d, Graph graph) {
        log.info("creating DTZ NSI for " + k + " zones, " + d + " dimensions");
        this.k = k;
        this.d = d;

        List<Integer> idList = DB.getObjectNST().selectRows().toOIDList("id");
        for (Integer nodeId : idList) {
            this.nodeZones.put(nodeId, new int[d]);
            this.nodeDistanceToZones.put(nodeId, new int[d][]);
        }

        log.debug("creating zones");
        for (int dim = 0; dim < d; dim++) {
            log.debug("\tdim " + (dim + 1));
            Map<Integer, Integer> zones = annotateDimension(k, 1, graph); // nodeId -> zone

            for (Map.Entry<Integer, Integer> e : zones.entrySet()) {
                Integer nodeId = e.getKey();
                this.nodeZones.get(nodeId)[dim] = e.getValue();
            }

            calculateDistances(dim, graph);
        }

    }

    public void calculateDistances(int currDim, Graph graph) {
        log.debug("calculating distance to " + this.k + " zones for dimension " + currDim);

        List<List<Integer>> groups = new ArrayList<List<Integer>>();
        for (int i = 0; i < this.k; i++) {
            groups.add(new ArrayList<Integer>());
        }

        for (Map.Entry<Integer, int[]> e : this.nodeZones.entrySet()) {
            Integer nodeId = e.getKey();
            int zone = e.getValue()[currDim];
            groups.get(zone).add(nodeId);

            this.nodeDistanceToZones.get(nodeId)[currDim] = new int[this.k];
        }

        // flood out from each zone group --- for every node, we want the dist to this zone
        for (int currZone = 0; currZone < this.k; currZone++) {
            if ((currZone + 1) % 10 == 0) {
                log.debug("\t" + (currZone + 1));
            }

            List<Integer> visited = new ArrayList<Integer>();
            List<Integer> frontier = groups.get(currZone);
            int dist = 0;
            while (frontier.size() > 0) {
                for (Integer nodeId : frontier) {
                    this.nodeDistanceToZones.get(nodeId)[currDim][currZone] = dist;
                }
                visited.addAll(frontier);
                frontier = new ArrayList<Integer>(ConversionUtils.nodesToIntegers(graph.getNeighbors(frontier)));
                frontier.removeAll(visited);
                dist++;
            }
        }
    }

    public double distanceAvg(Integer nodeId1, Integer nodeId2) {
        int distTotal = 0;

        for (int dim = 0; dim < this.d; dim++) {
            int zone1 = this.nodeZones.get(nodeId1)[dim];
            int zone2 = this.nodeZones.get(nodeId2)[dim];

            if (zone1 != zone2) {
                int dist1 = this.nodeDistanceToZones.get(nodeId1)[dim][zone2];
                int dist2 = this.nodeDistanceToZones.get(nodeId2)[dim][zone1];
                distTotal += (dist1 + dist2);
            }
        }
        return (double) distTotal;
    }

    public double distance(Integer nodeId1, Integer nodeId2) {
        int maxDist = 0;

        for (int dim = 0; dim < this.d; dim++) {
            int zone1 = this.nodeZones.get(nodeId1)[dim];
            int zone2 = this.nodeZones.get(nodeId2)[dim];

            if (zone1 != zone2) {
                int dist1 = this.nodeDistanceToZones.get(nodeId1)[dim][zone2];
                int dist2 = this.nodeDistanceToZones.get(nodeId2)[dim][zone1];

                maxDist = Math.max(dist1, maxDist);
                maxDist = Math.max(dist2, maxDist);
            }
        }
        return (double) maxDist;
    }
}
