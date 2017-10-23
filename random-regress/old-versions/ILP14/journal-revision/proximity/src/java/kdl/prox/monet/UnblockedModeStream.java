/**
 * $Id: UnblockedModeStream.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/* $Id */

package kdl.prox.monet;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;


/**
 * Communicates with Monet up to 4.6.2, using old Mapi protocol (non-blocked)
 */
public class UnblockedModeStream implements MonetStream {

    /*
     * The status of a MonetStream object is accessible from the status field. It should
     * be consulted before most commands.
     */
    private final static int READY = 0;
    private final static int RECV = 1;
    private final static int DISABLED = 2;
    private final static int MONETERROR = 3;
    private int status = READY;


    /**
     * The sockets and buffers
     */
    private Socket socket;
    private BufferedWriter toMonet;
    private BufferedReader fromMonet;


    /**
     * Class-based log4j logger.
     */
    private static Logger log = Logger.getLogger(UnblockedModeStream.class);


    /**
     * Constructor. Connects to a server, sets up the socket and the buffers, and logs in
     * Sets status to READY
     *
     * @param host
     * @param port
     * @param user
     * @
     */
    public UnblockedModeStream(String host, int port, String user) {
        try {
            socket = new Socket(host, port);
            fromMonet = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            toMonet = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            throw new MonetException("Failed to contact server " + host + " on port " + port + ": " + e + "\n");
        }
        write(user);
        promptMonet();
        promptMonet();
    }


    /**
     * Closes the stream.
     */
    public void close() {
        if (status == DISABLED) {
            throw new MonetException("close(): Stream already closed.");
        }
        //notify Mserver that the client is leaving
        write("quit();");
        // close buffers and socket
        try {
            toMonet.close();
            fromMonet.close();
            socket.close();
        } catch (IOException e) {
            log.debug("close(): IOException ignored : " + e);
        } finally {
            // record status as DISABLED
            status = DISABLED;
        }
    }


    /**
     * Reads a character from the input stream and returns it
     *
     * @return
     * @
     */
    private final char getChar() {
        char b;
        try {
            b = (char) fromMonet.read();
        } catch (IOException e) {
            throw new MonetException("getChar(): Failure to read next byte from Monet");
        }
        return b;
    }


    /**
     * The interaction protocol with the monet database server is very
     * simple. Each command (query) ends with '\n'. The results sent back
     * will end with '\1' monet prompt '\1'.
     * This private member function finds these '\1' markers.
     */
    private void promptMonet() {
        while (getChar() != '\1') {
        }
        // Now ready to get next command
        status = READY;
    }


    /**
     * Returns a line from the input buffer
     *
     * @return next line from the input buffer
     * @
     */
//    public String readLine() {
//
//        if (status == DISABLED) {
//            throw new MonetException("readline(): Stream already closed.");
//        }
//
//        if (status == READY) {
//            return "";
//        }
//
//        // Read the first character in the line and interpret it
//        char c = getChar();
//        switch (c) {
//            case '\n': {
//                // ignore blank lines
//                return readLine();
//            }
//
//            case '\1': {
//                // found marker. Eat the markers and return empty line
//                promptMonet();
//                return "";
//            }
//
//            case '#': {
//                // debugmask messages and BAT headings
//                // print first, ignore second
//                String errorText = readToNewLine(c);
//                if (Util.milMessages.isDebugEnabled() && !errorText.startsWith("#--") && !errorText.startsWith("# ")) {
//                    Util.milMessages.debug("#  " + errorText.trim());
//                }
//                return readLine();
//            }
//
//            case '!': {
//                // Error or warning
//                // if it's an error, throw an exception
//                // if a warning, display it to the logger and continue reading
//                String errorText = readToNewLine(c).trim();
//                if (errorText.startsWith("!ERROR")) {
//                    status = MONETERROR;
//                    Util.milMessages.error(errorText);
//                    String nextText = readLine();
//                    if (nextText.length() == 0) {
//                        throw new MonetException(errorText);
//                    } else {
//                        return nextText;
//                    }
//                } else {
//                    Util.milMessages.warn("#  " + errorText);
//                    return readLine();
//                }
//            }
//
//            default : {
//                // no special case. Return the rest of the line, dropping the last \n
//                return readToNewLine(c).trim();
//            }
//        }
//    }
    public String readLine() {

        if (status == DISABLED) {
            throw new MonetException("readline(): Stream already closed.");
        }

        if (status == READY) {
            System.out.println("Returning status rady");
            return "";
        }

        // Read the first character in the line and interpret it
        try {
            while (true) {
                char c = (char) fromMonet.read();
                if (c == '\1') { // found marker. Eat the markers and return empty line
                    promptMonet();
                    return "";
                } else if (c == '\n') {
                    // ignore it. return next line
                } else if (c == '#') {
                    // ignore comments. read to end of line and then get next char
                    fromMonet.readLine();
                } else if (c == '!') {
                    // Error or warning
                    // if it's an error, throw an exception
                    // if a warning, display it to the logger and continue reading
                    String errorText = c + fromMonet.readLine().trim();
                    if (errorText.startsWith("!ERROR")) {
                        status = MONETERROR;
                        String nextText = readLine();
                        if (nextText.length() == 0) {
                            throw new MonetException(errorText);
                        } else {
                            return nextText;
                        }
                    }
                } else {
                    // no special case. Return the rest of the line
                    return c + fromMonet.readLine();
                }
            }
        } catch (IOException e) {
            throw new MonetException("IOException: " + e);
        }
    }


    /**
     * Read the rest of the line from the input buffer, and add it to the first character c
     */
    private String readToNewLine(char c) {
        String line;
        try {
            line = fromMonet.readLine();
        } catch (IOException e) {
            throw new MonetException("Communication broken");
        }
        return c + line;
    }


    /**
     * This low-level operation sends a string to the server.
     */
    public void write(String msg) {

        if (status == DISABLED) {
            throw new MonetException("write() : Stream already closed.");
        }
        try {
            if (status == RECV || status == MONETERROR) {
                log.warn("write(): Stream in RECV or ERROR monet. Flushing()");
                toMonet.flush();
            }

            toMonet.write(msg, 0, msg.length());
            toMonet.write(";\n", 0, 2);
            toMonet.flush();
        } catch (IOException e) {
            throw new MonetException("Can not write to Monet");
        }
        status = RECV;
    }


}

