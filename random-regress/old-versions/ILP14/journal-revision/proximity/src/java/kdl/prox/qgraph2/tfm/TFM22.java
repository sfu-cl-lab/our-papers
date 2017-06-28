/**
 * $Id: TFM22.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM22.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;

import java.util.*;

/**
 * Name: "22. process constraint on an annotated links"
 * <p/>
 * Group: "constraints"
 * <p/>
 * Applicability: Applies to an asterisked, unannotated consolidated vertex that contains
 * all the items in a constraint in the list of constraints for the query. Is only
 * valid for constraints on links where one of the links is annotated, and the
 * end vertex is annotated with [0..]
 * <p/>
 * Behavior: Applies the constraint to the vertex, and removes the constraint from
 * the list
 * <p/>
 * TFMApp usage: Contains one item: {the vertex} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the consolidated vertex} (see above).
 * <p/>
 * NB: Assumes that if the end vertex is annotated, then it is NOT connected to another vertex.
 * This is because we remove Bs but not the elements that depend on them
 * This works for now, because TFM08 is not implemented.
 * <p/>
 * todo : change isApplicable so that this doesn't apply to the case of a B connected to other vertices, when we implement TFM08
 * <p/>
 */
public class TFM22 extends TFMGroupConstraintsParent {

    /**
     * Transformation method.
     */
    public String description() {
        return "apply a constraint on an annotated item";
    }


