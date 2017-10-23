#!/bin/csh -f
# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# ********************************************************************
# deploy-docs.sh [targets] (defaults to all targets)
# ********************************************************************

# ====================================================================
# Creates the documentation tarballs for a Proximity release.  See the
# Proximity Style Guide for information on tasks to be completed before
# running this script.
#
# Most aspects of documentation generation (deleting old files, making
# sure there are symboic links to the image files, copying the prox.css
# stylefile) are handled by the proxgendoc.sh script, which this script
# calls.  All we need to do here is generate the various documentation
# targets (which can be one individually or all at once), create the
# tarball(s), and copy them to the designated distribution location.
#
# Switches:
#   --help or --h     display help message
#   --debug or --d    run in debug mode
#   --force or --f    do not provide reminder prompts
#
# Parameters:
#   targets           one or more delivery targets from list 'distrib',
#                     'web', 'kdl', or 'all' (default is 'all')
#
# Variables:
#    PX_HOME          root of the Proximity installation
#    PX_DISTRIB       root of test area for released docs
#    PX_WEB           root of the test area for web version of docs
#    PX_KDL           root of the kdl (internal) doc test area
#    PX_DOCS          root of 'user' doc area in release tarfile
#    TUTORIAL_PDF     name of the PDF version of the Tutorial
#    QGUIDE_PDF       name of the PDF version of the QGraph Guide
#    COOKBOOK_PDF     name of the PDF version of the Proximity Cookbook
#    RELEASE_TARFILE  name of tar file containing docs for release
#    WEB_TARFILE      name of tar file containing docs for the web site
#    KDL_TARFILE      name of tar file containing docs for internal KDL
#                     distribution
#    RELEASE_AREA     directory on kittyhawk that holds the distrib and
#                     web tarballs
#    LOGFILE          file to hold trace of execution
#
# Notes:
#   - The environment variable PROX_HOME must be set to the root of the
#     Proxmity installation
#   - The release version number is encoded in the entity file
#     ProxCommonEntities.ent
#   - Assumes all images files are PNGs
#   - Run from the $PROX_HOME directory
#   - Full trace written to logfile; abbreviated trace sent to stdout
# ====================================================================

# --------------------------------------------------------------------
# Define variables

# Set variables for directory and file names

set PX_HOME         = "$PROX_HOME"
set PX_DOCTEST      = "$PX_HOME/doc-test"
set PX_DISTRIB      = "$PX_HOME/doc-test/distrib/doc/user"
set PX_WEB          = "$PX_HOME/doc-test/web/documentation"
set PX_KDL          = "$PX_HOME/doc-test/kdl/local-docs"
set PX_DOCS         = "$PX_HOME/doc/user"
set TUTORIAL_PDF    = "Tutorial.pdf"
set QGUIDE_PDF      = "QGraphGuide.pdf"
set COOKBOOK_PDF    = "ProxCookbook.pdf"
set RELEASE_TARFILE = "prox-release-docs.tar"
set WEB_TARFILE     = "prox-release-webdocs.tar"
set KDL_TARFILE     = "prox-release-kdldocs.tar"
set RELEASE_TARFILE_COMPRESSED = "$RELEASE_TARFILE.gz"
set WEB_TARFILE_COMPRESSED     = "$WEB_TARFILE.gz"
set KDL_TARFILE_COMPRESSED     = "$KDL_TARFILE.gz"
set RELEASE_AREA    = "kittyhawk:/nfs/kit/users3/loiselle"
set KDL_AREA        = "kittyhawk:/nfs/kit/data1/httpd/html/local-docs"
set LOGFILE         = "$PROX_HOME/proxdocgen.log"

# Set defaults

set DEBUG           = ""
set reminders       = "1"   # 1=display reminders
set targets         = ""    # default: generates all targets

# --------------------------------------------------------------------
# Delete log file if it already exists

if (-f $LOGFILE) then
   rm -f $LOGFILE
endif

# --------------------------------------------------------------------
# Identify switches.

while ($1 =~ -*)
   if (($1 =~ "--h") || ($1 =~ "--help")) then
      goto Help
   else if (($1 =~ "--d") || ($1 =~ "--debug")) then
      set DEBUG = "echo"
      shift
      continue
   else if (($1 =~ "--f") || ($1 =~ "--force")) then
      set reminders = "0"
      shift
      continue
   else
      echo "Unknown option: $1"
      goto Usage
   endif
