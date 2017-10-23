/**
 * $Id: LineParser.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: LineParser.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

/**
 * Parses a smart ascii II file. Usage: create an instance and call parseLine()
 * on each line in the file. After each call to parseLine() check the state
 * methods to determine the type of the just-parsed line (isComment(), etc.),
 * then call the appropriate getter to get the content corresponding to that
 * line's type (getComment(), etc.) Note that some types of lines have more
 * than one assocated getter. See parseLine() for details.
 */
public class LineParser {

    // my line type state variables; only one is true at a time:
    private boolean isAttribute;
    private boolean isComment;
    private boolean isLink;
    private boolean isObject;

    // comment line type content:
    private String comment;

    // attribute line type content:
    private String attributeName;
    private String attributeValue;

    // link line type content:
    private String linkO1Name;
    private String linkO2Name;

    // object line type content:
    private String objectName;


    public LineParser() {
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public String getComment() {
        return comment;
    }

    public String getLinkO1Name() {
        return linkO1Name;
    }

    public String getLinkO2Name() {
        return linkO2Name;
    }

    public String getObjectName() {
        return objectName;
    }

    public boolean isAttribute() {
        return isAttribute;
    }

    public boolean isComment() {
        return isComment;
    }

    public boolean isLink() {
        return isLink;
    }

    public boolean isObject() {
        return isObject;
    }

    /**
     * Parses line and sets my current state according to line's type:
     * <p/>
     * o attribute: isAttribute(), getAttributeName(), getAttributeValue()
     * o comment: isComment(), getComment()
     * o link: isLink(), getLinkO1Name(), getLinkO2Name()
     * o object: isObject(), getObjectName()
     *
     * @param line
     */
    public void parseLine(String line) {
        resetState();
        if (line.startsWith("#")) {
            isComment = true;
            comment = line.substring(1);
            return;
        } else if (line.startsWith("*")) {
            int linkSepIdx = line.indexOf("->");
            if (linkSepIdx == -1) {
                isObject = true;
                objectName = line.substring(1).trim();
            } else {
                isLink = true;
                linkO1Name = line.substring(1, linkSepIdx).trim();
                linkO2Name = line.substring(linkSepIdx + 2).trim();
                if (linkO1Name.length() == 0) {
                    throw new IllegalArgumentException("invalid link " +
                            "definition: no o1 found: '" + linkO1Name + "'");
                } else if (linkO2Name.length() == 0) {
                    throw new IllegalArgumentException("invalid link " +
                            "definition: no o2 found: '" + linkO2Name + "'");
                }
            }
            return;
        }
        // we have either an attribute or an unknown line type
        int firstColonIdx = line.indexOf(":");
        if (firstColonIdx != -1) {
            isAttribute = true;
            attributeName = line.substring(0, firstColonIdx).trim();
            attributeValue = line.substring(firstColonIdx + 1).trim();
        } else {
            throw new IllegalArgumentException("unknown line type: '" + line + "'");
        }
    }

    void resetState() {
        isAttribute = false;
        isComment = false;
        isLink = false;
        isObject = false;
    }

}
