"""
Compute joint probabilities and pseudo-likelihood estimates for a Bayes Net.

Use random selection semantics from a database.

Improvements to make:
- Before doing anything, check global consistancy (functor names, constants, ...)
- Currently assumes a single domain. Extending to multiple domains is easy but tedious.
- The data structure passed between atomList and jointProbabilities is too complex.
- Make consistent the choice of __str__ and __repr__ in classes.
- Derive more consistent nomenclature for a term with no value
- Are formulas always conjunctions of literals? If so, is the nomenclature
  consistent in this code?

"""

import copy
import math
import collections
import itertools as IT
import functools as FT

NO_PROB = -1

@FT.total_ordering
class Node(object):
    
    """
    Represent a functor, its arguments, and its value.
    
    Nodes in a Bayes Net structure have unspecified values (IN_TREE), while
    nodes used for querying have QUERY values.
    
    TODO:  Probably should be renamed Literal rather than Node. Or PRV? Only a literal if it has a value.
    """

    IN_TREE = '-'
    QUERY = '?' # TODO: Do we still need this?
    WILD = '*'
    
    def __init__ (self, functor, varList, val=IN_TREE):
        self.functor = functor
        self.varTuple = tuple(varList)
        self.val = val

    def __str__ (self):
        return self.functor+"("+",".join(self.varTuple)+")="+str(self.val)

    def __repr__ (self):
        return str(self)

    def __le__(self, node):
        return (self.functor <= node.functor and
                self.varTuple <= node.varTuple and
                self.val <= node.val)

    def __eq__(self, node):
        return (self._match(self.functor, node.functor) and
                all(self._match(sv, nv) for (sv, nv) in IT.izip (self.varTuple, node.varTuple)) and
                self._match(self.val, node.val))

    def __hash__(self):
        return (hash(self.functor)+hash(self.varTuple)+hash(self.val)) & 0XFFFFFFFF

    def isLiteral(self):
        return self.val != Node.QUERY and self.val != Node.IN_TREE

    def isQuery(self):
        return self.val == Node.QUERY

    def asTerm(self):
        node = copy.deepcopy(self)
        node.val = Node.IN_TREE
        return node

    def popVarList(self):
        pvl = []
        for v in self.varTuple:
            if not isConstant(v):
                pvl.append(v)
        return pvl

    def withResult(self, val, db):
        """ Return copy with the result value set to a value in the functor's range. """
        assert val in db.functorRange(self.functor)
        node = copy.deepcopy(self)
        node.val = val
        return node

    def _match(self, n1, n2):
        """ Match against a wild card or exact value """
        return n1 == Node.WILD or n2== Node.WILD or n1 == n2

@FT.total_ordering
class CP(object):
    
    """ Represent a conditional probability. """

    PRINT_DIGITS = 3
    _probFormat = '{0:.'+str(PRINT_DIGITS)+'f}'

    def __init__ (self, child, parentList, prob):
        self.child = child
        self.parentList = parentList
        self.prob = prob

    def __str__(self):
        return ("P("+str(self.child) +
                _prefNonNull("|", ",".join([str(n) for n in self.parentList]))+")=" +
                CP._probFormat.format(self.prob))

    def __repr__(self):
        return str(self)

    def __le__(self, oCP):
        return (self.child <= oCP.child and
                sorted(self.parentList) <= sorted(oCP.parentList) and
                (self.prob < oCP.prob or
                probEqual(self.prob, oCP.prob)))

    def __eq__(self, oCP):
        return self.child == oCP.child and sameNodeList(self.parentList, oCP.parentList) and probEqual(self.prob, oCP.prob)

