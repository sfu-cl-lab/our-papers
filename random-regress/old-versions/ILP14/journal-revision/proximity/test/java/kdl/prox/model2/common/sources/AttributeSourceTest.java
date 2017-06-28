/**
 * $Id: AttributeSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AttributeSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.sources;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.XMLUtil;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.List;

/**
 * Directly tests the Connection class. Note that it is indirectly tested by
 * many other test classes. For now tests scope behavior.
 */
public class AttributeSourceTest extends TestCase {

    Container container;
    NSTCache cache = new NSTCache();
    private Logger log = Logger.getLogger(AttributeSourceTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("1, 2, B");
        container.getObjectsNST().insertRow("1, 3, C");
        container.getObjectsNST().insertRow("2, 4, A");
        container.getObjectsNST().insertRow("2, 5, B");
        container.getObjectsNST().insertRow("2, 6, C");

        container.getSubgraphAttrs().defineAttribute("attr1", "value:int");
        container.getSubgraphAttrs().getAttrDataNST("attr1").insertRow("1, 10");
        container.getSubgraphAttrs().getAttrDataNST("attr1").insertRow("2, 11");

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("attr1", "value:int");
        DB.getObjectAttrs().getAttrDataNST("attr1").insertRow("1, 10");

        DB.getObjectAttrs().defineAttribute("attr2", "value:dbl");
        DB.getObjectAttrs().getAttrDataNST("attr2").insertRow("1, 10.5");
        DB.getObjectAttrs().getAttrDataNST("attr2").insertRow("1, 11.5");
        DB.getObjectAttrs().getAttrDataNST("attr2").insertRow("1, 12.5");
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testSubgAttr() {
        AttributeSource attributeSource = new AttributeSource("attr1");
        attributeSource.init(container, cache);
        NST dataTable = attributeSource.getSourceTable();
        assertEquals(2, dataTable.getRowCount());
    }

    public void testObjectAttr() {
        AttributeSource attributeSource = new AttributeSource("A", "attr1");
        attributeSource.init(container, cache);
        NST dataTable = attributeSource.getSourceTable();
        assertEquals(1, dataTable.getRowCount()); // the A in the second container doesn't have an attr val
    }

    public void testGetValues() {
        AttributeSource attributeSource = new AttributeSource("A", "attr1");
        attributeSource.init(container, cache);
        List uniqueValues = attributeSource.computeDistinctValues();
        assertEquals(1, uniqueValues.size());
        assertTrue(uniqueValues.contains("10"));

        // Try a continuous attribute, that needs to be binned
        attributeSource = new AttributeSource("A", "attr2");
        attributeSource.init(container, cache);
        attributeSource.setNumDistinctContinuousValues(2);
        uniqueValues = attributeSource.computeDistinctValues();
        assertEquals(attributeSource.getNumDistinctContinuousValues(), uniqueValues.size());
        assertTrue(uniqueValues.contains("10.5"));
        assertTrue(uniqueValues.contains("11.5"));

        // a single bin
        attributeSource.setNumDistinctContinuousValues(1);
        uniqueValues = attributeSource.computeDistinctValues();
        assertEquals(attributeSource.getNumDistinctContinuousValues(), uniqueValues.size());
        assertTrue(uniqueValues.contains("11.5"));
    }

    public void testFromXML() {
        Element sourceEle = new Element("xx");
        Element xmlEle = new Element("source");
        xmlEle.addContent(XMLUtil.createElementWithValue("source-class", "kdl.prox.model2.common.sources.AttributeSource"));
        xmlEle.addContent(XMLUtil.createElementWithValue("data-type", "flt"));
        xmlEle.addContent(XMLUtil.createElementWithValue("is-continuous", "true"));
        xmlEle.addContent(new Element("source-elements"));
        sourceEle.addContent(xmlEle);
        AttributeSource attributeSource = new AttributeSource(sourceEle);
        assertTrue(attributeSource.isContinuous());
    }

}
