/**
 * $Id: TFM11.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFM11.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.


*/

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.ConsQGVertex;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.TempTableMgr;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 Diagram:


         ,---.                   ,---.
   X    / ,-. \            X    / ,-. \
- - - -( ( *a) )   =>   - - - -( (   ) )
        \ `-' /                 \ `-' /
         `---'                   `---'
*/


/**
 * Name: "11. apply upper limit of numeric annotation"
 * <p/>
 * Group: "remove asterisk"
 * <p/>
 * Applicability: Applies to a single consolidated vertex ("X") with an asterisk
 * ("*a") and without an annotation. No asterisked edges are allowed. Zero or
 * more edges ("X") are allowed.
 * <p/>
 * Behavior: Removes the consolidated vertex's asterisk ("*a").
 * <p/>
 * TFMApp usage: Contains one item: {"*a"} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the consolidated vertex} (see above).
 * <p/>
 * SQL Details: Removes subgraphs from the consolidated vertex that don't match
 * "*a"'s asterisk source's ("a") upper bound.
 */
public class TFM11 extends Transformation {

    /**
     * Class-based log4j category for logging.
     */
    private static Logger log = Logger.getLogger(TFM11.class);


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (qgItems and their Query)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexCopy = (QGVertex) qgItemsCopy.get(0);
        // modify the copied Query
        qgVertexCopy.clearAsteriskSource();    // clear the asterisk
        // return the copied exec item(s)
        return new TFMExec(qgVertexCopy);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "apply upper limit of numeric annotation";
    }


    /**
     * Transformation method. NB: This method saves its results into the
     * existing "A" consolidated vertex's tempSGI NSTs, rather than into a
     * new one for the new consolidated vertex. Also note that we don't need to
     * do anything if "*a"'s asterisk source ("a") has no upper bound.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr) throws Exception {
        // get my args
        List qgItems = tfmApp.qgItems();
        QGVertex qgVertex = (QGVertex) qgItems.get(0);
        // get asterix source and name of the annotated item, and the upper bound
        String annotatedItemName = qgVertex.asteriskSourceItemName();
        int upperBound = qgVertex.asteriskSourceAnnotMax();
        // get input tables
        NST objTempSGINST = tempTableMgr.getNSTForVertex(qgVertex.catenatedName(), true);
        NST linkTempSGINST = tempTableMgr.getNSTForVertex(qgVertex.catenatedName(), false);
        // execute
        execTFMExecInternal(objTempSGINST, linkTempSGINST, annotatedItemName, upperBound);
        // NB: we do not need to save the results 
        // because removing an asterisk does not change the vertex's name 
        // and it's all done in-place

        return qgVertex.catenatedName();
    }


    /**
     * @param objTempSGINST
     * @param linkTempSGINST
     * @param vertexName
     * @param upperBound
     */
    protected void execTFMExecInternal(NST objTempSGINST, NST linkTempSGINST,
                                       String vertexName, int upperBound) {

        // do nothing to do if no upper bound
        if (upperBound == -1) {
            return;
        }

        // Steps:
        // 1. Get the subg_id in the objTempSGINST with vertexName
        //    that are above the upperBound
        // 2. Delete those subg_id from the object and link tempSGI
        NST filteredByNameNST = SGIUtil.getSubgraphItemsWithName(objTempSGINST, vertexName);
        // Do a group by the subg_id, and compute the count of group_id values
        filteredByNameNST.groupBy("subg_id");
        filteredByNameNST.addCountColumn("group_id", "group_cnt");
        // leave only those that are above the threshold 
        NST nameAndCountNST = filteredByNameNST.filter("group_cnt GE " + (upperBound + 1));

        // the subg_id that remain in the filteredByNameAndCountNST need to be removed

        // delete -- in place
        objTempSGINST.deleteRows("subg_id IN " + nameAndCountNST.getNSTColumn("subg_id").getBATName());
        linkTempSGINST.deleteRows("subg_id IN " + nameAndCountNST.getNSTColumn("subg_id").getBATName());

        nameAndCountNST.release();
        filteredByNameNST.release();
    }


    /**
     * Transformation method.
     */
    public Set isApplicable(QGVertex qgVertex) {
        HashSet tfmAppSet = new HashSet();        // returned value. filled next if applicable
        if (!(qgVertex instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (!qgVertex.isAsterisked()) {
            return tfmAppSet;
        } else if (isHasAsteriskedEdges(qgVertex)) {
            return tfmAppSet;
        } else if (qgVertex.annotation() != null) {
            return tfmAppSet;
        } else {
            tfmAppSet.add(new TFMApp(qgVertex));
            return tfmAppSet;
        }
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 11;
    }


}
