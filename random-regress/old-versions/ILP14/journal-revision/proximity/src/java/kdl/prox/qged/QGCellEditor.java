/**
 * $Id: QGCellEditor.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.awt.Color;
import java.awt.Component;
import java.text.ParseException;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;


/**
 * An editor that knows how to edit Annotation, condEleChild Element, and
 * QGConstraint objects via the passed Format object.
 */
public class QGCellEditor extends DefaultCellEditor {

    private QGEdFormat qgEdFormat;
    private boolean isEmptyInputOK;     // true if blank strings are acceptable, in which case null is returned. otherwise, a dialog is shown


    public QGCellEditor(QGEdFormat qgEdFormat, boolean isEmptyInputOK) {
        super(new JTextField());
        this.qgEdFormat = qgEdFormat;
        this.isEmptyInputOK = isEmptyInputOK;
    }

    /**
     * @return null if current input is invalid <em>or</em> empty. returns
     *         an instance of the appropriate type (as determined by my
     *         format object) o/w
     */
    public Object getCellEditorValue() {
        String inputText = (String) super.getCellEditorValue();
        if (inputText.trim().length() == 0) {
            return null;
        } else {
            try {
                Object parsedObj = qgEdFormat.parseObject(inputText);
                return parsedObj;
            } catch (ParseException e) {
                return null;
            }
        }
    }

    /**
     * Simply sets the border to a thin black line. Needed to keep the editing
     * component as small as possible. Otherwise it occludes the text being
     * entered.
     *
     * @param table
     * @param value
     * @param isSelected
     * @param row
     * @param column
     * @return
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
        ((JComponent) getComponent()).setBorder(new LineBorder(Color.black));
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public boolean stopCellEditing() {
        String inputText = (String) super.getCellEditorValue();
        Object cellVal = getCellEditorValue();  // null if empty *or* invalid
        boolean isEmptyInput = (inputText.trim().length() == 0);
        if (isEmptyInput && !isEmptyInputOK) {
            JOptionPane.showMessageDialog(null, "Empty (blank) input is not " +
                    "acceptable. Please correct text.");
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));
            return false;
        } else if (isEmptyInput || (cellVal != null)) {
            return super.stopCellEditing();
        } else {
            JOptionPane.showMessageDialog(null, "Correct text, or press escape " +
                    "to cancel. The error was: " + qgEdFormat.getErrorMessage(),
                    "Couldn't parse input", JOptionPane.ERROR_MESSAGE);
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));
//                cancelCellEditing();
            return false;
        }
    }

}