#
# $Id: fixAccents.py 3577 2007-09-05 18:04:00Z schapira $
#
# Part of the open-source Proximity system
#   (see LICENSE for copyright and license information).
#

# Script to take accented characters and replace them with ascii equivalents: e.g., 
# e with ` goes to just e.
# The encoding of the input file needs to be specified below; 
# choices include 'latin-1', 'utf8', ...

import unicodedata
import sys

if len(sys.argv) != 2:
		print "Usage: python fixAccents.py <input file>"
		sys.exit()

pf = open(sys.argv[1], 'r')
line = pf.readline()
while (line):
	line2 = line[:-1]	# removes newline
	line2 = unicode(line2, 'latin-1')	# the import data files were in this format
	line2 = unicodedata.normalize('NFKD', line2).encode('ASCII', 'ignore')
	print line2
	line = pf.readline()
	
