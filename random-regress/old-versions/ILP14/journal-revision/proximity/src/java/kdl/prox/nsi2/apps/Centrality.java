/**
 * $Id: Centrality.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.apps;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.nsi2.graph.Node;
import kdl.prox.nsi2.nsi.NSI;
import kdl.prox.nsi2.search.Search;
import kdl.prox.nsi2.util.ConversionUtils;
import kdl.prox.nsi2.util.GraphUtils;
import kdl.prox.script.SNA;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * The Centrality class consists of various methods to compute centrality measures, like betweenness
 * and closeness, on nodes or links.  These methods either require pathfinding or distance estimation, so
 * they heavily exploit Search methods and NSIs.
 */
public class Centrality {
    private static Logger log = Logger.getLogger(Centrality.class);

    private static Random rand = new Random();

    /**
     * Compute exact betweenness centrality for all nodes; uses Ulrik Brande's fast computation of betweenness
     * @return A list of nodes ordered by their betweenness centrality
     */
    public static List<Integer> betweenCentralRankingExact() {

        List<Integer> idRanking = new ArrayList<Integer>();

        DB.beginScope();
        NST betweenNST = SNA.calculateBetweennessNST(DB.getLinkNST(), true); // o1_id, value //todo: does this method work?

        NST sorted = betweenNST.sort("value DESC", "o1_id");
        ResultSet rs = sorted.selectRows("o1_id");
        while (rs.next()) {
            idRanking.add(rs.getOID(1));
        }
        DB.endScope();
        return idRanking;
    }

    /**
     * Approximation of betweenness centrality for every node
     * @param numPairs Number of searches used to estimate betweenness
     * @param search Search method used to find paths for each pair of nodes (e.g. breadth-first of best-first)
     * @return A list of nodes ordered by their betweenness centrality
     */
    public static List<Integer> betweenCentralRanking(int numPairs, Search search) {
        List<int[]> pairs = GraphUtils.chooseRandomNodePairs(numPairs);
        return betweenCentralRanking(pairs, search);
    }

    /**
     * Approximation of betweenness centrality for every node
     * @param pairs List of pairs of nodes to find paths
     * @param search Search method used to find paths for each pair of nodes (e.g. breadth-first of best-first)
     * @return A list of nodes ordered by their betweenness centrality
     */
    public static List<Integer> betweenCentralRanking(List<int[]> pairs, Search search) {

        Map<Integer, Double> nodeTotals = new HashMap<Integer, Double>(); // nodeId -> appearances

        for (int[] pair : pairs) {
            List<Node> pathNodes = search.search(pair[0], pair[1]).pathList();
            for (Integer node : ConversionUtils.nodesToIntegers(pathNodes)) {
                if (!nodeTotals.containsKey(node)) {
                    nodeTotals.put(node, 1.0);
                } else {
                    double previous = nodeTotals.get(node);
                    nodeTotals.put(node, previous + 1.0);
                }
            }
        }

        List<Integer> idRanking = new ArrayList<Integer>(nodeTotals.keySet());
        //sort descending according to appearances
        Collections.sort(idRanking, new scoreComparator(nodeTotals, false));
        return idRanking;
    }

