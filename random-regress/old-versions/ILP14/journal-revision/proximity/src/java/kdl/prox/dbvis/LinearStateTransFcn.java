/**
 * $Id: LinearStateTransFcn.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.DefaultSettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.SettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.VertexLocationFunction;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.*;


/**
 * A simple implementation of LayoutStateTransitionFunction that does linear
 * interpolation between the start and end states. In other words, for each vertex
 * it draws a line between its start and end X,Y locations, then divides it into
 * equal numIntermediateStates segments.
 */
public class LinearStateTransFcn extends LayoutStateTransitionFunction {

    private static final Logger log = Logger.getLogger(LinearStateTransFcn.class);


    public List calcIntermediateStates(Graph graph,
                                       VertexLocationFunction startVertLocFcn,
                                       VertexLocationFunction endVertLocFcn,
                                       int numIntermediateStates) {
        ArrayList vertLocFcnList = new ArrayList();

        // create numIntermediateStates empty states
        for (int stateIdx = 0; stateIdx < numIntermediateStates; stateIdx++) {
            vertLocFcnList.add(new DefaultSettableVertexLocationFunction());
        }

        // save delta x and y increments for each Vertex in vertToIncrCoordMap.
        // NB: we use Vertices from graph, which may appear in endVertLocFcn but
        // not startVertLocFcn (e.g., if they were added)
        Map vertToIncrCoordMap = new HashMap(); // maps Vertex instances to Coordinates instances representing the delta x and y to add to each vertex at each intermediate step
        for (Iterator vertIter = graph.getVertices().iterator(); vertIter.hasNext();) {
            Vertex vertex = (Vertex) vertIter.next();
            Point2D startLoc = LinearStateTransFcn.getStartLocation(vertex, startVertLocFcn, endVertLocFcn);
            Point2D endLoc = endVertLocFcn.getLocation(vertex);
            Assert.notNull(endLoc, "endLoc null");

            double deltaX = (endLoc.getX() - startLoc.getX()) / (numIntermediateStates + 1);
            double deltaY = (endLoc.getY() - startLoc.getY()) / (numIntermediateStates + 1);
            vertToIncrCoordMap.put(vertex, new Coordinates(deltaX, deltaY));
        }

        // iterate over intermediate states, calculating new Coordinates for
        // each Vertex, and saving in current state
        for (int stateIdx = 0; stateIdx < vertLocFcnList.size(); stateIdx++) {
            SettableVertexLocationFunction vertLocFcn = (SettableVertexLocationFunction) vertLocFcnList.get(stateIdx);
            for (Iterator vertIter = graph.getVertices().iterator(); vertIter.hasNext();) {
                Vertex vertex = (Vertex) vertIter.next();
                Point2D startLoc = LinearStateTransFcn.getStartLocation(vertex, startVertLocFcn, endVertLocFcn);
                Coordinates deltaCoords = (Coordinates) vertToIncrCoordMap.get(vertex);
                vertLocFcn.setLocation(vertex,
                        new Coordinates(startLoc.getX() + ((stateIdx + 1) * deltaCoords.getX()),
                                startLoc.getY() + ((stateIdx + 1) * deltaCoords.getY())));
            }
        }

        return vertLocFcnList;
    }

    /**
     * @param vertex
     * @param startVertLocFcn
     * @param endVertLocFcn
     * @return location for vertex. uses startVertLocFcn first, but if null uses
     *         endVertLocFcn. NB: does not check if endVertLocFcn is null
     */
    public static Point2D getStartLocation(Vertex vertex,
                                           VertexLocationFunction startVertLocFcn,
                                           VertexLocationFunction endVertLocFcn) {
        Point2D startLoc = startVertLocFcn.getLocation(vertex);
        if (startLoc == null) {
            startLoc = endVertLocFcn.getLocation(vertex);
        }
        return startLoc;
    }

}
