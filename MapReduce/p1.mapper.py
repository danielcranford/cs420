#!/usr/bin/env python

##
# Phase 1 mapper
import sys

def removePunctuation(tweet):
    '''
    Removes all punctuation by replacing it with whitespace except for @ and 
    # which signify user mentions and topics
    '''
    # not the most efficient method, but oh well
    for char in ''',./?;:'"\|]}[{=+-_()*&^%$!`~''': tweet = tweet.replace(char, " ")
    return tweet
    
def map(user, tweet):
    '''
    Given an input key (user) and value (tweet), parse the tweet into words.
    For any hashtags (words that begin with '#'), output the user and the 
    hashtag.
    '''
    for word in removePunctuation(tweet).split():
        if word[0] == '#':
            print "%s\t%s" % (user, word.lower())
            
def main():
    for line in sys.stdin:
        # Split line into "user tweet..." format
        try:
            map(*line.split(None,1))
        except:
            pass #skip bad lines

if __name__ == '__main__':
    main()
