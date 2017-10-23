/**
 * $Id: QGUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.util;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.ComparisonOperatorEnum;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class encapsulates graph operations that are mainly used by qGraph but can also
 * be used from other packages.
 * <p/>
 * It provides operations such as getMatchingObjects/Links which, given a condition.
 * return the list of objects/links that match it.
 * <p/>
 * It also provides getObjectsConnectedVia(Un)DirectedLinks which, given two NSTs
 * with objects and one NST with links, returns an NST with the list of the way
 * in which those objects are connected via those links.
 * <p/>
 * More closely related to qGraph, there is a method called connectConsolidatedVertices
 * which receives two SGI tables and a qGraph edge, and it creates a new SGI table with
 * the connected consolidated vertices.
 * <p/>
 * Finally, there is a reCheckLinks method that, given a table of links and a table
 * of objects (in the subgraph format), will make sure that the links are actually
 * connecting elements in the objects table.
 * <p/>
 * If an instance of this class is created with a Container that is not null, then
 * all the operations are limited to that container. Therefore, getMatchingLinks will
 * for example return the list of links that match a condition,  not from the
 * universe of  all links in the database but only from the list of links in the
 * container.
 * <p/>
 * The class creates some intermediate NSTs and BATs that need to be accessible
 * until the class is not needed anymore. Users of this class are expected
 * to call the release() method when they're done using it.
 */
public class QGUtil {

    private static Logger log = Logger.getLogger(QGUtil.class);

    /**
     * A mixed List of bat var names (StringS) and NSTs that are to be released
     * by release(). Managed by getXX() methods.
     */
    private List nstReleaseList = new ArrayList();

    // Source container against which the query is run --null if the query is to
    // be run against the entire database.
    // If not null, sourceContainer cannot contain child containers; just subgraphs.
    // When sourceContainer is not null, methods getObjectNST, getLinkNST, getAttrDataNST
    // filter their NST so that they only return objects and links that are part
    // of the container
    Container sourceContainer = null;

    // Some cached BATs, for speed. Set by constructor
    NST cachedListOfObjects = null;
    NST cachedListOfLinks = null;

    public QGUtil(Container sourceContainer) {
        this.sourceContainer = sourceContainer;
        if (sourceContainer != null) {
            NST[] objectAndLinkFilterNSTs = sourceContainer.getObjectAndLinkFilterNSTs();
            cachedListOfObjects = objectAndLinkFilterNSTs[0];
            cachedListOfLinks = objectAndLinkFilterNSTs[1];
            nstReleaseList.add(cachedListOfObjects);
            nstReleaseList.add(cachedListOfLinks);
        }
    }

    /**
     * Returns an array with a combination of elements from the first list
     * prepended with firstPrefix, followed by elements from the second
     * list prepended with secondPrefix
     *
     * @param firstList
     * @param secondList
     * @param firstPrefix
     * @param secondPrefix
     * @return
     */
    private String[] combineColNamesWithPrefix(List firstList, List secondList,
                                               String firstPrefix,
                                               String secondPrefix) {

        Assert.notNull(firstList, "null first list");
        Assert.notNull(secondList, "null second list");
        Assert.notNull(firstPrefix, "null first prefix -- use empty instead");
        Assert.notNull(secondPrefix, "null second prefix -- use empty instead");

        String[] retList = new String[firstList.size() + secondList.size()];
        int i = 0;

        // Copy elements, with prefixes, into array
        for (Iterator iter = firstList.iterator(); iter.hasNext();) {
            String thisElt = (String) iter.next();
            retList[i++] = firstPrefix + thisElt;
        }
        for (Iterator iter = secondList.iterator(); iter.hasNext();) {
            String thisElt = (String) iter.next();
            retList[i++] = secondPrefix + thisElt;
        }

        return retList;
    }

