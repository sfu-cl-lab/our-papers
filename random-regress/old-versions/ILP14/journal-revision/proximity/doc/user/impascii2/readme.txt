$Id: readme.txt 3658 2007-10-15 16:29:11Z schapira $

This directory contains files that document and demonstrate Proximity's "smart
ASCII" import feature, which supports creating graphs via a simple text format.
It contains the following files:

    example-db.txt         : a sample database in Smart ASCII format
    example-schema.py      : a Proximity Python file that defines the attributes
                             needed to load example-db.txt
    readme.txt             : this file
    smart-ascii-format.txt : defines the file format

To load the sample database do the following:

1. Run the script example-schema.py on a *new* database using the
   kdl.prox.app.PythonScript application. This will create the necessary
   Proximity tables, then define the attributes needed for the sample database.

2. Run the kdl.prox.app.ImportAsciiGraph application on example-db.txt. You can
   then browse the database with the kdl.prox.app.GUI2 application.
