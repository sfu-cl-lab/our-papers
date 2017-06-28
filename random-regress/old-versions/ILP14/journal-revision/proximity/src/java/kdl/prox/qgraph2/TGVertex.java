/**
 * $Id: TGVertex.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TGVertex.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import kdl.prox.util.Assert;


/**
 * Represents nodes in a TFMGraph.
 */
public class TGVertex extends TGItem {

    /**
     * The TGEdge instances leaving me.
     */
    private Set edges = new HashSet();

    /**
     * The augmented Query that I represent. Set by constructor.
     */
    private Query query;


    /**
     * Full-arg constructor. Saves args in IVs.
     */
    public TGVertex(Query query) {
        Assert.condition(query != null, "query null");
        this.query = query;
    }


    /**
     * Adds tgEdge to the end of my edges. Throws IllegalArgumentException if
     * tgEdge already in my edges.
     */
    void addEdge(TGEdge tgEdge) {
        boolean isAdded = edges.add(tgEdge);
        Assert.condition(isAdded, "tgEdge already in edges: " + tgEdge);
    }


    /**
     * Returns a copy of my edges.
     */
    public Set edges() {
        return Collections.unmodifiableSet(edges);
    }


    /**
     * Returns my query.
     */
    public Query query() {
        return query;
    }


}
