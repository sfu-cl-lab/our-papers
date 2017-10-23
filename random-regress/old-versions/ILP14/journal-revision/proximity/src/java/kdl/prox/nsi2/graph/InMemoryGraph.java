/**
 * $Id: InMemoryGraph.java 3658 2007-10-15 16:29:11Z schapira $
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
 * Very fast and very simple graph, which pre-fetches all the links and keeps them in memory
 * Of course it works when all the links fit in memory.
 */
public class InMemoryGraph implements Graph {
    private static Logger log = Logger.getLogger(InMemoryGraph.class);

    private Map<Integer, Set<Node>> links = new HashMap<Integer, Set<Node>>();

    public InMemoryGraph(boolean isDirected) {
        prefetch(isDirected);
    }

    public Set<Node> getNeighbors(Integer id) {
        Profiler.start("Memory.GetLinks");
        Set<Node> neighs = links.get(id);
        Profiler.end("Memory.GetLinks");
        return neighs;
    }

    public Set<Node> getNeighbors(Collection<Integer> ids) {
        ArrayList<Node> list = new ArrayList<Node>();
        for (Integer id : ids) {
            list.addAll(getNeighbors(id));
        }
        return new HashSet<Node>(list);
    }

    private void prefetch(boolean isDirected) {
        log.debug("prefetching links for " + DB.getObjectNST().getRowCount() + " nodes");

        Object[][] linkArray = readLinkArray(isDirected);

        int[] buffer = new int[100000]; // this should be able to hold the max expected degree!
        buffer[0] = (Integer) linkArray[0][1];
        Integer bufferId = (Integer) linkArray[0][0];
        int bufferCount = 1;
        int i;
        for (i = 1; i < linkArray.length; i++) {
            if (i % 100000 == 0) {
                log.debug("\t cached " + i + " links");
            }

            Integer o1 = (Integer) linkArray[i][0];

            if (!o1.equals(bufferId)) {
                int[] targets = new int[bufferCount];
                System.arraycopy(buffer, 0, targets, 0, bufferCount);
                Set<Node> neighbors = new HashSet<Node>();
                for (int target : targets) {
                    neighbors.add(new Node(target, 1.0));
                }
                links.put(bufferId, neighbors);

                bufferCount = 0;
                bufferId = o1;
            }

            buffer[bufferCount] = (Integer) linkArray[i][1];
            bufferCount++;
        }
        int[] targets = new int[bufferCount];
        System.arraycopy(buffer, 0, targets, 0, bufferCount);
        Set<Node> neighbors = new HashSet<Node>();
        for (int target : targets) {
            neighbors.add(new Node(target, 1.0));
        }
        links.put(bufferId, neighbors);
    }

    private Object[][] readLinkArray(boolean isDirected) {
        NST linkNST = DB.getLinkNST();
        NST sortedLinkNST;
        if (isDirected) {
            sortedLinkNST = linkNST.sort("o1_id", "o1_id,o2_id");
        } else {
            NST unsortedLinkNST = new NST("o1_id,o2_id", "oid,oid");
            unsortedLinkNST.insertRowsFromNST(linkNST.project("o1_id, o2_id"));
            unsortedLinkNST.insertRowsFromNST(linkNST.project("o2_id, o1_id"));
            sortedLinkNST = unsortedLinkNST.distinct().sort("o1_id", "o1_id,o2_id");
            unsortedLinkNST.release();
        }

        int row = 0;
        int rowCount = sortedLinkNST.getRowCount();
        int colCount = sortedLinkNST.getColumnCount();
        Object[][] linkArray = new Object[rowCount][colCount];
        ResultSet resultSet = sortedLinkNST.selectRows();
        while (resultSet.next()) {
            linkArray[row][0] = resultSet.getOID("o1_id");
            linkArray[row][1] = resultSet.getOID("o2_id");
            row++;
        }
        sortedLinkNST.release();
        linkNST.release();
        return linkArray;
    }
}
