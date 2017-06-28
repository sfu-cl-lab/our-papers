#! /usr/bin/perl -w

# $Id: text2xml.pl 3175 2007-02-26 20:02:55Z schapira $
# Part of the open-source Proximity system
# (see LICENSE for copyright and license information).

# Perl script to generate .xml for importing data from text files into
# Proximity 3.  See proximity3/doc/user/text2xml/text2xml.txt for
# description of format for input files.

# New options I'm allowing here:
# 1.
# object-file.txt<tab>onlyAttrs
# ...To mean "load the attributes specified on the following lines, but I already have the objects
# (or links) in my database."
# 2. 
# <tab>attrName<whitespace>type<whitespace>columnNumber
# ...So we can use only certain columns from the data
# Column numbers start at 0 for the prox id, 1 for the attrs.  When not specified, default is to
# auto-increment from the last.
# 

use strict;
use Symbol;

sub processSpecFile($$$$$);
sub processAttributes($$$$); 

# DATA STRUCTURES

# hash attrNames: for each obj/link file, the attributes it contains
#    key   	filename
#	 value 
#			-object or link
#			-whether we need the items themselves
#          	-for each column, which attribute (if any) it maps to
#    		so inner hash looks like: (type=> object or link, items => 1 or 0, 
#										1 => attr or "", 2 => attr or "", etc.)
my %attrNames;

# hash attrFiles: for each attribute, its output file handle
#    key   attribute name (prefixed with "O_" or "L_" to distinquish
#          between object and link attributes that might have same
#          name) 
#    value filehandle for that attribute's xml file
my %attrFiles;

# hash attrTypes: for each attribute, its type 
#    key   attribute name (prefixed with "O_" or "L_")
#    value one of "BIGINT", "DATE", "DATETIME",
#                 "DOUBLE", "INTEGER", "VARCHAR"
my %attrTypes;

# prox3 attrTypes. Used to convert from attrTypes to Prox3 types
my %prox3Types = qw(BIGINT lng DATE date DATETIME timestamp DOUBLE dbl INTEGER int VARCHAR str);

# *************************
# main body of program here
# *************************

my ($dbName, $objSpecFilename, $linkSpecFilename, $separatorChar) = checkArgs(@ARGV);

# constructs the hashes from spec files
my $needObjects = processSpecFile("O", $objSpecFilename, \%attrFiles,
					\%attrNames, \%attrTypes);
my $needLinks = processSpecFile("L", $linkSpecFilename, \%attrFiles,
					\%attrNames, \%attrTypes);

# opens xml files for each attribute, plus objects, links, and final concatenated file.
my ($objFH, $linkFH) = openFilesPrintHeaders($needObjects, $needLinks, $dbName, \%attrFiles);
processDataFiles(\%attrNames, $objFH, $linkFH);
close $objFH;
close $linkFH;

# concatenate the intermediate files
concatAll($needObjects, $needLinks, $dbName);


# *************************
# functions
# *************************
sub checkArgs {
	my @args = @_;
	# check number of command line arguments
	if (@args < 4) {
	   die "Usage: text2xml <databaseName> <objectSpecFile> <linkSpecFile> <separatorChar>\n<separatorChar> is one of the words {comma, space, tab}\n";
	}
	my ($dbName, $objSpecFilename, $linkSpecFilename, $separatorChar) =
	   @args;

	if ($separatorChar eq 'comma') {
	   $separatorChar = ',';
	} elsif ($separatorChar eq 'space') {
	   $separatorChar = ' ';
	} elsif ($separatorChar eq 'tab') {
	   $separatorChar = "\t";
	} else { 
	die "Usage: text2xml <databaseName> <objectSpecFile> <linkSpecFile> <separatorChar>\n<separatorChar> is one of the words {comma, space, tab}\n";
	}
	return ($dbName, $objSpecFilename, $linkSpecFilename, $separatorChar);
}

