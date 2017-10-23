/**
 * $Id: RDNJFrame.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.event.PDragEventHandler;
import edu.umd.cs.piccolox.swing.PScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;


/**
 * A JFrame that displays a saved RDN as a graph, using the Piccolo-based
 * RDNCanvas.
 */
public class RDNJFrame extends JFrame {

    public RDNJFrame(File rdnFile) throws Exception {
        super("Graphing RDN: " + rdnFile);

        RDNInput rdnInput = RDNInput.loadRDNFile(rdnFile);
        RDNCanvas rdnCanvas = new RDNCanvas(rdnInput.getRptDocuments(), rdnInput.getNameMap());
        rdnCanvas.removeInputEventListener(rdnCanvas.getPanEventHandler());
        rdnCanvas.removeInputEventListener(rdnCanvas.getZoomEventHandler());
        rdnCanvas.addInputEventListener(new PDragEventHandler());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new PScrollPane(rdnCanvas), BorderLayout.CENTER);
        setSize(getPreferredSize());
        setVisible(true);
        ;
    }

    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }

}
