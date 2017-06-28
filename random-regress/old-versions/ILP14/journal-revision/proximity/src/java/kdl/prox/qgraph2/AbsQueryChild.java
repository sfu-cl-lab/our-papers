/**
 * $Id: AbsQueryChild.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: AbsQueryChild.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;


/**
 * The interface that is implemented by classes that can be containted by an
 * AbstractQuery: QGItem (QGVertex and QGEdge), QGConstraint, and Subquery.
 */
public interface AbsQueryChild {

    /**
     * @return my Annotation. returns null if none
     */
    public Annotation annotation();

    public void deleteAnnotation();

    /**
     * @return the AbstractQuery that is my container (or parent).
     */
    public AbstractQuery parentAQuery();

    public void setAnnotation(Annotation ele);

    /**
     * Sets my container (or parent) to parentAQuery. One typically passes null
     * to clear the setting.
     */
    public void setParentAQuery(AbstractQuery parentAQuery);

}
