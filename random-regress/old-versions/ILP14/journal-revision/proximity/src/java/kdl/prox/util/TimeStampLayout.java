/**
 * $Id: TimeStampLayout.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.text.DecimalFormat;

/**
 * Replacement for Log4J's standard pattern layout utility.
 * It replaces the standard %-9r format element with a nicer display of
 * hhh:mm:ss.mss
 */
public class TimeStampLayout extends PatternLayout {

    public void setConversionPattern(String layout) {
        if (layout.trim().startsWith("%-9r")) {
            layout = layout.trim().substring(5);
        }
        super.setConversionPattern(layout);
    }

    public java.lang.String format(org.apache.log4j.spi.LoggingEvent loggingEvent) {
        double ts = (loggingEvent.timeStamp - LoggingEvent.getStartTime()) / 1000.0;
        int hours = ((int) (ts / 3600));
        int mins = ((int) ((ts % 3600) / 60));
        double secs = ((ts % 3600) % 60);

        String tsString;
        if (ts < 90) {
            tsString = new DecimalFormat("#0.000s").format(ts);
        } else {
            if (hours > 0) {
                tsString = new DecimalFormat("#0").format(hours) + ":" +
                        new DecimalFormat("00").format(mins) + ":";
            } else {
                tsString = new DecimalFormat("#0").format(mins) + ":";
            }
            tsString += new DecimalFormat("00").format(secs);
        }
        return "[" + tsString + "] " + super.format(loggingEvent);
    }
}
