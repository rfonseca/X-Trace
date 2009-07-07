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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

//import org.apache.log4j.Logger;

/**
 * X-trace reporting context framework that uses UDP packets sent to
 * a local daemon process.
 * 
 * By default, this reporting context sends reports to
 * the IP address '127.0.0.1', on port '7831'.
 * 
 * To change this behavior, specify a different IP address
 * and port number using the 'xtrace.udpdest' property:
 *     
 *    java -Dxtrace.reporter="edu.berkeley.xtrace.reporting.UdpReporter"
 *         -Dxtrace.udpdest="localhost:7831"
 *
 * @author George Porter
 */
public final class UdpReporter extends Reporter
{
	//private static final Logger LOG = Logger.getLogger(UdpReporter.class);

	/* Local report daemon state */
	private DatagramSocket localSock;
	private InetAddress localAddr;
	private int localPort;
	
	UdpReporter()
	{
		//LOG.info("Creating UdpReporter");
		
		// Setup the local UDP socket
		try {
			localSock = new DatagramSocket();
		} catch (final SocketException se) {
			//LOG.warn("Internal network error", se);
			localSock = null;
			return;
		}
		
		String udp_ip = "127.0.0.1";
		String udp_port = "7831";
		
		String udpDest = System.getProperty("xtrace.udpdest");
		if (udpDest != null) {
			try {
				String[] split = udpDest.split(":");
				udp_ip = split[0];
				udp_port = split[1];
			} catch (RuntimeException e) {
				//LOG.warn("Invalid xtrace.udpdest property. Expected host:port.", e);
			}
		}
		
		// Setup the UDP sending host
		try {
			this.localAddr = InetAddress.getByName(udp_ip);
		} catch (UnknownHostException uhe) {
			//LOG.warn("Unknown host: " + udp_ip, uhe);
			System.exit(1);
		}

		// Setup the UDP sending port
		try {
			this.localPort = Integer.parseInt(udp_port);
		} catch (NumberFormatException nfe) {
			//LOG.warn("Invalid UDP port: " + udp_port, nfe);
			System.exit(1);
		}
	}

	/**
	 * Closes this reporter, releasing any resources
	 */
	public synchronized void close()
	{
		if (reporter != null) {
			localSock.close();
			reporter = null;
		}
	}

	/**
	 * Sends a report to the local reporting daemon
	 *
	 * @param r the report to send
	 */
	@Override
	public synchronized void sendReport(Report r) {
		
		if (localSock == null) {
			return;
		}
		
		byte[] msg;
		try {
			msg = r.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			
			// We use the default encoding if UTF-8 is unavailable
			msg = r.toString().getBytes();
		}
		DatagramPacket pkt =
			new DatagramPacket(msg, 0, msg.length, localAddr, localPort);
		try {
			localSock.send(pkt);
		} catch (final IOException e) {
			//LOG.warn("Unable to send report", e);
		}
	}
}
