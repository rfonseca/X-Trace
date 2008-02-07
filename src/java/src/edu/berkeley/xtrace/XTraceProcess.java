package edu.berkeley.xtrace;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An X-Trace process, returned from {@link EventContext#startProcess(String, String)}.
 * 
 * For use with the {@link EventContext#startProcess(String, String)} and
 * {@link EventContext#endProcess(XtraceProcess)} methods.
 * 
 * @see EventContext#startProcess(String, String)
 * @see EventContext#endProcess(XtraceProcess)
 * 
 * @author Matei Zaharia
 */
public class XtraceProcess {
	final Metadata startCtx;
	final String agent;
	final String name;

	XtraceProcess(Metadata startCtx, String agent, String name) {
		this.startCtx = startCtx;
		this.agent = agent;
		this.name = name;
	}
	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XtraceProcess)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph.
	 * 
	 * @see #startProcess(String, String)
	 * @param process return value from #startProcess(String, String)
	 */
	public void endProcess(XtraceProcess process) {
		endProcess(name + " end");
	}
	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XtraceProcess, String)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph. This version
	 * of the function lets the user set a label for the end node.
	 * 
	 * @see #startProcess(String, String)
	 * @param process return value from #startProcess(String, String)
	 * @param label label for the end process X-Trace node
	 */
	public void endProcess(String label) {
		if (EventContext.getContext() != null) {
			XtraceEvent evt = EventContext.createEvent(agent, label);
			evt.addEdge(startCtx);
			evt.sendReport();
		}
	}
	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #failProcess(XtraceProcess, Throwable)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph. This version
	 * of the function should be called when a process fails, to report
	 * an exception. It will add an Exception field to the X-Trace report's
	 * metadata.
	 * 
	 * @see #startProcess(String, String)
	 * @param process return value from #startProcess(String, String)
	 * @param exception reason for failure
	 */
	public void failProcess(Throwable exception) {
		if (EventContext.getContext() != null) {
			XtraceEvent evt = EventContext.createEvent(agent, name + " failed");
			evt.addEdge(startCtx);
			
			// Write stack trace to a string buffer
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			pw.flush();
			
			evt.put("Exception", sw.toString());
			evt.sendReport();
		}
	}
}