@FT.total_ordering
class CPFormula(object):

    """
    Represent a conditional probability formula.

    Immutable so that it can be used in sets and as a dictionary key.
    child is a term or a literal and parentSet is a set of terms or literals.
    """

    def __init__(self, child, parentSet):
        self.child = child
        self.parentSet = frozenset(parentSet)

    def __eq__(self, oCPF):
        return (self.child     == oCPF.child and
                self.parentSet == oCPF.parentSet)

    def __hash__(self):
        return self.child.__hash__() ^ self.parentSet.__hash__()

    def __lt__(self, oCPF):
        return (self.child < oCPF.child or
                sorted(list(self.parentSet)) < sorted(list(oCPF.parentSet)))

    def __str__(self):
        return ("P("+str(self.child) +
                _prefNonNull("|", ",".join([str(n) for n in list(self.parentSet)]))+")")

    def __repr__(self):
        return str(self)

@FT.total_ordering
class Grounding(object):
    
    """ Represent a specific assignment of constants to variables. """
    """ Integrity: None of the variables should match a constant. """
    
    def __init__(self, env):
        self.env = env

    def extended(self, grounding):
        """ Return a new grounding that extends this grounding with another """
        assert set(self._vars()).isdisjoint(set(grounding._vars()))
        resenv = copy.deepcopy(self.env)
        resenv.extend(copy.deepcopy(grounding.env))
        return Grounding(resenv)

    def val(self, var):
        """ Return the value mapped to a population variable by this grounding. """
        if isConstant(var): 
            return var
        for (v, ground) in self.env:
            if v == var:
                return ground
        else:
            raise Exception ("Var not ground: " + var)

    def groundLiteral(self, literal):
        """ Return a ground copy of a literal """
        gndList = []
        for var in literal.varTuple:
            gndList.append(self.val(var))
        return Node (literal.functor, gndList, literal.val)

    def sortedList(self):
        """ Return the mappings in this grounding, sorted by variable name """
        self.env.sort(key=lambda(var, val) : var)
        return self.env

    def _vars(self):
        return [var for (var, val) in self.env]

    def __lt__(self, other):
        tv = [var for (var, val) in self.sortedList()]
        to = [var for (var, val) in other.sortedList()]
        return tv < to

    def __eq__(self, other):
        return self.sortedList() == other.sortedList()

    def __len__(self):
        return len(self.env)

    def __repr__(self):
        return '{' + ", ".join(v[0]+"="+str(v[1]) for v in self.env) + '}'