    /**
     * Used by TFM 02 and TFM 12
     * <p/>
     * A complicated routine that combines two consolitades vertices (represented
     * by the ObjTempSGINST tables)
     * <p/>
     * Finds all links of type Y, As in vertex A, Bs in vertex B, and connects them,
     * Removes rows that do not match the Y annotation
     * and recomputes subg_id if there is an expansion because no annotation is present
     * <p/>
     * Returns a table with the following format
     * a_(item_id subg_id name) o1_id o2_id link_id  b_(item_id ...) new_subg_id
     * <p/>
     * The resulting NST is materialized
     *
     * @param qgEdgeY
     * @param aObjTempSGINST
     * @param bObjTempSGINST
     * @param vertexAName
     * @param vertexBName
     * @return
     */
    public NST connectConsolidatedVertices(QGEdge qgEdgeY,
                                           NST aObjTempSGINST, NST bObjTempSGINST,
                                           String vertexAName, String vertexBName) {

        // 1. Get all links of the correct type (eg, ActorOf)
        // filteredLinksNST = [o1_id, o2_id, link_id]
        NST filteredLinksNST = getMatchingLinks(qgEdgeY.condEleChild());

        // 2. Get IDs of objects to be joined by these links, from each side
        // filteredAObjs = [item_id, subg_id, name]- idem for B
        NST filteredAObjs = SGIUtil.getSubgraphItemsWithName(aObjTempSGINST, vertexAName);
        NST filteredBObjs = SGIUtil.getSubgraphItemsWithName(bObjTempSGINST, vertexBName);

        // 3. Compute A --> links <-- B
        // aXbNST = [a_(item_id, subg_id, name), o1_id, o2_id, link_id, b_(...)]
        NST aXbNSTExt;
        if (qgEdgeY.isDirected()) {
            boolean isReverseDir = (!qgEdgeY.vertex1Name().equals(vertexAName));
            aXbNSTExt = getObjectsConnectedViaDirectedLinks(filteredAObjs, filteredBObjs,
                    filteredLinksNST, isReverseDir);
        } else {
            aXbNSTExt = getObjectsConnectedViaUndirectedLinks(filteredAObjs,
                    filteredBObjs, filteredLinksNST);
        }

        // getObjectsConnectedViaDirectedLinks, in its two forms, only looks at
        // item_id and o1_id/o2_id. If we are joining two identical consolidated
        // vertices (from self-loops), we now need to make sure that we only
        // keep rows that actually correspond to the same subgraph (otherwise,
        // we could end up having objects that are connected via links but
        // that belong to different subgraphs
        NST aXbNST;
        if (aObjTempSGINST == bObjTempSGINST) {
            aXbNST = aXbNSTExt.filter("a_subg_id EQ b_subg_id");
            aXbNSTExt.release();
        } else {
            aXbNST = aXbNSTExt;
        }
        // 4. apply annotations
        //         1. Group by A/B(item_id, subg_id) to group objects in same
        //            subg that are connected by many links
        //         2. Remove those whose counts are past the limits
        NST linksAndNewSubgNST;
        if (qgEdgeY.isAnnotated()) {
            Annotation yAnnot = qgEdgeY.annotation();
            aXbNST.groupBy("a_item_id, a_subg_id, b_item_id, b_subg_id");
            aXbNST.addCountColumn("group_id", "group_cnt");
            NST reducedNST = SGIUtil.getRowsWithinRange(aXbNST, "group_cnt",
                    yAnnot.annotMin(), yAnnot.annotMax());
            // Renumber them using the group_id column;
            // rows with the same id get the same new_subg_id
            // unless the join was made on a self-loop, in which case
            // the subgraphs are already re-numbered and we don't want to change that.
            if (aObjTempSGINST == bObjTempSGINST) {
                reducedNST.addCopyColumn("a_subg_id", "new_subg_id");
            } else {
                reducedNST.addCopyColumn("group_id", "new_subg_id");
            }
            linksAndNewSubgNST = reducedNST.copy();
            reducedNST.release();
        } else {
            // 5. materialize aAndbfilteredLinksNST and
            // number it to create new subgraphs
            aXbNST.addNumberColumn("new_subg_id");
            linksAndNewSubgNST = aXbNST.copy();
        }

        aXbNST.release();
        filteredAObjs.release();
        filteredBObjs.release();
        filteredLinksNST.release();

        return linksAndNewSubgNST;
    }


