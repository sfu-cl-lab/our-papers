/**
 * $Id: RPTNode.java 3673 2007-10-22 17:07:45Z afast $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt;

import kdl.prox.gui2.RPTNodeView;
import kdl.prox.model2.common.probdistributions.ProbDistribution;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.util.XMLUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Maintains the tree structure at each branch in the tree.
 */

public class RPTNode implements RPTNodeView {

    private static Logger log = Logger.getLogger(RPTNode.class);

    public FeatureSetting split = null;
    public ProbDistribution classLabelDistribution = null;
    protected RPTNode yesBranch = null;
    protected RPTNode noBranch = null;

    /**
     * Default constructor
     */
    public RPTNode() {
    }

    /**
     * Creates an RPT node in the tree with a given distribution of class labels
     *
     * @param distribution
     */
    public RPTNode(ProbDistribution distribution) {
        Assert.notNull(distribution, "Null distribution");
        classLabelDistribution = distribution;
    }

    /**
     * Reads in an RPTNode from the contents of an XML <node> element, containing a class label distribution
     * and maybe a split and the yes/no branches
     *
     * @param xmlEle
     */
    public RPTNode(Element xmlEle) {
        Element nodeEle = xmlEle.getChild("node");

        classLabelDistribution = XMLUtil.readProbDistributionFromXML(nodeEle);
        Element splitEle = nodeEle.getChild("split");
        if (splitEle != null) {
            setSplit(XMLUtil.readFeatureSettingFromXML(splitEle));
            setYesBranch(new RPTNode(nodeEle.getChild("yes-branch")));
            setNoBranch(new RPTNode(nodeEle.getChild("no-branch")));
        }
    }

    /**
     * Recursive copy methods for trees. Doesn't copy Probability Distributions or Splits
     * CAUTION: These could be shared by multiple trees.  Since this method is probably
     * going to be used primarily by the pruning methods, I suspect this is OK. -af
     *
     * @return
     */
    public RPTNode copy() {
        RPTNode newNode = new RPTNode(this.getClassLabelDistribution());
        newNode.split = this.split;

        if (!isLeaf()) {
            newNode.yesBranch = yesBranch.copy();
            newNode.noBranch = noBranch.copy();
        }

        return newNode;
    }

    public ProbDistribution getClassLabelDistribution() {
        return classLabelDistribution;
    }

    public ProbDistribution getClassLabelDistributionView() {
        return getClassLabelDistribution();
    }

    /**
     * Non-reversible method changes the node from a split node to a leaf node.
     * This removes all branches below this node.
     * (To be used by pruning)
     */
    public void collapse() {
        split = null;
        yesBranch = null;
        noBranch = null;
    }

    public int getDepth() {
        if (isLeaf()) {
            return 0;
        }
        return 1 + Math.max(yesBranch.getDepth(), noBranch.getDepth());
    }

    public double getInstanceCount() {
        return classLabelDistribution.getTotalNumValues();
    }

    public int getLeafCount() {
        if (isLeaf()) {
            return 1;
        }
        return yesBranch.getLeafCount() + noBranch.getLeafCount();
    }

    /**
     * Estimate of misclassification rate (resubstitution error) for this tree computed
     * from the training sample. The estimate ranges between 0 and (1 - defaultClassifcationRate)
     * e.g., for a 50/50 class split this is between 0 and 0.5. Generally, this is the overall
     * probability of incorrectly classifying an instance.
     * <p/>
     * <p/>
     * See Breiman, Friedman, Olshen, and Stone Definition 2.11 (pg 34-35) for more details.
     *
     * @param totalInstanceCount - used to compute the probability of an instance landing in this node.
     * @return
     */
    public double getTreeResubError(double totalInstanceCount) {
        if (isLeaf()) {
            return getNodeResubError(totalInstanceCount);
        }

        return yesBranch.getTreeResubError(totalInstanceCount) +
                noBranch.getTreeResubError(totalInstanceCount);
    }

    public double getNodeResubError(double totalInstanceCount) {
        double maxCount = -1;
        Object[] labels = classLabelDistribution.getDistinctValues();
        for (int i = 0; i < labels.length; i++) {
            Object label = labels[i];
            double currCount = classLabelDistribution.getCount(label);
            if (Double.compare(currCount, maxCount) > 0) {
                maxCount = currCount;
            }
        }
        return (1 - (maxCount * 1.0 / getInstanceCount())) *
                (getInstanceCount() / totalInstanceCount);
    }

    public FeatureSetting getSplit() {
        return split;
    }

    public RPTNode getNoBranch() {
        return noBranch;
    }

    public RPTNode getYesBranch() {
        return yesBranch;
    }

    public RPTNodeView getNoBranchView() {
        return getNoBranch();
    }

    public RPTNodeView getYesBranchView() {
        return getYesBranch();
    }

    public double getYesProportion() {
        return (isLeaf() ?
                1.0 :
                yesBranch.classLabelDistribution.getTotalNumValues() / classLabelDistribution.getTotalNumValues());
    }

    public boolean isLeaf() {
        return (split == null);
    }

    public void replaceWith(RPTNode newNode) {
        setSplit(newNode.split);
        setYesBranch(newNode.yesBranch);
        setNoBranch(newNode.noBranch);
        classLabelDistribution = newNode.classLabelDistribution;
    }

    public void setNoBranch(RPTNode n) {
        noBranch = n;
    }

    public void setYesBranch(RPTNode n) {
        yesBranch = n;
    }

    public void setSplit(FeatureSetting fs) {
        split = fs;
    }


    public String toString() {
        String ret = "";
        if (!isLeaf()) {
            ret += split.toString();
        } else {
            ret += "leaf with " + classLabelDistribution.getTotalNumValues() + " subgs";
        }
        return ret;
    }

    /**
     * Creates an XML representation of the RPT node
     * NB: Calls itself recursively on yes/no branches
     *
     * @return an XML element of type <node> with the class distribution, split, and yes/no branches
     */
    public Element toXML() {
        Element nodeEle = new Element("node");

        nodeEle.addContent(classLabelDistribution.toXML());
        if (!isLeaf()) {
            nodeEle.addContent(XMLUtil.createElementWithValue("split", split.toXML()));
            nodeEle.addContent(XMLUtil.createElementWithValue("yes-branch", yesBranch.toXML()));
            nodeEle.addContent(XMLUtil.createElementWithValue("no-branch", noBranch.toXML()));
        }

        return nodeEle;
    }

}