    /**
     * Approximation of betweenness centrality for every edge in the network
     * @param numPairs Number of searches used to estimate link betweenness
     * @param search Search method used to find paths between pairs of nodes. NB: Must use a search
     * method that returns a path (e.g. best-first). Cannot use breadth-first search.
     * @return A list of pairs of nodes, corresponding to links, ordered by their betweenness centrality
     */
    public static List<Integer[]> betweenCentralLinkRanking(int numPairs, Search search) {
        log.debug("calculating link betweenness ranking for " + numPairs + " pairs");
        Map<String, Double> linkTotals = new HashMap<String, Double>(); // linkId -> appearances

        ResultSet resultSet = DB.getLinkNST().selectRows("o1_id,o2_id");
        while (resultSet.next()) {
            Integer node1 = resultSet.getOID("o1_id");
            Integer node2 = resultSet.getOID("o2_id");
            if (node1 > node2) {
                Integer tmp = node1;
                node1 = node2;
                node2 = tmp;
            }
            String key = node1 + "::" + node2;
            linkTotals.put(key, rand.nextDouble() / 1000000.0);
        }

        List<int[]> pairs = GraphUtils.chooseRandomNodePairs(numPairs);

        int iterCount = 0;
        for (int[] pair : pairs) {
            if (++iterCount % 10000 == 0) {
                log.debug("\t" + iterCount + " " + linkTotals.size());
            }

            List<Node> path = search.search(pair[0], pair[1]).pathList();
            List<Integer> pathNodes = new ArrayList<Integer>(ConversionUtils.nodesToIntegers(path));

            for (int i = 1; i < pathNodes.size(); i++) {
                Integer node1 = pathNodes.get(i - 1);
                Integer node2 = pathNodes.get(i);

                if (node1 > node2) {
                    Integer tmp = node1;
                    node1 = node2;
                    node2 = tmp;
                }
                String key = node1 + "::" + node2;

                if (!linkTotals.containsKey(key)) {
                    log.debug("link betweenness key error: " + key);
                    linkTotals.put(key, rand.nextDouble() / 1000000.0);
                }
                linkTotals.put(key, linkTotals.get(key) + 1.0);
            }
        }

        List<String> idRanking = new ArrayList<String>(linkTotals.keySet());
        class MapComp implements Comparator<String> {
            private Map<String, Double> map;

            MapComp(Map<String, Double> m) {
                this.map = m;
            }

            public int compare(String o1, String o2) {
                double v1 = map.get(o1);
                double v2 = map.get(o2);
                if (v1 < v2) {
                    return 1;
                } else if (v1 > v2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
        Collections.sort(idRanking, new MapComp(linkTotals));

        List<Integer[]> pairRanking = new ArrayList<Integer[]>();
        for (String pairString : idRanking) {
            String[] elts = pairString.split("::");
            Integer node1 = Integer.parseInt(elts[0]);
            Integer node2 = Integer.parseInt(elts[1]);
            pairRanking.add(new Integer[]{node1, node2});
        }
        return pairRanking;
    }

    /**
     * Approximation of link betweenness centrality used to find the link with the highest centrality
     * over a subset of the nodes in the network.  Useful for clustering based on betweenness components.
     * @param numPairs Number of searches used to estimate link betweenness
     * @param search Search method used to find paths between pairs of nodes. NB: Must use a search
     * method that returns a path (e.g. best-first). Cannot use breadth-first search.
     * @param objectIds List of ids corresponding to nodes that can be used to estimate betweenness.  Can be either
     * all nodes or a subset.  This will constrict the source and destination for the searches used.
     * @return An Object[] consisting of o1_id, o2_id, and the estimated link betweenness for the
     * link with the highest estimated betweenness
     */
    public static Object[] betweenCentralLinkRanking(int numPairs, Search search, List<Integer> objectIds) {
        Map<String, Double> linkTotals = new HashMap<String, Double>(); // linkId -> appearances

        List<int[]> pairs = GraphUtils.chooseRandomNodePairs(numPairs, objectIds);

        int iterCount = 0;
        for (int[] pair : pairs) {
            if (++iterCount % 10000 == 0) {
                log.debug("\t" + iterCount + " " + linkTotals.size());
            }

            List<Node> path = search.search(pair[0], pair[1]).pathList();

            List<Integer> pathNodes = new ArrayList<Integer>(ConversionUtils.nodesToIntegers(path));

            for (int i = 1; i < pathNodes.size(); i++) {
                Integer node1 = pathNodes.get(i - 1);
                Integer node2 = pathNodes.get(i);

                if (node1 > node2) {
                    Integer tmp = node1;
                    node1 = node2;
                    node2 = tmp;
                }
                String key = node1 + "::" + node2;

                if (!linkTotals.containsKey(key)) {
                    linkTotals.put(key, rand.nextDouble() / 1000000.0);
                }
                linkTotals.put(key, linkTotals.get(key) + 1.0);
            }
        }

        List<String> idRanking = new ArrayList<String>(linkTotals.keySet());
        class MapComp implements Comparator<String> {
            private Map<String, Double> map;

            MapComp(Map<String, Double> m) {
                this.map = m;
            }

            public int compare(String o1, String o2) {
                double v1 = map.get(o1);
                double v2 = map.get(o2);
                if (v1 < v2) {
                    return -1;
                } else if (v1 > v2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
        Collections.sort(idRanking, new MapComp(linkTotals));

        String pairString = idRanking.get(idRanking.size() - 1);
        String[] elts = pairString.split("::");
        Integer node1 = Integer.parseInt(elts[0]);
        Integer node2 = Integer.parseInt(elts[1]);
        return new Object[]{node1, node2, linkTotals.get(pairString)};
    }

    /**
     * Approximation of closeness centrality for every node using an NSI.
     * @param eligibleNodes NST of nodes to restrict closeness estimation.  The returned nodes will be those identfied
     * in eligibleNodees.
     * @param numPathsPerNode Number of random nodes to select for each node
     * @param nsi NSI used to estimate distance, instead of searching, between pairs of nodes
     * @return A list of nodes ordered by their closeness centrality
     */
    public static List<Integer> closeCentralRanking(NST eligibleNodes, int numPathsPerNode, NSI nsi) {
        return closeCentralRanking(eligibleNodes.selectRows().toOIDList(1), numPathsPerNode, nsi);
    }

    /**
     * Approximation of closeness centrality for every node using a search routine.
     * @param eligibleNodes NST of nodes to restrict closeness estimation.  The returned nodes will be those identfied
     * in eligibleNodees.
     * @param numPathsPerNode Number of random nodes to select for each node
     * @param search Search method used to find distances between pairs of nodes. Search method does not need
     * to return a path, only a path length.
     * @return A list of nodes ordered by their closeness centrality
     */
    public static List<Integer> closeCentralRanking(NST eligibleNodes, int numPathsPerNode, Search search) {
        return closeCentralRanking(eligibleNodes.selectRows().toOIDList(1), numPathsPerNode, search);
    }

    /**
     * Approximation of closeness centrality for every node using an NSI.
     * @param eligibleNodes List of nodes to restrict closeness estimation.  The returned nodes will be those identfied
     * in eligibleNodees.
     * @param numPathsPerNode Number of random nodes to select for each node
     * @param nsi NSI used to estimate distance, instead of searching, between pairs of nodes
     * @return A list of nodes ordered by their closeness centrality
     */
    public static List<Integer> closeCentralRanking(List<Integer> eligibleNodes, int numPathsPerNode, NSI nsi) {
        Map<Integer, Double> closenessScores = new HashMap<Integer, Double>();
        int i = 0;
        for (Integer source : eligibleNodes) {
            if (++i % 100 == 0) {
                log.debug("\t" + i);
            }

            List<Integer> targets = StatUtil.sampleList(eligibleNodes, numPathsPerNode);

            double distTotal = 0.0;
            for (Integer target : targets) {
                if (source != target) {
                    distTotal += nsi.distance(source, target);
                }
            }
            closenessScores.put(source, distTotal);
        }

        Collections.sort(eligibleNodes, new scoreComparator(closenessScores, true));
        return eligibleNodes;
    }

    /**
     * Approximation of closeness centrality for every node using a search routine.
     * @param eligibleNodes List of nodes to restrict closeness estimation.  The returned nodes will be those identfied
     * in eligibleNodees.
     * @param numPathsPerNode Number of random nodes to select for each node
     * @param search Search method used to find distances between pairs of nodes. Search method does not need
     * to return a path, only a path length.
     * @return A list of nodes ordered by their closeness centrality
     */
    public static List<Integer> closeCentralRanking(List<Integer> eligibleNodes, int numPathsPerNode, Search search) {
        Map<Integer, Double> closenessScores = new HashMap<Integer, Double>();
        int i = 0;
        for (Integer source : eligibleNodes) {
            if (++i % 100 == 0) {
                log.debug("\t" + i);
            }

            List<Integer> targets = StatUtil.sampleList(eligibleNodes, numPathsPerNode);

            double distTotal = 0.0;
            for (Integer target : targets) {
                if (source != target) {
                    distTotal += search.search(source, target).pathlength();
                }
            }
            closenessScores.put(source, distTotal);
        }

        Collections.sort(eligibleNodes, new scoreComparator(closenessScores, true));
        return eligibleNodes;
    }

    public static class scoreComparator implements Comparator<Integer> {
        Random r = new Random();
        Map<Integer, Double> scores;
        boolean asc;

        scoreComparator(Map<Integer, Double> scores, boolean asc) {
            this.scores = scores;
            this.asc = asc;
        }

        // flips a coin in case of a tie
        public int compare(Integer node1, Integer node2) {
            double score1 = scores.get(node1);
            double score2 = scores.get(node2);
            if (score1 < score2) {
                return (asc ? -1 : 1);
            } else if (score1 > score2) {
                return (asc ? 1 : -1);
            } else {
                return (r.nextDouble() < 0.5 ? 1 : -1);
            }
        }
    }

}
