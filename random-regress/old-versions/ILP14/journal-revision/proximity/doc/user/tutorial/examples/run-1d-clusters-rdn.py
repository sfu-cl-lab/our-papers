# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Import the necessary class definitions.
from kdl.prox.model2.common.sources import *
from kdl.prox.model2.rpt import RPT
from kdl.prox.model2.rdn import RDN
from kdl.prox.model2.rdn.modules.listeners import LoggingListener

# Make sample "0" the training set and sample "1" the test set.
trainContainer = prox.getContainer("1d-clusters/samples/0")
testContainer = prox.getContainer("1d-clusters/samples/1")

# -------------------------
# Create component RPT
# -------------------------

# Define what we want to predict.

# The subgraph object whose label we want to predict
coreItemName = 'core_page'

# The name of the attribute we want to predict
attrToPredict = 'pagetype'

# The attribute we want to predict
# Format: item name, attribute name
classLabel = AttributeSource(coreItemName, attrToPredict)

# The set of attributes we'll use in learning the model.
inputSources = [ \
   AttributeSource('core_page', 'url_server_info'), \
   AttributeSource ('core_page', 'url_hierarchy1b'), \
   AttributeSource('linked_from_page', 'page_num_outlinks'), \
   AttributeSource('linked_from_page', 'pagetype'), \
   AttributeSource('linked_to_page', 'page_num_inlinks'), \
   AttributeSource('linked_to_page', 'pagetype'), \
   ItemSource("linked_from_page"), \
   ItemSource("linked_to_page")]

# ---------------------------------
# Begin modeling portion of script

print "Beginning modeling section"

# Instantiate the model.
print "Instantiating component RPT..."
rpt = RPT()
rpt.learningModule.stoppingModule.setMaxDepth(3)

# Train (learn) the tree.
print "Learning component RPT..."
rpt.learn(trainContainer, classLabel, inputSources)

# Output XML.
xmlFileName = 'ProxWebKB_RPTforRDN.xml'
rpt.save(xmlFileName)
print "RPT written to ", xmlFileName

# -------------------------
# Instantiate the RDN
# -------------------------

print "Instantiating RDN..."
rdn = RDN();

# Set the "burn-in" period for Gibbs sampling.
rdn.statisticModule.burnInSteps = 100

# Set the "gap" in previous Gibbs iterations to consider when doing
# new iterations
rdn.statisticModule.skipSteps = 2

# Set the number of Gibbs sampling iterations.
# This script stops after 200 iterations to limit execution time for this
# tutorial example.  In practice, many more iterations are needed.
numIterations = 200

# Print a logging statement every 10 iterations
rdn.addListener(LoggingListener(10));

# The component RPT has already been trained so there is no separate
# training step for the RDN.

# -------------------------
# Apply the RDN
# -------------------------

print "Applying RDN..."
predictionMap = rdn.apply({rpt: testContainer}, numIterations)

# Write out the predictions.  Applying the RDN employs Gibbs sampling to
# jointly estimate the marginal probabilities for each of its
# component models (the single RPT in this case). 
rptPredictions = predictionMap.get(rpt)
rptPredictions.setTrueLabels(testContainer, classLabel);

# Write out the predictions
print "Writing predictions..."
rdnAttrName = "rdn_pagetype_prediction"
rptPredictions.savePredictions(objectAttrs, rdnAttrName)

# -------------------------
# Evaluate the RDN
# -------------------------

print "Computing accuracy..."
acc = (1 - rptPredictions.getZeroOneLoss())
print "Computing area under ROC curve..."
auc = rptPredictions.getAUC('Student')

# Print summary of evaluation results.
print "RDN results:"
print "  Accuracy:                       ", str(acc)
print "  Area under ROC curve (Student): ", str(auc)
