Running the streaming version on amazonaws elastic mapreduce

Phase 1
Input: tweets.large/
Output: danielcranford/output/p1.streaming.large.<jid>
Mapper: danielcranford/streaming/p1.mapper.py
Reducer: danielcranford/streaming/p1.reducer.py


Phase 2
Input: danielcranford/output/p1.streaming.large.<jid>
Ouput: danielcranford/output/p2.streaming.large.<jid>
Mapper: danielcranford/streaming/cat.py
Reducer: danielcranford/streaming/p2.reducer.py

Running the java version on amazonaws elastic mapreduce
Jar: danielcranford/java/TagPairs.jar
Arguments: edu.jhu.cs.cs420.TagPairs s3n://tweets.large/ s3n://danielcranford/output/p1.java.large.<jid> 
s3n://danielcranford/output/p2.java.large.<jid>