sub openFilesPrintHeaders {
	my ($needObjects, $needLinks, $dbName) = @_;

	if ($needObjects) {
		# open objects xml file for output
		open OBJXMLFILE, ">objects.xml" or die "$!: objects.xml";
		print OBJXMLFILE "    <OBJECTS>\n";
	}

	if ($needLinks) {
		# open links xml file for output (will remain empty if no links)
		open LINKXMLFILE, ">links.xml" or die "$!: links.xml";
		print LINKXMLFILE "    <LINKS>\n";
	}

	# open file for final result of script
	local *XMLOUTPUT;
	open XMLOUTPUT, ">$dbName.xml" or die "$!: $dbName.xml";

	#print XMLOUTPUT "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	# UTF-8 encoding doesn't always work so this is switched to latin-1
	print XMLOUTPUT "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
	print XMLOUTPUT "<!DOCTYPE PROX3DB SYSTEM \"prox3db.dtd\">\n";
	print XMLOUTPUT "<PROX3DB>\n";
	close XMLOUTPUT;

	# open xml file for each attribute and write the first line
	my $attrName;
	my $filehandle;

	while (($attrName, $filehandle) = each %attrFiles) {
	   open $filehandle, ">$attrName.xml" or die "$!: $attrName.xml";
	   $attrName =~ /([OL])_(\S+)/;
	   my ($itemType, $attrNameNoPrefix) = ($1, $2);
	   print $filehandle
	"        <ATTRIBUTE NAME=\"$attrNameNoPrefix\" ITEM-TYPE=\"$itemType\"";
	   my $type = $attrTypes{$attrName};
	   my $prox3Type = $prox3Types{$type};
	   print $filehandle " DATA-TYPE=\"$prox3Type\">\n";
	}
	return (\*OBJXMLFILE, \*LINKXMLFILE);
}

sub processDataFiles {
	my $attrNamesRef = shift;
	my $objFH = shift;
	my $linkFH = shift;
	my $inputLine;

	foreach my $filename (keys %$attrNamesRef) {
	   open TEXTFILE, "$filename" or die "$!: $filename";
	   while ($inputLine = <TEXTFILE>) {
		  # read line and split into object id and attribute values
		  chomp $inputLine;
		  my @fields = split(/$separatorChar/, $inputLine);
		  my $itemId = shift @fields;
		  if ($attrNamesRef->{$filename}->{items}) {
		  	if ($attrNamesRef->{$filename}->{type} eq "O") {
			  print $objFH "        <OBJECT ID=\"$itemId\"/>\n";
			}
			else {	# links
				# link description includes o1 and o2 ids
				my ($o1Id, $o2Id) = splice(@fields, 0, 2);	# splice removes from orig list
				print $linkFH
					"        <LINK ID=\"$itemId\" O1-ID=\"$o1Id\" O2-ID=\"$o2Id\"/>\n";
			}
		  }
		  
		  if (@fields) {
			 # pass the remaining fields to processAttributes
			 processAttributes($itemId, $attrNames{$filename},
							   \@fields, \%attrFiles);
		  } # end if
	   } # end while
	   close TEXTFILE;
	} # end foreach
}

# Final output and cleanup: concatenate output into one xml file,
# delete working files.
sub concatAll {
	my $haveObjs = shift;
	my $haveLinks = shift;
	my $dbName = shift;

	if ($haveObjs) {
		system "cat objects.xml >> $dbName.xml; rm objects.xml";
		system "echo '    </OBJECTS>' >> $dbName.xml";
	}

	if ($haveLinks) {
		system "cat links.xml >> $dbName.xml; rm links.xml";
		system "echo '    </LINKS>' >> $dbName.xml";
	}

	my $haveAttrs = keys(%attrFiles);
	if ($haveAttrs) {
		system "echo '    <ATTRIBUTES>' >> $dbName.xml";

		foreach my $filehandle (values %attrFiles) {
		  print $filehandle "        </ATTRIBUTE>\n";
		  close $filehandle;
		}
		foreach my $attrName (sort keys(%attrFiles)) {
		  system "cat $attrName.xml >> $dbName.xml; rm $attrName.xml";
		}
		system "echo '    </ATTRIBUTES>' >> $dbName.xml";
	} # end if

	system "echo '</PROX3DB>' >> $dbName.xml";
}

