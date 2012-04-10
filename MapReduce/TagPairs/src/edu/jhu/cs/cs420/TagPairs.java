/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.jhu.cs.cs420;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityMapper;

/**
 *
 * @author dgc30
 */
public class TagPairs {
    public static class Phase1Map extends MapReduceBase implements org.apache.hadoop.mapred.Mapper<Text,Text,Text,Text> {
        
        @Override
        public void map(Text key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            String stringValue = value.toString();
            // replace punctuation with spaces excepts for # and @
            String replaceAll = stringValue.replaceAll("[\\p{Punct}&&[^@#]]", " ");
            for(String word : replaceAll.split("\\s+")) {
                if(word.length() > 0 && word.charAt(0) == '#') {
                    output.collect(key, new Text(word.toLowerCase()));
                }
            }
        }
    }
    
    public static class Phase1Reducer extends MapReduceBase implements org.apache.hadoop.mapred.Reducer<Text,Text,Text,Text> {

        private List<List<String>> pairWiseCombinations(SortedSet<String> set) {
            int resultSize = set.size() * (set.size() - 1) / 2;
            List<List<String>> result = new ArrayList<List<String>>(resultSize);
            
            String[] setArray = set.toArray(new String[set.size()]);
            for(int i = 0; i < setArray.length; i++) {
                for(int j = i + 1; j < setArray.length; j++) {
                    result.add(Arrays.asList(setArray[i], setArray[j]));
                }
            }
            
            return result;
        }
        
        private SortedSet<String> setValues(Iterator<Text> values) {
            SortedSet<String> result = new TreeSet<String>();
            while(values.hasNext()) {
                result.add(values.next().toString());
            }
            return result;
        }
        

        @Override
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            List<List<String>> pairs = pairWiseCombinations(setValues(values));
            for(List<String> pair : pairs) {
                output.collect(new Text(pair.get(0) + "," + pair.get(1)), key);                
            }
        }
    }
    
    public static class Phase2Reducer extends MapReduceBase implements Reducer<Text,Text,Text,Text> {

        @Override
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            // build comma separated user list
            // guaranteed at least one value
            StringBuilder sb = new StringBuilder(values.next().toString());
            while(values.hasNext()) {
                sb.append(",").append(values.next().toString());
            }
            output.collect(key, new Text(sb.substring(0, sb.length())));
        }
        
    }
    
    
    
    
    public static void main(String[] args) throws IOException {
        // Setup phase 1
        JobConf phase1 = new JobConf(TagPairs.class);
        phase1.setJobName("tagpairs-phase1");
        
        phase1.setOutputKeyClass(Text.class);
        phase1.setOutputValueClass(Text.class);
        
        phase1.setMapperClass(Phase1Map.class);
        phase1.setReducerClass(Phase1Reducer.class);
        
        // input format is "user tweet...\n" where user is the key and tweet is 
        // the rest of the line 
        phase1.set("key.value.separator.in.input.line", " ");
        phase1.setInputFormat(KeyValueTextInputFormat.class);
        phase1.setOutputFormat(TextOutputFormat.class);
        
        FileInputFormat.setInputPaths(phase1, new Path(args[0]));
        // todo would be nice to not have to use a temporary path, but I don't 
        // know how to configure hadoop to do this
        FileOutputFormat.setOutputPath(phase1, new Path(args[1]));

        // Setup phase 2
        JobConf phase2 = new JobConf(TagPairs.class);
        phase2.setJobName("tagpairs-phase2");
        
        phase2.setOutputKeyClass(Text.class);
        phase2.setOutputValueClass(Text.class);
        
        phase2.setMapperClass(IdentityMapper.class);
        phase2.setReducerClass(Phase2Reducer.class);
        
        phase2.setInputFormat(KeyValueTextInputFormat.class);
        phase2.setOutputFormat(TextOutputFormat.class);

        // todo would be nice to not have to use a temporary path, but I don't 
        // know how to configure hadoop to do this
        FileInputFormat.setInputPaths(phase2, new Path(args[1]));
        FileOutputFormat.setOutputPath(phase2, new Path(args[2]));

        // Run phase 1 and wait for it to complete
        JobClient.runJob(phase1).waitForCompletion();
        // Run phase 2
        JobClient.runJob(phase2);
        
    }
    
}
