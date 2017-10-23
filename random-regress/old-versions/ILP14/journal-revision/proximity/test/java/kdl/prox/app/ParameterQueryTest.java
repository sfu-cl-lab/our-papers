/**
 * $Id: ParameterQueryTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ParameterQueryTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.app;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qgraph2.QueryXMLUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;

/**
 * Testing parse condition
 */
public class ParameterQueryTest extends TestCase {
    Logger log = Logger.getLogger(ImportTextAppTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testSingleAnd() throws Exception {
        // test the default case single and no ors.
        URL queryFileURL = getClass().getResource("test-param-single-and.qg2.xml"); // file used for testing ExportXMLAppTest
        File queryFile = new File(queryFileURL.getFile());

        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        String item1Name = "Item1";
        String newValue = item1Name + ".attr1 = 1";
        ParameterQuery.parseCondition(newValue, graphQueryEle);

        //Get Item1 node from the XML
        List verticesList = graphQueryEle.getChild("query-body").getChildren("vertex");
        ListIterator vertIter = verticesList.listIterator();
        while (vertIter.hasNext()) {
            Element vertEle = (Element) vertIter.next();
            if (vertEle.getAttributeValue("name").equals(item1Name)) {
                // check to see if its got a condition
                assertNotNull(vertEle.getChild("condition"));
                Element condEle = vertEle.getChild("condition");
                assertNotNull(condEle.getChild("and"));
                Element andEle = condEle.getChild("and");
                List testList = andEle.getChildren("test");
                assertEquals(testList.size(), 2);
                Element newEle = (Element) testList.get(1);
                assertEquals(newEle.getChildText("attribute-name"), "attr1");
                assertEquals(newEle.getChildText("operator"), "eq");
                assertEquals(newEle.getChildText("value"), "1");
            }
        }

    }

    public void testNestedAnd() throws Exception {
        // test one <or> with multiple <ands>.
        URL queryFileURL = getClass().getResource("test-param-nested-and.qg2.xml"); // file used for testing ExportXMLAppTest
        File queryFile = new File(queryFileURL.getFile());

        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        String item1Name = "Item1";
        String newValue = item1Name + ".attr1 = 1";

        log.debug((new XMLOutputter()).outputString(graphQueryEle));

        ParameterQuery.parseCondition(newValue, graphQueryEle);

        log.info((new XMLOutputter()).outputString(graphQueryEle));

        //Get Item1 node from the XML
        List verticesList = graphQueryEle.getChild("query-body").getChildren("vertex");
        ListIterator vertIter = verticesList.listIterator();
        while (vertIter.hasNext()) {
            Element vertEle = (Element) vertIter.next();
            if (vertEle.getAttributeValue("name").equals(item1Name)) {
                // check to see if its got a condition
                assertNotNull(vertEle.getChild("condition"));
                Element condEle = vertEle.getChild("condition");
                assertNotNull(condEle.getChild("or"));
                Element orEle = condEle.getChild("or");
                assertEquals(orEle.getChildren().size(), 2);
                List andList = orEle.getChildren("and");
                //Check item1
                Element andEle1 = (Element) andList.get(0);
                List testList1 = andEle1.getChildren("test");
                assertEquals(2, testList1.size());
                Element newEle1 = (Element) testList1.get(1);
                assertEquals(newEle1.getChildText("attribute-name"), "attr1");
                assertEquals(newEle1.getChildText("operator"), "eq");
                assertEquals(newEle1.getChildText("value"), "1");
                //Check item2
                Element andEle2 = (Element) andList.get(0);
                List testList2 = andEle2.getChildren("test");
                assertEquals(2, testList2.size());
                Element newEle2 = (Element) testList2.get(1);
                assertEquals(newEle2.getChildText("attribute-name"), "attr1");
                assertEquals(newEle2.getChildText("operator"), "eq");
                assertEquals(newEle2.getChildText("value"), "1");
            }
        }

    }

    public void testOr() throws Exception {
        // test a single <or>.
        URL queryFileURL = getClass().getResource("test-param-or.qg2.xml"); // file used for testing ExportXMLAppTest
        File queryFile = new File(queryFileURL.getFile());

        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        String item1Name = "Item1";
        String newValue = item1Name + ".attr1 = 1";

        log.debug((new XMLOutputter()).outputString(graphQueryEle));

        ParameterQuery.parseCondition(newValue, graphQueryEle);

        log.info((new XMLOutputter()).outputString(graphQueryEle));

        //Get Item1 node from the XML
        List verticesList = graphQueryEle.getChild("query-body").getChildren("vertex");
        ListIterator vertIter = verticesList.listIterator();
        while (vertIter.hasNext()) {
            Element vertEle = (Element) vertIter.next();
            if (vertEle.getAttributeValue("name").equals(item1Name)) {
                // check to see if its got a condition
                assertNotNull(vertEle.getChild("condition"));
                Element condEle = vertEle.getChild("condition");
                assertNotNull(condEle.getChild("or"));
                Element orEle = condEle.getChild("or");
                assertEquals(orEle.getChildren().size(), 2);
                List testList = orEle.getChildren("test");
                Element newEle1 = (Element) testList.get(1);
                assertEquals(newEle1.getChildText("attribute-name"), "attr1");
                assertEquals(newEle1.getChildText("operator"), "eq");
                assertEquals(newEle1.getChildText("value"), "1");
            }
        }

    }
}