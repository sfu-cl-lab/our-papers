/**
 * $Id: CountDistinctAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

public class CountDistinctAggregator extends UnfilteredAggregator {

    public CountDistinctAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && !s.isContinuous() && !s.isSingleValue());
    }

    public NST createAggregateTable() {
        NST attributeTable = source.getSourceTable();
        attributeTable.addDistinctCountColumn("subg_id", "value", "count");
        NST aggrNST = attributeTable.filter("subg_id DISTINCT ROWS", "subg_id, count").renameColumn("count", "value");
        attributeTable.removeColumn("count");
        return aggrNST;
    }

    public String name() {
        return "count_distinct";
    }
}
