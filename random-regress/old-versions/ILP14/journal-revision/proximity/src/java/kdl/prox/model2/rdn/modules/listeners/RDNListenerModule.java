/**
 * $Id: RDNListenerModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.listeners;

import kdl.prox.db.Container;
import kdl.prox.model2.rpt.RPT;

import java.util.Map;

/**
 * A listener module is called
 * before the cycle (startup)
 * during the cycle (cycle)
 * after the cycle (cleanup)
 */
public interface RDNListenerModule {

    public void startup(Map<RPT, Container> modelsConts);

    public void cycle(int iteration);

    public void cleanup();

}
