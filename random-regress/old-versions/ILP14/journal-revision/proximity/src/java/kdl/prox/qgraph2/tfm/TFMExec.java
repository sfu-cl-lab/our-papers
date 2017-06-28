/**
 * $Id: TFMExec.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFMExec.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2.tfm;

import kdl.prox.qgraph2.QGItem;


/**
 * Concrete TFMQGItems subclass used during Transformation execution to hold
 * execution arguments. The QGItem instances are those required (in order) to
 * execute the transformation. In this sense these args are "outputs" from the
 * Transformation application.
 */
public class TFMExec extends TFMQGItems {

    // no IVs


    /**
     * One-arg constructor. Adds qgItem to my qgItems.
     */
    public TFMExec(QGItem qgItem) {
        super(qgItem);
    }


    /**
     * Two-arg constructor. Adds qgItem1 and qgItem2 to my qgItems, in order.
     */
    public TFMExec(QGItem qgItem1, QGItem qgItem2) {
        super(qgItem1, qgItem2);
    }


    /**
     * Three-arg constructor. Adds qgItem1, qgItem2, and qgItem3 to my qgItems,
     * in order.
     */
    public TFMExec(QGItem qgItem1, QGItem qgItem2, QGItem qgItem3) {
        super(qgItem1, qgItem2, qgItem3);
    }


}
