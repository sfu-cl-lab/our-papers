/**
 * $Id: QGObjTableModel.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import javax.swing.table.AbstractTableModel;


/**
 * Abstract superclass to TableModels that edit a Query, Subquery, or QGItem.
 */
public abstract class QGObjTableModel extends AbstractTableModel {

    /**
     * @param rowIndex
     * @param columnIndex
     * @return Class for the passed row
     */
    public abstract Class getClassAt(int rowIndex, int columnIndex);

    /**
     * @return the QGraph object that I'm modeling, either a Query, Subquery, or QGItem
     */
    public abstract Object getQGObject();

}
