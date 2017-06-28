/**
 * $Id: Aggregator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.List;

/**
 * Represents an aggregator, a class capable of summarizing a source on a per subgraph basis.
 * It is capable of creating a summary (e.g., count(actor.gender='m'),
 * finding an appropriate list of thresholds (e.g., 2, 4, 6), and
 * creating a binary table (feature vector) listing the subgraphs that match a threshold
 * (e.g., count(actor.gender='m') >= 4.
 */
public abstract class Aggregator {

    static Logger log = Logger.getLogger(Aggregator.class);


    static protected int numThresholds = 4;
    protected Source source;

    public Aggregator(Source s) {
        Assert.notNull(s, "null source");
        Assert.condition(appliesToSource(s), "The " + name() + " aggregator doesn't apply to the source " + s);
        this.source = s;
    }

    protected abstract boolean appliesToSource(Source s);

    /**
     * This is the main method. It computes the feature vector tables for all values/thresholds
     * and puts the in the NSTCache. It returns the keys to those NSTs saved in the cache.
     * The cache allows different aggregators to share intermediate tables. For example,
     * count and proportion can share the intermediate table for each attribute value, as in gender='m', gender='f'
     *
     * @param cache
     * @return a list of keys into the NST cache
     */
    public abstract List computeTables(NSTCache cache);

    /**
     * This is used to compute a single table. It opens up the feature setting and computes its table
     *
     * @param cache
     * @param fs
     * @return fs, the same feature setting... but add its table to the cache
     */
    public abstract FeatureSetting computeTable(NSTCache cache, FeatureSetting fs);

    /**
     * A utility method that computes the feature vector from the aggr table with a threshold
     * (eg, count(gender='m') > 2), or avg(salary) > 10
     * Can be overloaded by aggregators that compute the feature vector in a different way (e.g., mode)
     *
     * @param aggregateTable
     * @param threshold
     * @return an NST with [subg_id, match], where match is a boolean indicating whether the subg_id matches the FV
     */
    protected NST createFeatureTable(NST aggregateTable, String threshold) {
        return aggregateTable.match("subg_id", "value", thresholdOp(), threshold);
    }


    /**
     * Gets the distinct thresholds that can be used with the aggregate table.
     *
     * @param aggregateTable
     * @return
     */
    protected List findThresholds(NST aggregateTable) {
        List thresholds = aggregateTable.getDistinctColumnValuesBinned("value", numThresholds + 1);
        if (thresholds.size() <= numThresholds) {
            return thresholds;
        } else {
            return thresholds.subList(0, numThresholds);
        }
    }

    public static int getNumThresholds() {
        return numThresholds;
    }

    public abstract String name();


    public static void setNumThresholds(int thre) {
        numThresholds = thre;
    }

    /**
     * By defaul, the threshold operation is >=, but it can be redefined by specific aggregators
     *
     * @return >= by default, or the operation to use for creating the feature table
     */
    public String thresholdOp() {
        return ">=";
    }


    /**
     * Save this aggreator as an XML element
     *
     * @return an XML element for the aggregator
     */
    public Element toXML() {
        Element aggrEle = new Element("aggregator");
        aggrEle.addContent(XMLUtil.createElementWithValue("aggregator-class", getClass().getName()));
        return aggrEle;
    }
}
