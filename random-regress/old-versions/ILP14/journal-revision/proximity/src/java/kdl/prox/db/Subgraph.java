/**
 * $Id: Subgraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
 * $Id: Subgraph.java 3658 2007-10-15 16:29:11Z schapira $
 */
package kdl.prox.db;

import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a subgraph
 */

public class Subgraph {

    private static Logger log = Logger.getLogger(Subgraph.class);

    // IVs
    private int subgID;
    private Container parentContainer;
    private String objBATName;
    private String linkBATName;

    /**
     * Cached NSTs and Attributes
     * CoreNSTs correspond to the NSTs that save the values
     * ViewNSTs correspond to the view of CoreNSTs that contain values for this
     * particular subgraph only
     * Created as needed
     */
    private NST coreObjectNST = null;
    private NST coreLinkNST = null;


    /**
     * @param container
     * @param subgOID
     * @param objBATName
     * @param linkBATName
     */
    public Subgraph(Container container, int subgOID, String objBATName, String linkBATName) {
        Assert.notNull(container, "null container");
        Assert.stringNotEmpty(objBATName, "object BAT");
        Assert.stringNotEmpty(linkBATName, "link BAT");
        // store them in IVs
        this.parentContainer = container;
        this.subgID = subgOID;
        this.objBATName = objBATName;
        this.linkBATName = linkBATName;
    }


    /**
     * Deletes all linkd in this subgraph
     */
    private void deleteAllLinks() {
        NST nst = getCoreLinkNST();
        nst.deleteRows("subg_id = " + subgID);
    }


    /**
     * Deletes all objects in this subgraph
     */
    private void deleteAllObjects() {
        NST nst = getCoreObjectNST();
        nst.deleteRows("subg_id = " + subgID);
    }


    /**
     * Returns the NST that saves the links in the subgraphs. It returns the core one, even
     * if different subgraphs are stored in the same NST. To get the view of that
     * shared NST that corresponds to this particular subraphs, use getSubgLinkNST
     * <p/>
     * Caches the result
     *
     * @return
     * @
     */
    private NST getCoreLinkNST() {
        if (coreLinkNST == null) {
            coreLinkNST = new NST(linkBATName);
        }
        return coreLinkNST;
    }


    /**
     * Returns the NST that saves the objects in the subgraphs. It returns the core one, even
     * if different subgraphs are stored in the same NST. To get the view of that
     * shared NST that corresponds to this particular subraphs, use getSubgObjectNST
     * <p/>
     * Caches the result
     *
     * @return
     * @
     */
    private NST getCoreObjectNST() {
        if (coreObjectNST == null) {
            coreObjectNST = new NST(objBATName);
        }
        return coreObjectNST;
    }

    private int getItemsCount(boolean isObject) {
        NST subgItemsNST = (isObject ? getSubgObjectNST() : getSubgLinkNST());
        int cnt = subgItemsNST.getRowCount();
        subgItemsNST.release();
        return cnt;
    }


    /**
     * Returns a List of ProxLink instances for this subgraph.
     *
     * @return
     * @
     * @see ProxLink
     */
    public List getLinks() {
        ArrayList itemList = new ArrayList();
        NST subgLinkNST = getSubgLinkNST();
        NST nst = subgLinkNST.join(DB.getLinkNST(), "item_id = link_id", "item_id, name, o1_id, o2_id");
        // get them all and create items
        ResultSet resultSet = nst.selectRows();
        while (resultSet.next()) {
            int oid = resultSet.getOID(1);
            String name = resultSet.getString(2);
            int o1_id = -1;
            int o2_id = -1;
            String o1_idMaybeNil = resultSet.getString(3);
            String o2_idMaybeNil = resultSet.getString(4);
            if (o1_idMaybeNil != null) {
                o1_id = resultSet.getOID(3);
            }
            if (o2_idMaybeNil != null) {
                o2_id = resultSet.getOID(4);
            }
            ProxItem newItem = new ProxLink(oid, name, o1_id, o2_id);
            itemList.add(newItem);
        }
        nst.release();
        subgLinkNST.release();
        return itemList;
    }

    public int getLinkCount() {
        return getItemsCount(false);
    }


    /**
     * Returns a List of ProxObj instances for this subgraph.
     *
     * @return
     * @
     * @see ProxObj
     */
    public List getObjects() {
        ArrayList itemList = new ArrayList();
        NST subgObjectNST = getSubgObjectNST();
        ResultSet resultSet = subgObjectNST.selectRows("item_id, name");
        while (resultSet.next()) {
            int oid = resultSet.getOID(1);
            String name = resultSet.getString(2);
            ProxItem newItem = new ProxObj(oid, name);
            itemList.add(newItem);
        }
        subgObjectNST.release();
        return itemList;
    }

    public int getObjCount() {
        return getItemsCount(true);
    }


    /**
     * Returns the parent container
     *
     * @return
     */
    public Container getParentContainer() {
        return parentContainer;
    }


    /**
     * Return the subgraph OID
     *
     * @return
     */
    public int getSubgID() {
        return subgID;
    }


    /**
     * Returns the NST that contains the links in the subgraph,
     * with the corresponding filter.
     * Called should release
     *
     * @return
     * @
     */
    public NST getSubgLinkNST() {
        return getCoreLinkNST().filter("subg_id = " + subgID);
    }


    /**
     * Returns the NST that contains the objects in the subgraph,
     * with the corresponding filter
     * Caller should release
     *
     * @return
     * @
     */
    public NST getSubgObjectNST() {
        return getCoreObjectNST().filter("subg_id = " + subgID);
    }

    /**
     * Adds a link to this subgraph
     *
     * @param linkOID
     * @param name
     */
    public void insertLink(int linkOID, String name) {
        NST linkNST = getCoreLinkNST();
        linkNST.insertRow(new String[]{linkOID + "", subgID + "", name});
    }


    /**
     * Adds an object to this subgraph
     *
     * @param objOID
     * @param name
     */
    public void insertObject(int objOID, String name) {
        NST objNST = getCoreObjectNST();
        objNST.insertRow(new String[]{objOID + "", subgID + "", name});
    }


    /**
     * Removes the specified object from this subgraph.  Does not
     * check for any links that might connect to this object - assumes the
     * user is taking care of ensuring that the subgraph is connected.
     *
     * @param oid
     */
    public void removeObject(int oid) {
        getCoreObjectNST().deleteRows("subg_id = " + subgID + " AND item_id = " + oid);
    }


    /**
     * Removes the specified object from this subgraph.  Does not
     * check for any links that might connect to this object - assumes the
     * user is taking care of ensuring that the subgraph is connected.
     *
     * @param oid
     */
    public void removeObject(int oid, String name) {
        getCoreObjectNST().deleteRows("subg_id = " + subgID + " AND item_id = " + oid + " AND name = '" + name + "'");
    }


    /**
     * Removes the specified link from this subgraph.  Does not check for any
     * objects that might connect to this object - assumes the user is taking
     * care of ensuring the subgraph is connected.
     *
     * @param oid
     * @
     */
    public void removeLink(int oid) {
        getCoreLinkNST().deleteRows("subg_id = " + subgID + " AND item_id = " + oid);
    }
}
