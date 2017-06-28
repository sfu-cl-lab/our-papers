/**
 * $Id: RandomizeAttrTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.sample;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RandomizeAttrTest extends TestCase {

    private Logger log = Logger.getLogger(RandomizeAttrTest.class);

    private Attributes attrs;

    private final String ATTR_NAME = "number of slush puppies consumed per day";
    private final String RAND_ATTR_NAME = "some random meaningless number";

    protected void setUp() throws Exception {

        super.setUp();

        // connect to DB
        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

        // define attributes
        attrs = DB.getObjectAttrs();
        attrs.deleteAllAttributes();
        attrs.defineAttribute(ATTR_NAME, "int");

        // assign attribute values to objects
        /*
         obj-id  int-val
         1       101
         2       202
         3       303
         4       404
         5       505 // Man, that's a lot of slush puppies!
        */
        NST attrDataNST;
        attrDataNST = attrs.getAttrDataNST(ATTR_NAME);
        attrDataNST.deleteRows();
        attrDataNST.insertRow(new String[]{"41", "101"});
        attrDataNST.insertRow(new String[]{"42", "202"});
        attrDataNST.insertRow(new String[]{"43", "303"});
        attrDataNST.insertRow(new String[]{"44", "404"});
        attrDataNST.insertRow(new String[]{"45", "505"});
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }


    /**
     * Tests the randomize() function, which scrambles attr values
     */
    public void testRandomize() {
        RandomizeAttr.randomize(attrs, ATTR_NAME, RAND_ATTR_NAME);

        // check the to see that the values are represented
        List<Integer> vals = new ArrayList<Integer>();
        vals.add(new Integer(101));
        vals.add(new Integer(202));
        vals.add(new Integer(303));
        vals.add(new Integer(404));
        vals.add(new Integer(505));

        NST attrDataNST = attrs.getAttrDataNST(RAND_ATTR_NAME);
        assertEquals(5, attrDataNST.getRowCount());
        ResultSet s = attrDataNST.selectRows();
        while (s.next()) {
            Integer val = new Integer(s.getInt(2));
            assertTrue(vals.contains(val));
        }
    }

}
