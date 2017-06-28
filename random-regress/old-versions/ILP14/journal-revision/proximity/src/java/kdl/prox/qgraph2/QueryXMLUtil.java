/**
 * $Id: QueryXMLUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qgraph2;

import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Date;


/**
 * Provides functionality to translate a Query into XML.
 * todo opposite of QueryGraph2CompOp.queryFromGraphQueryEle(); merge to here?
 */
public class QueryXMLUtil {

    private static Logger log = Logger.getLogger(QueryXMLUtil.class);

    /**
     * Converts the <graph-query>) Element
     * into a QGraph query (Query). NB: From connectEdges(): If it can't find
     * either of an edge's vertices (by name) the invalid edge is left in the
     * query. This menas query might have edges that are partially connected,
     * or not connected at all. It's up to callers to validate and correct as
     * desired.
     */
    public static Query graphQueryEleToQuery(Element graphQueryEle) {
        // recall: <graph-query> -> description?, query-body, editor-data?
        Element queryBodyEle = graphQueryEle.getChild("query-body");
        if (queryBodyEle == null) {
            throw new IllegalArgumentException("bad query: no <query-body> " +
                    "element in " + graphQueryEle);
        }

        List queryBodyEles = queryBodyEle.getChildren();    // recall: <query-body> -> vertex, (edge | vertex)*, subquery*, constraint?
        Query query = (Query) queryFromQueryBodyEles(queryBodyEles, null,
                graphQueryEle.getAttributeValue("name"),
                graphQueryEle.getChildText("description"));     // no parent
        connectEdges(query);
        detachElesQuery(query);
        extractContraints(query, query.constEle());
        query.setConstEle(null);    // no longer needed. also, if we keep it around it can get out of sync with the query's constraints (QGConstraint instsances)
        return query;
    }

    /**
     * Called by queryFromGraphQueryEle(), connects edges to their vertices via
     * (global) names. NB: If it can't find either of an edge's vertices (by
     * name) then continues processing, <b>leaving</b> the invalid edge in
     * query. This menas query might have edges that are partially connected,
     * or not connected at all. It's up to callers to validate and correct as
     * desired.
     */
    private static void connectEdges(Query query) {
        // for each edge do the connection, ignoring missing vertices
        List qgItems = query.edges(true);        // edges. isRecurse
        Iterator qgItemIter = qgItems.iterator();
        while (qgItemIter.hasNext()) {
            QGEdge qgEdge = (QGEdge) qgItemIter.next();
            QGItem qgItemQGV1 = query.qgItemForName(qgEdge.vertex1Name());
            QGItem qgItemQGV2 = query.qgItemForName(qgEdge.vertex2Name());

            // connect vertex1 if possible
            if (qgItemQGV1 instanceof QGVertex) {
                QGVertex qgVertex1 = (QGVertex) qgItemQGV1;
                qgEdge.setVertex1(qgVertex1);
                qgVertex1.addEdge(qgEdge);
            } else {
                log.warn("couldn't find vertex1 for edge; leaving unconnected: " +
                        qgEdge + ", " + qgEdge.vertex1Name());
            }

            // connect vertex2 if possible
            if (qgItemQGV2 instanceof QGVertex) {
                QGVertex qgVertex2 = (QGVertex) qgItemQGV2;
                qgEdge.setVertex2(qgVertex2);
                if (qgItemQGV2 != qgItemQGV1) {   // in case it's a self-loop
                    qgVertex2.addEdge(qgEdge);
                }
            } else {
                log.warn("couldn't find vertex2 for edge; leaving unconnected: " +
                        qgEdge + ", " + qgEdge.vertex2Name());
            }
        }
    }

