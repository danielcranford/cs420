# Extracts tweets from the json format into the format expected by the engine
import sys
import json

def main():
    for line in sys.stdin:
        o = json.loads(line)
        try:
            # json uses unicode strings and some tweets (in foreign languages) have them
            # also, some strings may contain embedded new lines which mess up the line oriented format
            print o['user']['screen_name'].encode('utf-8'), o['text'].encode('utf-8').encode('string-escape')
        except KeyError:
            pass # ignore json objects we can't parse

if __name__ == '__main__':
    main()