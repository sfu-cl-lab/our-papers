/**
 * $Id: SchemaAnalysis.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.db;

import kdl.prox.dbmgr.NST;
import kdl.prox.monet.MonetException;
import kdl.prox.monet.ResultSet;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * This class retrieves summary information about the
 * schema of the database.
 */
public class SchemaAnalysis {
    String objTypeAttrName;
    String linkTypeAttrName;

    private static final Logger log = Logger.getLogger(SchemaAnalysis.class);


    /**
     * An object which holds statistics on a single <o1 type, link
     * type, o2 type> triple.
     */
    public static class LinkStats {
        private String objFrom;
        private String objTo;
        private String linkType;
        private double percentOccurrance;
        private int ctOccurrance;


        /**
         * Creates a statistics object for a particular link type.
         *
         * @param objFrom           the type of the object(s) at the begining of the link
         * @param objTo             the type of the object(s) at the end of the link
         * @param linkType          the type of the link
         * @param percentOccurrance the percent of links of this type
         *                          which connect object of type objFrom to object of type
         *                          objTo
         * @param ctOccurrance      the number of links of this type which
         *                          connect objects of type objFrom to objects of type objTo
         */
        public LinkStats(String objFrom, String objTo, String linkType,
                         double percentOccurrance, int ctOccurrance) {

            this.objFrom = objFrom;
            this.objTo = objTo;
            this.linkType = linkType;
            this.percentOccurrance = percentOccurrance;
            this.ctOccurrance = ctOccurrance;

        }

