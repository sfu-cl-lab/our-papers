
This directory contains resources used for testing.

java/ contains unit tests

functional/ contains files needed for functional testing 
            (query families acceptance tests)

run_nightly_tests.sh is a script that executes all the unit tests and
            sends an email message if there are any errors. Intented
            to be used as a cron job
            (e.g. 0 1 * * * cd /.../proximity3/ ; sh ./test/run_nightly_tests.sh loki.cs.umass.edu schapira@cs.umass.edu &> /dev/null)
