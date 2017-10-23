/**
 * $Id: SyntheticGraphLattice.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Creates a lattice graph, with n nodes and a given degree for each node.
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphLattice {
    private static Logger log = Logger.getLogger(SyntheticGraphLattice.class);
    private static Random rand = new Random();

    // lattice graph model
    public SyntheticGraphLattice(int n, int degree) {
        log.info("creating lattice graph with " + n + " nodes, " + degree + " degree");

        SyntheticGraphUtil.createObjects(n);

        Object[][] linkArray = new Object[n * degree][];
        int currLinkId = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 1; j <= degree; j++) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, (i + j) % n};
                currLinkId++;
            }
        }
        DB.getLinkNST().fastInsert(linkArray);
    }

    // re-wired lattice graph model
    public SyntheticGraphLattice(int n, int degree, double prob) {
        log.info("creating rewired lattice graph with " + n + " nodes, " + degree + " degree");

        SyntheticGraphUtil.createObjects(n);

        Object[][] linkArray = new Object[n * degree][];
        int currLinkId = 0;
        for (int src = 0; src < n; src++) {
            for (int j = 1; j <= degree; j++) {
                Integer dest;
                if (rand.nextDouble() < prob) {
                    dest = rand.nextInt(n);
                } else {
                    dest = (src + j) % n;
                }
                linkArray[currLinkId] = new Integer[]{currLinkId, src, dest};
                currLinkId++;
            }
        }
        DB.getLinkNST().fastInsert(linkArray);
    }

}
