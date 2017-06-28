/**
 * $Id: ImportUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.util;

import kdl.prox.dbmgr.DataTypeEnum;
import kdl.prox.dbmgr.NST;
import kdl.prox.dbmgr.NSTUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;


/**
 * Defines basic utilities for importing data via Monet's ascii_io module
 */
public class ImportUtil {

    private static final Logger log = Logger.getLogger(ImportUtil.class);

    private static final String TEMP_DIR_PREFIX = "prox-temp-io";
    public static final String DATA_FILE_EXTENSION = "data";
    public static final String FORMAT_FILE_EXTENSION = "format";
    public static final char COLUMN_DELIMITER = '\t';
    public static final String ATTRIBUTES_FILE_NAME = "attributes";
    public static final String CONTAINERS_FILE_NAME = "containers";
    public static final String LINKS_FILE_NAME = "links";
    public static final String OBJECTS_FILE_NAME = "objects";

    private ImportUtil() {
        // disallow instances
    }

    /**
     * Creates a Monet ascii_io format file named fileName and containing the
     * column names and column types in colNamesAndTypes, separated by
     * COLUMN_DELIMITER. The file is created in the temp directory specified in
     * getFullFilePath().
     *
     * @param fileName
     * @param colNamesAndTypes
     */
    public static void createFormatFile(String path, String fileName,
                                        String[][] colNamesAndTypes)
            throws IOException {
        Assert.stringNotEmpty(fileName, "empty fileName");
        fileName = fileName + "." + FORMAT_FILE_EXTENSION;
        File fullPath = getFullFilePath(path, fileName);
        Writer formatFileWriter = new BufferedWriter(new FileWriter(fullPath));
        StringBuffer lineSB = new StringBuffer();
        for (int rowIdx = 0; rowIdx < colNamesAndTypes.length; rowIdx++) {
            String[] colNameAndType = colNamesAndTypes[rowIdx];
            String columnName = colNameAndType[0];
            String columnType = colNameAndType[1];
            lineSB.append(columnName);
            lineSB.append(",");
            lineSB.append(columnType);
            lineSB.append('\n');
        }
        formatFileWriter.write(lineSB.toString());
        formatFileWriter.close();
    }

    /**
     * Deletes all files and subdirectories under dir. Returns true if all
     * deletions were successful. If a deletion fails, the method stops
     * attempting to delete and returns false.
     * <p/>
     * taken from http://javaalmanac.com/egs/java.io/DeleteDir.html
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }


    /**
     * Dumps all the columns of an NST into temporary files, and returns
     * the names of the files. Files are saved in the tmp/ directory.
     * <p/>
     * NB: Overwrites existing files.
     *
     * @param nst
     * @param prefix
     * @return
     */
    public static String[] dumpNST(NST nst, String prefix) {
        Assert.notNull(nst, "null nst");
        Assert.condition(NSTUtil.isNSTConsistent(nst), "inconsistent NST");

        List nstColumnNames = nst.getNSTColumnNames();
        String[] fileNames = new String[nstColumnNames.size()];
        for (int colIdx = 0; colIdx < nstColumnNames.size(); colIdx++) {
            String colName = (String) nstColumnNames.get(colIdx);
            fileNames[colIdx] = Util.delimitBackslash(System.getProperty("java.io.tmpdir", "tmp") +
                    System.getProperty("file.separator") + prefix + colName);   // NB: file separator is not necessary on Windows, but is required on Linux due to whether the temp dir includes the final separator or not
            // Clean up str columns
            if (nst.getNSTColumn(colName).getType() == DataTypeEnum.STR) {
                String cleanColName = "cleanedup_" + colName;
                nst.addStringCleanupColumn(colName, cleanColName);
                nst.tofile(fileNames[colIdx], cleanColName);
                nst.removeColumn(cleanColName);
            } else {
                nst.tofile(fileNames[colIdx], colName);
            }
        }
        return fileNames;
    }

    /**
     * Returns value as a String, with bad chars replaced by ones that Monet
     * (or import) doesn't mind. Destructively modifies value.
     *
     * @return
     */
    public static String getCleanAttrValue(StringBuffer value) {
        for (int charIdx = 0; charIdx < value.length(); charIdx++) {
            char theChar = value.charAt(charIdx);
            if (isBadChar(theChar)) {
                value.setCharAt(charIdx, '_');
            }
        }
        return value.toString();
    }

