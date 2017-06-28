/**
 * $Id: TFM21.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM21.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;

import java.util.*;

/**
 * Name: "21. process constraint on an annotated ivertex"
 * <p/>
 * Group: "constraints"
 * <p/>
 * Applicability: Applies to an asterisked, unannotated consolidated vertex that contains
 * all the items in a constraint in the list of constraints for the query.
 * <p/>
 * Behavior: Applies the constraint to the vertex, and removes the constraint from
 * the list
 * <p/>
 * TFMApp usage: Contains one item: {the vertex} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the consolidated vertex} (see above).
 * <p/>
 * NB: Assumes that the vertex with the annotation is NOT connected to more than one vertex.
 * This is because we remove Bs but not the elements that depend on them
 * This works for now, because TFM08 is not implemented
 * <p/>
 * todo : change isApplicable so that this doesn't apply to the case of a B connected to other vertices, when we implement TFM08
 * <p/>
 */
public class TFM21 extends TFMGroupConstraintsParent {

    /**
     * Transformation method.
     */
    public String description() {
        return "apply a constraint on an annotated item";
    }


    /**
     * Internal execute method, for the tests.
     * The procedure is:
     * 1. Get the objects from the SGI table for A
     * 2. Get the objects from the SGI table for B
     * 3. If the constraint involves attributes, join the tables above with the corresponding attr vals
     * 4. Create two NSTs that have head/a_item_id , head/b_item_id (or a_attr_value)
     * 5. Call [>=] on those two NSTs, to get the list of rows that will stay
     * 6. Apply the annotation
     * 7. Copy remaining subgraphs and elements into the new SGI tables
     * <p/>
     * It assumes that B is not connected to other vertices that depend on it. This is either because:
     * a. TFM08 is not implemented and therefore B, an annotated item, can not be connected to another
     *
     * @param qgUtil
     * @param objTempSGINST
     * @param linkTempSGINST
     * @param qgConstraint
     * @param isVertices             true if constraint applies to vertices, false o/w
     * @param edgesOnAnnotatedVertex
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
        // For simplicity in the code below, make item2Name be the annotated item
        String item1Name = qgConstraint.item1Name();
        String item2Name = qgConstraint.item2Name();
        String annotItemName = qgConstraint.annotItemName();
        Assert.notNull(annotItemName, "null annotItemName -- should not be executing TFM 21 but TFM 20 instead");
        if (annotItemName.equals(item1Name)) {
            item1Name = item2Name;
            item2Name = annotItemName;
        }
        NST aItems = SGIUtil.getSubgraphItemsWithName(objTempSGINST, item1Name);
        NST bItems = SGIUtil.getSubgraphItemsWithName(objTempSGINST, item2Name);

        NST remainingAsAndBsNST = createCompareTablesAndApplyComparison(qgConstraint, aItems, bItems, isVertices);

        // Re-apply the lower bound of the annotation and keep track of which As and Bs we need to keep
        // If the lower bound is 0, then all the As remain, and the Bs from the remainingAsAndBsNST table
        // If the lower bound is 1, then we keep As and Bs from the remainingAsAndBsNST table
        // If the lower bound >  1, then we keep the As and Bs from remainingAsAndBsNST after applying the annot.
        Annotation constraintAnnotation = qgConstraint.annotation();
        NST aSubgsToKeep = null;
        NST bSubgsToKeep = null;
        if (constraintAnnotation.annotMin() == 0) {
            aSubgsToKeep = aItems.copy();          // all the original subgraphs
            aSubgsToKeep.renameColumn("subg_id", "a_subg_id");
            bSubgsToKeep = remainingAsAndBsNST.copy(); // the Bs after the constraint
        } else if (constraintAnnotation.annotMin() == 1) {
            // do nothing. Rows with < 1 have already disappeared
            aSubgsToKeep = remainingAsAndBsNST.copy();  // the As after the constraint
            bSubgsToKeep = remainingAsAndBsNST.copy();  // the Bs after the constraint
        } else if (constraintAnnotation.annotMin() > 1) {
            remainingAsAndBsNST.groupBy("a_subg_id");
            remainingAsAndBsNST.addCountColumn("group_id", "vertex_cnt");
            // get the ones whose count is GE the lower bound of the annotation
            NST rowsWithinRange = SGIUtil.getRowsWithinRange(remainingAsAndBsNST,
                    "vertex_cnt", constraintAnnotation.annotMin(), -1);
            aSubgsToKeep = rowsWithinRange.copy();
            rowsWithinRange.release();
            bSubgsToKeep = aSubgsToKeep.copy();
        }
        remainingAsAndBsNST.release();

        // The next step is to fill in the object and link tables.
        // We do this by first copying all the As and their assoc. items, and then the Bs
        //    NB: this can be done because the annotated vertex B is not connected
        //        to other vertices; so, if A disappears, then the subgraph disappears with all its other elements
        NST subgsToKeep = objTempSGINST.filter("subg_id IN " + aSubgsToKeep.getNSTColumn("a_subg_id").getBATName());
        NST notAnnotItemsNST = subgsToKeep.filter("name NE '" + item2Name + "'");
        newObjSGINST.insertRowsFromNST(notAnnotItemsNST);
        notAnnotItemsNST.release();
        subgsToKeep.release();
        // b. Copy the distinct (b_subg_id, b_item) that are left
        NST remainingBsNST = bSubgsToKeep.project("b_item_id, b_subg_id, b_name");
        NST uniqueRemainingBsNST = remainingBsNST.distinct("b_item_id, b_subg_id");
        newObjSGINST.insertRowsFromNST(uniqueRemainingBsNST);
        remainingBsNST.release();
        uniqueRemainingBsNST.release();

        // release NSTs
        aSubgsToKeep.release();
        bSubgsToKeep.release();
        aItems.release();
        bItems.release();

        // Remove links that no longer connect items in the object table
        qgUtil.reCheckLinks(linkTempSGINST, newObjSGINST,
                item2Name, edgesOnAnnotatedVertex, newLinkSGINST);

        // Return the new tables in a list (objects, links)
        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(newObjSGINST);
        tempSGINSTs.add(newLinkSGINST);

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
        } else if (!qgVertex.isAsterisked()) {
            return tfmAppSet;
        }
        // continue: see if qgVertex's containing AbstractQuery has
        // constraints whose two items are contained in this qgVertex
        // and the asterisk is on one of them
        AbstractQuery parentAQuery = qgVertex.parentAQuery().rootQuery();
        List constraints = parentAQuery.constraints();
        Iterator constIter = constraints.iterator();
        while (constIter.hasNext()) {
            QGConstraint qgConst = (QGConstraint) constIter.next();
            // if the two items in this constraint are contained in the vertex, apply
            String item1Name = qgConst.item1Name();
            String item2Name = qgConst.item2Name();
            if (!qgConst.isEdgeConstraint() &&
                    qgVertex.names().contains(item1Name) &&
                    qgVertex.names().contains(item2Name) &&
                    qgConst.hasAnnot()) {
                // Removed 'if' for the case of constraints that involve items from two
                // different sub-queries. In that case, the asteriskSource will be
                // the entry point to the sub-query, when the constrained item is something
                // else.
                // Constraints on sub-query items only valid if the the annotated item
                // is [0..]
                // if ((item1Name.equals(asteriskSource) || item2Name.equals(asteriskSource))) {
                tfmAppSet.add(new TFMAppWithConstraint(qgVertex, qgConst));
                //}
            }
        }
        // done
        return tfmAppSet;
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 21;
    }


}