    /**
     * Returns an NST with all the object/links that match a given condition
     *
     * @param condEleChild
     * @param isObject
     * @return
     */
    private NST getMatchingItems(Element condEleChild, boolean isObject) {

        NST nonUniqueNST;

        if (condEleChild == null) { // just return all objects or links
            if (isObject) {
                nonUniqueNST = DB.getObjectNST().project("id");
            } else {
                nonUniqueNST = DB.getLinkNST().project("link_id");
                nonUniqueNST.renameColumn("link_id", "id");
            }
        } else {
            // handle non-null condition
            nonUniqueNST = processCondEleChild(condEleChild, isObject);
        }

        // call 'unique' because we can't depend on even
        // the top-level objects and links being unique (not to mention
        // other input items, like subgraphs and containers)
        NST uniqueNST = nonUniqueNST.distinct("id");
        nonUniqueNST.release();

        // And now restrict the set to those in the sourceContainer, if any
        NST filterNST = (isObject ? cachedListOfObjects : cachedListOfLinks);
        if (filterNST != null) {
            NST retNST = uniqueNST.intersect(filterNST, "id EQ id");
            uniqueNST.release();
            return retNST;
        }

        return uniqueNST;
    }

    /**
     * Finds all the links that match a given condition
     * Returns an NST with [o1_id o2_id link_id]
     * <p/>
     * Most of the work is done by
     *
     * @param condEleChild
     * @return
     */
    public NST getMatchingLinks(Element condEleChild) {
        NST matchingIDs = getMatchingItems(condEleChild, false);
        NST retNST = DB.getLinkNST().intersect(matchingIDs, "link_id EQ id");
        matchingIDs.release();
        return retNST;
    }


    /**
     * Finds all the objects that match a given condition
     * Returns an NST with [o_id]
     *
     * @param condEleChild
     * @return
     */
    public NST getMatchingObjects(Element condEleChild) {
        // don't add matchingIDs to nstReleaseList because it'll be
        // released when matchingObjects is released
        NST matchingIDs = getMatchingItems(condEleChild, true);
        matchingIDs.renameColumn("id", "o_id");
        return matchingIDs;
    }

    /**
     * Given a list of objects A, a list of objects B, and a set of links L
     * this method does two joins to compute
     * A X B, that is, an NST with the columns from A, L and B
     * and rows such that
     * <p/>
     * A.item_id = L.o1_id && B.item_id = L.o2_id
     * (if isReverseDir, then A.item_id = L.o2_id && B.item_id = L.o1_id)
     * <p/>
     * The join is done incrementally, with two calls to the NST join constructor
     * In the first pass, aObjs is joined with L (item_id = o1_id)
     * in the second pass, the result above is joined with B (o2_id = item_id)
     * <p/>
     * The object lists may come as a consolidated vertex, with (item_id, subg_id, name)
     * or simply as a list of IDS (o_id). This method is able to handle both
     *
     * @param aObjs
     * @param bObjs
     * @param links
     * @param isReverseDir
     * @return
     */
    public NST getObjectsConnectedViaDirectedLinks(NST aObjs, NST bObjs, NST links,
                                                   boolean isReverseDir) {

        Assert.notNull(aObjs, "null aObjs");
        Assert.notNull(bObjs, "null bObjs");
        Assert.notNull(links, "null links");

        // Reverse direction?
        String firstPassLinkEndName = (isReverseDir ? "o2_id" : "o1_id");
        String secondPassLinkEndName = (isReverseDir ? "o1_id" : "o2_id");

        // First join, A x L - column names depend on input tables
        NST firstPass;
        String[] firstPassColNames = combineColNamesWithPrefix(aObjs.getNSTColumnNames(),
                links.getNSTColumnNames(), "a_", "");
        if (aObjs.getNSTColumnNames().size() == 1) {
            firstPass = aObjs.join(links, "o_id" + " = " + firstPassLinkEndName).renameColumns(Util.join(firstPassColNames, ","));
        } else {
            firstPass = aObjs.join(links, "item_id" + " = " + firstPassLinkEndName).renameColumns(Util.join(firstPassColNames, ","));
        }
        // Second pass. Get A x L x B
        NST secondPass;
        String[] secondPassColNames = combineColNamesWithPrefix(Arrays.asList(firstPassColNames),
                bObjs.getNSTColumnNames(), "", "b_");
        if (bObjs.getNSTColumnNames().size() == 1) {
            secondPass = firstPass.join(bObjs, secondPassLinkEndName + " = " + "o_id").renameColumns(Util.join(secondPassColNames, ","));
        } else {
            secondPass = firstPass.join(bObjs, secondPassLinkEndName + " = " + "item_id").renameColumns(Util.join(secondPassColNames, ","));
        }

        nstReleaseList.add(firstPass);
        return secondPass;
    }

