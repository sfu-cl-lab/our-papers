/**
 * $Id: SNA.java 3680 2007-10-24 14:57:28Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import kdl.prox.app.DBUtil;
import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import sun.misc.Queue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;


/**
 * Defines various Social Network Analysis static methods.
 * <p/>
 * Note: The methods in this class are certainly not optimized for speed, and can
 * be very slow when run against large databases such as the IMDB.
 */
public class SNA {

    private static Logger log = Logger.getLogger(SNA.class);


    /**
     * Prohibit instances being created.
     */
    private SNA() {
    }


    public static NST calculateBetweennessNST(NST linkNST, boolean isUndirected) {
        // get the cleaned up bi-link table (no loops, no duplicate links, and
        // undirected)
        NST noLoopsNST = linkNST.filter("o1_id NE o2_id");
        NST o1o2NST = noLoopsNST.project("o1_id, o2_id");
        NST o2o1NST = noLoopsNST.project("o2_id, o1_id");
        NST biDirLink = o1o2NST.union(o2o1NST);
        NST neighborsNST = isUndirected ? biDirLink : o1o2NST.distinct();

        //initialize hash map to store centrality measure //todo it would be faster to do betweenness calcs in MIL
        HashMap BCMap = new HashMap();
        NST uniqueIDs = biDirLink.project("o1_id").distinct();
        ResultSet rs = uniqueIDs.selectRows();
        while (rs.next()) {
            int objID = rs.getOID(1); //object ids
            BCMap.put(new Integer(objID), new Integer(0));
        }

        //iterate over objects
        for (Iterator itr = BCMap.keySet().iterator(); itr.hasNext();) {
            Integer currObj = (Integer) itr.next();

            Stack S = new Stack();
            HashMap PMap = new HashMap();       //if no entry, should be empty list
            HashMap sigmaMap = new HashMap();   //if no entry, should by 0
            HashMap distMap = new HashMap();    //if no entry, should be -1
            sigmaMap.put(currObj, new Integer(1));
            distMap.put(currObj, new Integer(0));
            Queue Q = new Queue();
            Q.enqueue(currObj);
            while (!Q.isEmpty()) {
                try {
                    Integer v = (Integer) Q.dequeue();
                    S.push(v);
                    //get neighbors of v
                    Set neighbors = getNeighbors(neighborsNST, v);
                    for (Iterator nItr = neighbors.iterator(); nItr.hasNext();) {
                        Integer w = (Integer) nItr.next();
                        //found neighbor for the 1st time?
                        if (!distMap.containsKey(w)) {
                            Q.enqueue(w);
                            distMap.put(w, new Integer(((Integer) distMap.get(v)).intValue() + 1));
                        }
                        //shortest path to neighbor via v?
                        int distW = ((Integer) distMap.get(w)).intValue();
                        int distV = ((Integer) distMap.get(v)).intValue();
                        if (distW == distV + 1) {
                            int sigmaW = sigmaMap.containsKey(w) ? ((Integer) sigmaMap.get(w)).intValue() : 0;
                            int sigmaV = sigmaMap.containsKey(v) ? ((Integer) sigmaMap.get(v)).intValue() : 0;
                            sigmaMap.put(w, new Integer(sigmaW + sigmaV));
                            if (!PMap.containsKey(w)) {
                                PMap.put(w, new ArrayList());
                            }
                            ArrayList wList = (ArrayList) PMap.get(w);
                            wList.add(v);
                            PMap.put(w, wList);
                        }
                    }
                } catch (InterruptedException e) {//do nothing todo figure out what to do with this exception, does it ever happen?
                }
            }
            HashMap deltaMap = new HashMap();   //if no entry, should by 0
            // S returns vertices in order of non-increasing distance from currObj
            while (!S.isEmpty()) {
                Integer w = (Integer) S.pop();
                if (PMap.containsKey(w)) {
                    for (Iterator wItr = ((List) PMap.get(w)).iterator(); wItr.hasNext();) {
                        Integer v = (Integer) wItr.next();
                        int deltaW = deltaMap.containsKey(w) ? ((Integer) deltaMap.get(w)).intValue() : 0;
                        int deltaV = deltaMap.containsKey(v) ? ((Integer) deltaMap.get(v)).intValue() : 0;
                        deltaV += (((Integer) sigmaMap.get(v)).intValue() / ((Integer) sigmaMap.get(w)).intValue())
                                * (1 + deltaW);
                        deltaMap.put(v, new Integer(deltaV));
                        if (w.intValue() != currObj.intValue()) {
                            int bcW = BCMap.containsKey(w) ? ((Integer) BCMap.get(w)).intValue() : 0;
                            bcW += deltaW;
                            BCMap.put(w, new Integer(bcW));
                        }
                    }
                }
            }
        }
        //put values into nst
        uniqueIDs.addConstantColumn("value", "int", "0");
        //iterate over objects to write in betweeness value
        for (Iterator itr = BCMap.keySet().iterator(); itr.hasNext();) {
            Integer currObj = (Integer) itr.next();
            Integer currVal = (Integer) BCMap.get(currObj);
            if (isUndirected) {   //if undirected this overcounts by 2
                currVal = new Integer(currVal.intValue() / 2);
            }
            uniqueIDs.replace("o1_id EQ " + currObj, "value", currVal.toString());
        }

        return uniqueIDs;
    }

