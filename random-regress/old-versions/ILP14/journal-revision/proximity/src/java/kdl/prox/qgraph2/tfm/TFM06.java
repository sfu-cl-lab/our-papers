/**
 * $Id: TFM06.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 $Id: TFM06.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.*;

/*
 Diagram:

                  X                                *B  . .  X
              ,- - - -,                               '   '
            ,           ,                            :     v
         ,-+-.         ,-+-.                          ,---.
   W    / ,-. \       /     \    Z              W    / ,-. \   *B
- - - -( ( A ) )-----(   B   )- - - -   =>   - - - -( ( *B) )- - - -
        \ `-' /   Y   \     /                        \ `-' /    Z
         `---'         `---'                          `---'
                       [i..j]

*/

/**
 * Name: "6. absorb edge and annotated vertex of degree >1"
 * <p/>
 * Group: "5. annotated vertex of degree >1"
 * <p/>
 * Applicability: Applies to a consolidated unannotated vertex ("A") that is
 * connected by one edge ("Y") and possibly additional edges ("X") to a
 * non-consolidated annotated vertex ("B") with possibly other edges ("Z").
 * "Y" can not cross a Subquery boundary. No asterisks are allowed. "Y" can
 * not be a self loop.
 * <p/>
 * Behavior: "Absorbs" "B" into "A" via "Y" and replaces them with a new
 * consolidated vertex "*B". Adds "*B" asterisks to the new consolidated
 * vertex, the new "X" self loop, and any "Z" edges.
 * <p/>
 * TFMApp usage: Contains three items: {"B", "Y", "A"} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {"*B"} (see above).
 */
public class TFM06 extends Transformation {

