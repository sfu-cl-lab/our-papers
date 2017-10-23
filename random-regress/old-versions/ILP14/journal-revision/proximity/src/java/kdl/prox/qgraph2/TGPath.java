/**
 * $Id: TGPath.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TGPath.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import kdl.prox.util.Assert;


/**
 * Represents a particular path through a transformation graph, i.e., an ordered
 * list of Transformations from the initial Query to a final result. This class
 * is designed to hold intermediate as well as final paths. Thus, my xx
 */
public class TGPath {

    /**
     * The TGEdge instances making up my path. The first is the left-most edge and
     * the last is the right-most. In other words, the first edge is from the start
     * TGVertex and the last goes to a TGVertex with a fully-consolidated (i.e.,
     * solved) Query. Managed by pushEdge().
     */
    private List edges = new ArrayList();

    /**
     * The starting vertex. null if not set, i.e., if this path is an incomplete
     * (intermediate) one. Set by setStartVertex().
     */
    private TGVertex startVertex = null;


    /**
     * One-arg constructor. Saves startVertex and calls pushEdge() on tgEdge.
     */
    public TGPath(TGVertex startVertex, TGEdge tgEdge) {
        Assert.condition(startVertex != null, "startVertex null");
        // continue
        this.startVertex = startVertex;
        pushEdge(tgEdge);
    }


    /**
     * Returns a copy of my edges.
     */
    public List edges() {
        return Collections.unmodifiableList(edges);
    }


    /**
     * Returns the last TGVertex in me. Returns null if I have no edges.
     */
    public TGVertex endVertex() {
        if (edges.isEmpty()) {
            return null;
        } else {
            TGEdge lastTGEdge = (TGEdge) edges.get(edges.size() - 1);
            return lastTGEdge.vertex();
        }
    }


    /**
     * Returns the number of edges in my edges.
     *
     * @return
     */
    public int numEdges() {
        return edges.size();
    }


    /**
     * Adds tgEdge to the *start* of my edges.
     */
    void pushEdge(TGEdge tgEdge) {
        Assert.condition(tgEdge != null, "tgEdge null");
        // continue
        edges.add(0, tgEdge);
    }


    /**
     Sets my startVertex to startVertex.

     void setStartVertex(TGVertex startVertex) {
     Assert.condition(startVertex != null, "startVertex null");
     // continue: parse the xml file with validation, then run
     this.startVertex = startVertex;
     }*/


    /**
     * List the number and names of the transformations involved in the path
     */
    public String toString() {
        String pathName = "";
        Iterator tgEdgeIter = edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            pathName = pathName + ":" + tgEdge.transformation().number() + "/" + tgEdge.tfmApp().argString(true);
        }

        return (pathName.length() == 0 ? pathName : pathName.substring(1));
    }


    /**
     * Returns my startVertex.
     */
    public TGVertex startVertex() {
        return startVertex;
    }


}
