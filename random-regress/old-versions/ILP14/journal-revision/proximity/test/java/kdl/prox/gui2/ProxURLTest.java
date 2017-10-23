/**
 * $Id: ProxURLTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ProxURLTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;


public class ProxURLTest extends TestCase {

    private Container c1;
    private Container c2;
    private Container c3;
    private int s1;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
        c1 = rootContainer.createChild("c1");
        s1 = 1;
        c2 = rootContainer.createChild("c2");
        c3 = c1.createChild("c3");
    }


    private void createTestAttrs(Attributes attributes, int oid)  {
        attributes.deleteAllAttributes();
        attributes.defineAttribute("z1", "int");    // defined first, but last alphabetically
        attributes.defineAttribute("a1", "str");
        attributes.defineAttribute("a2", "v1:int, v2:str");

        NST attrDataNST = attributes.getAttrDataNST("z1");

        attrDataNST = attributes.getAttrDataNST("a1");
        attrDataNST.insertRow(new String[]{oid + "", "val1"});
        attrDataNST.insertRow(new String[]{oid + "", "val2"});

        attrDataNST = attributes.getAttrDataNST("a2");
        attrDataNST.insertRow(new String[]{oid + "", "1", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val2"});
        attrDataNST.insertRow(new String[]{oid + "", "2", "val1"});
    }


    public void testBadAddress() {
        try {
            new ProxURL("cont:");   // doesn't start with '/'
            fail("missed bad address");
        } catch (IllegalArgumentException iaExc) {
            // ignore
        }

        try {
            new ProxURL("cont:/a/");   // shouldn't end with '/'
            fail("missed bad address");
        } catch (IllegalArgumentException iaExc) {
            // ignore
        }
    }


    public void testBadProtocol() {
        try {
            new ProxURL("xx:/");
            fail("missed bad protocol");
        } catch (IllegalArgumentException iaExc) {
            // ignore
        }
    }


    public void testGetAttributes()  {
        createTestAttrs(DB.getLinkAttrs(), 0);
        createTestAttrs(DB.getContainerAttrs(), c1.getOid());

        Attributes attrs = new ProxURL("db:/objects").getAttributes(false);
        assertEquals(DB.getObjectAttrs(),attrs);

        attrs = new ProxURL("db:/objects/a1").getAttributes(true);
        assertEquals(DB.getObjectAttrs(), attrs);

        attrs = new ProxURL("db:/links").getAttributes(false);
        assertEquals(DB.getLinkAttrs(), attrs);

        attrs = new ProxURL("db:/containers").getAttributes(false);
        assertEquals(DB.getContainerAttrs(), attrs);

        createTestAttrs(c1.getSubgraphAttrs(), s1);
        attrs = new ProxURL("cont:/containers/c1").getAttributes(false);
        assertEquals(c1.getSubgraphAttrs(), attrs);
    }


    public void testGetContainerFromURL()  {
        Container container = new ProxURL("db:/containers").getContainer(false);
        assertEquals(DB.getRootContainer().getOid(), container.getOid());

        container = new ProxURL("db:/containers/c1").getContainer(false);
        assertEquals(c1.getOid(), container.getOid());

        container = new ProxURL("db:/containers/c2").getContainer(false);
        assertEquals(c2.getOid(), container.getOid());

        container = new ProxURL("db:/containers/c1/c3").getContainer(false);
        assertEquals(c3.getOid(), container.getOid());
    }


    public void testGetContainerBadURL()  {
        try {
            new ProxURL("db:/containers/xx").getContainer(false);
            fail("should have thrown IllegalArgumentException for non-container url");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }


    public void testGetFirstAddrComp() {
        String firstComponent = new ProxURL("cont:/a/b").getFirstAddressComponent();
        assertEquals("a", firstComponent);

        firstComponent = new ProxURL("db:/").getFirstAddressComponent();
        assertEquals(null, firstComponent);
    }


    public void testGetLastAddrComp() {
        ProxURL proxURL = new ProxURL("cont:/a/b");
        String lastComponent = proxURL.getLastAddressComponent();
        assertEquals("b", lastComponent);
    }


    public void testGetURLForPageNum() {
        ProxURL proxURL = new ProxURL("cont:/a/b");
        ProxURL expectedURL = new ProxURL("cont:/a/b#1");
        assertEquals(expectedURL, proxURL.getURLForPageNum(1));

        proxURL = new ProxURL("cont:/a/b#4");
        expectedURL = new ProxURL("cont:/a/b#1");
        assertEquals(expectedURL, proxURL.getURLForPageNum(1));

        proxURL = new ProxURL("cont:/a/b!p#4");
        expectedURL = new ProxURL("cont:/a/b!p#1");
        assertEquals(expectedURL, proxURL.getURLForPageNum(1));

        proxURL = new ProxURL("cont:/a/b?a#4");
        expectedURL = new ProxURL("cont:/a/b?a#1");
        assertEquals(expectedURL, proxURL.getURLForPageNum(1));

        proxURL = new ProxURL("cont:/a/b!p?a#4");
        expectedURL = new ProxURL("cont:/a/b!p?a#1");
        assertEquals(expectedURL, proxURL.getURLForPageNum(1));
    }


    public void testGoodProtocols() {
        new ProxURL("attr:/");
        new ProxURL("attrdefs:/");
        new ProxURL("cont:/");
        new ProxURL("db:/");
        new ProxURL("filter:/");
        new ProxURL("item:/");
        new ProxURL("query:/");
        new ProxURL("script:/");
        new ProxURL("subg:/");
    }


    public void testNoParamNoPageNum() {
        ProxURL proxURL = new ProxURL("cont:/a/b");
        assertEquals("cont", proxURL.getProtocol());
        assertEquals("/a/b", proxURL.getAddress());
        assertEquals(null, proxURL.getParameter());
        assertEquals(1, proxURL.getPageNum());
    }


    public void testNoParamYesPageNum() {
        ProxURL proxURL = new ProxURL("cont:/a/b#2");
        assertEquals("cont", proxURL.getProtocol());
        assertEquals("/a/b", proxURL.getAddress());
        assertEquals(null, proxURL.getParameter());
        assertEquals(2, proxURL.getPageNum());
    }

    public void testSimplestAllProtocols() {
        new ProxURL("attr:/");
        new ProxURL("attrdefs:/");
        new ProxURL("cont:/");
        new ProxURL("item:/");
        new ProxURL("subg:/");
    }


    public void testToString() {
        String proxURLStr = "cont:/a/b";
        assertEquals(proxURLStr, new ProxURL(proxURLStr).toString());

        proxURLStr = "cont:/a/b?a";
        assertEquals(proxURLStr, new ProxURL(proxURLStr).toString());

        proxURLStr = "cont:/a/b!p";
        assertEquals(proxURLStr, new ProxURL(proxURLStr).toString());

        proxURLStr = "cont:/a/b!p?a";
        assertEquals(proxURLStr, new ProxURL(proxURLStr).toString());

        proxURLStr = "cont:/a/b!p?a#2";
        assertEquals(proxURLStr, new ProxURL(proxURLStr).toString());

    }


    public void testYesParamNoPageNum() {
        ProxURL proxURL = new ProxURL("cont:/a/b!p");
        assertEquals("cont", proxURL.getProtocol());
        assertEquals("/a/b", proxURL.getAddress());
        assertEquals("p", proxURL.getParameter());
        assertEquals(1, proxURL.getPageNum());
    }


    public void testYesParamYesPageNum() {
        ProxURL proxURL = new ProxURL("cont:/a/b!p#2");
        assertEquals("cont", proxURL.getProtocol());
        assertEquals("/a/b", proxURL.getAddress());
        assertEquals("p", proxURL.getParameter());
        assertEquals(2, proxURL.getPageNum());
    }

}
