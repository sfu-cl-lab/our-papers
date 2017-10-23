/**
 * $Id: DBHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DBHandler.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;



/**
 * Callback for DBLineHandler via Interpeter. Handles parsing a smart ascii ii
 * file at the level of object, link, and attribute events. Implementations are
 * notified when an object or link defnition is encountered, and when an
 * attribute definition is encountered within an object or link.
 */
public interface DBHandler {

    public void addAttribute(String name, String value);

    public void addLink(String o1Name, String o2Name);

    public void addObject(String name);

}