    /**
     * Internal execute method, for the tests.
     * The procedure is:
     * 1. Get the links from the SGI table for X
     * 2. Get the links from the SGI table for Y
     * 3. If the constraint involves attributes, join the tables above with the corresponding attr vals
     * 4. Create two NSTs that have head/a_item_id , head/b_item_id (or a_attr_value)
     * 5. Call [>=] on those two NSTs, to get the list of rows that will stay
     * 6. Apply the annotation on the link
     * 7. Copy remaining subgraphs and elements into the new SGI tables
     * <p/>
     * NB: Assumes that if the end vertex is annotated, then it is NOT connected to another vertex.
     * This is because we remove Bs but not the elements that depend on them
     * This works for now, because TFM08 is not implemented
     *
     * @param qgUtil
     * @param objTempSGINST
     * @param linkTempSGINST
     * @param qgConstraint
     * @param isVertices             true if constraint applies to vertices, false o/w
     * @param edgesOnAnnotatedVertex
     * @return
     * @throws kdl.prox.monet.MonetException
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
        Assert.notNull(annotItemName, "null annotItemName -- should not be executing TFM 22 but TFM 20 instead");
        if (annotItemName.equals(item1Name)) {
            item1Name = item2Name;
            item2Name = annotItemName;
        }
        NST xItems = SGIUtil.getSubgraphItemsWithName(linkTempSGINST, item1Name);
        NST yItems = SGIUtil.getSubgraphItemsWithName(linkTempSGINST, item2Name);

        NST remainingXsAndYsNST = createCompareTablesAndApplyComparison(qgConstraint, xItems, yItems, isVertices);

        // Now join this table with the objects the Ys point to. This is so
        // that we can re-apply the annotation on Y. As an added convenience,
        // we'll also have the Cs (annotated end-points of Ys) that will
        // remain.
        NST linksInSubgraphs = DB.getLinkNST().intersect(remainingXsAndYsNST, "link_id = b_item_id");

        NST cItems = SGIUtil.getSubgraphItemsWithName(objTempSGINST, edgesOnAnnotatedVertex[0]);
        NST o1NST = linksInSubgraphs.intersect(cItems, "o1_id EQ item_id", "link_id, o1_id");
        NST o2NST = linksInSubgraphs.intersect(cItems, "o2_id EQ item_id", "link_id, o2_id");
        NST linkAndCEndNST = o1NST.union(o2NST, "link_id, o1_id");  // union works by col index, not name, so o2_id = o1_id
        NST remainingXsAndYsAndCsNST = remainingXsAndYsNST.join(linkAndCEndNST, "b_item_id  =  link_id");
        remainingXsAndYsAndCsNST.renameColumns("a_item_id, a_subg_id, a_name, b_item_id, b_subg_id, b_name, c_link_id, c_obj_id");
        remainingXsAndYsNST.release();
        linksInSubgraphs.release();
        cItems.release();
        o1NST.release();
        o2NST.release();
        linkAndCEndNST.release();

        // Re-apply the lower bound of the annotation and keep track of which subgraphs we need to keep
        // If the lower bound is 1, then we keep As and Bs from the remainingXsAndYsNST table
        // If the lower bound >  1, then we keep the As and Bs from remainingXsAndYsNST after applying the annot.
        Annotation constraintAnnotation = qgConstraint.annotation();
        NST itemSubgsToKeep = null;
        if (constraintAnnotation.annotMin() == 1) {
            // do nothing. Rows with < 1 have already disappeared
            itemSubgsToKeep = remainingXsAndYsAndCsNST.copy();  // the As after the constraint
        } else if (constraintAnnotation.annotMin() > 1) {
            remainingXsAndYsAndCsNST.groupBy("a_subg_id, c_obj_id");
            remainingXsAndYsAndCsNST.addCountColumn("group_id", "vertex_cnt");
            // get the ones whose count is GE the lower bound of the annotation
            NST rowsWithinRange = SGIUtil.getRowsWithinRange(remainingXsAndYsAndCsNST,
                    "vertex_cnt", constraintAnnotation.annotMin(), -1);
            itemSubgsToKeep = rowsWithinRange.copy();
            rowsWithinRange.release();
        }
        remainingXsAndYsAndCsNST.release();

        // The next step is to fill in the object and link tables.
        // We do this by first copying all the Xs and their assoc. items, and then the Ys that match
        //    NB: this can be done because the annotated vertex B is not connected
        //        to other vertices; so, if A disappears, then the subgraph disappears with all its other elements
        NST subgsToKeep = linkTempSGINST.filter("subg_id IN " + itemSubgsToKeep.getNSTColumn("a_subg_id").getBATName());
        NST notAnnotItemsNST = subgsToKeep.filter("name NE '" + item2Name + "'");
        newLinkSGINST.insertRowsFromNST(notAnnotItemsNST);
        notAnnotItemsNST.release();
        subgsToKeep.release();
        // b. Copy the distinct (b_subg_id, b_item) that are left
        NST remainingYsNST = itemSubgsToKeep.project("b_item_id, b_subg_id, b_name");
        NST uniqueRemainingYsNST = remainingYsNST.distinct("b_item_id, b_subg_id");
        newLinkSGINST.insertRowsFromNST(uniqueRemainingYsNST);
        remainingYsNST.release();
        uniqueRemainingYsNST.release();

        // do the same for objects; copy everything except the Cs, and then the Cs that are left
        subgsToKeep = objTempSGINST.filter("subg_id IN " + itemSubgsToKeep.getNSTColumn("a_subg_id").getBATName());
        notAnnotItemsNST = subgsToKeep.filter("name NE '" + edgesOnAnnotatedVertex[0] + "'");
        newObjSGINST.insertRowsFromNST(notAnnotItemsNST);
        notAnnotItemsNST.release();
        subgsToKeep.release();
        // b. Copy the distinct (b_subg_id, c_obj_id) that are left
        NST remainingCsNST = itemSubgsToKeep.project("c_obj_id, b_subg_id");
        remainingCsNST.addConstantColumn("name", "str", edgesOnAnnotatedVertex[0]);
        NST uniqueRemainingCsNST = remainingCsNST.distinct("c_obj_id, b_subg_id");
        newObjSGINST.insertRowsFromNST(uniqueRemainingCsNST);
        remainingCsNST.release();
        uniqueRemainingCsNST.release();

        // release NSTs
        itemSubgsToKeep.release();
        xItems.release();
        yItems.release();

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
        } else if (qgVertex.isAsterisked()) {
            return tfmAppSet;
        }
        // continue: see if qgVertex's containing AbstractQuery has
        // constraints whose two items are contained in this qgVertex
        // and the asterisk is on one of them
        AbstractQuery parentAQuery = qgVertex.parentAQuery();
        List constraints = parentAQuery.constraints();
        Iterator constIter = constraints.iterator();
        while (constIter.hasNext()) {
            QGConstraint qgConst = (QGConstraint) constIter.next();
            // if the two items in this constraint are contained in the vertex, apply
            String item1Name = qgConst.item1Name();
            String item2Name = qgConst.item2Name();
            if (qgConst.isEdgeConstraint() &&
                    qgVertex.names().contains(item1Name) &&
                    qgVertex.names().contains(item2Name) &&
                    qgConst.hasAnnot()) {
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
        return 22;
    }


}
