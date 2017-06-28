/**
 * $Id: TemporalItemSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TemporalItemSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.common.sources;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.util.NSTCache;

/**
 * kdl.prox.model2.common.sources
 * User: afast
 * Date: Apr 23, 2007
 * $Id: TemporalItemSourceTest.java 3658 2007-10-15 16:29:11Z schapira $
 */
public class TemporalItemSourceTest extends TestCase {

    Container container;

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        container = DB.createNewTempContainer();
        container.getObjectsNST().insertRow("1, 1, A");
        container.getObjectsNST().insertRow("2, 1, B");
        container.getObjectsNST().insertRow("3, 1, C");
        container.getObjectsNST().insertRow("4, 1, C");
        container.getObjectsNST().insertRow("5, 2, A");
        container.getObjectsNST().insertRow("6, 2, B");
        container.getObjectsNST().insertRow("7, 2, C");
        container.getObjectsNST().insertRow("8, 2, C");

        container.getSubgraphAttrs().defineAttribute("coreTime", "int");
        container.getSubgraphAttrs().getAttrDataNST("coreTime").insertRow("1, 1994");
        container.getSubgraphAttrs().getAttrDataNST("coreTime").insertRow("2, 2000");

        DB.getObjectAttrs().deleteAllAttributes();
        DB.getObjectAttrs().defineAttribute("salary", "flt");
        DB.getObjectAttrs().getAttrDataNST("salary").insertRow("3,1000").insertRow("4,10000");
        DB.getObjectAttrs().getAttrDataNST("salary").insertRow("7,1000").insertRow("8,10000");

        DB.getObjectAttrs().defineAttribute("related_time", "int");
        DB.getObjectAttrs().getAttrDataNST("related_time").insertRow("3,1993").insertRow("4,1995");
        DB.getObjectAttrs().getAttrDataNST("related_time").insertRow("7,1994").insertRow("8,2003");


    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    public void testIt() {
        ItemSource aggItem = new ItemSource("C");
        AttributeSource coreTime = new AttributeSource("coreTime");
        AttributeSource relatedTime = new AttributeSource("C", "related_time");

        TemporalItemSource temporalItemSource = new TemporalItemSource(aggItem, coreTime, relatedTime, "-1:0");
        NSTCache cache = new NSTCache();
        temporalItemSource.init(container, cache);
        NST dataTable = cache.getTable("src" + temporalItemSource.getSignature() + "_" + aggItem);

        TestUtil.verifyCollections(new String[]{"1@0.3@0.C.1993.1994",
                "1@0.4@0.C.1995.1994",
                "2@0.7@0.C.1994.2000",
                "2@0.8@0.C.2003.2000"}, dataTable);

        NST filteredTable = temporalItemSource.getSourceTable();
        TestUtil.verifyCollections(new String[]{"1@0.C"}, filteredTable);
    }

}