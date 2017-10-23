/**
 * $Id: RunFileBean.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RunFileBean.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import java.beans.PropertyChangeListener;


/**
 * Interface for 'running' an input object for now either a QGraph query or a
 * Jython script. Uses property changes to pass file output (during run) and
 * status (when done) back to the caller when done.
 */
public interface RunFileBean {

    public void addPropertyChangeListener(PropertyChangeListener listener);

    public String getInputObjectString();

    /**
     * Runs the passed input object (a File or an Element, currently), passing
     * output back to the listener via these property names: 'status', 'output'.
     *
     * @throws Exception
     */
    public void start() throws Exception;

}
