/**
 * $Id: Graph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import java.util.Collection;
import java.util.Set;

/**
 * The Graph interface is the lower-layer of the NSI package, encapsulating the access to the underlying database:
 *
 *      APP
 *      ------
 *      SEARCH
 *      ------
 *      NSI
 *      ------
 *      GRAPH
 *
 * All the Graph layer has to do is return a list of the neighbors of a particular node.
 *
 * There are several implementations of the Graph interface:
 *  - InMemory          : very fast, works when all the links fit in memory
 *  - OnDisk            : very slow, but allows for databases with an arbitrary number of links
 *  - CachedOnDiskGraph : intermediate solution, keeps an in-memory cache of some of the links (100x slower than InMemory!)
 *  - ArtificialGraph   : Is not based on the Monet database, but on a small network that is passed in to the constructor
 *
 */
public interface Graph {
    public Set<Node> getNeighbors(Integer id);

    public Set<Node> getNeighbors(Collection<Integer> ids);
}
