/**
 * $Id: TextPaneJythonConsole.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.script;

import kdl.prox.app.GUI2App;
import kdl.prox.gui2.BrowserJFrame;
import org.apache.log4j.Logger;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List;
import java.util.Vector;


/**
 * An interactive console that supports Jython evaluation, nesting of commands,
 * and completion.
 * <p/>
 * This is based heavily on Javier Iglesias's code, provided as part of GUESS system
 * We cleaned up the code, removed features that weren't needed for our interpreter,
 * and added completion.
 * (see http://www.hpl.hp.com/research/idl/projects/graphs/)
 * Their copyright notice follows
 * <p/>
 * <p>The text pane that implement short-cut and general behaviour from a bash
 * interpreter.</p>
 * <p/>
 * <p/>
 * <blockquote>
 * <p>Copyright (C) 2003, 2004 Javier Iglesias.</p>
 * <p/>
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.</p>
 * <p/>
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.</p>
 * <p/>
 * <p>You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA</p>
 * </blockquote>
 *
 * @author javier iglesias &lt;javier.iglesias@alawa.ch&gt;
 * @version $Id: TextPaneJythonConsole.java 3658 2007-10-15 16:29:11Z schapira $
 */
public class TextPaneJythonConsole extends JTextPane {

    private static final Logger log = Logger.getLogger(TextPaneJythonConsole.class);

    private static final String COMMAND_STYLE = "command";
    private static final String COMMENT_STYLE = "comment";
    private static final String PROMPT_STYLE = "prompt";
    private static final String ANSWER_STYLE = "answer";
    private static final String ERROR_STYLE = "error";
    private PythonInterpreter jython;
    private ConsoleDocument document;
    private int currentItem;
    private Vector history;


    /**
     * A set of document and writer, wrapping around a Jython interpreter
     */
    public TextPaneJythonConsole(PythonInterpreter jython) {
        try {
            this.jython = jython;

            // Create the document component, with its styles and key map
            document = new ConsoleDocument(this);
            setStyledDocument(document);
            prepareKeymap(getKeymap());

            history = prepareHistory();

            jython = prepareInterpreter();
            String welcomeMsg = "> Welcome to the Proximity Python Interpreter! \n" +
                    "> Use Ctrl-A and Ctrl-E to move to the beginning or end of the line\n" +
                    "> Use Ctrl-K and Ctrl-Y to kill (cut) and yank (paste)\n" +
                    "> Use UP and DOWN arrows to move through command history.\n" +
                    "> Use Ctrl-Alt-S to save the command history to a file.\n" +
                    "> Use Ctrl-Alt-C to clear the history.\n" +
                    "> Use Ctrl-Alt-X to execute a saved history --or any Jython file.\n" +
                    ">\n" +
                    "> Use Ctrl-Space to complete method names.\n" +
                    "> Use Ctrl-P after a '(' to get the argument list for a method.\n" +
                    ">\n" +
                    "> The 'prox' variable holds a reference to the main Proximity object.\n" +
                    "> Use printNST(nst, filter, colList, rowList) to print the contents of NSTs.\n" +
                    "\n";
            document.writeComment(welcomeMsg);
            document.prompt();

        } catch (Exception e) {
            log.warn(e);
        }
    }


