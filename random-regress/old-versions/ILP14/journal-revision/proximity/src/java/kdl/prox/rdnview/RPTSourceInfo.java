/**
 * $Id: RPTSourceInfo.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.rdnview;

/**
 * A simple class to store information that needs to be passed around
 */
public class RPTSourceInfo {

    public String item;
    public String attr;
    public boolean isObject;

    public RPTSourceInfo(String i, String a, boolean isO) {
        item = i;
        attr = a;
        isObject = isO;
    }

}
