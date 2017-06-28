/**
 * $Id: PopulateDB.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.dbmgr.NST;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines the populateDB() static method to aid easily setting up databases
 * The only possibly surprising features are:
 * - comments: start a line with "#" in column zero
 * - OID ranges: Generally, when an OID is expected the program accepts ranges
 * in the form of [START_OID-END_OID]
 * - OID sets: Similarly, you can specify individual OIDs in the
 * form of [OID1,OID2,...]
 * <p/>
 * Example contents for the file:
 * <p/>
 * attributes {
 * O CORE_ATTR_NAME str {
 * [20,21] +
 * }
 * L CONT_LINK_ATTR_NAME flt {
 * # comment
 * 1 10.0
 * 10 11.0
 * [4,6] 12.0
 * 8 14.0
 * }
 * }
 * <p/>
 * containers {
 * # comment
 * TEST_CONTAINER {
 * attributes {
 * SUBG_ATTR_NAME dbl {
 * [1,2,4] 1.0
 * 3 2.0
 * }
 * }
 * subgraph 0 {
 * O 20 CORE_NAME
 * L [1,2] LINK_NAME
 * }
 * subgraph 1 {
 * O [4-5] OBJ_NAME
 * L [3-4] LINK_NAME
 * }
 * }
 * }
 * <p/>
 * links {
 * 0 1 2
 * 5 6 6
 * # comment
 * }
 * <p/>
 * objects {
 * # here's a comment - '#' must start in column zero
 * [1-3]
 * 5
 * [11,13]
 * [15-15]
 * [22-21]
 * }
 * <p/>
 * Other samples of the format of this file can be found in test/kdl.prox.util
 */
public class PopulateDB {

    protected Logger log = Logger.getLogger(PopulateDB.class);

    private static void parseBlock(ParseState parseState, LineParser lineParser) {
        boolean isClosed = false;
        while (parseState.hasMoreLines()) {
            String line = parseState.getNextLine();
            if ("}".equals(line)) {
                isClosed = true;
                break;
            } else if (line.startsWith("#")) {  // skip comments
                continue;
            }
            try {
                lineParser.parseLine(line, parseState);
            } catch (IllegalArgumentException exc) {
                throw new IllegalArgumentException("error found in line #" +
                        parseState.getLineIdx() + ": " + parseState.getCurrentLine() + ": " + exc);
            }
        }
        // test that we exit because of a }, not because of EOF
        if (!isClosed) {
            throw new IllegalArgumentException("Missing closing }");
        }
    }

    /**
     * Clears the database then loads the data in fileName. See test files for
     * examples of the format.
     *
     * @param relativeClass class that fileName is relative to
     * @param fileName      data file
     */
    public static void populateDB(Class relativeClass, String fileName) {
        URL dataFileURL = relativeClass.getResource(fileName);
        String dataFile = dataFileURL.getFile();
        populateDB(dataFile);
    }

    /**
     * Clears the database then loads the data in fileName. See test files for
     * examples of the format.
     *
     * @param fileName data file
     */
    public static void populateDB(String fileName) {
        String dataString = Util.readStringFromFile(new File(fileName));
        ParseState parseState = new ParseState(dataString);
        while (parseState.hasMoreLines()) {
            String line = parseState.getNextLine();
            LineParser lineParser = null;
            if (line.matches("^objects\\s*\\{$")) {
                lineParser = new ObjectLineParser();
            } else if (line.matches("^links\\s*\\{$")) {
                lineParser = new LinkLineParser();
            } else if (line.matches("^attributes\\s*\\{$")) {
                lineParser = new AttributeDefLineParser();
            } else if (line.matches("^containers\\s*\\{$")) {
                lineParser = new ContainerLineParser();
            } else {
                throw new IllegalArgumentException("Unknown top-level category: " + line);
            }
            parseBlock(parseState, lineParser);
        }
    }

    //
    // inner classes
    //

    private static abstract class LineParser {

