/**
 * $Id: CountAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

public class CountAggregator extends ValueFilteredAggregator {

    public CountAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && !s.isSingleValue());
    }

    public NST createAggregateTable(NST valueTable) {
        // count (gender = 'm')
        return valueTable.aggregate("count", "subg_id", "value", source.getDistinctSubgraphs());
    }

    public String name() {
        return "count";
    }

}
