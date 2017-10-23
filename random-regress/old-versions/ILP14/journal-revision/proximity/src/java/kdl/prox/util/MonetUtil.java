/**
 * $Id: MonetUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: MonetUtil.java 3658 2007-10-15 16:29:11Z schapira $
 */


package kdl.prox.util;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTColumn;
import kdl.prox.monet.Connection;
import kdl.prox.monet.ResultSet;
import kdl.prox.monet.MonetException;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Defines static Monet utilities.
 */
public class MonetUtil {

    private static Logger log = Logger.getLogger(MonetUtil.class);

    public static final String INTERNAL_NIL_STRING = "nil_str";


    private MonetUtil() {
        // disallow instances
    }

    /**
     * Called by delimitValue(), delimits value using standard Java String
     * delimiters. Here are all of the delimiters supported by this class (from
     * "3.10.6 Escape Sequences for Character and String Literals",
     * http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#101089):
     * <p/>
     * \b    \ u0008: backspace BS
     * \t    \ u0009: horizontal tab HT
     * \n    \ u000a: linefeed LF
     * \f    \ u000c: form feed FF
     * \r    \ u000d: carriage return CR
     * \"    \ u0022: double quote "
     * \\    \ u005c: backslash \
     * <p/>
     * (Interesting note: Without the spaces after the backslashes in the above
     * unicode examples, the source line numbers from the compiler are off by
     * two!)
     *
     * @return
     */
    private static String addStringDelimiters(String value) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('\"');      // initial double quote
        for (int charIdx = 0; charIdx < value.length(); charIdx++) {
            char strChar = value.charAt(charIdx);
            if (strChar == '\b') {
                stringBuffer.append("\\b");
            } else if (strChar == '\t') {
                stringBuffer.append("\\t");
            } else if (strChar == '\n') {
                stringBuffer.append("\\n");
            } else if (strChar == '\f') {
                stringBuffer.append("\\f");
            } else if (strChar == '\r') {
                stringBuffer.append("\\r");
            } else if (strChar == '"') {
                stringBuffer.append("\\\"");
            } else if (strChar == '\\') {
                stringBuffer.append("\\\\");
            } else {
                stringBuffer.append(strChar);
            }
        }
        stringBuffer.append('\"');      // final double quote
        return stringBuffer.toString();
    }

    /**
     * Applies a series of commands to every row in the BAT
     *
     * @param batName
     * @param commands
     */
    public static void batLoop(String batName, String commands) {
        Connection.executeCommand(batName + "@batloop() {" + commands + "}");
    }

    public static String copy(String batName) {
        return Connection.executeAndSave(batName + ".copy().access(BAT_WRITE);");
    }

    public static int count(String batName) {
        return getRowCount(batName);
    }

    /**
     * Creates a BAT. Comma-separated types, as in oid,oid
     *
     * @param types
     * @return
     */
    public static String create(String types) {
        String[] colTypes = types.split(",");
        return MonetUtil.create(colTypes[0], colTypes[1]);
    }

    public static String create(String headType, String tailType) {
        String cmd = "bat(" + headType + "," + tailType + ")";
        cmd = cmd + ("void".equals(headType) ? ".seqbase(0@0)" : "");
        return Connection.executeAndSave(cmd);
    }


    /**
     * Monet utility that sends the results of an MIL Query to the debug stream
     *
     * @param cmd
     */
    private static void debugQuery(String cmd) {
        ResultSet resultSet = Connection.executeQuery(cmd);
        while (resultSet.next()) {
            log.info(resultSet.getLine());
        }
    }

    public static void deleteHeads(String batName, String val) {
        Connection.executeCommand(batName + ".delete(" + val + ")");
    }

    public static void deleteTails(String batName, String val) {
        Connection.executeCommand(batName + ".reverse().delete(" + val + ")");
    }

    /**
     * Delimits a string to match a particular dataTypeEnum:
     * <p/>
     * 'chr'  -> delimit with single quotes
     * 'date' -> delimit with date cast, with double quotes around value
     * 'dbl'  -> delimit with dbl cast
     * 'oid'  -> delimit with oid cast
     * 'str'  -> delimit with double quotes, and delmit embedded double quotes
     * with backslash ('\')
     * <p/>
     * A null value is handled specially: It is cast to dataTypeEnum's type.
     * For example: delimitValue(nil, DataTypeEnum.STR) -> "str(nil)"
     * <p/>
     * The result is suitable for storage in Monet.
     *
     * @param value        NB: can be null, which is converted to "type(nil)" based on dataTypeEnum
     * @param dataTypeEnum NB: can be null
     * @return
     */
    public static String delimitValue(String value, DataTypeEnum dataTypeEnum) {
        Assert.notNull(dataTypeEnum, "null dataTypeEnum");
        if (value == null || value.equals(INTERNAL_NIL_STRING)) {
            return dataTypeEnum + "(nil)";
        } else if (dataTypeEnum.equals(DataTypeEnum.STR)) {
            return addStringDelimiters(value);
        } else if (dataTypeEnum.equals(DataTypeEnum.CHR)) {
            return "'" + value + "'";
        } else if (dataTypeEnum.equals(DataTypeEnum.DATE)) {
            return "date(\"" + value + "\")";
        } else if (dataTypeEnum.equals(DataTypeEnum.DBL)) {
            return "dbl(" + value + ")";
        } else if (dataTypeEnum.equals(DataTypeEnum.FLT)) {
            return "flt(" + value + ")";
        } else if (dataTypeEnum.equals(DataTypeEnum.INT)) {
            return "int(" + value + ")";
        } else if (dataTypeEnum.equals(DataTypeEnum.OID)) {
            return "oid(" + value + ")";
        } else if (dataTypeEnum.equals(DataTypeEnum.LNG)) {
            return "lng(" + value + ")";
        } else if (dataTypeEnum.equals(DataTypeEnum.BAT)) {
            return "str(" + value + ")";
        } else {
            return value;
        }
    }

    /**
     * Applies a select statement to a BAT
     * valueFilter contains the string that goes inside the select, as in
     * 'oid(3)', or '1,5'
     *
     * @param batName
     * @param valueFilter
     * @param isHead
     * @return
     */
    private static String filter(String batName, String valueFilter, boolean isHead) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(isHead ? ".reverse()" : "");
        milSB.append(".select(");
        milSB.append(valueFilter);
        milSB.append(")");
        milSB.append(isHead ? ".reverse()" : "");
        return Connection.executeAndSave(milSB.toString());
    }

    public static String filterHeads(String batName, String valueFilter) {
        return filter(batName, valueFilter, true);
    }

    public static String filterTails(String batName, String valueFilter) {
        return filter(batName, valueFilter, false);
    }

    /**
     * Creates a temporary BAT and fills its tail with the passed in values
     *
     * @param type
     * @param values
     * @return
     */
    public static String createBATFromList(DataTypeEnum type, String[] values) {
        String batName = MonetUtil.create("oid", type.toString());
        for (int i = 0; i < values.length; i++) {
            Connection.executeCommand(batName + ".insert(" + i + "@0, " +
                    delimitValue(values[i], type) + ")");
        }
        return batName;
    }

    /**
     * Overload that actually takes a List as an argument
     *
     * @param type
     * @param values
     * @return
     */
    public static String createBATFromCollection(DataTypeEnum type, Collection values) {
        String batName = MonetUtil.create("oid", type.toString());
        int i = 0;
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            String val = iterator.next().toString();
            Connection.executeCommand(batName + ".insert(" + i++ + "@0, " + delimitValue(val, type) + ")");
        }
        return batName;
    }

    /**
     * Deletes a BAT that has been marked persistent
     *
     * @param batName
     */
    public static void deleteBATIfExists(String batName) {
        log.debug("deleteBATIfExists(): " + batName);
        if (isBATExists(batName)) {
            Connection.executeCommand("bat(\"" + batName + "\").persists(false);");
            Connection.executeCommand("unload(\"" + batName + "\");");
            Connection.commit();
            if (isBATExists(batName)) {
                throw new MonetException("BAT couldn't be deleted: " + batName + "\n");
            }
        }
    }

    /**
     * Returns the actual tmp_* table that a variable points to
     *
     * @param varName
     * @return
     */
    public static String getBBPName(String varName) {
        if (!varName.startsWith("var")) {
            varName = Util.quote(varName);
        }
        ResultSet resultSet = read("str(bat(" + varName + "))");
        Assert.condition(resultSet.next(), "bbpname not found for variable: " + varName);
        return resultSet.getString(0);
    }

    /**
     * Returns the number of rows in the BBP
     */
    public static int getBBPSize() {
        return Integer.parseInt(Connection.readValue("view_bbp_name().count().print();"));
    }

    /**
     * Returns a BAT with sunique head/tails of the original
     *
     * @param batName
     * @return
     */
    public static String getDistinctHeadTailValues(String batName) {
        return Connection.executeAndSave(batName + ".sunique()");
    }

    /**
     * Returns a BAT with kunique heads of the original
     *
     * @param batName
     * @return
     */
    public static String getDistinctHeadValues(String batName) {
        return Connection.executeAndSave(batName + ".kunique()");
    }

    /**
     * Returns the number of distinct values in the head of a BAT.
     *
     * @param batName
     * @return
     */

    public static int getDistinctHeadValuesRowCount(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".kunique().count().print();");
        return Integer.parseInt(Connection.readValue(milSB.toString()));
    }

    /**
     * Returns the number of distinct values in the tail of a BAT.
     *
     * @param batName
     * @return
     */
    public static int getDistinctTailValuesRowCount(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".reverse().kunique().count().print();");
        return Integer.parseInt(Connection.readValue(milSB.toString()));
    }

    /**
     * Returns a BAT with kunique tails of the original
     *
     * @param batName
     * @return
     */
    public static String getDistinctTailValues(String batName) {
        Assert.stringNotEmpty(batName, "empty BAT name");
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".reverse().kunique().reverse();");
        return Connection.executeAndSave(milSB.toString());
    }

    public static String getDistinctTailValuesBinned(String batName, int numBins) {
        Assert.stringNotEmpty(batName, "empty BAT name");
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".bin(");
        milSB.append(numBins);
        milSB.append(");");
        return Connection.executeAndSave(milSB.toString());
    }


    /**
     * Returns the size of the total swapmem used by Monet
     *
     * @return
     * @
     */
    public static int getMemUsage() {
        return Integer.parseInt(Connection.readValue("mem_usage().find(\"_tot/malloc\").print();"));
    }

    /**
     * Returns the min value of an int BAT.
     * <p/>
     * Assumes that the BAT exists, and that it's of the right type, to avoid
     * repeated checks.
     *
     * @param batName
     * @return min value. NB: returns -1 if there are no columns. this is because
     *         callers typically add one to the result, which results in numbering
     *         starting at zero
     */
    public static int min(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("int(");
        milSB.append(batName);
        milSB.append(".min()).print();");
        String min = Connection.readValue(milSB.toString());
        if ("nil".equals(min)) {
            return -1;
        } else {
            return Integer.parseInt(min);
        }
    }

    /**
     * Returns the number of rows in a BAT.
     *
     * @param batName
     * @return
     */
    public static int getRowCount(String batName) {
        return Integer.parseInt(Connection.readValue(batName + ".count().print();"));
    }

    /**
     * Returns the type of the tail of a BAT.
     *
     * @param batName
     * @return
     */
    public static DataTypeEnum getTailType(String batName) {
        return DataTypeEnum.enumForType(Connection.readValue(batName + ".tail().print();"));
    }

    /**
     * Returns the type of the head of a BAT.
     *
     * @param batName
     * @return
     */
    public static DataTypeEnum getHeadType(String batName) {
        return DataTypeEnum.enumForType(Connection.readValue(batName + ".head().print();"));
    }

    /**
     * Returns the number of variables defined in Monet
     */
    public static int getVarCount() {
        return Integer.parseInt(Connection.readValue("view_var_name().count().print();"));
    }

    /**
     * Uses {} operator to apply a grouping function to a BAT; returns the grouped BAT
     *
     * @param batName
     * @param function
     * @return
     */
    public static String group(String batName, String function) {
        return Connection.executeAndSave("{" + function + "}(" + batName + ")");
    }


    public static void insert(String batName, String h, String t) {
        Connection.executeCommand(batName + ".insert(" + h + "," + t + ")");
    }

    public static void insertRowsFromBAT(String destBAT, String fomBAT) {
        Connection.executeCommand(destBAT + ".insert(" + fomBAT + ")");
    }


    /**
     * Returns true if there is a BAT named batName in the view_bbp_name
     *
     * @return
     */
    public static boolean isBATExists(String batName) {
        String cnt = Connection.readValue("view_bbp_name()" +
                ".select(\"" + batName + "\").count().print();");
        return (Integer.parseInt(cnt) > 0);
    }

    /**
     * Monet utility that returns true if the BAT named batName is persistent
     * Assumes that the BAT does indeed exist
     *
     * @param batName
     * @return
     */
    public static boolean isBATPersists(String batName) {
        StringBuffer sb = new StringBuffer();
        sb.append("string(");
        sb.append(batName);
        sb.append(".info().find(\"batPersistence\"),0,3).print();");
        return "per".equalsIgnoreCase(Connection.readValue(sb.toString()));
    }

    /**
     * Monet utility that returns true if there is a type named dataType.
     *
     * @param dataType
     * @return
     */
    public static boolean isMonetTypeDefined(String dataType) {
        return DataTypeEnum.enumForType(dataType) != null;
    }

    /**
     * Returns true if the heads of the BAT are unique
     *
     * @param batName
     * @return
     * @
     */
    public static boolean isUniqueHeads(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".kunique()");
        String uniqueHeadBAT = Connection.executeAndSave(milSB.toString());
        boolean isUniqueHeads = getRowCount(uniqueHeadBAT) == getRowCount(batName);
        Connection.releaseSavedVar(uniqueHeadBAT);
        return isUniqueHeads;
    }

    public static String kdiff(String batName, String otherBatName) {
        return Connection.executeAndSave("kdiff(" + batName + "," + otherBatName + ")");
    }

    public static String kintersect(String batName, String otherBatName) {
        return Connection.executeAndSave("kintersect(" + batName + "," + otherBatName + ")");
    }

    /**
     * Makes a BAT writable
     */
    public static void makeBATWritable(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".access(BAT_WRITE);");
        Connection.executeCommand(milSB.toString());
    }

    /**
     * Returns the max value of an int BAT.
     * <p/>
     * Assumes that the BAT exists, and that it's of the right type, to avoid
     * repeated checks.
     *
     * @param batName
     * @return max value. NB: returns -1 if there are no columns. this is because
     *         callers typically add one to the result, which results in numbering
     *         starting at zero
     */
    public static int max(String batName) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("int(");
        milSB.append(batName);
        milSB.append(".max()).print();");
        String max = Connection.readValue(milSB.toString());
        if ("nil".equals(max)) {
            return -1;
        } else {
            return Integer.parseInt(max);
        }
    }

    /**
     * Monet utility that sends the contents of a BAT to the debug stream
     *
     * @param batName
     */
    public static void print(String batName) {
        printBAT(batName);
    }

    public static void print(NST nst) {
        printNST(nst);
    }

    public static void printBAT(String batName) {
        debugQuery(batName + ".print()");
    }

    /**
     * Monet utility that sends the contents of an NST to the debug stream
     *
     * @param nst
     */
    public static void printNST(NST nst) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("print(");
        List columnList = nst.getNSTColumns();
        for (int i = 0; i < columnList.size(); i++) {
            NSTColumn thisColumn = (NSTColumn) columnList.get(i);
            if (i != 0) {
                milSB.append(",");
            }
            milSB.append(thisColumn.getBATName());
        }
        milSB.append(")");
        log.info(nst.getNSTColumnNames());
        debugQuery(milSB.toString());
    }

    public static String project(String batName, String value) {
        return Connection.executeAndSave(batName + ".project(" + value + ")");
    }

    public static ResultSet read(String bat) {
        return read(bat, -1, -1);
    }

    public static ResultSet read(String bat, int from, int to) {
        Assert.condition((from == -1 && to == -1) || (from >= 0 && to >= from),
                "either from and to are both -1, or to is greater or equal than from:" +
                        from + "/" + to);
        StringBuffer milSB = new StringBuffer();
        milSB.append(bat);
        if (from != -1) {
            milSB.append(".slice(");
            milSB.append(from);
            milSB.append(",");
            milSB.append(to);
            milSB.append(")");
        }
        milSB.append(".print()");
        return Connection.executeQuery(milSB.toString());
    }

    public static ResultSet readHistogram(String batName) {
        return readHistogram(batName, false);
    }

    public static ResultSet readHistogram(String batName, boolean isSorted) {
        return read(batName + ".histogram()" + (isSorted ? ".sort()" : ""));
    }

    public static void releaseSavedVar(String batName) {
        Connection.releaseSavedVar(batName);
    }

    /**
     * Returns a BAT that is a copy of inputBAT, but only those rows whose head
     * != tail. NB: It's the caller's responsibility to release the returned
     * BAT when done.
     *
     * @param inputBAT
     * @return
     */
    public static String removeIdenticalHeadTail(String inputBAT) {
        StringBuffer milSB = new StringBuffer();
        milSB.append("prox_removeIdenticalHeadTail(");
        milSB.append(inputBAT);
        milSB.append(")");
        return Connection.executeAndSave(milSB.toString());
    }

    /**
     * Called by undelimitValue(), does the opposite of addStringDelimiters().
     * See addStringDelimiters() for list.
     *
     * @param value
     * @return
     */
    private static String removeStringDelimiters(String value) {
        StringBuffer stringBuffer = new StringBuffer();
        boolean wasPrevCharBackSlash = false;
        for (int charIdx = 0; charIdx < value.length(); charIdx++) {
            char theChar = value.charAt(charIdx);
            boolean isFirstOrLastChar = charIdx == 0 || charIdx == value.length() - 1;
            if (isFirstOrLastChar) {
                if (theChar != '"') {
                    return value;   // leave value alone if it does not have both outer delimiters
                } else {
                    // don't add outer delimiters
                }
            } else {
                if (wasPrevCharBackSlash) {
                    // replace just-added backslash with undelimited character
                    if (theChar == 'b') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\b');
                    } else if (theChar == 't') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\t');
                    } else if (theChar == 'n') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\n');
                    } else if (theChar == 'f') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\f');
                    } else if (theChar == 'r') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\r');
                    } else if (theChar == '"') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('"');
                    } else if (theChar == '\\') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        stringBuffer.append('\\');
                    } else if (theChar == '0') {
                        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                    } else {
                        throw new IllegalArgumentException("unrecognized " +
                                "backslash-delimited character: '" + theChar +
                                "'");
                    }
                } else {
                    stringBuffer.append(theChar);
                }
            }
            wasPrevCharBackSlash = theChar == '\\' && !wasPrevCharBackSlash;
        }
        return stringBuffer.toString();
    }

    /**
     * Utility to rename a BAT.
     * Generates a Monet error if the from BAR doesn't exist, or if
     * the to BAT already exists.
     *
     * @param fromName
     * @param toName
     */
    public static String renameBAT(String fromName, String toName) {
        Assert.stringNotEmpty(toName, "Renaming to empty toName");
        StringBuffer milSB = new StringBuffer();
        milSB.append(fromName);
        milSB.append(".rename(\"");
        milSB.append(toName);
        milSB.append("\");");
        Connection.executeCommand(milSB.toString());
        return toName;
    }

    /**
     * Replaces values in a BAT. Assumes that the from and to values are
     * already converted to the correct tail type.
     * ONLINE! Modifies the given BAT
     * <p/>
     * NOTE: Assumes that the head values are unique!
     *
     * @param batName
     * @param from
     * @param to
     */
    public static void replace(String batName, String from, String to) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".replace(");
        milSB.append(batName);
        milSB.append(".select(");
        milSB.append(from);
        milSB.append(").project(");
        milSB.append(to);
        milSB.append("))");
        Connection.executeCommand(milSB.toString());
    }

    public static void replaceWhenHead(String batName, String head, String to) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".replace(");
        milSB.append(head);
        milSB.append(",");
        milSB.append(to);
        milSB.append(")");
        Connection.executeCommand(milSB.toString());
    }

    /**
     * @param batName
     * @return
     */
    public static String reverse(String batName) {
        return Connection.executeAndSave(batName + ".copy().reverse()");
    }

    public static void saveBAT(String batName, String persistentName) {
        Assert.condition(persistentName.startsWith("bat"), "Only BATs with the prefix bat_ are allowed");
        setIsPersists(batName, true);
        renameBAT(batName, persistentName);
    }

    public static String slice(String bat, int from, int to) {
        return Connection.executeAndSave(bat + ".slice(" + from + "," + to + ")");
    }

    /**
     * Makes a BAT persistent.
     *
     * @param batName
     * @param isPersists
     */
    public static void setIsPersists(String batName, boolean isPersists) {
        StringBuffer milSB = new StringBuffer();
        milSB.append(batName);
        milSB.append(".persists(");
        milSB.append(isPersists);
        milSB.append(");");
        Connection.executeCommand(milSB.toString());
    }

    public static String sort(String batName) {
        return Connection.executeAndSave(batName + ".sort()");
    }

    /**
     * Returns the sum of a BAT
     *
     * @param varName
     * @return
     */
    public static double sum(String varName) {
        return Double.parseDouble(Connection.readValue(varName + ".sum().print()"));
    }

    /**
     * Undelimits a string to match a particular dataTypeEnum:
     * <p/>
     * 'oid' -> remove db id to return object id: "123@0" -> "123"
     * 'str' -> remove string delimiters
     * <p/>
     * The result is suitable for storage in Monet.
     *
     * @param value
     * @param dataTypeEnum
     * @return
     */
    public static String undelimitValue(String value, DataTypeEnum dataTypeEnum) {
        if (dataTypeEnum.equals(DataTypeEnum.STR)) {
            if ("nil".equals(value)) {
                return null;
            }
            return removeStringDelimiters(value);
        } else if (dataTypeEnum.equals(DataTypeEnum.OID)) {
            // recall OID format: number@stamp, e.g., "123@0"
            int firstAtPos = value.indexOf('@');
            Assert.condition(firstAtPos != -1, "no '@' found in value: " + value);
            return value.substring(0, firstAtPos);
        } else {
            return value;
        }
    }
}
