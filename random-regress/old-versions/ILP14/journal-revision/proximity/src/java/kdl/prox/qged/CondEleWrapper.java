/**
 * $Id: CondEleWrapper.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import org.jdom.Element;


/**
 * A simple wrapper around a QGItem's condEleChild Element. Used because generic
 * Elements don't have a toString() that's useful to us, which complicates both
 * default rendering and editing, both of which use toString() quite tightly. An
 * alternative is to define a custom table cell renderer that uses a
 * ConditionFormation instance, and to define a customer editor (which ended up
 * being too hard.) This approach (defining an Element wrapper and a toString()
 * for it) seemed simpler.
 */
public class CondEleWrapper {

    private static final ConditionFormat CONDITION_FORMAT = new ConditionFormat();

    private Element condEleChild;


    public CondEleWrapper(Element condEleChild) {
        this.condEleChild = condEleChild;
    }

    public Element getCondEleChild() {
        return condEleChild;
    }

    public String toString() {
        return CONDITION_FORMAT.format(this);
    }

}
