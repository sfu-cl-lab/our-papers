/**
 * $Id: AddAttribute.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AddAttribute.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.script;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class for creating new attribute from existing attributes using a special
 * language.  The user supplies a list of attributes, the name of the new
 * attribute, and the function used to create the new attribute in String
 * form.  This function can take the following forms:<pre>
 *      1)  Literal: a integer, real, string, or boolean.  Assigns to each id
 *          with at least one attribute value in the given attributes a
 *          constant value.  Examples: 3, -5.4, "prox", true
 *      2)  Attribute: name of an attribute that is contained in the given
 *          attributes.
 *      3)  Aggregation: count, avg, sum, prod, min, max.  Aggregates an
 *          attribute.  Examples: count(a), sum(b).
 *      4)  Arithmetic: combines two terms using an operator in
 *          {+, -, *, /, %}.  Note that only two terms can be added at a time.
 *          The function a + b + c is not currently supported.  Terms can be
 *          either literals, attributes, or aggregations.
 *          Examples: sum(a) + max(b), a % b, c - 3.
 *      5)  Switch: a list of (condition, expression) pairs.  Assigns to an
 *          item the expression corresponding to the FIRST condition met
 *          by the object.  Can contain an optional 'default' clause at the
 *          end to assign an expression to items that met none of the
 *          conditions. A condition is either a single comparison using
 *          the set of comparators {=, !=, <, >, <=, >=} (e.g. sum(a) = b)),
 *          or several comparisons combined by 'AND' operators.  An expression
 *          may be any arithmetic operation, literal, attribute, or aggregation.
 * </pre>
 * To illustrate the syntax of a switch statement and provide a simple example,
 * consider a database of three objects (1, 2, and 3) with two attributes
 * defined:
 * <pre>
 * A    id|value
 *      --------
 *      1 | 3
 *      2 | 5
 *      3 | 8
 *      3 | 20
 * <p/>
 * B    id|value
 *      --------
 *      1 | 1
 *      1 | 7
 *      2 | 11
 *      3 | -1
 *      3 | -3
 * </pre>
 * Suppose the function given is:
 * <pre>
 *      a = 3 AND b = 7 ==> 1,
 *      b > 0 ==> 2,
 *      default ==> a + b
 * </pre>
 * Object 1 meets condition 1 and is assigned 1 for its new attribute.
 * Objects 1 and 2 meet condition 2, but because object 1 already met a
 * previous condition, only object 2 is assigned 2 for its new attribute.
 * Object 3 did not meet any conditions and is handled by the default clause.
 * Note that object 3 is assigned four values because it has two values for A
 * and two values for B.
 */
public class AddAttribute {
    private List attrsToConsider;

    private boolean keepPrevAttrVals = false;

    // definition of the attribute creation language
    private static final String BOOLEAN = "(true|false)";
    private static final String STRING = "\".*?\"";
    private static final String INTEGER = "\\-?\\d+";
    private static final String REAL = "\\-?\\d*\\.\\d+";
    private static final String LITERAL = "(" +
            BOOLEAN +
            "|" +
            STRING +
            "|" +
            REAL +
            "|" +
            INTEGER +
            ")";
    //private static final String ATTRIBUTE = "[\\w\\_\\-]+";
    private static final String ATTRIBUTE = "\\S+";
    private static final String AGGREGATE_NAME = "(count|avg|sum|prod|min|max)";
    private static final String AGGREGATE_CALL = AGGREGATE_NAME +
            "\\((" +
            ATTRIBUTE +
            ")\\)";
    private static final String TERM = "(" +
            LITERAL +
            "|" +
            AGGREGATE_CALL +
            "|" +
            ATTRIBUTE +
            ")";
    private static final String OPERATOR = "[+\\-*/%]";
    private static final String ARITHMETIC = TERM +
            "\\s+" +
            OPERATOR +
            "\\s+" +
            TERM;
    private static final String EXPRESSION = "(" +
            ARITHMETIC +
            "|" +
            TERM +
            ")";
    private static final String COMPARATOR = "(<=|>=|!=|<|>|=)";
    private static final String COMPARISON = EXPRESSION +
            "\\s+" +
            COMPARATOR +
            "\\s+" +
            EXPRESSION;
    private static final String CONDITION = COMPARISON +
            "(\\s+AND\\s+" +
            COMPARISON +
            ")*";
    private static final String STATEMENT = CONDITION +
            "\\s+==>\\s+" +
            EXPRESSION;
    //private static final String STATEMENT = CONDITION + "\\s+==>\\s+" + "\\(\\s*" + EXPRESSION + "\\s*\\)";
    private static final String DEFAULT = "default\\s+==>\\s+" + EXPRESSION;
    private static final String SWITCH = "(" +
            STATEMENT +
            ", )*(" +
            STATEMENT +
            "|" +
            DEFAULT +
            ")";
    private static final String FUNCTION = "(" +
            EXPRESSION +
            "|" +
            SWITCH +
            ")";

