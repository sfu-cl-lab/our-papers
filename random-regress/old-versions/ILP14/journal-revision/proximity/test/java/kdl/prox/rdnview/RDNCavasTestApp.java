/**
 * $Id: RDNCavasTestApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PDragEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Test driver for looking at viewer results
 */
public class RDNCavasTestApp extends JFrame {

    private static final Logger log = Logger.getLogger(RDNCavasTestApp.class);


    public RDNCavasTestApp() throws Exception {
        super("RDN Viewer Test App");

        // make test cases
        List rdnTests = makeRDNTests();

        // create RDNCanvas
        RDNInput firstRDNInput = (RDNInput) rdnTests.get(0);
        final RDNCanvas rdnCanvas = new RDNCanvas(firstRDNInput.getRptDocuments(),
                firstRDNInput.getNameMap());
        setArcsPickable(rdnCanvas);
        rdnCanvas.removeInputEventListener(rdnCanvas.getPanEventHandler());
        rdnCanvas.removeInputEventListener(rdnCanvas.getZoomEventHandler());
        rdnCanvas.addInputEventListener(new PDragEventHandler());
        rdnCanvas.addInputEventListener(new PBasicInputEventHandler() {
            public void mouseClicked(PInputEvent event) {
                super.mouseClicked(event);
                PNode pickedNode = event.getPickedNode();
                log.warn("mouseClicked(): " + pickedNode + "\n\t" +
                        pickedNode.getGlobalBounds());
            }
        });

        // create UI
        final JComboBox testJComboBox = new JComboBox(rdnTests.toArray());
        testJComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RDNInput rdnInput = (RDNInput) testJComboBox.getSelectedItem();
                rdnCanvas.loadRDN(rdnInput.getRptDocuments(), rdnInput.getNameMap());
                setArcsPickable(rdnCanvas);
//                printArcs(rdnCanvas);
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JButton(new AbstractAction("Test") {
            public void actionPerformed(ActionEvent e) {
                printNodes(rdnCanvas);
                printArcs(rdnCanvas);
            }
        }), BorderLayout.NORTH);
        getContentPane().add(rdnCanvas, BorderLayout.CENTER);
        getContentPane().add(testJComboBox, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setVisible(true);
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        try {
            new RDNCavasTestApp();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * @return List of RDNTest instances
     * @throws Exception
     */
    private List makeRDNTests() throws Exception {
        List rdnTests = new ArrayList();
        rdnTests.add(RDNCanvasTest.getRDNInput1());
        rdnTests.add(RDNCanvasTest.getRDNInput2());
        rdnTests.add(RDNCanvasTest.getRDNInput3());
        rdnTests.add(RDNCanvasTest.getRDNInput4());
        rdnTests.add(RDNCanvasTest.getRDNInput5());
        rdnTests.add(RDNCanvasTest.getRDNInput6());
        rdnTests.add(RDNCanvasTest.getRDNInput7());
        rdnTests.add(RDNCanvasTest.getRDNInput8());
        rdnTests.add(RDNCanvasTest.getRDNInput9());
        rdnTests.add(RDNCanvasTest.getRDNInput11());
        return rdnTests;
    }

    private void printArcs(final RDNCanvas rdnCanvas) {
        log.warn("** printArcs():");
        List arcs = rdnCanvas.getArcs();
        for (Iterator arcIter = arcs.iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            log.warn("  " + arc);
        }
    }

    private void printNodes(RDNCanvas rdnCanvas) {
        log.warn("** printNodes():");
        List types = rdnCanvas.getTypes();
        for (Iterator typeIter = types.iterator(); typeIter.hasNext();) {
            String type = (String) typeIter.next();
            log.warn("* type: " + type);
            List variables = rdnCanvas.getVariables(type);
            for (Iterator varIter = variables.iterator(); varIter.hasNext();) {
                String variable = (String) varIter.next();
                log.warn("  var: " + variable);
            }
        }
    }

    /**
     * Sets the pickable of all Arcs in rdnCanvas to true so that we can print
     * info about them (they're not pickable by default so that they can't be
     * dragged).
     *
     * @param rdnCanvas
     */
    private void setArcsPickable(RDNCanvas rdnCanvas) {
        for (Iterator arcIter = rdnCanvas.getArcs().iterator(); arcIter.hasNext();) {
            Arc arc = (Arc) arcIter.next();
            PPath line = rdnCanvas.getLineForArc(arc);
            line.setPickable(true);
        }
    }

}