        protected Logger log = Logger.getLogger(LineParser.class);


        protected LineParser() {
        }

        protected int[] getOIDArray(String inputString) {
            String rangeRegex = "^\\[([0-9]+)\\s*-\\s*([0-9]+)\\]$";
            Pattern rangePattern = Pattern.compile(rangeRegex);
            Matcher rangeMatcher = rangePattern.matcher(inputString);

            String setRegex = "^\\[([0-9]+)(\\s*,\\s*([0-9]+))+\\]$";
            Pattern setPattern = Pattern.compile(setRegex);
            Matcher setMatcher = setPattern.matcher(inputString);

            int[] oidArray = null;
            if (rangeMatcher.matches()) {
                int int0 = Integer.parseInt(rangeMatcher.group(1));
                int int1 = Integer.parseInt(rangeMatcher.group(2));
                if (int0 <= int1) {
                    oidArray = new int[int1 - int0 + 1];
                    for (int intIdx = 0; intIdx < oidArray.length; intIdx++) {
                        oidArray[intIdx] = int0 + intIdx;
                    }
                } else {
                    oidArray = new int[]{};
                }
            } else if (setMatcher.matches()) {
                inputString = inputString.substring(1, inputString.length() - 1);
                String[] groups = inputString.split(",");
                oidArray = new int[groups.length];
                for (int intIdx = 0; intIdx < oidArray.length; intIdx++) {
                    oidArray[intIdx] = Integer.parseInt(groups[intIdx]);
                }
            } else if (inputString.matches("^[0-9]+$")) {
                oidArray = new int[]{Integer.parseInt(inputString)};
            } else {
                oidArray = null;
            }
            return oidArray;
        }

        public abstract void parseLine(String line, ParseState parseState);

    }


    public static class AttributeDefLineParser extends LineParser {


        public AttributeDefLineParser() {
            super();
        }

        public void parseLine(String line, ParseState parseState) {
            // Get the line. Create the attribute
            String[] lineElements = line.split("\\s");
            if (lineElements.length != 4 || !("{".equals(lineElements[3]))) {
                throw new IllegalArgumentException("Bad attribute definition");
            }
            boolean isObjectAttr = ("O".equals(lineElements[0]));
            String attrName = lineElements[1];
            String attrType = lineElements[2];
            Attributes attrs = (isObjectAttr ? DB.getObjectAttrs() : DB.getLinkAttrs());
            attrs.defineAttribute(attrName, attrType);

            // parse the block with attribute value
            NST attrDataNST = attrs.getAttrDataNST(attrName);
            AttributeValueLineParser attributeValueLineParser = new AttributeValueLineParser(attrDataNST);
            PopulateDB.parseBlock(parseState, attributeValueLineParser);
        }

    }


    public static class AttributeValueLineParser extends LineParser {

        private NST attrDataNST;


        public AttributeValueLineParser(NST attrDataNST) {
            super();
            this.attrDataNST = attrDataNST;
        }

        public void parseLine(String line, ParseState parseState) {
            String oidsPart = null;
            String valuePart = null;
            if (line.startsWith("[")) {
                int endIndex = line.indexOf(']');
                oidsPart = line.substring(0, endIndex + 1);
                valuePart = line.substring(endIndex + 2).trim();
            } else {
                String[] idAndValueStrings = line.split("\\s");
                oidsPart = idAndValueStrings[0];
                valuePart = line.substring(line.indexOf(oidsPart) + oidsPart.length()).trim();
            }
            int[] oidArray = getOIDArray(oidsPart);
            if (oidArray == null) {
                throw new IllegalArgumentException("Wrong attribute value format");
            }

            for (int idIdx = 0; idIdx < oidArray.length; idIdx++) {
                attrDataNST.insertRow(new String[]{oidArray[idIdx] + "", valuePart});
            }
        }
    }


    public static class ContainerContentsLineParser extends LineParser {

        private Container container;


