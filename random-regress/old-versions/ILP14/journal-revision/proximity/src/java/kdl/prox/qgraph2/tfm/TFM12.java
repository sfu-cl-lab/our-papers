/**
 * $Id: TFM12.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM12.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*
Diagram:

                  X                                    . .  X
              ,- - - -,                               '   '
            ,           ,                            :     v
         ,-+-.         ,-+-.                          ,---.
   W    / ,-. \       / ,-. \    Z              W    / ,-. \
- - - -( ( A ) )-----( ( B ) )- - - -   =>   - - - -( (   ) )- - - -
        \ `-' /   Y   \ `-' /                        \ `-' /    Z
         `---'         `---'                          `---'
*/

/**
 * Name: " 12. collapse two consolidated vertices and connecting edge"
 * <p/>
 * Group: "unannotated vertex"
 * <p/>
 * Applicability: Applies to a consolidated unannotated vertex ("A") that is
 * connected by one edge ("Y") and possibly additional edges ("X") to a
 * consolidated unannotated vertex ("B") with possibly other edges ("Z").
 * "Y" can not cross a Subquery boundary. No asterisks are allowed. "Y" can
 * not be a self loop.
 * <p/>
 * Behavior: "Absorbs" "B" into "A" via "Y" and replaces them with a new
 * consolidated vertex.
 * <p/>
 * TFMApp usage: Contains three items: {"B", "Y", "A"} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the new consolidated vertex} (see above).
 */

public class TFM12 extends Transformation {

    private static Logger log = Logger.getLogger(TFM12.class);


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (query and qgItems)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexBCopy = (QGVertex) qgItemsCopy.get(0);
        QGEdge qgEdgeYCopy = (QGEdge) qgItemsCopy.get(1);
        QGVertex qgVertexACopy = (QGVertex) qgItemsCopy.get(2);
        Query queryCopy = qgVertexBCopy.parentAQuery().rootQuery();
        // modify the copied Query, registering and saving necessary info first
        queryCopy.absorbEdgeAndVertex(qgEdgeYCopy, qgVertexBCopy);
        ConsQGVertex bAYConsQGV = new ConsQGVertex(qgItemsCopy);
        queryCopy.replaceQGVertex(qgVertexACopy, bAYConsQGV);
        // return the copied exec item(s)
        return new TFMExec(bAYConsQGV);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "collapse two consolidated vertices and connecting edge";
    }


    /**
     * Transformation method. Works by creating a "temp_sgi_N" table for the new
     * consolidated vertex, getting the "temp_sgi_N" table names for "A" and "B",
     * then joining via a "temp_link" table. Throws Exception if problems.
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
     * Assumes qgEdgeY goes from NST 'a' to NST 'b'. The caller has to enforce
     * this, possibly switching the args based on qgEdgeY and its vertex1 and 2.
     *
     * @param qgUtil
     * @param qgEdgeY
     * @param aObjTempSGINST
     * @param aLinkTempSGINST
     * @param bObjTempSGINST
     * @param bLinkTempSGINST
     * @return List with two items: [objTempSGINST, linkTempSGINST]
     */
    protected List execTFMExecInternal(QGUtil qgUtil, QGEdge qgEdgeY,
                                       String vertexAName, String vertexBName,
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

        // 2. Create the new tables by pulling in original subgraphs from
        //    the consolidated vertices, with their new subg_id (from recodeNSTs)
        NST aRecodeNST = linksAndNewSubgNST.projectDistinct("a_subg_id, new_subg_id");
        aRecodeNST.renameColumn("a_subg_id", "old_id").renameColumn("new_subg_id", "new_id");
        NST bRecodeNST = linksAndNewSubgNST.projectDistinct("b_subg_id, new_subg_id");
        bRecodeNST.renameColumn("b_subg_id", "old_id").renameColumn("new_subg_id", "new_id");
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(aObjTempSGINST, aRecodeNST,
                objectTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(aLinkTempSGINST, aRecodeNST,
                linkTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(bObjTempSGINST, bRecodeNST,
                objectTempSGINST);
        SGIUtil.pullSubgraphsFromConsolidatedVertexIntoNewVertex(bLinkTempSGINST, bRecodeNST,
                linkTempSGINST);

        // 3. And finally, copy the new links into the new table, with the new Y name
        SGIUtil.copyRowsIntoSGITableWithNewName(linkTempSGINST,
                linksAndNewSubgNST, "link_id", "new_subg_id", qgEdgeY.catenatedName());

        // and release
        linksAndNewSubgNST.release();
        aRecodeNST.release();
        bRecodeNST.release();

        return makeSGIListFromNSTs(objectTempSGINST, linkTempSGINST);
    }


    /**
     * Transformation method. qgVertexB is the "B" vertex candidate. (We don't test
     * "A" candidates).
     */
    public Set isApplicable(QGVertex qgVertexB) {
        HashSet tfmAppSet = new HashSet();        // returned value. filled next if applicable
        if (!(qgVertexB instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (qgVertexB.isAsterisked()) {
            return tfmAppSet;
        } else if (isHasAsteriskedEdges(qgVertexB)) {
            return tfmAppSet;
        } else if (qgVertexB.annotation() != null) {
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
        return 12;
    }


}
