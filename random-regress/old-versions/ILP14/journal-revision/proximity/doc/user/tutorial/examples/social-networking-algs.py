# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Define a prefix that we'll use for attribute and container names.
# For example, you might use the database name as a prefix when
# running these algorithms on the whole database (as shown below), or
# use a container name when running the algorithms on the objects and
# links in a specified container.

attrPrefix = "proxwebkb"

# Get the curent set of object attributes. We need this to test to see
# if the new attributes we're creating already exists.

currentAttrs = prox.objectAttrs

# ================================================================
# CLUSTERING COEFFICIENT

# Creates a new attribute that contains the value of the clustering
# coefficient.  An object must have at least two neighbors to
# calculate the clustering coefficient. The value is not calculated
# (and no attribute created) for objects having less than two neighbors.

print "Computing clustering coefficients"

# The name of the new attribute
clusterAttrName = attrPrefix + "-cluster-coeff"

# Delete this attribute if it already exists
if currentAttrs.isAttributeDefined(clusterAttrName):
   print "  Attribute " + clusterAttrName + " already exists. Deleting..."
   currentAttrs.deleteAttribute(clusterAttrName)

# Create the new clustering coefficient attribute
prox.addClusterCoeffAttribute(clusterAttrName)

# ================================================================
# CONNECTED COMPONENTS

print "Computing connected components"

# The name of the new container. Each subgraph in this container is
# connected component. The container will be a child of the root
# container.
outputContainerName = attrPrefix + "-connected-components"

# Delete this container if it already exists
rootContainer = prox.rootContainer
if rootContainer.hasChild(outputContainerName):
   print "  Container " + outputContainerName + " already exists. Deleting..."
   rootContainer.deleteChild(outputContainerName)

# Add the new cluster coefficient attribute
prox.computeConnectedComponents(outputContainerName)

# ================================================================
# HUBS AND AUTHORITIES

print "Computing hubs and authorities"

# The name of the new hubs attribute
hubsAttrName = attrPrefix + "-hubs"

# The name of the new authorities attribute
authoritiesAttrName = attrPrefix + "-authorities"

# Delete these attributes if they already exists
if currentAttrs.isAttributeDefined(hubsAttrName):
   print "  Attribute " + hubsAttrName + " already exists. Deleting..."
   currentAttrs.deleteAttribute(hubsAttrName)
if currentAttrs.isAttributeDefined(authoritiesAttrName):
   print "  Attribute " + authoritiesAttrName + " already exists. Deleting..."
   currentAttrs.deleteAttribute(authoritiesAttrName)

# Number of iterations. The paper on which the implementation is based
# suggests 20 as sufficient for most applications.
numIterations = 20

# Create the new attributes
prox.addHubsAndAuthoritiesAttributes(numIterations, hubsAttrName,
     authoritiesAttrName)
