/**
 * $Id: TFM18.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM18.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*

Diagram:

                   X                                *B  . .  X
              ,- - - -,                               '   '
            ,           ,                            :     v
         ,-+-.         ,-+-.                          ,---.
   W    / ,-. \       / ,-. \    Z              W    / ,-. \   *B
- - - -( ( A ) )-----( ( B ) )- - - -   =>   - - - -( ( *B) )- - - -
        \ `-' /   Y   \ `-' /                        \ `-' /    Z
         `---'         `---'                          `---'
                       [i..j]

*/

/**
 * Name: "18. collapse consolidated vertex and annotated consolidated vertex of
 * degree >1"
 * <p/>
 * Group: "5. annotated vertex of degree >1"
 * <p/>
 * Applicability: Applies to a consolidated unannotated vertex ("A") that is
 * connected by one edge ("Y") and possibly additional edges ("X") to a
 * consolidated annotated vertex ("B") with possibly other edges ("Z").
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
public class TFM18 extends Transformation {

    private static Logger log = Logger.getLogger(TFM18.class);


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
        // find the element over which we must add the asterisk
        String asteriskItemName = (qgEdgeYCopy.vertex1() == qgVertexBCopy ?
                qgEdgeYCopy.vertex1Name() : qgEdgeYCopy.vertex2Name());
        // add asterisks to starBConsQGV, Xs, and Zs regardless of whether there
        // was an X or Z
        starBConsQGV.setAsteriskSource(qgVertexBOrig, asteriskItemName);
        Iterator xzQGEdgeIter = xzQGEdges.iterator();
        // todo fix this
        // right now, we cannot deal with self-loops in this transformation. Throw an error
        Assert.condition(!xzQGEdgeIter.hasNext(), "Only one edge connecting two " +
                "consolidated vertices " +
                "is allowed, for the time being. Sorry.");
        while (xzQGEdgeIter.hasNext()) {
            QGEdge xzQGEdge = (QGEdge) xzQGEdgeIter.next();
            xzQGEdge.setAsteriskSource(qgVertexBOrig, asteriskItemName);
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

        // get args
        List qgItems = tfmApp.qgItems();
        QGVertex qgVertexB = (QGVertex) qgItems.get(0);
        QGEdge qgEdgeY = (QGEdge) qgItems.get(1);
        QGVertex qgVertexA = (QGVertex) qgItems.get(2);
        ConsQGVertex outConsQGVertex = (ConsQGVertex) tfmExec.qgItems().get(0);

        // get temp SGI NSTs from vertices
        NST aObjTempSGINST = tempTableMgr.getNSTForVertex(qgVertexA.catenatedName(), true);
        NST aLinkTempSGINST = tempTableMgr.getNSTForVertex(qgVertexA.catenatedName(), false);
        NST bObjTempSGINST = tempTableMgr.getNSTForVertex(qgVertexB.catenatedName(), true);
        NST bLinkTempSGINST = tempTableMgr.getNSTForVertex(qgVertexB.catenatedName(), false);

        // swap vertices in the call to internal exec
        // if they are not passed in the right order (A == vertex1 in edge)
        List objLinkTempSGINSTs;
        String vertexAName = (qgEdgeY.vertex1() == qgVertexA ? qgEdgeY.vertex1Name()
                : qgEdgeY.vertex2Name());
        String vertexBName = (qgEdgeY.vertex1() == qgVertexA ? qgEdgeY.vertex2Name()
                : qgEdgeY.vertex1Name());

        objLinkTempSGINSTs = execTFMExecInternal(qgUtil, qgEdgeY,
                vertexAName, vertexBName,
                qgVertexB.annotation(),
                aObjTempSGINST, aLinkTempSGINST,
                bObjTempSGINST, bLinkTempSGINST);

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
     * @param qgEdgeY
     * @param vertexAName
     * @param vertexBName
     * @param vertexAnnotEle
     * @param aObjTempSGINST
     * @param aLinkTempSGINST
     * @param bObjTempSGINST
     * @param bLinkTempSGINST
     * @return
     * @
     */
    public List execTFMExecInternal(QGUtil qgUtil, QGEdge qgEdgeY,
                                    String vertexAName, String vertexBName,
                                    Annotation vertexAnnotEle,
                                    NST aObjTempSGINST, NST aLinkTempSGINST,
                                    NST bObjTempSGINST, NST bLinkTempSGINST) {

        // first of all, create the linkTempSGINST and objectTempSGINST tables
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        NST objectTempSGINST = SGIUtil.createTempSGINST();

        // 1. These are two consolidated vertices that need to be joined
        // Finds all links of type Y, As in vertex A, Bs in vertex B,
        // connects them,
        // removes rows that do not match the Y annotation
        // and recomputes subg_id if there is an expansion because no annotation is present
        NST linksAndNewSubgNST = qgUtil.connectConsolidatedVertices(qgEdgeY,
                aObjTempSGINST, bObjTempSGINST,
                vertexAName, vertexBName);

        // 2. Apply annotation element on B vertex
        //    This is done by counting, for each A subgraph, how many different B subgraphs there are in the table.
        //    As a matter of fact, we check how many different Bs we have for each A subgraph
        //    This is in general the same as counting different b_subg_ids (since there can only
        //     be a single B per b_subg_id), but if the same B is repeated in different b_subg_ids
        //     we want to count them as 1.
        // If the annotation is 0, then get keep all the As, and the Bs from linksAndNewSubgNST
        // If the annotation is 1, then we keep the As and Bs from linksAndNewSubgNST
        // If the annotation is > 1, then we keep the As and Bs that pass the annotation
        NST aSubgsToKeep = null;
        NST bSubgsToKeep = null;
        if (vertexAnnotEle.annotMin() == 0) {
            // Get all the As
            aSubgsToKeep = aObjTempSGINST.copy();
            aSubgsToKeep.renameColumn("subg_id", "a_subg_id");
            bSubgsToKeep = linksAndNewSubgNST;
        } else if (vertexAnnotEle.annotMin() == 1) {
            aSubgsToKeep = linksAndNewSubgNST;
            bSubgsToKeep = aSubgsToKeep;
        } else {
            linksAndNewSubgNST.groupBy("a_subg_id, b_item_id");
            linksAndNewSubgNST.addDistinctCountColumn("a_subg_id", "group_id", "vertex_cnt");
            aSubgsToKeep = SGIUtil.getRowsWithinRange(linksAndNewSubgNST, "vertex_cnt",
                    vertexAnnotEle.annotMin(), -1);
            bSubgsToKeep = aSubgsToKeep;
        }

        // 3. Create the new tables by pulling in original subgraphs from
        //    the consolidated vertices, with their new subg_id (from recodeNSTs)
        aSubgsToKeep.addCopyColumn("a_subg_id", "a_new_id");
        NST aRecodeNST = aSubgsToKeep.projectDistinct("a_subg_id, a_new_id");
        aRecodeNST.renameColumn("a_subg_id", "old_id").renameColumn("a_new_id", "new_id");

        NST bRecodeNST = bSubgsToKeep.projectDistinct("b_subg_id, a_subg_id");
        bRecodeNST.renameColumn("b_subg_id", "old_id").renameColumn("a_subg_id", "new_id");

        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(aObjTempSGINST, aRecodeNST,
                objectTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(aLinkTempSGINST, aRecodeNST,
                linkTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(bObjTempSGINST, bRecodeNST,
                objectTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(bLinkTempSGINST, bRecodeNST,
                linkTempSGINST);
        aRecodeNST.release();
        bRecodeNST.release();

        // 4. And finally, copy the new links into the new table, with the new Y name
        SGIUtil.copyRowsIntoSGITableWithNewName(linkTempSGINST,
                bSubgsToKeep, "link_id", "a_subg_id", qgEdgeY.catenatedName());

        // And release (if they are copies they won't be released)
        linksAndNewSubgNST.release();
        aSubgsToKeep.release();

        return makeSGIListFromNSTs(objectTempSGINST, linkTempSGINST);
    }


    /**
     * Transformation method. qgVertexB is the "B" vertex candidate.
     * (We don't test "A" candidates).
     */
    public Set isApplicable(QGVertex qgVertexB) {
        HashSet tfmAppSet = new HashSet();      // returned value. filled next if applicable
        if (!(qgVertexB instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (qgVertexB.isAsterisked()) {
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
        return 18;
    }

}
