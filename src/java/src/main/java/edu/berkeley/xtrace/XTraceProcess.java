package edu.berkeley.xtrace;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An X-Trace process, returned from {@link XTraceContext#startProcess(String, String)}.
 * 
 * For use with the {@link XTraceContext#startProcess(String, String)} and
 * {@link XTraceContext#endProcess(XTraceProcess)} methods.
 * 
 * @see XTraceContext#startProcess(String, String)
 * @see XTraceContext#endProcess(XTraceProcess)
 * 
 * @author Matei Zaharia
 */
public class XTraceProcess {
	final XTraceMetadata startCtx;
	final String agent;
	final String name;

	XTraceProcess(XTraceMetadata startCtx, String agent, String name) {
		this.startCtx = startCtx;
		this.agent = agent;
		this.name = name;
	}
}
