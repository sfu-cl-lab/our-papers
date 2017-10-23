"""
   List that can have members deleted while iterating.
   At most one iterator can be active at one time.
"""
class MutIterList(object):
    def __init__(self, l):
        self.l = l
        self.next = -1

    def __iter__(self):
        assert self.next == -1
        self.next = 0
        while True:
            if self.next >= len(self.l):
                self.next = -1
                return
            else:
                self.next +=  1
                yield self.l[self.next - 1]

    def __len__(self):
        return len(self.l)

    def delItem(self, i):
        del self.l[i]
        if self.next > i:
            self.next -= 1
