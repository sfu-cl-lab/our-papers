package kdl.prox.rdnview;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.apache.log4j.Logger;

public class RPTReaderTest extends TestCase {

    private static final Logger log = Logger.getLogger(RPTReaderTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testGetNodeListNoNodes() throws IOException, JDOMException {
        Element rptEle = getXMLDocument("t1-actor-gender.xml");
        List list = RPTReader.getRPTNodeList("rpt2.dtd", rptEle);
        assertEquals(0, list.size());
    }

    public void testGetNodeListOneNode() throws IOException, JDOMException {
        Element rptEle = getXMLDocument("t1-actor-hasAward.xml");
        List list = RPTReader.getRPTNodeList("rpt2.dtd", rptEle);
        assertEquals(1, list.size());
    }

    /**
     * The RPT has six splits
     * @throws IOException
     * @throws JDOMException
     */
    public void testGetNodeListRPT2() throws IOException, JDOMException {
        Element rptEle = getXMLDocument("t11-ProxWebKB_RPT.xml");
        List list = RPTReader.getRPTNodeList("rpt2.dtd", rptEle);
        assertEquals(6, list.size());
    }

    public void testGetClassLabelSourceRPT2() throws IOException, JDOMException {
        Element rptEle = getXMLDocument("t11-ProxWebKB_RPT.xml");
        RPTSourceInfo info = RPTReader.getClassLabelSourceInfo("rpt2.dtd", rptEle);
        assertEquals("core_page", info.item);
        assertEquals("pagetype", info.attr);
    }
    
    public void testGetSplitSouceInfoRPT2() throws IOException, JDOMException {
        Element rptEle = getXMLDocument("t11-ProxWebKB_RPT.xml");
        List<Element> list = RPTReader.getRPTNodeList("rpt2.dtd", rptEle);
        RPTSourceInfo info = RPTReader.getSplitSourceInfo("rpt2.dtd", list.get(0));
        assertEquals("linked_to_page", info.item);
        assertEquals("page_num_inlinks", info.attr);
    }




    private File getFile(Class<? extends Object> rezClass, String fileName) {
        URL xmlFileURL = rezClass.getResource(fileName);
        Assert.condition(xmlFileURL != null, "couldn't find file relative " +
                "to class: " + fileName + ", " + rezClass);
        File xmlFile = new File(xmlFileURL.getFile());
        return xmlFile;
    }

    private Element getXMLDocument(String file) throws JDOMException, IOException {
        File xmlFile = getFile(getClass(), file);
        SAXBuilder saxBuilder = new SAXBuilder(true);
        Document document = saxBuilder.build(xmlFile);
        Element rptEle = document.getRootElement();
        return rptEle;
    }
}
