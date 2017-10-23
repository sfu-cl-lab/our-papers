/**
 * $Id: ArtificialGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A Graph that is not based on the Monet database, but on a small network that is passed in to the constructor
 */
public class ArtificialGraph implements Graph {
    private static Logger log = Logger.getLogger(ArtificialGraph.class);

    public Map<Integer, Set<Node>> links;

    public ArtificialGraph(Map<Integer, Set<Node>> links) {
        this.links = links;
    }

    public Set<Node> getNeighbors(Integer id) {
        return links.get(id);
    }

    public Set<Node> getNeighbors(Collection<Integer> ids) {
        ArrayList<Node> list = new ArrayList<Node>();
        for (Integer id : ids) {
            list.addAll(getNeighbors(id));
        }
        return new HashSet<Node>(list);
    }
}