# $Id: example-schema.py 3658 2007-10-15 16:29:11Z schapira $

from kdl.prox.dbmgr import DataTypeEnum

# print start info
prox.log.info("-> " + "'" + prox.scriptFile + "' running on database '" + \
	prox.dbName + "'. prox: '" + prox.toString())

# create Prox tables. NB: clears existing database!
prox.getProxDB().clearDB();
prox.initDB();

# define object attributes
prox.defineAttribute("s", 1, DataTypeEnum.STR)  # is also the 'name' attribute
prox.defineAttribute("i", 1, DataTypeEnum.INT)

# define link attributes
prox.defineAttribute("s", 0, DataTypeEnum.STR)

# done
prox.log.info('-> exiting script')
