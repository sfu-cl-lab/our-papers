/**
 * $Id: DegreeNSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * The Degree NSI labels each node with its degree.  It is not accurate.
 *
 */

public class DegreeNSI implements NSI {
    private static Logger log = Logger.getLogger(DegreeNSI.class);
    private static final String attrName = "degree";

    public DegreeNSI() {
        // store the degree of each node as an attribute
        log.info("adding degree attribute to all objects");

        NST linkNST = DB.getLinkNST().project("o1_id");
        linkNST.insertRowsFromNST(DB.getLinkNST().project("o2_id"));

        NST degreeNST = linkNST.addCountColumn("o1_id", "degree").distinct();
        DB.getObjectAttrs().defineAttributeOrClearValuesIfExists(attrName, "int");
        DB.getObjectAttrs().getAttrDataNST(attrName).insertRowsFromNST(degreeNST);

        linkNST.release();
        degreeNST.release();
    }

    public double distance(Integer nodeId1, Integer nodeId2) {
        String filterDef = "id EQ " + nodeId1 + " OR id EQ " + nodeId2;

        DB.beginScope();
        List<Integer> vals = DB.getObjectAttrs().getAttrDataNST(attrName).selectRows(filterDef, "value", "*").toIntList(1);
        DB.endScope();

        return 2.0 * DB.getObjectNST().getRowCount() - vals.get(0) - vals.get(1);
    }

}
