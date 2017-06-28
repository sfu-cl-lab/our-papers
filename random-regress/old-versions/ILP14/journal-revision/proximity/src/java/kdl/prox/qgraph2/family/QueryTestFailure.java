/**
 * $Id: QueryTestFailure.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryTestFailure.java 3658 2007-10-15 16:29:11Z schapira $
 */
package kdl.prox.qgraph2.family;

import kdl.prox.util.Assert;


/**
 * Simple class to store a test failure report
 */
public class QueryTestFailure {

    /**
     * An entry is specified by the family in which it occurs, the query, the specific path in the query, and the error
     * message
     * failureFamily and failureMessage cannot be empty
     * queryFamily and queryPath can be empty, for family and query-wide errors
     * <p/>
     * todo: There is also a timestamp for the error
     */
    private String failureFamily;
    private String failureQuery;
    private String failurePath;
    private String failureMessage;

    public QueryTestFailure(String family, String query, String path, String message) {
        Assert.stringNotEmpty(family, "QueryTestFailture(): null or empty family");
        Assert.stringNotEmpty(message, "QueryTestFailture(): null or empty message");
        if (query == null) {
            query = "";
        }
        if (path == null) {
            path = "";
        }

        failureFamily = family;
        failureQuery = query;
        failurePath = path;
        failureMessage = message;

    }


    /**
     * @return
     */
    public String getFamily() {
        return failureFamily;
    }


    /**
     * @return
     */
    public String getMessage() {
        return failureMessage;
    }


    /**
     * @return
     */
    public String getPath() {
        return failurePath;
    }


    /**
     * @return
     */
    public String getQuery() {
        return failureQuery;
    }


    /**
     * Convert failure report into string, in a fixed format
     */
    public String toString() {
        return "[" + failureFamily + "|" + failureQuery + "|" + failurePath + "]:" + failureMessage;
    }

}
