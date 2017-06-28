/**
 * $Id: QueryGen.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: QueryGen.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.family;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;
import org.jdom.Element;


/**
 * The actual class that generates a set of qgraph2 query (xml) files from a
 * <query-family> (xml) specification.
 * <p/>
 * Receives the root Element of an xml query-family specification, and creates a list with
 * all the possible query instances.
 * Those instances are then accessible via getQueryInstances
 */
public class QueryGen {

    /**
     * Class-based log4j category for logging.
     */
    private static Logger log = Logger.getLogger(QueryGen.class);


    /**
     * The <query-family> specification. Set by the constructor
     */
    private Element familyEle = null;


    /**
     * List of edges and vertices. Entries key-ed by name in the <query-family> specification
     * Set by loadandValidateEdges() and loadAndValidateVertices()
     */
    private HashMap Edges = new HashMap();
    private AbstractMap Vertices = new HashMap();


    /**
     * The list of all query instances.
     * Keyed by the name of  the query instance. Names describe the operations.
     * Set by generateQueryInstances()
     */
    private AbstractMap queryInstances = new HashMap();


    /**
     * Full-arg constructor.
     * <p/>
     * Builds tree of query instances from XML specification
     *
     * @param queryFamilyEle: root element of the xml specification for the family
     */
    public QueryGen(Element queryFamilyEle) throws Exception {
        Assert.notNull(queryFamilyEle, "queryFamilyEle null");
        familyEle = queryFamilyEle;

        // Get the list of vertices and edges
        try {
            loadAndValidateSpecification();
        } catch (IllegalArgumentException e) {
            log.fatal("QueryGen.processAndValidateSpecification(): " + e.getMessage());
            throw e;
        }
        generateQueryInstances();

    }


    /**
     * Generates all possible query instances for the query-family specification
     * Uses the Edges and Vertices hash maps. Assumes that they have been loaded
     * via loadAndValidateSpecification(). The result is another hashmap, where
     * each element is a JDOM Element with the <query-body> part of a qGraph
     * query. Entries in the hashmap are key-ed by the name of the query, which
     * is formed by concatenating all the operations that are performed to an
     * empty query in order to create a particular instance. For example, if the
     * query-family is a  vertex with annot-test set to A1 and another vertex
     * without annotation, such as
     * <p/>
     * <vertex name="A" annot-test="AN">
     * <vertex name="B">
     * <p/>
     * then the resulting query names are
     * <p/>
     * _A1inf_B
     * _A11_B
     * _A12_B
     * <p/>
     * NB: traverses the edge and vertex list in a deterministic way, so that
     * for the same input the same output is generated
     * <p/>
     * Inner Workings:
     * The algorithm uses two hash maps, currentMap and pastMap, that store
     * successive iterations of the algorithm. currentMap starts with a single
     * element, keyed with the empty string, that contains a JDOM Element for
     * the <query-body> element of the query. The algorithm then proceeds to
     * iterate through all the edges and vertices in the <query-family> and,
     * for each one expans the pastMap to add the new possible query elements
     * (and their variations):
     * <p/>
     * foreach edge,vertex
     * set pastMap = currentMap
     * set currentMap = new empty map
     * foreach element o in pastMap
     * foreach possible variation of the edge/vertex (e.g., vary annotation, or direction)
     * set n = copy of element o
     * set comp = new query component for this element, in qGraph syntax (e.g. <vertex><condition>...</condition></vertex>)
     * add comp to element n
     * set comp_name = name of element o + "_" + operation performed
     * add n to currentMap
     * end loop
     * end loop
     * end loop
     * <p/>
     * currentMap holds the complete list of query instances.
     */
    private void generateQueryInstances() {
        AbstractMap currentMap = new HashMap();
        Element root = new Element("query-body");
        currentMap.put("", root);

        // Foreach element in the specification
        // Sort them by name, to guarantee that the traversal is deterministic
        // todo: make sure that edge and vertex names do not overlap
        HashMap allElts = new HashMap(Vertices);
        allElts.putAll(Edges);
        Iterator allEltsNameIter = new TreeSet(allElts.keySet()).iterator();
        while (allEltsNameIter.hasNext()) {
            // Get this particular element in the <query-family> spec, and find all possible derivations
            String key = (String) allEltsNameIter.next();
            Element Elt = (Element) allElts.get(key);
            String eltType = Elt.getName();
            HashMap derivations = getPossibleElementDerivations(Elt);
            // Insert each of those derivation in the map, as new children of the existing queries
            // Start with a fresh map, keep the old one
            AbstractMap pastMap = currentMap;
            currentMap = new HashMap();
            // Foreach query in the map
            Iterator pastMapIter = new ArrayList(pastMap.keySet()).iterator();
            while (pastMapIter.hasNext()) {
                String origQueryName = (String) pastMapIter.next();
                Element origQuery = (Element) pastMap.get(origQueryName);
                // Iterate through the list of derivations for this element in <query-family>
                // and add it as child of the current query in the map
                // The new query appends the name of the current derivation to the name of the old query
                Iterator derivationsIter = new ArrayList(derivations.keySet()).iterator();
                while (derivationsIter.hasNext()) {
                    String thisDerivationName = (String) derivationsIter.next();
                    String newQueryName = origQueryName + "_" + thisDerivationName;
                    Element thisDerivation = (Element) derivations.get(thisDerivationName);
                    Element newQuery = (Element) origQuery.clone();
                    newQuery.addContent((Element) thisDerivation.clone());
                    currentMap.put(newQueryName, newQuery);
                }
            }
        }
        // At this point, currentMap has the final list of query instances
        queryInstances = currentMap;

    }


