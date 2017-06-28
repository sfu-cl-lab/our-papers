/**
 * $Id: SyntheticGraphUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import java.util.Random;

public class SyntheticGraphUtil {
    private static Logger log = Logger.getLogger(SyntheticGraphUtil.class);

    public static int createObjects(int n) {
        log.debug("writing objects to database");

        Object[][] data = new Object[n][];
        for (int i = 0; i < n; i++) {
            data[i] = new Integer[]{i};
        }

        DB.getObjectNST().fastInsert(data);
        log.debug("added " + n + " objects");
        return DB.getObjectNST().getRowCount();
    }


    public static NST chooseRandomNodePairsNST(int num) {
        Random rand = new Random();
        int n = DB.getObjectNST().getRowCount();
        String sources[] = new String[num];
        String targets[] = new String[num];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = "" + rand.nextInt(n);
            do {
                targets[i] = "" + rand.nextInt(n);
            } while (sources[i].equals(targets[i]));
        }

        String sourcesBAT = MonetUtil.createBATFromList(DataTypeEnum.OID, sources);
        String targetsBAT = MonetUtil.createBATFromList(DataTypeEnum.OID, targets);
        NST sourcesNST = new NST(new String[]{sourcesBAT}, "sid", "oid");
        NST targetsNST = new NST(new String[]{targetsBAT}, "tid", "oid");

        NST numberedObjectNST = DB.getObjectNST().copy();
        numberedObjectNST.addNumberColumn("number"); // id, number

        NST sourcesJoinedNST = numberedObjectNST.join(sourcesNST, "number EQ sid", "id"); // id
        sourcesJoinedNST.renameColumn("id", "sid");
        sourcesNST.release();

        NST targetsJoinedNST = numberedObjectNST.join(targetsNST, "number EQ tid", "id"); // id
        targetsJoinedNST.renameColumn("id", "tid");
        targetsNST.release();

        NST pairNST = new NST("o1_id,o2_id", "oid,oid");
        String[] idBATs = new String[]{sourcesJoinedNST.getNSTColumn("sid").getBATName(), targetsJoinedNST.getNSTColumn("tid").getBATName()};
        pairNST.insertRowsFromNST(new NST(idBATs, "o1_id,o2_id", "oid,oid"));

        numberedObjectNST.release();
        sourcesJoinedNST.release();
        targetsJoinedNST.release();

        return pairNST;
    }

}
