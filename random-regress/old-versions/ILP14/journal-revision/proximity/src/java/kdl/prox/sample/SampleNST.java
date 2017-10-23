/**
 * $Id: SampleNST.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.sample;

import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SampleNST {
    private static Logger log = Logger.getLogger(SampleNST.class);
    private static final String NUMBER_COL_NAME = "samplenumbercolumn";
    private static final String NUMBER_COL_NAME_RAND = "samplenumbercolumn_rand";
    private static final String ID_COL_NAME = "samplenstkeycolumn";


    /**
     * Create a List of new NSTs by randomly sampling rows from an existing NST.  The new
     * NSTs are disjoint.
     *
     * @param sourceNST
     * @param n               size of generated NSTs
     * @param numDisjointSets number of NSTs to create
     * @return
     */
    public static List sample(NST sourceNST, int n, int numDisjointSets) {
        String origColNames = sourceNST.getNSTColumnNamesAsString();

        NST sampleNST = sourceNST.copy();
        sampleNST.addKeyColumn(ID_COL_NAME);
        sampleNST.addNumberColumn(NUMBER_COL_NAME);

        String idBAT = sampleNST.getNSTColumn(ID_COL_NAME).getBATName();
        String numberBAT = sampleNST.getNSTColumn(NUMBER_COL_NAME).getBATName();
        String sampleBAT = RandomizeAttr.getRandomizedBat(idBAT, numberBAT);

        sampleNST.addColumnFromBATVar(sampleBAT, NUMBER_COL_NAME_RAND, "oid");

        // grab only the rows with randomized number values < n, in multiple chunks
        List sampleSets = new ArrayList();
        for (int i = 0; i < numDisjointSets; i++) {
            int lowerBound = i * n;
            int upperBound = lowerBound + n;
            NST sampleNSTSet = sampleNST.filter(NUMBER_COL_NAME_RAND + " BETWEEN " + lowerBound + "-" + (upperBound - 1),
                    origColNames);
            sampleSets.add(sampleNSTSet);
        }

        sampleNST.release();

        return sampleSets;
    }

    /**
     * Overload that creates a single sampled NST from an existing NST
     *
     * @param sourceNST
     * @param n
     * @return
     */
    public static NST sample(NST sourceNST, int n) {
        List sets = sample(sourceNST, n, 1);
        return (NST) sets.get(0);
    }


}
