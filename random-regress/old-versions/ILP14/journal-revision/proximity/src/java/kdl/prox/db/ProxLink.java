/**
 * $Id: ProxLink.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: ProxLink.java 3658 2007-10-15 16:29:11Z schapira $
*/

package kdl.prox.db;


/**
 * One of two concrete Item subclasses, represents Proximity links.
 * <p/>
 * todo equals() and hashCode() should check o1ID and o2ID values
 */
public class ProxLink extends ProxItem {

    /**
     * The ID of my object1 and object2, respectively. Set by constructor.
     */
    private int o1ID;
    private int o2ID;

    /**
     * My two ProxObjS. null if not set. Used by some programs to maintain
     * references to actual linked-to ProxObjS. Set by constructor.
     */
    private ProxObj proxObj1 = null;
    private ProxObj proxObj2 = null;


    /**
     * Two-arg constructor. Link endpoints not initialized
     *
     * @param id
     * @param name
     */
    public ProxLink(int id, String name) {
        this(id, name, -1, -1);
    }


    /**
     * Three-arg constructor. Saves args in IVs.
     */
    public ProxLink(int id, int o1ID, int o2ID) {
        super(id);
        this.o1ID = o1ID;
        this.o2ID = o2ID;
    }


    /**
     * Four-arg constructor. Saves args in IVs.
     */
    public ProxLink(int id, String name, int o1ID, int o2ID) {
        super(id, name);
        this.o1ID = o1ID;
        this.o2ID = o2ID;
    }


    /**
     * Full-arg constructor. Saves args in IVs.
     */
    ProxLink(int id, String name, int o1ID, int o2ID, ProxObj proxObj1,
             ProxObj proxObj2) {
        this(id, name, o1ID, o2ID);
        this.proxObj1 = proxObj1;
        this.proxObj2 = proxObj2;
    }

    /**
     * Compares this item to the specified object.
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof ProxLink) {
            ProxLink anotherItem = (ProxLink) anObject;
            return (getOid() == anotherItem.getOid() &&
                    (getName() == null ? anotherItem.getName() == null :
                    getName().equals(anotherItem.getName())) &&
                    itemType() == anotherItem.itemType() &&
                    o1ID() == -1 ? anotherItem.o1ID() == -1 : o1ID() == anotherItem.o1ID() &&
                    o2ID() == -1 ? anotherItem.o2ID() == -1 : o2ID() == anotherItem.o2ID());
        }
        return false;
    }


    /**
     * Item method.
     */
    public ItemTypeEnum itemType() {
        return ItemTypeEnum.LINK;
    }


    /**
     * Returns my o1ID.
     */
    public int o1ID() {
        return o1ID;
    }


    /**
     * Returns my o2ID.
     */
    public int o2ID() {
        return o2ID;
    }


    /**
     * Returns my proxObj1.
     */
    public ProxObj proxObj1() {
        return proxObj1;
    }


    /**
     * Returns my proxObj2.
     */
    public ProxObj proxObj2() {
        return proxObj2;
    }


    /**
     * Object method.
     */
    public String toString() {
        return itemType() + ": " + (getName() == null ? "<no name>" : getName()) +
                " (" + getOid() + ":" + o1ID + "->" + o2ID + ")";
    }


}