    /**
     * Given a list of objects A, a list of objects B, and a set of links L
     * this method gets the list of connections, irrespective of the direction
     * of the links
     * It works by calling getObjectsConnectedViaDirectedLinks twice and then merging the results
     * <p/>
     * The object lists may come as a consolidated vertex, with (item_id, subg_id, name)
     * or simply as a list of IDS (o_id). This method is able to handle both
     *
     * @param aObjs
     * @param bObjs
     * @param links
     * @return
     */
    public NST getObjectsConnectedViaUndirectedLinks(NST aObjs, NST bObjs, NST links) {

        NST firstPass = getObjectsConnectedViaDirectedLinks(aObjs, bObjs, links, true);
        NST secondPass = getObjectsConnectedViaDirectedLinks(aObjs, bObjs, links, false);

        NST allLinks = firstPass.copy();
        // Remove self-loops from the second pass; they were already brought in in the first pass
        NST secondNoLoops = secondPass.filter("o1_id NE o2_id");
        allLinks.insertRowsFromNST(secondNoLoops);

        firstPass.release();
        secondPass.release();
        secondNoLoops.release();
        return allLinks;
    }


    /**
     * Called by getMatchingItems, executes the boolean logic in condEleChild, which
     * is an <or>, <and>, <not>, or <test> Element. Handles <or>, <and>, and
     * <not> by recursing.
     * Returns an NST that contains the matching IDs
     * <p/>
     * The returned NST has to be released by the caller
     */
    private NST processCondEleChild(Element condEleChild, boolean isObject) {
        Assert.condition(condEleChild != null, "null condEleChild!");

        // continue: dispatch based on condEleChild
        String connector = condEleChild.getName();
        if ((connector.equalsIgnoreCase("or")) || (connector.equalsIgnoreCase("and"))) {
            // recurse on children in order, adding appropriate boolean between.
            // recall: <or> contains one or more <and> elements; <and> contains
            // one or more <not> or <test> elements
            List children = condEleChild.getChildren();
            int childNum = 1;            // updated in loop. NB: not 0!
            Iterator childIter = children.iterator();
            NST previousNST = null;
            while (childIter.hasNext()) {
                Element childElement = (Element) childIter.next();
                NST currentNST = processCondEleChild(childElement, isObject);
                if (childNum > 1) {
                    NST combinationNST;
                    if (connector.equalsIgnoreCase("or")) {
                        combinationNST = previousNST.union(currentNST, "id");
                    } else {
                        combinationNST = previousNST.intersect(currentNST, "id EQ id");
                    }
                    previousNST.release();
                    currentNST.release();
                    currentNST = combinationNST;
                }
                previousNST = currentNST;
                childNum++;
            }
            return previousNST;

        } else if (connector.equalsIgnoreCase("not")) {

            // recurse: <not> contains one <test> element
            Element testEle = (Element) condEleChild.getChildren().get(0);
            NST allItemsNST = (isObject ? DB.getObjectNST() : DB.getLinkNST());

            NST matches = processTestEle(testEle, isObject);
            String columnName = (isObject ? "id" : "link_id");
            NST notinItems = allItemsNST.difference(matches, columnName + " = id", columnName);
            notinItems.renameColumn(columnName, "id");
            matches.release();
            return notinItems;

        } else {        // <test>
            return processTestEle(condEleChild, isObject);
        }
    }

    /**
     * Processes a single test element
     * Returns the result an NST with and id column listing the items
     * that match the condition
     *
     * @param testEle
     * @param isObject
     */
    private NST processTestEle(Element testEle, boolean isObject) {
        Assert.condition(testEle.getName().equalsIgnoreCase("test"),
                "can only handle <test>");
        // get the ops for the condition and check they're OK
        List childEles = testEle.getChildren();
        Element operatorEle = (Element) childEles.get(0);
        Element operand1Ele = (Element) childEles.get(1);
        Element operand2Ele = null;
        if (childEles.size() == 3) {
            operand2Ele = (Element) childEles.get(2);
        }
        String operator = operatorEle.getText();
        String attrName = operand1Ele.getText();
        String attrVal = null;
        if (operand2Ele != null) {
            attrVal = operand2Ele.getText();
        }

        // Get the Attribute table
        Attributes attr = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        NST dataNST = attr.getAttrDataNST(attrName);

        // Add a filter if not exists; if it's exists, then just
        // return the entire id BAT for the attribute
        NST filteredNST;
        if (!operator.equalsIgnoreCase("exists")) {
            if (dataNST.getNSTColumn("value").getType() == DataTypeEnum.STR) {
                attrVal = "'" + attrVal + "'";
            }
            ComparisonOperatorEnum oper = ComparisonOperatorEnum.enumForString(operator);
            filteredNST = dataNST.filter("value " + oper + " " + attrVal, "id");
        } else {
            filteredNST = dataNST.project("id");
        }
        dataNST.release();
        return filteredNST;
    }

