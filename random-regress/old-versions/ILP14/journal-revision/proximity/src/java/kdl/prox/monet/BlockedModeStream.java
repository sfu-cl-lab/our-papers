/**
 * $Id: BlockedModeStream.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
 * The contents of this file are subject to the MonetDB Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://monetdb.cwi.nl/Legal/MonetDBLicense-1.1.html
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the MonetDB Database System.
 *
 * The Initial Developer of the Original Code is CWI.
 * Portions created by CWI are Copyright (C) 1997-2007 CWI.
 * All Rights Reserved.
 */

package kdl.prox.monet;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Communicates with Monet 4.10 and up, using new Mapi protocol (blocked)
 * <p/>
 * Taken almost verbatim from the MonetSocketBlocked class in Monet's JDBC sources. Original
 * code by Fabian Groffen. Portions of original comments provided below:
 * <p/>
 * A Socket for communicating with the MonetDB database in block mode.
 * <br /><br />
 * This MonetSocket performs basic operations like sending the server a
 * message and/or receiving a line from it.  A small interpretation of
 * all what is read is done, to supply some basic tools for the using
 * classes.<br />
 * For each read line, it is determined what type of line it is
 * according to the MonetDB MAPI protocol.i  This results in a line to
 * be PROMPT, HEADER, RESULT, ERROR or UNKNOWN.i  Use the getLineType()
 * function to retrieve the type of the last line read.
 * <br /><br />
 * <p/>
 * This implementation of MonetSocket uses block mode on the mapi
 * protocol.  It allows sending a multi line query as data is sent in
 * 'blocks' that are prefixed with a two byte integer indicating its
 * size.  The least significant bit of this integer represents the last
 * block in a sequence.
 */
final class BlockedModeStream implements MonetStream {

    private InputStream fromMonetRaw;
    private OutputStream toMonetRaw;
    private Socket con;

    /**
     * The type of the last line read
     */
    private int lineType;

    final static int UNKNOWN = 0;   // no line, or UNKNOWN
    final static int ERROR = 1;     // line starting with !
    final static int HEADER = 2;    // line starting with %
    final static int RESULT = 3;    // line starting with [
    final static int PROMPT1 = 4;
    final static int PROMPT2 = 5;
    final static int SOHEADER = 6;  // line starting with &
    final static int REDIRECT = 7;  // line starting with ^
    final static int INFO = 8;      // line starting with #

    /**
     * The blocksize (hardcoded in compliance with stream.mx)
     */
    final static int BLOCK = 8 * 1024 - 2;

    /**
     * A buffer which holds the blocks read
     */
    private StringBuffer readBuffer;
    /**
     * The number of available bytes to read
     */
    private short readState = 0;
    private int readPos = 0;
    private boolean lastBlock = false;

    /**
     * A short in two bytes for holding the block size in bytes
     */
    private byte[] blklen = new byte[2];

    BlockedModeStream(String host, int port) {
        try {
            con = new Socket(host, port);
            // set nodelay, as it greatly speeds up small messages (like we
            // often do)
            con.setTcpNoDelay(true);

            // note: Always use buffered streams, as they perform better,
            // even though you know exactly which blocks you have to fetch
            // from the stream.  They are probably able to prefetch so the
            // IO is blocking while the program is still doing something
            // else.
            fromMonetRaw = new BufferedInputStream(con.getInputStream(), BLOCK + 2);
            toMonetRaw = new BufferedOutputStream(con.getOutputStream(), BLOCK + 2);

            readBuffer = new StringBuffer();

            String challenge = readLine();
            String response = getChallengeResponse(
                    challenge,
                    "unknown",
                    "",
                    "mil",
                    "demo",
                    "plain");
            response = "BIG:monetdb:{plain}monetdb:mil::";
            writeInternal(response);

            // read monet response till prompt
            List redirects = null;
            String err = "", tmp;
            int lineType = 0;
            while (lineType != PROMPT1) {
                if ((tmp = readLine()) == null) {
                    throw new IOException("Connection to server lost!");
                }
                lineType = getLineType();
                if (lineType == ERROR) {
                    err += "\n" + tmp.substring(1);
                } else if (lineType == INFO) {
                    System.out.println(tmp.substring(1));
                } else if (lineType == REDIRECT) {
                    if (redirects == null)
                        redirects = new ArrayList();
                    redirects.add(tmp.substring(1));
                }
            }

            if (err != "") {
                close();
                throw new MonetException(err.trim());
            }
            if (redirects != null) {
                close();
                throw new IllegalArgumentException("wants to redirect");
            }


        } catch (Exception e) {
            throw new MonetException("Unable to connect: " + e.getMessage());
        }
    }

    public void close() {
        synchronized (this) {
            try {
                fromMonetRaw.close();
                toMonetRaw.close();
                con.close();
            } catch (IOException e) {
                // ignore it
            }
        }
    }


