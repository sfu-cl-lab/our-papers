/**
 * $Id: TFM09.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFM09.java 3658 2007-10-15 16:29:11Z schapira $
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

       *a  . .  Y
          '   '
         :     v
          ,---.                           ,---.
   *b    / ,-. \                   *b    / ,-. \
 - - - -( ( *a) )- - - -   =>    - - - -( ( *a) )- - - -
    X    \ `-' /    Z               X    \ `-' /    Z
          `---'                           `---'

*/

/**
 * Name: "9. absorb asterisked self-edge into asterisked vertex"
 * <p/>
 * Group: "2. self-edge"
 * <p/>
 * Applicability: Applies to any single unannotated consolidated asterisked vertex
 * ("*a") with one asterisked self loop ("Y *a") and possibly other edges, one
 * asterisked ("X *b") and one non-asterisked ("Z"). Regarding asterisks:
 * <p/>
 * o asterisked: vertex "*a", edge "X *b", edge "Y *a"
 * o not asterisked: vertex "Z"
 * o all "*a" asterisks must be from the same source ("*a")
 * o "X *b"'s asterisk source can be different or the same as that of "*a"
 * and "Y *a"
 * <p/>
 * Behavior: Removes the self loop ("Y *a").
 * <p/>
 * TFMApp usage: Contains two items: {the vertex, the self loop} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the vertex} (see above).
 */
