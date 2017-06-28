/**
 * $Id: LoggingListener.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rdn.modules.listeners;

import kdl.prox.db.Container;
import kdl.prox.model2.rdn.RDN;
import kdl.prox.model2.rpt.RPT;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Simply logs every x cycles
 */
public class LoggingListener implements RDNListenerModule {

    private static Logger log = Logger.getLogger(RDN.class);

    public static int DEFAULT_LOG_FREQUENCY = 5;
    private int logFrequency;

    public LoggingListener() {
        this(DEFAULT_LOG_FREQUENCY);
    }

    public LoggingListener(int frequency) {
        this.logFrequency = frequency;
    }

    public void startup(Map<RPT, Container> modelsConts) {
    }

    public void cycle(int iteration) {
        if ((iteration % logFrequency) == 0) {
            log.info("RDN Iteration: " + iteration);
        }
    }

    public void cleanup() {
    }
}
