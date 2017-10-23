/**
 * $Id: MatrixFactorizationNSI.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.nsi2.nsi;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.Property;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import kdl.prox.db.DB;
import kdl.prox.nsi2.graph.Graph;
import kdl.prox.nsi2.util.GraphUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Matrix Factorization NSI is based on techniques proposed by Mao & Saul in 2004.  It uses Singular
 * Value Decomposition, which can be very slow.  The technique provides fairly accurate distance estimates,
 * and it is not hindered by assymetric distances.
 */
public class MatrixFactorizationNSI implements NSI {

    private static Logger log = Logger.getLogger(MatrixFactorizationNSI.class);
    private int num_landmarks;
    private List<Integer> landmarks;
    private Algebra alg;
    private DoubleMatrix2D landMatrix;
    private Map<Integer, DoubleMatrix2D> hostToX;
    private Map<Integer, DoubleMatrix2D> hostToY;

    public MatrixFactorizationNSI(int numLandmarks, Graph graph) {
        this.num_landmarks = numLandmarks;
        this.landmarks = GraphUtils.chooseRandomNodes(this.num_landmarks);
        log.info("Creating Matrix Factorization NSI with " + this.landmarks.size() + " landmarks");
        this.landMatrix = new DenseDoubleMatrix2D(this.num_landmarks, this.num_landmarks);
        this.alg = new Algebra(Property.TWELVE.tolerance());  //sets the tolerance of equality to e-12
        annotateGraph(graph);
    }

    private void annotateGraph(Graph graph) {

        List<Integer> hosts = DB.getObjectNST().selectRows().toOIDList("id");
        Map<Integer, DoubleMatrix2D> Dhosts = new HashMap<Integer, DoubleMatrix2D>();

        //initialize Dhosts for each host
        for (Integer host : hosts) {
            Dhosts.put(host, new DenseDoubleMatrix2D(1, num_landmarks));
        }

        //flood from each landmark to get all necessary distances
        int landmarkCtr = 0;
        for (Integer landmark : landmarks) {
            log.debug("flooding landmark " + landmarkCtr);
            //get all distances to this landmark
            Map<Integer, Double> dists = GraphUtils.floodWeighted(landmark, graph);

            //set entries in landmark matrix
            for (int j = 0; j < num_landmarks; j++) {
                landMatrix.setQuick(landmarkCtr, j, dists.get(landmarks.get(j)));
            }

            //set entries for each host
            //Compute distance to landmarks for a given host (assume for now Dout = Din)
            for (Integer host : hosts) {
                Dhosts.get(host).setQuick(0, landmarkCtr, dists.get(host));
            }

            landmarkCtr++;
        }

        //calculate matrix factorization of landmark distance matrix with svd
        SingularValueDecomposition svd = new SingularValueDecomposition(landMatrix);

        DoubleMatrix2D U = svd.getU();
        DoubleMatrix2D S = svd.getS();
        DoubleMatrix2D V = svd.getV();

        DoubleMatrix2D Sroot = S.assign(cern.jet.math.Functions.sqrt);

        DoubleMatrix2D X = alg.mult(U, Sroot);
        DoubleMatrix2D Y = alg.mult(V, Sroot);

        hostToX = new HashMap<Integer, DoubleMatrix2D>();
        hostToY = new HashMap<Integer, DoubleMatrix2D>();

        log.debug("Computing host-landmark distances");
        //for each host, compute its distance to each landmark
        int ctr = 1;
        for (Integer host : hosts) {
            if (ctr % 1000 == 0) {
                log.debug("\t" + ctr);
            }

            DoubleMatrix2D Dhost = Dhosts.get(host);

            DoubleMatrix2D Xhost = alg.mult(alg.mult(Dhost, Y), alg.inverse(alg.mult(alg.transpose(Y), Y)));
            DoubleMatrix2D Yhost = alg.mult(alg.mult(Dhost, X), alg.inverse(alg.mult(alg.transpose(X), X)));

            hostToX.put(host, Xhost);
            hostToY.put(host, Yhost);

            ctr++;
        }

    }

    public double distance(Integer nodeId1, Integer nodeId2) {
        if (nodeId1 == nodeId2) {
            return 0.0;
        }
        //dist = X_1 * Y_2^T
        return alg.mult(hostToX.get(nodeId1), alg.transpose(hostToY.get(nodeId2))).getQuick(0, 0);
    }

}
