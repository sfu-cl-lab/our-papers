/**
 * $Id: ValueFilteredAggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.featuresettings.ValueFilteredFeatureSetting;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.NSTCreator;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A super class for aggregators such as count and proportion, which combine a value and a threshold
 */
public abstract class ValueFilteredAggregator extends Aggregator {


    public ValueFilteredAggregator(Source s) {
        super(s);
    }

    /**
     * This is the main method.
     * loop over the distinct values for the source
     * get the table filtered for the value                (gender = 'm')
     * create the aggregate table                          (count(gender = 'm'))
     * loop over the thresholds for this value
     * create the feature table                            (count(gender='m') > 2)
     * cache it
     *
     * @param cache
     * @return a list with the feature settings that have been computed. their toString() methods give keys into the cache
     */
    public List computeTables(NSTCache cache) {
        List tableNames = new ArrayList();

        List distinctValues = source.getDistinctValues();

        for (int valueIdx = 0; valueIdx < distinctValues.size(); valueIdx++) {
            final String value = (String) distinctValues.get(valueIdx);

            final NST valueTable = createValueTable(cache, value);                   // gender='m'
            final NST aggregateTable = createAggregateTable(valueTable);             // count(gender='m')

            List thresholds = findThresholds(aggregateTable);
            for (int thresIdx = 0; thresIdx < thresholds.size(); thresIdx++) {
                final String threshold = (String) thresholds.get(thresIdx);
                ValueFilteredFeatureSetting fs = new ValueFilteredFeatureSetting(source, this, value, threshold);
                cache.getOrCreateTable(fs.toString(), new NSTCreator() {
                    public NST create() {
                        return createFeatureTable(aggregateTable, threshold);       // count(gender='m')>2
                    }
                });
                tableNames.add(fs);
            }

            aggregateTable.release();
        }


        return tableNames;
    }

    public FeatureSetting computeTable(NSTCache cache, FeatureSetting fs) {
        Assert.condition(fs instanceof ValueFilteredFeatureSetting, "fs has to be of type ValueFilteredFeatureSetting");
        ValueFilteredFeatureSetting valueFS = (ValueFilteredFeatureSetting) fs;
        String value = valueFS.getValue();
        final String threshold = valueFS.getThreshold();
        final NST valueTable = createValueTable(cache, value);
        String tableName = name() + "(" + source + operator() + value + ")";
        // save/reuse the aggregator, just in case (avg(salary))
        final NST aggregateTable = cache.getOrCreateTable(tableName, new NSTCreator() {
            public NST create() {
                return createAggregateTable(valueTable);
            }
        });                // gender='m'
        cache.getOrCreateTable(fs.toString(), new NSTCreator() {
            public NST create() {
                return createFeatureTable(aggregateTable, threshold);      // count(gender='m')>2
            }
        });
        return valueFS;
    }


    /**
     * This is the method that computes the aggregation, as in (count(gender='m'))
     *
     * @return an NST with [subg_id, value], with aggregates over the subg_id
     */
    public abstract NST createAggregateTable(NST valueTable);

    /**
     * Returns the table filtered for a given value. Gets it from the cache, if it's there.
     *
     * @param cache
     * @param value
     * @return an NST with [subg_id, value] where value is the passed in valu
     */
    protected NST createValueTable(NSTCache cache, final String value) {
        String valueTableName = source + operator() + value;
        return cache.getOrCreateTable(valueTableName, new NSTCreator() {
            public NST create() {
                String quotedValue = value;
                if (source.getType() == DataTypeEnum.STR) {
                    quotedValue = Util.quote(value);
                }
                return source.getSourceTable().filter("value " + operator() + " " + quotedValue);
            }
        });
    }

    public abstract String name();

    public String operator() {
        return (source.isContinuous() ? ">" : "=");
    }

}
