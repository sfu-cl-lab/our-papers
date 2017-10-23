/**
 * $Id: OptimizedParameterQueryTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: OptimizedParameterQueryTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.qgraph2.family.QueryResultsValidator;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.PopulateDB;
import org.jdom.Element;

import java.io.File;
import java.net.URL;
import java.util.List;

public class OptimizedParameterQueryTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        Connection.beginScope();

        DB.clearDB();
        DB.initEmptyDB();
        PopulateDB.populateDB(this.getClass(), "parameter-query-db.txt");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }

    public void testIt() throws Exception {
        // first run the template query
        URL queryFileURL = this.getClass().getResource("parameter-query-template.qg2.xml");
        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(new File(queryFileURL.getFile()));
        QueryGraph2CompOp.queryGraph(graphQueryEle, null, "full", true);

        // then create a new container from the subgraphs where Movie.decade = 70
        NST match = DB.getContainer("full").getObjects("name = 'Movie' AND decade = 70");
        NST subgs = match.projectDistinct("subg_id");
        NST newObjects = DB.getContainer("full").getObjectsNST().intersect(subgs, "subg_id = subg_id");
        NST newLinks = DB.getContainer("full").getLinksNST().intersect(subgs, "subg_id = subg_id");
        DB.getRootContainer().createChildFromTempSGINSTs("70s", newObjects, newLinks);

        // and now verify that we get what we expected
        // for the 70s, the first subgraph should stay, with Movie 1 and ACtors 10,11,12
        NST expObjTempSGINST = SGIUtil.createTempSGINST();
        expObjTempSGINST.insertRow(new String[]{"1", "1", "Movie"});
        expObjTempSGINST.insertRow(new String[]{"10", "1", "Actor"});
        expObjTempSGINST.insertRow(new String[]{"11", "1", "Actor"});
        expObjTempSGINST.insertRow(new String[]{"12", "1", "Actor"});

        NST expLinkTempSGINST = SGIUtil.createTempSGINST();
        expLinkTempSGINST.insertRow(new String[]{"1", "1", "ActedIn"});
        expLinkTempSGINST.insertRow(new String[]{"2", "1", "ActedIn"});
        expLinkTempSGINST.insertRow(new String[]{"3", "1", "ActedIn"});

        NST actObjTempSGINST = DB.getContainer("70s").getObjectsNST();
        NST actLinkTempSGINST = DB.getContainer("70s").getLinksNST();
        QueryResultsValidator setHelper = new QueryResultsValidator(actObjTempSGINST, actLinkTempSGINST,
                expObjTempSGINST, expLinkTempSGINST);
        List failureMessages = setHelper.getFailureMessages();
        if (failureMessages.size() != 0) {
            fail(failureMessages.toString());
        }
    }


}
