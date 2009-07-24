package edu.berkeley.chukwa_xtrace;

import java.io.File;
import java.io.IOException;
import org.apache.hadoop.chukwa.Chunk;
import org.apache.hadoop.chukwa.datacollection.agent.ChukwaAgent;
import org.apache.hadoop.chukwa.datacollection.connector.ChunkCatcherConnector;
import org.apache.hadoop.chukwa.datacollection.connector.http.HttpConnector;
import org.apache.hadoop.conf.Configuration;
import edu.berkeley.xtrace.XTraceContext;

/**
 * Outputs a sequence of xtrace reports, sending them to a chukwa collector.
 * 
 * This mostly exists to provide minimal input data to test the indexer and
 * report extractor jobs.
 *
 */
public class XtrTestDataGenerator {
  ChukwaAgent agent;
  public XtrTestDataGenerator(String collectorUrl) throws ChukwaAgent.AlreadyRunningException, IOException {
    Configuration conf = new Configuration();
    File baseDir = new File(System.getProperty("test.build.data", "/tmp"));
    conf.set("chukwaAgent.checkpoint.dir", baseDir.getCanonicalPath());
    conf.setBoolean("chukwaAgent.checkpoint.enabled", false);
    conf.set("chukwaAgent.control.port", "0");
    agent = new ChukwaAgent(conf);

    HttpConnector connector = new HttpConnector(agent, collectorUrl);
    connector.start();

    System.setProperty("xtrace.reporter", "edu.berkeley.xtrace.reporting.TcpReporter");
    System.setProperty("xtrace.tcpdest", "localhost:7831");
    agent.processAddCommand("add edu.berkeley.chukwa_xtrace.XtrAdaptor XTrace TcpReportSource 0");
    
  }
  
  public static void main(String[] args) {
    try {
      if(args.length != 1 ) {
        System.out.println("need to specify a collector URL");
        System.exit(0);
      }
      XtrTestDataGenerator g = new XtrTestDataGenerator(args[0]);
      
      XTraceContext.startTrace("test", "testtrace", "atag");
      for(int i =1; i < 10; ++i)
        XTraceContext.logEvent("test", "alabel_"+i);

      System.out.println("OK");
      Thread.sleep(10 * 1000);
      g.agent.shutdown();
      System.out.println("agent exiting");
      System.exit(0);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
   

}
