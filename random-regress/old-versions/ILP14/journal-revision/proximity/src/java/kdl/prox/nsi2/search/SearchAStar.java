/**
 * $Id: SearchAStar.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.search;

import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.graph.Node;
import kdl.prox.nsi2.nsi.NSI;
import kdl.prox.nsi2.util.ConversionUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Performs A* search by using an NSI as a heuristic for distance. Combines the distance traveled so far
 * and the estimated distance remaining to determine which node to visit next.  Terminates when the target
 * is first reached, so this implementation is not necessarily optimal.
 */
public class SearchAStar implements Search {
    private static Logger log = Logger.getLogger(SearchAStar.class);
    private static Random rand = new Random();
    private static final int BEST_FIRST_MAX = 1000000;
    private NSI nsi;
    private Graph graph;

    public SearchAStar(NSI nsi, Graph graph) {
        this.nsi = nsi;
        this.graph = graph;
    }

    private int degree(Integer nodeId) {
        return this.graph.getNeighbors(nodeId).size();
    }

    public SearchResults search(int source, int target) {
        SearchResults sr = new SearchResults();
        sr.startTimer();

        if (source == target) {
            sr.addPath(new Node(source, 0.0));
            sr.addExplored(source);
            sr.stopTimer();
            return sr;
        }

        Set<Integer> visited = new HashSet<Integer>();
        PriorityQueue<SearchNode> open = new PriorityQueue<SearchNode>();
        List<Node> path = new ArrayList<Node>();
        path.add(new Node(source, 0.0));
        open.offer(new SearchNode(source, this.nsi.distance(source, target), path, 0.0));
        double currentPathLength;
        Map<Integer, Double> queuedDists = new HashMap<Integer, Double>();


        while ((open.size() > 0) && (visited.size() < BEST_FIRST_MAX)) {
            SearchNode currentSearchNode = open.poll(); //retrieves and removes first element
            Integer currentNode = currentSearchNode.nodeId;
            List<Node> currentPath = currentSearchNode.path;
            currentPathLength = 0.0;
            for (Node node : currentPath) {
                currentPathLength += node.weight;
            }
            visited.add(currentNode);

            //must copy neighbors so don't modify Graph
            Set<Node> neighbors = graph.getNeighbors(currentNode);
            Set<Integer> neighborIds = new HashSet<Integer>(ConversionUtils.nodesToIntegers(neighbors));

            if (neighborIds.contains(target)) {
                for (Node node : neighbors) {
                    if (node.id == target) {
                        currentPath.add(node);
                    }
                }
                visited.add(target);
                sr.addPath(currentPath);
                sr.addExplored(new ArrayList<Integer>(visited));
                sr.stopTimer();
                return sr;
            }

            neighbors.removeAll(visited);
            for (Node neighbor : neighbors) {
                //remove all previously visited neighbors
                if (visited.contains(neighbor.id)) {
                    continue;
                }
                List<Node> neighborPath = new ArrayList<Node>(currentPath);
                neighborPath.add(neighbor);

                Double oldDistFromSource = queuedDists.get(neighbor.id);
                double newDistFromSource = currentPathLength + neighbor.weight;

                //update cost to neighbor if we haven't seen it yet or the new cost is less
                if ((oldDistFromSource == null) || (oldDistFromSource > newDistFromSource)) {
                    SearchNode sn = new SearchNode(neighbor.id, this.nsi.distance(neighbor.id, target),
                            neighborPath, newDistFromSource);
                    open.offer(sn);
                    queuedDists.put(neighbor.id, newDistFromSource);
                }
            }
        }

        //exhausted the open list or hit the search max
        log.debug("exhausted open list...");
        sr.addExplored(new ArrayList<Integer>(visited));
        sr.stopTimer();
        return sr;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private class SearchNode implements Comparable {
        private Integer nodeId;
        private int degree;
        private List<Node> path;
        private double pathlength;
        private double dist;
        private int hc;

        public SearchNode(Integer nodeId, double dist, List<Node> path, double pathlength) {
            this.nodeId = nodeId;
            this.dist = dist + pathlength;
            this.path = path;
            this.degree = degree(nodeId);
            this.pathlength = pathlength;
            this.hc = nodeId;
        }

        public boolean equals(Object o) {
            return this.nodeId.equals(((SearchNode) o).nodeId);
        }

        public int hashCode() {
            return hc;
        }

        public int compareTo(Object o) {

            SearchNode other = (SearchNode) o;
            if (this.nodeId.equals(other.nodeId)) {
                return 0;
            } else {
                if (this.dist > other.dist) {
                    return 1;
                } else if (this.dist < other.dist) {
                    return -1;
                } else { // degree is the tiebreaker
                    if (this.degree > other.degree) {
                        return -1;
                    } else if (this.degree < other.degree) {
                        return 1;
                    } else { // distance travelled from source is the tiebreaker-tiebreaker
                        if (this.pathlength > other.pathlength) {
                            return 1;
                        } else if (this.pathlength < other.pathlength) {
                            return -1;
                        } else { // coin flip is the tiebreaker-tiebreaker-tiebreaker
                            if (rand.nextDouble() < 0.5) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }
                    }
                }
            }
        }

        public String toString() {
            String d = Double.toString(this.dist);
            return this.nodeId + " (" + d.substring(0, Math.min(d.length(), 5)) + ", " + this.degree + ", " + this.pathlength + ")";
        }
    }
}

