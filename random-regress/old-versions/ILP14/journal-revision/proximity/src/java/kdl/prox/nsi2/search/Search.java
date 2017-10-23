/**
 * $Id: Search.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.search;

import kdl.prox.nsi2.graph.Graph;

/**
 * The Search interface uses the lower layers to find paths between pairs of nodes.
 *
 *      APP
 *      ------
 *      SEARCH
 *      ------
 *      NSI
 *      ------
 *      GRAPH
 *
 * For every search, it returns a SearchResults instance, with the path and statistics about the search.
 *
 * There are several implementations of the Search interface:
 *  - SearchAStar        : performs A* search, requires an NSI
 *  - SearchBestFirst    : performs best-first search, requires an NSI
 *  - SearchBreadthFirst : performs bidirectional breadth-first search; no path is returned, just the path length
 *  - SearchUniformCost  : performs uniform cost search on weighted graphs; no path is returned, just the path cost
 *
 */

public interface Search {
    public SearchResults search(int from, int to);

    public void setGraph(Graph graph);
}