    /**
     * Returns a list with all possible derivations from an element
     * <p/>
     * If the element is a vertex, the  possible derivations are
     * annot-test=AX : no annotations
     * annot-test=A1: [1..]
     * annot-test=AN: [1..], [1..1], [1..2]
     * <p/>
     * If the element is an edge, the possible derivations are
     * annot-test=AX : no annotations
     * annot-test=A1: [1..]
     * annot-test=AN: [1..], [1..1], [1..2]
     * annot-test=AA: [], [1..],[1..1],[1..2]
     * <p/>
     * and, for each of those edge annot-test,
     * <p/>
     * dir-test:DX : no direction
     * dir-test:DO : no direction, A->B
     * dir-test:DB : no direction, A->B, B->A
     *
     * @param e the element from which derivations are to be listed
     */
    private HashMap getPossibleElementDerivations(Element e) {
        String eltType = e.getName();
        if (eltType.equalsIgnoreCase("vertex")) {
            return getPossibleVertexDerivations(e);
        } else {
            return getPossibleEdgeDerivations(e);
        }
    }


    /**
     * Lists all possible derivation for an Edge e
     *
     * @param e
     * @return
     */
    private HashMap getPossibleEdgeDerivations(Element e) {
        Assert.notNull(e, "Null edge receiveed");
        HashMap annotDerivations = new HashMap();
        HashMap allDerivations = new HashMap();
        String eltName = e.getAttributeValue("name").toUpperCase();
        String v1 = e.getAttributeValue("v1").toUpperCase();
        String v2 = e.getAttributeValue("v2").toUpperCase();
        String dirTest = e.getAttributeValue("dir-test");
        String annotTest = e.getAttributeValue("annot-test");

        // Both annotations and direction can vary
        // Vary annotations first, and then, for each possible derivation, add direction
        if (annotTest.equalsIgnoreCase("AX")) {
            annotDerivations.put(eltName, makeEdgeElement(eltName, v1, v2, "false", null, null));
        } else if (annotTest.equalsIgnoreCase("A1")) {
            // one with [1..]
            annotDerivations.put(eltName + "1inf", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), null));
        } else if (annotTest.equalsIgnoreCase("AN")) {
            // [1..], [1..1], [1..2]
            annotDerivations.put(eltName + "1inf", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), null));
            annotDerivations.put(eltName + "11", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), new Integer(1)));
            annotDerivations.put(eltName + "12", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), new Integer(2)));
        } else if (annotTest.equalsIgnoreCase("AA")) {
            // []. [1..], [1..1], [1..2]
            annotDerivations.put(eltName, makeEdgeElement(eltName, v1, v2, "false", null, null));
            annotDerivations.put(eltName + "1inf", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), null));
            annotDerivations.put(eltName + "11", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), new Integer(1)));
            annotDerivations.put(eltName + "12", makeEdgeElement(eltName, v1, v2, "false", new Integer(1), new Integer(2)));
        } else {
            log.fatal("getPossibleEdgeDerivations(): Unrecognized annot-test: " + annotTest);
            throw new RuntimeException("Unrecognized annot-test: " + annotTest);
        }

        // Now for each possible derivation, add derivations for directionality of the edge
        if (dirTest.equalsIgnoreCase("DX")) {
            allDerivations.putAll(annotDerivations);
        } else if (dirTest.equalsIgnoreCase("DO") || dirTest.equalsIgnoreCase("DB")) {
            Iterator derivationsIter = new ArrayList(annotDerivations.keySet()).iterator();
            while (derivationsIter.hasNext()) {
                String derName = (String) derivationsIter.next();
                Element derEle = (Element) annotDerivations.get(derName);
                // At least, we want to test non-directed and directed A->B
                Element nondirEle = (Element) derEle.clone();
                Element dirEle = (Element) derEle.clone();
                dirEle.getChild("directed").setText("true");
                allDerivations.put(derName, nondirEle);
                allDerivations.put(derName + "D", dirEle);
                // if testing both directions, add a third case: B->A
                if (dirTest.equalsIgnoreCase("DB")) {
                    String name = dirEle.getAttributeValue("name");
                    String vertex1 = dirEle.getChildText("vertex1");
                    String vertex2 = dirEle.getChildText("vertex2");
                    String isDirected = dirEle.getChildText("directed");
                    Integer minAnnot = null;
                    Integer maxAnnot = null;
                    Element annot = dirEle.getChild("numeric-annotation");
                    if (annot != null) {
                        String annotMin = annot.getChildText("min");
                        String annotMax = annot.getChildText("max");
                        if (annotMin != null) {
                            minAnnot = new Integer(annotMin);
                        }
                        if (annotMax != null) {
                            maxAnnot = new Integer(annotMax);
                        }
                    }
                    Element reverseElt =
                            makeEdgeElement(name, vertex2, vertex1, isDirected, minAnnot, maxAnnot);
                    allDerivations.put(derName + "RD", reverseElt);
                }
            }
        } else {
            log.fatal("getPossibleEdgeDerivations(): Unrecognized dir-test: " + dirTest);
            throw new RuntimeException("Unrecognized dir-test: " + dirTest);
        }

        return allDerivations;
    }


    /**
     * Lists all possible derivations for a Vertex e
     *
     * @param e
     * @return
     */
    private HashMap getPossibleVertexDerivations(Element e) {
        Assert.notNull(e, "Null vertex receiveed");
        HashMap derivations = new HashMap();
        String eltName = e.getAttributeValue("name").toUpperCase();

        // The only things that can vary are the annotations
        String annotTest = e.getAttributeValue("annot-test");
        if (annotTest.equalsIgnoreCase("AX")) {
            derivations.put(eltName, makeVertexElement(eltName, null, null));
        } else if (annotTest.equalsIgnoreCase("A1")) {
            // one with [1..]
            derivations.put(eltName + "1inf", makeVertexElement(eltName, new Integer(1), null));
        } else if (annotTest.equalsIgnoreCase("AN")) {
            // [1..], [1..1], [1..2]
            derivations.put(eltName + "1inf", makeVertexElement(eltName, new Integer(1), null));
            derivations.put(eltName + "11", makeVertexElement(eltName, new Integer(1), new Integer(1)));
            derivations.put(eltName + "12", makeVertexElement(eltName, new Integer(1), new Integer(2)));
        } else {
            log.fatal("getPossibleVertexDerivations(): Unrecognized annot-test: " + annotTest);
            throw new RuntimeException("Unrecognized annot-test: " + annotTest);
        }

        return derivations;
    }


    /**
     * Returns the Map with all the query instances for the <query-family> specification
     */
    public Map getQueryInstances() {
        return queryInstances;
    }


    /**
     * Traverse the list of edges in the <query-family> specification
     * and verify that they are correct.
     * Load all the edges into the Edges hash table.
     * NB: Assumes that the Vertices map has already been filled
     * Tests:
     * 1. name cannot be null
     * 2. name cannot already exist
     * 3. v1 cannot be null, and must be a valid vertex
     * 4. v2 cannot be null, and must be a valid vertex
     * 5. annot-test is either null or A+ or A*
     * 6. dir-test is either null (in which case we set it to false), or true or false
     */
    private void loadAndValidateEdges() throws IllegalArgumentException {
        Iterator edgeIter = familyEle.getChildren("edge").iterator();
        while (edgeIter.hasNext()) {
            Element edgeEle = (org.jdom.Element) edgeIter.next();
            String name = edgeEle.getAttributeValue("name").toUpperCase();
            String v1 = edgeEle.getAttributeValue("v1").toUpperCase();
            String v2 = edgeEle.getAttributeValue("v2").toUpperCase();
            String annotTest = edgeEle.getAttributeValue("annot-test"); // may be null
            String dirTest = edgeEle.getAttributeValue("dir-test"); // may be null
            // tests
            if ((name == null) || (name.length() == 0)) {
                throw new IllegalArgumentException("Edge in <query-family> specification has null name");
            }
            if (Edges.containsKey(name)) {
                throw new IllegalArgumentException("Edge " + name + " already defined");
            }
            if (Vertices.containsKey(name)) {
                throw new IllegalArgumentException("Edge " + name + " already defined as a vertex");
            }
            if (v1 == null) {
                throw new IllegalArgumentException("Empty value for v1 in Edge " + name);
            }
            if (!Vertices.containsKey(v1)) {
                throw new IllegalArgumentException("v1 in Edge " + name + " refers to an undefined vertex: " + v1);
            }
            if (v2 == null) {
                throw new IllegalArgumentException("Empty value for v2 in Edge " + name);
            }
            if (!Vertices.containsKey(v2)) {
                throw new IllegalArgumentException("v2 in Edge " + name + " refers to an undefined vertex: " + v1);
            }
            if (dirTest == null) {
                dirTest = "DX";
                edgeEle.setAttribute("dir-test", dirTest);
            }
            if (annotTest == null) {
                annotTest = "AX";
                edgeEle.setAttribute("annot-test", annotTest);
            }
            if (!annotTest.equalsIgnoreCase("AX")
                    && !annotTest.equalsIgnoreCase("A1")
                    && !annotTest.equalsIgnoreCase("AN")
                    && !annotTest.equalsIgnoreCase("AA")) {
                throw new IllegalArgumentException("Invalid annot-test value for Edge " + name + ": " + annotTest);
            }
            if (!dirTest.equalsIgnoreCase("DX")
                    && !dirTest.equalsIgnoreCase("DO")
                    && !dirTest.equalsIgnoreCase("DB")) {
                throw new IllegalArgumentException("Invalid dir-test value for Edge " + name + ": " + dirTest);
            }
            // Everything is OK.
            Edges.put(name, edgeEle);
        }
    }


    /**
     * Traverse the list of vertices in the <query-family> specification
     * and verify that they are correct.
     * Load all the vertices into the Vertices hash table.
     * Tests:
     * 1. name cannot be null
     * 2. annot-test is either null or A+
     * 3. name cannot already exist
     */
    private void loadAndValidateVertices() throws IllegalArgumentException {
        Iterator vertexIter = familyEle.getChildren("vertex").iterator();
        while (vertexIter.hasNext()) {
            Element vertEle = (Element) ((Element) vertexIter.next()).clone();
            String name = vertEle.getAttributeValue("name").toUpperCase();
            String annotTest = vertEle.getAttributeValue("annot-test"); // may be null
            if (annotTest == null) {
                annotTest = "AX";
                vertEle.setAttribute("annot-test", annotTest);
            }
            // tests
            if ((name == null) || (name.length() == 0)) {
                throw new IllegalArgumentException("Vertex in <query-family> specification has null name");
            }
            if (Vertices.containsKey(name)) {
                throw new IllegalArgumentException("Vertex " + name + " already defined");
            }
            if (!annotTest.equalsIgnoreCase("AX")
                    && !annotTest.equalsIgnoreCase("A1")
                    && !annotTest.equalsIgnoreCase("AN")) {
                throw new IllegalArgumentException("Invalid annot-test value for Vertex " + name + ": " + annotTest);
            }
            // Everything is OK.
            Vertices.put(name, vertEle);
        }
    }


    /**
     * Reads the <query-family> specification and stores elements in Edges and Vertices
     */
    private void loadAndValidateSpecification() throws IllegalArgumentException {
        loadAndValidateVertices();
        loadAndValidateEdges();
    }


    /**
     * Creates an XML element for a condition, following query-graph.dtd.
     * It takes the type of a vertex or edge and makes a condition that compares
     * objectType eq the vertex/edge name
     *
     * @param type the type of the edge/vertex
     */
    private Element makeConditionElement(String attribute, String type) {
        // The test
        Element testEle = new Element("test");
        Element operatorEle = new Element("operator");
        operatorEle.addContent("eq");
        Element attrNameEle = new Element("attribute-name");
        attrNameEle.addContent(attribute);
        Element valueEle = new Element("value");
        valueEle.addContent(type);
        testEle.addContent(operatorEle);
        testEle.addContent(attrNameEle);
        testEle.addContent(valueEle);
        // The condition
        Element condEle = new Element("condition");
        condEle.addContent(testEle);

        return condEle;
    }


    /**
     * Creates an XML  element for an edge, following query-graph.dtd
     *
     * @param name       the name of the edge
     * @param v1         the name of <vertex1>
     * @param v2         the name of <vertex2>
     * @param isDirected specifies whether the edge is directed or not
     * @param annotMin   the <min> value for a numeric annotation (a null value means no annotation)
     * @param annotMax   the <max> value for a numeric annotation (a null value means no upper bound)
     */
    private Element makeEdgeElement(String name, String v1, String v2, String isDirected, Integer annotMin, Integer annotMax) {
        Element newElt = new Element("edge");
        newElt.setAttribute("name", name);
        Element newV1Elt = new Element("vertex1");
        newV1Elt.addContent(v1);
        Element newV2Elt = new Element("vertex2");
        newV2Elt.addContent(v2);
        Element newIsDirected = new Element("directed");
        newIsDirected.addContent(isDirected);
        newElt.addContent(newV1Elt);
        newElt.addContent(newV2Elt);
        newElt.addContent(newIsDirected);
        newElt.addContent(makeConditionElement("linkType", name));
        if (annotMin != null) {
            Element newAnnot = new Element("numeric-annotation");
            Element newAnnotMin = new Element("min");
            newAnnotMin.addContent(annotMin.toString());
            newAnnot.addContent(newAnnotMin);
            if (annotMax != null) {
                Element newAnnotMax = new Element("max");
                newAnnotMax.addContent(annotMax.toString());
                newAnnot.addContent(newAnnotMax);
            }
            newElt.addContent(newAnnot);
        }

        return newElt;
    }


    /**
     * Creates an XML  element for a vertex, following query-graph.dtd
     *
     * @param name     the name of the vertex
     * @param annotMin the <min> value for a numeric annotation (a null value means no annotation)
     * @param annotMax the <max> value for a numeric annotation (a null value means no upper bound)
     */
    private Element makeVertexElement(String name, Integer annotMin, Integer annotMax) {
        Element newElt = new Element("vertex");
        newElt.setAttribute("name", name);
        newElt.addContent(makeConditionElement("objectType", name));
        if (annotMin != null) {
            Element newAnnot = new Element("numeric-annotation");
            Element newAnnotMin = new Element("min");
            newAnnotMin.addContent(annotMin.toString());
            newAnnot.addContent(newAnnotMin);
            if (annotMax != null) {
                Element newAnnotMax = new Element("max");
                newAnnotMax.addContent(annotMax.toString());
                newAnnot.addContent(newAnnotMax);
            }
            newElt.addContent(newAnnot);
        }

        return newElt;
    }
}
