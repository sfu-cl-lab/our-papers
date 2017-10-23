/**
 * $Id: RPTAggregatorSelectionModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.aggregatorselection;

import java.util.List;

/**
 * Interface for aggregator selection modules.
 */
public interface RPTAggregatorSelectionModule {

    List<String> getValidAggregatorNamesList();

    List<String> getInvalidAggregatorNamesForCore();

    RPTAggregatorSelectionModule setInvalidAggregatorsForCore(String[] invalidAggregatorNames);

    RPTAggregatorSelectionModule setValidAggregators(String[] validAggregatorNames);
}
