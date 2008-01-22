/*
 * Copyright (c) 2005,2006,2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package edu.berkeley.xtrace.server;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.servlet.CGI;

import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XtraceException;
import edu.berkeley.xtrace.reporting.Report;

/**
 * @author George Porter
 *
 */
public final class Main {
	private static final Logger LOG = Logger.getLogger(Main.class);

	private static ReportSource[] sources;
	
	private static BlockingQueue<String> incomingReportQueue, reportsToStorageQueue;

	private static ThreadPerTaskExecutor sourcesExecutor;

	private static ExecutorService storeExecutor;

	private static QueryableReportStore reportstore;
	
	private static final DateFormat DATE_FORMAT =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		
		// If they use the default configuration (the FileTree report store),
		// then they have to specify the directory in which to store reports
		if (System.getProperty("xtrace.server.store") == null) {
			if (args.length < 1) {
				System.err.println("Usage: Main <dataDir>");
				System.exit(1);
			}
			System.setProperty("xtrace.server.storedirectory", args[0]);
		}
		
		setupReportSources();
		setupReportStore();
		setupBackplane();
		setupWebInterface();
	}

	private static void setupReportSources() {
		
		incomingReportQueue = new ArrayBlockingQueue<String>(1024, true);
		sourcesExecutor = new ThreadPerTaskExecutor();
		
		// Default input sources
		String sourcesStr = "edu.berkeley.xtrace.server.UdpReportSource," +
		                    "edu.berkeley.xtrace.server.TcpReportSource";
		
		if (System.getProperty("xtrace.server.sources") != null) {
			sourcesStr = System.getProperty("xtrace.server.sources");
		} else {
			LOG.warn("No server report sources specified... using defaults (Udp,Tcp)");
		}
		String[] sourcesLst = sourcesStr.split(",");
		
		sources = new ReportSource[sourcesLst.length];
		for (int i = 0; i < sourcesLst.length; i++) {
			try {
				sources[i] = (ReportSource) Class.forName(sourcesLst[i]).newInstance();
			} catch (InstantiationException e1) {
				LOG.fatal("Could not instantiate report source", e1);
				System.exit(-1);
			} catch (IllegalAccessException e1) {
				LOG.fatal("Could not access report source", e1);
				System.exit(-1);
			} catch (ClassNotFoundException e1) {
				LOG.fatal("Could not find report source class", e1);
				System.exit(-1);
			}
			sources[i].setReportQueue(incomingReportQueue);
			try {
				sources[i].initialize();
			} catch (XtraceException e) {
				LOG.warn("Unable to initialize report source", e);
				// TODO: gracefully shutdown any previously started threads?
				System.exit(-1);
			}
			sourcesExecutor.execute((Runnable) sources[i]);
		}
	}
	
	private static void setupReportStore() {
		reportsToStorageQueue = new ArrayBlockingQueue<String>(1024);
		
		String storeStr = "edu.berkeley.xtrace.server.FileTreeReportStore";
		if (System.getProperty("xtrace.server.store") != null) {
			storeStr = System.getProperty("xtrace.server.store");
		} else {
			LOG.warn("No server report store specified... using default (FileTreeReportStore)");
		}
		
		reportstore = null;
		try {
			reportstore = (QueryableReportStore) Class.forName(storeStr).newInstance();
		} catch (InstantiationException e1) {
			LOG.fatal("Could not instantiate report store", e1);
			System.exit(-1);
		} catch (IllegalAccessException e1) {
			LOG.fatal("Could not access report store class", e1);
			System.exit(-1);
		} catch (ClassNotFoundException e1) {
			LOG.fatal("Could not find report store class", e1);
			System.exit(-1);
		}
		
		reportstore.setReportQueue(reportsToStorageQueue);
		try {
			reportstore.initialize();
		} catch (XtraceException e) {
			LOG.fatal("Unable to start report store", e);
			System.exit(-1);
		}
		
		storeExecutor = Executors.newSingleThreadExecutor();
		storeExecutor.execute(reportstore);
		
		/* Every N seconds we should sync the report store */
		String syncIntervalStr = System.getProperty("xtrace.server.syncinterval", "5");
		long syncInterval = Integer.parseInt(syncIntervalStr);
		Timer timer= new Timer();
		timer.schedule(new SyncTimer(reportstore), syncInterval*1000, syncInterval*1000);
		
		/* Add a shutdown hook to flush and close the report store */
		Runtime.getRuntime().addShutdownHook(new Thread() {
		  public void run() {
			  reportstore.shutdown();
		  }
		});
	}
	
	private static void setupBackplane() {
		new Thread(new Runnable() {
			public void run() {
				LOG.info("Backplane waiting for packets");
				
				while (true) {
					String msg = null;
					try {
						msg = incomingReportQueue.take();
					} catch (InterruptedException e) {
						LOG.warn("Interrupted", e);
						continue;
					}
					reportsToStorageQueue.offer(msg);
				}
			}
		}).start();
	}
	
	private static class ThreadPerTaskExecutor implements Executor {
	     public void execute(Runnable r) {
	         new Thread(r).start();
	     }
	 }
	
	private static void setupWebInterface() {
    String webDir = System.getProperty("xtrace.backend.webui.dir");
    if (webDir == null) {
      LOG.warn("No webui directory specified... using default (./src/webui)");
      webDir = "./src/webui";
    }
    
    int httpPort =
      Integer.parseInt(System.getProperty("xtrace.backend.httpport", "8080"));
    
    Server server = new Server(httpPort);
    Context context = new Context(server, "/");
    
    // Create a CGI servlet for scripts in webui/cgi-bin 
    ServletHolder cgiHolder = new ServletHolder(new CGI());
    cgiHolder.setInitParameter("cgibinResourceBase", webDir + "/cgi-bin");
    context.addServlet(cgiHolder, "*.cgi");
    context.addServlet(cgiHolder, "*.pl");
    context.addServlet(cgiHolder, "*.py");
    context.addServlet(cgiHolder, "*.rb");

    context.addServlet(new ServletHolder(
        new GetReportsServlet()), "/reports/*");
    context.addServlet(new ServletHolder(
        new GetLatestTaskServlet()), "/latestTask");
    context.addServlet(new ServletHolder(
        new LatestTasksServlet()), "/latestTasks");
    
    // Add an IndexServlet as the default servlet. This servlet will serve
    // a human-readable (HTML) latest tasks page for "/" and serve static
    // content for any other URL. Being the default servlet, it will get
    // invoked only for URLs that does not match the other patterns where we
    // have registered servlets above.
    context.setResourceBase(webDir + "/html");
    context.addServlet(new ServletHolder(new IndexServlet()), "/");
    
    try {
      server.start();
    } catch (Exception e) {
      LOG.warn("Unable to start web interface", e);
    }
	}
	
	private static class GetReportsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);
      String uri = request.getRequestURI();
      int pathLen = request.getServletPath().length() + 1;
      String taskId = uri.length() >= pathLen ? uri.substring(pathLen) : null;
      System.out.println(taskId);
      Writer out = response.getWriter();
      if (taskId != null) {
        Iterator<Report> iter;
        try {
          iter = reportstore.getReportsByTask(TaskID.createFromString(taskId));
        } catch (XtraceException e) {
          throw new ServletException(e);
        }
        while (iter.hasNext()) {
          out.write(iter.next().toString());
          out.write("\n");
        }
      }
    }
  }
	
	private static class GetLatestTaskServlet extends HttpServlet {
	  protected void doGet(HttpServletRequest request, HttpServletResponse response)
	      throws ServletException, IOException {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);
      Writer out = response.getWriter();
      
      List<TaskID> task = reportstore.getLatestTasks(1);
      if (task.size() != 1) {
        LOG.warn("getLatestTasks(1) returned " + task.size() + " entries");
        return;
      }
      try {
        Iterator<Report> iter = reportstore.getReportsByTask(task.get(0));
        while (iter.hasNext()) {
          Report r = iter.next();
          out.write(r.toString());
          out.write("\n");
        }
      } catch (XtraceException e) {
        LOG.warn("Internal error", e);
        out.write("Internal error: " + e);
      }
	  }
	}
  
  private static class LatestTasksServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      doLatestTasks(request, response, false);
    }
  }
  
  private static class IndexServlet extends DefaultServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      if(request.getRequestURI().equals("/")) {
        doLatestTasks(request, response, true);
      } else {
        super.doGet(request, response);
      }
    }
  }

  private static void doLatestTasks(HttpServletRequest request, 
      HttpServletResponse response, boolean outputHtml)
      throws ServletException, IOException {
    if (outputHtml)
      response.setContentType("text/html");
    else
      response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    Writer out = response.getWriter();
    long windowHours;
    try {
      windowHours = Long.parseLong(request.getParameter("window"));
      if (windowHours < 0) throw new IllegalArgumentException();
    } catch(Exception ex) {
      windowHours = 24;
    }
    long startTime = System.currentTimeMillis() - windowHours * 60 * 60 * 1000;
    Iterator<TaskID> iter = reportstore.getTasksSince(startTime);
    if (outputHtml) {
      out.write("<html><head><title>Latest Tasks</title></head>\n"
          + "<body><h1>X-Trace Latest Tasks</h1>\n"
          + "<table border=1 cellspacing=0 cellpadding=3>\n"
          + "<tr><th>Date</th><th>TaskID</th><th># Reports</th></tr>\n");
    } else {
      out.write("[\n");
    }
    // Remember last taskID in case the table contains a duplicate (not sure this can happen)
    boolean first = true;
    while (iter.hasNext()) {
      TaskID taskId = iter.next();

      long time = reportstore.lastUpdatedByTaskId(taskId);
      Date date = new Date(time);

      int count = reportstore.countByTaskId(taskId);

      if (outputHtml) {
        out.write("<tr><td>" + date.toString() + "</td><td>"
            + "<a href=\"getReports?taskid=" + taskId
            + "\">" + taskId + "</a></td><td>" + count
            + "</td></tr>\n");
      } else {
        if (!first)
        out.write(",\n");
        out.write("{ \"taskid\":\"" + taskId
            + "\", \"date-time\":\"" + DATE_FORMAT.format(date)
            + "\", \"reportcount\":\"" + count + "\" }");
      }
      first = false;
    }
    if (outputHtml) {
      out.write("</table>\n");
      int numTasks = reportstore.numUniqueTasks();
      int numReports = reportstore.numReports();
      out.write("<p>Database size: " + numTasks + " tasks, " 
          + numReports + " reports.  Data valid as of: " + reportstore.dataAsOf() + "</p>\n");
      out.write("</body></html>\n");
    } else {
      out.write("\n]\n");
    }
  }
  
	private static final class SyncTimer extends TimerTask {
		private QueryableReportStore reportstore;

		public SyncTimer(QueryableReportStore reportstore) {
			this.reportstore = reportstore;
		}

		public void run() {
			reportstore.sync();
		}
	}
}
