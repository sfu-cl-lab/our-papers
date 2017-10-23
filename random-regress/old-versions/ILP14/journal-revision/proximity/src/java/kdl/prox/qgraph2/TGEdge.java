/**
 * $Id: TGEdge.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: TGEdge.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import kdl.prox.qgraph2.tfm.TFMApp;
import kdl.prox.qgraph2.tfm.TFMExec;
import kdl.prox.qgraph2.tfm.Transformation;
import kdl.prox.util.Assert;


/**
 * Represents an edge in a TFMGraph.
 */
public class TGEdge extends TGItem {

    /**
     * The "arglist" of my transformation, i.e., the input that it requires in
     * order to apply. Set by constructor.
     */
    private TFMApp tfmApp;

    /**
     * The result of applying my transformation, i.e., the output from the copied
     * query that it requires in order to execute. Set by constructor.
     */
    private TFMExec tfmExec;

    /**
     * The Transformation that I represent. Set by constructor.
     */
    private Transformation transformation;

    /**
     * The vertext that I go to. Set by constructor.
     */
    private TGVertex vertex;


    /**
     * Full-arg constructor. Saves args in IVs.
     */
    public TGEdge(Transformation transformation, TFMApp tfmApp, TFMExec tfmExec,
                  TGVertex vertex) {
        Assert.condition(transformation != null, "transformation null");
        Assert.condition(tfmApp != null, "tfmApp null");
        Assert.condition(tfmExec != null, "tfmExec null");
        Assert.condition(vertex != null, "vertex null");
        // continue
        this.transformation = transformation;
        this.tfmApp = tfmApp;
        this.tfmExec = tfmExec;
        this.vertex = vertex;
    }


    /**
     * Returns my tfmApp.
     */
    public TFMApp tfmApp() {
        return tfmApp;
    }


    /**
     * Returns my tfmExec.
     */
    public TFMExec tfmExec() {
        return tfmExec;
    }


    /**
     * Returns my transformation.
     */
    public Transformation transformation() {
        return transformation;
    }


    /**
     * Returns my vertex.
     */
    public TGVertex vertex() {
        return vertex;
    }


}
