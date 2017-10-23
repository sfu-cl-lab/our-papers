/**
 * $Id: AggregatorTestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AggregatorTestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.aggregators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class AggregatorTestUtil extends TestCase {

    public static void verifyValues(NSTCache cache, List fsList, String[] expTables, String[][] expValues) {
        Assert.condition(expTables.length == expValues.length, "tables and length must be of the same size");

        List tableList = new ArrayList();
        for (int fsIdx = 0; fsIdx < fsList.size(); fsIdx++) {
            FeatureSetting featureSetting = (FeatureSetting) fsList.get(fsIdx);
            tableList.add(featureSetting.toString());
        }
        assertEquals("Number of tables don't match " + tableList, expTables.length, tableList.size());


        for (int tableIdx = 0; tableIdx < expTables.length; tableIdx++) {
            String expTable = expTables[tableIdx];
            if (!tableList.contains(expTable)) {
                fail(expTable + " not found in cache: " + tableList);
            } else {
                String[] expValue = expValues[tableIdx];
                List actualList = TestUtil.getDelimStringListForNST(cache.getTable(expTable));
                assertEquals(expValue.length, actualList.size());
                for (int eleIdx = 0; eleIdx < expValue.length; eleIdx++) {
                    Object element = expValue[eleIdx];
                    assertTrue("expected " + element + " (" + tableIdx + ") not found in actual set: " + actualList, actualList.contains(element));
                }
            }
        }
    }

    public void testIt() {
    }
}