    private String getChallengeResponse(
            String chalstr,
            String username,
            String password,
            String language,
            String database,
            String hash
    ) throws SQLException, IOException {
        int version = 0;
        String response;

        // hack alert
        readLine(); /* prompt */
        // parse the challenge string, split it on ':'
        String[] chaltok = chalstr.split(":");
        if (chaltok.length < 4) throw
                new SQLException("Server challenge string unusable!");

        // challenge string to use as salt/key
        String challenge = chaltok[0];
        // chaltok[1]; // server type, not needed yet
        try {
            version = Integer.parseInt(chaltok[2].trim());    // protocol version
        } catch (NumberFormatException e) {
            throw new SQLException("Protocol version unparseable: " + chaltok[3]);
        }

        // handle the challenge according to the version it is
        switch (version) {
            default:
                throw new SQLException("Unsupported protocol version: " + version);
            case 8:
                // proto 7 (finally) used the challenge and works with a
                // password hash.  The supported implementations come
                // from the server challenge.  We chose the best hash
                // we can find, in the order SHA1, MD5, plain.  Also,
                // the byte-order is reported in the challenge string,
                // which makes sense, since only blockmode is supported.
                // proto 8 made this obsolete, but retained the
                // byteorder report for future "binary" transports.  In
                // proto 8, the byteorder of the blocks is always little
                // endian because most machines today are.
                String hashes = (hash == null ? chaltok[3] : hash);
                String pwhash;
                if (hashes.indexOf("SHA1") != -1) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        md.update(password.getBytes("UTF-8"));
                        md.update(challenge.getBytes("UTF-8"));
                        byte[] digest = md.digest();
                        pwhash = "{SHA1}" + toHex(digest);
                    } catch (NoSuchAlgorithmException e) {
                        throw new AssertionError("internal error: " + e.toString());
                    } catch (UnsupportedEncodingException e) {
                        throw new AssertionError("internal error: " + e.toString());
                    }
                } else if (hashes.indexOf("MD5") != -1) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.update(password.getBytes("UTF-8"));
                        md.update(challenge.getBytes("UTF-8"));
                        byte[] digest = md.digest();
                        pwhash = "{MD5}" + toHex(digest);
                    } catch (NoSuchAlgorithmException e) {
                        throw new AssertionError("internal error: " + e.toString());
                    } catch (UnsupportedEncodingException e) {
                        throw new AssertionError("internal error: " + e.toString());
                    }
                } else if (hashes.indexOf("plain") != -1) {
                    pwhash = "{plain}" + password + challenge;
                } else {
                    throw new SQLException("no supported password hashes in " + hashes);
                }
                // TODO: some day when we need this, we should store
                // this
                if (chaltok[4].equals("BIG")) {
                    // byte-order of server is big-endian
                } else if (chaltok[4].equals("LIT")) {
                    // byte-order of server is little-endian
                } else {
                    throw new SQLException("Invalid byte-order: " + chaltok[5]);
                }

                // generate response
                response = "BIG:";    // JVM byte-order is big-endian
                response += username + ":" + pwhash + ":" + language;
                response += ":" + database + ":";

                return (response);
        }
    }

    /**
     * getLineType returns the type of the last line read.
     *
     * @return an integer representing the kind of line this is, one of the
     *         following constants: UNKNOWN, HEADER, ERROR, PROMPT,
     *         RESULT, REDIRECT, INFO
     */
    private int getLineType() {
        return (lineType);
    }

    private static String toHex(byte[] digest) {
        StringBuffer r = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            // zero out higher bits to get unsigned conversion
            int b = digest[i] << 24 >>> 24;
            if (b < 16) r.append("0");
            r.append(Integer.toHexString(b));
        }
        return (r.toString());
    }


    /**
     * readLine reads one line terminated by a newline character and
     * returns it without the newline character.  This operation can be
     * blocking if there is no information available (yet).  If a block
     * is marked as the last one, and the (left over) data does not end
     * in a newline, it is returned as the last "line" before returning
     * the prompt.
     *
     * @return a string representing the next line from the stream
     * @throws IOException if reading from the stream fails
     */
    public String readLine() throws MonetException {
        synchronized (fromMonetRaw) {
            try {
                /*
                * The blocked stream protocol consists of first a two byte
                * integer indicating the length of the block, then the
                * block, followed by another length + block.  The end of
                * such sequence is put in the last bit of the length, and
                * hence this length should be shifted to the right to
                * obtain the real length value first.
                * In this implementation we do not detect or use the user
                * flush as it is not needed to detect for us since the
                * higher level MAPI protocol defines a prompt which is used
                * for synchronisation.  We simply fetch blocks here as soon
                * as they are needed to process them line-based.
                *
                * The user-flush is a legacy thing now, and we simulate it
                * to the levels above, by inserting it at the end of each
                * 'lastBlock'.
                */
                int nl;
                while ((nl = readBuffer.indexOf("\n", readPos)) == -1) {
                    // not found, fetch us some more data
                    // start reading a new block of data if appropriate
                    if (readState == 0) {
                        if (lastBlock) {
                            if (readPos < readBuffer.length()) {
                                // there is still some stuff, but not
                                // terminated by a \n, send it to the user
                                String line = readBuffer.substring(readPos);

                                setLineType(readBuffer.charAt(readPos), line);

                                // move the cursor position
                                readPos = readBuffer.length();

                                return (line);
                            }

                            lastBlock = false;

                            lineType = PROMPT1;

                            return ("");    // we omit putting the prompt in here
                        }

                        // read next two bytes (short)
                        int size = fromMonetRaw.read(blklen);
                        if (size == -1) throw
                                new MonetException("End of stream reached");
                        if (size < 2) throw
                                new AssertionError("Illegal start of block");

                        // Get the int-value and store it in the readState.
                        // We store having the last block or not, for later
                        // to generate a prompt message.
                        readState = (short) (
                                (blklen[0] & 0xFF) >> 1 |
                                        (blklen[1] & 0xFF) << 7
                        );
                        lastBlock = (blklen[0] & 0x1) == 1;

                    }
                    // 'continue' fetching current block
                    byte[] data = new byte[BLOCK < readState ? BLOCK : readState];
                    int size = fromMonetRaw.read(data);
                    if (size == -1) throw
                            new MonetException("End of stream reached");

                    // update the state
                    readState -= size;

                    // clean up the buffer
                    readBuffer.delete(0, readPos);
                    readPos = 0;
                    // append the stuff to the buffer; let String do the charset
                    // conversion stuff
                    readBuffer.append(new String(data, 0, size, "UTF-8"));

                }
                // fill line, excluding newline
                String line = readBuffer.substring(readPos, nl);

                setLineType(readBuffer.charAt(readPos), line);

                // move the cursor position
                readPos = nl + 1;

                return (line);
            } catch (IOException e) {
                throw new MonetException(e);
            }
        }
    }

    /**
     * Returns the type of the string given.  This method assumes a
     * non-null string.
     *
     * @param first the first char from line
     * @param line  the string to examine
     * @return the type of the given string
     */
    private void setLineType(char first, String line) {
        lineType = UNKNOWN;
        switch (first) {
            case'!':
                lineType = ERROR;
                break;
            case'&':
                lineType = SOHEADER;
                break;
            case'%':
                lineType = HEADER;
                break;
            case'[':
                lineType = RESULT;
                break;
            case'^':
                lineType = REDIRECT;
                break;
            case'#':
                lineType = INFO;
                break;
            default:
                if (first == (char) 1 && line.length() == 2) {
                    if (line.charAt(1) == (char) 1) {
                        /* MAPI PROMPT1 */
                        lineType = PROMPT1;
                    } else if (line.charAt(1) == (char) 2) {
                        /* MAPI PROMPT2 (MORE) */
                        lineType = PROMPT2;
                    }
                }
                break;
        }
    }


    /**
     * writeLine puts the given string on the stream and flushes the
     * stream afterwards so the data will actually be sent.  The given
     * data String is wrapped within the query template.
     *
     * @param data the data to write to the stream
     * @throws IOException if writing to the stream failed
     */
    public void write(String data) {
        writeInternal(data + ";");
    }

    private void writeInternal(String data) throws MonetException {
        synchronized (toMonetRaw) {
            // In the same way as we read chunks from the socket, we
            // write chunks to the socket, so the server can start
            // processing while sending the rest of the input.
            try {
                byte[] bytes = data.getBytes("UTF-8");
                int len = bytes.length;
                int todo = len;
                short blocksize;
                while (todo > 0) {
                    if (todo <= BLOCK) {
                        // always fits, because of BLOCK's size
                        blocksize = (short) todo;
                        // this is the last block, so encode least
                        // significant bit in the first byte (little-endian)
                        blklen[0] = (byte) (blocksize << 1 & 0xFF | 1);
                        blklen[1] = (byte) (blocksize >> 7);
                    } else {
                        // always fits, because of BLOCK's size
                        blocksize = (short) BLOCK;
                        // another block will follow, encode least
                        // significant bit in the first byte (little-endian)
                        blklen[0] = (byte) (blocksize << 1 & 0xFF);
                        blklen[1] = (byte) (blocksize >> 7);
                    }

                    toMonetRaw.write(blklen);
                    // write the actual block
                    toMonetRaw.write(bytes, len - todo, blocksize);

                    todo -= blocksize;
                }

                // flush the stream
                toMonetRaw.flush();

                // reset the lineType variable, since we've sent data now and
                // the last line isn't valid anymore
                lineType = UNKNOWN;
            } catch (IOException e) {
                throw new MonetException(e);
            }
        }

    }


    /**
     * Destructor called by garbage collector before destroying this
     * object tries to disconnect the MonetDB connection if it has not
     * been disconnected already.
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