    /**
     * Does the majority of the work to calculate clustering coefficient.
     * <p/>
     * <p/>
     * The clustering coefficient for an object is the ratio of how many of its
     * neighbors are connected to each other over the possible number of connections
     * among those neighbors.
     * The approach taken is to
     * <p/><pre>
     *    a. Get a clean bi-link table, with self-loops and duplicates removed, and
     *       links treated as undirected links
     *    b. To compute the numerator, take the bi-link table and join it with itself
     *       to find, for each neighbor of an object, its own neighbors, again without
     *       self-loops, and then only keeping those neighbors that were neighbors of
     *       the original object (e.g. 1 -> 3 -> 17; then 17 is not kept because it's not
     *       directly connected to 1). Then we get a histogram of this, to count the actual
     *       number of interconnections between each object's neighbors
     *    c. To compute the denominator, we calculate a histogram to get the actual
     *       number of neighbors, and then compute the max-connections formula k (k-1) / 2
     * </pre><p/>
     * NOTE: Because in step b we get duplicate rows for each pair of neighbors
     * (e.g., 1 -> 3, 3 -> 1), we have to divide the count per object by 2. This two
     * would be canceled out with the 2 in the denominator, so we leave both out
     * in step b and c.
     *
     * @param linkNST
     * @return clustering coefficient NST: object OIDs and the clustering coefficient
     */
    private static NST calculateClusteringCoefficientNST(NST linkNST) {
        // get the cleaned up bi-link table (no loops, no duplicate links, and undirected)
        NST noDupLinksNST = linkNST.filter("o1_id NE o2_id", "o1_id, o2_id").distinct();
        NST biLinkNST = noDupLinksNST.union(noDupLinksNST.project("o2_id, o1_id"));
        NST uniqueIDs = biLinkNST.project("o1_id").addConstantColumn("numer", "int", "0");

        // Get the set of neighbors of neighbors for each, removing loops
        NST joinedLinks = biLinkNST.join(biLinkNST, "o2_id EQ o1_id", "o1_id, o2_id");
        NST neighConnsNoLoopsNST = joinedLinks.filter("o1_id NE o2_id");

        // calculate the numerator. Number of actual neighbors
        NST myNeighConnsNST = neighConnsNoLoopsNST.intersect(biLinkNST, "o1_id EQ o1_id AND o2_id EQ o2_id");
        NST numer = myNeighConnsNST.aggregate("count", "o1_id", "o2_id").renameColumn("o2_id", "numer");
        numer = numer.union(uniqueIDs, "o1_id"); // add zeros

        // calculate the denominator. Number of possible neighbors
        NST denom = biLinkNST.aggregate("count", "o2_id", "o1_id").renameColumn("o1_id", "ncount");
        denom = denom.filter("ncount >= 2");
        denom.addArithmeticColumn("ncount - 1", "dbl", "oneless");
        denom.addArithmeticColumn("ncount * oneless", "dbl", "denom");
        denom = denom.project("o2_id, denom");

        // Compute the ratio
        NST ccNST = numer.join(denom, "o1_id EQ o2_id");
        ccNST.castColumn("numer", "dbl").castColumn("denom", "dbl");
        ccNST.addArithmeticColumn("numer / denom", "dbl", "value");
        return ccNST.renameColumn("o1_id", "id").project("id, value");
    }


