/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.jhu.cs.cs420;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

/**
 *
 * @author dgc30
 */
public class WordCount {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private Text word = new Text();
        
        @Override
        public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> out, Reporter rprtr) throws IOException {
            String line = value.toString();
            StringTokenizer st = new StringTokenizer(line);
            while(st.hasMoreTokens()) {
                word.set(st.nextToken());
                out.collect(word, ONE);
            }
        }
    }
    
    public static final class Reduce extends MapReduceBase implements Reducer<Text,IntWritable,Text,IntWritable> {

        @Override
        public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> out, Reporter rprtr) throws IOException {
            int sum = 0;
            while(values.hasNext()) {
                sum += values.next().get();
            }
            out.collect(key, new IntWritable(sum));
        }
    }
    
    public static void main(String[] args) throws IOException {
        JobConf jobConf = new JobConf(WordCount.class);
        jobConf.setJobName("wordcount");
        
        jobConf.setOutputKeyClass(Text.class);
        jobConf.setOutputValueClass(IntWritable.class);
        
        jobConf.setMapperClass(Map.class);
        jobConf.setReducerClass(Reduce.class);
        
        jobConf.setInputFormat(TextInputFormat.class);
        jobConf.setOutputFormat(TextOutputFormat.class);
        
        FileInputFormat.setInputPaths(jobConf, new Path(args[0]));
        FileOutputFormat.setOutputPath(jobConf, new Path(args[1]));
        
        JobClient.runJob(jobConf);
    }
    
}
