package edu.berkeley.xtrace;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import edu.berkeley.xtrace.reporting.Report;
import edu.berkeley.xtrace.reporting.Reporter;

/**
 * An <code>XTraceEvent</code> makes propagating X-Trace metadata easier.
 * An application writer can initialize a new <code>XTraceEvent</code> when
 * a task begins (usually as the result of a new request arriving).
 * Each X-Trace metadata extracted from the request (or requests, if the
 * task is made up of concurrent requests) is added to this context
 * via the <code>addEdge</code> method.  Additionally, information
 * in the form of key-value pairs can be supplied as well.
 * <p>
 * The metadata that should be propagated into any new requests
 * is obtained via the <code>getNewMetadata</code> method.  Lastly,
 * a report obtained via <code>getNewReport</code> should be sent
 * to the report context, which will forward it to the reporting
 * infrastructure.
 * 
 * @author George Porter <gporter@cs.berkeley.edu>
 */
public class XTraceEvent {
	
	/** 
	 * Thread-local random number generator, seeded with machine name
	 * as well as current time. 
	 *
	 **/
	private static ThreadLocal<Random> random
		= new ThreadLocal<Random>() {
		@Override
		protected Random initialValue() {
			// It's very important to have a different random number seed for each thread,
			// so that we don't get OpID collisions in the same X-Trace graph. We therefore
			// base the seed on our hostname, process ID, thread ID, and current time.
			// Java provides no way to get the current PID; however, we can get something
			// similar on the Sun JVM by looking at the RuntimeMXBean (whose name will be
			// something like pid@hostname). This is the only solution I've found that
			// doesn't involve writing native code or exec'ing another process... (Matei)
			int processId = ManagementFactory.getRuntimeMXBean().getName().hashCode();
			try {
				return new Random(++threadId
						+ processId
						+ System.nanoTime()
						+ Thread.currentThread().getId()
						+ InetAddress.getLocalHost().getHostName().hashCode() );
			} catch (UnknownHostException e) {
				// Failed to get local host name; just use the other pieces
				return new Random(++threadId
						+ processId
						+ System.nanoTime()
						+ Thread.currentThread().getId());
			}
		}
	};
	private static volatile long threadId = 0;
	
	private Report report;
	private byte[] myOpId;
	private boolean willReport;

	/**
	 * Initialize a new XTraceEvent.  This should be done for each
	 * request or task processed by this node.
	 *
	 */
	public XTraceEvent(int opIdLength) {
		report = new Report();
		myOpId = new byte[opIdLength];
		random.get().nextBytes(myOpId);
		willReport = true;
	}
	/**
	* If any edge added to this event has a higher severity than the threshold (default),
	* this event will not be reported.
	*/
	public void addEdge(XTraceMetadata xtr) {
		if (xtr == null || !xtr.isValid()) {
			return;
		}
		// check for a severity level option field
		OptionField[] options = xtr.getOptions();
		if (options != null) {
			for (int i=0; i <xtr.getNumOptions(); i++) {
				if (options[i].getType()-OptionField.SEVERITY == 0) {
					int severity = (int) options[i].getPayload()[0] & 0xFF;
					willReport = severity < OptionSeverity.DEFAULT;
					report.put("Severity", severity+"");
				}
			}
		}
		
		XTraceMetadata newmd = new XTraceMetadata(xtr);
		newmd.setOpId(myOpId);
		
		report.put("X-Trace", newmd.toString(), false);
		report.put("Edge", xtr.getOpIdString());
	}

	public void put(String key, String value) {
		report.put(key, value);
	}
	
	public XTraceMetadata getNewMetadata() {
		XTraceMetadata xtr = report.getMetadata();
		XTraceMetadata xtr2;
		
		/* If we don't know the task id, return an
		 * invalid metadata
		 */
		if (xtr == null) {
			return new XTraceMetadata();
		}
		
		xtr2 = new XTraceMetadata(xtr);
		xtr2.setOpId(myOpId);
		
		return xtr2;
	}
	
	/**
	 * Set the event's metadata to a given value (used for tasks with no edges).
	 */
	public void setMetadata(XTraceMetadata xtr) {
		// TODO: the following line isn't defensive.  Fix by copying the
		// bytes, rather than just the reference.
		myOpId = xtr.getOpId();
		report.put("X-Trace", xtr.toString(), false);
	}
	
	/**
	 * Add a timestamp property with the current time.
	 */
	private void setTimestamp() {
		long time = System.currentTimeMillis();
		String value = String.format("%d.%03d", time/1000, time%1000);
		report.put("Timestamp", value, false);
	}
	
	/**
	 * Create a {@link edu.berkeley.xtrace.reporting.Report} representing this
	 * event, to send to the X-Trace back end. 
	 */
	public Report createReport() {
		setTimestamp();
		return report;
	}
	
	public void sendReport() {
		setTimestamp();
		if (!willReport) { return; }
		Reporter.getReporter().sendReport(report);
	}
}
