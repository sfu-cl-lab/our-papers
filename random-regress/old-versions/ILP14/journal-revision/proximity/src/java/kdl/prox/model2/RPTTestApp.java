/**
 * $Id: RPTTestApp.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2;

import kdl.prox.db.Container;
import kdl.prox.db.DB;
import kdl.prox.model2.common.predictions.Predictions;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.ItemSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.RPT;
import kdl.prox.model2.rpt.modules.learning.DefaultLearningModule;
import kdl.prox.model2.rpt.modules.stopping.DefaultStoppingModule;
import kdl.prox.util.Util;
import org.apache.log4j.Logger;

/**
 */
public class RPTTestApp {

    private static Logger log = Logger.getLogger(RPTTestApp.class);

    /**
     * Builds a new RPT on the webkb database, for test
     */
    public static void main(String[] args) {

        Util.initProxApp();
        DB.open("localhost:45678");

        Container trainContainer = DB.getContainer("1d-clusters/samples/0");
        Container testContainer = DB.getContainer("1d-clusters/samples/1");
        AttributeSource classLabelSource = new AttributeSource("core_page", "pagetype");
        Source[] inputSources = new Source[]{
                new AttributeSource("core_page", "url_server_info"),
                new AttributeSource("core_page", "url_hierarchy1b"),
                new AttributeSource("linked_from_page", "page_num_outlinks"),
                new AttributeSource("linked_to_page", "page_num_inlinks"),
                new ItemSource("linked_from_page"),
                new ItemSource("linked_to_page")
        };


        if (true) {
            RPT rpt = new RPT();
            ((DefaultLearningModule) rpt.learningModule).stoppingModule = new DefaultStoppingModule().setMaxDepth(3);
            rpt.learn(trainContainer, classLabelSource, inputSources);
            log.info("Saving RPT");
            rpt.save("rpt2.xml");
            log.info("Done");
            rpt.print();
            Predictions predictions1 = rpt.apply(testContainer);
            predictions1.setTrueLabels(testContainer, classLabelSource);
            log.info("ACC:" + (1 - predictions1.getZeroOneLoss()));
            log.info("CLL: " + predictions1.getConditionalLogLikelihood());
            log.info("AUC: " + predictions1.getAUC("Student"));
        } else {
            RPT rpt1 = new RPT().load("rpt1.xml");
            Predictions predictions1 = rpt1.apply(testContainer);
            predictions1.setTrueLabels(testContainer, classLabelSource);
            log.info("ACC:" + (1 - predictions1.getZeroOneLoss()));
            log.info("CLL: " + predictions1.getConditionalLogLikelihood());
            log.info("AUC: " + predictions1.getAUC("Student"));

            RPT rpt2 = new RPT().load("rpt2.xml");
            Predictions predictions2 = rpt2.apply(testContainer);
            predictions2.setTrueLabels(testContainer, classLabelSource);
            log.info("ACC:" + (1 - predictions2.getZeroOneLoss()));
            log.info("CLL: " + predictions2.getConditionalLogLikelihood());
            log.info("AUC: " + predictions2.getAUC("Student"));

            predictions1.compare(predictions2);
        }
        DB.close();

    }
}