class Database(object):
    
    """ Represent the database, specifying the functor values for given arguments. """
    
    Ndtup = collections.namedtuple('nd', ['validCount', 'groundingsCount'])

    BOOLEAN_RANGE = ['T', 'F']

    def __init__ (self, attributes, frangelist, constants):
        self.attributes = attributes
        self.functorRangeList = frangelist
        self.constants = constants
        self._integrityCheck()

    def _integrityCheck(self):
        """
        TODO: Check that all nonpredicate functors have an attribute entry for every grounding
        """
        constsUsed = set()
        functorsUsed = set()
        for node in self.attributes:
            functorsUsed.add(node.functor)
            for c in node.varTuple:
                constsUsed.add(c)
        assert constsUsed == set(self.constants)
        fr = set([f for (f,frange) in self.functorRangeList])
        fpreds = set([f for (f,frange) in self.functorRangeList if frange == Database.BOOLEAN_RANGE])
        assert (functorsUsed | fpreds) == fr

    def funcVal(self, node):
        """
        Return the value in this database for a ground term.

        If a literal, ignore its value.
        """
        for n in self.attributes:
            if n.functor == node.functor and all ([p[0]==p[1] for p in IT.izip(n.varTuple,node.varTuple)]):
                return n.val
        else:
            # Predicates absent from the database are presumed False
            if self.functorRange(node.functor) == Database.BOOLEAN_RANGE:
                return 'F'
            raise Exception ("Functor not ground: " + str(node))

    def query(self, lit, grounding):
        """	Return the value of a literal in this database under grounding. """
        return self.funcVal(grounding.groundLiteral(lit))

    def fillValue(self, literal, grounding):
        """ Return a copy of a literal, with the value for its term under grounding and this db. """
        gn = copy.deepcopy(literal)
        gn.val = self.query(gn, grounding)
        return gn

    def defaultProb(self, functor):
        """ Return probability for a value in a functor's range assuming uniform distribution. """
        return 1.0/self.functorRangeSize(functor)

    def functorRange(self, functor):
        """ Look up the range for a functor """
        for (name, range) in self.functorRangeList:
            if functor == name:
                return range
        else:
            raise Exception ("Functor " + functor + " not present in range list")

    def functorRangeSize(self, functor):
        """ Return cardinality of range for a functor. """
        return len(self.functorRange(functor))

    def functorOtherValue(self, functor, val):
        """ Return the other element for functors with a binary range. """
        range = self.functorRange(functor)
        assert len(range) == 2
        if val == range[0]:
            return range[1]
        else:
            return range[0]

    def generateGroundings(self, grounding, vars):
        """ Generate all possible groundings given a list of initial groundings. """
        assert entriesAreUnique(vars)
        
        if len(vars) == 0:
            if len(grounding) == 0:
                return []
            else:
                return [grounding]
        result = []
        for c in self.constants:
            gc = copy.deepcopy(grounding).extended(Grounding([(vars[0], c)]))
            result.extend(self.generateGroundings(gc, vars[1:]))
        return result

    def domain(self, formula):
        """ Return the domain of a formula. """
        varSet = set()
        for lit in formula:
            varSet = varSet | set(lit.popVarList())
        return self.generateGroundings(Grounding([]), list(varSet))

    def nd(self, formula, both=False):
        """ Compute Nd, the number of true instances of formula in this database. """
        if len(formula) == 0:
            if both:
                return Database.Ndtup(0,0)
            else:
                return 0
        if both:
            return self._checkValid(formula)
        else:
            return self._checkValid(formula).validCount

    def pd(self, formula):
        """ Compute Pd, the proportion of true instances of formula in database d. """
        if len(formula) == 0:
            return 1.0
        (cntValid, domSize) = self._checkValid(formula)
        return float(cntValid) / float(domSize)

    def _checkValid(self, formula):
        def matches(lit, gnd):
            return not lit.isLiteral() or self.query(lit,gnd)==lit.val
        pvs = set()
        for lit in formula:
            pvs |= set(lit.popVarList())

        if len(pvs) == 0:
            glst = [Grounding([])]
        else:
            glst = self.generateGroundings(Grounding([]), list(pvs))
        cntValid = sum(all(matches(lit, gnd) for lit in formula)
                       for gnd in glst)
        return Database.Ndtup(cntValid, len(glst))

    def computeGibbs(self, bnthetas, mb):
        """ Return the Gibbs probability for a CPF in a Markov blanket. """
        P = 1.0
        for cpf in mb.listCPF():
            P *= bnthetas[cpf]
        return P

