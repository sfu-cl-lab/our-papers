/**
 * $Id: NSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

/**
 * The NSI interface is where our intelligence lies: it provides smart ways of estimating distances between a pair
 * of nodes in the graph. It sits on top of the Graph layer, and provides those estimates to the Search layer, which
 * uses them to find short paths between nodes.
 *
 *      APP
 *      ------
 *      SEARCH
 *      ------
 *      NSI
 *      ------
 *      GRAPH
 *
 * The NSI implementations use different techniques to provide good (and efficient) estimates of the distances. In most
 * cases, they build an 'index' of the graph, and then use that index to compute the estimates.
 *
 * There are several implementations NSIs:
 *  - APSPNSI                : stores all pairs shortest path, distance is exact
 *  - DegreeNSI              : annotates each node with its degree
 *  - DTZNSI                 : more accurate than Zone, but has large increases in time/space complexity
 *  - GNPNSI                 : Global network positioning, Ng & Zhang - embeds nodes in a coordinate space
 *  - LandmarksNSI           : stores exact distance to a set of landmarks, distance triangulates
 *  - MatrixFactorizationNSI : Mao & Saul - uses matrix factorization techniques based on landmark nodes
 *  - SearchNSI              : distance is estimated by actually performing a search
 *  - ZoneNSI                : annotates each node with a vector of zone labels by randomly flooding the graph
 */
public interface NSI {
    public double distance(Integer n1, Integer n2);
}
