/**
 * $Id: RDNCanvasTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.net.URL;
import java.util.*;


public class RDNCanvasTest extends TestCase {

    private static Logger log = Logger.getLogger(RDNCanvasTest.class);


    protected void setUp() throws Exception {
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        super.setUp();
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @param arcs
     * @param arcStr
     * @return first Arc in arcs whose toString() equals arcStr. returns null if none found
     */
    private Arc getArcForToString(List arcs, String arcStr) {
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            if (arc.toString().equals(arcStr)) {
                return arc;
            }
        }
        return null;
    }

    /**
     * @param rezClass Class used for getting fileName resource
     * @param fileName
     * @return JDOM Document for the XML file passed in fileName, relative to rezClass
     * @throws Exception
     */
    public static Document getFileDocument(Class rezClass, String fileName)
            throws Exception {
        URL testXMLFileURL = rezClass.getResource(fileName);
        Assert.condition(testXMLFileURL != null, "couldn't find file relative " +
                "to class: " + fileName + ", " + rezClass);

        File queryFile = new File(testXMLFileURL.getFile());
        return new SAXBuilder(true).build(queryFile);
    }

    /**
     * RPTs (including class labels and features):
     * o t1-actor-gender.xml  : actor.gender <- {}
     * o t1-actor-hasAward.xml: actor.hasAward <- {actor.gender}
     * <p/>
     * Map: {}
     * Desired diagram:
     * <pre>
     * +-------------+
     * |* Actor      |
     * |             |
     * |    ,---.    |
     * |   /     \   |
     * |  (gender )  |
     * |   \     /   |
     * |    `-+-'    |
     * |      |      |
     * |      |      |
     * |      V      |
     * |    ,+--.    |
     * |   / has \   |
     * |  ( Award )  |
     * |   \     /   |
     * |    `---'    |
     * +-------------+
     * </pre>
     *
     * @throws Exception
     */
    public static RDNInput getRDNInput1() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t1-actor-gender.xml"),
                getFileDocument(RDNCavasTestApp.class, "t1-actor-hasAward.xml"),
        };
        Map nameMap = Collections.EMPTY_MAP;
        RDNInput rdnInput = new RDNInput("t1-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t2-actor-hasAward.xml: actor.hasAward <- {movie_actor.hasAward}
     * <p/>
     * Map: {actor: {movie_actor}}
     * Desired diagram:
     * <pre>
     * +-------------+
     * |* Actor      |
     * |             |
     * |    ,+--.    |
     * |   / has \   |
     * |  ( Award )  |
     * |   \     /   |
     * |    i---'    |
     * |   /     ^   |
     * +---+-----|---+
     *      `..,'
     * </pre>
     *
     * @throws Exception
     */
    public static RDNInput getRDNInput2() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t2-actor-hasAward.xml"),
        };
        Map nameMap = Collections.singletonMap("actor",
                Collections.singletonList("movie_actor"));
        RDNInput rdnInput = new RDNInput("t2-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t3-actor-gender.xml: actor.gender <- {}
     * o t3-movie-genre.xml : movie.genre <- {actor.degree*}
     * <p/>
     * Map: {}
     * Desired diagram:
     * <pre>
     * +-------------+    +-------------+
     * |* Movie      |   ,+* Actor      |
     * |             |  / |             |
     * |    ,---.    | /  |    ,---.    |
     * |   /     \   |/   |   /     \   |
     * |  ( genre )<-+    |  (gender )  |
     * |   \     /   |    |   \     /   |
     * |    `---'    |    |    `---'    |
     * +-------------+    +-------------+
     * </pre>
     */
    public static RDNInput getRDNInput3() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t3-actor-gender.xml"),
                getFileDocument(RDNCavasTestApp.class, "t3-movie-genre.xml"),
        };
        Map nameMap = Collections.EMPTY_MAP;
        RDNInput rdnInput = new RDNInput("t3-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t4-actor-gender.xml: actor.gender <- {}
     * o t4-movie-genre.xml : movie.genre <- {actor.gender}
     * <p/>
     * Map: {}
     * Desired diagram:
     * <pre>
     * +-------------+    +-------------+
     * |* Movie      |    |* Actor      |
     * |             |    |             |
     * |    ,---.    |    |    ,---.    |
     * |   /     \   |    |   /     \   |
     * |  ( genre )<-+----+--(gender )  |
     * |   \     /   |    |   \     /   |
     * |    `---'    |    |    `---'    |
     * +-------------+    +-------------+
     * </pre>
     */
    public static RDNInput getRDNInput4() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t4-actor-gender.xml"),
                getFileDocument(RDNCavasTestApp.class, "t4-movie-genre.xml"),
        };
        Map nameMap = Collections.EMPTY_MAP;
        RDNInput rdnInput = new RDNInput("t4-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t5-actor-gender.xml         : actor.gender <- {}
     * o t5-actor-hasAward.xml       : actor.hasAward <- {movie.genre}
     * o t5-movie-genre.xml          : movie.genre <- {actor.degree*, actor.hasAward, movie.isOWBlockbuster}
     * o t5-movie-isOWBlockbuster.xml: movie.isOWBlockbuster <- {actor_movie.isOWBlockbuster}
     * <p/>
     * Map: {movie: {actor_movie}}
     * Desired diagram:
     * <pre>
     *      _,...
     *    ,'     `.
     *   .'        \
     * +-+----------++    +-------------+
     * |*\Movie    / |    |* Actor      |
     * |  `.      ,' |    +             |
     * |    ,---.V   |   /|    ,---.    |
     * |   /     \   |  / |   /     \   |
     * |  ( ow(1) )  | /  |  (gender )  |
     * |   \     /   |/   |   \     /   |
     * |    `-+-'    +    |    `-+-'    |
     * |      |     /|    |             |
     * |      |    / |    |             |
     * |      V   /  |    |             |
     * |    ,---.V   |    |    ,---.    |
     * |   /     \   |    |   / has \   |
     * |  ( genre )<-+----+->( Award )  |
     * |   \     /   |    |   \     /   |
     * |    `---'    |    |    `---'    |
     * +-------------+    +-------------+
     * <p/>
     * 1 - really 'isOWBlockbuster'
     * <p/>
     * </pre>
     */
    public static RDNInput getRDNInput5() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t5-actor-gender.xml"),
                getFileDocument(RDNCavasTestApp.class, "t5-actor-hasAward.xml"),
                getFileDocument(RDNCavasTestApp.class, "t5-movie-genre.xml"),
                getFileDocument(RDNCavasTestApp.class, "t5-movie-isOWBlockbuster.xml"),
        };
        Map nameMap = Collections.singletonMap("movie",
                Collections.singletonList("actor_movie"));
        RDNInput rdnInput = new RDNInput("t5-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t6-actor-gender.xml: actorIn.salary <- {}  -- NB: link attribute
     * o t6-movie-genre.xml : movie.genre <- {actorIn.salary}
     * <p/>
     * Map: {}
     * Desired diagram:
     * <pre>
     * +-------------+    /-------------\
     * |* movie      |    |* actorIn    |
     * |             |    |             |
     * |    ,---.    |    |    ,---.    |
     * |   /     \   |    |   /     \   |
     * |  ( genre )<-+----+--(salary )  |
     * |   \     /   |    |   \     /   |
     * |    `---'    |    |    `---'    |
     * +-------------+    \-------------/
     * </pre>
     */
    public static RDNInput getRDNInput6() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t6-actor-gender.xml"),
                getFileDocument(RDNCavasTestApp.class, "t6-movie-genre.xml"),
        };
        Map nameMap = Collections.EMPTY_MAP;
        RDNInput rdnInput = new RDNInput("t6-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t7-book_role.xml: journal_book.book_role <- {journal_book.book_role (x4)}
     * <p/>
     * Map: {}
     * Desired diagram:
     * <pre>
     * +---------------+
     * |* journal_book |
     * |               |
     * |    ,+--.      |
     * |   / book\     |
     * |  ( _role )    |
     * |   \     /     |
     * |    i---'      |
     * |   /     ^     |
     * +---+-----|-----+
     *      `..,'
     * </pre>
     */
    public static RDNInput getRDNInput7() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t7-book_role.xml"),
        };
        Map nameMap = Collections.EMPTY_MAP;
        RDNInput rdnInput = new RDNInput("t7-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t8-actor-hasAward.xml: actor.hasAward (o) <- {movie_actor.birth_year}
     * <p/>
     * Map: actor -> {movie_actor}
     * Desired diagram:
     * <pre>
     * +-------------+
     * |* Actor      |
     * |             |
     * |    ,---.    |
     * |   /     \   |
     * |  (birthYr)--|---
     * |   \     /   |   |
     * |    `-+-'    |   |
     * |             |   |
     * |             |   |
     * |             |   |
     * |    ,+--.    |   |
     * |   / has \   |   |
     * |  ( Award )<-|---
     * |   \     /   |
     * |    `---'    |
     * +-------------+
     * </pre>
     */
    public static RDNInput getRDNInput8() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t8-actor-hasAward.xml"),
        };
        Map nameMap = Collections.singletonMap("actor",
                Collections.singletonList("movie_actor"));
        RDNInput rdnInput = new RDNInput("t8-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t9-actor-hasAward.xml: actor.hasAward (o) <- {movie_actor*}
     * <p/>
     * Map: actor -> {movie_actor}
     * Desired diagram:
     * <pre>
     *     +-------------+
     *  +--+-* Actor     |
     *  |  |             |
     *  |  |    ,+--.    |
     *  |  |   / has \   |
     *  +--+->( Award )  |
     *     |   \     /   |
     *     |    '---'    |
     *     |             |
     *     +-------------+
     * </pre>
     */
    public static RDNInput getRDNInput9() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t9-actor-hasAward.xml"),
        };
        Map nameMap = Collections.singletonMap("actor",
                Collections.singletonList("movie_actor"));
        RDNInput rdnInput = new RDNInput("t9-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * RPTs (including class labels and features):
     * o t11-ProxWebKB_RPT.xml: core_page.label (o) <- {linked_to_page.page_num_inlinks, linked_from_page*, linked_from_page.page_num_outlinks (x4)}
     * <p/>
     * RPT in new format
     * Map: core_page -> {linked_to_page, linked_from_page}
     * Desired diagram:
     * <pre>
     *            +------------+
     *            |            |
     *     +------+------------+-----+
     *  +--+* core|page        |     |
     *  |  |      v            |     |
     *  |  |    ,---.        ,-+-.   |
     *  |  |   /     \      /page_\  |
     *  +--+->( type  )    (num_in ) |
     *  |  |   \     /      \links/  |
     *  |  |    `-+-' <--    `---'   |
     *  |  |             |           |
     *  |  |    ,---.    |   ,-+-.   |
     *  |  |   /page_\   |  /url _\  |
     *  +--+--(num_out)  --(hierac ) |
     *     |   \links/      \ hy  /  |
     *     |    `-+-'        `---'   |
     *     +-------------------------+
     * </pre>
     *
     * @return
     */
    public static RDNInput getRDNInput11() throws Exception {
        Document[] rptEles = new Document[]{
                getFileDocument(RDNCavasTestApp.class, "t11-ProxWebKB_RPT.xml"),
        };
        Map nameMap = new HashMap();
        List nameList = new ArrayList();
        nameList.add("linked_to_page");
        nameList.add("linked_from_page");
        nameMap.put("core_page", nameList);
        RDNInput rdnInput = new RDNInput("t11-rdn.xml", rptEles, nameMap);
        return rdnInput;
    }

    /**
     * @param typeAndVar as passed to verifyRDNCanvas(). ex: "actor:gender,hasAward"
     * @return List of the type in typeAndVar, followed by zero or more variables
     *         in typeAndVar
     */
    private List getTypeAndVars(String typeAndVar) {
        List types = new ArrayList();

        // get type
        int firstColonIdx = typeAndVar.indexOf(':');
        if (firstColonIdx == -1) {
            types.add(typeAndVar);
        } else {
            types.add(typeAndVar.substring(0, firstColonIdx));
        }

        // get variables, if any
        if (firstColonIdx != -1) {
            String vars = typeAndVar.substring(firstColonIdx + 1);  // assumes something after ':'
            String[] varStrings = vars.split(",");
            types.addAll(Arrays.asList(varStrings));
        }

        return types;
    }

    /**
     * @param typesAndVars as passed to verifyRDNCanvas()
     * @return List of types (Strings) in typesAndVars
     */
    private List getTypesFromTypesAndVars(String[] typesAndVars) {
        List types = new ArrayList();
        for (int typeIdx = 0; typeIdx < typesAndVars.length; typeIdx++) {
            String typeAndVars = typesAndVars[typeIdx];         // ex: "actor:gender,hasAward"
            List typeAndVarsList = getTypeAndVars(typeAndVars); // ex: ["actor", "gender", "hasAward"]
            types.add(typeAndVarsList.get(0));
        }
        return types;
    }

    /**
     * @param arcStr as passed to verifyRDNCanvas(), except allows an optional
     *               single asterisk ('*') at the end to indicate that the arc is
     *               isMapped. ex: "actor->movie.genre", "core_page->core_page.label*",
     *               "actor.gender->actor.hasAward", "actor.hasAward<>movie.genre"
     * @return new Arc from arcStr
     * @see RDNCanvasTest#verifyRDNCanvas
     */
    private Arc makeArcFromToString(String arcStr) {
        boolean isBidirectional = false;
        String[] fromToStrs = arcStr.split("->");
        if (fromToStrs.length == 1) {
            fromToStrs = arcStr.split("<>");
            isBidirectional = true;
        }
        assertTrue(fromToStrs.length != 0);

        String fromStr = fromToStrs[0];
        String toStr = fromToStrs[1];
        String[] fromStrings = fromStr.split("\\.");    // ex: ["actor"], ["actor", "gender"], ["actor", "hasAward"]
        String[] toStrings = toStr.split("\\.");        // ex: ["movie", "genre"], ["actor", "hasAward"], ["movie", "genre"]
        String type1 = fromStrings[0];
        String var1 = (fromStrings.length == 1 ? null : fromStrings[1]);
        String type2 = toStrings[0];
        String var2 = toStrings[1];

        // extract final asterisk, if necessary
        boolean isMapped = false;
        if (var2.charAt(var2.length() - 1) == '*') {
            var2 = var2.substring(0, var2.length() - 1);
            isMapped = true;
        }

        Arc arc = new Arc(type1, var1, type2, var2, isMapped);
        arc.setBiDirectional(isBidirectional);
        return arc;
    }

    /**
     * Converts a List of Objects into a List of Strings by calling toString()
     * on input List.
     *
     * @param list input List
     * @return List of Strings, each corresponding to toString() called on input
     */
    private List makeToStringList(List list) {
        ArrayList toStrings = new ArrayList();
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            toStrings.add(object.toString());
        }
        return toStrings;
    }

    public void testRDN1() throws Exception {
        RDNInput rdnInput = getRDNInput1();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"actor:gender,hasAward",
                },
                new String[]{"actor.gender->actor.hasAward",
                });
    }

    public void testRDN2() throws Exception {
        RDNInput rdnInput = getRDNInput2();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"actor:hasAward",
                },
                new String[]{"actor.hasAward->actor.hasAward*", // isMapped
                });
    }

    public void testRDN3() throws Exception {
        RDNInput rdnInput = getRDNInput3();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"movie:genre",
                        "actor:gender",
                },
                new String[]{"actor->movie.genre",
                });
    }

    public void testRDN4() throws Exception {
        RDNInput rdnInput = getRDNInput4();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"movie:genre",
                        "actor:gender",
                },
                new String[]{"actor.gender->movie.genre",
                });
    }

    public void testRDN5() throws Exception {
        RDNInput rdnInput = getRDNInput5();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"movie:isOWBlockbuster,genre",
                        "actor:gender,hasAward",
                },
                new String[]{"movie.isOWBlockbuster->movie.isOWBlockbuster*", // self-loop, isMapped
                        "movie.isOWBlockbuster->movie.genre",
                        "actor->movie.genre",
                        "movie.genre<>actor.hasAward", // bi-directional
                });
    }

    public void testRDN6() throws Exception {
        RDNInput rdnInput = getRDNInput6();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"movie:genre",
                        "actorIn:salary", // NB: rare link item type!
                },
                new String[]{"actorIn.salary->movie.genre",
                });
    }

    public void testRDN7() throws Exception {
        RDNInput rdnInput = getRDNInput7();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"journal_book:book_role",
                },
                new String[]{"journal_book.book_role->journal_book.book_role",
                });
    }

    public void testRDN8() throws Exception {
        RDNInput rdnInput = getRDNInput8();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"actor:hasAward,birth_year",
                },
                new String[]{"actor.birth_year->actor.hasAward*", // isMapped
                });
    }

    public void testRDN9() throws Exception {
        RDNInput rdnInput = getRDNInput9();
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        verifyRDNCanvas(rdnCanvas,
                new String[]{"actor:hasAward",
                },
                new String[]{"actor->actor.hasAward*", // isMapped
                });
    }

    private void verifyArc(RDNCanvas rdnCanvas, Arc expArc, Arc actArc) {
        assertEquals(expArc.getType1(), actArc.getType1());
        assertEquals(expArc.getVar1(), actArc.getVar1());
        assertEquals(expArc.getType2(), actArc.getType2());
        assertEquals(expArc.getVar2(), actArc.getVar2());
        assertEquals(expArc.isBiDirectional(), actArc.isBiDirectional());
        assertEquals(expArc.isMapped(), actArc.isMapped());
        assertEquals(expArc, actArc);

        PPath arcLine = rdnCanvas.getLineForArc(actArc);
        assertNotNull(arcLine);

        PPath arcArrowHead1 = rdnCanvas.getArrowHead1ForArc(actArc);
        PPath arcArrowHead2 = rdnCanvas.getArrowHead2ForArc(actArc);
        PNode actCircle1 = rdnCanvas.getCircle1ForArc(actArc);
        PNode actCircle2 = rdnCanvas.getCircle2ForArc(actArc);
        assertNotNull(arcArrowHead1);
        if (actArc.isBiDirectional()) {
            assertNotNull(arcArrowHead2);
        } else {
            assertNull(arcArrowHead2);
        }
        assertNotNull(actCircle1);
        assertNotNull(actCircle2);
        assertEquals(rdnCanvas.getLayer(), arcLine.getParent());
        assertEquals(arcLine, arcArrowHead1.getParent());

        PNode expCircle1 = (actArc.isFromDegree() ?
                rdnCanvas.getDotForType(expArc.getType1()) :
                rdnCanvas.getCircleForVar(expArc.getType1(), actArc.getVar1()));
        PNode expCircle2 = rdnCanvas.getCircleForVar(expArc.getType2(), actArc.getVar2());
        assertEquals(expCircle1, actCircle1);
        assertEquals(expCircle2, actCircle2);

        // NB: following test (isArcPassesOutsideRect()) fails if verifyRDNCanvas()
        // doesn't call forceRectBoundsChanges():
        boolean arcPassesOutsideRect = rdnCanvas.isArcPassesOutsideRect(actArc);
        assertEquals(actArc.isMapped() || actArc.isSelfLoop(), arcPassesOutsideRect);
    }

    /**
     * Verifies that rdnCanvas contains the specified types, variables, and arcs,
     * and their corresponding PNodes.
     *
     * @param rdnCanvas
     * @param typesAndVars array of Strings, one for each item type (rectangle).
     *                     format of each String:
     *                     "&lt;item-type&gt;':'&lt;var1&gt;[','&lt;varN&gt;]*".
     * @param arcs         array of Strings, one for each arc (line). format is
     *                     the same as Arc.toString()
     * @see Arc#toString
     */
    private void verifyRDNCanvas(RDNCanvas rdnCanvas, String[] typesAndVars,
                                 String[] arcs) {
        rdnCanvas.forceRectBoundsChanges();     // otherwise test in verifyArc() fails (see docs near isArcPassesOutsideRect() call)

        // check list of types
        List expTypes = getTypesFromTypesAndVars(typesAndVars);
        List actTypes = rdnCanvas.getTypes();
        assertEquals(expTypes.size(), actTypes.size());
        assertTrue(expTypes.containsAll(actTypes));

        // check each type and its variables
        for (int expTypesAndVarsIdx = 0; expTypesAndVarsIdx < typesAndVars.length;
             expTypesAndVarsIdx++) {
            String expTypeAndVars = typesAndVars[expTypesAndVarsIdx];
            List expTypeAndVarsList = getTypeAndVars(expTypeAndVars);   // ex: ["actor", "gender", "hasAward"]
            String type = (String) expTypeAndVarsList.remove(0);
            List expVars = expTypeAndVarsList;

            verifyTypeVarNames(rdnCanvas, type, expVars);
            verifyTypePNodes(rdnCanvas, type);

            for (Iterator expVarIter = expVars.iterator(); expVarIter.hasNext();) {
                String expVar = (String) expVarIter.next();
                verifyVarPNodes(rdnCanvas, type, expVar);
            }
        }

        // check list of arcs
        List expArcStrs = Arrays.asList(arcs);          // Arc.toString() Strings
        List actArcs = rdnCanvas.getArcs();             // Arcs
        List actArcStrs = makeToStringList(actArcs);    // Arc.toString() Strings
        assertEquals(expArcStrs.size(), actArcStrs.size());
        assertTrue(expArcStrs.containsAll(actArcStrs));

        // check each arc
        for (int expArcIdx = 0; expArcIdx < arcs.length; expArcIdx++) {
            String expArcStr = arcs[expArcIdx];     // ex: "actor->movie.genre", "actor.gender->actor.hasAward", "actor.hasAward<>movie.genre"
            Arc expArc = makeArcFromToString(expArcStr);
            Arc actArc = getArcForToString(actArcs, expArcStr);
            assertNotNull(expArc);
            assertNotNull(actArc);
            assertEquals(expArcStr, expArc.toString());
            assertEquals(expArc.toString(), actArc.toString());
            verifyArc(rdnCanvas, expArc, actArc);
        }
    }

    private void verifyTypePNodes(RDNCanvas rdnCanvas, String type) {
        PNode rect = rdnCanvas.getRectForType(type);
        PText label = rdnCanvas.getLabel(rect);
        PNode dot = rdnCanvas.getDotForType(type);
        assertNotNull(rect);
        assertNotNull(label);
        assertNotNull(dot);
        assertEquals(type, label.getText());
        assertEquals(rdnCanvas.getLayer(), rect.getParent());
        assertEquals(rect, label.getParent());
        assertEquals(rect, dot.getParent());
    }

    private void verifyTypeVarNames(RDNCanvas rdnCanvas, String type, List expVars) {
        List actVars = rdnCanvas.getVariables(type);
        assertEquals(expVars.size(), actVars.size());
        assertTrue(expVars.containsAll(actVars));
    }

    private void verifyVarPNodes(RDNCanvas rdnCanvas, String type, String expVar) {
        PNode varCircle = rdnCanvas.getCircleForVar(type, expVar);
        assertNotNull(varCircle);

        PText varLabel = rdnCanvas.getLabel(varCircle);
        assertNotNull(varLabel);
        assertEquals(expVar, varLabel.getText());

        PNode actorRect = rdnCanvas.getRectForType(type);
        assertEquals(actorRect, varCircle.getParent());
        assertEquals(varCircle, varLabel.getParent());
    }

}
