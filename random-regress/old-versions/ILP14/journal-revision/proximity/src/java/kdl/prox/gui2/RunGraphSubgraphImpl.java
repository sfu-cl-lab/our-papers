/**
 * RunGraphSubgraphImpl.java,v 1.1 2004/05/25 16:52:42 schapira Exp
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import edu.uci.ics.jung.graph.Graph;
import kdl.prox.db.Container;
import kdl.prox.db.Subgraph;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


public class RunGraphSubgraphImpl extends AbstractRunFileImpl {

    private static Logger log = Logger.getLogger(RunGraphSubgraphImpl.class);

    // filled or set by prepareGraph(), respectively:
    Graph graph;
    String[] names = null;


    public RunGraphSubgraphImpl(ProxURL subgURL, Graph graph) {
        super(subgURL);
        this.graph = graph;
    }

    public String getInputObjectString() {
        if (inputObject instanceof ProxURL) {
            return ((ProxURL) inputObject).toString();
        } else {
            return "<Subgraph>";
        }
    }

    public String[] getNames() {
        Assert.notNull(names, "need to call start() before getting the names");
        return names;
    }

    /**
     * Prepares the subgraph for display.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        try {
            startLog4jRouting();
            fireChange("status", null, "starting subgraph layout");

            // do the computation
            GUIContentGenerator.getGraphForSubgURL((ProxURL) inputObject, graph);
            final ProxURL subgURL = (ProxURL) inputObject;
            Subgraph subgraph = subgURL.getSubgraph();
            Container parentContainer = subgraph.getParentContainer();
            names = GUIContentGenerator.getUniqueNamesInContainer(parentContainer);

            fireChange("graph", null, null);
        } finally {
            stopLog4jRouting();
            fireChange("status", null, "finished subgraph layout");
        }
    }

}