    private static final Pattern AGGREGATE_PATTERN = Pattern.compile(AGGREGATE_CALL);
    private static final Pattern ARITHMETIC_PATTERN = Pattern.compile(ARITHMETIC);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(ATTRIBUTE);
    private static final Pattern COMPARATOR_PATTERN = Pattern.compile(COMPARATOR);
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(COMPARISON);
    private static final Pattern CONDITION_PATTERN = Pattern.compile(CONDITION);
    private static final Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(EXPRESSION);
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(FUNCTION);
    private static final Pattern LITERAL_PATTERN = Pattern.compile(LITERAL);
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(OPERATOR);
    private static final Pattern STATEMENT_PATTERN = Pattern.compile(STATEMENT);
    private static final Pattern TERM_PATTERN = Pattern.compile(TERM);

    private Logger log = Logger.getLogger(AddAttribute.class);

    public AddAttribute() {
        keepPrevAttrVals = false;
        attrsToConsider = new ArrayList();
    }

    public AddAttribute(boolean keepPrevAttr) {
        keepPrevAttrVals = keepPrevAttr;
        attrsToConsider = new ArrayList();
    }

    public AddAttribute(List attrsToKeep) {
        keepPrevAttrVals = false;
        attrsToConsider = attrsToKeep;
    }

    /**
     * @param keepPrevAttr - if true, inserts into existing attribute the new values.
     *                     ERROR if the attribute exists and keepPrevAttr is false
     * @param attrsToKeep  -   list of attribute names to search over in a default clause.
     *                     Normally, if attrFunction is a function of another attribute(s), then the new attribute
     *                     gets created for all items that had the original.  However, if attrFunction either is a
     *                     switch statement with a default clause or else contains a literal, then it's not obvious
     *                     which items should get the new attribute.  This parameter controls the set of items.  If
     *                     it's null, then all items with *any* attributes (but not necessarily all items--is this
     *                     really desired behavior?) get the new one; if it's non-null, then all items with any of
     *                     the listed attributes get the new one.
     */
    public AddAttribute(List attrsToKeep, boolean keepPrevAttr) {
        keepPrevAttrVals = keepPrevAttr;
        attrsToConsider = attrsToKeep;
    }


    /**
     * Overload of addAttribute to function only on objects or links in
     * a container.
     *
     * @param container    - container that contains the objects or links whose
     *                     attribute values are to be included in computations
     * @param isObject     - is the attribute function over link or object attrs
     * @param newAttrName  - name of the new attribute
     * @param attrFunction - function to calculate the new attribute
     */
    public void addAttribute(Container container, boolean isObject, String newAttrName,
                             String attrFunction) {
        Attributes attributes = isObject ? DB.getObjectAttrs() : DB.getLinkAttrs();
        NST itemNST = container.getItemNST(isObject).project("item_id");
        itemNST.renameColumn("item_id", "id");
        addAttribute(attributes, newAttrName, attrFunction, itemNST);
        itemNST.release();
    }

    /**
     * Adds a new attribute based on a function of existing attributes
     *
     * @param attributes   -   attributes used to construct the new attribute
     * @param newAttrName  -   name of the new attribute
     * @param attrFunction -   string format of the attribute creation function
     */
    public void addAttribute(Attributes attributes, String newAttrName, String attrFunction) {
        addAttribute(attributes, newAttrName, attrFunction, null);
    }

    private void addAttribute(Attributes attributes, String newAttrName, String attrFunction, NST itemsFilterNST) {
        Assert.notNull(attributes, "attributes null");
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");
        Assert.stringNotEmpty(attrFunction, "attrFunction empty");

        Matcher matcher = FUNCTION_PATTERN.matcher(attrFunction);
        Assert.condition(matcher.matches(), attrFunction + " is invalid attr function");

        matcher = EXPRESSION_PATTERN.matcher(attrFunction);

        DB.beginScope();
        try {
            NST finalNST = matcher.matches() ?
                    nstForExpression(attributes, attrFunction, itemsFilterNST) :
                    nstForSwitch(attributes, attrFunction, itemsFilterNST);
            makeAttrFromNST(finalNST, "id", "value", newAttrName, attributes);
            finalNST.release();
        } catch (IllegalArgumentException e) {
            throw e;
        } finally {
            DB.endScope();
        }
    }

    /**
     * Adds a new attribute to all items named by the ModelItem.  All
     * items get the constant value sent in with newAttrValue.
     * (To append values to an existing attribute, call AddAttribute.setKeepPrevAttrVals() first.
     *
     * @param container
     * @param itemName
     * @param newAttrName
     * @param newAttrValue
     * @
     */
    public void addConstantAttribute(Container container, boolean isObject, String itemName,
                                     String newAttrName, Object newAttrValue) {
        Assert.notNull(container, "container null");
        Assert.stringNotEmpty(itemName, "itemName empty");
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");
        Assert.notNull(newAttrValue, "newAttrValue null");

        Attributes attributes = isObject ? DB.getObjectAttrs() : DB.getLinkAttrs();
        //filter container for the appropriate items
        NST containerNST = container.getItemNSTByName(isObject, itemName);
        containerNST.addConstantColumn("value", getDataType(newAttrValue).toString(), newAttrValue.toString());
        makeAttrFromNST(containerNST, "item_id", "value", newAttrName, attributes);
    }


