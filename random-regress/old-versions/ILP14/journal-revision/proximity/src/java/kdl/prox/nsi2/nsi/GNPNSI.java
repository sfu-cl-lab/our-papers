/**
 * $Id: GNPNSI.java 3658 2007-10-15 16:29:11Z schapira $
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
import org.spaceroots.mantissa.optimization.*;

import java.util.*;

/**
 * The GNP NSI is based on the Global Network Positioning system proposed by Ng & Zhang in 2002.
 * A set of landmarks is chosen, and all nodes are embedded in a low-dimensional coordinate space
 * using the landmarks as a frame of reference.  The embedding uses a simplex downhill optimization which
 * can be very slow and inaccurate.
 */
public class GNPNSI implements NSI {
    private static Logger log = Logger.getLogger(GNPNSI.class);
    private double annotations[][];
    private int numLandmarks;

    //class to compute the cost of landmark-landmark distances
    class LandmarkLandmarkCost implements CostFunction {
        Map<Integer, List<Double>> dists;
        List<Integer> landIds;

        LandmarkLandmarkCost(List<Integer> landIds, Map<Integer, List<Double>> dists) {
            this.dists = dists;
            this.landIds = landIds;
        }

        public double cost(double[] coords) throws CostException {
            int numLandmarks = this.landIds.size();
            double cost = 0.0;

            for (int i = 0; i < numLandmarks - 1; i++) {
                for (int j = i + 1; j < numLandmarks; j++) {
                    Integer iId = this.landIds.get(i);
                    //Integer jId = this.landIds.get(j);
                    double graphDist = dists.get(iId).get(j);


                    double eucDist = 0.0;
                    for (int k = 0; k < numLandmarks - 1; k++) {
                        eucDist += Math.pow(coords[i * (numLandmarks - 1) + k] - coords[j * (numLandmarks - 1) + k], 2.0);
                    }
                    eucDist = Math.sqrt(eucDist);
                    //normalized error
                    cost += Math.pow((graphDist - eucDist) / graphDist, 2);
                }
            }
            return cost;
        }
    }

    class LandmarkConvergence implements ConvergenceChecker {
        double threshold;

        LandmarkConvergence(double threshold) {
            this.threshold = threshold;
        }

        public boolean converged(PointCostPair[] pointCostPairs) {
            PointCostPair pcpMin = pointCostPairs[0];
            PointCostPair pcpMax = pointCostPairs[pointCostPairs.length - 1];
            return Math.abs(pcpMax.cost - pcpMin.cost) <= this.threshold;
        }
    }

    class LandmarkHostCost implements CostFunction {
        Map<Integer, List<Double>> dists;
        List<Integer> landIds;
        Integer hostId;

        LandmarkHostCost(List<Integer> landIds, Map<Integer, List<Double>> dists, Integer hostId) {
            this.dists = dists;
            this.landIds = landIds;
            this.hostId = hostId;
        }

        public double cost(double[] coords) throws CostException {
            int numLandmarks = this.landIds.size();
            double cost = 0.0;

            for (int i = 0; i < numLandmarks; i++) {
                Integer landmarkId = this.landIds.get(i);
                double[] landmarkCoords = annotations[landmarkId];
                double graphDist = dists.get(hostId).get(i);

                double eucDist = 0.0;
                for (int k = 0; k < numLandmarks - 1; k++) {
                    eucDist += Math.pow(landmarkCoords[k] - coords[k], 2.0);
                }
                eucDist = Math.sqrt(eucDist);
                //log.debug("dist " + landmarkId + ", " + hostId + ": " + graphDist + " / " + eucDist);
                cost += Math.pow((graphDist - eucDist) / graphDist, 2);
            }
            return cost;
        }

    }

    public GNPNSI(int numLandmarks, Graph graph) {
        this.numLandmarks = numLandmarks;
        int numNodes = DB.getObjectNST().getRowCount();
        this.annotations = new double[numNodes][numLandmarks];

        log.info("Creating GNP NSI with " + Math.min(numLandmarks, numNodes) + " landmarks");

        List<Integer> landmarkIds = GraphUtils.chooseRandomNodes(this.numLandmarks);

        // store the distance from every node to every landmark
        Map<Integer, List<Double>> distToLandmarks = new HashMap<Integer, List<Double>>();

        //initialize distances
        for (Integer node : DB.getObjectNST().selectRows().toOIDList("id")) {
            distToLandmarks.put(node, new ArrayList<Double>());
        }

        int ctr = 0;
        for (Integer landmarkId : landmarkIds) {
            log.debug("\t" + ctr++);
            //flood from current landmark
            //Map<Integer, Integer> dists = GraphUtils.flood(landmarkId, DB.getObjectNST().getRowCount(), graph);
            Map<Integer, Double> dists = GraphUtils.floodWeighted(landmarkId, graph);

            //add current distances from each node to landmark
            for (Integer node : dists.keySet()) {
                distToLandmarks.get(node).add(dists.get(node));
            }
        }


        LandmarkLandmarkCost costfunc = new LandmarkLandmarkCost(landmarkIds, distToLandmarks);
        LandmarkConvergence convergence = new LandmarkConvergence(0.001);
        int numParams = numLandmarks * (numLandmarks - 1);
        double[][] simplex = new double[numParams + 1][numParams];
        Arrays.fill(simplex[0], 0.0);
        for (int i = 0; i < numParams; i++) {
            Arrays.fill(simplex[i + 1], 0.0);
            simplex[i + 1][i] = 1.0;
        }

        log.debug("computing landmark coordinates");
        NelderMead nm = new NelderMead();
        try {
            PointCostPair minimum = nm.minimizes(costfunc, 100, convergence, simplex, 10, null);

            for (int i = 0; i < this.numLandmarks; i++) {
                double[] annotation = new double[numLandmarks - 1];
                System.arraycopy(minimum.point, i * (numLandmarks - 1), annotation, 0, annotation.length);

                this.annotations[landmarkIds.get(i)] = annotation;
            }
        } catch (org.spaceroots.mantissa.MantissaException e) {
            log.debug("nelder mead exception: " + e);
        }


        log.debug("computing host coordinates");
        ctr = 0;
        for (Integer host : DB.getObjectNST().selectRows().toOIDList("id")) {
            if (ctr % 100 == 0)
                log.debug("\t" + ctr);
            if (landmarkIds.contains(host)) {
                continue;
            }
            LandmarkHostCost costfunchost = new LandmarkHostCost(landmarkIds, distToLandmarks, host);
            double[][] startsimplex = new double[numLandmarks][numLandmarks - 1];
            Arrays.fill(startsimplex[0], 0.0);
            for (int i = 0; i < numLandmarks - 1; i++) {
                Arrays.fill(startsimplex[i + 1], 0.0);
                startsimplex[i + 1][i] = 1.0;
            }

            try {
                PointCostPair minimum = nm.minimizes(costfunchost, 10, convergence, startsimplex, 10, null);
                System.arraycopy(minimum.point, 0, this.annotations[host], 0, minimum.point.length);
            } catch (org.spaceroots.mantissa.MantissaException e) {
                // log.debug("nelder mead exception on point " + host + ": " + e);
            }
            ctr++;
        }
    }

    public double distance(Integer nodeId1, Integer nodeId2) {
        double[] vec1 = annotations[nodeId1];
        double[] vec2 = annotations[nodeId2];

        double sumSquaresDiffs = 0;
        for (int i = 0; i < (this.numLandmarks - 1); i++) {
            sumSquaresDiffs += Math.pow(vec1[i] - vec2[i], 2);
        }
        return Math.sqrt(sumSquaresDiffs);
    }

}
