# Unit tests for JSON server.

# Standard libraries
import json
import unittest

# Libraries that have to have been installed by pip
import requests

# Local modules
from jsonserver import PORT_NUM, TEMPLATE_NAME, POP_NAME, POP_VARS, FUNCTORS
from genBNtable import Grounding, SerializedGraph, Node, NetNode, BayesNet
from genBNtable import Database

DEFINE_URL = 'http://localhost:'+str(PORT_NUM)+'/define'

class TestJSONDefine(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testDefineSingleVarTemplate(self):
        res = _def_bn_one_node()
        self.assertEqual(200, res.status_code)

    def testDefineSingleVarTemplateNullPop(self):
        nn = NetNode(Node('DoesEDM',['A']),[])
        bn = BayesNet()
        bn.append(nn)
        res = requests.post(DEFINE_URL,
                headers={'Content-Type': 'application/json',
                         'Accept': 'application/json'},
                data=json.dumps({TEMPLATE_NAME: bn.serialize().json(),
                                 POP_NAME: {},
                                 FUNCTORS: [('DoesEDM',Database.BOOLEAN_RANGE)]})
        )
        self.assertEqual(200, res.status_code)

    def testDefineIncoherentTemplate(self):
        bn = BayesNet()
        nn = NetNode(Node('g',['X']),[])
        bn.append(nn)
        bn.append(NetNode(Node('g',['Y']),[nn]))
        res = requests.post(DEFINE_URL,
                headers={'Content-Type': 'application/json',
                         'Accept': 'application/json'},
                data=json.dumps({TEMPLATE_NAME: bn.serialize().json(),
                                 POP_NAME: {},
                                 FUNCTORS: [('g',['W','M'])]})
        )
        self.assertEqual(500, res.status_code)
        
        
class TestJSONGround(unittest.TestCase):

    def setUp(self):
        self.url = 'http://localhost:'+str(PORT_NUM)+'/ground'
        self.three_nodes_FR = [('DoesEDM', Database.BOOLEAN_RANGE),
                               ('DuetWith', Database.BOOLEAN_RANGE)]

    def tearDown(self):
        pass

    def test_one_node(self):
        res = _def_bn_one_node()
        assert res.status_code == 200
        res = requests.post(self.url,
                      headers={'Content-Type': 'application/json',
                               'Accept': 'application/json'},
                      data=json.dumps({POP_NAME: {'person': ['aa']},
                                       POP_VARS: [('A', 'person')],
                                       FUNCTORS: self.three_nodes_FR}
                          )
                      )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.headers['Access-Control-Allow-Origin'], '*')
        
        nn = NetNode(Node('DoesEDM',['aa']),[])
        sg = SerializedGraph()
        sg.add(nn)
        
        self.assertEqual(sg.json(), res.json())
    
    def test_three_nodes_one_pop(self):
        res = self._def_bn_three_nodes()
        assert res.status_code == 200

        res = requests.post(self.url,
                      headers={'Content-Type': 'application/json',
                               'Accept': 'application/json'},
                      data=json.dumps({POP_NAME: {'person': ['aa']},
                                       POP_VARS: [('A', 'person'),('B','person')],
                                       FUNCTORS: self.three_nodes_FR})
                      )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.headers['Access-Control-Allow-Origin'], '*')

        den = NetNode(Node('DoesEDM',['aa']),[])
        dun  = NetNode(Node('DuetWith',  ['aa','aa']),[])
        den2 = NetNode(Node('DoesEDM',['aa']),[den,dun])
        sg = SerializedGraph()
        sg.add(den)
        sg.add(dun)
        sg.add(den2)

        self.assertEqual(sg.json(), res.json())
    
    def test_three_nodes_two_pops(self):
        res = self._def_bn_three_nodes()
        assert res.status_code == 200
        pops = {'bigbang': ['t.o.p.', 'gdragon'],
                'browneyedgirls': ['jea', 'miryo']}

        res = requests.post(self.url,
                      headers={'Content-Type': 'application/json',
                               'Accept': 'application/json'},
                      data=json.dumps({POP_NAME: pops,
                                       POP_VARS: [('A', 'bigbang'),('B','browneyedgirls')],
                                       FUNCTORS: self.three_nodes_FR})
                      )
        self.assertEqual(res.status_code, 200)
        self.assertEqual(res.headers['Access-Control-Allow-Origin'], '*')

        sg = SerializedGraph()
        for bb in pops['bigbang']:
            for beg in pops['browneyedgirls']:
                den = NetNode(Node('DoesEDM',[bb]),[])
                dun  = NetNode(Node('DuetWith',  [bb,beg]),[])
                den2 = NetNode(Node('DoesEDM',[beg]),[den,dun])
                sg.add(den)
                sg.add(dun)
                sg.add(den2)

        self.assertEqual(sg.json(), res.json())

    def _def_bn_three_nodes(self):
        den = NetNode(Node('DoesEDM',['A']),[])
        dun  = NetNode(Node('DuetWith',  ['A','B']),[])
        den2 = NetNode(Node('DoesEDM',['B']),[den,dun])
        bn = BayesNet()
        bn.append(den)
        bn.append(dun)
        bn.append(den2)
        res = requests.post(DEFINE_URL,
                headers={'Content-Type': 'application/json',
                         'Accept': 'application/json'},
                data=json.dumps({TEMPLATE_NAME: bn.serialize().json(),
                                 POP_NAME: {'browneyedgirls': ['hyuna'],
                                            'bigbang': ['gdragon']},
                                FUNCTORS: self.three_nodes_FR})
        )
        return res

def _def_bn_one_node():
    nn = NetNode(Node('DoesEDM',['A']),[])
    bn = BayesNet()
    bn.append(nn)
    res = requests.post(DEFINE_URL,
        headers={'Content-Type': 'application/json',
                 'Accept': 'application/json'},
        data=json.dumps({TEMPLATE_NAME: bn.serialize().json(),
                         POP_NAME: {'person': ['hyuna']},
                         FUNCTORS: [('DoesEDM',Database.BOOLEAN_RANGE)]})
        )
    return res