@FT.total_ordering
class NetNode(object):
    
    """
    Represent a node within a Bayes net.

    In addition to the Node for the vertex, this has a list of parent Nodes.
    TODO: Find a better name.
    """
    """ 
    Integrity: A functor should never be repeated with an identical argument list.
    """

    def __init__(self, node, parents):
        self.node = node
        if not all(isinstance(nn, NetNode) for nn in parents):
            raise Exception ('One or more parents is not a NetNode')
        self.parents = tuple(parents)

    def __str__(self):
        return str(self.node)+" <- ("+", ".join([str(sn) for sn in self.parents])+")"
    def __eq__(self, other):
        return self.node == other.node and self.parents == other.parents

    def __le__(self, other):
        return (self.node <= other.node or
                self.parents <= other.parents
               )

    def popVarList(self):
        pvarSet = set()
        pvarSet |= set(self.node.popVarList())
        for n in self.parents:
            pvarSet |= set(n.popVarList())
        return list(pvarSet)

    def genRangeDict(self, db):
        """ Return ranges for every parent. """
        #TODO: Revise so that it builds a list, not a dict
        #print 'genRangeDict', self
        ranges = collections.defaultdict(list)
        for netn in self.parents:
            #print 'netn',type(netn),netn
            ranges[netn.node] = db.functorRange(netn.node.functor)
        return dict(ranges)

    def parentNodes(self):
        return [nn.node for nn in self.parents]

    def genParentVals(self, db):
        rangeList = self.genRangeDict(db).items()
        def gp(parentList, rangeList):
            if len(rangeList) == 0:
                return [parentList]
            result = []
            parent, range = rangeList[0]
            for r in range:
                plist = copy.deepcopy(parentList)
                plist.append(parent.withResult(r,db))
                result.extend(gp(plist,rangeList[1:]))
            return result
        return gp([], rangeList)

    def genThetas(self, db):
        thetaBar = []
        parentVals = self.genParentVals(db)
        for j in parentVals:
            formula = [copy.deepcopy(self.node)]
            formula.extend(j)
            jValid = db.nd(formula)
            for k in db.functorRange(self.node.functor):
                node = self.node.withResult(k, db)
                formula = [node]
                formula.extend(j)
                valid = db.nd(formula)
                #print k, j, valid, jValid
                thetaBar.append(CP(node, j, valid/float(jValid)))
        return thetaBar

class BayesNet(object):
    
    """ Represent a Bayes net. """

    def __init__(self, uniformDefault=False):
        """
        uniformDefault = False => require all probabilities to be in the table,
        uniformDefault = True  => if probability not in table, assume uniform distribution
        TODO: uniformDefault should be made an argument of jointProbabilities, not
           a property of the net.
        """
        self.nodes = []
        self.uniformDefault = uniformDefault

    def append(self, netNode):
        for child in netNode.parents:
            if child not in self.nodes:
                raise Exception ("BayesNet node " + str(netNode) + " added but child " + str (child) + " not already in net")
        self.nodes.append(netNode)

    def getNode(self, node):
        for nn in self.nodes:
            if nn.node == node:
                return nn
        else:
            raise Exception('node not in Bayes Net: ' + str(node))

    '''
    def getNodeCPF(self, node, cpf):
        """ Extract the CPF of a node from a CPF specifying values for an entire family. """
        netn = self.getNode(node)
        return CPFormula(Node('g',['X'],'M'),{})
    '''

    def jointProbs(self, grounding, db, ruleSet):
        probs = []
        joint = 1.0
        for node in self.nodes:
            #print "searching",node
            gn = db.fillValue(node.node, grounding)
            #print "filled node", gn
            gcn = [db.fillValue(n.node, grounding) for n in node.parents]
            #print "filled parents", gcn
            p = cpMatch(ruleSet, db, gn, gcn)
            if p == NO_PROB:
                if self.uniformDefault:
                    p = defaultProb(gn.functor)
                else:
                    raise Exception ("No probability rule found for query " + str(node) + " {ground to: " +
                                     str(gn) + " <- (" + ", ".join([str(parent) for parent in gcn]) +
                                     ")} (and no defaults permitted)")
            probs.append((gn, p))
            joint *= p
        probs.append(joint)
        probs.append(math.log(joint))
        return probs

    def variableList(self):
        vars = set()
        for n in self.nodes:
            for v in n.node.popVarList():
                vars.add(v)
        return sorted(list(vars))

    def genThetas(self, db):
        """ TODO: NetNode.genThetas should return dict of CPFormula, so no conversion required here. """
        cpfDict = {}
        for netn in self.nodes:
            #print 'netn', netn
            for cp in netn.genThetas(db):
                cpf = CPFormula(cp.child, frozenset(cp.parentList))
                cpfDict[cpf] = cp.prob
        return cpfDict

    def isCompatibleMB(self, mb):
        """ Raise exception if the Markov blanket is incompatible with this net. """
        mbt = set()
        focus = self.getNode(mb.focusNode)
        mbt.add(CPFormula(focus.node,focus.parentNodes()))
        for nn in self.nodes:
            if nn is not focus:
                if focus in nn.parents:
                    mbt.add(CPFormula(nn.node,nn.parentNodes()))
        mbterms = mb.asCPFTerms()
        if mbt != mbterms:
            raise Exception(str(mbt-mbterms)+'\n'+str(mbterms-mbt))

