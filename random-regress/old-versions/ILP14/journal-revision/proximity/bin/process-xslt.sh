#!/bin/csh -f
# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# ********************************************************************
# process-doc.sh <stylesheet> <document> <processor> <output-file>
# For example: process-doc.sh list-papers.xsl rexa-test.xml saxon test.txt
#
# --------------------------------------------------------------------
# This script is used to process an XML document using the specified
# stylesheet.
#
# To use this script you must
#  - Set up the XSLT tools, including using catalog files for 
#    resolving paths to DTDs and stylesheets
#  - Define the directory locations below for your local installation

# --------------------------------------------------------------------
# Set the locations for source and code directories

set JAVA_LIBS="/usr/java"                        # Java libs
set SAXON_HOME="$JAVA_LIBS/saxon6_5_3"           # Saxon libs
set XALAN_HOME="$JAVA_LIBS/xalan-j_2_5_2"        # Xalan libs
set XERCES_HOME="$JAVA_LIBS/xerces-2_6_2"        # Xerces libs
set EXSLT_HOME="/usr/local/sgml/exslt"           # EXSLT extension functions
set FOP_HOME="$JAVA_LIBS/fop-0.20.5"             # FOP libs
set SGML_HOME="/usr/local/sgml"                  # Catalog location

set DOCBOOK_HOME="$SGML_HOME/docbook-xsl-1.69.1"
set PROX_HOME="$HOME/prox-work/proximity"

# Stylesheet location
set STYLESHEET_PATH="$PROX_HOME/src/xml/doc/stylesheets"

# ********************************************************************

# --------------------------------------------------------------------
# Set defaults

set DEBUG=""
set validate="0"
   
# --------------------------------------------------------------------
# Identify stylesheet and target document

