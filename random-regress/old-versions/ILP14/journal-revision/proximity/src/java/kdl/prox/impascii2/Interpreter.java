/**
 * $Id: Interpreter.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Interpreter.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.impascii2;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class reads lines in a smart ascii ii file, decides their types, and
 * passes them to a DBHandler.
 */
public class Interpreter {

    // no IVs

    /**
     * Reads lines from a smart ascii ii file and calls appropriate methods
     * in dbHandler as each line is parsed.
     *
     * @param bufferedReader
     * @param dbHandler
     */
    public static void readLines(BufferedReader bufferedReader,
                                 DBHandler dbHandler) throws IOException {
        DBLineHandler dbLineHandler = new DBLineHandler(dbHandler);
        readLines(bufferedReader, dbLineHandler);
    }

    /**
     * Reads lines from a smart ascii ii file, calling appropriate methods in
     * lineHandler as they are parsed.
     */
    static void readLines(BufferedReader bufferedReader,
                          InterpreterLineHandler lineHandler) throws IOException {
        LineParser lineParser = new LineParser();
        for (String line = bufferedReader.readLine(); line != null;
             line = bufferedReader.readLine()) {
            if (line.trim().length() == 0) {
                continue;   // skip blank lines (LineParser doesn't like)
            }
            lineParser.parseLine(line);
            if (lineParser.isAttribute()) {
                String name = lineParser.getAttributeName();
                String value = lineParser.getAttributeValue();
                lineHandler.doAttribute(name, value);
            } else if (lineParser.isComment()) {
                String comment = lineParser.getComment();
                lineHandler.doComment(comment);
            } else if (lineParser.isLink()) {
                String o1Name = lineParser.getLinkO1Name();
                String o2Name = lineParser.getLinkO2Name();
                lineHandler.doLink(o1Name, o2Name);
            } else {    // lineParser.isObject())
                String name = lineParser.getObjectName();
                lineHandler.doObject(name);
            }
        }
    }

}