# process object and link specification files
# also keep track of whether there will need to be an <OBJECTS> or <LINKS> file (respectively).
#
# $attrNames->{$datafile} = (type=> O or L, items => 1 or 0, 
#							1 => attr or "", 2 => attr or "", etc.)
# (column numbers start at 0 for the prox id, 1 for the attrs)

sub processSpecFile($$$$$) {
	my ($objOrLink, $SpecFileName, $attrFilesHashRef,
		$attrNamesHashRef, $attrTypesHashRef) = @_;

	open SPECFILE, $SpecFileName or die "$!: $SpecFileName";

	my $needAnyItems = 0;
	my $dataFileName = "";
	my $colCounter = 1;
	while (my $specFileLine = <SPECFILE>) {
		chomp $specFileLine;
		if ($specFileLine eq "") {
			 # blank line marks the end of attribute list for this text file
			 next;
		} elsif ($specFileLine =~ /^\t(\S+)\s+(\S+)/) {

			# line begins with tab; must be attribute name, type pair
			my ($attrName, $attrType) = ($1, $2);

			# perhaps also column number
			if ($specFileLine =~ /^\t(\S+)\s+(\S+)\s+(\d+)/) {
				$colCounter = $3;
			}

			# prepend "O_" or "L_" to attribute name, to distinguish between
			# object and link attributes that might have same name
			$attrName = "$objOrLink\_$attrName";

			# add attribute name to hash of attributes/column-numbers for this text file
			$attrNamesHashRef->{$dataFileName}->{$colCounter} = $attrName;
			$colCounter++;

			# if we are seeing this attribute for the first time,
			# record its type and the filehandle for its xml file
			unless (exists $attrTypesHashRef->{$attrName}) {
				$attrTypesHashRef->{$attrName} = $attrType;
				# generate an anonymous glob for its filehandle
				$attrFilesHashRef->{$attrName} = gensym;
			}

		} else {
			# must be a text filename.  
			$dataFileName = $specFileLine;
			my $needItems = 1;
			if ($specFileLine =~ /^(\S+)\tonlyAttrs/) {
				$needItems = 0;
				$dataFileName = $1;
			}
			if ($needItems) {
				$needAnyItems = 1;
			}

			# initialize attribute hash for this text file
			$attrNamesHashRef->{$dataFileName} = {items=>$needItems, type => $objOrLink};
			$colCounter = 1;
		} # end if

   } # end while

   close SPECFILE;
   return $needAnyItems;

} # end sub

# For each attribute on the attributeList,
# if corresponding value on valueList is non-null,
# write it to the xml file for that attribute.

# recall: column numbers start at 0 for the prox id, 1 for the attrs. prox id has been stripped off by
# this point.
sub processAttributes ($$$$) {
   my ($itemId, $attributeHashRef, $valueListRef, $attrFilesHashRef) = @_;

	foreach my $col (keys %$attributeHashRef) {
		# except "type" and "items"
		next if ($col eq "type" || $col eq "items");

		if (@$valueListRef >= $col) {
			my $attrValue = $valueListRef->[$col - 1];
			if ($attrValue ne "") {
				my $attrNameWithPrefix = $attributeHashRef->{$col};
				my $handle = $attrFilesHashRef->{$attrNameWithPrefix};

				# clean up the data and eventually replace & with &amp; if
				# that works
				$attrValue =~ s/\&/_/g;
				# this fixes a typo (??)
				#$attrValue =~ s/<//g;

				print $handle
					"            <ATTR-VALUE ITEM-ID=\"$itemId\">" . 
					"<COL-VALUE>$attrValue</COL-VALUE></ATTR-VALUE>\n";
			}
		} # end if
	} # end for
} # end sub
