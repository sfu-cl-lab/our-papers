/**
 * $Id: QGValidationError.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: QGValidationError.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2002 by Matthew Cornell, David Jensen. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.qgraph2;

import java.util.ArrayList;
import java.util.List;
import kdl.prox.util.Assert;


/**
 * Error subclass used by QueryValidator. Contains a List of error Strings,
 * one per validation error. Also contains the Query that was being validated.
 */
public class QGValidationError extends Error {

    /**
     * Query that was being validated. Set by constructor.
     */
    private Query query;

    /**
     * List of Strings describing query's errors, one String per error. Set by
     * constructor.
     */
    private List errorList;


    /**
     * Full-arg constructor. Checks args and saves them in IVs.
     */
    public QGValidationError(Query query, List errorList) {
        Assert.condition(query != null, "null query");
        Assert.condition(errorList != null, "null errorList");
        Assert.condition(errorList.size() != 0, "empty errorList");

        // continue
        this.query = query;
        this.errorList = new ArrayList(errorList);	// make a copy
    }

    public List getErrorList() {
        return errorList;
    }

    /**
     * Returns only the textual part of the error message (does not include
     * the internal parts using the kdl.prox.etc)
     */
    public String getMessage() {
        String str, substr;
        int endIndex;

        str = (errorList.get(0)).toString();
        endIndex = str.indexOf(":");

        substr = str.substring(0, Math.max(0, endIndex));
        return substr;
    }

}
