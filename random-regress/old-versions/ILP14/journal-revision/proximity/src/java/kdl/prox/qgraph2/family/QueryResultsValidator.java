/**
 * $Id: QueryResultsValidator.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryResultsValidator.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.family;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * Helps compare expected and actual temporary SGI NSTs.
 * Usage:
 * <p/>
 * new QueryResultsValidator with actual and expected SGIs, or
 * with Container with results and mat file
 * <p/>
 * getFailureMessages()
 */
public class QueryResultsValidator {

    private static Logger log = Logger.getLogger(QueryResultsValidator.class);


    /**
     * Loaded by constructor
     * Store the expected and observed results in a set
     * of subgraphs, represented in turn as sets, each of which corresponds toan element in the set
     */
    private Set expectedResults;
    private Set actualResults;
    private int expectedRowCount;
    private int actualRowCount;

    // Should the name column of the SGI be used in the comparison?
    // It's not used when the results being compared are an SGI and a Mat file
    // It is used if the results being compared are four SGIs (two object and two link)
    private boolean isCompareName;


    /**
     * Constructor that receives the four SGI NSTs
     *
     * @param actObjSGINST
     * @param actLinkSGINST
     * @param expObjSGINST
     * @param expLinkSGINST
     * @
     */
    public QueryResultsValidator(NST actObjSGINST, NST actLinkSGINST,
                                 NST expObjSGINST, NST expLinkSGINST) {
        isCompareName = true;
        // load the SGI NSTs
        loadAllSGIs(actObjSGINST, actLinkSGINST, expObjSGINST, expLinkSGINST);
    }


    public QueryResultsValidator(Container resultCont, File matFile)
            throws IOException, MonetException {
        // do not compare the names in the SGI tables
        isCompareName = true;
        // get NSTs from resultContainer
        List actSGINSTs = getSGINSTsFromContainer(resultCont);
        NST actObjSGINST = (NST) actSGINSTs.get(0);
        NST actLinkSGINST = (NST) actSGINSTs.get(1);
        // read mat file into SGIs
        List expSGINSTs = readMatFileIntoSGINSTs(matFile);
        NST expObjSGINST = (NST) expSGINSTs.get(0);
        NST expLinkSGINST = (NST) expSGINSTs.get(1);
        // load the SGI NSTs
        loadAllSGIs(actObjSGINST, actLinkSGINST, expObjSGINST, expLinkSGINST);
        // release NSTs created when reading the MAT files
        // actual NSTs are deleted by the calling app, probably by deleting the container
        expObjSGINST.release();
        expLinkSGINST.release();
    }


    /**
     * Reads an attrDataNST and inserts all values into a Map,
     * from value -> item_id
     *
     * @param attrDataNST
     * @param valueToIdMap
     * @
     */
    private void addAttributeValuesToMap(NST attrDataNST, Map valueToIdMap) {
        ResultSet resultSet = attrDataNST.selectRows();
        while (resultSet.next()) {
            int item_id = resultSet.getOID(1);
            String value = resultSet.getString(2);
            valueToIdMap.put(value, new Integer(item_id));
        }
    }


    /**
     * Reads an SGI NST and fills a map keyed by subgraph. The elements of
     * the map are Sets, each item being "name" (of the item) . "item_id"
     *
     * @param tempSGINST
     * @param subgraphElements
     * @
     */
    private void addElementsToSubgMap(NST tempSGINST, Map subgraphElements) {
        ResultSet resultSet = tempSGINST.selectRows();
        while (resultSet.next()) {
            int itemID = resultSet.getOID(1);
            Integer subgID = new Integer(resultSet.getOID(2));
            String name = resultSet.getString(3);
            Set thisSubgraph = (TreeSet) subgraphElements.get(subgID);
            if (thisSubgraph == null) {
                thisSubgraph = new TreeSet();
                subgraphElements.put(subgID, thisSubgraph);
            }
            if (isCompareName) {
                thisSubgraph.add(name + "." + itemID);
            } else {
                thisSubgraph.add(itemID + "");
            }
        }
    }


    /**
     * Returns a list of strings with error messages
     *
     * @return
     */
    public List getFailureMessages() {
        List failureMessages = new ArrayList();
        if (!expectedResults.equals(actualResults)) {
            Iterator expectedResultsIter = expectedResults.iterator();
            while (expectedResultsIter.hasNext()) {
                String thisSubgraph = (String) expectedResultsIter.next();
                if (!actualResults.contains(thisSubgraph)) {
                    String msg = "Expected subgraph [" + thisSubgraph +
                            "] not found in actual results.";
                    failureMessages.add(msg);
                }
            }
            Iterator actualResultsIter = actualResults.iterator();
            while (actualResultsIter.hasNext()) {
                String thisSubgraph = (String) actualResultsIter.next();
                if (!expectedResults.contains(thisSubgraph)) {
                    String msg = "Subgraph [" + thisSubgraph +
                            "] found in results is not expected.";
                    failureMessages.add(msg);
                }
            }
        }
        if (expectedRowCount != actualRowCount) {
            failureMessages.add("Expected " + expectedRowCount + " rows in " +
                    "the SGI table, but found " + actualRowCount + " instead");
        }
        return failureMessages;
    }