    /**
     * Helper called by queryFromGraphQueryEle() and recursively, returns an
     * AbstractQuery (either a Query or Subquery) corresponding to
     * queryBodyOrSubqEles, based on parentAQuery: If parentAQuery is null then
     * returns a (top-level) Query instance. If parentAQuery is non-null then
     * returns a Subquery instance. . queryBodyOrSubqEles is a List of
     * the children from either a <query-body> Element, or a <subquery> Element
     * (*without* its <numeric-annotation>). Thus, queryBodyOrSubqEles will
     * contain this DTD pattern: vertex, (edge | vertex)*, subquery*, constraint?
     * parentAQuery is the parent Subquery or Query that I'm contained by.
     *
     * @param queryBodyOrSubqEles &lt;query-body> content
     * @param parentAQuery        null if Query (i.e., no parent)
     * @param name                Query name if parentAQuery null. null if has parent
     * @param description         Query description if parentAQuery null. null if has parent
     */
    private static AbstractQuery queryFromQueryBodyEles(List queryBodyOrSubqEles,
                                                        AbstractQuery parentAQuery,
                                                        String name,
                                                        String description) {
        AbstractQuery abstractQuery;    // return value. set and filled next
        if (parentAQuery == null)
            abstractQuery = new Query(name, description);
        else
            abstractQuery = new Subquery(parentAQuery);
        // create objects corresponding to queryBodyOrSubqEles
        Iterator queryBodyIter = queryBodyOrSubqEles.iterator();
        while (queryBodyIter.hasNext()) {
            Element queryBodyEle = (Element) queryBodyIter.next();
            String elementName = queryBodyEle.getName();
            if (elementName.equals("vertex")) {
                // recall: <vertex> -> condition?, numeric-annotation?
                String vertexName = queryBodyEle.getAttributeValue("name");
                Element condEle = queryBodyEle.getChild("condition");                // may be null
                Element annotEle = queryBodyEle.getChild("numeric-annotation");        // ""
                abstractQuery.addVertex(new QGVertex(vertexName, condEle,
                        annotEle));
            } else if (elementName.equals("edge")) {
                // recall: <edge> -> vertex1, vertex2, directed, condition?, numeric-annotation?
                String edgeName = queryBodyEle.getAttributeValue("name");
                String vertex1Name = queryBodyEle.getChildText("vertex1");
                String vertex2Name = queryBodyEle.getChildText("vertex2");
                String directedStr = queryBodyEle.getChildText("directed");
                Element condEle = queryBodyEle.getChild("condition");                // may be null
                Element annotEle = queryBodyEle.getChild("numeric-annotation");        // ""
                abstractQuery.addEdge(new QGEdge(edgeName, condEle, annotEle,
                        vertex1Name, vertex2Name, directedStr));
            } else if (elementName.equals("subquery")) {
                // recall: <subquery> -> vertex, (edge | vertex)*, subquery*, constraint?, numeric-annotation
                //                       \______________________  ______________________/
                //                                              \/
                //                                    identical to <query-body>
                Element annotEle = queryBodyEle.getChild("numeric-annotation");
                annotEle.detach();        // remove it before recursion
                List queryBodyEles = queryBodyEle.getChildren();
                Subquery subquery = (Subquery) queryFromQueryBodyEles(queryBodyEles,
                        abstractQuery, null, null);        // recurse. non-null parent
                subquery.setAnnotation(new Annotation(annotEle));
                abstractQuery.addSubquery(subquery);
            } else if (elementName.equals("constraint")) {
                abstractQuery.setConstEle(queryBodyEle);
            } else if (elementName.equals("add-link")) {
                String attrName = queryBodyEle.getAttributeValue("attrname");
                String attrValue = queryBodyEle.getAttributeValue("attrval");
                String vertex1Name = queryBodyEle.getChildText("vertex1");
                String vertex2Name = queryBodyEle.getChildText("vertex2");
                QGAddLink addLink = new QGAddLink(vertex1Name, vertex2Name, attrName, attrValue);
                abstractQuery.addAddLink(addLink);
            } else if (elementName.equals("cached")) {
                String itemName = queryBodyEle.getAttributeValue("item");
                String containerName = queryBodyEle.getAttributeValue("container");
                abstractQuery.addCachedItem(itemName, containerName);
            } else {
                throw new IllegalArgumentException("unexpected element name: " +
                        queryBodyEle);        // just in case
            }
        }
        // done
        return abstractQuery;
    }

