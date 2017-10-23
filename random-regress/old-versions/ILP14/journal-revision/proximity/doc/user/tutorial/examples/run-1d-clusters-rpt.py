# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Import the necessary class definitions.
from kdl.prox.model2.common.sources import *
from kdl.prox.model2.rpt import RPT

# Make sample "0" the training set and sample "1" the test set.
trainContainer = prox.getContainer("1d-clusters/samples/0")
testContainer = prox.getContainer("1d-clusters/samples/1")

# ---------------------------------
# Define what we want to predict.

# The subgraph object whose label we want to predict
coreItemName = 'core_page'

# The name of the attribute we want to predict
attrToPredict = 'pagetype'

# The attribute we want to predict
# Format: item name, attribute name
classLabel = AttributeSource(coreItemName, attrToPredict)

# The set of attributes we'll use in learning the model.
# We can use any of the attributes found in the input subgraghs
# including attribute on the core item, links connecting to the
# core item, or objects linked to the core item (in general, this can
# be multiple "hops" away. The specific instantiation of the RPT model
# in this example does not use link attributes and only considers
# attributes within one hop of the core item)

# Limited attribute set:
inputSources = [ \
   AttributeSource('core_page', 'url_server_info'), \
   AttributeSource ('core_page', 'url_hierarchy1b'), \
   AttributeSource('linked_from_page', 'page_num_outlinks'), \
   AttributeSource('linked_to_page', 'page_num_inlinks'), \
   ItemSource("linked_from_page"), \
   ItemSource("linked_to_page")]

# ---------------------------------
# Begin modeling portion of script

print "Beginning modeling section"

# Instantiate the model.
print "Instantiating model..."
rpt = RPT()
rpt.learningModule.stoppingModule.setMaxDepth(3)

# Train (learn) the tree.
print "Learning model..."
rpt.learn(trainContainer, classLabel, inputSources)

# Output XML.
xmlFileName = 'ProxWebKB_RPT.xml'
rpt.save(xmlFileName)
print "RPT written to ", xmlFileName

# Apply the model to the test set.
print "Applying model..."
predictions = rpt.apply(testContainer)
predictions.setTrueLabels(testContainer, classLabel)

# Save the predictions as an attribute
print "Writing predictions..."
rptAttrName = "rpt_pagetype_prediction"
predictions.savePredictions(testContainer.getSubgraphAttrs(), rptAttrName)

# Evaluate the model.
print "Computing accuracy (ACC)..." 
acc = (1 - predictions.getZeroOneLoss())
print "Computing area under ROC curve (AUC)..."
auc = predictions.getAUC("Student")
print "Computing conditional likelihood (CLL)..."
cll = predictions.getConditionalLogLikelihood()

# Print summary of all results.
print "RPT results:"
print "  ACC: ", str(acc)
print "  AUC: ", str(auc)
print "  CLL: ", str(cll)