    /**
     * Calculates the connected components following the algorithm described
     * in CLR Chap. 22
     *
     * @param objectNST
     * @param linkNST
     * @return
     */
    public static NST calculateConnectedComponentsNST(NST objectNST, NST linkNST) {
        Assert.condition(DBUtil.isDBConsistent(objectNST, linkNST), "cannot compute connected components; " +
                "found links to non-existing objects. Run DBUtil to help fix this problem.");
        NST edgeListNST = linkNST.project("o1_id, o2_id");
        NST connectedNST = objectNST.copy().addCopyColumn("id", "group");

        int numEdges = edgeListNST.getRowCount();
        for (int edgeIdx = 0; edgeIdx < numEdges; edgeIdx++) {
            // Get the o1 and o2 of this link
            ResultSet resultSet = edgeListNST.selectRows("*", "o1_id, o2_id", edgeIdx + "-" + edgeIdx);
            resultSet.next();
            int o1_id = resultSet.getOID(1);
            int o2_id = resultSet.getOID(2);

            // Get the groups of o1 and o2
            resultSet = connectedNST.selectRows("id = " + o1_id, "group", "*");
            resultSet.next();
            int o1_set_id = resultSet.getOID(1);

            resultSet = connectedNST.selectRows("id = " + o2_id, "group", "*");
            resultSet.next();
            int o2_set_id = resultSet.getOID(1);

            if (o1_set_id != o2_set_id) {        // If they are not in the same, connect them
                connectedNST.replace("group = " + o2_set_id, "group", o1_set_id + "");
            }
        }

        return connectedNST;
    }

    /**
     * Calculates the hubs and authorities coefficients, and returns them as an
     * array [hub, auth]
     * <p/>
     * Caller has to release two NSTs
     *
     * @param objectNST
     * @param linkNST
     * @param numIterations
     */
    private static NST[] calculateHubsAndAuthoritiesNSTs(NST objectNST,
                                                         NST linkNST,
                                                         int numIterations) {
        // input NSTs: obj (list of objects), o1o2 (link table without duplicate links)
        NST o1o2NST = linkNST.project("o1_id, o2_id").distinct();

        // initial hubs and auths set to 1 for all objects
        NST currHubNST = objectNST.copy();
        currHubNST.addConstantColumn("hub", "dbl", "1.0");
        NST currAuthNST = objectNST.copy();
        currAuthNST.addConstantColumn("auth", "dbl", "1.0");

        // iterate
        for (int iter = 0; iter < numIterations; iter++) {
            NST nextAuthNotNormalizedNST = calculateNextAuth(currAuthNST, currHubNST, o1o2NST);
            NST nextHubNotNormalizedNST = calculateNextHub(nextAuthNotNormalizedNST, currHubNST, o1o2NST);
            NST nextAuthNST = normalizeNST(nextAuthNotNormalizedNST);
            NST nextHubNST = normalizeNST(nextHubNotNormalizedNST);
            currAuthNST = nextAuthNST;
            currHubNST = nextHubNST;
        }

        return new NST[]{currHubNST, currAuthNST};
    }