end

if ($DEBUG != "") then
   echo -n "Switches:"
   if ($DEBUG != "") then
      echo " debug"
   endif
   if ($reminders == 0) then
      echo " force"
   endif
endif

# --------------------------------------------------------------------
# Identify targets

# Remaining command line arguments should be delivery targets (distrib,
# web, or kdl).  You can specify multiple targets or 'all' to generate
# all delivery targets.

set targetlist = ($argv[*])

if ($#targetlist == 0) then
   set targets  = ("distrib web kdl")
else
   while ($#targetlist >= 1)
      set target = $targetlist[1]
      if ($target == "all") then
         set targets = ("distrib web kdl")
         shift targetlist
         break
      else if ($target == "distrib") then
         set targets = ($targets "distrib")
         shift targetlist
         continue
      else if ($target == "web") then
         set targets = ($targets "web")
         shift targetlist
         continue
      else if ($target == "kdl") then
         set targets = ($targets "kdl")
         shift targetlist
         continue
      else
         echo "Unknown delivery target"
         goto Usage
      endif
   end
endif

if ($DEBUG != "") then
   echo "Delivery targets: $targets"
endif

# --------------------------------------------------------------------
# Provide reminders about overwriting files, etc.

if ($reminders == 1) then
  echo -n "WARNING: This script overwrites the existing doc tarballs. Proceed? (y/n) "
  set reply = $<
  if ("$reply" =~ [Nn]*) then
    goto Quit
  endif
endif

# --------------------------------------------------------------------
# Perform the necessary actions for each target.  This includes
#  - generating the new documentation files
#  - removing old copies of files, if needed
#  - creating tarball and copying it to the correct location
# --------------------------------------------------------------------

foreach target ($targets)
  switch ($target)

    # ----------------------------------------------------------------
    case "distrib":
    # ----------------------------------------------------------------

      echo "======================================================="
      echo "                Generating DISTRIB docs                "
      echo "======================================================="

      cd $PX_HOME

      # Confirm overwriting tarfile if it already exists
      if (($reminders == 1) && (-f $PX_DOCS/$RELEASE_TARFILE_COMPRESSED)) then
        echo -n "$PX_DOCS/$RELEASE_TARFILE_COMPRESSED exists. Overwrite? [y/n] "
        set reply = $<
        if ("$reply" =~ [Nn]*) then
          goto Quit
        endif
      endif

      # Generate the Tutorial
      echo "********************" >> $LOGFILE
      echo -n "Generating Tutorial..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c tutorial --html >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c tutorial --pdf >>& $LOGFILE

      # Generate the QGraph Guide
      echo "********************" >> $LOGFILE
      echo -n "Generating the QGraph Guide..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c qg --html >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c qg --pdf >>& $LOGFILE

      # Generate the Cookbook
      echo "********************" >> $LOGFILE
      echo -n "Generating the Cookbook..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c cookbook --html >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c cookbook --pdf >>& $LOGFILE

      # Update the 'doc' (release) directory by deleting old files
      # and copying new documentation files to directory.
      echo "********************" >> $LOGFILE
      echo -n "Updating the release  (doc) directory..." | tee -a $LOGFILE
      echo -n "Deleting old files..." | tee -a $LOGFILE
      $DEBUG rm -f  $PX_DOCS/tutorial/$TUTORIAL_PDF
      $DEBUG rm -rf $PX_DOCS/tutorial/HTML
      $DEBUG rm -f  $PX_DOCS/qgraph/$QGUIDE_PDF
      $DEBUG rm -rf $PX_DOCS/qgraph/HTML
      $DEBUG rm -f  $PX_DOCS/cookbook/$COOKBOOK_PDF
      $DEBUG rm -rf $PX_DOCS/cookbook/HTML

      echo "Copying release files to doc directory..." | tee -a $LOGFILE
      $DEBUG cp -RL $PX_DISTRIB/tutorial/HTML $PX_DOCS/tutorial/
      $DEBUG cp     $PX_DISTRIB/tutorial/$TUTORIAL_PDF $PX_DOCS/tutorial/
      $DEBUG cp -RL $PX_DISTRIB/qgraph/HTML $PX_DOCS/qgraph/
      $DEBUG cp     $PX_DISTRIB/qgraph/$QGUIDE_PDF $PX_DOCS/qgraph/
      $DEBUG cp -RL $PX_DISTRIB/cookbook/HTML $PX_DOCS/cookbook
      $DEBUG cp     $PX_DISTRIB/cookbook/$COOKBOOK_PDF $PX_DOCS/cookbook

      # Create tar file
      echo "********************" >> $LOGFILE
      echo "Creating tar file..." | tee -a $LOGFILE

      # The file omit-from-tar.txt lists files and directories (e.g., CVS)
      # that should not be included in the tarfile.
      # (Create tar file from $PX_DOCS so relatve paths are correct)
      cd $PX_DOCS
      $DEBUG tar --exclude-from $PX_DOCTEST/omit-from-tar.txt \
                 --dereference \
                 -cf $RELEASE_TARFILE \
                     tutorial/$TUTORIAL_PDF \
                     tutorial/HTML \
                     qgraph/$QGUIDE_PDF \
                     qgraph/HTML \
                     cookbook/$COOKBOOK_PDF \
                     cookbook/HTML

      # Compress and move the doc tarball
      set RELEASE_TARFILE_COMPRESSED = $RELEASE_TARFILE.gz
      $DEBUG gzip --force $RELEASE_TARFILE
      #$DEBUG mv $RELEASE_TARFILE_COMPRESSED $PX_DOCS/

      # Copy tarball to kittyhawk
      echo "********************" >> $LOGFILE
      echo "Copying tarfile to kittyhawk..." | tee -a $LOGFILE
      $DEBUG scp $PX_DOCS/$RELEASE_TARFILE_COMPRESSED $RELEASE_AREA

      breaksw

    # ----------------------------------------------------------------
    case "web":
    # ----------------------------------------------------------------

      echo "======================================================="
      echo "                Generating WEB docs                    "
      echo "======================================================="

      cd $PX_HOME

      # Confirm overwriting tarfile if it already exists
      if (($reminders == 1) && (-f $PX_DOCS/$WEB_TARFILE_COMPRESSED)) then
        echo -n "$PX_DOCS/$WEB_TARFILE_COMPRESSED exists. Overwrite? [y/n] "
        set reply = $<
        if ("$reply" =~ [Nn]*) then
          goto Quit
        endif
      endif

      # Generate the Tutorial
      echo "********************" >> $LOGFILE
      echo -n "Generating Tutorial..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c tutorial --html web >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c tutorial --pdf web >>& $LOGFILE

      # Generate the QGraph Guide
      echo "********************" >> $LOGFILE
      echo -n "Generating the QGraph Guide..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c qg --html web >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c qg --pdf web >>& $LOGFILE

      # Generate the Cookbook
      echo "********************" >> $LOGFILE
      echo -n "Generating the Cookbook..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c cookbook --html web >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c cookbook --pdf web >>& $LOGFILE

      # Create web doc tarball
      echo "********************" >> $LOGFILE
      echo "Creating tar file..." | tee -a $LOGFILE
      cd $PX_HOME/doc-test/web
      $DEBUG tar --exclude-from $PX_DOCTEST/omit-from-tar.txt \
                 --dereference \
                 -cf $WEB_TARFILE \
                     documentation/tutorial \
                     documentation/qgraph \
                     documentation/cookbook

      # Compress and move the web tarball
      set WEB_TARFILE_COMPRESSED = $WEB_TARFILE.gz
      $DEBUG gzip --force $WEB_TARFILE
      $DEBUG mv $WEB_TARFILE_COMPRESSED $PX_DOCS/

      # Copy tarball to kittyhawk
      echo "********************" >> $LOGFILE
      echo "Copying tarfile to kittyhawk..." | tee -a $LOGFILE
      $DEBUG scp $PX_DOCS/$WEB_TARFILE_COMPRESSED $RELEASE_AREA

      breaksw

    # ----------------------------------------------------------------
    case "kdl":
    # ----------------------------------------------------------------

      echo "======================================================="
      echo "                Generating INTERNAL docs               "
      echo "======================================================="

      cd $PX_HOME

      # Confirm overwriting tarfile if it already exists
      if (($reminders == 1) && (-f $PX_DOCS/$KDL_TARFILE_COMPRESSED)) then
        echo -n "$PX_DOCS/$KDL_TARFILE_COMPRESSED exists. Overwrite? [y/n] "
        set reply = $<
        if ("$reply" =~ [Nn]*) then
          goto Quit
        endif
      endif

      # Generate the Tutorial
      echo "********************" >> $LOGFILE
      echo -n "Generating Tutorial..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c tutorial --html kdl >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c tutorial --pdf kdl >>& $LOGFILE

      # Generate the QGraph Guide
      echo "********************" >> $LOGFILE
      echo -n "Generating the QGraph Guide..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c qg --html kdl >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c qg --pdf kdl >>& $LOGFILE

      # Generate the Cookbook
      echo "********************" >> $LOGFILE
      echo -n "Generating the Cookbook..." | tee -a $LOGFILE
      echo -n "HTML..."
      $DEBUG bin/genproxdoc.sh --c cookbook --html kdl >>& $LOGFILE
      echo "PDF..."
      $DEBUG bin/genproxdoc.sh --c cookbook --pdf kdl >>& $LOGFILE

      # Create kdl doc tarball
      echo "********************" >> $LOGFILE
      echo "Creating tar file..." | tee -a $LOGFILE
      cd $PX_HOME/doc-test/kdl/local-docs
      $DEBUG tar --exclude-from $PX_DOCTEST/omit-from-tar.txt \
                 --dereference \
                 -cf $KDL_TARFILE \
                     tutorial \
                     qgraph \
                     cookbook

      # Compress and move the web tarball
      set KDL_TARFILE_COMPRESSED = $KDL_TARFILE.gz
      $DEBUG gzip --force $KDL_TARFILE
      $DEBUG mv $KDL_TARFILE_COMPRESSED $PX_DOCS/

      # Copy tarball to kittyhawk
      echo "********************" >> $LOGFILE
      echo "Copying tarfile to kittyhawk..." | tee -a $LOGFILE
      $DEBUG scp $PX_DOCS/$KDL_TARFILE_COMPRESSED $KDL_AREA
      echo "Remember to uncompress the tarfile on kittyhawk"
      echo " (in data1/httpd/html/local-docs)"

      # Remove previously generated files from the 'kdl' directory of the
      # doc-test area.

      #echo "********************" >> $LOGFILE
      #echo "Cleaning up the doc-test 'kdl' area" | tee -a $LOGFILE
      #$DEBUG rm -f  $PX_KDL/tutorial/$TUTORIAL_PDF
      #$DEBUG rm -rm $PX_KDL/tutorial/*.html
      #$DEBUG rm -f  $PX_KDL/tutorial/prox.css
      #$DEBUG rm -f  $PX_KDL/qgraph/$QGRAPH_PDF
      #$DEBUG rm -rm $PX_KDL/qgraph/*.html
      #$DEBUG rm -f  $PX_KDL/cookbook/$COOKBOOK_PDF
      #$DEBUG rm -rm $PX_KDL/cookbook/*.html
      #$DEBUG rm -f  $PX_KDL/qgraph/prox.css

      breaksw
  endsw
end

# --------------------------------------------------------------------
# We're done!

echo "Documentation generation completed" | tee -a $LOGFILE
exit 0

# --------------------------------------------------------------------
Help:
  echo 'Usage: deploy-docs.sh [options] [targets]'
  echo 'Run from the $PROX_HOME directory'
  echo 'Options:'
  echo '   --debug or --d       run in debug mode'
  echo '   --help or --h        print usage instructions'
  echo '   --force or --f       skip warnings'
  echo 'Targets:'
  echo '   all                  all targets'
  echo '   distrib              documentation for the release tarball'
  echo '   web                  documentation for the KDL public website'
  echo '   kdl                  documentation for internal use only'
  echo 'You can specify multiple targets'
exit 1

# --------------------------------------------------------------------
Usage:
  echo 'Usage: deploy-docs.sh [options] [targets]'
  echo 'Run from the $PROX_HOME directory'
  echo 'Enter "deploy-docs.sh --h" for more information'
exit 1

# --------------------------------------------------------------------
Quit:
  echo 'Run aborted'
exit 1
