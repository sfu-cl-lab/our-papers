/**
 * $Id: ValueFilteredFeatureSetting.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt.featuresettings;

import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.aggregators.ValueFilteredAggregator;
import kdl.prox.model2.util.XMLUtil;
import org.jdom.Element;

public class ValueFilteredFeatureSetting extends FeatureSetting {

    protected String value;

    public ValueFilteredFeatureSetting(Source s, ValueFilteredAggregator a, String v, String t) {
        super(s, a, t);
        value = v;
    }

    public ValueFilteredFeatureSetting(Element fsEle) {
        super(fsEle);
        value = fsEle.getChild("feature-setting").getChild("fs-elements").getChildText("value");
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return aggregator.name() + "(" + source + ((ValueFilteredAggregator) aggregator).operator() + value + ")" + ">=" + threshold;
    }

    /**
     * Saves as XML. Takes the
     *
     * @return an XML element with all the standard FS items, plus a <value> item in the fs-element child
     */
    public Element toXML() {
        Element fsElement = super.toXML();
        fsElement.getChild("fs-elements").addContent(XMLUtil.createElementWithValue("value", value));
        return fsElement;
    }
}
