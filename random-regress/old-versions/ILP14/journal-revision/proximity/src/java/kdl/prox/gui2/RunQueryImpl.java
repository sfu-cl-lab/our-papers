/**
 * $Id: RunQueryImpl.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import java.io.File;
import kdl.prox.db.Container;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.qgraph2.QueryXMLUtil;
import org.jdom.Element;


/**
 * Concrete implementation that runs a QGraph query, taking care of routing
 * log4j output (via parent) to interested parties via property changes.
 * <p/>
 * todo would be nice to show % of query run
 * todo would be nice to support cancel()
 */
public class RunQueryImpl extends AbstractRunFileImpl {

    private Container inputContainer;
    private String outputContainerName;


    /**
     * @param queryFileOrEle      QGraph XML query File or Element to run
     * @param inputContainer
     * @param outputContainerName name of the top-level container to save results to
     */
    public RunQueryImpl(Object queryFileOrEle, Container inputContainer,
                        String outputContainerName) {
        super(queryFileOrEle);
        this.inputContainer = inputContainer;
        this.outputContainerName = outputContainerName;
        if (!((inputObject instanceof File) || (inputObject instanceof Element)))
        {
            throw new IllegalArgumentException("don't know how to run " +
                    "objects of type " + inputObject.getClass());
        }
    }

    public String getInputObjectString() {
        if (inputObject instanceof File) {
            return ((File) inputObject).getName();
        } else {
            return "<QGraph Query>";
        }
    }

    /**
     * Runs the query.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        try {
            startLog4jRouting();
            fireChange("status", null, "starting running query: " + inputObject);
            if (inputObject instanceof File) {
                Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile((File) inputObject);
                QueryGraph2CompOp.queryGraph(graphQueryEle,
                        inputContainer, outputContainerName, true);
            } else if (inputObject instanceof Element) {
                QueryGraph2CompOp.queryGraph((Element) inputObject,
                        inputContainer, outputContainerName, true);
            } else {
                throw new IllegalArgumentException("don't know how to run " +
                        "objects of type " + inputObject.getClass());
            }
        } finally {
            stopLog4jRouting();
            fireChange("status", null, "finished running query");
        }
    }

}
