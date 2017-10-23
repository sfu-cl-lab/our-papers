/**
 * $Id: GraphUtils.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.util;

import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.Exponential;
import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.monet.ResultSet;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.graph.Node;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Misc utilities on a graph
 * 
 */
public class GraphUtils {
    private static final Logger log = Logger.getLogger(GraphUtils.class);

    /**
     * Assigns a uniformly distributed value (weight) to each link
     * pdf: f(x) = {0;  for x < a, x>=b
     * {1/(b-a) for a<=x<b
     * mean: (b+a)/2
     * variance: (b-a)^2/12
     *
     * @param a min of distribution range
     * @param b max of distribution range
     */
    public static void assignLinkWeightsUniform(double a, double b) {

        final Random rand = new Random();
        RandomEngine randEng = new RandomEngine() {
            public int nextInt() {
                return rand.nextInt();
            }
        };
        Uniform uniformDistr = new Uniform(a, b, randEng);

        Object[][] data = new Object[DB.getLinkNST().getRowCount()][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{uniformDistr.nextDouble()};
        }

        addWeightAttr(data);
    }

    /**
     * Assigns an exponentially distributed value (weight) to each link
     * pdf: f(x) = ae^(-ax)
     * mean: 1/a
     * variance: 1/a^2
     *
     * @param lambda exponential paramater
     */
    public static void assignLinkWeightsExponential(double lambda) {
        final Random rand = new Random();
        RandomEngine randEng = new RandomEngine() {
            public int nextInt() {
                return rand.nextInt();
            }
        };
        Exponential expDistr = new Exponential(lambda, randEng);

        Object[][] data = new Object[DB.getLinkNST().getRowCount()][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{expDistr.nextDouble()};
        }

        addWeightAttr(data);
    }

    /**
     * Assigns a beta distributed value (weight) to each link
     * pdf: f(x) = k * x^(alpha-1) * (1-x)^(beta-1)
     * with k = g(alpha+beta)/(g(alpha)*g(beta)) and g(a) being the gamma function.
     * mean: alpha/(alpha + beta)
     * variance: (alpha*beta)/[(alpha + beta)^2 * (alpha + beta + 1)]
     *
     * @param alpha paramater
     * @param beta  paramater
     */
    public static void assignLinkWeightsBeta(double alpha, double beta) {
        final Random rand = new Random();
        RandomEngine randEng = new RandomEngine() {
            public int nextInt() {
                return rand.nextInt();
            }
        };
        Beta betaDistr = new Beta(alpha, beta, randEng);

        Object[][] data = new Object[DB.getLinkNST().getRowCount()][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{betaDistr.nextDouble()};
        }

        addWeightAttr(data);
    }

    /**
     * Assigns a binomial distributed value (weight) to each link
     * pmf: f(x) = (n choose x) * p^x *(1-p)^(n-x)
     * mean: np
     * variance: np * (1-p)
     *
     * @param n paramater
     * @param p paramater
     */
    public static void assignLinkWeightsBinomial(int n, double p) {
        final Random rand = new Random();
        RandomEngine randEng = new RandomEngine() {
            public int nextInt() {
                return rand.nextInt();
            }
        };
        Binomial binomialDistr = new Binomial(n, p, randEng);

        Object[][] data = new Object[DB.getLinkNST().getRowCount()][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{binomialDistr.nextDouble()};
        }

        addWeightAttr(data);
    }

    private static void addWeightAttr(Object[][] data) {
        NST linkNST = DB.getLinkNST().project("link_id");
        NST weightNST = new NST("weight", "dbl");
        weightNST.fastInsert(data);
        NST attrDataNST = new NST(new String[]{linkNST.getNSTColumn("link_id").getBATName(), weightNST.getNSTColumn("weight").getBATName()},
                new String[]{"id", "value"}, new String[]{"oid", "dbl"});

        DB.getLinkAttrs().defineAttributeWithData("weight", "dbl", attrDataNST);
        attrDataNST.release();
        weightNST.release();
        linkNST.release();
    }

    /**
     * Overload that uses all objects
     *
     * @param num number of nodes to select
     * @return a list of random node ids
     */
    public static List<Integer> chooseRandomNodes(int num) {
        return chooseRandomNodes(num, DB.getObjectNST());
    }

    /**
     * Choose a random set of nodes without replacement
     *
     * @param num           number of nodes to select
     * @param eligibleNodes subset of nodes to choose from
     * @return a list of random node ids
     */
    public static List<Integer> chooseRandomNodes(int num, NST eligibleNodes) {
        DB.beginScope();
        List<Integer> randNodes = eligibleNodes.selectRows("id RANDOM " + num, "id").toOIDList(1);
        DB.endScope();
        return randNodes;
    }

    /**
     * Choose random pairs of nodes.
     * Samples with replacement, but ensures source and target aren't the same
     *
     * @param num number of pairs to select
     * @return a list of random pairs of node ids
     */
    public static List<int[]> chooseRandomNodePairs(int num) {
        List<Integer> ids = DB.getObjectNST().selectRows().toOIDList("id");
        return chooseRandomNodePairs(num, ids);
    }

