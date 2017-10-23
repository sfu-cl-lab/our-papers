# $Id: proximity.py 3658 2007-10-15 16:29:11Z schapira $
from kdl.prox.monet import Connection
from kdl.prox.monet import ResultSet
from kdl.prox.dbmgr import NST
from kdl.prox.dbmgr import NSTUtil
from kdl.prox.db import DB
from kdl.prox.util  import MonetUtil
from kdl.prox.dbmgr import DataTypeEnum

objectNST=prox.objectNST
linkNST=prox.linkNST
rootContainer=prox.rootContainer
objectAttrs=prox.objectAttrs
linkAttrs=prox.linkAttrs
containerAttrs=prox.containerAttrs

# list all valid NSTs
def ls():
    return DB.ls()

    # Print SHOWING message
def colHeading(NST, filterDef="*", colList="*", rowList="*"):
    columns = NSTUtil.colListToArray(NST, colList)
    colHeading = "[SHOWING head"
    for col in columns:
        colHeading = colHeading+ ", " + col
    colHeading = colHeading + " WHERE " + filterDef + " LIMIT " + rowList + "]"
    return colHeading

    # Print to STDOUT
def printNST(NST, filterDef="*", colList="*", rowList="*"):
    print(colHeading(NST, filterDef, colList, rowList))
    resultSet = NST.selectRows(filterDef, colList, rowList)
    while (resultSet.next()):
        print (resultSet.getLine())

def browseNST(NST):
    prox.browse(NST)