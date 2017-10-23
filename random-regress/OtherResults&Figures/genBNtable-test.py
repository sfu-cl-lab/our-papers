'''
    Unit test for genBNtable.
    TODO:
    - Convert to native features of unittest package: assertAlmostEqual,
        assertItemsEqual, etc.
'''

import unittest
import random as RND
import StringIO as SIO

from contextlib import closing

# Convert file to use GBN for all references
# Or not ...
from genBNtable import *
import genBNtable as GBN

class TestClassProbabilities(unittest.TestCase):
    
    def setUp(self):
        #  Bayes net structure
        gY = NetNode(Node('g',['Y'],Node.IN_TREE),[])
        fXY = NetNode(Node('F',['X','Y'],Node.IN_TREE),[])
        gX = NetNode(Node('g',['X'],Node.IN_TREE),[gY,fXY])
        cX = NetNode(Node('cd',['X'],Node.IN_TREE),[gX])

        self.bn = BayesNet()
        self.bn.append(gY)
        self.bn.append(fXY)
        self.bn.append(gX)
        self.bn.append(cX)

        # Conditional probabilities for the above net
        self.cpList = []
        self.cpList.append(CP(Node ('g', ['Y'], 'M'), [], 0.5))
        self.cpList.append(CP(Node ('F', ['X', 'Y'], 'T'), [], 0.1))

        self.cpList.append(CP(Node('g', ['X'], 'W'), [Node('g', ['Y'], 'W'), Node('F', ['X', 'Y'], 'T')], 0.7))
        self.cpList.append(CP(Node('g', ['X'], 'M'), [Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'T')], 0.7))
        self.cpList.append(CP(Node('g', ['X'], 'M'), [Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'F')], 0.5))
        self.cpList.append(CP(Node('g', ['X'], 'W'), [Node('g', ['Y'], 'W'), Node('F', ['X', 'Y'], 'F')], 0.5))

        self.cpList.append(CP(Node ('cd', ['X'], 'T') , [Node('g', ['X'], 'M')], 0.6))
        self.cpList.append(CP(Node ('cd', ['X'], 'T') , [Node('g', ['X'], 'W')], 0.8))

        # Ranges for functors
        genderRange = ['M', 'W']
        functorRangeList = [('F', Database.BOOLEAN_RANGE), ('g', genderRange), ('cd', Database.BOOLEAN_RANGE)]

        # The constants---a single population, from which all variables are drawn (with replacement)
        constants = ['anna', 'bob']

        # The database
        # All constant arguments to functionals must come from the population given in constants
        self.db = Database([Node('F',['anna','bob'],'T'),
              Node('F',['bob','anna'],'T'),
              #Node('F',['bob','bob'],'F'),
              #Node('F',['anna','anna'],'F'),
              Node('g',['bob'],'M'),
              Node('g',['anna'],'W'),
              Node('cd',['anna'],'T'),
              #Node('cd',['bob'],'F')
                   ],
            functorRangeList,
            constants)

    def tearDown(self):
        pass

    def testFormatJointTable(self):
        with closing(SIO.StringIO()) as out:
            self.formatJointTableForLaTeX(jointProbabilities(self.db, self.cpList, self.bn), out)
            self.assertEqual(out.getvalue(), r"""\begin{tabular}{|c|c|c|c|c|c||c|c|}
\hline
X & Y & g(Y) & F(X,Y) & g(X) & cd(X) & Joint $p$ & ln~$p$ \\ \hline
anna & anna & W (0.5) & F (0.9) & W (0.5) & T (0.8) & 0.180 & -1.71\\
anna & bob & M (0.5) & T (0.1) & W (0.3) & T (0.8) & 0.012 & -4.42\\
bob & anna & W (0.5) & T (0.1) & M (0.3) & F (0.4) & 0.006 & -5.12\\
bob & bob & M (0.5) & F (0.9) & M (0.5) & F (0.4) & 0.090 & -2.41\\
\ldots & \ldots & \ldots & \ldots & \ldots & \ldots & \ldots & \ldots\\
\hline
\end{tabular}
""")

    def formatJointTableForLaTeX(self, joints, out):
        """
        Given a joint probability table, format it for LaTeX.
        This function will have to be tailored for every paper.

        This function simply generates the {tabular} part of the table. The prologue and epilogue,
        including the caption and label, must be specified in the including file.
        """
        (varList, atoms, probs) = joints
        cols = len(varList) + len (probs[0][1])
        out.write ("\\begin{tabular}{|" + "|".join(["c"]*(cols-2))+"||c|c|}\n")
        out.write ("\\hline\n")
        # Table header
        out.write (" & ".join(varList) + " & " + " & ".join([a for a in atoms]) + " & Joint $p$ & ln~$p$ \\\\ \\hline\n")
        # Table rows
        logps = []
        for (grounding, probs) in probs:
            out.write (" & ".join([val for (var, val) in grounding.sortedList()]) + " & " +
                " & ".join([str(n.val)+" ({:.1f})".format(p) for (n,p) in probs[:-2]]) +
                " & {:.3f}".format(probs[-2]) + " & {:.2f}".format(probs[-1]) + "\\\\\n")
            logps.append(probs[-1])
        # A line to indicate there are further entries in the DB
        out.write(" & ".join(["\ldots"]*cols) + "\\\\\n")
        # Close environment
        out.write ("\\hline\n\\end{tabular}\n")