    /**
     * Adds a new attribute to all subgraphs in the container.
     *
     * @param container
     * @param newAttrName
     * @param newAttrValue
     */
    public void addConstantSubgraphAttribute(Container container,
                                             String newAttrName,
                                             Object newAttrValue) {
        Assert.notNull(container, "container null");
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");
        Assert.notNull(newAttrValue, "newAttrValue null");

        Attributes attributes = container.getSubgraphAttrs();
        NST subgAttrNST = container.getDistinctSubgraphOIDs();
        subgAttrNST.addConstantColumn("value", getDataType(newAttrValue).toString(), newAttrValue.toString());
        makeAttrFromNST(subgAttrNST, "subg_id", "value", newAttrName, attributes);
        subgAttrNST.release();
        subgAttrNST.release();
    }

    /**
     * Creates a new attribute that represetns the number of days between two date attributes.
     * Subtracting <dateAttrName2> from <dateAttrName1>.
     *
     * @param attributes
     * @param newAttrName
     * @param dateAttrName1
     * @param dateAttrName2
     * @
     */
    public void addDateDiffAttributeFromTwoDates(Attributes attributes,
                                                 String newAttrName,
                                                 String dateAttrName1,
                                                 String dateAttrName2) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");
        Assert.stringNotEmpty(dateAttrName1, "dateAttrName1 empty");
        Assert.stringNotEmpty(dateAttrName2, "dateAttrName2 empty");

        // check types
        NST attrDataNST1 = attributes.getAttrDataNST(dateAttrName1); // error if not exist
        DataTypeEnum attr1Type = attrDataNST1.getNSTColumn("value").getType();
        Assert.condition(attr1Type.equals(DataTypeEnum.DATE), "Error: dateAttrName1 defined with type " +
                attr1Type + ", not DATE");
        NST attrDataNST2 = attributes.getAttrDataNST(dateAttrName2);
        DataTypeEnum attr2Type = attrDataNST2.getNSTColumn("value").getType();
        Assert.condition(attr2Type.equals(DataTypeEnum.DATE), "Error: dateAttrName2 defined with type " +
                attr2Type + ", not DATE");

