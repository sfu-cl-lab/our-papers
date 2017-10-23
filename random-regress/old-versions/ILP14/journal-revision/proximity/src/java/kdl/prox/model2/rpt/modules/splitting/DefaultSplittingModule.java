/**
 * $Id: DefaultSplittingModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.splitting;

import kdl.prox.dbmgr.NST;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The default splitting module computes contingency tables for all possible splits and finds the one
 * with the best gStatistic.
 */
public class DefaultSplittingModule implements RPTSplittingModule {

    protected static Logger log = Logger.getLogger(DefaultSplittingModule.class);


    protected boolean isChooseDeterministically = false;

    /**
     * Finds the best split
     */
    public Split chooseSplit(RPTState state, RPTScoringModule scoringMethod) {
        List fsList = state.featureSettingList;
        if (fsList.size() == 0) {
            return null;
        }

        if (isChooseDeterministically) {
            // sort the list, to always choose the same one in case of ties
            Collections.sort(fsList, new Comparator() {
                public int compare(Object o, Object o1) {
                    return o.toString().compareToIgnoreCase(o1.toString());
                }
            });
        } else {
            // shuffle (for every call/split), to break ties randomly
            Collections.shuffle(fsList);
        }

        NST labelNST = state.classLabel.getSourceTable();
        NST weightNST = state.subgIDs;

        // find the best
        int maxScoreIdx = 0;
        RPTScoringModule maxScore = null;
        for (int fsIdx = 0; fsIdx < fsList.size(); fsIdx++) {
            FeatureSetting featureSetting = (FeatureSetting) fsList.get(fsIdx);
            NST matchNST = state.nstCache.getTable(featureSetting.toString());
            RPTScoringModule newScore = scoringMethod.compute(state, labelNST, weightNST, matchNST, null);
            if (newScore.isBetterThan(maxScore)) {
                maxScoreIdx = fsIdx;
                maxScore = newScore;
            }
        }

        // return the best
        return new Split((FeatureSetting) fsList.get(maxScoreIdx), maxScore);
    }

    public boolean isChooseDeterministically() {
        return isChooseDeterministically;
    }

    public RPTSplittingModule setChooseDeterministically(boolean chooseDeterministically) {
        isChooseDeterministically = chooseDeterministically;
        return this;
    }

}
