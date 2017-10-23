/**
 * $Id: NSTCreator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.util;

import kdl.prox.dbmgr.NST;

/**
 * An abstract class, used to pass into the NSTCache the code that must be executed to compute an NST if it's not
 * already in the cache
 */
public abstract class NSTCreator {

    public abstract NST create();
}
