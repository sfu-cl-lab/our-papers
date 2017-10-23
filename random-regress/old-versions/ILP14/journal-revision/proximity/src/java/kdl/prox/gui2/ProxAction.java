/**
 * $Id: ProxAction.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.gui2;

/**
 * Used by GUIContentGenerator.getActionsForURL() to add URL(page)-specific
 * actions to BrowserJFrame content. Instantiate directly or create a subclass.
 */
public class ProxAction {

    private boolean isEnabled;
    private String name;

    public ProxAction(String name) {
        this(name, true);
    }

    public ProxAction(String name, boolean isEnabled) {
        this.name = name;
        this.isEnabled = isEnabled;
    }

    public boolean getIsEnabled() {
        return isEnabled;
    }

    public String getName() {
        return name;
    }

    public String getToolTipText() {
        return name;  // todo xx
    }

    /**
     * Concrete actions must implement this method.
     *
     * @param browserJFrame
     */
    public void performAction(BrowserJFrame browserJFrame) {
    };

}