    private static Logger log = Logger.getLogger(TFM06.class);


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (query and qgItems)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexBOrig = (QGVertex) tfmApp.qgItems().get(0);     // NB: *not* the copy!
        QGVertex qgVertexBCopy = (QGVertex) qgItemsCopy.get(0);
        QGEdge qgEdgeYCopy = (QGEdge) qgItemsCopy.get(1);
        QGVertex qgVertexACopy = (QGVertex) qgItemsCopy.get(2);
        Query queryCopy = qgVertexBCopy.parentAQuery().rootQuery();
        // modify the copied Query, registering and saving necessary info first
        Set xzQGEdges = new HashSet();              // will hold Xs and Zs. filled next
        xzQGEdges.addAll(qgVertexBCopy.edges());    // Y, Xs, and Zs. NB: done before absorption
        xzQGEdges.remove(qgEdgeYCopy);              // remove Y to get Xs and Zs
        queryCopy.absorbEdgeAndVertex(qgEdgeYCopy, qgVertexBCopy);
        ConsQGVertex starBConsQGV = new ConsQGVertex(qgItemsCopy);
        queryCopy.replaceQGVertex(qgVertexACopy, starBConsQGV);
        // add asterisks to starBConsQGV, Xs, and Zs regardless of whether there
        // was an X or Z
        starBConsQGV.setAsteriskSource(qgVertexBOrig);
        Iterator xzQGEdgeIter = xzQGEdges.iterator();
        while (xzQGEdgeIter.hasNext()) {
            QGEdge xzQGEdge = (QGEdge) xzQGEdgeIter.next();
            xzQGEdge.setAsteriskSource(qgVertexBOrig);
        }
        // return the copied exec item(s)
        return new TFMExec(starBConsQGV);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "absorb edge and annotated vertex of degree >1";
    }


    /**
     * Transformation method.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr)
            throws Exception {

        // get my args
        List qgItems = tfmApp.qgItems();
        QGVertex qgVertexB = (QGVertex) qgItems.get(0);
        QGEdge qgEdgeY = (QGEdge) qgItems.get(1);
        QGVertex qgVertexA = (QGVertex) qgItems.get(2);
        ConsQGVertex outConsQGVertex = (ConsQGVertex) tfmExec.qgItems().get(0);

        // get temp SGI NSTs for vertexA
        NST aObjTempSGINST = tempTableMgr.getNSTForVertex(qgVertexA.catenatedName(), true);
        NST aLinkTempSGINST = tempTableMgr.getNSTForVertex(qgVertexA.catenatedName(), false);
        Assert.notNull(aObjTempSGINST, "null obj SGI for " + qgVertexA.catenatedName());
        Assert.notNull(aLinkTempSGINST, "null link SGI");

        // Get the name of vertex A
        String vertexAName = (qgEdgeY.vertex1() == qgVertexA ?
                qgEdgeY.vertex1Name() : qgEdgeY.vertex2Name());

        List objLinkTempSGINSTs = execTFMExecInternal(qgUtil,
                vertexAName, qgEdgeY, qgVertexB,
                aObjTempSGINST, aLinkTempSGINST);

        // save new temp SGI NSTs and remove old
        NST objTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST linkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        tempTableMgr.putNSTForVertex(outConsQGVertex.catenatedName(),
                objTempSGINST, linkTempSGINST);
        tempTableMgr.clearNSTForVertex(qgVertexA.catenatedName());
        tempTableMgr.clearNSTForVertex(qgVertexB.catenatedName());

        return outConsQGVertex.catenatedName();
    }


    /**
     * @param qgUtil
     * @param vertexAName
     * @param qgEdgeY
     * @param qgVertexB
     * @param aObjTempSGINST
     * @param aLinkTempSGINST
     */
    protected List execTFMExecInternal(QGUtil qgUtil,
                                       String vertexAName, QGEdge qgEdgeY, QGVertex qgVertexB,
                                       NST aObjTempSGINST, NST aLinkTempSGINST) {

        // first, create the linkTempSGINST and objectTempSGINST tables
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        NST objectTempSGINST = SGIUtil.createTempSGINST();

        // 1. Find the objects in the consolidated vertex to be connected to Bs
        // aObjectsFromSGINST = [item_id, subg_id, name]
        NST aObjectsFromSGINST = SGIUtil.getSubgraphItemsWithName(aObjTempSGINST, vertexAName);

        // 2. Get all links of the correct type (eg, ActorOf)
        // linkListNST = [o1_id, o2_id, link_id]
        NST linkListNST = qgUtil.getMatchingLinks(qgEdgeY.condEleChild());

        // 3. Get all the objects that match the condition on B (eg. Movie)
        // bObjectListNST = [o_id]
        NST bObjectListNST = qgUtil.getMatchingObjects(qgVertexB.condEleChild());

        // 4. Compute A --> links <-- B
        // aXbNST = [a_(item_id, subg_id, name), o1_id, o2_id, link_id, o_id]
        NST aXbNST;
        if (qgEdgeY.isDirected()) {
            boolean isReverseDir = (!qgEdgeY.vertex1Name().equals(vertexAName));
            aXbNST = qgUtil.getObjectsConnectedViaDirectedLinks(aObjectsFromSGINST, bObjectListNST, linkListNST, isReverseDir);
        } else {
            aXbNST = qgUtil.getObjectsConnectedViaUndirectedLinks(aObjectsFromSGINST, bObjectListNST, linkListNST);
        }

        // 5. Remove rows (links) that do not match annotation of Y edge
        // Since the B vertex is annotated, so is the Y edge
        NST matchingLinks = aXbNST;
        matchingLinks.groupBy("a_subg_id, a_item_id, b_o_id");
        matchingLinks.addCountColumn("group_id", "group_cnt");
        Annotation yAnnot = qgEdgeY.annotation();
        NST reducedNST = SGIUtil.getRowsWithinRange(matchingLinks, "group_cnt",
                yAnnot.annotMin(), yAnnot.annotMax());

        // 6. If the annotation on the edge is [0..], then we need to
        //    copy all the As, and with them the Bs that match the edge annotation
        //    (which are stored in reducedNST)
        //   But if the annotation is [1..] or over, then we need to make
        //    sure that we only bring the As that have at least the specified number
        //    of Bs
        NST materializedFinalNST;
        if (qgVertexB.annotation().annotMin() > 0) {
            // 6. Remove rows (links) that do not match annotation of B vertex
            // This is done by counting, for each subgraph, how many different
            // end points there are in the table.
            reducedNST.groupBy("a_subg_id, b_o_id");
            reducedNST.addDistinctCountColumn("a_subg_id", "group_id", "vertex_cnt");
            NST finalNST = SGIUtil.getRowsWithinRange(reducedNST, "vertex_cnt",
                    qgVertexB.annotation().annotMin(), -1);
            materializedFinalNST = finalNST.copy();
            finalNST.release();

            // 7. finally, write new consolidated vertex into new tables
            // now, bring matching subgraphs from original tables
            String validSubgIds = "subg_id IN " + materializedFinalNST.getNSTColumn("a_subg_id").getBATName();
            SGIUtil.copySelectedRowsIntoSGITable(objectTempSGINST, aObjTempSGINST, validSubgIds);
            SGIUtil.copySelectedRowsIntoSGITable(linkTempSGINST, aLinkTempSGINST, validSubgIds);
        } else {
            // Bring in all the As
            objectTempSGINST.insertRowsFromNST(aObjTempSGINST);
            linkTempSGINST.insertRowsFromNST(aLinkTempSGINST);
            materializedFinalNST = reducedNST.copy(); // keep the list of Bs and their subg_ids to bring in
        }
        // and finally, bring the new ones from materializedFinalNST 
        SGIUtil.copyDistinctRowsIntoSGITableWithNewName(linkTempSGINST,
                materializedFinalNST, "link_id", "a_subg_id", qgEdgeY.catenatedName());
        SGIUtil.copyDistinctRowsIntoSGITableWithNewName(objectTempSGINST,
                materializedFinalNST, "b_o_id", "a_subg_id", qgVertexB.catenatedName());

        // return the new tables in a list (objects, links)
        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(objectTempSGINST);
        tempSGINSTs.add(linkTempSGINST);

        materializedFinalNST.release();
        reducedNST.release();
        aXbNST.release();
        bObjectListNST.release();
        linkListNST.release();
        aObjectsFromSGINST.release();

        return tempSGINSTs;
    }


    /**
     * Transformation method. qgVertexB is the "B" vertex candidate.
     * (We don't test "A" candidates).
     */
    public Set isApplicable(QGVertex qgVertexB) {
        HashSet tfmAppSet = new HashSet();      // returned value. filled next if applicable
        if ((qgVertexB instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (isHasAsteriskedEdges(qgVertexB)) {
            return tfmAppSet;
        } else if (qgVertexB.annotation() == null) {
            return tfmAppSet;
        }
        // continue: find all "Y" "A" pairs
        Iterator qgEdgeIter = qgVertexB.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdgeY = (QGEdge) qgEdgeIter.next();
            QGVertex qgVertexA = qgEdgeY.otherVertex(qgVertexB);
            if (qgEdgeY.isSelfLoop()) {
                continue;
            } else if (!(qgVertexA instanceof ConsQGVertex)) {
                continue;
            } else if (qgVertexA.isAsterisked()) {
                continue;
            } else if (isHasAsteriskedEdges(qgVertexA)) {
                continue;
            } else if (qgVertexA.annotation() != null) {
                continue;
            } else if (qgEdgeY.isCrossesSubqBoundsEdge()) {
                continue;
            }
            tfmAppSet.add(new TFMApp(qgVertexB, qgEdgeY, qgVertexA));
        }
        // done
        return tfmAppSet;
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 6;
    }


}
