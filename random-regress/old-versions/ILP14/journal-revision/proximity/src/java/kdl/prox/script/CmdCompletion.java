/**
 * $Id: CmdCompletion.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.script;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;


/**
 * Given a python interpreter and a string, and returns a list of possible
 * completions for the last object in the input/.
 */
public class CmdCompletion {

    private static Logger log = Logger.getLogger(CmdCompletion.class);
    private static final String[] METHOD_EXCLUDE_NAMES = new String[]{"equals",
            "notify",
            "notifyAll",
            "wait"};


    /**
     * Copied from JEdit (http://cvs.sourceforge.net/viewcvs.py/jedit/jEdit/org/gjt/sp/jedit/MiscUtilities.java?rev=1.83&view=auto)
     */
    private static boolean compareChars(char ch1, char ch2, boolean ignoreCase) {
        if (ignoreCase)
            return Character.toUpperCase(ch1) == Character.toUpperCase(ch2);
        else
            return ch1 == ch2;
    }


    public static List getMethodNames(PythonInterpreter pyInterpreter, String input, String prefix) {
        List methods = getMethods(pyInterpreter, input);
        Set stringSet = new HashSet();
        for (int i = 0; i < methods.size(); i++) {
            Method method = (Method) methods.get(i);
            String methodName = method.getName();
            if (prefix.equals("") || methodName.startsWith(prefix)) {
                stringSet.add(methodName);
            }
        }
        List sortedStrings = new ArrayList(stringSet);
        Collections.sort(sortedStrings);
        return sortedStrings;
    }


    public static List getMethodArgs(PythonInterpreter pyInterpreter, String input, String prefix) {
        List methods = getMethods(pyInterpreter, input);
        List argList = new ArrayList();
        for (int i = 0; i < methods.size(); i++) {
            Method method = (Method) methods.get(i);
            String methodName = method.getName();
            if (prefix.equals("") || methodName.equals(prefix)) {
                StringBuffer argSB = new StringBuffer();
                // argSB.append(methodName); // do not show method names
                argSB.append(" (");
                Class[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    argSB.append("void");
                }
                for (int parameterIdx = 0; parameterIdx < parameterTypes.length; parameterIdx++)
                {
                    Class parameterType = parameterTypes[parameterIdx];
                    if (parameterIdx > 0) {
                        argSB.append(", ");
                    }
                    argSB.append(parameterType.getName());
                }
                argSB.append(")");
                argList.add(argSB.toString());
            }
        }
        return argList;
    }


    /**
     * Copied from JEdit (http://cvs.sourceforge.net/viewcvs.py/jedit/jEdit/org/gjt/sp/jedit/MiscUtilities.java?rev=1.83&view=auto)
     * Returns the longest common prefix in the given set of strings.
     *
     * @param str        The strings
     * @param ignoreCase If true, case insensitive
     * @since jEdit 4.2pre2
     */
    public static String getLongestPrefix(List str, boolean ignoreCase) {
        if (str.size() == 0)
            return "";

        int prefixLength = 0;

        loop:
        for (; ;) {
            String s = str.get(0).toString();
            if (prefixLength >= s.length())
                break loop;
            char ch = s.charAt(prefixLength);
            for (int i = 1; i < str.size(); i++) {
                s = str.get(i).toString();
                if (prefixLength >= s.length())
                    break loop;
                if (!compareChars(s.charAt(prefixLength), ch, ignoreCase))
                    break loop;
            }
            prefixLength++;
        }

        return str.get(0).toString().substring(0, prefixLength);
    }


    /**
     * Given a Jython input string, return a list of methods that the final
     * returned class implements
     *
     * @param pyInterpreter
     * @param input
     * @return list of Method instances
     */
    public static List getMethods(PythonInterpreter pyInterpreter, String input) {
        List components = Util.splitQuotedString(input, '.');

        // take the first one and find the class of the variable
        PyObject pyObject = pyInterpreter.get((String) components.get(0));
        if (pyObject == null || !(pyObject instanceof PyJavaInstance)) {
            return new ArrayList();
        }
        Object javaObj = pyObject.__tojava__(Object.class);     // correct way, believe it or not
        Class currentClass = javaObj.getClass();

        // iterate over the remaining components, setting currentClass to the
        // return type of the methods
        for (int compIdx = 1; compIdx < components.size(); compIdx++) {
            String component = (String) components.get(compIdx);
            // remove method arguments, if any
            int firstParenIdx = component.indexOf('(');
            if (firstParenIdx != -1) {
                component = component.substring(0, firstParenIdx);
            }
            // find methods for the current class, and get the set that match the component
            // save the return types in a set.
            Method[] methods = currentClass.getMethods();
            Set returnTypeSet = new HashSet();
            for (int methodsIdx = 0; methodsIdx < methods.length; methodsIdx++)
            {
                String methodName = methods[methodsIdx].getName();
                if (methodName.equals(component)) {
                    returnTypeSet.add(methods[methodsIdx].getReturnType());
                }
            }
            // if there is no return type, or more than one, we can't complete
            if (returnTypeSet.size() != 1) {
                return new ArrayList();
            } else {
                currentClass = (Class) returnTypeSet.iterator().next();
            }
        }

        // finally, return the methods of the last class, excluding some
        Method[] methods = currentClass.getMethods();
        return Arrays.asList(methods);
    }


    public static boolean isExcludedMethodName(String methodName) {
        for (int excludedId = 0; excludedId < METHOD_EXCLUDE_NAMES.length; excludedId++)
        {
            String methodExcludeName = METHOD_EXCLUDE_NAMES[excludedId];
            if (methodExcludeName.equals(methodName)) {
                return true;
            }
        }
        return false;
    }

}
