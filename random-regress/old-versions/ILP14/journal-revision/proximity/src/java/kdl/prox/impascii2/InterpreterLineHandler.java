/**
 * $Id: InterpreterLineHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: InterpreterLineHandler.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

/**
 * Callback for Interpeter.
 */
public interface InterpreterLineHandler {

    public void doAttribute(String name, String value);

    public void doComment(String comment);

    public void doLink(String o1Name, String o2Name);

    public void doObject(String name);
}
