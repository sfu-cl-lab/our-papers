/**
 * $Id: CompositeFilter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.dbmgr;

import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

/**
 */
public class CompositeFilter implements Filter {

    /**
     * Class-based static logger
     */
    static Logger log = Logger.getLogger(CompositeFilter.class);

    Filter filter1;
    Filter filter2;
    LogicalConnectorEnum connector;

    /**
     * Full arg constructor
     *
     * @param f1
     * @param con
     * @param f2
     */
    public CompositeFilter(Filter f1, LogicalConnectorEnum con, Filter f2) {
        this.filter1 = f1;
        this.filter2 = f2;
        this.connector = con;
    }


    /**
     * Private method that applies one condition on an NST
     *
     * @param onNST
     * @return
     * @
     */
    public String getApplyCmd(NST onNST) {
        Assert.notNull(onNST, "Empty NST");

        StringBuffer milSB = new StringBuffer();
        milSB.append(filter1.getApplyCmd(onNST));
        milSB.append(".");
        milSB.append(getConnectorCommand(connector));
        milSB.append("(");
        milSB.append(filter2.getApplyCmd(onNST));
        milSB.append(")");
        return milSB.toString();
    }

    public LogicalConnectorEnum getConnector() {
        return connector;
    }

    /**
     * Private method that combines two conditions using the given connector
     *
     * @param connector
     * @return
     */
    private String getConnectorCommand(LogicalConnectorEnum connector) {
        Assert.notNull(connector, "connector");
        Assert.condition(connector == LogicalConnectorEnum.AND ||
                connector == LogicalConnectorEnum.OR, "unknown conneector: " + connector);
        if (connector == LogicalConnectorEnum.AND) {
            return "semijoin";
        } else if (connector == LogicalConnectorEnum.OR) {
            return "kunion";
        }
        return null;
    }

    public Filter getFilter1() {
        return filter1;
    }

    public Filter getFilter2() {
        return filter2;
    }

    /**
     * Returns a description of this filter in a way that can be used in
     * an MIL statement
     */
    public String getMILDescription() {
        return "_composite_filter_" + filter1.getMILDescription() + "_" + filter2.getMILDescription();
    }


}