
"""
    Compute joint probabilities and pseudo-likelihood estimate for a Bayes Net according to
    random selection semantics from a database.

    Written in haste---bugs likely remain! It's also been eight months since I wrote real
    python, so some things might be more concisely expressed using standard idioms.

    Improvements to make:
    1. Before doing anything, check global consistancy (functor names, constants, ...)
    2. Currently assumes a single domain. Extending to multiple domains is easy but tedious.
    3. The algorithm for generating all possible groundings (generateCombos) is far too limited.

    Written in old-skool python 2.7 by Ted Kirkpatrick, March 2013.
"""

import copy
import math

class Node (object):
    """
    A functor, including its value. Nodes in a Bayes Net structure have unspecified values (IN_TREE), while
    nodes used for querying have QUERY values.
    """
    IN_TREE = '-'
    QUERY = '?'
    
    def __init__ (this, functor, varList, val):
        this.functor = functor
        this.varList = varList
        this.val = val

    def __str__ (this):
        return this.functor+"("+",".join(this.varList)+") = "+str(this.val)

    def __repr__ (this):
        return str(this)
    
    """   No used, no more 
    def match (this, node, grounding):
        if not node.isLiteralNode():
            raise Exception ("Attempt to match nonprobability Node "+str(node)+" with probability value")
        
        if this.functor != node.functor:
            return (False,0)
        else:
            for (var,val) in zip(node.varList, this.varList):
                 if val != grounding.val(var):
                    return (False,(var,val))
            return (True, this.val)
    """

    def eq(this, node):
        return this.functor == node.functor and this.varList == node.varList and this.val == node.val

    def isLiteralNode(this):
        return this.val != Node.QUERY and this.val != Node.IN_TREE

    def isQueryNode(this):
        return this.val == Node.QUERY

class Rule (object):
    """ A rule specifying a conditional probability (the class is likely misnamed) """
    def __init__ (this, child, parentList, prob):
        this.child = child
        this.parentList = parentList
        this.prob = prob

    def __str__(this):
        return "P("+str(this.child)+"  |  "+",".join([str(n) for n in this.parentList])+") = "+str(this.prob)

class Grounding (object):
    """ A specific assignment of constants to variables """
    def __init__ (this, varList):
        this.varList = varList

    def val (this, var):
        for (v, ground) in this.varList:
            if v == var:
                return ground
        else:
            raise Exception ("Var not ground: " + var)

    def groundNode (this, node):
        gndList = []
        for var in node.varList:
            gndList.append(this.val(var))
        return Node (node.functor, gndList, node.val)

    def __repr__(this):
        return ", ".join(v[0]+"="+str(v[1]) for v in this.varList)

class Database (object):
    """ The database, specifying the functor values for given arguments """
    def __init__ (this, attrs):
        this.attributes = attrs

    def funcVal(this, node):
        if node.isLiteralNode():
            raise Exception ("Attempted to match nonquery Node "+str(node)+" probability")
        for n in this.attributes:
            if n.functor == node.functor and all ([p[0]==p[1] for p in zip(n.varList,node.varList)]):
                return n.val
        else:
            raise Exception ("Functor not ground: " + node)

class NetNode (object):
    """
    A node within a Bayes net. In addition to the functor description (Node), this has a list of parent Nodes.
    """
    def __init__(this, node, parents):
        this.node = node
        this.parents = parents

    def __str__(this):
        return str(this.node)+" <- ("+", ".join([str(sn) for sn in this.parents])+")"

