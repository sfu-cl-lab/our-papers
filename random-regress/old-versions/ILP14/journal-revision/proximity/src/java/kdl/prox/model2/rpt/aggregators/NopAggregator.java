/**
 * $Id: NopAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

import java.util.List;

/**
 * This is a dummy aggregator, which doesn't really aggregate. It's applied to sources that already have a single
 * value per subgraph, and thus do not need to be aggregated.
 * If the source is discrete (eg, gender) then the aggregator uses the = operator for thresholds, as in
 * nop(A.gender)='m', nop(A.gender)='f'
 * If the source is continuous (eg, salary) then the aggregator uses the >= operator, as in
 * nop(A.salary)>=10.0, nop(A.salary)>=20.0
 * <p/>
 * For sources  that are not normally considered continuous (eg, ints for a 'year' attribute), the user can force them
 * to be continuous so that they are processed with the >= operator
 */
public class NopAggregator extends UnfilteredAggregator {

    public NopAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && s.isSingleValue());
    }

    public NST createAggregateTable() {
        return source.getSourceTable();
    }

    /**
     * Redefine the thresholds. In this case, we're interested in looking at all the values,
     * from the original table, and without binning if the source is discrete (or use the normal binning if
     * it's discrete)
     *
     * @param aggregateTable
     * @return
     */
    protected List findThresholds(NST aggregateTable) {
        return (source.isContinuous() ? super.findThresholds(aggregateTable) : source.getSourceTable().getDistinctColumnValues("value"));
    }


    public String name() {
        return "nop";
    }

    /**
     * Overload. Uses = instead of >
     *
     * @return
     */
    public String thresholdOp() {
        return (source.isContinuous() ? ">=" : "=");
    }
}
