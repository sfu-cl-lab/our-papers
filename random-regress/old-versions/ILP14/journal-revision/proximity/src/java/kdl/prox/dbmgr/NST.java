/**
 * $Id: NST.java 3784 2007-11-19 19:43:06Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.dbmgr;

import kdl.prox.db.DB;
import kdl.prox.monet.Connection;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Represents our "Named Synchronized Tables" idea in Monet. An NST is a BAT
 * whose head is a 'str' and whose tail is a 'bat'. The head contains strings
 * that name the 'columns' of the bat, and the tail contains column tables. The
 * BATs all have 'oid' head types that provide the synchronization between
 * them.  For example, all attribute NSTs (aka 'AttrNST') have this format:
 * <p/><pre>
 * str         |  bat
 * ------------+-----------
 * 'name'      | &lt;name-bat&gt;
 * 'data-type' | &lt;type-bat&gt;
 * 'data'      | &lt;data-bat&gt;
 * </pre><p/>
 * This tells us that AttrNSTs have three columns ('name', 'data-type', and
 * 'data'), which are stored in three BATs (&lt;name-bat&gt;, &lt;type-bat&gt;, &lt;data-bat&gt;).
 * <p/>
 * Another way to think of an NST is as an 'object' with 'slots' whose names
 * are in the head, and whose values are in the tail.
 * <p/>
 * The derived columns can be of any valid Monet type. Special care needs to be
 * taken for columns that in turn store other NSTs. Internally, nested NSTs are
 * stored as STR values, with Monet's tmp_* name in them. However, the NST class
 * needs to differentiate somehow between plain STRs and STRs for BATs. In particular:
 * * the NST class needs to be told which columns store BATs, so that
 * the insertRows method knows to insert into them the bbpname of the variable
 * passed in, as opposed to the delimited string of the variable name,
 * which is the way STRs are normally dealt with.
 * Do this via the setIsBATNameColum(true) method.
 * * when a value is read from one of those columns, the standard resultSet method
 * cannot be used, because it would return a string with the actual tmp_*
 * value, which may be different on other instances of Monet (we don't want
 * to use any tmp_* names in the MIL that we generate, so that the same MIL
 * can be run against different Monet databases). Instead of using resultSet,
 * call getColumnValueAsBATName with the rowID (the head value) and the
 * column name, to get the name of a variable that refers to the BAT
 * whose name is specified in the column.
 * <p/>
 * The insertRow() inserts values in each of the internal BATs, making sure that
 * the headOIDs are synchronized. Similarly, deleteRows() deletes rows from
 * all internal BATs, keeping the headOIDs in-synch. Finally, selectRows() selects
 * a subset of the rows of the NST by printing the join of the internal BATs.
 * <p/>
 *
 * @see Filter class for more information
 */
public class NST {

    private static Logger log = Logger.getLogger(NST.class);

    /**
     * A list of NSTColumn objects, with information about its data type and bat
     *
     * @see NSTColumn
     */
    protected List<NSTColumn> columnList = new ArrayList<NSTColumn>();

    /**
     * True if release() has been called. False o/w.
     */
    protected boolean isReleased = false;

    /**
     * The name of the BAT that saves the top-level structure of this NST, if saved
     */
    protected String topBATName = null;

    /**
     * A cache of two-column joins. Used mostly for two-column joins
     */
    String headColName;
    String tailColName;
    String cachedBAT;


    /**
     * Private empty constructor
     */
    protected NST() {
        Connection.registerInCurrentScope(this);
    }


    /**
     * Creates an NST based on a top-level BAT that exists on the DB
     *
     * @param topBATName
     * @
     */
    public NST(String topBATName) {
        initializeFromDB(topBATName);
        Connection.registerInCurrentScope(this);
    }

    /**
     * Creates an NST based on a specification of cols:types
     *
     * @param colNames
     * @param colTypes
     */
    public NST(String[] colNames, String[] colTypes) {
        // create BATs
        String[] colBATs = new String[colTypes.length];
        for (int i = 0; i < colTypes.length; i++) {
            String columnType = colTypes[i].trim();
            Assert.condition(MonetUtil.isMonetTypeDefined(columnType), "invalid Monet data type: -" + columnType + "-");
            colBATs[i] = MonetUtil.create("oid", columnType);
        }
        initializeFromArray(colBATs, colNames, colTypes);
        Connection.registerInCurrentScope(this);
    }

    /**
     * Overload that allows the list of names and of types in a single string.
     * Values are separated by commas, as in "id, value" and "oid, str"
     *
     * @param colNames
     * @param colTypes
     */
    public NST(String colNames, String colTypes) {
        this(colNames.split(","), colTypes.split(","));
    }

    /**
     * Creates and NST based on a list of BATs, with the cols:types specification
     *
     * @param colBATs
     * @param colNames
     * @param colTypes
     */
    public NST(String[] colBATs, String[] colNames, String[] colTypes) {
        initializeFromArray(colBATs, colNames, colTypes);
        Connection.registerInCurrentScope(this);
    }

    /**
     * Creates and NST based on a list of BATs, with the cols:types specification
     *
     * @param colBATs
     * @param colNames
     * @param colTypes
     */
    public NST(String[] colBATs, String colNames, String colTypes) {
        this(colBATs, colNames.split(","), colTypes.split(","));
    }

    /**
     * Creates an NST based on the head/tail of a BAT. It saves the passed in BAT
     * as the two-column cache (along wit the column names), and creates
     * the NSTColumn structures as 'delayed execution' columns, meaning that instead
     * of passing a BAT to save in the column, it passes an MIL command to execute
     * iif the column is used.
     * This leads to better performance, as the splitting of the original BAT into
     * head and tail columns is only done if/when necessary. Also, putting the
     * original BAT in the two-column cache increases performance if the user only
     * requests the two column BAT. In that case, the NST behaves just like a BAT,
     * which is hidden inside the cleaner NST interface.
     *
     * @param batName
     * @param col1Name
     * @param col2Name
     */
    public NST(String batName, String col1Name, String col2Name) {
        // create the columns (with delayed computation of the BATs)
        String col1BAT = Connection.reserveVarName();
        String col2BAT = Connection.reserveVarName();
        String headColCmd = batName + ".mark(0@0).reverse()";
        String tailColCmd = batName + ".reverse().mark(0@0).reverse()";
        columnList.add(new NSTColumn(col1Name, null, col1BAT, headColCmd));  // delayed execution
        columnList.add(new NSTColumn(col2Name, null, col2BAT, tailColCmd));  // delayed execution
        // and save in the cache
        cachedBAT = batName;
        headColName = col1Name;
        tailColName = col2Name;
        Connection.registerInCurrentScope(this);
    }

    /**
     * Methods to add a column to an NST (constant, the result of an OP, from a BAT, etc)
     * For most of these, there is an overload in which the DataTypeEnum comes as a String,
     * for convenience.
     */

    /**
     * Adds a new column that is the result of applying operator to inputColName using
     * monet's multiplex operator. The expression has the form of
     * origCol op [operand]
     * For example, valid expressions are
     * <p/>
     * year1 diff year2
     * date1 year
     * col1 + col2
     * col1 > col2
     * name + 'aaaa'
     * <p/>
     * Note: op1 MUST be a column name!
     *
     * @param expression expression
     * @param type       type of the new column; if null, the it will be computed from the new BAT
     * @param newcolName name of created column
     */
    public NST addArithmeticColumn(String expression, String type, String newcolName) {
        String[] cmds = expression.split("(\\s)+");
        Assert.condition(cmds.length == 3 || cmds.length == 2,
                "Incorrect expression format; should be op1 oper [op2]");

        String baseCol = cmds[0];
        String operator = cmds[1];

        NSTColumn inputCol = getNSTColumn(baseCol);
        StringBuffer milSB = new StringBuffer();
        milSB.append("[");
        milSB.append(operator);
        milSB.append("](");
        milSB.append(inputCol.getBATName());
        if (cmds.length == 3) {
            milSB.append(",");
            if (!Util.isValueArgument(cmds[2])) {
                milSB.append(getNSTColumn(cmds[2]).getBATName());
            } else {
                milSB.append(MonetUtil.delimitValue(Util.unQuote(cmds[2]), inputCol.getType()));
            }
        }
        milSB.append(")");
        //System.out.print("\tmil command: " + milSB.toString() + "\n"); //zzz
        String newBATName = Connection.executeAndSave(milSB.toString());
        if (type == null) {
            type = MonetUtil.getTailType(newBATName).toString();
        }
        return addColumnFromBATVarInternal(newBATName, newcolName, DataTypeEnum.enumForType(type));
    }

    public NST addColumnFromBATVar(String fromBATName, String newColName, String type) {
        String tailType = MonetUtil.getTailType(fromBATName).toString();
        Assert.condition(tailType.equals(type), "Trying to create a column of type " + type +
                " with a BAT of type " + tailType);
        return addColumnFromBATVarInternal(fromBATName, newColName, DataTypeEnum.enumForType(type));
    }

    /**
     * Adds a new column named resColName at the end of the list of columns from
     * the specified BAT.
     * <p/>
     * Adds a new NSTColumn to the list and keeps track of the BAT
     * that it has to delete when the NST is release'd(). It makes a new reference
     * to the passed in BAT in its own scope, to avoid accidental removals of the BAT
     * by calls to endScope (@see NSTTest#testAddColumnWithinScope() for a test)
     * <p/>
     * Doesn't check that the BAT exists or that the column name is not
     * already taken -- assumes that those values are correct.
     *
     * @param fromBATName
     * @param newColName
     * @param type
     */
    protected NST addColumnFromBATVarInternal(String fromBATName, String newColName,
                                              DataTypeEnum type) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.stringNotEmpty(newColName, "new column name is empty");
        Assert.condition(!isColumnExists(newColName), "column already exists: " + newColName);

