/**
 * $Id: ViewFileJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: ViewFileJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import javax.swing.*;
import java.awt.*;
import java.io.*;


/**
 * A JFrame that displays a text File.
 */
public class ViewFileJFrame extends JFrame {

    private JTextArea jTextArea;


    public ViewFileJFrame(File file) {
        super("Viewing " + file);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        try {
            jTextArea.setText(getTextFromFile(file));
            jTextArea.select(0, 0);     // scroll to top (for long lists)
        } catch (IOException ioExc) {
            jTextArea.setText("Error getting text from file:\n" +
                    ioExc);
        }
        contentPane.add(new JScrollPane(jTextArea), BorderLayout.CENTER);
        setSize(600, 400);      // todo use getPreferredSize()
        setLocation(25, 25);    // todo caller should do
        setVisible(true);
    }


    /**
     * @param file
     * @return a String containing file's text
     */
    private String getTextFromFile(File file) throws IOException {
        long fileLen = file.length();
        byte[] buffer = new byte[(int) fileLen];
        BufferedInputStream bufInStr = new BufferedInputStream(new FileInputStream(file));
        bufInStr.read(buffer, 0, (int) fileLen);
        bufInStr.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(buffer, 0, (int) fileLen);
        return baos.toString();
    }


}
