/**
 * $Id: TempTableMgr.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TempTableMgr.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2;

import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.util.SGIUtil;
import kdl.prox.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Helper class instantiated by QueryGraph2CompOp.execTGPath() and used by
 * Transformation instances to manage the mapping of ConsQGVertex names to
 * temporary SGI NSTs.
 */
public class TempTableMgr {

    /**
     * Maps ConsQGVertex names to temporary object and link SGI NSTs,
     * respectively.
     */
    private Map objVertexNSTMap = new HashMap();
    private Map linkVertexNSTMap = new HashMap();


    TempTableMgr() {
    }


    /**
     * Removes the entry for consQGVertexName from my objVertexNSTMap and
     * linkVertexNSTMap. Does nothing if it's not a key.
     *
     * @param consQGVertexName
     */
    public void clearNSTForVertex(String consQGVertexName) {
        NST objVertexNST = (NST) objVertexNSTMap.remove(consQGVertexName);
        NST linkVertexNST = (NST) linkVertexNSTMap.remove(consQGVertexName);
        // release the NSTs
        if (objVertexNST != null) {
            objVertexNST.release();
        }
        if (linkVertexNST != null) {
            linkVertexNST.release();
        }
    }

    /**
     * Given an edge in the TgPath, remove all instances from the temp table if
     * the edge is composite (e.g., Vertex1.Edge1.Vertex2). Leave it if it's a
     * single node (e.g., Vertex1)
     *
     * @param listOfConsQGVertexNames
     */
    public void clearNSTForAllVerticesIfMultiple(String listOfConsQGVertexNames) {
        String[] names = listOfConsQGVertexNames.split("\\.");
        if (names.length > 1) {
            for (int i = 0; i < names.length; i++) {
                clearNSTForVertex(names[i]);
            }
        }
    }

    /**
     * Returns the temp SGI NST corresponding to consQGVertexName and isObject.
     * Returns null if consQGVertexName is not in the appropriate Map. Callers
     * should use QGItem.catenatedName() to get consQGVertexName.
     */
    public NST getNSTForVertex(String consQGVertexName, boolean isObject) {
        Map itemVewrtexNSTMap = (isObject ? objVertexNSTMap : linkVertexNSTMap);
        NST nst = (NST) itemVewrtexNSTMap.get(consQGVertexName);
        return nst;
    }

    /**
     * Returns a Set of consQGVertexNames currently in me.
     */
    Set getVertexNames() {
        return objVertexNSTMap.keySet();
    }

    /**
     * Stores args in appropriate maps.
     *
     * @param consQGVertexName
     * @param objTempSGINST    null if no objects
     * @param linkTempSGINST   null if no links
     */
    public void putNSTForVertex(String consQGVertexName, NST objTempSGINST,
                                NST linkTempSGINST) {

        Assert.stringNotEmpty(consQGVertexName, "consQGVertexName null or empty");
        NST nst = (NST) objVertexNSTMap.get(consQGVertexName);
        Assert.condition(nst == null, "found existing entry for vertex: " +
                consQGVertexName);

// set objTempSGINST or linkTempSGINST if null
        if (objTempSGINST == null) {
            objTempSGINST = SGIUtil.createTempSGINST();
        }
        if (linkTempSGINST == null) {
            linkTempSGINST = SGIUtil.createTempSGINST();
        }

        objVertexNSTMap.put(consQGVertexName, objTempSGINST);
        linkVertexNSTMap.put(consQGVertexName, linkTempSGINST);
    }


}
