package edu.berkeley.chukwa_xtrace;

import java.util.Calendar;
import java.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.chukwa.ChukwaArchiveKey;
import org.apache.hadoop.chukwa.ChunkImpl;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;

public class XtrLoader {

  /**
   * @param args
   */
  public static void main(String[] args) {
    System.out.println("usage:  cat ... | XtrLoader <outfile>");
    if(args.length == 0)
      System.exit(0);
    try {
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      
      Configuration conf = new Configuration();
      FileSystem fileSys = FileSystem.getLocal(conf);
      Calendar calendar = Calendar.getInstance();
      FSDataOutputStream out = fileSys.create(new Path(args[0]));
  
      SequenceFile.Writer seqFileWriter = SequenceFile.createWriter(conf, out,
          ChukwaArchiveKey.class, ChunkImpl.class,
          SequenceFile.CompressionType.NONE, null);
      
      ChunkImpl chunk;
      while((chunk= getNextChunkFromStdin(stdin)) != null) {
        ChukwaArchiveKey archiveKey = new ChukwaArchiveKey();
    
        calendar.set(Calendar.YEAR, 2008);
        calendar.set(Calendar.MONTH, Calendar.MAY);
        calendar.set(Calendar.DAY_OF_MONTH, 29);
        calendar.set(Calendar.HOUR, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        archiveKey.setTimePartition(calendar.getTimeInMillis());
        archiveKey.setDataType(chunk.getDataType());
        archiveKey.setStreamName(chunk.getStreamName());
        archiveKey.setSeqId(chunk.getSeqID());
        seqFileWriter.append(archiveKey, chunk);
      }
      seqFileWriter.close();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  static long lastSeqID=0;
  private static ChunkImpl getNextChunkFromStdin(BufferedReader stdin) throws IOException {
    StringBuilder sb = new StringBuilder();
    
    String s;
    while((s = stdin.readLine()) != null ) {
      if(s.length() > 1) {
        sb.append(s);
        sb.append("\n");
        if(s.contains("ReadMessageBegin"))
          sb.append("Start: server\n");
        if(s.contains("WriteMessageBegin"))
          sb.append("End: server\n");
        //FIXME: this is a good place to introduce Start/End tags
      } else    //stop on blank line, if it isn't the first one we see
        if(sb.length() > 1)
          break;
    }
    
    if(sb.length() < 1)
      return null;
    
    String lines = sb.toString();
    
    ChunkImpl c = new ChunkImpl("XTrace", "XtrLoader",
        lines.length() - 1L + lastSeqID, lines.getBytes(), null);
    lastSeqID += lines.length();
    c.addTag("cluster=\"beth_xtrace\"");
    
    return c;
  }

}
