/**
 * $Id: NSTCache.java 3732 2007-11-07 20:56:23Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.util;

import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a simple map from keys to NSTs, used to store NSTs.
 * Aggregators use it to share tables they've already pre-computed
 */
public class NSTCache {

    static Logger log = Logger.getLogger(NSTCache.class);

    private Map<String, NST> map;

    public NSTCache() {
        map = new HashMap<String, NST>();
    }

    public void clear() {
        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
            String tableName = (String) iterator.next();
            getTable(tableName).release();
        }
        map.clear();
    }

    public NST getOrCreateTable(String tableName, NSTCreator creator) {
        NST table = getTable(tableName);
        if (table == null || table.isReleased()) {
            log.debug("Creating " + tableName);
            table = creator.create();
            saveTable(tableName, table);
        } else {
            log.debug("Reusing " + tableName);
        }
        return table;
    }

    public NST getTable(String tableName) {
        return map.get(tableName);
    }

    public Set<String> listTables() {
        return map.keySet();
    }

    public NST removeTable(String tableName) {
        return map.remove(tableName);
    }

    public NST saveTable(String tableName, NST table) {
        return map.put(tableName, table);
    }


    public void invalidateTablesWithString(String attrName) {
        List<String> tableNamesToRemove = new ArrayList<String>();

        for (String tableName : map.keySet()) {
            if (tableName.contains(attrName)) {
                tableNamesToRemove.add(tableName);
            }
        }

        for (String tableName : tableNamesToRemove) {
            log.debug("Invalidating " + tableName);
            removeTable(tableName);
        }
    }
}
