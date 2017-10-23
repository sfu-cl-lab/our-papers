/**
 * $Id: ResultSet.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id: ResultSet.java 3658 2007-10-15 16:29:11Z schapira $ */

package kdl.prox.monet;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A ResultSet derived from a query. Provides next() and getColumn(i) methods, and getRowCount()
 * <p/>
 * Reads the entire resultSet into memory first, and them users can retrieve each column
 * with calls to next() and getColumn(i) and its derivatives.
 * Passing data from the Monet server to Java is time-consuming, and reading
 * large result sets into memory will consume lots of memory, so users
 * should keep resultSets are small as possible.
 */
public class ResultSet {

    private static Logger log = Logger.getLogger(ResultSet.class);

    /**
     * IVs. Set by constructor
     */
    protected MonetStream monetStream = null;
    protected String queryText = null;

    /**
     * The current row
     * Starts with 0 when the resultSet is created
     * Advances with each call to next()
     * Is set to -1 when next() goes past the last row
     */
    protected int rowNum;

    /**
     * The number of rows in the resultSet. Set by constructor
     */
    protected int rowCount;

    /**
     * The names of the columns --optional
     */
    private Map<String, Integer> columnNames = new HashMap<String, Integer>();

    /**
     * The last line read - returned by getLine()
     * next() sets currentLine to the next line, and processes it
     */
    protected String currentLine = null;

    /**
     * The list of column values (as strings) for the current row
     * First set by processcolumnNames, and then updated in every call to next()
     */
    protected ArrayList<String> columnValueList = null;

    /**
     * Stores all the lines returned by the resultSet.
     * Initialized by constructor, by going through the result and reading every row
     * Then next() advances the lines.
     */
    protected List<String> resultSetLines = new ArrayList<String>();


    /**
     * Default constructor
     */
    public ResultSet() {
        monetStream = null;
        queryText = null;
    }

    /**
     * Full-arg constructor. Keeps the stream, reads first line to make sure there are
     * no errors, and then reads everything into memory
     *
     * @param monetStream stream
     * @param query       query
     * @
     */
    public ResultSet(MonetStream monetStream, String query) {
        Assert.condition(monetStream != null, "Null stream passed to MonetResultSet");
        Assert.stringNotEmpty(query, "query");
        this.monetStream = monetStream;
        queryText = query;

        // initialize the list of column values and the row counter
        columnValueList = new ArrayList<String>();
        rowNum = 0;

        // Process all the headers and prepare the column count
        // Will throw an exception if there was an error with the query,
        // which is the desired behavior
        // Leaves currentLine set to first line after the #s
        // The first line contains all #s
        // the second line contains the name of the table, maybe
        // the third contains that name of the columns
        // and the fourht is again a line with all #s
        // For now, just get rid of the #s
        currentLine = this.monetStream.readLine();
        while (currentLine.length() > 0 && currentLine.charAt(0) == '#') {
            currentLine = this.monetStream.readLine();
        }

        // And now read all the lines
        rowCount = 0;
        while (currentLine.length() > 0) {
            resultSetLines.add(currentLine);
            rowCount++;
            currentLine = monetStream.readLine();
        }
        if (rowCount > 0) {
            ResultSet.parseRow(columnValueList, resultSetLines.get(0));
        }
    }

    /**
     * Finds the idx of the column by its name
     * Requires that columnName be set -use setColumnNames
     *
     * @param col
     * @return the idx of the column by that name, or throws an exception if not found
     */
    private Integer findColumnByName(String col) {
        Integer colIdx = columnNames.get(col);
        if (colIdx == null) {
            Assert.condition(columnNames.keySet().size() > 0, "columnNames were not initialized");
            throw new IllegalArgumentException("Column name not found " + col);
        }
        return colIdx;
    }


    /**
     * Returns a column in the current row as a boolean
     *
     * @param columnIndex
     * @return
     * @
     */
    public boolean getBoolean(int columnIndex) {
        String str = getColumn(columnIndex);
        return (str.equalsIgnoreCase("true"));
    }

    public boolean getBoolean(String col) {
        return getBoolean(findColumnByName(col));
    }


    /**
     * Returns a column in the current row as a char
     *
     * @param columnIndex
     * @return
     * @
     */
    public char getChar(int columnIndex) {
        String str = getColumn(columnIndex);
        // remove start/end quotes
        if ((str.startsWith("'")) && (str.endsWith("'"))) {
            str = str.substring(1, str.length() - 1);
        }
        return str.charAt(0);
    }

