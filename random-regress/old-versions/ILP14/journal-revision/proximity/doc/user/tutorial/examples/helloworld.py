# Part of the open-source Proximity system (see LICENSE for copyright
# and license information).

# Proximity scripts use the 'prox' object to access method from the
# Proximity class.

# The database name is a string containing the host and port
# information for the current database.
dbname = prox.getDbName()

# Print the "Hello World" message and database
# connection information.
print "Hello world, you're using database ", dbname
