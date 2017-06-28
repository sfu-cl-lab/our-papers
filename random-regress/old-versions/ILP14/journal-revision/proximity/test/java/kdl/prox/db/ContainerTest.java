/**
 * $Id: ContainerTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ContainerTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.db;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.monet.ResultSet;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Tests the Container class.
 *
 * @see Container
 */
public class ContainerTest extends TestCase {

    private Logger log = Logger.getLogger(ContainerTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        log.debug("Setting up ContainerTest");

        TestUtil.openTestConnection();
        Connection.beginScope();
        Container rootContainer = DB.getRootContainer();
        rootContainer.deleteAllChildren();
    }

    private Container createTestContainer() {
        Container rootContainer = DB.getRootContainer();
        // a container with some objs and links
        Container childContA = rootContainer.createChild("a");
        NST objectNST = childContA.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "0", "A"});
        objectNST.insertRow(new String[]{"2", "1", "B"});
        objectNST.insertRow(new String[]{"3", "2", "C"});
        NST linkNST = childContA.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "X"});
        linkNST.insertRow(new String[]{"1", "2", "X"});
        childContA.getSubgraphAttrs().deleteAllAttributes();
        childContA.getSubgraphAttrs().defineAttribute("test-1", "str");
        childContA.getSubgraphAttrs().getAttrDataNST("test-1").insertRow(new String[]{"0", "a"});
        childContA.getSubgraphAttrs().getAttrDataNST("test-1").insertRow(new String[]{"1", "x"});

        // some objects with attributes
        DB.getObjectNST().deleteRows();
        DB.getObjectNST().insertRow(new String[]{"1"});
        DB.getObjectNST().insertRow(new String[]{"3"});
        DB.getObjectNST().insertRow(new String[]{"2"});

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("test-1", "str");
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"1", "a"});
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"2", "b"});
        DB.getObjectAttrs().getAttrDataNST("test-1").insertRow(new String[]{"3", "c"});

        DB.getObjectAttrs().defineAttribute("test-2", "str");
        DB.getObjectAttrs().getAttrDataNST("test-2").insertRow(new String[]{"1", "x"});
        DB.getObjectAttrs().getAttrDataNST("test-2").insertRow(new String[]{"2", "y"});

        DB.getObjectAttrs().defineAttribute("test-3", "str");
        DB.getObjectAttrs().getAttrDataNST("test-3").insertRow(new String[]{"1", "m"});
        DB.getObjectAttrs().getAttrDataNST("test-3").insertRow(new String[]{"2", "n"});

        // some links with attributes
        DB.getLinkNST().deleteRows();
        DB.getLinkNST().insertRow(new String[]{"1", "0", "1"});
        DB.getLinkNST().insertRow(new String[]{"3", "0", "2"});
        DB.getLinkNST().insertRow(new String[]{"2", "1", "0"});

        DB.getLinkAttrs().deleteAllAttributes();
        DB.getLinkAttrs().defineAttribute("test-1", "str");
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"1", "a"});
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"2", "b"});
        DB.getLinkAttrs().getAttrDataNST("test-1").insertRow(new String[]{"3", "c"});

        DB.getLinkAttrs().defineAttribute("test-2", "str");
        DB.getLinkAttrs().getAttrDataNST("test-2").insertRow(new String[]{"1", "x"});
        DB.getLinkAttrs().getAttrDataNST("test-2").insertRow(new String[]{"2", "y"});

        DB.getLinkAttrs().defineAttribute("test-n", "str");
        DB.getLinkAttrs().getAttrDataNST("test-n").insertRow(new String[]{"1", "x"});

        return childContA;
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }


    public void testChangeItemName() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        // item_id, subgraph_id, name
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});
        Container origContainer = rootContainer.createChildFromTempSGINSTs("root-child-0",
                objTempSGINST, linkTempSGINST);

        origContainer.changeItemName("A", "Z", true);

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "Z"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "Z"});
        expObjTempSGINST.insertRow(new String[]{"2", "3", "B"});

        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "5", "X"});

        // compare them
        NST actObjTempSGINST = origContainer.getItemNST(true);
        NST actLinkTempSGINST = origContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

    }

    public void testCopySubgraphs() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});

        Container origContainer = rootContainer.createChildFromTempSGINSTs("root-child-0",
                objTempSGINST, linkTempSGINST);
        Container copyContainer = rootContainer.createChildFromTempSGINSTs("root-child-1",
                objTempSGINST, linkTempSGINST);

        // do the copy
        origContainer.copySubgraphsFromContainer(copyContainer);

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "3", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "7", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "7", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "8", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "9", "B"});

        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"1", "7", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "8", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "11", "X"});

        // compare them
        NST actObjTempSGINST = origContainer.getItemNST(true);
        NST actLinkTempSGINST = origContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        assertEquals(8, origContainer.getSubgraphCount());
        assertEquals(8, origContainer.getItemNST(true).getRowCount());
        assertEquals(6, origContainer.getItemNST(false).getRowCount());
        List subgIds = origContainer.getSubgraphOIDs();
        assertEquals(8, subgIds.size());
        assertTrue(subgIds.contains(new Integer(1)));
        assertTrue(subgIds.contains(new Integer(2)));
        assertTrue(subgIds.contains(new Integer(3)));
        assertTrue(subgIds.contains(new Integer(5)));
        assertTrue(subgIds.contains(new Integer(7)));
        assertTrue(subgIds.contains(new Integer(8)));
        assertTrue(subgIds.contains(new Integer(9)));
        assertTrue(subgIds.contains(new Integer(11)));
    }

    public void testCopySubgraphsOntoEmptyContainer() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});

        Container origContainer = rootContainer.createChild("root-child-0");
        Container copyContainer = rootContainer.createChildFromTempSGINSTs("root-child-1",
                objTempSGINST, linkTempSGINST);

        // do the copy
        origContainer.copySubgraphsFromContainer(copyContainer);

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "3", "B"});

        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "5", "X"});

        // compare them
        NST actObjTempSGINST = origContainer.getItemNST(true);
        NST actLinkTempSGINST = origContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        assertEquals(4, origContainer.getSubgraphCount());
        assertEquals(4, origContainer.getItemNST(true).getRowCount());
        assertEquals(3, origContainer.getItemNST(false).getRowCount());
        List subgIds = origContainer.getSubgraphOIDs();
        assertEquals(4, subgIds.size());
        assertTrue(subgIds.contains(new Integer(1)));
        assertTrue(subgIds.contains(new Integer(2)));
        assertTrue(subgIds.contains(new Integer(3)));
        assertTrue(subgIds.contains(new Integer(5)));
    }

    public void testCopySubgraphsWithAttributes() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});

        Container origContainer = rootContainer.createChildFromTempSGINSTs("root-child-0",
                objTempSGINST, linkTempSGINST);
        origContainer.getSubgraphAttrs().defineAttribute("name", "str");
        NST origContAttrsDataNST = origContainer.getSubgraphAttrs().getAttrDataNST("name");
        origContAttrsDataNST.insertRow(new String[]{"1", "a"});
        origContAttrsDataNST.insertRow(new String[]{"5", "b"});
        origContAttrsDataNST.insertRow(new String[]{"3", "c"});
        origContAttrsDataNST.release();
        Container copyContainer = rootContainer.createChildFromTempSGINSTs("root-child-1",
                objTempSGINST, linkTempSGINST);
        copyContainer.getSubgraphAttrs().defineAttribute("name", "str");
        NST copyContAttrsDataNST = copyContainer.getSubgraphAttrs().getAttrDataNST("name");
        copyContAttrsDataNST.insertRow(new String[]{"1", "a"});
        copyContAttrsDataNST.insertRow(new String[]{"5", "b"});
        copyContAttrsDataNST.release();

        // do the copy
        origContainer.copySubgraphsFromContainer(copyContainer);
        assertEquals(8, origContainer.getSubgraphCount());

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "3", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "6", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "6", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "7", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "8", "B"});

        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "5", "X"});
        expLinkTempSGINST.insertRow(new String[]{"1", "6", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "7", "Y"});
        expLinkTempSGINST.insertRow(new String[]{"1", "10", "X"});

        // compare them
        NST actObjTempSGINST = origContainer.getItemNST(true);
        NST actLinkTempSGINST = origContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        // check the attributes
        NST newAttrsDataNST = origContainer.getSubgraphAttrs().getAttrDataNST("name");
        assertEquals(5, newAttrsDataNST.getRowCount());
        Map valsMap = new HashMap();
        ResultSet rs = newAttrsDataNST.selectRows();
        while (rs.next()) {
            int oid = rs.getOID(1);
            String val = rs.getString(2);
            valsMap.put(new Integer(oid), val);
        }
        String val = (String) valsMap.get(new Integer(1));
        assertTrue(val != null && val.equals("a"));
        val = (String) valsMap.get(new Integer(5));
        assertTrue(val != null && val.equals("b"));
        val = (String) valsMap.get(new Integer(3));
        assertTrue(val != null && val.equals("c"));
        val = (String) valsMap.get(new Integer(7));
        assertTrue(val != null && val.equals("a"));
        val = (String) valsMap.get(new Integer(11));
        assertTrue(val != null && val.equals("b"));
        newAttrsDataNST.release();
    }

    public void testCopySubgraphsWithFilter() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        // item_id, subgraph_id, name
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});
        Container origContainer = rootContainer.createChildFromTempSGINSTs("root-child-0",
                objTempSGINST, linkTempSGINST);

        // put some attributes on the subgraphs
        origContainer.getSubgraphAttrs().defineAttribute("names", "str");
        NST origContAttrsDataNST = origContainer.getSubgraphAttrs().getAttrDataNST("names");
        origContAttrsDataNST.insertRow(new String[]{"1", "a"});
        origContAttrsDataNST.insertRow(new String[]{"5", "b"});
        origContAttrsDataNST.insertRow(new String[]{"3", "c"});
        origContAttrsDataNST.release();

        // do the copy
        // test copying only subgraphs 1 & 2
        Container copyContainer = rootContainer.createChild("root-child-1");
        NST toCopyIDs = new NST("subg_id", "oid").insertRow(new String[]{"1"}).insertRow(new String[]{"2"});

        int numCopied = copyContainer.copySubgraphsFromContainer(origContainer, toCopyIDs);
        assertEquals(2, numCopied);
        assertEquals(2, copyContainer.getSubgraphCount());

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "B"});
        expObjTempSGINST.insertRow(new String[]{"1", "2", "A"});

        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});

        // compare them
        NST actObjTempSGINST = copyContainer.getItemNST(true);
        NST actLinkTempSGINST = copyContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        // check the attributes
        NST newAttrsDataNST = copyContainer.getSubgraphAttrs().getAttrDataNST("names");
        assertEquals(1, newAttrsDataNST.getRowCount());
        Map valsMap = new HashMap();
        ResultSet rs = newAttrsDataNST.selectRows();
        while (rs.next()) {
            int oid = rs.getOID(1);
            String val = rs.getString(2);
            valsMap.put(new Integer(oid), val);
        }
        String val = (String) valsMap.get(new Integer(1));
        assertTrue(val != null && val.equals("a"));
        newAttrsDataNST.release();
        toCopyIDs.release();
    }


    public void testCreateChild() {
        String childName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        rootContainer.createChild(childName);
        List childNames = rootContainer.getChildrenNames();
        assertEquals(1, childNames.size());
        assertTrue(childNames.contains(childName));
    }

    public void testCreateChildBadNames() {
        Container rootContainer = DB.getRootContainer();
        try {
            rootContainer.createChild("blah!blah");
            fail("No ! should be allowed in container name");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            rootContainer.createChild("blah?blah");
            fail("No ? should be allowed in container name");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            rootContainer.createChild("blah#blah");
            fail("No # should be allowed in container name");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    public void testCreateChildFromTempSGINSTs() {
        String childName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();

        // Insert the data
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        objTempSGINST.insertRow(new String[]{"2", "1", "B"});
        objTempSGINST.insertRow(new String[]{"1", "2", "A"});
        objTempSGINST.insertRow(new String[]{"2", "3", "B"});

        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        linkTempSGINST.insertRow(new String[]{"1", "5", "X"});


        Container container = rootContainer.createChildFromTempSGINSTs(childName,
                objTempSGINST, linkTempSGINST);

        try {
            List childNames = rootContainer.getChildrenNames();
            assertEquals(1, childNames.size());
            assertTrue(childNames.contains(childName));
            assertEquals(4, container.getSubgraphCount());

            Subgraph subg = container.getSubgraph(1);
            NST subgObjectNST = subg.getSubgObjectNST();
            NST subgLinkNST = subg.getSubgLinkNST();
            assertEquals(2, subgObjectNST.getRowCount());
            assertEquals(1, subgLinkNST.getRowCount());
            subgObjectNST.release();
            subgLinkNST.release();

            subg = container.getSubgraph(2);
            subgObjectNST = subg.getSubgObjectNST();
            subgLinkNST = subg.getSubgLinkNST();
            assertEquals(1, subgObjectNST.getRowCount());
            assertEquals(1, subgLinkNST.getRowCount());
            subgObjectNST.release();
            subgLinkNST.release();

            subg = container.getSubgraph(3);
            subgObjectNST = subg.getSubgObjectNST();
            subgLinkNST = subg.getSubgLinkNST();
            assertEquals(1, subgObjectNST.getRowCount());
            assertEquals(0, subgLinkNST.getRowCount());
            subgObjectNST.release();
            subgLinkNST.release();

            subg = container.getSubgraph(5);
            subgObjectNST = subg.getSubgObjectNST();
            subgLinkNST = subg.getSubgLinkNST();
            assertEquals(0, subgObjectNST.getRowCount());
            assertEquals(1, subgLinkNST.getRowCount());
            subgObjectNST.release();
            subgLinkNST.release();
        } catch (Exception e) {
        } finally {
            objTempSGINST.release();
            linkTempSGINST.release();
        }
    }


    public void testDeleteNotAChild() {
        String badChildName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        try {
            rootContainer.deleteChild(badChildName);
            fail("deleted a container that's not a child");
        } catch (Exception e) {
        }
    }

    public void testDeleteAllChildren() {
        Container rootContainer = DB.getRootContainer();
        rootContainer.createChild("root-child-0");
        rootContainer.createChild("root-child-1");
        assertEquals(2, rootContainer.getChildrenNames().size());
        rootContainer.deleteAllChildren();
        assertEquals(0, rootContainer.getChildrenNames().size());
    }

    public void testDeleteExistingChild() {
        String realChildName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        rootContainer.createChild(realChildName);
        String objBAT = rootContainer.getChild(realChildName).getSubgObjBatName();
        String linkBAT = rootContainer.getChild(realChildName).getSubgLinkBatName();
        String attrBAT = rootContainer.getChild(realChildName).getSubgAttrBatName();
        rootContainer.deleteChild(realChildName);
        List childNames = rootContainer.getChildrenNames();
        assertEquals(0, childNames.size());
        assertFalse(MonetUtil.isBATExists(objBAT));
        assertFalse(MonetUtil.isBATExists(linkBAT));
        assertFalse(MonetUtil.isBATExists(attrBAT));
    }

    public void testDeleteIsRecursive() {
        String realChildName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        rootContainer.createChild(realChildName);
        Container theChild = rootContainer.getChild(realChildName);
        theChild.createChild("child-child-0");
        rootContainer.deleteChild(realChildName);
        assertEquals(0, DB.getRootContainer().getChildrenCount());
    }

    public void testGetChildrenCount() {
        Container rootContainer = DB.getRootContainer();
        String firstChildName = "root-child-0";
        rootContainer.createChild(firstChildName);
        assertEquals(1, rootContainer.getChildrenCount());

        Container childContainer = rootContainer.getChild(firstChildName);
        assertEquals(0, childContainer.getChildrenCount());
    }

    public void testGetChildrenNST() {
        DB.beginScope();
        Container rootContainer = DB.getRootContainer();
        String firstChildName = "root-child-0";
        rootContainer.createChild(firstChildName);
        assertEquals(1, rootContainer.getChildrenNST().getRowCount());

        String secondChildName = "child-0-child";
        Container child = rootContainer.getChild(firstChildName);
        child.createChild(secondChildName);
        assertEquals(1, rootContainer.getChildrenNST().getRowCount());
        assertEquals(1, child.getChildrenNST().getRowCount());
        DB.endScope();
    }


    public void testGetLinks() {
        Container testContainer = createTestContainer();
        NST linksAndAttrs = testContainer.getLinks("*", "*");
        assertEquals(2, linksAndAttrs.getRowCount()); // duplicates
        assertEquals(6, linksAndAttrs.getNSTColumnNames().size());
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("item_id"));
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("subg_id"));
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("name"));
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("test-2"));
        assertTrue(linksAndAttrs.getNSTColumnNames().contains("test-n"));

        linksAndAttrs = testContainer.getLinks("test-n != nil");
        assertEquals(2, linksAndAttrs.getRowCount());
    }

    public void testGetObjects() {
        Container testContainer = createTestContainer();
        NST objsAndAttrs = testContainer.getObjects("*", "*");
        assertEquals(3, objsAndAttrs.getRowCount());
        assertEquals(6, objsAndAttrs.getNSTColumnNames().size());
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("item_id"));
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("subg_id"));
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("name"));
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("test-1"));
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("test-2"));
        assertTrue(objsAndAttrs.getNSTColumnNames().contains("test-3"));

        objsAndAttrs = testContainer.getObjects("test-3 != nil");
        assertEquals(2, objsAndAttrs.getRowCount());
    }

    public void testGetObjectsByName() {
        Container testContainer = createTestContainer();
        NST objsAndAttrs = testContainer.getObjectsNSTByName("A, B, X");
        assertEquals(2, objsAndAttrs.getRowCount());
    }


    public void testGetParent() {
        // create this hierarchy: root -> a -> b
        Container rootContainer = DB.getRootContainer();
        Container aContainer = rootContainer.createChild("a");
        Container bContainer = aContainer.createChild("b");

        // test b's parent = a
        Container bParentCont = bContainer.getParent();
        assertEquals(aContainer.getOid(), bParentCont.getOid());

        // test a's parent = root container
        Container aParentCont = aContainer.getParent();
        assertEquals(rootContainer.getOid(), aParentCont.getOid());

        // test root container's parent throws an exception
        try {
            rootContainer.getParent();
            fail("asking for root's parent should throw an exception");
        } catch (IllegalArgumentException iaExc) {
            // expected exception
        }
    }

    public void testGetUnique() {
        Container testContainer = createTestContainer();
        assertEquals(1, testContainer.getUniqueLinks().getRowCount()); // not duplicates
        assertEquals(3, testContainer.getUniqueObjects().getRowCount()); // not duplicates
    }

    public void testGetSubgraphOIDs() {
        Container rootContainer = DB.getRootContainer();
        Container sourceContainer = rootContainer.createChild("a");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "2", "B"});
        objectNST.insertRow(new String[]{"3", "3", "C"});
        objectNST.insertRow(new String[]{"4", "6", "D"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "Z"});
        linkNST.insertRow(new String[]{"2", "1", "Z"});
        linkNST.insertRow(new String[]{"3", "1", "Z"});
        linkNST.insertRow(new String[]{"5", "1", "Z"});
        linkNST.insertRow(new String[]{"6", "3", "Z"});

        List subgraphOIDs = sourceContainer.getSubgraphOIDs();
        assertEquals(4, subgraphOIDs.size());
        assertTrue(subgraphOIDs.contains(new Integer(1)));
        assertTrue(subgraphOIDs.contains(new Integer(2)));
        assertTrue(subgraphOIDs.contains(new Integer(3)));
        assertTrue(subgraphOIDs.contains(new Integer(6)));
    }

    public void testGetSubgraphsWithAttrs() {
        Container rootContainer = DB.getRootContainer();
        Container sourceContainer = rootContainer.createChild("a");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "2", "B"});
        objectNST.insertRow(new String[]{"3", "3", "C"});
        objectNST.insertRow(new String[]{"4", "6", "D"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "Z"});
        linkNST.insertRow(new String[]{"2", "1", "Z"});
        linkNST.insertRow(new String[]{"3", "1", "Z"});
        linkNST.insertRow(new String[]{"5", "1", "Z"});
        linkNST.insertRow(new String[]{"6", "3", "Z"});

        Attributes attributes = sourceContainer.getSubgraphAttrs();
        attributes.defineAttribute("test-1", "str");
        NST attrDataNST = attributes.getAttrDataNST("test-1");
        attrDataNST.insertRow("1, A").insertRow("2, b").insertRow("6, c");

        NST subgraphsWithAttrs = sourceContainer.getSubgraphs("*", "test-1");
        assertEquals(4, subgraphsWithAttrs.getRowCount());
        assertEquals(2, subgraphsWithAttrs.getNSTColumns().size());
        subgraphsWithAttrs.release();
        subgraphsWithAttrs = sourceContainer.getSubgraphs("test-1 != nil");
        assertEquals(3, subgraphsWithAttrs.getRowCount());
        subgraphsWithAttrs.release();
        subgraphsWithAttrs = sourceContainer.getSubgraphs("test-1 = nil");
        assertEquals(1, subgraphsWithAttrs.getRowCount());
        subgraphsWithAttrs.release();
        attrDataNST.release();

        // make sure that the NST is not released when no attributes are requested
        subgraphsWithAttrs = sourceContainer.getSubgraphs("*");
        assertFalse(subgraphsWithAttrs.isReleased());
        subgraphsWithAttrs.release();
    }

    public void testMakeChild() {
        Container rootContainer = DB.getRootContainer();
        Container sourceContainer = rootContainer.createChild("a");
        Container targetContainer = rootContainer.createChild("b");
        try {
            DB.getRootContainer().makeChildOf(sourceContainer);
            fail("Shouldn't allow making the root container a child of another");
        } catch (Exception e) {
            // ignore
        }

        targetContainer.makeChildOf(sourceContainer);
        assertEquals(sourceContainer.getOid(), targetContainer.getParent().getOid());
        assertTrue(sourceContainer.getChildrenNames().contains("b"));
        assertFalse(rootContainer.getChildrenNames().contains("b"));
        assertNotNull(DB.getContainer("a/b"));

        targetContainer.makeChildOf(rootContainer);
        assertEquals(rootContainer.getOid(), targetContainer.getParent().getOid());
        assertFalse(sourceContainer.getChildrenNames().contains("b"));
        assertTrue(rootContainer.getChildrenNames().contains("b"));
        try {
            assertNull(DB.getContainer("a/b"));
            fail("Should have thrown an exception");
        } catch (Exception e) {
            //ignore 
        }
    }


    public void testMergeContainers() {
        Container rootContainer = DB.getRootContainer();

        // Some test data, to be inserted in both containers
        NST objTempSGINST = SGIUtil.createTempSGINST();
        NST linkTempSGINST = SGIUtil.createTempSGINST();
        objTempSGINST.insertRow(new String[]{"1", "1", "A"});
        linkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        Container contOne = rootContainer.createChildFromTempSGINSTs("root-child-0",
                objTempSGINST, linkTempSGINST);
        objTempSGINST.release();
        linkTempSGINST.release();

        objTempSGINST = SGIUtil.createTempSGINST();
        linkTempSGINST = SGIUtil.createTempSGINST();
        objTempSGINST.insertRow(new String[]{"2", "2", "B"});
        linkTempSGINST.insertRow(new String[]{"2", "2", "Y"});
        Container contTwo = rootContainer.createChildFromTempSGINSTs("root-child-1",
                objTempSGINST, linkTempSGINST);

        Container destContainer = rootContainer.createChild("root-child-2");

        // do the merge
        Container.mergeContainers(Arrays.asList(new Object[]{contOne, contTwo}), destContainer);

        // make expected results and compare them
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "A"});
        expObjTempSGINST.insertRow(new String[]{"2", "2", "B"});
        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "X"});
        expLinkTempSGINST.insertRow(new String[]{"2", "2", "Y"});

        // compare them
        NST actObjTempSGINST = destContainer.getItemNST(true);
        NST actLinkTempSGINST = destContainer.getItemNST(false);
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();

        objTempSGINST.release();
        linkTempSGINST.release();
        expObjTempSGINST.release();
        expLinkTempSGINST.release();

        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        // do the merge again, with dest onto itself -- should be no change
        NST objNST = destContainer.getItemNST(true);
        NST linkNST = destContainer.getItemNST(false);
        int objRows = objNST.getRowCount();
        int linkRows = linkNST.getRowCount();
        objNST.release();
        linkNST.release();

        Container.mergeContainers(Arrays.asList(new Object[]{destContainer}), destContainer);

        objNST = destContainer.getItemNST(true);
        linkNST = destContainer.getItemNST(false);
        int newObjRows = objNST.getRowCount();
        int newLinkRows = linkNST.getRowCount();
        assertEquals(objRows, newObjRows);
        assertEquals(linkRows, newLinkRows);
        objNST.release();
        linkNST.release();
    }


    public void testName() {
        String childName = "root-child-0";
        Container rootContainer = DB.getRootContainer();
        Container childContainer = rootContainer.createChild(childName);
        assertEquals(childName, childContainer.getName());

        for (int charIdx = 0; charIdx < Assert.INVALID_NAME_CHARS.length(); charIdx++) {
            char badChar = Assert.INVALID_NAME_CHARS.charAt(charIdx);
            try {
                rootContainer.createChild("xx" + badChar + "xx");
                fail("container names should not include " + badChar);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }


    public void testNotExistingContainer() {
        try {
            new Container(15);
            fail("Created container object from non-existent container --invalid");
        } catch (Exception e) {
        }
    }

    public void testRename() {
        try {
            DB.getRootContainer().rename("xx");
            fail("Shouldn't allow renaming the root container");
        } catch (Exception e) {
            // ignore
        }

        Container rootContainer = DB.getRootContainer();
        Container child = rootContainer.createChild("xxx");
        child.rename("yyy");
        assertEquals("yyy", child.getName());
        assertFalse(rootContainer.hasChild("xxx"));
        assertTrue(rootContainer.hasChild("yyy"));
    }


    public void testRootContainerSubgraphOpsFail() {
        Container rootContainer = DB.getRootContainer();
        try {
            rootContainer.getSubgraphAttrs();
            fail("Got subgraph attrs of root container --invalid");
        } catch (Exception e) {
        }
        try {
            rootContainer.getSubgraphOIDs();
            fail("Got subgraph OIDs from root container --invalid");
        } catch (Exception e) {
        }
    }

}