    public char getChar(String col) {
        return getChar(findColumnByName(col));
    }


    /**
     * Returns the content of the ith column, as a string. NB: Does *not* un-
     * delimit the string.
     *
     * @param i
     * @return
     */
    public String getColumn(int i) {
        if (monetStream == null) {
            throw new MonetException("ResultSet has already been closed.");
        }
        validateRow();
        if (i > columnValueList.size()) {
            throw new MonetException("Unknown column index " + i);
        }
        return columnValueList.get(i);
    }

    public String getColumn(String col) {
        return getColumn(findColumnByName(col));
    }


    /**
     * Returns the number of columns in the resultSet
     * columnValueList is first initialized by processcolumnNames
     */
    public int getColumnCount() {
        if (monetStream == null) {
            throw new MonetException("ResultSet has already been closed.");
        }
        return columnValueList.size();
    }


    /**
     * Returns the current line, as a list of strings, one for each column
     *
     * @return
     * @
     */
    public List getColumnList() {
        if (monetStream == null) {
            throw new MonetException("ResultSet has already been closed.");
        }
        validateRow();
        return (List) columnValueList.clone();
    }


    /**
     * Returns a column in the current row as a double
     *
     * @param columnIndex
     * @return
     * @
     */
    public double getDouble(int columnIndex) {
        return Double.parseDouble(getColumn(columnIndex));
    }

    public double getDouble(String col) {
        return getDouble(findColumnByName(col));
    }


    /**
     * Returns a column in the current row as a float
     *
     * @param columnIndex
     * @return
     * @
     */
    public float getFloat(int columnIndex) {
        return Float.parseFloat(getColumn(columnIndex));
    }

    public float getFloat(String col) {
        return getFloat(findColumnByName(col));
    }


    /**
     * Returns a column in the current row as an int
     *
     * @param columnIndex
     * @return
     * @
     */
    public int getInt(int columnIndex) {
        return Integer.parseInt(getColumn(columnIndex));
    }

    public int getInt(String col) {
        return getInt(findColumnByName(col));
    }


    /**
     * Returns the current line, as read from the Monet server
     *
     * @return
     * @
     */
    public String getLine() {
        if (monetStream == null) {
            throw new MonetException("ResultSet has already been closed.");
        }
        validateRow();
        return currentLine;
    }


    /**
     * Returns the text of the query that created this resultSet
     *
     * @return
     */
    public String getQueryText() {
        return queryText;
    }


    /**
     * Returns the number of rows in this resultSet
     * Rows go from 0 - (rowCount-1)
     *
     * @return
     */
    public int getRowCount() {
        return rowCount;
    }


    /**
     * getInt() variation that is called when the desired column is known to be
     * an OID. Returns the column in the current row as an int.
     *
     * @param columnIndex
     * @return
     * @
     */
    public int getOID(int columnIndex) {
        String oidStrVal = getColumn(columnIndex);     // recall OID format: number@stamp, e.g., "123@0"
        String oidStr = MonetUtil.undelimitValue(oidStrVal, DataTypeEnum.OID);
        return Integer.parseInt(oidStr);
    }

    public int getOID(String col) {
        return getOID(findColumnByName(col));
    }


    /**
     * Returns a column in the current row as a string
     *
     * @param columnIndex
     * @return
     * @
     */
    public String getString(int columnIndex) {
        String str = getColumn(columnIndex);
        String value = MonetUtil.undelimitValue(str, DataTypeEnum.STR);
        if (value == null) {
            log.warn("getString(): Read nil value from database for column " +
                    columnIndex + " in row " + rowNum +
                    (columnIndex == 0 ? "" : "(headID: " + getColumn(0) + ")") +
                    ". The query text was: " + queryText);
        }
        return value;
    }

    public String getString(String col) {
        Integer colIdx = findColumnByName(col);
        return getString(colIdx);
    }


    /**
     * Returns true if the ith column was the Monet nil value.
     *
     * @param columnIndex
     * @return
     */
    public boolean isColumnNil(int columnIndex) {
        String columnVal = getColumn(columnIndex);
        return columnVal.equals("nil");
    }

    public boolean isColumnNil(String col) {
        return isColumnNil(findColumnByName(col));
    }


