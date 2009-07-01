package edu.berkeley.chukwa_xtrace;

import org.apache.hadoop.chukwa.extraction.demux.processor.mapper.AbstractProcessor;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecord;
import org.apache.hadoop.chukwa.extraction.engine.ChukwaRecordKey;
import org.apache.hadoop.chukwa.extraction.engine.Record;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.*;

import edu.berkeley.xtrace.reporting.Report;
import edu.berkeley.xtrace.*;


public class XtrExtract {
  

  class Map extends Mapper <ChukwaRecordKey, ChukwaRecord, byte[], Report> {
    
    protected void map(ChukwaRecordKey key, ChukwaRecord value, 
        Mapper<ChukwaRecordKey, ChukwaRecord, byte[], Report>.Context context)
        throws IOException, InterruptedException 
    {
      Report xtrReport = Report.createFromString(value.getValue(Record.bodyField));
      context.write(xtrReport.getMetadata().getTaskId().get(), xtrReport);
    }
  }
  
  class Reduce extends Reducer<byte[], Report,byte[],List<Report>> {
    
    protected  void   reduce(byte[] taskID, Iterable<Report> values, 
          Reducer<byte[], Report,byte[],List<Report>>.Context context) {
      
      //Should sort values topologically and output list.  or?
      
    }
  }
  
  /*
  @Override
  protected void parse(String recordEntry,
      OutputCollector<ChukwaRecordKey, ChukwaRecord> output, Reporter reporter)
      throws Throwable {
    Report xtrReport = Report.createFromString(recordEntry);
    
    ChukwaRecord record = new ChukwaRecord();
    key = new ChukwaRecordKey();
    this.buildGenericRecord(record, null, 0, "xtrace");
    //Key here is the MapReduce sort key: TaskID means that all the ops from the
    //same task will get sorted together at the reducer
    key.setKey(xtrReport.getMetadata().getTaskId().toString());
    
    //Somehow need to use xtrReport and bundle it into the ChukwaRecord
    
    output.collect(key, record);
  } */

}
