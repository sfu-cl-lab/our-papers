/**
 * $Id: QGConstraint.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/** $Id: QGConstraint.java 3658 2007-10-15 16:29:11Z schapira $ */

package kdl.prox.qgraph2;


import java.io.Serializable;
import java.util.List;
import kdl.prox.qged.QGConstraintFormat;
import kdl.prox.util.Assert;
import org.jdom.Element;

/**
 * Represents one constraint from the list of possibly many constraints in a QGQuery.
 * QGConstraintS are created for every &lt;test> element in the &lt;constraint>
 * section of a query. &lt;test> elements are connected by &lt;and>S. When
 * processing a query, the transformations that apply a constraint do their work
 * and then remove the constraint from the list of constraints in the Query object.
 */
public class QGConstraint implements AbsQueryChild, Serializable {

    private static final QGConstraintFormat QGCONSATRAINT_FORMAT = new QGConstraintFormat();    // used by toString()

    // the elements of a constraint: operator, and two items
    private String operator;
    private String item1Name;
    private String item1AttrName;       // null if no attribute name -> using item1Name's ID
    private String item2Name;
    private String item2AttrName;       // null if no attribute name -> using item2Name's ID

    private boolean isEdgeConstraint;
    private Annotation annotation = null;   // null if no annotation on either item
    private String annotItemName = null;    // ""


    /**
     * The Subquery or Query that contains me. Set by setParentAQuery(). null if
     * not set. Not used; simply provided to conform to AbsQueryChild interface.
     */
    private AbstractQuery parentAQuery = null;


    public QGConstraint(Element condEle, boolean isEdge) {
        Assert.notNull(condEle, "empty condEle");

        // expand elements of condEle
        setOperator(condEle.getChildText("operator"));
        List itemEles = condEle.getChildren("item");
        Element item1 = (Element) itemEles.get(0);
        Element item2 = (Element) itemEles.get(1);
        isEdgeConstraint = isEdge;
        setItem1Name(item1.getChildText("item-name"));
        setItem1AttrName(item1.getChildText("attribute-name")); // may be null
        setItem2Name(item2.getChildText("item-name"));
        setItem2AttrName(item2.getChildText("attribute-name")); // may be null
    }


    public QGConstraint(Element condEle, String annotItemName,
                        Annotation annotation, boolean isEdge) {
        this(condEle, isEdge);
        Assert.notNull(annotation, "null annotation");
        Assert.stringNotEmpty(annotItemName, "empty annotItemName");
        this.annotation = annotation;
        this.annotItemName = annotItemName;
    }

    public QGConstraint() {
        item1Name = "item1";
        item2Name = "item2";
        operator = "<>";
        isEdgeConstraint = false;
    }


    public Annotation annotation() {
        return annotation;
    }


    public String annotItemName() {
        return annotItemName;
    }


    public void deleteAnnotation() {
        // do nothing. Annotations not allowed on constraints
    }


    public boolean equals(Object anObject) {
        if (this == anObject)
            return true;
        if (!(anObject instanceof QGConstraint))
            return false;
        if (getClass() != anObject.getClass())
            return false;		// different class
        // continue: compare condEleChild items
        QGConstraint qgConstraint = (QGConstraint) anObject;
        return operator.equals(qgConstraint.operator()) &&
                item1Name.equals(qgConstraint.item1Name()) &&
                ((item1AttrName == null ? qgConstraint.item1AttrName() == null :
                item1AttrName.equals(qgConstraint.item1AttrName()))) &&
                item2Name.equals(qgConstraint.item2Name()) &&
                ((item2AttrName == null ? qgConstraint.item2AttrName() == null :
                item2AttrName.equals(qgConstraint.item2AttrName())));
    }


    public boolean hasAnnot() {
        return (annotation != null);
    }


    public boolean isEdgeConstraint() {
        return isEdgeConstraint;
    }

    public String item1AttrName() {
        return item1AttrName;
    }


    public String item2AttrName() {
        return item2AttrName;
    }


    public String item1Name() {
        return item1Name;
    }

    public String item2Name() {
        return item2Name;
    }


    public String operator() {
        return operator;
    }


    /**
     * AbsQueryChild method. Returns my parentAQuery. null if not set
     */
    public AbstractQuery parentAQuery() {
        return parentAQuery;
    }


    public void setAnnotation(Annotation ele) {
        // do nothing. Annotations not allowed on contraints
    }

    public void setIsEdgeConstraint(boolean isEdge) {
        isEdgeConstraint = isEdge;
    }


    public void setItem1Name(String itemName) {
        Assert.stringNotEmpty(itemName, "empty itemName");
        this.item1Name = itemName;
    }


    public void setItem2Name(String itemName) {
        Assert.stringNotEmpty(itemName, "empty itemName");
        this.item2Name = itemName;
    }


    public void setItem1AttrName(String item1AttrName) {
        this.item1AttrName = item1AttrName;
    }


    public void setItem2AttrName(String item2AttrName) {
        this.item2AttrName = item2AttrName;
    }


    public void setOperator(String operator) {
        Assert.stringNotEmpty(operator, "empty operator");
        this.operator = operator;
    }


    /**
     * AbsQueryChild method. Sets my parentAQuery to parentAQuery. Works with
     * AbstractQuery.addXX() methods. Pass null to clear it.
     */
    public void setParentAQuery(AbstractQuery parentAQuery) {
        this.parentAQuery = parentAQuery;
    }

    public String toString() {
        return QGCONSATRAINT_FORMAT.format(this);
    }


}
