/**
 * $Id: TableModelTest.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import junit.framework.TestCase;
import kdl.prox.TestUtil;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGAddLink;
import kdl.prox.qgraph2.QGConstraint;
import kdl.prox.qgraph2.QGEdge;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.QGVertex;
import kdl.prox.qgraph2.Query;
import kdl.prox.qgraph2.Subquery;


public class TableModelTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        // call initDBOncePerAllTests() (even though don't need db to test) so
        // that log4j init happens only once
        TestUtil.initDBOncePerAllTests();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Recall Query table content:
     * +--------------+-------+
     * | Type         | Query |
     * +--------------+-------+
     * | Name         | ...   |
     * +--------------+-------+
     * | Description  | ...   |
     * +--------------+-------+
     * | Constraint 1 | ...   |
     * +--------------+-------+
     * | Constraint 2 | ...   |
     * +--------------+-------+
     */
    public void testTableModelForQuery1() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(),
                "hot100-neighborhood.qg2.xml"); // has constraints
        Object[] consts = query.constraints().toArray();
        QGConstraint const1 = (QGConstraint) consts[0];
        QGConstraint const2 = (QGConstraint) consts[1];
        QGObjTableModel tableModel = QueryCanvas.getTableModel(query);

        assertEquals(2, tableModel.getColumnCount());
        assertEquals(5, tableModel.getRowCount());

        assertEquals("Property", tableModel.getColumnName(0));
        assertEquals("Value", tableModel.getColumnName(1));

        assertEquals(false, tableModel.isCellEditable(0, 0));
        assertEquals(false, tableModel.isCellEditable(0, 1));
        assertEquals(false, tableModel.isCellEditable(1, 0));
        assertEquals(true, tableModel.isCellEditable(1, 1));
        assertEquals(false, tableModel.isCellEditable(2, 0));
        assertEquals(true, tableModel.isCellEditable(2, 1));
        assertEquals(false, tableModel.isCellEditable(3, 0));
        assertEquals(true, tableModel.isCellEditable(3, 1));
        assertEquals(false, tableModel.isCellEditable(4, 0));
        assertEquals(true, tableModel.isCellEditable(4, 1));

        assertEquals(String.class, tableModel.getClassAt(0, 0));
        assertEquals(String.class, tableModel.getClassAt(0, 1));
        assertEquals(String.class, tableModel.getClassAt(1, 0));
        assertEquals(String.class, tableModel.getClassAt(1, 1));
        assertEquals(String.class, tableModel.getClassAt(2, 0));
        assertEquals(String.class, tableModel.getClassAt(2, 1));
        assertEquals(String.class, tableModel.getClassAt(3, 0));
        assertEquals(QGConstraint.class, tableModel.getClassAt(3, 1));
        assertEquals(String.class, tableModel.getClassAt(4, 0));
        assertEquals(QGConstraint.class, tableModel.getClassAt(4, 1));

        assertEquals("Type", tableModel.getValueAt(0, 0));
        assertEquals("Query", tableModel.getValueAt(0, 1));
        assertEquals("Name", tableModel.getValueAt(1, 0));
        assertEquals("New Query", tableModel.getValueAt(1, 1));
        assertEquals("Description", tableModel.getValueAt(2, 0));
        assertEquals("New Editor Query", tableModel.getValueAt(2, 1));
        assertEquals("Constraint 1", tableModel.getValueAt(3, 0));
        assertEquals(const1, tableModel.getValueAt(3, 1));
        assertEquals("Constraint 2", tableModel.getValueAt(4, 0));
        assertEquals(const2, tableModel.getValueAt(4, 1));
    }

    /**
     * Recall Query table content:
     * +--------------+-------+
     * | Type         | Query |
     * +--------------+-------+
     * | Name         | ...   |
     * +--------------+-------+
     * | Description  | ...   |
     * +--------------+-------+
     */
    public void testTableModelForQuery2() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(), "test-query.xml");  // no constraints
        QGObjTableModel tableModel = QueryCanvas.getTableModel(query);

        assertEquals(2, tableModel.getColumnCount());
        assertEquals(3, tableModel.getRowCount());

        assertEquals("Property", tableModel.getColumnName(0));
        assertEquals("Value", tableModel.getColumnName(1));

        assertEquals(false, tableModel.isCellEditable(0, 0));
        assertEquals(false, tableModel.isCellEditable(0, 1));
        assertEquals(false, tableModel.isCellEditable(1, 0));
        assertEquals(true, tableModel.isCellEditable(1, 1));
        assertEquals(false, tableModel.isCellEditable(2, 0));
        assertEquals(true, tableModel.isCellEditable(2, 1));

        assertEquals(String.class, tableModel.getClassAt(0, 0));
        assertEquals(String.class, tableModel.getClassAt(0, 1));
        assertEquals(String.class, tableModel.getClassAt(1, 0));
        assertEquals(String.class, tableModel.getClassAt(1, 1));
        assertEquals(String.class, tableModel.getClassAt(2, 0));
        assertEquals(String.class, tableModel.getClassAt(2, 1));

        assertEquals("Type", tableModel.getValueAt(0, 0));
        assertEquals("Query", tableModel.getValueAt(0, 1));
        assertEquals("Name", tableModel.getValueAt(1, 0));
        assertEquals("Test Query", tableModel.getValueAt(1, 1));
        assertEquals("Description", tableModel.getValueAt(2, 0));
        assertEquals("a test query", tableModel.getValueAt(2, 1));
    }

    /**
     * Recall Query table content:
     * +--------------+-------+
     * | Type         | Query |
     * +--------------+-------+
     * | Name         | ...   |
     * +--------------+-------+
     * | Description  | ...   |
     * +--------------+-------+
     * | Constraint 1 | ...   |
     * +--------------+-------+
     * | Constraint 2 | ...   |
     * +--------------+-------+
     * | Add edge 1   | ...   |
     * +--------------+-------+
     * | Add edge 2   | ...   |
     * +--------------+-------+
     */
    public void testTableModelForQuery3() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(),
                "hot100-neighborhood-add-link.qg2.xml"); // has constraints
        Object[] consts = query.constraints().toArray();
        QGConstraint const1 = (QGConstraint) consts[0];
        QGConstraint const2 = (QGConstraint) consts[1];
        Object[] addLinks = query.addLinks().toArray();
        QGAddLink addLink1 = (QGAddLink) addLinks[0];
        QGAddLink addLink2 = (QGAddLink) addLinks[1];
        QGObjTableModel tableModel = QueryCanvas.getTableModel(query);

        assertEquals(2, tableModel.getColumnCount());
        assertEquals(7, tableModel.getRowCount());

        assertEquals("Property", tableModel.getColumnName(0));
        assertEquals("Value", tableModel.getColumnName(1));

        assertEquals(false, tableModel.isCellEditable(0, 0));
        assertEquals(false, tableModel.isCellEditable(0, 1));
        assertEquals(false, tableModel.isCellEditable(1, 0));
        assertEquals(true, tableModel.isCellEditable(1, 1));
        assertEquals(false, tableModel.isCellEditable(2, 0));
        assertEquals(true, tableModel.isCellEditable(2, 1));
        assertEquals(false, tableModel.isCellEditable(3, 0));
        assertEquals(true, tableModel.isCellEditable(3, 1));
        assertEquals(false, tableModel.isCellEditable(4, 0));
        assertEquals(true, tableModel.isCellEditable(4, 1));
        assertEquals(false, tableModel.isCellEditable(5, 0));
        assertEquals(true, tableModel.isCellEditable(5, 1));
        assertEquals(false, tableModel.isCellEditable(6, 0));
        assertEquals(true, tableModel.isCellEditable(6, 1));

        assertEquals(String.class, tableModel.getClassAt(0, 0));
        assertEquals(String.class, tableModel.getClassAt(0, 1));
        assertEquals(String.class, tableModel.getClassAt(1, 0));
        assertEquals(String.class, tableModel.getClassAt(1, 1));
        assertEquals(String.class, tableModel.getClassAt(2, 0));
        assertEquals(String.class, tableModel.getClassAt(2, 1));
        assertEquals(String.class, tableModel.getClassAt(3, 0));
        assertEquals(QGConstraint.class, tableModel.getClassAt(3, 1));
        assertEquals(String.class, tableModel.getClassAt(4, 0));
        assertEquals(QGConstraint.class, tableModel.getClassAt(4, 1));
        assertEquals(String.class, tableModel.getClassAt(5, 0));
        assertEquals(QGAddLink.class, tableModel.getClassAt(5, 1));
        assertEquals(String.class, tableModel.getClassAt(6, 0));
        assertEquals(QGAddLink.class, tableModel.getClassAt(6, 1));

        assertEquals("Type", tableModel.getValueAt(0, 0));
        assertEquals("Query", tableModel.getValueAt(0, 1));
        assertEquals("Name", tableModel.getValueAt(1, 0));
        assertEquals("New Query", tableModel.getValueAt(1, 1));
        assertEquals("Description", tableModel.getValueAt(2, 0));
        assertEquals("New Editor Query", tableModel.getValueAt(2, 1));
        assertEquals("Constraint 1", tableModel.getValueAt(3, 0));
        assertEquals(const1, tableModel.getValueAt(3, 1));
        assertEquals("Constraint 2", tableModel.getValueAt(4, 0));
        assertEquals(const2, tableModel.getValueAt(4, 1));
        assertEquals("Add-link 1", tableModel.getValueAt(5, 0));
        assertEquals(addLink1, tableModel.getValueAt(5, 1));
        assertEquals("Add-link 2", tableModel.getValueAt(6, 0));
        assertEquals(addLink2, tableModel.getValueAt(6, 1));
    }

    /**
     * Recall Subqery table content:
     * +--------------+---------+
     * | Type         | Subqery |
     * +--------------+---------+
     * | Annotation   | ...     |
     * +--------------+---------+
     */
    public void testTableModelForSubquery() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(),
                "hot100-neighborhood.qg2.xml");
        QGVertex qgVertex = (QGVertex) query.qgItemForName("Co-queriedUser");
        Subquery subquery = (Subquery) qgVertex.parentAQuery();
        QGObjTableModel tableModel = QueryCanvas.getTableModel(subquery);

        assertEquals(2, tableModel.getColumnCount());
        assertEquals(2, tableModel.getRowCount());

        assertEquals("Property", tableModel.getColumnName(0));
        assertEquals("Value", tableModel.getColumnName(1));

        assertEquals(false, tableModel.isCellEditable(0, 0));
        assertEquals(false, tableModel.isCellEditable(0, 1));
        assertEquals(false, tableModel.isCellEditable(1, 0));
        assertEquals(true, tableModel.isCellEditable(1, 1));

        assertEquals(String.class, tableModel.getClassAt(0, 0));
        assertEquals(String.class, tableModel.getClassAt(0, 1));
        assertEquals(String.class, tableModel.getClassAt(1, 0));
        assertEquals(Annotation.class, tableModel.getClassAt(1, 1));

        assertEquals("Type", tableModel.getValueAt(0, 0));
        assertEquals("Subquery", tableModel.getValueAt(0, 1));
        assertEquals("Annotation", tableModel.getValueAt(1, 0));
        assertEquals(subquery.annotation(), tableModel.getValueAt(1, 1));
    }

    public void testTableModelForQGItem() throws Exception {
        Query query = QueryCanvasTest.loadQueryFromFile(getClass(),
                "hot100-neighborhood.qg2.xml");
        QGVertex qgVertex = (QGVertex) query.qgItemForName("CoreFile");
        QGEdge qgEdge1 = (QGEdge) query.qgItemForName("co-ownsFile");   // isDirected
        QGEdge qgEdge2 = (QGEdge) query.qgItemForName("afterQuery");    // !isDirected
        verifyTableModelForQGItem(qgVertex);
        verifyTableModelForQGItem(qgEdge1);
        verifyTableModelForQGItem(qgEdge2);

        query = QueryCanvasTest.loadQueryFromFile(getClass(), "test-query.xml");
        qgVertex = (QGVertex) query.qgItemForName("vertex1");
        verifyTableModelForQGItem(qgVertex);
    }

    /**
     * Recall QGItem table content:
     * <p/>
     * +--------------+-------+
     * | Type         | Edge  |  or 'Vertex'
     * +--------------+-------+
     * | Name         | ...   |
     * +--------------+-------+
     * | Annotation   | ...   |
     * +--------------+-------+
     * | Condition    | ...   |
     * +--------------+-------+
     * | Is Directed  | ...   |  only if edge
     * +--------------+-------+
     */
    private void verifyTableModelForQGItem(QGItem qgItem) {
        boolean isEdge = qgItem instanceof QGEdge;
        QGObjTableModel tableModel = QueryCanvas.getTableModel(qgItem);

        assertEquals(2, tableModel.getColumnCount());
        assertEquals(isEdge ? 5 : 4, tableModel.getRowCount());

        assertEquals("Property", tableModel.getColumnName(0));
        assertEquals("Value", tableModel.getColumnName(1));

        assertEquals(false, tableModel.isCellEditable(0, 0));
        assertEquals(false, tableModel.isCellEditable(0, 1));
        assertEquals(false, tableModel.isCellEditable(1, 0));
        assertEquals(true, tableModel.isCellEditable(1, 1));
        assertEquals(false, tableModel.isCellEditable(2, 0));
        assertEquals(true, tableModel.isCellEditable(2, 1));
        assertEquals(false, tableModel.isCellEditable(3, 0));
        assertEquals(true, tableModel.isCellEditable(3, 1));
        if (isEdge) {
            assertEquals(false, tableModel.isCellEditable(4, 0));
            assertEquals(true, tableModel.isCellEditable(4, 1));
        }

        assertEquals(String.class, tableModel.getClassAt(0, 0));
        assertEquals(String.class, tableModel.getClassAt(0, 1));
        assertEquals(String.class, tableModel.getClassAt(1, 0));
        assertEquals(String.class, tableModel.getClassAt(1, 1));
        assertEquals(String.class, tableModel.getClassAt(2, 0));
        assertEquals(Annotation.class, tableModel.getClassAt(2, 1));
        assertEquals(String.class, tableModel.getClassAt(3, 0));
        assertEquals(CondEleWrapper.class, tableModel.getClassAt(3, 1));
        if (isEdge) {
            assertEquals(String.class, tableModel.getClassAt(4, 0));
            assertEquals(Boolean.class, tableModel.getClassAt(4, 1));
        }

        assertEquals("Type", tableModel.getValueAt(0, 0));
        assertEquals((qgItem instanceof QGVertex ? "Vertex" : "Edge"),
                tableModel.getValueAt(0, 1));
        assertEquals("Name", tableModel.getValueAt(1, 0));
        assertEquals(qgItem.firstName(), tableModel.getValueAt(1, 1));
        assertEquals("Annotation", tableModel.getValueAt(2, 0));
        assertEquals(qgItem.annotation(), tableModel.getValueAt(2, 1));
        assertEquals("Condition", tableModel.getValueAt(3, 0));
        assertEquals(qgItem.condEleChild(),
                ((CondEleWrapper) tableModel.getValueAt(3, 1)).getCondEleChild());
        if (isEdge) {
            assertEquals("Is Directed", tableModel.getValueAt(4, 0));
            assertEquals(new Boolean(((QGEdge) qgItem).isDirected()),
                    tableModel.getValueAt(4, 1));
        }
    }

}