        /**
         * Equals for LinkStats objects compares all of the data in
         * the object - including the percentOccurance and ctOccurance
         * variables.
         */

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LinkStats)) return false;

            final LinkStats linkStats = (LinkStats) o;

            if (ctOccurrance != linkStats.ctOccurrance) return false;
            if (percentOccurrance != linkStats.percentOccurrance) return false;
            if (linkType != null ? !linkType.equals(linkStats.linkType) : linkStats.linkType != null) return false;
            if (objFrom != null ? !objFrom.equals(linkStats.objFrom) : linkStats.objFrom != null) return false;
            if (objTo != null ? !objTo.equals(linkStats.objTo) : linkStats.objTo != null) return false;

            return true;
        }

        /**
         * The hashcode for the object is based on all instance
         * variables, including percent Occurance and ctOccurance.
         */
        public int hashCode() {
            int result;
            long temp;
            result = (objFrom != null ? objFrom.hashCode() : 0);
            result = 29 * result + (objTo != null ? objTo.hashCode() : 0);
            result = 29 * result + (linkType != null ? linkType.hashCode() : 0);
            temp = Double.doubleToLongBits(percentOccurrance);
            result = 29 * result + (int) (temp ^ (temp >>> 32));
            result = 29 * result + ctOccurrance;
            return result;
        }


        public String getObjFrom() {
            return objFrom;
        }

        public void setObjFrom(String objFrom) {
            this.objFrom = objFrom;
        }

        public String getObjTo() {
            return objTo;
        }

        public void setObjTo(String objTo) {
            this.objTo = objTo;
        }

        public String getLinkType() {
            return linkType;
        }

        public void setLinkType(String linkType) {
            this.linkType = linkType;
        }

        public double getPercentOccurrance() {
            return percentOccurrance;
        }

        public void setPercentOccurrance(double percentOccurrance) {
            this.percentOccurrance = percentOccurrance;
        }

        public int getCtOccurrance() {
            return ctOccurrance;
        }

        public void setCtOccurrance(int ctOccurrance) {
            this.ctOccurrance = ctOccurrance;
        }

    }


    /**
     * Class which performs simple schema analyis.
     *
     * @param objTypeAttrName  the name of the attribute which holds object type information
     * @param linkTypeAttrName the name of the attribute which holds link type information
     */
    public SchemaAnalysis(String objTypeAttrName, String linkTypeAttrName) {
        this.objTypeAttrName = objTypeAttrName;
        this.linkTypeAttrName = linkTypeAttrName;
    }

    /**
     * Adds one LinkStats object to the set of linkStats for each o2
     * type that this o1 type links to.
     *
     * @param filteredLinksNST the NST which has link_id, o1_id, and
     *                         o2_id in it, as well as other columns.
     * @param objTypeAttrVal   the o1 type
     * @param linkTypeAttrVal  the link type
     * @param linkStats        the set of existing LinkStats objects to which
     *                         we are adding
     * @param totalLinks       the total number of links of this type
     */
    private void addLinkStatsForO1Type(NST filteredLinksNST, String objTypeAttrVal,
                                       String linkTypeAttrVal, Set linkStats,
                                       int totalLinks) {

        // Get the o2_id of those links whose o1_id are of type objTypeAttrVal
        NST filteredO1TypeNST = DB.getObjects(objTypeAttrName + " = '" + objTypeAttrVal + "'");
        NST filteredObjTypesNST = filteredO1TypeNST.join(filteredLinksNST, "id = o1_id", "o2_id");

        // And now get the object types of those o2
        NST objTypeDataNST = DB.getObjects(objTypeAttrName + " != nil");
        NST filteredO2TypesNST = filteredObjTypesNST.join(objTypeDataNST, "o2_id = id", objTypeAttrName);

        // cycle through result set and add values to set
        ResultSet resultSet = filteredO2TypesNST.getColumnHistogram(objTypeAttrName);
        while (resultSet.next()) {
            String uniqueVal = resultSet.getString(0);
            int count = resultSet.getInt(1);
            double percentage = ((double) count) / totalLinks;
            linkStats.add(new LinkStats(objTypeAttrVal, uniqueVal, linkTypeAttrVal,
                    percentage, count));
        }

        //release NSTs
        filteredO1TypeNST.release();
        filteredObjTypesNST.release();
        filteredO2TypesNST.release();
        objTypeDataNST.release();
    }

    /**
     * Copied in from SchemaAnalysisLabEx.java, written by Andy Fast
     * et al. Gets the types of all the attributes in the DB.
     *
     * @param itemTypeAttrVal the name of the attribute holding the type
     * @param isObject        a boolean indicating whether to get objects (true) or links (false)
     * @return a Set of Strings describing the types.
     * @
     */
    public Set getAttributeTypes(String itemTypeAttrVal, boolean isObject) {
        NST filteredNST;
        Attributes attrs;
        String typeAttrName;

        if (isObject) {
            attrs = DB.getObjectAttrs();
            typeAttrName = objTypeAttrName;
        } else {
            attrs = DB.getLinkAttrs();
            typeAttrName = linkTypeAttrName;
        }
        filteredNST = attrs.getAttrDataNST(typeAttrName).filter("value = '" + itemTypeAttrVal + "'");

        // Iterate over the list of attributes; get the data for each, filter by those passed in,
        // and count if there are any rows
        Set attrList = new HashSet();
        ListIterator attrNameIter = attrs.getAttributeNames().listIterator();
        while (attrNameIter.hasNext()) {
            String attrName = (String) attrNameIter.next();
            if (attrName.equalsIgnoreCase(typeAttrName)) {
                continue; // skip the object/link type attribute
            }
            NST thisAttrNST = attrs.getAttrDataNST(attrName);
            NST superFilteredNST = thisAttrNST.join(filteredNST, "A.id = B.id ", "B.id");
            if (superFilteredNST.getRowCount() > 0) {
                attrList.add(attrName);
            }
            thisAttrNST.release();
            superFilteredNST.release();
        }
        log.debug("returning " + attrList.toString());
        filteredNST.release();
        return attrList;
    }

    /**
     * Copied in from SchemaAnalysisLabEx.java, written by Andy Fast et al
     *
     * @param isObject
     * @return
     * @
     */
    public Set getItemTypes(boolean isObject) {
        NST typeAttrNST;
        if (isObject) {
            Attributes attrs = DB.getObjectAttrs();
            if (attrs.isSingleValued(objTypeAttrName)) {
                typeAttrNST = attrs.getAttrDataNST(objTypeAttrName);
            } else {
                throw new MonetException("Type attr must be single valued, foo.");
            }
        } else {
            Attributes attrs = DB.getLinkAttrs();
            if (attrs.isSingleValued(linkTypeAttrName)) {
                typeAttrNST = attrs.getAttrDataNST(linkTypeAttrName);
            } else {
                throw new MonetException("Type attr must be single valued, foo.");
            }
        }
        Set vals = new HashSet();
        vals.addAll(typeAttrNST.getDistinctColumnValues("value"));
        typeAttrNST.release();
        return vals;
    }

    /**
     * Gets a set of all types which occur on the objects at the
     * beginning of the links in the filteredLinksNST NST. The set of
     * links, which is provided in the NST passed in, should already
     * contain only links of the type we are searching for.
     *
     * @param filteredLinksNST an NST which has link_id, o1_id, and
     *                         o2_id columns. Can be built by getFilteredLinksNST.
     * @return a set of String type names, one per o1 type
     */
    private Set getLinkConnectedO1Types(NST filteredLinksNST) {
        //get the DataNST for the objecttype attribute
        NST objTypeDataNST = DB.getObjects(objTypeAttrName + " != nil");
        NST filteredObjTypesNST = objTypeDataNST.join(filteredLinksNST, "id = o1_id", objTypeAttrName);

        List values = filteredObjTypesNST.getDistinctColumnValues(objTypeAttrName);
        Set objTypes = new HashSet();
        objTypes.addAll(values);

        //release NSTs
        filteredObjTypesNST.release();
        objTypeDataNST.release();

        //return set of values
        return objTypes;
    }


    /**
     * Gets a set of objects which contain summary information for
     * each (object 1 type, link type, object 2 type) triple that
     * exists for this link type. Summary information includes the
     * number of links of this type which go between objects of the
     * first type and objects of the second type, and is provided in a
     * LinkStats object.<p>
     *
     * @param linkTypeAttrVal type of link
     * @return set of LinkStats objects with information about the
     *         objects at the ends of this type of link
     */
    public Set getLinkConnectedObjStats(String linkTypeAttrVal) {

        Set linkStats = new HashSet();
        //get link attributes data structure
        NST filteredLinksNST = DB.getLinks(linkTypeAttrName + " = '" + linkTypeAttrVal + "'", linkTypeAttrName);
        int totalLinks = filteredLinksNST.getRowCount();

        Set types = getLinkConnectedO1Types(filteredLinksNST);
        Iterator iter = types.iterator();
        while (iter.hasNext()) {
            String objType = (String) iter.next();
            addLinkStatsForO1Type(filteredLinksNST, objType, linkTypeAttrVal, linkStats, totalLinks);
        }
        //release NSTs
        filteredLinksNST.release();

        return linkStats;

    }

    /**
     * Returns the linkConnectedObjStats as a set of StringsS
     * Each string contains
     * objFrom -> objTo (int(pct)%)
     *
     * @param linkTypeAttrVal
     */
    public Set getLinkConnectedObjStatsAsStringSet(String linkTypeAttrVal) {
        Set stringSet = new HashSet();
        Set linkConnectedObjStats = getLinkConnectedObjStats(linkTypeAttrVal);
        Iterator linkStatsIter = linkConnectedObjStats.iterator();
        while (linkStatsIter.hasNext()) {
            LinkStats thisStat = (LinkStats) linkStatsIter.next();
            StringBuffer statSB = new StringBuffer();
            statSB.append(thisStat.getObjFrom());
            statSB.append(" -> ");
            statSB.append(thisStat.getObjTo());
            statSB.append(" (");
            long pct = Math.round(thisStat.getPercentOccurrance() * 100);
            statSB.append(pct);
            statSB.append("%)");
            stringSet.add(statSB.toString());
        }
        return stringSet;
    }

}
