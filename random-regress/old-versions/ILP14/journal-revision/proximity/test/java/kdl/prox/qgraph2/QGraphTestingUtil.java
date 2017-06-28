/**
 * $Id: QGraphTestingUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QGraphTestingUtil.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2;

import kdl.prox.util.Assert;
import org.jdom.Element;


/**
 * Contains static util methods useful for testing transformations.
 */
public class QGraphTestingUtil {

    // no IVs
    
    
    /**
     * Returns a <condition> Element that combines two <test> ElementS using a
     * containing boolean Element.
     *
     * @param testEle1
     * @param testEle2     can be null
     * @param logicEleName
     * @return
     */
    public static Element combineTestElements(Element testEle1, Element testEle2, String logicEleName) {
        Element combinedEle = new Element(logicEleName);
        combinedEle.addContent(testEle1);
        if (testEle2 != null) {
            combinedEle.addContent(testEle2);
        }
        Element condEle = new Element("condition");
        condEle.addContent(combinedEle);
        return condEle;
    }


    /**
     * Returns a new <numeric-annotation> Element containing the min and
     * (optional) max values.
     *
     * @param min 0, 1, ...
     * @param max 0, 1, ... ; -1 if not to be included
     * @return
     */
    public static Element makeAnnotationElement(int min, int max) {
        Element annotEle = new Element("numeric-annotation");
        Element newAnnotMin = new Element("min");
        newAnnotMin.addContent(min + "");
        annotEle.addContent(newAnnotMin);
        if (max != -1) {
            Element newAnnotMax = new Element("max");
            newAnnotMax.addContent(max + "");
            annotEle.addContent(newAnnotMax);
        }
        return annotEle;
    }


    /**
     * Creates a <condition> Element, following query-graph.dtd. Takes the name
     * of an attribute and its expected value, and makes a condition that
     * compares attrName eq value.
     *
     * @param attrName
     * @param value    (can be null -- for exists operator)
     */
    public static Element makeConditionElement(String attrName, String operator, String value) {
        Element testEle = makeTestElement(attrName, operator, value);
        Element condEle = new Element("condition");
        condEle.addContent(testEle);
        return condEle;
    }


    public static Element makeConstraintTestElement(String operator,
                                                    String item1Name, String item1AttrName,
                                                    String item2Name, String item2AttrName) {
        Assert.stringNotEmpty(operator, "empty operator");
        Assert.stringNotEmpty(item1Name, "empty item1Name");
        Assert.stringNotEmpty(item2Name, "empty item2Name");
        // create test element and add the operator
        Element testEle = new Element("test");
        Element operatorEle = new Element("operator");
        operatorEle.addContent(operator);
        testEle.addContent(operatorEle);
        // create the first element and add it to the test element
        Element item1Ele = new Element("item");
        Element attr1NameEle = new Element("item-name");
        attr1NameEle.addContent(item1Name);
        item1Ele.addContent(attr1NameEle);
        if (item1AttrName == null) {
            item1Ele.addContent(new Element("id"));
        } else {
            Element item1AttrNameEle = new Element("attribute-name");
            item1AttrNameEle.addContent(item1AttrName);
            item1Ele.addContent(item1AttrNameEle);
        }
        testEle.addContent(item1Ele);
        // create the second element and add it to the test element
        Element item2Ele = new Element("item");
        Element attr2NameEle = new Element("item-name");
        attr2NameEle.addContent(item2Name);
        item2Ele.addContent(attr2NameEle);
        if (item2AttrName == null) {
            item2Ele.addContent(new Element("id"));
        } else {
            Element item2AttrNameEle = new Element("attribute-name");
            item2AttrNameEle.addContent(item2AttrName);
            item2Ele.addContent(item2AttrNameEle);
        }
        testEle.addContent(item2Ele);
        return testEle;
    }


    public static Element makeTestElement(String attrName, String operator, String value) {
        Element testEle = new Element("test");
        Element operatorEle = new Element("operator");
        operatorEle.addContent(operator);
        Element attrNameEle = new Element("attribute-name");
        attrNameEle.addContent(attrName);
        testEle.addContent(operatorEle);
        testEle.addContent(attrNameEle);
        if (value != null) {
            Element valueEle = new Element("value");
            valueEle.addContent(value);
            testEle.addContent(valueEle);
        }
        return testEle;
    }


}
