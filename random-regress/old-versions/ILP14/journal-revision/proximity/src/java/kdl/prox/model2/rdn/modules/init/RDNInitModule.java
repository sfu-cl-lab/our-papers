/**
 * $Id: RDNInitModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.init;

import kdl.prox.db.Container;
import kdl.prox.model2.rpt.RPT;

import java.util.Map;

/**
 * The init module is responsible for seeding the class labels for each RPT before the RDN begins
 * to cycle. Normaly the cycle() method randomizes the class labels, but other initializations
 * are possible (e.g., initialize from an NST, or from the results of applying an RPT)
 * <p/>
 * NOTE: The NST with the class labels are expected to be empty originally!
 */
public interface RDNInitModule {

    public void startup(Map<RPT, Container> modelsConts);

    public void seedLabels();

    public void cleanup();

}
