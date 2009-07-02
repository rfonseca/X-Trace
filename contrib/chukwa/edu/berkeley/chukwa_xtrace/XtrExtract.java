package edu.berkeley.chukwa_xtrace;

import org.apache.hadoop.chukwa.extraction.demux.processor.mapper.AbstractProcessor;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecord;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecordKey;
import org.apache.hadoop.chukwa.extraction.engine.Record;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import java.io.IOException;
import java.util.*;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.Path;

import edu.berkeley.chukwajobs.MRExtractChunks;
import edu.berkeley.xtrace.reporting.Report;
import edu.berkeley.xtrace.*;

/**
 * MapReduce job to process xtrace reports coming out of chukwa demux.
 * 
 * Map phase unwraps the chukwa records, reduce phase does trace reconstruction.
 * 
 * We use task ID as the reduce sort key.
 *
 */
public class XtrExtract extends Configured implements Tool {
  
  /**
   * with more than 10,000 reports, switch to on-disk sort, 
   * instead of in-memory topological sort.
   */
  static final int MAX_IN_MEMORY_REPORTS = 10* 1000;

  public class MapClass extends Mapper <ChukwaRecordKey, ChukwaRecord, byte[], Report> {
    
    protected void map(ChukwaRecordKey key, ChukwaRecord value, 
        Mapper<ChukwaRecordKey, ChukwaRecord, byte[], Report>.Context context)
        throws IOException, InterruptedException 
    {
      Report xtrReport = Report.createFromString(value.getValue(Record.bodyField));
      context.write(xtrReport.getMetadata().getTaskId().get(), xtrReport);
    }
  }
  
  public class Reduce extends Reducer<byte[], Report,byte[],List<Report>> {
    
    protected  void   reduce(byte[] taskID, Iterable<Report> values, 
          Reducer<byte[], Report,byte[],List<Report>>.Context context) 
          throws IOException, InterruptedException
    {
      HashMap<String, Report> reports = new HashMap<String, Report>();
      HashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();

      Counter reportCounter = context.getCounter("app", "reports");
      
      int numReports = 0;
      for(Report r: values) {
        reportCounter.increment(1);
        numReports++;

        if(numReports < MAX_IN_MEMORY_REPORTS) {
          reports.put(r.getMetadata().getOpIdString(), r);
          //increment link counts for children
          for(String outLink: r.get("Edge")) {
            Integer oldCount = counts.get(outLink);
            if(oldCount == null)
              oldCount = 0;
            counts.put(outLink, oldCount +1);
          }
        } else if(numReports == MAX_IN_MEMORY_REPORTS) {
          //bail out, prepare to do an external sort.
          return;
        } else
          ;
    //      do the external sort
      }
      
      
      List<Report> finalOutput = new ArrayList<Report>(reports.size());
      for(Report r: reports.values())
        finalOutput.add(r);
      
      
      /*
      //at this point, we have a map from metadata to report, and also
      //from report op ID to inlink count.
      //next step is to do a topological sort.

      Queue<Report> zeroInlinkReports = new LinkedList<Report>();
      
        //first pass: find entries with no inlinks
      for(java.util.Map.Entry<String, Integer> count : counts.entrySet()) {
        if(count.getValue() == 0) {
          Report r = reports.get(count.getKey());
          zeroInlinkReports.add(r);
        }
      }
      List<Report> finalOutput = new ArrayList<Report>(reports.size());
      while(!zeroInlinkReports.isEmpty()) {
        Report r = zeroInlinkReports.poll();
        assert r != null: "apparently your graph is cyclic";
        for(String outLink: r.get("Edge")) {
          Integer oldCount = counts.get(outLink);
          if(oldCount == null)
            oldCount = 0;
          if(oldCount == 1)
            zeroInlinkReports.add(reports.get(outLink));
          counts.put(outLink, oldCount -1);
        }
        
      }*/

      context.write(taskID, finalOutput);
      //Should sort values topologically and output list.  or?
      
    } //end reduce
    
  }//end reduce class

  @Override
  public int run(String[] arg) throws Exception {
    Job extractor = new Job(getConf());
    extractor.setMapperClass(MapClass.class);
    extractor.setReducerClass(Reduce.class);
    extractor.setJobName("x-trace reconstructor-nosort");
    extractor.setInputFormatClass(SequenceFileInputFormat.class);
    extractor.setOutputFormatClass(SequenceFileOutputFormat.class);
    FileInputFormat.setInputPaths(extractor, new Path(arg[0]));
    FileOutputFormat.setOutputPath(extractor, new Path(arg[1]));
    System.out.println("looks OK.  Submitting.");
    extractor.waitForCompletion(false);
    return 0;

  }

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(),
        new XtrExtract(), args);
    System.exit(res);
  }

}
