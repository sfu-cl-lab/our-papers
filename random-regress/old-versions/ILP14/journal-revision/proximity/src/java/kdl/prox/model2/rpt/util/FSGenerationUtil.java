/**
 * $Id: FSGenerationUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.util;

import kdl.prox.db.Container;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.common.sources.TemporalAttributeSource;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.aggregators.AggregatorFinderUtil;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This generation module looks at ALL the possible combinations of sources and aggregators by default, but
 * allows users to limit the list of aggregators via setValidAggregators.
 * <p/>
 * Also, by default it doesn't generate Mode and Proportion for core items, although it's possible to change that
 * via setInvalidAggregatorsForCore()
 */
public class FSGenerationUtil {

    static Logger log = Logger.getLogger(FSGenerationUtil.class);

    /**
     * Used by RPT.learn: Creates all the possible Feature Settings
     * from the sources and the validAggregatorNamesList
     *
     * @param cont
     * @param sourceList
     * @param rptState
     * @param validAggregatorNamesList
     * @param invalidAggregatorNamesForCore @return a list of the feature settings that were created, and whose tables are in the nstCache
     */
    public static List<FeatureSetting> createFSTables(Container cont, Source[] sourceList, RPTState rptState, List<String> validAggregatorNamesList, List<String> invalidAggregatorNamesForCore) {
        List fsList = new ArrayList<FeatureSetting>();

        Set allAggregatorClasses = AggregatorFinderUtil.searchForAggregatorClasses();
        for (int sourceIdx = 0; sourceIdx < sourceList.length; sourceIdx++) {
            Source source = sourceList[sourceIdx];
            source.init(cont, rptState.nstCache);

            for (Iterator iterator = allAggregatorClasses.iterator(); iterator.hasNext();) {
                Class aggregatorClass = (Class) iterator.next();
                String aggregatorClassName = aggregatorClass.getName();
                if (isAggregatorApplies(aggregatorClassName, source, rptState, validAggregatorNamesList, invalidAggregatorNamesForCore)) {
                    try {
                        Constructor constructor = aggregatorClass.getConstructor(new Class[]{Source.class});
                        Aggregator aggregator = (Aggregator) constructor.newInstance(new Object[]{source});
                        log.debug("Creating tables for " + aggregator + "," + source);
                        fsList.addAll(aggregator.computeTables(rptState.nstCache));
                    } catch (InvocationTargetException e) {
                        log.debug("Skipping aggregator " + aggregatorClass.getName() + " for " + source);
                    } catch (Exception e) {
                        log.debug("Creation of aggregator " + aggregatorClass.getName() + " failed for " + source);
                        e.printStackTrace();
                    }
                }
            }
        }

        return fsList;
    }

    public static List<FeatureSetting> createFSTables(Container cont, Source[] sourceList, RPTState rptState) {

        List<String> validAggregatorNamesList = new ArrayList<String>();
        Set allAggregatorClasses = AggregatorFinderUtil.searchForAggregatorClasses();
        for (Iterator iterator = allAggregatorClasses.iterator(); iterator.hasNext();) {
            Class aggregatorClass = (Class) iterator.next();
            String aggregatorClassName = aggregatorClass.getName();
            validAggregatorNamesList.add(aggregatorClassName);
        }

        List<String> invalidAggregatorNamesForCore = new ArrayList<String>();
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ModeAggregator");
        invalidAggregatorNamesForCore.add("kdl.prox.model2.rpt.aggregators.ProportionAggregator");

        return createFSTables(cont, sourceList, rptState, validAggregatorNamesList, invalidAggregatorNamesForCore);
    }

    /**
     * Checks whether an aggregator should be applied to a given source.
     *
     * @param aggregatorClassName
     * @param source
     * @param rptState
     * @param validAggregatorNamesList
     * @param invalidAggregatorNamesForCore @return true if the aggregator applies to the source in this state
     */
    private static boolean isAggregatorApplies(String aggregatorClassName, Source source, RPTState rptState, List<String> validAggregatorNamesList, List<String> invalidAggregatorNamesForCore) {
        if (!validAggregatorNamesList.contains(aggregatorClassName)) {
            return false;
        }

        if (source instanceof AttributeSource && invalidAggregatorNamesForCore.contains(aggregatorClassName)) {
            AttributeSource attributeSource = (AttributeSource) source;
            AttributeSource classLabel = rptState.classLabel;
            if (classLabel != null && attributeSource.isOnSameItem(classLabel)) {
                return false;
            }
        }

        if (source instanceof TemporalAttributeSource) {
            return isAggregatorApplies(aggregatorClassName, ((TemporalAttributeSource) source).getAggAttr(), rptState, validAggregatorNamesList, invalidAggregatorNamesForCore);
        }

        return true;
    }


    /**
     * Used by RPT.apply: creates tables for a list of already established FeatureSettings
     * NOTE: Remember to clear the nstCache before calling it --unless you intend to re-use old tables.
     *
     * @param cont
     * @param fsList
     * @param rptState
     * @return the same list of featureSettings,  whose tables are in the nstCache
     */
    public static List<FeatureSetting> createFSTables(Container cont, List<FeatureSetting> fsList, RPTState rptState) {
        for (int fsIdx = 0; fsIdx < fsList.size(); fsIdx++) {
            FeatureSetting featureSetting = fsList.get(fsIdx);
            featureSetting.getSource().init(cont, rptState.nstCache);
            featureSetting.getAggregator().computeTable(rptState.nstCache, featureSetting);
        }
        return fsList;
    }


}
