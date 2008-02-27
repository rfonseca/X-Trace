package edu.berkeley.xtrace.server;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.facebook.thrift.TException;
import com.facebook.thrift.server.TThreadPoolServer;
import com.facebook.thrift.transport.TServerSocket;
import com.facebook.thrift.transport.TServerTransport;
import com.facebook.thrift.transport.TTransportException;

import edu.berkeley.xtrace.XTraceException;
import edu.berkeley.xtrace.reporting.XTraceReporter;

public class ThriftReportSource implements ReportSource {
	private static final Logger LOG = Logger.getLogger(ThriftReportSource.class);
	
	static BlockingQueue<String> q;
	private int thriftport;

	private TThreadPoolServer server;
	TServerTransport serverTransport;

	public void initialize() throws XTraceException {
		String thriftportstr = System.getProperty("xtrace.backend.thriftport", "7832");
		try {
			thriftport = Integer.parseInt(thriftportstr);
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid thrift report port: " + thriftport, nfe);
			thriftport = 7832;
		}
		
		XTraceReporterHandler handler = new XTraceReporterHandler();
	    XTraceReporter.Processor processor = new XTraceReporter.Processor(handler);
	    
		try {
			serverTransport = new TServerSocket(thriftport);
		} catch (TTransportException e) {
			throw new XTraceException("Unable to open Thrift server", e);
		}
	    server = new TThreadPoolServer(processor, serverTransport);
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
		server.stop();
		serverTransport.close();
	}
	
	public static class XTraceReporterHandler implements XTraceReporter.Iface {
		private static final Logger LOG = Logger.getLogger(XTraceReporterHandler.class);

		public void ping() throws TException {
			LOG.info("Received ping() request");
		}

		public void sendReport(String report) throws TException {
			LOG.debug("Received report: " + report);
			ThriftReportSource.q.offer(report);
		}		
	}

	public void run() {
	    LOG.info("Starting the Thrift report source on port " + thriftport);
	    server.serve();
	}
}