class TestGrounding(unittest.TestCase):
    
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testGroundingLenNull(self):
        self.assertEqual(len(Grounding([])), 0)

    def testGroundingLenMult(self):
        self.assertEqual(len(Grounding([('X', 'anne'), ('Y', 'bob')])), 2)

    def testGroundingSortedListNull(self):
        self.assertEqual(Grounding([]).sortedList(), [])

    def testGroundingSortedListMultiple(self):
        self.assertEqual(Grounding([('Z', 2), ('A', 500), ('P', -3)]).sortedList(),
                         [('A', 500), ('P', -3), ('Z', 2)])

    def testGroundingEqNull(self):
        self.assertTrue(Grounding([]) == Grounding([]))

    def testGroundingEqSingle(self):
        self.assertTrue(Grounding([('X', 'anne')]) == Grounding ([('X', 'anne')]))

    def testGroundingEqDoubleSameOrd(self):
        self.assertTrue(Grounding([('X', 'anne'), ('Y', 'bob')]) ==
                        Grounding([('X', 'anne'), ('Y', 'bob')]))

    def testGroundingEqDoubleDiffOrd(self):
        self.assertTrue(Grounding([('X', 'anne'), ('Y', 'bob')]) ==
                        Grounding([('Y', 'bob'),  ('X', 'anne')]))

    def testGroundingValVar(self):
        self.assertTrue(Grounding([('X', 'anne')]).val('X'), 'anne')

    def testGroundingValVarAbsent(self):
        with self.assertRaises(Exception):
            Grounding([('X', 'anne')]).val('Y')

    def testGroundingValConstSelf(self):
        self.assertTrue(Grounding([('X', 'anne')]).val('baby'), 'baby')

    def testGroundingExtendNull(self):
        self.assertEqual(Grounding([('X', 'anne')]).extended(Grounding([])),
                         Grounding([('X', 'anne')]))

    def testGroundingExtendSingle(self):
        self.assertEqual(Grounding([('Y', 'anne')]).extended(Grounding([('X', 'bob')])),
                          Grounding([('X', 'bob'), ('Y', 'anne')]))

    def testGroundingExtendNonDisjoint(self):
        with self.assertRaises(AssertionError):
            Grounding([('X', 'hello'), ('Y', 'goodbye')]).extended(Grounding([('Y', 'au revoir')]))

    def testGroundingExtendedNewValue(self):
        first = Grounding([('X', 'first')])
        ext = Grounding([('Y', 'ext')])
        firstExt = first.extended(ext)
        extExt = ext.extended(Grounding([('Z', 'extExt')]))

        self.assertEqual(first, Grounding([('X', 'first')]))
        self.assertEqual(ext, Grounding([('Y', 'ext')]))
        self.assertEqual(firstExt, Grounding([('X', 'first'), ('Y', 'ext')]))
        self.assertEqual(extExt, Grounding([('Y', 'ext'), ('Z', 'extExt')]))

class TestUtils(unittest.TestCase):

    def setUp(self):
        self.epsilon  = .00001

    def tearDown(self):
        pass

    def testProbEqualZero(self):
        self.assertTrue(GBN.probEqual(0.0, 0.0))

    def testProbEqualOneZero(self):
        self.assertFalse(GBN.probEqual(0.0, 1.0))

    def testProbEqualOneOneFract(self):
        self.assertTrue(GBN.probEqual(1.0, 1.0+(0.99*self.epsilon)))

    def testProbEqualOneOneEps(self):
        self.assertFalse(GBN.probEqual(1.0, 1.0+self.epsilon))

    def testNLequalNull(self):
        self.assertTrue(GBN.sameNodeList([], []))

    def testNLequalNullNon(self):
        self.assertFalse(GBN.sameNodeList([Node('g', ['X'])], []))

    def testNLequalDiffSingle(self):
        self.assertFalse(GBN.sameNodeList([Node('g', ['X'])], [Node('g', ['Y'])]))

    def testNLEqualSingle(self):
        self.assertTrue(GBN.sameNodeList([Node('g', ['X'])], [Node('g', ['X'])]))

    def testNLEqualMultiple(self):
        self.assertTrue(GBN.sameNodeList([Node('g', ['X']), Node('F', ['X', 'Y'])],
                                         [Node('F', ['X', 'Y']), Node('g', ['X'])]))

    def testNLEqualMultipleDiff(self):
        self.assertFalse(GBN.sameNodeList([Node('g', ['X']), Node('F', ['X', 'Y'])],
                                         [Node('F', ['X', 'Y']), Node('g', ['Z'])]))

