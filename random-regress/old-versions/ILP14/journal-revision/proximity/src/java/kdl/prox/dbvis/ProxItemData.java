/**
 * $Id: ProxItemData.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;


/**
 * Encapsulates Proximity data for all cases of vertex types - non-pseudo,
 * pseudo-not-pager, and pseudo-and-pager. IV usage (used, unused) depends on
 * case:
 * <ul>
 * <li>non-pseudo - OID, label, color
 * <li>pseudo
 * <ul>
 * <li>pager - numShown, numTotal
 * </ul>
 * </ul>
 * A note on special <i>pseudo</i> and <i>pager</i> vertices and edges: Some
 * Vertex and Edge instances are considered pseudo ones if they don't represent
 * an actual object or link in the database. These are used to indicate that
 * 'paging' has occurred, i.e., that not all vertices or edges are shown. We have
 * the following isPseudo and isPager cases:
 * <ul>
 * <li>!isPseudo && !isPager - <i>regular</i>, i.e., non-pseudo (Vertex or Edge)
 * <li>!isPseudo && isPager - n/a: all pagers are pseudo
 * <li>isPseudo && !isPager - connects a pager vertex to a regular one (Edge)
 * <li>isPseudo && isPager - pager (Vertex or Edge)
 * </ul>
 * These cases are indicated by one instance variable, pager, which communicates
 * two classes of information:
 * <ol>
 * <li>whether set or not: indicates the Vertex or Edge is pseudo (if non-null),
 * or regular (if null)
 * <li>if set: Boolean value: true -> is 'pager'; false -> not 'pager'
 * </ol>
 * See isPseudo() and isPager() for access methods.
 */
public interface ProxItemData {

    public String getColor();

    public String getLabel();

    public String getName();

    public Integer getOID();

    public Boolean getPager();

    public int getPagerNumTotal();

    public int getPagerNumShown();

    public int getPageSize();

    /**
     * @return true if vertOrEdge is a special 'pager' Vertex or Edge. returns
     *         false o/w
     * @see #isPseudo
     */
    boolean isPager();

    /**
     * @return true if vertOrEdge is a special 'pseudo' Vertex or Edge. returns
     *         false o/w. use isPagerVertOrEdge() to determine if a pseudo Vertex
     *         or Edge is also a pager
     * @see #isPager
     */
    boolean isPseudo();

    public void setColor(String color);

    public void setLabel(String label);

    public void setName(String name);

    public void setOID(Integer oid);

    public void setPager(Boolean isPager);

    public void setPagerNumTotal(int numHidden);

    public void setPagerNumShown(int numShown);

    public void setPageSize(int pageSize);

}
