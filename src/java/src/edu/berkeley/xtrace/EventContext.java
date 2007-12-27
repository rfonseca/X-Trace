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

import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.berkeley.xtrace.reporting.ReportingContext;

/**
 * High-level API for maintaining a per-thread X-Trace context (task and
 * operation ID) and reporting events.
 * 
 * Usage:
 * <ul>
 * <li> When communication is received, set the context using 
 *      <code>EventContext.set()</code>.
 * <li> To record an operation, call <code>EventContext.logEvent()</code>. 
 *      Or, to add extra fields to the event report, call 
 *      <code>EventContext.createEvent()</code>, add fields to the
 *      returned {@link XtraceEvent} object, and send it using 
 *      {@link XtraceEvent#sendReport()}.
 * <li> When calling another service, get the current context using
 *      <code>EventContext.get()</code> and send it as metadata. After receiving a
 *      reply, add an edge from both the reply's metadata and the current context
 *      in the report for the reply.
 * <li> Clear the context using <code>EventContext.clear()</code>. 
 * </ul>
 * 
 * @author Matei Zaharia <matei@berkeley.edu>
 */
public class EventContext {
	/** Thread-local current operation context, used in logEvent. **/
	private static ThreadLocal<Metadata> context
		= new ThreadLocal<Metadata>() {
		@Override
		protected Metadata initialValue() {
			return null;
		}
	};
	
	/** Cached hostname of the current machine. **/
	private static String hostname = null;
	
	/**
	 * Set the X-Trace context for logEvent.
	 * 
	 * @param ctx the new context
	 */
	public static void setContext(Metadata ctx) {
		if (ctx != null && ctx.isValid()) {
			context.set(ctx);
		} else {
			context.set(null);
		}
	}
	
	/**
	 * Get the current X-Trace context.
	 * 
	 * @return current X-Trace context
	 */
	public static Metadata getContext() {
		return context.get();
	}
	
	/**
	 * Clear current X-Trace context to an invalid task and operation ID.
	 */
	public static void clearContext() {
		context.set(null);
	}

	/**
	 * Creates a new task context, adds an edge from the current context,
	 * sets the new context, and reports it to the X-Trace process.
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static void logEvent(String agent, String label) {
		if (!isValid())
			return;
		ReportingContext.getReportCtx().sendReport(
				createEvent(agent, label).getNewReport());
	}
	
	/**
	 * Creates a new event context, adds an edge from the current context, and
	 * sets the new context. Returns the newly created event without reporting
	 * it.
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static XtraceEvent createEvent(String agent, String label) {
	    
	    if (!isValid()) {
	        return null;
	    }
	    
	    XtraceEvent event = new XtraceEvent(4);
	    event.addEdge(getContext());
	    
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
	    
	    setContext(event.getNewMetadata());
	    return event;
	}

	/**
	 * Is there a valid X-Trace context?
	 * 
	 * @return true if getContext() is a valid context
	 */
	public static boolean isValid() {
		return getContext() != null;
	}
	
	/**
	 * Begin a "process", which will be ended with 
	 * {@link #endProcess(XtraceProcess)}, by creating an event
	 * with the given agent and label strings. This function returns an
	 * XtrMetadata object that must be passed into 
	 * {@link #endProcess(XtraceProcess)} or 
	 * {@link #failProcess(XtraceProcess, Throwable)} to create the corresponding 
	 * process-end event.
	 * 
	 * Example usage:
	 * <pre>
	 * XtrMetadata startCtx = EventContext.startProcess("node", "action start");
	 * ...
	 * EventContext.endProcess(startCtx);
	 * </pre>
	 * 
	 * The call to {@link #endProcess(XtraceProcess)} will
	 * create an edge from both the start context and the current X-Trace
	 * context, forming a subprocess box on the X-Trace graph.
	 * 
	 * @param agent name of current agent
	 * @param process  name of process
	 * @return the process object created
	 */
	public static XtraceProcess startProcess(String agent, String process) {
		logEvent(agent, process + " start");
		return new XtraceProcess(getContext(), agent, process);
	}
}
