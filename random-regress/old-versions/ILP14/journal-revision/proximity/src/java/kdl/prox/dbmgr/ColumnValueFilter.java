/**
 * $Id: ColumnValueFilter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;


/**
 * Implements a filter based on the value of a column
 */
public class ColumnValueFilter implements Filter {

    String column;
    String expectedValue;
    ComparisonOperatorEnum operator;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnValueFilter.class);


    /**
     * Full-arg constructor
     *
     * @param column
     * @param op
     * @param value
     */
    public ColumnValueFilter(String column, ComparisonOperatorEnum op, String value) {
        Assert.stringNotEmpty(column, "Empty column");
        Assert.notNull(value, "Null value");
        this.column = column;
        this.operator = op;
        this.expectedValue = value;
    }


    /**
     * Shortcut: operator is EQ
     *
     * @param column
     * @param value
     */
    public ColumnValueFilter(String column, String value) {
        this(column, ComparisonOperatorEnum.EQ, value);
    }


    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");

        // do a select on the BAT that column
        NSTColumn theColumn = onNST.getNSTColumn(column);
        StringBuffer milSB = new StringBuffer();

        // Generate MIL according to operator (EQ,NE, GE, LE, GT, LT)
        // Cannot use [=] op for EQ/NE because of it wouldn't allow to search for nil values
        //    ( a=nil always returns nil --it's not defined)
        if (operator == ComparisonOperatorEnum.EQ) {
            milSB.append(theColumn.getBATName());
            milSB.append(".uselect(");
            milSB.append(MonetUtil.delimitValue(expectedValue, theColumn.getType()));
            milSB.append(")");
        } else if (operator == ComparisonOperatorEnum.NE) {
            milSB.append(theColumn.getBATName());
            milSB.append(".kdiff(");
            milSB.append(theColumn.getBATName());
            milSB.append(".uselect(");
            milSB.append(MonetUtil.delimitValue(expectedValue, theColumn.getType()));
            milSB.append("))");
        } else if (operator == ComparisonOperatorEnum.LE) {
            milSB.append(theColumn.getBATName());
            milSB.append(".uselect(");
            milSB.append(theColumn.getType().toString());
            milSB.append("(nil),");
            milSB.append(MonetUtil.delimitValue(expectedValue, theColumn.getType()));
            milSB.append(")");
        } else if (operator == ComparisonOperatorEnum.GE) {
            milSB.append(theColumn.getBATName());
            milSB.append(".uselect(");
            milSB.append(MonetUtil.delimitValue(expectedValue, theColumn.getType()));
            milSB.append(",");
            milSB.append(theColumn.getType().toString());
            milSB.append("(nil))");
        } else if (operator == ComparisonOperatorEnum.GT || operator == ComparisonOperatorEnum.LT) {
            milSB.append("[");
            milSB.append(operator == ComparisonOperatorEnum.GT ? ">" : "<");
            milSB.append("](");
            milSB.append(theColumn.getBATName());
            milSB.append(", const ");
            milSB.append(MonetUtil.delimitValue(expectedValue, theColumn.getType()));
            milSB.append(").uselect(true)");
        }
        milSB.append("");

        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return column + "_" + operator.toString() + "_value";
    }

}
