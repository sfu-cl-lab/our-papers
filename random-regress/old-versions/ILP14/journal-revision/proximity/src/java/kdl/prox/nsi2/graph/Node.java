/**
 * $Id: Node.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import java.io.Serializable;

/**
 * This is the basic representation of a neighboring node: id, and the weight of the link connecting to it
 */
public class Node implements Comparable, Serializable {

    public Integer id;
    public Double weight;

    public Node(Integer id, Double weight) {
        this.id = id;
        this.weight = weight;
    }

    public Node(Node wn) {
        this.id = wn.id;
        this.weight = wn.weight;
    }

    public boolean equals(Node wn) {
        return (this.id.equals(wn.id)) && (this.weight.equals(wn.weight));

    }

    public int hashcode() {
        return this.id;
    }

    public int compareTo(Object other) {
        //compare by weights
        return Double.compare(this.weight, ((Node) other).weight);
    }

    public String toString() {
        return this.id + ":" + this.weight;
    }
}
