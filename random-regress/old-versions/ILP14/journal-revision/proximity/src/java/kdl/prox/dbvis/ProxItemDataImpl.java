/**
 * $Id: ProxItemDataImpl.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;


/**
 * Delegate used by ProxSpaseVertex and ProxDirectedSparseEdge to store
 * ProxItemData.
 *
 * @see ProxItemData
 */
public class ProxItemDataImpl implements ProxItemData {

    private String color = null;
    private Boolean isPager = null;
    private String label = null;
    private String name = null;
    private Integer oid = null;
    private int pagerNumHidden;
    private int pagerNumShown;
    private int pageSize = 0;


    public String getColor() {
        return color;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public Integer getOID() {
        return oid;
    }

    public Boolean getPager() {
        return isPager;
    }

    public int getPagerNumShown() {
        return pagerNumShown;
    }

    public int getPagerNumTotal() {
        return pagerNumHidden;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isPager() {
        return (getPager() != null) && (getPager().booleanValue());
    }

    public boolean isPseudo() {
        return getPager() != null;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOID(Integer oid) {
        this.oid = oid;
    }

    public void setPager(Boolean isPager) {
        this.isPager = isPager;
    }

    public void setPagerNumShown(int numShown) {
        this.pagerNumShown = numShown;
    }

    public void setPagerNumTotal(int numHidden) {
        this.pagerNumHidden = numHidden;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

}
