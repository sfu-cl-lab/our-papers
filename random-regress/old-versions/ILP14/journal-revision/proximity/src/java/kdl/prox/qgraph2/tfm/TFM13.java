/**
 * $Id: TFM13.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFM13.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implemented.

*/

package kdl.prox.qgraph2.tfm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.qgraph2.AbstractQuery;
import kdl.prox.qgraph2.ConsQGVertex;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.Subquery;
import kdl.prox.qgraph2.TempTableMgr;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import org.apache.log4j.Logger;


/**
 * Name: "13. eliminate subquery box"
 * <p/>
 * Group: "vertex only"
 * <p/>
 * Diagram:
 * <p/>
 * <p/>
 * +-----------+
 * |   ,---.   |                 ,---.
 * X   |  / ,-. \  |           X    / ,-. \
 * - - - -|-( (   ) ) |   =>   - - - -( (   ) )
 * |  \ `-' /  |                \ `-' /
 * |   `---'   |                 `---'
 * +-----------+                 [i..j]
 * [i..j]
 * <p/>
 * Applicability: Applies to an unannotated consolidated vertex that is the only
 * vertex in an (annotated subquery.
 * <p/>
 * Behavior: Eliminates the subquery box by moving the vertex to the subquery's
 * parent, and moving the subquery's annotation to the vertex. No asterisks are
 * allowed.
 * <p/>
 * TFMApp usage: Contains one item: {the vertex} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {the consolidated vertex} (see above).
 */
public class TFM13 extends Transformation {

    private static final Logger log = Logger.getLogger(TFM13.class);

    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (query and qgItems)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));	// throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexCopy = (QGVertex) qgItemsCopy.get(0);
        Query queryCopy = qgVertexCopy.parentAQuery().rootQuery();
        // modify the copied Query:
        //	1) move items from qgVertexCopy's parent Subquery up to *its* parent,
        //		removing the Subquery from its parent
        //	2) set qgVertexCopy's annotation to the Subquery's annotation
        Subquery parSubqCp = (Subquery) qgVertexCopy.parentAQuery();
        queryCopy.flattenSubquery(parSubqCp);
        qgVertexCopy.setAnnotation(parSubqCp.annotation());
        // return the copied exec item(s)
        return new TFMExec(qgVertexCopy);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "eliminate subquery box";
    }


    /**
     * Transformation method.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr) throws Exception {
        // an SQL no-op!

        return null;
    }


    /**
     * Transformation method.
     */
    public Set isApplicable(QGVertex qgVertex) {
        HashSet tfmAppSet = new HashSet();		// returned value. filled next if applicable
        if (!(qgVertex instanceof ConsQGVertex)) {
            return tfmAppSet;
        } else if (qgVertex.isAsterisked()) {
            return tfmAppSet;
        } else if (isHasAsteriskedEdges(qgVertex)) {
            return tfmAppSet;
        } else if (qgVertex.annotation() != null) {
            return tfmAppSet;
        }
        // continue: see if qgVertex's containing AbstractQuery has any other
        // vertices
        AbstractQuery parentAQuery = qgVertex.parentAQuery();
        if (!(parentAQuery instanceof Subquery)) {
            return tfmAppSet;
        } else if (parentAQuery.vertices(false).size() != 1) {	// isRecurse
            return tfmAppSet;
        } else {
            // continue: we have a consolidated annotated vertex that is the
            // only vertex in a Subquery. See if there are unresolved constraints
            boolean containsConstraint = false;
            List constraints = parentAQuery.rootQuery().constraints();
            Iterator constIter = constraints.iterator();
            while (constIter.hasNext()) {
                QGConstraint qgConst = (QGConstraint) constIter.next();
                String item1Name = qgConst.item1Name();
                String item2Name = qgConst.item2Name();
                if (qgVertex.names().contains(item1Name) && qgVertex.names().contains(item2Name))
                    containsConstraint = true;
            }
            // Can only remove the sub-query if all its constraints have been dealt with
            if (!containsConstraint) {
                tfmAppSet.add(new TFMApp(qgVertex));
            }
            return tfmAppSet;
        }
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 13;
    }


}
