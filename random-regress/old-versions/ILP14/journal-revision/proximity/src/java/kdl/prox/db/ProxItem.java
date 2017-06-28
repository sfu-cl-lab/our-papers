/**
 * $Id: ProxItem.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: ProxItem.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2001 by Matthew Cornell. All Rights Reserved.

Status: Implemented.

*/

package kdl.prox.db;


/**
 * Holds an item's information. Recall that an item is either an object or a link.
 */
public abstract class ProxItem {

    /**
     * The item's id. Set by constructor.
     */
    private int id;

    /**
     * The item's name in the subgraph. null if none. Set by constructor.
     */
    private String name = null;


    /**
     * One-arg constructor. Leaves name null.
     */
    ProxItem(int id) {
        this.id = id;
    }


    /**
     * Full-arg constructor.
     */
    ProxItem(int id, String name) {
        this.id = id;
        this.name = name;
    }


    /**
     * Compares this item to the specified object.
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof ProxItem) {
            ProxItem anotherItem = (ProxItem) anObject;
            return (id == anotherItem.getOid() &&
                    (name == null ? anotherItem.getName() == null :
                    name.equals(anotherItem.getName())) &&
                    itemType() == anotherItem.itemType());
        }
        return false;
    }


    /**
     * Returns my name.
     */
    public String getName() {
        return name;
    }


    /**
     * Returns my id.
     */
    public int getOid() {
        return id;
    }


    /**
     * Returns the hashCode for this item
     */
    public int hashCode() {
        return id +
                (name == null ? 0 : name.hashCode()) +
                (itemType().hashCode());
    }


    /**
     * Returns my Proximity itemType.
     */
    public abstract ItemTypeEnum itemType();


    /**
     * Object method.
     */
    public String toString() {
        return id + ": (" + itemType() + ")" +
                (name != null ? ": " + name : "");
    }


}
