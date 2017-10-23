/**
 * $Id: MonetException.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: MonetException.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.monet;


/**
 * A simple exception class for Monet-related exceptions
 */
public class MonetException extends RuntimeException {

    public MonetException() {
        super();
    }


    public MonetException(String s) {
        super(s);
    }


    public MonetException(Throwable cause) {
        super(cause);
    }


    public MonetException(String message, Throwable cause) {
        super(message, cause);
    }


    public String toString() {
        String message = getMessage();
        return (message != null) ? (message) : "";
    }

}
