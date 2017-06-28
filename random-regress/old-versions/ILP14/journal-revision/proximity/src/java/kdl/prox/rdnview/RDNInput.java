/**
 * $Id: RDNInput.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.rdnview;

import kdl.prox.util.Assert;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.util.*;


/**
 * Encapsulates input to RDNCanvas.
 */
public class RDNInput {

    private String description;        // optional description of the input. null if none
    private Document[] rptDocuments;      // input as passed to RDNCanvas()
    private Map nameMap;               // ""


    public RDNInput(String description, Document[] rptDocus, Map nameMap) {
        this.description = description;
        this.rptDocuments = rptDocus;
        this.nameMap = nameMap;
    }

    public String getDescription() {
        return description;
    }

    public Map getNameMap() {
        return nameMap;
    }

    public Document[] getRptDocuments() {
        return rptDocuments;
    }

    /**
     * Top-level utility that loads a RDN XML file and returns an RDNInput for it.
     *
     * @param rdnFile RDN XML file
     * @return RDNInput for the passed file
     */
    public static RDNInput loadRDNFile(File rdnFile) throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder(true);
        Document document = saxBuilder.build(rdnFile);
        Element rdnEle = document.getRootElement();

        // create rptDocuments based on <rpt-files> element
        List rptDocs = new ArrayList();     // List of RPT Elements
        Element rptFilesEle = rdnEle.getChild("rpt-files");
        Assert.notNull(rptFilesEle, "couldn't find <rpt-files> element in " +
                "file: " + rdnFile);

        List fileEles = rptFilesEle.getChildren("file");
        for (Iterator fileEleIter = fileEles.iterator(); fileEleIter.hasNext();) {
            Element fileEle = (Element) fileEleIter.next();
            String fileName = fileEle.getText();
            File rptFile = new File(rdnFile.getParentFile(), fileName);
            Assert.condition(rptFile.exists(), "could not find RPT file: " + rptFile);

            saxBuilder = new SAXBuilder(true);
            document = saxBuilder.build(rptFile);
            rptDocs.add(document);
        }

        // create nameMap based on <item-maps> element
        Map nameMap = new HashMap();    // maps 'from' String -> List of 'to' Strings
        Element itemMapsEle = rdnEle.getChild("item-maps"); // null if none specified
        if (itemMapsEle != null) {
            List mapEles = itemMapsEle.getChildren("map");
            for (Iterator mapEleIter = mapEles.iterator(); mapEleIter.hasNext();) {
                Element mapEle = (Element) mapEleIter.next();
                String fromText = mapEle.getChildText("from");
                List toEles = mapEle.getChildren("to");

                List toList = (List) nameMap.get(fromText);
                if (toList == null) {
                    toList = new ArrayList();
                    nameMap.put(fromText, toList);
                }

                for (Iterator toEleIter = toEles.iterator(); toEleIter.hasNext();) {
                    Element toEle = (Element) toEleIter.next();
                    String toText = toEle.getText();
                    toList.add(toText);
                }
            }
        }

        return new RDNInput(rdnFile.getName(),
                (Document[]) rptDocs.toArray(new Document[rptDocs.size()]),
                nameMap);
    }

    public String toString() {
        return (description == null ? "<no description>" : description) + " - " +
                rptDocuments.length + " file(s), " + nameMap;
    }

}
