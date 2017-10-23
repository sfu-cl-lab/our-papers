/**
 * $Id: ExportXMLAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ExportXMLAppTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.ImportUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;


public class ExportXMLAppTest extends TestCase {

    private static Logger log = Logger.getLogger(ExportXMLAppTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // test objects and links
        DB.getObjectNST().deleteRows();
        DB.insertObject(1);
        DB.insertObject(2);
        DB.insertObject(3);

        DB.getLinkNST().deleteRows();
        DB.insertLink(1, 1, 2);
        DB.insertLink(2, 1, 3);

        // test attributes
        Attributes attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        String dataType = "year:int,gross:int";
        String attrName = "attr1";
        attrs.defineAttribute(attrName, dataType);
        NST attrNST = attrs.getAttrDataNST(attrName);
        String[] row1 = new String[]{"1", "1995", "1000"};
        String[] row2 = new String[]{"2", "1996", "100"};
        attrNST.insertRow(row1);
        attrNST.insertRow(row2);

        attrs = DB.getLinkAttrs();
        attrs.deleteAllAttributes();
        dataType = "int";
        attrName = "attr1";
        attrs.defineAttribute(attrName, dataType);
        attrNST = attrs.getAttrDataNST(attrName);
        row1 = new String[]{"1", "1995"};
        row2 = new String[]{"2", "1996"};
        attrNST.insertRow(row1);
        attrNST.insertRow(row2);

        attrs = DB.getContainerAttrs();
        attrs.deleteAllAttributes();
        dataType = "time:int";
        attrName = "attr1";
        attrs.defineAttribute(attrName, dataType);
        attrNST = attrs.getAttrDataNST(attrName);
        row1 = new String[]{"0", "1"};
        row2 = new String[]{"1", "3"};
        attrNST.insertRow(row1);
        attrNST.insertRow(row2);

        // test containers, nested and with subgraph attributes
        Container rootCont = DB.getRootContainer();
        rootCont.deleteAllChildren();
        Container childContA = rootCont.createChild("a");
        Container childContB = childContA.createChild("b");
        rootCont.createChild("c");

        NST objectNST = childContA.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "0", "A"});
        objectNST.insertRow(new String[]{"2", "1", "B"});
        objectNST.insertRow(new String[]{"3", "2", "C"});
        NST linkNST = childContA.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "1", "X"});

        objectNST = childContB.getItemNST(true);
        objectNST.insertRow(new String[]{"1", "0", "A"});
        linkNST = childContB.getItemNST(false);
        linkNST.insertRow(new String[]{"1", "0", "X"});

        Attributes subgraphAttrs = childContA.getSubgraphAttrs();
        dataType = "year:int";
        attrName = "attr1";
        subgraphAttrs.defineAttribute(attrName, dataType);
        attrNST = subgraphAttrs.getAttrDataNST(attrName);
        row1 = new String[]{"1", "1995"};
        row2 = new String[]{"2", "1996"};
        attrNST.insertRow(row1);
        attrNST.insertRow(row2);
        subgraphAttrs = childContB.getSubgraphAttrs();
        dataType = "year:int";
        attrName = "attr1";
        subgraphAttrs.defineAttribute(attrName, dataType);
        attrNST = subgraphAttrs.getAttrDataNST(attrName);
        row1 = new String[]{"1", "1995"};
        attrNST.insertRow(row1);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    private File getTempXMLFile() {
        return new File(System.getProperty("java.io.tmpdir", "tmp"), "export-test.xml");
    }

    public void testDumpNST() {
        String[] files = ImportUtil.dumpNST(DB.getObjectNST(), "obj_");
        assertEquals(1, files.length);
        assertEquals(Util.delimitBackslash(System.getProperty("java.io.tmpdir", "tmp") +
                System.getProperty("file.separator") + "obj_id"),
                files[0]);  // NB: file separator is not necessary on Windows, but is required on Linux due to whether the temp dir includes the final separator or not
        String actualFileCont = Util.readStringFromFile(new File(files[0]));
        String expectedFileCont = "1\n2\n3\n";
        assertEquals(expectedFileCont, actualFileCont);
    }

    public void testDumpNSTCarriageReturn() {
        Attributes attrs = DB.getObjectAttrs();
        String dataType = "str";
        String attrName = "str-attr";
        attrs.defineAttribute(attrName, dataType);
        NST attrNST = attrs.getAttrDataNST(attrName);
        attrNST.insertRow(new String[]{"1", "Stephen \n King"});
        attrNST.insertRow(new String[]{"2", "John Malcovich"});

        String[] files = ImportUtil.dumpNST(attrNST, "str-attr_");
        assertEquals(2, files.length);
        assertEquals(Util.delimitBackslash(System.getProperty("java.io.tmpdir", "tmp") +
                System.getProperty("file.separator") + "str-attr_id"),
                files[0]);  // NB: file separator is not necessary on Windows, but is required on Linux due to whether the temp dir includes the final separator or not
        String actualFileCont = Util.readStringFromFile(new File(files[0]));
        String expectedFileCont = "1\n2\n";
        assertEquals(expectedFileCont, actualFileCont);

        assertEquals(Util.delimitBackslash(System.getProperty("java.io.tmpdir", "tmp") +
                System.getProperty("file.separator") + "str-attr_value"),
                files[1]);  // NB: file separator is not necessary on Windows, but is required on Linux due to whether the temp dir includes the final separator or not
        actualFileCont = Util.readStringFromFile(new File(files[1]));
        expectedFileCont = "\"Stephen _ King\"\n\"John Malcovich\"\n";
        assertEquals(expectedFileCont, actualFileCont);
    }

    public void testExportAll() throws Exception {
        File tempFile = getTempXMLFile();
        new ExportXMLApp(tempFile);
        validateXMLFile("export-xml-full.xml", tempFile);
    }

    public void testExportAttribute() throws Exception {
        File tempFile = getTempXMLFile();
        new ExportXMLApp(tempFile, ImportExportType.OBJECT_ATTRIBUTE, "attr1");
        validateXMLFile("export-xml-attribute.xml", tempFile);
    }

    public void testExportContainer() throws Exception {
        File tempFile = getTempXMLFile();
        new ExportXMLApp(tempFile, ImportExportType.CONTAINER, "/a/b");
        validateXMLFile("export-xml-container.xml", tempFile);
    }

    public void testExportContainerAttribute() throws Exception {
        File tempFile = getTempXMLFile();
        new ExportXMLApp(tempFile, ImportExportType.CONTAINER_ATTRIBUTE, "attr1");
        validateXMLFile("export-xml-container-attribute.xml", tempFile);
    }

    private void validateXMLFile(String expectedFileStr, File actualFile) {
        URL expectedFileURL = getClass().getResource(expectedFileStr);
        String expectedFile = expectedFileURL.getFile();
        String actualFileCont = Util.readStringFromFile(actualFile);
        String expectedFileCont = Util.readStringFromFile(new File(expectedFile));
        assertEquals(expectedFileCont, actualFileCont);
    }

}
