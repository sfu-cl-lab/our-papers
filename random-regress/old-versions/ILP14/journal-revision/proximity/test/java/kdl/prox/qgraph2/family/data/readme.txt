;; -*- mode: outline -*-

$Id: readme.txt 1738 2004-12-16 18:36:16Z cornell $


This file gives a brief summary of the steps involved in creating and
running a query family acceptance (functional) test. Following are
these sections: concepts, a brief processing summary, instructions for
setting up and running, processing details, and todo items.


* Concepts

We have decided to test the qGraph2 implementation via query families,
where a 'query family' is a description of a set of queries, including
which 'variations' should be made in order to create specific query
instances from it. A 'query instance' is a real qGraph2 query (XML
file) that is generated from a family. One family will generate one or
more instances, which are later run against test data to check
correctness.

We're currently unclear on a concrete definition for a query family,
but our intution tells us:

  o Each family uses the same set of transformations.

  o Constraints, subqueries, and vertex (not edge) annotations are
    tested in their own families, i.e., they are not variations in
    particular families.

  o Each family uses the same data set to test all query instances in
    the family, i.e., the test data set is shared among all instantiated
    queries in a family.


* Processing Summary

For each family, the test writer should:

  1. make '<family name>.fam.xml'   (1)
  2. run QueryGenApp                (creates N 'query_<description>.xml' files)
  3. edit '<family name>.fam.dat'   (1)
  4. edit 'query_<description>.mat' (N)
  5. run FamilyTestApp
  6. (optional) run BatchFamilyTestApp


* Setup and Running

You can compile and run the programs by either using the JBuilder
project in this directory (query-accept-test.jpx), or the Ant build
file (build.xml). These are documented next. Note that xml processing
requires access to the DTD files specified in each xml file. We
provide this access using symbolic links. Run the script make-links.sh
in this directory to create them. Do this each time you create a new
family.


** using JBuilder

The project file has three run configurations (those ending "App") and
one unit test target ("AllTests"). Run the appropriate
configuration. Note that they take fixed arguments which, if files or
directories, are relative to this directory:

  QueryGenApp: input from the file test.fam.xml (paste into it your
    family xml), and output to fam-gen

  FamilyTestApp: database config from prox.prop, input from fam-test
    (make a symbolic link to the desired families/family_NN directory)

  BatchFamilyTestApp: database config from prox.prop, input from
    the families directory


** using Ant

Follow the instructions in build.xml. Each application target asks for
its inputs, which are relative from this directory. Here are the app
targets:

  gen-family
  test-family
  test-families


* Details

1. The test writer creates a 'query family' XML file that describes
the family's 'base query' (a simplified qGraph2 query) and specifies
which variations on the base query should be made in order to generate
query 'instances' (true qGraph2 query files) from it. The query family
file is placed in a new directory for that family, under the
"families" directory in this directory. The file's name has this
format: '<family name>.fam.xml', and the new family's directory's name
has this format: 'family_<family name>'. For example:

    '1.fam.xml' in the directory 'families/family_1/'

A family's variations can include the following two types: direction
and numeric annotation. (Note that below we specify shortcut 'labels'
for each variation, e.g., 'DX', 'A1', etc.)

  o Direction: Each edge in a family can be given a label that
    indicates whether and how edge direction should be varied. There
    are three possibilities:

    'DX': (i.e., none) : direction is not tested
    'DO': (i.e., one)  : test undirected and one direction (from v1 to v2)
    'DB': (i.e., both) : 'DO' test + test other direction (from v2 to v1)

  o Annotation: Each edge and vertex in the family can be given a
  label that indicates whether and how to numeric annotations should
  be varied. There are four possibilities:

    'AX': (i.e., none): annotation is not tested
    'A1': (i.e., one) : test [1..]
    'AN': (non-empty) : 'A1' test + [1..1], and [1..2] (i.e., [1..], [1..1], and [1..2])
    'AA': (all)       : 'AN' test + test no annotation


Note that labeling an edge or vertex with 'DX' or 'AX' is the same as
not labeling it at all. The latter approach is recommended. Also note
that not all variations are valid for edges and vertices; here are the
valid variations:

  o Edges: any direction (DX, DO, DB), and/or any annotation ('AX', 'A1', 'AN', 'AA')

  o Verticies: 'A1' or 'AN'. (Recall that supplying *no* vertex
    annotation requires creating a different family.)


