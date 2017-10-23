/**
 * $Id: GUI2App.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.app;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.gui2.BrowserJFrame;
import kdl.prox.gui2.ShortcutsAccessory;
import kdl.prox.qged.QGraphEditorJFrame;
import kdl.prox.util.Assert;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Main application for the browser-style Proximity database browser user
 * interface (open databases, run scripts, browse attribute and collections,
 * etc.) Manages JFrames created by me so that we know when to exit. Our policy
 * is this: Exit when the last known JFrame is closed.
 */
public class GUI2App {

    private static Logger log = Logger.getLogger(GUI2App.class);
    private static List jFrames = new ArrayList();  // all JFrames created by this application. used to figure out when to exit. newer are added to the end
    private static File CURRENT_DIRECTORY = null;


    public GUI2App() {
        BrowserJFrame browserJFrame = makeNewBrowserJFrame(null);
        browserJFrame.openInterpreter();
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Adds jFrame to my jFrames, and tells it to call removeJFrame() when it
     * closes.
     *
     * @param jFrame
     */
    private void addJFrame(final JFrame jFrame) {
        jFrames.add(jFrame);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                removeJFrame(jFrame);
            }
        });
    }

    /**
     * Quits the application.
     */
    public void exit() {
        System.exit(0); // shutdown hook takes care of closing the connection
    }

    /**
     * Prompts the user for a container to save a query into, validates the
     * entered name, asks permission to delete if existing container.
     *
     * @return non-null container name to use. return null if canceled, invalid,
     *         or didn't want to delete existing
     */
    public static String getContainerFromUser() {
        String containerName = JOptionPane.showInputDialog(null,
                "Enter Container", "Enter Query Output Container",
                JOptionPane.PLAIN_MESSAGE);
        if (containerName == null) {
            return null;     // user canceled
        }

        String isInvalidNameStr = Assert.isInvalidName(containerName);
        if (isInvalidNameStr != null) {
            JOptionPane.showMessageDialog(null, isInvalidNameStr,
                    "Invalid Container Name", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Container childContainer = DB.getRootContainer().getChild(containerName);
        if (childContainer != null) {
            if (!GUI2App.isUserSaysOK(null, "Delete container " +
                    childContainer.getName() + "?")) {
                return null;     // user didn't want to delete existing container
            } else {
                DB.getRootContainer().deleteChild(containerName);
            }
        }

        return containerName;
    }

    /**
     * One arg overload that uses null parent and default title.
     *
     * @param isSaveDialog
     * @return
     */
    public static File getFileFromUser(boolean isSaveDialog) {
        return getFileFromUser(null, isSaveDialog);
    }

    /**
     * Two arg overload that uses default title.
     */
    public static File getFileFromUser(Component parent, boolean isSaveDialog) {
        return getFileFromUser(parent, isSaveDialog, null);
    }

    /**
     * Prompts the user for a file to act on. Uses ShortcutsAccessory to ease
     * storing popular locations and/or files. Full overload.
     *
     * @param parent       parent component. pass null if none
     * @param isSaveDialog true if should show save dialog. false if open dialog
     * @param title        dialog title to show. pass null if don't care
     * @return File instance corresponding to selection. null if user canceled
     */
    public static File getFileFromUser(Component parent, boolean isSaveDialog,
                                       String title) {
        JFileChooser chooser = new JFileChooser();
        ShortcutsAccessory shortcuts = new ShortcutsAccessory(chooser, "prox");
        chooser.setAccessory(shortcuts);
        chooser.setCurrentDirectory(CURRENT_DIRECTORY);
        Dimension d = new Dimension(700, 400);
        chooser.setMinimumSize(d);
        chooser.setPreferredSize(d);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        while (true) {
            int showDialogResult = (isSaveDialog ? chooser.showSaveDialog(null) :
                    chooser.showOpenDialog(parent));
            if (showDialogResult == JFileChooser.APPROVE_OPTION) {
                CURRENT_DIRECTORY = chooser.getSelectedFile();
                if (isSaveDialog) {
                    File theFile = chooser.getSelectedFile();
                    if (theFile.exists()) {
                        // show confirm dialog box. Return on OK
                        int answer = JOptionPane.showConfirmDialog(null,
                                "Overwrite " + theFile.toString() + "?");
                        if (answer == JOptionPane.YES_OPTION) {
                            return theFile;
                        } else if (answer == JOptionPane.CANCEL_OPTION) {
                            return null;
                        } else if (answer == JOptionPane.NO_OPTION) {
                            // continue in the loop. Ask for the filename again.
                        }
                    } else {
                        return theFile;
                    }
                } else {
                    return CURRENT_DIRECTORY;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * @return most recently added BrowserJFrame, or null if there are none
     */
    public static BrowserJFrame getNewestBrowserJFrame() {
        ArrayList jFramesCopy = new ArrayList(jFrames);
        Collections.reverse(jFramesCopy);
        for (Iterator jFrameIter = jFramesCopy.iterator(); jFrameIter.hasNext();) {
            JFrame jFrame = (JFrame) jFrameIter.next();
            if (jFrame instanceof BrowserJFrame) {
                return (BrowserJFrame) jFrame;
            }
        }

        return null;
    }

    /**
     * @return returns the current GUI2App instance. returns a new one if one isn't found
     */
    public static GUI2App getOrMakeGUI2App() {
        BrowserJFrame newestBrowserJFrame = getNewestBrowserJFrame();
        if (newestBrowserJFrame != null) {
            return newestBrowserJFrame.getGui2App();
        } else {
            return new GUI2App();
        }
    }

    /**
     * Shows a dialog with question and returns true if the user clicked 'OK'.
     *
     * @param question
     * @return
     */
    public static boolean isUserSaysOK(Component parent, String question) {
        int result = JOptionPane.showConfirmDialog(parent, question, "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return (result == JOptionPane.OK_OPTION);
    }

    /**
     * Args: hostAndPort.
     */
    public static void main(String[] args) {
        // check args -- error message has to go to system out
        // because Log4J hasn't been initialized yet
        if (args.length != 1) {
            System.out.println("wrong number of args (" + args.length + ")");
            printUsage();
            return;
        }

        // set look and feel to system one (makes ShortcutsAccessory look
        // better, at least on Windows
        try {
            // UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Util.initProxApp();     // NB: lcf file is hard-coded relative to working directory
        String hostAndPort = args[0];
        DB.open(hostAndPort);
        new GUI2App();
    }

    /**
     * Creates a new BrowserJFrame showing proxURL. NB: It's crucial for methods
     * to create new BrowserJFrame instances via this method, rather than
     * directly, because this does important management of frames.
     *
     * @param proxURL starting address to show. null if should show home page
     */
    public BrowserJFrame makeNewBrowserJFrame(String proxURL) {
        BrowserJFrame browserJFrame = new BrowserJFrame(this);
        setNewBrowserJFrameLocation(browserJFrame);
        addJFrame(browserJFrame);
        browserJFrame.setVisible(true);
        if (proxURL != null) {
            browserJFrame.goTo(proxURL);
        }
        return browserJFrame;
    }

    private static void printUsage() {
        System.out.println("Usage: java " + GUI2App.class.getName() +
                " hostAndPort\n" +
                "\thostAndPort: <host>:<port>\n");
    }

    /**
     * Called when a JFrame added by addJFrame() is closing, removes it from my
     * jFrames and exits if it's the last one.
     *
     * @param jFrame
     */
    private void removeJFrame(JFrame jFrame) {
        if (jFrames.size() == 1 && isModifiedQueryEditors()) {
            int result = JOptionPane.showConfirmDialog(jFrame, "There are " +
                    "modified queries; discard changes and quit?", "Discard Changes?",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        jFrame.setVisible(false); // because BrowserJFrame() calls setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        boolean isRemoved = jFrames.remove(jFrame);
        if (!isRemoved) {
            log.warn("jFrame wasn't a known one: " + jFrame);
            return;
        }

        if (jFrames.size() == 0) {
            exit();
        }
    }

    /**
     * @return true if there is at least one modified query editor open.
     */
    private boolean isModifiedQueryEditors() {
        for (Iterator jFrameIter = jFrames.iterator(); jFrameIter.hasNext();) {
            JFrame jFrame = (JFrame) jFrameIter.next();
            if (!(jFrame instanceof QGraphEditorJFrame)) {
                continue;
            } else {
                QGraphEditorJFrame qGraphEditorJFrame = (QGraphEditorJFrame) jFrame;
                if (qGraphEditorJFrame.isDirty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets browserJFrame's location based on the last JFrame saved in my
     * jFrames. Does nothing if there are none.
     *
     * @param browserJFrame the new browser. NB: should not yet be on my jFrames
     *                      todo might be nice to figure which is on top (layer) and use it instead
     */
    private void setNewBrowserJFrameLocation(BrowserJFrame browserJFrame) {
        if (jFrames.size() == 0) {
            return; // leave default location
        }

        JFrame lastJFrame = (JFrame) jFrames.get(jFrames.size() - 1);
        browserJFrame.setLocation(lastJFrame.getX() + 32, lastJFrame.getY() + 32);  // arbitrary spacing
    }

    //
    // inner classes
    //

    public static class ShutdownHook extends Thread {

        public ShutdownHook() {
        }

        public void run() {
            DB.close();
        }
    }

}
