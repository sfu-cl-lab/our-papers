/**
 * $Id: LayoutStateTransitionFunction.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VertexLocationFunction;

import java.util.List;


/**
 * Supports layout animation via the creation of intermediate VertexLocationFunction
 * instances, given start and end states. The details (linear, radial, etc.)
 * of how the intermediate states are created are up to implementing classes.
 */
public abstract class LayoutStateTransitionFunction {

    /**
     * Creates and returns the intermediate states that animate the layout from
     * startVertLocFcn to endVertLocFcn. Note that it's the implementation's
     * responsibility to ensure that all vertices in <b>either</b> startVertLocFcn
     * <b>or</b> endVertLocFcn appear in all intermediate states. The simplest
     * way to do this is to use locations from endVertLocFcn if there's no entry
     * in startVertLocFcn. Later we may treat added vertices specially, e.g.,
     * by fading them in at the animation's end.
     *
     * @param numIntermediateStates number of states to add between start and end.
     *                              must be >0. (It's up to callers to check.)
     * @return a List of numIntermediateStates intermediate VertexLocationFunction
     *         instances representing animated states between the passed start and
     *         end states. NB: does not include the start and end states
     */
    public abstract List calcIntermediateStates(Graph graph,
                                                VertexLocationFunction startVertLocFcn,
                                                VertexLocationFunction endVertLocFcn,
                                                int numIntermediateStates);

}
