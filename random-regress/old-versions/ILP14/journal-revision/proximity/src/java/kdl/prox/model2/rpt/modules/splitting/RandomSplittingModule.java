/**
 * $Id: RandomSplittingModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */


package kdl.prox.model2.rpt.modules.splitting;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import kdl.prox.util.stat.StatUtil;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * A test splittigng module: chooses a feature setting randomly from the list
 */
public class RandomSplittingModule implements RPTSplittingModule {

    protected static Logger log = Logger.getLogger(RandomSplittingModule.class);

    public Split chooseSplit(RPTState state, RPTScoringModule scoringMethod) {
        List fsList = state.featureSettingList;
        if (fsList.size() == 0) {
            return null;
        }

        NST labelNST = state.classLabel.getSourceTable();
        NST weightNST = state.subgIDs;

        FeatureSetting featureSetting = (FeatureSetting) StatUtil.randomChoice(fsList);
        NST matchNST = state.nstCache.getTable(featureSetting.toString());
        RPTScoringModule score = scoringMethod.compute(state, labelNST, weightNST, matchNST, null);

        return new Split(featureSetting, score);
    }
}
