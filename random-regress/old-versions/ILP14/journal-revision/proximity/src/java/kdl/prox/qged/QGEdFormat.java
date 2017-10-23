/**
 * $Id: QGEdFormat.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import java.text.Format;
import java.text.ParsePosition;


/**
 * A superclass to QGraph format classes. Manages an error message for users
 * when parseObject() fails. NB: Subclasses must manage errorMessage. Use the
 * parseFailed() utility method for this.
 */
public abstract class QGEdFormat extends Format {

    /**
     * My error message. Set to null if no error in parseObject(). Set to useful
     * error message if there was an error.
     */
    protected String errorMessage = null;

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Utility that returns null after setting my errorMessage and pos's
     * errorIndex.
     *
     * @param errorMessage
     * @param pos
     * @param errorIndex
     * @return
     */
    protected Object parseFailed(String errorMessage, ParsePosition pos, int errorIndex) {
        this.errorMessage = errorMessage;
        pos.setErrorIndex(errorIndex);
        return null;
    }

}
