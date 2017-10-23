/**
 * $Id: ObjectCloner.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.qgraph2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Utility that makes a "deep" copy of an Object (unlike Object.clone()) using
 * serialization.
 * <p/>
 * Source: http://www.google.com/search?q=cache:UlbgKtuNZ3QC:www.javaworld.com/javaworld/javatips/jw-javatip76.html+java+deep+copy+objects&hl=en
 * <p/>
 * Usage:
 * <p/>
 * Vector vNew = (Vector)(ObjectCloner.deepCopy(vector1));
 * <p/>
 * todo move to Util!
 */
public class ObjectCloner {

    // so that nobody can accidentally create an ObjectCloner object
    private ObjectCloner() {
    }


    // returns a deep copy of an object
    static public Object deepCopy(Object oldObj) throws Exception {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            // serialize and pass the object
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bin);
            // return the new object
            return ois.readObject();
        } catch (Exception e) {
            System.out.println("Exception in ObjectCloner = " + e);
            throw(e);
        } finally {
            oos.close();
            ois.close();
        }
    }


}
