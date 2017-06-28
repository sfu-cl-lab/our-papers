/**
 * $Id: TFMGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TFMGraph.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import kdl.prox.util.Assert;


/**
 * Represents a transformation graph, i.e., the result of applying all applicable
 * Transformations to an initial Query until a final node is converged on. It is
 * directed from the starting TGVertex through TGEdges to the final TGVertex.
 * The starting node is the initial Query read from XML. Each edge represents the
 * application of a particular Transformation and the resulting augmented query
 * (in the node pointed to by the edge). During the generation of the TFMGraph
 * identical nodes are merged. Transformation is abbreviated "TFM", and TFMGraph
 * is abbreviated "TG".
 */
public class TFMGraph {

    /**
     * The starting vertex. Set by constructor.
     */
    private TGVertex startVertex;


    /**
     * Full-arg constructor. Saves args in IVs.
     */
    public TFMGraph(TGVertex startVertex) {
        Assert.condition(startVertex != null, "null startVertex");
        // continue
        this.startVertex = startVertex;
    }


    /**
     * Returns true if ancTGVertex (ancestor) is an ancestor of desTGV
     * (descendant). Returns false o/w. Used to test for cycles.
     */
    boolean isAncestorTGVertex(TGVertex ancTGVertex, TGVertex desTGVertex) {
        Set descendantTGVs = tgVertices(ancTGVertex);
        return descendantTGVs.contains(desTGVertex);
    }


    /**
     * Returns my startVertex.
     */
    public TGVertex startVertex() {
        return startVertex;
    }


    /**
     * Returns a Set of all of my tgEdge instances.
     */
    public Set tgEdges() {
        Set tgEdges = new HashSet();		// return value. filled next
        // get all TGVertex instances and merge all their edges
        Set tgVertices = tgVertices();
        Iterator tgVertexIter = tgVertices.iterator();
        while (tgVertexIter.hasNext()) {
            TGVertex tgVertex = (TGVertex) tgVertexIter.next();
            Iterator tgEdgeIter = tgVertex.edges().iterator();
            while (tgEdgeIter.hasNext()) {
                TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
                tgEdges.add(tgEdge);
            }
        }
        return tgEdges;
    }


    /**
     * Returns the first TGVertex whose query equals query. Returns null if none
     * found.
     */
    TGVertex tgVertexEqualToQuery(Query query) {
        Set tgVertices = tgVertices();
        Iterator tgVertexIter = tgVertices.iterator();
        while (tgVertexIter.hasNext()) {
            TGVertex tgVertex = (TGVertex) tgVertexIter.next();
            if (tgVertex.query().equals(query))
                return tgVertex;
        }
        return null;	// none found
    }


    /**
     * Returns a Set of all of my TGVertex instances.
     */
    public Set tgVertices() {
        return tgVertices(startVertex);
    }


    /**
     * Internal method called by tgVertices(), returns a Set of all of tgVertex's
     * TGVertexs, including tgVertex itself.
     */
    private Set tgVertices(TGVertex tgVertex) {
        Set tgVertices = new HashSet();		// return value. filled next
        // recurse on children through edges
        Iterator tgEdgeIter = tgVertex.edges().iterator();
        while (tgEdgeIter.hasNext()) {
            TGEdge tgEdge = (TGEdge) tgEdgeIter.next();
            TGVertex childTGVertex = tgEdge.vertex();
            tgVertices.addAll(tgVertices(childTGVertex));
        }
        // add tgVertex itself and return
        tgVertices.add(tgVertex);
        return tgVertices;
    }


}
