/**
 * $Id: RPTUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.util;

import org.apache.log4j.Logger;

/**
 * Basic utility methods for the RPT class
 */
public class RPTUtil {

    private static Logger log = Logger.getLogger(RPTUtil.class);

    public static String getDepthIndentation(int depth) {
        String tabs = "";
        for (int i = 0; i < depth; i++) {
            tabs += "\t";
        }
        return (tabs);
    }

}
