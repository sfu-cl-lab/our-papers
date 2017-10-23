/**
 * $Id: TFMList.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: TFMList.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.qgraph2.tfm;

import java.util.ArrayList;
import java.util.List;


/**
 * Top-level helper class that contains a List of all Transformations, including
 * those that can:
 * <p/>
 */
public class TFMList {

    /**
     * List of Transformations. Filled by constructor.
     */
    private List transformations = new ArrayList();


    /**
     * Full-arg constructor. Fill my transformations based on isUseNonSQLTFMs.
     */
    public TFMList() {
        transformations.add(new TFM01());	// # 1 done
        transformations.add(new TFM02());	// # 2 done
        transformations.add(new TFM06());	// # 6 done
        // transformations.add(new TFM08());	// # 8 not done
        transformations.add(new TFM09());	// # 9 done
        transformations.add(new TFM11());	// #11 done
        transformations.add(new TFM12());	// #12 done
        transformations.add(new TFM13());	// #13 done
        transformations.add(new TFM18());	// #18 done
        transformations.add(new TFM20());   // #20 done
        transformations.add(new TFM21());   // #21 done
        transformations.add(new TFM22());   // #22 done
    }


    /**
     * Returns my transformations. NB: Not a copy!
     *
     * @return
     */
    public List transformations() {
        return transformations;
    }


}