    /**
     * Computes the authorities coefficient based on the current hubs and auths
     * coefficients.
     * newAuth = sum(hubs coefficient of objects that point to me)
     * <p/>
     * Caller must release returned NST.
     *
     * @param currAuthNST
     * @param currHubNST
     * @param o1o2NST
     * @return
     */
    private static NST calculateNextAuth(NST currAuthNST, NST currHubNST, NST o1o2NST) {
        NST currHubCoeffs = o1o2NST.join(currHubNST, "o1_id EQ id", "o2_id, hub");
        currHubCoeffs.renameColumn("o2_id", "id");
        NST hubSum = currHubCoeffs.aggregate("sum", "id", "hub").renameColumn("hub", "auth");
        return hubSum.union(currAuthNST, "id"); // add those with zeros
    }

    private static NST calculateNextHub(NST nextAuthNST, NST currHubNST, NST o1o2NST) {
        NST currAuthCoeffs = o1o2NST.join(nextAuthNST, "o2_id EQ id", "o1_id, auth");
        currAuthCoeffs.renameColumn("o1_id", "id");
        NST authSum = currAuthCoeffs.aggregate("sum", "id", "auth").renameColumn("auth", "hub");
        return authSum.union(currHubNST, "id"); // add those with zeros
    }

    /**
     * Implements the betweenness centrality algorithm for unweighted graphs,
     * as described in "A Faster Algorithm for Betweenness Centrality",
     * U. Brandes, Journal of Mathematical Sociology 25(2):163-177, 2001.
     * <p/>
     * The betweenness centrality for a node v counts the number of shortest paths
     * between all other nodes in the graph that contain v. Note that a path of
     * length 1 does not contribute to these scores because there is no interior node.
     *
     * @param inputContainer
     * @param outputAttrName
     * @param isUndirected
     */
    public static void computeBetweennessCentrality(Container inputContainer,
                                                    String outputAttrName, boolean isUndirected) {
        DB.beginScope();

        // Apply filter
        NST linkNST;
        if (inputContainer == null) {
            linkNST = DB.getLinkNST();
        } else {
            NST[] objectAndLinkFilterNSTs = inputContainer.getObjectAndLinkFilterNSTs();
            linkNST = DB.getLinkNST().intersect(objectAndLinkFilterNSTs[1], "link_id = id");
        }

        // create the attribute
        NST bcNST = calculateBetweennessNST(linkNST, isUndirected);
        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.defineAttributeWithData(outputAttrName, "int", bcNST);

        // done
        DB.endScope();
    }

    /**
     * Calculates the clustering coefficient for objects either in a container or
     * in the entire db. Notes:
     * <p/>
     * <UL>
     * <LI>does not save values for objects with fewer than 2 neighbors</LI>
     * <LI>treats links as undirected</LI>
     * </UL>
     *
     * @param inputContainer input container. pass null to run on entire database
     * @param outputAttrName name of object attribute to save coefficients into
     */
    public static void computeClusteringCoefficient(Container inputContainer,
                                                    String outputAttrName) {

        DB.beginScope();

        // Apply filter
        NST linkNST;
        if (inputContainer == null) {
            linkNST = DB.getLinkNST();
        } else {
            NST[] objectAndLinkFilterNSTs = inputContainer.getObjectAndLinkFilterNSTs();
            linkNST = DB.getLinkNST().intersect(objectAndLinkFilterNSTs[1], "link_id = id");
        }

        // create the attribute
        NST ccNST = calculateClusteringCoefficientNST(linkNST);
        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.defineAttributeWithData(outputAttrName, "dbl", ccNST);

        // done
        DB.endScope();
    }

