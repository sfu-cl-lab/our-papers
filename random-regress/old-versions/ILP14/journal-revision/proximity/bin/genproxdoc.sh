#!/bin/csh -f
# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# ********************************************************************
# This is the main document generation script for Proximity.  Use this
# script to
#  - generate HTML and PDF versions of the Proximity Tutorial, QGraph
#    Guide, Cookbook, individual 'recipes', individual technical 
#    description documents, and test documents
#  - validate the XML for any of these documents
#  - generate 'review' copies of these documents that include index
#    terms, glossary terms, and image filenames in the formatted
#    document
#  - generate a list of all image files used in the document (supresses
#    document generation)
#
# To use this script you must
#  - Set up the DocBook tools
#      - XSLT processor (used for processing files):  Saxon 6.5.3
#      - XML processor (ussed for validation):  Xalan
#      - XML parser:  Xerces
#      - catalog files for resolving paths to DTDs and stylesheets
#  - Install and configure related tools and libraries
#      - FOP (0.20.5)
#  - Define the directory locations below for your local installation
#
# See also related scripts that call this script:
#    deploydocs.sh        Create all doc sets for a release
#    gengloss.sh          Generate draft of the master glossary
#
# --------------------------------------------------------------------
# Command line syntax:
# --------------------------------------------------------------------
#
#    proxgendoc [options] <target> [module] <format> [destination]
#
# where parameters in <> are required and those in [] are optional.
# (Module names are required when generating a modular document; see below.)
#
# Examples:
#    Generate the HTML version of the Tutorial to be posted on the
#    KDL public web site:
#       proxgendoc.sh tutorial --html web
#
#    Print steps but do not generate the PDF version of the QGraph Guide
#    to be included in the Proximity distribution:
#       proxgendoc.sh --d td TD_QGraph --pdf
#
# --------------------------------------------------------------------
# Options
# --------------------------------------------------------------------
# Multiple options are supported.  The need for profiling is usually
# determined by the target document; the options below let you override
# defaults for a given document.
#
#    --help or --h       print usage instructions
#    --debug or --d      run in debug mode (no document generation)
#    --clean or --c      delete old files before generating
#    --force or --f      generate PDF using existing FO file
#    --justclean or --j  just clean directories; do not generate docs
#    --noprofile or --n  do not make a profiling pass
#    --profile or --p    make a profiling pass
#    --review or --r     "review" mode (print index and glossary terms
#                           and image file names)
#    --imagelist         print list of image files used in document
#                          (must edit prox-fo.xsl; does not process
#                          document normally)
#
# --------------------------------------------------------------------
# Targets
# --------------------------------------------------------------------
# The following list shows the documents that can be generated using
# this script.  See also the list of modular documents, below.
#
#    tutorial            Proximity Tutorial
#    qg                  Proximity Qgraph Guide
#    cookbook            Proximity Cookbook (all recipes)
#    test                Test document (must be named Test.xml and be
#                          located in $PROX_HOME/src/xml/doc/user/test)
#    gloss               Working/draft copy of the Proximity glossary;
#                          used by gengloss.sh script; not to be used
#                          from the command line with genproxdoc.sh
#
# --------------------------------------------------------------------
# Modular document targets
# --------------------------------------------------------------------
# Some documents can be generated modularly.  In these cases, you must
# specify the module as well, as shown below.  The module name is the
# filename for that module without its extension.
#
#    recipe              An individual recipe
#    td                  A technical description document
#
# Note that there is no option to generate the complete set of
# technical descriptions; you must generate each module individually.
#
# --------------------------------------------------------------------
# Formats
# --------------------------------------------------------------------
#    --html              generate HTML
#    --pdf               generate PDF (includes FO generation)
#    --fo                generate FO only (no PDF)
#    --validate or --v   validate only; no other processing
#
# --------------------------------------------------------------------
# Destinations
# --------------------------------------------------------------------
#    distrib             included in release tarball (default; relative links)
#    web                 posted on KDL public web pages (relative links)
#    kdl                 on internal KDL web pages (relative links)
#
# --------------------------------------------------------------------
# XML validation
# --------------------------------------------------------------------
# You do not need to specify a destination when validating a document:
#
#       proxgendoc.sh [options] <target> --v
#
# ********************************************************************

# --------------------------------------------------------------------
# Set the locations for source and code directories