class TestNode(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testLtNiladic(self):
        self.assertLess(Node('f', []), Node('g', []))

    def testLtMonadic(self):
        self.assertLess(Node('f', ['X']), Node('f', ['Y']))

    def testGtMonadic(self):
        self.assertGreater(Node('f', ['Y']), Node('f', ['X']))

    def testLtDyadic(self):
        self.assertLess(Node('f', ['X', 'Y']), Node('f', ['X', 'Z']))

    def testGtDyadic(self):
        self.assertGreater(Node('f', ['Y', 'X']), Node('f', ['W', 'X']))

    def testLtVal(self):
        self.assertLess(Node('f', ['X', 'Y'], 'A'), Node('f', ['X', 'Y'], 'B'))

    # Wild card equality
    def testEqWildFunctor(self):
        self.assertEqual(Node('g', ['X']), Node(Node.WILD, ['X']))

    def testEqWildSingleVar(self):
        self.assertEqual(Node('g', ['X']), Node('g', [Node.WILD]))

    def testEqWildValLeft(self):
        self.assertEqual(Node('g', ['X'], Node.WILD), Node('g', ['X'], 'T'))

    def testEqWildValRight(self):
        self.assertEqual(Node('g', ['X'], 'T'), Node('g', ['X'], Node.WILD))

    def testEqWildSomeVars(self):
        self.assertEqual(Node('F', ['X', 'Y'], 'T'), Node('F', [Node.WILD, Node.WILD], 'T'))
    
    # Conversions
    def testAsTree(self):
        self.assertEqual(Node('g',['X'],'W').asTerm(), Node('g',['X']))

class TestCP(unittest.TestCase):
    
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testEqualNoParents(self):
        self.assertEqual(CP(Node('g', ['X']), [], 0.0),
                         CP(Node('g', ['X']), [], 0.0))

    def testNotEqualFunctors(self):
        self.assertNotEqual(CP(Node('g', ['X']), [], 0.0),
                            CP(Node('h', ['X']), [], 0.0))

    def testNotEqualVars(self):
        self.assertNotEqual(CP(Node('g', ['X']), [], 0.0),
                            CP(Node('g', ['Y']), [], 0.0))

    def testNotEqualProbs(self):
        self.assertNotEqual(CP(Node('g', ['X']), [], 0.0),
                            CP(Node('g', ['X']), [], 0.1))

    def testNotEqualParents(self):
        self.assertNotEqual(CP(Node('g', ['X']), [], 0.0),
                            CP(Node('g', ['X']), [Node('F', ['X', 'Y'])], 0.0))
    def testEqualParents(self):
        self.assertEqual(CP(Node('g', ['X']), [Node('g', ['Z']), Node('F', ['X', 'Y'])], 0.0),
                         CP(Node('g', ['X']), [Node('F', ['X', 'Y']), Node('g', ['Z'])], 0.0))

    def testLtNoParentsFunc(self):
        self.assertLess(CP(Node('g', ['X']), [], 0.0),
                        CP(Node('h', ['X']), [], 0.0))

    def testLtNoParentsVars(self):
        self.assertLess(CP(Node('g', ['X']), [], 0.0),
                        CP(Node('g', ['Y']), [], 0.0))        

    def testLtNoParentsProb(self):
        self.assertLess(CP(Node('g', ['X']), [], 0.0),
                        CP(Node('g', ['X']), [], 0.1))

    def testLtParents(self):
        self.assertLess(CP(Node('g', ['X']), [Node('c', ['X']), Node('b', ['X'])], 0.0),
                        CP(Node('g', ['X']), [Node('b', ['X']), Node('c', ['Y'])], 0.0))

    def testGtNoParents(self):
        self.assertGreater(CP(Node('g', ['Y']), [], 0.0),
                           CP(Node('h', ['X']), [], 0.0))

    def testGtParents(self):
        self.assertGreater(CP(Node('g', ['X']), [Node('c', ['Y']), Node('b', ['X'])], 0.0),
                           CP(Node('g', ['X']), [Node('b', ['X']), Node('c', ['X'])], 0.0))

class TestCPFormula(unittest.TestCase):

    def setUp(self):
        self.gX = Node('g', ['X'])
        self.gY = Node('g', ['Y'])
        self.fXY = Node('F', ['X', 'Y'])
        self.fXZ = Node('F', ['X', 'Z'])

    def tearDown(self):
        pass

    def fullyEqual(self, a, b):
        return a == b and a.__hash__() == b.__hash__()

    def fullyNotEqual(self, a, b):
        return a != b and a.__hash__() != b.__hash__()

    def testEqBasic(self):
        a = CPFormula(self.gX,{})
        b = CPFormula(self.gX,{})
        self.assertTrue(self.fullyEqual(a,b))

    def testNeBasic(self):
        a = CPFormula(self.gX,{})
        b = CPFormula(self.gY,{})
        self.assertTrue(self.fullyNotEqual(a, b))

    def testEqParents(self):
        a = CPFormula(self.gX, {self.fXY})
        b = CPFormula(self.gX, {self.fXY})
        self.assertTrue(self.fullyEqual(a, b))
        
    def testNeParents(self):
        a = CPFormula(self.gX, {self.fXY})
        b = CPFormula(self.gX, {self.fXZ})
        self.assertTrue(self.fullyNotEqual(a, b))

    def testNotEqualParents(self):
        self.assertTrue(self.fullyNotEqual(CPFormula(Node('g', ['X']), {}),
                                           CPFormula(Node('g', ['X']), {Node('F', ['X', 'Y'])})))

    def testEqualParents(self):
        self.assertTrue(self.fullyEqual(CPFormula(Node('g', ['X']), {Node('g', ['Z']), Node('F', ['X', 'Y'])}),
                                        CPFormula(Node('g', ['X']), {Node('F', ['X', 'Y']), Node('g', ['Z'])})))

    def testLtNoParentsFunc(self):
        self.assertLess(CPFormula(Node('g', ['X']), {}),
                        CPFormula(Node('h', ['X']), {}))

    def testLtNoParentsVars(self):
        self.assertLess(CPFormula(Node('g', ['X']), {}),
                        CPFormula(Node('g', ['Y']), {}))        

    def testLtParents(self):
        self.assertLess(CPFormula(Node('g', ['X']), {Node('c', ['X']), Node('b', ['X'])}),
                        CPFormula(Node('g', ['X']), {Node('b', ['X']), Node('c', ['Y'])}))

    def testGtNoParents(self):
        self.assertGreater(CPFormula(Node('g', ['Y']), {}),
                           CPFormula(Node('h', ['X']), {}))

    def testGtParents(self):
        self.assertGreater(CPFormula(Node('g', ['X']), {Node('c', ['Y']), Node('b', ['X'])}),
                           CPFormula(Node('g', ['X']), {Node('b', ['X']), Node('c', ['X'])}))

class TestMarkovBlanket(unittest.TestCase):

    def setUp(self):
        self.gX = Node('g',['X'])
        self.fXY = Node('f',['X', 'Y'])
        self.gY = Node('g',['Y'])

    def tearDown(self):
        pass

    def testSingleAsCPFTerms(self):
        mb = MarkovBlanket(
            Node('g',['X']),
            {CPFormula(Node('g',['X'],'M'),{})})
        self.assertEqual(mb.asCPFTerms(),{CPFormula(Node('g',['X']),{})})

    def testParentsAsCPFTerms(self):
        mb = MarkovBlanket(
            Node('g',['X']),
            {CPFormula(Node('g',['X'],'M'),{Node('F',['X','Y'],'T')})})
        self.assertEqual(mb.asCPFTerms(),
                         {CPFormula(Node('g',['X']),{Node('F',['X','Y'])})})

    def testMultipleTerms(self):
        mb = MarkovBlanket(
            Node('g',['X']),
            {CPFormula(Node('g',['X'],'M'),{Node('F',['X','Y'],'T')}),
             CPFormula(Node('F',['X','Y'],'T'),{Node('cd',['Y'],'T')})})
        self.assertEqual(mb.asCPFTerms(),
                         {CPFormula(Node('g',['X']),{Node('F',['X','Y'])}),
                          CPFormula(Node('F',['X','Y']),{Node('cd',['Y'])})})

class TestDatabaseConstructor(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testCreatesEmpty(self):
        Database([], [], [])
        self.assertTrue(True) # If we completed constructor, test succeeded

    def testExcessConstant(self):
        with self.assertRaises(AssertionError):
            Database([], [], ['a'])

    def testMissingConstant(self):
        with self.assertRaises(AssertionError):
            Database([Node('g', ['hao'], 'W')], [('g', ['M', 'W'])], [])

    def testExcessPredicate(self):
        Database([], [('cd', Database.BOOLEAN_RANGE)], [])
        self.assertTrue(True) # If we completed constructor, test succeeded

    def testExcessNonPredicate(self):
        with self.assertRaises(AssertionError):
           Database([], [('g', ['M', 'W'])], [])

    """ 
    See TODO in Database._integrityCheck.
    def testIncompleteNonPredicate(self):
        with self.assertRaises(AssertionError):
            Database([Node('g',['anne'],'W'), Node('cd',['bob'],'T'), Node('cd',['anne'],'F')],
                     [('g',['M','W']), ('cd',Database.BOOLEAN_RANGE)],
                     ['anne','bob'])
    """

class TestDatabaseTwoConstants(unittest.TestCase):

    def setUp(self):
        # Constants
        constants = ['anne', 'bob']
        
        # Ranges for functors
        genderRange = ['M', 'W']
        functorRangeList = [('F', Database.BOOLEAN_RANGE),
                            ('g', genderRange),
                            ('cd', Database.BOOLEAN_RANGE)]

        self.db = Database([Node('F', ['anne','bob'], 'T'),
                            Node('g', ['bob'], 'M'),
                            Node('g',  ['anne'], 'W')],
                            functorRangeList,
                            constants)

    def tearDown(self):
        pass

    def testGenGroundsNull(self):
        self.assertEqual(self.db.generateGroundings(Grounding([]), []), []);

    def testGenGroundsNonUnique(self):
        with self.assertRaises(AssertionError):
            self.db.generateGroundings(Grounding([]), ['X', 'X'])

    def testGenGroundsSingle(self):
        result = self.db.generateGroundings(Grounding([]), ['X'])
        self.assertEqual(len(result), 2)
        self.assertIn(Grounding([('X', 'anne')]), result)
        self.assertIn(Grounding([('X', 'bob')]), result)

    def testGenGroundsDouble(self):
        result = self.db.generateGroundings(Grounding([]), ['X', 'Y'])
        self.assertEqual(len(result), 4)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'anne')]), result)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'bob')]),  result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'anne')]), result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'bob')]),  result)

    def testGenGroundsTriple(self):
        result = self.db.generateGroundings(Grounding([]), ['X', 'Y', 'Z'])
        self.assertEqual(len(result), 8)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'anne'), ('Z', 'anne')]), result)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'anne'), ('Z', 'bob')]),  result)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'bob'),  ('Z', 'anne')]), result)
        self.assertIn(Grounding([('X', 'anne'), ('Y', 'bob'),  ('Z', 'bob')]),  result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'anne'), ('Z', 'anne')]), result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'anne'), ('Z', 'bob')]),  result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'bob'),  ('Z', 'anne')]), result)
        self.assertIn(Grounding([('X', 'bob'),  ('Y', 'bob'),  ('Z', 'bob')]),  result)

    def testDomainNull(self):
        self.assertEqual(self.db.domain([]), [])

    def testDomainSingle(self):
        self.assertTrue(sameElements(self.db.domain([Node('g', ['X'], Node.QUERY)]),
                                     self.db.generateGroundings(Grounding([]), ['X'])))

    def testDomainDouble(self):
        self.assertTrue(sameElements(self.db.domain([Node('g', ['Y'], Node.QUERY),
                                                     Node('F', ['X', 'Y'], Node.QUERY)]),
                                     self.db.generateGroundings(Grounding([]), ['Y', 'X'])))

    def testAbsentPredicate(self):
        self.assertEqual(self.db.funcVal(Node('cd', ['anne'])), 'F')

    def testGibbsEmptyPredicate(self):
        """
        Test the T-valued case of a predicate that never appears in the database.

        Absence implies the predicate (and hence the literal) is universally false.
        """
        bn = BayesNet()
        cdX = Node('cd', ['X'])
        bn.append(NetNode(cdX, []))
        cpf = CPFormula(cdX.withResult('T', self.db), [])
        mb = MarkovBlanket(cdX,{cpf})
        bn.isCompatibleMB(mb)
        bnth = bn.genThetas(self.db)
        self.assertTrue(probEqual(self.db.computeGibbs(bnth, mb), 0.0))

    def testGibbsEmptyPredicateFalse(self):
        """
        Test the F-valued case of a predicate that never appears in the database.

        Absence implies the predicate is universally false and hence the literal
        is universsaly true.
        """
        bn = BayesNet()
        cdX = Node('cd', ['X'])
        bn.append(NetNode(cdX, []))
        cpf = CPFormula(cdX.withResult('F', self.db), [])
        mb = MarkovBlanket(cdX,{cpf})
        bn.isCompatibleMB(mb)
        bnth = bn.genThetas(self.db)
        self.assertTrue(probEqual(self.db.computeGibbs(bnth, mb), 1.0))

