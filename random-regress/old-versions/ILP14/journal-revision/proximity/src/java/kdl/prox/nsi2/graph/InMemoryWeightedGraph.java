/**
 * $Id: InMemoryWeightedGraph.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.graph;

import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Like the inMemory graph, but instead of setting the weight of the neighboring node to 1, it
 * actually uses the 'weight' link attribute to get the weight of the connection to the neighbors
 */
public class InMemoryWeightedGraph implements Graph {
    private static Logger log = Logger.getLogger(InMemoryWeightedGraph.class);

    private Map<Integer, Set<Node>> links = new HashMap<Integer, Set<Node>>();

    public InMemoryWeightedGraph(boolean isDirected) {
        this(isDirected, null);
    }

    public InMemoryWeightedGraph(boolean isDirected, NST linkNST) {
        prefetch(isDirected, linkNST);
    }


    public Set<Node> getNeighbors(Integer id) {
        return links.get(id);
    }

    public Set<Node> getNeighbors(Collection<Integer> ids) {
        ArrayList<Node> list = new ArrayList<Node>();
        for (Integer id : ids) {
            list.addAll(getNeighbors(id));
        }
        return new HashSet<Node>(list);
    }

    private void prefetch(boolean isDirected, NST linkNST) {
        log.debug("prefetching links for " + DB.getObjectNST().getRowCount() + " nodes");

        Object[][] linkArray = readWeightedLinkArray(isDirected, linkNST);

        List<Node> buffer = new ArrayList<Node>();
        buffer.add(new Node((Integer) linkArray[0][1], (Double) linkArray[0][2]));
        Integer bufferId = (Integer) linkArray[0][0];
        int i;
        for (i = 1; i < linkArray.length; i++) {
            if (i % 100000 == 0) {
                log.debug("\t cached " + i + " links");
            }

            Integer o1 = (Integer) linkArray[i][0];

            if (!o1.equals(bufferId)) {
                links.put(bufferId, new HashSet<Node>(buffer));
                buffer.clear();
                bufferId = o1;
            }

            buffer.add(new Node((Integer) linkArray[i][1], (Double) linkArray[i][2]));
        }
        links.put(bufferId, new HashSet<Node>(buffer));
    }

    private Object[][] readWeightedLinkArray(boolean isDirected, NST links) {
        NST linkNST;
        if (links == null) {
            linkNST = DB.getLinks("*", "weight");
        }
        else {
            NST weightNST = DB.getLinkAttrs().getAttrDataNST("weight");
            linkNST = links.join(weightNST, "link_id = id", "link_id, o1_id, o2_id, value");
            linkNST.renameColumns("link_id, o1_id, o2_id, weight");
        }
        NST sortedLinkNST;
        if (isDirected) {
            sortedLinkNST = linkNST.sort("o1_id", "o1_id,o2_id, weight");
        } else {
            NST unsortedLinkNST = new NST("o1_id,o2_id, weight", "oid,oid, dbl");

            NST direction1NST = linkNST.project("o1_id, o2_id, weight");
            unsortedLinkNST.insertRowsFromNST(direction1NST);
            direction1NST.release();

            NST direction2NST = linkNST.project("o2_id, o1_id, weight");
            unsortedLinkNST.insertRowsFromNST(direction2NST);
            direction2NST.release();

            NST unsortedLinkNSTDistinct = unsortedLinkNST.distinct();
            sortedLinkNST = unsortedLinkNSTDistinct.sort("o1_id", "o1_id,o2_id, weight");
            unsortedLinkNSTDistinct.release();
            unsortedLinkNST.release();
        }

        int row = 0;
        int rowCount = sortedLinkNST.getRowCount();
        int colCount = sortedLinkNST.getColumnCount();
        Object[][] linkArray = new Object[rowCount][colCount];
        ResultSet resultSet = sortedLinkNST.selectRows();
        while (resultSet.next()) {
            if (row % 10000 == 0) {
                log.debug("\t" + row);
            }
            linkArray[row][0] = resultSet.getOID("o1_id");
            linkArray[row][1] = resultSet.getOID("o2_id");
            linkArray[row][2] = resultSet.getDouble("weight");
            row++;
        }
        sortedLinkNST.release();
        linkNST.release();
        return linkArray;
    }

}