    /**
     * Called by queryFromGraphQueryEle(), detaches all Elements in query to
     * make the serialization-based copying more efficient. It is not done in
     * queryFromQueryBodyEles() because it causes ConcurrentModificationException
     * while iterating. The following ElementS are detached: QGItem.condEleChild
     * and AbstractQuery.constEle.
     *
     * @param query Query whose ElementS are to be detached
     */
    private static void detachElesQuery(Query query) {
        QueryIterator queryIter = new QueryIterator();
        QueryIterHandler queryIterHandler = new QueryIterHandlerEmptyAdapter() {

            public void constraint(QGConstraint qgConstraint) {
                // constraints don't have elements to detach.
            }

            public void edge(QGEdge qgEdge) {
                if (qgEdge.condEleChild() != null)
                    qgEdge.condEleChild().detach();
            }

            public void startAbstractQuery(AbstractQuery abstractQuery) {
                if (abstractQuery.constEle() != null)
                    abstractQuery.constEle().detach();
            }

            public void vertex(QGVertex qgVertex) {
                if (qgVertex.condEleChild() != null)
                    qgVertex.condEleChild().detach();
            }

        };
        queryIter.setHandler(queryIterHandler);
        queryIter.iterate(query);
    }

    /**
     * Takes the constEle of a query and goes through the list of elements, creating
     * QGConstraint objects and adding them to the query's constraints variable.
     * If it finds an <and> element, it calls itself recursively.
     *
     * @param query
     * @param constEle
     */
    private static void extractContraints(Query query, Element constEle) {
        Assert.notNull(query, "null query");
        if (constEle == null) {
            return;
        }

        List constEleChildren = constEle.getChildren();
        Iterator childIter = constEleChildren.iterator();
        while (childIter.hasNext()) {
            Element thisChildConst = (Element) childIter.next();
            if (thisChildConst.getName().equals("and")) { // recurse
                extractContraints(query, thisChildConst);
            } else if (thisChildConst.getName().equals("test")) {
                // see if they are annotated
                // checking that only one is annotated, and that it's a vertex,
                //   is done by QueryValidator
                List itemEles = thisChildConst.getChildren("item");
                String item1Name = ((Element) itemEles.get(0)).getChildText("item-name");
                String item2Name = ((Element) itemEles.get(1)).getChildText("item-name");
                QGItem item1 = query.qgItemForName(item1Name);  // null if not found
                QGItem item2 = query.qgItemForName(item2Name);  // ""
                boolean isEdge = (item1 instanceof QGEdge);
                if (item1 == null || item2 == null) {
                    // missing or unknown item. just continue; QueryValidator,
                    // if called, will detect error and complain
                    log.warn("missing or unknown item1 (" + item1Name + ": " +
                            (item1 == null ? "missing" : "OK") + ") or item2 (" +
                            item2Name + ": " + (item2 == null ? "missing" : "OK") + ")");
                    query.addConstraint(new QGConstraint(thisChildConst, isEdge));  // bogus isEdge
                } else if (item1.isAnnotated()) {
                    query.addConstraint(new QGConstraint(thisChildConst,
                            item1.firstName(), item1.annotation(), isEdge));
                } else if (item2.isAnnotated()) {
                    query.addConstraint(new QGConstraint(thisChildConst,
                            item2.firstName(), item2.annotation(), isEdge));
                } else {
                    query.addConstraint(new QGConstraint(thisChildConst, isEdge));
                }
            } else {
                // unknown connector OR or NOT (accepted by the DTD, but a syntax error)
                throw new IllegalArgumentException("only <and> and <test> allowed in constraints: "
                        + thisChildConst.getName());
            }
        }
    }


    /**
     * @param graphQueryEle &lt;graph-query&gt; element
     * @return &lt;editor-data&gt; within graphQueryEle. returns null if none found
     */
    public static Element getEditorDataElement(Element graphQueryEle) {
        // recall: <graph-query> -> description?, query-body, editor-data?
        return graphQueryEle.getChild("editor-data");
    }


