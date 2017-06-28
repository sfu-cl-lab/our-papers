/**
 * $Id: RPTState.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 *
 */

package kdl.prox.model2.rpt;

import kdl.prox.db.Container;
import kdl.prox.dbmgr.NST;
import kdl.prox.model2.common.sources.AttributeSource;
import kdl.prox.model2.common.sources.Source;
import kdl.prox.model2.rpt.featuresettings.FeatureSetting;
import kdl.prox.model2.rpt.modules.scoring.RPTScoringModule;
import kdl.prox.model2.util.NSTCache;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to hold information that is passed between recursive invocations of the
 * apply and learn methods.
 */
public class RPTState {

    public NSTCache nstCache;
    public Container trainContainer;
    public AttributeSource classLabel;
    public Source[] inputSources;

    public List<FeatureSetting> featureSettingList;
    public int depth;
    public NST subgIDs;

    //public RPTScoringModule currentBestScore;  // is this being used?
    //public HashMap<String, RPTScoringModule> currentBestAttrScore;
    //public HashMap<String, Integer> featureSettingsScoreCounts;

    public Map<String, Double> itemAcLevels;

    public List<RPTScoringModule> pseudosamples;

    private static Logger log = Logger.getLogger(RPTState.class);

    public RPTState() {
        nstCache = new NSTCache();
        depth = 0;
        featureSettingList = new ArrayList<FeatureSetting>();
        itemAcLevels = new HashMap<String, Double>();
        pseudosamples = null;
        //currentBestAttrScore = new HashMap<String, RPTScoringModule>();
        //featureSettingsScoreCounts = new HashMap<String, Integer>();
    }

    public RPTState(RPTState source, int newDepth, NST newSubgIDs) {
        nstCache = source.nstCache;
        trainContainer = source.trainContainer;
        classLabel = source.classLabel;
        featureSettingList = new ArrayList<FeatureSetting>(source.featureSettingList);
        inputSources = source.inputSources;
        itemAcLevels = source.itemAcLevels;
        pseudosamples = source.pseudosamples;
        //currentBestAttrScore = source.currentBestAttrScore;
        //featureSettingsScoreCounts = source.featureSettingsScoreCounts;

        depth = newDepth;
        subgIDs = newSubgIDs;
    }

}

