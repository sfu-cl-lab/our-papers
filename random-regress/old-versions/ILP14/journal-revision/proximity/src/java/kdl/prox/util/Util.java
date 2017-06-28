/**
 * $Id: Util.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Util.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.util;

import kdl.prox.db.DB;
import org.apache.log4j.*;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;

import java.io.*;
import java.net.URL;
import java.util.*;


/**
 * Util contains various useful static utility methods.
 */
public class Util {

    private static Logger log = Logger.getLogger(Util.class);
    private static final String LCF_FILE_NAME = "prox.lcf";     // standard log4j file

    /**
     * Class-based version numbers. Set by static initializer,
     * accessed by getCoreVersion() and getGUIVersion()
     */
    private static String coreVersion = "?";
    private static String guiVersion = "?";

    /**
     * Public log4j Logger for MIL commands. Usage: Instead of:
     * <p/>
     * cat.debug(milSB.toString());
     * <p/>
     * Do this:
     * <p/>
     * Util.mil.debug(milSB.toString());
     * <p/>
     * milMessages is used in MonetStream to print debug info
     */
    public static Logger mil = Logger.getLogger("mil");
    public static Logger milMessages = Logger.getLogger("milMessages");

    /**
     Static initializer that loads the version numbers.
     Values stored in coreVersion and guiVersion static variables
     */
    static {
        // do basic config
        LogManager.resetConfiguration();
        BasicConfigurator.configure();

        Properties versionProperties = new Properties();
        String versionPath = "resource/version.properties";
        URL versionURL = Util.class.getResource(versionPath);
        if (versionURL == null) {
            log.error("version file not found: " + versionPath);
        }
        try {
            InputStream inputStream = versionURL.openStream();
            versionProperties.load(inputStream);
        } catch (Exception e) {
            log.error("error reading version file: " + versionPath);
        }
        // set version variables
        coreVersion = versionProperties.getProperty("prox.version.core", "?");
        guiVersion = versionProperties.getProperty("prox.version.gui", "?");
    }


    /**
     * Don't let anyone instantiate this class
     */
    private Util() {
    }