    /**
     * Returns a Writer for the specified fileName. The file is created in the
     * temp directory
     *
     * @param fileName
     * @return
     * @throws java.io.IOException
     */
    public static Writer getDataWriter(String path, String fileName)
            throws IOException {
        File fullPath = getFullFilePath(path, fileName + "." + DATA_FILE_EXTENSION);
        return new BufferedWriter(new FileWriter(fullPath));
    }

    /**
     * Returns a File with the full path to fileName, using a temporary
     * directory. Assumes the directory exists.
     *
     * @param fileName
     * @return
     */
    public static File getFullFilePath(String path, String fileName) {
        return new File(path, fileName);
    }

    /**
     * Creates a new temporary directory
     */
    public static File getTempDir() throws IOException {
        File tempDir = File.createTempFile(TEMP_DIR_PREFIX, "");
        if (!tempDir.delete()) {
            throw new IOException();
        }
        if (!tempDir.mkdir()) {
            throw new IOException();
        }
        return tempDir;
    }

    /**
     * @param theChar
     * @return true if theChar is one that causes Monet errors
     */
    public static boolean isBadChar(char theChar) {
        return (theChar == '\'') || (theChar == '\"') || (theChar == '\n') ||
                (theChar == COLUMN_DELIMITER);
    }

    public static int lineCount(File x) throws IOException {
        BufferedReader buffReader = new BufferedReader(new FileReader(x));
        int lineCnt = 0;
        for (String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
            lineCnt++;
        }

        return lineCnt;
    }

    public static NST loadNST(String path, String fileNameRoot) throws IOException {
        return loadNST(path, fileNameRoot, fileNameRoot);
    }

    /**
     * Loads an NST into memory, from a file and a format definition file
     *
     * @param formatFileName
     * @param dataFileName
     * @return
     */
    public static NST loadNST(String path, String formatFileName, String dataFileName) throws IOException {
        File formatFullPath = getFullFilePath(path, formatFileName + "." + FORMAT_FILE_EXTENSION);
        if (!formatFullPath.exists()) {
            return null;
        }

        String colNames = "";
        String colTypes = "";
        try {
            BufferedReader buffReader = new BufferedReader(new FileReader(formatFullPath));
            for (String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                String[] elts = line.split(",");
                colNames += elts[0] + ",";
                colTypes += elts[1] + ",";
            }
            colNames = colNames.substring(0, colNames.length() - 1);
            colTypes = colTypes.substring(0, colTypes.length() - 1);
        } catch (IOException e) {
            log.fatal("Error reading file " + formatFullPath + ":" + e);
            return null;
        }

        return loadNST(path, dataFileName, colNames, colTypes);
    }

    public static NST loadNST(String path, String dataFileName, String colNames, String colTypes) throws IOException {
        if (!dataFileName.endsWith("." + DATA_FILE_EXTENSION)) {
            dataFileName = dataFileName + "." + DATA_FILE_EXTENSION;
        }
        File dataFullPath = getFullFilePath(path, dataFileName);
        if (!dataFullPath.exists()) {
            return null;
        }

        NST nst = new NST(colNames, colTypes).fromfile(dataFullPath.getAbsolutePath());
        int rowCount = nst.getRowCount();
        int lineCount = lineCount(dataFullPath);
        if (rowCount != lineCount) {
            throw new IllegalArgumentException("Error loading file " + dataFullPath +
                    "; resulting NST doesn't have the same number of lines as file (" + rowCount + " vs " + lineCount + ")." +
                    "Check whether the file contains special characters, accents, etc. that Monet might not accept");
        }
        return nst;
    }

    /**
     * Writes a line to writer that contains the columns in columns, each
     * separated by COLUMN_DELIMITER.
     *
     * @param writer
     * @param columns
     */
    public static void writeLine(Writer writer, String[] columns)
            throws IOException {
        StringBuffer lineSB = new StringBuffer();
        for (int columnIdx = 0; columnIdx < columns.length; columnIdx++) {
            String column = columns[columnIdx];
            if (columnIdx != 0) {
                lineSB.append(COLUMN_DELIMITER);
            }
            lineSB.append(column);
        }
        lineSB.append('\n');
        writer.write(lineSB.toString());
    }

}