    /**
     * Returns a new graphQueryEle for query. NB: Does not add editor data.
     *
     * @param query
     * @return a &lt;graph-query&gt; Element
     */
    public static Element queryToGraphQueryEle(Query query) {
        QueryIterator queryIter = new QueryIterator();
        QueryToXMLHandler queryHandler = new QueryToXMLHandler(query);
        queryIter.setHandler(queryHandler);
        queryIter.iterate(query);
        return queryHandler.getGraphQuery();
    }


    /**
     * Utility that returns a JDom Element for the &lt;graph-query&gt; in
     * queryFile. File overload. NB: Do <b>NOT</b> use this overload if
     * queryFile is based on a URL that was obtained via Class.getResource().
     * This overload will fail to find the file, due to how JDOM's
     * SAXBuilder.build(File) method works. Instead use the URL overload below.
     *
     * @param queryFile
     */
    public static Element graphQueryEleFromFile(File queryFile) throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder(true);    // validating
        Document document = saxBuilder.build(queryFile);
        Element queryGraphEle = document.getRootElement();
        return queryGraphEle;
    }

    /**
     * URL overload. We need this because the File overload didn't correctly
     * handle File objects that referred to XML files inside JARs. (See note
     * above.)
     *
     * @param fileURL
     */
    public static Element graphQueryEleFromFile(URL fileURL) throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder(true);    // validating
        Document document = saxBuilder.build(fileURL);
        Element queryGraphEle = document.getRootElement();
        return queryGraphEle;
    }

    /**
     * Generate a graphQueryEle from an XML String
     *
     * @param xmlString
     * @return
     * @throws Exception
     */
    public static Element graphQueryEleFromXML(String xmlString) throws Exception {
        File tempFile = new File("temp-xml-dump.xml");
        Util.saveStringToFile(xmlString, tempFile);
        Element element = graphQueryEleFromFile(tempFile);
        tempFile.delete();
        return element;
    }

    /**
     * Saves graphQueryEle to file as pretty XML. Takes a graphQueryEle instead
     * of a Query so that callers can first modify the passed Element, for
     * example by adding editor data.
     *
     * @param graphQueryEle a &lt;graph-query&gt; Element
     * @param file          File to save graphQueryEle to
     * @throws IOException
     */
    public static void graphQueryEleToFile(Element graphQueryEle, File file) throws IOException {
        BufferedWriter buffW = null;
        try {
            buffW = new BufferedWriter(new FileWriter(file));
            if (graphQueryEle.getParent() == null) {
                Document document = new Document(graphQueryEle);
                document.setDocType(new DocType("graph-query", "graph-query.dtd"));
                new XMLOutputter(Format.getPrettyFormat()).output(document, buffW);
            } else {
                log.info("Writing directly");
                new XMLOutputter(Format.getPrettyFormat()).output(graphQueryEle, buffW);
            }
        } finally {
            if (buffW != null) {
                buffW.close();
            }
        }
    }

    public static String graphQueryEleToXML(Element graphQueryEle) {
        String xml = "";
        Date currentTime = new Date();   //To add a random element to the file name to avoid collisions
        File tempFile = new File(System.getProperty("java.io.tmpdir", "tmp"), "xml-dump-" + currentTime.getTime() + ".xml");
        try {
            graphQueryEleToFile(graphQueryEle, tempFile);
            xml = Util.readStringFromFile(tempFile);
        } catch (IOException e) {
            log.error("Problems generating XML : " + e);
        }
        tempFile.delete();

        return xml;
    }

    //
    // inner classes
    //

    /**
     * Handler that generates XML from a Query.
     */
    static public class QueryToXMLHandler extends QueryIterHandlerEmptyAdapter {

        Element graphQuery;
        Element currentEle;
        Element queryBody;

        Query query;


        public QueryToXMLHandler(Query query) {
            this.query = query;

            //Create graphQueryElement
            graphQuery = new Element("graph-query");
            graphQuery.setAttribute("name", query.getName());

            //add description
            Element qIDesc = new Element("description");
            qIDesc.addContent(query.getDescription());

            //Create query-body element
            queryBody = new Element("query-body");
            graphQuery.addContent(qIDesc);
            graphQuery.addContent(queryBody);

            currentEle = queryBody;
        }

        public void addLink(QGAddLink qgAddLink) {
            Element newAddLinkElt = new Element("add-link");
            newAddLinkElt.setAttribute("attrname", qgAddLink.getAttrName());
            newAddLinkElt.setAttribute("attrval", qgAddLink.getAttrValue());
            Element vertex1 = new Element("vertex1");
            Element vertex2 = new Element("vertex2");
            //Add vertex1,vertex2 name
            vertex1.addContent(qgAddLink.getVertex1Name());
            vertex2.addContent(qgAddLink.getVertex2Name());
            //Add vertices to newAddEdgeElt
            newAddLinkElt.addContent(vertex1);
            newAddLinkElt.addContent(vertex2);
            currentEle.addContent(newAddLinkElt);
        }

        public void cachedElement(QGItem item, String containerName) {
            Element newCachedElt = new Element("cached");
            newCachedElt.setAttribute("item", item.firstName());
            newCachedElt.setAttribute("container", containerName);
            currentEle.addContent(newCachedElt);
        }

        public void constraint(QGConstraint constraint) {
            Element newConstraint = new Element("test");

            Element opEle = new Element("operator");
            opEle.addContent(constraint.operator());
            newConstraint.addContent(opEle);

            //Create the first item
            Element item1Ele = new Element("item");
            Element item1NameEle = new Element("item-name");
            item1NameEle.addContent(constraint.item1Name());
            item1Ele.addContent(item1NameEle);
            Element item1AttrEle;
            if (constraint.item1AttrName() != null) {
                item1AttrEle = new Element("attribute-name");
                item1AttrEle.addContent(constraint.item1AttrName());
            } else {
                //Must be id
                item1AttrEle = new Element("id");
            }
            item1Ele.addContent(item1AttrEle);

            //Create the second item
            Element item2Ele = new Element("item");
            Element item2NameEle = new Element("item-name");
            item2NameEle.addContent(constraint.item2Name());
            item2Ele.addContent(item2NameEle);
            Element item2AttrEle;
            if (constraint.item2AttrName() != null) {
                item2AttrEle = new Element("attribute-name");
                item2AttrEle.addContent(constraint.item2AttrName());
            } else {
                //Must be id
                item2AttrEle = new Element("id");
            }
            item2Ele.addContent(item2AttrEle);

            newConstraint.addContent(item1Ele);
            newConstraint.addContent(item2Ele);

            //Add it to the and element
            Element constraintEle = queryBody.getChild("constraint");
            Element andEle = constraintEle.getChild("and");
            andEle.addContent(newConstraint);
        }

        public void edge(QGEdge qgEdge) {
            //Create edge element
            //Element newEdgeEle = new Element(qgEdge.firstName());
            Element newEdgeEle = new Element("edge");
            newEdgeEle.setAttribute("name", qgEdge.firstName());
            //Write out vertices
            Element vertex1 = new Element("vertex1");
            Element vertex2 = new Element("vertex2");
            //Add vertex1,vertex2 name
            vertex1.addContent(qgEdge.vertex1().firstName());
            vertex2.addContent(qgEdge.vertex2().firstName());

            //Add vertices to EdgeEle
            newEdgeEle.addContent(vertex1);
            newEdgeEle.addContent(vertex2);

            //Write out directed
            Element directed = new Element("directed");
            String directedString = ((qgEdge.isDirected()) ? "true" : "false");
            directed.addContent(directedString);
            newEdgeEle.addContent(directed);

            //Get Annotation
            Annotation edgeAnnot = qgEdge.annotation();
            //Get Condition	child
            Element condEleChild = qgEdge.condEleChild();

            //Make a condition element
            if (condEleChild != null) {
                Element condEle = new Element("condition");
                condEleChild.detach();
                condEle.addContent(condEleChild);
                newEdgeEle.addContent(condEle);
            }
            //Make an annotation element
            Element annotEle = new Element("numeric-annotation");
            int max = -1;
            int min = -1;
            if (edgeAnnot != null) {
                max = edgeAnnot.annotMax();
                min = edgeAnnot.annotMin();
            }
            if (min != -1) {
                Element minEle = new Element("min");
                minEle.addContent("" + min);
                annotEle.addContent(minEle);
            }

            if (max != -1) {
                Element maxEle = new Element("max");
                maxEle.addContent("" + max);
                annotEle.addContent(maxEle);
            }

            if (edgeAnnot != null) {
                newEdgeEle.addContent(annotEle);
            }

            //Add to queryBody
            currentEle.addContent(newEdgeEle);
        }

        public void endAbstractQuery(AbstractQuery abstractQuery) {

            //Check if this is a subquery
            if (abstractQuery.getClass().toString().indexOf("Sub") > 0) {

                //Make an annotation element
                Annotation edgeAnnot = ((Subquery) abstractQuery).annotation();

                Element annotEle = new Element("numeric-annotation");
                int max = -1;
                int min = -1;
                if (edgeAnnot != null) {
                    max = edgeAnnot.annotMax();
                    min = edgeAnnot.annotMin();
                }
                if (min != -1) {
                    Element minEle = new Element("min");
                    minEle.addContent("" + min);
                    annotEle.addContent(minEle);
                }

                if (max != -1) {
                    Element maxEle = new Element("max");
                    maxEle.addContent("" + max);
                    annotEle.addContent(maxEle);
                }

                if (edgeAnnot != null) {
                    currentEle.addContent(annotEle);
                }
                queryBody.addContent(currentEle);
            }


        }

        public Element getGraphQuery() {
            return graphQuery;
        }

        public void startAbstractQuery(AbstractQuery abstractQuery) {
            //Check if this is a subquery
            if (abstractQuery.getClass().toString().indexOf("Sub") > 0) {
                currentEle = new Element("subquery");
                return;
            }
            currentEle = queryBody;
        }

        public void startConstraints() {
            List constraints = query.constraints();
            if (constraints.size() > 0) {
                if (queryBody.getChild("constraint") == null) {
                    Element constraintEle = new Element("constraint");
                    queryBody.addContent(constraintEle);
                    Element constraintAndEle = new Element("and");
                    constraintEle.addContent(constraintAndEle);
                }
            }
        }

        public void vertex(QGVertex qgVertex) {
            //Create edge element
            //Element newVertexEle = new Element(qgVertex.firstName());
            Element newVertexEle = new Element("vertex");
            newVertexEle.setAttribute("name", qgVertex.firstName());
            //Get Annotation
            Annotation vertexAnnot = qgVertex.annotation();
            //Get Condition	child
            Element condEleChild = qgVertex.condEleChild();

            //Make a condition element
            if (condEleChild != null) {
                Element condEle = new Element("condition");
                condEleChild.detach();
                condEle.addContent(condEleChild);
                newVertexEle.addContent(condEle);
            }

            //Make an annotation element
            int min = -1;
            int max = -1;
            Element annotEle = new Element("numeric-annotation");
            if (vertexAnnot != null) {
                max = vertexAnnot.annotMax();
                min = vertexAnnot.annotMin();
            }
            if (min != -1) {
                Element minEle = new Element("min");
                minEle.addContent("" + min);
                annotEle.addContent(minEle);
            }

            if (max != -1) {
                Element maxEle = new Element("max");
                maxEle.addContent("" + max);
                annotEle.addContent(maxEle);
            }
            //Add annotation
            if (vertexAnnot != null) {
                newVertexEle.addContent(annotEle);
            }
            //Add to queryBody
            currentEle.addContent(newVertexEle);
        }

    }


}
