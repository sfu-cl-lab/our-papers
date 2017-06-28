/**
 * $Id: QGraphQueryTester.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.family;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.qgraph2.TFMGraph;
import kdl.prox.qgraph2.TGPath;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * runs all transformation paths, test that they produce same result
 * and compares actual result to expected results
 */
public class QGraphQueryTester implements QueryTester {

    private static Logger log = Logger.getLogger(QGraphQueryTester.class);

    private List<QueryTestFailure> failureList;


    public List<QueryTestFailure> testQuery(QueryDataPair queryDataPair, String familyName) throws Exception {

        failureList = new ArrayList<QueryTestFailure>();

        // iterate over all transformation graph paths. error if no solutions
        File queryFile = queryDataPair.getQueryFile();
        Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);
        QueryGraph2CompOp queryGraph2CO = new QueryGraph2CompOp(graphQueryEle);
        TFMGraph tfmGraph = queryGraph2CO.tfmGraph();
        List tgPaths = QueryGraph2CompOp.tgPathsFromTFMGraph(tfmGraph);    // returns only solutions
        Assert.condition(!tgPaths.isEmpty(), "no solutions for query: " + queryFile);

        // execute all paths
        Iterator tgPathIter = tgPaths.iterator();
        while (tgPathIter.hasNext()) {
            TGPath tgPath = (TGPath) tgPathIter.next();
            testQueryPath(familyName, queryDataPair, tgPath, queryGraph2CO);
        }

        return failureList;
    }

    /**
     * Tests the query in queryDataPair by executing it using tgPath and
     * comparing the results to the .mat file in queryDataPair.
     *
     * @param queryDataPair
     * @param tgPath
     * @param queryGraph2CO
     */
    private void testQueryPath(String familyName, QueryDataPair queryDataPair, TGPath tgPath, QueryGraph2CompOp queryGraph2CO) throws Exception {

        // root container, where results are to be saved
        // todo : use temp container
        Container container = DB.getRootContainer();

        // get the name of this query
        String queryName = queryDataPair.getQueryFile().getName();

        // execute the path, saving the result into its collection
        String queryCollName = "query-coll";
        if (container.hasChild(queryCollName)) {
            container.deleteChild(queryCollName);
        }
        log.debug("testQueryPath(): executing query path: " + tgPath + " -> " +
                queryCollName); // TEST

        DB.beginScope();
        try {
            Container resultContainer = queryGraph2CO.execTGPath(tgPath, new HashMap(), null, queryCollName);
            // create a set helper, and report errors if any
            QueryResultsValidator setHelper = new QueryResultsValidator(resultContainer, queryDataPair.getMatFile());
            List failureMessages = setHelper.getFailureMessages();
            if (failureMessages.size() != 0) {
                log.warn("===> " + queryName + "|" + tgPath.toString() + ":ACTUAL AND EXPECTED RESULTS DO NOT MATCH");
                for (Iterator iter = failureMessages.iterator(); iter.hasNext();) {
                    String msg = (String) iter.next();
                    QueryTestFailure thisFailure = new QueryTestFailure(familyName, queryName, tgPath.toString(), msg);
                    failureList.add(thisFailure);
                }
            } else {
                log.warn("===> " + queryName + "|" + tgPath.toString() + ":Actual and Expected results match");
            }
        } catch (Exception e) {
            String msg = "exception testing path " + tgPath + ":" + e;
            QueryTestFailure thisFailure = new QueryTestFailure(familyName, queryName, tgPath.toString(), msg);
            failureList.add(thisFailure);
            log.error(msg);
            e.printStackTrace();
        } finally {
            DB.endScope();
            // delete the container and release its NSTs
            if (container.hasChild(queryCollName)) {
                container.deleteChild(queryCollName);
            }
        }

    }

}
