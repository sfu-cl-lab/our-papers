/**
 * $Id: QGItemTableModel.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.QGVertex;
import org.apache.log4j.Logger;
import org.jdom.Element;


/**
 * The TableModel returned by QueryCanvas.getTableModel() to represent QGItem
 * instances as a property sheet/editor. The returned table contains a property
 * description in the left column, and the corresponding value in the right
 * column. Here's the list of properties shown:
 * <p/>
 * +--------------+-------+
 * | Type         | Edge  |  or 'Vertex'
 * +--------------+-------+
 * | Name         | ...   |
 * +--------------+-------+
 * | Annotation   | ...   |
 * +--------------+-------+
 * | Condition    | ...   |
 * +--------------+-------+
 * | Is Directed  | ...   |  only if edge
 * +--------------+-------+
 */
public class QGItemTableModel extends QGObjTableModel {

    private static final Logger log = Logger.getLogger(QGItemTableModel.class);

    private QGItem qgItem;


    public QGItemTableModel(QGItem qgItem) {
        this.qgItem = qgItem;
    }

    public Class getClassAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else if (rowIndex == 0) {     // Type row
            return String.class;
        } else if (rowIndex == 1) {     // Name row
            return String.class;
        } else if (rowIndex == 2) {     // Annotation row
            return Annotation.class;
        } else if (rowIndex == 3) {     // Condition row
            return CondEleWrapper.class;
        } else {                        // Is Directed row
            return Boolean.class;
        }
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int column) {
        return (column == 0 ? "Property" : "Value");
    }

    public Object getQGObject() {
        return qgItem;
    }

    public int getRowCount() {
        return qgItem instanceof QGEdge ? 5 : 4;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex == 0) {            // Type row
            if (columnIndex == 0) {
                return "Type";
            } else {
                return (qgItem instanceof QGVertex ? "Vertex" : "Edge");
            }
        } else if (rowIndex == 1) {     // Name row
            if (columnIndex == 0) {
                return "Name";
            } else {
                return qgItem.firstName();
            }
        } else if (rowIndex == 2) {     // Annotation row
            if (columnIndex == 0) {
                return "Annotation";
            } else {
                return qgItem.annotation();     // null if none
            }
        } else if (rowIndex == 3) {     // Condition row
            if (columnIndex == 0) {
                return "Condition";
            } else {
                // todo cache?:
                return new CondEleWrapper(qgItem.condEleChild());  // null if none
            }
        } else {                        // Is Directed row
            if (columnIndex == 0) {
                return "Is Directed";
            } else {
                return new Boolean(((QGEdge) qgItem).isDirected());
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
        if (rowIndex == 1) {            // Name
            qgItem.setFirstName((String) aValue);
        } else if (rowIndex == 2) {     // Annotation
            qgItem.setAnnotation((Annotation) aValue);      // might be null
        } else if (rowIndex == 3) {     // Condition
            // sadly, QGraph requires the following round-about sequence for
            // setting the condition
            CondEleWrapper condEleWrapper = (CondEleWrapper) aValue;     // might be null
            qgItem.deleteCondEleChild();
            if (condEleWrapper != null) {
                Element newCondEleChild = condEleWrapper.getCondEleChild();
                Element condElement = new Element("condition");
                newCondEleChild.detach();   // remove from old parent
                condElement.addContent(newCondEleChild);
                qgItem.setCondition(condElement);
                newCondEleChild.detach();
            }
        } else {    // Is Directed
            Boolean isDirected = (Boolean) aValue;
            QGEdge qgEdge = (QGEdge) qgItem;
            qgEdge.setDirected(isDirected.booleanValue());
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

}
