/**
 * $Id: NSTColumn.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.monet.Connection;
import kdl.prox.util.Assert;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

/**
 * Represents a column in an NST
 * Stores the column name, its data type, and the BAT where the data are stored
 * <p/>
 * Normally the batName corresponds to a BAT in Monet.
 * However, if the NST to which this column belongs has an implicit filter, then
 * the filter has to be applied to the original BAT before the data can be accessed.
 * In those cases, the batName contains a sequence of MIL commands that create
 * that dynamic view of the core BAT. For example, if the column belongs to an NST
 * with a filter that says "column B = 2", then the the batName would be something
 * like this:
 * <p/>
 * o batName : tmp_124.semijoin(tmp_125.select(2).mirror)
 * <p/>
 * In any case, the batName can be used as part of MIL commands. For example,
 * to count the number of rows in a column, the following command is valid:
 * <p/>
 * getBATName() + ".count.print"
 * <p/>
 * Depending on whether the containing NST has a filter or not, the command will
 * translate into either one of the following lines, respectively:
 * <p/>
 * tmp_124.semijoin(tmp_125.select(2).mirror).count.print
 * tmp_124.count.print
 * <p/>
 * Of course, if the batName has a sequence of commands, then the column should
 * be treated as read-only, because there is no way of getting to the core
 * BAT to which modifications should be made.
 */
public class NSTColumn {

    private static Logger log = Logger.getLogger(NSTColumn.class);

    /**
     * IVs: Set by constructor, read by getters
     * batName can actually have MIL operations to get the subset of data
     */
    private String name;
    private DataTypeEnum type;
    private String batName;
    private String command;
    private boolean isDelimitAsBATName = false; // columns are delimited by their type
    private boolean isDelayedExecution = false;


    public NSTColumn(String name, DataTypeEnum type, String batName) {
        this(name, type, batName, null);
    }

    public NSTColumn(String name, DataTypeEnum type, String batName, String command) {
        Assert.stringNotEmpty(name, "Column with empty name");
        Assert.stringNotEmpty(batName, "Column with empty batName");

        this.name = name;
        this.type = type;
        this.batName = batName;
        this.command = command;
        if (this.command != null) {
            this.isDelayedExecution = true;
        }
    }

    /**
     * Returns the name of bat where the data are stored
     * This might not be a real BAT, but instead a list of commands that
     * can be executed to get the values for the column
     *
     * @return
     */
    public String getBATName() {
        if (isDelayedExecution) {
            Connection.executeCommand("var " + batName + " := " + command);
            isDelayedExecution = false;
        }
        return batName;
    }

    public String getDelayedExecutionBATName() {
        if (isDelayedExecution) {
            return batName;
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the column
     *
     * @return
     */
    public String getName() {
        return name;
    }


    /**
     * Returns the data type of the column
     *
     * @return
     */
    public DataTypeEnum getType() {
        if (type == null) {
            type = MonetUtil.getTailType(getBATName());
            Assert.notNull(type, "Unknown column type on column " + name);
        }
        return type;
    }


    public boolean isDelayedExecution() {
        return isDelayedExecution;
    }


    /**
     * True is teh column value should be delimited before an insert
     *
     * @return
     */
    public boolean isDelimitAsBATName() {
        return isDelimitAsBATName;
    }


    /**
     * Changes tha name of the column
     *
     * @param newName
     */
    public void rename(String newName) {
        Assert.stringNotEmpty(newName, "empty newName");
        name = newName;
    }


    public void setBAT(String newBATName) {
        batName = newBATName;
    }


    /**
     * When set to false, column values are not delimited when inserted into BAT
     *
     * @param delimit
     */
    public void setDelimitAsBATName(boolean delimit) {
        isDelimitAsBATName = delimit;
    }


    /**
     * Object method.
     */
    public String toString() {
        return getBATName();
    }
}