class TestGibbsRegressionCPS(unittest.TestCase):

    def setUp(self):
        self.db = genSmallDB()

    def tearDown(self):
        pass

    '''
    def testSingleFormula(self):
        bn = BayesNet()
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        bn.append(ngX)
        bnth = bn.genThetas(self.db)

        target Node of MB probably has to be grounded---causes problems in
        BayesNet getNode(). Or maybe target node has to be expressed in
        terms of BayesNet---i.e., using population variables.

        For the proposed GroundedMB, if a CPF has a Node that is a TREE,
        that indicates the computation ranges over all literals.

        This probably means I have to fix/modify isCompatibleMB for
        the case of GroundedMBs.
        mb = GroundedMB(
                Node('g',['X']),
                Grounding([('X','anne')]),
                {CPFormula(Node('g',['X'],'W'), {})})
        gibbs = self.db.computeGibbsRegression(bn, bnth, mb)
    '''
        

    '''
    def testSample(self):
        bn = BayesNet()
        # ... create BN ...

        bnth = bn.genThetas(self.db)
        
        mb = GroundedMB(Node('g',['X']),
          Grounding([('X','sam')]),
          {CPFormula(Node('g',['X'],'W'),
                     {CPFormula(Node('g',['Y'],'W'),{}),
                      CPFormula(Node('F',['X','Y'],'T'),{})}),
           CPFormula(Node('cd',['X']), # Note no value
                     {CPFormula(Node('g',['X'],'W'),{})})})
                    
        
        gibbs = self.db.computeGibbsRegression(bn, bnth, mb)
    '''

