/**
 * $Id: RDNDBModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.db;

import kdl.prox.db.Container;
import kdl.prox.model2.rpt.RPT;

import java.util.Map;

/**
 * A DB module hides all the complexity of managing attributes for the RDN, updating their values
 * after each iteration, etc
 * <p/>
 * The update method must save in the database the values of the new predicted classes for the
 * CORE ITEM in each subgraph listed in subgClasses
 */
public interface RDNDBModule {

    public void startup(Map<RPT, Container> modelsConts);

    public void update(RPT rpt, Map<String, String> subgClasses, int iteration);

    public void cleanup();

}
