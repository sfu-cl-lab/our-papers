/**
 * $Id: RDNTestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

/**
 * $Id: RDNTestUtil.java 3658 2007-10-15 16:29:11Z schapira $
 */

package kdl.prox.model2.rdn;

import kdl.prox.db.Attributes;
import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.db.Subgraph;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.probdistributions.DiscreteProbDistribution;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.aggregators.ModeAggregator;
import kdl.prox.model2.rpt.aggregators.NopAggregator;
import kdl.prox.model2.rpt.featuresettings.UnfilteredFeatureSetting;
import org.apache.log4j.Logger;

public class RDNTestUtil {

    static Logger log = Logger.getLogger(RDNTestUtil.class);

    public static String MOVIE_TEST_CONT_NAME = "MovieTestContainer";
    public static String STUDIO_TEST_CONT_NAME = "studioTestContainer";

    public static void createDB() {
        DB.getRootContainer().deleteAllChildren();
        Container movieTestContainer = DB.getRootContainer().createChild(MOVIE_TEST_CONT_NAME);
        Container studioTestContainer = DB.getRootContainer().createChild(STUDIO_TEST_CONT_NAME);

        /* This will create a training set of movie subgraphs that all look like
         *   core_movie ------- studio ------- linked_movie1
         *                             ------- linked_movie2
         * with attributes movie.year and linked_movie.month, and class labels movie.month.
         * subgID   itemID  name        attributes      values
         * 1        1       movie       {class,year}    {jan,95}
         * 1        7       studio      {color}         {red}
         * 1        2       lk_movie    {month,year}    {---,93}
         * 1        3       lk_movie    {month,year}    {---,95}
         *
         * 2        2       movie       {class,year}    {jan,93}
         * 2        7       studio      {color}         {red}
         * 2        1       lk_movie    {month,year}    {---,95}
         * 2        3       lk_movie    {month,year}    {---,95}
         *
         * 3        3       movie       {class,year}    {jan,95}
         * 3        7       studio      {color}         {red}
         * 3        1       lk_movie    {month,year}    {---,95}
         * 3        2       lk_movie    {month,year}    {---,93}
         *
         * 4        4       movie       {class,year}    {feb,94}
         * 4        8       studio      {color}         {blue}
         * 4        5       lk_movie    {month,year}    {---,94}
         * 4        6       lk_movie    {month,year}    {---,93}
         *
         * 5        5       movie       {class,year}    {feb,94}
         * 5        8       studio      {color}         {blue}
         * 5        4       lk_movie    {month,year}    {---,94}
         * 5        6       lk_movie    {month,year}    {---,93}
         *
         * 6        6       movie       {class,year}    {feb,93}
         * 6        8       studio      {color}         {blue}
         * 6        4       lk_movie    {month,year}    {---,94}
         * 6        5       lk_movie    {month,year}    {---,94}
         */

        /* This will create a training set of movie subgraphs that all look like
         *   core_studio ------- linked_movie1
         *               ------- linked_movie2
         *               ------- linked_movie3
         * with attributes linked_movie.month and class labels studio.color
         * subgID   itemID  name        attributes      values
         * 10        7       studio      {class}         {red}
         * 10        1       lk_movie    {month}         {---}
         * 10        2       lk_movie    {month}         {---}
         * 10        3       lk_movie    {month}         {---}
         * 20        8       studio      {class}         {blue}
         * 20        1       lk_movie    {month}         {---}
         * 20        2       lk_movie    {month}         {---}
         * 20        3       lk_movie    {month}         {---}
         */
        //Note: links are not put in the subgraphs.

        // create item attributes
        Attributes objAttrs = DB.getObjectAttrs();
        objAttrs.deleteAllAttributes();
        objAttrs.defineAttribute("color", "str");
        objAttrs.defineAttribute("month", "str");
        objAttrs.defineAttribute("year", "int");

        NST attrNST = objAttrs.getAttrDataNST("color");
        attrNST.insertRow(new String[]{"7", "red"});
        attrNST.insertRow(new String[]{"8", "blue"});
        attrNST.release();

        attrNST = objAttrs.getAttrDataNST("month");
        attrNST.insertRow(new String[]{"1", "jan"});
        attrNST.insertRow(new String[]{"2", "jan"});
        attrNST.insertRow(new String[]{"3", "jan"});
        attrNST.insertRow(new String[]{"4", "feb"});
        attrNST.insertRow(new String[]{"5", "feb"});
        attrNST.insertRow(new String[]{"6", "feb"});
        attrNST.release();

        attrNST = objAttrs.getAttrDataNST("year");
        attrNST.insertRow(new String[]{"1", "95"});
        attrNST.insertRow(new String[]{"2", "93"});
        attrNST.insertRow(new String[]{"3", "95"});
        attrNST.insertRow(new String[]{"4", "94"});
        attrNST.insertRow(new String[]{"5", "94"});
        attrNST.insertRow(new String[]{"6", "93"});
        attrNST.release();

        Subgraph sub = movieTestContainer.getSubgraph(1);
        sub.insertObject(1, "movie");
        sub.insertObject(7, "studio");
        sub.insertObject(2, "lk_movie");
        sub.insertObject(3, "lk_movie");

        sub = movieTestContainer.getSubgraph(2);
        sub.insertObject(2, "movie");
        sub.insertObject(7, "studio");
        sub.insertObject(1, "lk_movie");
        sub.insertObject(3, "lk_movie");

        sub = movieTestContainer.getSubgraph(3);
        sub.insertObject(3, "movie");
        sub.insertObject(7, "studio");
        sub.insertObject(1, "lk_movie");
        sub.insertObject(2, "lk_movie");

        sub = movieTestContainer.getSubgraph(4);
        sub.insertObject(4, "movie");
        sub.insertObject(8, "studio");
        sub.insertObject(5, "lk_movie");
        sub.insertObject(6, "lk_movie");

        sub = movieTestContainer.getSubgraph(5);
        sub.insertObject(5, "movie");
        sub.insertObject(8, "studio");
        sub.insertObject(4, "lk_movie");
        sub.insertObject(6, "lk_movie");

        sub = movieTestContainer.getSubgraph(6);
        sub.insertObject(6, "movie");
        sub.insertObject(8, "studio");
        sub.insertObject(4, "lk_movie");
        sub.insertObject(5, "lk_movie");

        sub = studioTestContainer.getSubgraph(10);
        sub.insertObject(7, "studio");
        sub.insertObject(1, "lk_movie");
        sub.insertObject(2, "lk_movie");
        sub.insertObject(3, "lk_movie");

        sub = studioTestContainer.getSubgraph(20);
        sub.insertObject(8, "studio");
        sub.insertObject(4, "lk_movie");
        sub.insertObject(5, "lk_movie");
        sub.insertObject(6, "lk_movie");
    }

