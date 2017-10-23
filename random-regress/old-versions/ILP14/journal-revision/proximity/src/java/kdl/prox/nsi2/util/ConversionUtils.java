/**
 * $Id: ConversionUtils.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.util;

import kdl.prox.nsi2.graph.Node;

import java.util.*;

/**
 * Utilities to convert from one data representation to another (maps to arrays)
 */
public class ConversionUtils {

    /**
     * Convert an array of ints into a set of Integers
     * @param targets array of ints
     * @return Set of integers
     */
    public static Set<Integer> intArraytoSet(int[] targets) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int target : targets) {
            list.add(target);
        }
        return new HashSet<Integer>(list);
    }

    /**
     * Convert a comma separated string of integers into a list of integers
     * @param s String of comma separated integers
     * @return List of integers
     */
    public static List<Integer> stringToIntegerList(String s) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        String[] strings = s.split(",");
        for (String str : strings) {
            list.add(Integer.parseInt(str));
        }
        return list;
    }

    /**
     * Convert a collection of nodes into a collection of integers corresponding to the node ids
     * @param nodes A collection of Nodes
     * @return A collection of Integers (node ids)
     */
    public static Collection<Integer> nodesToIntegers(Collection<Node> nodes) {
        List<Integer> ids = new ArrayList<Integer>();
        for (Node node : nodes) {
            ids.add(node.id);
        }
        return ids;
    }
}
