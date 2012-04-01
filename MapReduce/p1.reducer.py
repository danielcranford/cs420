##
# Phase 1 reducer
import sys
import itertools

def reduce(user, tagList):
    '''
    Generates all possible pairs of hashtags tweeted by a user given the 
    sorted output of p1.mapper.py
    '''
    # Enumerate pairs of hash tags tweeted by the current user
    # throw tagList into a set to remove duplicates
    for (tag1, tag2) in itertools.combinations(set(tagList), 2):
        print tag1, tag2, user
        
def main():
    # first read
    (user,tag) = sys.stdin.readline().split()
    lastUser = user
    tagList = [tag]
    for line in sys.stdin:
        # Accumulate tags from the same user
        (user, tag) = line.split()
        if user == lastUser:
            # just append
            tagList.append(tag)
        else:
            # time to reduce!
            reduce(lastUser, tagList)
            lastUser = user
            tagList = [tag]
    # and one last reduce to get the last user in the input
    reduce(lastUser, tagList)
    
if __name__ == '__main__':
    main()