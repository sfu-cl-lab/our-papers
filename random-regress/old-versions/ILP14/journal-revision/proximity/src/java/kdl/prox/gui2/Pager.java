/**
 * $Id: Pager.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: Pager.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.gui2;

import kdl.prox.dbmgr.NST;
import kdl.prox.monet.ResultSet;
import kdl.prox.util.Assert;
import org.apache.log4j.Logger;


/**
 * Class that helps break long HTML lists into viewable 'pages'. Page numbers
 * start with 1 (not 0).
 */
public class Pager {

    private static Logger log = Logger.getLogger(Pager.class);

    private NST nst; // set by NST constructor
    private int pageNum;
    private int numRowsPerPage;
    private int numPages;
    private int numRows;


    /**
     * Constructor for a pager based on an NST
     *
     * @param theNST
     * @param pageNum
     * @param numRowsPerPage
     */
    public Pager(NST theNST, int pageNum, int numRowsPerPage) {
        this.pageNum = pageNum;
        this.numRowsPerPage = numRowsPerPage;
        numRows = theNST.getRowCount();
        numPages = (numRows % numRowsPerPage == 0 ? numRows / numRowsPerPage :
                (numRows / numRowsPerPage) + 1);
        Assert.condition(this.numRowsPerPage > 0, "numRowsPerPage must be > 0");
        checks();
        // and save the NST
        this.nst = theNST;
    }

    private void checks() {
        boolean isValidPageNum = (this.pageNum <= numPages) || (this.pageNum == 1 && numPages == 0);
        Assert.condition(this.pageNum > 0, "pageNum must be > 0");
        Assert.condition(isValidPageNum, "pageNum must be <= number of " +
                "pages (" + numPages + "," + this.pageNum + ")");
    }

    /**
     * @return previous page number. returns -1 if no previous (at page 1)
     */
    public int getPrevPageNum() {
        return (pageNum == 1 ? -1 : pageNum - 1);
    }


    /**
     * @return previous next number. returns -1 if no next (at last page)
     */
    public int getNextPageNum() {
        return (pageNum == numPages ? -1 : pageNum + 1);
    }


    public int getNumPages() {
        return numPages;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getPageNum() {
        return pageNum;
    }


    public ResultSet getResultSet() {
        return getResultSet("*");
    }

    public ResultSet getResultSet(String colList) {
        int startRowNum = (pageNum - 1) * numRowsPerPage;
        int endRowNum = startRowNum + numRowsPerPage - 1;
        return nst.selectRows("*", colList, startRowNum + "-" + endRowNum, true);
    }

    public boolean hasNext() {
        return (pageNum < numPages);
    }

    public boolean hasPrev() {
        return pageNum > 1;

    }

    public void next() {
        pageNum += 1;
        checks();
    }

    public void prev() {
        pageNum -= 1;
        checks();
    }


}