    /**
     * Check that the links left in the link table still connect objects in the new object table.
     * <p/>
     * The process is the following:
     * a. First, only keep those subgraphs that are in the object table
     * b. In the subgraphs that stay, all the edges that are not incident should stay -- their elements were not removed
     * c. In the subgrapgs that stay, incident edges stay if one of their endpoints, an itemName, is still in the object table
     *
     * @param origLinkTempSGINST
     * @param newObjectTempSGINST
     * @param itemName
     * @param incidentEdges
     * @param newLinkTempSGINST
     */
    public void reCheckLinks(NST origLinkTempSGINST, NST newObjectTempSGINST,
                             String itemName, String[] incidentEdges,
                             NST newLinkTempSGINST) {

        // a. First keep only the subgraphs in the object table
        NST firstPassLinks = origLinkTempSGINST.intersect(newObjectTempSGINST, "subg_id EQ subg_id");

        // These are the types of links that we need to remove
        NST edgeListNST = new NST("name", "str");
        for (int edgeIdx = 0; edgeIdx < incidentEdges.length; edgeIdx++) {
            String incidentEdge = incidentEdges[edgeIdx];
            edgeListNST.insertRow(new String[]{incidentEdge});
        }

        // b. Of the subgraphs that stay, all the links that are not in incidentEdges stay
        NST otherLinks = firstPassLinks.filter("name NOTIN " + edgeListNST.getNSTColumn("name").getBATName());
        newLinkTempSGINST.insertRowsFromNST(otherLinks);
        otherLinks.release();

        // c. The other links, the ones in incidentEdges, are only going to be added if their Bs (itemName) are in Objects
        NST bListInObjects = newObjectTempSGINST.filter("name EQ '" + itemName + "'");

        NST incidentLinks = firstPassLinks.filter("name IN " + edgeListNST.getNSTColumn("name").getBATName());
        NST fullIncidentLinks = incidentLinks.join(DB.getLinkNST(), "item_id EQ link_id");
        fullIncidentLinks.addKeyColumn("key");
        incidentLinks.release();
        edgeListNST.release();

        //    We will put intermediate results in a new NST, named tempLinkIds, and finally insert the distinct
        //    rows into newLinkTempSGINST
        NST tempLinkIds = new NST("key", "oid");

        // c1. Insert those whose o1 is in the list of Bs
        NST o1 = fullIncidentLinks.intersect(bListInObjects, "subg_id = subg_id AND o1_id = item_id", "key");
        tempLinkIds.insertRowsFromNST(o1);
        // c2. Insert those whose o2 is in the list of Bs
        NST o2 = fullIncidentLinks.intersect(bListInObjects, "subg_id = subg_id AND o2_id = item_id", "key");
        tempLinkIds.insertRowsFromNST(o2);
        o1.release();
        o2.release();

        // d. Finally add into newLink table
        // do it from the unique keys, to avoid duplicates. tempLinkIds may have duplicates
        // from self-loops (inserted twice because both o1 and o2 id are in the list)
        // but they will have the same values in key. So, we get the distinct keys
        // and filter tempLinkIds by that. Then we insert into the final table
        // (note: we could do the same with insertDistinctRows, but that requires a very
        //        expensive group_by)
        NST uniqueTempLinks = fullIncidentLinks.filter("item_id KEYIN " + tempLinkIds.getNSTColumn("key").getBATName(), "item_id, subg_id, name");
        newLinkTempSGINST.insertRowsFromNST(uniqueTempLinks);
        uniqueTempLinks.release();

        tempLinkIds.release();
        firstPassLinks.release();
        bListInObjects.release();
        fullIncidentLinks.release();
    }


    /**
     * Releases any vars created during calls to other public methods.
     */
    public void release() {
        for (Iterator nstIter = nstReleaseList.iterator(); nstIter.hasNext();) {
            NST releaseNST = (NST) nstIter.next();
            releaseNST.release();
        }
    }
}