    /**
     * Gets both the object and links subg_items NSTs from a Container
     * and returns them in a list.
     *
     * @param resultCont
     * @return
     * @
     */
    private List getSGINSTsFromContainer(Container resultCont) {
        Assert.notNull(resultCont, "null resultCont");
        NST objNST = resultCont.getItemNST(true);
        NST linkNST = resultCont.getItemNST(false);
        // put them in a list and return
        List NSTsList = new ArrayList();
        NSTsList.add(objNST);
        NSTsList.add(linkNST);
        return NSTsList;
    }


    /**
     * Called by constructor, returns a Set: Elements in the sets are strings. Each
     * element (string) represents a subgraph, and the names of the items in
     * the subgraph are sorted alphabetically (e.g., "a1 b1 y1"). Names are the
     * names of the items in the subgraph, and the numbers are the item_ids.
     *
     * @param objTempSGINST
     * @param linkTempSGINST
     * @return
     */
    private Set loadTempSGINST(NST objTempSGINST, NST linkTempSGINST) {
        // Read everything into a map of sets
        // Uses a  map to store the elements of each subgraph
        Map subgraphElements = new HashMap();
        addElementsToSubgMap(objTempSGINST, subgraphElements);
        addElementsToSubgMap(linkTempSGINST, subgraphElements);
        
        // Everything has been read. Thansform the map of sets into a set of strings	
        Set subgraphStringsSet = new TreeSet();
        Iterator subgraphElementsIter = subgraphElements.keySet().iterator();
        while (subgraphElementsIter.hasNext()) {
            Integer thisSubgraphID = (Integer) subgraphElementsIter.next();
            Set thisSubgraph = (TreeSet) subgraphElements.get(thisSubgraphID);
            String thisSubgString = "";
            Iterator thisSubgElements = thisSubgraph.iterator();
            while (thisSubgElements.hasNext()) {
                thisSubgString = thisSubgString + " " + (String) thisSubgElements.next();
            }
            thisSubgString = thisSubgString + " ";
            subgraphStringsSet.add(thisSubgString);
        }
        return subgraphStringsSet;
    }


    /**
     * Internal method call from constructors
     *
     * @param actObjTempSGINST
     * @param actLinkTempSGINST
     * @param expObjTempSGINST
     * @param expLinkTempSGINST
     * @
     */
    private void loadAllSGIs(NST actObjTempSGINST, NST actLinkTempSGINST,
                             NST expObjTempSGINST, NST expLinkTempSGINST) {

        Assert.notNull(actObjTempSGINST, "null actObjTempSGINST");
        Assert.notNull(actLinkTempSGINST, "null actLinkTempSGINST");
        Assert.notNull(expObjTempSGINST, "null expObjTempSGINST");
        Assert.notNull(expLinkTempSGINST, "null expLinkTempSGINST");

        actualResults = loadTempSGINST(actObjTempSGINST, actLinkTempSGINST);
        expectedResults = loadTempSGINST(expObjTempSGINST, expLinkTempSGINST);
        actualRowCount = actObjTempSGINST.getRowCount() + actLinkTempSGINST.getRowCount();
        expectedRowCount = expObjTempSGINST.getRowCount() + expLinkTempSGINST.getRowCount();
    }


    /**
     * Reads a .mat file and saves its contents into two SGI tables,
     * object and link.
     *
     * @param matFile
     * @return
     */
    private List readMatFileIntoSGINSTs(File matFile)
            throws IllegalArgumentException, IOException, MonetException {
        Assert.notNull(matFile, "null matFile"); 
        // first of all, we read the ObjectName attribute into a 
        // name->id map (names are unique, and the db is small enough to
        // fit into memory).
        Map nameToIdMap = new HashMap();
        NST objNameNST = DB.getObjectAttrs().getAttrDataNST("ObjectName");
        NST linkNameNST = DB.getLinkAttrs().getAttrDataNST("LinkName");
        addAttributeValuesToMap(objNameNST, nameToIdMap);
        addAttributeValuesToMap(linkNameNST, nameToIdMap);
        // read line by line and save into an SGI. 
        // The item_id comes from the map
        // the subg_id is just a counter that we make up, incremented per line
        // the name is always a fixed string, because it won't be used in the comparison
        // All the rows go into the same SGI, irrespective of whether they are
        // objects or links, because the addElementsToSubgMap method doesn't really care
        NST sgiNST = SGIUtil.createTempSGINST();
        int subg_id = 0;
        String name = "dummy";
        BufferedReader buffReader = new BufferedReader(new FileReader(matFile));
        for (String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
            subg_id++;
            StringTokenizer stringTokenizer = new StringTokenizer(line);
            while (stringTokenizer.hasMoreTokens()) {
                String item_name = stringTokenizer.nextToken();
                Integer item_id = (Integer) nameToIdMap.get(item_name);
                Assert.notNull(item_id, "item_name not found in map!: " + item_name);
                name = item_name.substring(0, 1).toUpperCase();
                sgiNST.insertRow(new String[]{item_id.intValue() + "", subg_id + "", name});
            }
        }

        NST distinctRowsNST = sgiNST.distinct();
        Assert.condition(distinctRowsNST.getRowCount() == sgiNST.getRowCount(),
                "Error reading mat file.  Found duplicate entries in subgraph.");
        distinctRowsNST.release();

        // put them in a list and return
        // The link SGI is going to be an empty one
        List NSTsList = new ArrayList();
        NSTsList.add(sgiNST);
        NSTsList.add(SGIUtil.createTempSGINST());
        return NSTsList;
    }


}
