/**
 * $Id: TFMGroupConstraintsParent.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFMGroupConstraintsParent.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import kdl.prox.db.Attributes;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import kdl.prox.qgraph2.*;
import kdl.prox.qgraph2.util.ObjectCloner;
import kdl.prox.qgraph2.util.QGUtil;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Abstract superclass of TFM20 and TFM21, the transformations that deal with constraints
 * Defines some common methods, such as apply() and some helper routines
 */
abstract class TFMGroupConstraintsParent extends Transformation {

    protected static Logger log = Logger.getLogger(TFMGroupConstraintsParent.class);

    private AbstractQuery query;  // Saved by execTFMExec, to be used in TFM21's internal method


    /**
     * Transformation method. Throws Exception if problems.
     */
    public TFMExec applyTFMApp(TFMApp tfmApp) throws Exception {
        Assert.condition(tfmApp instanceof TFMAppWithConstraint,
                "tfmApp not a TFMAppWithConstraint");

        // deep copy tfmApp (qgItems and their Query)
        TFMAppWithConstraint tfmAppCopy = (TFMAppWithConstraint) (ObjectCloner.deepCopy(tfmApp));    // throws Exception
        List qgItemsCopy = tfmAppCopy.qgItems();
        // get my args
        QGVertex qgVertexCopy = (QGVertex) qgItemsCopy.get(0);
        QGConstraint qgConstraint = tfmAppCopy.getConstraint();
        // modify the copied Query by removing the constraint from the list
        AbstractQuery parentAQuery = qgVertexCopy.parentAQuery().rootQuery();
        parentAQuery.removeConstraint(qgConstraint);
        // return the copied exec item(s)
        return new TFMExec(qgVertexCopy);
    }

    /**
     * Compares objects in a subbgraph, according to a constraint. Either ids or attribute values
     * <p/>
     * Returns an NST with the rows that match the condition
     * <p/>
     *
     * @param qgConstraint
     * @param aItems
     * @param bItems
     * @param isVertices
     * @return
     */
    protected NST createCompareTablesAndApplyComparison(QGConstraint qgConstraint,
                                                        NST aItems, NST bItems,
                                                        boolean isVertices) {

        NST returnNST;

        String item1AttrName = qgConstraint.item1AttrName();
        String item2AttrName = qgConstraint.item2AttrName();
        String operator = qgConstraint.operator();

        NST aAndBItems = aItems.join(bItems, "A.subg_id = B.subg_id");
        aAndBItems.renameColumn("A.item_id", "a_item_id");
        aAndBItems.renameColumn("B.item_id", "b_item_id");
        aAndBItems.renameColumn("A.subg_id", "a_subg_id");
        aAndBItems.renameColumn("B.subg_id", "b_subg_id");
        aAndBItems.renameColumn("A.name", "a_name");
        aAndBItems.renameColumn("B.name", "b_name");

        if (item1AttrName == null) {
            // return an NST with a filter such that a_item_id OP b_item_id
            returnNST = aAndBItems.filter("a_item_id " + operator + " b_item_id");
        } else {
            // get the two dataNSTs for the attributes
            Attributes attrs = (isVertices ? DB.getObjectAttrs() : DB.getLinkAttrs());
            NST attr1DataNST = attrs.getAttrDataNST(item1AttrName);
            NST attr2DataNST = attrs.getAttrDataNST(item2AttrName);

            // now do a join between the items NSTs and the corresponding attr DataNSTs
            NST aAndBItemsWithAttr = aAndBItems.join(attr1DataNST, "a_item_id = id");
            NST aAndBItemsWithAllAttr = aAndBItemsWithAttr.join(attr2DataNST, "b_item_id = id");

            // return an NST with a filter such that A.value OP B.value
            returnNST = aAndBItemsWithAllAttr.filter("A.value " + operator + " B.value");
            returnNST.removeColumn("A.value");
            returnNST.removeColumn("B.value");
            returnNST.removeColumn("A.id");
            returnNST.removeColumn("B.id");

            attr1DataNST.release();
            attr2DataNST.release();
            aAndBItemsWithAttr.release();
            aAndBItemsWithAllAttr.release();
        }

        aAndBItems.release();
        return returnNST;

    }


