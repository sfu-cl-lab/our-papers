/**
 * $Id: ProportionAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;

public class ProportionAggregator extends ValueFilteredAggregator {

    public ProportionAggregator(Source s) {
        super(s);
    }

    protected boolean appliesToSource(Source s) {
        return (s instanceof AttributeSource && !s.isSingleValue());
    }

    /**
     * Returns a table of count(gender='m') / items_in_subg
     *
     * @return
     */
    public NST createAggregateTable(NST valueTable) {
        // count of all subgraphs  first
        final NST subgCountNST = source.getDistinctSubgraphs();
        // tcount of the value table
        NST countTable = valueTable.aggregate("count", "subg_id", "value", subgCountNST);
        NST propNST = countTable.join(subgCountNST, "subg_id = subg_id");

        // and divide
        propNST.castColumn("value", "dbl").castColumn("count", "dbl");
        propNST.addArithmeticColumn("value / count", "dbl", "prop");
        NST aggrNST = propNST.project("A.subg_id, prop").renameColumns("subg_id, value");
        countTable.release();
        propNST.release();
        return aggrNST;
    }

    public String name() {
        return "prop";
    }

}
