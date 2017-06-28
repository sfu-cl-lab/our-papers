/**
 * $Id: LabelGroup.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.qged;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import java.awt.Font;
import kdl.prox.qgraph2.AbsQueryChild;
import kdl.prox.qgraph2.Annotation;
import kdl.prox.qgraph2.QGItem;
import kdl.prox.qgraph2.Subquery;
import kdl.prox.util.Assert;


/**
 * Groups textual information for a QGItem or Subquery.
 */
public class LabelGroup extends PPath {

    private static final int LINE_HEIGHT = 16;      // todo base on font
    private static final ConditionFormat CONDITION_FORMAT = new ConditionFormat();

    private AbsQueryChild absQueryChild;

    private PText annotPText = new PText();
    private PText condPText = new PText();
    private PText namePText = new PText();


    /**
     * Initializes based on absQueryChild's contents.
     *
     * @param absQueryChild
     */
    public LabelGroup(AbsQueryChild absQueryChild) {
        Assert.condition((absQueryChild instanceof QGItem) ||
                (absQueryChild instanceof Subquery), "absQueryChild wasn't an " +
                "instance of QGItem or Subquery: " + absQueryChild);
        this.absQueryChild = absQueryChild;
        setPickable(false);
        setChildrenPickable(false);

        // create piccolo nodes for content
        setPathToRectangle(0, 0, 100, 50);  // arbitrary location and size. location fixed by caller. size doesn't matter
        setStrokePaint(null);
//        setStrokePaint(Color.LIGHT_GRAY);   // TEST - shows group outline

        namePText.setOffset(0, LINE_HEIGHT * 0);
        namePText.setFont(namePText.getFont().deriveFont(Font.ITALIC));
        condPText.setOffset(0, LINE_HEIGHT * 1);
        annotPText.setOffset(0, LINE_HEIGHT * 2);

        addChild(namePText);
        addChild(condPText);
        addChild(annotPText);

        // do initial update
        updateContent();
    }

    /**
     * @return annotation text associated with my absQueryChild. returns null if none
     */
    public String getAnnotationText() {
        Annotation annotation = absQueryChild.annotation();
        return (annotation == null ? null : annotation.annotationString());
    }

    /**
     * @return condition text associated with my absQueryChild. returns null if none
     */
    public String getConditionText() {
        if (absQueryChild instanceof QGItem) {
            QGItem qgItem = (QGItem) absQueryChild;
            return CONDITION_FORMAT.format(new CondEleWrapper(qgItem.condEleChild()));
        } else {
            return null;
        }
    }

    /**
     * @return name associated with my absQueryChild. returns null if none
     */
    public String getName() {
        return (absQueryChild instanceof QGItem ?
                ((QGItem) absQueryChild).firstName() : null);
    }

    /**
     * Sets text of my IVs according to my absQueryChild's content.
     */
    public void updateContent() {
        annotPText.setText(getAnnotationText());
        condPText.setText(getConditionText());
        namePText.setText(getName());
        QueryCanvas.shrinkBoundsToFitPNode(this, 0);
    }

}