    /**
     * Transformation method.
     */
    public String execTFMExec(TFMExec tfmExec, TFMApp tfmApp,
                              Query query, QGUtil qgUtil, TempTableMgr tempTableMgr) throws Exception {
        Assert.condition(tfmApp instanceof TFMAppWithConstraint,
                "tfmApp not a TFMAppWithConstraint");
        TFMAppWithConstraint mytfmApp = (TFMAppWithConstraint) tfmApp;

        // get my args
        QGConstraint qgConstraint = mytfmApp.getConstraint();
        QGVertex inQGVertex = (QGVertex) mytfmApp.qgItems().get(0);
        ConsQGVertex outConsQGVertex = (ConsQGVertex) tfmExec.qgItems().get(0);

        // get temp SGI NSTs for inQGVertex
        NST aObjTempSGINST = tempTableMgr.getNSTForVertex(inQGVertex.catenatedName(), true);
        NST aLinkTempSGINST = tempTableMgr.getNSTForVertex(inQGVertex.catenatedName(), false);
        Assert.notNull(aObjTempSGINST, "null obj SGI for " + inQGVertex.catenatedName());
        Assert.notNull(aLinkTempSGINST, "null link SGIfor " + inQGVertex.catenatedName());

        // decide if this is a constraint on vertices or edges
        // (can only be vertices for TFM 21 -- constraints on annotated items)
        // (can only be edges for TFM 22 -- constraints on annotated edges)
        QGItem qgItem = query.rootQuery().qgItemForName(qgConstraint.item1Name());
        boolean isVertices = (qgItem instanceof QGVertex);

        // Get the list of incident edges for annotated item, if TFM21, or name of annotated vertex if TFM22
        String annotItemName = qgConstraint.annotItemName();
        String[] incidentEdges = new String[]{};
        if (annotItemName != null) {
            if (isVertices) {
                QGVertex annotVertex = (QGVertex) query.rootQuery().qgItemForName(annotItemName);
                Set edges = annotVertex.edges();
                incidentEdges = new String[edges.size()];
                int i = 0;
                for (Iterator iterator = edges.iterator(); iterator.hasNext(); i++) {
                    QGEdge qgEdge = (QGEdge) iterator.next();
                    incidentEdges[i] = qgEdge.firstName();
                }
            } else {
                QGEdge annotEdge = (QGEdge) query.rootQuery().qgItemForName(annotItemName);
                QGVertex annotatedVertex = annotEdge.vertex1().isAnnotated() ?
                        annotEdge.vertex1() : annotEdge.vertex2();
                incidentEdges = new String[]{annotatedVertex.catenatedName()};
            }
        }

        // execute the actual code
        List objLinkTempSGINSTs = execTFMExecInternal(qgUtil,
                aObjTempSGINST, aLinkTempSGINST,
                qgConstraint, isVertices, incidentEdges);

        // save new temp SGI NSTs and remove old
        NST objTempSGINST = (NST) objLinkTempSGINSTs.get(0);
        NST linkTempSGINST = (NST) objLinkTempSGINSTs.get(1);

        // replace old vertex
        tempTableMgr.clearNSTForVertex(inQGVertex.catenatedName());
        tempTableMgr.clearNSTForVertex(inQGVertex.catenatedName());
        tempTableMgr.putNSTForVertex(outConsQGVertex.catenatedName(),
                objTempSGINST, linkTempSGINST);

        return outConsQGVertex.catenatedName();
    }


    /**
     * Internal execute method, for the tests
     *
     * @param qgUtil
     * @param objTempSGINST
     * @param linkTempSGINST
     * @param qgConstraint
     * @param isVertices             true if constraint applies to vertices, false o/w
     * @param edgesOnAnnotatedVertex
     * @return
     * @
     */
    abstract protected List execTFMExecInternal(QGUtil qgUtil, NST objTempSGINST,
                                                NST linkTempSGINST, QGConstraint qgConstraint,
                                                boolean isVertices, String[] edgesOnAnnotatedVertex);


}
