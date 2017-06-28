/**
 * $Id: Distribution.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.util.stat;


/**
 * Abstract superclass for random number generators.
 */
public abstract class Distribution {

    /**
     * @return a random number according to the distribution implemented in the
     *         concrete subclass
     */
    public abstract double getRandomNumber();

}
