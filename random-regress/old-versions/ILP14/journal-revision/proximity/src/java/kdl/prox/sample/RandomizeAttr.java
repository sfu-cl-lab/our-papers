/**
 * $Id: RandomizeAttr.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.sample;

import kdl.prox.db.Attributes;
import kdl.prox.dbmgr.NST;
import kdl.prox.monet.Connection;
import kdl.prox.util.MonetUtil;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: rattigan
 * Date: Feb 6, 2004
 * Time: 4:03:17 PM
 * To change this template use Options | File Templates.
 */
public class RandomizeAttr {
    private static Logger log = Logger.getLogger(RandomizeAttr.class);

    /**
     * Randomize the values of an attribute for the objects in a given attr data NST
     *
     * @param attrs
     * @param origAttrName
     * @param newAttrName
     * @
     */
    public static void randomize(Attributes attrs, String origAttrName, String newAttrName) {
        NST origAttrDataNST = attrs.getAttrDataNST(origAttrName);
        String attrTypeDef = attrs.getAttrTypeDef(origAttrName);

        // make a copy of the BATs holding the ids and values
        String idBat = origAttrDataNST.getNSTColumn("id").getBATName();
        String valBat = origAttrDataNST.getNSTColumn("value").getBATName();

        //
        //log.debug("origAttrDataNST:");
        //MonetUtil.printNST(conn, origAttrDataNST, log);
        //log.debug("idBat:");
        //MonetUtil.printBAT(conn, idBat, log);
        //log.debug("valBat:");
        //MonetUtil.printBAT(conn, valBat, log);
        //

        String shuffledBat = getRandomizedBat(idBat, valBat);
        NST tmpNST = new NST(shuffledBat, "id", "value");

        // save the new values as an attr
        attrs.defineAttribute(newAttrName, attrTypeDef);
        NST newAttrDataNST = attrs.getAttrDataNST(newAttrName);
        newAttrDataNST.insertRowsFromNST(tmpNST);

        // clean up after yourself, you slob
        tmpNST.release();
    }

    /**
     * Helper method for randomize attr, but tastes great all by itself if you know what you're
     * doing.  Given two (synchronized, as in coming from the same NST) BATs, one with OIDs and
     * one with attr values, return a single BAT with OID head values and attr tail values.
     *
     * @param idBat
     * @param valBat
     * @return
     */
    public static String getRandomizedBat(String idBat, String valBat) {

        /*
log.debug("idBat:");
MonetUtil.printBAT(conn, idBat, log);
log.debug("valBat:");
MonetUtil.printBAT(conn, valBat, log);

log.debug("idBat has " + MonetUtil.getRowCount(conn, idBat) + " rows");
log.debug("valBat has " + MonetUtil.getRowCount(conn, valBat) + " rows");
log.debug("idBat has " + MonetUtil.max(conn, idBat) + " max");
log.debug("valBat has " + MonetUtil.max(conn, valBat) + " max");
log.debug("idBat has " + MonetUtil.getFirstOID(conn, idBat) + " first");
log.debug("valBat has " + MonetUtil.getFirstOID(conn, valBat) + " first");
        */

        String idBatClip = MonetUtil.slice(idBat, 0, 20);
        String valBatClip = MonetUtil.slice(valBat, 0, 20);
        //log.debug("idBat:");
        //MonetUtil.printBAT(conn, idBatClip, log);
        //log.debug("valBat:");
        //MonetUtil.printBAT(conn, valBatClip, log);
        Connection.releaseSavedVar(idBatClip);
        Connection.releaseSavedVar(valBatClip);

        String newIdBat = Connection.executeAndSave(idBat + ".copy().reverse().mark(oid(0));");
        String newValBat = Connection.executeAndSave(valBat + ".copy().reverse().mark(oid(0));");
        /*
        log.debug("newIdBat:");
        MonetUtil.printBAT(conn, newIdBat, log);
        log.debug("newValBat:");
        MonetUtil.printBAT(conn, newValBat, log);
        */

        String idOrderBat = Connection.executeAndSave("uniform(" + newIdBat + ".count());");
        String valOrderBat = Connection.executeAndSave("uniform(" + newValBat + ".count());");

        /*
        log.debug("idOrderBat:");
        MonetUtil.printBAT(conn, idOrderBat, log);
        log.debug("valOrderBat:");
        MonetUtil.printBAT(conn, valOrderBat, log);
        */

        String idsWithOrdersBat = Connection.executeAndSave(newIdBat + ".join(" + idOrderBat + ");");
        String valsWithOrdersBat = Connection.executeAndSave(newValBat + ".join(" + valOrderBat + ");");

        /*
        log.debug("idsWithOrdersBat:");
        MonetUtil.printBAT(conn, idsWithOrdersBat, log);
        log.debug("valsWithOrdersBat:");
        MonetUtil.printBAT(conn, valsWithOrdersBat, log);
        */

        String shuffledBat = Connection.executeAndSave(idsWithOrdersBat + ".join(" + valsWithOrdersBat + ".reverse())");

        /*
        log.debug("shuffledBat:");
        MonetUtil.printBAT(conn, shuffledBat, log);
        */

        Connection.releaseSavedVar(newIdBat);
        Connection.releaseSavedVar(newValBat);
        Connection.releaseSavedVar(idOrderBat);
        Connection.releaseSavedVar(valOrderBat);
        Connection.releaseSavedVar(idsWithOrdersBat);
        Connection.releaseSavedVar(valsWithOrdersBat);

        return shuffledBat;
    }

}
