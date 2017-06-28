/**
 * $Id: AggregatorFinderUtilTest.java 3749 2007-11-13 20:13:43Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AggregatorFinderUtilTest.java 3749 2007-11-13 20:13:43Z schapira $
 */

package kdl.prox.model2.rpt.aggregators;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests that we can find all the appropriate Aggregator classes
 * Modify tests as new classes are created
 */
public class AggregatorFinderUtilTest extends TestCase {

    private static final Logger log = Logger.getLogger(AggregatorFinderUtilTest.class);

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    /**
     * NB: requires being updated when new Aggregators are added.
     *
     * @return
     */
    private static Set getExpectedClasses() {
        Set<Class<?>> expectedClasses = new HashSet<Class<?>>();
        expectedClasses.add(AverageAggregator.class);
        expectedClasses.add(CountAggregator.class);
        expectedClasses.add(CountDistinctAggregator.class);
        expectedClasses.add(DegreeAggregator.class);
        expectedClasses.add(MaxAggregator.class);
        expectedClasses.add(MinAggregator.class);
        expectedClasses.add(ModeAggregator.class);
        expectedClasses.add(NopAggregator.class);
        expectedClasses.add(ProportionAggregator.class);
        expectedClasses.add(SumAggregator.class);
        return expectedClasses;
    }

    /**
     * Tests running using classpath directory for classes (normal developer mode).
     */
    public void testUsingClasspath() {
        Set expectedClasses = getExpectedClasses();
        Set actualClasses = AggregatorFinderUtil.searchForAggregatorClasses();
        if (!actualClasses.equals(expectedClasses)) {
            log.info("Found classes: " + actualClasses);
            log.info("Expected classes: " + expectedClasses);
            fail("Difference in the sets");
        }
    }


    /**
     * Exposes a bug when running Proximity on Windows and using proximity.jar
     * for classes (normal user mode). Following commands created the proximity.jar
     * file in this directory:
     * <pre>
     * $ cd proximity3/classes
     * $ zip ../temp.zip -r kdl/prox/model2/rpt/aggregators/
     * $ mv ../temp.zip ../test/java/kdl/prox/model2/rpt/aggregators/proximity-test.jar
     * </pre>
     * NB: Requires that IDEA be configured to copy *.jar files from source to
     * the compiled directory. NB: requires being updated when new Aggregators are
     * added.
     */
    public void testUsingJar() {
        Set expectedClasses = getExpectedClasses();

        // get our test proximity.jar
        Class rezClass = AggregatorFinderUtilTest.class;
        String testJarName = "proximity-test.jar";
        URL proxJarURL = rezClass.getResource(testJarName);
        Assert.notNull(proxJarURL, "test jar file not found: " + testJarName +
                ", relative to: " + rezClass);

        // get the actual classes using the test jar, and compare
        File testJarFile = new File(proxJarURL.getFile());
        Set actualClasses = AggregatorFinderUtil.searchForAggregatorClassesInternal(testJarFile);
        assertEquals(expectedClasses, actualClasses);
    }

}
