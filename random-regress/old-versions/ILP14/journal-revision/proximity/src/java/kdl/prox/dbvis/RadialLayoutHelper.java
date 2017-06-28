/**
 * $Id: RadialLayoutHelper.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.ConnectedGraphPredicate;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.DefaultSettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.VertexLocationFunction;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * Static class that helps compute radial layout polar coordinates for Vertices,
 * using the algorithm from this paper: <i>Eades, P., "Drawing Free Trees,"
 * Bulletin of the Institute of Combinatorics and its Applications, pp. 10-36,
 * 1992.</i>
 * <p/>
 * Usage: 1) Call computePolarCoordinates(), passing it a startVertex and optional
 * sort key (for unit tests), then pass result to xx() makeVertexLocationFunction()
 * to calculate corresponding Vertex x,y locations based on a canvas size.
 */
public class RadialLayoutHelper {

    private static final Logger log = Logger.getLogger(RadialLayoutHelper.class);


    /**
     * Calculates the polar coordinates (rho and theta) for the radial layout of
     * every Vertex in graph, based on Eades' algorithm (see class docs for ref).
     *
     * @param centerVertex   center of the free tree
     * @param vertComparator used by tests to make ordering deterministic. see GraphAsTree
     * @return Map map of Vertex -> PolarCoordinate pairs that specifies the
     *         coordinate of each Vertex found from centerVertex
     * @throws IllegalArgumentException if centerVertex's graph is not connected
     * @see PolarCoordinate
     */
    public static Map computePolarCoordinates(Vertex centerVertex, Comparator vertComparator) {
        ArchetypeGraph graph = centerVertex.getGraph();
        boolean isConnected = ConnectedGraphPredicate.getInstance().evaluateGraph(graph);
        Assert.condition(isConnected, "graph not connected: " + graph);

        Map vertexToPolarCoordMap = new HashMap();
        GraphAsTree graphAsTree = new GraphAsTree(centerVertex, vertComparator);
        int width = graphAsTree.getWidth(centerVertex);
        computePolarCoordsSubTree(graphAsTree, centerVertex, vertexToPolarCoordMap,
                true, width, 0, 0, 2.0 * Math.PI);
        return vertexToPolarCoordMap;
    }

    /**
     * Called by computeCoords() and recursively, does Eades' DrawSubTree1
     * algorithm.
     *
     * @param graphAsTree
     * @param vertex
     * @param vertexToPolarCoordMap where results are saved
     * @param rho
     * @param alpha1
     * @param alpha2
     */
    private static void computePolarCoordsSubTree(GraphAsTree graphAsTree, Vertex vertex,
                                                  Map vertexToPolarCoordMap, boolean isRoot, int width, int rho,
                                                  double alpha1, double alpha2) {
        int delta = 1;
        double tau = (isRoot ? 2 * Math.PI : 2 * Math.acos((double) rho /
                ((double) rho + (double) delta)));   // handle special case of root vertex, which gets the entire circle (0 -> 2 PI)
        double s;
        double alpha;
        double theta = (alpha1 + alpha2) / 2;
//        log.warn("cCST(): " + vertex + "@, (" + rho + ", " + (theta / Math.PI) +
//                " PI), " + width + ", <" + (alpha1 / Math.PI) + " PI, " +
//                (alpha2 / Math.PI) + " PI>, " + (tau / Math.PI) + " PI");
        setPolarCoords(vertex, vertexToPolarCoordMap, rho, theta);
//        setAnnulusWedge(vertex, alpha1, alpha2);
        if (tau < (alpha2 - alpha1)) {
            s = tau / width;
            alpha = (alpha1 + alpha2 - tau) / 2;
        } else {
            s = (alpha2 - alpha1) / width;
            alpha = alpha1;
        }
        List successors = graphAsTree.getSuccessors(vertex);
        for (Iterator succIter = successors.iterator(); succIter.hasNext();) {
            Vertex childVertex = (Vertex) succIter.next();
            int childWidth = graphAsTree.getWidth(childVertex);
            computePolarCoordsSubTree(graphAsTree, childVertex, vertexToPolarCoordMap, false, childWidth,
                    rho + delta, alpha, alpha + (s * (double) childWidth));
            alpha += (s * (double) childWidth);
        }
    }

