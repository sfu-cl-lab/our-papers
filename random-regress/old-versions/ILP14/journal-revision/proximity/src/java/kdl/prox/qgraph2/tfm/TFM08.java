/**
 * $Id: TFM08.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFM08.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2.tfm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import kdl.prox.qgraph2.ConsQGVertex;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.TempTableMgr;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;

/*
Diagram:

                 W *a                               *a . .  W
.             ,- - - -,                      .        '   '
  .U        ,           ,                      .U    :     v
    .*b  ,-+-.         ,-+-.                     .*a  ,---.
      . / ,-. \  *a   /     \    Z                 . / ,-. \    Z
       ( ( *a) )-----(   B   )- - - -   =>          ( ( *a) )- - - -
      ' \ `-' /   X   \     /                      ' \ `-' /
    '    `-+-'         `-+-'                     '    `---'
  ' V       `           '                      ' V   :     ^
'             `- - - -'                      '        .   .
                  Y                                    - -  Y

*/

/**
 * Name: "8. absorb asterisked edge and adjacent vertex"
 * <p/>
 * Group: "3. unannotated vertex"
 * <p/>
 * Applicability: Applies to a consolidated asterisked vertex ("*a") that is
 * connected by one edge ("X *a") and possibly additional edges ("W *a" and
 * "Y") to a non-consolidated unannotated vertex ("B") with possibly other
 * edges ("Z"). "*a" has possible other edges, both asterisked ("U *b") and
 * unanstersked ("V"). "X *a" can not cross a Subquery boundary. "Y" can not
 * be a self loop. Regarding asterisks:
 * <p/>
 * o all "*a" asterisks must be from the same source ("*a")
 * o "U *b"'s asterisk source can be different or the same as "*a" asterisks
 * o "*a" is asterisked
 * o "B" is not asterisked
 * o "X *a" is asterisked
 * o "*a" can have asterisked ("U *b") and unasterisked ("V") edges not
 * involving "B"
 * o "B" can have asterisked ("W *a") and unasterisked ("Y") edges involving
 * "*a", but all other edges ("Z") are not asterisked
 * <p/>
 * Behavior: "Absorbs" "B" into "*a" via "X *a".
 * <p/>
 * TFMApp usage: Contains three items: {"B", "X *a", "*a"} (see above).
 * <p/>
 * TFMExec usage: Contains one item copy: {"*a"} (see above).
 * <p/>
 * todo clean up docs, add diagrams, replace refs to data examples with diagram refs
 */
public class TFM08 extends Transformation {

    /**
     Class-based log4j category for logging.
     */
//	private static Category cat = Category.getInstance(TFM08.class.getName());


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        // deep copy tfmApp (query and qgItems)
        TFMApp tfmAppCopy = (TFMApp) (ObjectCloner.deepCopy(tfmApp));	// throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexBCopy = (QGVertex) qgItemsCopy.get(0);
        QGEdge qgEdgeXStarACopy = (QGEdge) qgItemsCopy.get(1);
        QGVertex qgVertexStarACopy = (QGVertex) qgItemsCopy.get(2);
        Query queryCopy = qgVertexBCopy.parentAQuery().rootQuery();
        // modify the copied Query
        queryCopy.absorbEdgeAndVertex(qgEdgeXStarACopy, qgVertexBCopy);
        qgVertexStarACopy.addNames(qgEdgeXStarACopy);
        qgVertexStarACopy.addNames(qgVertexBCopy);
        // return the copied exec item(s)
        return new TFMExec(qgVertexStarACopy);
    }


    /**
     * Transformation method.
     */
    public String description() {
        return "absorb asterisked edge and adjacent vertex";
    }


    /**
     * Transformation method.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr) throws Exception {
        throw new UnsupportedOperationException();  /** todo fix! xx */
    }


    /**
     * Transformation method. qgVertexB is the "B" vertex candidate. (We don't test
     * "A" candidates).
     */
    public Set isApplicable(QGVertex qgVertexB) {
        HashSet tfmAppSet = new HashSet();		// returned value. filled next if applicable
        if (qgVertexB instanceof ConsQGVertex) {
            return tfmAppSet;
//		} else if(qgVertexB.isAsterisked()) {			// non-ConsQGVertex are never asterisked
//			return tfmAppSet;
        } else if (qgVertexB.annotation() != null) {
            return tfmAppSet;
        }
        // continue: find all "X *a" "*a" pairs
        Iterator qgEdgeIter = qgVertexB.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdgeXStarA = (QGEdge) qgEdgeIter.next();
            QGVertex qgVertexStarA = qgEdgeXStarA.otherVertex(qgVertexB);
            Query query = qgVertexB.parentAQuery().rootQuery();
            if (qgEdgeXStarA.isSelfLoop()) {
                continue;
            } else if (!qgEdgeXStarA.isAsterisked()) {
                continue;
            } else if (!(qgVertexStarA instanceof ConsQGVertex)) {
                continue;
            } else if (!qgVertexStarA.isAsterisked()) {
                continue;
            } else if (!qgEdgeXStarA.isSameAsteriskSource(qgVertexStarA)) {
                continue;
            } else if (qgEdgeXStarA.isCrossesSubqBoundsEdge()) {
                continue;
            }
            // continue: we have a "B" "X *a" "*a" candidate
            if (isValidAsteriskEdges(qgVertexB,
                    (ConsQGVertex) qgVertexStarA))
                tfmAppSet.add(new TFMApp(qgVertexB, qgEdgeXStarA, qgVertexStarA));
        }
        // done
        return tfmAppSet;
    }


    /**
     * Called by isApplicable(), returns true if the passed args are a valid
     * combination (candidate) for this transormation. Returns false o/w. Assumes
     * qgEdgeXStarA and qgVertexStarA are asterisked with the same asterisk
     * source. Checks asterisks on these edges: "U *b", "V", "W *a", "Y", and "Z".
     */
    private boolean isValidAsteriskEdges(QGVertex qgVertexB,
                                         ConsQGVertex qgVertexStarA) {
        // see if qgVertexStarA has any asterisked edges with difference sources
        // (checks "W *a" and "Y").
        Iterator qgEdgeIter = qgVertexStarA.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            QGVertex otherQGVertex = qgEdge.otherVertex(qgVertexStarA);
            if ((otherQGVertex == qgVertexB) && qgEdge.isAsterisked() &&
                    !qgEdge.isSameAsteriskSource(qgVertexStarA))
                return false;	// found a bad "U", or "W"
        }
        // see if qgVertexB has any asterisked edges to other vertices ("Z")
        qgEdgeIter = qgVertexB.edges().iterator();
        while (qgEdgeIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgEdgeIter.next();
            if ((qgEdge.otherVertex(qgVertexB) != qgVertexStarA) &&
                    qgEdge.isAsterisked())
                return false;	// found an asterisked "Z"
        }
        // done
        return true;
    }


    /**
     * Transformation method.
     */
    public int number() {
        return 8;
    }


}
