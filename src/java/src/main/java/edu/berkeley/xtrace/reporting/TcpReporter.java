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

/**
 * X-trace reporting context framework.  This context sends reports to a
 * TCP host and port set via the xtrace.tcpdest system property. For example,
 * to send reports to reports.x-trace.net:7000, run your program with:
 * 
 *   java -Dxtrace.reporter="edu.berkeley.xtrace.reporting.TcpReporter" \
 *        -Dxtrace.tcpdest="reports.x-trace.net:7000"
 *
 * @author Matei Zaharia
 */
public final class TcpReporter extends Reporter
{
	private static final Logger LOG = Logger.getLogger(TcpReporter.class);
	
	// Connection to server or frontend daemon.
	private Socket socket;
	private DataOutputStream out;
	
	TcpReporter()
	{
		LOG.info("Creating TcpReportingContext");
		
		InetAddress host = null;
		int port = 0;
		
		String tcpDest = System.getProperty("xtrace.tcpdest");
		if (tcpDest == null) {
			LOG.warn("TcpReportingContext was used, but no xtrace.tcpdest "
					+ " system property has been set. X-Trace reports "
					+ " will be ignored. Please specify the command line"
					+ " option -Dxtrace.tcpdest=host:port to provide a"
					+ " destination for X-Trace reports.");
			return;
		}
		
		try {
			String[] split = tcpDest.split(":");
			host = InetAddress.getByName(split[0]);
			port = Integer.parseInt(split[1]);
			
		} catch (Exception e) {
			LOG.warn("Invalid xtrace.tcpdest property. Expected host:port.", e);
			System.exit(1);
		}
		
		try {
			socket = new Socket(host, port);
			// TODO: Maybe use BufferedOutputStream? In that case, make sure
			// to call flush() periodically for timely reporting.
			out = new DataOutputStream(socket.getOutputStream());
			
		} catch (Exception se) {
			LOG.warn("Failed to create X-Trace TCP socket", se);
			socket = null;
		}
	}

	/**
	 * Closes this reporter, releasing any resources
	 */
	public synchronized void close()
	{
		if (socket != null) {
			LOG.info("Closing TcpReporter");
			try {
				out.flush();
				socket.close();
			} catch (IOException e) {;}
			socket = null;
		}
	}
	
	public synchronized void flush() {
		super.flush();
		if (socket != null) {
			try {
				out.flush();
			} catch (IOException e) {;}
		}
	}

	/**
	 * Sends a report to the local reporting daemon
	 *
	 * @param r the report to send
	 */
	@Override
	public synchronized void sendReport(Report r) {
		try {
			if (socket != null) {
				byte[] bytes = r.toString().getBytes("UTF-8");
				out.writeInt(bytes.length);
				out.write(bytes);
			}
		} catch (IOException e) {
			LOG.warn("Couldn't send report", e);
			close();
		}
	}
}
