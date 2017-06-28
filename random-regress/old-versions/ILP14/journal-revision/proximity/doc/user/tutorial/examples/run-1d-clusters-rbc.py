# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Import the necessary class definitions.
from kdl.prox.model2.common.sources import *
from kdl.prox.model2.rbc import RBC

# Make sample "0" the training set and sample "1" the test set.
trainContainer = prox.getContainer("1d-clusters/samples/0")
testContainer = prox.getContainer("1d-clusters/samples/1")

# ---------------------------------
# Define what we want to predict.

# The subgraph object whose label we want to predict
coreItemName = 'core_page'

# The name of the attribute we want to predict
attrToPredict = 'isStudent'

# The attribute we want to predict.
# Format: item name, 
#         attribute name,
#         0/1=core item is link/object,
#         0/1=attribute is not continuous/continuous

classLabel = AttributeSource(coreItemName, attrToPredict)

# The set of features we'll use in learning the model.
# Features are attributes of the core item, links connecting to the
# core item, or objects linked to the core item (in general, this can
# be multiple "hops" away; for this model, we're only interested in
# attributes within one hop of the core item)
# Format: item name,
#         attribute name, 

inputSources = [ \
   AttributeSource('core_page', 'url_server_info'), \
   AttributeSource('core_page', 'url_hierarchy1b'), \
   AttributeSource('linked_from_page', 'url_server_info'), \
   AttributeSource('linked_from_page', 'url_hierarchy1b'), \
   AttributeSource('linked_from_page', 'page_num_outlinks'), \
   AttributeSource('linked_from_page', 'page_num_inlinks'), \
   AttributeSource('linked_to_page', 'url_server_info'), \
   AttributeSource('linked_to_page', 'url_hierarchy1b'), \
   AttributeSource('linked_to_page', 'page_num_outlinks'), \
   AttributeSource('linked_to_page', 'page_num_inlinks'), \
   AttributeSource('linked_to', 'link_tag'), \
   AttributeSource('linked_from', 'link_tag')]

# ---------------------------------
# Begin modeling portion of script

print "Beginning modeling section"

# Instantiate the model.
print "Instantiating model..."
rbc = RBC()

# Train (learn) the model.
print "Learning model..."
rbc.learn(trainContainer, classLabel, inputSources)

# Output XML.
xmlFileName = 'ProxWebKB_RBC.xml'
rbc.save(xmlFileName)
print "RBC written to ", xmlFileName

# Apply the model to the test set.
print "Applying model..."
predictions = rbc.apply(testContainer)
predictions.setTrueLabels(testContainer, classLabel);

# Save the predictions as an attribute
print "Writing predictions..."
rbcAttrName = "rbc_isStudent_prediction"
predictions.savePredictions(testContainer.getSubgraphAttrs(), rbcAttrName)


# Evaluate the model.
print "Computing accuracy (ACC)..." 
acc = (1 - predictions.getZeroOneLoss())
print "Computing area under ROC curve (AUC)..."
auc = predictions.getAUC("1")
print "Computing conditional likelihood (CLL)..."
cll = predictions.getConditionalLogLikelihood()

# Print summary of all results.
print "RBC results:"
print "  ACC: ", str(acc)
print "  AUC: ", str(auc)
print "  CLL: ", str(cll)
