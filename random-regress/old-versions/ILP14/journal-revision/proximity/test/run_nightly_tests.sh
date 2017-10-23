#!/bin/sh

#
# $Id: run_nightly_tests.sh 3676 2007-10-23 12:26:53Z root $
#
# Script to run nightly tests.
# Parameters:
#     path.to.Monet : path to directory under which bin/Mserver can be found
#     Monet.port    : port in which Monet should listen
#     mail.tolist   : comma-separated list of addresses to send email to
# Example: run_nightly_tests /usr/local/Monet-mars 40000 loki.cs.umass.edu,schapira@cs.umass.edu
#
# Assumes:
#    Java  installed on /usr/java/jdk1.5.0_10
#    Ant   installed on /usr/local/apache-ant-1.6.1/
#    Monet installed on path.to.Monet
#
# To be run from top level directory, proximity/
# See root's cronjob on aeroplane for example use
#

# Check parameters
if [ $# -ne 3 ]
then
  echo "Usage: run_nightly_tests.sh path.to.Monet Monet.port mail.tolist"
  exit
fi

pathToMonet=$1
monetPort=$2
emailToList=$3

proxDir=`pwd`

# Update local copy of repository
#export CVS_RSH=ssh
#cvs -q up -dP
svn up

# Startup MServer, and save its PID
rm -rf ${pathToMonet}/var/MonetDB/dbfarm/test_db
rm -rf ${pathToMonet}/var/MonetDB4/dbfarm/test_db
killall Mserver
${pathToMonet}/bin/Mserver --dbname test_db ${proxDir}/resources/init-mserver.mil --set port=${monetPort} &
serverPID=$!

# Run tests, saving results to file (NB: Ant target only outputs errors/failures)
# NB: DISPLAY value is specific to root@aeroplane
export ANT_HOME=/nfs/kit/data2/prox-test/apache-ant-1.6.2/
export JAVA_HOME=/usr/java/jdk1.5.0_10
rm -r prox.log.mil
rm -f ant-err.txt
$ANT_HOME/bin/ant --noconfig clean run-nightly-tests 2> ant-err.txt

# Send mail if errors/failures
if [ -s ant-err.txt ]
then
    find ${proxDir}/reports/junit/ -type f -exec grep "FAILED" '{}' ';' -print >> ant-err.txt
    find ${proxDir}/reports/junit/ -type f -exec grep "ERROR" '{}' ';' -print >> ant-err.txt
    cat ant-err.txt | mail -s "Proximity3 Tests Failed" ${emailToList}
else
    ts=`date`
    echo "Finished at ${ts}" | mail -s "Proximity on kittyhawk" ${emailToList}
fi

# Kill server
kill -9 $serverPID
