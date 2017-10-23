/**
 * $Id: RPT.java 3732 2007-11-07 20:56:23Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.aggregatorselection.RPTAggregatorSelectionModule;
import kdl.prox.model2.rpt.modules.learning.DefaultLearningModule;
import kdl.prox.model2.rpt.modules.learning.RPTLearningModule;
import kdl.prox.model2.rpt.modules.pruning.DefaultPruningModule;
import kdl.prox.model2.rpt.modules.pruning.RPTPruningModule;
import kdl.prox.model2.rpt.util.BranchingUtil;
import kdl.prox.model2.rpt.util.FSGenerationUtil;
import kdl.prox.model2.rpt.util.RPTUtil;
import kdl.prox.model2.util.NSTCache;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.monet.MonetException;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.python.core.PyList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * RPT implements a Relational Probability Tree as described in:
 * <p/>
 * J.ÊNeville, D.ÊJensen, L.ÊFriedland, and M.ÊHay. Learning relational probability trees.
 * In Proceedings of the 9th ACM SIGKDD International Conference on Knowledge Discovery
 * and Data Mining., 2003.
 * <p/>
 * RPTs are recursively learned probability estimation trees for relational data. RPT provides access
 * to two extendable modules: learning and pruning. The default learning module includes extendable modules
 * for learning RPTs from relational data including modules for scoring, splitting, stopping,
 * aggregation, and feature significance.  See rpt.modules for more information.
 * <p/>
 * Pruning is an optional operation.  Modules for cost-complexity pruning are included.
 */

public class RPT {

    private static Logger log = Logger.getLogger(RPT.class);

    RPTNode root = null;

    AttributeSource classLabel = null;
    Source[] sourceList = null;

    boolean isLearned = false;

    public RPTLearningModule learningModule;
    public RPTPruningModule pruningModule;

    /**
     * Creates an RPT with the default modules.
     * These modules are public, so you can set them directly
     */
    public RPT() {
        learningModule = new DefaultLearningModule();
        pruningModule = new DefaultPruningModule();
    }

    /**
     * Apply the learned RPT to a container. The RPT must have been learn'ed before.
     * Returns the predictions that the RPT makes.
     * Call predictions.setTrueLabels(container, attrSource) before asking for accuracy metrics.
     * <p/>
     * The extra nstCache parameter can be passed in to avoid recomputing feature settings
     * in successive calls to apply on the same RPT (eg, from the RDN, or while pruning).
     *
     * @param testContainer
     * @return predictions of labels for the subgraphs in the container.
     */
    public Predictions apply(Container testContainer) {
        DB.beginScope();
        Predictions predictions = apply(testContainer, new NSTCache());
        DB.endScope();
        return predictions;
    }

    public Predictions apply(Container testContainer, NSTCache nstCache) {
        Assert.condition(isLearned, "You must call learn before using apply");

        // prepare the cache, the state, and generate tables for the learn'ed feature settings
        RPTState rptState = new RPTState();
        rptState.nstCache = nstCache;
        FSGenerationUtil.createFSTables(testContainer, getLearnedSplits(), rptState);
        rptState.subgIDs = testContainer.getDistinctSubgraphOIDs().addConstantColumn("weight", "dbl", "1");

        return applyRecursive(root, rptState);
    }

    /**
     * Recursively apply the node and its children
     *
     * @param node
     * @param currentState
     * @return predictions for the subgraphs that match the node
     */
    private Predictions applyRecursive(RPTNode node, RPTState currentState) {
        Predictions nodePredictions = new Predictions();
        if (node == null) {
            return nodePredictions;
        }

        if (node.isLeaf()) {
            nodePredictions.setPredictions(currentState.subgIDs, node.classLabelDistribution);
        } else {
            DB.beginScope();
            NST[] branchSubgIDs = BranchingUtil.getBranchSubgIDs(node.split, currentState);
            RPTState yesState = new RPTState(currentState, 0, branchSubgIDs[0]);
            nodePredictions.setPredictions(applyRecursive(node.yesBranch, yesState));
            RPTState noState = new RPTState(currentState, 0, branchSubgIDs[1]);
            nodePredictions.setPredictions(applyRecursive(node.noBranch, noState));
            DB.endScope();
        }

        return nodePredictions;
    }


    public AttributeSource getClassLabel() {
        Assert.condition(isLearned, "You must call learn before using apply");
        return classLabel;
    }

    public int getDepth() {
        Assert.condition(isLearned, "You must call learn before using expandNode");
        return root.getDepth();
    }

    /**
     * Finds the featureSettings along the branches of the tree
     */
    public List<FeatureSetting> getLearnedSplits() {
        Assert.condition(isLearned, "You must call learn before trying to get the learned splits");
        final ArrayList<FeatureSetting> splitList = new ArrayList<FeatureSetting>();
        new RPTWalker() {
            public void processNode(RPTNode node, int depth) {
                if (!node.isLeaf()) {
                    splitList.add(node.split);
                }
            }
        }.walk(root);
        return splitList;
    }

    public int getLeafCount() {
        Assert.condition(isLearned, "You must call learn before trying to get the leaf count");
        return root.getLeafCount();
    }