class MarkovBlanket(object):
    
    """
    Represent the Markov Blanket of a literal, with every term assigned a value.
    
    TODO: Integrity constraint: a term can occur multiple times (say as itself,
    and in its role as parent of its child). Every occurence must have the same
    value.
    """

    def __init__(self, focusNode, cpfSet):
        self.focusNode = focusNode
        self.cpfSet = cpfSet

    def __str__(self):
        return 'MB('+str(focusNode)+': '+str(list(self.cpfSet))+')'

    def listCPF(self):
        """ Return a list of all the CPFormulas in this blanket. """
        return list(self.cpfSet)

    def asCPFTerms(self):
        """ Return the set of CPFormulas as terms rather than literals. """
        cpfs = set()
        for cpf in self.cpfSet:
            pset = set(p.asTerm() for p in cpf.parentSet)
            cpfs.add(CPFormula(cpf.child.asTerm(), pset))
        return cpfs

def cpMatch(cpList, db, node, parents):  
    """
    Locate the value for a ground node and its parents in a CP list, return NO_PROB if not found.

    For functors with binary ranges, when all parents match but child's value
    does not, return 1-prob for other value.
    """
    def getProb (node):
        for cp in cpList:
            #print cp
            if (cp.child == node and
                len(cp.parentList)==len(parents) and
                all([n[0] == n[1] for n in zip(cp.parentList,parents)])):
                #print "winning eq", [n for n in zip(cp.parentList,parents)]
                return cp.prob
        else:
            return NO_PROB
    
    prob = getProb (node)
    if prob == NO_PROB and db.functorRangeSize(node.functor) == 2:
        tn = copy.copy(node)
        tn.val = db.functorOtherValue(tn.functor, tn.val)
        prob =  getProb (tn)
        #print "second try prob", tn, prob
        if prob != NO_PROB:
            return 1 - prob
        else:
            return prob
    return prob

def _prefNonNull(pref, s):
    ''' Prefix string s with pref if s non-null. '''
    if len(s) == 0:
        return s
    return pref+s

def sameNodeList(nl1, nl2):
    """ Return True if two lists have the same Node entries. """
    if len(nl1) != len(nl2):
        return False
    for n in nl1:
        if n not in nl2:
            return False
    return True

EPSILON = .00001

def probEqual(p1, p2):
    """ Compare two probabilities within a standard tolerance. """
    return abs(p1-p2) < EPSILON

def isConstant(name):
    return name[0].islower()

def atomList(joints):
    """ Return the atoms, derived from the first entry in the joint probability table. """
    assert len(joints) > 0
    first = joints[0]
    functorList = first[1][:-2] # Second element of row, last two elements of that are joint prob and log prob
    atomList = []
    for (node,_) in functorList:
        atomList.append(node.functor+"("+",".join(node.varTuple)+")")
    return atomList

def jointProbabilities(db, cpList, bn):
    """ Compute the joint probabilities for all combinations of values. """
    vars = bn.variableList()
    combs = db.generateGroundings(Grounding([]), vars)
    joints = []
    for grounding in combs:
        joints.append((grounding, bn.jointProbs(grounding, db, cpList)))
    return (vars, atomList(joints), joints)

def entriesAreUnique(collection):
    """
    Return true if every item in a collection occurs exactly once.

    This algorithm is as (in)efficient as set conversion. I've isolated
    it here so that improving it here improves all callers.
    """
    return len(set(collection)) == len(collection)
