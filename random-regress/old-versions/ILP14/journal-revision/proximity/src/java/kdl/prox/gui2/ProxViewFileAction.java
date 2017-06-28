/**
 * $Id: ProxViewFileAction.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

public class ProxViewFileAction extends ProxAction {

    public ProxViewFileAction(String name, boolean isEnabled) {
        super(name, isEnabled);
    }

    public void performAction(BrowserJFrame browserJFrame) {
        browserJFrame.viewFile();
    }
}
