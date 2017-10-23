#!/bin/csh -f

# -------------------------------------------------------------------
# GENGLOSS.SH
#
# Creates a temporary file, GlossaryStub.xml, that contains a 
# <glossterm> element for each term in the master glossary file
# (ProximityGlossary.xml).  Then processes that file using the
# glossary-stubs.xsl stylesheet to produce the PDF or HTML file
# containing all the glossary definitions.
#
# Run from $PROX_HOME:
#  > cd $PROX_HOME
#  > bin/gengloss.sh [pdf|html]
# -------------------------------------------------------------------

# The relative path from the generated GlossaryStub file to the master
# glossary "database" file must be the same as that used by the other
# documents (Tutorial, QGraph Guide).  We put the generated file in 
# a "temp" directory as we re-generate the file each time.

set PX_HOME = "$PROX_HOME"
set GLOSS_HOME = "$PX_HOME/src/xml/doc"
set GLOSS_DEST = "$PX_HOME/src/xml/doc/user/temp"

# If the temp directory doesn't exist, create it
if (! -d "$GLOSS_DEST") then
  echo "Creating temp directory for stub file"
  mkdir $GLOSS_DEST
endif

# First (and only) argument specifies output format

if ($#argv == 1) then
   set docformat=$1
else
   goto Usage
endif

# Create the GlossaryStub.xml file
# We do this by processing the master glossary file using a XSLT stylesheet
# glossary-stubs.xml, which creates the 'stub' file containing all the
# glossary terms.

# Parameters:
#   glossary-stubs.xsl     - XSLT stylefile
#   ProximityGlossary.xml  - master glossary file (definitions 'database')
#   saxon                  - XSLT processor
#   GlossaryStub.xml       - generated file; used by genproxdoc.sh

echo "Generating stub file..."
bin/process-xslt.sh glossary-stubs.xsl $GLOSS_HOME/ProximityGlossary.xml saxon $GLOSS_DEST/GlossaryStub.xml

# Generate the PDF
# This process uses the stub file created above as the base document.

echo "*****************************************************************"
echo "Reminder: Make sure that glossary parameters in prox-common.xsl"
echo "are not commented out"
echo "*****************************************************************"

bin/genproxdoc.sh gloss --$1

echo "Done"
echo "Generated glossary is in $GLOSS_DEST"
exit 0

Usage:
  echo 'Usage: gengloss.sh [format]'
  echo '  where format is one of "pdf" or "html"'
exit 1

