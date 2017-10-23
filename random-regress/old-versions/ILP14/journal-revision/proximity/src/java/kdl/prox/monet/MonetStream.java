/**
 * $Id: MonetStream.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.monet;

/**
 * Implements the low-level communication with a Monet server, via a TCP/IP socket
 * Encapsulates the sockets and buffers and their low-level routines, and provides hooks
 * to read, write, and close
 */
public interface MonetStream {
    String readLine();

    void write(String msg);

    void close();
}
