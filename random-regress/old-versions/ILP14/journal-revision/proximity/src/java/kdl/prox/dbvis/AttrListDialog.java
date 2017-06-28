/**
 * $Id: AttrListDialog.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;


/**
 * A dialog that's used by DBVisualizerJFrame to let users specify attributes
 * to use for the graph's elements' visual properties. Modified from the Sun
 * example to support 'special' list items at the beginning of the list. Thus,
 * the value returned from showDialog() is now either an Integer (indicating
 * which of the 'special' items was picked, starting with zero for the first
 * one) or a String (indicating a non-special item was picked).
 * <p/>
 * todo req: support escape -> cancel
 * todo bug: doesn't scroll to selected item when using VERTICAL_WRAP
 * <p/>
 * <p/>
 * From http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/ListDialog.java
 * <p/>
 * <p/>
 * ListDialog.java is a 1.4 class meant to be used by programs such as
 * ListDialogRunner.  It requires no additional files.
 * <p/>
 * Use this modal dialog to let the user choose one string from a long
 * list.  See ListDialogRunner.java for an example of using ListDialog.
 * The basics:
 * <pre>
 *  String[] choices = {"A", "long", "array", "of", "strings"};
 *  String selectedName = ListDialog.showDialog(
 *                              componentInControllingFrame,
 *                              locatorComponent,
 *                              "A description of the list:",
 *                              "Dialog Title",
 *                              choices,
 *                              choices[0]);
 * </pre>
 */
public class AttrListDialog extends JDialog implements ActionListener {

    private static AttrListDialog DIALOG;
    private static int VALUE_INDEX = -1;    // index of selected value. -1 if null or no selection

    private JList list;


    private void setValueIndex(int newValue) {
        VALUE_INDEX = newValue;
        list.setSelectedIndex(VALUE_INDEX);
    }

    private AttrListDialog(Frame frame, Component locationComp, String labelText,
                           String title, Object[] data, int initialValueIdx,
                           String longValue) {
        super(frame, title, true);

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        //
        final JButton setButton = new JButton("Set");
        setButton.setActionCommand("Set");
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        //main part of the dialog
        list = new JList2(data) {   // works around JDK 1.4 bug. see class for ref
            //Subclass JList to workaround bug 4832765, which can cause the
            //scroll pane to not let the user easily scroll up to the beginning
            //of the list.  An alternative would be to set the unitIncrement
            //of the JScrollBar to a fixed value. You wouldn't get the nice
            //aligned scrolling, but it should work.
            public int getScrollableUnitIncrement(Rectangle visibleRect,
                                                  int orientation,
                                                  int direction) {
                int row;
                if (orientation == SwingConstants.VERTICAL &&
                        direction < 0 && (row = getFirstVisibleIndex()) != -1) {
                    Rectangle r = getCellBounds(row, row);
                    if ((r.y == visibleRect.y) && (row != 0)) {
                        Point loc = r.getLocation();
                        loc.y--;
                        int prevIndex = locationToIndex(loc);
                        Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                        if (prevR == null || prevR.y >= r.y) {
                            return 0;
                        }
                        return prevR.height;
                    }
                }
                return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
            }
        };

        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        if (longValue != null) {
            list.setPrototypeCellValue(longValue); //get extra space
        }
//        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
//        list.setLayoutOrientation(JList.VERTICAL_WRAP);   // bug causes bug: won't scroll properly to show
        list.setVisibleRowCount(-1);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setButton.doClick(); //emulate button click
                }
            }
        });
        JScrollPane listScroller = new JScrollPane(list);
//        listScroller.setPreferredSize(new Dimension(250, 80));
        listScroller.setPreferredSize(new Dimension(400, 200));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor(list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        //Initialize values.
        setValueIndex(initialValueIdx);
        pack();
        setLocationRelativeTo(locationComp);
    }

    /**
     * Handle clicks on the Set and Cancel buttons.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if ("Set".equals(e.getActionCommand())) {
            int selectedIndex = list.getSelectedIndex();
            AttrListDialog.VALUE_INDEX = selectedIndex;
        } else {    // cancel
            AttrListDialog.VALUE_INDEX = -1;
        }
        AttrListDialog.DIALOG.setVisible(false);
    }

    /**
     * @param frameComp       determines which frame the dialog depends on;
     *                        should be a component in the dialog's controlling
     * @param locationComp    frame should be null if you want the dialog
     *                        to come up with its left corner in the center of
     *                        the screen; otherwise, it should be the component
     *                        on top of which the dialog should appear
     * @param labelText
     * @param title
     * @param possibleValues
     * @param initialValueIdx index of initial selection in possibleValues
     * @param longValue       sample item with longest length. pass null for none
     * @return index of the selected item. returns -1 if 'Cancel' clicked, or
     *         if there was no selection
     */
    public static int showDialog(Component frameComp, Component locationComp,
                                 String labelText, String title,
                                 String[] possibleValues, int initialValueIdx,
                                 String longValue) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
        DIALOG = new AttrListDialog(frame, locationComp, labelText, title,
                possibleValues, initialValueIdx, longValue);
        DIALOG.setVisible(true);
        return VALUE_INDEX;
    }


    /**
     * To work around Sun bug #4654916 'JList and JTree should scroll automatically with first-letter navigation'
     * - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4654916
     */
    public class JList2 extends JList {

        public JList2(final Object[] listData) {
            super(listData);
        }

        public void setSelectedIndex(int index) {
            super.setSelectedIndex(index);
            ensureIndexIsVisible(index);
        }

    }

}
