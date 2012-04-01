##
# Phase 2 reducer
import sys
import itertools

def reduce(tagPair, userList):
    '''
    Simply outputs the already accumulated and sorted output of p1.reducer.py
    in the desired format.
    '''
    print "%s, %s : %s" % (tagPair[0], tagPair[1], ' '.join(userList))
        
def main():
    # first read
    (tag1,tag2,user) = sys.stdin.readline().split()
    lastTagPair = (tag1,tag2)
    userList = [user]
    for line in sys.stdin:
        # Accumulate users who tweeted the same pair of tags
        (tag1,tag2,user) = line.split()
        tagPair = (tag1,tag2)
        if tagPair == lastTagPair:
            # just append
            userList.append(user)
        else:
            # time to reduce!
            reduce(lastTagPair, userList)
            lastTagPair = tagPair
            userList = [user]
    # and one last reduce to get the last user in the input
    reduce(lastTagPair, userList)
    
if __name__ == '__main__':
    main()