        public ContainerContentsLineParser(Container container) {
            super();
            this.container = container;
        }

        public void parseLine(String line, ParseState parseState) {
            // Get the line. create and dispatch based on type - either an
            // attributes or a subgraph definition
            String[] lineElements = line.split("\\s");
            if (lineElements.length == 2 && "attributes".equals(lineElements[0]) && "{".equals(lineElements[1])) {
                SubgraphAttributeDefLineParser contAttrDefLineParser = new SubgraphAttributeDefLineParser(container);
                PopulateDB.parseBlock(parseState, contAttrDefLineParser);
            } else if (lineElements.length == 3 && "subgraph".equals(lineElements[0]) && "{".equals(lineElements[2])) {
                int subgID = Integer.parseInt(lineElements[1]);
                SubgraphLineParser subgraphLineParser = new SubgraphLineParser(container, subgID);
                PopulateDB.parseBlock(parseState, subgraphLineParser);
            } else {
                throw new IllegalArgumentException("Bad attributes or subgraph definition");
            }
        }

    }


    public static class ContainerLineParser extends LineParser {


        public ContainerLineParser() {
            super();
        }

        public void parseLine(String line, ParseState parseState) {
            // Get the line. Create the container (under root)
            String[] lineElements = line.split("\\s");
            if (lineElements.length != 2 || !("{".equals(lineElements[1]))) {
                throw new IllegalArgumentException("Bad container definition");
            }
            String containerName = lineElements[0];
            Container newContainer = DB.getRootContainer().createChild(containerName);

            // parse the block - contains either attribute or subgraph blocks
            ContainerContentsLineParser contentsLineParser = new ContainerContentsLineParser(newContainer);
            PopulateDB.parseBlock(parseState, contentsLineParser);
        }

    }


    public static class LinkLineParser extends LineParser {

        private NST linkNST;


        public LinkLineParser() {
            super();
            this.linkNST = DB.getLinkNST();
        }

        public void parseLine(String line, ParseState parseState) {
            int[] linkO1O2IDs = parseLineInternal(line);
            if (linkO1O2IDs == null) {
                throw new IllegalArgumentException("Wrong link line");
            }

            int linkOID = linkO1O2IDs[0];
            int o1OID = linkO1O2IDs[1];
            int o2OID = linkO1O2IDs[2];
            linkNST.insertRow(new String[]{linkOID + "", o1OID + "", o2OID + ""});
        }

        public int[] parseLineInternal(String line) {
            int[] linkO1O2IDs = null;

            String[] linkO1O2OIDs = line.split("\\s");
            if (linkO1O2OIDs.length != 3) {
                return null;
            }

            try {
                linkO1O2IDs = new int[]{Integer.parseInt(linkO1O2OIDs[0]),
                        Integer.parseInt(linkO1O2OIDs[1]),
                        Integer.parseInt(linkO1O2OIDs[2])};
            } catch (NumberFormatException e) {
                return null;
            }

            return linkO1O2IDs;
        }

    }


    public static class ObjectLineParser extends LineParser {

        private NST objectNST;


        public ObjectLineParser() {
            super();
            this.objectNST = DB.getObjectNST();
        }

        public void parseLine(String line, ParseState parseState) {
            int[] oidArray = parseLineInternal(line);
            if (oidArray == null) {
                throw new IllegalArgumentException("Wrong object format");
            }

            for (int idIdx = 0; idIdx < oidArray.length; idIdx++) {
                objectNST.insertRow(new String[]{oidArray[idIdx] + ""});
            }
        }