2. The test writer runs the 'query instance generator' program
(kdl.prox.test.qg2.QueryGenApp), which generates query instances from
the '<family name>.fam.xml' file by creating one qGraph2 query (XML
file) for each combination of variations specified in the family. Each
file resides in the same directory as the family, and each file's name
has this format: 'query_<description>.xml'. Ex:

    'query_A_B1inf_C_D_X1inf_Y1infDir.xml'

The description encodes the particular variations the file
contains. (todo more on names)

In addition to the '*.xml' query instance files the generator program
creates the following empty 'stub' files for filling-in by test
writers:

  a. the family's 'test data set' in this format:

      '<family name>.fam.dat'

  b. a 'data match file' for *each* query in this format:

      'query_<description>.mat'

As a safety measure, the program moves all old files ('*.qg2.xml',
'*.fam.dat', and '*.mat' files to a new backup directory.


3. The test writer copies the generated files to the
'families/family_NN/' directory, then fills in the empty test data set
file ('*.fam.dat') using a simple text format, which looks like this:

  a1
  a2 x1 b1
  a2 x2 b1
  a3 x3 b2
  ...

You can see that each row contains either a single object or (more
commonly) a single edge with its o1 object listed to its left and its
o2 object listed to its right(i.e., the edge goes from the left object
to the right one). The rows are not ordered. Note that we use the same
data set for all query instances in the family. In other words, the
test data set is shared among all instantiated queries in a
family. (This seems to be one of the characteristics of a family.)

The test program uses this file to generate a smart ASCII graph file
that is then interpreted to populate the database. To simplify
creating test data and matches, we use a fixed schema for types and
attributes:

  o link type attribute   : 'linkType'
  o object type attribute : 'objectType'

  o link name attribute   : 'linkName'
  o object name attribute : 'objectName'      <- todo reduntant with nickname

  o link types   : 'W', 'X', 'Y', 'Z'
  o object types : 'A', 'B', 'C', 'D'

  o link names   : <w|x|y|z><N>, where n >= 0, i.e, w0, y33
  o object names : <a|b|c|d><N>, where n >= 0, i.e, a0, c22

The program determines link and object type values by simply using the
uppercased first letter of the name in the file.


4. For each query instance the test writer fills in the empty data
match file ('*.mat'), specifying the query's expected matching
subgraphs from the test data set file ('*.fam.dat'). We will use
another simple text format, which looks like this:

  a2 x1 b1
  a2 x2 b1
  ...

You can see that, like the format in step 3, each row specifies a
matching subgraph by listing its items (objects or links). Neither the
items in a row or the rows themselves are ordered.


5. A completed 'test' for a query family is a directory containing the
files listed above:

    a. query family file: '<family name>.fam.xml'
    b. query instance files: 'query_*.xml'
    c. family dataset file: '<family name>.fam.dat'
    d. query data match files (for each query instance): 'query_<description>.mat'


6. The test writer runs each test using the 'query family test'
program (kdl.prox.test.qg2.FamilyTestApp), which performs these tasks:

    a. create a new database
    b. populate it with the family's test data
    c. test each query, comparing actual and expected results
    d. log test results

Each query is tested by running it and comparing its actual results to
the expected results in its data match file. Note that as a 'sanity
check' we run the query multiple ways, one for each path in the
transformation graph , the results must all be identical. Finally the
program saves the test results into a log file, including each test's
success (pass or fail), and transformation usage distribution.


7. We test all families via the 'qGraph2 test suite' program
(kdl.prox.test.qg2.BatchFamilyTestApp), which tests all query families
and logs overall test results, including overall transformation usage
distribution. The program warns if a) any transformations were not
used by *any* family, and b) if any families use exactly the *same*
transformations. (todo NB: These two features are not yet
implemented.)

todo output format, logging (incl. sample lcf file), etc.


* Todo

todo change "A*" and "A*" to "*" and "+", respectively

todo define family:
  o same transformations
  o same input data file
  o one set of vertex annotations
  o ...

todo integrate the following:


** notes

the query instance generator program should create query instance file
names in a deterministic manner, so that accidentally re-running it
(which moves all files to a backup directory) can be easily undone by
simply moving the files back to the parent directory
