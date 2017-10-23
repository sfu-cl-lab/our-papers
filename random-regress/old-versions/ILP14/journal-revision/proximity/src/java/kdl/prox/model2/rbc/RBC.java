/**
 * $Id: RBC.java 3722 2007-11-07 15:27:53Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rbc;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rbc.estimators.Estimator;
import kdl.prox.model2.rbc.estimators.MultinomialEstimator;
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
import java.util.HashMap;
import java.util.List;

public class RBC {

    private static Logger log = Logger.getLogger(RBC.class);

    // Can be changed by users to control how the prob distrs are created
    public Estimator estimatorModule = new MultinomialEstimator();

    // inputs
    protected AttributeSource classLabel;
    protected List<AttributeSource> sourceList;

    // learned variables
    protected DiscreteProbDistribution classDist;
    protected HashMap<AttributeSource, ConditionalDistribution> sourceToCondDistMap;
    protected boolean isLearned;

    public RBC() {
        isLearned = false;
    }

    /**
     * Computes the Conditional Probability Distributions from the data
     *
     * @param trainCont
     * @param label
     * @param sources
     * @return itself
     */
    public RBC learn(Container trainCont, AttributeSource label, PyList sources) {
        return learn(trainCont, label, Util.listFromPyList(sources));
    }

    public RBC learn(Container trainCont, AttributeSource label, List<AttributeSource> sources) {
        log.info("starting to induce model...");
        NSTCache cache = new NSTCache();

        //save variables in model, will be needed when applying it
        classLabel = label;
        sourceList = new ArrayList<AttributeSource>(sources);

        // checks
        Assert.notNull(trainCont, "training container is null");
        classLabel.init(trainCont, cache);
        Assert.condition(!classLabel.isContinuous(), "class label must be discrete");
        Assert.condition(classLabel.isSingleValue(), "cannot have more than one '" + classLabel.getItemName() + "' item per subgraph");

        // calculate overall class distribution
        classDist = (DiscreteProbDistribution) new DiscreteProbDistribution().addAttributeValuesFromNST(classLabel.getSourceTable());

        // compute a conditional probability distribution for each attribute
        sourceToCondDistMap = new HashMap<AttributeSource, ConditionalDistribution>();
        NST classNST = classLabel.getSourceTable();
        for (AttributeSource currSource : sources) {
            log.info("updating model with " + currSource + "...");

            currSource.init(trainCont, cache);
            NST attrNST = currSource.getSourceTable();
            NST classValueNST = attrNST.join(classNST, "subg_id = subg_id", "A.subg_id AS subg_id, A.value AS value, B.value AS class");

            // make new conditional estimator and save it in the <source -> estimator> map
            ConditionalDistribution conditionalDistribution = new ConditionalDistribution(estimatorModule);
            conditionalDistribution.recordClassAndValueDistribution(classValueNST, currSource.isContinuous());
            sourceToCondDistMap.put(currSource, conditionalDistribution);
            classValueNST.release();
        }

        log.info("induce model done...");
        isLearned = true;
        return this;
    }

    /**
     * Uses the learned Conditional Probability Distributions to estimate values for the class label
     * of unseen subgraps
     *
     * @param testCont
     * @return a predictions object, where all the probabilities are normalized
     */
    public Predictions apply(Container testCont) {
        Assert.condition(isLearned, "You must call learn before using apply");
        Assert.notNull(testCont, "test container is null");
        NSTCache cache = new NSTCache();

        DB.beginScope();
        log.info("starting to apply model...");

        // Init the default predictions for all subgraphs => the observed class distribution
        NST subgsToPredict = testCont.getDistinctSubgraphOIDs();
        Predictions predictions = new Predictions().setPredictions(subgsToPredict, classDist).normalizeForAll();

        // now that each subgraph has a default probability distribution for the label,
        // update those distributions based on the observed values of each attribute,
        // following the learned CPD.
        Object[] classValues = classDist.getDistinctValues();
        for (AttributeSource currSource : sourceList) {
            log.debug("updating predictions with " + currSource.getAttrName() + "...");
            ConditionalDistribution cDist = sourceToCondDistMap.get(currSource);
            if (cDist.isEmpty()) {
                continue;
            }

            // get the observed values (per subg) for the current source
            currSource.init(testCont, cache);
            NST sourceNST = currSource.getSourceTable();

            // update the probabilities for each possible value of the class label
            int randomSeed = (int) System.currentTimeMillis();
            for (Object label : classValues) {
                HashMap<String, Double> subgIDToProbsMap = cDist.getSmoothedProbsForLabelGivenAttribute(label + "", sourceNST, randomSeed);
                for (String subgID : subgIDToProbsMap.keySet()) {
                    predictions.getProbDistribution(subgID).adjustCount(label, subgIDToProbsMap.get(subgID));
                }
            }
            predictions.normalizeForAll();
        }

        DB.endScope();
        log.info("apply model done...");
        return predictions;
    }

    /**
     * Returns the distribution of the class labels, once learned
     * Note that users are free to change this distribution --see, for example, LGM models
     *
     * @return
     */
    public DiscreteProbDistribution getClassDistribution() {
        Assert.condition(isLearned, "You must call learn before using apply");
        return classDist;
    }


    /**
     * Returns the conditional distribution for a given source, once learned
     * Note that users are free to change this distribution --see, for example, LGM models
     *
     * @return
     */
    public ConditionalDistribution getConditionalDistributionForSource(AttributeSource s) {
        Assert.condition(isLearned, "You must call learn before using apply");
        return sourceToCondDistMap.get(s);
    }


    /**
     * Saves the RBC to a file in XML format
     *
     * @param fileName
     */
    public void save(String fileName) {
        Assert.condition(isLearned, "You must call learn before saving an RBC");
        Assert.stringNotEmpty(fileName, "invalid filename: empty");

        try {
            FileWriter fileW = new FileWriter(fileName);
            BufferedWriter buffW = new BufferedWriter(fileW);
            PrintWriter printW = new PrintWriter(buffW);

            printW.println("<!DOCTYPE rbc2 SYSTEM \"" + XMLUtil.RBC2_XML_SIGNATURE + "\">");
            printW.println("<!-- Created by Proximity -->\n");

            // save the estimator class
            Element estimatorEle = new Element("estimator-class");
            estimatorEle.addContent(estimatorModule.getClass().getName());

            // save the class label and its distribution
            Element classLabelEle = new Element("class-label");
            classLabelEle.addContent(classLabel.toXML());
            classLabelEle.addContent(classDist.toXML());

            // and the sources with their conditional distributions
            Element sourcesEle = new Element("sources");
            for (AttributeSource source : sourceList) {
                Element sourceEle = new Element("source-element");
                sourceEle.addContent(source.toXML());
                sourceEle.addContent(sourceToCondDistMap.get(source).toXML());
                sourcesEle.addContent(sourceEle);
            }

            // put them all together
            Element rptElem = new Element("rbc2");
            rptElem.addContent(estimatorEle);
            rptElem.addContent(classLabelEle);
            rptElem.addContent(sourcesEle);

            // and send it to the file
            new XMLOutputter(Format.getPrettyFormat()).output(rptElem, printW);

            printW.close();
            buffW.close();
            fileW.close();

        } catch (Exception exc) {
            log.error("Error saving the RBC to a file", exc);
        }
    }

    /**
     * Loads an RBC from a file in XML format
     *
     * @param fileName
     */
    public RBC load(String fileName) {
        Assert.stringNotEmpty(fileName, "invalid filename: empty");

        Element rootElem;
        try {
            SAXBuilder saxBuilder = new SAXBuilder(true);    // validating
            Document document = saxBuilder.build(new File(fileName));
            rootElem = document.getRootElement();
        } catch (Exception exc) {
            throw new MonetException("Unable to open the file for reading RBC: " + exc);
        }

        // read the estimator
        estimatorModule = XMLUtil.readEstimatorFromXML(rootElem);

        // read the class label and its distribution
        Source classLabelFromFile = XMLUtil.readSourceFromXML(rootElem.getChild("class-label"));
        ProbDistribution classDistFromFile = XMLUtil.readProbDistributionFromXML(rootElem.getChild("class-label"));
        Assert.condition(classLabelFromFile instanceof AttributeSource, "class label is not of type AttributeSource");
        Assert.condition(classDistFromFile instanceof DiscreteProbDistribution, "class label probability distribution is not of type DiscreteProbabilityDistribution");
        classLabel = (AttributeSource) classLabelFromFile;
        classDist = (DiscreteProbDistribution) classDistFromFile;

        // read in the sources, with their conditional distributions        
        List<Element> sourceElements = rootElem.getChild("sources").getChildren("source-element");
        sourceList = new ArrayList<AttributeSource>();
        sourceToCondDistMap = new HashMap<AttributeSource, ConditionalDistribution>();
        for (Element sourceElement : sourceElements) {
            Source source = XMLUtil.readSourceFromXML(sourceElement);
            Assert.condition(source instanceof AttributeSource, "source is not of type AttributeSource: " + source);
            sourceList.add((AttributeSource) source);

            ConditionalDistribution condDist = new ConditionalDistribution(estimatorModule, sourceElement.getChild("conditional-distr"));
            sourceToCondDistMap.put((AttributeSource) source, condDist);
        }

        // loading an RBC from an XML is equivalent to learning it
        isLearned = true;

        return this;

    }
}
