/**
 * $Id: Arc.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;


/**
 * Represents a dependency arc from a type count or variable to a variable.
 * Variable self-loops ok.
 */
public class Arc {

    private String type1;   // 'from' item type
    private String var1;    // 'from' variable. null if from type's dot

    private String type2;   // 'to' item type
    private String var2;    // 'to' variable

    private boolean isBiDirectional;
    private boolean isMapped;   // true if either of my original end points was specified in terms of the item map (see RDNCanvas()'s classFeatureItemMap docs)


    /**
     * Full-arg constructor, leaves isBidirectional false. Latter must be
     * corrected by callers via setIsBiDirectional()
     *
     * @param type1
     * @param var1
     * @param type2
     * @param var2
     * @param isMapped
     */
    public Arc(String type1, String var1, String type2, String var2, boolean isMapped) {
        this.type1 = type1;
        this.type2 = type2;
        this.var1 = var1;
        this.var2 = var2;
        this.isMapped = isMapped;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Arc)) {
            return false;
        }

        Arc otherArc = (Arc) other;
        return type1.equals(otherArc.getType1()) &&
                (var1 == null ? otherArc.getVar1() == null : var1.equals(otherArc.getVar1())) &&
                type2.equals(otherArc.getType2()) &&
                var2.equals(otherArc.getVar2()) &&
                isBiDirectional == otherArc.isBiDirectional() &&
                isMapped == otherArc.isMapped();
    }

    public String getType1() {
        return type1;
    }

    public String getType2() {
        return type2;
    }

    public String getVar1() {
        return var1;
    }

    public String getVar2() {
        return var2;
    }

    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + type1.hashCode();
        hash = hash * 31
                + (var1 == null ? 0 : var1.hashCode());
        hash = hash * 31 + type2.hashCode();
        hash = hash * 31 + var2.hashCode();
        hash = hash * 31 + var2.hashCode();
        hash = hash * 31 + (isBiDirectional ? 1 : 0);   // todo xx OK?
        hash = hash * 31 + (isMapped ? 1 : 0);          // todo xx OK?
        return hash;
    }

    public boolean isBiDirectional() {
        return isBiDirectional;
    }

    public boolean isMapped() {
        return isMapped;
    }

    /**
     * @return true if I am from a degree dot, i.e., if my var1 is null. returns
     *         false if from a variable
     */
    public boolean isFromDegree() {
        return var1 == null;
    }

    /**
     * @param otherArc
     * @return true if my type1 and var1 fields equal otherArc's type1 and var1
     *         fields, respectively, and ditto for '2' their fields. returns
     *         false o/w. ignores isBiDirectional
     */
    public boolean isReverseOf(Arc otherArc) {
        return type1.equals(otherArc.getType2()) &&
                (var1 == null ? otherArc.getVar2() == null : var1.equals(otherArc.getVar2())) &&
                type2.equals(otherArc.getType1()) &&
                var2.equals(otherArc.getVar1());
    }

    public boolean isSelfLoop() {
        return type1.equals(type2) &&
                (var1 == null ? var2 == null : var1.equals(var2));
    }

    public void setBiDirectional(boolean biDirectional) {
        isBiDirectional = biDirectional;
    }

    /**
     * NB: toString() used in unit tests. don't change!
     *
     * @return
     */
    public String toString() {
        return type1 + (var1 == null ? "" : "." + var1) +
                (isBiDirectional ? "<>" : "->") + type2 + "." + var2 +
                (isMapped ? "*" : "");
    }

}
