/*
 * Written by Ari Rabkin, UC Berkeley.
 * All rights reserved, distribution outside UC Berkeley or collaborating
 * institutions prohibited without authorization.
 * 
 * This notice is a placeholder until I figure out how to really release code.
 * Probably will ultimately become a chukwa contrib, and hence apache license.
 * 
 */
package edu.berkeley.chukwa_xtrace;

import org.apache.hadoop.chukwa.*;
import org.apache.hadoop.chukwa.datacollection.adaptor.AbstractAdaptor;
import org.apache.hadoop.chukwa.datacollection.adaptor.Adaptor;
import org.apache.hadoop.chukwa.datacollection.adaptor.AdaptorException;
import org.apache.hadoop.chukwa.datacollection.adaptor.AdaptorShutdownPolicy;
import org.apache.log4j.Logger;
import edu.berkeley.xtrace.server.*;
import edu.berkeley.xtrace.XTraceException;
import java.util.concurrent.*;

/**
 * Adaptor that wraps an xtrace report source, so that xtrace messages
 * can get picked up by the chukwa agent.
 * Takes one mandatory param, the class name of the report source,
 * implicitly inside package edu.berkeley.xtrace.server
 * 
 */
public class XtrAdaptor extends AbstractAdaptor implements Runnable {
  
  ReportSource rs;
  Thread pump = new Thread(this);
  Thread reportSourceThread;
  BlockingQueue<String> q = new ArrayBlockingQueue<String>(1000);
  volatile boolean stopping = false;
  long offset = 0;
  static Logger log = Logger.getLogger(XtrAdaptor.class);
  
  
  static final String XTR_RS_PACKAGE = "edu.berkeley.xtrace.server.";
  /**
   * Get an xtrace report source, of name classname
   * @param classname
   * @return a report source. Defaults to UdpReportSource on error.
   */
  static ReportSource getXtrReportSource(String name) {
    try { 
    Object obj = Class.forName(XTR_RS_PACKAGE + name).newInstance();
    if (ReportSource.class.isInstance(obj)) {
      return (ReportSource) obj;
    } else
      return new UdpReportSource();
    } catch(Exception e) {
      log.warn(e);
      return new UdpReportSource();
    }
  }
  
  /*
   * This is effectively the main thread associated with the adaptor;
   * however, each ReportSource separately might have a thread.
   */
  public void run() {
    try {
      log.info("starting Pump Thread");
      while(!stopping) {
        String report = q.take();
        log.info("got a report");
        byte[] data = report.getBytes();
        offset += data.length;
        ChunkImpl i = new ChunkImpl(type, "xtrace", offset, data, this);
        dest.add(i);
      }
    } catch(InterruptedException e) {
      
    }
    log.info("XtrAdaptor stopping");
  }

  @Override
  public void start( long offset) throws AdaptorException {

    this.offset = offset;
    try{
      rs.initialize();
      rs.setReportQueue(q);
      reportSourceThread = new Thread(rs);
      reportSourceThread.start();
      pump.start();
      log.info("starting Report Source");
    } catch(XTraceException e) {
      throw new AdaptorException(e);
    }
  }

  @Override
  public String getCurrentStatus() throws AdaptorException {
    return type;
  }

  @Override
  public String parseArgs(String params) {
    rs = getXtrReportSource(params);  
    return params; //no optional params 
  }

  @Override
  public void hardStop() throws AdaptorException {
    shutdown(AdaptorShutdownPolicy.HARD_STOP);
  }

  @Override
  public long shutdown() throws AdaptorException {
    return shutdown(AdaptorShutdownPolicy.GRACEFULLY);
  }

  @Override
  public long shutdown(AdaptorShutdownPolicy shutdownPolicy)
      throws AdaptorException {
    switch(shutdownPolicy) {
    case HARD_STOP:
    case GRACEFULLY:
    case WAIT_TILL_FINISHED:
      rs.shutdown();
      stopping = true;
    }
    return offset;
  }

}
