/**
 * $Id: ConsQGVertex.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: ConsQGVertex.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A special type of QGVertex that has been consolidated, i.e. brought into SQL
 * memory. NB: Unlike other QGItems, ConsQGVertex's contents are *not* meaningful:
 * name, condEle, and annotEle.
 */
public class ConsQGVertex extends QGVertex {

    /**
     Class-based log4j category for logging.
     */
//	private static Category cat = Category.getInstance(ConsQGVertex.class.getName());


    /**
     * Full-arg constructor. qgItems is the list of items stored in the
     * Transformation that is creating me. The items' names are added to my names
     * in order to create a new globally unique name for me.
     */
    public ConsQGVertex(List qgItems) throws IllegalArgumentException {
        super(namesFromQGItems(qgItems), null, null);	// name, condEle, annotEle
    }


    /**
     * Called by constructor, returns a List of names of items in qgItems.
     */
    private static List namesFromQGItems(List qgItems) {
        List namesList = new ArrayList();	// return value. filled next
        Iterator qgItemIter = qgItems.iterator();
        while (qgItemIter.hasNext()) {
            QGItem qgItem = (QGItem) qgItemIter.next();
            namesList.addAll(qgItem.names());
        }
        return namesList;
    }


    /**
     * QGItem method. Returns true because, unlike QGVertex, ConsQGVertex instances
     * can be asterisked.
     */
    public boolean isSupportsAsterisks() {
        return true;
    }


}
