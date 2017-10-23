/**
 * $Id: GraphGenerator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2.rptviewer2;

import edu.uci.ics.jung.graph.impl.SparseTree;
import kdl.prox.gui2.RPTNodeView;

/**
 * Build a JUNG graph from an RPT
 * Takes the root RPT node and traverses it, creating JUNG nodes and edges
 */
public class GraphGenerator {

    static double totalWeight;

    public static SparseTree getGraph(RPTNodeView rptNodeView) {
        SparseTree rptGraph = genGraph(rptNodeView);
        return rptGraph;
    }

    private static SparseTree genGraph(RPTNodeView rootNode) {
        RptVertex root = new RptVertex(rootNode);
        root.setColorMap();
        totalWeight = rootNode.getClassLabelDistributionView().getTotalNumValues();
        SparseTree graphT = new SparseTree(root);
        recurseOnRPNode(graphT, root, rootNode);
        return graphT;
    }

    private static void recurseOnRPNode(SparseTree graphT, RptVertex parent, RPTNodeView rptNode) {
        if (rptNode.getNoBranchView() == null || rptNode.getYesBranchView() == null) {
            return;
        }


        RptVertex yesChild = new RptVertex(rptNode.getYesBranchView(), true);
        RptVertex noChild = new RptVertex(rptNode.getNoBranchView(), false);

        //Add each child to the graph
        graphT.addVertex(yesChild);
        graphT.addVertex(noChild);

        //Set weights on children
        double yesWeight = yesChild.getClassLabelDistributionView().getTotalNumValues() / totalWeight;
        double noWeight = noChild.getClassLabelDistributionView().getTotalNumValues() / totalWeight;

        graphT.addEdge(new RptDirectedEdge(parent, yesChild, yesWeight, true));
        graphT.addEdge(new RptDirectedEdge(parent, noChild, noWeight, false));

        recurseOnRPNode(graphT, yesChild, rptNode.getYesBranchView());
        recurseOnRPNode(graphT, noChild, rptNode.getNoBranchView());
    }

}
