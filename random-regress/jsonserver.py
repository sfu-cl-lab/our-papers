# Simple JSON response service

import os
import json

# Modules that must be installed via pip
import mimeparse
from bottle import route, run, request, response, abort

# Modules imported from local sources
import genBNtable as BN

PORT_NUM = 2500
TEMPLATE_NAME = 'template'
POP_NAME = 'populations'
POP_VARS = 'pop_vars'
FUNCTORS = 'functor_ranges'

# Defined by the caller

db = None
bn = BN.BayesNet()

@route('/define',method='OPTIONS')
def options():
    print "In /define OPTIONS"
    return _options_return()

@route('/define',method='POST')
def define():
    print "in define"
    global db, bn
    # Check to make sure JSON is ok
    mimetype = mimeparse.best_match(['application/json'],
                                    request.headers.get('Accept'))
    if not mimetype:
        return abort(406)

    # Check to make sure the data we're getting is JSON
    if request.headers.get('Content-Type') != 'application/json':
        return abort(415)

    response.headers.append('Content-Type', mimetype)
    response.headers.append("Access-Control-Allow-Origin", "*")

    dbbndef = json.load(request.body)
    db = BN.Database([],
                     dbbndef[FUNCTORS],
                     dbbndef[POP_NAME],
                     empty_db=True
        )
    bn = BN.BayesNet.loadSG(BN.SerializedGraph.loadJSON(dbbndef[TEMPLATE_NAME]))
    if not bn.isCoherent():
        response.status = 500
        return {}

    if not bn.isCompatibleFR(db):
        response.status = 500
        return {}
    '''
    # Alternative approach: The above will fail on Chrome with a CORS error if
    # there is any error.
    # The following will emphasize the 500 error but not print a useful traceback
    # on the server side.
    try:
        bn = BN.BayesNet.loadSG(BN.SerializedGraph.loadJSON(dbbndef[TEMPLATE_NAME]))
    except Exception as exp:
        # Nonstandard to bottle package but works for now:
        # Set response.status to error and return.
        # This ensures that response will have a header including
        # "Access-Control-Allow-Origin=*". 
        # A more standard "bottle" approach would use one of
        # HTTPResponse or HTTPError.
        print exp
        response.status = 500
        return {}
    '''
    return {'answer': 'Done'}

@route('/ground',method='OPTIONS')
def options():
    print "In /ground OPTIONS"
    return _options_return()

@route('/ground',method='POST')
def ground():
    print "in ground"
    global bn, db
    # Check to make sure JSON is ok
    mimetype = mimeparse.best_match(['application/json'],
                                    request.headers.get('Accept'))
    if not mimetype:
        return abort(406)

    # Check to make sure the data we're getting is JSON
    if request.headers.get('Content-Type') != 'application/json':
        return abort(415)

    response.headers.append('Content-Type', mimetype)
    response.headers.append("Access-Control-Allow-Origin", "*")

    data = json.load(request.body)
    db = BN.Database(db.attributes,
                     db.functorRangeList,
                     data[POP_NAME],
                     empty_db=True
        )
    return bn.groundBy(db,data[POP_VARS]).json()

@route('/data/<item>',method='GET')
def get_item(item):
    response.headers.append("Access-Control-Allow-Origin", "*")
    return {'a': 5, 'b' : 7}

@route('/post',method='POST')
def post_item():
    """
        Just a demonstration of POST. It particularly shows how to handle
        CORS issues.
    """
    print "Entered post_item"
    # Check to make sure JSON is ok
    mimetype = mimeparse.best_match(['application/json'], request.headers.get('Accept'))
    if not mimetype:
        return abort(406)

    # Check to make sure the data we're getting is JSON
    if request.headers.get('Content-Type') != 'application/json':
        return abort(415)

    response.headers.append('Content-Type', mimetype)
    response.headers.append("Access-Control-Allow-Origin", "*")

    # Parse the request
    data = json.load(request.body)
    print "Received", data
    print "Ready to return from POST"

    return {"answer": "Done"}

@route('/post',method='OPTIONS')
def options():
    print "Entered /post OPTIONS"
    return _options_return()

def _options_return():
    response.headers.append("Access-Control-Allow-Origin", "*")
    response.headers.append("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS, DELETE")
    response.headers.append("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, X-Requested-With, X-CRSF-Token, Access-Control-Allow-Origin")
    return {}

# Fire the engines
if __name__ == '__main__':
    run(host='0.0.0.0', port=os.getenv('PORT', PORT_NUM), quiet=True)
