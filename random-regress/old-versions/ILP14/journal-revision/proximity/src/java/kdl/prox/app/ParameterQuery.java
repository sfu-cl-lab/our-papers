/**
 * $Id: ParameterQuery.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.app;

import kdl.prox.db.DB;
import kdl.prox.monet.MonetException;
import kdl.prox.qgraph2.QueryGraph2CompOp;
import kdl.prox.qgraph2.QueryXMLUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.python.core.PyList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

public class ParameterQuery {

    private static Logger log = Logger.getLogger(ParameterQuery.class);

    /**
     * Runs a QGraph 2.0 query on a Proximity database.
     * <p/>
     * Args: hostAndPort queryXMLFile collectionName [inputContainer]
     * <p/>
     * The syntax for inputContainer is a path delimited by '/' characters,
     * starting with '/', e.g.:
     * <p/><pre>
     *      /c1/c2  -- container c2 under container c1 under the root container
     * </pre><p/>
     * Pass the empty string or '/' for inputContainer to run on the entire database.
     */
    public static void main(String[] args) {
        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length < 3) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        String hostAndPort = args[0];
        String queryFileName = args[1];
        String outputContainerName = args[2];

        //String inputContainerPath = (args.length == 4 ? args[3] : null); // entire db by default
        String inputContainerPath = null;

        // get the extra conditions
        List conditions = new ArrayList();
        if (args.length > 3) {
            int i;
            for (i = 3; i < args.length; i++) {
                conditions.add(args[i]);
            }

        }

        Util.initProxApp();
        log.debug("main(): " + queryFileName + ", " + outputContainerName +
                (inputContainerPath != null ? ", " + inputContainerPath : ""));
        if ((queryFileName.length() == 0) || (outputContainerName.length() == 0)) {
            log.fatal("one of the two args was empty: '" + queryFileName + "', '" +
                    outputContainerName + "'");
            printUsage();
            System.exit(-1);
        }
        try {
            DB.open(hostAndPort);
            File queryFile = new File(queryFileName);
            ParameterQuery.queryGraph(queryFile, outputContainerName, conditions);
        } catch (MonetException monExc) {
            log.error("error running query", monExc);
        } finally {
            DB.close();
        }
    }


    private static void printUsage() {
        System.out.println("Usage: java " + ParameterQuery.class.getName() +
                " hostAndPort queryXMLFile containerName [inputContainer]\n" +
                "\thostAndPort: <host>:<port>\n" +
                "\tqueryXMLFile - QGraph 2.0 query XML file; either " +
                " relative to the working directory, or absolute\n" +
                "\tcontainerName: name of the container to create and save the " +
                "results in\n" +
                "\t[condition]: optional condition to be and'ed onto subgraph items (e.g. Author.name=Joe)");
    }

    /**
     * Runs queryGraph with a PyList overload
     *
     * @param queryFile
     * @param outputContainerName
     * @param pyConditions
     */

    public static void queryGraph(File queryFile,
                                  String outputContainerName,
                                  PyList pyConditions) {
        List conditions = Util.listFromPyList(pyConditions);
        queryGraph(queryFile, outputContainerName, conditions);
    }

    /**
     * Runs the passed query. NB: Deletes the containerName if it exists without
     * prompting.
     */
    public static void queryGraph(File queryFile,
                                  String outputContainerName,
                                  List conditions) {

        /*
        // get the input container, if not null. NB: we allow a syntax that is
        // different from the ProxURL syntax used internally. specifically,
        // '/' and '' are treated as representing the entire db
        Container inputContainer = null;
        try {
            if (inputContainerPath != null && inputContainerPath.length() > 0 &&
                    !("/".equals(inputContainerPath))) {
                ProxURL proxURL = new ProxURL("cont:/containers" + inputContainerPath);
                inputContainer = proxURL.getContainer(proxDB, false);
            }
        } catch (IllegalArgumentException iaExc) {
            throw new IllegalArgumentException("invalid input container path: '" +
                    inputContainerPath + "'");
        }
        */
        try {
            // read the graph query xml file and pass it to queryGraph()
            log.info("* processing query: " + queryFile.getName());
            Element graphQueryEle = QueryXMLUtil.graphQueryEleFromFile(queryFile);

            //log.debug((new XMLOutputter()).outputString(graphQueryEle));

            ListIterator condIter = conditions.listIterator();
            while (condIter.hasNext()) {
                ParameterQuery.parseCondition((String) condIter.next(), graphQueryEle);
            }

            QueryGraph2CompOp.queryGraph(graphQueryEle, null, outputContainerName, true);
            log.info("* done executing query");
        } catch (Exception exc) {
            log.error("error processing query", exc);
        }
    }


    protected static void parseCondition(String newValue, Element graphQueryEle) {

        //Parse the string and add children as appropriate to newElement
        //First, remove the spaces from the condition just in case
        String label = newValue;
        //label = label.replaceAll(" ", "");
        StringTokenizer spaceRemover = new StringTokenizer(label, " '", true);
        String noSpaces = "";
        while (spaceRemover.hasMoreTokens()) {
            String token = spaceRemover.nextToken();
            if (!token.equalsIgnoreCase(" ")) {
                if (token.equalsIgnoreCase("'")) {
                    token += spaceRemover.nextToken("'");
                    token += spaceRemover.nextToken("' ");
                }
                noSpaces += token;
                //log.debug("Next token:" + token);
            }
        }
        label = noSpaces;
        //log.debug("Label:" + label);
        StringTokenizer testTokens = new StringTokenizer(label, "=><", true);

        if (testTokens.countTokens() == 0) {
            return;
        }

        // did we see all three required parts of the test string?
        if (testTokens.countTokens() < 3) {
            System.out.println("Invalid condition: " +
                    newValue + ". Condition should" +
                    " be in the form " +
                    " <item-name>[.<attrbute-name>] " +
                    "<operator> " +
                    "<value>");
            System.exit(-1);

        }

        // the first token is item-name with or without attribute
        String item1 = testTokens.nextToken();
        String operator;
        String opString = "eq";

        // the = is only one token but <= and >= are two.
        if (testTokens.countTokens() == 2) {
            operator = testTokens.nextToken();
        } else {
            operator = testTokens.nextToken() + testTokens.nextToken();
        }

        // the remaining token is the value
        String value = testTokens.nextToken();


        if (operator.equals("=")) {
            opString = "eq";
        }
        if (operator.equals("<=")) {
            opString = "le";
        }
        if (operator.equals(">=")) {
            opString = "ge";
        }
        if (operator.equalsIgnoreCase("<>")) {
            opString = "ne";
        }
        if (operator.equals("<")) {
            opString = "lt";
        }
        if (operator.equals(">")) {
            opString = "gt";
        }

        String item1Name;
        String item1AttrName = null;

        //Need to parse items to find item-names and attributes names
        StringTokenizer item1Tokens = new StringTokenizer(item1,
                ".", false);
        item1Name = item1Tokens.nextToken();
        if (item1Tokens.countTokens() > 0) {
            //This has an attribute
            item1AttrName = item1Tokens.nextToken();
        }

        // now have:
        // item1Name
        // item1AttrName
        // opString
        // value
        log.debug("item1Name: " + item1Name);
        log.debug("item1AttrName: " + item1AttrName);
        log.debug("opString: " + opString);
        log.debug("value: " + value);

        // create a new test element
        Element opEle = new Element("operator");
        opEle.addContent(opString);
        Element attrEle = new Element("attribute-name");
        attrEle.addContent(item1AttrName);
        Element valEle = new Element("value");
        valEle.addContent(value);

        Element testEle = new Element("test");
        testEle.addContent(opEle);
        testEle.addContent(attrEle);
        testEle.addContent(valEle);

        List verticesList = graphQueryEle.getChild("query-body").getChildren("vertex");
        ListIterator vertIter = verticesList.listIterator();
        while (vertIter.hasNext()) {
            Element vertEle = (Element) vertIter.next();
            if (vertEle.getAttributeValue("name").equals(item1Name)) {
                // check to see if its got a condition
                Element condEle = vertEle.getChild("condition");
                if (condEle == null) {
                    //no condition
                    condEle = new Element("condition");
                    condEle.addContent(testEle);
                    vertEle.addContent(condEle);
                } else {
                    //There is a pre-existing condition
                    //First check to see if it has an <or>
                    Element orEle = condEle.getChild("or");
                    if (orEle == null) {
                        // no <or> then see if it's got an <and>
                        Element andEle = condEle.getChild("and");
                        if (andEle == null) {
                            // grab any existing children
                            List children = condEle.getChildren();
                            List grandchildren = new ArrayList(children);
                            children.clear();

                            //log.debug("children: " + children);
                            //log.debug("grandchildren: " + grandchildren);

                            // put any existing content into the and
                            andEle = new Element("and");
                            andEle.addContent(grandchildren);
                            andEle.addContent(testEle);
                            condEle.addContent(andEle);
                        } else {
                            andEle.addContent(testEle);
                        }
                    } else {
                        //Has an <or> element
                        //Get <and> children and add new element to each
                        // if no <and>s then add to or directly
                        List andList = orEle.getChildren("and");
                        if (andList.size() > 0) {
                            ListIterator andIter = andList.listIterator();
                            while (andIter.hasNext()) {
                                Element currAndEle = (Element) andIter.next();
                                currAndEle.addContent((Element) testEle.clone());
                            }
                        } else {
                            orEle.addContent(testEle);
                        }

                    }
                    log.debug((new XMLOutputter()).outputString(graphQueryEle));


                }
            }
        }
    }

}
