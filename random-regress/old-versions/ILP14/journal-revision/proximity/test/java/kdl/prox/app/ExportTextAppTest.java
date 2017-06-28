/**
 * $Id: ExportTextAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ExportTextAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.DB;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;

public class ExportTextAppTest extends TestCase {

    Logger log = Logger.getLogger(ExportTextAppTest.class);

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

    /**
     * This is an 'interesting' test. The process is as follows:
     * - Create a database from an XML file (export-xml-full.xml)
     * - Dump the database as text (what we want to try)
     * - Clear the database and re-import from the text output (we assume that import works)
     * - Export the database again to an XML file
     * - Compare this new output with the original import file
     *
     * @throws Exception
     */
    public void testImportAndExport() throws Exception {
        // we use this simple test: import export-xml-full.xml, export it, and then reimport it, re-export it
        // then compare files.
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());
        new ImportXMLApp(importedXMLFile, true);

        // now exported as text
        File tempDir = ImportUtil.getTempDir();
        new ExportTextApp(tempDir.getPath());

        // now import that text, and re-export it
        DB.clearDB();
        DB.initEmptyDB();
        new ImportTextApp(tempDir.getPath());
        File exportedXMLFile = getTempXMLFile();
        new ExportXMLApp(exportedXMLFile);

        // and compare the files
        validateXMLFile(importedXMLFile, exportedXMLFile);

        // and delete the directory
        ImportUtil.deleteDir(tempDir);
    }

    private void validateXMLFile(File expectedFile, File actualFile) {
        String actualFileCont = Util.readStringFromFile(actualFile);
        String expectedFileCont = Util.readStringFromFile(expectedFile);
        assertEquals(expectedFileCont, actualFileCont);
    }

}