public class TFM09 extends Transformation {
    private static Logger log = Logger.getLogger(TFM09.class);

    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (qgItems and their Query)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexCopy = (QGVertex) qgItemsCopy.get(0);
        QGEdge selfLoopQGEdgeCopy = (QGEdge) qgItemsCopy.get(1);
        Query queryCopy = qgVertexCopy.parentAQuery().rootQuery();
        // modify the copied Query
        queryCopy.removeEdgeFromParent(selfLoopQGEdgeCopy);
        qgVertexCopy.addNames(selfLoopQGEdgeCopy);
        // return the copied exec item(s)
        return new TFMExec(qgVertexCopy);
    }

    /**
     * Transformation method.
     */
    public String description() {
        return "absorb asterisked self-edge into asterisked vertex";
    }

    /**
     * Transformation method.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr)
            throws Exception {

        // get my args
        List qgItems = tfmApp.qgItems();
        QGVertex starAVertex = (QGVertex) qgItems.get(0);
        QGEdge yStarAQGEdge = (QGEdge) qgItems.get(1);
        ConsQGVertex outConsQGVertex = (ConsQGVertex) tfmExec.qgItems().get(0);
        QGEdge origYQGEdge = (QGEdge) (query.qgItemForName(yStarAQGEdge.firstName()));

        // get temp SGI NSTs for vertexA
        NST aObjTempSGINST = tempTableMgr.getNSTForVertex(starAVertex.catenatedName(), true);
        NST aLinkTempSGINST = tempTableMgr.getNSTForVertex(starAVertex.catenatedName(), false);
        Assert.notNull(aObjTempSGINST, "null obj SGI for " + starAVertex.catenatedName());
        Assert.notNull(aLinkTempSGINST, "null link SGI for " + starAVertex.catenatedName());

        // execute, reversing direction if necessary
        String vertexAName;
        String vertexBName;

        // See if the edge had an asterisk (for TFM09) - If so, pass in the
        // annotation's source item name, and make sure that vertexB is the one with
        // the annotation
        Annotation annotEle = null;
        String asteriskedEdgeFirstName = null;
        if (starAVertex.isAsterisked()) {
            QGVertex asteriskedEdge = null;
            if (origYQGEdge.vertex1().isAnnotated()) {
                vertexBName = origYQGEdge.vertex1Name();
                vertexAName = origYQGEdge.vertex2Name();
                asteriskedEdge = origYQGEdge.vertex1();
            } else {
                vertexAName = origYQGEdge.vertex1Name();
                vertexBName = origYQGEdge.vertex2Name();
                asteriskedEdge = origYQGEdge.vertex2();
            }
            asteriskedEdgeFirstName = starAVertex.asteriskSourceItemName();
            annotEle = asteriskedEdge.annotation();
        } else {
            vertexAName = yStarAQGEdge.vertex1Name();
            vertexBName = yStarAQGEdge.vertex2Name();
        }

        QGVertex annotVertex = (QGVertex) query.rootQuery().qgItemForName(vertexBName);
        Set edges = annotVertex.edges();
        String[] incidentEdges = new String[edges.size()];
        int i = 0;
        for (Iterator iterator = edges.iterator(); iterator.hasNext(); i++) {
            QGEdge qgEdge = (QGEdge) iterator.next();
            incidentEdges[i] = qgEdge.firstName();
        }
        List objLinkTempSGINSTs = execTFMExecInternal(qgUtil, origYQGEdge,
                vertexAName, vertexBName,
                annotEle, asteriskedEdgeFirstName,
                incidentEdges, aObjTempSGINST, aLinkTempSGINST);

        // save new temp SGI NSTs and remove old
        NST objTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST linkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        tempTableMgr.putNSTForVertex(outConsQGVertex.catenatedName(),
                objTempSGINST, linkTempSGINST);
        tempTableMgr.clearNSTForVertex(starAVertex.catenatedName());

        return outConsQGVertex.catenatedName();
    }

    protected List execTFMExecInternal(QGUtil qgUtil,
                                       QGEdge yStarAQGEdge,
                                       String vertexAName, String vertexBName,
                                       Annotation asteriskedEdgeAnnotation,
                                       String asteriskedEdgeFirstName,
                                       String[] incidentEdges,
                                       NST aObjTempSGINST, NST aLinkTempSGINST) {
        Assert.notNull(asteriskedEdgeFirstName, "edge first name cannot be null");

        // create the new tables to be returned
        NST objectTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();

        // 1. We can treat these as two consolidated vertices, in which B = A
        //    the task is then to combine then, effectively removing the self-loop
        //   Use then the same method used by TFM 12, which joins two consolidated vertices
        // in this case, however, there is no recoding of subg_ids, because the
        //    edges MUST be annotated.
        // [ a_(item_id subg_id name)  link_id o1_id o2_id   b_(item_id ...)   new_subg_id]
        NST linksAndNewSubgNST = qgUtil.connectConsolidatedVertices(yStarAQGEdge,
                aObjTempSGINST, aObjTempSGINST,
                vertexAName, vertexBName);

        // 2. Copy from original object tables those subgraphs that are
        // listed in the linksAndNewSubgNST table, using their new subg_ids

        // First copy everything BUT bs
        NST notBsNST = aObjTempSGINST.filter("name NE '" + vertexBName + "'");
        objectTempSGINST.insertRowsFromNST(notBsNST);
        notBsNST.release();

        // and now copy the Bs from the link table
        NST linksAndNewSubgNSTCopy = linksAndNewSubgNST.project("b_item_id, new_subg_id, b_name");
        NST uniqueLinksAndNewSubgNST = linksAndNewSubgNSTCopy.distinct("b_item_id, new_subg_id");
        objectTempSGINST.insertRowsFromNST(uniqueLinksAndNewSubgNST);
        linksAndNewSubgNSTCopy.release();
        uniqueLinksAndNewSubgNST.release();

        // Into the new link table, we need to copy only those links that
        // connect objects still in the Object table (some Bs may have been
        // removed in the previous step) But first, assign the new subg_id to the links
        qgUtil.reCheckLinks(aLinkTempSGINST,
                objectTempSGINST, vertexBName, incidentEdges, linkTempSGINST);

        // 4. And finally add the Y links
        SGIUtil.copyRowsIntoSGITableWithNewName(linkTempSGINST,
                linksAndNewSubgNST, "link_id", "new_subg_id", yStarAQGEdge.catenatedName());

        // release
        linksAndNewSubgNST.release();

        // 5. For TFM 09, if the vertex is asterisked, take care of it
        //    Find those subgraphs that have fewer Bs than required
        //    and delete them
        if (asteriskedEdgeAnnotation.annotMin() > 0) {
            // Get the objects corresponding to asteriskedEdgeFirstName
            // count how many distinct ones we have per subgraph
            // and get the ones whose count is >= the annotation
            String filterDef = "name EQ '" + asteriskedEdgeFirstName + "'";
            NST asteriskedEdgeObjects = objectTempSGINST.filter(filterDef);
            asteriskedEdgeObjects.groupBy("subg_id");
            asteriskedEdgeObjects.addCountColumn("group_id", "vertex_cnt");
            NST toKeepNST = SGIUtil.getRowsWithinRange(asteriskedEdgeObjects,
                    "vertex_cnt", asteriskedEdgeAnnotation.annotMin(), -1);
            String toKeepBAT = toKeepNST.getNSTColumn("subg_id").getBATName();
            // and delete the ones that do not match from the object and link tables
            objectTempSGINST.deleteRows("subg_id NOTIN " + toKeepBAT);
            linkTempSGINST.deleteRows("subg_id NOTIN " + toKeepBAT);
            toKeepNST.release();
            asteriskedEdgeObjects.release();
        }

        return makeSGIListFromNSTs(objectTempSGINST, linkTempSGINST);
    }

    /**
     * Transformation method.
     */
    public Set isApplicable(QGVertex qgVertex) {
        HashSet tfmAppSet = new HashSet();        // returned value. filled next if applicable
        Set selfLoops = qgVertex.selfLoops();    // used below for testing
        if (!(qgVertex instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (!qgVertex.isAsterisked()) {
            return tfmAppSet;
        } else if (qgVertex.annotation() != null) {
            return tfmAppSet;
        } else if (selfLoops.size() != 1) {
            return tfmAppSet;
        }
        // continue: have one self loop ("Y *a")
        QGEdge selfLoopQGEdge = (QGEdge) (selfLoops.iterator().next());
        if (!selfLoopQGEdge.isAsterisked()) {
            return tfmAppSet;
        } else if (!selfLoopQGEdge.isSameAsteriskSource(qgVertex)) {
            return tfmAppSet;
        }
        // continue: have "Y *a" with same source as "*a"
        tfmAppSet.add(new TFMApp(qgVertex, selfLoopQGEdge));
        // done
        return tfmAppSet;
    }

    /**
     * Transformation method.
     */
    public int number() {
        return 9;
    }

}
