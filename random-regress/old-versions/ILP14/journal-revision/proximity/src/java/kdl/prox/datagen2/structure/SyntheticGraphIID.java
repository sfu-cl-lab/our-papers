/**
 * $Id: SyntheticGraphIID.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.datagen2.structure;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import kdl.prox.util.stat.Distribution;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Creates a random graph, with nodes with a degree specified in degreeDistribs or objectSpec
 * There are two types of nodes, S and T. Saves their type in objectType attribute (and
 * a linkType for the connections between them.)
 * <p/>
 * NOTE: It assumes the database is empty, but initialized!
 */
public class SyntheticGraphIID {
    private static Logger log = Logger.getLogger(SyntheticGraphIID.class);

    private static final String OBJ_TYPE_ATTR_NAME = "objectType";
    private static final String LINK_TYPE_ATTR_NAME = "linkType";
    private static final String CORE_OBJ_NAME = "S";

    private static final String PERIPH_OBJ_NAME = "T";

    private static final double DELTA = 0.00001;    // used by verifyDegreeDistribsSums() to decide how close to 1.0 the sums must be

    /**
     * Generate generate S-T object structure with varying degrees
     * of T objects for each S object.
     *
     * @param numCoreObjs    the number of S objects to be created
     * @param degreeDistribs probability distribution over degree distributions
     *                       for the core objects. it's an array of arrays where
     *                       the inner arrays are pairs containing a Double (the
     *                       probability for the following distribution) and the
     *                       Distribution the probability appplies to.
     * @throws IllegalArgumentException if types in degreeDistribs don't match
     *                                  the {Double, Distribution} pattern, or
     *                                  if probabilities in degreeDistribs don't
     *                                  sum to 1.0
     */
    public SyntheticGraphIID(int numCoreObjs, Object[][] degreeDistribs) {
        Assert.condition(numCoreObjs >= 0, "numCoreObjs wasn't >= 0: " + numCoreObjs);
        Assert.notNull(degreeDistribs, "null degreeDistribs");
        Assert.condition(degreeDistribs.length != 0, "empty degreeDistribs");
        verifyDegDistTypes(degreeDistribs);
        verifyDegDistProbSums(degreeDistribs);

        Integer[][] objectSpec = new Integer[numCoreObjs][2];
        for (int coreObjNum = 0; coreObjNum < numCoreObjs; coreObjNum++) {
            objectSpec[coreObjNum][0] = 1;
            objectSpec[coreObjNum][1] = getDegreeFromDist(degreeDistribs);
            ;
        }
        createIIDStructure(objectSpec);
    }

    /**
     * Generate a fixed structure based on the number of core objects presented.
     * This method will generate S-T structure with a fixed number of Ts for each S.
     * For example, an object spec that contains
     * <p/>
     * 2 4
     * 3 2
     * <p/>
     * will create 2 S objects connected to 4 T objects,
     * add         3 S objects connected to 2 T objects.
     *
     * @param objectSpec specifies the number of S's and for each S the number of T's
     */
    public SyntheticGraphIID(Integer[][] objectSpec) {
        createIIDStructure(objectSpec);
    }


    public static String getObjTypeAttrName() {
        return OBJ_TYPE_ATTR_NAME;
    }

    public static String getLinkTypeAttrName() {
        return LINK_TYPE_ATTR_NAME;
    }

    public static String getCoreObjName() {
        return CORE_OBJ_NAME;
    }

    public static String getPeriphObjName() {
        return PERIPH_OBJ_NAME;
    }