    /**
     * Converts polar coordinates stored in vertexToPolarCoordMap to rectangular.
     *
     * @param graph
     * @param vertexToPolarCoordMap as returned by computePolarCoordinates()
     * @param size                  canvas size for conversion
     * @param vertexDiameter
     * @return a VertexLocationFunction containing rectangular coordinates
     *         corresponding to vertexToPolarCoordMap
     * @throws IllegalArgumentException if any Vertext in graph does not have an
     *                                  entry in vertexToPolarCoordMap
     */
    public static VertexLocationFunction convertPolarToRectangular(Graph graph,
                                                                   Map vertexToPolarCoordMap,
                                                                   Dimension size,
                                                                   int vertexDiameter) {
        DefaultSettableVertexLocationFunction vertLocFcn = new DefaultSettableVertexLocationFunction();

        // retrieve previously calculated polar coordinates for each Vertex,
        // compute corresponding retangular coordinates based on my size, and
        // set Vertex locations
        double height = getHeight(size, vertexDiameter);
        double width = getWidth(size, vertexDiameter);
        int maxRho = RadialLayoutHelper.getMaxRho(vertexToPolarCoordMap);
        Vertex[] vertices = (Vertex[]) graph.getVertices().toArray(new Vertex[0]);
        for (int i = 0; i < vertices.length; i++) {
            Vertex vertex = vertices[i];
            PolarCoordinate polarCoord = RadialLayoutHelper.getPolarCoordinate(vertex, vertexToPolarCoordMap);
            Assert.notNull(polarCoord, "polarCoord was null: " + vertex);   // happens, for example, when graph is not connected
            double rectX = PolarCoordinate.calcX(polarCoord.getRho(), polarCoord.getTheta(), width, height, maxRho);
            double rectY = PolarCoordinate.calcY(polarCoord.getRho(), polarCoord.getTheta(), width, height, maxRho);
            vertLocFcn.setLocation(vertex, new Coordinates(rectX + (vertexDiameter / 2),
                    rectY + (vertexDiameter / 2)));
        }

        return vertLocFcn;
    }

    public static int getCenterX(Dimension size, int vertexDiameter) {
        return (int) (getWidth(size, vertexDiameter) / 2) + (vertexDiameter / 2);
    }

    public static int getCenterY(Dimension size, int vertexDiameter) {
        return (int) (getHeight(size, vertexDiameter) / 2) + (vertexDiameter / 2);
    }

    private static double getHeight(Dimension size, int vertexDiameter) {
        double height = size.getHeight();
        return height - vertexDiameter;
    }

    /**
     * @return the max Rho of all the polar coordinates for all the vertices in
     *         the graph. returns -1 if no polar coordinates found
     * @throws IllegalArgumentException if any Vertext in graph does not have an
     *                                  entry in vertexToPolarCoordMap
     */
    public static int getMaxRho(Map vertexToPolarCoordMap) {
        int maxRho = -1;
        Set vertices = vertexToPolarCoordMap.keySet();
        for (Iterator vertIter = vertices.iterator(); vertIter.hasNext();) {
            Vertex vertex = (Vertex) vertIter.next();
            PolarCoordinate polarCoord = RadialLayoutHelper.getPolarCoordinate(vertex,
                    vertexToPolarCoordMap);
            Assert.notNull(polarCoord, "polarCoord was null: " + vertex);
            maxRho = Math.max(polarCoord.getRho(), maxRho);
        }
        return maxRho;
    }

    /**
     * @param vertex
     * @param vertexToPolarCoordMap as returned by computePolarCoordinates()
     * @return the radius (rho) and angle (theta) associated with vertex.
     *         returns null if computePolarCoordinates() has not been called
     */
    public static PolarCoordinate getPolarCoordinate(Vertex vertex, Map vertexToPolarCoordMap) {
        return (PolarCoordinate) vertexToPolarCoordMap.get(vertex);
    }

    public static double getRadius(Dimension size, int vertexDiameter, Map vertexToPolarCoordMap) {
        double height = getHeight(size, vertexDiameter);
        double width = getWidth(size, vertexDiameter);
        int maxRho = RadialLayoutHelper.getMaxRho(vertexToPolarCoordMap);
        return PolarCoordinate.getRadius(height, width, maxRho);
    }

    private static double getWidth(Dimension size, int vertexDiameter) {
        double width = size.getWidth();
        return width - vertexDiameter;
    }

    public static void printVertLocFcn(String message, VertexLocationFunction vertLocFcn) {
        log.warn(message + ": " + vertLocFcn + ":");
        Iterator vertexIterator = vertLocFcn.getVertexIterator();
        if (!vertexIterator.hasNext()) {
            log.warn("  no entries");
        }
        while (vertexIterator.hasNext()) {
            Vertex vertex = (Vertex) vertexIterator.next();
            log.warn("  " + vertex + " -> " + vertLocFcn.getLocation(vertex));
        }
    }

    private static void setPolarCoords(Vertex vertex, Map vertexToPolarCoordMap,
                                       int rho, double theta) {
        vertexToPolarCoordMap.put(vertex, new PolarCoordinate(rho, theta));
    }

}
