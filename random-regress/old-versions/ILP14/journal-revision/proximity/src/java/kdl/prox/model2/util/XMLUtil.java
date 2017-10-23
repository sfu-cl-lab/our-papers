/**
 * $Id: XMLUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.util;

import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rbc.estimators.Estimator;
import kdl.prox.model2.rpt.aggregators.Aggregator;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.util.Assert;
import org.jdom.Element;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of methods for making XML writing easier.
 * <p/>
 * This class is used to save an RPT to XML, and to read it back in. An RPT's XML representation is made up
 * of a class-label element, and the rpt-body, which contains instances of the <node> element, for the RPTNodes in the tree
 * <p/>
 * The classes that can save to XML (via toXML()) are RPTNode, FeatureSetting, Source, Aggregator,
 * and DiscreteProbabilityDistribution. In general, those classes have constructors that receive an XML element of the
 * same format that they dump, and recreate the instance.
 * <p/>
 * When the classes have subclasses (e.g., Source, FeatureSetting, Aggregator), the corresponding element contains a 'type' entry
 * (e.g., fs-class, source-class) indicatng the full name of the class that the XML element represents. When loading
 * the XML back, we use the readXXXFromXML() methods below to parse the type field, and create an instance of the
 * correct class. In addition, the derived classes usually overwrite the XML-based constructor, opening up the XML
 * element and finding extra attributes that they need in the 'elements' entry (e.g., fs-elements, source-elements).
 * For example, for an ItemSource, the XML saved has a source-class equal to kdl.prox.model2.common.sources.ItemSource, which
 * is parsed by readSourceFromXML below to create an instance of the ItemSource class. The constructor calls the
 * standard Source constructor, but then it also opens up the source-elements item and reads in the item-name attribute.
 * In the same way, ItemSource.toXML() adds to the standard Source.toXML() the item-name attribute.
 */
public abstract class XMLUtil {

    public static final String RPT2_XML_SIGNATURE = "rpt2.dtd";
    public static final String RBC2_XML_SIGNATURE = "rbc2.dtd";

    public static Element createElementWithValue(String eleName, String value) {
        Element theEle = new Element(eleName);
        theEle.addContent(value);
        return theEle;
    }

    public static Element createElementWithValue(String eleName, Element value) {
        Element theEle = new Element(eleName);
        theEle.addContent(value);
        return theEle;
    }


    /**
     * Returns the rpt-body element, from the root
     *
     * @param rootElement
     * @return
     */
    public static Element getBodyElementFromRootElement(Element rootElement) {
        return rootElement.getChild("rpt-body");
    }

    /**
     * Returns the class-label element, from the root
     *
     * @param rootElement
     * @return
     */
    public static Element getClassLabelElementFromRootElement(Element rootElement) {
        return rootElement.getChild("class-label");
    }

    /**
     * Returns the feature-setting element for a node
     *
     * @param nodeElement
     * @return
     */
    public static Element getFeatureSettingElementFromNodeElement(Element nodeElement) {
        Element split = nodeElement.getChild("split");
        Assert.notNull(split, "nodeElement doesn't have split!");
        return split.getChild("feature-setting");
    }

    /**
     * Returns a list of all the <node> elements that contain splits, starting from the root and recursing through the
     * left and right branch. Leaves don't have splits, and are therefore not considered.
     *
     * @param rootEle
     * @return a list of Element objects for the <node>
     */
    public static List<Element> getSplitNodeElementsFromXML(Element rootEle) {
        List<Element> nodeList = new ArrayList<Element>();
        if (rootEle != null) {
            rootEle = rootEle.getChild("node");
            if (rootEle.getChild("split") != null) {
                nodeList.add(rootEle);
                nodeList.addAll(getSplitNodeElementsFromXML(rootEle.getChild("yes-branch")));
                nodeList.addAll(getSplitNodeElementsFromXML(rootEle.getChild("no-branch")));
            }
        }
        return nodeList;
    }


    /**
     * General method: creates an instance of the specified class Type
     *
     * @param classType
     * @param ele
     * @return an object of the desired classType, created via the constructor that receives an XML elememt
     */
    public static Object instantiateObjectFromXML(String classType, Object ele) {
        try {
            Class theClass = Class.forName(classType);
            Constructor constructor = theClass.getConstructor(new Class[]{Element.class});
            return constructor.newInstance(new Object[]{ele});
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Creates an Aggregator of the type specified in the contEle, with the given source as parameter
     *
     * @param contEle
     * @param s
     * @return an aggregator on the given source
     */
    public static Aggregator readAggregatorFromXML(Element contEle, Source s) {
        String aggType = contEle.getChild("aggregator").getChildText("aggregator-class");
        try {
            Class theClass = Class.forName(aggType);
            Constructor constructor = theClass.getConstructor(new Class[]{Source.class});
            return (Aggregator) constructor.newInstance(new Object[]{s});
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Creates an estimator from an XML element that has a child named estimator-class
     *
     * @param estEle
     * @return an estimator of class estimator-class
     */
    public static Estimator readEstimatorFromXML(Element estEle) {
        String fsType = estEle.getChildText("estimator-class");
        try {
            Constructor constructor = ((Class) Class.forName(fsType)).getConstructor();
            return (Estimator) constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Creates a feature setting from an XML element of type <feature-setting>
     * Reads the fs-class attribute of the node an creates an instance of the appropriate FeatureSetting class
     *
     * @param contEle
     * @return a FeatureSetting of class <fs-class>
     */
    public static FeatureSetting readFeatureSettingFromXML(Element contEle) {
        String fsType = contEle.getChild("feature-setting").getChildText("fs-class");
        return (FeatureSetting) instantiateObjectFromXML(fsType, contEle);
    }


    /**
     * Creates a probability distribution from an XML element of type <class-label-distr>
     * Reads the prob-distr attribute of the node an creates an instance of the appropriate ProbDistr class
     *
     * @param contEle
     * @return a FeatureSetting of class <fs-class>
     */
    public static ProbDistribution readProbDistributionFromXML(Element contEle) {
        String probClass = contEle.getChild("class-label-distr").getChildText("distr-class");
        return (ProbDistribution) instantiateObjectFromXML(probClass, contEle);
    }

    /**
     * Creates a source from an XML element of type <source>
     * Reads the source-class attribute of the node an creates an instance of the appropriate Source class
     *
     * @param contEle
     * @return a Source of class <source-class>
     */
    public static Source readSourceFromXML(Element contEle) {
        String sourceType = contEle.getChild("source").getChildText("source-class");
        return (Source) instantiateObjectFromXML(sourceType, contEle);
    }


}