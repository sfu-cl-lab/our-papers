/**
 * $Id: RPTWalker.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt;

/**
 * Abstract class used to walk an RPT. Define the processNode method, indicating what to do on
 * each node, and then call walk() on the root of the tree.
 */
public abstract class RPTWalker {

    /**
     * processNode method will be called for every node in the tree, in pre-order
     *
     * @param node
     * @param depth
     */
    public abstract void processNode(RPTNode node, int depth);


    /**
     * Basic method to walk through a tree.
     *
     * @param root
     */
    public void walk(RPTNode root) {
        walk(root, 0);
    }

    private void walk(RPTNode node, int depth) {
        if (node == null) {
            return;
        }

        processNode(node, depth);
        if (!node.isLeaf()) {
            walk(node.yesBranch, depth + 1);
            walk(node.noBranch, depth + 1);
        }
    }
}
