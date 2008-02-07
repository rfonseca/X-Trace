package edu.berkeley.xtrace;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An X-Trace process, returned from {@link XTrace#startProcess(String, String)}.
 * 
 * For use with the {@link XTrace#startProcess(String, String)} and
 * {@link XTrace#endProcess(XTraceProcess)} methods.
 * 
 * @see XTrace#startProcess(String, String)
 * @see XTrace#endProcess(XTraceProcess)
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
