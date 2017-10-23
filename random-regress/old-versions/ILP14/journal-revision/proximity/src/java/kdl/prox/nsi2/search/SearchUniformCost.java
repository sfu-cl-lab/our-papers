/**
 * $Id: SearchUniformCost.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.search;

import kdl.prox.db.DB;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.graph.Node;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Performs uniform-cost search over a weighted graph.  The returned SearchResults instance
 * does not contain the found path, only the path cost.  The search Will always find the shortest cost
 * path. 
 */
public class SearchUniformCost implements Search {
    private static Logger log = Logger.getLogger(SearchUniformCost.class);
    private Graph graph;

    public SearchUniformCost(Graph graph) {
        this.graph = graph;
    }

    public WeightedPathlessSearchResults search(int source, int target) {
        WeightedPathlessSearchResults wpsr = new WeightedPathlessSearchResults();
        wpsr.startTimer();

        if (source == target) {
            wpsr.setPathlength(0.0);
            wpsr.addExplored(source);
            wpsr.stopTimer();
            return wpsr;
        }

        PriorityQueue<Node> frontier = new PriorityQueue<Node>(DB.getObjectNST().getRowCount(), new NodeComparator<Node>());

        frontier.offer(new Node(source, 0.0));
        Set<Integer> expanded = new HashSet<Integer>();

        double pathLength;

        while (true) {
            //get the next closest node
            Node closest = frontier.poll();
            if (!expanded.add(closest.id)) {
                continue;
            }

            if (closest.id.equals(target)) {
                pathLength = closest.weight;
                break;
            }

            //get all neighbors, and set distance to edge weight plus distance of current node
            //if lower than current estimated distance, update frontier accordingly
            Set<Node> neighbors = graph.getNeighbors(closest.id);
            for (Node neighbor : neighbors) {
                Node neighborClone = new Node(neighbor);
                neighborClone.weight += closest.weight;

                //always add to queue, will check if have already seen node with better weight
                frontier.offer(neighborClone);

            }
        }

        wpsr.setPathlength(pathLength);
        wpsr.addExplored(new HashSet<Integer>(expanded));
        wpsr.stopTimer();
        return wpsr;

    }

    private class WeightedPathlessSearchResults extends SearchResults {
        private double pathlength;

        public WeightedPathlessSearchResults() {
            super();
            this.pathlength = -1;
            this.path = null;
        }

        public double pathlength() {
            return this.pathlength;
        }

        public void setPathlength(double pathlength) {
            this.pathlength = pathlength;
        }

        public List<Node> pathList() {
            log.warn("Cannot return path for uniform-cost search!");
            return null;
        }
    }

    private class NodeComparator<T> implements Comparator<T> {

        public int compare(Object wn1, Object wn2) {
            //compare by weights
            return Double.compare(((Node) wn1).weight, ((Node) wn2).weight);
        }
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

}