set PROX_HOME="/Users/loiselle/work/proximity" # Prox3 home dir
set JAVA_LIBS="/usr/java"                   # Java libs
set SAXON_HOME="$JAVA_LIBS/saxon6_5_3"      # Saxon libs
#set XALAN_HOME="$JAVA_LIBS/xalan-j_2_5_2"   # Xalan libs
set XERCES_HOME="$JAVA_LIBS/xerces-2_9_0"   # Xerces libs (newer version)
set FOP_HOME="$JAVA_LIBS/fop-0.20.5"        # FOP 0.20.5 libs
#set FOP_HOME="$JAVA_LIBS/fop-0.93"          # FOP 0.93 libs
set SGML_HOME="/usr/local/sgml"             # Catalog location
set DOCBOOK_XSL_HOME="$SGML_HOME/docbook-xsl-1.71.1" # DocBook XSL location

# --------------------------------------------------------------------
# Set global variables and default values

set nonomatch                     # used for DEBUG flag
set DEBUG=""                      # echos cmds instead of executing
set profiledresult="0"            # records errors in profiling
set genfo="0"                     # records errors in FO generation
set gendoc="0"                    # records errors in PDF/HTML generation

set deleteoldfiles="0"            # delete previously generated files?
set cleanonly="0"                 # only clean, do not generate docs?
set useexistingfo="0"             # re-use existing .fo file?
set reviewmode="0"                # generate working draft?
set imagelistmode="0"             # generate list of image files?

set doctarget=""                  # e.g., tutorial, qg
set docdirname=""                 # directory that contains doc files
set docsrcfile=""                 # base file for the target
set htmlgendir="HTML-tmp/"        # Temporary directory to hold generated
                                  #   HTML; files moved after generation
set docformat=""                  # HTML or PDF?
set docdestination="distrib"      # Version (destination) of generated
                                  #   docs; note default
set profiled="0"                  # default to not use profiling
set validate="0"                  # default to not validate
set overrideprofile="0"           # did user specify value on cmd line?
set notimplcase=""                # anticipated functionality that is
                                  #   not yet implemented

set srcpath="$PROX_HOME/src/xml/doc"     # base path to doc source files
set htmlparams="base.dir=$htmlgendir"    # provide default for command-line
                                         #   params for HTML generation

# Following doesn't seem to work if set here; need to set in prox-fo.xsl
set foparams='draft.mode="no"'    # additional params for FO generation
                                  # that may vary with target (see also
                                  # tocparams, below)

# The generate.toc parameter controls which document components get a
# table of contents, list of figures, etc.  Set the value of tocparams
# to the default value for generate.toc specified in the FO stylesheet.
# (The value set in this script overrides the value fro the stylesheet.)
# This lets us change the value for any documents that don't use the
# default values.

set tocparams="book toc,title,procedure article nop"

set proxcss="prox.css"            # Name of CSS style sheet for HTML docs

# --------------------------------------------------------------------
# Identify options.  (Make sure any new options use a unique initial
# letter.)

while ($1 =~ -*)
   if (($1 =~ "--h") || ($1 =~ "--help")) then
      goto Help
   else if (($1 =~ "--d") || ($1 =~ "--debug")) then
      set DEBUG="echo"
      echo "Debugging run - no document generation"
      shift
      continue

   else if (($1 =~ "--c") || ($1 =~ "--clean")) then
      set deleteoldfiles="1"
      set cleanonly="0"
      echo "Deleting previously generated files"
      shift
      continue
   else if (($1 =~ "--j") || ($1 =~ "--justclean")) then
      set deleteoldfiles="1"
      set cleanonly="1"
      echo "Deleting previously generated files"
      shift
      continue
   else if (($1 =~ "--f") || ($1 =~ "--force")) then
      set useexistingfo="1"
      echo "Using existing FO file"
      shift
      continue
   # The following two options let you override the default behavior
   # for the specified document
   else if (($1 =~ "--n") || ($1 =~ "--noprofile")) then
      if ($overrideprofile == 1) then
         echo "Error: Attempt to set profiling status twice"
         goto Usage
      endif
      set overrideprofile="1"
      set profiled="0"
      echo "(Not profiled)"
      shift
      continue
   else if (($1 =~ "--p") || ($1 =~ "--profile")) then
      if ($overrideprofile == 1) then
         echo "Error: Attempt to set profiling status twice"
         goto Usage
      endif
      set profiled="1"
      set overrideprofile="1"
      echo "(Profiled)"
      shift
      continue
   else if (($1 =~ "--r") || ($1 =~ "--review")) then
      set reviewmode="1"
      echo "Generating review copy"
      shift
      continue
   else if ($1 =~ "--imagelist") then
      set imagelistmode="1"
      echo "Generating image file list"
      echo "************************************************************"
      echo "Did you uncomment the template in prox-fo.xsl?"
      echo "Reminder: Printing the image list prevents normal processing"
      echo "************************************************************"
      shift
      continue

   else
      echo "Unknown option: $1"
      goto Usage
   endif
