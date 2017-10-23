/**
 * $Id: SubqueryTableModel.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.Subquery;
import org.apache.log4j.Logger;


/**
 * The TableModel returned by QueryCanvas.getTableModel() to represent Subquery
 * instances as a property sheet/editor. The returned table contains a property
 * description in the left column, and the corresponding value in the right
 * column. Here's the list of properties shown:
 * <pre>
 * +--------------+---------+
 * | Type         | Subqery |
 * +--------------+---------+
 * | Annotation   | ...     |
 * +--------------+---------+
 * </pre>
 */
public class SubqueryTableModel extends QGObjTableModel {

    private static final Logger log = Logger.getLogger(SubqueryTableModel.class);

    private Subquery subquery;


    public SubqueryTableModel(Subquery subquery) {
        this.subquery = subquery;
    }

    public Class getClassAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else if (rowIndex == 0) {
            return String.class;
        } else {
            return Annotation.class;
        }
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int column) {
        return (column == 0 ? "Property" : "Value");
    }

    public Object getQGObject() {
        return subquery;
    }

    public int getRowCount() {
        return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex == 0) {            // Type row
            return (columnIndex == 0 ? "Type" : "Subquery");
        } else {     // Annotation row
            if (columnIndex == 0) {
                return "Annotation";
            } else {
                return subquery.annotation();   // null if none
            }
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return false;
        } else if (rowIndex == 0) {
            return false;
        } else {
            return true;
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // only one editable cell: annotation
        subquery.setAnnotation((Annotation) aValue);    // might be null
        fireTableCellUpdated(rowIndex, columnIndex);
    }

}
