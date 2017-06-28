/**
 * $Id: ProfileData.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util.profiling;

import kdl.prox.util.Assert;

public class ProfileData {

    int timesCalled;
    long elapsedTime;
    long lastStart;

    public ProfileData() {
        timesCalled = 0;
        elapsedTime = 0;
        lastStart = -1;
    }

    public ProfileData startCall() {
        lastStart = System.nanoTime();
        return this;
    }

    public ProfileData endCall() {
        Assert.condition(lastStart != -1, "Call startCall before calling endCall");
        timesCalled++;
        elapsedTime += (System.nanoTime() - lastStart);
        lastStart = -1;
        return this;
    }

    public int getTimesCalled() {
        return timesCalled;
    }

    public long getTotalElapsedTime() {
        return elapsedTime / 1000000;
    }

    public double getAvgElapsedTime() {
        return (getTotalElapsedTime() / (timesCalled * 1.0));
    }

    public double getMaxPerMS() {
        return (1000.0 / getAvgElapsedTime());
    }

}
