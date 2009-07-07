package edu.berkeley.xtrace.server;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.XTraceException;

/**
 * 
 * @author Matei Zaharia
 * @author George Porter
 *
 */
public class NonblockingTcpReportSource implements ReportSource {
	private static final Logger LOG = Logger.getLogger(NonblockingTcpReportSource.class);
	private static final int MAX_REPORT_LENGTH = 256*1024;
	
	private int tcpport;
	private BlockingQueue<String> q;

	private ServerSocketChannel serverChannel;
	private Selector selector;
	private ByteBuffer readBuffer = ByteBuffer.allocateDirect(256*1024);

	public void initialize() throws XTraceException {
		String tcpportstr = System.getProperty("xtrace.backend.tcpport", "7831");
		try {
			tcpport = Integer.parseInt(tcpportstr);
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid tcp report port: " + tcpportstr, nfe);
			tcpport = 7831;
		}
		
		try {
			selector = SelectorProvider.provider().openSelector();
			
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress("0.0.0.0", tcpport));
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			throw new XTraceException("Unable to open TCP server socket", e);
		}
	}

	public void setReportQueue(BlockingQueue<String> q) {
		this.q = q;
	}

	public void shutdown() {
		try {
			serverChannel.close();
			selector.close();
		} catch (IOException e) {
			LOG.warn("Unable to close TCP server socket", e);
		}
	}

	public void run() {
		while (true) {
			try {
				// Wait for an IO event on any of the registered sockets
				selector.select();
				
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();
					if (key.isValid()) {
						if (key.isAcceptable()) {
							accept(key);
						} else if (key.isReadable()) {
							read(key);
						}
					}
				}
			} catch(IOException e) {
				LOG.warn("Error in select loop", e);
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ);
	}
	
	private void read(SelectionKey key) throws IOException {
		ReadHandler handler = (ReadHandler) key.attachment();
		if (handler == null) {
			handler = new ReadHandler(key);
			key.attach(handler);
		}
		handler.handleRead();
	}
	
	private final class ReadHandler {
		private final SelectionKey key;
		
		private int msgPos = 0;
		private byte[] msgBuf = null;
		
		private int lengthPos = 0;
		private byte[] lengthBuf = new byte[4];
		
		public ReadHandler(SelectionKey key) {
			this.key = key;
		}

		public void handleRead() throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			readBuffer.clear();
			int numBytes = 0;
			try {
				numBytes = channel.read(readBuffer);
			} catch (IOException e) {
				// Connection was forcibly closed by the remote side
			  key.cancel();
			  channel.close();
			  return;
			}
			
			if (numBytes < 0) {
				// Connection was cleanly closed by remote side
			  key.cancel();
			  channel.close();
			  return;
			}

			// Read reports from the byte stream. Each report starts with a 4-byte integer
			// representing its length, followed by that number of bytes representing an UTF-8
			// encoded string for the message text. We use two buffers to be able to read
			// arbitrary amounts of bytes at a time: lengthBuf is used to store the bytes for
			// a length field (in case we get only part of one on some reads), and msgBuf is
			// used to store each message before sending it on. Each buffer has a position
			// (lengthPos and msgPos) which is the index of the first unread byte.
			readBuffer.flip();
			while (readBuffer.hasRemaining()) {
				if (msgBuf != null) {
					// We were in the middle of reading a message; see how much more of it we can get 
					int num = Math.min(readBuffer.remaining(), msgBuf.length - msgPos);
					readBuffer.get(msgBuf, msgPos, num);
					msgPos += num;
					if (msgPos == msgBuf.length) {
						String message = new String(msgBuf, 0, msgBuf.length, "UTF-8");
						q.offer(message);
						msgBuf = null;
						msgPos = 0;
					}
				} else {
					// We need to read a length field for a new message
					int num = Math.min(readBuffer.remaining(), lengthBuf.length - lengthPos);
					readBuffer.get(lengthBuf, lengthPos, num);
					lengthPos += num;
					if (lengthPos == lengthBuf.length) {
						// Got the entire length field; switch mode to reading the message itself
					  int length = new DataInputStream(
					  		new ByteArrayInputStream(lengthBuf)).readInt();
						if (length <= 0 || length > MAX_REPORT_LENGTH) {
							Socket sock = channel.socket();
							LOG.warn("Closing ReadReportsThread for "
								+ sock.getInetAddress() + ":" + sock.getPort() 
								+ " due to invalid length: " + length);
							key.cancel();
							channel.close();
							return;
						}
						lengthPos = 0;
						msgBuf = new byte[length];
						msgPos = 0;
					}
				}
			}
		}
	}
}