    public static List<int[]> chooseRandomNodePairs(int num, List<Integer> ids) {
        Random rand = new Random();

        int n = ids.size();
        List<int[]> pairs = new ArrayList<int[]>();
        for (int i = 0; i < num; i++) {
            int source = rand.nextInt(n);
            int target;
            do {
                target = rand.nextInt(n);
            } while (source == target);
            int src = ids.get(source);
            int tar = ids.get(target);

            pairs.add(new int[]{src, tar});
        }
        return pairs;
    }

    /**
     * Floods the graph from a single source node and returns a map
     * of distances (number of hops) for each node to the source
     *
     * @param source node to seed flood
     * @param max    maximum number of hops to flood out
     * @param graph  view of graph
     * @return map of node ids to distance from seed
     */
    public static Map<Integer, Integer> flood(Integer source, int max, Graph graph) {
        int numNodes = DB.getObjectNST().getRowCount();

        int currDist = 0;
        List<Integer> frontier = new ArrayList<Integer>();
        frontier.add(source);

        Map<Integer, Integer> distanceMap = new HashMap<Integer, Integer>();

        while (distanceMap.size() < numNodes) {

            for (Integer node : frontier) {
                distanceMap.put(node, currDist);
            }

            List<Integer> neighbors = new ArrayList<Integer>(ConversionUtils.nodesToIntegers(graph.getNeighbors(frontier)));
            neighbors.removeAll(distanceMap.keySet());

            if (neighbors.size() == 0) {
                break;
            }

            frontier = neighbors;

            currDist++;
            if (currDist > max) {
                break;
            }
        }
        return distanceMap;
    }

    /**
     * Dijkstra's algorithm
     * Floods the graph from a single source node and returns a map
     * of distances (shortest weighted path) for each node to the source
     *
     * @param source node to seed flood
     * @param graph  view of weighted graph
     * @return map of node ids to distance from seed
     */
    public static Map<Integer, Double> floodWeighted(Integer source, Graph graph) {
        Map<Integer, Double> exactDistances = new HashMap<Integer, Double>();
        Map<Integer, Double> estimatedDistancesMap = new HashMap<Integer, Double>();
        PriorityQueue<Node> estimatedDistances = new PriorityQueue<Node>(DB.getObjectNST().getRowCount(),
                new NodeComparator<Node>());

        //assign all nodes (except source) an estimated distance of infinity
        for (Integer node : DB.getObjectNST().selectRows().toOIDList("id")) {
            if (node.equals(source)) {
                estimatedDistances.offer(new Node(node, 0.0));
                estimatedDistancesMap.put(node, 0.0);
            } else {
                estimatedDistances.offer(new Node(node, Double.MAX_VALUE));
                estimatedDistancesMap.put(node, Double.MAX_VALUE);
            }
        }

        while (estimatedDistancesMap.size() > 0) {
            //get the node with lowest estimated distance and mark as exact
            Node closest = estimatedDistances.poll();
            if (!estimatedDistancesMap.containsKey(closest.id)) {
                continue;
            }
            estimatedDistancesMap.remove(closest.id);
            exactDistances.put(closest.id, closest.weight);

            //get all neighbors, and set distance to edge weight plus distance of current node
            //if lower than current estimated distance, update
            Set<Node> neighbors = graph.getNeighbors(closest.id);
            for (Node neighbor : neighbors) {
                Node neighborClone = new Node(neighbor);
                neighborClone.weight += closest.weight;

                //neighbor is still estimated AND has larger estimated weight, update with new distance
                if (estimatedDistancesMap.containsKey(neighborClone.id) &&
                        (estimatedDistancesMap.get(neighborClone.id) > neighborClone.weight)) {
                    estimatedDistancesMap.put(neighborClone.id, neighborClone.weight);
                    estimatedDistances.offer(neighborClone);
                }
            }
        }
        return exactDistances;
    }

    private static class NodeComparator<T> implements Comparator<T> {

        public int compare(Object wn1, Object wn2) {
            //compare by weights
            return Double.compare(((Node) wn1).weight, ((Node) wn2).weight);
        }

    }

    public static Map<Integer, List<Integer>> getComponentMap() {
        return getComponentMap(DB.getObjectNST(), DB.getLinkNST());
    }

    public static Map<Integer, List<Integer>> getComponentMap(NST objectNST, NST linkNST) {
        List<Integer[]> edges = new ArrayList<Integer[]>();
        ResultSet resultSet = linkNST.selectRows("o1_id,o2_id");
        while (resultSet.next()) {
            edges.add(new Integer[]{resultSet.getOID(1), resultSet.getOID(2)});
        }
        log.debug("got " + edges.size() + " edges");

        return getComponentMap(objectNST.selectRows().toOIDList("id"), edges);
    }

