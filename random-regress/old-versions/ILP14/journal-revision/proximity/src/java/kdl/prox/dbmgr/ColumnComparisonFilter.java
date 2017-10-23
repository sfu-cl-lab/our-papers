/**
 * $Id: ColumnComparisonFilter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.dbmgr;

import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

/**
 * ColumnComparisonFilter - selects rows from an NST based on a comparison
 * between two columns of the NST. Allowed comparisons are:
 * <, <= , = , != , =>,  >
 * See ComparisonOperatorEnum for more details
 */
public class ColumnComparisonFilter implements Filter {
    // The columns in the NST that the filter is applied to
    private String columnA;
    private String columnB;
    private ComparisonOperatorEnum op;

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(ColumnComparisonFilter.class);


    /**
     * Full-arg constructor
     *
     * @param columnA
     * @param op
     * @param columnB
     */
    public ColumnComparisonFilter(String columnA, ComparisonOperatorEnum op, String columnB) {
        Assert.stringNotEmpty(columnA, "Null A column");
        Assert.stringNotEmpty(columnB, "Null B column");
        Assert.notNull(op, "Null operator");
        this.columnA = columnA;
        this.columnB = columnB;
        this.op = op;
    }

    /**
     *
     */
    public ColumnComparisonFilter(String columnA, String columnB) {
        Assert.stringNotEmpty(columnA, "Null A column");
        Assert.stringNotEmpty(columnB, "Null B column");
        this.columnA = columnA;
        this.columnB = columnB;

        this.op = ComparisonOperatorEnum.EQ;
    }

    /**
     * The operations to apply the filter
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");
        int operation = 0; //Default is equal
        StringBuffer milSB = new StringBuffer();
        milSB.append("[");
        if (op.equals(ComparisonOperatorEnum.EQ)) {
            milSB.append("=");
        } else if (op.equals(ComparisonOperatorEnum.GE)) {
            milSB.append(">=");
        } else if (op.equals(ComparisonOperatorEnum.GT)) {
            milSB.append(">");
        } else if (op.equals(ComparisonOperatorEnum.LE)) {
            milSB.append("<=");
        } else if (op.equals(ComparisonOperatorEnum.LT)) {
            milSB.append("<");
        } else if (op.equals(ComparisonOperatorEnum.NE)) {
            milSB.append("!=");
        } else {
            // should never happen.
            throw new UnsupportedOperationException("Invalid operator: " + op);
        }

        NSTColumn colA = onNST.getNSTColumn(columnA);
        NSTColumn colB = onNST.getNSTColumn(columnB);
        DataTypeEnum colAType = colA.getType();
        DataTypeEnum colBType = colB.getType();
        String colABAT = colA.getBATName();
        String colBBAT = colB.getBATName();

        if (colAType == DataTypeEnum.DBL && (colBType == DataTypeEnum.FLT || colBType == DataTypeEnum.INT)) {
            colBBAT = "[dbl](" + colBBAT + ")";
        } else if (colBType == DataTypeEnum.DBL && (colAType == DataTypeEnum.FLT || colAType == DataTypeEnum.INT)) {
            colABAT = "[dbl](" + colABAT + ")";
        } else if (colAType == DataTypeEnum.FLT && colBType == DataTypeEnum.INT) {
            colBBAT = "[flt](" + colBBAT + ")";
        } else if (colBType == DataTypeEnum.FLT && colAType == DataTypeEnum.INT) {
            colABAT = "[flt](" + colABAT + ")";
        }

        milSB.append("](");
        milSB.append(colABAT);
        milSB.append(",");
        milSB.append(colBBAT);
        milSB.append(").select(true)");

        return milSB.toString();
    }


    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return "compare_" + columnA + "_" + op + "_" + columnB;
    }

}
