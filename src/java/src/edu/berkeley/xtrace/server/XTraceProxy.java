package edu.berkeley.xtrace.server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceException;
import edu.berkeley.xtrace.reporting.Report;
import edu.berkeley.xtrace.reporting.Reporter;

public class XTraceProxy {
	private static final Logger LOG = Logger.getLogger(XTraceProxy.class);
	private static BlockingQueue<String> incomingReportQueue;
	private static ThreadPerTaskExecutor sourcesExecutor;
	private static ReportSource[] sources;

	public static void main(String[] args) {
		setupReportSources();
		setupBackplane();
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
			} catch (XTraceException e) {
				LOG.warn("Unable to initialize report source", e);
				// TODO: gracefully shutdown any previously started threads?
				System.exit(-1);
			}
			sourcesExecutor.execute((Runnable) sources[i]);
		}
	}
	
	private static void setupBackplane() {
		final Reporter reporter = Reporter.getReporter();
		
		/* Sync the reporter every n seconds */
		String syncIntervalStr = System.getProperty("xtrace.proxy.syncinterval", "5");
		long syncInterval = Integer.parseInt(syncIntervalStr);
		Timer timer = new Timer();
		timer.schedule(new SyncTimer(reporter), syncInterval*1000, syncInterval*1000);
		
		new Thread(new Runnable() {
			public void run() {
				LOG.info("Proxy waiting for reports");
				
				while (true) {
					String msg = null;
					try {
						msg = incomingReportQueue.take();
					} catch (InterruptedException e) {
						LOG.warn("Interrupted", e);
						continue;
					}
					LOG.debug("Received report of length " + msg.length());
					LOG.debug(msg);
					Report rpt = Report.createFromString(msg);
					reporter.sendReport(rpt);
					LOG.debug("Report sent to backend");
				}
			}
		}).start();
	}

	private static class ThreadPerTaskExecutor implements Executor {
		public void execute(Runnable r) {
			new Thread(r).start();
		}
	}
	
	private static final class SyncTimer extends TimerTask {
		private Reporter reporter;

		public SyncTimer(Reporter reporter) {
			this.reporter = reporter;
		}

		public void run() {
			reporter.flush();
		}
	}

}
