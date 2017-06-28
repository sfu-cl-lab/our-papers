import unittest

from utils import MutIterList

class TestMutListIter(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testEmpty(self):
        a = MutIterList([])
        with self.assertRaises(StopIteration):
            a.__iter__().next()
        self.assertEqual(0,len(a))

    def testSingle(self):
        a = MutIterList([1])
        c = []
        for b in a:
            c.append(b)
        self.assertEqual([1], c)
        self.assertEqual(1, len(a))

    def testSingleDel(self):
        a = MutIterList([1])
        self.assertEqual(1,len(a))
        c = []
        for b in a:
            if b == 1:
                a.delItem(0)
            else:
                c.append(b)
        self.assertEqual([], c)
        self.assertEqual(0,len(a))

    def testDelAfter(self):
        a = MutIterList(range(5))
        self.assertEqual(5,len(a))
        c = []
        for b in a:
            if b == 1:
                a.delItem(2)
            c.append(b)
        self.assertEqual([0,1,3,4],c)
        self.assertEqual(4,len(a))

    def testDoubleDel(self):
        a = MutIterList([0,1])
        self.assertEqual(2,len(a))
        c = []
        for b in a:
            if b == 0:
                a.delItem(0)
            else:
                c.append(b)
        self.assertEqual([1], c)
        self.assertEqual(1,len(a))

    def testLastDel(self):
        a = MutIterList(range(5))
        self.assertEqual(5,len(a))
        c = []
        for b in a:
            if b == 4:
                a.delItem(4)
            else:
                c.append(b)
        self.assertEqual(range(4), c)
        self.assertEqual(4,len(a))

    def testTripleDel(self):
        a = MutIterList([0,1,2])
        self.assertEqual(3,len(a))
        c = []
        for b in a:
            if b == 1:
                a.delItem(1)
            else:
                c.append(b)
        self.assertEqual([0,2], c)
        self.assertEqual(2,len(a))
        
    def testMultConcurrentIter(self):
        a = MutIterList(range(3))
        b = a.__iter__()
        b.next()
        c = a.__iter__()
        with self.assertRaises(AssertionError):
            c.next()

    def testMultSeqIter(self):
        a = MutIterList(range(4))
        for b in a:
            if b == 2:
                a.delItem(2)

        c = []
        for d in a:
            c.append(d)
        self.assertEqual([0,1,3], c)
