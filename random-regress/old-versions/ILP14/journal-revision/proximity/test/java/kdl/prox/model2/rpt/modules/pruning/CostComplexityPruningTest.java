/**
 * $Id: CostComplexityPruningTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: CostComplexityPruningTest.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rpt.modules.pruning;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: afast
 * Date: Mar 29, 2007
 * Time: 12:29:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class CostComplexityPruningTest extends TestCase {

    private static final Logger log = Logger.getLogger(CostComplexityPruningTest.class);


    protected void setUp() throws Exception {
        super.setUp();

        TestUtil.initDBOncePerAllTests();
        TestUtil.openTestConnection();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.closeTestConnection();
    }

    public void testFindAndRemoveWeakestLink() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        RPT rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();

        CostComplexityPruning ccp = new CostComplexityPruning(rpt, 5);
        PruningRPTNode prunedTree = ccp.findAndRemoveWeakestLink(new PruningRPTNode(rootNode));
        //Should prune no-branch of a copy of the tree, leaving rootNode untouched
        assertNotSame(prunedTree, rootNode);
        assertEquals(4, rootNode.getLeafCount());
        assertEquals(3, prunedTree.getLeafCount());

        assertEquals(true, prunedTree.getNoBranch().isLeaf());
    }

    public void testGetPruningSequence() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        RPT rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();

        CostComplexityPruning ccp = new CostComplexityPruning(rpt, 5);
        List<PruningRPTNode> pruningSequence = ccp.getPruningSequence(rootNode);

        //assertSame(pruningSequence.get(0).getLeafCount(), rootNode.getLeafCount()); //Always stores the learned tree in index 0
        assertEquals(3, pruningSequence.size());
        assertEquals(3, pruningSequence.get(1).getLeafCount());
        assertEquals(true, (pruningSequence.get(2)).isLeaf()); //Final tree is a single node.

        //Assert the alpha increases throughout the sequence
        for (int i = 0; i < pruningSequence.size() - 1; i++) {
            PruningRPTNode firstNode = pruningSequence.get(i);
            PruningRPTNode secondNode = pruningSequence.get(i + 1);

            log.info("First Node: " + firstNode.getAlpha());
            log.info("Second Node: " + secondNode.getAlpha());

            assertTrue(secondNode.getAlpha() > firstNode.getAlpha());
        }
    }

    public void testGetT1() {
        File inFile = new File(getClass().getResource("2dtree-test.xml").getFile());
        RPT rpt = new RPT().load(inFile.getPath());
        RPTNode rootNode = rpt.getRootNode();

        CostComplexityPruning ccp = new CostComplexityPruning(rpt, 5);
        PruningRPTNode t1 = ccp.getT1(rootNode, rootNode.getInstanceCount());

        assertNotSame(t1, rootNode);
        assertEquals(t1.getInstanceCount(), rootNode.getInstanceCount());
        assertEquals(rootNode.getTreeResubError(rootNode.getInstanceCount()), t1.getTreeResubError(t1.getInstanceCount()), 0.00001);
        assertEquals(4, t1.getLeafCount());
    }
}