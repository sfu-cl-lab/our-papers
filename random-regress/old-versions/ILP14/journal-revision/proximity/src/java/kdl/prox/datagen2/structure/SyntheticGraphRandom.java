/**
 * $Id: SyntheticGraphRandom.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

/**
 * Creates a random graph, with n nodes and e edges
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphRandom {
    private static Logger log = Logger.getLogger(SyntheticGraphRandom.class);


    public SyntheticGraphRandom(int n, double ratio) {
        this(n, new Double(ratio * n).intValue());
    }

    public SyntheticGraphRandom(int n, int e) {
        log.info("creating random graph with " + n + " nodes, " + e + " edges");

        SyntheticGraphUtil.createObjects(n);

        NST pairNST = SyntheticGraphUtil.chooseRandomNodePairsNST(e);
        pairNST.addNumberColumn("link_id");
        NST linkNST = pairNST.project("link_id, o1_id, o2_id");
        DB.getLinkNST().insertRowsFromNST(linkNST);

        pairNST.release();
        linkNST.release();
    }
}
