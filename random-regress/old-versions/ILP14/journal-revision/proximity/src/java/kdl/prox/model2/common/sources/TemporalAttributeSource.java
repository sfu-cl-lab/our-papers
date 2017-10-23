/**
 * $Id: TemporalAttributeSource.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.common.sources;

import kdl.prox.db.Container;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.NSTCreator;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.jdom.Element;


/**
 * An AttributeSource that compares time on peripheral items vs. time on core item, and
 * retains only those within a given interval
 */
public class TemporalAttributeSource extends AttributeSource {
    private AttributeSource aggAttr;
    private AttributeSource coreTimeAttr;
    private AttributeSource relatedTimeAttr;
    private String intervalMin;
    private String intervalMax;


    public TemporalAttributeSource(AttributeSource aggAttr, AttributeSource coreTime, AttributeSource relatedTime, String interval) {
        super(aggAttr.getItemName(), aggAttr.getAttrName());
        Assert.condition(aggAttr.isOnSameItem(relatedTime), "Related time must be on the same item as agg attr.");

        this.aggAttr = aggAttr;
        this.coreTimeAttr = coreTime;
        this.relatedTimeAttr = relatedTime;
        Assert.condition(interval.indexOf(":") != -1, "Interval must have a : .");

        String[] intervals = interval.split(":");
        Assert.condition(intervals.length == 2, "Must only have a min and a max.");
        this.intervalMin = intervals[0];
        this.intervalMax = intervals[1];
    }

    public TemporalAttributeSource(Element sourceEle) {
        super(sourceEle);
        aggAttr = new AttributeSource(sourceEle);
        Element elements = sourceEle.getChild("source").getChild("source-elements");
        intervalMin = elements.getChildText("interval-min");
        intervalMax = elements.getChildText("interval-max");

        Element coreEle = elements.getChild("core-time-source");
        Assert.condition(coreEle != null, "core Ele must be defined");
        String itemName = coreEle.getChildText("item-name");
        if (itemName != null) {
            coreTimeAttr = new AttributeSource(itemName, coreEle.getChildText("attr-name"));
        } else {
            coreTimeAttr = new AttributeSource(coreEle.getChildText("attr-name"));
        }

        Element relatedEle = elements.getChild("related-time-source");
        relatedTimeAttr = new AttributeSource(relatedEle.getChildText("item-name"), relatedEle.getChildText("attr-name"));
    }

    protected NST computeSourceTable(final Container cont, final NSTCache cache) {

        final NST timeNST = cache.getOrCreateTable("src" + getSignature(), new NSTCreator() {
            public NST create() {
                NST coreNST = coreTimeAttr.computeSourceTable(cont, cache);
                NST relatedNST = relatedTimeAttr.computeAttributeTable(cont, cache, "item_id,value");

                return coreNST.join(relatedNST, "subg_id = subg_id", "A.subg_id AS subg_id, item_id, A.value as core_time, B.value as related_time");
            }
        });

        NST joinNST = cache.getOrCreateTable("src" + getSignature() + "_" + aggAttr, new NSTCreator() {
            public NST create() {
                NST aggNST = aggAttr.computeAttributeTable(cont, cache, "item_id,value");

                String[] joinCols = new String[]{"subg_id", "item_id"};
                return aggNST.join(timeNST, joinCols, joinCols, "A.subg_id AS subg_id, A.item_id AS item_id, value,related_time, core_time");
            }
        });

        joinNST.addArithmeticColumn("core_time + " + intervalMin, "int", "intervalMin");
        joinNST.addArithmeticColumn("core_time + " + intervalMax, "int", "intervalMax");

        NST filteredNST = joinNST.filter("related_time >= intervalMin AND related_time <= intervalMax", "subg_id, value");

        joinNST.removeColumn("intervalMin");
        joinNST.removeColumn("intervalMax");

        return filteredNST;

    }

    public AttributeSource getAggAttr() {
        return aggAttr;
    }

    public AttributeSource getCoreTimeAttr() {
        return coreTimeAttr;
    }

    public AttributeSource getRelatedTimeAttr() {
        return relatedTimeAttr;
    }

    protected String getSignature() {
        return coreTimeAttr + "_" + relatedTimeAttr;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && other instanceof TemporalAttributeSource) {
            TemporalAttributeSource o = (TemporalAttributeSource) other;
            return (aggAttr.equals(o.aggAttr) && intervalMin.equals(intervalMin) && intervalMax.equals(intervalMax) &&
                    coreTimeAttr.equals(o.coreTimeAttr) && relatedTimeAttr.equals(relatedTimeAttr));
        }
        return false;
    }

    public String toString() {
        return aggAttr.toString() + "[" + coreTimeAttr.toString() + "+" + intervalMin + "<=" + relatedTimeAttr.toString() + "<=" + coreTimeAttr.toString() + "+" + intervalMax + "]";
    }

    public Element toXML() {
        Element sourceEle = super.toXML();
        sourceEle.getChild("source-elements").addContent(XMLUtil.createElementWithValue("interval-min", intervalMin));
        sourceEle.getChild("source-elements").addContent(XMLUtil.createElementWithValue("interval-max", intervalMax));

        Element coreEle = new Element("core-time-source");
        if (coreTimeAttr.getItemName() != null) {
            coreEle.addContent(XMLUtil.createElementWithValue("item-name", coreTimeAttr.getItemName()));
        }

        coreEle.addContent(XMLUtil.createElementWithValue("attr-name", coreTimeAttr.getAttrName()));
        sourceEle.getChild("source-elements").addContent(coreEle);

        Element relatedEle = new Element("related-time-source");

        relatedEle.addContent(XMLUtil.createElementWithValue("item-name", relatedTimeAttr.getItemName()));
        relatedEle.addContent(XMLUtil.createElementWithValue("attr-name", relatedTimeAttr.getAttrName()));

        sourceEle.getChild("source-elements").addContent(relatedEle);

        return sourceEle;
    }
}
