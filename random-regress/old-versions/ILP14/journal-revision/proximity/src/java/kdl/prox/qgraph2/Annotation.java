/**
 * $Id: Annotation.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: Annotation.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.io.Serializable;
import kdl.prox.qged.AnnotationFormat;
import kdl.prox.util.Assert;
import org.jdom.Element;


/**
 * Represents a numeric annotation, either on a QGItem (QGVertex or QGEdge), or
 * on an AbstractQuery.
 */
public class Annotation implements Serializable {

    private static final AnnotationFormat ANNOTATION_FORMAT = new AnnotationFormat();    // used by toString()

    private int annotMin;   // -1 if no value was specified
    private int annotMax;   // ""


    /**
     * Integer-based constructor. Throws IllegalArgumentException if
     * invalid parameters - recall validity conditions:
     * <p/>
     * Each numeric annotation is of the form [i..j], [i..], or [i],
     * where i and j are whole numbers and i < j.
     */
    public Annotation(int min, int max) {
        annotMin = min;
        annotMax = max;
        checkAnnotation();
    }


    /**
     * Full-arg constructor. Throws IllegalArgumentException if
     * annotationElement is invalid - recall validity conditions:
     * <p/>
     * Each numeric annotation is of the form [i..j], [i..], or [i],
     * where i and j are whole numbers and i < j.
     */
    public Annotation(Element annotationElement) {
        Assert.condition(annotationElement != null, "null annotationElement");
        
        // set annotMin and annotMax from annotationElement, checking its validity
        String minVal = annotationElement.getChildText("min");
        String maxVal = annotationElement.getChildText("max");
        
        // set annotMin
        if (minVal == null) {
            annotMin = -1;
        } else {
            try {
                annotMin = Integer.valueOf(minVal).intValue();
            } catch (NumberFormatException nfExc) {
                throw new IllegalArgumentException("min isn't an integer: '" +
                        minVal + "': " + nfExc);
            }
        }
        
        // set annotMax
        if (maxVal == null) {
            annotMax = -1;
        } else {
            try {
                annotMax = Integer.valueOf(maxVal).intValue();
            } catch (NumberFormatException nfExc) {
                throw new IllegalArgumentException("max isn't an integer: '" +
                        maxVal + "': " + nfExc);
            }
        }
        checkAnnotation();
    }


    public int annotMax() {
        return annotMax;
    }


    public int annotMin() {
        return annotMin;
    }


    /**
     * @return a pretty String for my annotation
     */
    public String annotationString() {
        return ANNOTATION_FORMAT.format(this);
    }


    private void checkAnnotation() {
        // check annotMin and annotMax (whole numbers; i < j)
        if (annotMin != -1)
            Assert.condition(annotMin >= 0, "min isn't >= 0: '" + annotMin + "'");
        if (annotMax != -1)
            Assert.condition(annotMax >= 0, "max isn't >= 0: '" + annotMax + "'");
        if ((annotMin != -1) && (annotMax != -1))
            Assert.condition(annotMin <= annotMax,
                    "min isn't <= max: '" + annotMin + "', '" + annotMax + "'");
    }


    public void setMin(int min) {
        this.annotMin = min;
    }

    public void setMax(int max) {
        this.annotMax = max;
    }

    public String toString() {
        return annotationString();
    }


}
