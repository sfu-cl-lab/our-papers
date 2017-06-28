/**
 * $Id: QueryTableModel.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import kdl.prox.qgraph2.QGAddLink;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.Query;
import org.apache.log4j.Logger;


/**
 * The TableModel returned by QueryCanvas.getTableModel() to represent Query
 * instances as a property sheet/editor. The returned table contains a property
 * description in the left column, and the corresponding value in the right
 * column. Here's the list of properties shown:
 * <pre>
 * +--------------+-------+
 * | Type         | Query |
 * +--------------+-------+
 * | Name         | ...   |
 * +--------------+-------+
 * | Description  | ...   |
 * +--------------+-------+
 * | Constraint 1 | ...   |  if any
 * +--------------+-------+
 * | Constraint 2 | ...   |
 * +--------------+-------+
 * | ...          |       |
 * +--------------+-------+
 * | add-link 1   | ...   |  if any
 * +--------------+-------+
 * | add-link 2   | ...   |
 * +--------------+-------+
 * </pre>
 */
public class QueryTableModel extends QGObjTableModel {

    private static final Logger log = Logger.getLogger(QueryTableModel.class);
    private Query query;


    public QueryTableModel(Query query) {
        this.query = query;
    }

    public Class getClassAt(int rowIndex, int columnIndex) {
        if ((columnIndex == 1) && (rowIndex >= 3)) {
            if (rowIndex <= query.constraints().size() + 2) {
                return QGConstraint.class;
            } else {
                return QGAddLink.class;
            }
        } else {
            return String.class;
        }
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int column) {
        return (column == 0 ? "Property" : "Value");
    }

    public Object getQGObject() {
        return query;
    }

    public int getRowCount() {
        return 3 + query.constraints().size() + query.addLinks().size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex == 0) {            // Type row
            return (columnIndex == 0 ? "Type" : "Query");
        } else if (rowIndex == 1) {     // Name row
            return (columnIndex == 0 ? "Name" : query.getName());
        } else if (rowIndex == 2) {     // Description row
            return (columnIndex == 0 ? "Description" : query.getDescription());
        } else
        if ((rowIndex > 2) && (rowIndex <= query.constraints().size() + 2)) {
            // columnIndex >= 3 -> QGConstraint rows
            Object[] consts = query.constraints().toArray();
            int constIdx = rowIndex - 3;
            if (columnIndex == 0) {
                return "Constraint " + (constIdx + 1);
            } else {
                return (QGConstraint) consts[constIdx];
            }
        } else {
            // columnIndex >= 3 -> add-link rows
            Object[] addLinks = query.addLinks().toArray();
            int addLinksIdx = rowIndex - query.constraints().size() - 3;
            if (columnIndex == 0) {
                return "Add-link " + (addLinksIdx + 1);
            } else {
                return (QGAddLink) addLinks[addLinksIdx];
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
        // only one editable column (1), so check rows
        if (rowIndex == 1) {
            query.setName((String) aValue);
        } else if (rowIndex == 2) {
            query.setDescription((String) aValue);
        } else
        if ((rowIndex > 2) && (rowIndex <= query.constraints().size() + 2)) {
            // set constraint
            Object[] consts = query.constraints().toArray();
            int constIdx = rowIndex - 3;
            QGConstraint currQGConst = (QGConstraint) consts[constIdx];
            QGConstraint newQGConst = (QGConstraint) aValue;
            // instead of replacing currQGConst with newQGConst in its
            // container, we copy content to it. we do this rather than
            // replacement for simplicity; otherwise we'd have to deal with
            // isEdgeConstraint, annotation, and annotItemName, which require
            // a Query instance
            currQGConst.setOperator(newQGConst.operator());
            currQGConst.setItem1Name(newQGConst.item1Name());
            currQGConst.setItem2Name(newQGConst.item2Name());
            currQGConst.setItem1AttrName(newQGConst.item1AttrName());
            currQGConst.setItem2AttrName(newQGConst.item2AttrName());
        } else {
            // set constraint
            Object[] addLinks = query.addLinks().toArray();
            int addLinksIdx = rowIndex - query.constraints().size() - 3;
            QGAddLink currQGAddLink = (QGAddLink) addLinks[addLinksIdx];
            QGAddLink newQGAddLink = (QGAddLink) aValue;
            currQGAddLink.setFrom(newQGAddLink);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

}
