/**
 * $Id: CachedOnDiskGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.profiling.Profiler;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * intermediate solution, keeps an in-memory cache of some of the links.
 * This solution is approx. 100x slower than InMemory!.
 *
 * Parameters:
 *   - size: number of nodes to keep in memory
 *
 * Constants:
 *   - DEFAULT_INIT_RATIO  : pct of nodes to pre-fetch upon creation
 *   - DEFAULT_DELETE_RATIO: pct of cache to clear whenever it gets full
 *
 *
 */
public class CachedOnDiskGraph implements Graph {
    private static final Logger log = Logger.getLogger(CachedOnDiskGraph.class);
    private NST linkNST;

    private final HashMap<Integer, Set<Node>> cache = new HashMap<Integer, Set<Node>>();

    private static final int DEFAULT_INIT_RATIO = 2;      //  1/2 of cache is pre-fetched
    private static final int DEFAULT_DELETE_RATIO = 20;   //  1/20th of the cache is cleared  whenever the cache gets full

    private final int cacheSize;
    private final String sectionAll;
    private final String sectionMiss;

    public CachedOnDiskGraph(boolean isDirectional, int size) {
        // prepare link table
        NST links = DB.getLinkNST().project("o1_id AS from, o2_id AS to");
        if (!isDirectional) {
            NST links2 = DB.getLinkNST().project("o2_id, o1_id");
            links.insertRowsFromNST(links2);
            links2.release();
        }
        links.castColumn("from", "int");
        links.castColumn("to", "int");
        linkNST = links.sort("from", "from, to");
        links.release();

        // initialize cache
        cacheSize = size;

        // labels for profiler
        sectionAll = "Disk_" + cacheSize + ".getLinks";
        sectionMiss = "Disk_" + cacheSize + ".miss";

        prefetchCache();
    }


    public Set<Node> getNeighbors(Integer id) {
        Profiler.start(sectionAll);

        Set<Node> retVal = cache.get(id);
        if (retVal == null) {
            Profiler.start(sectionMiss);
            retVal = retrieveID(id);
            Profiler.end(sectionMiss);
        }
        Profiler.end(sectionAll);

        return retVal;
    }

    public Set<Node> getNeighbors(Collection<Integer> ids) {
        Set<Node> neighbors = new HashSet<Node>();

        for (Integer id : ids) {
            neighbors.addAll(getNeighbors(id));
        }

        return neighbors;
    }


    /**
     * Fills up the cache with the top 1/DEFAULT_INIT_RATIO of the rows, ordered by their degree
     * If all nodes fill in the cache, it gets them all quickly
     */
    private void prefetchCache() {
        ResultSet resultSet;

        linkNST.addCountColumn("from", "degree");
        NST nodeNST = linkNST.distinct("from");
        if (cacheSize >= nodeNST.getRowCount()) {
            NST allRows = linkNST.aggregate("batstrconcat", "from", "to");
            resultSet = allRows.selectRows("from, to");
            allRows.release();
        } else {
            int cacheInitSize = Math.round(cacheSize / DEFAULT_INIT_RATIO);
            NST topFrom = nodeNST.rangeSorted("degree DESC", "0-" + cacheInitSize);
            NST topAll = linkNST.filter("from IN " + topFrom.getNSTColumn("from")).aggregate("batstrconcat", "from", "to");
            resultSet = topAll.selectRows("from, to");
            topFrom.release();
            topAll.release();
        }
        linkNST.removeColumn("degree");

        while (resultSet.next()) {
            int fromId = resultSet.getInt(1);
            String toList = resultSet.getString(2);
            ArrayList<Node> neighbors = new ArrayList<Node>();
            String[] strings = toList.split(",");
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                neighbors.add(new Node(Integer.parseInt(string.trim()), 1.0));
            }
            cache.put(fromId, new HashSet<Node>(neighbors));
        }
    }

    /**
     * Called when there is a miss. Goes to the db and gets the neighbors for the id,
     * and saves it in the cache.
     * If the cache is already full, cleans it a bit (removes 1 / DEFAULT_DELETE_RATIO of the entries)
     *
     * @param id
     * @return
     */
    private Set<Node> retrieveID(Integer id) {
        // read it from the db
        ResultSet resultSet = linkNST.selectRows("from = " + id, "to");
        ArrayList<Node> neighbors = new ArrayList<Node>();
        while (resultSet.next()) {
            neighbors.add(new Node(resultSet.getInt(1), 1.0));
        }
        HashSet<Node> set = new HashSet<Node>(neighbors);

        // clean cache if necessary
        if (cache.size() >= cacheSize) {
            int cacheDeleteSize = Math.round(cacheSize / DEFAULT_DELETE_RATIO);
            Random r = new Random();
            List<Integer> toDelete = new ArrayList<Integer>();
            for (Integer key : cache.keySet()) {
                int i = r.nextInt(cacheSize);
                if (i < cacheDeleteSize) {
                    toDelete.add(key);
                    if (toDelete.size() == cacheDeleteSize) {
                        break;
                    }
                }
            }
            for (Integer i : toDelete) {
                cache.remove(i);
            }
        }

        // and put it
        if (cache.size() < cacheSize) {
            cache.put(id, set);
        }
        return set;
    }

}
