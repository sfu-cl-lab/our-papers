/**
 * $Id: AddAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AddAttributeTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.script;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


public class AddAttributeTest extends TestCase {

    private static Logger log = Logger.getLogger(AddAttributeTest.class);

    int o0ID;
    int o1ID;
    int o2ID;
    int o3ID;

    int link01ID;
    int link23ID;

    private void buildDatabase() {
        DB.getObjectNST().deleteRows();
        o0ID = DB.insertObject();
        o1ID = DB.insertObject();
        o2ID = DB.insertObject();
        o3ID = DB.insertObject();
        DB.getLinkNST().deleteRows();
        link01ID = DB.insertLink(o0ID, o1ID);
        link23ID = DB.insertLink(o2ID, o3ID);

        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.deleteAllAttributes();
        objectAttrs.defineAttribute("A", "int");
        objectAttrs.defineAttribute("B", "int");
        objectAttrs.defineAttribute("dbl_attr", "dbl");
        objectAttrs.defineAttribute("flt_attr", "flt");
        objectAttrs.defineAttribute("container_obj_attr", "int");

        NST attrDataNST = objectAttrs.getAttrDataNST("A");
        attrDataNST.insertRow(new String[]{o0ID + "", "3"});
        attrDataNST.insertRow(new String[]{o1ID + "", "5"});
        attrDataNST.insertRow(new String[]{o2ID + "", "8"});

        attrDataNST = objectAttrs.getAttrDataNST("B");
        attrDataNST.insertRow(new String[]{o0ID + "", "-1"});
        attrDataNST.insertRow(new String[]{o1ID + "", "7"});
        attrDataNST.insertRow(new String[]{o2ID + "", "11"});

        attrDataNST = objectAttrs.getAttrDataNST("dbl_attr");
        attrDataNST.insertRow(new String[]{o0ID + "", "3.0"});
        attrDataNST.insertRow(new String[]{o1ID + "", "5.0"});
        attrDataNST.insertRow(new String[]{o2ID + "", "8"});

        attrDataNST = objectAttrs.getAttrDataNST("flt_attr");
        attrDataNST.insertRow(new String[]{o0ID + "", "3.0"});
        attrDataNST.insertRow(new String[]{o1ID + "", "5.0"});
        attrDataNST.insertRow(new String[]{o2ID + "", "8"});

        attrDataNST = objectAttrs.getAttrDataNST("container_obj_attr");
        attrDataNST.insertRow(new String[]{o0ID + "", "2"});
        attrDataNST.insertRow(new String[]{o1ID + "", "4"});
        attrDataNST.insertRow(new String[]{o2ID + "", "6"});

        Attributes linkAttrs = DB.getLinkAttrs();
        linkAttrs.deleteAllAttributes();
        linkAttrs.defineAttribute("type", "str");
        attrDataNST = linkAttrs.getAttrDataNST("type");
        attrDataNST.insertRow(new String[]{link01ID + "", "l"});
        attrDataNST.insertRow(new String[]{link23ID + "", "l"});
        attrDataNST.insertRow(new String[]{link23ID + "", "x"});

        Container rootContainer = DB.getRootContainer();

        if (rootContainer.hasChild("child")) {
            rootContainer.deleteChild("child");
        }

        Container childContainer = rootContainer.createChild("child");
        int subgID = 1;
        Subgraph subgraph = childContainer.getSubgraph(subgID);
        subgraph.insertObject(o0ID, "X");
        subgraph.insertObject(o1ID, "X");
        subgraph.insertObject(o2ID, "X");

        if (rootContainer.hasChild("child2")) {
            rootContainer.deleteChild("child2");
        }

        Container childContainer2 = rootContainer.createChild("child2");
        int subgID2 = 2;
        subgraph = childContainer2.getSubgraph(subgID2);
        subgraph.insertObject(o0ID, "X");
        subgraph.insertObject(o1ID, "X");
        subgraph.insertObject(o2ID, "Y");
        subgraph.insertObject(o3ID, "Y");

        int subgID3 = 3;
        subgraph = childContainer2.getSubgraph(subgID3);
        subgraph.insertObject(o0ID, "X");
        subgraph.insertObject(o2ID, "Y");
    }

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();
        buildDatabase();
    }

    protected void tearDown() {
        TestUtil.closeTestConnection();
    }


    public void testAddConstantAttribute() {
        Container container = DB.getRootContainer().getChild("child");
        new AddAttribute().addConstantAttribute(container, true, "X", "ConstantAttr", new Integer(1));

        Attributes attributes = DB.getObjectAttrs();
        Assert.condition(attributes.isAttributeDefined("ConstantAttr"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("ConstantAttr");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.1",
                "1@0.1",
                "2@0.1"}, new HashSet<String>(valList));
    }

    public void testAddConstantSubgraphAttribute() {
        Container container = DB.getRootContainer().getChild("child");
        new AddAttribute().addConstantSubgraphAttribute(container, "ConstantAttr", new Integer(1));

        Attributes attributes = container.getSubgraphAttrs();
        Assert.condition(attributes.isAttributeDefined("ConstantAttr"), "Error: failed to define attr");
        NST attrDataNST = attributes.getAttrDataNST("ConstantAttr");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{"1@0.1"}, new HashSet<String>(valList));
    }

    public void testAddDateDiffAttributeFromTwoDates() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        attributes.defineAttribute("date_attr", "date");
        NST attrDataNST = attributes.getAttrDataNST("date_attr");
        attrDataNST.insertRow(new String[]{"1", "2002-10-03"});
        attrDataNST.insertRow(new String[]{"2", "2004-01-22"});
        attrDataNST.insertRow(new String[]{"3", "1990-01-11"});
        attributes.defineAttribute("date_attr2", "date");
        attrDataNST = attributes.getAttrDataNST("date_attr2");
        attrDataNST.insertRow(new String[]{"1", "2002-10-02"});
        attrDataNST.insertRow(new String[]{"2", "2004-02-23"});
        attrDataNST.insertRow(new String[]{"3", "1991-02-12"});
        //MonetUtil.printNST(DB.getConnection(),attrNST,log);

        addAttribute.addDateDiffAttributeFromTwoDates(attributes, "diff_attr",
                "date_attr", "date_attr2");

        attrDataNST = attributes.getAttrDataNST("diff_attr");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "1@0.1",
                "2@0.-32",
                "3@0.-397"}, new HashSet<String>(valList));

        attributes.deleteAttribute("date_attr");
        attributes.deleteAttribute("date_attr2");
        attributes.deleteAttribute("diff_attr");
    }

    public void testAddOutDegreeAttribute() {
        AddAttribute addAttribute = new AddAttribute();

        // this db has 4 objs and 2 links from buildDatabase(): 0-->1  2-->3
        addAttribute.addOutDegreeAttr("degree", DB.getLinkNST());

        Attributes attributes = DB.getObjectAttrs();
        NST attrDataNST = attributes.getAttrDataNST("degree");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.1",
                "2@0.1"}, new HashSet<String>(valList));
        attributes.deleteAttribute("degree");

        // Now make a bidirectional link NST and try again
        NST linkNST = DB.getLinkNST().copy();
        NST linkNSTRev = linkNST.project("link_id, o2_id, o1_id");
        linkNST.insertRowsFromNST(linkNSTRev); // the link_ids are repeated, but it doesn't matter for the test
        addAttribute.addOutDegreeAttr("degree", linkNST);

        attrDataNST = attributes.getAttrDataNST("degree");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.1",
                "1@0.1",
                "2@0.1",
                "3@0.1",}, new HashSet<String>(valList));
        attributes.deleteAttribute("degree");
        linkNST.release();
        linkNSTRev.release();
    }

    public void testAddDegreeAttribute2() {
        AddAttribute addAttribute = new AddAttribute();
        // add one more link, so now we have: 0->1, 2->3, and 0->3
        DB.insertLink(o0ID, o3ID);
        addAttribute.addOutDegreeAttr("degree", DB.getLinkNST());

        Attributes attributes = DB.getObjectAttrs();
        NST attrDataNST = attributes.getAttrDataNST("degree");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.2",
                "2@0.1"}, new HashSet<String>(valList));
        attributes.deleteAttribute("degree");

    }

    public void testAddIDAttribute() {
        AddAttribute addAttribute = new AddAttribute();

        Attributes attributes = DB.getObjectAttrs();
        addAttribute.addIDAttribute("id", true);
        NST attrDataNST = attributes.getAttrDataNST("id");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.0@0",
                "1@0.1@0",
                "2@0.2@0",
                "3@0.3@0"}, new HashSet<String>(valList));
        attributes.deleteAttribute("id");

        attributes = DB.getLinkAttrs();
        addAttribute.addIDAttribute("id", false);
        attrDataNST = attributes.getAttrDataNST("id");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.0@0",
                "1@0.1@0"}, new HashSet<String>(valList));
        attributes.deleteAttribute("id");
    }

    public void testAddRandomIdAttribute() {
        AddAttribute addAttribute = new AddAttribute();

        Attributes attributes = DB.getObjectAttrs();
        addAttribute.addRandomAttribute("rand", true);
        NST attrDataNST = attributes.getAttrDataNST("rand");
        List<String> valList = attrDataNST.selectRows().toStringList(2);
        for (int i = 0; i < valList.size(); i++) {
            double val = Double.parseDouble(valList.get(i));
            Assert.condition(val >= 0.0 && val <= 1.0, "bad random attr val");
        }
        attributes.deleteAttribute("rand");

        attributes = DB.getLinkAttrs();
        addAttribute.addRandomAttribute("rand", false);
        attrDataNST = attributes.getAttrDataNST("rand");
        valList = attrDataNST.selectRows().toStringList(2);
        for (int i = 0; i < valList.size(); i++) {
            double val = Double.parseDouble(valList.get(i));
            Assert.condition(val >= 0.0 && val <= 1.0, "bad random attr val");
        }
        attributes.deleteAttribute("rand");
    }

    public void testAddLinkAttributeFromObject() {
        new AddAttribute().addLinkAttributeFromObject("type = 'l'", true, "B", "link_B");
        assertTrue(DB.getLinkAttrs().isAttributeDefined("link_B"));
        TestUtil.verifyCollections(new String[]{
                link01ID + "@0.-1",
                link23ID + "@0.11"}, DB.getLinkAttrs().getAttrDataNST("link_B"));

        // no value defined for Obj 3, so link23ID doesn't get a value
        new AddAttribute().addLinkAttributeFromObject("type = 'l'", false, "B", "link_C");
        assertTrue(DB.getLinkAttrs().isAttributeDefined("link_C"));
        TestUtil.verifyCollections(new String[]{
                link01ID + "@0.7"}, DB.getLinkAttrs().getAttrDataNST("link_C"));

        // test * (all links)
        new AddAttribute().addLinkAttributeFromObject("*", true, "B", "link_D");
        assertTrue(DB.getLinkAttrs().isAttributeDefined("link_D"));
        TestUtil.verifyCollections(new String[]{
                link01ID + "@0.-1",
                link23ID + "@0.11"}, DB.getLinkAttrs().getAttrDataNST("link_D"));
    }

    public void testAddObjectAttributeFromLink() {
        // Link from 2->3 has type both x and l, so o2ID gets two rows
        // o1ID and o3ID are not the starting end of any link, so they don't get the attr
        new AddAttribute().addObjectAttributeFromLink("type = 'l'", "type", "object_type", true);
        assertTrue(DB.getObjectAttrs().isAttributeDefined("object_type"));
        TestUtil.verifyCollections(new String[]{
                o0ID + "@0.l",
                o2ID + "@0.l",
                o2ID + "@0.x"}, DB.getObjectAttrs().getAttrDataNST("object_type"));

        // test the filter. only get type = 'x'
        // o2ID still gets two, one for l and 1 for x
        new AddAttribute().addObjectAttributeFromLink("type = 'x'", "type", "object_type2", true);
        assertTrue(DB.getObjectAttrs().isAttributeDefined("object_type2"));
        TestUtil.verifyCollections(new String[]{
                o2ID + "@0.l",
                o2ID + "@0.x"}, DB.getObjectAttrs().getAttrDataNST("object_type2"));

        // test all links, and on o2
        // in this case, it's o3ID that gets two rows
        new AddAttribute().addObjectAttributeFromLink("*", "type", "object_type3", false);
        assertTrue(DB.getObjectAttrs().isAttributeDefined("object_type3"));
        TestUtil.verifyCollections(new String[]{
                o1ID + "@0.l",
                o3ID + "@0.l",
                o3ID + "@0.x"}, DB.getObjectAttrs().getAttrDataNST("object_type3"));
    }

    public void testAddRandomAttribute() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        Container container = DB.getRootContainer().getChild("child");

        addAttribute.addRandomAttribute(container, true, "X", "RandomAttr");

        Assert.condition(attributes.isAttributeDefined("RandomAttr"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("RandomAttr");
        ResultSet resultSet = attrDataNST.selectRows();

        assertEquals(resultSet.getRowCount(), 3);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 0);
        assertEquals(resultSet.getFloat(2), 0.5, 0.5);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 1);
        assertEquals(resultSet.getFloat(2), 0.5, 0.5);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 2);
        assertEquals(resultSet.getFloat(2), 0.5, 0.5);

        attributes.deleteAttribute("RandomAttr");
    }

    public void testAddRandomBinaryAttribute() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        Container container = DB.getRootContainer().getChild("child");
        addAttribute.addRandomBinaryAttributeWithPrior(container, true, "X", "RandomAttr", 0.5);

        Assert.condition(attributes.isAttributeDefined("RandomAttr"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("RandomAttr");

        ResultSet resultSet = attrDataNST.selectRows();

        assertEquals(resultSet.getRowCount(), 3);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 0);
        assertTrue(resultSet.getInt(2) == 1 || resultSet.getInt(2) == 0);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 1);
        assertTrue(resultSet.getInt(2) == 1 || resultSet.getInt(2) == 0);
        resultSet.next();
        assertEquals(resultSet.getOID(1), 2);
        assertTrue(resultSet.getInt(2) == 1 || resultSet.getInt(2) == 0);

        attributes.deleteAttribute("RandomAttr");
    }

    public void testAddYearAttributeFromDate() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        attributes.defineAttribute("date_attr", "date");
        NST attrDataNST = attributes.getAttrDataNST("date_attr");
        attrDataNST.insertRow(new String[]{"1", "2002-10-03"});
        attrDataNST.insertRow(new String[]{"2", "2004-01-22"});
        attrDataNST.insertRow(new String[]{"3", "1990-01-11"});

        addAttribute.addYearAttributeFromDate(attributes, "year_attr", "date_attr");

        attrDataNST = attributes.getAttrDataNST("year_attr");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "1@0.2002",
                "2@0.2004",
                "3@0.1990"}, new HashSet<String>(valList));

        attributes.deleteAttribute("date_attr");
        attributes.deleteAttribute("year_attr");
    }

    public void testAggregate() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        addAttribute.addAttribute(attributes, "C", "max(b)");

        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.-1",
                "1@0.7",
                "2@0.11"}, new HashSet<String>(valList));

        attributes.deleteAttribute("C");
    }

    public void testArithmetic() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        addAttribute.addAttribute(attributes, "C", "\"Ross\"   +   a");

        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.Ross3",
                "1@0.Ross5",
                "2@0.Ross8"}, new HashSet<String>(valList));

        attributes.deleteAttribute("C");
    }

    public void testAttribute() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        addAttribute.addAttribute(attributes, "C", "a");

        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.3",
                "1@0.5",
                "2@0.8"}, new HashSet<String>(valList));

        attributes.deleteAttribute("C");
    }

    public void testAttributeForContainer() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();
        Container childContainer;
        Container rootContainer = DB.getRootContainer();
        List<String> valList;
        NST attrDataNST;

        childContainer = rootContainer.getChild("child");
        addAttribute.addAttribute(childContainer, true, "D", "container_obj_attr");
        attrDataNST = attributes.getAttrDataNST("D");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);

        TestUtil.verifyCollections(new String[]{
                "0@0.2",
                "1@0.4",
                "2@0.6"}, new HashSet<String>(valList));

        attributes.deleteAttribute("D");
    }

    public void testCopyAttrFromItem() {
        Attributes attributes = DB.getObjectAttrs();

        Container container = DB.getRootContainer().getChild("child2");
        AddAttribute addAttr = new AddAttribute();
        addAttr.copyAttrFromItem(container, true, "X", "A", true, "Y", "new_attr");
        Assert.condition(attributes.isAttributeDefined("new_attr"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("new_attr");
        ResultSet resultSet = attrDataNST.selectRows();

        List<String> resultList = new ArrayList<String>();
        while (resultSet.next()) {
            int id = resultSet.getOID(1);
            int value = resultSet.getInt(2);
            resultList.add(id + " " + value);
            log.debug(id + ": " + value);
        }
        assertEquals(resultSet.getRowCount(), 4);
        assertTrue(resultList.contains(o2ID + " " + 3));
        assertTrue(resultList.contains(o2ID + " " + 5));
        assertTrue(resultList.contains(o3ID + " " + 3));
        assertTrue(resultList.contains(o3ID + " " + 5));

        attributes.deleteAttribute("new_attr");
        attrDataNST.release();

        // test copying an attribute from an object to a link
        int subgID2 = 2;
        Subgraph subgraph = container.getSubgraph(subgID2);
        subgraph.insertLink(link01ID, "L");
        // todo: make this function not break if subg 3 has no links
        int subgID3 = 3;
        subgraph = container.getSubgraph(subgID3);
        subgraph.insertLink(link23ID, "L");

        addAttr.copyAttrFromItem(container, true, "X", "A", false, "L", "new_attr");
        Assert.condition(DB.getLinkAttrs().isAttributeDefined("new_attr"), "Error: failed to define attr");

        attrDataNST = DB.getLinkAttrs().getAttrDataNST("new_attr");
        resultSet = attrDataNST.selectRows();

        resultList = new ArrayList<String>();
        while (resultSet.next()) {
            int id = resultSet.getOID(1);
            int value = resultSet.getInt(2);
            resultList.add(id + " " + value);
            log.debug(id + ": " + value);
        }
        assertEquals(resultSet.getRowCount(), 3);
        assertTrue(resultList.contains(link01ID + " " + 3));
        assertTrue(resultList.contains(link01ID + " " + 5));
        assertTrue(resultList.contains(link23ID + " " + 3));
        DB.getLinkAttrs().deleteAttribute("new_attr");
        attrDataNST.release();
    }

    public void testCopyAttrFromItemToSubgraph() {
        Container container = DB.getRootContainer().getChild("child2");
        new AddAttribute().copyAttrFromItemToSubgraph(container, true, "X", "A", "new_attr");

        Attributes attributes = container.getSubgraphAttrs();
        Assert.condition(attributes.isAttributeDefined("new_attr"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("new_attr");
        ResultSet resultSet = attrDataNST.selectRows();
        List<String> resultList = new ArrayList<String>();
        while (resultSet.next()) {
            resultList.add(resultSet.getOID(1) + " " + resultSet.getInt(2));
        }
        assertEquals(3, resultSet.getRowCount());
        assertTrue(resultList.contains(o2ID + " " + 3));
        assertTrue(resultList.contains(o2ID + " " + 5));
        assertTrue(resultList.contains(o3ID + " " + 3));

        attributes.deleteAttribute("new_attr");
    }

    public void testCopyAttrFromSubgraphToItem() {
        Attributes attributes = DB.getObjectAttrs();
        attributes.deleteAttributeIfExists("B");

        // Copy from an Item to a Subgraph
        Container container = DB.getRootContainer().getChild("child2");
        new AddAttribute().copyAttrFromItemToSubgraph(container, true, "X", "A", "new_attr");

        // And now copy back to a new item attribute
        new AddAttribute().copyAttrFromSubgraphToItem(container, "new_attr", true, "X", "B");

        Assert.condition(attributes.isAttributeDefined("B"), "Error: failed to define attr");

        NST attrDataNST = attributes.getAttrDataNST("B");
        ResultSet resultSet = attrDataNST.selectRows();
        List<String> resultList = new ArrayList<String>();
        while (resultSet.next()) {
            resultList.add(resultSet.getOID(1) + " " + resultSet.getInt(2));
        }
        assertEquals(4, resultSet.getRowCount());
        assertTrue(resultList.contains(o0ID + " " + 3));
        assertTrue(resultList.contains(o0ID + " " + 5));
        assertTrue(resultList.contains(o1ID + " " + 3));
        assertTrue(resultList.contains(o1ID + " " + 5));
    }

    public void testDifferentTypes() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        String switchStr =
                "5 < flt_attr    ==>\t1, " +
                        "default ==> 3";
        addAttribute.addAttribute(attributes, "C", switchStr);
        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.3",
                "1@0.3",
                "2@0.1"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");

        switchStr =
                "dbl_attr > 5    ==>\t1, " +
                        "default ==> 3";
        addAttribute.addAttribute(attributes, "C", switchStr);
        attrDataNST = attributes.getAttrDataNST("C");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.3",
                "1@0.3",
                "2@0.1"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");
    }

    public void testIdsWithAtLeastOneAttrDefined() {

        // set up situation where object 4 appears twice in every attribute
        // table it appears in.  (simplest case: it has one attribute, two values of it.)

        Attributes objectAttrs = DB.getObjectAttrs();
        NST attrDataNST = objectAttrs.getAttrDataNST("A");
        attrDataNST.insertRow(new String[]{o3ID + "", "4"});
        attrDataNST.insertRow(new String[]{o3ID + "", "5"});

        AddAttribute addAttribute = new AddAttribute();
        addAttribute.addAttribute(objectAttrs, "C", "\"hello\"");

        attrDataNST = objectAttrs.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);

        // before using this list as a HashSet, see if it contains duplicates
        HashSet<String> hs = new HashSet<String>(valList);
        assertEquals(valList.size(), hs.size());

        TestUtil.verifyCollections(new String[]{
                "0@0.hello",
                "1@0.hello",
                "2@0.hello",
                "3@0.hello"}, hs);


        buildDatabase();    // clean up
    }

    public void testKeepValues() {
        // Add first round
        Container container = DB.getRootContainer().getChild("child");
        new AddAttribute().addConstantAttribute(container, true, "X", "ConstantAttr", new Integer(1));

        // Add again, should give an error
        try {
            new AddAttribute().addConstantAttribute(container, true, "X", "ConstantAttr", new Integer(1));
            fail("Should have complained about existing attribute");
        } catch (Exception e) {
            // ignore
        }

        // Add again, but with keep values
        new AddAttribute(true).addConstantAttribute(container, true, "X", "ConstantAttr", new Integer(2));
        Attributes attributes = DB.getObjectAttrs();
        NST attrDataNST = attributes.getAttrDataNST("ConstantAttr");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.1",
                "1@0.1",
                "2@0.1",
                "0@0.2",
                "1@0.2",
                "2@0.2"}, new HashSet<String>(valList));

        // uncomment this to check for the failure of sending in the wrong item type
        // addAttribute.addConstantAttribute(container, item, "ConstantAttr", item);

        attributes.deleteAttribute("ConstantAttr");
    }

    public void testLiteral() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        addAttribute.addAttribute(attributes, "C", "\"Ross\"");

        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.Ross",
                "1@0.Ross",
                "2@0.Ross"}, new HashSet<String>(valList));

        attributes.deleteAttribute("C");
    }

    public void testSwitch() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        String switchStr =
                "a != 5 AND b != 8    ==>\t1, " +
                        "b > 0 ==> 2, " +
                        "default ==> 3";
        addAttribute.addAttribute(attributes, "C", switchStr);
        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.1",
                "1@0.2",
                "2@0.1"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");

        String switchStr2 =
                "a != 5 AND b != 8    ==>\t\"N\", " +
                        "default ==> \"Y\"";
        addAttribute.addAttribute(attributes, "D", switchStr2);
        attrDataNST = attributes.getAttrDataNST("D");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.N",
                "1@0.Y",
                "2@0.N"}, new HashSet<String>(valList));
        attributes.deleteAttribute("D");
    }

    public void testSwitchDivideByZero() {
        AddAttribute addAttribute = new AddAttribute();
        Attributes attributes = DB.getObjectAttrs();

        addAttribute.addAttribute(attributes, "D", "0");
        String switchStr =
                "d != 0  ==> a / d, " +
                        "default ==> -99";
        addAttribute.addAttribute(attributes, "C", switchStr);
        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.-99",
                "1@0.-99",
                "2@0.-99"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");
        attributes.deleteAttribute("D");
    }

    public void testSwitchWithAttrsToConsider() {
        Attributes attributes = DB.getObjectAttrs();

        String switchStr =
                "a <= 5 ==> 3, " +
                        "default ==> 8";
        new AddAttribute(Arrays.asList("A")).addAttribute(attributes, "C", switchStr);
        NST attrDataNST = attributes.getAttrDataNST("C");
        List<String> valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.3",
                "1@0.3",
                "2@0.8"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");


        switchStr = "b <= 7 ==> 3, " +
                "default ==> 8";
        new AddAttribute(Arrays.asList("B")).addAttribute(attributes, "C", switchStr);
        attrDataNST = attributes.getAttrDataNST("C");
        valList = TestUtil.getDelimStringListForNST(attrDataNST);
        TestUtil.verifyCollections(new String[]{
                "0@0.3",
                "1@0.3",
                "2@0.8"}, new HashSet<String>(valList));
        attributes.deleteAttribute("C");
    }
}
