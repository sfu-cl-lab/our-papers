/**
 * $Id: AggregatorFinderUtil.java 3749 2007-11-13 20:13:43Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.aggregators;

import kdl.prox.util.Assert;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AggregatorFinderUtil {

    private static Logger log = Logger.getLogger(AggregatorFinderUtil.class);

    private static Set<Class<Aggregator>> aggregatorClassSet = null;
    private static final String AGGREGATOR_CLASS_ENDING = "Aggregator.class";


    /**
     * Zero-arg overload. Looks for class names that end with *Aggregator.class in
     * the classpath. Looks at JAR/Zip files (proximity.jar only, for now), and
     * subdirectories, and returns a Set of Class instances. Caches the results.
     *
     * @return Set of Aggregator* Class instances
     */
    public static Set<Class<Aggregator>> searchForAggregatorClasses() {
        return searchForAggregatorClassesInternal(null);
    }

    /**
     * One-arg overload for testing.
     *
     * @param testJarFile jar file to use for search. pass null for normal classpath search
     * @return Set of Aggregator* Class instances
     */
    static Set<Class<Aggregator>> searchForAggregatorClassesInternal(File testJarFile) {
        if ((aggregatorClassSet == null) || (testJarFile != null)) {   // don't cache for testing
            aggregatorClassSet = new HashSet<Class<Aggregator>>();

            // get the names of all classes under the Aggregators/ package
            List<String> classNames = searchForAggregatorClassNames(testJarFile);
            for (Iterator<String> iterator = classNames.iterator(); iterator.hasNext();) {
                String className = iterator.next();

                // try to instantiate
                Class<?> aggregatorClass;
                try {
                    aggregatorClass = Class.forName(className);
                    if (Aggregator.class.isAssignableFrom(aggregatorClass) &&
                            aggregatorClass != Aggregator.class &&
                            !Modifier.isAbstract(aggregatorClass.getModifiers())) {
                        aggregatorClassSet.add((Class<Aggregator>) aggregatorClass);
                        log.debug("Found Aggregator class " + className);
                    } else {
                        log.debug("Skipping class " + className);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Could not instantiate class in Aggregator package: " +
                            className);
                }
            }
        }
        return aggregatorClassSet;
    }


    /**
     * Searches for names of *Aggregator.class files in classpath. Both in JAR/Zip
     * files, and subdirectories. Currently, only looks at proximity.jar JAR file.
     *
     * @param testJarFile jar file to use for search. pass null for normal classpath search
     * @return List of full class names off *Aggregator classes
     */
    private static List<String> searchForAggregatorClassNames(File testJarFile) {
        List<String> allClasses = new ArrayList<String>();
        if (testJarFile != null) {  // constrain search to testJarFile
            Assert.condition(testJarFile.exists(), "test jar file doesn't " +
                    "exist: " + testJarFile);
            allClasses.addAll(searchForAggregatorClassNamesInJar(testJarFile));
        } else {                    // do normal classpath search
            String classpathProp = System.getProperty("java.class.path");
            String pathSeparator = System.getProperty("path.separator");
            String[] classpaths = classpathProp.split(pathSeparator);
            for (int cpIdx = 0; cpIdx < classpaths.length; cpIdx++) {
                String pathElement = classpaths[cpIdx];
                File pathFile = new File(pathElement);
                if (pathFile.isDirectory()) {
                    allClasses.addAll(searchForAggregatorClassNamesInDir(pathFile.toString(), pathFile));
                } else if (pathFile.exists() && pathElement.endsWith("proximity.jar")) {
                    allClasses.addAll(searchForAggregatorClassNamesInJar(pathFile));
                }
            }
        }
        return allClasses;
    }

    /**
     * Searches for *Aggregator.class files in sub-directories, and recurses
     *
     * @param pathElement
     * @param pathFile
     * @return List of full class names of Aggregator classes
     */
    static List<String> searchForAggregatorClassNamesInDir(String pathElement, File pathFile) {
        char fileSepChar = System.getProperty("file.separator").charAt(0);
        List<String> theseClasses = new ArrayList<String>();
        String[] list = pathFile.list();
        for (int i = 0; i < list.length; i++) {
            File file = new File(pathFile, list[i]);
            if (file.isDirectory()) {
                theseClasses.addAll(searchForAggregatorClassNamesInDir(pathElement, file));
            } else if (file.exists() && (file.length() != 0) &&
                    file.toString().endsWith(AGGREGATOR_CLASS_ENDING)) {
                String classFileName = file.toString().substring(pathElement.length() + 1);
                classFileName = classFileName.substring(0, classFileName.indexOf(".class"));
                classFileName = classFileName.replace(fileSepChar, '.');
                theseClasses.add(classFileName);
            }
        }
        return theseClasses;
    }

    /**
     * Searches for classes *Aggregator.class in a given JAR/Zip file
     *
     * @param pathFile
     * @return List of full class names of Aggregator classes
     */
    private static List<String> searchForAggregatorClassNamesInJar(File pathFile) {
        // recall that zip files use a forward slash char to separate paths, as
        // mentioned here: http://bugs.sun.com/bugdatabase/view_bug.do;:WuuT?bug_id=4244499:
        // Within a ZIP file, pathnames use the forward slash / as separator, as
        // required by the ZIP <A HREF="ftp://ftp.uu.net/pub/archiving/zip/doc/appnote-970311-iz.zip">spec</A>.
        List<String> theseClasses = new ArrayList<String>();
        try {
            ZipFile zipFile = new ZipFile(pathFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().toString();
                if (entry.endsWith(AGGREGATOR_CLASS_ENDING)) {
                    entry = entry.substring(0, entry.indexOf(".class"));
                    entry = entry.replace('/', '.');
                    theseClasses.add(entry);
                }
            }
        } catch (IOException e) {
            log.warn("Error while processing Jar file " + pathFile + " : " + e);
        }
        return theseClasses;
    }


}
