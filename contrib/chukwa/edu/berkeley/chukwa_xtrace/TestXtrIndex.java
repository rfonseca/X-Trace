package edu.berkeley.chukwa_xtrace;

import org.apache.hadoop.io.Text;
import junit.framework.TestCase;
import edu.berkeley.xtrace.reporting.Report;
import edu.berkeley.xtrace.*;
import java.util.*;

public class TestXtrIndex extends TestCase {

  public void testEndDetect() throws Exception {
    HashMap<String, Report> reports = new HashMap<String, Report>();
    
    TaskID id = new TaskID(8);
    XTraceMetadata meta = new XTraceMetadata(id,1);
    
    XTraceContext.setThreadContext(meta);
    //test case with no end
    XTraceEvent evt = XTraceContext.createEvent("agentStr", "labelStr");
    Report start = evt.createReport();
    
      //try it with empty report first
    reports.put(start.getMetadata().getOpIdString(), start);
    Report end = XtrIndex.findMatchingEnd(reports, start, "theThing");
    assertNull(end);

      //try again, after adding start field
    start.put("Start", "theThing");    
    reports.put(start.getMetadata().getOpIdString(), start);
    end = XtrIndex.findMatchingEnd(reports, start, "theThing");
    assertNull(end);
    
      //now add an end
    //test case with start followed immediately by end
    evt = XTraceContext.createEvent("agentStr", "labelStr2");
    evt.addEdge(start.getMetadata());
    end = evt.createReport();
    end.put("End", "theThing");
    start.put(XtrExtract.OUTLINK_FIELD, end.getMetadata().getOpIdString());
    reports.put(end.getMetadata().getOpIdString(), end);
    Report e = XtrIndex.findMatchingEnd(reports, start, "theThing");
    assertNotNull(e);
    assertTrue(e == end);//should get same report back
    //now how about a event in the middle?
    
  }
  
  public void testIndexing() {    
    HashMap<String, Report> reports = new HashMap<String, Report>();
    
    TaskID id = new TaskID(8);
    XTraceMetadata meta = new XTraceMetadata(id,1);
    
    XTraceContext.setThreadContext(meta);
    //test case with no end
    XTraceEvent evt = XTraceContext.createEvent("agentStr", "labelStr");
    Report start = evt.createReport();
    
      //try it with empty report first
    reports.put(start.getMetadata().getOpIdString(), start);
    Text[] index = XtrIndex.indexGraph(reports);
    assertEquals(0, index.length);

      //try again, after adding start field
    start.put("Start", "theThing");    
    reports.put(start.getMetadata().getOpIdString(), start);
    index = XtrIndex.indexGraph(reports);
    assertEquals(0, index.length);

      //now add an end
    //test case with start followed immediately by end
    evt = XTraceContext.createEvent("agentStr", "labelStr2");
    evt.addEdge(start.getMetadata());
    Report end = evt.createReport();
    end.put("End", "theThing");
    start.put(XtrExtract.OUTLINK_FIELD, end.getMetadata().getOpIdString());
    reports.put(end.getMetadata().getOpIdString(), end);
    index = XtrIndex.indexGraph(reports);
    assertEquals(1, index.length);
    String s = index[0].toString();
    assertTrue(s.startsWith("theThing "));
    
    //should test with an even bigger graph here
    
    
    System.out.println("index entry was " + index[0]);    
    
  }

}
