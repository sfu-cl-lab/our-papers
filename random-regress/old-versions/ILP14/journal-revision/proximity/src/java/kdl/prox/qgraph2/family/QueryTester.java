/**
 * $Id: QueryTester.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.family;

import java.util.List;

/**
 * specifies how to test a query (given
 */
public interface QueryTester {

    public List<QueryTestFailure> testQuery(QueryDataPair queryDataPair, String familyName) throws Exception;

}