    /**
     * mode([movie.year])=95 {feb: 36.0, jan: 36.0}
     * leaf with 18.0 subgs {feb: 0.0, jan: 18.0}
     * mode([movie.year])=94 {feb: 36.0, jan: 18.0}
     * leaf with 18.0 subgs {feb: 18.0, jan: 0.0}
     * mode([studio.color])=red {feb: 18.0, jan: 18.0}
     * leaf with 18.0 subgs {feb: 0.0, jan: 18.0}
     * leaf with 18.0 subgs {feb: 18.0, jan: 0.0}
     */
    public static RPT createMovieRPT() {
        // set the isSingleValue and isContinuous here, so that the applies? method of the Aggregators work
        final Source movieLabel = new AttributeSource("movie", "month").setIsContinuous(false).setIsSingleValue(true);
        final Source movieYear = new AttributeSource("movie", "year").setIsContinuous(false).setIsSingleValue(true);
        final Source studioColor = new AttributeSource("studio", "color").setIsContinuous(false).setIsSingleValue(true);

        RPTNode movieRoot = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 36).addAttributeValue("feb", 36));
        movieRoot.setSplit(new UnfilteredFeatureSetting(movieYear, new NopAggregator(movieYear), "95"));

        RPTNode movieY = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 18).addAttributeValue("feb", 0));

        RPTNode movieN = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 18).addAttributeValue("feb", 36));
        movieN.setSplit(new UnfilteredFeatureSetting(movieYear, new NopAggregator(movieYear), "94"));

        RPTNode movieNY = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 0).addAttributeValue("feb", 18));

        RPTNode movieNN = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 18).addAttributeValue("feb", 18));
        movieNN.setSplit(new UnfilteredFeatureSetting(studioColor, new NopAggregator(studioColor), "red"));

        RPTNode movieNNY = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 18).addAttributeValue("feb", 0));
        RPTNode movieNNN = new RPTNode(new DiscreteProbDistribution().addAttributeValue("jan", 0).addAttributeValue("feb", 18));

        movieRoot.setYesBranch(movieY);
        movieRoot.setNoBranch(movieN);
        movieN.setYesBranch(movieNY);
        movieN.setNoBranch(movieNN);
        movieNN.setYesBranch(movieNNY);
        movieNN.setNoBranch(movieNNN);

        return new RPT().learnFromRPTNode(movieRoot, (AttributeSource) movieLabel);
    }

    /**
     * mode([lk_movie.month])=jan {red: 8.0, blue: 8.0}
     * leaf with 8.0 subgs {red: 8.0, blue: 0.0}
     * leaf with 8.0 subgs {red: 0.0, blue: 8.0}
     */
    public static RPT createStudioRPT() {
        final Source studioLabel = new AttributeSource("studio", "color").setIsContinuous(false).setIsSingleValue(true);
        final Source movieMonth = new AttributeSource("lk_movie", "month").setIsContinuous(false).setIsSingleValue(false);

        RPTNode studioRoot = new RPTNode(new DiscreteProbDistribution().addAttributeValue("red", 8).addAttributeValue("blue", 8));
        studioRoot.setSplit(new UnfilteredFeatureSetting(movieMonth, new ModeAggregator(movieMonth), "jan"));

        RPTNode studioY = new RPTNode(new DiscreteProbDistribution().addAttributeValue("red", 8).addAttributeValue("blue", 0));
        RPTNode studioN = new RPTNode(new DiscreteProbDistribution().addAttributeValue("red", 0).addAttributeValue("blue", 8));

        studioRoot.setYesBranch(studioY);
        studioRoot.setNoBranch(studioN);

        return new RPT().learnFromRPTNode(studioRoot, (AttributeSource) studioLabel);
    }


}
