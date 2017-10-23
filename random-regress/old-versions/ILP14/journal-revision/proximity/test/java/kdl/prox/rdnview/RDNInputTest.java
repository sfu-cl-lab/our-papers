/**
 * $Id: RDNInputTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Assert;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.net.URL;


public class RDNInputTest extends TestCase {

    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private File getFile(Class rezClass, String fileName) {
        URL xmlFileURL = rezClass.getResource(fileName);
        Assert.condition(xmlFileURL != null, "couldn't find file relative " +
                "to class: " + fileName + ", " + rezClass);
        File xmlFile = new File(xmlFileURL.getFile());
        return xmlFile;
    }

    public void testRDN1() throws Exception {
        File xmlFile = getFile(getClass(), "t1-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput1();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN2() throws Exception {
        File xmlFile = getFile(getClass(), "t2-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput2();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN3() throws Exception {
        File xmlFile = getFile(getClass(), "t3-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput3();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN4() throws Exception {
        File xmlFile = getFile(getClass(), "t4-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput4();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN5() throws Exception {
        File xmlFile = getFile(getClass(), "t5-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput5();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN6() throws Exception {
        File xmlFile = getFile(getClass(), "t6-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput6();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN7() throws Exception {
        File xmlFile = getFile(getClass(), "t7-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput7();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN8() throws Exception {
        File xmlFile = getFile(getClass(), "t8-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput8();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN9() throws Exception {
        File xmlFile = getFile(getClass(), "t9-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput9();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    public void testRDN11() throws Exception {
        File xmlFile = getFile(getClass(), "t11-rdn.xml");
        RDNInput actRDNInput = RDNInput.loadRDNFile(xmlFile);
        RDNInput expRDNInput = RDNCanvasTest.getRDNInput11();
        verifyRDNInputs(expRDNInput, actRDNInput);
    }

    /**
     * Verifies that the two RDNInput instances are equal.
     *
     * @param expRDNInput
     * @param actRDNInput
     */
    private void verifyRDNInputs(RDNInput expRDNInput, RDNInput actRDNInput) {
        assertEquals(expRDNInput.getDescription(), actRDNInput.getDescription());
        assertEquals(expRDNInput.getNameMap(), actRDNInput.getNameMap());

        Document[] expRPTDocs = expRDNInput.getRptDocuments();
        Document[] actRPTDocs = actRDNInput.getRptDocuments();
        assertEquals(expRPTDocs.length, actRPTDocs.length);
        for (int rptEleIdx = 0; rptEleIdx < expRPTDocs.length; rptEleIdx++) {
            Element expRPTEle = expRPTDocs[rptEleIdx].getRootElement();
            Element actRPTEle = actRPTDocs[rptEleIdx].getRootElement();
            TestUtil.verifyElements(expRPTEle, actRPTEle);
        }
    }

}
