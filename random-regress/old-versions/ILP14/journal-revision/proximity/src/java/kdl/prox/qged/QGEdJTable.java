/**
 * $Id: QGEdJTable.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGAddLink;
import kdl.prox.qgraph2.QGConstraint;
import org.apache.log4j.Logger;


/**
 * JTable subclass that simply returns the correct editor based on the type of
 * object in a cell. Only accepts QGItemTableModel models, or empty models.
 */
public class QGEdJTable extends JTable {
    private static final Logger log = Logger.getLogger(QGEdJTable.class);

    private static final TableCellRenderer QGED_RENDERER = new QGEdRenderer();

    public QGEdJTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnSelectionAllowed(true);
        setRowSelectionAllowed(true);
        getTableHeader().setReorderingAllowed(false);
    }

    public TableCellEditor getCellEditor(int row, int column) {
        QGObjTableModel qgItemTableModel = (QGObjTableModel) getModel();
        Class cellClass = qgItemTableModel.getClassAt(row, column);
        // todo cache?
        if (cellClass.equals(Annotation.class)) {
            return new QGCellEditor(new AnnotationFormat(), true);
        } else if (cellClass.equals(QGAddLink.class)) {
            return new QGCellEditor(new QGAddLinkFormat(), false);
        } else if (cellClass.equals(CondEleWrapper.class)) {
            return new QGCellEditor(new ConditionFormat(), true);
        } else if (cellClass.equals(QGConstraint.class)) {
            return new QGCellEditor(new QGConstraintFormat(), false);
        } else if (cellClass.equals(Boolean.class)) {
            return new QGCellEditor(new BooleanFormat(), false);
        } else {
            return super.getCellEditor(row, column);    // for Strings
        }
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
        return QGED_RENDERER;
    }

    public void setModel(TableModel dataModel) {
        if ((dataModel instanceof QGObjTableModel) ||
                (dataModel == null) ||
                (dataModel.getColumnCount() == 0) && (dataModel.getRowCount() == 0))
        {
            super.setModel(dataModel);
        } else {
            throw new IllegalArgumentException("only accepts null and " +
                    "QGObjTableModel: " + dataModel);
        }
    }

    //
    // inner classes
    //

    /**
     * A table cell renderer that manages text font and color based on cell
     * content.
     */
    static class QGEdRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable jTable, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int rowIndex, int columnIndex) {
            // set defaults (text, font, etc)
            super.getTableCellRendererComponent(jTable, value, isSelected,
                    hasFocus, rowIndex, columnIndex);
            if (!(jTable.getModel() instanceof QGObjTableModel)) {
                return this;
            }

            // set font
            Font baseFont = getFont();
            Font plainFont = new Font(baseFont.getName(), baseFont.getStyle(),
                    baseFont.getSize());
            Font boldItalicFont = new Font(baseFont.getName(),
                    Font.BOLD | Font.ITALIC, baseFont.getSize());
            Font italicFont = new Font(baseFont.getName(), Font.ITALIC,
                    baseFont.getSize());
            if (columnIndex == 0) {                      // property column
                setFont(boldItalicFont);
            } else if (columnIndex == 1 && rowIndex == 0) {   // type cell
                setFont(italicFont);
            } else {
                setFont(plainFont);
            }

            // NB: for now we don't changecolor as it caused problems for default
            // look and feel on Mac OS X

            return this;
        }

    }


}