        public int[] parseLineInternal(String line) {
            String rangeRegex = "^\\[([0-9]+)\\s*-\\s*([0-9]+)\\]$";
            Pattern rangePattern = Pattern.compile(rangeRegex);
            Matcher rangeMatcher = rangePattern.matcher(line);

            String setRegex = "^\\[([0-9]+)(\\s*,\\s*([0-9]+))+\\]$";
            Pattern setPattern = Pattern.compile(setRegex);
            Matcher setMatcher = setPattern.matcher(line);

            int[] oidArray = null;
            if (rangeMatcher.matches()) {
                int int0 = Integer.parseInt(rangeMatcher.group(1));
                int int1 = Integer.parseInt(rangeMatcher.group(2));
                if (int0 <= int1) {
                    oidArray = new int[int1 - int0 + 1];
                    for (int intIdx = 0; intIdx < oidArray.length; intIdx++) {
                        oidArray[intIdx] = int0 + intIdx;
                    }
                } else {
                    oidArray = new int[]{};
                }
            } else if (setMatcher.matches()) {
                line = line.substring(1, line.length() - 1);
                String[] groups = line.split(",");
                oidArray = new int[groups.length];
                for (int intIdx = 0; intIdx < oidArray.length; intIdx++) {
                    oidArray[intIdx] = Integer.parseInt(groups[intIdx]);
                }
            } else if (line.matches("^[0-9]+$")) {
                oidArray = new int[]{Integer.parseInt(line)};
            } else {
                return null;
            }
            return oidArray;
        }

    }


    private static class ParseState {
        private String[] lines;
        private int lineIdx = 0;

        public ParseState(String rawLines) {
            lines = rawLines.split("\n");

        }

        public String getCurrentLine() {
            return lines[lineIdx - 1].trim();
        }

        public int getLineIdx() {
            return lineIdx;
        }

        public String getNextLine() {
            return lines[lineIdx++].trim();
        }

        public boolean hasMoreLines() {
            return (lineIdx < lines.length);
        }

    }


    public static class SubgraphAttributeDefLineParser extends LineParser {

        private Container container;


        public SubgraphAttributeDefLineParser(Container container) {
            super();
            this.container = container;
        }

        public void parseLine(String line, ParseState parseState) {
            // Get the line. Create the container attribute
            String[] lineElements = line.split("\\s");
            if (lineElements.length != 3 || !("{".equals(lineElements[2]))) {
                throw new IllegalArgumentException("Bad attribute definition");
            }
            String attrName = lineElements[0];
            String attrType = lineElements[1];
            Attributes attrs = container.getSubgraphAttrs();
            attrs.defineAttribute(attrName, attrType);

            // parse the block with attribute value
            NST attrDataNST = attrs.getAttrDataNST(attrName);
            AttributeValueLineParser attributeValueLineParser = new AttributeValueLineParser(attrDataNST);
            PopulateDB.parseBlock(parseState, attributeValueLineParser);
        }

    }


    public static class SubgraphLineParser extends LineParser {

        private Container container;
        private int subgID;


        public SubgraphLineParser(Container container, int subgID) {
            super();
            this.container = container;
            this.subgID = subgID;
        }

        public void parseLine(String line, ParseState parseState) {
            NST itemNST;
            if (line.startsWith("O")) {
                itemNST = container.getObjectsNST();
            } else if (line.startsWith("L")) {
                itemNST = container.getLinksNST();
            } else {
                throw new IllegalArgumentException("Wrong subgraph item format");
            }
            line = line.substring(1).trim();
            String oidsPart = null;
            String name = null;
            if (line.startsWith("[")) {
                int endIndex = line.indexOf(']');
                oidsPart = line.substring(0, endIndex + 1);
                name = line.substring(endIndex + 2).trim();
            } else {
                String[] idAndValueStrings = line.split("\\s");
                oidsPart = idAndValueStrings[0];
                name = line.substring(line.indexOf(oidsPart) + oidsPart.length()).trim();
            }
            int[] oidArray = getOIDArray(oidsPart);
            if (oidArray == null) {
                throw new IllegalArgumentException("Wrong attribute value format");
            }

            for (int idIdx = 0; idIdx < oidArray.length; idIdx++) {
                itemNST.insertRow(new String[]{oidArray[idIdx] + "", subgID + "", name});
            }
        }
    }


}
