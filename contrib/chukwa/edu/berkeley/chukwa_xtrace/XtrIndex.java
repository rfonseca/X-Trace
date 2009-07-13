package edu.berkeley.chukwa_xtrace;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecord;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecordKey;
import org.apache.hadoop.chukwa.extraction.engine.Record;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import edu.berkeley.xtrace.reporting.Report;

/**
 * Builds a start-end index for xtrace graphs.
 * 
 * Input is a sequence file, with task ID as byteswritable for key
 * and an ArrayWritable of Texts for value; one Report per Text.
 * 
 *  Output is a bytesWritable for key [task ID]
 *  and a an ArrayWritable of Texts for value.
 *  Each text is the distribution for a single start/stop pair
 *
 */
public class XtrIndex extends Configured implements Tool {
  
  /**
   * Hadoop docs say to do this if you pass an ArrayWritable to reduce.
   */
  public static class TextArrayWritable extends ArrayWritable {
      public TextArrayWritable() { super(Text.class); } 
    } 
 
  public static class MapClass extends Mapper<BytesWritable, ArrayWritable, BytesWritable, TextArrayWritable>  {
    
    @Override
    protected void map(BytesWritable key, ArrayWritable value, 
        Mapper<BytesWritable, ArrayWritable,BytesWritable, TextArrayWritable>.Context context)
        throws IOException, InterruptedException 
    {
      Map<String, Report> reports = new LinkedHashMap<String, Report>();
      
      Writable[] repts = value.get();
      if(repts.length == 0 || !(repts[0] instanceof Text)) {
        System.out.println("error: bad input.");
        return; //bail out more drastically
      }
      Text[] repts_as_text = (Text[]) repts;
      for(Text t: repts_as_text) {
        Report r = Report.createFromString(t.toString());
        reports.put(r.getMetadata().getOpIdString(), r);
      }
      
      Text[] indexed = indexGraph(reports);
      TextArrayWritable output = new TextArrayWritable();
      output.set(indexed);
      
      context.write(key, output);
    }

  }
  
  
  
  /** 
   * Indexes a set of reports, using Start and End tags
   * output is a list of entries of the form:
   *   A: time1,time2,time3
   *   
   *   If no matches, will return an empty array
   */
  @SuppressWarnings("unchecked")
  public static Text[] indexGraph(Map<String, Report> reports) {
    org.apache.commons.collections.MultiMap index = new 
    org.apache.commons.collections.MultiHashMap();
      //map from start tag to opIds of nodes containing the ends
    
    
    for(Map.Entry<String, Report> report: reports.entrySet()) {
      Report start = report.getValue();
      List<String> starts = start.get("Start");
      if(starts != null) {
        for(String s: starts) {
          Report end = findMatchingEnd(reports, start, s);
          if(end == null)
            continue;
          List<String> endTL = end.get("Timestamp");
          List<String> staTL = start.get("Timestamp");
          if(staTL != null && endTL != null && staTL.size() > 0 && endTL.size() > 0) {
            
            //FIXME: perhaps parse more cleverly?
            double startT = Double.parseDouble(staTL.get(0));
            double endT = Double.parseDouble(endTL.get(0));
            
            Long diff = new Long( (long) (1000 * (endT - startT)));
            index.put(s, diff);
          }
        }
      }
    }
    
    Text[] out = new Text[index.size()];
    int i = 0;
    for(Object k: index.keySet()) {
      StringBuilder sb = new StringBuilder();
      sb.append(k.toString());
      sb.append(' ');
      Collection coll = (Collection) index.get(k);
      for(Object v: coll) {
        assert v instanceof Long: "how did a non-Long get into my collection?";
        sb.append(v.toString());
        sb.append(",");
      }
      sb.deleteCharAt(sb.length() -1);
      Text t = new Text(sb.toString());
      out[i++] = t;
    }
    
    return out;
  }
  
  //do a BFS find closest report to start with endTag
  static Report findMatchingEnd(Map<String, Report> reports,
      Report start, String endTag) {
    
    LinkedList<Report> bfsQ = new LinkedList<Report>();
    Set<String> seen = new HashSet<String>();
    bfsQ.add(start);
    
    while(!bfsQ.isEmpty()) {
      Report cur = bfsQ.poll();
      List<String> ends = cur.get("End");
      if(ends != null && ends.contains(endTag))
        return cur;

      List<String> outlinks = start.get(XtrExtract.OUTLINK_FIELD);
      if(outlinks == null)
        return null;
      for(String s: outlinks) {
        if(seen.contains(s))
          continue;
        else
          seen.add(s);
        Report r = reports.get(s);
        if(r != null)
          bfsQ.add(r);
      }
    }
    return null;
  }

  @Override
  public int run(String[] arg) throws Exception {
    Job extractor = new Job(getConf());
    extractor.setMapperClass(MapClass.class);
    //no reduce, just identity

    extractor.setJobName("x-trace indexer");
    extractor.setJarByClass(this.getClass());
    
    extractor.setMapOutputKeyClass(BytesWritable.class);
    extractor.setMapOutputValueClass(TextArrayWritable.class);
    
    extractor.setOutputKeyClass(BytesWritable.class);
    extractor.setOutputValueClass(TextArrayWritable.class);
    
    extractor.setInputFormatClass(SequenceFileInputFormat.class);
    extractor.setOutputFormatClass(SequenceFileOutputFormat.class);
    FileInputFormat.setInputPaths(extractor, new Path(arg[0]));
    FileOutputFormat.setOutputPath(extractor, new Path(arg[1]));
    System.out.println("looks OK.  Submitting.");
    extractor.submit();
//    extractor.waitForCompletion(false);
    return 0;

  }

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(),
        new XtrExtract(), args);
    System.exit(res);
  }

}
