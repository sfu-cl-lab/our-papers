/**
 * $Id: ColorLegend.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * A JPanel that knows how to show colors from a ColorManager.
 */
public class ColorLegend extends JPanel {

    public ColorLegend(Color[] colors, String[] names) {
        super(new GridLayout(1, 0));
        setOpaque(true);
        setLayout(new BorderLayout());

        JTable jTable = new JTable(new LegendModel(colors, names));
        jTable.getColumnModel().getColumn(0).setPreferredWidth(40);     // color column
        jTable.getColumnModel().getColumn(1).setPreferredWidth(200);    // names column
        jTable.setPreferredScrollableViewportSize(new Dimension(250, 200));
        jTable.setDefaultRenderer(Color.class, new ColorRenderer(true));

        JScrollPane jScrollPane = new JScrollPane(jTable);
        add(jScrollPane, BorderLayout.CENTER);
    }

    //
    // inner classes
    //

    class LegendModel extends AbstractTableModel {

        private final String[] columnNames = {"Color", "Name"};
        private Color[] colors;
        private String[] names;


        public LegendModel(Color[] colors, String[] names) {
            this.colors = colors;
            this.names = names;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            return colors.length;
        }

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return colors[row];
            } else {
                return names[row];
            }
        }

    }


    class ColorRenderer extends JLabel implements TableCellRenderer {

        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;


        public ColorRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object color,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setBackground((Color) color);
            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null) {
                        selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                table.getSelectionBackground());
                    }
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null) {
                        unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                table.getBackground());
                    }
                    setBorder(unselectedBorder);
                }
            }
            return this;
        }

    }


}
