/**
 * $Id: ImportTextAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ImportTextAppTest.java 3658 2007-10-15 16:29:11Z schapira $
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

public class ImportTextAppTest extends TestCase {

    Logger log = Logger.getLogger(ImportTextAppTest.class);

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

        // do the same thing that ImportXMLApp does, but do not load the data into Prox, just generate the files
        File tempDir = ImportUtil.getTempDir();
        final String tempDirPath = tempDir.getPath();
        new ImportXMLApp().convertXMLToText(importedXMLFile, false, tempDirPath);

        // Now that we have the directory, load the file
        new ImportTextApp(tempDirPath);

        File exportedXMLFile = getTempXMLFile();
        new ExportXMLApp(exportedXMLFile);
        validateXMLFile(importedXMLFile, exportedXMLFile);

        // and delete the directory
        ImportUtil.deleteDir(tempDir);
    }

    public void testImportEmptyDBNoObjs() throws Exception {
        // we use this simple test: import export-xml-full.xml, export it,
        // then compare files.
        URL importedXMLFileURL = getClass().getResource("export-xml-full.xml"); // file used for testing ExportXMLAppTest
        File importedXMLFile = new File(importedXMLFileURL.getFile());

        // do the same thing that ImportXMLApp does, but do not load the data into Prox, just generate the files
        File tempDir = ImportUtil.getTempDir();
        final String tempDirPath = tempDir.getPath();
        new ImportXMLApp().convertXMLToText(importedXMLFile, false, tempDirPath);

        // delete the objects file -- should work without a complaint
        ImportUtil.getFullFilePath(tempDirPath, "containers.format").delete();
        ImportUtil.getFullFilePath(tempDirPath, "objects.format").delete();
        ImportUtil.getFullFilePath(tempDirPath, "links.format").delete();
        new ImportTextApp(tempDirPath);

        // and delete the directory
        ImportUtil.deleteDir(tempDir);
    }

    private void validateXMLFile(File expectedFile, File actualFile) {
        String actualFileCont = Util.readStringFromFile(actualFile);
        String expectedFileCont = Util.readStringFromFile(expectedFile);
        assertEquals(expectedFileCont, actualFileCont);
    }

}