end

if ($DEBUG != "") then
    echo "Parsed switches"
endif
   
# --------------------------------------------------------------------
# Identify target document

# After parsing options, we should see the following arguments
# remaining:
#   <target> [module] <format> [destination]
# Because some of the options require specifying an individual module
# in addition to the target document, we have to finish parsing this
# argument (possibly a pair of arguments) before continuing to parse the 
# remaining arguments.
#
# The <target> identifier is next, and must not start with "-"

if (($#argv < 2) || ($1 =~ -*)) then
   echo "Improper arguments"
   goto Usage
else
   set doctarget = $1
   if ($DEBUG != "") then
      echo "Parsed doc target:" $doctarget
   endif
   shift
endif

# Once we establish the target, set the name of the root XML file
# and the directory containing the source file(s).  Some targets
# require additional command line parameters, also set here.

switch ($doctarget)
  case "tutorial":
    set profiled="1"
    set basedoc="Tutorial"
    set docdirname="tutorial"
    set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
    echo "Generating Proximity Tutorial"
    breaksw

  case "qg":
    set profiled="1"
    set basedoc="QGraphGuide"
    set docdirname="qgraph"
    set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
    echo "Generating Proximity QGraph Guide"
    breaksw

  case "cookbook":
    set profiled="1"
    set basedoc="ProxCookbook"
    set docdirname="cookbook"
    set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
    set htmlparams = "base.dir=$htmlgendir chunk.first.sections=1 chunk.section.depth=1 generate.section.toc.level=1 toc.section.depth=1 chunk.tocs.and.lots=0 section.autolabel=1 section.autolabel.max.depth=1 section.label.includes.component.label=1 navig.showtitles=1"
    set tocparams = "book toc,title chapter toc"
    set foparams='draft.mode="no"'
    echo "Generating Proximity Cookbook"
    breaksw

  case "recipe":
    set basedoc=$1
    set docdirname="cookbook"
    shift
    set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
    set htmlparams = "base.dir=$htmlgendir use.id.as.filename=1 chunk.section.depth=0 generate.section.toc.level=0"
    echo "Generating individual recipe"
    breaksw

# removed support for target 08/2006; no longer included in doc set
#  case "p2xml":
#    set basedoc="Prox2XMLFormat"
#    set docsrcpath="$PROX_HOME/src/xml/doc/user/historic"
#    echo "Generating Proximity 2 XML Data Format documentation"
#    breaksw

# Modular documents require an additional argument that specifies the
# target module to generate.

  case "td":
    # We don't have a wrapper file for TDs; each is its own, stand-alone
    # document, so it becomes the basedoc.
    if (($#argv < 2) || ($1 =~ -*)) then
       echo "Improper arguments"
       goto Usage
    else
       set basedoc = $1
       set docdirname="tech-desc"
       shift
       set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
       set htmlparams = "base.dir=$htmlgendir use.id.as.filename=1 chunk.section.depth=0 navig.showtitles="0""
       echo "Generating technical description: " $basedoc
       breaksw
    endif

# Provide a way to test new documents without having to explicitly
# add them to the script as a new document type.  Set profiling on the
# command line.

  case "test":
    set basedoc="Test"
    set docdirname="test"
    set docsrcpath="$PROX_HOME/src/xml/doc/user/$docdirname"
    # Uses input opton, if provided
    set profiled=$profiled
    echo "Generating test document"
    breaksw

# The following documents are generated from other scripts that 
# call this script:

  case "gloss":
    set basedoc="GlossaryStub"    # generated file, see gengloss.sh
    set docdirname="test"         # glossary goes in 'test' dir
    set docsrcpath="$PROX_HOME/src/xml/doc/user/temp"
    set foparams = 'draft.mode="yes" double.sided="0"'
    echo "Generating Proximity Glossary"
    breaksw

  default:
    echo "Unknown document"
    goto Usage
endsw

if ($DEBUG != "") then
    echo "Profiled = $profiled"
    echo "Parsed document identifier:" $basedoc
    echo "Special parameters:"
    echo "   HTML: $htmlparams"
    echo "   PDF: $foparams"
endif

# --------------------------------------------------------------------
# Identify output format and destination

# After determining the target document, we should see the following,
# required and optional arguments remaining:
#   <format>  [destination]
# The <format> identifier is next, and must start with "--"
# Validation ignores destination and defaults to 'distrib' for other
# documents.

# The output format determines the appropriate XSLT customization style
# file.  Although we avoid creating specialized style files as much as
# possible, the format of technical descriptions is different enough from
# other docs to require a separate style file.

while ($#argv >= 1)
  if ($1 !~ --*) break
  if (($1 == "--validate") || ($1 == "--v")) then
     set docformat="Validate"
     set profiled="0"
     shift

  else if ($1 == "--html") then
     set docformat="HTML"
     shift
     if ($#argv >= 1) then
        set docdestination=$1
     endif
     # If technical description, use separate style file
     if ($doctarget == "td") then
        set stylesheet="prox-td-html.xsl"
     else
        set stylesheet="prox-html.xsl"
     endif

  else if ($1 == "--pdf") then
     set docformat="PDF"
     shift
     if ($#argv >= 1) then
        set docdestination=$1
     endif
     # If technical description, use separate style file
     if ($doctarget == "td") then
        set stylesheet="prox-td-fo.xsl"
     else
        set stylesheet="prox-fo.xsl"
     endif

  else if ($1 == "--fo") then
     set docformat="FO"
     shift
     if ($#argv >= 1) then
        set docdestination=$1
     endif
     set stylesheet="prox-fo.xsl"

  else
     echo "Unknown format"
     goto Usage
  endif
end

# --------------------------------------------------------------------
# Make sure we specified a legal docformat

if (($docformat == "") && ($validate == "0")) then
   echo "You must specify an output format or validate the document"
   goto Usage
endif

# Define filenames

set docsrcfile = $basedoc.xml

if ($profiled == "1") then
  set profiledxml = $basedoc.$docdestination.xml
  set docfo = $basedoc.$docdestination.fo
  set docpdf = $basedoc.$docdestination.pdf
  set docpdfname = $basedoc.pdf
else
  set docfo = $basedoc.fo
  set docpdf = $basedoc.pdf
endif

# Print current settings
echo "Output format: $docformat ($docdestination)"

# Print more info if debugging
if ($DEBUG != "") then
    echo "docfo: $docfo"
    echo "docsrcfile: $docsrcfile"
    echo "docpdf: $docpdf"
endif

# --------------------------------------------------------------------
# Define the directories that will hold the generated files.

set testtopdir = ""
set subdir = ""

# Doc test directories for each destination
set distribtestdir = "$PROX_HOME/doc-test/distrib/doc/user"
set webtestdir = "$PROX_HOME/doc-test/web/documentation"
set kdltestdir = "$PROX_HOME/doc-test/kdl/local-docs"

# Some destinations use an 'HTML' subdirectory for HTML files;
# others place HTML files at the same level as PDF files.

switch ($docdestination)
  case "distrib":
     set testtopdir = $distribtestdir
     set subdir = "HTML"
     breaksw
  case "web":
     set testtopdir = $webtestdir
     set subdir = ""
     breaksw
  case "kdl":
     set testtopdir = $kdltestdir
     set subdir = "HTML"
     breaksw
endsw

if ($docformat == "HTML") then
   set testdir=$testtopdir/$docdirname/$subdir
else
   set testdir=$testtopdir/$docdirname
endif

if ($DEBUG != "") then
    echo "testtopdir: $testtopdir"
    echo "subdir: $subdir"
    echo "distribtestdir: $distribtestdir"
    echo "webtestdir: $webtestdir"
    echo "kdltestdir: $kdltestdir"
    echo "testdir: $testdir"
endif

# Create the directory if it doesn't exist
if (! -d $testdir) then
   mkdir $testdir
endif

# --------------------------------------------------------------------
# Delete previous set of generated files, if requested.  This deletes
# both the files in the doc-test area as well as any generated files
# in the source tree.  Note that only the generated files for the
# specified format and distribution are cleaned.

if (($deleteoldfiles == 1) && (-d $testdir)) then
   echo "Deleting generated files"
   if ($docformat == "PDF") then
      # List of (potential) files to be deleted
      set deletelist = ($docsrcpath/$docpdf\
                        $docsrcpath/$docfo\
                        $testdir/$docpdf)
      foreach file ($deletelist)
        if (-e $file) then
           $DEBUG rm $file
        endif
      end
      # profiledxml is only defined if profiling
      if ($profiled == 1) then
         #DEBUG rm $docsrcpath/$profiledxml
      endif

   else if ($docformat == "HTML") then
      # Delete generated files in src tree
      $DEBUG rm $docsrcpath/$htmlgendir/*.html
      if ($profiled == 1) then
        $DEBUG rm $docsrcpath/$profiledxml
      endif
      # Delete generated and copied files in doc-test tree
      $DEBUG rm -rf $testdir/*
   else
      echo "Unable to delete existing $docformat documents"
   endif

   if ($cleanonly == 1) then
      goto Done
   endif
endif

# --------------------------------------------------------------------
# Define paths to required libraries
# (Note that CLASSPATH is redefined here and does not use values from
# .bash_profile)

# Classpath for FOP 0.20.5
set FOP_CLASSPATH=$FOP_HOME/build/fop.jar:$FOP_HOME/lib/batik.jar:$FOP_HOME/lib/xalan-2.4.1.jar:$JAVA_LIBS/jimi/JimiProClasses.jar:$FOP_HOME/lib/avalon-framework-cvs-20020806.jar

# Classpath for FOP 0.93
#set FOP_CLASSPATH=$FOP_HOME/build/fop.jar:$FOP_HOME/lib/avalon-framework-4.2.0.jar:$FOP_HOME/lib/batik-all-1.6.jar:$FOP_HOME/lib/commons-io-1.1.jar:$FOP_HOME/lib/commons-logging-1.0.4.jar:$FOP_HOME/lib/serializer-2.7.0.jar:$FOP_HOME/lib/xalan-2.7.0.jar:$FOP_HOME/lib/xercesImpl-2.7.1.jar:$FOP_HOME/lib/xml-apis.1.3.02.jar:$FOP_HOME/lib/xmlgraphics-commons-1.1.jar:$FOP_HOME/lib/fop-hyph.jar:$JAVA_LIBS/jimi/JimiProClasses.jar

# Classpath for validation:
set VALCLASSPATH="$XERCES_HOME/xercesSamples.jar:$XERCES_HOME/xercesImpl.jar"

# Classpath for XSLT processing:
set CLASSPATH="{$FOP_CLASSPATH}:$SAXON_HOME/saxon.jar:$DOCBOOK_XSL_HOME/extensions/saxon65.jar:$XERCES_HOME/xercesImpl.jar:$JAVA_LIBS/resolver-1.0.jar:$SGML_HOME"

if ($DEBUG != "") then
    echo "CLASSPATH: $CLASSPATH"
endif

# --------------------------------------------------------------------
# Define some shortcuts for long commands used more than once
#
# Parameters
#   See also the discussion of these tools in Bob Stayton's excellent
# "DocBook XSL: The Complete Guide" (available at
# http://www.sagehill.net/docbookxsl/index.html); this document is
# essential for setting up a DocBook system.
#   
# Java parameters
#
#   Use the Xerces parser instead of the built-in Saxon parser:
#
#      -Dorg.apache.xerces.xni.parser.XMLParserConfiguration=
#         org.apache.xerces.parsers.XIncludeParserConfiguration
#      -Djavax.xml.transform.TransformerFactory=
#         com.icl.saxon.TransformerFactoryImpl
#
#   Enable Xinclude processing
#
#      -Dorg.apache.xerces.xni.parser.XMLParserConfiguration=
#         org.apache.xerces.parsers.XIncludeParserConfiguration
#
# Parameters used by the Saxon XSLT processor
#
# Command line options
#  -x <classname>  -  Use specified SAX parser for source file
#  -y <classname>  -  Use specified SAX parser for stylesheet file
#  -r <classname>  -  Use the specified URIResolver to process all URIs
#  -u  -  Indicates that the names of the source document and the style
#         document are URLs
#  -w0 -  Indicates the policy for handling recoverable errors in the
#         stylesheet: w0 means recover silently
#  -o <filename>   -  Send output to named file
#
# Parameters used by XSLT Proximity customization stylesheets
#
#   prox.docname        tutorial, qgraph, cookbook, etc.  Used for
#                       special handling required by specific documents,
#                       e..g, the cookbook inserts pagebreaks before 
#                       level 1 sections
#   prox.destination    distrib, web, or kdl
#   prox.review         1 if generating a review copy, 0 otherwise
#   prox.imagelist      1 if generating the imagelist, 0 otherwise
#

# -------------------
# generateprofiledxml
# -------------------

set generateprofiledxml='java\
       -classpath $CLASSPATH\
       -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl\
       -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl\
       -Dorg.apache.xerces.xni.parser.XMLParserConfiguration=org.apache.xerces.parsers.XIncludeParserConfiguration\
       com.icl.saxon.StyleSheet\
       -x org.apache.xml.resolver.tools.ResolvingXMLReader\
       -y org.apache.xml.resolver.tools.ResolvingXMLReader\
       -r org.apache.xml.resolver.tools.CatalogResolver\
       -u -w0\
       -o $profiledxml\
       $docsrcfile\
       $DOCBOOK_XSL_HOME/profiling/profile.xsl\
       profile.condition="$docdestination"\
       prox.docname="$docdirname"\
       prox.destination="$docdestination"'


# ------------
# generatehtml
# ------------

set generatehtml='java\
       -Xmx512M\
       -classpath $CLASSPATH\
       -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl\
       -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl\
       -Dorg.apache.xerces.xni.parser.XMLParserConfiguration=org.apache.xerces.parsers.XIncludeParserConfiguration\
       com.icl.saxon.StyleSheet\
       -x org.apache.xml.resolver.tools.ResolvingXMLReader\
       -y org.apache.xml.resolver.tools.ResolvingXMLReader\
       -r org.apache.xml.resolver.tools.CatalogResolver\
       -u -w0\
       $docsrcfile\
       $srcpath/stylesheets/$stylesheet\
       $htmlparams\
       prox.destination="$docdestination"'

# ----------
# generatefo
# ----------

set generatefo='java\
       -classpath $CLASSPATH\
       -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl\
       -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl\
       -Dorg.apache.xerces.xni.parser.XMLParserConfiguration=org.apache.xerces.parsers.XIncludeParserConfiguration\
       com.icl.saxon.StyleSheet\
       -x org.apache.xml.resolver.tools.ResolvingXMLReader\
       -y org.apache.xml.resolver.tools.ResolvingXMLReader\
       -r org.apache.xml.resolver.tools.CatalogResolver\
       -u -w0\
       -o $docfo\
       $docsrcfile\
       $srcpath/stylesheets/$stylesheet\
       $foparams\
       generate.toc="$tocparams"\
       prox.destination="$docdestination"\
       prox.imagelist=$imagelistmode\
       prox.docname="$docdirname"\
       prox.review=$reviewmode'

# -----------
# generatepdf
# -----------

# FOP 0.20.5
set generatepdf='java\
      -Xmx512M\
      -classpath $CLASSPATH\
      org.apache.fop.apps.Fop\
      -fo $docfo\
      -pdf $docpdf'

# Left in for testing new versions in the future
# FOP 0.93 (Java command)
#set generatepdf='java\
#     -Xmx512M\
#     -classpath $CLASSPATH\
#     org.apache.fop.cli.Main\
#     -fo $docfo\
#     -pdf $docpdf'

# -----------
# validatexml
# -----------

set validatexml='java\
       -classpath $VALCLASSPATH\
       sax.Counter\
       -v $docsrcfile'

# --------------------------------------------------------------------
# Profiling pass

# Change to directory containing source files
cd $docsrcpath
if ($DEBUG != "") then
   echo "Changing to $docsrcpath directory"
endif

# Make profiling pass, if needed
if (($profiled == "1") && ($useexistingfo != "1")) then
   echo "Profiling pass..."
   $DEBUG eval $generateprofiledxml
   set profiledresult = $status
   set docsrcfile = $profiledxml
endif

# --------------------------------------------------------------------
# Generate document

switch ($docformat)
  case "HTML":
    echo "Generating HTML..."
    $DEBUG eval $generatehtml
    set gendoc = $status
    breaksw
  case "FO":
    echo "Generating $docfo..."
    $DEBUG eval $generatefo
    set genfo = $status
    breaksw
  case "PDF"
    if ((! -f $docfo) || ($useexistingfo == 0)) then
      echo "Generating $docfo..."
      $DEBUG eval $generatefo
      set genfo = $status
    endif
    echo "Generating $docpdf..."
    echo "Using FO file $docfo"
    $DEBUG eval $generatepdf
    set gendoc = $status
    breaksw
  case "Validate"
    echo "Validating XML..."
    $DEBUG eval $validatexml
    # If we're only validating, we're done
    goto Done
endsw

# --------------------------------------------------------------------
# Move the generated file(s) to the proper local doc-test directory. 
# The $PROX_HOME/doc-test directory provides the same file hierarchy
# as used for the final destination for these files, enabling easier
# testing of links between documents, pages, and javadoc pages.

if ($docformat != "Validate") then
   echo "Moving generated files to $docdestination test directory"
   if ($docformat == 'HTML') then
      $DEBUG cp -R $htmlgendir/* $testdir

      # If needed, create symlink to the source 'images' directory
      if ((! -e "$testdir/images") && (-e "$docsrcpath/images")) then
         echo "Creating symbolic link to images directory"
         $DEBUG ln -s $docsrcpath/images $testdir/images
      endif

      # Copy the CSS stylesheet to the test directory
      # (The stylesheet is also likely in $htmlgendir; this ensures
      # getting the latest "official" version)
      $DEBUG cp $PROX_HOME/src/xml/doc/stylesheets/$proxcss $testdir

   else if ($docformat == 'PDF') then
      if ($profiled == "1") then
         $DEBUG cp $docpdf $testtopdir/$docdirname/$docpdfname
         echo "(Changing name to $docpdfname)"
      else
         $DEBUG cp $docpdf $testtopdir/$docdirname/$docpdf
         echo "(Changing name to $docpdf)"
      endif
   endif
endif

# --------------------------------------------------------------------
# Report a warning if errors occurred

if ($profiledresult != 0) then
  echo " "
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " Error in profiling pass (likely bad XML)"
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " "
endif
if ($genfo != 0) then
  echo " "
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " Error in XSL-FO generation - examine trace"
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " "
endif
if ($gendoc != 0) then
  echo " "
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " Errors in document generation - examine trace"
  echo "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*"
  echo " "
endif

# --------------------------------------------------------------------
# We're done!
Done:
exit 0

# --------------------------------------------------------------------
Help:
  echo 'Usage:'
  echo '   proxgendoc [options] <document> [module] <format> [destination]'
  echo ' '
  echo 'Parameters:'
  echo '   options:            special processing instructions'
  echo '   document:           document to generate'
  echo '   module:             required for technical descriptions'
  echo '   format:             output format'
  echo '   destination:        where the generated docs will go'
  echo ' '
  echo 'Options:'
  echo '   --help or --h       print usage instructions'
  echo '   --debug or --d      run in debug mode (no document generation)'
  echo '   --clean or --c      delete old files before generating'
  echo '   --force or --f      generate PDF using existing FO file'
  echo '   --justclean or --j  just clean directories; do not generate docs'
  echo '   --noprofile of --n  do not make a profiling pass'
  echo '   --profile or --p    make a profiling pass'
  echo '   --r or --review     create working draft version'
  echo '   --imagelist         print list of image files used in document'
  echo '                         (does not process document normally)'
  echo 'Document:'
  echo '   tutorial            Proximity Tutorial'
  echo '   qgraph (qg)         Proxmity QGraph Guide'
  echo '   cookbook            Proximity Cookbook'
  echo '   recipe              Individual recipe (requires module)'
  echo '   td                  Technical description (requires module)'
  echo '   test                Test document (must be named Test.xml and'
  echo '                         be in $PROX_HOME/src/xml/doc/user/test)'
  echo ' '
  echo 'Output format:'
  echo '   --html              generate HTML'
  echo '   --pdf               generate PDF (includes FO generation)'
  echo '   --fo                generate FO only (no PDF)'
  echo '   --validate or --v   validate only; no other processing'
exit 1

NotImplemented:
  switch ($notimplcase)
      echo 'This functionality has not been implemented'
      echo "See 'genproxdoc.sh --h' for more information"
  endsw
exit 1

Usage:
  echo 'Usage: proxgendoc [options] <document> [module] <format> [destination]'
  echo "See 'proxgendoc --h' for more information"
exit 1

