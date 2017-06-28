/**
 * $Id: QueryDataPair.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.family;

import kdl.prox.util.Assert;

import java.io.File;

/**
 * Helper class used by FamilyTestApp to represent a query instance file and
 * its associated data match file.
 */
public class QueryDataPair {

    /**
     * Descripion of this query (from file names). Set by constructor.
     */
    private String description;

    /**
     * Data match file ('query_<description>.mat'). Set by constructor.
     */
    private File matFile;

    /**
     * Query instance file ('query_<description>.xml'). Set by constructor.
     */
    private File queryFile;


    /**
     * Full-arg constructor. Saves arg in appropriate IV.
     *
     * @param queryOrMatFile
     * @param isQueryFile
     */
    public QueryDataPair(String description, File queryOrMatFile,
                         boolean isQueryFile) {
        QueryDataPair.checkFile(queryOrMatFile);
        Assert.stringNotEmpty(description, "null or empty description");
        if (isQueryFile) {
            this.queryFile = queryOrMatFile;
        } else {
            this.matFile = queryOrMatFile;
        }
        this.description = description;
    }


    /**
     * Util that throws IllegalArgumentException if queryOrMatFile is null
     * or doesn't exist. Does nothing o/w.
     *
     * @param queryOrMatFile
     */
    static void checkFile(File queryOrMatFile) {
        Assert.condition(queryOrMatFile != null && queryOrMatFile.exists(),
                "queryOrMatFile null or doesn't exist: " + queryOrMatFile);
    }


    /**
     * Returns my description.
     *
     * @return
     */
    public String getDescription() {
        return description;
    }


    /**
     * Returns my matFile.
     *
     * @return
     */
    public File getMatFile() {
        return matFile;
    }


    /**
     * Returns my queryFile.
     *
     * @return
     */
    public File getQueryFile() {
        return queryFile;
    }


    /**
     * Sets my matFile to matFile.
     */
    public void setMatFile(File matFile) {
        QueryDataPair.checkFile(matFile);
        this.matFile = matFile;
    }


    /**
     * Sets my queryFile to queryFile.
     */
    public void setQueryFile(File queryFile) {
        QueryDataPair.checkFile(queryFile);
        this.queryFile = queryFile;
    }

}
