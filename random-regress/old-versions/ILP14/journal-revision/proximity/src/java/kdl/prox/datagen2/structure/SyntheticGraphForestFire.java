/**
 * $Id: SyntheticGraphForestFire.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Createst a ForestFire Graph
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphForestFire {
    private static Logger log = Logger.getLogger(SyntheticGraphForestFire.class);
    private static Random rand = new Random();

    private double[] cumProbs; // cached probs for geometric distribution
    private double burnRatio;

    private Map<Integer, List<Integer>> linksIn = new HashMap<Integer, List<Integer>>(); // o1_id -> [ o2_id's ]
    private Map<Integer, List<Integer>> linksOut = new HashMap<Integer, List<Integer>>(); // o2_id -> [ o1_id's ]

    public SyntheticGraphForestFire(int n, double burnProbFor, double burnProbBack) {
        log.info("creating forest fire graph with " + n + " nodes, forward burn prob " + burnProbFor + ", backward burn prob " + burnProbBack);

        initProbs(burnProbFor);
        burnRatio = burnProbBack / burnProbFor;

        SyntheticGraphUtil.createObjects(n);

        // start the graph with a lonely object
        this.linksIn.put(0, new ArrayList<Integer>());
        this.linksOut.put(0, new ArrayList<Integer>());

        // nodes will be numbered 1...n
        log.debug("burning links");
        for (int node = 1; node < n; node++) {
            if (((node + 1) % 10000) == 0) {
                log.debug("\t" + (node + 1));
            }
            linksIn.put(node, new ArrayList<Integer>());
            linksOut.put(node, new ArrayList<Integer>());

            Integer ambass = rand.nextInt(node);
            linksIn.get(ambass).add(node);
            linksOut.get(node).add(ambass);
            burnIterative(node, ambass);
        }

        writeLinksToDatabase();
        log.debug("created graph with " + DB.getObjectNST().getRowCount() + " objects, " + DB.getLinkNST().getRowCount() + " links");
    }


    private int writeLinksToDatabase() {
        log.debug("writing links to database");

        List<Integer[]> linkList = new ArrayList<Integer[]>();
        int linkId = 0;
        for (Integer source : linksOut.keySet()) {
            List<Integer> targets = linksOut.get(source);

            for (Integer target : targets) {
                linkList.add(new Integer[]{linkId++, source, target});
            }
        }

        Object[][] linkArray = new Object[linkList.size()][];
        int idx = 0;
        for (Integer[] linkInfo : linkList) {
            linkArray[idx] = linkInfo;
            idx++;
        }

        DB.getLinkNST().fastInsert(linkArray);
        log.debug("added " + linkArray.length + " links");
        return DB.getLinkNST().getRowCount();
    }


    private void burnIterative(Integer sourceNode, Integer startNode) {
        Set<Integer> unavail = new HashSet<Integer>();
        unavail.add(sourceNode);
        unavail.add(startNode);

        List<Integer> burnList = new ArrayList<Integer>();
        burnList.add(startNode);
        while (!burnList.isEmpty()) {
            Integer ambassNode = burnList.remove(0);

            List<Integer> neighborsOut = new ArrayList<Integer>(this.linksOut.get(ambassNode));
            List<Integer> neighborsIn = new ArrayList<Integer>(this.linksIn.get(ambassNode));
            neighborsOut.removeAll(unavail);
            neighborsIn.removeAll(unavail);

            neighborsIn.removeAll(neighborsOut);

            int degree = nextGeometric();
            int degreeIn = 0;
            int degreeOut = 0;
            for (int i = 0; i < degree; i++) {
                // flip coin according to the ratio
                double threshold = burnRatio / (burnRatio + 1.0);
                if (rand.nextDouble() < threshold) {
                    degreeIn++;
                } else {
                    degreeOut++;
                }
            }

            int countIn = neighborsIn.size();
            int countOut = neighborsOut.size();
            if (degreeIn > countIn) {
                int needed = degreeIn - countIn;
                degreeOut = Math.min(degreeOut + needed, countOut);
                degreeIn = countIn;
            }
            if (degreeOut > countOut) {
                int needed = degreeOut - countOut;
                degreeIn = Math.min(degreeIn + needed, countIn);
                degreeOut = countOut;
            }

            Collections.shuffle(neighborsIn);
            Collections.shuffle(neighborsOut);
            List<Integer> targetsIn = neighborsIn.subList(0, degreeIn);
            List<Integer> targetsOut = neighborsOut.subList(0, degreeOut);

            List<Integer> targets = new ArrayList<Integer>();
            targets.addAll(targetsIn);
            targets.addAll(targetsOut);

            this.linksOut.get(sourceNode).addAll(targets);
            for (Integer target : targets) {
                this.linksIn.get(target).add(sourceNode);
            }

            unavail.addAll(targets);
            burnList.addAll(targets);
        }
    }

    // initialize the probs for the geometric distribution
    private void initProbs(double burnProb) {
        this.cumProbs = new double[1000];
        this.cumProbs[0] = 1.0 - burnProb;
        for (int i = 1; i < this.cumProbs.length; i++) {
            this.cumProbs[i] = this.cumProbs[i - 1] + Math.pow(burnProb, i) * (1.0 - burnProb);
        }
    }

    private int nextGeometric() {
        double r = rand.nextDouble();
        int n = 0;
        while ((cumProbs[n] < r) && (n < cumProbs.length)) {
            n++;
        }
        return n;
    }
}
