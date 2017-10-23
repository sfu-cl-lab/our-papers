/**
 * $Id: SyntheticGraphToroidalLattice.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.util.ConversionUtils;
import kdl.prox.nsi2.util.GraphUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates a graph as a toroidal lattice with 'side' nodes per size.
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphToroidalLattice {
    private static Logger log = Logger.getLogger(SyntheticGraphLattice.class);
    private int n;

    public SyntheticGraphToroidalLattice(int side) {
        n = side * side;
        log.info("creating toroidal lattice graph with " + side + " nodes per side (" + n + " total)");

        SyntheticGraphUtil.createObjects(n);

        Object[][] linkArray = new Object[n * 4][];
        int currLinkId = 0;
        for (int i = 0; i < n; i++) {

            // left
            if (!(i % side == 0)) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i - 1};
                currLinkId++;
            } else {//on left side, link to right side
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i + side - 1};
                currLinkId++;
            }

            // right
            if (!(i % side == (side - 1))) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i + 1};
                currLinkId++;
            } else {//on right side, link to left side
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i - side + 1};
                currLinkId++;
            }

            // up
            if (i >= side) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i - side};
                currLinkId++;
            } else {//on top side, link to bottom side
                linkArray[currLinkId] = new Integer[]{currLinkId, i, n - side + i};
                currLinkId++;
            }

            // down
            if ((i + side) <= (n - 1)) {
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i + side};
                currLinkId++;
            } else {//on bottom side, link to top side
                linkArray[currLinkId] = new Integer[]{currLinkId, i, i % side};
                currLinkId++;
            }
        }

        DB.getLinkNST().fastInsert(linkArray);
    }

    public void addLongRangeConnections(int q, double alpha, Graph graph) {
        //following Kleinberg's "Navigation in a small world"
        //add q long range connections to each node randomly chosen with
        //probability proportional to r^(-alpha) where r is manhattan distance (i.e., shortest hop count)

        int side = (int) Math.sqrt(this.n);

        Object[][] longRangeLinkArray = new Object[this.n * q][];

        int linkAddedCount = 0;
        int currLinkId = DB.getLinkNST().max("link_id") + 1;

        //random candidates, get more than one at a time for efficiency
        List<Integer> candidates = GraphUtils.chooseRandomNodes(side);

        Random rand = new Random();

        //get all nodes
        List<Integer> objectIds = DB.getObjectNST().selectRows().toOIDList("id");

        //for each node, randomly choose q long range links
        for (int i = 0; i < objectIds.size(); i++) {
            if (i % 500 == 0)
                log.debug("Getting long range for node " + i);
            Integer currNode = objectIds.get(i);
            List<Integer> currLongAdded = new ArrayList<Integer>();
            for (int j = 0; j < q; j++) {
                boolean added = false;
                while (!added) {
                    Integer candidate;
                    if (candidates.size() > 0) {
                        candidate = candidates.remove(0);
                    } else {
                        candidates = GraphUtils.chooseRandomNodes(side);
                        candidate = candidates.remove(0);
                    }

                    boolean candidateOkay = false;
                    while (!candidateOkay) {
                        //don't add self as neighbor
                        //don't try to add current neighbors as a new neighbor
                        //don't try to readd new long range neighbors as a new neighbor (for q>1)
                        if (currNode.equals(candidate) ||
                                ConversionUtils.nodesToIntegers(graph.getNeighbors(currNode)).contains(candidate) ||
                                currLongAdded.contains(candidate)) {
                            if (candidates.size() > 0) {
                                candidate = candidates.remove(0);
                            } else {
                                candidates = GraphUtils.chooseRandomNodes(side);
                                candidate = candidates.remove(0);
                            }
                            continue;
                        }
                        candidateOkay = true;
                    }

                    int rowCurr = (int) Math.floor(currNode / side);
                    int colCurr = currNode % side;
                    int rowCand = (int) Math.floor(candidate / side);
                    int colCand = candidate % side;

                    int rowDiff = Math.abs(rowCurr - rowCand);
                    int colDiff = Math.abs(colCurr - colCand);

                    int r = Math.min(rowDiff, (side - rowDiff)) + Math.min(colDiff, (side - colDiff));

                    double prob = Math.pow((double) r, -1.0 * alpha);

                    double randNum = rand.nextDouble();
                    if (randNum < prob) {
                        longRangeLinkArray[linkAddedCount] = new Integer[]{currLinkId, currNode, candidate};
                        linkAddedCount++;
                        currLinkId++;
                        currLongAdded.add(candidate);
                        added = true;
                    }
                }
            }
        }
        DB.getLinkNST().fastInsert(longRangeLinkArray);
    }
}
