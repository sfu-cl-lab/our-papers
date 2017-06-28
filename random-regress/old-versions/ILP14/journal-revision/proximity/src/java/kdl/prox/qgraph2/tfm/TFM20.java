/**
 * $Id: TFM20.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM20.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.AbstractQuery;
import kdl.prox.qgraph2.ConsQGVertex;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Name: "20. process constraint on non-annotated items"
 * <p/>
 * Group: "constraints"
 * <p/>
 * Applicability: Applies to an unasterisked, unannotated consolidated vertex that contains
 * all the items in a constraint in the list of constraints for the query.
 * <p/>
 * Behavior: Applies the constraint to the vertex, and removes the constraint from
 * the list
 * <p/>
 * TFMApp usage: Contains one item: {the vertex} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the consolidated vertex} (see above).
 */
public class TFM20 extends TFMGroupConstraintsParent {

    /**
     * Transformation method.
     */
    public String description() {
        return "apply a constraint on an unannotated item";
    }


    /**
     * Internal execute method, for the tests
     *
     * @param qgUtil
     * @param objTempSGINST
     * @param linkTempSGINST
     * @param qgConstraint
     * @param isVertices             true if constraint applies to vertices, false o/w
     * @param edgesOnAnnotatedVertex not used -- just for compatibility with TFM21
     * @return
     * @
     */
    protected List execTFMExecInternal(QGUtil qgUtil, NST objTempSGINST,
                                       NST linkTempSGINST, QGConstraint qgConstraint,
                                       boolean isVertices, String[] edgesOnAnnotatedVertex) {

        // create the new tables to be returned
        NST newObjSGINST = SGIUtil.createTempSGINST();
        NST newLinkSGINST = SGIUtil.createTempSGINST();

        // Take objects A and B (item1Name and item2Name from constraint)
        String item1Name = qgConstraint.item1Name();
        String item2Name = qgConstraint.item2Name();
        NST aItems = SGIUtil.getSubgraphItemsWithName((isVertices ? objTempSGINST : linkTempSGINST), item1Name);
        NST bItems = SGIUtil.getSubgraphItemsWithName((isVertices ? objTempSGINST : linkTempSGINST), item2Name);

        NST limitedNST = createCompareTablesAndApplyComparison(qgConstraint, aItems, bItems, isVertices);

        // And finally apply the filter to the original NSTs, to keep only
        // those that we want.
        NST saveObjNST = objTempSGINST.filter("subg_id IN " + limitedNST.getNSTColumn("a_subg_id").getBATName());
        NST saveLinkNST = linkTempSGINST.filter("subg_id IN " + limitedNST.getNSTColumn("a_subg_id").getBATName());
        newObjSGINST.insertRowsFromNST(saveObjNST);
        newLinkSGINST.insertRowsFromNST(saveLinkNST);
        saveObjNST.release();
        saveLinkNST.release();

        // Return the new tables in a list (objects, links)
        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(newObjSGINST);
        tempSGINSTs.add(newLinkSGINST);

        aItems.release();
        bItems.release();
        limitedNST.release();

        return tempSGINSTs;
    }


    /**
     * Transformation method.
     */
    public Set isApplicable(QGVertex qgVertex) {
        HashSet tfmAppSet = new HashSet();        // returned value. filled next if applicable
        if (!(qgVertex instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (qgVertex.annotation() != null) {
            return tfmAppSet;
        } else if (qgVertex.isAsterisked()) {
            return tfmAppSet;
        }
        // continue: see if qgVertex's containing AbstractQuery has
        // constraints whose two items are contained in this qgVertex
        AbstractQuery parentAQuery = qgVertex.parentAQuery().rootQuery();
        List constraints = parentAQuery.constraints();
        Iterator constIter = constraints.iterator();
        while (constIter.hasNext()) {
            QGConstraint qgConst = (QGConstraint) constIter.next();
            // if the two items in this constraint are contained in the vertex, apply
            // but those items cannot have annotations!
            String item1Name = qgConst.item1Name();
            String item2Name = qgConst.item2Name();
            if (qgVertex.names().contains(item1Name) &&
                    qgVertex.names().contains(item2Name) &&
                    !qgConst.hasAnnot()) {
                tfmAppSet.add(new TFMAppWithConstraint(qgVertex, qgConst));
            }
        }
        // done
        return tfmAppSet;
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 20;
    }


}
