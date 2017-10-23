/**
 * $Id: QGAddLink.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2;

import kdl.prox.qged.QGAddLinkFormat;
import kdl.prox.util.Assert;

import java.io.Serializable;

/**
 * An update edge from a QGraph query.
 * After the query is run, all add-link elements are added between the
 * specified vertices in the resulting subgraps.
 * For example, with a query A --> B --> C[] and an add-link A --> C,
 * for all pairs of A and C in a given subgraph, a new link will be created
 * with the given attrname/attrvalue
 */
public class QGAddLink implements AbsQueryChild, Serializable {

    private static final QGAddLinkFormat QG_ADDLINK_FORMAT = new QGAddLinkFormat();    // used by toString()

    /**
     * Names of vertices to relate.
     */
    private String vertex1Name;
    private String vertex2Name;

    /**
     * Name and value of the attribute to create for the new edge
     */
    private String attrName;
    private String attrValue;

    /**
     * The Subquery or Query that contains me. Set by setParentAQuery(). null if
     * not set.
     */
    private AbstractQuery parentAQuery = null;

    public QGAddLink(String vertex1Name, String vertex2Name, String attrName, String attrValue) {
        Assert.condition(vertex1Name != null, "vertex1Name null");
        Assert.condition(vertex2Name != null, "vertex2Name null");
        Assert.condition(attrName != null, "attrName null");
        Assert.condition(attrValue != null, "attrValue null");

        this.vertex1Name = vertex1Name;
        this.vertex2Name = vertex2Name;
        this.attrName = attrName;
        this.attrValue = attrValue;
    }

    public QGAddLink() {
        this("vertex1", "vertex2", "attrname", "attrval");
    }

    public String getAttrName() {
        return attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public String getVertex1Name() {
        return vertex1Name;
    }

    public String getVertex2Name() {
        return vertex2Name;
    }

    // AbsQueryChild methods. Ignore

    public Annotation annotation() {
        return null;  // No annotations
    }

    public void deleteAnnotation() {
        // No annotations
    }

    public AbstractQuery parentAQuery() {
        return parentAQuery;

    }

    public void setFrom(QGAddLink otherAddLink) {
        vertex1Name = otherAddLink.getVertex1Name();
        vertex2Name = otherAddLink.getVertex2Name();
        attrName = otherAddLink.getAttrName();
        attrValue = otherAddLink.getAttrValue();
    }

    public void setAnnotation(Annotation ele) {
        // No annotations
    }

    public void setParentAQuery(AbstractQuery parentAQuery) {
        this.parentAQuery = parentAQuery;
    }

    public String toString() {
        return QG_ADDLINK_FORMAT.format(this);
    }
}