    /**
     * Editor commands
     */
    private class EditorActionClipboardKill extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.clipboardKillCommandLine();
        }
    }

    private class EditorActionClipboardPaste extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.clipboardPaste();
        }
    }

    private class EditorActionCommandComplete extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.completeCommandLine();
        }
    }

    private class EditorActionCommandCompleteParams extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.completeParametersPopUp();
        }
    }

    private class EditorActionDelete extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.delete();
        }
    }

    private class EditorActionFileExecute extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            executeFile();
        }
    }

    private class EditorActionMoveEnd extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            int offset = document.getCommandLineEndOffset();
            if (document.getIsOffsetOnCommandLine(offset)) {
                setCaretPosition(offset);
            }
        }
    }

    private class EditorActionMoveLeft extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            int offset = getCaretPosition() - 1;
            if (document.getIsOffsetOnCommandLine(offset)) {
                setCaretPosition(offset);
            }
        }
    }

    private class EditorActionMoveRight extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            int offset = getCaretPosition() + 1;
            if (document.getIsOffsetOnCommandLine(offset)) {
                setCaretPosition(offset);
            }
        }
    }

    private class EditorActionMoveStart extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            int offset = document.getCommandLineStartOffset();
            if (document.getIsOffsetOnCommandLine(offset)) {
                setCaretPosition(offset);
            }
        }
    }

    private class EditorActionHistoryNext extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.historySelectNextItem();
        }
    }

    private class EditorActionHistoryPrevious extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            document.historySelectPreviousItem();
        }
    }

    private class EditorActionHistoryReset extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            resetHistory();
        }
    }

    private class EditorActionHistorySave extends AbstractAction {
        public void actionPerformed(ActionEvent ae) {
            saveHistory();
        }
    }

    public void executeFile() {
        // prompt for file
        File file = GUI2App.getFileFromUser(this, false);
        if (file == null) {
            return;
        }
        document.insertAndExecuteCommand("execfile(\"" + file.toString() + "\")");
    }


    public String getEnvironment(String key) {
        try {
            return jython.eval("ENV['" + key + "']").toString();
        } catch (Exception e) {
            return "";
        }
    }


    private Vector prepareHistory() {
        Vector answer = new Vector();
        answer.add(""); // top sentinelle
        answer.add(""); // bottom sentinelle
        currentItem = 0;

        return answer;
    }


    private PythonInterpreter prepareInterpreter() {
        jython.exec("ENV = {}");
        setEnvironment("PS1", ">>> ");
        setEnvironment("PS2", "... ");
        setEnvironment("PATH_SEPARATOR", System.getProperty("path.separator"));
        setEnvironment("COLS", "80");

        return jython;
    }


    private void prepareKeymap(Keymap map) {
        // special characters
        // history actions
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                new EditorActionHistoryPrevious());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                new EditorActionHistoryNext());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke("control alt S"),
                new EditorActionHistorySave());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke("control alt C"),
                new EditorActionHistoryReset());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke("control alt X"),
                new EditorActionFileExecute());

        // replace actions that move caret
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                new EditorActionMoveLeft());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                new EditorActionMoveRight());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_B,
                KeyEvent.CTRL_MASK), new EditorActionMoveLeft());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                KeyEvent.CTRL_MASK), new EditorActionMoveRight());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                KeyEvent.CTRL_MASK), new EditorActionMoveStart());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                KeyEvent.CTRL_MASK), new EditorActionMoveEnd());

        // delete
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                KeyEvent.CTRL_MASK), new EditorActionDelete());

        // replace CTRL+... clipboard actions
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                KeyEvent.CTRL_MASK), new EditorActionClipboardKill());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                KeyEvent.CTRL_MASK), new EditorActionClipboardPaste());

        // completion
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
                KeyEvent.CTRL_MASK), new EditorActionCommandComplete());
        map.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.CTRL_MASK), new EditorActionCommandCompleteParams());
    }

    public void resetHistory() {
        history = prepareHistory();
        JOptionPane.showMessageDialog(this, "History Cleared");
    }

    public void saveHistory() {
        // prompt for file
        File file = GUI2App.getFileFromUser(this, true);
        if (file == null) {
            return;
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write("# Proximity Interpreter History");
            for (int historyIdx = 0; historyIdx < history.size(); historyIdx++) {
                String command = (String) history.elementAt(historyIdx);
                out.write(command + '\n');
            }
            JOptionPane.showMessageDialog(this, "History Saved to " + file.toString());
        } catch (IOException exc) {
            log.error(exc);
            JOptionPane.showMessageDialog(null, exc.getMessage(),
                    "Error Saving History", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }


    public void setEnvironment(String key, String value) {
        jython.exec(new StringBuffer("ENV['").append(key).append("'] = '")
                .append(value).append("'")
                .toString());
    }


    private class ConsoleDocument extends DefaultStyledDocument {
        private Component guicomponent;

        private int multilining = 0; // level of multilining
        private StringBuffer multiline;
        private boolean CLEARING = false;

        private Clipboard clipboard = null;


        public ConsoleDocument(Component component) {
            guicomponent = component;
            initializeStyles();
            clipboardGet();
        }


        /**
         * Clipboard commands
         */
        public void clipboardAdd(String s) {
            clipboardGet();
            if (clipboard != null) {
                try {
                    StringSelection ss = new StringSelection(s);
                    clipboard.setContents(ss, ss);
                } catch (Exception ex) {
                    log.warn(ex);
                }
            }
        }


        private void clipboardGet() {
            if (clipboard == null) {
                try {
                    clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                } catch (Exception ex) {
                    log.warn(ex);
                }
            }
        }


        public void clipboardKillCommandLine() {
            try {
                int start = getCaretPosition();
                int end = getCommandLineEndOffset();
                clipboardAdd(getText(start, end - start));
                remove(start, end - start);
            } catch (Exception e) {
                log.warn(e);
            }
        }


        public void clipboardPaste() {
            if (clipboard != null) {
                try {
                    Transferable clipData = clipboard.getContents(clipboard);
                    if (clipData != null && clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String s = (String) (clipData.getTransferData(DataFlavor.stringFlavor));
                        insertString(getCaretPosition(), s, getStyle(COMMAND_STYLE));
                    }
                } catch (Exception e) {
                    log.warn(e);
                }
            }
        }


        private void commandLineSet(String command) {
            int start = getCommandLineStartOffset();
            int end = getCommandLineEndOffset();

            try {
                remove(start, end - start);
                insertString(getCommandLineStartOffset(), command, getStyle(COMMAND_STYLE));
            } catch (Exception e) {
                log.warn(e);
            }
        }


        /**
         * Completion operations
         * It only completes at the beginning of a line (including an indented line)
         */
        public void completeCommandLine() {
            try {
                int start = getCommandLineStartOffset();
                int end = getCommandLineEndOffset();
                String command = getText(start, end - start);

                // make sure that the command includes a variable name followed by a .
                int lastPeriodIdx = command.lastIndexOf('.');
                if (lastPeriodIdx == -1) {
                    log.warn("* Enter a full variable name followed by a '.'");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                // get the list of completions
                final String commandUpToPeriod = command.substring(0, lastPeriodIdx);
                String commandAfterPeriod = command.substring(lastPeriodIdx + 1);
                List list = CmdCompletion.getMethodNames(jython, commandUpToPeriod.trim(), commandAfterPeriod);

                if (list.size() == 0) {
                    log.warn("* No Completions");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                } else if (list.size() == 1) {
                    // complete directly on the command line
                    String longestCommonPrefix = CmdCompletion.getLongestPrefix(list, false);
                    prompt();
                    commandLineSet(commandUpToPeriod + "." + longestCommonPrefix);
                } else {
                    // show popup
                    JPopupMenu popup = new JPopupMenu();
                    ActionListener selectActionListener = new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JMenuItem source = (JMenuItem) (e.getSource());
                            try {
                                commandLineSet(commandUpToPeriod + "." + source.getText());
                            } catch (Exception exc) {
                                log.warn(exc);
                            }
                        }
                    };
                    for (int i = 0; i < list.size(); i++) {
                        String methodName = (String) list.get(i);
                        if (!CmdCompletion.isExcludedMethodName(methodName)) {
                            JMenuItem menuItem = new JMenuItem(methodName);
                            Font font = menuItem.getFont();
                            menuItem.setFont(new Font(font.getName(), font.getStyle(), font.getSize() - 2));
                            menuItem.addActionListener(selectActionListener);
                            popup.add(menuItem);
                        }
                    }
                    Rectangle caretPos = modelToView(getCaret().getDot());
                    popup.show(guicomponent, caretPos.x, caretPos.y);
                }
            } catch (Exception e) {
                log.warn(e);
            }
        }


        private void completeParametersPopUp() {
            try {
                int start = getCommandLineStartOffset();
                int end = getCommandLineEndOffset();
                String command = getText(start, end - start);

                // make sure that the command includes a variable name followed by a .
                int lastPeriodIdx = command.lastIndexOf('.');
                if (lastPeriodIdx == -1) {
                    log.warn("* Enter a full variable name followed by a '.'");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                // get the list of completions
                String commandUpToPeriod = command.substring(0, lastPeriodIdx);
                String commandAfterPeriod = command.substring(lastPeriodIdx + 1);

                // make sure that the command includes a method name followed by a (
                int firstParenIdx = commandAfterPeriod.indexOf('(');
                if (firstParenIdx == -1) {
                    log.warn("* Enter a method name followed by a '('");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                // remove everything after the ( and use that go get the methods and their arguments
                commandAfterPeriod = commandAfterPeriod.substring(0, firstParenIdx);
                List list = CmdCompletion.getMethodArgs(jython, commandUpToPeriod.trim(), commandAfterPeriod);

                if (list.size() == 0) {
                    log.warn("* No Method Found\n");
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                JPopupMenu popup = new JPopupMenu();
                for (int i = 0; i < list.size(); i++) {
                    String methodArgs = (String) list.get(i);
                    JMenuItem menuItem = new JMenuItem(methodArgs);
                    Font font = menuItem.getFont();
                    menuItem.setFont(new Font(font.getName(), font.getStyle(), font.getSize() - 2));
                    // no action listener
                    popup.add(menuItem);
                }
                Rectangle caretPos = modelToView(getCaret().getDot());
                popup.show(guicomponent, caretPos.x, caretPos.y);

            } catch (Exception e) {
                log.warn(e);
            }
        }

        /**
         * Delete one character
         */
        public void delete() {
            try {
                if (getCaretPosition() < getLength()) {
                    remove(getCaretPosition(), 1);
                }
            } catch (Exception e) {
                log.warn(e);
            }
        }

        /**
         * Offset methods. Find position of caret or command line
         */
        protected int getCommandLineEndOffset() {
            return getCommandLineEndOffset(getLength()) - 1;
        }


        protected int getCommandLineEndOffset(int offset) {
            return getParagraphElement(offset).getEndOffset();
        }


        protected int getCommandLineStartOffset() {
            return getCommandLineStartOffset(getLength());
        }


        protected int getCommandLineStartOffset(int offset) {
            return getParagraphElement(offset).getStartOffset() +
                    getEnvironment("PS1").length();
        }


        private boolean getIsOffsetOnCommandLine(int offset) {
            int start = getCommandLineStartOffset();

            return ((offset >= start) && (offset <= getLength()));
        }


        private int getOffsetOnCommandLine(int offset) {
            if (!getIsOffsetOnCommandLine(offset)) {
                offset = getLength();
                if (document.getIsOffsetOnCommandLine(offset)) {
                    setCaretPosition(offset);
                }
            }

            return offset;
        }


        protected int getWordStartOffset(int offset) throws BadLocationException {
            int start = getCommandLineStartOffset(offset);
            String text = getText(start, offset - start).trim();

            return start + Math.max(0, text.lastIndexOf(" ") + 1);
        }


        /**
         * Executes the complete command passed in
         *
         * @param command
         * @throws BadLocationException
         */
        private void execCommand(String command) throws BadLocationException {
            BrowserJFrame.showWaitCursor(guicomponent, true);
            try {
                // do command execution
                DocumentWriter buffer = new DocumentWriter(this, getStyle(ANSWER_STYLE));
                jython.setOut(buffer);
                Object oval = jython.eval(command);
                jython.set("_", oval);
                if (oval instanceof PyFunction) {
                    jython.set("_", jython.eval("apply(_, ())"));
                }
                jython.exec("if _ != None: print _");
            } catch (Exception e) {
                try {
                    jython.exec(command);
                } catch (Throwable e2) {
                    superInsertString(getLength(),
                            e.getClass() + "\n",
                            getStyle(ERROR_STYLE));
                    superInsertString(getLength(),
                            e.toString() + "\n",
                            getStyle(ERROR_STYLE));
                }
            }
            BrowserJFrame.showWaitCursor(guicomponent, false);
        }


        /**
         * History commands
         */
        private void historyAddItem(String command) {
            if ((command.indexOf('\n') == -1) && // FIXME : find a way to include multiline commands in history
                    !history.get(history.size() - 2).equals(command)) {
                history.insertElementAt(command, history.size() - 1);
            }

            currentItem = history.size() - 1;
        }


        public void historySelectPreviousItem() {
            currentItem = Math.max(1, --currentItem);
            commandLineSet((String) history.get(currentItem));
        }


        public void historySelectNextItem() {
            currentItem = Math.min(history.size() - 1, ++currentItem);
            commandLineSet((String) history.get(currentItem));
        }


        private void initializeStyles() {
            Style def =
                    StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

            def = addStyle(null, def);

            Style style = addStyle(PROMPT_STYLE, def);
            StyleConstants.setBold(style, true);

            style = addStyle(COMMAND_STYLE, def);
            StyleConstants.setBold(style, true);

            style = addStyle(ANSWER_STYLE, def);
            StyleConstants.setForeground(style, Color.darkGray);

            style = addStyle(ERROR_STYLE, def);
            StyleConstants.setForeground(style, Color.red);

            style = addStyle(COMMENT_STYLE, def);
            StyleConstants.setForeground(style, Color.blue);
        }


        public void insertAndExecuteCommand(String command) {
            commandLineSet(command);
            try {
                insertString(getCommandLineEndOffset(), "\n", getStyle(COMMAND_STYLE));
            } catch (BadLocationException e) {
                log.warn(e);
            }
        }

        public void insertString(int offset, String text, AttributeSet a) throws BadLocationException {
            // only insertions on the last line are allowed
            offset = getOffsetOnCommandLine(offset);

            // if it is not a command to execute, simply print the line
            if (text.indexOf('\n') == -1) {
                superInsertString(offset, text, getStyle(COMMAND_STYLE));
                return;
            }

            // THERE IS A COMMAND TO EXECUTE

            // move to end of the line, and insert the return
            moveToEndOfLine();
            superInsertString(getCommandLineEndOffset(), "\n", getStyle(COMMAND_STYLE));

            // delimit the command to execute
            int start = getCommandLineStartOffset(offset);
            int length = getCommandLineEndOffset() - start - 1; // "-1" to get rid of final '\n'
            String command = getText(start, length);
            if (command.length() > 0)
                historyAddItem(command);

            // this is a multiline command start
            if (command.trim().endsWith(":")) {
                multilining++;
                if (multiline == null) {
                    multiline = new StringBuffer(command).append('\n');
                } else {
                    multiline.append(command).append('\n');
                    // 'else' statements fall here
                    if ((command.indexOf("else") > -1) ||
                            (command.indexOf("elif") > -1) ||
                            (command.indexOf("except") > -1) ||
                            (command.indexOf("finally") > -1)) {
                        multilining--;
                    }
                }
                prompt();
                return;
            }

            // this expression is part of a multiline command
            if (multilining > 0) {
                multiline.append(command).append("\n");
                // check if a level has been closed
                if (command.trim().equals("pass") || !command.startsWith("\t") ||
                        (command.length() == 0)) {
                    multilining--;
                    if (multilining > 0) {
                        prompt();
                        return;
                    }

                    // retrieve the complete command
                    command = multiline.toString();
                    multiline = null;
                } else {
                    prompt();
                    return;
                }
            }

            // let Jython execute the command if necessary
            if (command.trim().length() > 0) {
                execCommand(command);
            }

            // append the prompt
            prompt();
        }


        private void moveToEndOfLine() {
            int offset1 = getCommandLineEndOffset();
            if (getIsOffsetOnCommandLine(offset1)) {
                setCaretPosition(offset1);
            }
        }


        private void prompt() throws BadLocationException {
            String prompt;
            if (multilining > 0) {
                prompt = getEnvironment("PS2");
            } else {
                prompt = getEnvironment("PS1");
            }
            superInsertString(getLength(), prompt, getStyle(PROMPT_STYLE));
        }


        /**
         * Interface methods
         */

        public void remove(int offset, int length) throws BadLocationException {
            // refuse to remove anything that is not on the latest line
            if (getIsOffsetOnCommandLine(offset) || CLEARING) {
                super.remove(offset, length);
            }
        }


        /**
         * Inserts directly onto the document, without executing or checking for multiline
         */
        private void superInsertString(int offset, String text, AttributeSet a)
                throws BadLocationException {
            super.insertString(offset, text, a);
        }


        public void writeComment(String commentMsg) throws BadLocationException {
            superInsertString(getLength(), commentMsg, getStyle(COMMENT_STYLE));
        }
    }

    /**
     * Used to redirect output from jython interpreter
     * Writes styled text
     */
    private class DocumentWriter extends Writer {
        private ConsoleDocument doc = null;
        private Style style = null;


        public DocumentWriter(ConsoleDocument doc, Style style) {
            this.doc = doc;
            this.style = style;
        }


        public void close() {
        }


        public void flush() {
        }


        public void write(String text) {
            try {
                doc.superInsertString(doc.getLength(), text, style);
            } catch (Exception e) {
                log.warn(e);
            }
        }


        public void write(char[] cbuf, int off, int len) {
            write(cbuf, off, len);
        }

    }


}