        NST joinedNST = attrDataNST1.join(attrDataNST2, "A.id EQ B.id");
        joinedNST.addArithmeticColumn("A.value diff B.value", "int", "difference");
        makeAttrFromNST(joinedNST, "A.id", "difference", newAttrName, attributes);
        joinedNST.release();
    }

    /**
     * For each object, counts its degree with respect to the link NST provided.
     * (The equivalent of "select o1_id, count(*)" from the link table).  Link NST
     * could be regular link table, or could be e.g. bidirection filtered one
     * constructed in PathQuery package.
     * <p/>
     * This function could also be written to work on a specific container & modelItem, but
     * this not implemented yet.
     *
     * @param newAttrName
     * @param linkNST
     */
    public void addOutDegreeAttr(String newAttrName, NST linkNST) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        // get histogram
        Attributes attributes = DB.getObjectAttrs();
        linkNST.addCountColumn("o1_id", "degree");
        makeAttrFromNST(linkNST.distinct("o1_id"), "o1_id", "degree",
                newAttrName, attributes);
        linkNST.removeColumn("degree");
    }


    public void addInDegreeAttr(String newAttrName, NST linkNST) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        // get histogram
        Attributes attributes = DB.getObjectAttrs();
        linkNST.addCountColumn("o2_id", "degree");
        makeAttrFromNST(linkNST.distinct("o2_id"), "o2_id", "degree",
                newAttrName, attributes);
        linkNST.removeColumn("degree");
    }

    /**
     * Adds a new attribute for objects or links (depending on isObject)
     * that contains the IDs of the items in the corresponding table
     * Can be used to have qGraph conditions that limit the range of a vertex/edge
     * to a set of IDs in the database
     *
     * @param newAttrName
     * @param isObject
     */
    public void addIDAttribute(String newAttrName, boolean isObject) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        String colName = (isObject ? "id" : "link_id");
        NST idNST = (isObject ? DB.getObjectNST() : DB.getLinkNST()).project(colName);
        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());

        idNST.addCopyColumn(colName, "value");
        makeAttrFromNST(idNST, colName, "value", newAttrName, attributes);
        idNST.release();
    }

    /**
     * Adds a random attribute between 0 and 1 to every object or link in the database --- serves the
     * same purpose as the above, but handles cases where ids are not sequential
     *
     * @param newAttrName
     * @param isObject
     */
    public void addRandomAttribute(String newAttrName, boolean isObject) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        String colName = (isObject ? "id" : "link_id");
        NST idNST = (isObject ? DB.getObjectNST() : DB.getLinkNST()).project(colName);
        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());

        idNST.addRandomColumn("value");
        makeAttrFromNST(idNST, colName, "value", newAttrName, attributes);
        idNST.release();
    }

    /**
     * Adds a new attribute (or appends values if the attribute exists) to all items named by the ModelItem.
     * All items get uniform random float in the range [0,1].
     *
     * @param container
     * @param isObject
     * @param itemName
     * @param newAttrName
     * @
     */
    public void addRandomAttribute(Container container, boolean isObject, String itemName, String newAttrName) {
        Assert.notNull(container, "container null");
        Assert.stringNotEmpty(itemName, "itemName empty");
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());

        //filter container for the appropriate items
        NST containerNST = container.getItemNSTByName(isObject, itemName);
        containerNST.addRandomColumn("value");
        makeAttrFromNST(containerNST, "item_id", "value", newAttrName, attributes);
        containerNST.release();
        containerNST.release();
    }

    public void addRandomBinaryAttribute(Container container, boolean isObject, String itemName,
                                         String newAttrName) {
        addRandomBinaryAttributeWithPrior(container, isObject, itemName, newAttrName, 0.5);
    }


    /**
     * Adds a new attribute (or appends values if the attribute exists) to all items named by the ModelItem.
     * All items are assigned either 0 or 1 based probability expressed in pOnes.
     *
     * @param container
     * @param isObject
     * @param itemName
     * @param newAttrName
     * @param pOnes
     */
    public void addRandomBinaryAttributeWithPrior(Container container, boolean isObject, String itemName,
                                                  String newAttrName, double pOnes) {
        Assert.notNull(container, "container null");
        Assert.stringNotEmpty(itemName, "itemName empty");
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");

        double threshold = 1 - pOnes;

        Attributes attributes = (isObject ? DB.getObjectAttrs() : DB.getLinkAttrs());

        //filter container for the appropriate items
        NST containerNST = container.getItemNSTByName(isObject, itemName);
        containerNST.addRandomColumn("value");
        containerNST.addArithmeticColumn("value > " + threshold, "bit", "more_than_half");
        containerNST.castColumn("more_than_half", "int");
        makeAttrFromNST(containerNST, "item_id", "more_than_half", newAttrName, attributes);
        containerNST.release();
    }


    public void addYearAttributeFromDate(Attributes attributes, String newAttrName,
                                         String dateAttrName) {
        Assert.stringNotEmpty(newAttrName, "newAttrName empty");
        Assert.stringNotEmpty(dateAttrName, "dateAttrName empty");

        NST attrDataNST = attributes.getAttrDataNST(dateAttrName);
        DataTypeEnum attrType = attrDataNST.getNSTColumn("value").getType();
        Assert.condition(attrType.equals(DataTypeEnum.DATE),
                "Error: dateAttrName defined with type " + attrType + ", not DATE");

        attrDataNST.addArithmeticColumn("value year", "int", "year");
        makeAttrFromNST(attrDataNST, "id", "year", newAttrName, attributes);
        attrDataNST.removeColumn("year");
        attrDataNST.release();
    }

    /**
     * Copies an attribute from one item within a subgraph to another.
     * Notes: a) currently chokes if there's a subgraph without the "to" item. (todo)
     * b) only copies attrs from itemA to itemB once, even if they occur together
     * in multiple subgraphs (this is probably desired behavior).
     *
     * @param container
     * @param isFromItemObject
     * @param fromItemName
     * @param fromAttrName
     * @param isToItemObject
     * @param toItemName
     * @param toAttrName
     */
    public void copyAttrFromItem(Container container,
                                 boolean isFromItemObject, String fromItemName, String fromAttrName,
                                 boolean isToItemObject, String toItemName, String toAttrName) {
        Assert.notNull(container, "container null");

        Attributes attributes = (isFromItemObject ? DB.getObjectAttrs() : DB.getLinkAttrs());
        Attributes toAttributes = (isToItemObject ? DB.getObjectAttrs() : DB.getLinkAttrs());

        NST containerNST = container.getItemNSTByName(isFromItemObject, fromItemName);
        NST oldAttrNST = attributes.getAttrDataNST(fromAttrName); // id, value
        NST joinedNST = containerNST.leftOuterJoin(oldAttrNST, "item_id EQ id");
        NST fromNST = joinedNST.project("item_id, subg_id, value");
        containerNST.release();
        oldAttrNST.release();
        joinedNST.release();

        containerNST = container.getItemNSTByName(isToItemObject, toItemName);
        joinedNST = fromNST.leftOuterJoin(containerNST, "subg_id EQ subg_id");
        joinedNST.groupBy("A.item_id, B.item_id", "temp");
        NST distinctNST = joinedNST.filter("temp DISTINCT ROWS", "B.item_id, value");
        containerNST.release();
        joinedNST.release();
        fromNST.release();

        makeAttrFromNST(distinctNST, "B.item_id", "value", toAttrName, toAttributes);
        distinctNST.release();
    }

    public void copyAttrFromItemToSubgraph(Container container, boolean isObject, String itemName, String attrName,
                                           String newSubgAttrName) {
        Assert.notNull(container, "container null");

        Attributes attributesFrom = isObject ? DB.getObjectAttrs() : DB.getLinkAttrs();
        Attributes attributesTo = container.getSubgraphAttrs();

        NST oldAttrNST = attributesFrom.getAttrDataNST(attrName); // id, value
        NST containerNST = container.getItemNSTByName(isObject, itemName);
        NST joinedNST = containerNST.leftOuterJoin(oldAttrNST, "item_id EQ id");
        makeAttrFromNST(joinedNST, "subg_id", "value", newSubgAttrName, attributesTo);

        joinedNST.release();
        containerNST.release();
        oldAttrNST.release();
    }

    public void copyAttrFromSubgraphToItem(Container container, String fromAttrName,
                                           boolean toIsObject, String toItem, String toAttrName) {
        Assert.notNull(container, "container null");

        Attributes attributesFrom = container.getSubgraphAttrs();
        Attributes attributesTo = toIsObject ? DB.getObjectAttrs() : DB.getLinkAttrs();

        NST fromNST = attributesFrom.getAttrDataNST(fromAttrName); // id, value
        NST containerNST = container.getItemNSTByName(toIsObject, toItem);
        NST joinedNST = fromNST.leftOuterJoin(containerNST, "id EQ subg_id");
        joinedNST.groupBy("item_id, value", "temp"); // no repetitions
        NST distinctNST = joinedNST.filter("temp DISTINCT ROWS", "item_id, value");
        makeAttrFromNST(distinctNST, "item_id", "value", toAttrName, attributesTo);

        containerNST.release();
        joinedNST.release();
        fromNST.release();
        distinctNST.release();
    }

    /**
     * Finds the objAttrName attribute on the O1/O2 end of links linkFilterDef
     * and copies the values as attributes on the link itsef
     *
     * @param linkFilterDef
     * @param isO1
     * @param objAttrName
     * @param newLinkAttrName
     */
    public void addLinkAttributeFromObject(String linkFilterDef, boolean isO1, String objAttrName,
                                           String newLinkAttrName) {
        Assert.stringNotEmpty(objAttrName, "Empty objAttrName");
        Assert.stringNotEmpty(newLinkAttrName, "Empty newLinkAttrName");

        // find links
        NST linksNST = DB.getLinks(linkFilterDef);

        // find the attributes for the o1/o2 object and add it to the table
        NST objAttrNST = DB.getObjectAttrs().getAttrDataNST(objAttrName);
        NST fullLinkAndAttrNST = linksNST.join(objAttrNST, (isO1 ? "o1_id" : "o2_id") + " = id");

        // make the atribute
        String attrTypeDef = DB.getObjectAttrs().getAttrTypeDef(objAttrName);
        NST newAttrDataNST = fullLinkAndAttrNST.project("link_id, value").renameColumn("link_id", "id");
        DB.getLinkAttrs().defineAttributeWithData(newLinkAttrName, attrTypeDef, newAttrDataNST);

        newAttrDataNST.release();
        fullLinkAndAttrNST.release();
        objAttrNST.release();
        linksNST.release();
    }

    /**
     * Finds the objAttrName attribute on the O1/O2 end of links linkFilterDef
     * and copies the values as attributes on the link itsef
     *
     * @param linkFilterDef
     * @param linkAttrName
     * @param isO1
     * @param newObjAttrName
     */
    public void addObjectAttributeFromLink(String linkFilterDef, String linkAttrName,
                                           String newObjAttrName, boolean isO1) {
        Assert.stringNotEmpty(linkAttrName, "Empty linkAttrName");
        Assert.stringNotEmpty(newObjAttrName, "Empty newLinkAttrName");

        // find links
        NST linksNST = DB.getLinks(linkFilterDef);

        // find the attributes for the link and add it to the table
        NST linkAttrNST = DB.getLinkAttrs().getAttrDataNST(linkAttrName);
        NST fullLinkAndAttrNST = linksNST.join(linkAttrNST, "link_id = id");

        // make the atribute
        String attrTypeDef = DB.getLinkAttrs().getAttrTypeDef(linkAttrName);
        String idCol = (isO1 ? "o1_id" : "o2_id");
        NST newAttrDataNST = fullLinkAndAttrNST.project(idCol + ", value").renameColumn(idCol, "id");
        DB.getObjectAttrs().defineAttributeWithData(newObjAttrName, attrTypeDef, newAttrDataNST);

        newAttrDataNST.release();
        fullLinkAndAttrNST.release();
        linkAttrNST.release();
        linksNST.release();
    }

    /**
     * Gets the id's that have at least one attribute defined
     * If attrsToConsider is empty, all attributes are used.
     *
     * @param attributes     - an Attributes object whose attribute data will determine which id's to get
     * @param itemsFilterNST
     * @return an NST with unique ids / at least one attribute from the member variable attrsToConsider,
     *         whose value in the given Attributes object.
     * @
     */
    private NST getIdsWithAtLeastOneAttrDefined(Attributes attributes, NST itemsFilterNST) {
        List attrNames = attrsToConsider.isEmpty() ? attributes.getAttributeNames() : attrsToConsider;

        // loop over all the attributes
        NST idsWithAtLeastOneAttrDefinedNST = null;
        Iterator attrNameIter = attrNames.iterator();
        while (attrNameIter.hasNext()) {
            String attrName = (String) attrNameIter.next();
            NST attrDataNST = attributes.getAttrDataNST(attrName).project("id");
            NST idsForAttrNST = attrDataNST.distinct("id");

            // unions the id's for the current attribute to the id's found for previous attributes
            if (idsWithAtLeastOneAttrDefinedNST == null) {
                idsWithAtLeastOneAttrDefinedNST = idsForAttrNST;
            } else {
                NST newNST = idsWithAtLeastOneAttrDefinedNST.union(idsForAttrNST, "id");
                idsWithAtLeastOneAttrDefinedNST.release();
                idsWithAtLeastOneAttrDefinedNST = newNST;
            }
        }

        if (idsWithAtLeastOneAttrDefinedNST == null) {
            idsWithAtLeastOneAttrDefinedNST = new NST("id", "oid");
        }

        if (itemsFilterNST != null) {
            NST filteredNST = idsWithAtLeastOneAttrDefinedNST.intersect(itemsFilterNST, "id EQ id");
            idsWithAtLeastOneAttrDefinedNST.release();
            return filteredNST;
        } else {
            return idsWithAtLeastOneAttrDefinedNST;
        }
    }

    private static DataTypeEnum getDataType(Object newAttrValue) {
        DataTypeEnum attrDataType = null;
        if (newAttrValue instanceof Double) {
            attrDataType = DataTypeEnum.DBL;
        } else if (newAttrValue instanceof Integer) {
            attrDataType = DataTypeEnum.INT;
        } else if (newAttrValue instanceof Float) {
            attrDataType = DataTypeEnum.FLT;
        } else if (newAttrValue instanceof String) {
            attrDataType = DataTypeEnum.STR;
        } else if (newAttrValue instanceof Long) {
            attrDataType = DataTypeEnum.LNG;
        } else {
            Assert.condition(false,
                    "Cannot determine attribute type for type " +
                            newAttrValue.getClass());
        }
        return attrDataType;
    }

    private void makeAttrFromNST(NST finalNST, String idCol, String valCol,
                                 String newAttrName, Attributes attributes) {

        NST fromNST = finalNST.project(idCol + "," + valCol);
        String attrType = finalNST.getNSTColumn(valCol).getType().toString();

        if (!attributes.isAttributeDefined(newAttrName)) {

            attributes.defineAttributeWithData(newAttrName, attrType, fromNST);

        } else {
            NST attrDataNST = attributes.getAttrDataNST(newAttrName);
            String type = attrDataNST.getNSTColumn("value").getType().toString();

            Assert.condition(keepPrevAttrVals, newAttrName + " is already defined." +
                    "Remove first, or use keepPrevVals flag if you want to add to the existing values.");
            Assert.condition(type.equals(attrType),
                    "Error: attribute already defined with type " + type);

            attrDataNST.insertRowsFromNST(fromNST);
        }

        fromNST.release();
    }


    /**
     * Creates an NST for an aggregation (e.g. count(a))
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param attrName       - name of the attribute to be aggregated
     * @param aggregate      - string format of the aggregation
     * @param itemsFilterNST
     * @return a (id, value) NST for the aggregation
     * @
     */
    private NST nstForAggregate(Attributes attributes, String attrName,
                                String aggregate, NST itemsFilterNST) {
        NST attributeNST = nstForAttribute(attributes, attrName, itemsFilterNST);
        NST aggregateNST = attributeNST.aggregate(aggregate, "id", "value");
        attributeNST.release();
        return aggregateNST;
    }

    /**
     * Creates an NST for an attribute (e.g. a)
     *
     * @param attributes    - contains attribute information for computing new attribute
     * @param attrName      - name of the attribute to get data for
     * @param itemFilterNST
     * @return a (id, value) NST for the attribute
     * @
     */
    private NST nstForAttribute(Attributes attributes, String attrName, NST itemFilterNST) {
        NST attrDataNST = attributes.getAttrDataNST(attrName);
        // filters attribute by items if items given
        if (itemFilterNST != null) {
            attrDataNST = attrDataNST.intersect(itemFilterNST, "id EQ id");
        }
        return attrDataNST;
    }


    /**
     * Creates an NST for an expression (e.g. sum(a) * b)
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param expression     - string format of the expression
     * @param itemsFilterNST
     * @return a (id, value) NST for the expression
     * @
     */
    private NST nstForExpression(Attributes attributes, String expression, NST itemsFilterNST) {
        Matcher matcher = ARITHMETIC_PATTERN.matcher(expression);

        // Expression is either the combination of two terms using arithmetic
        // or an individual term
        if (matcher.matches()) {
            matcher = TERM_PATTERN.matcher(expression);
            int start = 0;

            // find term1
            matcher.find(start);
            String term1 = matcher.group();
            start = matcher.end();

            // find operator
            Matcher matcher2 = OPERATOR_PATTERN.matcher(expression);
            matcher2.find(start);
            start = matcher2.end();
            String operator = matcher2.group();

            // find term2
            matcher.find(start);
            String term2 = matcher.group();

            // find the types
            NST term1NST = nstForTerm(attributes, term1, itemsFilterNST);
            NST term2NST = nstForTerm(attributes, term2, itemsFilterNST);
            NST arithNST = term1NST.join(term2NST, "id EQ id");
            arithNST.addArithmeticColumn("A.value " + operator + " B.value", null, "value"); // null ==> ask Monet to compute type of new column
            arithNST.renameColumn("A.id", "id");
            NST retNST = arithNST.project("id, value");

            term1NST.release();
            term2NST.release();
            arithNST.release();
            return retNST;
        }

        return nstForTerm(attributes, expression, itemsFilterNST);
    }

    /**
     * Creates a NST for a literal (e.g. 3, "8", true, -5.4)
     *
     * @param attributes       - contains attribute information for computing new attribute
     * @param literal          - string format of the literal
     * @param itemsFilteredNST
     * @return a (id, value) NST for the literal whose id's have at least one attribute
     *         defined in the given Attributes object
     * @
     */
    private NST nstForLiteral(Attributes attributes, String literal, NST itemsFilteredNST) {
        NST nstForLiteral = getIdsWithAtLeastOneAttrDefined(attributes, itemsFilteredNST);
        nstForLiteral.addConstantColumn("value", literal);
        return nstForLiteral;
    }

    /**
     * Creates a NST for a clause of a switch statement (e.g. a > b ==> 3)
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param statement      - string format of the clause
     * @param itemsFilterNST
     * @return a (id, value) NST for the clause
     * @
     */
    private NST nstForStatement(Attributes attributes, String statement,
                                NST itemsFilterNST) {
        Matcher matcher = CONDITION_PATTERN.matcher(statement);
        matcher.find();

        // process the corresponding condition
        String condition = matcher.group();
        NST conditionNST = nstMatchesForCondition(attributes, condition, itemsFilterNST);
        int start = matcher.end();

        // eat up next symbols
        matcher = Pattern.compile("\\s+==>\\s+").matcher(statement);
        matcher.find(start);
        start = matcher.end();

        matcher = EXPRESSION_PATTERN.matcher(statement);
        matcher.find(start);

        // process the corresponding expression
        // but only on those items that match the condition!
        String expression = matcher.group();
        NST localFilterNST = conditionNST.intersect(itemsFilterNST, "id EQ id");
        NST exprNST = nstForExpression(attributes, expression, localFilterNST);
        localFilterNST.release();

        return exprNST;
    }

    /**
     * Creates an NST for a switch statement (e.g. a > b ==> 3, g = 3 AND h = 5 ==> 2, default ==> 9)
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param switchStr      - string format of the switch statement
     * @param itemsFilterNST
     * @return a (id, value) NST for the switch statement
     * @
     */
    private NST nstForSwitch(Attributes attributes, String switchStr,
                             NST itemsFilterNST) {
        NST switchNST = null;
        NST itemIDNST;
        itemIDNST = getIdsWithAtLeastOneAttrDefined(attributes, itemsFilterNST);

        Matcher matcher = STATEMENT_PATTERN.matcher(switchStr);

        // loop over all the clauses of the switch statement
        int start = 0;
        while (matcher.find(start)) {
            String statement = matcher.group();
            log.debug("STATEMENT: " + statement);

            //if statement ends in a comma, lop it off:
            if (statement.endsWith(",")) {
                statement = statement.substring(0, statement.length() - 1);
            }

            NST statementNST = nstForStatement(attributes, statement, itemIDNST);

            // remove from the list of itemIDs the items that met the first condition
            NST newItemNST = itemIDNST.difference(statementNST, "id EQ id");
            itemIDNST.release();
            itemIDNST = newItemNST;

            // update the NST that will eventually be returned
            if (switchNST == null) {
                switchNST = statementNST;
            } else {
                switchNST.insertRowsFromNST(statementNST);
                statementNST.release();
            }

            start = matcher.end();
        }

        if (switchNST == null) {
            throw new IllegalArgumentException("No match found in switchStr");
        }

        // checks to see if there is a default clause, and handles it
        matcher = DEFAULT_PATTERN.matcher(switchStr);

        if (matcher.find(start)) {
            String defaultStr = matcher.group();

            matcher = Pattern.compile("default\\s+==>\\s+").matcher(defaultStr);

            matcher.find();
            start = matcher.end();

            matcher = EXPRESSION_PATTERN.matcher(defaultStr);

            matcher.find(start);

            // default clause adds the value of its corresponding expression
            // to all items not already processed by the switch statement
            String expression = matcher.group();
            NST exprNST = nstForExpression(attributes, expression, itemIDNST);
            switchNST.insertRowsFromNST(exprNST);
            exprNST.release();
        }

        itemIDNST.release();

        return switchNST;
    }

    /**
     * Creates a NST for a term (e.g. a, 3, sum(b))
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param term           - string format of the term
     * @param itemsFilterNST
     * @return a (id, value) NST for the term
     * @
     */
    private NST nstForTerm(Attributes attributes, String term, NST itemsFilterNST) {
        // Is the term a simple number?
        Matcher matcher = LITERAL_PATTERN.matcher(term);
        if (matcher.matches()) {
            return nstForLiteral(attributes, term, itemsFilterNST);
        }

        // Is the term an aggregate call?
        matcher = AGGREGATE_PATTERN.matcher(term);

        if (matcher.matches()) {
            String aggregate = matcher.group(1);
            String attrName = matcher.group(2);
            return nstForAggregate(attributes, attrName, aggregate, itemsFilterNST);
        }

        // Should match an attribute
        matcher = ATTRIBUTE_PATTERN.matcher(term);
        if (matcher.matches()) {
            return nstForAttribute(attributes, term, itemsFilterNST);
        }

        return null;
    }

    /**
     * Creates an NST for a comparison (e.g. a = "X")
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param comparison     - string format of the comparison
     * @param itemsFilterNST
     * @return an NST with a list of IDs matching a comparison
     * @
     */
    private NST nstMatchesForComparison(Attributes attributes, String comparison,
                                        NST itemsFilterNST) {
        // extract the two expressions of the comparison
        Matcher matcher = EXPRESSION_PATTERN.matcher(comparison);
        int start = 0;

        // get the first expression
        matcher.find(start);
        String expression1 = matcher.group();
        start = matcher.end();

        // get comparator
        Matcher matcher2 = COMPARATOR_PATTERN.matcher(comparison);
        matcher2.find(start);
        String comparator = matcher2.group();

        // get the second expression
        start = matcher2.end();
        matcher.find(start);
        String expression2 = matcher.group();

        // process the expressions
        NST expr1NST = nstForExpression(attributes, expression1, itemsFilterNST);
        NST expr2NST = nstForExpression(attributes, expression2, itemsFilterNST);

        // convert types for comparisons, if necessary
        DataTypeEnum expr1Type = expr1NST.getNSTColumn("value").getType();
        DataTypeEnum expr2Type = expr2NST.getNSTColumn("value").getType();
        if (expr1Type == DataTypeEnum.DBL && (expr2Type == DataTypeEnum.FLT || expr2Type == DataTypeEnum.INT)) {
            expr2NST.castColumn("value", "dbl");
        } else if (expr2Type == DataTypeEnum.DBL && (expr1Type == DataTypeEnum.FLT || expr1Type == DataTypeEnum.INT)) {
            expr1NST.castColumn("value", "dbl");
        } else if (expr1Type == DataTypeEnum.FLT && expr2Type == DataTypeEnum.INT) {
            expr2NST.castColumn("value", "flt");
        } else if (expr2Type == DataTypeEnum.FLT && expr1Type == DataTypeEnum.INT) {
            expr1NST.castColumn("value", "flt");
        }

        NST compNST = expr1NST.join(expr2NST, "A.id EQ B.id");
        compNST.addArithmeticColumn("A.value " + comparator + " B.value", "bit", "value");
        NST retNST = compNST.filter("value EQ 'true'", "A.id");
        retNST.renameColumn("A.id", "id");

        expr1NST.release();
        expr2NST.release();
        compNST.release();

        return retNST;
    }

    /**
     * Creates a NST for a condition (e.g. a > b AND sum(c) != 3)
     *
     * @param attributes     - contains attribute information for computing new attribute
     * @param condition      - string format of the condition
     * @param itemsFilterNST
     * @return an NSt with IDs for matches to a complex ANDed condition
     * @
     */
    private NST nstMatchesForCondition(Attributes attributes, String condition,
                                       NST itemsFilterNST) {
        Matcher matcher = COMPARISON_PATTERN.matcher(condition);
        int start = 0;
        NST conditionNST = null;

        // loop over all the comparisons in the condition
        while (matcher.find(start)) {
            String comparison = matcher.group();
            NST matchesNST = nstMatchesForComparison(attributes, comparison, itemsFilterNST);

            // AND the comparisons together
            if (conditionNST == null) {
                conditionNST = matchesNST;
            } else {
                NST newNST = conditionNST.intersect(matchesNST, "id EQ id");
                conditionNST.release();
                conditionNST = newNST;
            }
            start = matcher.end();

            // just to be more safe, eat up the AND explicitly
            Pattern pattern2 = Pattern.compile("\\s+AND\\s+");
            Matcher matcher2 = pattern2.matcher(condition);
            if (!matcher2.find(start)) {
                break;
            }
            start = matcher2.end();
        }

        return conditionNST;
    }
}
