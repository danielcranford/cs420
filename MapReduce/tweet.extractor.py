#!/usr/bin/env python
# Extracts tweets from the json format into the format expected by the Map/Reduce engine
import sys
import json

def main():
    for line in sys.stdin:
        o = json.loads(line)
        try:
            # json uses unicode strings and some tweets (in foreign languages) have them
            # also, some strings may contain embedded new lines which mess up the line oriented format
            # so use json.dumps to encode them and [1:-1] to strip off the quotes
            print "%s\t%s" % (json.dumps(o['user']['screen_name'])[1:-1], json.dumps(o['text'])[1:-1])
        except KeyError:
            pass # ignore json objects we can't parse

if __name__ == '__main__':
    main()
