/**
 * $Id: ImportXMLAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;


public class ImportXMLAppTest extends TestCase {

    Logger log = Logger.getLogger(ImportXMLAppTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // completely clear the db
        DB.getObjectNST().deleteRows();
        DB.getLinkNST().deleteRows();
        DB.getObjectAttrs().deleteAllAttributes();
        DB.getLinkAttrs().deleteAllAttributes();
        DB.getContainerAttrs().deleteAllAttributes();
        DB.getRootContainer().deleteAllChildren();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    private File getTempXMLFile() {
        return new File(System.getProperty("java.io.tmpdir", "tmp"), "export-test.xml");
    }

    public void testImportEmptyDB() throws Exception {
        // we use this simple test: import export-xml-full.xml, export it,
        // then compare files.
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        new ImportXMLApp(importedXMLFile, false);

        File exportedXMLFile = getTempXMLFile();
        new ExportXMLApp(exportedXMLFile);
        validateXMLFile(importedXMLFile, exportedXMLFile);
    }

    public void testImportFailsIfAttributeExists() {
        DB.getObjectAttrs().defineAttribute("attr1", "type:str");
        DB.getObjectAttrs().getAttrDataNST("attr1").insertRow("1, 'a'");

        DB.getLinkAttrs().defineAttribute("attr1", "int");
        DB.getLinkAttrs().getAttrDataNST("attr1").insertRow("1, 1");

        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            fail("Should have thrown an exception --importing attribute that already exists");
        } catch (Exception e) {
        }

        // with the true option, it should still give an error because object attr1 is of different types
        try {
            new ImportXMLApp(importedXMLFile, true);
            fail("Should have thrown an exception because object attr1 is str and of diff type in XML file");
        } catch (Exception e) {
        }

        // after removing the object attr, but leaving the link one, it should work
        DB.getObjectAttrs().deleteAttribute("attr1");
        try {
            new ImportXMLApp(importedXMLFile, true);
            assertEquals(1, DB.getObjectAttrs().getAttributeNames().size());
            assertEquals(1, DB.getLinkAttrs().getAttributeNames().size());
            assertEquals(3, DB.getLinkAttrs().getAttrDataNST("attr1").getRowCount());
        } catch (Exception e) {
            //
        }
    }

    public void testImportFailsIfContainerNameExists() {
        DB.getRootContainer().createChild("a");
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            fail("Should have thrown an exception --importing container name that already exists");
        } catch (Exception e) {
        }
    }

    public void testImportFailsIfLinksNotEmpty() {
        DB.insertLink(1, 1);
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            fail("Should have thrown an exception --importing when links already exist");
        } catch (Exception e) {
        }

        // but with the true option, it shouldn't give an error
        try {
            new ImportXMLApp(importedXMLFile, true);
            assertEquals(3, DB.getLinkNST().getRowCount());
        } catch (Exception e) {
            //
        }
    }

    public void testImportFailsIfObjectNotEmpty() {
        DB.insertObject();
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            fail("Should have thrown an exception --importing when objects already exist");
        } catch (Exception e) {
            // ignore
        }

        // but with the true option, it shouldn't give an error
        try {
            new ImportXMLApp(importedXMLFile, true);
            assertEquals(4, DB.getObjectNST().getRowCount());
        } catch (Exception e) {
            //
        }
    }

    public void testImportNonEmptyDB() throws Exception {
        String dataType = "year:int,gross:int";
        String attrName = "attr0";
        DB.getLinkAttrs().defineAttribute(attrName, dataType);
        NST attrNST = DB.getLinkAttrs().getAttrDataNST(attrName);
        String[] row1 = new String[]{"1", "1995", "1000"};
        attrNST.insertRow(row1);

        DB.getRootContainer().createChild("x");

        // we use this simple test: import export-xml-full.xml, export it,
        // then compare files.
        URL importedXMLFileURL = getClass().getResource("export-xml-full-with-container-7.xml");
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        new ImportXMLApp(importedXMLFile, false);

        // let's make sure that there are the right number of containers and attributes
        assertEquals(2, DB.getLinkAttrs().getAttrNST().getRowCount());
        assertEquals(4, DB.getContainerNST().getRowCount());
    }

    public void testImportStringWithCommas() throws Exception {
        // we use this simple test: import export-xml-full.xml, export it,
        // then compare files.
        URL importedXMLFileURL = getClass().getResource("export-xml-with-comma.xml");
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        new ImportXMLApp(importedXMLFile, false);

        File exportedXMLFile = getTempXMLFile();
        new ExportXMLApp(exportedXMLFile);
        validateXMLFile(importedXMLFile, exportedXMLFile);
    }

    public void testRecodeIfContainerIDExists() {
        DB.getRootContainer().createChild("x");
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            fail("Should have thrown an exception for existing cntainer ID");
        } catch (Exception e) {
        }

        try {
            new ImportXMLApp(importedXMLFile, true);
            assertEquals(4, DB.getContainerNST().getRowCount());
            assertEquals(0, DB.getContainer("x").getOid());
            assertEquals(1, DB.getContainer("a").getOid());
            assertEquals(2, DB.getContainer("a/b").getOid());
            assertEquals(3, DB.getContainer("c").getOid());
            ResultSet resultSet = DB.getContainerAttrs().getAttrDataNST("attr1").selectRows();
            while (resultSet.next()) {
                int oid = resultSet.getOID(1);
                int value = resultSet.getInt(2);
                // 0 (which is a in the XML file) should be recoded to 1, and 1 (b) to 2
                assertTrue((oid == 1 && value == 1) || (oid == 2 && value == 3));
            }
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    public void testNoRecodeIfContainerIDNotExists() {
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        try {
            new ImportXMLApp(importedXMLFile, false);
            assertEquals(3, DB.getContainerNST().getRowCount());
            assertEquals(0, DB.getContainer("a").getOid());
            assertEquals(1, DB.getContainer("a/b").getOid());
            assertEquals(2, DB.getContainer("c").getOid());
        } catch (Exception e) {
        }
    }

    private void validateXMLFile(File expectedFile, File actualFile) {
        String actualFileCont = Util.readStringFromFile(actualFile);
        String expectedFileCont = Util.readStringFromFile(expectedFile);
        assertEquals(expectedFileCont, actualFileCont);
    }

}
