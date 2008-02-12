package edu.berkeley.xtrace.server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceException;

/**
 * 
 * @author Matei Zaharia
 * @author George Porter
 *
 */
public class TcpReportSource implements ReportSource {
	private static final Logger LOG = Logger.getLogger(TcpReportSource.class);
	private static final int MAX_REPORT_LENGTH = 256*1024;
	
	private int tcpport;

	private BlockingQueue<String> q;

	private ServerSocket serversock;

	public void initialize() throws XTraceException {
		String tcpportstr = System.getProperty("xtrace.backend.tcpport", "7831");
		try {
			tcpport = Integer.parseInt(tcpportstr);
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid tcp report port: " + tcpportstr, nfe);
			tcpport = 7831;
		}
		
		try {
			serversock = new ServerSocket(tcpport);
		} catch (IOException e) {
			throw new XTraceException("Unable to open TCP server socket", e);
		}
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
		try {
			serversock.close();
		} catch (IOException e) {
			LOG.warn("Unable to close TCP server socket", e);
		}
	}

	public void run() {
		try {
			LOG.info("TcpReportSource started on port " + tcpport);
			
			while (true) {
				Socket sock = serversock.accept();
				new TcpClientHandler(sock).start();
			}
		} catch(IOException e) {
			LOG.warn("Error while accepting a TCP client", e);
		}
	}
	
	private final class TcpClientHandler extends Thread {
		
		private Socket sock;

		public TcpClientHandler(Socket sock) {
			this.sock = sock;
		}
		
		public void run() {
			try {
				LOG.info("Starting TcpClientHandler for "
						+ sock.getInetAddress() + ":" + sock.getPort());
				
				byte[] buf = new byte[MAX_REPORT_LENGTH];
				DataInputStream in = new DataInputStream(sock.getInputStream());
				while (true) {
					int length = in.readInt();
					if (length <= 0 || length > MAX_REPORT_LENGTH) {
						LOG.info("Closing ReadReportsThread for "
						+ sock.getInetAddress() + ":" + sock.getPort() 
						+ " due to bad length: " + length);
						sock.close();
						return;
					}
					in.readFully(buf, 0, length);
					String message = new String(buf, 0, length, "UTF-8");
					q.offer(message);
				}
			} catch(EOFException e) {
				LOG.info("Closing ReadReportsThread for "
						+ sock.getInetAddress() + ":" + sock.getPort()
						+ " normally (EOF)");
			} catch(Exception e) {
				LOG.warn("Closing ReadReportsThread for "
						+ sock.getInetAddress() + ":" + sock.getPort(), e);
			}
		}

	}
}
