/**
 * $Id: RPTNodeView.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import kdl.prox.model2.common.probdistributions.ProbDistribution;

/**
 * A simple interface used to make the RPT Viewer capable of displaying the two types of RPTs: old and new (rpt and rpt2)
 * These are the methods that an RP(T)Node must provide in order to be displayed in the RPT Viewer
 */
public interface RPTNodeView {

    public ProbDistribution getClassLabelDistributionView();

    public RPTNodeView getNoBranchView();

    public RPTNodeView getYesBranchView();

    public double getYesProportion();

    public boolean isLeaf();

    public String toString();

}