class BayesNet (object):
    """ The Bayes net """
    def __init__(this):
        this.nodes = []

    def append(this, netNode):
        for child in netNode.parents:
            if child not in this.nodes:
                raise Exception ("BayesNet node " + str(netNode) + " added but child " + str (child) + " not already in net")
        this.nodes.append(netNode)

    def jointProbs(this, grounding, db, ruleSet):
        probs = []
        joint = 1.0
        for node in this.nodes:
            #print "searching",node
            gn = fillNode(node.node, grounding, db)
            #print "filled node", gn
            gcn = [fillNode(n.node, grounding, db) for n in node.parents]
            #print "filled parents", gcn
            p = ruleMatch(ruleSet, gn, gcn)
            if p == -1:
                p = default(gn.functor)
            probs.append((gn, p))
            joint *= p
        probs.append(joint)
        probs.append(math.log(joint))
        return probs

    def variableList(this):
        vars = set()
        for n in this.nodes:
            for v in n.node.varList:
                vars.add(v)
        return sorted(list(vars))

def query (node, grounding, db):
    """ Ground a node and look it up in the db """
    return db.funcVal(grounding.groundNode(node))

def fillNode(node, grounding, db):
    """ Return a grounded node, with the value for its functor according to db """
    gn = copy.deepcopy(node)
    gn.val = query(gn, grounding, db)
    return gn

def ruleMatch (ruleSet, node, parents):
    """
    Locate the value for a grounded node and its parents in a rule set, return -1 if not found.
    For functors with binary ranges, when all parents match but child's value does not, return 1-prob for other value.
    """
    def getProb (node):
        for rule in ruleSet:
            #print rule
            if (rule.child.eq(node) and
                len(rule.parentList)==len(parents) and
                all([n[0].eq(n[1]) for n in zip(rule.parentList,parents)])):
                #print "winning eq", [n for n in zip(rule.parentList,parents)]
                return rule.prob
        else:
            return -1
    
    prob = getProb (node)
    if prob == -1 and functorRangeSize(node.functor) == 2:
        tn = copy.copy(node)
        tn.val = functorOtherValue(tn.functor, tn.val)
        prob =  getProb (tn)
        if prob != -1:
            return 1 - prob
        else:
            return prob
    return prob

def default(functor):
    """ Return default uniform distribution for the range of a functor """
    return 1.0/functorRangeSize(functor)

def functorRange(functor):
    """ Look up the range for a functor """
    for (name, range) in functorRangeList:
        if functor == name:
            return range
    else:
        raise Exception ("Functor " + functor + " not present in range list")

def functorRangeSize(functor):
    """ Return cardinality of range for a functor """
    return len(functorRange(functor))

def functorOtherValue(functor, val):
    """ For functors with a binary range, return the other element """
    range = functorRange(functor)
    assert len(range) == 2
    if val == range[0]:
        return range[1]
    else:
        return range[0]

def atomList(joints):
    """ Return the atoms, derived from the first entry in the joint probability table """
    assert len(joints) > 0
    first = joints[0]
    functorList = first[1][:-2] # Second element of row, last two elements of that are joint prob and log prob
    atomList = []
    for (node,_) in functorList:
        atomList.append(node.functor+"("+",".join(node.varList)+")")
    return atomList

def jointProbabilities(constants, db, ruleList, bn):
    """ Compute the joint probabilities for all combinations of values """
    vars = bn.variableList()
    combs = generateCombos(vars, constants)
    joints = []
    for grounding in combs:
        joints.append((grounding, bn.jointProbs(grounding, db, ruleList)))
    return (vars, atomList(joints), joints)

def generateCombos(vars,constants):
    """ Generate all possible groundings (assignments of constants to variables) """
    # SUPER NOT GENERALIZED---TOO LATE AT NIGHT FOR ME TO DO RECURSIVE ALGORITHMS
    assert len(vars) == 2 and len(constants) == 2
    combs = []
    for c1 in constants:
        for c2 in constants:
            combs.append(Grounding([(vars[0], c1), (vars[1], c2)]))
    return combs