        // add the new variable to the list, and to list of things to delete upon release()
        // we make a new reference to it WITH IsIGNORE on, so that it's not released accidentally by endScope()
        String colBatName = Connection.executeAndSave(fromBATName, true);
        columnList.add(new NSTColumn(newColName, type, colBatName));
        MonetUtil.makeBATWritable(colBatName);
        Connection.releaseSavedVar(fromBATName);
        return this;
    }

    /**
     * Adds a column with true/false values, depending on whether each row
     * matches the filterDef or not.
     *
     * @param filterDef
     * @param newColName
     * @return this NST
     */
    public NST addConditionColumn(String filterDef, String newColName) {
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        if (filterCmd == null) {
            return addConstantColumn(newColName, "bit", "true");
        } else {
            String baseCol = Connection.executeAndSave(getNSTColumn(0).getBATName() + ".project(false)");
            String matchCol = Connection.executeAndSave(filterCmd + ".project(true)");
            String newBatName = Connection.executeAndSave(matchCol + ".kunion(" + baseCol + ")");
            Connection.releaseSavedVar(baseCol);
            Connection.releaseSavedVar(matchCol);
            return addColumnFromBATVarInternal(newBatName, newColName, DataTypeEnum.BIT);
        }
    }

    /**
     * Adds a new column to the NST, of the specified type, and with all rows
     * with the same specified value.
     *
     * @param colName
     * @param type    -- null gets the type from Monet
     * @param value   -- null writes a nil
     */
    public NST addConstantColumn(String colName, String type, String value) {
        DataTypeEnum type1 = DataTypeEnum.enumForType(type);
        String operation = getNSTColumn(0).getBATName();
        if (value != null) {
            operation += ".project(" + MonetUtil.delimitValue(value, type1) + ")";
        } else {
            operation += ".project(" + type1.toString() + "(nil))";
        }

        String newCol = Connection.executeAndSave(operation);
        return addColumnFromBATVarInternal(newCol, colName, type1);
    }


    /**
     * Adds a constant column with the value already delimited to the type we want
     * So, for example, if you pass in 3.0, it creates a DBL.
     *
     * @param newColName
     * @param value
     */
    public NST addConstantColumn(String newColName, String value) {
        String operation = getNSTColumn(0).getBATName();
        operation += ".project(" + value + ")";
        String newCol = Connection.executeAndSave(operation);
        return addColumnFromBATVarInternal(newCol, newColName, MonetUtil.getTailType(newCol));
    }


    /**
     * Adds a new column with a copy of a given column.
     *
     * @param onColName
     * @param newColName
     * @
     */
    public NST addCopyColumn(String onColName, String newColName) {
        Assert.stringNotEmpty(onColName, "onColName empty");
        String newCol = Connection.executeAndSave(getNSTColumn(onColName).getBATName() + ".copy().access(BAT_WRITE)");
        return addColumnFromBATVarInternal(newCol, newColName, getNSTColumn(onColName).getType());
    }

    /**
     * Adds a column with a histogram of a given column.
     *
     * @param onColName
     * @param newColName
     * @
     */
    public NST addCountColumn(String onColName, String newColName) {
        Assert.stringNotEmpty(onColName, "onColName empty");
        String batName = getNSTColumn(onColName).getBATName();
        String operation = batName + ".join(" + batName + ".histogram())";
        String newCol = Connection.executeAndSave(operation);
        return addColumnFromBATVarInternal(newCol, newColName, DataTypeEnum.INT);
    }

    /**
     * Adds a column with the count of unique values in column changingColName
     * for each different value of baseColName. For example, suppose we have
     * two columns with the following values:
     * <p/><pre>
     * A   B
     * -----
     * 1   A
     * 1   B
     * 1   C
     * !   B
     * 2   X
     * 2   Y
     * 3   A
     * </pre><p/>
     * It will create a new column (colName) at the end of the NST with the
     * following values
     * <p/><pre>
     * A   B  cnt
     * ----------
     * 1   A    3
     * 1   B    3
     * 1   C    3
     * 1   B    3
     * 2   X    2
     * 2   Y    2
     * 3   A    1
     * </pre><p/>
     * It works by first putting in a single BAT the baseColName and
     * changingColName tails, then finding the distinct (baseColName, changingColName)
     * combinations, and then doing a histogram based on baseColName. Finally,
     * the resulting BAT with (baseColName | distinct count) is joined with
     * baseColName, so as to expand it and have a row for every row in the NST.
     * This join is added as a new column in the NST.
     *
     * @param baseColName
     * @param changingColName
     * @param colName
     * @
     */
    public NST addDistinctCountColumn(String baseColName, String changingColName,
                                      String colName) {
        Assert.stringNotEmpty(baseColName, "onColName empty");
        Assert.stringNotEmpty(changingColName, "changingColName empty");
        NSTColumn baseColumn = getNSTColumn(baseColName); //throws exception if not present
        NSTColumn changingColumn = getNSTColumn(changingColName); //throws exception if not present
        // get the two BATs and put their tails them in the same BAT
        // and then do a unique on the head and a histogram on the head also
        StringBuffer milSB = new StringBuffer();
        milSB.append(baseColumn.getBATName());
        milSB.append(".reverse().join(");
        milSB.append(changingColumn.getBATName());
        milSB.append(").sunique().reverse().histogram()");
        String resBAT = Connection.executeAndSave(milSB.toString());
        // and now join back with the first column, to get one row per row in the NST
        milSB = new StringBuffer();
        milSB.append(baseColumn.getBATName());
        milSB.append(".join(");
        milSB.append(resBAT);
        milSB.append(")");
        String joinedBAT = Connection.executeAndSave(milSB.toString());
        // and add this last BAT as a column
        addColumnFromBATVarInternal(joinedBAT, colName, DataTypeEnum.INT);
        Connection.releaseSavedVar(resBAT);

        return this;
    }

    /**
     * Adds a new column that stores the values of the key (head) of the BATs
     *
     * @param colName
     */
    public NST addKeyColumn(String colName) {
        String operation = getNSTColumn(0).getBATName() + ".mirror()";
        String newBATName = Connection.executeAndSave(operation);
        return addColumnFromBATVarInternal(newBATName, colName, DataTypeEnum.OID);
    }

    /**
     * Adds a column to the NST with a numbering 0-n (as oid)
     *
     * @param newColName
     */
    public NST addNumberColumn(String newColName) {
        return addNumberColumn(newColName, 0);
    }

    /**
     * Adds a column to the NST with a numbering start-n (as oid)
     *
     * @param newColName
     */
    public NST addNumberColumn(String newColName, int start) {
        String operation = getNSTColumn(0).getBATName() + ".mark(oid(" + start + "))";
        String newBATName = Connection.executeAndSave(operation);
        return addColumnFromBATVarInternal(newBATName, newColName, DataTypeEnum.OID);
    }

    /**
     * Adds a column to the NST with a random float number
     *
     * @param newColName
     */
    public NST addRandomColumn(String newColName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("[rnd](");
        milSB.append(getNSTColumn(0).getBATName());
        milSB.append(".project(0.0))");
        String randomBat = Connection.executeAndSave(milSB.toString());
        return addColumnFromBATVarInternal(randomBat, newColName, DataTypeEnum.FLT);
    }

    /**
     * Adds a new random binary column.
     *
     * @param newColName
     * @param threshold
     * @return
     */
    public NST addRandomBinaryColumn(String newColName, double threshold) {
        this.addRandomColumn("value");
        this.addArithmeticColumn("value > " + threshold, "bit", "more_than_half");
        this.castColumn("more_than_half", "int");
        this.removeColumn("value");
        this.renameColumn("more_than_half", newColName);
        return this;
    }

    /**
     * Adds a column to the NST with a random float number, and sorts by it
     *
     * @param newColName
     */
    public NST addRandomSortColumn(String newColName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("[rnd](");
        milSB.append(getNSTColumn(0).getBATName());
        milSB.append(").reverse().sort().reverse().mark(oid(0))");
        String newCol = Connection.executeAndSave(milSB.toString());
        // Remove the random sort column if it exists
        if (isColumnExists(newColName)) {
            removeColumn(newColName);
        }
        // add the BAT as a new column
        return addColumnFromBATVarInternal(newCol, newColName, DataTypeEnum.OID);
    }

    public NST addStringCleanupColumn(String colName, String newColName) {
        Assert.stringNotEmpty(colName, "empty colName");
        Assert.condition(getNSTColumn(colName).getType() == DataTypeEnum.STR, "Only valid with string columns");
        StringBuffer milSB = new StringBuffer();
        milSB.append("[substitute](");
        milSB.append(getNSTColumn(colName).getBATName());
        milSB.append(", \"\\n\", \"_\", true)");
        String newCol = Connection.executeAndSave(milSB.toString());
        return addColumnFromBATVarInternal(newCol, newColName, DataTypeEnum.STR);
    }

    /**
     * Adds a column to the NST with a substring of some other column
     *
     * @param colName
     * @param newColName
     * @param from       : starting position (from 0)
     * @param length     : number of chars to include
     * @return this NST
     */
    public NST addSubstringColumn(String colName, String newColName, int from, int length) {
        Assert.stringNotEmpty(colName, "empty colName");
        StringBuffer milSB = new StringBuffer();
        milSB.append("[string](");
        milSB.append(getNSTColumn(colName).getBATName());
        milSB.append(",");
        milSB.append(from);
        milSB.append(",");
        milSB.append(length);
        milSB.append(")");
        String newCol = Connection.executeAndSave(milSB.toString());
        return addColumnFromBATVarInternal(newCol, newColName, DataTypeEnum.STR);
    }

    /**
     * Returns a two-column NST with an aggregated value of valCol for each value of aggrCol.
     * For example, given the following NST:
     * <pre>
     *   A    B    C
     *   ------------
     *   1    a    10
     *   1    b    7
     *   2    a    13
     *   2    d    19
     *   2    e    1
     *   3    b    47
     * </pre>
     * A call to <code>aggregate("sum", "A", "C")</code> yields the following NST:
     * <pre>
     *   A    C
     *   -------
     *   1    17
     *   2    33
     *   3    47
     * </pre>
     * If a baseNST is provided, the resulting aggregate table will have one row
     * for each row in the aggrCol in the baseNST. This is used for cases where you
     * actually want an aggregate even if it's zero (for example, even if the value
     * if not present in this NST). In the example above, if baseNST was
     * <pre>
     *   A    D
     *   ------
     *   1    a
     *   1    b
     *   2    a
     *   2    d
     *   2    e
     *   3    b
     *   4    m
     * </pre>
     * A call to <code>aggregate("sum", "A", "C", baseNST)</code> yields the following NST:
     * <pre>
     *   A    C
     *   -------
     *   1    17
     *   2    33
     *   3    47
     *   4     0   --> notice that this row is not present in the original NST
     *
     * @param aggregateOp NB: can't use "histogram"
     * @param aggrCol
     * @param valCol
     * @return a two-column NST, with the grouping column and the group aggregates
     */
    public NST aggregate(String aggregateOp, String aggrCol, String valCol) {
        return aggregate(aggregateOp, aggrCol, valCol, null);
    }

    public NST aggregate(String aggregateOp, String aggrCol, String valCol, NST base) {
        Assert.stringNotEmpty(aggregateOp, "empty aggregateOp");
        Assert.stringNotEmpty(aggrCol, "empty aggrCol");
        Assert.stringNotEmpty(valCol, "empty valCol");
        String mainBatCmd = getTwoNSTColumnsAsCmd(aggrCol, valCol);
        String aggrBAT;
        if ("mode".equals(aggregateOp)) {
            Assert.condition(base == null, "mode does not support a base NST");
            // This is a special aggregator operation defined in init-mserver
            aggrBAT = Connection.executeAndSave("{mode_row}(" + mainBatCmd + ")");
        } else if (base != null) {
            String baseBatName = base.getNSTColumn(aggrCol).getBATName();
            aggrBAT = Connection.executeAndSave("{" + aggregateOp + "}(" + mainBatCmd + "," + baseBatName + ".reverse())");
        } else {
            aggrBAT = Connection.executeAndSave("{" + aggregateOp + "}(" + mainBatCmd + ")");
        }
        return new NST(aggrBAT, aggrCol, valCol);
    }

    public double avg(String colName) {
        Assert.condition(NSTUtil.isColumnNumeric(this, colName), "Cannot compute avf on a non-numeric column");
        return Double.parseDouble(Connection.readValue(getNSTColumn(colName).getBATName() + ".avg().print()"));
    }

    public double median(String colName) {
        Assert.condition(NSTUtil.isColumnNumeric(this, colName), "Cannot compute median on a non-numeric column");
        NST sorted = this.sort(colName, colName);
        List<Double> vals = sorted.selectRows().toDoubleList(colName);
        double ret;
        int size = vals.size();
        if (size % 2 == 0) {
            double ret1 = vals.get(size / 2 - 1);
            double ret2 = vals.get(size / 2);
            ret = (ret1 + ret2) / 2;
        } else {
            ret = vals.get((size - 1) / 2);
        }
        sorted.release();
        return ret;
    }

    public NST castColumn(String colName, String castType) {
        Assert.stringNotEmpty(colName, "onColName empty");
        Assert.notNull(castType, "null cast type");

        NSTColumn theColumn = getNSTColumn(colName);
        StringBuffer milSB = new StringBuffer();
        milSB.append("[");
        milSB.append(castType);
        milSB.append("](");
        milSB.append(theColumn.getBATName());
        milSB.append(")");
        String newBATName = Connection.executeAndSave(milSB.toString());
        // now release existing BAT, and replace with current one
        releaseColumnBAT(theColumn);
        int pos = columnList.indexOf(theColumn);
        columnList.remove(theColumn);
        columnList.add(pos, new NSTColumn(colName, DataTypeEnum.enumForType(castType), newBATName));
        return this;
    }


    /**
     * Returns a copy of this NST
     */
    public NST copy() {
        Assert.condition(!isReleased(), "illegal operation for released NST");

        // go through list of column names, and save them
        String[] colNames = new String[getColumnCount()];
        String[] colTypes = new String[getColumnCount()];
        String[] batNames = new String[getColumnCount()];
        for (int i = 0; i < getColumnCount(); i++) {
            NSTColumn nstColumn = getNSTColumn(i);
            colNames[i] = nstColumn.getName();
            colTypes[i] = nstColumn.getType().toString();
            batNames[i] = Connection.executeAndSave(nstColumn + ".copy().access(BAT_WRITE)");
        }

        // createTempNST with those BATs
        return new NST(batNames, colNames, colTypes);
    }


    /**
     * Helper method for JOIN methods
     * Given r1 and r2, two BATs that give a renumbering of the heads of two NSTs,
     * it adds to this NST columns coming from the respective NSTs, with their heads recoded
     * so that they are combined
     *
     * @param firstNST
     * @param secondNST
     * @param r1
     * @param r2
     * @param newColNames
     */
    private void copyJoinColumns(NST firstNST, NST secondNST, String r1, String r2, String[] newColNames) {
        for (int colIdx = 0; colIdx < newColNames.length; colIdx++) {
            String colName = newColNames[colIdx];
            DataTypeEnum colType;
            String colBAT;
            if (colIdx < firstNST.getColumnCount()) {
                NSTColumn parentCol = firstNST.getNSTColumn(colIdx);
                colType = parentCol.getType();
                colBAT = r1 + ".reverse().join(" + parentCol.getBATName() + ")";
            } else {
                NSTColumn parentCol = secondNST.getNSTColumn(colIdx - firstNST.getColumnCount());
                colType = parentCol.getType();
                colBAT = r2 + ".reverse().join(" + parentCol.getBATName() + ")";
            }
            columnList.add(new NSTColumn(colName, colType, colBAT));
        }
    }

    /**
     * Creates a new NST that is the cartesian product of two other NSTs
     *
     * @param otherNST
     */
    public NST cross(NST otherNST) {
        return cross(otherNST, "*");
    }

    public NST cross(NST otherNST, String colListSpec) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.notNull(otherNST, "Based on null second NST");

        NST crossNST = new NST();

        // create a BAT that
        // 1. does an cartesian join between the two NSTs, via a join and two theta-joins
        // 2. saves the mapping between heads in the two BATs
        String firstNSTBAT = getNSTColumn(0).getBATName();
        String secondNSTBAT = otherNST.getNSTColumn(0).getBATName();
        String joinBAT = Connection.executeAndSave(firstNSTBAT + ".mirror().cross(" + secondNSTBAT + ".mirror())");

        // join BAT has in the head the heads of the firstNST and in the
        // tail the corresponding tails of the second NST.
        // We need to renumber them, and create a BAT that contains
        // the heads of the first NST with their corresponding new number,
        // and the same for the heads of the second NST. We will call those
        // two BATs r1 and r2, respectively, and we will use them to get
        // the actual values from the original BATs
        String r1 = Connection.executeAndSave(joinBAT + ".mark(0@0)");
        String r2 = Connection.executeAndSave(joinBAT + ".reverse().mark(0@0)");

        // copy columns, add call to apply filter in BatName for each col
        crossNST.copyJoinColumns(this, otherNST, r1, r2, NSTUtil.combineJoinedNSTColNames(this, otherNST));

        // and get the ones we care about
        NST materializedNST = new NST();
        String[][] colNames = NSTUtil.colListToNewNameMap(crossNST, colListSpec);
        for (int colIdx = 0; colIdx < colNames.length; colIdx++) {
            String colName = colNames[colIdx][0];
            String newName = colNames[colIdx][1];
            NSTColumn parentCol = crossNST.getNSTColumn(colName);
            DataTypeEnum type = parentCol.getType();
            String columnBat = Connection.executeAndSave(parentCol.getBATName() + ".access(BAT_WRITE)");
            materializedNST.columnList.add(new NSTColumn(newName, type, columnBat));
        }

        crossNST.release();
        Connection.releaseSavedVar(joinBAT);
        Connection.releaseSavedVar(r1);
        Connection.releaseSavedVar(r2);

        return materializedNST;
    }

    public int count() {
        return getRowCount();
    }

    // Ignored
    public int count(String colName) {
        return count();
    }

    /**
     * Deletes an NST by making sure it is not persistent, and (if necessary)
     * recursively deleting all child BATs (for the case of NSTs).
     */
    public void delete() {
        if (topBATName == null) {
            return; // if not saved, there's nothing to delete
        }
        StringBuffer milSB = new StringBuffer();
        milSB.append("delete_nst(bat(");
        milSB.append(topBATName);
        milSB.append("));");
        Connection.executeQuery(milSB.toString());
        DB.commit();
        topBATName = null;
        release();
    }


    /**
     * Deletes rows with a filter, specified in a string
     *
     * @param filterDef (optional, "*" by default)
     */
    public NST deleteRows(String filterDef) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        invalidateCache();

        // Compute the external filter
        String filterBATName = null;
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        if (filterCmd != null) {
            filterBATName = Connection.executeAndSave(filterCmd);
        }
        // Go through the columns and delete from each BAT
        Iterator<NSTColumn> columnListIter = getNSTColumns().iterator();
        while (columnListIter.hasNext()) {
            NSTColumn thisColumn = columnListIter.next();
            // do delete
            StringBuffer milSB = new StringBuffer();
            milSB.append(thisColumn.getBATName());
            milSB.append(".access(BAT_WRITE).delete(");
            if (filterBATName != null) {
                milSB.append(filterBATName);
            }
            milSB.append(");");
            Connection.executeCommand(milSB.toString());
        }
        // Release the filter
        Connection.releaseSavedVar(filterBATName);

        return this;
    }

    public NST deleteRows() {
        return deleteRows("*");
    }


    /**
     * Returns a string describing the NST
     *
     * @return
     */
    public String describe() {
        StringBuffer sb = new StringBuffer();
        sb.append(NumberFormat.getInstance().format(getRowCount()));
        sb.append(" rows, ");
        sb.append(getColumnCount());
        sb.append(" columns [");
        sb.append(getNSTColumnNamesAsString());
        sb.append(":");
        sb.append(getNSTColumnTypesAsString());
        sb.append("] ");
        sb.append(topBATName == null ? "-- in memory " : "-- saved as " + topBATName);
        return sb.toString();
    }

    /**
     * Returns a subset of the NST such that the values of col1 are NOT in the col2 of an NST
     *
     * @param otherNST
     * @param filterDef (optional, "*" by default)
     * @return a filter of this NST with rows that are not in the other NST
     */
    public NST difference(NST otherNST, String filterDef, String colListSpec) {
        String[] joinCols = NSTUtil.getJoinColumns(filterDef);
        return filter(joinCols[0] + " NOTIN " + otherNST.getNSTColumn(joinCols[1]).getBATName(), colListSpec);
    }

    public NST difference(NST otherNST, String fiterDef) {
        return difference(otherNST, fiterDef, "*");
    }


    /**
     * Return a new NST where the tuples in the colums specified by keyColList are unique.
     *
     * @param keyColumnList (optional, "*" by default)
     * @return a filter of this NST with unique rows based on keyColumnList
     */
    public NST distinct(String keyColumnList) {
        String[] keyColumns = NSTUtil.colListToArray(this, keyColumnList);
        if (keyColumns.length == 0) {
            return this.copy();
        } else if (keyColumns.length == 1) {
            return this.filter(keyColumns[0] + " DISTINCT ROWS");
        } else if (keyColumns.length == 2 && getColumnCount() == 2) {
            String twoColsCmd = getTwoNSTColumnsAsCmd(keyColumns[0], keyColumns[1]);
            String distinctHeadTailValues = MonetUtil.getDistinctHeadTailValues(twoColsCmd);
            NST retNST = new NST(distinctHeadTailValues, keyColumns[0], keyColumns[1]);
            retNST.makeWritable();
            return retNST;
        } else {
            groupBy(keyColumnList, "union_grp");
            NST unique = this.filter("union_grp DISTINCT ROWS");
            if (unique.isColumnExists("union_grp")) {
                unique.removeColumn("union_grp");
            }
            this.removeColumn("union_grp");
            return unique;
        }
    }

    public NST distinct() {
        return distinct("*");
    }


    /**
     * Inserts rows from an array into the NST. The array is ordered by rows, with each column representing
     * a column in the NST (in the same order!)
     * It works by writing the array to disk, and then using the fromFile() method. Believe it or not, for large
     * arrays (50 rows or more) this is one or two orders of magnitude faster than doing repeated inserts!
     * <p/>
     * NOTE: This only works when the Monet server and the Java client are running on the same machine!
     * The file is saved in the temp/ directory, which the Monet server must of course have access to.
     *
     * @param data
     */
    public void fastInsert(Object[][] data) {
        invalidateCache();

        File temp = null;
        try {
            // Create temp file.
            temp = File.createTempFile("prox", null);
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            // Loop over the data and write
            for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                Object[] row = data[rowIdx];
                for (int colIdx = 0; colIdx < row.length; colIdx++) {
                    Object o = row[colIdx];
                    if (colIdx > 0) {
                        out.write("\t");
                    }
                    out.write(o.toString());
                }
                out.write("\n");
            }
            // close
            out.close();
            // and read
            this.fromfile(temp.getAbsolutePath());
        } catch (IOException e) {
            throw new MonetException("fastInsert failed with IOError: " + e);
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }


    /**
     * Returns a MATERIALIZED NST with the filter and set of columns and the selected rows.
     * <p/>
     * The filter is applied first. Then the resulting (filtered) NST is copy-d over with the
     * desired columns. Finally, if the rowList doesn't specify from,to, an order column is added to the
     * NST (random order or not) and a new filter is applied to the NST to keep the desired rows.
     *
     * @param filterDef
     * @param colListSpec (optional, "*" by default)
     * @return
     * @see kdl.prox.dbmgr.FilterFactory class for information about the filterDef
     * @see kdl.prox.dbmgr.NSTUtil#colListToArray(NST,String) method for information about the colList
     */
    public NST filter(String filterDef, String colListSpec) {
        NST nst = new NST();

        String filterApplyCmd;
        String filterBat = null;

        boolean isEmptyFilter = false;
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        if (filterCmd == null) {
            filterApplyCmd = ".copy()";
        } else {
            filterBat = Connection.executeAndSave(filterCmd);
            // If the filter is empty, then there's no need to perform all the semijoins
            if (MonetUtil.getRowCount(filterBat) == 0) {
                isEmptyFilter = true;
            }
            filterApplyCmd = ".semijoin(" + filterBat + ")";
        }

        String[][] colNames = NSTUtil.colListToNewNameMap(this, colListSpec);
        for (int colIdx = 0; colIdx < colNames.length; colIdx++) {
            String colName = colNames[colIdx][0];
            String newName = colNames[colIdx][1];
            NSTColumn parentCol = getNSTColumn(colName);
            DataTypeEnum type = parentCol.getType();
            String columnBat;
            if (!isEmptyFilter) {
                columnBat = Connection.executeAndSave(parentCol.getBATName() + filterApplyCmd + ".access(BAT_WRITE)");
            } else {
                // If the filter is empty, then there's no need to perform all the semijoins
                // Just create new BATs of the specific type
                // In addition, Monet sometimes screws up trying to create BATs with head=tail=void,
                // which cause problems. Creating new BATs manually avoids the problem
                columnBat = MonetUtil.create("oid", type.toString());
            }
            nst.columnList.add(new NSTColumn(newName, type, columnBat));
        }

        if (filterBat != null) {
            Connection.releaseSavedVar(filterBat);
        }
        return nst;
    }

    public NST filter(String filterDef) {
        return filter(filterDef, "*");
    }


    /**
     * Loads the contents of a file into an NST.
     * The NST must have been created with the appropriate columns and data types, of course, and
     * the file must have the same number of COLS as the NST.
     * This method APPENDs the new values, so if the NST already has some rows, they are
     * preserved.
     *
     * @param fileNameFullPath
     */
    public NST fromfile(String fileNameFullPath) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        invalidateCache();

        String[] selectColumns = NSTUtil.colListToArray(this, "*");
        if (selectColumns.length == 0) {
            return this;
        }

        // Create BATs for names, seps, bats,  which will be passed to
        String names = MonetUtil.create("void,str");
        String seps = MonetUtil.create("void,str");
        String types = MonetUtil.create("void,str");
        for (int i = 0; i < selectColumns.length; i++) {
            // Will throw exception if the column doesn't exist
            NSTColumn thisColumn = getNSTColumn(selectColumns[i]);
            StringBuffer milSB = new StringBuffer();
            milSB.append(names);
            milSB.append(".append(\"");
            milSB.append(thisColumn.getName());
            milSB.append("\");");
            if (i == (selectColumns.length - 1)) {
                milSB.append(seps);
                milSB.append(".append(\"\\n\");");
            } else {
                milSB.append(seps);
                milSB.append(".append(\"\\t\");");
            }
            milSB.append(types);
            milSB.append(".append(");
            milSB.append(MonetUtil.delimitValue(thisColumn.getType().toString(), DataTypeEnum.STR));
            milSB.append(");");
            Connection.executeCommand(milSB.toString());
        }

        // Now call the dump command
        StringBuffer milSB = new StringBuffer();
        milSB.append("load(");
        milSB.append(names);
        milSB.append(",");
        milSB.append(seps);
        milSB.append(",");
        milSB.append(types);
        milSB.append(",\"");
        milSB.append(Util.delimitBackslash(fileNameFullPath));
        milSB.append("\",");
        milSB.append("-1");
        milSB.append(")");
        String loadedBATs = Connection.executeAndSave(milSB.toString());

        // And now insert into existing BATs
        for (int i = 0; i < selectColumns.length; i++) {
            NSTColumn thisColumn = getNSTColumn(selectColumns[i]);
            milSB = new StringBuffer();
            milSB.append(thisColumn.getBATName());
            milSB.append(".append(");
            milSB.append(loadedBATs);
            milSB.append(".find(\"");
            milSB.append(thisColumn.getName());
            milSB.append("\")");
            milSB.append(")");
            Connection.executeCommand(milSB.toString());
        }
        Connection.releaseSavedVar(names);
        Connection.releaseSavedVar(seps);
        Connection.releaseSavedVar(types);
        Connection.releaseSavedVar(loadedBATs);

        return this;
    }


    public int getColumnCount() {
        Assert.condition(!isReleased(), "Invalid operation on a released NST");
        return getNSTColumns().size();
    }

    /**
     * Returns a resultSet with the histogram for a column. The head (col 0) contains
     * the distinct values, and the tail (1) the count
     *
     * @param colName
     */
    public ResultSet getColumnHistogram(String colName) {
        return getColumnHistogram(colName, false, false, false);
    }

    public ResultSet getColumnHistogram(String colName, boolean isSorted, boolean isSortedByFreq, boolean isSortedReverse) {
        String hist = getColumnHistogramBAT(colName, isSorted, isSortedByFreq, isSortedReverse);
        ResultSet resultSet = Connection.executeQuery(hist + ".print()");
        Connection.releaseSavedVar(hist);
        return resultSet;
    }


    /**
     * Returns an NST with the histogram for a column. It has two columns: colName, count
     *
     * @param colName
     */
    public NST getColumnHistogramAsNST(String colName) {
        return getColumnHistogramAsNST(colName, false, false, false);
    }

    public NST getColumnHistogramAsNST(String colName, boolean isSorted, boolean isSortedByFreq, boolean isSortedReverse) {
        String histBAT = getColumnHistogramBAT(colName, isSorted, isSortedByFreq, isSortedReverse);
        return new NST(histBAT, colName, "count");
    }

    private String getColumnHistogramBAT(String colName, boolean isSorted, boolean isSortedByFreq, boolean isSortedReverse) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(getNSTColumn(colName).getBATName());
        milSB.append(".histogram()");
        if (isSorted) {
            milSB.append(isSortedByFreq ? ".reverse()" : "");
            milSB.append(isSortedReverse ? ".sort_rev()" : ".sort()");
            milSB.append(isSortedByFreq ? ".reverse()" : "");
        }
        return Connection.executeAndSave(milSB.toString());
    }

    /**
     * Returns a Monet variable whose value is the name of some BAT.
     * This is used to get the names of the BATs for attrDataNST and Container Subgraphs
     * without passing their tmp_ names into Java. This, in turn, allows us to
     * generate MIL code without any references to tmp_ variables, and therefore
     * to generate a MIL script that can be executed against other databases.
     * The returned variable holds a str with the name of the tmp_ table;
     * it needs to be converted to bat() before it can be used.
     * The reference to this variable, which is hard to delete in the Container
     * class when returning the SubgAttrs (it's used to create the NST for the
     * Attribute object, which is then never released), is not registered in the
     * current scope, to avoid warning messages when the connection is closed.
     * <p/>
     * <p/>
     * See also setIsBATNameColumn
     *
     * @param colName
     * @param rowID
     */
    public String getColumnValueAsBATName(String colName, int rowID) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(getNSTColumn(colName).getBATName());
        milSB.append(".find(oid(");
        milSB.append(rowID);
        milSB.append("))");
        return Connection.executeAndSave(milSB.toString(), true);
    }

    public int getDistinctColumnValuesCount(String colName) {
        Assert.stringNotEmpty(colName, "Null column");
        return MonetUtil.getDistinctTailValuesRowCount(this.getNSTColumn(colName).getBATName());
    }

    public List<String> getDistinctColumnValues(String colName) {
        Assert.stringNotEmpty(colName, "Null column");
        String distinctTailValues = MonetUtil.getDistinctTailValues(this.getNSTColumn(colName).getBATName());
        List values = MonetUtil.read(distinctTailValues).toStringList(1);
        Connection.releaseSavedVar(distinctTailValues);
        return values;
    }

    public List getDistinctColumnValuesBinned(String colName, int numBins) {
        Assert.stringNotEmpty(colName, "Null column");
        return MonetUtil.read(this.getNSTColumn(colName) + ".bin(" + numBins + ")").toStringList(1);
    }


    /**
     * Finds the rows that match a join between two NSTs, and returns an NST listing the map
     *
     * @param otherNST
     * @param thisColNames
     * @param otherNSTColNames
     * @return an NST with columns [first, second], listing the headIDs of the matching rows from each NST
     */
    public NST getMatchingRows(NST otherNST,
                               String[] thisColNames,
                               String[] otherNSTColNames) {
        Assert.notNull(otherNST, "Based on null second NST");
        Assert.condition(thisColNames.length > 0, "firstColName empty");
        Assert.condition(thisColNames.length == otherNSTColNames.length,
                "number of columns to join on first NST not equal to number of " +
                        "columns to join on second NST: " +
                        thisColNames.length + " / " + otherNSTColNames.length);

        // get a mapping from the heads of the first NST to the heads of the
        // second NST where they will be joined
        String oidMapBAT = oidMapBAT(otherNST, thisColNames, otherNSTColNames, true);

        return new NST(oidMapBAT, "first", "second");
    }


    /**
     * Returns an NSTColumn from the list of columns in the NST
     *
     * @param columnIndex
     * @return
     * @
     */
    public NSTColumn getNSTColumn(int columnIndex) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        if (columnIndex >= getColumnCount()) {
            throw new MonetException("Invalid NSTColumn index: " + columnIndex);
        } else {
            return getNSTColumns().get(columnIndex);
        }
    }

    /**
     * Returns an NSTColumn from the list of columns in the NST
     *
     * @param columnName
     * @return
     * @
     */
    public NSTColumn getNSTColumn(String columnName) {
        for (int i = 0; i < getColumnCount(); i++) {
            NSTColumn thisColumn = getNSTColumn(i);
            if (thisColumn.getName().equalsIgnoreCase(columnName)) {
                return thisColumn;
            }
        }
        throw new MonetException("Invalid NSTColumn name: " + columnName + " (" + getNSTColumnNamesAsString() + ")");
    }

    /**
     * Returns a List of my column names.
     */
    public List getNSTColumnNames() {
        return new ArrayList(Arrays.asList(getNSTColumnNamesAsString().split(",")));
    }

    /**
     * Returns a String of comma-separated column names.
     */
    public String getNSTColumnNamesAsString() {
        return NSTUtil.getNSTColumnInfoAsString(this, "name");
    }

    /**
     * Returns a String of comma-separated column names.
     */
    public String getNSTColumnTypesAsString() {
        return NSTUtil.getNSTColumnInfoAsString(this, "type");
    }

    /**
     * Returns the list with the column info
     */
    public List<NSTColumn> getNSTColumns() {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        return columnList;
    }

    /**
     * Returns numRows selected at random from this NST. If numRows > this.getRowCount() all the rows are returned.
     *
     * @param baseColName
     * @param randomColName
     * @param numRows
     * @return
     */
    public NST getRandomAggregate(String baseColName, String randomColName, String numRows) {

        NSTColumn baseCol = this.getNSTColumn(baseColName);
        NSTColumn cntCol = this.getNSTColumn(randomColName);
        NST k = new NST("key", "oid");
        NSTColumn addCol = k.getNSTColumn("key");

        StringBuffer loopSB = new StringBuffer();
        loopSB.append("var x:=" + cntCol + ".semijoin(" + baseCol + ".select($h)).sample(" + numRows + ").mirror();");
        loopSB.append(addCol + ".insert(x);");
        //Want the unique values of the tail on baseCol
        MonetUtil.batLoop(baseCol + ".reverse().kunique()", loopSB.toString());

        return this.filter("xxx KEYIN " + (k.getNSTColumn("key")));
    }

    /**
     * Returns the number of rows in the NST
     *
     * @param filterDef (optional, "*" by default)
     * @return
     */
    public int getRowCount(String filterDef) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        return MonetUtil.getRowCount((filterCmd != null ? filterCmd : getNSTColumn(0).getBATName()));
    }

    public int getRowCount() {
        return getRowCount("*");
    }

    /**
     * Returns a BAT where the head is the first column and the tail is the second one
     * This method will cache the result (and save it in its own scope, so that it's not deleted accidentally).
     * Used in conjunction with the single-bat constructor, it can implement quick two-column bats).
     *
     * @param col1
     * @param col2
     * @see NST#NST(String,String,String)
     */
    private String getTwoNSTColumnsAsCmd(String col1, String col2) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        if (cachedBAT != null && col1.equals(headColName) && col2.equals(tailColName)) {
            return cachedBAT;
        } else if (cachedBAT != null && col1.equals(tailColName) && col2.equals(headColName)) {
            return cachedBAT + ".reverse()";
        } else {
            String bat1 = getNSTColumn(col1).getBATName();  // throws exception if not found
            String bat2 = getNSTColumn(col2).getBATName();  // throws exception if not found
            StringBuffer milSB = new StringBuffer();
            if (col1.equalsIgnoreCase(col2)) {
                milSB.append(bat1);
                milSB.append(".reverse().mirror()");
            } else {
                milSB.append(bat1);
                milSB.append(".reverse().join(");
                milSB.append(bat2);
                milSB.append(")");
            }

            String twoColBAT = milSB.toString();
            if (cachedBAT == null) {
                cachedBAT = Connection.executeAndSave(twoColBAT, true); // save this outside of the current Connection.scope!
                headColName = col1;
                tailColName = col2;
                return cachedBAT;
            }
            return twoColBAT;
        }
    }

    /**
     * Public method to get two columns.
     *
     * @param col1
     * @param col2
     * @return
     */
    public String getTwoNSTColumns(String col1, String col2) {
        return getTwoNSTColumnsAsCmd(col1, col2);
    }


    /**
     * Adds a column at the end of the NST named newColName in which
     * different values correspond to different values in the specified columns
     * <p/>
     * If the newColName column already exists, it deletes it.
     * <p/>
     *
     * @param colList
     * @param newColName (optional, group_id by default)
     */
    public NST groupBy(String colList, String newColName) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.stringNotEmpty(colList, "empty colList");

        // Originally, this method used successive calls to CTgroup
        // CTGroup works great when the expected number of groups is small
        // and it fits in the CPU cache. If the number of groups is not small
        // then CTgroup is very inefficient, and CTrefine is much much faster.
        // However, CTrefine expects the grouping BAT to be tail-sorted
        // (From the Monet docs:
        // refine the ordering of a tail-ordered BAT by sub-ordering on the
        // values of a second bat 'a' (where the heads of a and b match 1-1).

        // go through list of column names, and create the group_by BAT
        String grouping = "";
        String[] columnNames = NSTUtil.colListToArray(this, colList);
        for (int i = 0; i < columnNames.length; i++) {
            String batName = getNSTColumn(columnNames[i]).getBATName();
            if (i == 0) {
                grouping = Connection.executeAndSave(batName + ".prox_group()");
            } else {
                Connection.executeCommand(grouping + ":=" + grouping + ".reverse().sort().reverse().CTrefine(" + batName + ")");
            }
        }

        // Remove the group_by column if it exists
        if (isColumnExists(newColName)) {
            removeColumn(newColName);
        }

        // add the BAT as a new column
        return addColumnFromBATVarInternal(grouping, newColName, DataTypeEnum.OID);
    }

    public NST groupBy(String colList) {
        return groupBy(colList, "group_id");
    }


    private void initializeFromArray(String[] colBATs, String[] colNames, String[] colTypes) {
        Assert.condition(colNames.length == colTypes.length, "Array sizes different");
        Assert.condition(colNames.length == colBATs.length, "Array sizes different");

        // create columns
        columnList.clear();
        for (int i = 0; i < colNames.length; i++) {
            String colName = colNames[i].trim();
            DataTypeEnum colType = (colTypes[i] == null ? null : DataTypeEnum.enumForType(colTypes[i].trim()));
            String colBAT = colBATs[i];
            columnList.add(new NSTColumn(colName, colType, colBAT));
        }
        Assert.condition(getColumnCount() > 0, "NST has no columns");
    }

    private void initializeColumnsFromBAT(String topBATName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("[tail]([bat](bat(");
        milSB.append(this.topBATName);
        milSB.append("))).print()");
        ResultSet resultSet;
        try {
            resultSet = Connection.executeQuery(milSB.toString());
        } catch (MonetException e) {
            if (!MonetUtil.isBATExists(topBATName)) {
                throw new IllegalArgumentException("NST doesn't exist: " + topBATName);
            } else {
                throw e;
            }
        }

        int colIdx = 0;
        while (resultSet.next()) {
            String columnName = resultSet.getString(0);
            DataTypeEnum columnType = DataTypeEnum.enumForType(resultSet.getString(1));
            StringBuffer colBuffer = new StringBuffer();
            colBuffer.append("bat(bat(");
            colBuffer.append(this.topBATName);
            colBuffer.append(").fetch(");
            colBuffer.append(colIdx++);
            colBuffer.append("))");
            columnList.add(new NSTColumn(columnName, columnType, colBuffer.toString()));
        }
    }

    /**
     * initializes column list from db.
     * <p/>
     * Called from the constructor
     *
     * @param topBATName
     * @
     */
    private void initializeFromDB(String topBATName) {
        Assert.stringNotEmpty(topBATName, "empty topBATName");
        this.topBATName = NSTUtil.normalizeName(topBATName);

        // Clear list of columns, and read NST top-level BAT and, for each row, create a col
        // we also get the tail types for each column
        columnList.clear();
        if (DB.OBJ_NST_NAME.equals(topBATName)) {
            initializeColumnsFromCore(NSTTypeEnum.OBJ_NST_COL_NAMES, NSTTypeEnum.OBJ_NST_COL_TYPES);
        } else if (DB.LINK_NST_NAME.equals(topBATName)) {
            initializeColumnsFromCore(NSTTypeEnum.LINK_NST_COL_NAMES, NSTTypeEnum.LINK_NST_COL_TYPES);
        } else if (DB.CONT_NST_NAME.equals(topBATName)) {
            initializeColumnsFromCore(NSTTypeEnum.CONT_NST_COL_NAMES, NSTTypeEnum.CONT_NST_COL_TYPES);
        } else if (DB.OBJ_ATTR_NST_NAME.equals(topBATName) ||
                DB.LINK_ATTR_NST_NAME.equals(topBATName) ||
                DB.CONT_ATTR_NST_NAME.equals(topBATName)) {
            initializeColumnsFromCore(NSTTypeEnum.ATTR_NST_COL_NAMES, NSTTypeEnum.ATTR_NST_COL_TYPES);
        } else {
            initializeColumnsFromBAT(topBATName);
        }
    }

    private void initializeColumnsFromCore(String[] colNames, String[] colTypes) {
        for (int colIdx = 0; colIdx < colNames.length; colIdx++) {
            String columnName = colNames[colIdx];
            DataTypeEnum columnType = DataTypeEnum.enumForType(colTypes[colIdx]);
            // we could store this in a Monet variable with executeAndSave, which would
            // make the MIL code look cleaner... but: even persistent NSTs would now
            // have internal variables and need to be released. We'd decided to give up
            // cleanliness in the MIL code for extra convenience in the API.
            StringBuffer colBuffer = new StringBuffer();
            colBuffer.append("bat(bat(");
            colBuffer.append(this.topBATName);
            colBuffer.append(").fetch(");
            colBuffer.append(colIdx);
            colBuffer.append("))");
            columnList.add(new NSTColumn(columnName, columnType, colBuffer.toString()));
        }
    }

    /**
     * Inserts a row into my tables, using the elements in values. The head OID
     * in all the tables in the next available OID.
     *
     * @param values (either as an array of strings, or a comma-separated string)
     * @return this NST
     * @
     */
    public NST insertRow(String values) {
        invalidateCache();

        String[] valArray;
        if (getColumnCount() == 1) {
            // faster, no need to split the strings
            valArray = new String[]{Util.unQuote(values.trim())};
        } else {
            // Convert comma-separated list to array
            List valList = Util.splitQuotedString(values, ',');
            valArray = new String[valList.size()];
            for (int valIdx = 0; valIdx < valList.size(); valIdx++) {
                String val = (String) valList.get(valIdx);
                valArray[valIdx] = Util.unQuote(val.trim());
            }
        }
        return insertRow(valArray);
    }

    public NST insertRow(String[] values) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.condition(values.length == getColumnCount(), "# values != # columns in NST insertRow");
        invalidateCache();

        // Go through the columns, and insert one at a time
        StringBuffer milSB = new StringBuffer();
        for (int i = 0; i < getColumnCount(); i++) {
            NSTColumn thisColumn = getNSTColumn(i);
            milSB.append(thisColumn.getBATName());
            milSB.append(".append(");
            milSB.append(MonetUtil.delimitValue(values[i], thisColumn.isDelimitAsBATName() ? DataTypeEnum.BAT : thisColumn.getType()));
            milSB.append("); ");
        }
        Connection.executeCommand(milSB.toString());

        return this;
    }

    /**
     * Inserts a set of rows into my tables.
     *
     * @param rows
     * @return this NST
     */
    public NST insertRows(List rows) {
        for (int valueIdx = 0; valueIdx < rows.size(); valueIdx++) {
            insertRow((String) rows.get(valueIdx));
        }
        return this;
    }

    /**
     * Gets all the rows from the second NST and inserts them into me.
     * Recodes the heads of the otherNST, to avoid duplicates
     * Other NST must have the same number of columns (and types) as me
     *
     * @param otherNST
     * @return this NST
     */
    public NST insertRowsFromNST(NST otherNST) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.notNull(otherNST, "null otherNST");
        invalidateCache();

        Assert.condition(getColumnCount() == otherNST.getColumnCount(), "otherNST doesn't have the same number of columns");
        for (int i = 0; i < getColumnCount(); i++) {
            NSTColumn thisColumnI = getNSTColumn(i);
            NSTColumn otherColumnI = otherNST.getNSTColumn(i);
            Assert.condition(thisColumnI.getType() == otherColumnI.getType(), "column types don't match");
        }

        // Go through all the columns and insert one at a time
        for (int i = 0; i < getColumnCount(); i++) {
            StringBuffer milSB = new StringBuffer();
            NSTColumn thisColumn = getNSTColumn(i);
            NSTColumn otherColumn = otherNST.getNSTColumn(i);
            milSB.append(thisColumn.getBATName());
            milSB.append(".addend(");
            milSB.append(otherColumn.getBATName());
            milSB.append(")");
            Connection.executeCommand(milSB.toString());
        }

        return this;
    }

    /**
     * Returns a subset of the NST such that the values of col1 are in the col2 of an NST
     *
     * @param otherNST
     * @param fiterDef
     * @return a filter of this NST such  that all rows are in the other NST
     */
    public NST intersect(NST otherNST, String fiterDef) {
        return intersect(otherNST, fiterDef, "*");
    }

    public NST intersect(NST otherNST, String filterDef, String colListSpec) {
        String[] filterDefs = filterDef.split(" AND ");
        if (filterDefs.length == 1) {

            String[] joinCols = NSTUtil.getJoinColumns(filterDef);
            return filter(joinCols[0] + " IN " + otherNST.getNSTColumn(joinCols[1]).getBATName(), colListSpec);

        } else {

            // Uses Monet's ds_link method to perform a paired join of two BATs
            // Used instead of regular NST joins on two columns because it's
            // more efficient in terms of memory management.
            // ds_link computes hash-value for the combination of each BATs
            // and then perfoms the join over that.
            String batWithKeys = MonetUtil.create("bat, bat");

            StringBuffer milSB = new StringBuffer();
            milSB.append(batWithKeys);
            for (int i = 0; i < filterDefs.length; i++) {
                String joinCols[] = NSTUtil.getJoinColumns(filterDefs[i]);
                milSB.append(".insert(");
                milSB.append(this.getNSTColumn(joinCols[0]));
                milSB.append(",");
                milSB.append(otherNST.getNSTColumn(joinCols[1]));
                milSB.append(")");
            }
            Connection.executeCommand(milSB.toString());

            String keyBatName = Connection.executeAndSave("ds_link(" + batWithKeys + ").reverse();");
            NST filteredNST = this.filter("xx KEYIN " + keyBatName, colListSpec);
            Connection.releaseSavedVar(keyBatName);
            Connection.releaseSavedVar(batWithKeys);

            return filteredNST;

        }
    }


    /**
     * Clears the cache. Must be called from methods that render the cache invalid
     * If the cache stores columns of delayed execution, they must be computed
     * before the cache is released!
     */
    private void invalidateCache() {
        if (cachedBAT != null) {
            if (getNSTColumn(headColName).isDelayedExecution()) {
                getNSTColumn(headColName).getBATName(); // compute it
            }
            if (getNSTColumn(tailColName).isDelayedExecution()) {
                getNSTColumn(tailColName).getBATName(); // compute it
            }
            Connection.releaseSavedVar(cachedBAT);
        }
        cachedBAT = null;
        headColName = null;
        tailColName = null;
    }


    public boolean isColumnExists(String colName) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        for (int i = 0; i < getColumnCount(); i++) {
            NSTColumn thisColumn = getNSTColumn(i);
            if (thisColumn.getName().equals(colName)) {
                return true;
            }
        }
        return false;
    }


    public boolean isReleased() {
        return isReleased;
    }

    /**
     * A multi-column join
     *
     * @param otherNST
     * @param firstColNames
     * @param secondColNames
     * @param colListSpec
     * @return
     */
    public NST join(NST otherNST, String[] firstColNames, String[] secondColNames, String colListSpec) {
        Assert.notNull(otherNST, "Based on null second NST");
        Assert.condition(firstColNames.length > 0, "firstColName empty");
        Assert.condition(firstColNames.length == secondColNames.length,
                "number of columns to join on first NST not equal to number of " +
                        "columns to join on second NST");

        if (!"*".equals(colListSpec) && firstColNames.length == 1) {
            String[][] columns = NSTUtil.colListToNewNameMap(this, colListSpec);
            if (columns.length == 2) {
                String expCol0 = columns[0][0];
                String expCol1 = columns[1][0];
                if (expCol0.toUpperCase().startsWith("A.")) {
                    expCol0 = expCol0.substring(2);
                }
                if (expCol1.toUpperCase().startsWith("B.")) {
                    expCol1 = expCol1.substring(2);
                }
                if (isColumnExists(expCol0) && otherNST.isColumnExists(expCol1)) {
                    // this is done in the hope that the NST's have their twoColumns cached
                    String thisBATCmd = this.getTwoNSTColumnsAsCmd(firstColNames[0], expCol0);
                    String otherBATCmd = otherNST.getTwoNSTColumnsAsCmd(secondColNames[0], expCol1);
                    String contTableBAT = Connection.executeAndSave(thisBATCmd + ".reverse().join(" + otherBATCmd + ")");

                    String newCol0 = columns[0][1];
                    String newCol1 = columns[1][1];
                    if (newCol0.toUpperCase().startsWith("A.")) {
                        newCol0 = newCol0.substring(2);
                    }
                    if (newCol1.toUpperCase().startsWith("B.")) {
                        newCol1 = newCol1.substring(2);
                    }

                    if (newCol0.equals(newCol1)) {
                        newCol0 = "A." + newCol0;
                        newCol1 = "B." + newCol1;
                    }
                    return new NST(contTableBAT, newCol0, newCol1);
                }
            }
        }

        NST joinNST = new NST();

        String[] newColNames = NSTUtil.combineJoinedNSTColNames(this, otherNST);

        // get a mapping from the heads of the first NST to the heads of the second NST where they will be joined
        String oidMapBAT = oidMapBAT(otherNST, firstColNames, secondColNames, true);

        /**
         * Fix for Monet 4.6.2. For some strange reason (which has been fixed in 4.10)
         * a join with zero rows creates an NST whose BATs can be used but later fail in join
         * operations (as if they were empty)
         * The fix here checks whether the join results in 0 rows, and if so it simply creates a
         * new NST with empty, but fresh, BATs.
         * Incidentally, this is also faster, since all the intra-NST joins are avoided.
         */
        String r1 = null;
        String r2 = null;
        int joinRowCount = MonetUtil.getRowCount(oidMapBAT);
        if (joinRowCount == 0) {
            int newColId = 0;
            for (int colIdx = 0; colIdx < getColumnCount(); colIdx++) {
                NSTColumn col = getNSTColumn(colIdx);
                String colBAT = Connection.executeAndSave("bat(oid," + col.getType().toString() + ")");
                joinNST.columnList.add(new NSTColumn(newColNames[newColId++], col.getType(), colBAT));
            }
            for (int colIdx = 0; colIdx < otherNST.getColumnCount(); colIdx++) {
                NSTColumn col = otherNST.getNSTColumn(colIdx);
                String colBAT = Connection.executeAndSave("bat(oid," + col.getType().toString() + ")");
                joinNST.columnList.add(new NSTColumn(newColNames[newColId++], col.getType(), colBAT));
            }
        } else {
            // oidMapBAT has in the head the heads of the firstNST and in the
            // tail the corresponding tails of the second NST.
            // We need to renumber them, and create a BAT that contains
            // the heads of the first NST with their corresponding new number,
            // and the same for the heads of the second NST. We will call those
            // two BATs r1 and r2, respectively, and we will use them to get
            // the actual values from the original BATs
            r1 = Connection.executeAndSave(oidMapBAT + ".mark(0@0)");
            r2 = Connection.executeAndSave(oidMapBAT + ".reverse().mark(0@0)");
            // Add r1 and r2 to list of variables to delete upon release

            // copy columns, add call to apply filter in BatName for each col
            joinNST.copyJoinColumns(this, otherNST, r1, r2, newColNames);
        }

        NST materializedNST = new NST();
        String[][] colNamesMap = NSTUtil.colListToNewNameMap(joinNST, colListSpec);
        for (int colIdx = 0; colIdx < colNamesMap.length; colIdx++) {
            String oldColName = colNamesMap[colIdx][0];
            String newColName = colNamesMap[colIdx][1];
            NSTColumn parentCol = joinNST.getNSTColumn(oldColName);
            DataTypeEnum type = parentCol.getType();
            String columnBat = Connection.executeAndSave(parentCol.getBATName() + ".access(BAT_WRITE)");
            materializedNST.columnList.add(new NSTColumn(newColName, type, columnBat));
        }

        Connection.releaseSavedVar(oidMapBAT);
        if (joinRowCount > 0) {
            Connection.releaseSavedVar(r1);
            Connection.releaseSavedVar(r2);
        }
        joinNST.release();
        return materializedNST;
    }

    public NST joinUnless(NST otherNST, String[] firstColNames, String[] secondColNames,
                          String[] firstUnlessCols, String[] secondUnlessCols, String colListSpec) {
        Assert.notNull(otherNST, "Based on null second NST");
        Assert.condition(firstColNames.length > 0, "firstColName empty");
        Assert.condition(firstColNames.length == secondColNames.length,
                "number of columns to join on first NST not equal to number of columns to join on second NST");
        Assert.condition(firstUnlessCols.length > 0, "firstColName empty");
        Assert.condition(firstUnlessCols.length == secondUnlessCols.length,
                "number of columns to join on first NST not equal to number of columns to join on second NST");

        NST joinNST = new NST();

        String[] newColNames = NSTUtil.combineJoinedNSTColNames(this, otherNST);

        // get a mapping from the heads of the first NST to the heads of the second NST where they will be joined
        String oidMapBATOrig = oidMapBAT(otherNST, firstColNames, secondColNames, true);
        String oidMapBATUnless = oidMapBAT(otherNST, firstUnlessCols, secondUnlessCols, false);
        String oidMapBAT = Connection.executeAndSave(oidMapBATOrig + ".sdiff(" + oidMapBATUnless + ")");
        Connection.releaseSavedVar(oidMapBATOrig);
        Connection.releaseSavedVar(oidMapBATUnless);

        /**
         * Fix for Monet 4.6.2. For some strange reason (which has been fixed in 4.10)
         * a join with zero rows creates an NST whose BATs can be used but later fail in join
         * operations (as if they were empty)
         * The fix here checks whether the join results in 0 rows, and if so it simply creates a
         * new NST with empty, but fresh, BATs.
         * Incidentally, this is also faster, since all the intra-NST joins are avoided.
         */
        String r1 = null;
        String r2 = null;
        int joinRowCount = MonetUtil.getRowCount(oidMapBAT);
        if (joinRowCount == 0) {
            int newColId = 0;
            for (int colIdx = 0; colIdx < getColumnCount(); colIdx++) {
                NSTColumn col = getNSTColumn(colIdx);
                String colBAT = Connection.executeAndSave("bat(oid," + col.getType().toString() + ")");
                joinNST.columnList.add(new NSTColumn(newColNames[newColId++], col.getType(), colBAT));
            }
            for (int colIdx = 0; colIdx < otherNST.getColumnCount(); colIdx++) {
                NSTColumn col = otherNST.getNSTColumn(colIdx);
                String colBAT = Connection.executeAndSave("bat(oid," + col.getType().toString() + ")");
                joinNST.columnList.add(new NSTColumn(newColNames[newColId++], col.getType(), colBAT));
            }
        } else {
            // oidMapBAT has in the head the heads of the firstNST and in the
            // tail the corresponding tails of the second NST.
            // We need to renumber them, and create a BAT that contains
            // the heads of the first NST with their corresponding new number,
            // and the same for the heads of the second NST. We will call those
            // two BATs r1 and r2, respectively, and we will use them to get
            // the actual values from the original BATs
            r1 = Connection.executeAndSave(oidMapBAT + ".mark(0@0)");
            r2 = Connection.executeAndSave(oidMapBAT + ".reverse().mark(0@0)");
            // Add r1 and r2 to list of variables to delete upon release

            // copy columns, add call to apply filter in BatName for each col
            joinNST.copyJoinColumns(this, otherNST, r1, r2, newColNames);
        }

        NST materializedNST = new NST();
        String[][] colNamesMap = NSTUtil.colListToNewNameMap(joinNST, colListSpec);
        for (int colIdx = 0; colIdx < colNamesMap.length; colIdx++) {
            String oldColName = colNamesMap[colIdx][0];
            String newColName = colNamesMap[colIdx][1];
            NSTColumn parentCol = joinNST.getNSTColumn(oldColName);
            DataTypeEnum type = parentCol.getType();
            String columnBat = Connection.executeAndSave(parentCol.getBATName() + ".access(BAT_WRITE)");
            materializedNST.columnList.add(new NSTColumn(newColName, type, columnBat));
        }

        Connection.releaseSavedVar(oidMapBAT);
        if (joinRowCount > 0) {
            Connection.releaseSavedVar(r1);
            Connection.releaseSavedVar(r2);
            joinNST.release();
        }
        return materializedNST;
    }


    public NST join(NST otherNST, String[] firstColNames, String[] secondColNames) {
        return join(otherNST, firstColNames, secondColNames, "*");
    }

    public NST join(NST otherNST, String filterDef, String colListSpec) {
        String[] joinCols = NSTUtil.findColsInFilterDef(filterDef);
        return join(otherNST, new String[]{joinCols[0]}, new String[]{joinCols[1]}, colListSpec);
    }

    public NST joinUnless(NST otherNST, String filterDef, String unlessDef, String colListSpec) {
        String[] joinCols = NSTUtil.findColsInFilterDef(filterDef);
        String[] unlessCols = NSTUtil.findColsInFilterDef(unlessDef);

        return joinUnless(otherNST,
                new String[]{joinCols[0]}, new String[]{joinCols[1]},
                new String[]{unlessCols[0]}, new String[]{unlessCols[1]},
                colListSpec);
    }

    public NST join(NST otherNST, String filterDef) {
        return join(otherNST, filterDef, "*");
    }


    /**
     * Performs an outer join on this NST with another specified NST
     * An outer join behaves like an ordinary join with the exception that
     * if no match is found from a row in this NST to the rows of other NST,
     * the row is inserted into the resulting NST with 'nil' values for all
     * the columns of the other NST.
     * <p/>
     * For example, consider the two NSTs below:
     * <p/><pre>
     * A    B
     * -------
     * 1    2
     * 3    4
     * </pre><p/><pre>
     * A    B
     * -------
     * 1    2
     * </pre><p/>
     * If one were to outer join on columns A and B:<br>
     * <code>firstNST.leftOuterJoin(secondNST, String[]{"A", "B"}, String[]{"A", "B"},
     * String[]{"A1", "B1", "A2", "B2"}, true);</code>
     * <p/>
     * The resulting NST is:
     * <p/><pre>
     * A1   B1  A2  B2
     * ---------------
     * 1    2   1   2
     * 3    4   nil nil
     * </pre>
     *
     * @param otherNST
     * @param thisNSTColNamesToJoin
     * @param otherNSTColNamesToJoin
     * @param colListSpec            ("*" by default)
     * @return the outer join of this NST to the other NST, joining on the given columns
     */
    public NST leftOuterJoin(NST otherNST,
                             String[] thisNSTColNamesToJoin,
                             String[] otherNSTColNamesToJoin,
                             String colListSpec) {
        Assert.notNull(otherNST, "otherNST null");
        Assert.condition(thisNSTColNamesToJoin.length > 0, "must specify at least one col from this NST to join on");
        Assert.condition(otherNSTColNamesToJoin.length > 0, "must specify at least one col from other NST to join on");
        Assert.condition(thisNSTColNamesToJoin.length == otherNSTColNamesToJoin.length, "col names to join must be of equal length");

        String[] newColNames = NSTUtil.combineJoinedNSTColNames(this, otherNST);

        NST leftOuterJoin = new NST();

        // Compute the mapping first, and the rows that don't match
        String oidMapBAT = oidMapBAT(otherNST, thisNSTColNamesToJoin, otherNSTColNamesToJoin, true);
        String r1 = Connection.executeAndSave(oidMapBAT + ".mark(0@0)");
        String r2 = Connection.executeAndSave(oidMapBAT + ".reverse().mark(0@0)");
        String kdiffBAT = Connection.executeAndSave(this.getNSTColumn(0).getBATName() + ".kdiff(" + oidMapBAT + ").mark(0@0)");

        // put the rows that match into a dummy join NST, and materialize the ones that we care about in leftOuterJoin
        NST joinNST = new NST();
        joinNST.copyJoinColumns(this, otherNST, r1, r2, newColNames);
        String[][] colNames = NSTUtil.colListToNewNameMap(joinNST, colListSpec);
        for (int colIdx = 0; colIdx < colNames.length; colIdx++) {
            String colName = colNames[colIdx][0];
            String newName = colNames[colIdx][1];
            NSTColumn parentCol = joinNST.getNSTColumn(colName);
            DataTypeEnum type = parentCol.getType();
            String columnBat = Connection.executeAndSave(parentCol.getBATName() + ".access(BAT_WRITE)");
            leftOuterJoin.columnList.add(new NSTColumn(newName, type, columnBat));
        }

        // put the rows from this NST that DON'T MATCH into another dummy NST, add NIL projections from the other NST,
        // and add the ones we care about to the leftOuterJoin NST
        NST rowsNotInJoinNST = new NST();
        for (int i = 0; i < joinNST.getColumnCount(); ++i) {
            NSTColumn origCol = joinNST.getNSTColumn(i);
            String colName = origCol.getName();
            DataTypeEnum colType = origCol.getType();
            String batCmd;
            if (i < this.getColumnCount()) {
                batCmd = this.getNSTColumn(i).getBATName() + ".semijoin(" + kdiffBAT + ")";
            } else {
                batCmd = kdiffBAT + ".project(" + colType + "(nil))";
            }
            rowsNotInJoinNST.columnList.add(new NSTColumn(colName, colType, batCmd));
        }
        for (int colIdx = 0; colIdx < colNames.length; colIdx++) {
            String colName = colNames[colIdx][0];
            String newName = colNames[colIdx][1];
            NSTColumn leftOuterJoinColumn = leftOuterJoin.getNSTColumn(newName);
            NSTColumn nilColumn = rowsNotInJoinNST.getNSTColumn(colName);
            Connection.executeCommand(leftOuterJoinColumn + ".addend(" + nilColumn + ");");
        }

        // finally, release variables
        joinNST.release();
        rowsNotInJoinNST.release();
        Connection.releaseSavedVar(r1);
        Connection.releaseSavedVar(r2);
        Connection.releaseSavedVar(oidMapBAT);
        Connection.releaseSavedVar(kdiffBAT);

        return leftOuterJoin;
    }

    public NST leftOuterJoin(NST otherNST, String filterDef) {
        return leftOuterJoin(otherNST, filterDef, "*");
    }

    public NST leftOuterJoin(NST otherNST, String filterDef, String colListSpec) {
        String[] joinColumns = NSTUtil.getJoinColumns(filterDef);
        return leftOuterJoin(otherNST,
                NSTUtil.colListToArray(this, joinColumns[0]),
                NSTUtil.colListToArray(this, joinColumns[1]),
                colListSpec);
    }

    public NST leftOuterJoin(NST otherNST, String[] thisNSTColNamesToJoin, String[] otherNSTColNamesToJoin) {
        return leftOuterJoin(otherNST, thisNSTColNamesToJoin, otherNSTColNamesToJoin, "*");
    }

    public NST match(String baseCol, String onCol, String operator, String value) {
        DataTypeEnum colType = getNSTColumn(onCol).getType();
        if (colType == DataTypeEnum.STR) {
            value = "'" + value + "'"; // quote it if necessary
        }

        String twoColBATCmd = getTwoNSTColumnsAsCmd(baseCol, onCol);
        StringBuffer milSB = new StringBuffer();
        milSB.append("[");
        milSB.append(operator);
        milSB.append("](");
        milSB.append(twoColBATCmd);
        milSB.append(",");
        milSB.append(MonetUtil.delimitValue(Util.unQuote(value), colType));
        milSB.append(")");
        String newBATName = Connection.executeAndSave(milSB.toString());
        return new NST(newBATName, baseCol, "match");
    }

    /**
     * Makes all the BATs in this NST writable
     */
    public NST makeWritable() {
        for (Iterator<NSTColumn> iterator = getNSTColumns().iterator(); iterator.hasNext();) {
            NSTColumn nstColumn = iterator.next();
            StringBuffer milSB = new StringBuffer();
            milSB.append(nstColumn.getBATName());
            milSB.append(".access(BAT_WRITE);");
            Connection.executeCommand(milSB.toString());
        }
        return this;
    }

    public int max(String colName) {
        return MonetUtil.max(getNSTColumn(colName).getBATName());
    }

    public int min(String colName) {
        return MonetUtil.min(getNSTColumn(colName).getBATName());
    }

    /**
     * Helper method for leftOuterJoin and join constructor.
     * Finds a mapping from the head values
     * of thisNST to the headValues of otherNST where all the given columns
     * of the row in thisNST is equals to all the given columns of the row in
     * otherNST.  If joinOnNil is true
     * nil is treated as a regular value and join on.  Otherwise nils are
     * treated in standard join behavior, i.e., they do not match other nils
     *
     * @param otherNST
     * @param thisNSTColNames
     * @param otherNSTColNames
     * @param isAndCondition
     * @return a BAT with the mappings from the head values of thisNST to the
     *         head values of otherNST
     * @
     */
    private String oidMapBAT(NST otherNST,
                             String[] thisNSTColNames,
                             String[] otherNSTColNames,
                             boolean isAndCondition) {
        Assert.condition(thisNSTColNames.length > 0, "thisNSTColNames.length == 0 in oidMapBAT");
        Assert.condition(otherNSTColNames.length > 0, "otherNSTColNames.length == 0 in oidMapBAT");
        Assert.condition(thisNSTColNames.length == otherNSTColNames.length, "thisNSTColNames.length != otherNSTColNames.length " +
                "in oidMapBAT");

        // if there's only one column to join by, we return a quick join
        if (thisNSTColNames.length == 1) {
            return Connection.executeAndSave(this.getNSTColumn(thisNSTColNames[0]) + ".join(" + otherNST.getNSTColumn(otherNSTColNames[0]) + ".reverse())");
        }

        // otherwise, we get independent joins and then intersect them
        String[] oidMapBATs = new String[thisNSTColNames.length];
        for (int i = 0; i < thisNSTColNames.length; ++i) {
            String thisNSTBAT = this.getNSTColumn(thisNSTColNames[i]).getBATName();
            String otherNSTBAT = otherNST.getNSTColumn(otherNSTColNames[i]).getBATName();
            oidMapBATs[i] = Connection.executeAndSave(thisNSTBAT + ".join(" + otherNSTBAT + ".reverse())");
        }

        StringBuffer milCmd = new StringBuffer(oidMapBATs[0]);
        for (int i = 1; i < oidMapBATs.length; ++i) {
            milCmd.append(isAndCondition ? ".sintersect(" : ".sunion(");
            milCmd.append(oidMapBATs[i]);
            milCmd.append(")");
        }
        String oidMapBATsIntersection = Connection.executeAndSave(milCmd.toString());

        // and we must release the intermediate results
        for (int i = 0; i < oidMapBATs.length; ++i) {
            Connection.releaseSavedVar(oidMapBATs[i]);
        }

        return oidMapBATsIntersection;
    }

    /**
     * Reorders the columns in the given sequence. It changes the index of
     * columns in columnList, so that for example getNSTCol(0) now returns
     * the first column in the sequence, getNSTCol(1) the next one, etc.
     * Useful for example when calling insertRowsFromNST, to make the column
     * list of the two NSTs compatible.
     *
     * @param colList
     * @return this NST, with a new order for the columns
     */
    public NST orderColumns(String colList) {
        ArrayList<NSTColumn> newColumnList = new ArrayList<NSTColumn>();

        String[] orderedColumnNames = NSTUtil.colListToArray(this, colList);
        Assert.condition(orderedColumnNames.length == getColumnCount(), "You must specify the order for all columns");

        for (int colIdx = 0; colIdx < orderedColumnNames.length; colIdx++) {
            String orderedColumnName = orderedColumnNames[colIdx];
            NSTColumn thisColumn = getNSTColumn(orderedColumnName);
            Assert.condition(!newColumnList.contains(thisColumn), "Do not repeat columns");
            newColumnList.add(thisColumn);
        }

        columnList = newColumnList;
        return this;
    }

    public void print() {
        print("*", "*", "*");
    }

    /**
     * Prints the selected rows and cols and filter to the logger. It simply calls the selectRows
     * method and iterates over the ResultSet, printing the rows. It also prints a nice title row
     * with information about the SELECT.
     * This is the Java equivalent to the Python printNST proc stored in proximity.py
     *
     * @param filterDef
     * @param colList
     * @param rowList
     * @see NST#selectRows(String,String,String,boolean) for information about how the filters are applied
     */
    public void print(String filterDef, String colList, String rowList) {
        StringBuffer sb = new StringBuffer();
        String[] columns = NSTUtil.colListToArray(this, colList);
        sb.append("[SHOWING head");
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            sb.append(",");
            sb.append(column);
        }
        sb.append(" WHERE ");
        sb.append(filterDef);
        sb.append(" LIMIT ");
        sb.append(rowList);
        sb.append("]");

        ResultSet resultSet = selectRows(filterDef, colList, rowList, true);
        System.out.println(sb);
        while (resultSet.next()) {
            System.out.println(resultSet.getLine());
        }

    }

    public double prod(String onCol) {
        Assert.condition(NSTUtil.isColumnNumeric(this, onCol), "Cannot compute prod on a non-numeric column");
        return Double.parseDouble(Connection.readValue(getNSTColumn(onCol).getBATName() + ".prod().print();"));
    }

    /**
     * Selects a set of columns, using the filter command.
     * Shortcut for filter("*", cols, "*")
     *
     * @param colListSpec
     */
    public NST project(String colListSpec) {
        return filter("*", colListSpec);
    }

    /**
     * Selects a set of columns, using the filter command, and then applies a unique.
     *
     * @param colListSpec
     */
    public NST projectDistinct(String colListSpec) {
        NST projectNST = this.project(colListSpec);
        NST retNST = projectNST.distinct();
        projectNST.release();
        return retNST;
    }

    /**
     * Selects a set of rows, using the filter command.
     * Shortcut for filter("*", "*", range)
     *
     * @param range
     */
    public NST range(String range) {
        int[] fromTo = NSTUtil.rowListToArray(range);
        int from = fromTo[0];
        int to = fromTo[1];
        if (from == -1 && to == -1) {
            return this.copy();
        }
        if (NSTUtil.isRandomRowList(range)) {
            addRandomSortColumn("randomOrder");
        } else {
            addNumberColumn("randomOrder");
        }
        NST retNST = this.filter("randomOrder BETWEEN " + from + "-" + to, "*");
        this.removeColumn("randomOrder");
        retNST.removeColumn("randomOrder");
        return retNST;
    }

    public NST rangeSorted(String onCol, String range) {
        Assert.condition(!NSTUtil.isRandomRowList(range), "Do not use RANDOM ranges with rangeSorted()");
        NST sorted = this.sort(onCol, "*");
        NST retNST = sorted.range(range);
        sorted.release();
        return retNST;
    }

    public NST randomize(String idCol, String valCol) {
        Assert.stringNotEmpty(idCol, "empty aggrCol");
        Assert.stringNotEmpty(valCol, "empty valCol");

        String newIdBat = Connection.executeAndSave(getNSTColumn(idCol).getBATName() + ".reverse().mark(0@0);");
        String newValBat = Connection.executeAndSave(getNSTColumn(valCol).getBATName() + ".reverse().mark(0@0);");
        String idOrderBat = Connection.executeAndSave("uniform(" + newIdBat + ".count());");
        String valOrderBat = Connection.executeAndSave("uniform(" + newValBat + ".count());");
        String idsWithOrdersBat = Connection.executeAndSave(newIdBat + ".join(" + idOrderBat + ");");
        String valsWithOrdersBat = Connection.executeAndSave(newValBat + ".join(" + valOrderBat + ");");

        String shuffledBat = Connection.executeAndSave(idsWithOrdersBat + ".join(" + valsWithOrdersBat + ".reverse())");
        NST randomizeNST = new NST(shuffledBat, idCol, valCol);

        Connection.releaseSavedVar(newIdBat);
        Connection.releaseSavedVar(newValBat);
        Connection.releaseSavedVar(idOrderBat);
        Connection.releaseSavedVar(valOrderBat);
        Connection.releaseSavedVar(idsWithOrdersBat);
        Connection.releaseSavedVar(valsWithOrdersBat);

        return randomizeNST;

    }

    /**
     * Releases vars in tempVarNames. NB: Must be called on all new NSTs when
     * they are no longer needed. Otherwise important resources will not be
     * released in the database, esp. lots of RAM.
     * Ignores requests to release Proximity's internal tables
     * Ignores requests to release already released tables
     */
    public void release() {
        if (isReleased()) {
            return;
        }

        releaseInternalVars();

        // do not mark Proximity's tables as released
        if (!NSTUtil.isCoreNST(this)) {
            if (topBATName != null) {
                Connection.releaseSavedVar(Util.unQuote(topBATName));
            }
            isReleased = true;
        }
    }

    private void releaseColumnBAT(NSTColumn nstColumn) {
        if (nstColumn.isDelayedExecution()) {
            Connection.releaseSavedVar(nstColumn.getDelayedExecutionBATName());
        } else {
            if (nstColumn.getBATName().indexOf('.') == -1) {
                Connection.releaseSavedVar(nstColumn.getBATName());
                if (NSTUtil.isCoreNST(this)) {
                    columnList.remove(nstColumn);
                }
            }
        }
    }

    private void releaseInternalVars() {
        for (int colIdx = 0; colIdx < getColumnCount(); colIdx++) {
            releaseColumnBAT(columnList.get(colIdx));
        }
        if (cachedBAT != null && !NSTUtil.isCoreNST(this)) {
            Connection.releaseSavedVar(cachedBAT);
        }
    }

    public NST removeColumn(String colName) {
        Assert.notNull(colName, "null columnName");

        if (colName.equals(headColName) || colName.equals(tailColName)) {
            invalidateCache();
        }

        NSTColumn nstColumn = getNSTColumn(colName);
        releaseColumnBAT(nstColumn);
        columnList.remove(nstColumn);

        return this;
    }

    /**
     * Renames a column
     *
     * @param oldName
     * @param newName
     */
    public NST renameColumn(String oldName, String newName) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        Assert.stringNotEmpty(oldName, "empty oldName");
        Assert.stringNotEmpty(newName, "empty newName");
        Assert.condition(!isColumnExists(newName) || newName.equals(oldName), "column name already exists: " + newName);

        NSTColumn theOldCol = getNSTColumn(oldName); // throws exc if not found
        theOldCol.rename(newName);

        // Update name of cache
        if (oldName.equals(headColName)) {
            headColName = newName;
        }
        if (oldName.equals(tailColName)) {
            tailColName = newName;
        }

        return this;
    }

    /**
     * Renames all the columns in the NST
     *
     * @param newColNames
     * @return
     */
    public NST renameColumns(String newColNames) {
        Assert.condition(!isReleased(), "illegal operation for released NST");
        invalidateCache();

        String[] colNames = newColNames.split(",");
        Assert.condition(colNames.length == getColumnCount(), "incorrect number of column names");
        for (int colIdx = 0; colIdx < getColumnCount(); colIdx++) {
            getNSTColumn(colIdx).rename(colNames[colIdx].trim());
        }
        return this;
    }

    /**
     * Replaces the values in column colName with value on those
     * rows that match the filter
     * (Explicit filter is required to avoid accidentally changing all rows.
     * Forces users to pass a "*" filter if they want to change all rows)
     * If value is not a value ('a' for strings, or a number), it's interpreted to be columnName
     * Also, simple expressions of the form
     * colName [+|-|*|/, >, diff] [colName|value] are allowed in the replacement value
     * For example,
     * name + 'aaa'
     * name + name
     * weight * 20
     * weight / height
     *
     * @param filterDef
     * @param colName
     * @param value
     */
    public NST replace(String filterDef, String colName, String value) {
        if (colName.equals(headColName) || colName.equals(tailColName)) {
            invalidateCache();
        }

        NSTColumn column = getNSTColumn(colName);
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        if (filterCmd == null) {
            filterCmd = column.getBATName();
        }

        StringBuffer milSB = new StringBuffer();
        if (Util.isValueArgument(value)) {
            milSB.append(column.getBATName());
            milSB.append(".replace(");
            milSB.append(filterCmd);
            milSB.append(".project(");
            milSB.append(MonetUtil.delimitValue(Util.unQuote(value), column.getType()));
            milSB.append("))");
        } else if (value.split("(\\s)+").length == 1) {
            milSB.append(column.getBATName());
            milSB.append(".replace(");
            milSB.append(getNSTColumn(value).getBATName());
            milSB.append(".semijoin(");
            milSB.append(filterCmd);
            milSB.append("))");
        } else {
            String[] cmds = value.split("(\\s)+");
            if (cmds.length == 3) {
                String baseCol = cmds[0];
                String operator = cmds[1];
                String operand = cmds[2];
                milSB.append(column.getBATName());
                milSB.append(".replace([");
                milSB.append(operator);
                milSB.append("](");
                milSB.append(getNSTColumn(baseCol).getBATName());
                milSB.append(".semijoin(");
                milSB.append(filterCmd);
                milSB.append("),");
                if (Util.isValueArgument(operand)) {
                    milSB.append(MonetUtil.delimitValue(Util.unQuote(operand), column.getType()));
                } else {
                    milSB.append(getNSTColumn(operand).getBATName());
                }
                milSB.append("));");
            } else {
                throw new IllegalArgumentException("Invalid replace format: " + value);
            }
        }
        Connection.executeCommand(milSB.toString());

        return this;
    }


    public String save() {
        return save(null);
    }

    /**
     * Makes an NST persistent, by making its top-level BAT and all its
     * children BATs persistent
     *
     * @param name
     */
    public String save(String name) {
        log.debug("save() : " + name);
        Assert.condition(!NSTUtil.isCoreNST(this), "illegal operation for a core Proximity NST -- make a copy of it");

        if (name != null) {
            Assert.condition(!name.toLowerCase().startsWith("tmp_"), "NST names cannot begin 'tmp_'");
            Assert.condition(NSTUtil.normalizeName(name).equals(topBATName) || !MonetUtil.isBATExists(name),
                    "Another NST with that name already exists");
        } else {
            // Re-save with same name
            if (topBATName != null) {
                name = Util.unQuote(topBATName);
            }
        }

        // Create a new top-level BAT
        String newTop;
        newTop = MonetUtil.create("str,str");
        MonetUtil.setIsPersists(newTop, true);

        // iterate over the columns, and for each rename them to the new bat_name format,
        // and insert its batName in the NST
        List<NSTColumn> cols = getNSTColumns();
        String[] newBatNames = new String[cols.size()];
        for (int i = 0; i < newBatNames.length; i++) {
            NSTColumn nstColumn = cols.get(i);
            String columnName = nstColumn.getName();
            String batName = nstColumn.getBATName();
            if (name != null) {
                newBatNames[i] = "bat_" + name + "_" + columnName;
                MonetUtil.renameBAT(batName, newBatNames[i]);
            } else {
                newBatNames[i] = batName;
            }
        }
        for (int i = 0; i < newBatNames.length; i++) {
            NSTColumn nstColumn = cols.get(i);
            String columnName = nstColumn.getName();
            StringBuffer milSB = new StringBuffer();
            milSB.append(newTop);
            milSB.append(".insert(\"");
            milSB.append(columnName);
            milSB.append("\",str(");
            milSB.append(NSTUtil.normalizeName(newBatNames[i]));
            milSB.append("))");
            Connection.executeCommand(milSB.toString());
        }

        // make persistent
        StringBuffer milSB = new StringBuffer();
        milSB.append("[persists]([bat](");
        milSB.append(newTop);
        milSB.append("),const true);");
        Connection.executeCommand(milSB.toString());

        // if there was a top BAT, get rid of it
        if (topBATName != null && name != null) {
            MonetUtil.setIsPersists("bat(" + topBATName + ")", false);
            DB.commit();
        }

        // rename
        if (name != null && newTop != null && !name.equals(newTop)) {
            MonetUtil.renameBAT(newTop, name);
            Connection.releaseSavedVar(newTop);
            newTop = name;
        }

        releaseInternalVars();
        initializeFromDB(newTop);
        return topBATName;
    }

    /**
     * This overload returns all the rows and all the columns
     *
     * @return
     * @
     */
    public ResultSet selectRows() {
        return selectRows("*", "*", "*", false);
    }

    public ResultSet selectRows(String colList) {
        return selectRows("*", colList, "*", false);
    }

    public ResultSet selectRows(String filterDef, String colList) {
        return selectRows(filterDef, colList, "*", false);
    }

    public ResultSet selectRows(String filterDef, String colList, String rowList) {
        return selectRows(filterDef, colList, rowList, false);
    }


    /**
     * Corresponds to a SELECT colums WHERE condition. Constructs a print()
     * statement for all the requested BATs. The resultSet has the headOID in
     * the first column. NB: This has the side-effect of essentially making
     * column access 1-based (like JDBC), not zero-based (like Java).
     *
     * @param filterDef
     * @param colList
     * @param rowList
     * @param isRespectOrder when true, the result set will be sorted by the key column.
     */
    public ResultSet selectRows(String filterDef, String colList, String rowList, boolean isRespectOrder) {
        Assert.condition(!isReleased(), "illegal operation for released NST");

        String[] selectColumns = NSTUtil.colListToArray(this, colList);
        int[] fromTo = NSTUtil.rowListToArray(rowList);
        int from = fromTo[0];
        int to = fromTo[1];

        Assert.notNull(selectColumns, "Null selectColumns");
        Assert.condition(selectColumns.length > 0, "No columns specified");
        Assert.condition((from == -1 && to == -1) || (from >= 0 && to >= from),
                "either from and to are both -1, or to is greater or equal than from");

        // Create a print statement with all the columns
        // if all the columns and the filter are existing bats saved on disk (ie, they don't depend on a given connection/scope)
        //   then we can use the multi-threaded connections, if enabled
        boolean isOnDisk = true;
        StringBuffer milSB = new StringBuffer();
        milSB.append("print(");
        String filterCmd = FilterFactory.getFilterCmd(filterDef, this);
        for (int i = 0; i < selectColumns.length; i++) {
            // Will throw exception if the column doesn't exist
            NSTColumn thisColumn = getNSTColumn(selectColumns[i]);
            String colBatName = thisColumn.getBATName();
            if (!colBatName.startsWith("bat(")) {
                isOnDisk = false;
            }
            // Apply filters and ranges to the first column
            // In this case, we don't need the filtered BAT Name
            // because the print statement takes care of doing the join
            // and therefore rows from the core BAT that are not in the
            // filtered first column will not be included
            milSB.append(colBatName);
            if (filterCmd != null && i == 0) {
                milSB.append(".semijoin(");
                milSB.append(filterCmd);
                milSB.append(")");
                if (FilterFactory.isInFilter(filterDef)) {
                    isOnDisk = false;
                }
            }
            if (i == 0 && isRespectOrder) {
                milSB.append(".sort()");
            }
            if (i == 0 && from != -1) {
                milSB.append(".slice(");
                milSB.append(from);
                milSB.append(",");
                milSB.append(to);
                milSB.append(")");
            }
            milSB.append(",");
        }
        // At this point, milSB has at least one column, and ends with a ,
        // Remove the comma
        String queryStr = milSB.deleteCharAt(milSB.length() - 1).append(")").toString();
        if (isOnDisk) {
            return Connection.executeQueryWithConnectionPool(queryStr).setColumnNames(selectColumns);
        } else {
            return Connection.executeQuery(queryStr).setColumnNames(selectColumns);
        }
    }

    /**
     * Remembers that a particular column stores BAT names
     * This is necessary because otherwise the value for the column
     * gets delimited as a string upon insertion. If it is set to be saved as
     * BAT, then the delimiter used is .bbpname()
     *
     * @param colName
     * @param isBatName
     */
    public NST setIsBATNameColumn(String colName, boolean isBatName) {
        getNSTColumn(colName).setDelimitAsBATName(isBatName);
        return this;
    }

    /**
     * Physically sorts an NST.
     * Returns a copy of an NST where the values of onCols are physically sorted (in the BAT) ascendingly.
     * Use DESC keyword to sort in descending order, as in
     * "name, salary DESC, age"
     * <p/>
     * Speeds up searches on the sorted columns and, because it also makes sure that the other columns have ascending head
     * values, also ensures that intra-column joins (ie, joins between the BATs that make up the NST) are fast.
     *
     * @param onCols
     * @param colList
     * @return a new this, a copy of this NST where the BATs are sorted
     */
    public NST sort(String onCols, String colList) {
        // go through list of column names, and create the group_by BAT
        String ordering = "";
        String[] columnNames = NSTUtil.colListToArray(this, onCols);
        for (int i = 0; i < columnNames.length; i++) {
            String colName = columnNames[i];
            int asIndex = colName.toUpperCase().trim().indexOf(" DESC");
            boolean isReverse;
            if (asIndex != -1) {
                colName = colName.substring(0, asIndex);
                isReverse = true;
            } else {
                isReverse = false;
            }

            String cmd = (isReverse ? "sort_rev" : "sort");
            String iterCmd = (isReverse ? "CTrefine_rev" : "CTrefine");
            String batName = getNSTColumn(colName).getBATName();
            if (i == 0) {
                ordering = Connection.executeAndSave(batName + ".reverse()." + cmd + "().reverse()");
            } else {
                Connection.executeCommand(ordering + ":=" + ordering + "." + iterCmd + "(" + batName + ")");
            }
        }

        Connection.executeCommand(ordering + ":=" + ordering + ".number()");
        String newKeys = Connection.executeAndSave(ordering + ".mark(0@0).reverse().copy()");

        String[][] colNamesMap = NSTUtil.colListToNewNameMap(this, colList);
        String[] newColBATs = new String[colNamesMap.length];
        String[] newColTypes = new String[colNamesMap.length];
        String[] newColNames = new String[colNamesMap.length];
        for (int colIdx = 0; colIdx < colNamesMap.length; colIdx++) {
            String oldColName = colNamesMap[colIdx][0];
            String newColName = colNamesMap[colIdx][1];
            String colBAT = getNSTColumn(oldColName).getBATName();
            newColBATs[colIdx] = Connection.executeAndSave(newKeys + ".join(" + colBAT + ").sort().reverse().mark(0@0).reverse()");
            newColTypes[colIdx] = getNSTColumn(oldColName).getType().toString();
            newColNames[colIdx] = newColName;
        }

        Connection.releaseSavedVar(ordering);
        Connection.releaseSavedVar(newKeys);

        return new NST(newColBATs, newColNames, newColTypes);
    }

    public NST sort(String onCols) {
        return sort(onCols, "*");
    }

    public double sum(String onCol) {
        Assert.condition(NSTUtil.isColumnNumeric(this, onCol), "Cannot compute sum on a non-numeric column");
        return Double.parseDouble(Connection.readValue(getNSTColumn(onCol).getBATName() + ".sum().print();"));
    }

    /**
     * Saves an NST into a tab-delimited file. If colList is specified, it will only
     * dump the given columns.
     * NB: Dumps oid columns as ints, so they can be more easily loaded onto other
     * applications.
     *
     * @param fileNameFullPath
     */
    public NST tofile(String fileNameFullPath) {
        return tofile(fileNameFullPath, "*");
    }

    public NST tofile(String fileNameFullPath, String colList) {
        Assert.condition(!isReleased(), "illegal operation for released NST");

        String[] selectColumns = NSTUtil.colListToArray(this, colList);
        if (selectColumns.length == 0) {
            return this;
        }

        // Create BATs for names, seps, bats,  which will be passed to
        String names = MonetUtil.create("void,str");
        String seps = MonetUtil.create("void,str");
        String bats = MonetUtil.create("void,bat");
        for (int i = 0; i < selectColumns.length; i++) {
            // Will throw exception if the column doesn't exist
            NSTColumn thisColumn = getNSTColumn(selectColumns[i]);
            StringBuffer milSB = new StringBuffer();
            milSB.append(names);
            milSB.append(".append(\"");
            milSB.append(thisColumn.getName());
            milSB.append("\");");
            if (i == (selectColumns.length - 1)) {
                milSB.append(seps);
                milSB.append(".append(\"\\n\");");
            } else {
                milSB.append(seps);
                milSB.append(".append(\"\\t\");");
            }
            if (thisColumn.getType() == DataTypeEnum.OID) {
                milSB.append(bats);
                milSB.append(".append([int](");
                milSB.append(thisColumn.getBATName());
                milSB.append("));");
            } else {
                milSB.append(bats);
                milSB.append(".append(");
                milSB.append(thisColumn.getBATName());
                milSB.append(");");
            }
            Connection.executeCommand(milSB.toString());
        }

        // Now call the dump command
        StringBuffer milSB = new StringBuffer();
        milSB.append("dump(");
        milSB.append(names);
        milSB.append(",");
        milSB.append(seps);
        milSB.append(",");
        milSB.append(bats);
        milSB.append(",\"");
        milSB.append(Util.delimitBackslash(fileNameFullPath));
        milSB.append("\",");
        milSB.append("-1");
        milSB.append(")");
        Connection.executeQuery(milSB.toString());

        Connection.releaseSavedVar(names);
        Connection.releaseSavedVar(seps);
        Connection.releaseSavedVar(bats);

        return this;
    }

    public NST union(NST otherNST, String keyColumnList) {
        // insert into a copy.
        NST nst = this.copy();
        nst.insertRowsFromNST(otherNST);
        NST retNST = nst.distinct(keyColumnList);
        nst.release();
        return retNST;
    }

    public NST union(NST otherNST) {
        return union(otherNST, "*");
    }
}