    /**
     * Advances the pointer
     *
     * @return false if there are no more rows
     */
    public boolean next() {
        if (monetStream == null) {
            throw new MonetException("ResultSet has already been closed.");
        }

        if (rowNum == -1) {
            return false; // read past the last line
        }

        if (rowNum >= rowCount) {
            rowNum = -1;
            return false;
        } else {
            currentLine = resultSetLines.get(rowNum);
            ResultSet.parseRow(columnValueList, currentLine);
            rowNum++;
            return true;
        }
    }


    /**
     * Processes the current line, dividing it up into a list of strings,
     * one for each column
     * Sets the columnValueList variable to the new list of columns
     * getcolumn(i) returns the appropriate column
     *
     * @param columnValueListInput List to hold resulting columns. NB: destructively modified (cleared then filled)
     * @param row                  input row to parse
     */
    static void parseRow(List<String> columnValueListInput, String row) {
        Assert.stringNotEmpty(row, "row");

        // Clear the column value list
        columnValueListInput.clear();

        // First, remove the [ (leave the ] to mark the last column
        int from = 1;
        for (int i = 1; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"' || c == '\'') {
                // Read until eol or another "/'
                int j;
                for (j = i + 1; ; j++) {    // end test in body
                    // advance the pointer until we reach EOL, the matching
                    // delimiter (but not after a backslash)
                    boolean isEOL = (j >= row.length());
                    boolean isDelimitMatch = (row.charAt(j) == c);
                    boolean isPrevCharSlash = (row.charAt(j - 1) == '\\');
                    if (isEOL || (isDelimitMatch && !isPrevCharSlash)) {
                        break;
                    }
                }
                // Move i too ;
                i = j;
            }
            if (c == ',' || c == ']') {
                // Add the string to the list of column values
                // NB: we do not call MonetUtil.undelimitValue() on toAdd here
                // because it's handled by ResultSet.getString()
                columnValueListInput.add(row.substring(from, i).trim());
                from = i + 1;
            }
        }
    }

    /**
     * Sets names for the columns in the resultSet, so you can used named get'ers
     *
     * @param colNames
     */
    public ResultSet setColumnNames(String[] colNames) {
        columnNames.put("HEAD_IDX", 0);
        for (int i = 0; i < colNames.length; i++) {
            columnNames.put(colNames[i], i + 1);
        }
        return this;
    }

    /**
     * For compatibility with JDBC
     *
     * @return
     */
    public int size() {
        return getRowCount();
    }


    /**
     * Returns a List of Doubles for columnIndex, starting from the current row to the end.
     *
     * @param columnIndex
     * @return
     */
    private List<Double> toDoubleList(int columnIndex) {
        List<Double> dbls = new ArrayList<Double>();
        while (next()) {
            dbls.add(getDouble(columnIndex));
        }
        return dbls;
    }

    public List<Double> toDoubleList(String col) {
        return toDoubleList(findColumnByName(col));
    }

    /**
     * Returns a List of Integers for columnIndex, starting from the current row
     * to the end.
     *
     * @param columnIndex
     * @return
     * @
     */
    public List<Integer> toIntList(int columnIndex) {
        List<Integer> ints = new ArrayList<Integer>();
        while (next()) {
            ints.add(getInt(columnIndex));
        }
        return ints;
    }

    public List<Integer> toIntList(String col) {
        return toIntList(findColumnByName(col));
    }

    /**
     * Returns a List of Integers (OIDs) for columnIndex, starting from the current row to the end.
     *
     * @param columnIndex
     * @return
     */
    public List<Integer> toOIDList(int columnIndex) {
        List<Integer> oids = new ArrayList<Integer>();
        while (next()) {
            oids.add(getOID(columnIndex));
        }
        return oids;
    }

    public List<Integer> toOIDList(String col) {
        return toOIDList(findColumnByName(col));
    }

    /**
     * Returns a List of StringS for columnIndex, starting from the current row
     * to the end.
     *
     * @param columnIndex
     * @return
     * @
     */
    public List<String> toStringList(int columnIndex) {
        List<String> strings = new ArrayList<String>();
        while (next()) {
            strings.add(getString(columnIndex));
        }
        return strings;
    }

    public List<String> toStringList(String col) {
        return toStringList(findColumnByName(col));
    }


    /**
     * Makes sure that the current rowNum is valid (> 0)
     *
     * @ if haven't called next() yet, or moved beyond last row
     */
    private void validateRow() {
        if (rowNum == 0) {
            throw new MonetException("Call next() to get the first row");
        }
        if (rowNum == -1) {
            throw new MonetException("You've already read past the last row");
        }
    }

}
