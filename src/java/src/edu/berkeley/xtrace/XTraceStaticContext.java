package edu.berkeley.xtrace;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class XTraceStaticContext {
	
	/** Cached hostname of the current machine. **/
	private static String hostname = null;
	
	public static XTraceMetadata logEvent(XTraceMetadata md, String agent, String label) {
		if (md == null || !md.isValid()) {
			return null;
		}
		XTraceEvent evt = createEvent(md, agent, label);
		evt.sendReport();
		return evt.getNewMetadata();
	}
	
	public static XTraceEvent createEvent(XTraceMetadata oldContext, String agent, String label) {
		if (oldContext == null || !oldContext.isValid()) {
			return null;
		}
		
	  int opIdLength = oldContext.getOpIdLength();
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
		
		return event;
	}
	
	/**
	 * Creates a new task context, adds an edge from the current thread's context,
	 * sets the new context, and reports it to the X-Trace server.
	 * This version of this function allows extra event fields to be specified
	 * as variable arguments after the agent and label. For example, to add a
	 * field called "DataSize" with value 4320, use
	 * 
	 * <code>XTraceContext.logEvent(oldContext, "agent", "label", "DataSize" 4320)</code>
	 * 
	 * @param agent name of current agent
	 * @param label description of the task
	 */
	public static XTraceMetadata logEvent(XTraceMetadata oldContext, String agent,
			                                  String label, Object... args) {
		if (oldContext == null || !oldContext.isValid()) {
			return null;
		}
		
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"XTraceStaticContext.logEvent requires an even number of arguments.");
		}
		
		XTraceEvent event = createEvent(oldContext, agent, label);
		for (int i=0; i<args.length/2; i++) {
			String key = args[2*i].toString();
			String value = args[2*i + 1].toString();
			event.put(key, value);
		}
		event.sendReport();
		return event.getNewMetadata();
	}
}
