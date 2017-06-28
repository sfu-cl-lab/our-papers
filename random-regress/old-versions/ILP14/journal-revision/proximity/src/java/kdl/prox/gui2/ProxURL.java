/**
 * $Id: ProxURL.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ProxURL.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a Proximity URL. The sytax is:
 * <pre>
 *  &lt;prox-url&gt; -&gt; &lt;protocol&gt; ':' &lt;address&gt; ['!' &lt;parameter&gt;] ['#' &lt;pageNum&gt;]
 *  &lt;protocol&gt; -&gt; 'attr' | 'attrdefs | 'cont' | 'item' | 'subg'
 *  &lt;address&gt; -&gt; '/' | ['/' &lt;component&gt;]*
 *  &lt;parameter&gt; -&gt; chars
 *  </pre>
 * For example, this prox URL:
 * <pre>
 *  'subg:/containers/c1/1!attrvals?newpage#2'
 * </pre>
 * has these components:
 * <pre>
 *      protocol              : 'subg'
 *      address               : '/containers/c1/1'
 *      parameter             : 'attrvals'
 *      pageNum               : 2
 * </pre>
 * Note: URLs can have multiple colons, but the first one is used to delimit the
 * protocol. This means containers should *not* have '/' chars in their names.
 * Note: This means that container names should *not* include '!' (for parameters)
 * <p/>
 * todo enforce/document!
 * <p/>
 * We have decided to use the following protocols and paramenters to name
 * entities in a Proximity database. (By <i>entity</i> we mean an item -- object
 * or link --, a container, a subgraph, a set of attribute definitions, or a
 * particular attribute.)
 * <p/>
 * Protocols:
 * <ul>
 * See below -- PROTOCOLS instance variable
 * </ul>
 * Parameters:
 * <ul>
 * <li>...!attrvals</li>
 * <li>...(others)</li>
 * </ul>
 * Thus we have the following ways to uniquely name all proximity entities:
 * <pre>
 * an object  : item:/objects/0        (add !attrvals to get its attr vals)
 * a link     : item:/links/0           ""
 * a container: cont:/containers/c1/0   ""
 * a subgraph : subg:/containers/c1/0   ""
 * <p/>
 * obj attrs  : attrdefs:/objects
 * link attrs : attrdefs:/links
 * cont attrs : attrdefs:/containers
 * subg attrs : attrdefs:/containers/c1
 * <p/>
 * an obj attr: attr:/objects/a1       (add !histogram to get its histogram)
 * a link attr: attr:/links/a1          ""
 * a cont attr: attr:/containers/a1     ""
 * a subg attr: attr:/containers/c1/a1  ""
 * </pre>
 */
public class ProxURL {

    private static final List PROTOCOLS = new ArrayList();
    private static Logger log = Logger.getLogger(ProxURL.class);

    static {
        PROTOCOLS.add("attr");
        PROTOCOLS.add("attrdefs");
        PROTOCOLS.add("db");
        PROTOCOLS.add("cont");
        PROTOCOLS.add("filter");
        PROTOCOLS.add("item");
        PROTOCOLS.add("query");
        PROTOCOLS.add("script");
        PROTOCOLS.add("subg");
    }

    private String protocol;
    private String address;
    private String parameter;
    private String pageNum;