class TestNdPdFixedDB(unittest.TestCase):
    
    def setUp(self):
        self.db = genSmallDB()

    def testNDNull(self):
        self.assertEqual(self.db.nd([]), 0)

    def testNDNullBoth(self):
        self.assertEqual(self.db.nd([], both=True), (0,0))

    def testPDNull(self):
        self.assertEqual(self.db.pd([]), 1.0)

    def testNDFalseNoValue(self):
        self.assertEqual(self.db.nd([Node('g', ['anne'])]), 1)

    def testNDFalse(self):
        self.assertEqual(self.db.nd([Node('g', ['anne'], 'M')]), 0)

    def testPDFalse(self):
        self.assertEqual(self.db.pd([Node('g', ['anne'], 'M')]), 0.0)

    def testNDTrue(self):
        self.assertEqual(self.db.nd([Node('g', ['anne'], 'W')]), 1)

    def testPDTrue(self):
        self.assertEqual(self.db.pd([Node('g', ['anne'], 'W')]), 1.0)

    def testNDGenderNoValue(self):
        self.assertEqual(self.db.nd([Node('g', ['X'])]), 3)        

    def testNDGender(self):
        self.assertEqual(self.db.nd([Node('g', ['X'], 'M')]), 1)

    def testPDGender(self):
        self.assertEqual(self.db.pd([Node('g', ['X'], 'M')]), 1.0/3.0)

    def testNDFriendNoValue(self):
        self.assertEqual(self.db.nd([Node('F', ['X', 'Y'])]), 9)

    def testNDFriend(self):
        self.assertEqual(self.db.nd([Node('F', ['X', 'Y'], 'T')]), 5)

    def testNDFriendBoth(self):
        self.assertEqual(self.db.nd([Node('F', ['X', 'Y'], 'T')], both=True), (5,9))

    def testPDFriend(self):
        self.assertEqual(self.db.pd([Node('F', ['X', 'Y'], 'T')]), 5.0/9.0)

    def testNDMult(self):
        self.assertEqual(self.db.nd([Node('cd', ['X'], 'F'),
                                     Node('g', ['X'], 'W')]),
                                    1)

    def testPDMult(self):
        self.assertEqual(self.db.pd([Node('cd', ['X'], 'F'),
                                     Node('g', ['X'], 'W')]),
                                    1.0/3.0)

    def testNDThreeNoValue(self):
        self.assertEqual(self.db.nd([Node('cd', ['Y']),
                                     Node('g', ['Y'], 'M'),
                                     Node('F', ['bob', 'Y'], 'T')]),
                                    1)

    def testNDThree(self):
        self.assertEqual(self.db.nd([Node('cd', ['Y'], 'T'),
                                     Node('g', ['Y'], 'W'),
                                     Node('F', ['bob', 'Y'], 'T')]),
                                    1)

    def testNDThreeBoth(self):
        self.assertEqual(self.db.nd([Node('cd', ['Y'], 'T'),
                                     Node('g', ['Y'], 'W'),
                                     Node('F', ['bob', 'Y'], 'T')],
                                     both=True),
                                    (1,3))

    def testPDThree(self):
        self.assertEqual(self.db.pd([Node('cd', ['Y'], 'T'),
                                     Node('g', ['Y'], 'W'),
                                     Node('F', ['bob', 'Y'], 'T')]),
                                    1.0/3.0)

class TestGibbs(unittest.TestCase):

    def setUp(self):
        self.constants = ['anne', 'bob', 'mary']
        # Ranges for functors
        self.genderRange = ['M', 'W']
        self.functorRangeList = [('F', Database.BOOLEAN_RANGE), ('g', self.genderRange), ('cd', Database.BOOLEAN_RANGE)]
        self.smallDB = genSmallDB()

    def tearDown(self):
        pass

    def testNoParentNoChild(self):
        bn = BayesNet()
        gX = Node('g',['X'])
        bn.append(NetNode(gX, []))
        db = self.smallDB
        bnth = bn.genThetas(db)
        mb = MarkovBlanket(gX,{CPFormula(Node('g',['X'],'M'), {})})
        bn.isCompatibleMB(mb)
        self.assertTrue(probEqual(db.computeGibbs(bnth, mb), 1.0/3.0))

    def testTwoParentsNoChild(self):
        bn = BayesNet()
        fXY = NetNode(Node('F',['X','Y']),[])
        gY = NetNode(Node('g',['Y']),[])
        gX = Node('g',['X'])
        bn.append(fXY)
        bn.append(gY)
        bn.append(NetNode(gX, [fXY,gY]))
        bnth = bn.genThetas(self.smallDB)

        mb = MarkovBlanket(gX, {CPFormula(Node('g',['X'],'M'),
                                              {Node('F',['X','Y'],'T'),
                                               Node('g',['Y'],'M')})})
        bn.isCompatibleMB(mb)
        self.assertTrue(
            probEqual(self.smallDB.computeGibbs(bnth, mb), 0.5))

    def testNoParentOneChild(self):
        bn = BayesNet()
        fXY = Node('F', ['X','Y'])
        nfXY = NetNode(fXY,[])
        ngY = NetNode(Node('g',['Y']),[nfXY])
        bn.append(nfXY)
        bn.append(ngY)
        bnth = bn.genThetas(self.smallDB)
        
        mb = MarkovBlanket(
                 fXY,
                 {CPFormula(Node('F',['X','Y'],'T'), {}),
                  CPFormula(Node('g',['Y'],'W'),{Node('F',['X','Y'],'T')})})
        bn.isCompatibleMB(mb)
        
        gibbs = self.smallDB.computeGibbs(bnth, mb)
        self.assertTrue(probEqual(gibbs, 1.0/3.0))

    def testParentChildrenCoparents(self):
        gX = Node('g',['X'])
        cdX = Node('cd',['X'])
        fXY = Node('F',['X','Y'])
        gY = Node('g',['Y'])
        cdY = Node('cd',['Y'])

        ngX = NetNode(gX,[])
        ncdX = NetNode(cdX,[])
        nfXY = NetNode(fXY,[ngX])
        ngY = NetNode(gY,[nfXY])
        ncdY = NetNode(cdY,[nfXY, ncdX])

        
        tgX = Node('g',['X'],'M')
        tcdX = Node('cd',['X'],'F')
        tfXY = Node('F',['X','Y'],'T')
        cpffXY = CPFormula(tfXY,{tgX})
        tgY = Node('g',['Y'],'W')
        cpfgY = CPFormula(tgY,{tfXY})
        tcdY = Node('cd',['Y'],'F')
        cpfcdY = CPFormula(tcdY,{tfXY,tcdX})
        
        bn = BayesNet()
        bn.append(ngX)
        bn.append(ncdX)
        bn.append(nfXY)
        bn.append(ngY)
        bn.append(ncdY)
        bnth = bn.genThetas(self.smallDB)
        
        mb = MarkovBlanket(
                 fXY,
                 {cpffXY, cpfgY, cpfcdY})
        bn.isCompatibleMB(mb)

        gibbs = self.smallDB.computeGibbs(bnth, mb)
        self.assertTrue(probEqual(gibbs, 20.0/75.0))

