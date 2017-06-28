/**
 * $Id: AttributeGeneratorTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.attributes;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.datagen2.structure.SyntheticGraphIID;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.modules.learning.DefaultLearningModule;
import kdl.prox.model2.rpt.modules.significance.DefaultSignificanceModule;
import kdl.prox.model2.rpt.modules.splitting.DefaultSplittingModule;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Util;
import kdl.prox.util.stat.NormalDistribution;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

public class AttributeGeneratorTest extends TestCase {

    private static final Logger log = Logger.getLogger(AttributeGeneratorTest.class);

    private static final String IID_CORE_S_QUERY_NAME = "iid-coreS-query-test.xml";
    private static final String IID_CORE_T_QUERY_NAME = "iid-coreT-query-test.xml";
    private File coreSFile;
    private File coreTFile;

    private static final String S_ATTR_PREFIX = "s_attr";        // prefix for S attributes. combine with number at end starting with 0, e.g., "s_attr0"
    private static final String T_ATTR_PREFIX = "t_attr";        // "" T ""

    private static String sContainerName = "sContainer";
    private static String tContainerName = "tContainer";
    private static String outputContainer = "test-cont";

    Container sTrainContainer;  // only used to create an RPT to predict s objects (overwritten by datagenerator)
    Container tTrainContainer;  // only used to create an RPT to predict t objects (overwritten by datagenerator)
    Container RDNContainer;

    AttributeSource genderSource;
    AttributeSource ageSource;
    AttributeSource heightSource;

    public void setUp() throws Exception {
        super.setUp();

        Util.initProxApp();

        TestUtil.openTestConnection();

        DB.clearDB();
        DB.initEmptyDB();

        DB.getObjectNST().insertRow("1");
        DB.getObjectNST().insertRow("2");
        DB.getObjectNST().insertRow("3");
        DB.getObjectNST().insertRow("4");
        DB.getObjectNST().insertRow("5");
        DB.getObjectNST().insertRow("6");
        DB.getObjectNST().insertRow("7");
        DB.getObjectNST().insertRow("8");
        DB.getObjectNST().insertRow("9");

        DB.getLinkNST().insertRow("1, 1, 2"); // link id, o1 id, o2 id
        DB.getLinkNST().insertRow("2, 1, 3");
        DB.getLinkNST().insertRow("3, 4, 5");
        DB.getLinkNST().insertRow("4, 4, 6");
        DB.getLinkNST().insertRow("5, 7, 8");
        DB.getLinkNST().insertRow("6, 7, 9");

        DB.getObjectAttrs().deleteAllAttributes();

        DB.getObjectAttrs().defineAttribute(SyntheticGraphIID.getObjTypeAttrName(), "str");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("1, S");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("2, T");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("3, T");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("4, S");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("5, T");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("6, T");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("7, S");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("8, T");
        DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName()).insertRow("9, T");

        DB.getObjectAttrs().defineAttribute("age", "int");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("1, 40");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("4, 45");
        DB.getObjectAttrs().getAttrDataNST("age").insertRow("7, 10");

        DB.getObjectAttrs().defineAttribute("gender", "str");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("2, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("3, m");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("5, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("6, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("8, f");
        DB.getObjectAttrs().getAttrDataNST("gender").insertRow("9, f");

        DB.getObjectAttrs().defineAttribute("height", "flt");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("2, 5.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("3, 6.5");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("5, 5.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("6, 6.09");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("8, 6.4");
        DB.getObjectAttrs().getAttrDataNST("height").insertRow("9, 7.4");

        // create two containers for RPT training
        RDNContainer = DB.getRootContainer().createChild(outputContainer);
        sTrainContainer = DB.getRootContainer().createChild(sContainerName);
        tTrainContainer = DB.getRootContainer().createChild(tContainerName);

        sTrainContainer.getObjectsNST().insertRow("1, 1, S");
        sTrainContainer.getObjectsNST().insertRow("2, 1, T");
        sTrainContainer.getObjectsNST().insertRow("3, 1, T");
        sTrainContainer.getLinksNST().insertRow("1, 1, linkedTo");
        sTrainContainer.getLinksNST().insertRow("2, 1, linkedTo");

        sTrainContainer.getObjectsNST().insertRow("4, 2, S");
        sTrainContainer.getObjectsNST().insertRow("5, 2, T");
        sTrainContainer.getObjectsNST().insertRow("6, 2, T");
        sTrainContainer.getLinksNST().insertRow("3, 2, linkedTo");
        sTrainContainer.getLinksNST().insertRow("4, 2, linkedTo");

        sTrainContainer.getObjectsNST().insertRow("7, 3, S");
        sTrainContainer.getObjectsNST().insertRow("8, 3, T");
        sTrainContainer.getObjectsNST().insertRow("9, 3, T");
        sTrainContainer.getLinksNST().insertRow("5, 3, linkedTo");
        sTrainContainer.getLinksNST().insertRow("6, 3, linkedTo");


        tTrainContainer.getObjectsNST().insertRow("2, 1, T");
        tTrainContainer.getObjectsNST().insertRow("1, 1, S");
        tTrainContainer.getObjectsNST().insertRow("3, 1, linkedT");

        tTrainContainer.getObjectsNST().insertRow("3, 2, T");
        tTrainContainer.getObjectsNST().insertRow("1, 2, S");
        tTrainContainer.getObjectsNST().insertRow("2, 2, linkedT");

        tTrainContainer.getObjectsNST().insertRow("5, 3, T");
        tTrainContainer.getObjectsNST().insertRow("4, 3, S");
        tTrainContainer.getObjectsNST().insertRow("6, 3, linkedT");

        tTrainContainer.getObjectsNST().insertRow("6, 4, T");
        tTrainContainer.getObjectsNST().insertRow("4, 4, S");
        tTrainContainer.getObjectsNST().insertRow("5, 4, linkedT");

        tTrainContainer.getObjectsNST().insertRow("8, 5, T");
        tTrainContainer.getObjectsNST().insertRow("7, 5, S");
        tTrainContainer.getObjectsNST().insertRow("9, 5, linkedT");

        tTrainContainer.getObjectsNST().insertRow("9, 6, T");
        tTrainContainer.getObjectsNST().insertRow("7, 6, S");
        tTrainContainer.getObjectsNST().insertRow("8, 6, linkedT");

        RDNContainer.getObjectsNST().insertRow("1, 1, S");
        RDNContainer.getObjectsNST().insertRow("2, 1, T");
        RDNContainer.getObjectsNST().insertRow("3, 1, T");
        RDNContainer.getLinksNST().insertRow("1, 1, linkedTo");
        RDNContainer.getLinksNST().insertRow("2, 1, linkedTo");

        RDNContainer.getObjectsNST().insertRow("4, 2, S");
        RDNContainer.getObjectsNST().insertRow("5, 2, T");
        RDNContainer.getObjectsNST().insertRow("6, 2, T");
        RDNContainer.getLinksNST().insertRow("3, 2, linkedTo");
        RDNContainer.getLinksNST().insertRow("4, 2, linkedTo");

        RDNContainer.getObjectsNST().insertRow("7, 3, S");
        RDNContainer.getObjectsNST().insertRow("8, 3, T");
        RDNContainer.getObjectsNST().insertRow("9, 3, T");
        RDNContainer.getLinksNST().insertRow("5, 3, linkedTo");
        RDNContainer.getLinksNST().insertRow("6, 3, linkedTo");

        RDNContainer.getObjectsNST().insertRow("2, 4, T");
        RDNContainer.getObjectsNST().insertRow("1, 4, S");
        RDNContainer.getObjectsNST().insertRow("3, 4, linkedT");
        RDNContainer.getLinksNST().insertRow("1, 4, linksFrom");
        RDNContainer.getLinksNST().insertRow("2, 4, linksTo");

        RDNContainer.getObjectsNST().insertRow("3, 5, T");
        RDNContainer.getObjectsNST().insertRow("1, 5, S");
        RDNContainer.getObjectsNST().insertRow("2, 5, linkedT");
        RDNContainer.getLinksNST().insertRow("2, 5, linksFrom");
        RDNContainer.getLinksNST().insertRow("1, 5, linksTo");

        RDNContainer.getObjectsNST().insertRow("5, 6, T");
        RDNContainer.getObjectsNST().insertRow("4, 6, S");
        RDNContainer.getObjectsNST().insertRow("6, 6, linkedT");
        RDNContainer.getLinksNST().insertRow("3, 6, linksFrom");
        RDNContainer.getLinksNST().insertRow("4, 6, linksTo");

        RDNContainer.getObjectsNST().insertRow("6, 7, T");
        RDNContainer.getObjectsNST().insertRow("4, 7, S");
        RDNContainer.getObjectsNST().insertRow("5, 7, linkedT");
        RDNContainer.getLinksNST().insertRow("4, 7, linksFrom");
        RDNContainer.getLinksNST().insertRow("3, 7, linksTo");

        RDNContainer.getObjectsNST().insertRow("8, 8, T");
        RDNContainer.getObjectsNST().insertRow("7, 8, S");
        RDNContainer.getObjectsNST().insertRow("9, 8, linkedT");
        RDNContainer.getLinksNST().insertRow("5, 8, linksFrom");
        RDNContainer.getLinksNST().insertRow("6, 8, linksTo");

        RDNContainer.getObjectsNST().insertRow("9, 9, T");
        RDNContainer.getObjectsNST().insertRow("7, 9, S");
        RDNContainer.getObjectsNST().insertRow("8, 9, linkedT");
        RDNContainer.getLinksNST().insertRow("6, 9, linksFrom");
        RDNContainer.getLinksNST().insertRow("5, 9, linksTo");

        genderSource = new AttributeSource("T", "gender");
        ageSource = new AttributeSource("S", "age");
        heightSource = new AttributeSource("T", "height");
        heightSource.setIsContinuous(false);


        URL xmlFileURL = getClass().getResource(IID_CORE_S_QUERY_NAME);
        if (xmlFileURL == null) {
            throw new IllegalArgumentException("Could not find query file " + IID_CORE_S_QUERY_NAME);
        }
        coreSFile = new File(xmlFileURL.getFile());

        xmlFileURL = getClass().getResource(IID_CORE_T_QUERY_NAME);
        if (xmlFileURL == null) {
            throw new IllegalArgumentException("Could not find query file " + IID_CORE_T_QUERY_NAME);
        }
        coreTFile = new File(xmlFileURL.getFile());

    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testGenerateIIDAttributes() {
        // set up some variables
        int maxIterations = 10;

        // create some RPT models
        RPT sRPT = new RPT();
        RPT tRPT = new RPT();

        // Create simple 1-level rpt.  use height to predict age
        ((DefaultLearningModule) sRPT.learningModule).splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        ((DefaultLearningModule) sRPT.learningModule).significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        sRPT.learn(sTrainContainer, ageSource, new Source[]{heightSource});
        sRPT.printFull();

        // Create simple 1-level rpt.  use gender to predict height
        ((DefaultLearningModule) tRPT.learningModule).splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        ((DefaultLearningModule) tRPT.learningModule).significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        tRPT.learn(tTrainContainer, genderSource, new Source[]{ageSource});
        tRPT.printFull();

        HashMap<File, RPT> queryToModelMap = new HashMap<File, RPT>();
        queryToModelMap.put(coreSFile, sRPT);
        queryToModelMap.put(coreTFile, tRPT);

        // create a new data generator
        new AttributeGenerator(queryToModelMap, maxIterations);

        // verify the data is as expected
        String itemName = sRPT.getClassLabel().getItemName();
        String rptAttrName = sRPT.getClassLabel().getAttrName();
        String attrName = itemName + "_" + rptAttrName + "_label";

        NST attrNST = DB.getObjectAttrs().getAttrDataNST(attrName);
        attrNST.print();

        TestUtil.verifyCollections(new Object[]{"1@0", "4@0", "7@0"}, attrNST.project("id"));

        itemName = tRPT.getClassLabel().getItemName();
        rptAttrName = tRPT.getClassLabel().getAttrName();
        attrName = itemName + "_" + rptAttrName + "_label";
        attrNST = DB.getObjectAttrs().getAttrDataNST(attrName);
        attrNST.print();
        TestUtil.verifyCollections(new Object[]{"2@0", "3@0", "5@0", "6@0", "8@0", "9@0"}, attrNST.project("id"));

        // Finally, verify that there are no containers left
        assertEquals(3, DB.getRootContainer().getChildrenCount()); //tcontainer, scontainer, test-cont (all for RPT learn)

    }

    public void testIIDStructureAndAttributes() {
        int numSObjs = 2;
        int numTObjs = 2;

        DB.getObjectAttrs().defineAttribute(S_ATTR_PREFIX + "0", "str");
        DB.getObjectAttrs().getAttrDataNST(S_ATTR_PREFIX + "0").insertRow("1, red");
        DB.getObjectAttrs().getAttrDataNST(S_ATTR_PREFIX + "0").insertRow("4, red");
        DB.getObjectAttrs().getAttrDataNST(S_ATTR_PREFIX + "0").insertRow("7, red");

        DB.getObjectAttrs().defineAttribute(T_ATTR_PREFIX + "0", "str");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("2, 0");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("3, 0");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("5, 0");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("6, 0");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("8, 0");
        DB.getObjectAttrs().getAttrDataNST(T_ATTR_PREFIX + "0").insertRow("9, 0");

        AttributeSource sAttrSource = new AttributeSource("S", S_ATTR_PREFIX + "0");
        AttributeSource tAttrSource = new AttributeSource("T", T_ATTR_PREFIX + "0");

        // create some RPT models
        RPT sRPT = new RPT();
        RPT tRPT = new RPT();

        // Create simple 1-level rpt.  use height to predict age
        ((DefaultLearningModule) sRPT.learningModule).splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        ((DefaultLearningModule) sRPT.learningModule).significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        sRPT.learn(sTrainContainer, sAttrSource, new Source[]{heightSource});

        // Create simple 1-level rpt.  use gender to predict height
        ((DefaultLearningModule) tRPT.learningModule).splittingModule = new DefaultSplittingModule().setChooseDeterministically(true);
        ((DefaultLearningModule) tRPT.learningModule).significanceModule = new DefaultSignificanceModule().setpVal(0.01);
        tRPT.learn(tTrainContainer, tAttrSource, new Source[]{ageSource});

        DB.clearDB();
        DB.initEmptyDB();

        // generate subgraphs
        Object[][] degreeDistribs = new Object[][]{
                {1.0, new NormalDistribution((double) numTObjs, 0.0000001)}}; // tiny stdDev ensures deterministic selection of degree
        new SyntheticGraphIID(numSObjs, degreeDistribs);

        //double tolerance = 0.2;
        int maxIterations = 3;
        HashMap<File, RPT> queryToModelMap = new HashMap<File, RPT>();
        queryToModelMap.put(coreSFile, sRPT);
        queryToModelMap.put(coreTFile, tRPT);

        // create a new data generator
        new AttributeGenerator(queryToModelMap, maxIterations);

        // test S attribute values. do 2-way join between S
        // attributes (objecttype, s_attr1) to get results predicted
        // by the S CPD
        String itemName = sRPT.getClassLabel().getItemName();
        String rptAttrName = sRPT.getClassLabel().getAttrName();
        String attrName = itemName + "_" + rptAttrName + "_label";

        NST otAttrDataNST = DB.getObjectAttrs().getAttrDataNST(SyntheticGraphIID.getObjTypeAttrName());
        NST s1AttrDataNST = DB.getObjectAttrs().getAttrDataNST(attrName);

        DB.getObjects("*", "*").print();

        assertEquals(numSObjs, s1AttrDataNST.getRowCount());

        NST otS1NST = otAttrDataNST.join(s1AttrDataNST, "id = id").renameColumns("ot_id, ot_val, s1_id, s1_val");
        ResultSet resultSet = otS1NST.selectRows("ot_val, s1_val");
        assertEquals(numSObjs, resultSet.getRowCount());
        while (resultSet.next()) {
            assertEquals(SyntheticGraphIID.getCoreObjName(), resultSet.getString(1));
            assertEquals("red", resultSet.getString(2));
        }

        // test T attribute values. do 2-way join between T attributes
        // (objecttype, t_attr1) to get results predicted by the T CPD
        itemName = tRPT.getClassLabel().getItemName();
        rptAttrName = tRPT.getClassLabel().getAttrName();
        attrName = itemName + "_" + rptAttrName + "_label";

        NST t1AttrDataNST = DB.getObjectAttrs().getAttrDataNST(attrName);
        assertEquals(numSObjs * numTObjs, t1AttrDataNST.getRowCount());

        NST otT1NST = otAttrDataNST.join(t1AttrDataNST, "id = id").renameColumns("ot_id, ot_val, t1_id, t1_val");
        resultSet = otT1NST.selectRows("ot_val, t1_val");
        assertEquals(numSObjs * numTObjs, resultSet.getRowCount());
        while (resultSet.next()) {
            assertEquals(SyntheticGraphIID.getPeriphObjName(), resultSet.getString(1));
            assertEquals("0", resultSet.getString(2));
        }

        // Finally, verify that there are no containers left
        assertEquals(0, DB.getRootContainer().getChildrenCount());

        // add this back in later?
        //test final measurement of the empirical distribuion, for testing the
        // tolerance component

        otAttrDataNST.release();
        s1AttrDataNST.release();
        otS1NST.release();
        otT1NST.release();
    }

}
