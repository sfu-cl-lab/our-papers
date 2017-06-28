/**
 * $Id: Profiler.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util.profiling;

import kdl.prox.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * A very basic class for profiling.
 * Use start() and end() with a given label to record calls to a block of code.
 * clear() and clearAll() remove stats about a particular label
 * getStats() returns the information gathered about a particular label
 * printStats() prints a report for all labels
 */
public class Profiler {

    private static HashMap<String, ProfileData> sections = new HashMap<String, ProfileData>();

    private Profiler() {
    }

    public static void start(String section) {
        ProfileData profileData = sections.get(section);
        if (profileData == null) {
            profileData = new ProfileData();
            profileData.startCall();
            sections.put(section, profileData);
        } else {
            profileData.startCall();
        }
    }

    public static void end(String section) {
        ProfileData profileData = sections.get(section);
        Assert.notNull(profileData, section + " does not exist. Call start(section) before calling end");
        profileData.endCall();
    }

    public static void clear(String section) {
        ArrayList<String> removeList = new ArrayList<String>();
        for (String s : sections.keySet()) {
            if (s.startsWith(section)) {
                removeList.add(s);
            }
        }
        for (String s : removeList) {
            sections.remove(s);
        }
    }

    public static void clearAll() {
        sections = new HashMap<String, ProfileData>();
    }

    public static ProfileData getStats(String section) {
        ProfileData profileData = sections.get(section);
        Assert.notNull(profileData, section + " does not exist");
        return profileData;
    }

    public static void printStats() {
        System.out.format("%30s \t %10s \t %15s \t %15s \t %15s \n",
                "Section", "Calls", "Time (ms)", "Avg time (ms)", "Max per ms");
        System.out.format("%30s \t %10s \t %15s \t %15s \t %15s \n",
                "-------", "-----", "---------", "-------------", "----------");
        ArrayList<String> keys = new ArrayList<String>(sections.keySet());
        Collections.sort(keys);
        for (String s : keys) {
            ProfileData profileData = sections.get(s);
            System.out.format("%30s \t %10d \t %15d \t %15.3f \t %,15f \n",
                    s,
                    profileData.getTimesCalled(),
                    profileData.getTotalElapsedTime(),
                    profileData.getAvgElapsedTime(),
                    profileData.getMaxPerMS());
        }
    }
}
