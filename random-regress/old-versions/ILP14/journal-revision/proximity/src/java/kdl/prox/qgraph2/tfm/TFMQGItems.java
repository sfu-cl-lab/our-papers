/**
 * $Id: TFMQGItems.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFMQGItems.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.util.Assert;


/**
 * An abstract helper superclass used by Transformation instances. Holds a List of
 * QGItems that is used in different way by concrete subclasses.
 */
public abstract class TFMQGItems implements java.io.Serializable /* TEST */ {

    /**
     * My QGItem instances. Managed by construtor.
     */
    private List qgItems = new ArrayList();


    /**
     * One-arg constructor. Adds qgItem to my qgItems.
     */
    TFMQGItems(QGItem qgItem) {
        Assert.condition(qgItem != null, "qgItem null");
        // continue
        qgItems.add(qgItem);
    }


    /**
     * Two-arg constructor. Adds qgItem1 and qgItem2 to my qgItems, in order.
     */
    TFMQGItems(QGItem qgItem1, QGItem qgItem2) {
        this(qgItem1);
        Assert.condition(qgItem2 != null, "qgItem2 null");
        // continue
        qgItems.add(qgItem2);
    }


    /**
     * Three-arg constructor. Adds qgItem1, qgItem2, and qgItem3 to my qgItems,
     * in order.
     */
    TFMQGItems(QGItem qgItem1, QGItem qgItem2, QGItem qgItem3) {
        this(qgItem1, qgItem2);
        Assert.condition(qgItem3 != null, "qgItem3 null");
        // continue
        qgItems.add(qgItem3);
    }


    /**
     * Utility that returns a "pretty" string showing my contents ("arguments").
     * isJustName controls whether each item's catenatedName() is added (if true)
     * or the item's toString() (if false).
     */
    public String argString(boolean isJustName) {
        // set qgItemsSB
        StringBuffer qgItemsSB = new StringBuffer();	// filled next
        Iterator qgItemIter = qgItems().iterator();
        while (qgItemIter.hasNext()) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            if (isJustName) {
                qgItemsSB.append(qgItem.catenatedName());
                qgItemsSB.append("|");
            } else {
                qgItemsSB.append(qgItem);
                qgItemsSB.append(", ");
            }
        }
        qgItemsSB.setLength(qgItemsSB.length() - (isJustName ? 1 : 2));	// remove final "|" or ", "
        // done
        return qgItemsSB.toString();
    }


    /**
     * Returns a copy of my qgItems.
     */
    public List qgItems() {
        return Collections.unmodifiableList(qgItems);
    }


}
