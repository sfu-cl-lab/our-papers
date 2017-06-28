/**
 * $Id: QueryIterHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryIterHandler.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2;


/**
 * Used by QueryIterator to "walk" QueryS.
 */
public interface QueryIterHandler {

    /**
     * Called when an addLink is encountered.
     *
     * @param qgAddLink
     */
    public void addLink(QGAddLink qgAddLink);

    /**
     * Called when a cachedElement is encountered
     *
     * @param itemName
     * @param containerName the name of the container where it's cached
     */
    public void cachedItem(String itemName, String containerName);


    /**
     * Called when a constraint is encountered.
     *
     * @param qgConstraint
     */
    public void constraint(QGConstraint qgConstraint);


    /**
     * Called when an edge is encountered.
     *
     * @param qgEdge
     */
    public void edge(QGEdge qgEdge);


    /**
     * Called when an AbstractQuery is finished.
     *
     * @param abstractQuery
     */
    public void endAbstractQuery(AbstractQuery abstractQuery);


    /**
     * Called when an AbstractQuery is started, before its vertices, edges, and
     * subqueries are done.
     *
     * @param abstractQuery
     */
    public void startAbstractQuery(AbstractQuery abstractQuery);


    /**
     * Called when constraints are started.
     */
    public void startConstraints();


    /**
     * Called when edges are started.
     */
    public void startEdges();


    /**
     * Called when vertices are started.
     */
    public void startVertices();


    /**
     * Called when a vertex is encountered.
     *
     * @param qgVertex
     */
    public void vertex(QGVertex qgVertex);


}
