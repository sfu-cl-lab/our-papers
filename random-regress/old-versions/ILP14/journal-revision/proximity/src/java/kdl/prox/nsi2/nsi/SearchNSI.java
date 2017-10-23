/**
 * $Id: SearchNSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

import kdl.prox.nsi2.search.Search;
import org.apache.log4j.Logger;

/**
 * The Search NSI uses a searching routine to provide distances. As such, this NSI has zero initialization
 * costs, but can be very slow to return distance estimates.  This NSI is useful as a wrapper of searching
 * methods for functions that require an NSI. 
 */
public class SearchNSI implements NSI {
    private static Logger log = Logger.getLogger(SearchNSI.class);
    private Search search;

    public SearchNSI(Search search) {
        this.search = search;
    }

    public double distance(Integer node1, Integer node2) {
        return search.search(node1, node2).pathlength();
    }
}
