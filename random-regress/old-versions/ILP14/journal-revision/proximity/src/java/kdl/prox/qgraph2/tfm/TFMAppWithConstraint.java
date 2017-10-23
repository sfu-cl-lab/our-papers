/**
 * $Id: TFMAppWithConstraint.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFMAppWithConstraint.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.util.Assert;

/**
 * A special type of TFMApp that also has a constraint
 */
public class TFMAppWithConstraint extends TFMApp {

    // my constraint
    private QGConstraint qgConstraint;

    /**
     * Two-arg constructor. Adds qgItem1 and the constraint to my qgItems, in order
     *
     * @param qgItem1
     * @param qgConst
     */
    public TFMAppWithConstraint(QGItem qgItem1, QGConstraint qgConst) {
        super(qgItem1);
        Assert.notNull(qgConst, "null const");
        qgConstraint = qgConst;
    }


    /**
     * Utility that returns a "pretty" string showing my contents ("arguments").
     * isJustName controls whether each item's catenatedName() is added (if true)
     * or the item's toString() (if false).
     */
    public String argString(boolean isJustName) {
        StringBuffer sb = new StringBuffer();
        sb.append(super.argString(isJustName));
        sb.append(" / ");
        sb.append(qgConstraint.toString());

        // done
        return sb.toString();
    }


    public QGConstraint getConstraint() {
        return qgConstraint;
    }
}
