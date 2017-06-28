/**
 * $Id: CreateLinksTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CreateLinksTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;

/**
 * Tests the DB class.
 *
 * @see kdl.prox.db.DB
 */
public class CreateLinksTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        Connection.beginScope();

        // setup: 5 links of ActorIn type, 1 of a different type
        DB.getLinkNST().deleteRows();
        DB.getLinkNST().insertRow("1, 30, 300");
        DB.getLinkNST().insertRow("2, 40, 300");
        DB.getLinkNST().insertRow("3, 30, 400");
        DB.getLinkNST().insertRow("4, 40, 400");
        DB.getLinkNST().insertRow("5, 50, 400");
        DB.getLinkNST().insertRow("6, 750, 7400");  // not the right kind

        DB.getLinkAttrs().deleteAllAttributes();
        DB.getLinkAttrs().defineAttribute("link_type", "str");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("1, ActorIn");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("2, ActorIn");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("3, ActorIn");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("4, ActorIn");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("5, ActorIn");
        DB.getLinkAttrs().getAttrDataNST("link_type").insertRow("6, NotActorIn");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }

    public void testcreateShortcutLinks() {
        // Create actor-actor links via movie connections
        NST actorInLinks = DB.getLinks("link_type = 'ActorIn'");
        NST collaboratedLinks = actorInLinks.join(actorInLinks, "o2_id = o2_id");
        NST noLoops = collaboratedLinks.filter("A.o1_id != B.o1_id", "A.o1_id, B.o1_id");
        NST noRepeats = noLoops.distinct("A.o1_id, B.o1_id");
        DB.createLinks(noRepeats.renameColumns("from, to"));

        assertEquals(6, noRepeats.getRowCount());
        assertEquals(12, DB.getLinkNST().getRowCount());
    }

    public void testcreateShortcutLinksWithAttrs() {
        // Create actor-actor links via movie connections
        NST actorInLinks = DB.getLinks("link_type = 'ActorIn'");
        NST collaboratedLinks = actorInLinks.join(actorInLinks, "o2_id = o2_id");
        NST noLoops = collaboratedLinks.filter("A.o1_id != B.o1_id", "A.o1_id, B.o1_id");
        NST noRepeats = noLoops.distinct("A.o1_id, B.o1_id");

        noRepeats.renameColumns("from, to");
        noRepeats.addConstantColumn("attr_link_type", "str", "Collaborator");
        DB.createLinks(noRepeats);

        assertEquals(6, noRepeats.getRowCount());
        assertEquals(12, DB.getLinkNST().getRowCount());
    }


}
