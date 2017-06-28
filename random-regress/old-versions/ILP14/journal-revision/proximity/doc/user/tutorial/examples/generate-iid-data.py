# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Generate synthetic i.i.d. data.

# I.i.d. data generation creates a set of 1d-star subgraphs each having a
# core object (S) and linked (peripheral) objects (T).  We give each
# instance in S a single, discrete attribute that's used as a class
# label.  We also add a number of discrete attributes to the objects
# in S and T.

# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# WARNING: erases the database; run on a new or test database!
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

from kdl.prox.datagen2.structure import SyntheticGraphIID
from kdl.prox.datagen2.attributes import AttributeGenerator
from kdl.prox.model2.rpt import RPT
from kdl.prox.util.stat import NormalDistribution
from java.io import File

# Define a helper function that reads RPT files
def loadRPT(modelFile):
    return RPT().load(modelFile)

def loadFile(fileName):
    return File(fileName)

# Data generation requires an empty database. Check to see if the
# database already contains data so we can warn users accordingly.

# Returns true if the top-level tables are defined and empty
isEmpty = DB.isProxTablesEmpty()

# If database is NOT empty, ask user whether to overwrite the
# existing data.

genData = 1
if (not isEmpty):
   prompt = "Database is not empty. Overwrite existing data?"
   genData = prox.getYesNoFromUser(prompt)

# Do nothing if the database is not empty and the user says "No"

if (not genData):
   print 'No data generated.'

# Continue if database is empty or if user says to overwrite
# existing data

else:
   print 'Clearing database'
   DB.clearDB()
   print 'Initializing database'
   DB.initEmptyDB()

   # ------------------------------------------------------------
   # Generate the graph structure
   # ------------------------------------------------------------

   # Specify a probability distribution over degree distributions for
   # the S objects.

   # We create graphs that contain two different degree distributions
   # for the S objects.  One half of the S objects have normally
   # distributed degrees with a mean of 2 and standard deviation
   # approaching zero. The second half have normally distributed
   # degrees with a mean of 5 and standard deviation of 1.

   degreeDistribs = [[0.5, NormalDistribution(2.0, 0.0000001)],
                     [0.5, NormalDistribution(5.0, 1.0)]]

   # Generate the i.i.d. graph structure.  The generated data will
   # include four S objects.

   print 'creating graph structure'
   SyntheticGraphIID(4, degreeDistribs)

   # ------------------------------------------------------------
   # Generate attribute values
   # ------------------------------------------------------------

   print 'generating i.i.d. attributes'

   sClassQuery = loadFile("iid-class-query.xml")
   sAttrQuery  = loadFile("iid-coreS-query.xml")
   tAttrQuery  = loadFile("iid-coreT-query.xml")
   sClassRPT   = loadRPT("s-class-rpt.xml")
   sAttrRPT    = loadRPT("s-attr-rpt.xml")
   tAttrRPT    = loadRPT("t-attr-rpt.xml")

   queriesAndModels = {sClassQuery: sClassRPT, sAttrQuery: sAttrRPT, tAttrQuery: tAttrRPT}

   # Specify the number of Gibbs sampling iterations to use in
   # conditioning the data

   iters = 3
   AttributeGenerator(queriesAndModels, iters)

