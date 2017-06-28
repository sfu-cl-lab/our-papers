/**
 * $Id: ItemSource.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.common.sources;

import kdl.prox.db.Container;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.jdom.Element;

public class ItemSource extends Source {


    protected String itemName;
    protected NST subgNST;

    public ItemSource(String itemName) {
        Assert.stringNotEmpty(itemName, "itemName cannot be empty");
        this.itemName = itemName;
    }

    public ItemSource(Element sourceEle) {
        super(sourceEle);
        Element elements = sourceEle.getChild("source").getChild("source-elements");
        itemName = elements.getChildText("item-name");
    }


    /**
     * Overloaded from Source class. In this case, we cannot use the sourceTable to get the distinct subgraphs,
     * because it does not contain those subgraphs where there are not itemNames!
     * NOTE: We compute the distinct during computeSourceTable --> faster
     *
     * @return the distinct subgraphs
     */
    protected NST computeDistinctSubgraphs() {
        assertIsInitialized();
        return subgNST;
    }


    /**
     * Returns the [subg_id, value] table.
     * In this case, it simply filters the object/link table and removes items that do not match itemName
     *
     * @param container
     * @param cache
     * @return a table [subg_id, colList]
     */
    protected NST computeSourceTable(Container container, NSTCache cache) {
        return computeSourceTable(container, cache, "");
    }


    protected NST computeSourceTable(Container container, NSTCache cache, String colList) {
        // Find out whether this is an obj/link attribute
        NST objectsByName = container.getItemNSTByName(true, itemName);
        NST linksByName = container.getItemNSTByName(false, itemName);
        int objectsCount = objectsByName.getRowCount();
        int linksCount = linksByName.getRowCount();
        if (objectsCount > 0 && linksCount > 0) {
            throw new IllegalArgumentException("Container has both objects and links with the same name: " + itemName);
        }
        boolean isObjectItem = objectsCount > 0;

        NST itemsNST = (isObjectItem ? objectsByName : linksByName);
        NST sourceNST = itemsNST.project("subg_id, name, " + colList).renameColumn("name", "value");
        subgNST = container.getItemNST(isObjectItem).filter("subg_id DISTINCT ROWS", "subg_id");
        itemsNST.release();
        return sourceNST;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && other instanceof ItemSource) {
            ItemSource o = (ItemSource) other;
            return (itemName.equals(o.itemName));
        }
        return false;
    }


    public String getItemName() {
        return itemName;
    }


    /**
     * Overloaded, because these sources are discrete, and of type STR
     */
    public DataTypeEnum getType() {
        return DataTypeEnum.STR;
    }

    public boolean isContinuous() {
        return false;
    }


    public String toString() {
        return "[" + itemName + "]";
    }

    public Element toXML() {
        Element sourceEle = super.toXML();
        sourceEle.getChild("source-elements").addContent(XMLUtil.createElementWithValue("item-name", itemName));
        return sourceEle;
    }
}
