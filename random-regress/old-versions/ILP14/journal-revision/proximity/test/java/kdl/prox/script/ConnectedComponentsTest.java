/**
 * $Id: ConnectedComponentsTest.java 3680 2007-10-24 14:57:28Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */


package kdl.prox.script;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.SGIUtil;
import org.apache.log4j.Logger;

import java.util.List;


public class ConnectedComponentsTest extends TestCase {

    private static Logger log = Logger.getLogger(ConnectedComponentsTest.class);

    private Container sourceContainer;


    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();

        TestUtil.openTestConnection();
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getRootContainer().deleteAllChildren();

        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);
        DB.insertObject(4);
        DB.insertObject(5);
        DB.insertObject(6);
        DB.insertObject(7);
        DB.insertObject(8);
        DB.insertObject(9);
        DB.insertObject(10);

        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 3);
        DB.insertLink(3, 2, 3);
        DB.insertLink(4, 2, 4);
        DB.insertLink(5, 5, 7);
        DB.insertLink(6, 5, 6);
        DB.insertLink(7, 8, 9);

        sourceContainer = DB.getRootContainer().createChild("cont-1");
        NST objectNST = sourceContainer.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "1", "A"});
        objectNST.insertRow(new String[]{"2", "1", "A"});
        objectNST.insertRow(new String[]{"3", "1", "A"});
        objectNST.insertRow(new String[]{"5", "1", "A"});
        objectNST.insertRow(new String[]{"6", "1", "A"});
        objectNST.insertRow(new String[]{"8", "1", "A"});
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "A"});
        linkNST.insertRow(new String[]{"2", "1", "A"});
        linkNST.insertRow(new String[]{"3", "1", "A"});
        linkNST.insertRow(new String[]{"6", "1", "A"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testConnectedComponents() {
        String containerName = "test-cont";
        Container outputContainer = SNA.computeConnectedComponents(null, containerName);

        // get the container and its tables
        NST actObjTempSGINST = outputContainer.getItemNST(true);
        NST actLinkTempSGINST = outputContainer.getItemNST(false);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"4", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "O"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "O"});
        expObjTempSGINST.insertRow(new String[]{"7", "2", "O"});
        expObjTempSGINST.insertRow(new String[]{"8", "3", "O"});
        expObjTempSGINST.insertRow(new String[]{"9", "3", "O"});
        expObjTempSGINST.insertRow(new String[]{"10", "4", "O"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"4", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"5", "2", "L"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "L"});
        expLinkTempSGINST.insertRow(new String[]{"7", "3", "L"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        expObjTempSGINST.release();
        expLinkTempSGINST.release();
    }

    public void testConnectedComponentsBadLinks() {
        // add a link that connects objects that don't exist
        DB.insertLink(8, 1, 1234);
        String containerName = "test-cont";

        try {
            SNA.computeConnectedComponents(null, containerName);
            fail("Should have failed because the db is in inconsistent state");
        } catch (Exception e) {
            // ignore -- expected behavior
        }
    }

    public void testConnectedComponentsOnContainer() {
        String containerName = "test-cont";
        Container outputContainer = SNA.computeConnectedComponents(sourceContainer, containerName);

        // get the container and its tables
        NST actObjTempSGINST = outputContainer.getItemNST(true);
        NST actLinkTempSGINST = outputContainer.getItemNST(false);

        // make expected results
        NST expObjTempSGINST = makeTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"2", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"3", "1", "O"});
        expObjTempSGINST.insertRow(new String[]{"5", "2", "O"});
        expObjTempSGINST.insertRow(new String[]{"6", "2", "O"});
        expObjTempSGINST.insertRow(new String[]{"8", "3", "O"});

        NST expLinkTempSGINST = makeTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "L"});
        expLinkTempSGINST.insertRow(new String[]{"6", "2", "L"});

        // compare them
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }

        expObjTempSGINST.release();
        expLinkTempSGINST.release();
    }

    public void testConnectedComponentsBadLinksOnContainer() {
        // add a link that connects objects that don't exist
        DB.insertLink(8, 1, 1234);
        NST linkNST = sourceContainer.getItemNST(false);
        linkNST.insertRow(new String[]{"8", "1", "A"});
        String containerName = "test-cont";

        try {
            SNA.computeConnectedComponents(sourceContainer, containerName);
            fail("Should have failed because the db is in inconsistent state");
        } catch (Exception e) {
            // ignore -- expected behavior
        }
    }

    private NST makeTempSGINST() {
        return SGIUtil.createTempSGINST();
    }


}
