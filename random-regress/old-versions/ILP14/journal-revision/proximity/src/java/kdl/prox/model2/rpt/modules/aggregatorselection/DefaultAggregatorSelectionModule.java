/**
 * $Id: DefaultAggregatorSelectionModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.aggregatorselection;

import kdl.prox.model2.rpt.aggregators.AggregatorFinderUtil;

import java.util.*;

/**
 * Aggregator selection can be used to restrict the aggregators considered when learning a tree.
 */
public class DefaultAggregatorSelectionModule implements RPTAggregatorSelectionModule {

    // A list to restrict the aggregators that are used. by default, contains all names, and
    // A list of aggregators to skip for core items; by default contains Mode and Proportion
    private List<String> validAggregatorNamesList;
    private List<String> invalidAggregatorNamesForCore;

    public DefaultAggregatorSelectionModule() {
        initValidAggregatorNames();
        initInvalidAggretorNamesForCore();
    }


    public List<String> getValidAggregatorNamesList() {
        return validAggregatorNamesList;
    }

    public List<String> getInvalidAggregatorNamesForCore() {
        return invalidAggregatorNamesForCore;
    }

    /**
     * Initialize the list of aggregators that it doesn't make sense to use on core items.
     * By default,
     */
    protected void initInvalidAggretorNamesForCore() {
        invalidAggregatorNamesForCore = new ArrayList<String>();
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ModeAggregator");
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ProportionAggregator");
    }


    /**
     * Initialize the list of validAggregators with ALL possible aggregators
     */
    protected void initValidAggregatorNames() {
        validAggregatorNamesList = new ArrayList<String>();
        Set allAggregatorClasses = AggregatorFinderUtil.searchForAggregatorClasses();
        for (Iterator iterator = allAggregatorClasses.iterator(); iterator.hasNext();) {
            Class aggregatorClass = (Class) iterator.next();
            String aggregatorClassName = aggregatorClass.getName();
            validAggregatorNamesList.add(aggregatorClassName);
        }
    }

    /**
     * Restrict the aggregators to use for core items. Use null to reset to the default
     *
     * @param invalidAggregatorNames
     */
    public RPTAggregatorSelectionModule setInvalidAggregatorsForCore(String[] invalidAggregatorNames) {
        if (invalidAggregatorNames == null) {
            initInvalidAggretorNamesForCore();
        } else {
            invalidAggregatorNamesForCore = Arrays.asList(invalidAggregatorNames);
        }
        return this;
    }

    /**
     * Restrict the aggregators to use. Use null to reset to the default
     *
     * @param validAggregatorNames
     */
    public RPTAggregatorSelectionModule setValidAggregators(String[] validAggregatorNames) {
        if (validAggregatorNames == null) {
            initValidAggregatorNames();
        } else {
            validAggregatorNamesList = Arrays.asList(validAggregatorNames);
        }
        return this;
    }

}