    /**
     * Compute the connected components of the graph given the nodes and edges. Direction of edges is ignored.
     * @param nodes List of nodes to be considered
     * @param edges List of edges (2 element Integer array of o1_id, o2_id) to be considered
     * @return A map consisting of lists of nodes corresponding to connected components. The key is an arbirtrary
     * member of the given component.
     */
    public static Map<Integer, List<Integer>> getComponentMap(List<Integer> nodes, List<Integer[]> edges) {
        Map<Integer, Integer> nodeToComponent = new HashMap<Integer, Integer>();
        Map<Integer, List<Integer>> componentToNodes = new HashMap<Integer, List<Integer>>();
        for (Integer node : nodes) {
            List<Integer> comp = new ArrayList<Integer>();
            comp.add(node);
            componentToNodes.put(node, comp);
            nodeToComponent.put(node, node);
        }

        for (Integer[] e : edges) {
            Integer node1 = e[0];
            Integer node2 = e[1];

            Integer comp1 = nodeToComponent.get(node1);
            Integer comp2 = nodeToComponent.get(node2);

            if (comp1 != comp2) {
                //always add the smaller component to the bigger component
                if (componentToNodes.get(comp1).size() > componentToNodes.get(comp2).size()) {
                    for (Integer node : componentToNodes.get(comp2)) {
                        nodeToComponent.remove(node);
                        nodeToComponent.put(node, comp1);
                    }

                    componentToNodes.get(comp1).addAll(componentToNodes.get(comp2));
                    componentToNodes.remove(comp2);
                } else {
                    for (Integer node : componentToNodes.get(comp1)) {
                        nodeToComponent.remove(node);
                        nodeToComponent.put(node, comp2);
                    }

                    componentToNodes.get(comp2).addAll(componentToNodes.get(comp1));
                    componentToNodes.remove(comp1);
                }
            }
            if (componentToNodes.size() == 1) {
                break;
            }
        }

        return componentToNodes;
    }


    public static void pruneDisconnected(){
        Map<Integer, List<Integer>> components = getComponentMap();
        List<Integer> idKeep = new ArrayList<Integer>();
        int biggest = 0;
        for (List<Integer> comp : components.values()) {
            if (comp.size() > biggest) {
                biggest = comp.size();
                idKeep = comp;
            }
        }

        log.debug("retaining largest component: " + biggest);

        String idBAT = MonetUtil.createBATFromCollection(DataTypeEnum.OID, idKeep);
        NST objNST = DB.getObjectNST();
        int beforeObjNum = objNST.getRowCount();
        objNST.deleteRows("id NOTIN " + idBAT);
        int afterObjNum = objNST.getRowCount(); 

        NST linkNST = DB.getLinkNST();
        int beforeLinkNum  = linkNST.getRowCount();
        linkNST.deleteRows("o1_id NOTIN " + idBAT + " AND o2_id NOTIN " + idBAT);
        int afterLinkNum  = linkNST.getRowCount();

        DB.commit();

        log.debug("deleted " + (beforeObjNum - afterObjNum) + " objects and "
                + (beforeLinkNum - afterLinkNum) + " links");
    }

    /**
     * Compute the minimum spanning tree of the given graph using Prim's algorithm
     * @param graph Graph abstraction to provide access to neighbors
     * @return A map consisting of node -> (node -> weight) for each node in the minimum
     * spanning tree.
     */
    public static Map<Integer, Map<Integer, Double>> prims(Graph graph) {
        log.info("Computing minimum spanning tree with Prim's Algorithm...");
        double mst = 0.0;
        List<Integer> idList = DB.getObjectNST().selectRows().toOIDList("id");
        Random rand = new Random();

        Map<Integer, Map<Integer, Double>> tree = new HashMap<Integer, Map<Integer, Double>>();
        PriorityQueue<Edge> edges = new PriorityQueue<Edge>();

        Set<Integer> nodes = new HashSet<Integer>();
        Integer firstNode = idList.get(rand.nextInt(idList.size()));
        nodes.add(firstNode);

        Set<Node> firstNeighbors = graph.getNeighbors(firstNode);
        for (Node neigh : firstNeighbors) {
            edges.offer(new Edge(firstNode, neigh.id, neigh.weight));
        }

        while (nodes.size() < idList.size()) {
            Edge bestEdge = edges.poll();
            if (nodes.contains(bestEdge.to)) {
                continue;
            }

            nodes.add(bestEdge.to);
            mst += bestEdge.weight;
            if (!tree.containsKey(bestEdge.from)) {
                tree.put(bestEdge.from, new HashMap<Integer, Double>());
            }
            tree.get(bestEdge.from).put(bestEdge.to, bestEdge.weight);

            if (!tree.containsKey(bestEdge.to)) {
                tree.put(bestEdge.to, new HashMap<Integer, Double>());
            }
            tree.get(bestEdge.to).put(bestEdge.from, bestEdge.weight);

            //add new edges to queue
            Set<Node> neighbors = graph.getNeighbors(bestEdge.to);
            for (Node neigh : neighbors) {
                edges.offer(new Edge(bestEdge.to, neigh.id, neigh.weight));
            }
        }

        log.info("Minimum spanning tree weight = " + mst);
        return tree;
    }

    private static class Edge implements Comparable {
        public final Integer from;
        public final Integer to;
        public final Double weight;

        public Edge(Integer from, Integer to, Double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }


        public int compareTo(Object o) {
            return this.weight.compareTo(((Edge) o).weight);
        }
    }

}
