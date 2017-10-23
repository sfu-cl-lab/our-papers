/**
 * $Id: AbstractRunFileImpl.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: AbstractRunFileImpl.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import spin.demo.Assert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.OutputStream;


/**
 * Abstract implementation that implements common facets of running a file, esp.
 * routing Log4J output back to the user.
 */
public abstract class AbstractRunFileImpl implements RunFileBean {

    protected Object inputObject;
    private PropertyChangeListener listener;
    protected RunFileOutStream runFileOutStream = new RunFileOutStream();
    private WriterAppender runFileWriterAppender;


    public AbstractRunFileImpl(Object inputObject) {
        this.inputObject = inputObject;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        Assert.offEDT();
        this.listener = listener;
    }

    protected void fireChange(String name, Object oldValue, Object newValue) {
        if (listener != null) {
            listener.propertyChange(new PropertyChangeEvent(this,
                    name, oldValue, newValue));
        }
    }

    protected void startLog4jRouting() {
        runFileWriterAppender = new WriterAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN),
                runFileOutStream);
        runFileWriterAppender.addFilter(new ThreadFilter(Thread.currentThread()));
        runFileWriterAppender.setThreshold(Level.INFO);  // recall: DEBUG < INFO < WARN < ERROR < FATAL
        Logger.getRootLogger().addAppender(runFileWriterAppender);
    }

    protected void stopLog4jRouting() {
        Logger.getRootLogger().removeAppender(runFileWriterAppender);
    }

    /**
     * Used to catch output from the Jython interperter, so that it can be
     * routed for the user to see. Nested inner class in order to access
     * fireChange().
     */
    class RunFileOutStream extends OutputStream {

        private StringBuffer sb = new StringBuffer();


        public RunFileOutStream() {
        }

        public void write(int b) {
            sb.append((char) b);
        }

        public void flush() {
            // todo req: input flag and setter that controls whether to scroll to bottom on flush() or not
            fireChange("output", null, sb.toString());
            sb.setLength(0);
        }

    }


    /**
     * A helper class used to filter log4j events according to which thread they
     * originate from.
     */
    static class ThreadFilter extends Filter {

        /**
         * The Thread whose events I am interested in. I.e., this thread's events are
         * accepted. All others are passed on to other filters.
         */
        private Thread thread;

        ThreadFilter(Thread thread) {
            if (thread == null) {
                throw new IllegalArgumentException("null thread");
            }
            this.thread = thread;
        }

        /**
         * Returns Filter.DENY if the current thread (which event originated
         * from) != my thread. Returns Filter.NEUTRAL o/w.
         */
        public int decide(LoggingEvent event) {
            Thread currThread = Thread.currentThread();
            if (thread != currThread) {
                return Filter.DENY;        // different thread - don't log
            } else {
                return Filter.NEUTRAL;    // pass logging decision along the filter chain
            }
        }

    }


}
