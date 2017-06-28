/**
 * $Id: ShrinkDBTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import java.util.HashSet;
import java.util.List;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;


/**
 * Tests ShrinkDB.shrinkDB().
 *
 * @see ShrinkDB#shrinkDB
 */
public class ShrinkDBTest extends TestCase {

    private static Logger log = Logger.getLogger(ShrinkDBTest.class);
    private static final String OBJ_ATTR = "object_type";
    private static final String LINK_ATTR = "link_type";
    private static final String KEEP_CONTAINER = "keep_container";

    /**
     * Database:
     * <pre>
     *  0 -> 1 -> 2  } objects
     *     0    1    } links
     * </pre>
     * <p/>
     * obj attrs: object_type (str)
     * 0: 'obj'
     * 1: 'obj'
     * 2: 'obj'
     * <p/>
     * link attrs: link_type (str)
     * 0: 'link'
     * 1: 'link'
     * <p/>
     * keep_container:
     * O: 0 'keep'
     * L: 1 'keep'
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        DB.getObjectNST().deleteRows();
        DB.insertObject(0);
        DB.insertObject(1);
        DB.insertObject(2);

        DB.getLinkNST().deleteRows();
        DB.insertLink(0, 0, 1);
        DB.insertLink(1, 1, 2);

        Attributes attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute(OBJ_ATTR, "str");
        NST attrNST = attrs.getAttrDataNST(OBJ_ATTR);
        attrNST.insertRow(new String[]{"0", "obj"});
        attrNST.insertRow(new String[]{"1", "obj"});

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute(LINK_ATTR, "str");
        attrNST = attrs.getAttrDataNST(LINK_ATTR);
        attrNST.insertRow(new String[]{"0", "link"});
        attrNST.insertRow(new String[]{"1", "link"});


        Container rootCont = DB.getRootContainer();
        rootCont.deleteAllChildren();
        Container keepCont = rootCont.createChild(KEEP_CONTAINER);

        NST objectNST = keepCont.getItemNST(true);
        objectNST.insertRow(new String[]{"0", "0", "keep"});    // item_id, subg_id, name

        NST linkNST = keepCont.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "0", "keep"});      // item_id, subg_id, name
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testShrinkDB() {
        Container keepCont = DB.getRootContainer().getChild(KEEP_CONTAINER);
        ShrinkDB.shrinkDB(keepCont);
        verifyObjects();
        verifyLinks();
        verifyAttributes();
    }

    private void verifyAttributes() {
        // test the object attribute
        Attributes attrs = DB.getObjectAttrs();
        NST attrNST = attrs.getAttrDataNST(OBJ_ATTR);   // id, value
        ResultSet resultSet = attrNST.selectRows();
        List actObjIDList = resultSet.toStringList(1);

        Container keepCont = DB.getRootContainer().getChild(KEEP_CONTAINER);
        resultSet = keepCont.getObjectsNST().selectRows();
        List expObjIDList = resultSet.toStringList(1);

        TestUtil.verifyCollections(expObjIDList, new HashSet(actObjIDList));

        // test the link attribute
        attrs = DB.getLinkAttrs();
        attrNST = attrs.getAttrDataNST(LINK_ATTR);      // id, value
        resultSet = attrNST.selectRows();
        List actLinkIDList = resultSet.toStringList(1);

        resultSet = keepCont.getLinksNST().selectRows();
        List expLinkIDList = resultSet.toStringList(1);

        TestUtil.verifyCollections(expLinkIDList, new HashSet(actLinkIDList));
    }

    private void verifyLinks() {
        NST linkNST = DB.getLinkNST();       // link_id, o1_id, o2_id
        ResultSet resultSet = linkNST.selectRows();
        List actLinkIDList = resultSet.toStringList(1);

        Container keepCont = DB.getRootContainer().getChild(KEEP_CONTAINER);
        resultSet = keepCont.getLinksNST().selectRows();
        List expLinkIDList = resultSet.toStringList(1);

        TestUtil.verifyCollections(expLinkIDList, new HashSet(actLinkIDList));
    }

    private void verifyObjects() {
        NST objectNST = DB.getObjectNST();   // id
        ResultSet resultSet = objectNST.selectRows();
        List actObjIDList = resultSet.toStringList(1);

        Container keepCont = DB.getRootContainer().getChild(KEEP_CONTAINER);
        resultSet = keepCont.getObjectsNST().selectRows();
        List expObjIDList = resultSet.toStringList(1);

        TestUtil.verifyCollections(expObjIDList, new HashSet(actObjIDList));
    }

}
