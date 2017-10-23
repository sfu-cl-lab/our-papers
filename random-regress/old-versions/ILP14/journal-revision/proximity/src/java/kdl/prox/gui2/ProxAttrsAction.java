/**
 * $Id: ProxAttrsAction.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

public class ProxAttrsAction extends ProxAction {

    public ProxAttrsAction(String name, boolean isEnabled) {
        super(name, isEnabled);
    }

    public void performAction(BrowserJFrame browserJFrame) {
        browserJFrame.goTo(browserJFrame.getCurrentProxURL() + "!" + GUIContentGenerator.ATTR_VAL_PARAM);
    }
}
