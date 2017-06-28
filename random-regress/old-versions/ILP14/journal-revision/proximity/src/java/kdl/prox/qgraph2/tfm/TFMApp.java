/**
 * $Id: TFMApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFMApp.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2.tfm;

import java.io.Serializable;
import kdl.prox.qgraph2.QGItem;


/**
 * Concrete TFMQGItems subclass used during Transformation application to hold
 * application arguments. The QGItem instances are those required (in order) to
 * apply the transformation. In this sense these args are "inputs" to the
 * Transformation.
 */
public class TFMApp extends TFMQGItems implements Serializable {

    // no IVs


    /**
     * One-arg constructor. Adds qgItem to my qgItems.
     */
    public TFMApp(QGItem qgItem) {
        super(qgItem);
    }


    /**
     * Two-arg constructor. Adds qgItem1 and qgItem2 to my qgItems, in order.
     */
    public TFMApp(QGItem qgItem1, QGItem qgItem2) {
        super(qgItem1, qgItem2);
    }


    /**
     * Three-arg constructor. Adds qgItem1, qgItem2, and qgItem3 to my qgItems,
     * in order.
     */
    public TFMApp(QGItem qgItem1, QGItem qgItem2, QGItem qgItem3) {
        super(qgItem1, qgItem2, qgItem3);
    }


}
