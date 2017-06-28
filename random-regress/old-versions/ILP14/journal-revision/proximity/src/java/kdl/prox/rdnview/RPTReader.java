/**
 * $Id: RPTReader.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.rdnview;

import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.ItemSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.util.XMLUtil;
import org.jdom.Element;

import java.util.List;

/**
 * Reads information from the RPTs for the RDN.
 * Encapsulates the code that deals with old and new versions of the RPT
 */
public class RPTReader {

    private RPTReader() {
    }

    /**
     * Returns item, attribute, isObject for a source (EstItemAttr, in the original RPT)
     *
     * @param docType
     * @param rptRootElement
     * @return
     */
    public static RPTSourceInfo getClassLabelSourceInfo(String docType, Element rptRootElement) {
        if (docType.equals(XMLUtil.RPT2_XML_SIGNATURE)) {
            return getRPTSourceInfoForRPT2(XMLUtil.getClassLabelElementFromRootElement(rptRootElement));
//        } else if (docType.equals("rpt.dtd")) {
//            return getRPTSourceInfoForRPT(rptRootElement.getChild("rpt-info").getChild("class-label"));
        } else {
            throw new IllegalArgumentException("Unknown RPT format: " + docType);
        }
    }

    private static RPTSourceInfo getRPTSourceInfoForRPT(Element rptElement) {
        String classType = rptElement.getChildText("item");
        String classVar = rptElement.getChildText("attribute");
        boolean isObject = (Boolean.valueOf(rptElement.getChildText("is-object"))).booleanValue();
        return new RPTSourceInfo(classType, classVar, isObject);
    }

    /**
     * todo sets the is-object flag to true always, because the new RPT file format doesn't
     * record whether the item is an object or a link. Doesn't affect too much, just the rounded corners of the boxes
     *
     * @param rptElement
     * @return
     */
    private static RPTSourceInfo getRPTSourceInfoForRPT2(Element rptElement) {
        Source source = XMLUtil.readSourceFromXML(rptElement);
        if (source instanceof AttributeSource) {
            AttributeSource attrSource = (AttributeSource) source;
            return new RPTSourceInfo(attrSource.getItemName(), attrSource.getAttrName(), true);
        } else if (source instanceof ItemSource) {
            ItemSource itemSource = (ItemSource) source;
            return new RPTSourceInfo(itemSource.getItemName(), null, true);
        } else { // unknown type?
            return new RPTSourceInfo(null, null, true);
        }
    }


    /**
     * Gets the information on the item.attr on a split
     *
     * @param docType
     * @param rptElement
     * @return
     */
    public static RPTSourceInfo getSplitSourceInfo(String docType, Element rptElement) {
        if (docType.equals(XMLUtil.RPT2_XML_SIGNATURE)) {
            return getRPTSourceInfoForRPT2(XMLUtil.getFeatureSettingElementFromNodeElement(rptElement));
//        } else if (docType.equals("rpt.dtd")) {
//            Element featureEle = rptElement.getChild("feature");
//            RPTSourceInfo info = getRPTSourceInfoForRPT(featureEle);
//            String aggregatorSig = featureEle.getChildText("aggregator");
//            String aggrPackage = FeatureManager.getClassNameFromSignature(aggregatorSig);
//            if ("kdl.prox.old.model.features.DegreeFeature".equals(aggrPackage)) {
//                info.attr = null;
//            }
//            return info;
        } else {
            throw new IllegalArgumentException("Unknown RPT format: " + docType);
        }
    }


    /**
     * Returns a list of all the internal, non-leaf nodes in the RPT
     *
     * @param docType
     * @param rootElement
     * @return
     */
    public static List<Element> getRPTNodeList(String docType, Element rootElement) {
        if (docType.equals(XMLUtil.RPT2_XML_SIGNATURE)) {
            return XMLUtil.getSplitNodeElementsFromXML(XMLUtil.getBodyElementFromRootElement(rootElement));
//        } else if (docType.equals("rpt.dtd")) {
//            Element rptBodyEle = rootElement.getChild("rpt-body");
//            return rptBodyEle.getChildren("node");
        } else {
            throw new IllegalArgumentException("Unknown RPT format: " + docType);
        }
    }

}
