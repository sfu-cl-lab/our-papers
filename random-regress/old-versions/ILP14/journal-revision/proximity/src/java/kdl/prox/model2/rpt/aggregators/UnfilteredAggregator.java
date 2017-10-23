/**
 * $Id: UnfilteredAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.NSTCreator;
import kdl.prox.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * A super class for aggregators such as count and proportion, which combine a value and a threshold
 */
public abstract class UnfilteredAggregator extends Aggregator {

    public UnfilteredAggregator(Source s) {
        super(s);
    }

    /**
     * This is the main method.
     * create the aggregate table                     avg(salary)
     * loop over the thresholds
     * create the feature table                       avg(salary) > 10
     *
     * @param cache
     * @return a list with the feature settings that have been computed. their toString() methods give keys into the cache
     */
    public List computeTables(NSTCache cache) {
        List tableNames = new ArrayList();

        final NST aggregateTable = createAggregateTable();     // avg(salary)
        List thresholds = findThresholds(aggregateTable);

        for (int thresIdx = 0; thresIdx < thresholds.size(); thresIdx++) {
            final String threshold = (String) thresholds.get(thresIdx);
            FeatureSetting fs = new UnfilteredFeatureSetting(source, this, threshold);
            cache.getOrCreateTable(fs.toString(), new NSTCreator() {
                public NST create() {
                    return createFeatureTable(aggregateTable, threshold);       // avg(salary > 10
                }
            });
            tableNames.add(fs);
        }
        aggregateTable.release();

        return tableNames;
    }

    public FeatureSetting computeTable(NSTCache cache, FeatureSetting fs) {
        Assert.condition(fs instanceof UnfilteredFeatureSetting, "fs has to be of type UnfilteredFeatureSetting");
        UnfilteredFeatureSetting unFiltered = (UnfilteredFeatureSetting) fs;
        final String threshold = unFiltered.getThreshold();

        String tableName = name() + "(" + source + ")";  // save/reuse the aggregator, just in case (avg(salary))
        final NST aggregateTable = cache.getOrCreateTable(tableName, new NSTCreator() {
            public NST create() {
                return createAggregateTable();
            }
        });
        cache.getOrCreateTable(fs.toString(), new NSTCreator() {
            public NST create() {
                return createFeatureTable(aggregateTable, threshold);  // avg(salary)>2
            }
        });
        return unFiltered;
    }


    /**
     * This is the method that computes the aggregation, as in avg(salary)
     *
     * @return an NST with [subg_id, value], with aggregates over the subg_id
     */
    public abstract NST createAggregateTable();


}