    /**
     * Returns a copy of string with all '\' chars delimited with a second '\'
     */
    public static String delimitBackslash(String string) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int charIdx = 0; charIdx < string.length(); charIdx++) {
            char strChar = string.charAt(charIdx);
            if (strChar == '\\') {
                stringBuffer.append("\\");
            }
            stringBuffer.append(strChar);
        }
        return stringBuffer.toString();
    }


    /**
     * Return the core Proximity version number, read from a prop file
     * when the class is loaded (see coreVersion and guiVersion class variables)
     */
    public static String getCoreVersion() {
        return coreVersion;
    }


    /**
     * Return the core Proximity version number, read from a prop file
     * when the class is loaded (see coreVersion and guiVersion class variables)
     */
    public static String getGUIVersion() {
        return guiVersion;
    }


    /**
     * Returns the current Proximity Three Monet database schema's major version
     * number. Used to track a database's schema history.
     *
     * @return the current version as an int
     */
    public static int getSchemaMajorVersion() {
        return DB.SCHEMA_MAJOR_VERSION;
    }


    /**
     * Returns the current Proximity Three Monet database schema's minor version
     * number. Used to track a database's schema history.
     *
     * @return the current minor version as an int
     */
    public static int getSchemaMinorVersion() {
        return DB.SCHEMA_MINOR_VERSION;
    }


    /**
     * Initializes the log4j logging package, using a config file. If no config
     * file is specified, it does basic init. Prints an info message if file is
     * found . Does basic init if not found, and warns user.
     */
    public static void initLog4J() {
        // do basic config (needed both for null and invalid LCF_FILE_NAME)
        LogManager.resetConfiguration();
        BasicConfigurator.configure();
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);

        // try to load the file
        File logConfigFile = new File(LCF_FILE_NAME);
        if (logConfigFile.exists()) {
            log.info("* found log config file: " + logConfigFile.getAbsolutePath() + "'");
            LogManager.resetConfiguration();
            mil.setAdditivity(false); // See the file full-debug-plus-mil-file.lcf for an example on how to set the additivity back to true,
            milMessages.setAdditivity(false); //  so that its output also goes to the default appenders.
            PropertyConfigurator.configure(logConfigFile.getAbsolutePath());
        } else {
            log.warn("* log config file not found (using defaults): " +
                    logConfigFile.getAbsolutePath() + "'");
        }
    }


    /**
     * Initializes the Proximity Application
     * - initializes the logger
     */
    public static void initProxApp() {
        initLog4J();
    }

    /**
     * Returns true if the string is +,-,*,/
     */
    public static boolean isArithmeticOp(String arg) {
        return ("*".equals(arg) || "+".equals(arg) || "-".equals(arg) || "/".equals(arg));
    }


    /**
     * Returns true if the string represents a number
     */
    public static boolean isNumber(String arg) {
        // +/- digit(s).digit(s)   OR   +/- .digits(s)
        return arg.matches("^((\\+|\\-)?([0-9]+(\\.[0-9]+)?)|\\.[0-9]+)$");
    }

    /**
     * Returns true is the string is surrounded by "" or ''
     */
    public static boolean isQuoted(String arg) {
        return ((arg.startsWith("\"") || arg.startsWith("'")) &&
                (arg.endsWith("\"") || arg.endsWith("'")));
    }

    /**
     * Return true if the string is a quoted literal, or a number literal
     * Otherwise, returns true --it will be interpreted as a column name
     */
    public static boolean isValueArgument(String arg) {
        // if it's a quoted value, then it's a value
        return (isQuoted(arg) || isNumber(arg));
    }

    /**
     * Joins the elements of a collection with a , (or connector, rather) into a single string
     *
     * @param values
     * @param connector
     * @return a string of the values separated by commas
     */
    public static String join(Collection values, String connector) {
        String retString = "";
        int i = 0;
        for (Iterator valIterator = values.iterator(); valIterator.hasNext();) {
            String value = (String) valIterator.next();
            if (i > 0) {
                retString += connector;
            }
            retString += value;
            i++;
        }
        return retString;
    }

    public static String join(String[] values, String connector) {
        return join(Arrays.asList(values), connector);
    }

    /**
     * Scripting utility that returns a List based on (copied from) the passed
     * PyList. Inspired by http://bpaosf.bpa.arizona.edu/~kurtf/courses/mis507a/TiP/html/Chapter09.html
     */
    public static List listFromPyList(PyList pyList) {
        Assert.notNull(pyList, "pyList null");
        // continue
        return new ArrayList(Arrays.asList((Object[]) pyList.__tojava__(Object[].class)));
    }


    public static Map mapFromPyDict(PyDictionary pyDict) {
        Assert.notNull(pyDict, "pyDict null");
        // continue
        Map map = new HashMap();
        PyList keys = pyDict.keys();
        int size = keys.__len__();
        for (int i = 0; i < size; i++) {
            PyObject key = keys.pop();
            PyObject value = pyDict.get(key);
            map.put(key.__tojava__(Object.class), value.__tojava__(Object.class));
        }
        return map;
    }


    public static String unQuote(String str) {
        if (str == null) {
            return str;
        }
        if (isQuoted(str)) {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }


    public static String quote(String arg) {
        if (!isQuoted(arg)) {
            arg = "\"" + arg + "\"";
        }
        return arg;
    }

    /**
     * Parses the string, keeping quoted sections as a single word
     *
     * @param source
     * @param separator
     * @return a list of sub-strings separated by separator, keeping quoted portions together
     */
    public static List splitQuotedString(String source, char separator) {
        List words = new ArrayList();

        int from = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'') {
                // Read until eol or another "/'
                int j;
                for (j = i + 1; ; j++) {    // end test in body
                    // advance the pointer until we reach EOL, the matching
                    // delimiter (but not after a backslash)
                    boolean isEOL = (j >= source.length());
                    boolean isDelimitMatch = (source.charAt(j) == c);
                    boolean isPrevCharSlash = (source.charAt(j - 1) == '\\');
                    if (isEOL || (isDelimitMatch && !isPrevCharSlash)) {
                        break;
                    }
                }
                // Move i too ;
                i = j;
            }
            // Add the string to the list
            if (c == separator || i >= source.length() - 1) {
                String toAdd = source.substring(from, i + 1);
                if (separator == ' ') {
                    toAdd = toAdd.trim();
                } else if (toAdd.lastIndexOf(separator) != -1) {
                    toAdd = toAdd.substring(0, toAdd.lastIndexOf(separator));
                }
                if (toAdd.length() > 0) {
                    words.add(toAdd);
                }
                from = i + 1;
            }
        }

        return words;
    }

    /**
     * @param file
     * @return a String that contains file's contents. note that newlines are
     *         replaced by '\n'
     */
    public static String readStringFromFile(File file) {
        StringBuffer fileSB = new StringBuffer();
        BufferedReader bufferedReader = null;
        try {
            String line;
            bufferedReader = new BufferedReader(new FileReader(file));
            while ((line = bufferedReader.readLine()) != null) {
                fileSB.append(line);
                fileSB.append('\n');
            }
        } catch (IOException e) {
            junit.framework.Assert.fail("error reading: " + file);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return fileSB.toString();
    }

    /**
     * Saves a string to a file
     */
    public static void saveStringToFile(String str, File file) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        out.print(str);
        out.flush();
        out.close();
    }
}