class TestThetaEstSmallDB(unittest.TestCase):
    
    def setUp(self):
        self.gY = NetNode(Node('g', ['Y']), [])
        self.Fxy = NetNode(Node('F', ['X', 'Y']), [])
        self.FxyGy = NetNode(Node('F', ['X', 'Y']), [self.gY])
        self.gX = NetNode(Node('g', ['X']), [self.Fxy, self.gY])

        self.db = genSmallDB()

    def tearDown(self):
        pass

    def testThetasEmpty(self):
        bn = BayesNet()
        self.assertEqual(bn.genThetas(self.db), {})

    def testThetasSingle(self):
        bn = BayesNet()
        bn.append(self.gY)
        self.assertTrue(sameElements(bn.genThetas(self.db),
                                     {CPFormula(Node('g', ['Y'], 'M'), []) : 0.3333333333,
                                      CPFormula(Node('g', ['Y'], 'W'), []) : 0.6666666667}))

    def testThetasDouble(self):
        bn = BayesNet()
        bn.append(self.gY)
        bn.append(self.FxyGy)
        self.assertTrue(sameElements(bn.genThetas(self.db),
                                     {CPFormula(Node('g', ['Y'], 'M'), []) : 0.3333333333,
                                      CPFormula(Node('g', ['Y'], 'W'), []) : 0.6666666667,
                                      CPFormula(Node('F', ['X', 'Y'], 'T'), [Node('g', ['Y'], 'M')]) : 0.6666667,
                                      CPFormula(Node('F', ['X', 'Y'], 'T'), [Node('g', ['Y'], 'W')]) : 0.5,
                                      CPFormula(Node('F', ['X', 'Y'], 'F'), [Node('g', ['Y'], 'M')]) : 0.3333333,
                                      CPFormula(Node('F', ['X', 'Y'], 'F'), [Node('g', ['Y'], 'W')]) : 0.5
                                     }))

class TestNetNode(unittest.TestCase):
    
    def setUp(self):
        self.db = genSmallDB()

    def tearDown(self):
        pass

    def testNetNodePopVarsNoParent(self):
        self.assertEqual(NetNode(Node('g', ['Z']), []).popVarList(), ['Z'])

    def testNetNodePopVarsNoParentDouble(self):
        self.assertTrue(sameElements(NetNode(Node('F', ['Q', 'R']), []).popVarList(), ['Q', 'R']))

    def testNetNodePopVarsParents(self):
        self.assertTrue(sameElements(NetNode(Node('F', ['Q', 'R']),
                                             [NetNode(Node('g', ['Q']), []),
                                              NetNode(Node('F', ['Q', 'S']), [])]).popVarList(), ['Q', 'R', 'S']))

    def testValuesOrphan(self):
        res = NetNode(Node('g', ['X']), ()).genRangeDict(self.db)
        test = {}
        self._equalDicts(res, test)

    def testNonNetNodeParent(self):
        with self.assertRaises(Exception):
            NetNode(Node('g', ['X']), [Node('F', ['X', 'Y'])])

    def testValuesOneParent(self):
        res = NetNode(Node('g', ['X']), [NetNode(Node('F', ['X', 'Y']),[])]).genRangeDict(self.db)
        test = {Node('F', ['X', 'Y']) : ['T', 'F']}
        self._equalDicts(res, test)

    def testValuesTwoParents(self):
        res = NetNode(Node('g', ['X']), (NetNode(Node('F', ['X', 'Y']),[]), NetNode(Node('F', ['Z', 'Y']),[]))).genRangeDict(self.db)
        test = {Node('F', ['X', 'Y']) : ['T', 'F'], Node('F', ['Z', 'Y']) : ['T', 'F']}
        self._equalDicts(res, test)

    def testGenParentsOrphan(self):
        self.assertEqual(NetNode(Node('g', ['X']), ()).genParentVals(self.db), [[]])

    def testGenParentsSingle(self):
        self.assertTrue(sameElements(NetNode(Node('g', ['X']), (NetNode(Node('F', ['X', 'Y']),[]),)).genParentVals(self.db),
                                    [[Node('F', ['X', 'Y'], 'T')], [Node('F', ['X', 'Y'], 'F')]]))

    def testGenParentsDouble(self):
        self.assertTrue(sameElements(NetNode(Node('g', ['X']), [NetNode(Node('F', ['X', 'Y']),[]),
                                                                NetNode(Node('F', ['X', 'Z']),[])]).genParentVals(self.db),
                                     [[Node('F', ['X', 'Y'], 'T'), Node('F', ['X', 'Z'], 'T')],
                                      [Node('F', ['X', 'Y'], 'T'), Node('F', ['X', 'Z'], 'F')],
                                      [Node('F', ['X', 'Y'], 'F'), Node('F', ['X', 'Z'], 'T')],
                                      [Node('F', ['X', 'Y'], 'F'), Node('F', ['X', 'Z'], 'F')]]))


    def _equalDicts(self, res, test):
        self.assertEqual(len(res), len(test))
        self.assertDictContainsSubset(test, res)

    def testParentsNone(self):
        nn = NetNode(Node('g',['X']),[])
        self.assertEqual(nn.parentNodes(),[])

    def testParentsTwo(self):
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        fXY = Node('F',['X','Y'])
        nfXY = NetNode(fXY,[])
        cdY = Node('cd',['Y'])
        ncdY = NetNode(cdY,[ngX,nfXY])
        self.assertEqual(ncdY.parentNodes(),[gX,fXY])
        
    def testThetaOrphan(self):
        self.assertTrue(sameElements(NetNode(Node('g', ['X']), []).genThetas(self.db),
                                     [CP(Node('g', ['X'], 'M'), [], 0.33333333),
                                      CP(Node('g', ['X'], 'W'), [], 0.66666666)]))

    def testThetaGroundLiteral(self):
        self.assertTrue(sameElements(NetNode(Node('g', ['anne']), []).genThetas(self.db),
                                     [CP(Node('g', ['anne'], 'M'), [], 0.0),
                                      CP(Node('g', ['anne'], 'W'), [], 1.0)]))

    def testThetaParent(self):
        self.assertTrue(sameElements(NetNode(Node('g', ['X']), [NetNode(Node('F', ['X', 'Y']),[])]).genThetas(self.db),
                                     [CP(Node('g', ['X'], 'M'), [Node('F', ['X', 'Y'], 'T')], 0.4),
                                      CP(Node('g', ['X'], 'W'), [Node('F', ['X', 'Y'], 'T')], 0.6),
                                      CP(Node('g', ['X'], 'M'), [Node('F', ['X', 'Y'], 'F')], 0.25),
                                      CP(Node('g', ['X'], 'W'), [Node('F', ['X', 'Y'], 'F')], 0.75)]))

