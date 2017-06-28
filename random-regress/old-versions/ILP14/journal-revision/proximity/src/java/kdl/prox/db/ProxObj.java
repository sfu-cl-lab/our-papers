/**
 * $Id: ProxObj.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 *  $Id: ProxObj.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

/**
 * One of two concrete Item subclasses, represents Proximity objects.
 */
public class ProxObj extends ProxItem {

    // no IVs


    /**
     * One-arg constructor.
     */
    public ProxObj(int id) {
        super(id);
    }


    /**
     * Full-arg constructor.
     */
    public ProxObj(int id, String name) {
        super(id, name);
    }


    /**
     * Item method.
     */
    public ItemTypeEnum itemType() {
        return ItemTypeEnum.OBJECT;
    }


    /**
     * Object method.
     */
    public String toString() {
        return itemType() + ": " + (getName() == null ? "<no name>" : getName()) +
                " (" + getOid() + ")";
    }


}
