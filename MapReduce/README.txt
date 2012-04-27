Building the java version requires ant. To build it run "ant jar" in the root of the project directory 

Running the streaming version on amazonaws elastic mapreduce
Phase 1
Input: tweets.large/
Output: <phase_one_output>
Mapper: danielcranford/streaming/p1.mapper.py
Reducer: danielcranford/streaming/p1.reducer.py


Phase 2
Input: <phase_one_ouput>
Ouput: <phase_two_output>
Mapper: danielcranford/streaming/cat.py
Reducer: danielcranford/streaming/p2.reducer.py

Running the java version on amazonaws elastic mapreduce
Jar: danielcranford/java/TagPairs.jar
Arguments: edu.jhu.cs.cs420.TagPairs s3n://tweets.large/ <phase_one_output> <phase_two_output> 


Ouput from successful runs of my code is available at
s3n://danielcranford/output/p2.streaming.large.2
s3n://danielcranford/output/p2.java.large.1

