import math
def nCr(n,r):
	"""returns binomial coefficient for n items"""
	f = math.factorial
	return f(n)/f(r)/f(n-r)


def test_conjecture(m):
	"""returns 1 if the conjecture holds for m, 0 otherwise"""
	closedform = m * 2**(m-1)
	print "Closed Form for", m, "=", closedform 
	BinomialTerms = map(lambda l: l * nCr(m,l), range(1,m+1))
	#sum over binomial coeff * size of set
	return closedform == sum(BinomialTerms)
 
def test_upperbound(M):
    """tests the conjecture for everything from 1 to M"""
    for m in range(M):
        if not test_conjecture(m):
            print m, "is a counterexample"
            break
    else:
        print "works for 1 through", M
        
                 

