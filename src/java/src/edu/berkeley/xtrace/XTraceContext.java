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


package edu.berkeley.xtrace;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * High-level API for maintaining a per-thread X-Trace context (task and
 * operation ID) and reporting events.
 * 
 * Usage:
 * <ul>
 * <li> When communication is received, set the context using 
 *      <code>XTraceContext.setThreadContext()</code>.
 * <li> To record an operation, call <code>XTraceContext.logEvent()</code>. 
 *      Or, to add extra fields to the event report, call 
 *      <code>XTraceContext.createEvent()</code>, add fields to the
 *      returned {@link XTraceEvent} object, and send it using 
 *      {@link XTraceEvent#sendReport()}.
 * <li> When calling another service, get the current context's metadata using
 *      <code>XTraceContext.getThreadContext()</code> and send it to the
 *      destination service as a field in your network protocol.
 *      After receiving a reply, add an edge from both the reply's metadata and
 *      the current context in the report for the reply.
 * <li> Clear the context using <code>XTraceContext.clearThreadContext()</code>. 
 * </ul>
 * 
 * @author Matei Zaharia <matei@berkeley.edu>
 */
public class XTraceContext {
	/** Thread-local current operation context, used in logEvent. **/
	private static ThreadLocal<XTraceMetadata> context
		= new ThreadLocal<XTraceMetadata>() {
		@Override
		protected XTraceMetadata initialValue() {
			return null;
		}
	};
	
	/** Cached hostname of the current machine. **/
	private static String hostname = null;

	private static int defaultOpIdLength = 8;
	
	/**
	 * Set the X-Trace context for the current thread, to link it causally to
	 * events that may have happened in a different thread or on a different host.
	 * 
	 * @param ctx the new context
	 */
	public synchronized static void setThreadContext(XTraceMetadata ctx) {
		if (ctx != null && ctx.isValid()) {
			context.set(ctx);
		} else {
			context.set(null);
		}
	}
	
	/**
	 * Get the current thread's X-Trace context, that is, the metadata for the
	 * last event to have been logged by this thread.
	 * 
	 * @return current thread's X-Trace context
	 */
	public synchronized static XTraceMetadata getThreadContext() {
		return context.get();
	}
	
	/**
	 * Clear current thread's X-Trace context.
	 */
	public synchronized static void clearThreadContext() {
		context.set(null);
	}

	/**
	 * Creates a new task context, adds an edge from the current thread's context,
	 * sets the new context, and reports it to the X-Trace server.
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static void logEvent(String agent, String label) {
		if (!isValid()) {
			return;
		}
		createEvent(agent, label).sendReport();
	}

	/**
	 * Creates a new task context, adds an edge from the current thread's context,
	 * sets the new context, and reports it to the X-Trace server.
	 * This version of this function allows extra event fields to be specified
	 * as variable arguments after the agent and label. For example, to add a
	 * field called "DataSize" with value 4320, use
	 * 
	 * <code>XTraceContext.logEvent("agent", "label", "DataSize" 4320)</code>
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static void logEvent(String agent, String label, Object... args) {
		if (!isValid()) {
			return;
		}
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"XTraceContext.logEvent requires an even number of arguments.");
		}
		XTraceEvent event = createEvent(agent, label);
		for (int i=0; i<args.length/2; i++) {
			String key = args[2*i].toString();
			String value = args[2*i + 1].toString();
			event.put(key, value);
		}
		event.sendReport();
	}
	
	/**
	 * Creates a new event context, adds an edge from the current thread's
	 * context, and sets the new context. Returns the newly created event without
	 * reporting it. If there is no current thread context, nothing is done.
	 * 
	 * The returned event can be sent with {@link XTraceEvent#sendReport()}.
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static XTraceEvent createEvent(String agent, String label) {
		if (!isValid()) {
			return null;
		}

		XTraceMetadata oldContext = getThreadContext();
		
		int opIdLength = defaultOpIdLength;
		if (oldContext != null) {
			opIdLength = oldContext.getOpIdLength();
		}
		XTraceEvent event = new XTraceEvent(opIdLength);
		
		event.addEdge(oldContext);

		try {
			if (hostname == null) {
				hostname = InetAddress.getLocalHost().getHostName();
			}
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}

		event.put("Host", hostname);
		event.put("Agent", agent);
		event.put("Label", label);

		setThreadContext(event.getNewMetadata());
		return event;
	}

	/**
	 * Is there a context set for the current thread?
	 * 
	 * @return true if there is a current context
	 */
	public static boolean isValid() {
		return getThreadContext() != null;
	}
	
	/**
	 * Begin a "process", which will be ended with 
	 * {@link #endProcess(XTraceProcess)}, by creating an event
	 * with the given agent and label strings. This function returns an
	 * XtrMetadata object that must be passed into 
	 * {@link #endProcess(XTraceProcess)} or 
	 * {@link #failProcess(XTraceProcess, Throwable)} to create the corresponding 
	 * process-end event.
	 * 
	 * Example usage:
	 * <pre>
	 * XtraceProcess process = XTrace.startProcess("node", "action start");
	 * ...
	 * XTrace.endProcess(process);
	 * </pre>
	 * 
	 * The call to {@link #endProcess(XTraceProcess)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph.
	 * 
	 * @param agent name of current agent
	 * @param process  name of process
	 * @return the process object created
	 */
	public static XTraceProcess startProcess(String agent, String process, Object... args) {
		logEvent(agent, process + " start", args);
		return new XTraceProcess(getThreadContext(), agent, process);
	}

	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XTraceProcess)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph.
	 * 
	 * @see XTraceContext#startProcess(String, String)
	 * @param process return value from #startProcess(String, String)
	 */
	public static void endProcess(XTraceProcess process) {
		endProcess(process, process.name + " end");
	}
	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XTraceProcess, String)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph. This version
	 * of the function lets the user set a label for the end node.
	 * 
	 * @see #startProcess(String, String)
	 * @param process return value from #startProcess(String, String)
	 * @param label label for the end process X-Trace node
	 */
	public static void endProcess(XTraceProcess process, String label) {
		if (getThreadContext() != null) {
			XTraceMetadata oldContext = getThreadContext();
			XTraceEvent evt = createEvent(process.agent, label);
			if (oldContext != process.startCtx) {
				evt.addEdge(process.startCtx);	// Make sure we don't get a double edge from startCtx
			}
			evt.sendReport();
		}
	}
	
	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. 
	 * See {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #failProcess(XTraceProcess, Throwable)} will
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
	public static void failProcess(XTraceProcess process, Throwable exception) {
		if (getThreadContext() != null) {
			XTraceMetadata oldContext = getThreadContext();
			XTraceEvent evt = createEvent(process.agent, process.name + " failed");
			if (oldContext != process.startCtx) {
				evt.addEdge(process.startCtx);	// Make sure we don't get a double edge from startCtx
			}

			// Write stack trace to a string buffer
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			pw.flush();

			evt.put("Exception", IoUtil.escapeNewlines(sw.toString()));
			evt.sendReport();
		}
	}


	public static void failProcess(XTraceProcess process, String reason) {
		if (getThreadContext() != null) {
			XTraceMetadata oldContext = getThreadContext();
			XTraceEvent evt = createEvent(process.agent, process.name + " failed");
			if (oldContext != process.startCtx) {
				evt.addEdge(process.startCtx);	// Make sure we don't get a double edge from startCtx
			}
			evt.put("Reason", reason);
			evt.sendReport();
		}
	}

	public static void startTrace(String agent, String title, String... tags) {
		TaskID taskId = new TaskID(8);
		setThreadContext(new XTraceMetadata(taskId, 0L));
		XTraceEvent event = createEvent(agent, "Start Trace: " + title);
		event.put("Title", title);
		for (String tag: tags) {
			event.put("Tag", tag);
		}
		event.sendReport();
	}

	public static int getDefaultOpIdLength() {
		return defaultOpIdLength;
	}

	public static void setDefaultOpIdLength(int defaultOpIdLength) {
		XTraceContext.defaultOpIdLength = defaultOpIdLength;
	}

	public static void writeThreadContext(DataOutput out) throws IOException {
		XTraceMetadata.write(getThreadContext(), out);
	}
	
	public static void readThreadContext(DataInput in) throws IOException {
		setThreadContext(XTraceMetadata.read(in));
	}
	
	/**
	 * Replace the current context with a new one, returning the value of
	 * the old context.
	 * 
	 * @param newContext The context to replace the current one with.
	 * @return
	 */
	public synchronized static XTraceMetadata switchThreadContext(XTraceMetadata newContext) {
		XTraceMetadata oldContext = getThreadContext();
		setThreadContext(newContext);
		return oldContext;
	}
}