    public static Container computeConnectedComponents(Container inputContainer,
                                                       String outputContainerName) {

        DB.beginScope();

        Container container = DB.getRootContainer();
        Assert.condition(!container.hasChild(outputContainerName), "container already exists: '" +
                outputContainerName + "'");

        NST objectNST = DB.getObjectNST();
        NST linkNST = DB.getLinkNST();
        if (inputContainer != null) {
            NST[] objectAndLinkFilterNSTs = inputContainer.getObjectAndLinkFilterNSTs();
            objectNST = objectNST.intersect(objectAndLinkFilterNSTs[0], "id EQ id");
            linkNST = linkNST.intersect(objectAndLinkFilterNSTs[1], "link_id EQ id");
        }

        // get groups of connected objects
        NST ccObjectNST;
        try {
            ccObjectNST = calculateConnectedComponentsNST(objectNST, linkNST);
        } catch (IllegalArgumentException e) {
            DB.endScope();
            throw e;
        }
        // get groups for links based on groups of objects (using link table)
        NST ccLinkNST = linkNST.join(ccObjectNST, "o1_id EQ id");

        // create a container
        NST subgObjectNST = ccObjectNST.renameColumn("id", "item_id").renameColumn("group", "subg_id");
        NST subgLinkNST = ccLinkNST.project("link_id, group").renameColumn("link_id", "item_id").renameColumn("group", "subg_id");
        subgObjectNST.addConstantColumn("name", "str", "O");
        subgLinkNST.addConstantColumn("name", "str", "L");
        Container connectedContainer = DB.getRootContainer().createChildFromTempSGINSTs(outputContainerName, subgObjectNST, subgLinkNST);
        DB.commit();    // persist container

        // done
        DB.endScope();

        return connectedContainer;
    }

    public static void computeHubsAndAuthorities(Container inputContainer,
                                                 int numIterations,
                                                 String hubAttrName,
                                                 String authAttrName) {
        DB.beginScope();

        NST objectNST = DB.getObjectNST();
        NST linkNST = DB.getLinkNST();
        if (inputContainer != null) {
            NST[] objectAndLinkFilterNSTs = inputContainer.getObjectAndLinkFilterNSTs();
            objectNST = objectNST.intersect(objectAndLinkFilterNSTs[0], "id EQ id");
            linkNST = linkNST.intersect(objectAndLinkFilterNSTs[1], "link_id EQ id");
        }

        NST[] hubAndAuthNSTs = calculateHubsAndAuthoritiesNSTs(objectNST, linkNST, numIterations);
        NST hubNST = hubAndAuthNSTs[0];
        NST authNST = hubAndAuthNSTs[1];

        // create the attribute
        Attributes objectAttrs = DB.getObjectAttrs();
        objectAttrs.defineAttributeWithData(hubAttrName, "dbl", hubNST);
        objectAttrs.defineAttributeWithData(authAttrName, "dbl", authNST);

        DB.endScope();

    }

    private static Set getNeighbors(NST linkNST, Integer o1ID) {
        ResultSet rs = linkNST.selectRows("o1_id EQ " + o1ID.intValue(), "o2_id", "*");
        Set o2IDs = new HashSet();
        while (rs.next()) {
            o2IDs.add(new Integer(rs.getOID(1)));
        }
        return o2IDs;
    }

    /**
     * Normalizes an NST, so that the sum of its second column is equal to one
     * <p/>
     * Caller releases
     *
     * @param inputNST
     * @return
     */
    protected static NST normalizeNST(NST inputNST) {
        String baseName = inputNST.getNSTColumn(0).getName();
        String colName = inputNST.getNSTColumn(1).getName();
        inputNST.addArithmeticColumn(colName + " * " + colName, null, "squareCol");
        double sum = inputNST.sum("squareCol");

        // make sure that the sum doesn't come in the E notation, because addArithmeticColumn will interpret
        // it as a column name!
        DecimalFormat dec = new DecimalFormat("##############.############");
        String formattedsum = dec.format(sum);

        inputNST.addArithmeticColumn("squareCol / " + formattedsum, "dbl", "normal");
        NST retNST = inputNST.project(baseName + ", normal");
        retNST.renameColumn("normal", colName);
        return retNST;
    }
}
