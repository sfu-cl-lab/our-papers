/**
 * $Id: PythonScriptRunner.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/*
$Id: PythonScriptRunner.java 3658 2007-10-15 16:29:11Z schapira $

Author: Matthew Cornell, cornell@cs.umass.edu
Copyright (c) 2001 by Matthew Cornell. All Rights Reserved.

Status: Implementing.

*/

package kdl.prox.script;

import java.io.File;


/**
 * Test class that executes the Jython script file passed as an arg.
 */
public class PythonScriptRunner {


    /**
     * Full-arg constructor. Evaluates the specified Jython script file on the
     * Proximity database specified in the configuration file (and available
     * here via System properties). Binds an instance of the Proximity class
     * (instantiated for dbName) as the variable "prox".
     *
     * @param scriptFile
     */
    public PythonScriptRunner(File scriptFile, String[] args) {
    }


}
