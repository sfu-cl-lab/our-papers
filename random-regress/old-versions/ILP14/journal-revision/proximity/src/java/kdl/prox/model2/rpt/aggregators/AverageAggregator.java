/**
 * $Id: AverageAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

public class AverageAggregator extends UnfilteredAggregator {

    public AverageAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && s.isContinuous() && !s.isSingleValue());
    }

    public NST createAggregateTable() {
        return source.getSourceTable().aggregate("avg", "subg_id", "value");
    }

    public String name() {
        return "avg";
    }
}
