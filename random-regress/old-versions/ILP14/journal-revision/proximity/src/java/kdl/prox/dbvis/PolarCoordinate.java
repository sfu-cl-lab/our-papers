/**
 * $Id: PolarCoordinate.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.dbvis;

/**
 * A simple rho theta pair. Used by RadialLayoutHelper.
 *
 * @see RadialLayoutHelper
 */
public class PolarCoordinate {

    private int rho;
    private double theta;


    public PolarCoordinate(int rho, double theta) {
        this.rho = rho;
        this.theta = theta;
    }

    public int getRho() {
        return rho;
    }

    public double getTheta() {
        return theta;
    }

    public static double calcX(int rho, double theta, double width, double height, int maxRho) {
        double radius = getRadius(height, width, maxRho);
        return ((Math.cos(theta) * radius * rho) + (width / 2));
    }

    public static double calcY(int rho, double theta, double width, double height, int maxRho) {
        double radius = getRadius(height, width, maxRho);
        return ((height / 2) - (Math.sin(theta) * radius * rho));
    }

    public static double getRadius(double height, double width, int maxRho) {
        double minDimension = (height < width ? height : width);
        double radius = (maxRho > 0) ? minDimension / maxRho : minDimension;
        return radius / 2;
    }

    public String toString() {
        return "(" + (theta / Math.PI) + " PI, " + rho + ")";
    }

}
