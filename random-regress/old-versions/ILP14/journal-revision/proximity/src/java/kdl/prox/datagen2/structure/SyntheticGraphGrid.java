/**
 * $Id: SyntheticGraphGrid.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import org.apache.log4j.Logger;

/**
 * Creates a grid graph, with n nodes and sqrt(n) sides.
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphGrid {
    private static Logger log = Logger.getLogger(SyntheticGraphGrid.class);

    // lattice graph model
    public SyntheticGraphGrid(int n) {
        int side = (int) Math.sqrt(n);
        log.info("creating grid graph with " + side + " nodes per side (~" + n + " total)");

        SyntheticGraphUtil.createObjects((int) (Math.pow((int) Math.sqrt(n), 2)));

        Object[][] linkArray = new Object[n * 4][];
        int currLinkId = 0;
        for (int i = 0; i < n; i++) {

            // left
            if (!(i % side == 0)) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i - 1};
                currLinkId++;
            }

            // right
            if (!(i % side == (side - 1))) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i + 1};
                currLinkId++;
            }

            // up
            if (i >= side) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i - side};
                currLinkId++;
            }

            // down
            if ((i + side) <= (n - 1)) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i + side};
                currLinkId++;
            }
        }

        Object[][] linkArraySub = new Object[currLinkId][];
        System.arraycopy(linkArray, 0, linkArraySub, 0, currLinkId);
        DB.getLinkNST().fastInsert(linkArraySub);
    }
}