if ($#argv != 4) then
   echo "Improper arguments"
   goto Usage
else
   set stylesheet = $1
   shift
   set docsrcfile = $1
   shift
   set xsltprocessor = $1
   shift
   set outfile = $1
endif

if ($DEBUG != "") then
  echo "stylesheet = $stylesheet"
  echo "document = $docsrcfile"
endif

set outputfile=$outfile

if ($DEBUG != "") then
  echo "outputfile = $outputfile"
endif

# --------------------------------------------------------------------
# Define paths to required libraries

# for some reason saxon653.jar doesn't work and we must use saxon651.jar

# pruned classpath
# includes Saxon extension functions
set CLASSPATH="$SAXON_HOME/saxon.jar:$XALAN_HOME/bin/xalan.jar:$XALAN_HOME/bin/xml-apis.jar:$XERCES_HOME/xercesImpl.jar:$XERCES_HOME/xml-apis.jar:$XERCES_HOME/xercesSamples.jar:$JAVA_LIBS/resolver-1.0.jar:$SGML_HOME"

# original classpath

#set CLASSPATH="$SAXON_HOME/saxon.jar:$DOCBOOK_XSL_HOME/extensions/saxon651.jar:$XALAN_HOME/bin/xalan.jar:$XALAN_HOME/bin/xml-apis.jar:$DOCBOOK_XSL_HOME/extensions/xalan2.jar:$XERCES_HOME/xercesImpl.jar:$XERCES_HOME/xml-apis.jar:$XERCES_HOME/xercesSamples.jar:$FOP_HOME/build/fop.jar:$FOP_HOME/lib/batik.jar:$FOP_HOME/lib/xalan-2.4.1.jar:$FOP_HOME/lib/JimiProClasses.jar:$FOP_HOME/lib/avalon-framework-cvs-20020806.jar:$JAVA_LIBS/resolver-1.0.jar:$SGML_HOME"

# classpath without libraries

#set CLASSPATH="$SAXON_HOME/saxon.jar:$DOCBOOK_XSL_HOME/extensions/saxon651.jar:$DOCBOOK_XSL_HOME/extensions/xalan2.jar:$XERCES_HOME/xercesImpl.jar:$XERCES_HOME/xml-apis.jar:$XERCES_HOME/xercesSamples.jar:$FOP_HOME/build/fop.jar:$FOP_HOME/lib/batik.jar:$FOP_HOME/lib/xalan-2.4.1.jar:$FOP_HOME/lib/JimiProClasses.jar:$FOP_HOME/lib/avalon-framework-cvs-20020806.jar:$JAVA_LIBS/resolver-1.0.jar:$SGML_HOME"

if ($DEBUG != "") then
  echo "CLASSPATH: $CLASSPATH"
  echo " "
endif

# --------------------------------------------------------------------
# Define some shortcuts for long commands

if ($DEBUG != "") then
  echo "confirming variable bindings:"
  echo "  docsrcfile = $docsrcfile"
  echo "  stylesheetpath = $STYLESHEET_PATH"
  echo "  stylesheet = $stylesheet"
  echo "  outputfile = $outputfile"
endif

# ---------------
# processdocsaxon
# ---------------

# Uses Xerces parser with Saxon XSLY processor

# Command line options
#  -x <classname>  -  Use specified SAX parser for source file
#  -y <classname>  -  Use specified SAX parser for stylesheet file
#  -r <classname>  -  Use the specified URIResolver to process all URIs
#  -u  -  Indicates that the names of the source document and the style
#         document are URLs
#  -w0 -  Indicates the policy for handling recoverable errors in the
#         stylesheet: w0 means recover silently
#  -o <filename>   -  Send output to named file

set processdocsaxon="java -Xmx1024M\
                 -classpath $CLASSPATH\
                 -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl\
                 -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl\
                 -Djavax.xml.transform.TransformerFactory=com.icl.saxon.TransformerFactoryImpl\
                 com.icl.saxon.StyleSheet\
                 -x org.apache.xml.resolver.tools.ResolvingXMLReader\
                 -y org.apache.xml.resolver.tools.ResolvingXMLReader\
                 -r org.apache.xml.resolver.tools.CatalogResolver\
                 -u -w0\
                 -o $outputfile\
                 $docsrcfile\
                 $STYLESHEET_PATH/$stylesheet"

# ---------------
# processdocxalan
# ---------------

# Uses Xalan (which uses the Xerces parser)

# Command line options:
#  -XML  -  Use XML formatter and add XML header
#  -URIRESOLVER <classname>  -  Use specified URI resolver
#  -IN <filename>            -  Source file
#  -XSL <stylesheet>         -  Stylesheet filename
#  -OUT <filename>           -  Output file

# Untested
set processdocxalan="java -Xmx1024M\
                 -classpath $CLASSPATH\
                 -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl\
                 -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl\
                 -Djavax.xml.transform.TransformerFactory=com.icl.saxon.TransformerFactoryImpl\
                 org.apache.xalan.xslt.Process\
                 -XML\
                 -URIRESOLVER org.apache.xml.resolver.tools.CatalogResolver\
                 -IN $docsrcfile\
                 -XSL $STYLESHEET_PATH/$stylesheet\
                 -OUT $outputfile"

# --------------------------------------------------------------------
# Process the document

if ($DEBUG == "") then
  if ($xsltprocessor == "saxon") then
     echo 'Using Saxon'
     eval $processdocsaxon
  else if ($xsltprocessor == "xalan") then
     echo 'Using Xalan'
     eval $processdocxalan
  else
     echo "No processor specified, using Saxon"
     eval $processdocsaxon
endif

# --------------------------------------------------------------------
# We're done!
exit 0

# --------------------------------------------------------------------
Help:
  echo 'Usage: process-doc.sh [stylesheet] [document] [processor]'
  echo "   processor can be 'saxon' or 'xalan'"
exit 1

Usage:
  echo 'Usage: process-doc [stylesheet] [document] [processor]'
  echo 'process-doc --h for more information'
exit 1