class TestBayesNet(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testGetNodeSingle(self):
        bn = BayesNet()
        nn = NetNode(Node('g',['X']),[])
        bn.append(nn)
        self.assertEqual(id(bn.getNode(Node('g',['X']))), id(nn))

    def testGetNodeOneChild(self):
        bn = BayesNet()
        parent = NetNode(Node('F',['X','Y']),[])
        child = NetNode(Node('g',['X']),[parent])
        bn.append(parent)
        bn.append(child)
        self.assertEqual(id(bn.getNode(Node('F',['X','Y']))), id(parent))

    def testGetChild(self):
        bn = BayesNet()
        parent = NetNode(Node('F',['X','Y']),[])
        child = NetNode(Node('g',['X']),[parent])
        bn.append(parent)
        bn.append(child)
        self.assertEqual(id(bn.getNode(Node('g',['X']))), id(child))

    def testGetMiddle(self):
        bn = BayesNet()
        grandparent = NetNode(Node('cd',['X']),[])
        parent = NetNode(Node('F',['X','Y']),[grandparent])
        child = NetNode(Node('g',['X'],), [parent])
        bn.append(grandparent)
        bn.append(parent)
        bn.append(child)
        self.assertEqual(id(bn.getNode(Node('F',['X','Y']))), id(parent))

    # Compatibility of a Markov Blanket with the net

    def testSingleNode(self):
        bn = BayesNet()
        n = Node('g', ['X'])
        bn.append(NetNode(n,[]))
        mb = MarkovBlanket(n,{CPFormula(Node('g',['X'],'W'),{})})
        bn.isCompatibleMB(mb)

    def testExcessNode(self):
        bn = BayesNet()
        n = Node('g', ['X'])
        bn.append(NetNode(n,[]))
        mb = MarkovBlanket(n,{CPFormula(Node('g',['X'],'W'),{}),
                                 CPFormula(Node('cd',['X'],'T'),{})})
        with self.assertRaises(Exception):
            bn.isCompatibleMB(mb)

    def testTwoParents(self):
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        gY = Node('g',['Y'])
        ngY = NetNode(gY,[])
        fXY = Node('F',['X','Y'])
        nfXY = NetNode(fXY,[ngX,ngY])

        bn = BayesNet()
        bn.append(ngX)
        bn.append(ngY)
        bn.append(nfXY)

        mb = MarkovBlanket(
            fXY,
            {CPFormula(Node('F',['X','Y'],'T'),
                       {Node('g',['Y'],'W'),
                        Node('g',['X'],'M')})})

        bn.isCompatibleMB(mb)

    def testChildAndCoparent(self):
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        gY = Node('g',['Y'])
        ngY = NetNode(gY,[])
        fXY = Node('F',['X','Y'])
        nfXY = NetNode(fXY,[ngX,ngY])

        bn = BayesNet()
        bn.append(ngX)
        bn.append(ngY)
        bn.append(nfXY)

        mb = MarkovBlanket(
            gX,
            {CPFormula(Node('F',['X','Y'],'T'),
                       {Node('g',['Y'],'W'),
                        Node('g',['X'],'M')}),
             CPFormula(Node('g',['X'],'W'),{})})

        bn.isCompatibleMB(mb)

    def testMissingCoparent(self):
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        gY = Node('g',['Y'])
        ngY = NetNode(gY,[])
        fXY = Node('F',['X','Y'])
        nfXY = NetNode(fXY,[ngX,ngY])

        bn = BayesNet()
        bn.append(ngX)
        bn.append(ngY)
        bn.append(nfXY)

        mb = MarkovBlanket(
            gX,
            {CPFormula(Node('F',['X','Y'],'T'),
                       {Node('g',['Y'],'W')}),
             CPFormula(Node('g',['X'],'W'),{})})

        with self.assertRaises(Exception):
            bn.isCompatibleMB(mb)

    def testExcessConditional(self):
        gX = Node('g',['X'])
        ngX = NetNode(gX,[])
        gY = Node('g',['Y'])
        ngY = NetNode(gY,[])
        fXY = Node('F',['X','Y'])
        nfXY = NetNode(fXY,[ngX,ngY])

        bn = BayesNet()
        bn.append(ngX)
        bn.append(ngY)
        bn.append(nfXY)

        mb = MarkovBlanket(
            gX,
            {CPFormula(Node('F',['X','Y'],'T'),
                       {Node('g',['Y'],'W'),
                        Node('cd',['X'],'T')}),
             CPFormula(Node('g',['X'],'W'),{})})

        with self.assertRaises(Exception):
            bn.isCompatibleMB(mb)

    '''
    # Locating a NetNode's CPFormula within a family CPFormula
    
    def testSingletonGetCPFGivenFamilyCPF(self):
        bn = BayesNet()
        nn = NetNode(Node('g',['X']),[])
        bn.append(nn)
        self.assertEqual(bn.getNodeCPF(bn.getNode(Node('g',['X'])),
                                       CPFormula(Node('g',['X'],'M'),{})),
                         CPFormula(Node('g',['X'],'M'),{}))

    def testOneParentGivenFamilyCPF(self):
        bn = BayesNet()
        cdX = NetNode(Node('cd',['X']),[])
        gX = NetNode(Node('g',['X']),[cdX])
        bn.append(cdX)
        bn.append(gX)
        self.assertEqual(bn.getNodeCPF(bn.getNode(Node('g',['X'])),
                                       CPFormula(Node('g',['X']),{Node('cd',['X'])})),
                         CPFormula(Node('g',['X']),{Node('cd',['X'])}))
    '''

class TestTable1(unittest.TestCase):

    """
    Regenerate the values in Table 1 of the random regression paper.
    """
    
    def setUp(self):
        constants = ['anna', 'bob']
        genderRange = ['M', 'W']
        functorRangeList = [('F', Database.BOOLEAN_RANGE), ('g', genderRange), ('cd', Database.BOOLEAN_RANGE)]

        nodeList = [Node('g', ['anna'], 'W'),
                    Node('g', ['bob'], 'M'),
                    Node('cd', ['anna'], 'T'),
                    #Node('cd', ['bob'], 'F'),
                    Node('F', ['anna', 'bob'], 'T'),
                    Node('F', ['bob', 'anna'], 'T')
            ]

        self.db = Database(nodeList, functorRangeList, constants)

    def testNDLine1(self):
        formula = [Node('g', ['X'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'T')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=1, groundingsCount=4))

    def testPDLine1(self):
        formula = [Node('g', ['X'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'T')]
        self.assertEqual(self.db.pd(formula), 0.25)

    def testNDline2(self):
        formula = [Node('g', ['X'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'F')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=0, groundingsCount=4))

    def testPDline2(self):
        formula = [Node('g', ['X'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['X', 'Y'], 'F')]
        self.assertEqual(self.db.pd(formula), 0.0)

    def testNDline3(self):
        formula = [Node('cd', ['X'], 'T'), Node('g', ['X'], 'W')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=1, groundingsCount=2))

    def testPDline3(self):
        formula = [Node('cd', ['X'], 'T'), Node('g', ['X'], 'W')]
        self.assertEqual(self.db.pd(formula), 0.5)

    def testNDline4(self):
        formula = [Node('g', ['anna'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['anna', 'Y'], 'T')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=1, groundingsCount=2))

    def testPDline4(self):
        formula = [Node('g', ['anna'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['anna', 'Y'], 'T')]
        self.assertEqual(self.db.pd(formula), 0.5)

    def testNDline5(self):
        formula = [Node('g', ['anna'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['anna', 'Y'], 'F')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=0, groundingsCount=2))

    def testPDline5(self):
        formula = [Node('g', ['anna'], 'W'), Node('g', ['Y'], 'M'), Node('F', ['anna', 'Y'], 'F')]
        self.assertEqual(self.db.pd(formula), 0.0)

    def testNDline6(self):
        formula = [Node('cd', ['anna'], 'T'), Node('g', ['anna'], 'W')]
        self.assertEqual(self.db.nd(formula, both=True), Database.Ndtup(validCount=1, groundingsCount=1))

    def testPDline6(self):
        formula = [Node('cd', ['anna'], 'T'), Node('g', ['anna'], 'W')]
        self.assertEqual(self.db.pd(formula), 1.0)
        
class TestInstanceProbabilitiesGeneratedDB(unittest.TestCase):
    
    def setUp(self):
        TOTAL_PERSON = 10
        constants = ['sam']
        for i in range(TOTAL_PERSON - len(constants)):
            constants.append('p'+str(i))

        # Ranges for functors
        genderRange = ['M', 'W']
        functorRangeList = [('F', Database.BOOLEAN_RANGE), ('g', genderRange), ('cd', Database.BOOLEAN_RANGE)]

        # Build tables
        pM = .51
        pCD_M = .6
        pCD_W = .8
        pWWT = .55
        pMMT = .63
        pMMF = .55
        pWWF = .45

        gender = {}
        nodeList = []
        for p in constants:
            if RND.random() <= pM:
                gender[p] = 'M'
                nodeList.append(Node('g', [p], 'M'))
                if RND.random() <= pCD_M:
                    nodeList.append(Node('cd', [p], 'T'))
                else:
                    nodeList.append(Node('cd', [p], 'F'))
            else:
                gender[p] = 'W'
                nodeList.append(Node('g', [p], 'W'))
                if RND.random() <= pCD_W:
                    nodeList.append(Node('cd', [p], 'T'))
                else:
                    nodeList.append(Node('cd', [p], 'F'))

        for p in constants:
            for pf in constants:
                if gender[p] == 'M' and gender[pf] == 'M':
                    if RND.random() <= pMMT:
                        nodeList.append(Node('F', [p, pf], 'T'))
                    else:
                        nodeList.append(Node('F', [p, pf], 'F'))
                if gender[p] == 'W' and gender[pf] == 'W':
                    if RND.random() <= pWWT:
                        nodeList.append(Node('F', [p, pf], 'T'))
                    else:
                        nodeList.append(Node('F', [p, pf], 'F'))

        self.db = Database(nodeList, functorRangeList, constants)

def sameElements(c1, c2):
    '''
    Check that two collections contain the same elements, regardless of order.
    
    Both collections must be single-level. Elements may be repeated,
    but must occur the same number of times in each collection.
    '''
    #print 'left ', sorted(list(c1))
    #print 'right', sorted(list(c2))
    return sorted(list(c1)) == sorted(list(c2))

def sameCPFDicts(ut, d1, d2):
    """
    Return True if d1 == d2 within the probability tolerance.

    d1 and d1 are dicts whose keys are CPFormulas and whose values are probabilities.
    Probability tolerance is defined by genBNtable.probEqual()
    ut is a UnitTest.
    """
    ut.assertEqual(d1.keys(), d2.keys())
    for k in d1:
        ut.assertTrue(probEqual(d1[k], d2[k]))

def genSmallDB():
    """ Generate a small database. """
    constants = ['anne', 'bob', 'mary']
    # Ranges for functors
    genderRange = ['M', 'W']
    functorRangeList = [('F', Database.BOOLEAN_RANGE), ('g', genderRange), ('cd', Database.BOOLEAN_RANGE)]

    return Database([Node('g', ['anne'], 'W'),
                        Node('g', ['mary'], 'W'),
                        Node('g', ['bob'], 'M'),
                            
                        # This instance of Friend is reflexive and symmetric
                        Node('F', ['anne', 'bob'], 'T'),
                        Node('F', ['bob', 'anne'], 'T'),
                        #Node('F', ['anne', 'mary'], 'F'),
                        #Node('F', ['mary', 'anne'], 'F'),
                        #Node('F', ['bob', 'mary'], 'F'),
                        #Node('F', ['mary', 'bob'], 'F'),
                        Node('F', ['anne', 'anne'], 'T'),
                        Node('F', ['bob', 'bob'], 'T'),
                        Node('F', ['mary', 'mary'], 'T'),
                            
                        Node('cd', ['anne'], 'T'),
                        #Node('cd', ['bob'], 'F'),
                        #Node('cd', ['mary'], 'F')
                        ],
                        functorRangeList,
                        constants)

if __name__ == '__main__':
    unittest.main()
