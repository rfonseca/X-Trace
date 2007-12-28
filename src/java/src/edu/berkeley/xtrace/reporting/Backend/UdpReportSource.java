package edu.berkeley.xtrace.reporting.Backend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XtraceException;

public class UdpReportSource implements ReportSource {
	private static final Logger LOG = Logger.getLogger(UdpReportSource.class);

	private BlockingQueue<String> q;
	private DatagramSocket socket;

	public void initialize() throws XtraceException {
		
		String udpSource = System.getProperty("xtrace.udpsource", "127.0.0.1:7831");
		
		InetAddress localAddr;
		try {
			localAddr = InetAddress.getByName(udpSource.split(":")[0]);
		} catch (UnknownHostException e) {
			throw new XtraceException("Unknown host: " + udpSource.split(":")[0], e);
		}
		int localPort = Integer.parseInt(udpSource.split(":")[1]);
		try {
			socket = new DatagramSocket(localPort, localAddr);
		} catch (SocketException e) {
			throw new XtraceException("Unable to open socket", e);
		}

		LOG.info("UDPReportSource initialized on " + localAddr + ":" + localPort);
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
		if (socket != null)
			socket.close();
	}

	public void run() {
		LOG.info("UDPReportSource listening for packets");
		
		while (true) {
			byte[] buf = new byte[4096];
			DatagramPacket p = new DatagramPacket(buf, buf.length);

		    try {
				socket.receive(p);
			} catch (IOException e) {
				LOG.warn("Unable to receive report", e);
			}
			
			//LOG.debug("Received Report");
			
		    try {
				q.offer(new String(p.getData(), 0, p.getLength(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOG.warn("UTF-8 not available", e);
			}
		}
	}
}
