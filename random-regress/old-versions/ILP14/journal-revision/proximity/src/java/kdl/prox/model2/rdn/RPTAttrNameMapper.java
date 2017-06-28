/**
 * $Id: RPTAttrNameMapper.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTWalker;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This utility class maps the RPT's class labels to temporary attributes,
 * and re-maps the learned feature settings in each RPT's to use those temporary attributes.
 * <p/>
 * When done, cleanup() restores the original feature settings and removes the created attributes.
 */
public class RPTAttrNameMapper {

    protected static Logger log = Logger.getLogger(RPTAttrNameMapper.class);

    public static final String ATTR_PREFIX = "rdn_temp_";

    private HashMap<RPT, Container> modelsConts;
    Map<String, String> classToTempAttrMap = new HashMap<String, String>(); // old -> new
    Map<String, String> tempAttrToClassMap = new HashMap<String, String>(); // new -> old


    public void startup(Map<RPT, Container> modelsConts) {
        this.modelsConts = new HashMap<RPT, Container>(modelsConts);
        findRPTLabels();
        createAttributes();
        remapRPTSources(classToTempAttrMap);
    }


    public void cleanup() {
        remapRPTSources(tempAttrToClassMap);
        deleteAttributes();
    }


    /**
     * Creates the temporary attributes
     */
    private void createAttributes() {
        for (RPT rpt : modelsConts.keySet()) {
            String origAttrName = rpt.getClassLabel().getAttrName();
            String newAttrName = classToTempAttrMap.get(origAttrName);
            DB.getObjectAttrs().copyAttribute(origAttrName, newAttrName);
        }
    }

    /**
     * Deletes the temporary attributes
     */
    private void deleteAttributes() {
        for (RPT rpt : modelsConts.keySet()) {
            String origAttrName = rpt.getClassLabel().getAttrName();
            String newAttrName = classToTempAttrMap.get(origAttrName);
            DB.getObjectAttrs().deleteAttribute(newAttrName);
        }
    }


    /**
     * Finds the class label each RPT and renames itto ATTR_PREFIX + label,
     * saving the correspondance in the two maps
     */
    private void findRPTLabels() {
        for (RPT rpt : modelsConts.keySet()) {
            AttributeSource label = rpt.getClassLabel();
            Assert.condition(!label.isSubgraphAttr(), "The RPT's class label cannot be a subgraph attribute!");
            String labelName = label.getAttrName();
            classToTempAttrMap.put(labelName, ATTR_PREFIX + labelName);
            tempAttrToClassMap.put(ATTR_PREFIX + labelName, labelName);
        }
    }

    /**
     * Finds attributes in the RPTs and renames them according to the oldToNewMap (including the class labels)
     * when called from startup(), the attributes are mapped to the new temporal attribute names
     * When called from cleanup(), they are mapped back to their original values
     *
     * @param oldToNewMap
     */
    private void remapRPTSources(final Map<String, String> oldToNewMap) {
        for (RPT rpt : modelsConts.keySet()) {

            // remap the class label
            AttributeSource attributeSource = rpt.getClassLabel();
            String newLabel = oldToNewMap.get(attributeSource.getAttrName());
            if (newLabel != null) { // could be null if it's already been remapped before, for another RPT!
                log.debug("Mapping RPT label " + attributeSource + " to " + newLabel);
                attributeSource.setAttrName(newLabel);
            }

            // remap all the attributes in the splits
            new RPTWalker() {
                public void processNode(RPTNode node, int depth) {
                    if (!node.isLeaf()) {
                        final Source source = node.getSplit().getSource();
                        if (source instanceof AttributeSource) {
                            AttributeSource s = (AttributeSource) source;
                            final String labelAttr = s.getAttrName();
                            if (oldToNewMap.get(labelAttr) != null) {
                                String newAttrName = oldToNewMap.get(labelAttr);
                                log.debug("Mapping RPT attribute " + s + " to " + newAttrName);
                                s.setAttrName(newAttrName);
                            }
                        }
                    }
                }
            }.walk(rpt.getRootNode());
        }
    }

}
