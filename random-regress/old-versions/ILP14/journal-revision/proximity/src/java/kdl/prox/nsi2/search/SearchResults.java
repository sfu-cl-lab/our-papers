/**
 * $Id: SearchResults.java 3705 2007-11-02 18:23:25Z maier $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.search;

import kdl.prox.nsi2.graph.Node;
import kdl.prox.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stores information about a search: the path, and statistics like
 *    - list of explored nodes
 *    - time spent in the search
 *
 * An instance of SearchResults is returned by the Search implementations.
 * 
 */
public class SearchResults {
    protected List<Node> path; // holds the shortest path found by the search
    private List<Integer> explored;
    private long startTime;
    private long elapsed;

    public SearchResults() {
        elapsed = -1;
        startTime = -1;
        explored = new ArrayList<Integer>();
        path = new ArrayList<Node>();
    }

    public void addExplored(Integer id) {
        explored.add(id);
    }

    public void addExplored(Collection<Integer> ids) {
        explored.addAll(ids);
    }

    public void addPath(Node node) {
        path.add(node);
    }

    public void addPath(List<Node> nodes) {
        path.addAll(nodes);
    }

    public double pathlength() {
        double pathlength = 0.0;
        for (Node node : path) {
            pathlength += node.weight;
        }
        return pathlength;
    }

    public int numHops() {
        return path.size() - 1;
    }

    public int exploredCount() {
        return explored.size();
    }

    public List<Node> pathList() {
        return this.path;
    }

    public List exploredList() {
        return explored;
    }

    public long elapsed() {
        Assert.condition(elapsed != -1, "Call startTimer() and stopTimer() first!");
        return this.elapsed;
    }

    public void startTimer() {
        startTime = System.nanoTime();
    }

    public void stopTimer() {
        Assert.condition(startTime != -1, "Call startTimer() first!");
        elapsed = System.nanoTime() - startTime;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("path length: ");
        sb.append(numHops());
        sb.append(", explored count: ");
        sb.append(explored.size());
        sb.append("\n\tpath: ");
        sb.append(this.path.toString());
        return sb.toString();
    }

    public boolean equals(Object other) {
        SearchResults otherSR = (SearchResults) other;
        if (this.numHops() != otherSR.numHops()) {            
            return false;
        }
        else {
            for (int i=0; i<this.path.size(); i++) {
                if (! this.path.get(i).id.equals(otherSR.path.get(i).id)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        return numHops() * (int)pathlength();
    }
}
