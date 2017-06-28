/**
 * $Id: Transformation.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: Transformation.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.TempTableMgr;
import kdl.prox.qgraph2.util.QGUtil;


/**
 * The abstract superclass for all QGraph 2.0 transformations. A transformation
 * converts a particular class of Queries into a simpler one via (ultimately) the
 * execution of corresponding SQL commands. Works with the TFMApp class.
 * Transformation is abbreviated "TFM". The naming convention for concrete
 * subclasses is to simply name them "TFM<nn>" where nn is the two-digit
 * transformation number as specified by
 * proximity/doc/developer/qgraph2/transform.txt, e.g., TFM01, TFM14, etc.
 */
public abstract class Transformation {

    // no IVs

    // no constructor


    /** todo application cost! xx */


    /**
     * Returns the result of applying this transformation to tfmApp (obtained via
     * isApplicable()). Methods will typically perform the following steps:
     * <p/>
     * 1) Deep copy tfmApp, which copies qgItems and their Query. For example:
     * <p/>
     * TFMApp tfmAppCopy = (TFMApp)(ObjectCloner.deepCopy(tfmApp));	// throws Exception
     * List qgItemsCopy = tfmAppCopy.qgItems();
     * // get my args
     * QGVertex qgVertexCopy = (QGVertex)qgItemsCopy.get(0);
     * Query queryCopy = qgVertexCopy.parentAQuery().rootQuery();
     * <p/>
     * 2) Modify the copied Query, typically by replacing QGVertex and QGEdge
     * instances in qgItemCopy with new ones.
     * <p/>
     * 3) Create and return a TFMExec instance that contains one or more QGItems
     * from the *copied* Query. The item(s) returned in the TFMExec should be
     * the one(s) execTFMExec() needs (along with the original TFMApp) to
     * actually perform the transformation via SQL. This will typically be the
     * new items (e.g., a ConsQGVertex) that replaced the one(s) in tfmApp.
     * <p/>
     * Throws Exception if problems. (The deep copy method throws Exception.)
     */
    public abstract TFMExec applyTFMApp(TFMApp tfmApp) throws Exception;


    /**
     * Utility that returns qgVertex's asterisked edges. The result is empty if
     * there are none.
     */
    Set asteriskedEdges(QGVertex qgVertex) {
        Set asteriskedEdges = new HashSet();	// return value. filled next
        Iterator qgEdgeIter = qgVertex.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            if (qgEdge.isAsterisked())
                asteriskedEdges.add(qgEdge);
        }
        return asteriskedEdges;
    }


    /**
     * Returns my description, as specified in
     * proximity/doc/developer/qgraph2/transform.txt.
     */
    public abstract String description();


    /**
     * Executes the SQL required to perform the transformation specified by tfmExec
     * and tfmApp on proxDB, using tempTableMgr to manage "temp_sgi_N" tables.
     * tfmApp is the object that was passed to applyTFMApp() (i.e., the original
     * input items), and tfmExec is the object returned by applyTFMApp() (i.e.,
     * copied output items). query is the original Query, which some
     * transformations need to enable retrieving asterisk sources. Throws Exception
     * if problems. Regarding using tempTableMgr, typically:
     * <p/>
     * o subclasses that create a ConsQGVertex will call
     * TempTableMgr.createTempSGITable() to create a "temp_sgi_N" table that
     * will store the vertex's contents
     * <p/>
     * o subclasses call TempTableMgr.release() when a ConsQGVertex is merged
     * with another vertex
     * <p/>
     * Returns the name of the vertex under which the temp_sgi_N table was stored.
     * null if the transformation is a NON-SQL one (such as TFM13)
     *
     * @param tfmExec
     * @param tfmApp
     * @param query
     * @param qgUtil
     * @param tempTableMgr
     * @return
     * @throws Exception
     */
    public abstract String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                                       Query query, QGUtil qgUtil,
                                       TempTableMgr tempTableMgr) throws Exception;


    /**
     * Returns the ways in which this Transformation can apply to qgVertex,
     * possibly including other items around it. The returned Set contains TFMApp
     * instances, one for each possible application of this Transformation. For
     * example, some Transformations apply to a pair of vertices. In these cases,
     * the single passed vertex may be transformed in multiple ways, depending on
     * the other vertices connected to it. The Set returned acts as a
     * "isAppplicable()" flag: It is empty if this transformation cannot be
     * applied, and is non-empty (i.e., contains TFMApp instances) otherwise. The
     * contained TFMApp instances are used by the Transformation to 1) apply
     * itself (and generate a new Query), and 2) to actually generate SQL to
     * execute.
     * <p/>
     * NB: Most Transformations will want to check for:
     * <p/>
     * o edges crossing subquery boundaries (see Query.isCrossesSubqBoundsEdge())
     * <p/>
     * o asterisks (see isAsterisked())
     */
    public abstract Set isApplicable(QGVertex qgVertex);


    /**
     * Utility that returns true if qgVertex has any asterisked edges. Returns
     * false o/w.
     */
    boolean isHasAsteriskedEdges(QGVertex qgVertex) {
        Set asteriskedEdges = asteriskedEdges(qgVertex);
        return (asteriskedEdges.size() != 0);
    }

    protected List makeSGIListFromNSTs(NST objectTempSGINST, NST linkTempSGINST) {
        List tempSGINSTs = new ArrayList();
        tempSGINSTs.add(objectTempSGINST);
        tempSGINSTs.add(linkTempSGINST);
        return tempSGINSTs;
    }


    /**
     * Returns my number, as specified in
     * proximity/doc/developer/qgraph2/transform.txt.
     */
    public abstract int number();

}
