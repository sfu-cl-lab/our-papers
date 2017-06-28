/**
 * $Id: ModeAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

import java.util.List;

public class ModeAggregator extends UnfilteredAggregator {

    public ModeAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && !s.isContinuous() && !s.isSingleValue());
    }

    public NST createAggregateTable() {
        return source.getSourceTable().aggregate("mode", "subg_id", "value");
    }

    /**
     * Redefine the thresholds. In this case, we're interested in looking at all the values,
     * from the original table, and without binning!
     *
     * @param aggregateTable
     * @return
     */
    protected List findThresholds(NST aggregateTable) {
        return source.getDistinctValues();
    }


    public String name() {
        return "mode";
    }

    /**
     * Overload. Uses = instead of >
     *
     * @return
     */
    public String thresholdOp() {
        return "=";
    }
}
