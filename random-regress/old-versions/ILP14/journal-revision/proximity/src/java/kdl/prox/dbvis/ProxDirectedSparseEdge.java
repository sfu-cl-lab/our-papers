/**
 * $Id: ProxDirectedSparseEdge.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;


/**
 * Represents a Proximity link.
 *
 * @see ProxItemData
 */
public class ProxDirectedSparseEdge extends DirectedSparseEdge implements ProxItemData {

    ProxItemData proxItemData = new ProxItemDataImpl();


    public ProxDirectedSparseEdge(Vertex o1Vertex, Vertex o2Vertex, Integer linkOID) {
        super(o1Vertex, o2Vertex);
        setOID(linkOID);
    }

    public ProxDirectedSparseEdge(Vertex o1Vertex, Vertex o2Vertex, Integer linkOID, String label) {
        this(o1Vertex, o2Vertex, linkOID);
        setLabel(label);
    }

    public String getColor() {
        return proxItemData.getColor();
    }

    public String getLabel() {
        return proxItemData.getLabel();
    }

    public String getName() {
        return proxItemData.getName();
    }

    public Integer getOID() {
        return proxItemData.getOID();
    }

    public Boolean getPager() {
        return proxItemData.getPager();
    }

    public int getPagerNumShown() {
        return proxItemData.getPagerNumShown();
    }

    public int getPagerNumTotal() {
        return proxItemData.getPagerNumTotal();
    }

    public int getPageSize() {
        return proxItemData.getPageSize();
    }

    public boolean isPager() {
        return proxItemData.isPager();
    }

    public boolean isPseudo() {
        return proxItemData.isPseudo();
    }

    public void setColor(String color) {
        proxItemData.setColor(color);
    }

    public void setLabel(String label) {
        proxItemData.setLabel(label);
    }

    public void setName(String name) {
        proxItemData.setName(name);
    }

    public void setOID(Integer oid) {
        proxItemData.setOID(oid);
    }

    public void setPager(Boolean isPager) {
        proxItemData.setPager(isPager);
    }

    public void setPagerNumShown(int numShown) {
        proxItemData.setPagerNumShown(numShown);
    }

    public void setPagerNumTotal(int numHidden) {
        proxItemData.setPagerNumTotal(numHidden);
    }

    public void setPageSize(int pageSize) {
        proxItemData.setPageSize(pageSize);
    }

}
