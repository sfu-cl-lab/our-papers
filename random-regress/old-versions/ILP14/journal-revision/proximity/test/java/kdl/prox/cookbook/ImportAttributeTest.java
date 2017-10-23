/**
 * $Id: ImportAttributeTest.java 3784 2007-11-19 19:43:06Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ImportAttributeTest.java 3784 2007-11-19 19:43:06Z schapira $
 */

package kdl.prox.cookbook;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.util.Assert;

import java.io.File;
import java.net.URL;

/**
 * Tests the DB class.
 *
 * @see kdl.prox.db.DB
 */
public class ImportAttributeTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        Connection.beginScope();

        // setup: create a 'persons_id' attribute
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("persons_id", "int");
        DB.getObjectAttrs().getAttrDataNST("persons_id").insertRow("200, 15");
        DB.getObjectAttrs().getAttrDataNST("persons_id").insertRow("201, 16");
        DB.getObjectAttrs().getAttrDataNST("persons_id").insertRow("202, 25");
        DB.getObjectAttrs().getAttrDataNST("persons_id").insertRow("205, 35");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        Connection.endScope();
        TestUtil.closeTestConnection();
    }

    public void testIt() {
        String importFileName = "attribute-import.txt";
        URL importFileURL = getClass().getResource(importFileName);
        Assert.notNull(importFileURL, "test import file not found: " + importFileName);

        // import the new attribute
        // make sure that the file is tab-delimited! 
        NST importedNST = new NST("id, value", "int, str");
        String fullPath = new File(importFileURL.getFile()).getPath();   // so that it's converted to the correct Windows format
        importedNST.fromfile(fullPath);

        // and join with the persons_id attribute
        NST personIDs = DB.getObjectAttrs().getAttrDataNST("persons_id");
        NST classNST = importedNST.join(personIDs, "A.id = B.value", "B.id, A.value");
        DB.getObjectAttrs().defineAttributeWithData("class_label", "str", classNST);

        assertTrue(DB.getObjectAttrs().isAttributeDefined("class_label"));
        TestUtil.verifyCollections(new String[]{"200@0.+", "202@0.+", "205@0.-"}, DB.getObjectAttrs().getAttrDataNST("class_label"));
    }


}
