/**
 * $Id: BranchingUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.util;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;

/**
 * The default branching finds the subgIDs that match the split and puts them in the YES side.
 * The ones that don't match the split go to NO
 * Missing values are added to both YES and NO with a given weight, proportional to the ratio of YES/NO.
 */
public class BranchingUtil {

    protected static Logger log = Logger.getLogger(BranchingUtil.class);

    public static NST[] getBranchSubgIDs(FeatureSetting split, RPTState state) {
        NST subgIDs = state.subgIDs;
        NST splitTable = state.nstCache.getTable(split.toString());

        // Find the matches
        NST tMatches = subgIDs.intersect(splitTable.filter("match = 'true'"), "subg_id");
        NST fMatches = subgIDs.intersect(splitTable.filter("match = 'false'"), "subg_id");

        // make sure that the sum doesn't come in the E notation, because addArithmeticColumn will interpret
        // it as a column name!
        DecimalFormat dec = new DecimalFormat("##############.############");

        // Find missing values, and split them proportionally adding some of them to the trueValues
        NST missing = subgIDs.difference(splitTable, "subg_id");
        if (missing.getRowCount() > 0) {
            int origCount = splitTable.getRowCount();
            double pctTrue = (origCount > 0 ? (tMatches.getRowCount() * 1.0) / (origCount * 1.0) : 0.5); // avoid division by zero
            String formatPctTrue = dec.format(pctTrue);
            String format1MinusPct = dec.format((1 - pctTrue));
            NST missingFalse = missing.copy();
            missing.replace("*", "weight", "weight * " + formatPctTrue);
            missingFalse.replace("*", "weight", "weight * " + format1MinusPct);
            tMatches.insertRowsFromNST(missing);
            fMatches.insertRowsFromNST(missingFalse);
        }

        return new NST[]{tMatches, fMatches};
    }

}
