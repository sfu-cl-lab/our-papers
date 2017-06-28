# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Create samples from a known, existing container of subgraphs.

# Create two samples. Each will be placed in a new container and each
# will hold approximately one-half the subgraphs from the original
# container.

print "Sampling database..."
prox.sampleContainer("1d-clusters",2,"samples")