def formatJointTableForLaTeX(joints):
    """
`   Given a joint probability table, format it for LaTeX.
    This function will have to be tailored for every paper.

    This function simply generates the {tabular} part of the table. The prologue and epilogue,
    including the caption and label, must be specified in the including file.
    """
    (varList, atoms, probs) = joints
    cols = len(varList) + len (probs[0][1])
    with open("table1.tex","w") as out:
        out.write ("\\begin{tabular}{|" + "|".join(["c"]*(cols-2))+"||c|c|}\n")
        out.write ("\\hline\n")
        # Table header
        out.write (" & ".join(varList) + " & " + " & ".join([a for a in atoms]) + " & Joint $p$ & ln~$p$ \\\\ \\hline\n")
        # Table rows
        logps = []
        for (grounding, probs) in probs:
            out.write (" & ".join([val for (var, val) in grounding.varList]) + " & " +
               " & ".join([str(n.val)+" ({:.1f})".format(p) for (n,p) in probs[:-2]]) +
            " & {:.2f}".format(probs[-2]) + " & {:.2f}".format(probs[-1]) + "\\\\\n")
            logps.append(probs[-1])
        # A line to indicate there are further entries in the DB
        out.write(" & ".join(["\ldots"]*cols) + "\\\\\n")
        # Close environment
        out.write ("\\hline\n\\end{tabular}\n")

    with open("tab1plogp.tex","w") as plogp:
        plogp.write("\\newcommand{\\pseudologp}{"+"{:.2f}".format(sum(logps)/len(logps))+"}\n")

# ---- The system that we are analyzing ----

#  Bayes net structure
gY = NetNode(Node('g',['Y'],Node.IN_TREE),[])
fXY = NetNode(Node('F',['X','Y'],Node.IN_TREE),[])
gX = NetNode(Node('g',['X'],Node.IN_TREE),[gY,fXY])
cX = NetNode(Node('cd',['X'],Node.IN_TREE),[gX])

bn = BayesNet()
bn.append(gY)
bn.append(fXY)
bn.append(gX)
bn.append(cX)

# Conditional probabilities for the above net (incomplete---see defaultFunctorVals for remaining cases)
cdM = Rule(Node ('cd', ['X'], 'T') , [Node('g', ['X'], 'M')], 0.6)
cdF = Rule(Node ('cd', ['X'], 'T') , [Node('g', ['X'], 'W')], 0.8)
gW = Rule(Node('g', ['X'], 'W'), [Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'T')], 0.7)
gM = Rule(Node('g', ['X'], 'M'), [Node('g', ['Y'], 'W'), Node('F', ['X', 'Y'], 'T')], 0.3)
ruleList = [cdM,cdF,gW,gM]

# Debugging cases
#gyParents = Rule(Node('g', ['Y'],'M'),[Node('cd',['X'],'T')],0.2)
#gY = Rule(Node('g', ['Y'], 'M'), [], .4)

# Ranges for functors
booleanRange = ['T', 'F']
functorRangeList = [('F', booleanRange), ('g', ['M', 'W']), ('cd', booleanRange)]

# The database
# All constant arguments to functionals must come from the population given in constants
db = Database([Node('F',['anna','bob'],'T'),
              Node('F',['bob','anna'],'T'),
              Node('F',['bob','bob'],'F'),
              Node('F',['anna','anna'],'F'),
              Node('g',['bob'],'M'),
              Node('g',['anna'],'W'),
              Node('cd',['anna'],'T'),
              Node('cd',['bob'],'F')
                   ])

# The constants---a single population, from which all variables are drawn (with replacement)
constants = ['anna', 'bob']

"""
grounding = Grounding([('X','anna'), ('Y','bob')])
print "bob's gender", query (Node('g',['Y'],Node.QUERY), grounding, db)
print "F(anna,bob)", query (Node('F',['X','Y'],Node.QUERY), grounding, db)
print "F(bob,bob)", query (Node('F',['Y','Y'],Node.QUERY), grounding, db)

bn.jointProbs(grounding, db, ruleList)
"""

#print jointProbabilities(constants, db, ruleList, bn)
formatJointTableForLaTeX(jointProbabilities(constants, db, ruleList, bn))