    /**
     * Do the actual work: for each row of objectSpec
     * - Create n core objects
     * - Create m peripheral objects
     * - Set their types
     * - Create links between them
     * - Save the link type
     *
     * @param objectSpec
     */
    private void createIIDStructure(Integer[][] objectSpec) {
        Assert.notNull(objectSpec, "Object spec can't be null");

        // for each pair in objectSpec create a core object S
        DB.getObjectAttrs().defineAttribute(OBJ_TYPE_ATTR_NAME, "str");
        DB.getLinkAttrs().defineAttribute(LINK_TYPE_ATTR_NAME, "str");
        NST objAttrDataNST = DB.getObjectAttrs().getAttrDataNST(OBJ_TYPE_ATTR_NAME);
        NST linkAttrDataNST = DB.getLinkAttrs().getAttrDataNST(LINK_TYPE_ATTR_NAME);

        for (Object[] coreObject : objectSpec) {
            int numCoreObjects = (Integer) coreObject[0];
            int numTObjects = (Integer) coreObject[1];

            for (int coreObjNum = 0; coreObjNum < numCoreObjects; coreObjNum++) {
                int newSObjID = DB.insertObject();
                objAttrDataNST.insertRow(newSObjID + "," + CORE_OBJ_NAME);

                // insert fixed number of 'T' objects linked to 'S' object
                for (int linkNum = 0; linkNum < numTObjects; linkNum++) {
                    int newTObjID = DB.insertObject();
                    objAttrDataNST.insertRow(newTObjID + "," + PERIPH_OBJ_NAME);

                    int newLinkID = DB.insertLink(newSObjID, newTObjID);
                    linkAttrDataNST.insertRow(newLinkID + "," + CORE_OBJ_NAME + "-" + PERIPH_OBJ_NAME);
                }
            }

        }

        objAttrDataNST.release();
    }


    /**
     * @param degreeDistribs is an array of probs. on degree
     * @return a random degree from degreeDistribs. works by randomly selecting
     *         one of the distributions based on its probability, then calling
     *         getRandomNumber() on its Distribution
     */
    private int getDegreeFromDist(Object[][] degreeDistribs) {
        double randVal = Math.random();
        return getDegreeFromDist(degreeDistribs, randVal);
    }

    /**
     * Overload used for testing, takes randVal as parameter.
     *
     * @param degreeDistribs is an array of probs-distribution pairs
     *                       probs will tell you from which distribution to choose
     *                       distribution is used to sample a degree
     * @param randVal        as returned by Math.random()
     * @return a long representing the degree
     */
    private int getDegreeFromDist(Object[][] degreeDistribs, double randVal) {
        double threshold = 0.0;     // current upper value of range being tested

        for (Object[] probDistPair : degreeDistribs) {
            double prob = (Double) probDistPair[0];
            threshold = threshold + prob;
            Distribution dist = (Distribution) probDistPair[1];
            if (randVal <= threshold) { // in range for dist
                float randomNumber = new Float(dist.getRandomNumber());
                return Math.round(randomNumber);
            }
        }
        throw new RuntimeException("failed to pick a distribution for randVal: " + randVal);
    }


    /**
     * @param degreeDistribs is an array of probs. on degree
     * @throws IllegalArgumentException if probabilities in degreeDistribs don't
     *                                  sum to 1.0
     */
    private void verifyDegDistProbSums(Object[][] degreeDistribs) {
        double sum = 0.0;
        for (Object[] probDistPair : degreeDistribs)
            sum += (Double) probDistPair[0];

        Assert.condition(Math.abs(sum - 1.0) <= DELTA, "sum was not within " + DELTA + " of 1.0: " + sum);
    }

    /**
     * @param degreeDistribs is an array of probs. on degree
     * @throws IllegalArgumentException if types in degreeDistribs don't match
     *                                  the {Double, Distribution} pattern
     */
    private void verifyDegDistTypes(Object[][] degreeDistribs) {
        for (Object[] probDistPair : degreeDistribs) {
            Assert.condition(probDistPair.length == 2, "probDistPair didn't " +
                    "contain exactly 2 elements: " + Arrays.asList(probDistPair));
            Assert.condition(probDistPair[0] instanceof Double, "first " +
                    "element (probability) wasn't a Double: " + probDistPair[0]);
            Assert.condition(probDistPair[1] instanceof Distribution, "second " +
                    "element (probability) wasn't a Distribution: " + probDistPair[1]);
        }
    }
}
