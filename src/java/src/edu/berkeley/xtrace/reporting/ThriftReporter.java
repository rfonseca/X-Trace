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

package edu.berkeley.xtrace.reporting;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.facebook.thrift.TException;
import com.facebook.thrift.protocol.TBinaryProtocol;
import com.facebook.thrift.protocol.TProtocol;
import com.facebook.thrift.transport.TSocket;
import com.facebook.thrift.transport.TTransport;
import com.facebook.thrift.transport.TTransportException;

import edu.berkeley.xtrace.reporting.XTraceReporter.Client;

/**
 * X-trace reporting context framework.  This context sends reports
 * using Facebook's Thrift RPC protocol.  Set the
 * host and port via the xtrace.thriftdest system property. For example,
 * to send reports to reports.x-trace.net:7832, run your program with:
 * 
 *   java -Dxtrace.reporter="edu.berkeley.xtrace.reporting.ThriftReporter" \
 *        -Dxtrace.thriftdest="reports.x-trace.net:7832"
 *
 */
public final class ThriftReporter extends Reporter
{
	private static final Logger LOG = Logger.getLogger(ThriftReporter.class);

	String host = null;
	int port = 0;
	int retryinterval;
	boolean shouldRetry = true;
	
	private TSocket transport;
	private Client client;
	
	ThriftReporter()
	{
		LOG.info("Creating ThriftReportingContext");
		
		String thriftDest = System.getProperty("xtrace.thriftdest");
		if (thriftDest == null) {
			LOG.warn("ThriftReportingContext was used, but no xtrace.thriftdest "
					+ " system property has been set. X-Trace reports "
					+ " will be ignored. Please specify the command line"
					+ " option -Dxtrace.thriftdest=host:port to provide a"
					+ " destination for X-Trace reports.");
			return;
		}
		
		String retryInterval = System.getProperty("xtrace.thrift.retryinterval", "10");
		
		try {
			String[] split = thriftDest.split(":");
			host = split[0];
			port = Integer.parseInt(split[1]);
			retryinterval = Integer.parseInt(retryInterval);
			
		} catch (Exception e) {
			LOG.warn("Invalid xtrace.thriftdest property. Expected host:port.", e);
			System.exit(1);
		}
		
		open();
	}
	
	private synchronized void open()
	{
		LOG.info("Opening Thrift connection to " + host + ":" + port);
		transport = new TSocket(host, port); 
	    TProtocol protocol = new TBinaryProtocol(transport);
	    client = new XTraceReporter.Client(protocol);
	    shouldRetry = true;
	    
	    while (shouldRetry) {
	    	try {
				transport.open();
				return;
				
			} catch (TTransportException e) {
				LOG.warn("Unable to connect to the backend server", e);
				LOG.warn("Will retry in " + retryinterval + " seconds");
				try {
					Thread.sleep(retryinterval * 1000);
				} catch (InterruptedException e1) {}
			}
	    }
	}

	/**
	 * Closes this reporter, releasing any resources
	 */
	public synchronized void close()
	{
		if (client != null) {
			LOG.info("Closing ThriftReporter");
			transport.close();
			client = null;
		}
		shouldRetry = false;
	}
	
	public synchronized void flush() {
		super.flush();
		if (client != null) {
			LOG.debug("Flushing ThriftReporter");
			try {
				transport.flush();
			} catch (TTransportException e) {
				LOG.warn("Error flushing Thrift connection...reopening", e);
				close();
				open();
			}
		}
	}

	/**
	 * Sends a report via Thrift to the remote side
	 *
	 * @param r the report to send
	 */
	@Override
	public synchronized void sendReport(Report r) {
		try {
			if (client != null) {
				String reportstr = r.toString();
				client.sendReport(reportstr);
			}
		} catch (TException e) {
			LOG.warn("Unable to send report...reopening", e);
			close();
			open();
		}
	}
}
