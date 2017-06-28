/**
 * $Id: FormatTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.Query;
import org.apache.log4j.Logger;


/**
 * Tests I/O of the three Format classes - annotations, conditions, constraints.
 * These tests all use a 'round-trip' approach: Call Format.parseObject() on an
 * input string, call Format.format() on the result, and compare the input and
 * output.
 */
public class FormatTest extends TestCase {

    private static final Logger log = Logger.getLogger(FormatTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // recall valid annotations: [i..j], [i..], or [i]
    public void testAnnotation() throws ParseException {
        // test good formats
        Annotation annotation;

        annotation = verifyAnnotation("[1]");
        assertEquals(1, annotation.annotMin());
        assertEquals(1, annotation.annotMax());

        annotation = verifyAnnotation("[1..2]");
        assertEquals(1, annotation.annotMin());
        assertEquals(2, annotation.annotMax());

        annotation = verifyAnnotation("[1..]");
        assertEquals(1, annotation.annotMin());
        assertEquals(-1, annotation.annotMax());
        
        // test bad formats
        try {
            verifyAnnotation("[1..1]");
            fail("invalid format");
        } catch (ParseException e) {
            // expected
        }

        try {
            verifyAnnotation("[..1]");
            fail("invalid format");
        } catch (ParseException e) {
            // expected
        }
    }

    /**
     * Test each condition Element in test-query-cond.xml:
     * <ol>
     * <li>load Element from test file
     * <li>generate user string via format()
     * <li>compare user string against expected value
     * <li>re-create Element via parseObject() on user string
     * <li>compare Element to original (using content-based Element comparison)
     * </ol>
     */
    public void testCondEleChild() throws Exception {
        // fill nameCondStringMap for test-query-cond.xml
        Map nameCondStringMap = new HashMap();
        nameCondStringMap.put("1", "and(attr = val , hot100 = 1)");
        nameCondStringMap.put("2", "attr = val");

        // test condEleChild Elements
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(), "test-query-cond.xml");
        List vertices = query.vertices(false);
        for (Iterator vertIter = vertices.iterator(); vertIter.hasNext();) {
            QGItem qgItem = (QGItem) vertIter.next();
            String expectedCondStr = (String) nameCondStringMap.get(qgItem.firstName());
            if (expectedCondStr != null) {
                verifyQGItem(qgItem, expectedCondStr);
            }
        }
    }

    /**
     * Test each QGConstraint in test-query-cond.xml:
     * <ol>
     * <li>load Query from test file
     * <li>generate user string via format()
     * <li>compare user string against expected value
     * <li>re-create QGConstraint via parseObject() on user string
     * <li>compare QGConstraint to original
     * </ol>
     * <p/>
     */
    public void testQGConstraint() throws Exception {
        // fill nameConstStringMap for test-query-cond.xml
        Map nameConstStringMap = new HashMap();
        nameConstStringMap.put("1", "1 <> 2");
        nameConstStringMap.put("2", "2.url = 1.type");
        nameConstStringMap.put("3", "3.url < 'vertex name with spaces'.'attr name with spaces'");

        // test QGConstraints
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(), "test-query-cond.xml");
        List qgConstraints = query.constraints();
        for (Iterator qgConstIter = qgConstraints.iterator(); qgConstIter.hasNext();) {
            QGConstraint qgConstraint = (QGConstraint) qgConstIter.next();
            String item1Name = qgConstraint.item1Name();
            String expectedConstStr = (String) nameConstStringMap.get(item1Name);
            if (expectedConstStr == null) {
                throw new IllegalArgumentException("no expected constraint " +
                        "string found for item1Name: " + item1Name);
            }

            verifyQGConstraint(qgConstraint, expectedConstStr);
        }
    }

    /**
     * Does a 'round-trip' test of AnnotationFormat on input by parsing it into
     * an Annotation, formatting the Annotation back into a string, and comparing
     * the result to the original input.
     *
     * @param input
     * @return
     * @throws ParseException
     */
    private Annotation verifyAnnotation(String input) throws ParseException {
        AnnotationFormat format = new AnnotationFormat();
        Annotation annotation = (Annotation) format.parseObject(input);
        String annotStr = format.format(annotation);
        assertEquals(input, annotStr);
        return annotation;
    }

    /**
     * Does a 'round-trip' test of QGConstraintFormat on qgConst by
     * formatting it into a user string, comparing that string to expectedConstStr,
     * parsing the user string back into a QGConstraint, and comparing the the
     * result to the original input.
     *
     * @param qgConst
     * @param expectedConstStr
     */
    private void verifyQGConstraint(QGConstraint qgConst, String expectedConstStr)
            throws ParseException {
        QGConstraintFormat qgConstFormat = new QGConstraintFormat();
        String constStr = qgConstFormat.format(qgConst);
        assertEquals(expectedConstStr, constStr);
        QGConstraint parsedQGConst = (QGConstraint) qgConstFormat.parseObject(constStr);
//        assertEquals(qgConst, parsedQGConst);     // doesn't work because QGConstraint.equals() isn't defined
        assertEquals(qgConst.operator(), parsedQGConst.operator());
        assertEquals(qgConst.item1Name(), parsedQGConst.item1Name());
        assertEquals(qgConst.item2Name(), parsedQGConst.item2Name());
        assertEquals(qgConst.item1AttrName(), parsedQGConst.item1AttrName());
        assertEquals(qgConst.item2AttrName(), parsedQGConst.item2AttrName());
    }

    /**
     * Does a 'round-trip' test of ConditionFormat on qgItem's condEleChild by
     * formatting it into a user string, comparing that string to expectedCondStr,
     * parsing the user string back into an Element, and comparing the the result
     * to the original input.
     *
     * @param qgItem
     * @param expectedCondStr
     * @throws ParseException
     */
    private void verifyQGItem(QGItem qgItem, String expectedCondStr)
            throws ParseException {
        ConditionFormat conditionFormat = new ConditionFormat();
        String condStr = conditionFormat.format(new CondEleWrapper(qgItem.condEleChild()));
        assertEquals(expectedCondStr, condStr);
        CondEleWrapper condEleWrapper = (CondEleWrapper) conditionFormat.parseObject(condStr);
        TestUtil.verifyElements(qgItem.condEleChild(), condEleWrapper.getCondEleChild());
    }

}
