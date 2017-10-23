/**
 * $Id: DBLineHandler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: DBLineHandler.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import kdl.prox.monet.MonetException;


/**
 * Handler that translates line handling into object and link creation, which
 * is delegated to the passed DBHander.
 */
public class DBLineHandler implements InterpreterLineHandler {

    private DBHandler dbHandler;
    private boolean hasSeenFirstItemDef = false;

    public DBLineHandler(DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public void doAttribute(String name, String value) {
        if (!hasSeenFirstItemDef) {
            throw new IllegalArgumentException("tried to start attribute " +
                    "before first object or link definition");
        }
        try {
            dbHandler.addAttribute(name, value);
        } catch (MonetException monExc) {
            throw new IllegalArgumentException("error handling object: " + monExc); // todo better way to handle?
        }
    }

    public void doComment(String comment) {
        // ignore
    }

    public void doLink(String o1Name, String o2Name) {
        try {
            hasSeenFirstItemDef = true;
            dbHandler.addLink(o1Name, o2Name);
        } catch (MonetException monExc) {
            throw new IllegalArgumentException("error handling object: " + monExc); // todo better way to handle?
        }
    }

    public void doObject(String name) {
        try {
            hasSeenFirstItemDef = true;
            dbHandler.addObject(name);
        } catch (MonetException monExc) {
            throw new IllegalArgumentException("error handling object: " + monExc); // todo better way to handle?
        }
    }

}
