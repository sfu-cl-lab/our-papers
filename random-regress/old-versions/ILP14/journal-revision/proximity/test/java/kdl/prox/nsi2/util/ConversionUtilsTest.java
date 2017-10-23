/**
 * $Id: ConversionUtilsTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ConversionUtilsTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.nsi2.util;

import junit.framework.TestCase;
import kdl.prox.TestUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: maier
 * Date: May 21, 2007
 * Time: 4:45:49 PM
 */
public class ConversionUtilsTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testintArraytoSet() {
        int[] targets = new int[]{0, 1, 2, 3, 4};
        Set<Integer> actual = ConversionUtils.intArraytoSet(targets);
        Set<Integer> expected = new HashSet<Integer>();
        expected.add(0);
        expected.add(1);
        expected.add(2);
        expected.add(3);
        expected.add(4);
        TestUtil.verifyCollections(expected, actual);
    }

    public void teststringToIntegerList() {
        List<Integer> actual = ConversionUtils.stringToIntegerList("0,1,2,3,4");
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        expected.add(1);
        expected.add(2);
        expected.add(3);
        expected.add(4);
        TestUtil.verifyCollections(expected, actual);
    }


}
