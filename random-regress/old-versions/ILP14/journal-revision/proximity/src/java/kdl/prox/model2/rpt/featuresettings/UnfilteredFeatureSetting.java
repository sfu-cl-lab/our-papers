/**
 * $Id: UnfilteredFeatureSetting.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.featuresettings;

import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.aggregators.UnfilteredAggregator;
import org.jdom.Element;

public class UnfilteredFeatureSetting extends FeatureSetting {

    public UnfilteredFeatureSetting(Source s, UnfilteredAggregator a, String t) {
        super(s, a, t);
    }

    public UnfilteredFeatureSetting(Element fsEle) {
        super(fsEle);
    }

    public String toString() {
        return aggregator.name() + "(" + source + ")" + ((UnfilteredAggregator) aggregator).thresholdOp() + threshold;
    }

}
