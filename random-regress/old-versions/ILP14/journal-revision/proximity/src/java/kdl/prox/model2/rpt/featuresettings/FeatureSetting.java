/**
 * $Id: FeatureSetting.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.featuresettings;

import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

public abstract class FeatureSetting {

    static Logger log = Logger.getLogger(FeatureSetting.class);


    protected Source source;
    protected Aggregator aggregator;
    protected String threshold;

    public FeatureSetting(Source s, Aggregator a, String t) {
        Assert.notNull(s, "Null source");
        Assert.notNull(a, "Null aggregator");
        source = s;
        aggregator = a;
        threshold = t;
    }

    /**
     * Creates a new FeatureSetting from an element of type <feature-setting>
     * Called from XMLUtil.readFeatureSettingFromXML. At this point, this constructor is of the type
     * specified in fs-class.
     * Can be extended by derived classes to read extra values from fs-elements
     *
     * @param contEle
     */
    public FeatureSetting(Element contEle) {
        Element fsEle = contEle.getChild("feature-setting");
        source = XMLUtil.readSourceFromXML(fsEle);
        aggregator = XMLUtil.readAggregatorFromXML(fsEle, source);
        threshold = fsEle.getChildText("threshold");
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public Source getSource() {
        return source;
    }

    public String getThreshold() {
        return threshold;
    }

    public abstract String toString();

    /**
     * Saves the feature setting as an XML element of type <feature-setting>
     * Calls toXMLElements() method, which derived classes can instantiate to add their own elements to the featuresetting
     *
     * @return the XML element
     */
    public Element toXML() {
        Element splitTop = new Element("feature-setting");
        splitTop.addContent(XMLUtil.createElementWithValue("fs-class", this.getClass().getName()));
        splitTop.addContent(source.toXML());
        splitTop.addContent(aggregator.toXML());
        splitTop.addContent(XMLUtil.createElementWithValue("threshold", threshold));
        splitTop.addContent(new Element("fs-elements"));
        return splitTop;
    }
}
