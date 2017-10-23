/**
 * $Id: TFM01.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFM01.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implemented.

*/

package kdl.prox.qgraph2.tfm;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.ConsQGVertex;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.TempTableMgr;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
Name: "1. get vertex"

Group: "vertex only"

Diagram:

        ,---.                   ,---.
  X    /     \            X    / ,-. \
- - - -(       )   =>   - - - -( (   ) )
       \     /                 \ `-' /
        `---'                   `---'

Applicability: Applies to any single non-consolidated vertex without an
   annotation. No asterisks are allowed.

Behavior: Replaces the vertex with a consolidated one, bringing it into
   SQL memory.

TFMApp usage: Contains one item: {the vertex} (see above).

TFMExec usage: Contains one item copy: {the new consolidated vertex} (see above).

*/

public class TFM01 extends Transformation {

    private static Logger log = Logger.getLogger(TFM01.class);


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (qgItems and their Query)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexCopy = (QGVertex) qgItemsCopy.get(0);
        Query queryCopy = qgVertexCopy.parentAQuery().rootQuery();
        // modify the copied Query
        ConsQGVertex newConsQGVertex = new ConsQGVertex(qgItemsCopy);
        queryCopy.replaceQGVertex(qgVertexCopy, newConsQGVertex);
        // return the copied exec item(s)
        return new TFMExec(newConsQGVertex);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "get vertex";
    }


    /**
     * Transformation method. Works by creating a "temp_sgi_N" table for my vertex,
     * creating and filling a standard temporary boolean table, copying it to the
     * "temp_sgi_N" table, then dropping the boolean table. Throws Exception if
     * problems.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr) throws Exception {
        QGVertex inQGVertex = (QGVertex) tfmApp.qgItems().get(0);
        ConsQGVertex outConsQGVertex = (ConsQGVertex) tfmExec.qgItems().get(0);
        NST tempSGINST = execTFMExecInternal(qgUtil, inQGVertex);
        tempTableMgr.putNSTForVertex(outConsQGVertex.catenatedName(),
                tempSGINST, null);      // save objects; have no links

        return outConsQGVertex.catenatedName();
    }


    protected NST execTFMExecInternal(QGUtil qgUtil, QGVertex inQGVertex) {

        NST match = qgUtil.getMatchingObjects(inQGVertex.condEleChild());

        match.renameColumn("o_id", "item_id");
        match.addNumberColumn("subg_id");
        match.addConstantColumn("name", "str", inQGVertex.catenatedName());

        return match;
    }


    /**
     * Transformation method.
     */
    public Set isApplicable(QGVertex qgVertex) {
        HashSet tfmAppSet = new HashSet();        // returned value. filled next if applicable
        if (qgVertex instanceof ConsQGVertex) {
            return tfmAppSet;
//		} else if(qgVertex.isAsterisked()) {		// non-ConsQGVertex are never asterisked
//			return tfmAppSet;
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
        return 1;
    }


}
