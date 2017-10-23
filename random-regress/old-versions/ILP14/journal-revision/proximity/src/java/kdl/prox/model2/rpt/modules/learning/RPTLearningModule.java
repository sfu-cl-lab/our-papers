/**
 * $Id: RPTLearningModule.java 3658 2007-10-15 16:29:11Z schapira $
 *
 * Part of the open-source Proximity system
 *   (see LICENSE for copyright and license information).
 */

package kdl.prox.model2.rpt.modules.learning;

import kdl.prox.model2.rpt.RPTNode;
import kdl.prox.model2.rpt.RPTState;
import kdl.prox.model2.rpt.modules.aggregatorselection.RPTAggregatorSelectionModule;

public interface RPTLearningModule {

    public RPTNode learn(RPTState state);

    public IntermediateSplit getNextSplit(RPTNode node, RPTState state);

    public void applySplit(IntermediateSplit split);

    public RPTAggregatorSelectionModule getAggregatorSelectionModule();

}