    public ProxURL(String proxURL) {
        // recall the syntax:
        //  <prox-url> -> <protocol> ':' <address> ['!' <parameter>] ['#' <pageNum>]
        //  <protocol> -> 'attr' | 'attrdefs | 'cont' | ... (see PROTOCOLS IV above)
        //  <address> -> '/' | ['/' <component>]*
        //  <parameter> -> chars
        //  <pageNum> -> int
        //
        // ex: "cont:/containers/movie stars 2d: us 96-01, window 90-01!param#2"

        // from proxURL remove required protocol from start, saving into
        // protocol and addressParamPageNum
        int firstColonIdx = proxURL.indexOf(':');
        Assert.condition(firstColonIdx != -1, "invalid proxURL: no colon: '" +
                proxURL + "'");
        protocol = proxURL.substring(0, firstColonIdx);
        Assert.condition(PROTOCOLS.contains(protocol),
                "unrecognized protocol: '" + protocol + "'");
        String addressParamPageNum = proxURL.substring(firstColonIdx + 1);

        // from addressParamPageNum remove optional pageNum from end,
        // saving into pageNum and addressParam
        String addressParam;
        int lastPoundIdx = addressParamPageNum.lastIndexOf('#');
        if (lastPoundIdx == -1) {
            pageNum = null;
            addressParam = addressParamPageNum;
        } else {
            pageNum = addressParamPageNum.substring(lastPoundIdx + 1);
            addressParam = addressParamPageNum.substring(0, lastPoundIdx);
        }

        // from addressParam remove optional parameter from end, saving into
        // parameter and address
        int lastBangIdx = addressParam.lastIndexOf('!');
        if (lastBangIdx == -1) {
            parameter = null;
            address = addressParam;
        } else {
            parameter = addressParam.substring(lastBangIdx + 1);
            address = addressParam.substring(0, lastBangIdx);
        }
        Assert.condition(address.startsWith("/"), "address didn't start with " +
                "'/': '" + address + "'");
        if (!"/".equals(address)) {
            Assert.condition(!address.endsWith("/"), "address ended with " +
                    "'/': '" + address + "'");
        }
    }

    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof ProxURL) {
            ProxURL anotherItem = (ProxURL) anObject;
            return (protocol.equals(anotherItem.getProtocol()) &&
                    address.equals(anotherItem.getAddress()) &&
                    parameter == null ? anotherItem.getParameter() == null : parameter.equals(anotherItem.getParameter()) &&
                    getPageNum() == anotherItem.getPageNum());
        }
        return false;
    }


    public String getAddress() {
        return address;
    }

    /**
     * @return String[] of my address split by '/'
     */
    public String[] getAddressComponents() {
        return address.split("/");
    }

    public String getAddressSansLastComponent() {
        int lastSlashIdx = address.lastIndexOf('/');
        return address.substring(0, lastSlashIdx);
    }

    /**
     * @param isIgnoreLastComponent true if my address's last component should
     *                              not be used in looking up the Attributes
     * @return Attributes corresponding to my address. ignores my protocol
     * @
     */
    Attributes getAttributes(boolean isIgnoreLastComponent) {
        String addrToUse = (isIgnoreLastComponent ? getAddressSansLastComponent()
                : address);
        Attributes attributes;
        if ("/containers".equals(addrToUse)) {                // container
            attributes = DB.getContainerAttrs();
        } else if (addrToUse.startsWith("/containers/")) {    // subgraph
            Container container = getContainer(isIgnoreLastComponent);
            attributes = container.getSubgraphAttrs();
        } else if ("/links".equals(addrToUse)) {              // links
            attributes = DB.getLinkAttrs();
        } else if ("/objects".equals(addrToUse)) {            // objects
            attributes = DB.getObjectAttrs();
        } else {
            throw new IllegalArgumentException("unknown address type: '" +
                    addrToUse + "'");
        }
        return attributes;
    }

    /**
     * NB: applies only to 'cont' protocol.
     *
     * @param isIgnoreLastComponent true if my address's last component should
     *                              not be used in looking up the Container
     * @return Container corresponding to my address
     * @
     */
    public Container getContainer(boolean isIgnoreLastComponent) {
        String addrToUse = (isIgnoreLastComponent ? getAddressSansLastComponent()
                : address);
        Container container = null;
        String[] components = addrToUse.split("/");
        for (int compIdx = 1; compIdx < components.length; compIdx++) {
            String component = components[compIdx];
            if (compIdx == 1) {
                if (component.equals("containers")) {
                    container = DB.getRootContainer();
                } else {
                    break;
                }
            } else {
                container = container.getChild(component);
                if (container == null) {
                    break;
                }
            }
        }
        if (container == null) {
            throw new IllegalArgumentException("no container for address: '" +
                    addrToUse + "'");
        }
        return container;
    }

    /**
     * Returns a list of four strings: "objects|links", attrName, colName, attrValue
     *
     * @return
     */
    public List getFilterSpec() {
        ArrayList specList = new ArrayList();
        String[] addressComponents = getAddressComponents();
        specList.add(addressComponents[1]);
        specList.add(addressComponents[2]);
        specList.add(addressComponents[3]);
        int firstSlashIdx = address.indexOf('/');
        int secondSlashIdx = address.indexOf('/', firstSlashIdx + 1);
        int thirdSlashIdx = address.indexOf('/', secondSlashIdx + 1);
        int fourthSlashIdx = address.indexOf('/', thirdSlashIdx + 1);
        specList.add(address.substring(fourthSlashIdx + 1));
        return specList;
    }

    /**
     * @return portion of address after first '/'. returns null if none
     */
    public String getFirstAddressComponent() {
        String[] components = getAddressComponents();
        if (components.length == 0) {
            return null;
        } else {
            return components[1];
        }
    }

    /**
     * @return last component as an int. throws NumberFormatException if it's
     *         not an integer
     */
    public int getItemOID() {
        int lastSlashIdx = address.lastIndexOf('/');
        return Integer.parseInt(address.substring(lastSlashIdx + 1));
    }

    /**
     * @return portion of address after last '/'. returns null if none
     */
    public String getLastAddressComponent() {
        String[] components = getAddressComponents();
        if (components.length == 0) {
            return null;
        } else {
            return components[components.length - 1];
        }
    }

    public int getPageNum() {
        return (pageNum == null ? 1 : Integer.parseInt(pageNum));
    }

    public String getParameter() {
        return parameter;
    }

    public String getProtocol() {
        return protocol;
    }

    /**
     * NB: applies only to 'subg' protocol.
     *
     * @return Subgraph corresponding to my address
     * @
     */
    Subgraph getSubgraph() {
        String subgId = getLastAddressComponent();
        Container container = getContainer(true);
        Assert.notNull(container, "Container doesn't exist: " + address);
        int subgIdInt = Integer.parseInt(subgId);
        return container.getSubgraph(subgIdInt);
    }

    public ProxURL getURLForPageNum(int newPageNum) {
        String newURL = protocol + ":" + address +
                (parameter == null ? "" : "!" + parameter) +
                "#" + newPageNum;
        return new ProxURL(newURL);
    }

    public int hashCode() {
        return protocol.hashCode() +
                address.hashCode() +
                (parameter == null ? 0 : parameter.hashCode()) +
                getPageNum();
    }

    private boolean isAttrsOfType(String type) {
        String[] addressComponents = getAddressComponents();
        return (addressComponents.length >= 1 && type != null &&
                "attr".equals(protocol) &&
                type.equals(addressComponents[1]));
    }

    public boolean isContainerAttrs() {
        return isAttrsOfType("containers");
    }

    public boolean isLinkAttrs() {
        return isAttrsOfType("links");
    }

    public boolean isObjectAttrs() {
        return isAttrsOfType("objects");
    }

    /**
     * @return reconstructed url <em>without</em> parameter.
     */
    public String toString() {
        // return protocol + ":" + address;
        return protocol + ":" + address +
                (parameter == null ? "" : "!" + parameter) +
                (pageNum == null ? "" : "#" + pageNum);
    }


}