    public RPTNode getRootNode() {
        Assert.condition(isLearned, "You must call learn before trying to get the learned node");
        return root;
    }


    /**
     * Learn the model of the classLabel from a train container and a set of sources.
     *
     * @param trainContainer
     * @param label
     * @param sources
     */
    public RPT learn(Container trainContainer, AttributeSource label, List<Source> sources) {
        Source[] sourceArray = new Source[sources.size()];
        int i = 0;
        for (Source s : sources) {
            sourceArray[i++] = s;
        }
        return learn(trainContainer, label, sourceArray);
    }

    public RPT learn(Container trainContainer, AttributeSource label, PyList sources) {
        return learn(trainContainer, label, Util.listFromPyList(sources));
    }

    public RPT learn(Container trainContainer, AttributeSource label, Source[] sources) {
        DB.beginScope();

        classLabel = label;
        sourceList = sources;

        // initialize the state, and compute the FS tables
        RPTState rptState = new RPTState();
        rptState.inputSources = sources;
        rptState.trainContainer = trainContainer;
        rptState.classLabel = classLabel;

        log.info("Creating feature tables");
        RPTAggregatorSelectionModule aggModule = learningModule.getAggregatorSelectionModule();
        rptState.featureSettingList = FSGenerationUtil.createFSTables(trainContainer, sourceList, rptState,
                aggModule.getValidAggregatorNamesList(), aggModule.getInvalidAggregatorNamesForCore());
        log.info("Done creating feature tables: " + rptState.featureSettingList.size() + " features.");

        // initialize the class label
        classLabel.init(trainContainer, rptState.nstCache);
        rptState.subgIDs = classLabel.getDistinctSubgraphs().removeColumn("count").addConstantColumn("weight", "dbl", "1.0");

        root = learningModule.learn(rptState);
        isLearned = true;

        root = pruningModule.prune(root, rptState);

        rptState.nstCache.clear();
        DB.endScope();

        return this;
    }

    /**
     * Learns a tree from a pre-existing root node.
     * Designed To be used only for applying a tree to a new container.
     * (See modules.pruning.TestSampleSelection for details)
     *
     * @param newRoot
     * @param newClassLabel
     */
    public RPT learnFromRPTNode(RPTNode newRoot, AttributeSource newClassLabel) {
        root = newRoot;
        classLabel = newClassLabel;
        isLearned = true;
        return this;
    }

    /**
     * Loads a tree from a file in XML format
     */
    public RPT load(String fileName) {
        Assert.stringNotEmpty(fileName, "invalid filename: empty");

        Element rptElem;
        try {
            SAXBuilder saxBuilder = new SAXBuilder(true);    // validating
            Document document = saxBuilder.build(new File(fileName));
            rptElem = document.getRootElement();
        } catch (Exception exc) {
            throw new MonetException("Unable to open the file for reading RPT" + exc);
        }

        // Read/save the class label, and the root node
        Source classLabelSource = XMLUtil.readSourceFromXML(rptElem.getChild("class-label"));
        Assert.condition(classLabelSource instanceof AttributeSource, "class label is not of type AttributeSource");
        classLabel = (AttributeSource) classLabelSource;
        root = new RPTNode(rptElem.getChild("rpt-body"));

        // loading an RPT from an XML is equivalent to learning it
        isLearned = true;

        return this;
    }

    /**
     * Prints the tree to System.out
     */
    public void print() {
        new RPTWalker() {
            public void processNode(RPTNode node, int depth) {
                System.out.println(RPTUtil.getDepthIndentation(depth) + node.toString());
            }
        }.walk(root);
    }

    public void printFull() {
        System.out.println("Predict: " + classLabel);
        new RPTWalker() {
            public void processNode(RPTNode node, int depth) {
                System.out.println(RPTUtil.getDepthIndentation(depth) + node.toString() + " " + node.getClassLabelDistribution());
            }
        }.walk(root);
    }

    /**
     * Saves the tree to a file in XML format
     *
     * @param fileName
     */
    public void save(String fileName) {
        Assert.condition(isLearned, "You must call learn before saving a tree");
        Assert.stringNotEmpty(fileName, "invalid filename: empty");

        try {
            FileWriter fileW = new FileWriter(fileName);
            BufferedWriter buffW = new BufferedWriter(fileW);
            PrintWriter printW = new PrintWriter(buffW);

            printW.println("<!DOCTYPE rpt2 SYSTEM \"" + XMLUtil.RPT2_XML_SIGNATURE + "\">");
            printW.println("<!-- Created by Proximity -->\n");

            // save the class label...
            Element classLabelEle = new Element("class-label");
            classLabelEle.addContent(classLabel.toXML());
            // and the root node
            final Element rptBodyElem = new Element("rpt-body");
            rptBodyElem.addContent(root.toXML());

            Element rptElem = new Element("rpt2");
            rptElem.addContent(classLabelEle);
            rptElem.addContent(rptBodyElem);

            // and send it to the file
            new XMLOutputter(Format.getPrettyFormat()).output(rptElem, printW);

            printW.close();
            buffW.close();
            fileW.close();

        } catch (Exception exc) {
            log.error("Error saving the RPT to a file", exc);
        }
    }

}
