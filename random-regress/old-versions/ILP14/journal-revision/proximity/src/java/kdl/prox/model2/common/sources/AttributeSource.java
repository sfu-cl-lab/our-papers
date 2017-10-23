/**
 * $Id: AttributeSource.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.common.sources;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.NSTCreator;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.jdom.Element;

public class AttributeSource extends Source {

    protected boolean isSubgraphAttr;

    protected String itemName;
    protected String attrName;

    public AttributeSource(String attrName) {
        Assert.stringNotEmpty(attrName, "attrName cannot be empty");
        this.attrName = attrName;
        this.isSubgraphAttr = true;
    }

    public AttributeSource(String itemName, String attrName) {
        Assert.stringNotEmpty(itemName, "itemName cannot be empty");
        Assert.stringNotEmpty(attrName, "attrName cannot be empty");
        this.itemName = itemName;
        this.attrName = attrName;
        this.isSubgraphAttr = false;
    }

    public AttributeSource(Element sourceEle) {
        super(sourceEle);
        Element elements = sourceEle.getChild("source").getChild("source-elements");
        itemName = elements.getChildText("item-name");
        attrName = elements.getChildText("attr-name");
        isSubgraphAttr = (itemName == null);
    }

    /**
     * Returns the [subg_id, colList] table for this source.
     * If it's a subgraph attribute, it simply returns the attribute's table
     * Otherwise, it find the objects/links with the given itemName, and joins them with the attrTable
     *
     * @param container
     * @param cache
     * @return a table [subg_id, colList]
     */
    protected NST computeSourceTable(Container container, NSTCache cache) {
        return computeAttributeTable(container, cache, "value");
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && other instanceof AttributeSource) {
            AttributeSource o = (AttributeSource) other;
            return (isOnSameItem(o) && attrName.equals(o.attrName));
        }
        return false;
    }

    /**
     * Used by DefaultFSGenerationModule, to check whether a given source is on the same item as the classLabel
     * Can be redefined by derived classes
     *
     * @param otherSource
     * @return true if they are both not subgraphAttrs, and the itemNames are the same
     */
    public boolean isOnSameItem(AttributeSource otherSource) {
        if (isSubgraphAttr || otherSource.isSubgraphAttr) {
            return false;
        }

        return itemName.equals(otherSource.itemName);
    }

    public boolean isSubgraphAttr() {
        return isSubgraphAttr;
    }


    /**
     * Extracted, to make it easy to extend this class
     *
     * @param container
     * @param cache
     * @param colList
     */
    protected NST computeAttributeTable(final Container container, NSTCache cache, String colList) {
        NST attrTable;
        if (isSubgraphAttr) {
            Attributes attributes = container.getSubgraphAttrs();
            NST attrDataNST = attributes.getAttrDataNST(attrName);
            attrTable = attrDataNST.project("id, " + colList).renameColumn("id", "subg_id");
        } else {
            // Find out whether this is an obj/link attribute
            String contTableName = "container" + container.getName() + "_" + itemName;

            NST objectsByName = cache.getOrCreateTable(contTableName + "_objs", new NSTCreator() {
                public NST create() {
                    return container.getItemNSTByName(true, itemName);
                }
            });
            NST linksByName = cache.getOrCreateTable(contTableName + "_links", new NSTCreator() {
                public NST create() {
                    return container.getItemNSTByName(false, itemName);
                }
            });

            int objectsCount = objectsByName.getRowCount();
            int linksCount = linksByName.getRowCount();
            if (objectsCount > 0 && linksCount > 0) {
                throw new IllegalArgumentException("Container has both objects and links with the same name: " + itemName);
            }
            NST itemsNST = (objectsCount > 0 ? objectsByName : linksByName);
            Attributes attributes = (objectsCount > 0 ? DB.getObjectAttrs() : DB.getLinkAttrs());
            NST attrDataNST = attributes.getAttrDataNST(attrName);
            attrTable = itemsNST.join(attrDataNST, "item_id = id", "subg_id, " + colList);
        }
        return attrTable;
    }


    public String toString() {
        return "[" + (itemName != null ? (itemName + ".") : "") + attrName + "]";
    }

    public Element toXML() {
        Element sourceEle = super.toXML();
        if (itemName != null) {
            sourceEle.getChild("source-elements").addContent(XMLUtil.createElementWithValue("item-name", itemName));
        }
        sourceEle.getChild("source-elements").addContent(XMLUtil.createElementWithValue("attr-name", attrName));
        return sourceEle;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
